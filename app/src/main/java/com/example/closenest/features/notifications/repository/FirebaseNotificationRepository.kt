package com.example.closenest.features.notifications.repository

import com.example.closenest.core.notification.AppointmentReminderKind
import com.example.closenest.core.notification.appointmentReminderDedupeKey
import com.example.closenest.features.notifications.model.NotificationItem
import com.example.closenest.features.notifications.model.NotificationStatus
import com.example.closenest.features.notifications.model.NotificationSummary
import com.example.closenest.features.notifications.model.isCreatedToday
import com.example.closenest.features.notifications.model.toNotificationItem
import com.example.closenest.features.notifications.model.toMap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Firebase-backed implementation of NotificationRepository.
 * Fetches notifications from Firestore in real-time (without push notifications).
 *
 * Notifications are stored in: users/{userId}/notifications/{notificationId}
 */
class FirebaseNotificationRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : NotificationRepository {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val NOTIFICATIONS_SUBCOLLECTION = "notifications"
    }

    /**
     * Observes notifications for the current user in real-time.
     * Returns empty Flow if user is not logged in.
     * Orders by createdAtMillis in descending order.
     */
    override fun observeNotifications(): Flow<List<NotificationItem>> = callbackFlow {
        var notificationRegistration: ListenerRegistration? = null

        fun stopNotificationListener() {
            notificationRegistration?.remove()
            notificationRegistration = null
        }

        fun listenForUserNotifications(userId: String) {
            stopNotificationListener()
            notificationRegistration = firestore
                .collection(USERS_COLLECTION)
                .document(userId)
                .collection(NOTIFICATIONS_SUBCOLLECTION)
                .orderBy("createdAtMillis", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val notifications = snapshot.documents
                            .mapNotNull { it.toNotificationItem() }
                        trySend(notifications)
                    } else {
                        trySend(emptyList())
                    }
                }
        }

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val currentUserId = firebaseAuth.currentUser?.uid
            if (currentUserId == null) {
                stopNotificationListener()
                trySend(emptyList())
            } else {
                listenForUserNotifications(currentUserId)
            }
        }

        auth.addAuthStateListener(authListener)

        awaitClose {
            stopNotificationListener()
            auth.removeAuthStateListener(authListener)
        }
    }

    /**
     * Computes notification summary statistics from the notifications Flow.
     * Counts: total, unread (ACTIVE status), and today's notifications.
     */
    override fun observeNotificationSummary(): Flow<NotificationSummary> =
        observeNotifications().map { notificationList ->
            NotificationSummary(
                totalCount = notificationList.size,
                unreadCount = notificationList.count { it.status == NotificationStatus.ACTIVE },
                todayCount = notificationList.count { it.isCreatedToday() }
            )
        }

    override suspend fun upsertNotification(notification: NotificationItem, userId: String?) {
        val currentUid = userId ?: auth.currentUser?.uid
            ?: error("No signed-in Firebase user for notification upsert.")
        val documentId = notification.dedupeKey?.takeIf { it.isNotBlank() }
            ?.toFirestoreDocumentId()
            ?: notification.id.toFirestoreDocumentId()
        val document = firestore
            .collection(USERS_COLLECTION)
            .document(currentUid)
            .collection(NOTIFICATIONS_SUBCOLLECTION)
            .document(documentId)

        firestore.runTransaction { transaction ->
            val existing = transaction.get(document)
            if (existing.exists()) {
                return@runTransaction
            }
            val notificationToSave = notification.copy(
                id = documentId,
            )
            transaction.set(document, notificationToSave.toMapWithUserId(currentUid))
        }.await()
    }

    /**
     * Marks a notification as READ.
     * Updates the status field in Firestore.
     */
    override suspend fun markAsRead(notificationId: String) {
        val currentUid = auth.currentUser?.uid ?: return

        firestore
            .collection(USERS_COLLECTION)
            .document(currentUid)
            .collection(NOTIFICATIONS_SUBCOLLECTION)
            .document(notificationId)
            .update("status", NotificationStatus.READ.name)
            .await()
    }

    /**
     * Dismisses a notification by setting status to DISMISSED.
     * Updates the status field in Firestore.
     */
    override suspend fun dismissNotification(notificationId: String) {
        val currentUid = auth.currentUser?.uid ?: return

        firestore
            .collection(USERS_COLLECTION)
            .document(currentUid)
            .collection(NOTIFICATIONS_SUBCOLLECTION)
            .document(notificationId)
            .update("status", NotificationStatus.DISMISSED.name)
            .await()
    }

    /**
     * Permanently deletes a notification document from Firestore.
     */
    override suspend fun deleteNotification(notificationId: String) {
        val currentUid = auth.currentUser?.uid ?: return

        firestore
            .collection(USERS_COLLECTION)
            .document(currentUid)
            .collection(NOTIFICATIONS_SUBCOLLECTION)
            .document(notificationId)
            .delete()
            .await()
    }

    override suspend fun deleteAppointmentReminderNotifications(appointmentId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val notificationCollection = firestore
            .collection(USERS_COLLECTION)
            .document(currentUid)
            .collection(NOTIFICATIONS_SUBCOLLECTION)

        val batch = firestore.batch()
        AppointmentReminderKind.entries.forEach { kind ->
            val documentId = appointmentReminderDedupeKey(appointmentId, kind)
                .toFirestoreDocumentId()
            batch.delete(notificationCollection.document(documentId))
        }
        batch.commit().await()
    }

    /**
     * Fetches a single notification by ID.
     * Returns null if notification not found or user not logged in.
     */
    override suspend fun getNotification(notificationId: String): NotificationItem? {
        val currentUid = auth.currentUser?.uid ?: return null

        val snapshot = firestore
            .collection(USERS_COLLECTION)
            .document(currentUid)
            .collection(NOTIFICATIONS_SUBCOLLECTION)
            .document(notificationId)
            .get()
            .await()

        return snapshot.toNotificationItem()
    }
}

private fun String.toFirestoreDocumentId(): String = replace("/", "_")

private fun NotificationItem.toMapWithUserId(userId: String): Map<String, Any?> {
    return toMap() + ("userId" to userId)
}

object NotificationRepositoryProvider {
    val repository: NotificationRepository by lazy {
        FirebaseNotificationRepository(
            firestore = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance()
        )
    }
}
