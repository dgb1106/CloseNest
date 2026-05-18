package com.example.closenest.features.homepage.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.closenest.R
import com.example.closenest.features.homepage.model.MapMarker
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private data class GeocodingResult(
    val name: String,
    val fullAddress: String,
    val point: Point
)

@Composable
fun HomeMapScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6F1EA))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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

@SuppressLint("UnusedBoxWithConstraintsScope")
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
    val context = LocalContext.current
    val accessToken = context.getString(R.string.mapbox_access_token)
    val coroutineScope = rememberCoroutineScope()

    var mapSearchQuery by rememberSaveable { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<GeocodingResult>>(emptyList()) }
    var showDropdown by remember { mutableStateOf(false) }
    var isSearchFocused by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var selectedPoint by remember { mutableStateOf<Point?>(null) }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(2.0)
            center(Point.fromLngLat(-98.0, 39.5))
            pitch(0.0)
            bearing(0.0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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

        // Search overlay at top of map
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showDropdown && searchResults.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    LazyColumn {
                        items(searchResults) { result ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        mapSearchQuery = result.name
                                        selectedPoint = result.point
                                        showDropdown = false
                                        mapViewportState.setCameraOptions {
                                            center(result.point)
                                            zoom(14.0)
                                            pitch(0.0)
                                            bearing(0.0)
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Place,
                                    contentDescription = null,
                                    tint = Color(0xFFE18567),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = result.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF2A2724),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = result.fullAddress,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF7A6F67),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = mapSearchQuery,
                onValueChange = { query ->
                    mapSearchQuery = query
                    searchJob?.cancel()
                    if (query.length >= 2) {
                        searchJob = coroutineScope.launch {
                            delay(300)
                            isSearching = true
                            try {
                                searchResults = geocode(query, accessToken)
                                showDropdown = searchResults.isNotEmpty() && isSearchFocused
                            } catch (_: Exception) {
                                searchResults = emptyList()
                                showDropdown = false
                            }
                            isSearching = false
                        }
                    } else {
                        searchResults = emptyList()
                        showDropdown = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        isSearchFocused = focusState.isFocused
                        if (focusState.isFocused && searchResults.isNotEmpty()) {
                            showDropdown = true
                        } else if (!focusState.isFocused) {
                            showDropdown = false
                        }
                    },
                shape = RoundedCornerShape(22.dp),
                singleLine = true,
                leadingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFE18567)
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = "Tìm kiếm",
                            tint = Color(0xFF5F5650)
                        )
                    }
                },
                trailingIcon = {
                    Icon(
                        Icons.Outlined.Share,
                        contentDescription = "Chia sẻ"
                    )
                },
                label = { Text("Khu vực tìm kiếm") },
                supportingText = {
                    Text(
                        text = "12 - 15 thg 9  •  1 phòng  •  2 khách",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFFE18567),
                    unfocusedBorderColor = Color(0xFFE3DCD3)
                )
            )
        }
    }
}

private suspend fun geocode(
    query: String,
    accessToken: String
): List<GeocodingResult> = withContext(Dispatchers.IO) {
    val encoded = URLEncoder.encode(query, "UTF-8")
    val url = URL("https://api.mapbox.com/geocoding/v5/mapbox.places/$encoded.json?access_token=$accessToken&limit=5&language=vi")
    val connection = url.openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        val response = connection.inputStream.bufferedReader().readText()
        val json = JSONObject(response)
        val features = json.getJSONArray("features")
        (0 until features.length()).map { i ->
            val feature = features.getJSONObject(i)
            val center = feature.getJSONArray("center")
            GeocodingResult(
                name = feature.getString("text"),
                fullAddress = feature.getString("place_name"),
                point = Point.fromLngLat(center.getDouble(0), center.getDouble(1))
            )
        }
    } finally {
        connection.disconnect()
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
