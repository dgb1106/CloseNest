package com.example.closenest.features.notifications

import com.example.closenest.features.notifications.model.NotificationFilterType
import com.example.closenest.features.notifications.model.NotificationItem
import com.example.closenest.features.notifications.model.NotificationStatus
import com.example.closenest.features.notifications.model.NotificationSummary
import com.example.closenest.features.notifications.repository.NotificationRepository
import com.example.closenest.features.notifications.viewmodel.NotificationsViewModel
import com.example.closenest.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads notifications sorted by newest and builds summary`() = runTest {
        val repository = FakeNotificationRepository(
            listOf(
                notification(id = "old", createdAtMillis = 1_000L, status = NotificationStatus.READ),
                notification(id = "new", createdAtMillis = System.currentTimeMillis(), status = NotificationStatus.ACTIVE)
            )
        )
        val viewModel = NotificationsViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf("new", "old"), viewModel.uiState.value.notifications.map { it.id })
        assertEquals(2, viewModel.uiState.value.summary.totalCount)
        assertEquals(1, viewModel.uiState.value.summary.unreadCount)
    }

    @Test
    fun `unread filter returns active notifications and toggles back to all`() = runTest {
        val repository = FakeNotificationRepository(
            listOf(
                notification(id = "active", status = NotificationStatus.ACTIVE),
                notification(id = "read", status = NotificationStatus.READ)
            )
        )
        val viewModel = NotificationsViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onFilterSelected(NotificationFilterType.UNREAD)
        advanceUntilIdle()

        assertEquals(listOf("active"), viewModel.uiState.value.filteredNotifications.map { it.id })

        viewModel.onFilterSelected(NotificationFilterType.UNREAD)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.filteredNotifications.size)
    }

    @Test
    fun `select active notification marks it as read and keeps selected copy read`() = runTest {
        val item = notification(id = "noti-1", status = NotificationStatus.ACTIVE)
        val repository = FakeNotificationRepository(listOf(item))
        val viewModel = NotificationsViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.selectNotification(item)
        advanceUntilIdle()

        assertEquals("noti-1", repository.lastMarkedReadId)
        assertEquals(NotificationStatus.READ, viewModel.uiState.value.selectedNotification?.status)
    }

    @Test
    fun `dismiss notification delegates delete to repository`() = runTest {
        val repository = FakeNotificationRepository(listOf(notification(id = "noti-1")))
        val viewModel = NotificationsViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onNotificationDismiss("noti-1")
        advanceUntilIdle()

        assertEquals("noti-1", repository.lastDeletedId)
    }

    @Test
    fun `repository error exposes friendly error state`() = runTest {
        val viewModel = NotificationsViewModel(ErrorNotificationRepository())
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Không thể đồng bộ thông báo từ máy chủ. Bạn thử lại sau nhé.", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.selectedNotification)
    }

    private class FakeNotificationRepository(
        initialNotifications: List<NotificationItem>
    ) : NotificationRepository {
        private val notifications = MutableStateFlow(initialNotifications)
        var lastMarkedReadId: String? = null
        var lastDeletedId: String? = null

        override fun observeNotifications(): Flow<List<NotificationItem>> = notifications

        override fun observeNotificationSummary(): Flow<NotificationSummary> =
            MutableStateFlow(NotificationSummary(0, 0, 0))

        override suspend fun upsertNotification(notification: NotificationItem, userId: String?) = Unit

        override suspend fun markAsRead(notificationId: String) {
            lastMarkedReadId = notificationId
            notifications.value = notifications.value.map {
                if (it.id == notificationId) it.copy(status = NotificationStatus.READ) else it
            }
        }

        override suspend fun dismissNotification(notificationId: String) = Unit

        override suspend fun getNotification(notificationId: String): NotificationItem? =
            notifications.value.firstOrNull { it.id == notificationId }

        override suspend fun deleteNotification(notificationId: String) {
            lastDeletedId = notificationId
            notifications.value = notifications.value.filterNot { it.id == notificationId }
        }

        override suspend fun deleteAppointmentReminderNotifications(appointmentId: String) = Unit
    }

    private class ErrorNotificationRepository : NotificationRepository {
        override fun observeNotifications(): Flow<List<NotificationItem>> = flow {
            throw IllegalStateException()
        }

        override fun observeNotificationSummary(): Flow<NotificationSummary> =
            MutableStateFlow(NotificationSummary(0, 0, 0))

        override suspend fun upsertNotification(notification: NotificationItem, userId: String?) = Unit

        override suspend fun markAsRead(notificationId: String) = Unit

        override suspend fun dismissNotification(notificationId: String) = Unit

        override suspend fun getNotification(notificationId: String): NotificationItem? = null

        override suspend fun deleteNotification(notificationId: String) = Unit

        override suspend fun deleteAppointmentReminderNotifications(appointmentId: String) = Unit
    }
}
