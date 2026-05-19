package com.example.closenest.features.notifications.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.closenest.R
import com.example.closenest.core.ui.theme.CloseNestAttention
import com.example.closenest.core.ui.theme.CloseNestConnected
import com.example.closenest.core.ui.theme.CloseNestWarm
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
    val color: Color
) {
    CHECK_IN(R.string.notification_type_check_in, CloseNestAttention),
    MEMORY_REMINDER(R.string.notification_type_memory_reminder, CloseNestWarm),
    BIRTHDAY(R.string.notification_type_birthday, CloseNestConnected),
    STREAK(R.string.notification_type_streak, CloseNestAttention),
    REFLECTION_REMINDER(R.string.notification_type_reflection_reminder, CloseNestWarm),
    ENCOURAGEMENT(R.string.notification_type_encouragement, CloseNestConnected)
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