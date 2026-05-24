package com.example.closenest.features.homepage.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val DefaultMapZoom = 2f
private const val FocusedMapZoom = 14f

@Composable
fun HomeMapScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "CloseNest",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 34.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        )

        MapCard(modifier = Modifier.fillMaxSize())
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
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            MapBackground()
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun MapBackground() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasLocationPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    var hasCenteredOnUser by rememberSaveable { mutableStateOf(false) }
    var isMapLoaded by remember { mutableStateOf(false) }

    val defaultCenter = LatLng(39.5, -98.0)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCenter, DefaultMapZoom)
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasLocationPermission = context.hasLocationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(hasLocationPermission, hasCenteredOnUser, isMapLoaded) {
        if (!hasLocationPermission || hasCenteredOnUser) {
            return@LaunchedEffect
        }

        val userLatLng = context.findBestUserLocation()
        if (userLatLng != null) {
            if (isMapLoaded) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(userLatLng, FocusedMapZoom)
                )
            } else {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                    userLatLng,
                    FocusedMapZoom
                )
            }
            hasCenteredOnUser = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapLoaded = { isMapLoaded = true },
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
            uiSettings = MapUiSettings(
                mapToolbarEnabled = false,
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false
            )
        )

        Card(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 20.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .width(76.dp)
                    .height(38.dp)
            ) {
                ZoomControlButton(
                    symbol = "-",
                    onClick = {
                        coroutineScope.launch {
                            if (isMapLoaded) {
                                cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                            }
                        }
                    }
                )
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    color = Color(0xFFE8E1D9)
                )
                ZoomControlButton(
                    symbol = "+",
                    onClick = {
                        coroutineScope.launch {
                            if (isMapLoaded) {
                                cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ZoomControlButton(
    symbol: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(38.dp)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color(0xFF2A2724)
        )
    }
}

private suspend fun Context.findBestUserLocation(): LatLng? {
    if (!hasLocationPermission()) {
        return null
    }

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    val lastLocation = runCatching { fusedLocationClient.lastLocation.await() }.getOrNull()
    if (lastLocation != null) {
        return lastLocation.toLatLng()
    }

    val cancellationTokenSource = CancellationTokenSource()
    return try {
        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .await()
            ?.toLatLng()
    } finally {
        cancellationTokenSource.cancel()
    }
}

private fun Location.toLatLng(): LatLng = LatLng(latitude, longitude)

private fun Context.hasLocationPermission(): Boolean {
    val fineLocationGranted = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseLocationGranted = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return fineLocationGranted || coarseLocationGranted
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) {
            continuation.resume(result)
        }
    }
    addOnFailureListener { exception ->
        if (continuation.isActive) {
            continuation.resumeWithException(exception)
        }
    }
    addOnCanceledListener {
        if (continuation.isActive) {
            continuation.cancel()
        }
    }
}
