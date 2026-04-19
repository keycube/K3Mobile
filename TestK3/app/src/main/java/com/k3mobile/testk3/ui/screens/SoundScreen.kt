package com.k3mobile.testk3.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k3mobile.testk3.R
import com.k3mobile.testk3.main.K3AccessibilityService
import com.k3mobile.testk3.ui.MainViewModel
import kotlin.math.roundToInt

/**
 * Sound and accessibility settings screen.
 *
 * Allows the user to adjust TTS volume, effects volume, vibrations,
 * and choose between three screen modes:
 * - Screen on (no TTS/sounds)
 * - Black screen (TTS + sounds, brightness at minimum)
 * - Screen off (TTS + sounds, requires accessibility service)
 *
 * @param model Shared [MainViewModel].
 * @param onBack Callback to navigate back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundScreen(model: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var ttsVolume by remember { mutableStateOf(model.savedTtsVolume) }
    var effectsVolume by remember { mutableStateOf(model.savedEffectsVolume) }
    var vibrationEnabled by remember { mutableStateOf(model.savedVibrationEnabled) }
    var screenOnMode by remember { mutableStateOf(model.savedScreenMode) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        K3TopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
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

            // Screen mode selection (3 radio cards)
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.screen_mode_title), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))

            val screenModeLabels = listOf(
                stringResource(R.string.screen_mode_on),
                stringResource(R.string.screen_mode_black),
                stringResource(R.string.screen_mode_off)
            )
            val screenModeDescs = listOf(
                stringResource(R.string.screen_mode_on_desc),
                stringResource(R.string.screen_mode_black_desc),
                stringResource(R.string.screen_mode_off_desc)
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                screenModeLabels.forEachIndexed { idx, label ->
                    val isSelected = screenOnMode == idx
                    Card(
                        onClick = {
                            screenOnMode = idx
                            if (idx == 2 && !isAccessibilityServiceEnabled(context)) {
                                showAccessibilityDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFF5F5F5) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) Color.Black else Color(0xFFE0E0E0)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(18.dp).padding(2.dp), contentAlignment = Alignment.Center) {
                                if (isSelected) {
                                    Surface(shape = MaterialTheme.shapes.extraLarge, color = Color.Black, modifier = Modifier.size(14.dp)) {}
                                } else {
                                    Surface(shape = MaterialTheme.shapes.extraLarge, color = Color.Transparent,
                                        border = BorderStroke(1.5.dp, Color.LightGray), modifier = Modifier.size(14.dp)) {}
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(label, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                Text(screenModeDescs[idx], fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Accept button
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

    // Accessibility service dialog
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

/**
 * Checks whether [K3AccessibilityService] is currently enabled in system settings.
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