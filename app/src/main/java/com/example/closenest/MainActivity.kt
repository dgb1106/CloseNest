package com.example.closenest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.closenest.ui.CloseNestApp
import com.example.closenest.ui.theme.AppTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseAuth.getInstance().signOut()
        enableEdgeToEdge()
        setContent {
            CloseNestRoot()
        }
    }
}

@Composable
private fun CloseNestRoot() {
    AppTheme(dynamicColor = false) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
            CloseNestApp()
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CloseNestPreview() {
    CloseNestRoot()
}
