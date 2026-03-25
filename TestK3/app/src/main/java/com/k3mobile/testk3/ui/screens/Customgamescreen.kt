package com.k3mobile.testk3.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k3mobile.testk3.R
import com.k3mobile.testk3.ui.MainViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomGameScreen(
    model: MainViewModel,
    onConfirmer: (category: String, speed: Float) -> Unit,
    onAnnuler: () -> Unit,
    onSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val categoryLabels = listOf(
        stringResource(R.string.cat_phrases),
        stringResource(R.string.cat_stories),
        stringResource(R.string.cat_custom)
    )
    val categoryDb = listOf("phrases", "histoires", "textes personnalisées")
    val speedLabels = listOf(
        stringResource(R.string.speed_slow),
        stringResource(R.string.speed_normal),
        stringResource(R.string.speed_fast),
        stringResource(R.string.speed_very_fast),
        stringResource(R.string.speed_max)
    )
    val speedValues = listOf(1f, 1.5f, 2f, 2.5f, 3f)

    var categoryIndex by remember { mutableStateOf(model.savedCategoryIndex) }
    var speedIndex    by remember { mutableStateOf(model.savedSpeedIndex) }
    var step          by remember { mutableStateOf(AudioStep.CATEGORY) }
    val speed = speedValues[speedIndex]

    val ttsChooseType  = stringResource(R.string.tts_choose_text_type)
    val ttsChooseSpeed = stringResource(R.string.tts_choose_speed)

    LaunchedEffect(Unit) { model.speak(ttsChooseType) }

    LaunchedEffect("keys") {
        for (event in model.keyChannel) {
            val keyCode = event.keyCode
            when (step) {
                AudioStep.CATEGORY -> {
                    val idx = when (keyCode) {
                        KeyEvent.KEYCODE_1 -> 0; KeyEvent.KEYCODE_2 -> 1; KeyEvent.KEYCODE_3 -> 2; else -> -1
                    }
                    if (idx >= 0) {
                        categoryIndex = idx
                        model.speak(context.getString(R.string.tts_selected, categoryLabels[idx]))
                        model.speakQueued(ttsChooseSpeed)
                        step = AudioStep.SPEED
                    } else if (keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_BACK) {
                        model.stopSpeaking(); onAnnuler()
                    }
                }
                AudioStep.SPEED -> {
                    val idx = when (keyCode) {
                        KeyEvent.KEYCODE_1 -> 0; KeyEvent.KEYCODE_2 -> 1; KeyEvent.KEYCODE_3 -> 2
                        KeyEvent.KEYCODE_4 -> 3; KeyEvent.KEYCODE_5 -> 4; else -> -1
                    }
                    if (idx >= 0) {
                        speedIndex = idx
                        model.speak(context.getString(R.string.tts_speed_selected, speedLabels[idx]))
                        model.speakQueued(context.getString(R.string.tts_recap, categoryLabels[categoryIndex], speedLabels[idx]))
                        step = AudioStep.CONFIRM
                    } else if (keyCode == KeyEvent.KEYCODE_DEL) {
                        model.speak(ttsChooseType); step = AudioStep.CATEGORY
                    }
                }
                AudioStep.CONFIRM -> {
                    when (keyCode) {
                        KeyEvent.KEYCODE_ENTER -> {
                            model.stopSpeaking(); model.sound.playNavigation()
                            model.savedCategoryIndex = categoryIndex; model.savedSpeedIndex = speedIndex
                            onConfirmer(categoryDb[categoryIndex], speedValues[speedIndex])
                        }
                        KeyEvent.KEYCODE_DEL -> { model.speak(ttsChooseType); step = AudioStep.CATEGORY }
                    }
                }
            }
        }
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
                Text(text = stringResource(R.string.app_name), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(stringResource(R.string.setup_game), fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
            Spacer(modifier = Modifier.height(32.dp))
            StepIndicator(currentStep = step)
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.text_type), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = categoryLabels[categoryIndex], onValueChange = {}, readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categoryLabels.forEachIndexed { idx, label ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { categoryIndex = idx; expanded = false })
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(stringResource(R.string.audio_speed), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))
            Slider(
                value = speed, onValueChange = { newVal -> speedIndex = speedValues.indexOfFirst { it >= newVal }.coerceAtLeast(0) },
                valueRange = 1f..3f, steps = 3, modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.onBackground, activeTrackColor = MaterialTheme.colorScheme.onBackground)
            )
            Text("${speedLabels[speedIndex]} — ${(speed * 10).roundToInt() / 10f} ${stringResource(R.string.words_per_sec)}",
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(48.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onAnnuler) { Text(stringResource(R.string.cancel)) }
                Button(
                    onClick = {
                        model.savedCategoryIndex = categoryIndex; model.savedSpeedIndex = speedIndex
                        onConfirmer(categoryDb[categoryIndex], speedValues[speedIndex])
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
                ) { Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.background) }
            }
        }
    }
}

enum class AudioStep { CATEGORY, SPEED, CONFIRM }

@Composable
private fun StepIndicator(currentStep: AudioStep) {
    val steps = listOf(stringResource(R.string.step_text_type), stringResource(R.string.step_speed), stringResource(R.string.step_confirm))
    val currentIdx = currentStep.ordinal
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        steps.forEachIndexed { idx, label ->
            val active = idx == currentIdx; val done = idx < currentIdx
            Surface(
                shape = MaterialTheme.shapes.small,
                color = when { active -> MaterialTheme.colorScheme.onBackground; done -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f); else -> MaterialTheme.colorScheme.surfaceVariant },
                modifier = Modifier.weight(1f)
            ) {
                Text("${idx + 1}. $label", fontSize = 11.sp,
                    color = when { active || done -> MaterialTheme.colorScheme.background; else -> MaterialTheme.colorScheme.onSurfaceVariant },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
            }
        }
    }
}