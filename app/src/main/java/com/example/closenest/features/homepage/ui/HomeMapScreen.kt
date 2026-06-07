package com.example.closenest.features.homepage.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.closenest.core.notification.AppointmentReminderScheduler
import com.example.closenest.features.homepage.model.AppointmentItem
import com.example.closenest.features.homepage.model.MemoryItem
import com.example.closenest.features.homepage.repository.AppointmentRepositoryProvider
import com.example.closenest.features.homepage.repository.MemoryRepositoryProvider
import com.example.closenest.features.notifications.repository.NotificationRepositoryProvider
import com.example.closenest.features.relationships.model.RelationshipProfile
import com.example.closenest.features.relationships.repository.RelationshipRepositoryProvider
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
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
    openAppointmentId: String? = null,
    onAppointmentOpened: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appointmentRepository = remember { AppointmentRepositoryProvider.repository }
    val memoryRepository = remember { MemoryRepositoryProvider.repository }
    val relationshipRepository = remember { RelationshipRepositoryProvider.repository }
    var appointmentList by remember { mutableStateOf<List<AppointmentItem>>(emptyList()) }
    var showAppointmentsDialog by rememberSaveable { mutableStateOf(false) }
    var expandedAppointments by rememberSaveable { mutableStateOf(false) }
    var memories by remember { mutableStateOf<List<MemoryItem>>(emptyList()) }
    var relationshipProfiles by remember { mutableStateOf<List<RelationshipProfile>>(emptyList()) }
    var selectedMemory by remember { mutableStateOf<MemoryItem?>(null) }
    var selectedAppointment by remember { mutableStateOf<AppointmentItem?>(null) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        appointmentList = runCatching {
            appointmentRepository.getUpcomingAppointments()
        }.getOrDefault(emptyList())

        memories = runCatching {
            memoryRepository.getMemories()
        }.getOrDefault(emptyList())

        relationshipProfiles = runCatching {
            relationshipRepository.observeRelationships().first()
        }.getOrDefault(emptyList())
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    val latestAppointments = runCatching {
                        appointmentRepository.getUpcomingAppointments()
                    }.getOrDefault(emptyList())
                    appointmentList = latestAppointments

                    memories = runCatching {
                        memoryRepository.getMemories()
                    }.getOrDefault(emptyList())

                    relationshipProfiles = runCatching {
                        relationshipRepository.observeRelationships().first()
                    }.getOrDefault(emptyList())
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(openAppointmentId, appointmentList) {
        val appointmentId = openAppointmentId ?: return@LaunchedEffect
        val appointment = appointmentList.firstOrNull { it.id == appointmentId }
            ?: return@LaunchedEffect
        selectedAppointment = appointment
        onAppointmentOpened()
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

        // Collapsed header
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expandedAppointments = !expandedAppointments }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val label = buildAnnotatedString {
                    if (appointmentList.isEmpty()) {
                        withStyle(SpanStyle(color = Color(0xFF9E9E9E))) {
                            append("Bạn chưa có cuộc hẹn nào")
                        }
                    } else {
                        withStyle(SpanStyle(color = Color(0xFF9E9E9E))) {
                            append("Bạn đang có ")
                        }
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF8B5E34),
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append("${appointmentList.size}")
                        }
                        withStyle(SpanStyle(color = Color(0xFF9E9E9E))) {
                            append(" cuộc hẹn")
                        }
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 22.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (expandedAppointments) "▲" else "▼",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 14.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // Expanded detail cards - horizontal swipeable pager
        AnimatedVisibility(
            visible = expandedAppointments && appointmentList.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val appointmentPagerState = rememberPagerState(
                initialPage = 0,
                pageCount = { appointmentList.size }
            )

            Column {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 12.dp)
                    ) {
                        HorizontalPager(
                            state = appointmentPagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) { page ->
                            val appointment = appointmentList[page]
                            AppointmentDetailRow(
                                appointment = appointment,
                                relationshipProfiles = relationshipProfiles,
                                onNavigate = { lat, lng ->
                                    val uri = Uri.parse("google.navigation:q=$lat,$lng")
                                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                        setPackage("com.google.android.apps.maps")
                                    }
                                    if (intent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(intent)
                                    } else {
                                        val webUri = Uri.parse(
                                            "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng"
                                        )
                                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                    }
                                },
                                onCancelAppointment = { appointmentId ->
                                    coroutineScope.launch {
                                        runCatching {
                                            appointmentRepository.deleteAppointment(appointmentId)
                                        }.onSuccess {
                                            AppointmentReminderScheduler.cancelAppointmentReminders(
                                                context = context,
                                                appointmentId = appointmentId
                                            )
                                            runCatching {
                                                NotificationRepositoryProvider.repository
                                                    .deleteAppointmentReminderNotifications(appointmentId)
                                            }

                                            appointmentList = runCatching {
                                                appointmentRepository.getUpcomingAppointments()
                                            }.getOrDefault(emptyList())

                                            if (appointmentList.isEmpty()) {
                                                expandedAppointments = false
                                            }

                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.appointment_cancel_success),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }.onFailure {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.appointment_cancel_error),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(appointmentList.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(if (appointmentPagerState.currentPage == index) 10.dp else 8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (appointmentPagerState.currentPage == index) {
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
            subtitleLabel = "V?i",
            location = memory.location,
            note = memory.note,
            photoUri = memory.photoUri,
            onDismiss = { selectedMemory = null }
        )
    }

    selectedAppointment?.let { appointment ->
        AppointmentDetailDialog(
            appointment = appointment,
            relationshipProfiles = relationshipProfiles,
            onDismiss = { selectedAppointment = null },
            onNavigate = { lat, lng ->
                val uri = Uri.parse("google.navigation:q=$lat,$lng")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                }
            },
            onCancelAppointment = { appointmentId ->
                coroutineScope.launch {
                    runCatching {
                        appointmentRepository.deleteAppointment(appointmentId)
                    }.onSuccess {
                        AppointmentReminderScheduler.cancelAppointmentReminders(
                            context = context,
                            appointmentId = appointmentId
                        )
                        runCatching {
                            NotificationRepositoryProvider.repository
                                .deleteAppointmentReminderNotifications(appointmentId)
                        }
                        appointmentList = runCatching {
                            appointmentRepository.getUpcomingAppointments()
                        }.getOrDefault(emptyList())
                        selectedAppointment = null
                        Toast.makeText(context, context.getString(R.string.appointment_cancel_success), Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Toast.makeText(context, context.getString(R.string.appointment_cancel_error), Toast.LENGTH_SHORT).show()
                    }
                }
            }
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
                    val dt1 = formatAppointmentDateTime(appointment.appointmentDateMillis)
                    Text(
                        text = dt1.timeLine,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF757575)
                    )
                    Text(
                        text = dt1.dateLine,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
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

            if (!appointment.note.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Notes,
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = appointment.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF616161)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppointmentDetailRow(
    appointment: AppointmentItem,
    relationshipProfiles: List<RelationshipProfile>,
    onNavigate: (lat: Double, lng: Double) -> Unit,
    onCancelAppointment: (appointmentId: String) -> Unit
) {
    var contactAction by remember { mutableStateOf<ContactAction?>(null) }
    var showCancelConfirm by remember { mutableStateOf(false) }
    var showAllParticipants by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val participants = appointment.participantContactNames.zip(appointment.participantContactIds)
    val participantSummary = when (participants.size) {
        0 -> ""
        1 -> participants.first().first
        else -> "${participants.first().first} + ${participants.size - 1} người khác"
    }

    fun performContactAction(action: ContactAction, personIndex: Int) {
        val (name, contactId) = participants[personIndex]
        val profile = relationshipProfiles.find { it.id == contactId }
        when (action) {
            ContactAction.Call -> {
                val phone = profile?.phoneNumber?.takeIf { it.isNotBlank() }
                if (phone != null) {
                    context.startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$phone") })
                } else {
                    Toast.makeText(context, context.getString(R.string.appointment_contact_no_phone, name), Toast.LENGTH_SHORT).show()
                }
            }
            ContactAction.Email -> {
                val email = profile?.email?.takeIf { it.isNotBlank() }
                if (email != null) {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "Không tìm thấy ứng dụng email.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.appointment_contact_no_email, name), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header: icon + date
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
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
            val dt2 = formatAppointmentDateTime(appointment.appointmentDateMillis)
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = dt2.timeLine,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dt2.dateLine,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Participants
        if (participantSummary.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = participants.size > 1) { showAllParticipants = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.People,
                    contentDescription = null,
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = participantSummary,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (participants.size > 1) Color(0xFF8B5E34) else Color(0xFF616161),
                    fontWeight = if (participants.size > 1) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Location
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Place,
                contentDescription = null,
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = appointment.location,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF616161)
            )
        }

        // Note
        appointment.note?.takeIf { it.isNotBlank() }?.let { noteText ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Notes,
                    contentDescription = null,
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = noteText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF616161)
                )
            }
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (participants.size == 1) {
                        performContactAction(ContactAction.Call, 0)
                    } else {
                        contactAction = ContactAction.Call
                    }
                },
                modifier = Modifier.heightIn(min = 32.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Call,
                    contentDescription = "Gọi điện",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Button(
                onClick = {
                    if (participants.size == 1) {
                        performContactAction(ContactAction.Email, 0)
                    } else {
                        contactAction = ContactAction.Email
                    }
                },
                modifier = Modifier.heightIn(min = 32.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = "Gửi email",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Button(
                onClick = {
                    onNavigate(appointment.locationLatitude, appointment.locationLongitude)
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 32.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Directions,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text("Chỉ đường", fontSize = 12.sp)
            }

            Button(
                onClick = { showCancelConfirm = true },
                modifier = Modifier.heightIn(min = 32.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text("Xóa", fontSize = 12.sp)
            }
        }
    }

    // Contact bottom sheet
    contactAction?.let { action ->
        ContactBottomSheet(
            appointment = appointment,
            relationshipProfiles = relationshipProfiles,
            action = action,
            onPersonSelected = { index ->
                performContactAction(action, index)
                contactAction = null
            },
            onDismiss = { contactAction = null }
        )
    }

    // Cancel confirmation
    if (showCancelConfirm) {
        CancelConfirmDialog(
            appointmentName = appointment.name,
            onConfirm = {
                showCancelConfirm = false
                onCancelAppointment(appointment.id)
            },
            onDismiss = { showCancelConfirm = false }
        )
    }

    // All participants dialog
    if (showAllParticipants) {
        AllParticipantsDialog(
            participants = participants,
            onDismiss = { showAllParticipants = false }
        )
    }
}

@Composable
private fun AllParticipantsDialog(
    participants: List<Pair<String, String>>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Danh sách người tham gia",
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
                participants.forEach { (name, _) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.People,
                            contentDescription = null,
                            tint = Color(0xFF757575),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF616161)
                        )
                    }
                }
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

    val activeAppointments = appointments

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
private val appointmentTimeFormatter = SimpleDateFormat("HH:mm", Locale("vi"))

private data class AppointmentDateTime(val timeLine: String, val dateLine: String)

private fun formatAppointmentDateTime(dateMillis: Long): AppointmentDateTime {
    val date = Date(dateMillis)
    val calendar = java.util.Calendar.getInstance().apply { time = date }
    val isAllDay = calendar.get(java.util.Calendar.HOUR_OF_DAY) == 0 &&
            calendar.get(java.util.Calendar.MINUTE) == 0
    val datePart = appointmentDateFormatter.format(date)
    return if (isAllDay) {
        AppointmentDateTime("Cả ngày", datePart)
    } else {
        AppointmentDateTime(appointmentTimeFormatter.format(date), datePart)
    }
}

@Composable
private fun AppointmentDetailDialog(
    appointment: AppointmentItem,
    relationshipProfiles: List<RelationshipProfile>,
    onDismiss: () -> Unit,
    onNavigate: (lat: Double, lng: Double) -> Unit,
    onCancelAppointment: (appointmentId: String) -> Unit
) {
    var contactAction by remember { mutableStateOf<ContactAction?>(null) }
    var showCancelConfirm by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val participants = appointment.participantContactNames.zip(appointment.participantContactIds)
    val participantSummary = when (participants.size) {
        0 -> ""
        1 -> participants.first().first
        else -> "${participants.first().first} + ${participants.size - 1} người khác"
    }

    fun performContactAction(action: ContactAction, personIndex: Int) {
        val (name, contactId) = participants[personIndex]
        val profile = relationshipProfiles.find { it.id == contactId }
        when (action) {
            ContactAction.Call -> {
                val phone = profile?.phoneNumber?.takeIf { it.isNotBlank() }
                if (phone != null) {
                    context.startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$phone") })
                } else {
                    Toast.makeText(context, context.getString(R.string.appointment_contact_no_phone, name), Toast.LENGTH_SHORT).show()
                }
            }
            ContactAction.Email -> {
                val email = profile?.email?.takeIf { it.isNotBlank() }
                if (email != null) {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "Không tìm thấy ứng dụng email.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.appointment_contact_no_email, name), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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
                        text = "Bạn có hẹn với",
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

                if (participantSummary.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.People,
                            contentDescription = null,
                            tint = Color(0xFF9E9E9E),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = participantSummary,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (participants.size > 1) Color(0xFF8B5E34) else Color(0xFF616161),
                            fontWeight = if (participants.size > 1) FontWeight.Medium else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val dt3 = formatAppointmentDateTime(appointment.appointmentDateMillis)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Event,
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${dt3.timeLine}, ${dt3.dateLine}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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

                appointment.note?.takeIf { it.isNotBlank() }?.let { noteText ->
                    DetailRow(label = "Nội dung", value = noteText)
                }

                HorizontalDivider(color = Color(0xFFE0E0E0))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            if (participants.size == 1) {
                                performContactAction(ContactAction.Call, 0)
                            } else {
                                contactAction = ContactAction.Call
                            }
                        },
                        modifier = Modifier.heightIn(min = 32.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Call,
                            contentDescription = "Gọi điện",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (participants.size == 1) {
                                performContactAction(ContactAction.Email, 0)
                            } else {
                                contactAction = ContactAction.Email
                            }
                        },
                        modifier = Modifier.heightIn(min = 32.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = "Gửi email",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Button(
                        onClick = {
                            onNavigate(appointment.locationLatitude, appointment.locationLongitude)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 32.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Directions,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Chỉ đường", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { showCancelConfirm = true },
                        modifier = Modifier.heightIn(min = 32.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Xóa", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    contactAction?.let { action ->
        ContactBottomSheet(
            appointment = appointment,
            relationshipProfiles = relationshipProfiles,
            action = action,
            onPersonSelected = { index ->
                performContactAction(action, index)
                contactAction = null
            },
            onDismiss = { contactAction = null }
        )
    }

    if (showCancelConfirm) {
        CancelConfirmDialog(
            appointmentName = appointment.name,
            onConfirm = {
                showCancelConfirm = false
                onCancelAppointment(appointment.id)
            },
            onDismiss = { showCancelConfirm = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactBottomSheet(
    appointment: AppointmentItem,
    relationshipProfiles: List<RelationshipProfile>,
    action: ContactAction,
    onPersonSelected: (index: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val participants = appointment.participantContactNames.zip(appointment.participantContactIds)
    val label = when (action) {
        ContactAction.Call -> "Gọi điện cho"
        ContactAction.Email -> "Gửi email cho"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            participants.forEachIndexed { index, (name, _) ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F6FA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPersonSelected(index) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.People,
                                contentDescription = null,
                                tint = Color(0xFF757575),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = if (action == ContactAction.Call) Icons.Outlined.Call else Icons.Outlined.Email,
                            contentDescription = null,
                            tint = Color(0xFF757575),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private enum class ContactAction { Call, Email }

@Composable
private fun CancelConfirmDialog(
    appointmentName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Xác nhận hủy cuộc hẹn",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Bạn chắc chắn muốn hủy cuộc hẹn \"$appointmentName\" không?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF616161)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Giữ lại")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        )
                    ) {
                        Text("Hủy cuộc hẹn")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailDialog(
    title: String,
    itemTitle: String,
    subtitle: String,
    subtitleLabel: String,
    location: String?,
    note: String?,
    photoUri: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hasPhoto = !photoUri.isNullOrBlank()
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(photoUri)
            .crossfade(true)
            .size(720)
            .build()
    )
    val isPhotoLoading = hasPhoto && (
        painter.state is AsyncImagePainter.State.Loading ||
            painter.state is AsyncImagePainter.State.Empty
    )

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

                if (isPhotoLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    if (hasPhoto && painter.state is AsyncImagePainter.State.Success) {
                        androidx.compose.foundation.Image(
                            painter = painter,
                            contentDescription = "Ảnh",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = itemTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DetailRow(label = subtitleLabel, value = subtitle)

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
