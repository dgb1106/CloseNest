package com.example.closenest.features.notifications.repository

import com.example.closenest.core.notification.AppointmentReminderKind
import com.example.closenest.core.notification.appointmentReminderDedupeKey
import com.example.closenest.features.notifications.model.NotificationActionType
import com.example.closenest.features.notifications.model.NotificationItem
import com.example.closenest.features.notifications.model.NotificationStatus
import com.example.closenest.features.notifications.model.NotificationSummary
import com.example.closenest.features.notifications.model.NotificationType
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemoryNotificationRepository : NotificationRepository {
    private val notifications = MutableStateFlow(seedNotifications())

    override fun observeNotifications(): Flow<List<NotificationItem>> = notifications.asStateFlow()

    override fun observeNotificationSummary(): Flow<NotificationSummary> =
        notifications.map { notificationList ->
            val now = System.currentTimeMillis()
            val todayStart = now - (now % (24 * 60 * 60 * 1000))

            NotificationSummary(
                totalCount = notificationList.size,
                unreadCount = notificationList.count { it.status == NotificationStatus.ACTIVE },
                todayCount =
                    notificationList.count {
                        it.createdAtMillis >= todayStart &&
                            it.status == NotificationStatus.ACTIVE
                    }
            )
        }

    override suspend fun upsertNotification(notification: NotificationItem, userId: String?) {
        val notificationId = notification.dedupeKey?.takeIf { it.isNotBlank() } ?: notification.id
        notifications.update { current ->
            val existing = current.firstOrNull { it.id == notificationId }
            val notificationToSave = notification.copy(
                id = notificationId,
                status = existing?.status ?: notification.status,
                createdAtMillis = existing?.createdAtMillis ?: notification.createdAtMillis,
                completedAtMillis = existing?.completedAtMillis ?: notification.completedAtMillis
            )
            if (existing == null) {
                current + notificationToSave
            } else {
                current.map { item ->
                    if (item.id == notificationId) notificationToSave else item
                }
            }
        }
    }

    override suspend fun markAsRead(notificationId: String) {
        notifications.update { current ->
            current.map { notification ->
                if (notification.id == notificationId) {
                    notification.copy(status = NotificationStatus.READ)
                } else {
                    notification
                }
            }
        }
    }

    override suspend fun dismissNotification(notificationId: String) {
        notifications.update { current ->
            current.map { notification ->
                if (notification.id == notificationId) {
                    notification.copy(status = NotificationStatus.DISMISSED)
                } else {
                    notification
                }
            }
        }
    }

    override suspend fun getNotification(notificationId: String): NotificationItem? {
        return notifications.value.firstOrNull { it.id == notificationId }
    }

    override suspend fun deleteNotification(notificationId: String) {
        notifications.update { current ->
            current.filter { it.id != notificationId }
        }
    }

    override suspend fun deleteAppointmentReminderNotifications(appointmentId: String) {
        val ids = AppointmentReminderKind.entries
            .map { kind -> appointmentReminderDedupeKey(appointmentId, kind) }
            .toSet()
        notifications.update { current ->
            current.filter { it.id !in ids }
        }
    }

    private fun seedNotifications(): List<NotificationItem> {
        val now = System.currentTimeMillis()
        return listOf(
            // 1. Check-in suggestion (Minh Anh - 7 days ago)
            NotificationItem(
                id = UUID.randomUUID().toString(),
                type = NotificationType.CHECK_IN,
                status = NotificationStatus.ACTIVE,
                relationshipId = "rel-1",
                relationshipName = "Minh Anh",
                relationshipAvatarUrl = null,
                title = "Bạn chưa trò chuyện với Minh Anh trong 7 ngày.",
                description = "Có lẽ hôm nay bạn nên gửi một tin nhắn ngắn?",
                createdAtMillis = now - 2.hoursInMillis,
                expiresAtMillis = now + 7.daysInMillis,
                actionLabel = "Send message",
                actionType = NotificationActionType.SEND_MESSAGE
            ),
            
            // 2. Memory reminder (Gia Hân - 6 months anniversary)
            NotificationItem(
                id = UUID.randomUUID().toString(),
                type = NotificationType.MEMORY_REMINDER,
                status = NotificationStatus.ACTIVE,
                relationshipId = "rel-2",
                relationshipName = "Gia Hân",
                relationshipAvatarUrl = null,
                title = "Nhắc nhở: Coffee Láng Hạ",
                description = "Bạn đã đến đây cùng Gia Hân cách đây 6 tháng. Hãy sống lại khoảnh khắc đó!",
                createdAtMillis = now - 1.daysInMillis,
                expiresAtMillis = now + 30.daysInMillis,
                actionLabel = "View memory",
                actionType = NotificationActionType.VIEW_MEMORY
            ),
            
            // 3. Birthday reminder (Bảo Nam - in 3 days)
            NotificationItem(
                id = UUID.randomUUID().toString(),
                type = NotificationType.BIRTHDAY,
                status = NotificationStatus.ACTIVE,
                relationshipId = "rel-3",
                relationshipName = "Bảo Nam",
                relationshipAvatarUrl = null,
                title = "Sinh nhật của Bảo Nam còn 3 ngày nữa.",
                description = "Hãy thử liên lạc hoặc lên kế hoạch cho một điều gì đó đặc biệt.",
                createdAtMillis = now - 3.hoursInMillis,
                expiresAtMillis = now + 3.daysInMillis,
                actionLabel = "Send gift idea",
                actionType = NotificationActionType.SEND_GIFT
            ),
            
            // 4. Streak encouragement (6-day streak)
            NotificationItem(
                id = UUID.randomUUID().toString(),
                type = NotificationType.STREAK,
                status = NotificationStatus.READ,
                relationshipId = null,
                relationshipName = null,
                relationshipAvatarUrl = null,
                title = "🔥 Chuỗi 6 ngày!",
                description = "Hãy giữ vững đà này. Ghi lại một kỷ niệm hoặc tương tác hôm nay.",
                createdAtMillis = now - 12.hoursInMillis,
                expiresAtMillis = now + 1.daysInMillis,
                actionLabel = "Log now",
                actionType = NotificationActionType.LOG_MEMORY
            ),
            
            // 5. Reflection reminder (end of day prompt)
            NotificationItem(
                id = UUID.randomUUID().toString(),
                type = NotificationType.REFLECTION_REMINDER,
                status = NotificationStatus.ACTIVE,
                relationshipId = null,
                relationshipName = null,
                relationshipAvatarUrl = null,
                title = "Hôm nay của bạn thế nào?",
                description = "Hãy dành một chút thời gian để nhìn lại các mối quan hệ của mình.",
                createdAtMillis = now - 4.hoursInMillis,
                expiresAtMillis = now + 2.hoursInMillis,
                actionLabel = "Reflect",
                actionType = NotificationActionType.REFLECT
            ),
            
            // 6. General encouragement (weekly summary)
            NotificationItem(
                id = UUID.randomUUID().toString(),
                type = NotificationType.ENCOURAGEMENT,
                status = NotificationStatus.ACTIVE,
                relationshipId = null,
                relationshipName = null,
                relationshipAvatarUrl = null,
                title = "Bạn đang làm rất tốt!",
                description = "Bạn đã kết nối với 3 người trong tuần này. Hãy tiếp tục vun đắp những mối quan hệ đó.",
                createdAtMillis = now - 2.daysInMillis,
                expiresAtMillis = now + 14.daysInMillis,
                actionLabel = null,
                actionType = NotificationActionType.DISMISS
            )
        )
    }

    private val Int.daysInMillis: Long
        get() = this * 24L * 60L * 60L * 1_000L

    private val Int.hoursInMillis: Long
        get() = this * 60L * 60L * 1_000L
}
