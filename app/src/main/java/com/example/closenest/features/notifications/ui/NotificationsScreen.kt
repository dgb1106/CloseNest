package com.example.closenest.features.notifications.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.closenest.R
import com.example.closenest.features.notifications.model.NotificationActionType
import com.example.closenest.features.notifications.model.NotificationFilterType
import com.example.closenest.features.notifications.model.NotificationItem
import com.example.closenest.features.notifications.model.NotificationStatus
import com.example.closenest.features.notifications.model.NotificationType
import com.example.closenest.features.notifications.viewmodel.NotificationsUiState
import com.example.closenest.features.notifications.viewmodel.NotificationsViewModel
import com.example.closenest.core.ui.theme.AppTheme
import com.example.closenest.core.ui.theme.CloseNestAttention
import com.example.closenest.core.ui.theme.CloseNestConnected
import com.example.closenest.core.ui.theme.CloseNestWarm
import java.util.concurrent.TimeUnit

@Composable
fun NotificationsRoute(
    onNotificationAction: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel = viewModel(factory = NotificationsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NotificationsScreen(
        uiState = uiState,
        onFilterSelected = viewModel::onFilterSelected,
        onNotificationMarkAsRead = viewModel::onNotificationMarkAsRead,
        onNotificationDismiss = viewModel::onNotificationDismiss,
        onNotificationAction = onNotificationAction,
        onNotificationSelected = viewModel::selectNotification,
        onNotificationDeselected = viewModel::deselectNotification,
        modifier = modifier
    )
}

@Composable
fun NotificationsScreen(
    uiState: NotificationsUiState,
    onFilterSelected: (NotificationFilterType) -> Unit,
    onNotificationMarkAsRead: (String) -> Unit,
    onNotificationDismiss: (String) -> Unit,
    onNotificationAction: (String) -> Unit,
    onNotificationSelected: (NotificationItem) -> Unit,
    onNotificationDeselected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Title
        item {
            Text(
                text = stringResource(R.string.notifications_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Filter chips row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NotificationFilterType.entries.forEach { filterType ->
                    FilterChip(
                        label = getFilterLabel(filterType),
                        selected = uiState.selectedFilter == filterType,
                        onClick = { onFilterSelected(filterType) }
                    )
                }
            }
        }

        // Summary stats card
        item {
            SummaryStatsCard(
                totalCount = uiState.summary.totalCount,
                unreadCount = uiState.summary.unreadCount,
                todayCount = uiState.summary.todayCount,
                selectedFilter = uiState.selectedFilter
            )
        }

        // Content: Loading, Error, Empty, or List
        when {
            uiState.isLoading -> {
                item {
                    LoadingState()
                }
            }

            uiState.errorMessage != null -> {
                item {
                    ErrorState(message = uiState.errorMessage)
                }
            }

            uiState.filteredNotifications.isEmpty() -> {
                item {
                    EmptyState(
                        filter = uiState.selectedFilter,
                        hasAnyNotifications = uiState.notifications.isNotEmpty()
                    )
                }
            }

            else -> {
                items(
                    items = uiState.filteredNotifications,
                    key = { it.id }
                ) { notification ->
                    NotificationCard(
                        notification = notification,
                        onMarkAsRead = { onNotificationMarkAsRead(notification.id) },
                        onDismiss = { onNotificationDismiss(notification.id) },
                        onAction = { onNotificationAction(notification.id) },
                        onCardClick = { onNotificationSelected(notification) }
                    )
                }
            }
        }
        }

        // Bottom Sheet for notification details
        if (uiState.selectedNotification != null) {
            NotificationDetailBottomSheet(
                notification = uiState.selectedNotification,
                onMarkAsRead = { onNotificationMarkAsRead(uiState.selectedNotification.id) },
                onDismiss = { onNotificationDismiss(uiState.selectedNotification.id) },
                onAction = { onNotificationAction(uiState.selectedNotification.id) },
                onClose = { onNotificationDeselected() }
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = if (selected) {
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.primary,
                labelColor = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            AssistChipDefaults.assistChipColors()
        }
    )
}

@Composable
private fun SummaryStatsCard(
    totalCount: Int,
    unreadCount: Int,
    todayCount: Int,
    selectedFilter: NotificationFilterType,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = when (selectedFilter) {
                    NotificationFilterType.ALL -> stringResource(
                        R.string.notifications_summary_all,
                        totalCount
                    )
                    NotificationFilterType.UNREAD -> stringResource(
                        R.string.notifications_summary_unread,
                        unreadCount
                    )
                    NotificationFilterType.TODAY -> stringResource(
                        R.string.notifications_summary_today,
                        todayCount
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationItem,
    onMarkAsRead: () -> Unit,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
    onCardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val notificationColor = getNotificationColor(notification.type)
    val timeAgoText = getTimeAgoText(notification.createdAtMillis)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCardClick)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. UNREAD INDICATOR (red dot)
            if (notification.status == NotificationStatus.ACTIVE) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 2. AVATAR (48dp circle with initials or icon)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(notificationColor.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (!notification.relationshipName.isNullOrEmpty()) {
                    Text(
                        text = notification.relationshipName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = notificationColor
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsNone,
                        contentDescription = null,
                        tint = notificationColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 3. CONTENT (flex 1: name + time on top row, description on second row)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Name + Time row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.relationshipName ?: stringResource(R.string.notification_type_encouragement),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = timeAgoText,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Description (1 line only)
                Text(
                    text = notification.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 4. OPTIONAL THUMBNAIL IMAGE (48dp square)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsNone,
                    contentDescription = null,
                    tint = colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun LoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    message: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.error_loading_notifications),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun EmptyState(
    filter: NotificationFilterType,
    hasAnyNotifications: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.NotificationsNone,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = colorScheme.outline
        )
        
        Text(
            text = when {
                !hasAnyNotifications -> stringResource(R.string.notifications_empty_title)
                else -> stringResource(R.string.notifications_empty_filtered_title)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface
        )
        
        Text(
            text = when {
                !hasAnyNotifications -> stringResource(R.string.notifications_empty_description)
                else -> stringResource(R.string.notifications_empty_filtered_description, filter.name)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun getNotificationColor(type: NotificationType) = when (type) {
    NotificationType.CHECK_IN,
    NotificationType.STREAK -> CloseNestAttention
    
    NotificationType.MEMORY_REMINDER,
    NotificationType.REFLECTION_REMINDER -> CloseNestWarm
    
    NotificationType.BIRTHDAY,
    NotificationType.ENCOURAGEMENT -> CloseNestConnected
}

@Composable
private fun getFilterLabel(filterType: NotificationFilterType): String = when (filterType) {
    NotificationFilterType.ALL -> stringResource(R.string.notification_filter_all)
    NotificationFilterType.UNREAD -> stringResource(R.string.notification_filter_unread)
    NotificationFilterType.TODAY -> stringResource(R.string.notification_filter_today)
}

@Composable
private fun getTimeAgoText(createdAtMillis: Long): String {
    val now = System.currentTimeMillis()
    val diffMillis = now - createdAtMillis
    
    return when {
        diffMillis < TimeUnit.MINUTES.toMillis(1) -> 
            stringResource(R.string.time_now)
        diffMillis < TimeUnit.HOURS.toMillis(1) -> {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
            String.format(stringResource(R.string.time_minutes), minutes)
        }
        diffMillis < TimeUnit.DAYS.toMillis(1) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
            String.format(stringResource(R.string.time_hours), hours)
        }
        else -> {
            val days = TimeUnit.MILLISECONDS.toDays(diffMillis)
            String.format(stringResource(R.string.time_days), days)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationsScreenPreview() {
    AppTheme {
        val mockUiState = NotificationsUiState(
            isLoading = false,
            notifications = listOf(
                NotificationItem(
                    id = "1",
                    type = NotificationType.CHECK_IN,
                    status = NotificationStatus.ACTIVE,
                    relationshipId = "rel-1",
                    relationshipName = "Minh Anh",
                    relationshipAvatarUrl = null,
                    title = "Đã 7 ngày chưa trò chuyện",
                    description = "Có lẽ bạn nên gửi một tin nhắn ngắn?",
                    createdAtMillis = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2),
                    expiresAtMillis = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7),
                    actionLabel = "Send message",
                    actionType = NotificationActionType.SEND_MESSAGE
                ),
                NotificationItem(
                    id = "2",
                    type = NotificationType.BIRTHDAY,
                    status = NotificationStatus.ACTIVE,
                    relationshipId = "rel-2",
                    relationshipName = "Bảo Nam",
                    relationshipAvatarUrl = null,
                    title = "Sinh nhật còn 3 ngày",
                    description = "Hãy thử liên lạc hoặc lên kế hoạch cho một điều gì đó đặc biệt.",                 createdAtMillis = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3),
                    expiresAtMillis = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3),
                    actionLabel = "Send gift idea",
                    actionType = NotificationActionType.SEND_GIFT
                ),
                NotificationItem(
                    id = "3",
                    type = NotificationType.ENCOURAGEMENT,
                    status = NotificationStatus.READ,
                    relationshipId = null,
                    relationshipName = null,
                    relationshipAvatarUrl = null,
                    title = "Bạn đang làm rất tốt!",
                    description = "Bạn đã kết nối với 3 người trong tuần này.",       createdAtMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1),
                    expiresAtMillis = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(14),
                    actionLabel = null,
                    actionType = NotificationActionType.DISMISS
                )
            ),
            summary = com.example.closenest.features.notifications.model.NotificationSummary(
                totalCount = 6,
                unreadCount = 3,
                todayCount = 2
            ),
            selectedFilter = NotificationFilterType.ALL,
            filteredNotifications = emptyList()
        )

        Surface(color = MaterialTheme.colorScheme.background) {
            NotificationsScreen(
                uiState = mockUiState.copy(
                    filteredNotifications = mockUiState.notifications
                ),
                onFilterSelected = {},
                onNotificationMarkAsRead = {},
                onNotificationDismiss = {},
                onNotificationAction = {},
                onNotificationSelected = {},
                onNotificationDeselected = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationDetailBottomSheet(
    notification: NotificationItem,
    onMarkAsRead: () -> Unit,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val notificationColor = getNotificationColor(notification.type)
    val timeAgoText = getTimeAgoText(notification.createdAtMillis)

    ModalBottomSheet(
        onDismissRequest = onClose,
        modifier = modifier,
        containerColor = colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.32f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = colorScheme.onSurface
                    )
                }
            }

            // Notification type badge and time
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = notificationColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = getNotificationTypeLabel(notification.type),
                        style = MaterialTheme.typography.labelSmall,
                        color = notificationColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Text(
                    text = timeAgoText,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Avatar and Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(notificationColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!notification.relationshipName.isNullOrEmpty()) {
                        Text(
                            text = notification.relationshipName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = notificationColor
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsNone,
                            contentDescription = null,
                            tint = notificationColor,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!notification.relationshipName.isNullOrEmpty()) {
                        Text(
                            text = notification.relationshipName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                    }
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(16.dp))

            // Full description
            Text(
                text = notification.description,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Expiration info if applicable
            if (notification.expiresAtMillis != null && notification.expiresAtMillis > System.currentTimeMillis()) {
                val expiresInDays = (notification.expiresAtMillis - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)
                if (expiresInDays < 7) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = CloseNestAttention.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.notification_expires_soon),
                                style = MaterialTheme.typography.labelSmall,
                                color = CloseNestAttention,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = String.format(
                                    stringResource(R.string.notification_expires_in),
                                    expiresInDays
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = CloseNestAttention
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Primary action button
                if (!notification.actionLabel.isNullOrEmpty() && notification.actionType != NotificationActionType.DISMISS) {
                    Button(
                        onClick = {
                            onAction()
                            onClose()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = notificationColor
                        )
                    ) {
                        Text(
                            text = notification.actionLabel,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Mark as read button
                if (notification.status == NotificationStatus.ACTIVE) {
                    FilledTonalButton(
                        onClick = {
                            onMarkAsRead()
                            onClose()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.notification_mark_as_read),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Dismiss button
                Button(
                    onClick = {
                        onDismiss()
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.errorContainer,
                        contentColor = colorScheme.error
                    )
                ) {
                    Text(
                        text = stringResource(R.string.notification_dismiss),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun getNotificationTypeLabel(type: NotificationType): String =
    stringResource(type.labelRes)
