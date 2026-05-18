package com.example.closenest.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.closenest.R
import com.example.closenest.core.model.MainTab

@Composable
fun HomeBottomBar(
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        MainTab.Map to Icons.Outlined.Place,
        MainTab.Relationships to Icons.Outlined.PeopleAlt,
        MainTab.Notifications to Icons.Outlined.Notifications,
        MainTab.Profile to Icons.Outlined.AccountCircle
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 10.dp,
                bottom = 0.dp
            ),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 12.dp,
        shadowElevation = 10.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.take(2).forEach { (tab, icon) ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    BottomTabItem(
                        tab = tab,
                        selected = selectedTab == tab,
                        icon = icon,
                        onClick = { onSelectTab(tab) }
                    )
                }
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                AddTabButton(
                    selected = selectedTab == MainTab.Add,
                    onClick = { onSelectTab(MainTab.Add) }
                )
            }

            items.drop(2).forEach { (tab, icon) ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
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
}

@Composable
private fun AddTabButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val background = if (selected) {
        colorScheme.primary
    } else {
        Color.Transparent
    }
    val contentColor = if (selected) {
        colorScheme.onPrimary
    } else {
        colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(background),
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
}

@Composable
private fun BottomTabItem(
    tab: MainTab,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val color = if (selected) colorScheme.onSurface else colorScheme.onSurfaceVariant
    val background = if (selected) {
        colorScheme.primary.copy(alpha = 0.14f)
    } else {
        Color.Transparent
    }

        Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompositionLocalProvider(LocalContentColor provides color) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(tab.labelRes),
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = stringResource(tab.labelRes),
            modifier = Modifier.fillMaxWidth(),
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = color,
            lineHeight = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Clip
        )
    }
}
