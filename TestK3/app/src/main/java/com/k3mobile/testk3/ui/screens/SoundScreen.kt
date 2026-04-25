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

/**
 * Sound settings screen.
 *
 * Allows the user to adjust TTS volume, effects volume, and vibrations.
 * Screen mode selection has been moved to [ScreenModeScreen].
 *
 * @param model Shared [MainViewModel].
 * @param onBack Callback to navigate back.
 */
@Composable
fun SoundScreen(model: MainViewModel, onBack: () -> Unit) {
    var ttsVolume by remember { mutableStateOf(model.savedTtsVolume) }
    var effectsVolume by remember { mutableStateOf(model.savedEffectsVolume) }
    var vibrationEnabled by remember { mutableStateOf(model.savedVibrationEnabled) }

    Column(modifier = Modifier.fillMaxSize()) {
        K3TopBar(onBack = onBack)

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.sound_title), fontSize = 28.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp))

            Text(stringResource(R.string.audio_volume), style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))
            Slider(value = ttsVolume.toFloat(), onValueChange = { ttsVolume = it.roundToInt() },
                valueRange = 0f..100f, modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.onBackground, activeTrackColor = MaterialTheme.colorScheme.onBackground))
            Text("$ttsVolume%", style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 24.dp))

            Text(stringResource(R.string.effects_volume), style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))
            Slider(value = effectsVolume.toFloat(), onValueChange = { effectsVolume = it.roundToInt() },
                valueRange = 0f..100f, modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.onBackground, activeTrackColor = MaterialTheme.colorScheme.onBackground))
            Text("$effectsVolume%", style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    model.savedTtsVolume = ttsVolume; model.savedEffectsVolume = effectsVolume
                    model.savedVibrationEnabled = vibrationEnabled
                    model.sound.vibrationEnabled = vibrationEnabled
                    onBack()
                },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
                ) { Text(stringResource(R.string.accept), color = MaterialTheme.colorScheme.background) }
            }
        }
    }
}