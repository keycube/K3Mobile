package com.k3mobile.testk3.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k3mobile.testk3.R
import com.k3mobile.testk3.ui.MainViewModel
import kotlin.math.roundToInt
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.k3mobile.testk3.main.K3AccessibilityService
import com.k3mobile.testk3.main.K3AppState

/**
 * Sound and accessibility settings screen.
 *
 * Allows the user to adjust:
 * - **TTS volume** (0–100%) — controls how loud the text-to-speech output is.
 * - **Effects volume** (0–100%) — controls earcon sounds (validation, delete, etc.).
 * - **Vibrations** toggle — enables/disables haptic feedback on victory.
 * - **Screen-on mode** toggle — when enabled, disables all TTS, earcons, and the
 *   countdown timer for sighted users who don't need audio feedback.
 *
 * Changes are only persisted when the user taps "Accept".
 *
 * @param model Shared [MainViewModel].
 * @param onBack Callback to navigate back.
 */
@Composable
fun SoundScreen(model: MainViewModel, onBack: () -> Unit) {
    var ttsVolume     by remember { mutableStateOf(model.savedTtsVolume) }
    var effectsVolume by remember { mutableStateOf(model.savedEffectsVolume) }
    var vibrationEnabled by remember { mutableStateOf(model.savedVibrationEnabled) }
    var screenOnMode by remember { mutableStateOf(model.savedScreenMode) }
    val context = LocalContext.current
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        K3TopBar(onBack = onBack)

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.sound_title), fontSize = 28.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp))

            // TTS volume slider
            Text(stringResource(R.string.audio_volume), style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))
            Slider(value = ttsVolume.toFloat(), onValueChange = { ttsVolume = it.roundToInt() },
                valueRange = 0f..100f, modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.onBackground, activeTrackColor = MaterialTheme.colorScheme.onBackground))
            Text("$ttsVolume%", style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 24.dp))

            // Effects volume slider
            Text(stringResource(R.string.effects_volume), style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))
            Slider(value = effectsVolume.toFloat(), onValueChange = { effectsVolume = it.roundToInt() },
                valueRange = 0f..100f, modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.onBackground, activeTrackColor = MaterialTheme.colorScheme.onBackground))
            Text("$effectsVolume%", style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(24.dp))

            // Vibration toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.vibration_enabled), style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = vibrationEnabled,
                    onCheckedChange = { vibrationEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color.Black,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.LightGray
                    )
                )
            }

            // Screen-on mode toggle
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.screen_on_mode), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = screenOnMode,
                    onCheckedChange = {
                        screenOnMode = it
                        if (!it && !isAccessibilityServiceEnabled(context)) {
                            showAccessibilityDialog = true
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color.Black,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.LightGray
                    )
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Accept button — persists all changes
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    model.savedTtsVolume = ttsVolume; model.savedEffectsVolume = effectsVolume
                    model.savedVibrationEnabled = vibrationEnabled
                    model.sound.vibrationEnabled = vibrationEnabled
                    model.savedScreenMode = screenOnMode
                    onBack()
                },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
                ) { Text(stringResource(R.string.accept), color = MaterialTheme.colorScheme.background) }
            }
        }
    }

    if (showAccessibilityDialog) {
        AlertDialog(
            onDismissRequest = { showAccessibilityDialog = false },
            title = { Text(stringResource(R.string.permission_required), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.permission_description), textAlign = TextAlign.Center, fontSize = 14.sp)
                    Text(stringResource(R.string.permission_steps), fontSize = 13.sp, lineHeight = 20.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccessibilityDialog = false
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
                ) { Text(stringResource(R.string.open_settings), color = MaterialTheme.colorScheme.background) }
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityDialog = false }) {
                    Text(stringResource(R.string.continue_without), color = Color.Gray)
                }
            }
        )
    }
}

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