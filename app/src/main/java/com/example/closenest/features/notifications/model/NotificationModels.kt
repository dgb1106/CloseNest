package com.example.closenest.features.notifications.model

import androidx.annotation.StringRes
import com.example.closenest.R
import java.time.Instant

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val status: NotificationStatus,
    val relationshipId: String?,
    val relationshipName: String?,
    val relationshipAvatarUrl: String?,
    val title: String,
    val description: String,
    val createdAtMillis: Long,
    val expiresAtMillis: Long?,
    val actionLabel: String?,
    val actionType: NotificationActionType?
)

data class NotificationSummary(
    val totalCount: Int,
    val unreadCount: Int,
    val todayCount: Int
)

data class NotificationFilters(
    val filterType: NotificationFilterType = NotificationFilterType.ALL
)

enum class NotificationType(
    @param:StringRes val labelRes: Int,
    @param:StringRes val colorRes: Int
) {
    CHECK_IN(R.string.notification_type_check_in, R.color.notification_attention),
    MEMORY_REMINDER(R.string.notification_type_memory_reminder, R.color.notification_warm),
    BIRTHDAY(R.string.notification_type_birthday, R.color.notification_connected),
    STREAK(R.string.notification_type_streak, R.color.notification_attention),
    REFLECTION_REMINDER(R.string.notification_type_reflection_reminder, R.color.notification_warm),
    ENCOURAGEMENT(R.string.notification_type_encouragement, R.color.notification_connected)
}

enum class NotificationStatus {
    ACTIVE,
    READ,
    DISMISSED
}

enum class NotificationActionType {
    SEND_MESSAGE,
    VIEW_MEMORY,
    SEND_GIFT,
    LOG_INTERACTION,
    REFLECT,
    VIEW_PROFILE,
    MARK_READ,
    DISMISS
}

enum class NotificationFilterType {
    ALL,
    UNREAD,
    TODAY
}