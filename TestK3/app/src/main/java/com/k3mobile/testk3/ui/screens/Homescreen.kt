package com.k3mobile.testk3.ui.screens

import android.content.Intent
import android.provider.Settings
import android.view.KeyEvent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.k3mobile.testk3.R
import com.k3mobile.testk3.main.K3AccessibilityService
import com.k3mobile.testk3.main.K3AppState
import com.k3mobile.testk3.ui.MainViewModel

/**
 * Checks whether [K3AccessibilityService] is currently enabled in system settings.
 *
 * Uses [Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES] as the primary check,
 * with [K3AppState.isServiceConnected] as a runtime fallback for devices
 * (e.g. MIUI) where the secure setting may not update immediately.
 *
 * @param context Application or activity context.
 * @return `true` if the service is enabled.
 */
private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val expectedComponent = android.content.ComponentName(
        context, K3AccessibilityService::class.java
    ).flattenToString()

    val enabledServices = android.provider.Settings.Secure.getString(
        context.contentResolver,
        android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    return enabledServices.split(':').any { it.equals(expectedComponent, ignoreCase = true) }
}

/**
 * Home screen — the main entry point of the application.
 *
 * Displays the app name, a "Start" button, and a settings icon.
 * Shows a blocking dialog if [K3AccessibilityService] is not enabled,
 * guiding the user to the system accessibility settings.
 *
 * Keyboard shortcuts:
 * - ENTER: start a game
 * - S: open settings
 *
 * On first display (when TTS is ready), speaks a welcome message.
 *
 * @param model Shared [MainViewModel].
 * @param onPartiePersonnalisee Callback to navigate to game setup.
 * @param onSettings Callback to navigate to settings.
 */
@Composable
fun HomeScreen(
    model: MainViewModel,
    onPartiePersonnalisee: () -> Unit,
    onSettings: () -> Unit = {}
) {
    val context        = LocalContext.current
    val isTtsReady     by model.isTtsReady.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var serviceEnabled   by remember { mutableStateOf(isAccessibilityServiceEnabled(context) || K3AppState.isServiceConnected) }
    var hasSpokenWelcome by remember { mutableStateOf(false) }
    val welcomeTts = stringResource(R.string.welcome_tts)

    // Re-check accessibility service status when the app resumes
    // (user may have just enabled it in system settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { serviceEnabled = isAccessibilityServiceEnabled(context) || K3AppState.isServiceConnected
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Speak welcome message once when TTS becomes ready
    LaunchedEffect(isTtsReady) {
        if (isTtsReady && !hasSpokenWelcome) { hasSpokenWelcome = true; model.speak(welcomeTts) }
    }

    // Physical keyboard navigation
    LaunchedEffect(Unit) {
        for (event in model.keyChannel) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_ENTER -> { model.stopSpeaking(); model.sound.playNavigation(); onPartiePersonnalisee() }
                KeyEvent.KEYCODE_S     -> { model.stopSpeaking(); model.sound.playNavigation(); onSettings() }
            }
        }
    }

    // Accessibility service permission dialog
    if (!serviceEnabled) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.permission_required), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.permission_description), textAlign = TextAlign.Center, fontSize = 14.sp)
                    Text(stringResource(R.string.permission_steps), fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
                ) { Text(stringResource(R.string.open_settings), color = MaterialTheme.colorScheme.background) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.app_name), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(64.dp))
            Button(
                onClick = onPartiePersonnalisee, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
            ) { Text(stringResource(R.string.start_game), color = MaterialTheme.colorScheme.background, modifier = Modifier.padding(vertical = 4.dp)) }
        }
    }
}