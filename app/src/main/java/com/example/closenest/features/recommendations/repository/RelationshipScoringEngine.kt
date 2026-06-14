package com.example.closenest.features.recommendations.repository

import com.example.closenest.features.recommendations.model.RelationshipActivityContext
import com.example.closenest.features.recommendations.model.RelationshipComputedMetrics
import com.example.closenest.features.recommendations.model.RelationshipEvaluationContext
import com.example.closenest.features.recommendations.model.RelationshipEvaluationProfile
import com.example.closenest.features.recommendations.model.RelationshipImportantSignals
import com.example.closenest.features.recommendations.model.RelationshipReflectionContext
import com.example.closenest.features.relationships.model.RelationshipPriority
import com.example.closenest.features.relationships.model.RelationshipProfile
import com.example.closenest.features.relationships.model.RelationshipTag
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToInt

internal object RelationshipScoringEngine {
    fun buildEvaluationContexts(
        relationships: List<RelationshipProfile>,
        activities: List<StoredRelationshipActivity>,
        reflections: List<StoredReflectionEntry>,
        appointments: List<StoredAppointmentEntry>,
        nowMillis: Long
    ): List<RelationshipEvaluationContext> {
        val zoneId = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()

        return relationships.map { relationship ->
            val relationshipActivities = activities
                .filter { activity -> activity.contactId == relationship.id }
                .sortedByDescending { activity -> activity.createdAtMillis }
            val relationshipReflections = reflections
                .filter { reflection -> relationship.id in reflection.contactIds }
                .sortedByDescending { reflection -> reflection.createdAtMillis }
            val relationshipAppointments = appointments
                .filter { appointment -> appointment.contactId == relationship.id }
                .filter { appointment -> appointment.appointmentDateMillis >= nowMillis }
                .sortedBy { appointment -> appointment.appointmentDateMillis }

            buildEvaluationContext(
                relationship = relationship,
                activities = relationshipActivities,
                reflections = relationshipReflections,
                appointments = relationshipAppointments,
                today = today,
                zoneId = zoneId
            )
        }
    }

    fun shouldGenerateRecommendation(context: RelationshipEvaluationContext): Boolean {
        val metrics = context.computedMetrics
        val signals = context.importantSignals
        return metrics.attentionScore >= AttentionGenerationThreshold ||
            signals.hasBirthdaySoon ||
            signals.hasMemoryAnniversarySoon ||
            signals.hasUpcomingAppointment
    }

    fun recommendationType(context: RelationshipEvaluationContext): String {
        val signals = context.importantSignals
        return when {
            signals.hasBirthdaySoon -> RecommendationTypeBirthday
            signals.hasMemoryAnniversarySoon -> RecommendationTypeMemoryReminder
            signals.hasUpcomingAppointment -> RecommendationTypeAppointment
            context.computedMetrics.attentionScore >= AttentionGenerationThreshold -> RecommendationTypeCheckIn
            else -> RecommendationTypeRelationshipReview
        }
    }

    private fun buildEvaluationContext(
        relationship: RelationshipProfile,
        activities: List<StoredRelationshipActivity>,
        reflections: List<StoredReflectionEntry>,
        appointments: List<StoredAppointmentEntry>,
        today: LocalDate,
        zoneId: ZoneId
    ): RelationshipEvaluationContext {
        val touchEvents = buildList {
            activities.forEach { activity ->
                add(TouchEvent(type = activity.type, occurredAtMillis = activity.createdAtMillis))
            }
            reflections.forEach { reflection ->
                add(TouchEvent(type = TouchTypeReflection, occurredAtMillis = reflection.createdAtMillis))
            }
        }.sortedByDescending { event -> event.occurredAtMillis }

        val lastTouch = touchEvents.firstOrNull()
        val hasLoggedTouch = lastTouch != null
        val fallbackInactiveMillis = lastTouch?.occurredAtMillis ?: relationship.createdAtMillis
        val inactiveDays = daysBetween(fallbackInactiveMillis, today, zoneId)
        val daysSinceLastTouch = lastTouch?.let { event ->
            daysBetween(event.occurredAtMillis, today, zoneId)
        }
        val expectedTouchIntervalDays = expectedTouchIntervalDays(
            tag = relationship.tag,
            priority = relationship.priority
        )
        val overdueDays = max(0, inactiveDays - expectedTouchIntervalDays)

        val touchCountLast7Days = touchEvents.count { event ->
            daysBetween(event.occurredAtMillis, today, zoneId) < 7
        }
        val touchCountLast30Days = touchEvents.count { event ->
            daysBetween(event.occurredAtMillis, today, zoneId) < 30
        }
        val touchCountLast90Days = touchEvents.count { event ->
            daysBetween(event.occurredAtMillis, today, zoneId) < 90
        }

        val priorityScore = relationship.priority.toPriorityScore()
        val recencyScore = recencyClosenessScore(
            hasLoggedTouch = hasLoggedTouch,
            inactiveDays = inactiveDays,
            expectedTouchIntervalDays = expectedTouchIntervalDays
        )
        val frequencyScore = frequencyScore(
            touchCountLast30Days = touchCountLast30Days,
            expectedTouchIntervalDays = expectedTouchIntervalDays
        )
        val profileRichnessScore = profileRichnessScore(relationship)
        val specialDateResult = specialDateResult(
            relationship = relationship,
            activities = activities,
            today = today,
            zoneId = zoneId
        )
        val appointmentResult = upcomingAppointmentResult(
            appointments = appointments,
            today = today,
            zoneId = zoneId
        )
        val recentMoodContextScore = recentMoodContextScore(
            reflections = reflections,
            today = today,
            zoneId = zoneId
        )

        val bondScore = weightedScore(
            priorityScore to 0.30,
            relationship.tag.toTagScore() to 0.20,
            recencyScore to 0.25,
            frequencyScore to 0.15,
            profileRichnessScore to 0.10
        )
        val overdueScore = overdueScore(
            inactiveDays = inactiveDays,
            expectedTouchIntervalDays = expectedTouchIntervalDays
        )
        val attentionScore = weightedScore(
            overdueScore to 0.45,
            priorityScore to 0.20,
            specialDateResult.score to 0.20,
            appointmentResult.score to 0.10,
            recentMoodContextScore to 0.05
        )

        return RelationshipEvaluationContext(
            currentDateIso = today.toString(),
            relationship = RelationshipEvaluationProfile(
                id = relationship.id,
                name = relationship.name,
                tag = relationship.tag.name,
                priority = relationship.priority.name,
                birthdayIso = relationship.birthdayIso,
                interests = relationship.interests,
                notes = relationship.notes,
                createdAtMillis = relationship.createdAtMillis
            ),
            computedMetrics = RelationshipComputedMetrics(
                bondScore = bondScore,
                attentionScore = attentionScore,
                bondLabel = bondScore.toBondLabel(),
                attentionLabel = attentionScore.toAttentionLabel(
                    hasBirthdaySoon = specialDateResult.hasBirthdaySoon,
                    hasMemoryAnniversarySoon = specialDateResult.hasMemoryAnniversarySoon
                ),
                hasLoggedTouch = hasLoggedTouch,
                lastTouchAtMillis = lastTouch?.occurredAtMillis,
                lastTouchType = lastTouch?.type,
                daysSinceLastTouch = daysSinceLastTouch,
                expectedTouchIntervalDays = expectedTouchIntervalDays,
                overdueDays = overdueDays,
                touchCountLast7Days = touchCountLast7Days,
                touchCountLast30Days = touchCountLast30Days,
                touchCountLast90Days = touchCountLast90Days,
                priorityScore = priorityScore,
                recencyScore = recencyScore,
                frequencyScore = frequencyScore,
                specialDateScore = specialDateResult.score,
                profileRichnessScore = profileRichnessScore
            ),
            recentActivities = activities
                .take(MaxRecentActivities)
                .map { activity ->
                    RelationshipActivityContext(
                        type = activity.type,
                        title = activity.title,
                        note = activity.note,
                        location = activity.location,
                        createdAtMillis = activity.createdAtMillis
                    )
                },
            importantSignals = RelationshipImportantSignals(
                birthdayInDays = specialDateResult.birthdayInDays,
                hasBirthdaySoon = specialDateResult.hasBirthdaySoon,
                memoryAnniversaryInDays = specialDateResult.memoryAnniversaryInDays,
                hasMemoryAnniversarySoon = specialDateResult.hasMemoryAnniversarySoon,
                upcomingAppointmentInDays = appointmentResult.upcomingAppointmentInDays,
                hasUpcomingAppointment = appointmentResult.hasUpcomingAppointment
            ),
            reflectionContext = reflections
                .take(MaxRecentReflections)
                .map { reflection ->
                    RelationshipReflectionContext(
                        mood = reflection.mood,
                        feelings = reflection.feelings,
                        sources = reflection.sources,
                        createdAtMillis = reflection.createdAtMillis
                    )
                }
        )
    }

    private fun expectedTouchIntervalDays(
        tag: RelationshipTag,
        priority: RelationshipPriority
    ): Int {
        val baseDays = when (tag) {
            RelationshipTag.Partner -> 3
            RelationshipTag.CloseFriend -> 7
            RelationshipTag.Family -> 10
            RelationshipTag.Friend -> 14
            RelationshipTag.Classmate,
            RelationshipTag.Coworker -> 21
            RelationshipTag.Mentor,
            RelationshipTag.Other -> 30
        }
        val multiplier = when (priority) {
            RelationshipPriority.High -> 0.75
            RelationshipPriority.Medium -> 1.0
            RelationshipPriority.Low -> 1.25
        }
        return max(1, (baseDays * multiplier).roundToInt())
    }

    private fun RelationshipPriority.toPriorityScore(): Int {
        return when (this) {
            RelationshipPriority.High -> 90
            RelationshipPriority.Medium -> 60
            RelationshipPriority.Low -> 35
        }
    }

    private fun RelationshipTag.toTagScore(): Int {
        return when (this) {
            RelationshipTag.Partner -> 95
            RelationshipTag.CloseFriend -> 90
            RelationshipTag.Family -> 85
            RelationshipTag.Friend -> 65
            RelationshipTag.Mentor -> 60
            RelationshipTag.Classmate -> 50
            RelationshipTag.Coworker -> 45
            RelationshipTag.Other -> 40
        }
    }

    private fun recencyClosenessScore(
        hasLoggedTouch: Boolean,
        inactiveDays: Int,
        expectedTouchIntervalDays: Int
    ): Int {
        if (!hasLoggedTouch) return 10
        val ratio = inactiveDays.toDouble() / expectedTouchIntervalDays
        return when {
            ratio <= 0.5 -> 100
            ratio <= 1.0 -> 85
            ratio <= 2.0 -> 60
            ratio <= 4.0 -> 35
            else -> 15
        }
    }

    private fun frequencyScore(
        touchCountLast30Days: Int,
        expectedTouchIntervalDays: Int
    ): Int {
        val expectedMonthlyTouches = max(1, (30.0 / expectedTouchIntervalDays).roundToInt())
        return ((touchCountLast30Days.toDouble() / expectedMonthlyTouches) * 100)
            .roundToInt()
            .coerceIn(0, 100)
    }

    private fun profileRichnessScore(relationship: RelationshipProfile): Int {
        val signals = listOf(
            relationship.birthdayIso,
            relationship.notes,
            relationship.phoneNumber ?: relationship.email,
            relationship.interests.takeIf { interests -> interests.isNotEmpty() }?.joinToString()
        )
        return ((signals.count { value -> !value.isNullOrBlank() }.toDouble() / signals.size) * 100)
            .roundToInt()
    }

    private fun specialDateResult(
        relationship: RelationshipProfile,
        activities: List<StoredRelationshipActivity>,
        today: LocalDate,
        zoneId: ZoneId
    ): SpecialDateResult {
        val birthdayInDays = relationship.birthdayIso
            ?.let { birthdayIso -> runCatching { LocalDate.parse(birthdayIso) }.getOrNull() }
            ?.let { birthday -> daysUntilNextMonthDay(birthday, today) }
        val birthdayScore = when (birthdayInDays) {
            null -> 0
            0 -> 100
            in 1..3 -> 90
            in 4..7 -> 75
            in 8..14 -> 45
            else -> 0
        }

        val memoryAnniversaryInDays = activities
            .filter { activity -> activity.kind == StoredActivityKind.Memory }
            .mapNotNull { activity ->
                val memoryDate = Instant.ofEpochMilli(activity.createdAtMillis)
                    .atZone(zoneId)
                    .toLocalDate()
                val memoryAgeDays = ChronoUnit.DAYS.between(memoryDate, today).toInt()
                if (memoryAgeDays >= MinimumMemoryAgeForAnniversaryDays) {
                    daysUntilNextMonthDay(memoryDate, today)
                } else {
                    null
                }
            }
            .minOrNull()
        val memoryScore = when (memoryAnniversaryInDays) {
            null -> 0
            0 -> 90
            in 1..3 -> 75
            in 4..7 -> 55
            else -> 0
        }

        return SpecialDateResult(
            birthdayInDays = birthdayInDays,
            hasBirthdaySoon = birthdayInDays != null && birthdayInDays in 0..7,
            memoryAnniversaryInDays = memoryAnniversaryInDays,
            hasMemoryAnniversarySoon = memoryAnniversaryInDays != null && memoryAnniversaryInDays in 0..7,
            score = max(birthdayScore, memoryScore)
        )
    }

    private fun upcomingAppointmentResult(
        appointments: List<StoredAppointmentEntry>,
        today: LocalDate,
        zoneId: ZoneId
    ): AppointmentResult {
        val upcomingAppointmentInDays = appointments
            .map { appointment ->
                daysUntil(appointment.appointmentDateMillis, today, zoneId)
            }
            .filter { days -> days >= 0 }
            .minOrNull()
        val score = when (upcomingAppointmentInDays) {
            null -> 0
            0 -> 90
            in 1..3 -> 75
            in 4..7 -> 55
            else -> 0
        }
        return AppointmentResult(
            upcomingAppointmentInDays = upcomingAppointmentInDays,
            hasUpcomingAppointment = upcomingAppointmentInDays != null && upcomingAppointmentInDays in 0..7,
            score = score
        )
    }

    private fun recentMoodContextScore(
        reflections: List<StoredReflectionEntry>,
        today: LocalDate,
        zoneId: ZoneId
    ): Int {
        val recentReflections = reflections.filter { reflection ->
            daysBetween(reflection.createdAtMillis, today, zoneId) <= RecentReflectionWindowDays
        }
        if (recentReflections.isEmpty()) return 0

        val joinedSignals = recentReflections
            .flatMap { reflection -> listOf(reflection.mood) + reflection.feelings }
            .joinToString(separator = " ")
            .lowercase()

        return when {
            NegativeMoodSignals.any { signal -> signal in joinedSignals } -> 75
            PositiveMoodSignals.any { signal -> signal in joinedSignals } -> 35
            else -> 45
        }
    }

    private fun overdueScore(
        inactiveDays: Int,
        expectedTouchIntervalDays: Int
    ): Int {
        return ((inactiveDays.toDouble() / expectedTouchIntervalDays) * 100)
            .roundToInt()
            .coerceIn(0, 100)
    }

    private fun daysUntilNextMonthDay(
        sourceDate: LocalDate,
        today: LocalDate
    ): Int {
        var nextDate = sourceDate.withYear(today.year)
        if (nextDate.isBefore(today)) {
            nextDate = nextDate.plusYears(1)
        }
        return ChronoUnit.DAYS.between(today, nextDate).toInt()
    }

    private fun daysBetween(
        millis: Long,
        today: LocalDate,
        zoneId: ZoneId
    ): Int {
        val date = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
        return ChronoUnit.DAYS.between(date, today).toInt()
    }

    private fun daysUntil(
        millis: Long,
        today: LocalDate,
        zoneId: ZoneId
    ): Int {
        val date = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
        return ChronoUnit.DAYS.between(today, date).toInt()
    }

    private fun weightedScore(vararg parts: Pair<Int, Double>): Int {
        return parts.sumOf { (score, weight) -> score * weight }
            .roundToInt()
            .coerceIn(0, 100)
    }

    private fun Int.toBondLabel(): String {
        return when {
            this >= 75 -> "close"
            this >= 50 -> "warm"
            this >= 30 -> "light"
            else -> "new_or_sparse"
        }
    }

    private fun Int.toAttentionLabel(
        hasBirthdaySoon: Boolean,
        hasMemoryAnniversarySoon: Boolean
    ): String {
        return when {
            hasBirthdaySoon -> "upcoming_birthday"
            hasMemoryAnniversarySoon -> "memory_reminder"
            this >= 70 -> "needs_attention"
            this >= 50 -> "watch"
            else -> "stable"
        }
    }
}

internal data class StoredRelationshipActivity(
    val contactId: String,
    val contactName: String?,
    val kind: StoredActivityKind,
    val type: String,
    val title: String?,
    val note: String?,
    val location: String?,
    val createdAtMillis: Long
)

internal data class StoredReflectionEntry(
    val contactIds: List<String>,
    val contactNames: List<String>,
    val mood: String,
    val feelings: List<String>,
    val sources: List<String>,
    val createdAtMillis: Long
)

internal data class StoredAppointmentEntry(
    val contactId: String?,
    val name: String,
    val location: String?,
    val appointmentDateMillis: Long
)

internal enum class StoredActivityKind {
    Interaction,
    Memory
}

private data class TouchEvent(
    val type: String,
    val occurredAtMillis: Long
)

private data class SpecialDateResult(
    val birthdayInDays: Int?,
    val hasBirthdaySoon: Boolean,
    val memoryAnniversaryInDays: Int?,
    val hasMemoryAnniversarySoon: Boolean,
    val score: Int
)

private data class AppointmentResult(
    val upcomingAppointmentInDays: Int?,
    val hasUpcomingAppointment: Boolean,
    val score: Int
)

internal const val RecommendationTypeCheckIn = "CHECK_IN"
internal const val RecommendationTypeMemoryReminder = "MEMORY_REMINDER"
internal const val RecommendationTypeBirthday = "BIRTHDAY"
internal const val RecommendationTypeAppointment = "APPOINTMENT"
internal const val RecommendationTypeRelationshipReview = "RELATIONSHIP_REVIEW"

private const val TouchTypeReflection = "reflection"
private const val AttentionGenerationThreshold = 50
private const val MaxRecentActivities = 5
private const val MaxRecentReflections = 3
private const val MinimumMemoryAgeForAnniversaryDays = 30
private const val RecentReflectionWindowDays = 14

private val NegativeMoodSignals = listOf(
    "khó chịu",
    "rất khó chịu",
    "buồn",
    "lo lắng",
    "cô đơn",
    "mặc cảm"
)

private val PositiveMoodSignals = listOf(
    "dễ chịu",
    "rất dễ chịu",
    "vui",
    "biết ơn",
    "nhẹ nhõm",
    "bình yên"
)
