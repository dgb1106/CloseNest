package com.example.closenest.features.notifications.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.closenest.R
import com.example.closenest.core.ui.theme.CloseNestAttention
import com.example.closenest.core.ui.theme.CloseNestConnected
import com.example.closenest.core.ui.theme.CloseNestWarm
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

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
    LOG_MEMORY,
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

// ========== Extension Functions for Firestore Serialization ==========

/**
 * Converts NotificationItem to a Map for Firestore storage.
 * Enums are stored as their String name.
 * All fields including nullables are included.
 */
fun NotificationItem.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "type" to type.name,
    "status" to status.name,
    "relationshipId" to relationshipId,
    "relationshipName" to relationshipName,
    "relationshipAvatarUrl" to relationshipAvatarUrl,
    "title" to title,
    "description" to description,
    "createdAtMillis" to createdAtMillis,
    "expiresAtMillis" to expiresAtMillis,
    "actionLabel" to actionLabel,
    "actionType" to actionType?.name
)

/**
 * Converts a Firestore DocumentSnapshot to a NotificationItem.
 * Handles missing/null fields gracefully.
 * Returns null if document doesn't contain essential fields.
 */
fun DocumentSnapshot.toNotificationItem(): NotificationItem? {
    return try {
        val notificationId = getString("id")?.takeIf { it.isNotBlank() } ?: id
        val typeStr = getString("type") ?: return null
        val statusStr = getString("status") ?: return null
        val title = getString("title") ?: return null
        val description = getString("description") ?: return null
        val createdAtMillis = getMillis("createdAtMillis") ?: return null

        val type = typeStr.toEnumOrNull<NotificationType>() ?: return null
        val status = statusStr.toEnumOrNull<NotificationStatus>() ?: return null

        val actionTypeStr = getString("actionType")
        val actionType = actionTypeStr?.toEnumOrNull<NotificationActionType>()

        NotificationItem(
            id = notificationId,
            type = type,
            status = status,
            relationshipId = getString("relationshipId"),
            relationshipName = getString("relationshipName"),
            relationshipAvatarUrl = getString("relationshipAvatarUrl"),
            title = title,
            description = description,
            createdAtMillis = createdAtMillis,
            expiresAtMillis = getMillis("expiresAtMillis"),
            actionLabel = getString("actionLabel"),
            actionType = actionType
        )
    } catch (e: Exception) {
        null
    }
}

private fun DocumentSnapshot.getMillis(field: String): Long? {
    return when (val value = get(field)) {
        is Number -> value.toLong()
        is Timestamp -> value.toDate().time
        else -> null
    }
}

private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? {
    val normalized = trim().replace("-", "_").uppercase()
    return enumValues<T>().firstOrNull { it.name == normalized }
}
