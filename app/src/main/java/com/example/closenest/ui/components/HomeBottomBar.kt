package com.example.closenest.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.closenest.model.MainTab

@Composable
fun HomeBottomBar(
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit
) {
    val items = listOf(
        MainTab.Map to Icons.Outlined.Place,
        MainTab.Relationships to Icons.Outlined.Share,
        MainTab.Notifications to Icons.Outlined.Notifications,
        MainTab.Profile to Icons.Outlined.AccountCircle
    )

    Surface(
        tonalElevation = 10.dp,
        shadowElevation = 10.dp,
        color = Color(0xFFFFFBF7)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            items.take(2).forEach { (tab, icon) ->
                BottomTabItem(
                    label = tab.label,
                    selected = selectedTab == tab,
                    icon = {
                        Icon(icon, contentDescription = tab.label)
                    },
                    onClick = { onSelectTab(tab) }
                )
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clickable { onSelectTab(MainTab.Map) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.AddCircle,
                    contentDescription = "Thêm",
                    tint = Color(0xFF1F1C19),
                    modifier = Modifier.size(34.dp)
                )
            }

            items.drop(2).forEach { (tab, icon) ->
                BottomTabItem(
                    label = tab.label,
                    selected = selectedTab == tab,
                    icon = {
                        Icon(icon, contentDescription = tab.label)
                    },
                    onClick = { onSelectTab(tab) }
                )
            }
        }
    }
}

@Composable
private fun BottomTabItem(
    label: String,
    selected: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val color = if (selected) Color(0xFF1F1C19) else Color(0xFF857A72)

    Column(
        modifier = Modifier
            .width(76.dp)
            .wrapContentHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides color) {
            icon()
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
