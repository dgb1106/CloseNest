package com.example.closenest.features.chatbot.ui

import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ChipColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.closenest.R
import com.example.closenest.core.ui.theme.AppTheme
import com.example.closenest.features.chatbot.model.ChatMessage
import com.example.closenest.features.chatbot.model.ChatMode
import com.example.closenest.features.chatbot.model.ChatRole
import com.example.closenest.features.chatbot.model.ChatSession
import com.example.closenest.features.chatbot.viewmodel.ChatbotUiState
import com.example.closenest.features.chatbot.viewmodel.ChatbotViewModel
import com.example.closenest.features.chatbot.viewmodel.QuickRecipient
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun FloatingChatbotOverlay(
    modifier: Modifier = Modifier,
    viewModel: ChatbotViewModel = viewModel(factory = ChatbotViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isPanelOpen by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }

        if (isPanelOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.16f))
                    .clickable { isPanelOpen = false }
                    .imePadding()
            )
            ChatbotPanel(
                uiState = uiState,
                onClose = { isPanelOpen = false },
                onNewConversation = viewModel::requestNewConversation,
                onSelectMode = viewModel::selectMode,
                onSelectSession = viewModel::selectSession,
                onStartSessionSelection = viewModel::startSessionSelection,
                onCancelSessionSelection = viewModel::cancelSessionSelection,
                onToggleSessionSelection = viewModel::toggleSessionSelection,
                onDeleteSelectedSessions = viewModel::deleteSelectedSessions,
                onInputChanged = viewModel::onInputChanged,
                onSend = viewModel::sendMessage,
                onQuickSend = viewModel::sendQuickMessage,
                onDismissError = viewModel::clearError,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            )
        } else {
            DraggableChatButton(
                containerWidthPx = containerWidthPx,
                containerHeightPx = containerHeightPx,
                onClick = {
                    viewModel.requestNewConversation()
                    isPanelOpen = true
                }
            )
        }
    }
}

@Composable
private fun DraggableChatButton(
    containerWidthPx: Float,
    containerHeightPx: Float,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val buttonSize = 58.dp
    val buttonSizePx = with(density) { buttonSize.toPx() }
    val edgePaddingPx = with(density) { 16.dp.toPx() }
    val bottomPaddingPx = with(density) { 112.dp.toPx() }
    val minX = edgePaddingPx
    val maxX = (containerWidthPx - buttonSizePx - edgePaddingPx).coerceAtLeast(minX)
    val minY = edgePaddingPx
    val maxY = (containerHeightPx - buttonSizePx - bottomPaddingPx).coerceAtLeast(minY)

    var isInitialized by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var x by remember { mutableFloatStateOf(0f) }
    var y by remember { mutableFloatStateOf(0f) }
    var snapRequest by remember { mutableIntStateOf(0) }
    var lastInteractionMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(containerWidthPx, containerHeightPx) {
        if (containerWidthPx <= 0f || containerHeightPx <= 0f) return@LaunchedEffect
        if (!isInitialized) {
            x = maxX
            y = (containerHeightPx * 0.56f).coerceIn(minY, maxY)
            isInitialized = true
        } else {
            x = x.coerceIn(minX, maxX)
            y = y.coerceIn(minY, maxY)
        }
    }

    LaunchedEffect(snapRequest) {
        if (!isInitialized || containerWidthPx <= 0f) return@LaunchedEffect
        val targetX = if (x + buttonSizePx / 2f < containerWidthPx / 2f) {
            minX
        } else {
            maxX
        }
        animate(
            initialValue = x,
            targetValue = targetX,
            animationSpec = tween(durationMillis = 260)
        ) { value, _ ->
            x = value.coerceIn(minX, maxX)
        }
    }

    LaunchedEffect(lastInteractionMillis, isDragging, isInitialized) {
        if (!isInitialized || isDragging) return@LaunchedEffect
        delay(IdleSnapDelayMillis)
        snapRequest += 1
    }

    Surface(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = x.roundToInt(),
                    y = y.roundToInt()
                )
            }
            .size(buttonSize)
            .shadow(10.dp, CircleShape)
            .clip(CircleShape)
            .pointerInput(containerWidthPx, containerHeightPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalDrag = Offset.Zero
                    var hasDragged = false
                    var isPastTouchSlop = false
                    lastInteractionMillis = System.currentTimeMillis()

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { pointer ->
                            pointer.id == down.id
                        } ?: break

                        if (!change.pressed) {
                            break
                        }

                        val dragAmount = change.position - change.previousPosition
                        if (dragAmount != Offset.Zero) {
                            totalDrag += dragAmount
                            if (!isPastTouchSlop && totalDrag.getDistance() > viewConfiguration.touchSlop) {
                                isPastTouchSlop = true
                                hasDragged = true
                                isDragging = true
                            }
                            if (isPastTouchSlop) {
                                x = (x + dragAmount.x).coerceIn(minX, maxX)
                                y = (y + dragAmount.y).coerceIn(minY, maxY)
                                change.consume()
                            }
                        }
                        lastInteractionMillis = System.currentTimeMillis()
                    }

                    isDragging = false
                    lastInteractionMillis = System.currentTimeMillis()
                    snapRequest += 1
                    if (!hasDragged) {
                        onClick()
                    }
                }
            },
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = stringResource(R.string.chatbot_open),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun ChatbotPanel(
    uiState: ChatbotUiState,
    onClose: () -> Unit,
    onNewConversation: () -> Unit,
    onSelectMode: (ChatMode) -> Unit,
    onSelectSession: (ChatSession) -> Unit,
    onStartSessionSelection: () -> Unit,
    onCancelSessionSelection: () -> Unit,
    onToggleSessionSelection: (String) -> Unit,
    onDeleteSelectedSessions: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onQuickSend: (String) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .fillMaxHeight(0.78f)
            .statusBarsPadding(),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 4.dp,
        shadowElevation = 10.dp,
        color = MaterialTheme.colorScheme.onPrimary
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            ChatbotHeader(
                mode = uiState.mode,
                onNewConversation = onNewConversation,
                onClose = onClose
            )

            AnimatedVisibility(
                visible = uiState.errorMessage != null || uiState.errorMessageRes != null
            ) {
                val message = uiState.errorMessage
                    ?: uiState.errorMessageRes?.let { messageRes -> stringResource(messageRes) }
                message?.let {
                    ErrorBanner(
                        message = it,
                        onDismiss = onDismissError
                    )
                }
            }

            ChatbotMessages(
                uiState = uiState,
                onSelectMode = onSelectMode,
                onSelectSession = onSelectSession,
                onStartSessionSelection = onStartSessionSelection,
                onCancelSessionSelection = onCancelSessionSelection,
                onToggleSessionSelection = onToggleSessionSelection,
                onDeleteSelectedSessions = onDeleteSelectedSessions,
                onInputChanged = onInputChanged,
                onSend = onSend,
                onQuickSend = onQuickSend,
                modifier = Modifier.weight(1f)
            )

            if (uiState.mode != null) {
                ChatbotInput(
                    mode = uiState.mode,
                    value = uiState.inputText,
                    canSend = uiState.canSend,
                    isSending = uiState.isSending,
                    recentRecipients = uiState.recentRecipients,
                    showGiftRecipientQuickReplies = uiState.showGiftRecipientQuickReplies,
                    showGiftRequirementQuickReplies = uiState.showGiftRequirementQuickReplies,
                    onValueChange = onInputChanged,
                    onSend = onSend,
                    onQuickReplyClick = onQuickSend
                )
            }
        }
    }
}

@Composable
private fun ChatbotHeader(
    mode: ChatMode?,
    onNewConversation: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 14.dp, end = 8.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.chatbot_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
//            Text(
//                text = mode?.let { stringResource(it.labelRes()) }
//                    ?: stringResource(R.string.chatbot_welcome),
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant,
//                maxLines = 1,
//                overflow = TextOverflow.Ellipsis
//            )
        }

        if (mode != null) {
            IconButton(onClick = onNewConversation) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.chatbot_new_chat)
                )
            }
        }

        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.chatbot_close)
            )
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.common_close),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatbotMessages(
    uiState: ChatbotUiState,
    onSelectMode: (ChatMode) -> Unit,
    onSelectSession: (ChatSession) -> Unit,
    onStartSessionSelection: () -> Unit,
    onCancelSessionSelection: () -> Unit,
    onToggleSessionSelection: (String) -> Unit,
    onDeleteSelectedSessions: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onQuickSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val messageCount = uiState.messages.size +
        if (uiState.showModeOptions) 1 else 0 +
        if (uiState.isSending) 1 else 0

    LaunchedEffect(messageCount, uiState.sessionId) {
        if (uiState.sessionId != null && messageCount > 0) {
            listState.animateScrollToItem(messageCount - 1)
        }
    }

    if (uiState.isLoading && uiState.messages.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (uiState.showModeOptions) {
            item(key = "chat-home") {
                ChatHome(
                    uiState = uiState,
                    onSelectMode = onSelectMode,
                    onSelectSession = onSelectSession,
                    onStartSessionSelection = onStartSessionSelection,
                    onCancelSessionSelection = onCancelSessionSelection,
                    onToggleSessionSelection = onToggleSessionSelection,
                    onDeleteSelectedSessions = onDeleteSelectedSessions,
                    onInputChanged = onInputChanged,
                    onSend = onSend,
                    onQuickSend = onQuickSend
                )
            }
        } else {
            items(
                items = uiState.messages,
                key = { message -> message.id }
            ) { message ->
                MessageBubble(message = message)
            }
        }

        if (uiState.isSending) {
            item(key = "typing") {
                TypingBubble()
            }
        }
    }
}

@Composable
private fun ChatHome(
    uiState: ChatbotUiState,
    onSelectMode: (ChatMode) -> Unit,
    onSelectSession: (ChatSession) -> Unit,
    onStartSessionSelection: () -> Unit,
    onCancelSessionSelection: () -> Unit,
    onToggleSessionSelection: (String) -> Unit,
    onDeleteSelectedSessions: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onQuickSend: (String) -> Unit
) {
    val isBusy = uiState.isSending || uiState.isCreatingSession || uiState.isDeletingSessions
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        WelcomeInput(
            value = uiState.inputText,
            canSend = uiState.canSend,
            isSending = isBusy,
            onValueChange = onInputChanged,
            onSend = onSend
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ModeChip(
                label = stringResource(R.string.chatbot_mode_vent),
                icon = Icons.Outlined.FavoriteBorder,
                enabled = !isBusy,
                onClick = { onSelectMode(ChatMode.Vent) },
            )
            ModeChip(
                label = stringResource(R.string.chatbot_mode_gift),
                icon = Icons.Outlined.CardGiftcard,
                enabled = !isBusy,
                onClick = { onSelectMode(ChatMode.GiftAdvice) }
            )
        }
        ChatHistorySection(
            sessions = uiState.sessions,
            isSelecting = uiState.isSelectingSessions,
            selectedSessionIds = uiState.selectedSessionIds,
            isDeleting = uiState.isDeletingSessions,
            onSelectSession = onSelectSession,
            onStartSelection = onStartSessionSelection,
            onCancelSelection = onCancelSessionSelection,
            onToggleSelection = onToggleSessionSelection,
            onDeleteSelectedSessions = onDeleteSelectedSessions
        )
    }
}

@Composable
private fun WelcomeInput(
    value: String,
    canSend: Boolean,
    isSending: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .onKeyEvent { event ->
                if (event.key == Key.Enter && event.type == KeyEventType.KeyUp && canSend) {
                    onSend()
                    true
                } else {
                    false
                }
            },
        enabled = !isSending,
        placeholder = { Text(text = stringResource(R.string.chatbot_welcome)) },
        trailingIcon = {
            IconButton(
                onClick = onSend,
                enabled = canSend
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = stringResource(R.string.chatbot_send)
                )
            }
        },
        shape = RoundedCornerShape(20.dp),
        minLines = 1,
        maxLines = 3,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(
            onSend = {
                if (canSend) {
                    onSend()
                }
            }
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedContainerColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun QuickRecipientsSection(
    recipients: List<QuickRecipient>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onRecipientClick: (QuickRecipient) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Gần đây",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(
                items = recipients,
                key = { recipient -> recipient.id }
            ) { recipient ->
                AssistChip(
                    onClick = { onRecipientClick(recipient) },
                    enabled = enabled,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color.White,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    label = { Text(text = recipient.name) }
                )
            }
        }
    }
}

@Composable
private fun ChatHistorySection(
    sessions: List<ChatSession>,
    isSelecting: Boolean,
    selectedSessionIds: Set<String>,
    isDeleting: Boolean,
    onSelectSession: (ChatSession) -> Unit,
    onStartSelection: () -> Unit,
    onCancelSelection: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onDeleteSelectedSessions: () -> Unit
) {
    val selectedCount = selectedSessionIds.size
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isSelecting, selectedCount) {
        if (!isSelecting || selectedCount == 0) {
            showDeleteDialog = false
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.chatbot_history_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (sessions.isNotEmpty()) {
                if (isSelecting) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onCancelSelection,
                            enabled = !isDeleting
                        ) {
                            Text(text = stringResource(R.string.chatbot_history_cancel))
                        }
                        TextButton(
                            onClick = { showDeleteDialog = true },
                            enabled = selectedCount > 0 && !isDeleting,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.chatbot_history_delete_count,
                                    selectedCount
                                )
                            )
                        }
                    }
                } else {
                    TextButton(
                        onClick = onStartSelection,
                        enabled = !isDeleting
                    ) {
                        Text(text = stringResource(R.string.chatbot_history_select))
                    }
                }
            }
        }

        if (sessions.isEmpty()) {
            Text(
                text = stringResource(R.string.chatbot_history_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        sessions.take(MaxHistoryItems).forEach { session ->
            val isSelected = selectedSessionIds.contains(session.id)
            ChatHistoryItem(
                session = session,
                isSelected = isSelected,
                selectionEnabled = isSelecting,
                isDeleting = isDeleting,
                onClick = {
                    if (isSelecting) {
                        onToggleSelection(session.id)
                    } else {
                        onSelectSession(session)
                    }
                }
            )
        }
    }

    if (showDeleteDialog && selectedCount > 0) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) {
                    showDeleteDialog = false
                }
            },
            title = {
                Text(text = stringResource(R.string.chatbot_history_delete_confirm_title))
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.chatbot_history_delete_confirm_body,
                        selectedCount
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteSelectedSessions()
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(text = stringResource(R.string.chatbot_history_delete_confirm_action))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !isDeleting
                ) {
                    Text(text = stringResource(R.string.chatbot_history_cancel))
                }
            }
        )
    }
}

@Composable
private fun ChatHistoryItem(
    session: ChatSession,
    isSelected: Boolean,
    selectionEnabled: Boolean,
    isDeleting: Boolean,
    onClick: () -> Unit
) {
    val modeLabel = stringResource(session.mode.labelRes())
    val updatedLabel = remember(session.updatedAtMillis) {
        if (session.updatedAtMillis > 0L) {
            DateUtils.getRelativeTimeSpanString(
                session.updatedAtMillis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            ).toString()
        } else {
            ""
        }
    }
    val subtitle = if (updatedLabel.isBlank()) {
        modeLabel
    } else {
        "$modeLabel · $updatedLabel"
    }

    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !isDeleting, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = session.mode.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (selectionEnabled) {
                Icon(
                    imageVector = if (isSelected) {
                        Icons.Outlined.CheckCircle
                    } else {
                        Icons.Outlined.RadioButtonUnchecked
                    },
                    contentDescription = null,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        enabled = enabled,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        label = {
            Text(
                text = label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )

    )
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.User
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val uriHandler = LocalUriHandler.current
    val linkedText = remember(message.text, contentColor) {
        buildLinkedMessageText(
            text = message.text,
            defaultColor = contentColor
        )
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 6.dp,
                bottomEnd = if (isUser) 6.dp else 18.dp
            ),
            color = bubbleColor,
            contentColor = contentColor
        ) {
            ClickableText(
                text = linkedText,
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium.copy(color = contentColor),
                onClick = { offset ->
                    linkedText
                        .getStringAnnotations(tag = LinkAnnotationTag, start = offset, end = offset)
                        .firstOrNull()
                        ?.item
                        ?.let(uriHandler::openUri)
                }
            )
        }
    }
}

@Composable
private fun TypingBubble() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = stringResource(R.string.chatbot_sending),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ChatbotInput(
    mode: ChatMode,
    value: String,
    canSend: Boolean,
    isSending: Boolean,
    recentRecipients: List<QuickRecipient>,
    showGiftRecipientQuickReplies: Boolean,
    showGiftRequirementQuickReplies: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuickReplyClick: (String) -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.onPrimary
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (mode == ChatMode.GiftAdvice && showGiftRecipientQuickReplies && recentRecipients.isNotEmpty()) {
                QuickRecipientsSection(
                    recipients = recentRecipients,
                    enabled = !isSending,
                    modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 12.dp),
                    onRecipientClick = { recipient ->
                        onQuickReplyClick(recipient.name)
                    }
                )
            }
            if (mode == ChatMode.GiftAdvice && showGiftRequirementQuickReplies) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 10.dp, end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 4.dp)
                ) {
                    item {
                        QuickReplyChip(
                            label = "Không có yêu cầu",
                            enabled = !isSending,
                            onClick = { onQuickReplyClick("Không có yêu cầu đặc biệt.") }
                        )
                    }
                    item {
                        QuickReplyChip(
                            label = "Sinh nhật",
                            enabled = !isSending,
                            onClick = { onQuickReplyClick("Tặng sinh nhật.") }
                        )
                    }
                    item {
                        QuickReplyChip(
                            label = "Dưới 300k",
                            enabled = !isSending,
                            onClick = { onQuickReplyClick("Ngân sách dưới 300k.") }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .onKeyEvent { event ->
                            if (event.key == Key.Enter && event.type == KeyEventType.KeyUp && canSend) {
                                onSend()
                                true
                            } else {
                                false
                            }
                        },
                    enabled = !isSending,
                    label = { Text(text = stringResource(R.string.chatbot_input_label)) },
                    shape = RoundedCornerShape(18.dp),
                    minLines = 1,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (canSend) {
                                onSend()
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                IconButton(
                    onClick = onSend,
                    enabled = canSend,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = stringResource(R.string.chatbot_send)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickReplyChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        enabled = enabled,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = Color.White,
            labelColor = MaterialTheme.colorScheme.onSurface
        ),
        label = { Text(text = label) }
    )
}

@StringRes
private fun ChatMode.labelRes(): Int {
    return when (this) {
        ChatMode.General -> R.string.chatbot_mode_general_label
        ChatMode.Vent -> R.string.chatbot_mode_vent_label
        ChatMode.GiftAdvice -> R.string.chatbot_mode_gift_label
    }
}

private fun ChatMode.icon(): ImageVector {
    return when (this) {
        ChatMode.General -> Icons.Outlined.ChatBubbleOutline
        ChatMode.Vent -> Icons.Outlined.FavoriteBorder
        ChatMode.GiftAdvice -> Icons.Outlined.CardGiftcard
    }
}

private fun buildLinkedMessageText(
    text: String,
    defaultColor: Color
) = buildAnnotatedString {
    var currentIndex = 0
    UrlRegex.findAll(text).forEach { match ->
        val start = match.range.first
        val endExclusive = match.range.last + 1
        if (currentIndex < start) {
            append(text.substring(currentIndex, start))
        }
        val url = match.value
        pushStringAnnotation(tag = LinkAnnotationTag, annotation = url)
        withStyle(
            SpanStyle(
                color = LinkColor,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append(url)
        }
        pop()
        currentIndex = endExclusive
    }
    if (currentIndex < text.length) {
        withStyle(SpanStyle(color = defaultColor)) {
            append(text.substring(currentIndex))
        }
    }
}

private const val IdleSnapDelayMillis = 1_200L
private const val MaxHistoryItems = 12
private const val LinkAnnotationTag = "chat_link"
private val UrlRegex = Regex("""https?://[^\s]+""")
private val LinkColor = Color(0xFF1A73E8)

@Preview(name = "Floating Chatbot Button", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun FloatingChatbotButtonPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            DraggableChatButton(
                containerWidthPx = 360f,
                containerHeightPx = 720f,
                onClick = {}
            )
        }
    }
}

@Preview(name = "Floating Chatbot Home", showBackground = true, widthDp = 420, heightDp = 760)
@Composable
private fun FloatingChatbotHomePreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            ChatbotPanel(
                uiState = previewHomeUiState(),
                onClose = {},
                onNewConversation = {},
                onSelectMode = {},
                onSelectSession = {},
                onStartSessionSelection = {},
                onCancelSessionSelection = {},
                onToggleSessionSelection = {},
                onDeleteSelectedSessions = {},
                onInputChanged = {},
                onSend = {},
                onQuickSend = {},
                onDismissError = {},
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            )
        }
    }
}

@Preview(name = "Floating Chatbot Conversation", showBackground = true, widthDp = 420, heightDp = 760)
@Composable
private fun FloatingChatbotConversationPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            ChatbotPanel(
                uiState = previewConversationUiState(),
                onClose = {},
                onNewConversation = {},
                onSelectMode = {},
                onSelectSession = {},
                onStartSessionSelection = {},
                onCancelSessionSelection = {},
                onToggleSessionSelection = {},
                onDeleteSelectedSessions = {},
                onInputChanged = {},
                onSend = {},
                onQuickSend = {},
                onDismissError = {},
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            )
        }
    }
}

private fun previewHomeUiState(): ChatbotUiState {
    val now = System.currentTimeMillis()
    return ChatbotUiState(
        isLoading = false,
        sessions = listOf(
            ChatSession(
                id = "session-1",
                userId = "preview-user",
                mode = ChatMode.Vent,
                title = "Xả stress sau giờ làm",
                createdAtMillis = now - 86_400_000L,
                updatedAtMillis = now - 15 * 60_000L
            ),
            ChatSession(
                id = "session-2",
                userId = "preview-user",
                mode = ChatMode.GiftAdvice,
                title = "Tặng quà cho Linh",
                createdAtMillis = now - 3 * 86_400_000L,
                updatedAtMillis = now - 2 * 3_600_000L
            )
        ),
        inputText = "Hôm nay mình hơi rối, muốn nói chuyện một chút."
    )
}

private fun previewConversationUiState(): ChatbotUiState {
    val now = System.currentTimeMillis()
    return ChatbotUiState(
        isLoading = false,
        sessionId = "session-gift",
        mode = ChatMode.GiftAdvice,
        messages = listOf(
            ChatMessage(
                id = "message-1",
                sessionId = "session-gift",
                role = ChatRole.Assistant,
                text = "Mình giúp bạn chọn quà. Bạn muốn tặng cho ai?",
                createdAtMillis = now - 6 * 60_000L
            ),
            ChatMessage(
                id = "message-2",
                sessionId = "session-gift",
                role = ChatRole.User,
                text = "Mình muốn tặng cho Linh, ngân sách khoảng 500k.",
                createdAtMillis = now - 5 * 60_000L
            ),
            ChatMessage(
                id = "message-3",
                sessionId = "session-gift",
                role = ChatRole.Assistant,
                text = "Bạn có thể tham khảo nến thơm hoặc set skincare mini. Link mẫu: https://example.com/gift-set",
                createdAtMillis = now - 4 * 60_000L
            )
        ),
        sessions = listOf(
            ChatSession(
                id = "session-gift",
                userId = "preview-user",
                mode = ChatMode.GiftAdvice,
                title = "Tặng quà cho Linh",
                createdAtMillis = now - 86_400_000L,
                updatedAtMillis = now - 4 * 60_000L
            )
        ),
        recentRecipients = listOf(
            QuickRecipient(id = "recipient-1", name = "Linh"),
            QuickRecipient(id = "recipient-2", name = "Minh"),
            QuickRecipient(id = "recipient-3", name = "An")
        ),
        inputText = "Có món nào thiên về chăm sóc bản thân không?"
    )
}
