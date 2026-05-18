package com.example.closenest.features.homepage.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.closenest.features.homepage.model.MapMarker
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location

@Composable
fun HomeMapScreen(
    modifier: Modifier = Modifier
) {
    var searchText by rememberSaveable { mutableStateOf("San Francisco") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6F1EA))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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

        MapCard(modifier = Modifier.weight(1f))
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
private fun MapCard(
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(size = 26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F4F8)),
        modifier = modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            MapBackground()
        }
    }
}

@Composable
private fun MapBackground() {
    var requestedCameraOnce by remember { mutableStateOf(false) }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(2.0)
            center(Point.fromLngLat(-98.0, 39.5))
            pitch(0.0)
            bearing(0.0)
        }
    }

    MapboxMap(
        modifier = Modifier.fillMaxSize(),
        mapViewportState = mapViewportState,
        scaleBar = {},
        logo = {
            Logo()
        },
        attribution = {
            Attribution()
        }
    ) {
        MapEffect(Unit) { mapView ->
            mapView.location.updateSettings {
                locationPuck = createDefault2DPuck(withBearing = true)
                enabled = true
                puckBearing = PuckBearing.HEADING
                puckBearingEnabled = true
            }

            if (!requestedCameraOnce) {
                requestedCameraOnce = true
                mapView.location.addOnIndicatorPositionChangedListener(
                    object : OnIndicatorPositionChangedListener {
                        override fun onIndicatorPositionChanged(point: Point) {
                            mapViewportState.setCameraOptions {
                                center(point)
                                zoom(14.0)
                                pitch(0.0)
                                bearing(0.0)
                            }
                            mapView.location.removeOnIndicatorPositionChangedListener(this)
                        }
                    }
                )
            }
        }
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
