package com.example.closenest.features.homepage.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.location.Location
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.closenest.R
import com.example.closenest.features.homepage.model.AppointmentItem
import com.example.closenest.features.homepage.model.MemoryItem
import com.example.closenest.features.homepage.repository.AppointmentRepositoryProvider
import com.example.closenest.features.homepage.repository.MemoryRepositoryProvider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val DefaultMapZoom = 2f
private const val FocusedMapZoom = 14f

private const val PinWidthDp = 12
private const val PinHeightDp = 26

private fun vectorToPinBitmapDescriptor(
    context: Context,
    drawableRes: Int,
    density: Float
): BitmapDescriptor {
    val drawable: Drawable = ContextCompat.getDrawable(context, drawableRes)!!
    val w = (PinWidthDp * density).toInt()
    val h = (PinHeightDp * density).toInt()
    drawable.setBounds(0, 0, w, h)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

@Composable
fun HomeMapScreen(
    modifier: Modifier = Modifier
) {
    val appointmentRepository = remember { AppointmentRepositoryProvider.repository }
    val memoryRepository = remember { MemoryRepositoryProvider.repository }
    var upcomingCount by remember { mutableStateOf<Int?>(null) }
    var appointmentList by remember { mutableStateOf<List<AppointmentItem>>(emptyList()) }
    var showAppointmentsDialog by rememberSaveable { mutableStateOf(false) }
    var memories by remember { mutableStateOf<List<MemoryItem>>(emptyList()) }
    var selectedMemory by remember { mutableStateOf<MemoryItem?>(null) }
    var selectedAppointment by remember { mutableStateOf<AppointmentItem?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    val latestAppointments = runCatching {
                        appointmentRepository.getUpcomingAppointments()
                    }.getOrDefault(emptyList())
                    appointmentList = latestAppointments
                    upcomingCount = latestAppointments.size

                    memories = runCatching {
                        memoryRepository.getMemories()
                    }.getOrDefault(emptyList())
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CloseNest",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 34.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(14.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAppointmentsDialog = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val label = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF9E9E9E))) {
                        append("Bạn đang có ")
                    }
                    when (val count = upcomingCount) {
                        null -> withStyle(SpanStyle(color = Color(0xFF9E9E9E))) {
                            append("...")
                        }
                        else -> withStyle(
                            SpanStyle(
                                color = Color(0xFF8B5E34),
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append(count.toString())
                        }
                    }
                    withStyle(SpanStyle(color = Color(0xFF9E9E9E))) {
                        append(" cuộc hẹn")
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 22.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "▼",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 14.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        MapCard(
            memories = memories,
            appointments = appointmentList,
            onMemorySelected = { selectedMemory = it },
            onAppointmentSelected = { selectedAppointment = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }

    if (showAppointmentsDialog) {
        AppointmentsDialog(
            appointments = appointmentList,
            onDismiss = { showAppointmentsDialog = false }
        )
    }

    selectedMemory?.let { memory ->
        DetailDialog(
            title = "Kỷ niệm",
            itemTitle = memory.title,
            subtitle = "Với ${memory.contactName}",
            location = memory.location,
            note = memory.note,
            photoUri = memory.photoUri,
            onDismiss = { selectedMemory = null }
        )
    }

    selectedAppointment?.let { appointment ->
        DetailDialog(
            title = "Cuộc hẹn",
            itemTitle = appointment.name,
            subtitle = formatAppointmentDate(appointment.appointmentDateMillis),
            location = appointment.location,
            note = null,
            photoUri = null,
            onDismiss = { selectedAppointment = null }
        )
    }
}

@Composable
private fun AppointmentsDialog(
    appointments: List<AppointmentItem>,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { appointments.size })

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(horizontal = 4.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cuộc hẹn sắp tới",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Đóng",
                            tint = Color(0xFF757575)
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) { page ->
                    AppointmentCard(appointment = appointments[page])
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(appointments.size) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index) {
                                        Color(0xFF616161)
                                    } else {
                                        Color(0xFFBDBDBD)
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentCard(appointment: AppointmentItem) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F6FA)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8E1D9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Event,
                        contentDescription = null,
                        tint = Color(0xFF5D4E37)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = appointment.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatAppointmentDate(appointment.appointmentDateMillis),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF757575)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = null,
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = appointment.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF616161)
                )
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun MapCard(
    memories: List<MemoryItem>,
    appointments: List<AppointmentItem>,
    onMemorySelected: (MemoryItem) -> Unit,
    onAppointmentSelected: (AppointmentItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(size = 26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F4F8)),
        modifier = modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            MapBackground(
                memories = memories,
                appointments = appointments,
                onMemorySelected = onMemorySelected,
                onAppointmentSelected = onAppointmentSelected
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun MapBackground(
    memories: List<MemoryItem> = emptyList(),
    appointments: List<AppointmentItem> = emptyList(),
    onMemorySelected: (MemoryItem) -> Unit = {},
    onAppointmentSelected: (AppointmentItem) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val density = context.resources.displayMetrics.density

    val memoryPin = remember {
        MapsInitializer.initialize(context)
        vectorToPinBitmapDescriptor(context, R.drawable.red_pin, density)
    }
    val appointmentPin = remember {
        MapsInitializer.initialize(context)
        vectorToPinBitmapDescriptor(context, R.drawable.blue_pin, density)
    }

    var hasLocationPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    var hasCenteredOnUser by rememberSaveable { mutableStateOf(false) }
    var isMapLoaded by remember { mutableStateOf(false) }

    val defaultCenter = LatLng(39.5, -98.0)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCenter, DefaultMapZoom)
    }

    val nowMillis = remember { System.currentTimeMillis() }
    val activeAppointments = remember(appointments) {
        appointments.filter { it.appointmentDateMillis >= nowMillis }
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
        ) {
            memories.forEach { memory ->
                val lat = memory.locationLatitude ?: return@forEach
                val lng = memory.locationLongitude ?: return@forEach
                Marker(
                    state = MarkerState(position = LatLng(lat, lng)),
                    title = memory.title,
                    snippet = memory.contactName,
                    icon = memoryPin,
                    onClick = {
                        onMemorySelected(memory)
                        true
                    }
                )
            }

            activeAppointments.forEach { appointment ->
                Marker(
                    state = MarkerState(
                        position = LatLng(
                            appointment.locationLatitude,
                            appointment.locationLongitude
                        )
                    ),
                    title = appointment.name,
                    snippet = appointment.location,
                    icon = appointmentPin,
                    onClick = {
                        onAppointmentSelected(appointment)
                        true
                    }
                )
            }
        }

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

private val appointmentDateFormatter = SimpleDateFormat("EEEE, dd 'tháng' MM, yyyy", Locale("vi"))

private fun formatAppointmentDate(dateMillis: Long): String {
    return appointmentDateFormatter.format(Date(dateMillis))
}

@Composable
private fun DetailDialog(
    title: String,
    itemTitle: String,
    subtitle: String,
    location: String?,
    note: String?,
    photoUri: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Đóng",
                            tint = Color(0xFF757575)
                        )
                    }
                }

                photoUri?.takeIf { it.isNotBlank() }?.let { uri ->
                    val bitmap = remember(uri) {
                        runCatching {
                            val parsed = Uri.parse(uri)
                            context.contentResolver.openInputStream(parsed)?.use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }
                        }.getOrNull()
                    }
                    if (bitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Ảnh",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = itemTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                DetailRow(label = "Với", value = subtitle)

                location?.takeIf { it.isNotBlank() }?.let { loc ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Place,
                            contentDescription = null,
                            tint = Color(0xFF9E9E9E),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = loc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF616161)
                        )
                    }
                }

                note?.takeIf { it.isNotBlank() }?.let { noteText ->
                    DetailRow(label = "Mô tả", value = noteText)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF9E9E9E)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
