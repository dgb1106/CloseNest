package com.example.closenest.features.recommendations.model

data class RelationshipRecommendation(
    val id: String,
    val userId: String,
    val relationshipId: String,
    val relationshipName: String,
    val relationshipAvatarUrl: String?,
    val type: String,
    val status: String,
    val relationshipStatus: String,
    val bondScore: Int,
    val attentionScore: Int,
    val bondLabel: String,
    val attentionLabel: String,
    val summary: String,
    val reasons: List<String>,
    val suggestedActions: List<RecommendationAction>,
    val primaryActionType: String,
    val primaryActionLabel: String,
    val notificationTitle: String,
    val notificationDescription: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val expiresAtMillis: Long?,
    val source: String,
    val model: String,
    val promptVersion: String
)

data class RecommendationAction(
    val type: String,
    val priority: String,
    val title: String,
    val suggestedText: String?
)

data class RelationshipRecommendationDraft(
    val relationshipStatus: String,
    val summary: String,
    val reasons: List<String>,
    val suggestedActions: List<RecommendationAction>,
    val primaryActionType: String,
    val primaryActionLabel: String,
    val notificationTitle: String,
    val notificationDescription: String
)

data class RelationshipEvaluationContext(
    val currentDateIso: String,
    val relationship: RelationshipEvaluationProfile,
    val computedMetrics: RelationshipComputedMetrics,
    val recentActivities: List<RelationshipActivityContext>,
    val importantSignals: RelationshipImportantSignals,
    val reflectionContext: List<RelationshipReflectionContext>
)

data class RelationshipEvaluationProfile(
    val id: String,
    val name: String,
    val tag: String,
    val priority: String,
    val birthdayIso: String?,
    val interests: List<String>,
    val notes: String?,
    val createdAtMillis: Long
)

data class RelationshipComputedMetrics(
    val bondScore: Int,
    val attentionScore: Int,
    val bondLabel: String,
    val attentionLabel: String,
    val hasLoggedTouch: Boolean,
    val lastTouchAtMillis: Long?,
    val lastTouchType: String?,
    val daysSinceLastTouch: Int?,
    val expectedTouchIntervalDays: Int,
    val overdueDays: Int,
    val touchCountLast7Days: Int,
    val touchCountLast30Days: Int,
    val touchCountLast90Days: Int,
    val priorityScore: Int,
    val recencyScore: Int,
    val frequencyScore: Int,
    val specialDateScore: Int,
    val profileRichnessScore: Int
)

data class RelationshipActivityContext(
    val type: String,
    val title: String?,
    val note: String?,
    val location: String?,
    val createdAtMillis: Long
)

data class RelationshipImportantSignals(
    val birthdayInDays: Int?,
    val hasBirthdaySoon: Boolean,
    val memoryAnniversaryInDays: Int?,
    val hasMemoryAnniversarySoon: Boolean,
    val upcomingAppointmentInDays: Int?,
    val hasUpcomingAppointment: Boolean
)

data class RelationshipReflectionContext(
    val mood: String,
    val feelings: List<String>,
    val sources: List<String>,
    val createdAtMillis: Long
)
