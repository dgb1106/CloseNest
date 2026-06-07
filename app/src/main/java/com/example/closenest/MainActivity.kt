package com.example.closenest

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.closenest.core.notification.AppointmentReminderAppointmentIdExtra
import com.example.closenest.core.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private val appointmentIdToOpen = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appointmentIdToOpen.value = intent.appointmentIdExtra()
        setContent {
            CloseNestRoot(
                openAppointmentId = appointmentIdToOpen.value,
                onAppointmentOpened = { appointmentIdToOpen.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        appointmentIdToOpen.value = intent.appointmentIdExtra()
    }
}

@Composable
private fun CloseNestRoot(
    openAppointmentId: String? = null,
    onAppointmentOpened: () -> Unit = {}
) {
    val permissionsToRequest = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // app continues regardless of grant status
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionsToRequest)
    }

    AppTheme(dynamicColor = false) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
            CloseNestApp(
                openAppointmentId = openAppointmentId,
                onAppointmentOpened = onAppointmentOpened
            )
        }
    }
}

private fun Intent.appointmentIdExtra(): String? {
    return getStringExtra(AppointmentReminderAppointmentIdExtra)?.takeIf { it.isNotBlank() }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CloseNestPreview() {
    CloseNestRoot()
}
