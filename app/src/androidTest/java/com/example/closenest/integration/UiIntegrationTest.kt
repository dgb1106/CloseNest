package com.example.closenest.integration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.closenest.R
import com.example.closenest.core.ui.theme.AppTheme
import com.example.closenest.core.ui.theme.ThemeMode
import com.example.closenest.features.auth.ui.AuthScreen
import com.example.closenest.features.homepage.ui.AddHubScreen
import com.example.closenest.features.homepage.ui.AppointmentScreen
import com.example.closenest.features.homepage.ui.MemoryScreen
import com.example.closenest.features.homepage.ui.ReflectionScreen
import com.example.closenest.features.homepage.viewmodel.AddHubUiState
import com.example.closenest.features.homepage.viewmodel.ReflectionContactListItem
import com.example.closenest.features.notifications.model.NotificationActionType
import com.example.closenest.features.notifications.model.NotificationFilterType
import com.example.closenest.features.notifications.model.NotificationItem
import com.example.closenest.features.notifications.model.NotificationStatus
import com.example.closenest.features.notifications.model.NotificationSummary
import com.example.closenest.features.notifications.model.NotificationType
import com.example.closenest.features.notifications.ui.NotificationsScreen
import com.example.closenest.features.notifications.viewmodel.NotificationsUiState
import com.example.closenest.features.profile.model.ProfileUiState
import com.example.closenest.features.profile.model.RelationshipQuickPreview
import com.example.closenest.features.profile.model.UserProfile
import com.example.closenest.features.profile.ui.ProfileScreen
import com.example.closenest.features.relationships.model.RelationshipPriority
import com.example.closenest.features.relationships.model.RelationshipTag
import com.example.closenest.features.relationships.ui.AddRelationshipScreen
import com.example.closenest.features.relationships.ui.RelationshipsScreen
import com.example.closenest.features.relationships.viewmodel.AddRelationshipUiState
import com.example.closenest.features.relationships.viewmodel.RelationshipListItem
import com.example.closenest.features.relationships.viewmodel.RelationshipsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UiIntegrationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun auth_loginScreen_submitsCredentialsAndShowsFriendlyErrorDialog() {
        var submittedEmail = ""
        var submittedPassword = ""
        var dismissed = false

        composeRule.setCloseNestContent {
            AuthScreen(
                message = "Sai thông tin đăng nhập. Vui lòng kiểm tra lại email và mật khẩu.",
                isLoading = false,
                onLogin = { email, password ->
                    submittedEmail = email
                    submittedPassword = password
                },
                onDismissMessage = { dismissed = true },
                onNavigateToRegister = {},
                onGoogleLoginClick = {}
            )
        }

        composeRule.onNodeWithText("Sai thông tin đăng nhập. Vui lòng kiểm tra lại email và mật khẩu.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("OK").performClick()
        composeRule.onNodeWithText("Email").performTextInput("user@test.local")
        composeRule.onNodeWithText("Mật khẩu").performTextInput("secret123")
        composeRule.onAllNodesWithText("Đăng nhập")[1].performClick()

        assertTrue(dismissed)
        assertEquals("user@test.local", submittedEmail)
        assertEquals("secret123", submittedPassword)
    }

    @Test
    fun relationship_listShowsDataAndInvokesSearchFilterAddAndDetailCallbacks() {
        var searchQuery = ""
        var selectedTag: RelationshipTag? = null
        var addClicked = false
        var selectedRelationshipId = ""

        composeRule.setCloseNestContent {
            var visibleSearchQuery by remember { mutableStateOf("") }
            RelationshipsScreen(
                uiState = RelationshipsUiState(
                    isLoading = false,
                    searchQuery = visibleSearchQuery,
                    relationships = listOf(sampleRelationshipListItem()),
                    totalRelationships = 1
                ),
                onSearchQueryChanged = {
                    visibleSearchQuery = it
                    searchQuery = it
                },
                onTagSelected = { selectedTag = it },
                onResetFilters = {},
                onAddRelationship = { addClicked = true },
                onRelationshipSelected = { selectedRelationshipId = it }
            )
        }

        composeRule.onNodeWithText("Danh bạ của bạn (1)").assertIsDisplayed()
        composeRule.onNodeWithText("An Nguyen").assertIsDisplayed().performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("An")
        composeRule.onNodeWithText("Gia đình").performClick()
        composeRule.onNodeWithText("Thêm người mới").performClick()

        assertEquals("rel-1", selectedRelationshipId)
        assertEquals("An", searchQuery)
        assertEquals(RelationshipTag.Family, selectedTag)
        assertTrue(addClicked)
    }

    @Test
    fun relationship_emptyStateIsVisible() {
        composeRule.setCloseNestContent {
            RelationshipsScreen(
                uiState = RelationshipsUiState(isLoading = false),
                onSearchQueryChanged = {},
                onTagSelected = {},
                onResetFilters = {},
                onAddRelationship = {},
                onRelationshipSelected = {}
            )
        }

        composeRule.onNodeWithText("Chưa có ai trong danh bạ thân quen").assertIsDisplayed()
    }

    @Test
    fun relationship_errorStateIsVisible() {
        composeRule.setCloseNestContent {
            RelationshipsScreen(
                uiState = RelationshipsUiState(
                    isLoading = false,
                    errorMessageRes = R.string.relationships_sync_error
                ),
                onSearchQueryChanged = {},
                onTagSelected = {},
                onResetFilters = {},
                onAddRelationship = {},
                onRelationshipSelected = {}
            )
        }

        composeRule.onNodeWithText("Không thể đồng bộ danh bạ từ máy chủ. Bạn thử lại sau nhé.")
            .assertIsDisplayed()
    }

    @Test
    fun relationship_addFormShowsValidationAndCollectsInputCallbacks() {
        var submittedName = ""
        var tag = RelationshipTag.Friend
        var saved = false

        composeRule.setCloseNestContent {
            var uiState by remember {
                mutableStateOf(AddRelationshipUiState(nameError = true))
            }

            AddRelationshipScreen(
                uiState = uiState,
                onNavigateBack = {},
                onNameChanged = { uiState = uiState.copy(name = it, nameError = false) },
                onTagChanged = {
                    tag = it
                    uiState = uiState.copy(selectedTag = it)
                },
                onPhoneNumberChanged = {},
                onEmailChanged = {},
                onBirthdayChanged = {},
                onInterestsChanged = {},
                onNotesChanged = {},
                onPriorityChanged = {},
                onToggleMoreDetails = {},
                onErrorMessageShown = {},
                onSave = {
                    submittedName = uiState.name
                    saved = true
                }
            )
        }

        composeRule.onNode(hasText("Tên người này") and hasSetTextAction())
            .performTextInput("Binh Tran")
        composeRule.onNodeWithText("Gia đình").performClick()
        composeRule.onNodeWithText("Lưu vào danh bạ").performClick()

        assertEquals("Binh Tran", submittedName)
        assertEquals(RelationshipTag.Family, tag)
        assertTrue(saved)
    }

    @Test
    fun addHub_entryPointsAndSavedFeedbackAreVisible() {
        var reflectionOpened = false
        var memoryOpened = false
        var appointmentOpened = false
        var relationshipOpened = false
        var feedbackDismissed = false

        composeRule.setCloseNestContent {
            AddHubScreen(
                uiState = AddHubUiState(
                    isLoadingContacts = false,
                    contacts = listOf(sampleContact()),
                    memorySavedMessageRes = R.string.add_interaction_saved
                ),
                onAddRelationship = { relationshipOpened = true },
                onOpenReflection = { reflectionOpened = true },
                onOpenMemory = { memoryOpened = true },
                onOpenAppointment = { appointmentOpened = true },
                onSavedFeedbackDismissed = { feedbackDismissed = true }
            )
        }

        composeRule.onNodeWithText("Đã lưu").assertIsDisplayed()
        composeRule.onNodeWithText("Đóng").performClick()
        composeRule.onNodeWithText("Bắt đầu").performClick()
        composeRule.onAllNodesWithText("Lưu kỷ niệm")[1].performScrollTo().performClick()
        composeRule.onNodeWithText("Tạo cuộc hẹn").performScrollTo().performClick()
        composeRule.onAllNodesWithText("Thêm người mới")[1].performScrollTo().performClick()

        assertTrue(feedbackDismissed)
        assertTrue(reflectionOpened)
        assertTrue(memoryOpened)
        assertTrue(appointmentOpened)
        assertTrue(relationshipOpened)
    }

    @Test
    fun reflectionForm_exposesContactSelectionStep() {
        composeRule.setCloseNestContent {
            ReflectionScreen(
                uiState = AddHubUiState(
                    isLoadingContacts = false,
                    contacts = listOf(sampleContact())
                ),
                onNavigateBack = {},
                onContactToggled = {},
                onMoodSelected = {},
                onFeelingToggled = {},
                onSourceToggled = {},
                onCompleteReflection = {}
            )
        }

        composeRule.onNodeWithText("Bạn đã gặp hoặc trò chuyện với ai?").assertIsDisplayed()
        composeRule.onNodeWithText("An Nguyen").assertIsDisplayed()
    }

    @Test
    fun memoryForm_exposesValidationError() {
        composeRule.setCloseNestContent {
            MemoryScreen(
                uiState = AddHubUiState(
                    isLoadingContacts = false,
                    contacts = listOf(sampleContact()),
                    memoryErrorMessageRes = R.string.add_interaction_contact_required
                ),
                onNavigateBack = {},
                onContactToggled = {},
                onContactQueryChanged = {},
                onTitleChanged = {},
                onTypeSelected = {},
                onNoteChanged = {},
                onAddPhotoClick = {},
                onLocationChanged = {},
                onLocationSelected = { _, _, _ -> },
                onSave = {}
            )
        }
        composeRule.onNodeWithText("Hãy chọn ít nhất một người để lưu kỷ niệm.").assertIsDisplayed()
    }

    @Test
    fun appointmentForm_exposesValidationError() {
        composeRule.setCloseNestContent {
            AppointmentScreen(
                uiState = AddHubUiState(
                    isLoadingContacts = false,
                    contacts = listOf(sampleContact()),
                    appointmentErrorMessageRes = R.string.add_appointment_location_select_required
                ),
                onNavigateBack = {},
                onContactToggled = {},
                onContactQueryChanged = {},
                onLocationChanged = {},
                onLocationSelected = { _, _, _ -> },
                onDateSelected = {},
                onAllDayToggled = {},
                onTimeSelected = { _, _ -> },
                onNoteChanged = {},
                onSave = {}
            )
        }
        composeRule.onNodeWithText("Hãy chọn địa điểm từ danh sách gợi ý.").assertIsDisplayed()
    }

    @Test
    fun notifications_showSummaryFilterDetailActionAndDismissCallbacks() {
        val notification = sampleNotification()
        var selectedFilter: NotificationFilterType? = null
        var dismissedId = ""
        var selectedId = ""
        var actionId = ""
        var deselected = false

        composeRule.setCloseNestContent {
            var selectedNotification by remember {
                mutableStateOf<NotificationItem?>(null)
            }

            NotificationsScreen(
                uiState = NotificationsUiState(
                    isLoading = false,
                    notifications = listOf(notification),
                    summary = NotificationSummary(totalCount = 1, unreadCount = 1, todayCount = 1),
                    filteredNotifications = listOf(notification),
                    selectedNotification = selectedNotification
                ),
                onFilterSelected = { selectedFilter = it },
                onNotificationDismiss = { dismissedId = it },
                onNotificationAction = { actionId = it.id },
                onNotificationSelected = {
                    selectedId = it.id
                    selectedNotification = it
                },
                onNotificationDeselected = {
                    deselected = true
                    selectedNotification = null
                }
            )
        }

        composeRule.onNodeWithText("Thông báo").assertIsDisplayed()
        composeRule.onNodeWithText("Bạn có 1 thông báo").assertIsDisplayed()
        composeRule.onNodeWithText("Chưa đọc").performClick()
        composeRule.onNodeWithText("Nhắc hẹn cà phê").performClick()
        composeRule.onNodeWithText("Xem cuộc hẹn").performClick()

        assertEquals(NotificationFilterType.UNREAD, selectedFilter)
        assertEquals("noti-1", selectedId)
        assertEquals("noti-1", actionId)
        assertEquals("", dismissedId)
        assertTrue(deselected)
    }

    @Test
    fun profile_showsProfileRelationshipPreviewAndLogoutDialog() {
        var menuItem = ""
        var logoutCancelled = false

        composeRule.setCloseNestContent {
            ProfileScreen(
                uiState = ProfileUiState(
                    isLoading = false,
                    user = UserProfile(
                        uid = "user-1",
                        firstName = "Minh",
                        lastName = "Le",
                        email = "minh@test.local",
                        streakCount = 3
                    ),
                    recentRelationships = listOf(
                        RelationshipQuickPreview(id = "rel-1", name = "An Nguyen", initials = "AN")
                    )
                ),
                showLogoutDialog = true,
                onMenuItemClicked = { menuItem = it },
                onConfirmLogout = {},
                themeMode = ThemeMode.LIGHT,
                onThemeModeChange = {},
                onDismissSettingsDialog = {},
                onDismissUiCustomizationDialog = {},
                onDismissLanguageDialog = {},
                onCancelLogout = { logoutCancelled = true }
            )
        }

        composeRule.onNodeWithText("Minh Le").assertIsDisplayed()
        composeRule.onNodeWithText("minh@test.local").assertIsDisplayed()
        composeRule.onNodeWithText("An Nguyen").assertIsDisplayed()
        composeRule.onNodeWithText("Tài khoản").performClick()
        composeRule.onNodeWithText("Xác nhận đăng xuất").assertIsDisplayed()
        composeRule.onNodeWithText("Hủy").performClick()

        assertEquals("account", menuItem)
        assertTrue(logoutCancelled)
    }

    private fun sampleRelationshipListItem() = RelationshipListItem(
        id = "rel-1",
        name = "An Nguyen",
        initials = "AN",
        tag = RelationshipTag.Family,
        birthdayIso = "2000-01-02",
        phoneNumber = "0900000000",
        email = "an@test.local",
        interests = listOf("cafe", "sach"),
        notes = "Ban than",
        priority = RelationshipPriority.High
    )

    private fun sampleContact() = ReflectionContactListItem(
        id = "rel-1",
        name = "An Nguyen"
    )

    private fun sampleNotification() = NotificationItem(
        id = "noti-1",
        type = NotificationType.APPOINTMENT_REMINDER,
        status = NotificationStatus.ACTIVE,
        relationshipId = "rel-1",
        relationshipName = "An Nguyen",
        relationshipAvatarUrl = null,
        title = "Nhắc hẹn cà phê",
        description = "Bạn có cuộc hẹn tại thư viện.",
        createdAtMillis = System.currentTimeMillis(),
        expiresAtMillis = null,
        actionLabel = "Xem cuộc hẹn",
        actionType = NotificationActionType.VIEW_APPOINTMENT,
        sourceEntityId = "app-1",
        sourceEntityType = "appointment"
    )

}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.setCloseNestContent(
    content: @Composable () -> Unit
) {
    setContent {
        AppTheme(themeMode = ThemeMode.LIGHT, dynamicColor = false) {
            content()
        }
    }
}
