package com.example.closenest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.closenest.R
import com.example.closenest.model.MainTab

@Composable
fun HomeBottomBar(
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit
) {
    val items = listOf(
        MainTab.Map to Icons.Outlined.Place,
        MainTab.Relationships to Icons.Outlined.PeopleAlt,
        MainTab.Notifications to Icons.Outlined.Notifications,
        MainTab.Profile to Icons.Outlined.AccountCircle
    )

    Surface(
        modifier = Modifier.navigationBarsPadding(),
        tonalElevation = 12.dp,
        shadowElevation = 10.dp,
        color = MaterialTheme.colorScheme.surface
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
                    tab = tab,
                    selected = selectedTab == tab,
                    icon = icon,
                    onClick = { onSelectTab(tab) }
                )
            }

            AddTabButton(
                selected = selectedTab == MainTab.Add,
                onClick = { onSelectTab(MainTab.Add) }
            )

            items.drop(2).forEach { (tab, icon) ->
                BottomTabItem(
                    tab = tab,
                    selected = selectedTab == tab,
                    icon = icon,
                    onClick = { onSelectTab(tab) }
                )
            }
        }
    }
}

@Composable
private fun AddTabButton(
    selected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val background = if (selected) {
        colorScheme.primary
    } else {
        colorScheme.primary.copy(alpha = 0.12f)
    }
    val contentColor = if (selected) {
        colorScheme.onPrimary
    } else {
        colorScheme.primary
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = stringResource(R.string.tab_add),
            tint = contentColor,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun BottomTabItem(
    tab: MainTab,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val color = if (selected) colorScheme.onSurface else colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .width(76.dp)
            .wrapContentHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides color) {
            Icon(icon, contentDescription = stringResource(tab.labelRes))
        }
        Text(
            text = stringResource(tab.labelRes),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
