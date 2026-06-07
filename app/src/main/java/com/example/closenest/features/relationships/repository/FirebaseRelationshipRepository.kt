package com.example.closenest.features.relationships.repository

import com.example.closenest.features.relationships.model.NewRelationshipRequest
import com.example.closenest.features.relationships.model.RelationshipPriority
import com.example.closenest.features.relationships.model.RelationshipProfile
import com.example.closenest.features.relationships.model.RelationshipTag
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

class FirebaseRelationshipRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : RelationshipRepository {

    override fun observeRelationships(): Flow<List<RelationshipProfile>> = callbackFlow {
        var relationshipRegistration: ListenerRegistration? = null

        fun stopRelationshipListener() {
            relationshipRegistration?.remove()
            relationshipRegistration = null
        }

        fun listenForUserRelationships(userId: String) {
            stopRelationshipListener()
            relationshipRegistration = relationshipsCollection(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val relationships = snapshot
                        ?.documents
                        .orEmpty()
                        .filter { document -> !document.metadata.hasPendingWrites() }
                        .mapNotNull { document -> document.toRelationshipProfile(defaultUserId = userId) }
                        .sortedWith(
                            compareBy<RelationshipProfile> { relationship ->
                                relationship.name.lowercase()
                            }.thenByDescending { relationship ->
                                relationship.priority.value
                            }
                        )

                    trySend(relationships)
                }
        }

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val currentUserId = firebaseAuth.currentUser?.uid
            if (currentUserId == null) {
                stopRelationshipListener()
                trySend(emptyList())
            } else {
                listenForUserRelationships(currentUserId)
            }
        }

        auth.addAuthStateListener(authListener)

        awaitClose {
            stopRelationshipListener()
            auth.removeAuthStateListener(authListener)
        }
    }

    override suspend fun addRelationship(request: NewRelationshipRequest) {
        val userId = auth.currentUser?.uid ?: error("No signed-in Firebase user.")
        val now = System.currentTimeMillis()
        val document = relationshipsCollection(userId).document()

        document.set(
            mapOf(
                FieldId to document.id,
                FieldUserId to userId,
                FieldName to request.name,
                FieldTag to request.tag.name,
                FieldBirthdayIso to request.birthdayIso,
                FieldPhoneNumber to request.phoneNumber,
                FieldEmail to request.email,
                FieldInterests to request.interests,
                FieldNotes to request.notes,
                FieldAvatarUrl to null,
                FieldPriority to request.priority.name,
                FieldCreatedAtMillis to now,
                FieldUpdatedAtMillis to now
            )
        ).awaitCompletion()
    }

    override suspend fun deleteRelationship(relationshipId: String) {
        val userId = auth.currentUser?.uid ?: error("No signed-in Firebase user.")

        relationshipsCollection(userId)
            .document(relationshipId)
            .delete()
            .awaitCompletion()
    }

    override suspend fun updateRelationship(relationshipId: String, request: NewRelationshipRequest) {
        val userId = auth.currentUser?.uid ?: error("No signed-in Firebase user.")
        val now = System.currentTimeMillis()

        relationshipsCollection(userId)
            .document(relationshipId)
            .update(
                mapOf(
                    FieldName to request.name,
                    FieldTag to request.tag.name,
                    FieldBirthdayIso to request.birthdayIso,
                    FieldPhoneNumber to request.phoneNumber,
                    FieldEmail to request.email,
                    FieldInterests to request.interests,
                    FieldNotes to request.notes,
                    FieldPriority to request.priority.name,
                    FieldUpdatedAtMillis to now
                )
            )
            .awaitCompletion()
    }

    private fun relationshipsCollection(userId: String) =
        firestore.collection(UsersCollection)
            .document(userId)
            .collection(RelationshipsCollection)
}

object RelationshipRepositoryProvider {
    val repository: RelationshipRepository by lazy {
        FirebaseRelationshipRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance()
        )
    }
}

private fun DocumentSnapshot.toRelationshipProfile(defaultUserId: String): RelationshipProfile? {
    val name = getNullableString(FieldName)?.takeIf { it.isNotBlank() } ?: return null

    return RelationshipProfile(
        id = getNullableString(FieldId) ?: id,
        userId = getNullableString(FieldUserId) ?: defaultUserId,
        name = name,
        tag = getNullableString(FieldTag).toRelationshipTagOrDefault(),
        birthdayIso = getNullableString(FieldBirthdayIso),
        phoneNumber = getNullableString(FieldPhoneNumber),
        email = getNullableString(FieldEmail),
        interests = getStringList(FieldInterests),
        notes = getNullableString(FieldNotes),
        avatarUrl = getNullableString(FieldAvatarUrl),
        priority = getNullableString(FieldPriority).toEnumOrNull<RelationshipPriority>() ?: RelationshipPriority.Medium,
        createdAtMillis = getMillis(FieldCreatedAtMillis) ?: 0L,
        updatedAtMillis = getMillis(FieldUpdatedAtMillis) ?: 0L
    )
}

private fun DocumentSnapshot.getNullableString(field: String): String? =
    get(field) as? String

private fun DocumentSnapshot.getStringList(field: String): List<String> =
    (get(field) as? List<*>)
        ?.mapNotNull { value -> value as? String }
        ?: emptyList()

private fun DocumentSnapshot.getMillis(field: String): Long? {
    return when (val value = get(field)) {
        is Number -> value.toLong()
        is Timestamp -> value.toDate().time
        else -> null
    }
}

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

private inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? =
    this?.let { value -> enumValues<T>().firstOrNull { enumValue -> enumValue.name == value } }

private fun String?.toRelationshipTagOrDefault(): RelationshipTag {
    return when (this) {
        "Mentor" -> RelationshipTag.Other
        else -> this.toEnumOrNull<RelationshipTag>() ?: RelationshipTag.Friend
    }
}

private const val UsersCollection = "users"
private const val RelationshipsCollection = "relationships"

private const val FieldId = "id"
private const val FieldUserId = "userId"
private const val FieldName = "name"
private const val FieldTag = "tag"
private const val FieldBirthdayIso = "birthdayIso"
private const val FieldPhoneNumber = "phoneNumber"
private const val FieldEmail = "email"
private const val FieldInterests = "interests"
private const val FieldNotes = "notes"
private const val FieldAvatarUrl = "avatarUrl"
private const val FieldPriority = "priority"
private const val FieldCreatedAtMillis = "createdAtMillis"
private const val FieldUpdatedAtMillis = "updatedAtMillis"
