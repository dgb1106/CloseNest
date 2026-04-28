package com.example.closenest.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.closenest.model.DemoMarkers
import com.example.closenest.model.MapMarker
import kotlin.math.abs

private enum class SuggestionSheetState {
    Collapsed,
    Expanded
}

@Composable
fun HomeMapScreen(
    modifier: Modifier = Modifier
) {
    var searchText by rememberSaveable { mutableStateOf("San Francisco") }
    var sheetState by rememberSaveable { mutableStateOf(SuggestionSheetState.Expanded) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    val collapsedHeight = 88.dp
    val expandedHeight = 340.dp
    val sheetHeight by animateDpAsState(
        targetValue = if (sheetState == SuggestionSheetState.Expanded) {
            expandedHeight
        } else {
            collapsedHeight
        },
        animationSpec = tween(durationMillis = 260),
        label = "sheetHeight"
    )
    val surfaceAlpha by animateFloatAsState(
        targetValue = if (sheetState == SuggestionSheetState.Expanded) 0.24f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "surfaceAlpha"
    )

    val draggableState = rememberDraggableState { delta ->
        if (sheetState == SuggestionSheetState.Expanded || delta < 0f) {
            dragOffsetPx = (dragOffsetPx + delta).coerceAtLeast(0f)
        }
    }
    val handleDragStopped: (Float) -> Unit = { velocity ->
        val collapseThresholdPx = 120f
        sheetState = when {
            dragOffsetPx > collapseThresholdPx || velocity > 1200f -> SuggestionSheetState.Collapsed
            dragOffsetPx < -collapseThresholdPx || velocity < -1200f -> SuggestionSheetState.Expanded
            else -> sheetState
        }
        dragOffsetPx = 0f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6F1EA))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = collapsedHeight),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SearchBar(
                value = searchText,
                onValueChange = { searchText = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(label = "Bộ lọc")
                    FilterChip(label = "Sắp xếp")
                }
                Text(
                    text = "99 kết quả",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5F5650),
                    maxLines = 1
                )
            }

            FakeMapCard(
                modifier = Modifier.weight(1f)
            )
        }

        if (sheetState == SuggestionSheetState.Expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = sheetHeight)
                    .background(Color.Black.copy(alpha = surfaceAlpha))
                    .clickable { sheetState = SuggestionSheetState.Collapsed }
            )
        }

        SuggestionSheet(
            state = sheetState,
            height = sheetHeight,
            dragOffsetPx = dragOffsetPx,
            onHeaderClick = {
                sheetState = if (sheetState == SuggestionSheetState.Expanded) {
                    SuggestionSheetState.Collapsed
                } else {
                    SuggestionSheetState.Expanded
                }
                dragOffsetPx = 0f
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .offset(y = with(density) { (dragOffsetPx / 3f).toDp() })
                .draggable(
                    state = draggableState,
                    orientation = androidx.compose.foundation.gestures.Orientation.Vertical,
                    onDragStopped = { velocity -> handleDragStopped(velocity) }
                )
        )
    }
}

@Composable
private fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Outlined.Search, contentDescription = null)
        },
        trailingIcon = {
            Icon(Icons.Outlined.Share, contentDescription = "Chia sẻ")
        },
        label = { Text("Khu vực tìm kiếm") },
        supportingText = {
            Text(
                text = "12 - 15 thg 9  •  1 phòng  •  2 khách",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@Composable
private fun FilterChip(label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE3DCD3), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF2A2724),
            maxLines = 1
        )
        Text(
            text = "▾",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF7A6F67)
        )
    }
}

@Composable
private fun FakeMapCard(
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F4F8)),
        modifier = modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            FakeMapBackground()

            DemoMarkers.forEach { marker ->
                val offsetX = maxWidth * marker.x
                val offsetY = maxHeight * marker.y
                PersonMarker(
                    marker = marker,
                    offsetX = offsetX,
                    offsetY = offsetY
                )
            }
        }
    }
}

@Composable
private fun FakeMapBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = Color(0xFFF5F7FB), style = Fill)

        val verticals = listOf(0.12f, 0.24f, 0.37f, 0.51f, 0.67f, 0.82f)
        val horizontals = listOf(0.16f, 0.29f, 0.46f, 0.61f, 0.78f)

        verticals.forEach { x ->
            drawLine(
                color = Color(0xFFD4DAE4),
                start = androidx.compose.ui.geometry.Offset(size.width * x, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width * x, size.height),
                strokeWidth = 20f,
                cap = StrokeCap.Round
            )
        }

        horizontals.forEach { y ->
            drawLine(
                color = Color(0xFFD4DAE4),
                start = androidx.compose.ui.geometry.Offset(0f, size.height * y),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height * y),
                strokeWidth = 20f,
                cap = StrokeCap.Round
            )
        }

        drawLine(
            color = Color(0xFFBFC7D3),
            start = androidx.compose.ui.geometry.Offset(size.width * 0.08f, size.height * 0.82f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.88f, size.height * 0.18f),
            strokeWidth = 18f,
            cap = StrokeCap.Round
        )

        drawLine(
            color = Color(0xFFE3E7EF),
            start = androidx.compose.ui.geometry.Offset(size.width * 0.04f, size.height * 0.08f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.92f, size.height * 0.92f),
            strokeWidth = 10f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))
        )
    }
}

@Composable
private fun PersonMarker(
    marker: MapMarker,
    offsetX: Dp,
    offsetY: Dp
) {
    val markerSize = if (marker.highlighted) 54.dp else 36.dp
    val background = if (marker.highlighted) Color(0xFF273746) else Color.White
    val border = if (marker.highlighted) Color(0xFF3E5667) else Color(0xFFE3DDD6)
    val textColor = if (marker.highlighted) Color.White else Color(0xFF2A2622)

    Box(
        modifier = Modifier
            .offset(
                x = offsetX - markerSize / 2,
                y = offsetY - markerSize / 2
            )
            .size(markerSize)
            .clip(CircleShape)
            .background(background)
            .border(2.dp, border, CircleShape)
            .clickable(enabled = marker.highlighted) {},
        contentAlignment = Alignment.Center
    ) {
        if (marker.highlighted) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Outlined.Place,
                    contentDescription = null,
                    tint = Color(0xFFE18567),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = marker.label,
                    color = textColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        } else {
            Text(
                text = marker.label,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SuggestionSheet(
    state: SuggestionSheetState,
    height: Dp,
    dragOffsetPx: Float,
    onHeaderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
        color = Color(0xFFFFFBF7),
        tonalElevation = 12.dp,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            SheetHeader(
                expanded = state == SuggestionSheetState.Expanded,
                onClick = onHeaderClick
            )

            if (state == SuggestionSheetState.Expanded || abs(dragOffsetPx) > 1f) {
                ExpandedSuggestionContent()
            }
        }
    }
}

@Composable
private fun SheetHeader(
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFD6CEC5))
            )
            Text(
                text = "Ăn tối với Nam Tran",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF221F1B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Tối nay  •  1,2 dặm",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF746A63),
                maxLines = 1
            )
        }

        Icon(
            imageVector = if (expanded) {
                Icons.Outlined.KeyboardArrowDown
            } else {
                Icons.Outlined.KeyboardArrowUp
            },
            contentDescription = if (expanded) "Thu gọn" else "Mở rộng",
            tint = Color(0xFF5B524B)
        )
    }
}

@Composable
private fun ExpandedSuggestionContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFD7B99A))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(Color(0xFFB48762))
                drawRect(
                    color = Color(0xFFDDBB93),
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.07f, size.height * 0.18f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.86f, size.height * 0.56f)
                )
                drawRect(
                    color = Color(0xFF6C4D35),
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.13f, size.height * 0.28f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.22f, size.height * 0.38f)
                )
                drawRect(
                    color = Color(0xFF8A664A),
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.42f, size.height * 0.36f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.18f, size.height * 0.24f)
                )
                drawRect(
                    color = Color(0xFF5A4433),
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.62f, size.height * 0.30f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.18f, size.height * 0.36f)
                )
            }
        }

        Text(
            text = "Mô tả",
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFF2B2621),
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Một lời gợi ý gặp mặt gần vị trí hiện tại, phù hợp cho bữa tối nhanh và tiện di chuyển.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6F655E)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "1 phòng  •  2 khách",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF746A63),
                maxLines = 1
            )
            Button(
                onClick = {},
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Chi tiết")
            }
        }
    }
}
