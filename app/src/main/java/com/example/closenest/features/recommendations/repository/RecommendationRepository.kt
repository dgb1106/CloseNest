package com.example.closenest.features.recommendations.repository

import com.example.closenest.features.recommendations.model.RecommendationAction
import com.example.closenest.features.recommendations.model.RelationshipActivityContext
import com.example.closenest.features.recommendations.model.RelationshipComputedMetrics
import com.example.closenest.features.recommendations.model.RelationshipEvaluationContext
import com.example.closenest.features.recommendations.model.RelationshipImportantSignals
import com.example.closenest.features.recommendations.model.RelationshipRecommendation
import com.example.closenest.features.relationships.model.RelationshipPriority
import com.example.closenest.features.relationships.model.RelationshipProfile
import com.example.closenest.features.relationships.model.RelationshipTag
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

interface RecommendationRepository {
    fun observeRecommendations(): Flow<List<RelationshipRecommendation>>

    suspend fun refreshRelationshipRecommendations(
        nowMillis: Long = System.currentTimeMillis(),
        maxRecommendations: Int = DefaultMaxRecommendations
    ): List<RelationshipRecommendation>
}

class FirebaseRecommendationRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val aiService: RelationshipRecommendationAiService
) : RecommendationRepository {
    override fun observeRecommendations(): Flow<List<RelationshipRecommendation>> = callbackFlow {
        var recommendationRegistration: ListenerRegistration? = null

        fun stopRecommendationListener() {
            recommendationRegistration?.remove()
            recommendationRegistration = null
        }

        fun listenForRecommendations(userId: String) {
            stopRecommendationListener()
            recommendationRegistration = recommendationsCollection(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val recommendations = snapshot
                        ?.documents
                        .orEmpty()
                        .mapNotNull { document -> document.toRelationshipRecommendation(defaultUserId = userId) }
                        .sortedByDescending { recommendation -> recommendation.updatedAtMillis }

                    trySend(recommendations)
                }
        }

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val userId = firebaseAuth.currentUser?.uid
            if (userId == null) {
                stopRecommendationListener()
                trySend(emptyList())
            } else {
                listenForRecommendations(userId)
            }
        }

        auth.addAuthStateListener(authListener)

        awaitClose {
            stopRecommendationListener()
            auth.removeAuthStateListener(authListener)
        }
    }

    override suspend fun refreshRelationshipRecommendations(
        nowMillis: Long,
        maxRecommendations: Int
    ): List<RelationshipRecommendation> {
        val userId = auth.currentUser?.uid ?: error("No signed-in Firebase user.")
        val relationships = loadRelationships(userId)
        if (relationships.isEmpty()) return emptyList()

        val contexts = RelationshipScoringEngine.buildEvaluationContexts(
            relationships = relationships,
            activities = loadActivities(userId),
            reflections = loadReflections(userId),
            appointments = loadAppointments(userId, relationships),
            nowMillis = nowMillis
        )

        val candidates = contexts
            .filter(RelationshipScoringEngine::shouldGenerateRecommendation)
            .sortedWith(
                compareByDescending<RelationshipEvaluationContext> { context ->
                    context.computedMetrics.attentionScore
                }.thenBy { context ->
                    context.relationship.name.lowercase()
                }
            )
            .take(maxRecommendations)

        val savedRecommendations = candidates.map { context ->
            val aiReply = aiService.evaluate(context)
            saveRecommendation(
                userId = userId,
                context = context,
                aiReply = aiReply,
                nowMillis = nowMillis
            )
        }

        expireMissingActiveRecommendations(
            userId = userId,
            activeRecommendationIds = savedRecommendations.map { recommendation -> recommendation.id }.toSet(),
            nowMillis = nowMillis
        )

        return savedRecommendations
    }

    private suspend fun saveRecommendation(
        userId: String,
        context: RelationshipEvaluationContext,
        aiReply: RelationshipRecommendationAiReply,
        nowMillis: Long
    ): RelationshipRecommendation {
        val recommendationId = context.toRecommendationId()
        val document = recommendationsCollection(userId).document(recommendationId)
        val existing = document.get().awaitResult()
        val savedStatus = existing.preservedStatus(nowMillis)
        val createdAtMillis = existing.getMillis(FieldCreatedAtMillis) ?: nowMillis
        val expiresAtMillis = nowMillis + RecommendationExpiryMillis
        val recommendationType = RelationshipScoringEngine.recommendationType(context)

        val recommendation = RelationshipRecommendation(
            id = recommendationId,
            userId = userId,
            relationshipId = context.relationship.id,
            relationshipName = context.relationship.name,
            relationshipAvatarUrl = null,
            type = recommendationType,
            status = savedStatus,
            relationshipStatus = aiReply.draft.relationshipStatus,
            bondScore = context.computedMetrics.bondScore,
            attentionScore = context.computedMetrics.attentionScore,
            bondLabel = context.computedMetrics.bondLabel,
            attentionLabel = context.computedMetrics.attentionLabel,
            summary = aiReply.draft.summary,
            reasons = aiReply.draft.reasons,
            suggestedActions = aiReply.draft.suggestedActions,
            primaryActionType = aiReply.draft.primaryActionType,
            primaryActionLabel = aiReply.draft.primaryActionLabel,
            notificationTitle = aiReply.draft.notificationTitle,
            notificationDescription = aiReply.draft.notificationDescription,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = nowMillis,
            expiresAtMillis = expiresAtMillis,
            source = RecommendationSourceLlm,
            model = aiReply.modelName,
            promptVersion = aiService.promptVersion
        )

        document.set(
            recommendation.toFirestoreMap(
                context = context,
                rawLlmJson = aiReply.rawJson
            ),
            SetOptions.merge()
        ).awaitCompletion()

        return recommendation
    }

    private suspend fun expireMissingActiveRecommendations(
        userId: String,
        activeRecommendationIds: Set<String>,
        nowMillis: Long
    ) {
        val snapshot = recommendationsCollection(userId).get().awaitResult()
        snapshot.documents
            .filter { document -> document.id !in activeRecommendationIds }
            .filter { document -> document.getNullableString(FieldStatus) == RecommendationStatusActive }
            .filter { document -> document.getNullableString(FieldSource) == RecommendationSourceLlm }
            .forEach { document ->
                recommendationsCollection(userId)
                    .document(document.id)
                    .set(
                        mapOf(
                            FieldStatus to RecommendationStatusExpired,
                            FieldUpdatedAtMillis to nowMillis
                        ),
                        SetOptions.merge()
                    )
                    .awaitCompletion()
            }
    }

    private suspend fun loadRelationships(userId: String): List<RelationshipProfile> {
        val snapshot = relationshipsCollection(userId).get().awaitResult()
        return snapshot.documents
            .mapNotNull { document -> document.toRelationshipProfile(defaultUserId = userId) }
    }

    private suspend fun loadActivities(userId: String): List<StoredRelationshipActivity> {
        val interactions = interactionsCollection(userId)
            .get()
            .awaitResult()
            .documents
            .mapNotNull { document ->
                document.toStoredActivity(kind = StoredActivityKind.Interaction)
            }
        val memories = memoriesCollection(userId)
            .get()
            .awaitResult()
            .documents
            .mapNotNull { document ->
                document.toStoredActivity(kind = StoredActivityKind.Memory)
            }
        return interactions + memories
    }

    private suspend fun loadReflections(userId: String): List<StoredReflectionEntry> {
        val snapshot = reflectionsCollection(userId).get().awaitResult()
        return snapshot.documents.mapNotNull { document ->
            val contactIds = document.getStringList(FieldInteractedContactIds)
            val createdAtMillis = document.getMillis(FieldCreatedAtMillis) ?: return@mapNotNull null
            StoredReflectionEntry(
                contactIds = contactIds,
                contactNames = document.getStringList(FieldInteractedContactNames),
                mood = document.getNullableString(FieldMood).orEmpty(),
                feelings = document.getStringList(FieldFeelings),
                sources = document.getStringList(FieldSources),
                createdAtMillis = createdAtMillis
            )
        }
    }

    private suspend fun loadAppointments(
        userId: String,
        relationships: List<RelationshipProfile>
    ): List<StoredAppointmentEntry> {
        val relationshipsByName = relationships.associateBy { relationship ->
            relationship.name.trim().lowercase()
        }

        val snapshot = appointmentsCollection(userId).get().awaitResult()
        return snapshot.documents.mapNotNull { document ->
            val name = document.getNullableString(FieldName)?.takeIf { value -> value.isNotBlank() }
                ?: return@mapNotNull null
            val appointmentDateMillis = document.getMillis(FieldAppointmentDateMillis)
                ?: return@mapNotNull null
            val explicitContactId = document.getNullableString(FieldContactId)
                ?: document.getNullableString(FieldRelationshipId)
            val exactNameContactId = relationshipsByName[name.trim().lowercase()]?.id
            StoredAppointmentEntry(
                contactId = explicitContactId ?: exactNameContactId,
                name = name,
                location = document.getNullableString(FieldLocation),
                appointmentDateMillis = appointmentDateMillis
            )
        }
    }

    private fun relationshipsCollection(userId: String) =
        firestore.collection(UsersCollection)
            .document(userId)
            .collection(RelationshipsCollection)

    private fun interactionsCollection(userId: String) =
        firestore.collection(UsersCollection)
            .document(userId)
            .collection(InteractionsCollection)

    private fun memoriesCollection(userId: String) =
        firestore.collection(UsersCollection)
            .document(userId)
            .collection(MemoriesCollection)

    private fun reflectionsCollection(userId: String) =
        firestore.collection(UsersCollection)
            .document(userId)
            .collection(ReflectionsCollection)

    private fun appointmentsCollection(userId: String) =
        firestore.collection(UsersCollection)
            .document(userId)
            .collection(AppointmentsCollection)

    private fun recommendationsCollection(userId: String) =
        firestore.collection(UsersCollection)
            .document(userId)
            .collection(RecommendationsCollection)
}

object RecommendationRepositoryProvider {
    val repository: RecommendationRepository by lazy {
        FirebaseRecommendationRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance(),
            aiService = RelationshipRecommendationAiServiceProvider.service
        )
    }
}

private fun DocumentSnapshot.toRelationshipProfile(defaultUserId: String): RelationshipProfile? {
    val name = getNullableString(FieldName)?.takeIf { value -> value.isNotBlank() } ?: return null
    return RelationshipProfile(
        id = getNullableString(FieldId) ?: id,
        userId = getNullableString(FieldUserId) ?: defaultUserId,
        name = name,
        tag = getNullableString(FieldTag).toEnumOrNull<RelationshipTag>() ?: RelationshipTag.Friend,
        birthdayIso = getNullableString(FieldBirthdayIso),
        phoneNumber = getNullableString(FieldPhoneNumber),
        email = getNullableString(FieldEmail),
        interests = getStringList(FieldInterests),
        notes = getNullableString(FieldNotes),
        avatarUrl = getNullableString(FieldAvatarUrl),
        priority = getNullableString(FieldPriority).toEnumOrNull<RelationshipPriority>()
            ?: RelationshipPriority.Medium,
        createdAtMillis = getMillis(FieldCreatedAtMillis) ?: 0L,
        updatedAtMillis = getMillis(FieldUpdatedAtMillis) ?: 0L
    )
}

private fun DocumentSnapshot.toStoredActivity(
    kind: StoredActivityKind
): StoredRelationshipActivity? {
    val contactId = getNullableString(FieldContactId) ?: return null
    val createdAtMillis = getMillis(FieldCreatedAtMillis) ?: return null
    return StoredRelationshipActivity(
        contactId = contactId,
        contactName = getNullableString(FieldContactName),
        kind = kind,
        type = getNullableString(FieldType) ?: kind.name.lowercase(),
        title = getNullableString(FieldTitle),
        note = getNullableString(FieldNote),
        location = getNullableString(FieldLocation),
        createdAtMillis = createdAtMillis
    )
}

private fun DocumentSnapshot.toRelationshipRecommendation(
    defaultUserId: String
): RelationshipRecommendation? {
    val recommendationId = getNullableString(FieldId) ?: id
    val relationshipId = getNullableString(FieldRelationshipId) ?: return null
    val relationshipName = getNullableString(FieldRelationshipName) ?: return null
    return RelationshipRecommendation(
        id = recommendationId,
        userId = getNullableString(FieldUserId) ?: defaultUserId,
        relationshipId = relationshipId,
        relationshipName = relationshipName,
        relationshipAvatarUrl = getNullableString(FieldRelationshipAvatarUrl),
        type = getNullableString(FieldType) ?: RecommendationTypeCheckIn,
        status = getNullableString(FieldStatus) ?: RecommendationStatusActive,
        relationshipStatus = getNullableString(FieldRelationshipStatus).orEmpty(),
        bondScore = getLongValue(FieldBondScore)?.toInt() ?: 0,
        attentionScore = getLongValue(FieldAttentionScore)?.toInt() ?: 0,
        bondLabel = getNullableString(FieldBondLabel).orEmpty(),
        attentionLabel = getNullableString(FieldAttentionLabel).orEmpty(),
        summary = getNullableString(FieldSummary).orEmpty(),
        reasons = getStringList(FieldReasons),
        suggestedActions = getRecommendationActions(FieldSuggestedActions),
        primaryActionType = getNullableString(FieldPrimaryActionType).orEmpty(),
        primaryActionLabel = getNullableString(FieldPrimaryActionLabel).orEmpty(),
        notificationTitle = getNullableString(FieldNotificationTitle).orEmpty(),
        notificationDescription = getNullableString(FieldNotificationDescription).orEmpty(),
        createdAtMillis = getMillis(FieldCreatedAtMillis) ?: 0L,
        updatedAtMillis = getMillis(FieldUpdatedAtMillis) ?: 0L,
        expiresAtMillis = getMillis(FieldExpiresAtMillis),
        source = getNullableString(FieldSource).orEmpty(),
        model = getNullableString(FieldModel).orEmpty(),
        promptVersion = getNullableString(FieldPromptVersion).orEmpty()
    )
}

private fun RelationshipRecommendation.toFirestoreMap(
    context: RelationshipEvaluationContext,
    rawLlmJson: String
): Map<String, Any?> {
    return mapOf(
        FieldId to id,
        FieldUserId to userId,
        FieldRelationshipId to relationshipId,
        FieldRelationshipName to relationshipName,
        FieldRelationshipAvatarUrl to relationshipAvatarUrl,
        FieldType to type,
        FieldStatus to status,
        FieldRelationshipStatus to relationshipStatus,
        FieldBondScore to bondScore,
        FieldAttentionScore to attentionScore,
        FieldBondLabel to bondLabel,
        FieldAttentionLabel to attentionLabel,
        FieldSummary to summary,
        FieldReasons to reasons,
        FieldSuggestedActions to suggestedActions.map { action -> action.toFirestoreMap() },
        FieldPrimaryActionType to primaryActionType,
        FieldPrimaryActionLabel to primaryActionLabel,
        FieldNotificationTitle to notificationTitle,
        FieldNotificationDescription to notificationDescription,
        FieldCreatedAtMillis to createdAtMillis,
        FieldUpdatedAtMillis to updatedAtMillis,
        FieldExpiresAtMillis to expiresAtMillis,
        FieldSource to source,
        FieldModel to model,
        FieldPromptVersion to promptVersion,
        FieldInputSnapshot to context.toFirestoreMap(),
        FieldRawLlmJson to rawLlmJson
    )
}

private fun RecommendationAction.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        FieldActionType to type,
        FieldActionPriority to priority,
        FieldActionTitle to title,
        FieldActionSuggestedText to suggestedText
    )
}

private fun RelationshipEvaluationContext.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "currentDate" to currentDateIso,
        "relationship" to mapOf(
            "id" to relationship.id,
            "name" to relationship.name,
            "tag" to relationship.tag,
            "priority" to relationship.priority,
            "birthdayIso" to relationship.birthdayIso,
            "interests" to relationship.interests,
            "notes" to relationship.notes,
            "createdAtMillis" to relationship.createdAtMillis
        ),
        "computedMetrics" to computedMetrics.toFirestoreMap(),
        "recentActivities" to recentActivities.map { activity -> activity.toFirestoreMap() },
        "importantSignals" to importantSignals.toFirestoreMap(),
        "reflectionContext" to reflectionContext.map { reflection ->
            mapOf(
                "mood" to reflection.mood,
                "feelings" to reflection.feelings,
                "sources" to reflection.sources,
                "createdAtMillis" to reflection.createdAtMillis
            )
        }
    )
}

private fun RelationshipComputedMetrics.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "bondScore" to bondScore,
        "attentionScore" to attentionScore,
        "bondLabel" to bondLabel,
        "attentionLabel" to attentionLabel,
        "hasLoggedTouch" to hasLoggedTouch,
        "lastTouchAtMillis" to lastTouchAtMillis,
        "lastTouchType" to lastTouchType,
        "daysSinceLastTouch" to daysSinceLastTouch,
        "expectedTouchIntervalDays" to expectedTouchIntervalDays,
        "overdueDays" to overdueDays,
        "touchCountLast7Days" to touchCountLast7Days,
        "touchCountLast30Days" to touchCountLast30Days,
        "touchCountLast90Days" to touchCountLast90Days,
        "priorityScore" to priorityScore,
        "recencyScore" to recencyScore,
        "frequencyScore" to frequencyScore,
        "specialDateScore" to specialDateScore,
        "profileRichnessScore" to profileRichnessScore
    )
}

private fun RelationshipActivityContext.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "type" to type,
        "title" to title,
        "note" to note,
        "location" to location,
        "createdAtMillis" to createdAtMillis
    )
}

private fun RelationshipImportantSignals.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "birthdayInDays" to birthdayInDays,
        "hasBirthdaySoon" to hasBirthdaySoon,
        "memoryAnniversaryInDays" to memoryAnniversaryInDays,
        "hasMemoryAnniversarySoon" to hasMemoryAnniversarySoon,
        "upcomingAppointmentInDays" to upcomingAppointmentInDays,
        "hasUpcomingAppointment" to hasUpcomingAppointment
    )
}

private fun DocumentSnapshot.getRecommendationActions(field: String): List<RecommendationAction> {
    return (get(field) as? List<*>)
        ?.mapNotNull { value -> value as? Map<*, *> }
        ?.mapNotNull { map ->
            val type = map[FieldActionType] as? String ?: return@mapNotNull null
            val title = map[FieldActionTitle] as? String ?: return@mapNotNull null
            RecommendationAction(
                type = type,
                priority = map[FieldActionPriority] as? String ?: "MEDIUM",
                title = title,
                suggestedText = map[FieldActionSuggestedText] as? String
            )
        }
        ?: emptyList()
}

private fun DocumentSnapshot.preservedStatus(nowMillis: Long): String {
    val existingStatus = getNullableString(FieldStatus)
    val expiresAtMillis = getMillis(FieldExpiresAtMillis)
    val shouldPreserve = existingStatus != null &&
        existingStatus in PreservedRecommendationStatuses &&
        (expiresAtMillis == null || expiresAtMillis > nowMillis)
    return if (shouldPreserve) {
        existingStatus
    } else {
        RecommendationStatusActive
    }
}

private fun RelationshipEvaluationContext.toRecommendationId(): String {
    return "relationship_${relationship.id}"
}

private fun DocumentSnapshot.getNullableString(field: String): String? =
    get(field) as? String

private fun DocumentSnapshot.getStringList(field: String): List<String> =
    (get(field) as? List<*>)
        ?.mapNotNull { value -> value as? String }
        ?: emptyList()

private fun DocumentSnapshot.getLongValue(field: String): Long? {
    return when (val value = get(field)) {
        is Number -> value.toLong()
        is Timestamp -> value.toDate().time
        else -> null
    }
}

private fun DocumentSnapshot.getMillis(field: String): Long? = getLongValue(field)

private inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? =
    this?.let { value -> enumValues<T>().firstOrNull { enumValue -> enumValue.name == value } }

private suspend fun Task<*>.awaitCompletion() {
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener {
            if (continuation.isActive) {
                continuation.resume(Unit)
            }
        }
        addOnFailureListener { exception ->
            if (continuation.isActive) {
                continuation.resumeWithException(exception)
            }
        }
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) {
            continuation.resume(result)
        }
    }
    addOnFailureListener { exception ->
        if (continuation.isActive) {
            continuation.resumeWithException(exception)
        }
    }
}

const val DefaultMaxRecommendations = 5

private const val UsersCollection = "users"
private const val RelationshipsCollection = "relationships"
private const val InteractionsCollection = "interactions"
private const val MemoriesCollection = "memories"
private const val ReflectionsCollection = "reflections"
private const val AppointmentsCollection = "appointments"
private const val RecommendationsCollection = "recommendations"

private const val RecommendationStatusActive = "ACTIVE"
private const val RecommendationStatusDismissed = "DISMISSED"
private const val RecommendationStatusCompleted = "COMPLETED"
private const val RecommendationStatusSnoozed = "SNOOZED"
private const val RecommendationStatusExpired = "EXPIRED"
private const val RecommendationSourceLlm = "LLM"
private val PreservedRecommendationStatuses = setOf(
    RecommendationStatusDismissed,
    RecommendationStatusCompleted,
    RecommendationStatusSnoozed
)
private const val RecommendationExpiryMillis = 7L * 24L * 60L * 60L * 1_000L

private const val FieldId = "id"
private const val FieldUserId = "userId"
private const val FieldName = "name"
private const val FieldRelationshipId = "relationshipId"
private const val FieldRelationshipName = "relationshipName"
private const val FieldRelationshipAvatarUrl = "relationshipAvatarUrl"
private const val FieldRelationshipStatus = "relationshipStatus"
private const val FieldTag = "tag"
private const val FieldBirthdayIso = "birthdayIso"
private const val FieldPhoneNumber = "phoneNumber"
private const val FieldEmail = "email"
private const val FieldInterests = "interests"
private const val FieldNotes = "notes"
private const val FieldAvatarUrl = "avatarUrl"
private const val FieldPriority = "priority"
private const val FieldContactId = "contactId"
private const val FieldContactName = "contactName"
private const val FieldType = "type"
private const val FieldStatus = "status"
private const val FieldTitle = "title"
private const val FieldNote = "note"
private const val FieldLocation = "location"
private const val FieldAppointmentDateMillis = "appointmentDateMillis"
private const val FieldInteractedContactIds = "interactedContactIds"
private const val FieldInteractedContactNames = "interactedContactNames"
private const val FieldMood = "mood"
private const val FieldFeelings = "feelings"
private const val FieldSources = "sources"
private const val FieldBondScore = "bondScore"
private const val FieldAttentionScore = "attentionScore"
private const val FieldBondLabel = "bondLabel"
private const val FieldAttentionLabel = "attentionLabel"
private const val FieldSummary = "summary"
private const val FieldReasons = "reasons"
private const val FieldSuggestedActions = "suggestedActions"
private const val FieldPrimaryActionType = "primaryActionType"
private const val FieldPrimaryActionLabel = "primaryActionLabel"
private const val FieldNotificationTitle = "notificationTitle"
private const val FieldNotificationDescription = "notificationDescription"
private const val FieldCreatedAtMillis = "createdAtMillis"
private const val FieldUpdatedAtMillis = "updatedAtMillis"
private const val FieldExpiresAtMillis = "expiresAtMillis"
private const val FieldSource = "source"
private const val FieldModel = "model"
private const val FieldPromptVersion = "promptVersion"
private const val FieldInputSnapshot = "inputSnapshot"
private const val FieldRawLlmJson = "rawLlmJson"
private const val FieldActionType = "type"
private const val FieldActionPriority = "priority"
private const val FieldActionTitle = "title"
private const val FieldActionSuggestedText = "suggestedText"
