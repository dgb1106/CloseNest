package com.example.closenest.features.notifications.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.closenest.features.notifications.model.NotificationFilterType
import com.example.closenest.features.notifications.model.NotificationItem
import com.example.closenest.features.notifications.model.NotificationStatus
import com.example.closenest.features.notifications.model.NotificationSummary
import com.example.closenest.features.notifications.repository.NotificationRepository
import com.example.closenest.features.notifications.repository.NotificationRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val notifications: List<NotificationItem> = emptyList(),
    val summary: NotificationSummary = NotificationSummary(0, 0, 0),
    val selectedFilter: NotificationFilterType = NotificationFilterType.ALL,
    val filteredNotifications: List<NotificationItem> = emptyList(),
    val errorMessage: String? = null,
    val selectedNotification: NotificationItem? = null
)

class NotificationsViewModel(
    private val repository: NotificationRepository
) : ViewModel() {
    // Local mutable state for filters
    private val filters = MutableStateFlow(NotificationFilters())
    
    // Local mutable state for selected notification
    private val selectedNotification = MutableStateFlow<NotificationItem?>(null)

    // Combined UI state: repository notifications + local filter state + summary stats
    val uiState: StateFlow<NotificationsUiState> = combine(
        repository.observeNotifications(),
        repository.observeNotificationSummary(),
        filters,
        selectedNotification
    ) { notifications, summary, currentFilters, selected ->
        val filteredNotifications = applyFilters(notifications, currentFilters)

        NotificationsUiState(
            isLoading = false,
            notifications = notifications,
            summary = summary,
            selectedFilter = currentFilters.filterType,
            filteredNotifications = filteredNotifications,
            errorMessage = null,
            selectedNotification = selected
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotificationsUiState()
    )

    fun onFilterSelected(filterType: NotificationFilterType) {
        filters.update { current ->
            val newFilterType = if (current.filterType == filterType) {
                NotificationFilterType.ALL
            } else {
                filterType
            }
            current.copy(filterType = newFilterType)
        }
    }

    fun onNotificationMarkAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markAsRead(notificationId)
        }
    }

    fun onNotificationDismiss(notificationId: String) {
        viewModelScope.launch {
            repository.dismissNotification(notificationId)
        }
    }

    fun selectNotification(notification: NotificationItem) {
        selectedNotification.value = notification
    }

    fun deselectNotification() {
        selectedNotification.value = null
    }

    fun clearFilters() {
        filters.value = NotificationFilters()
    }

    private fun applyFilters(
        notifications: List<NotificationItem>,
        filters: NotificationFilters
    ): List<NotificationItem> {
        val now = System.currentTimeMillis()
        val todayStart = now - (now % (24 * 60 * 60 * 1000))

        return when (filters.filterType) {
            NotificationFilterType.ALL -> notifications
            NotificationFilterType.UNREAD -> notifications.filter {
                it.status == NotificationStatus.ACTIVE
            }
            NotificationFilterType.TODAY -> notifications.filter {
                it.createdAtMillis >= todayStart && it.status == NotificationStatus.ACTIVE
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                NotificationsViewModel(
                    repository = NotificationRepositoryProvider.repository
                )
            }
        }
    }
}

private data class NotificationFilters(
    val filterType: NotificationFilterType = NotificationFilterType.ALL
)
