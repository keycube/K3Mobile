package com.k3mobile.testk3.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    var customTextCount by remember { mutableStateOf(-1) }

    // Rafraîchit le nombre de textes perso à chaque fois que l'écran revient au premier plan
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                model.loadTextsByCategory("textes personnalisées")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Met à jour customTextCount dès que model.texts change
    val texts by model.texts.collectAsState()
    LaunchedEffect(texts) {
        if (customTextCount != -1 || texts.isNotEmpty()) {
            customTextCount = texts.size
        }
    }

    // Chargement initial
    LaunchedEffect(Unit) {
        model.loadTextsByCategory("textes personnalisées")
        kotlinx.coroutines.delay(300)
        customTextCount = model.texts.value.size
    }

    // Si "textes personnalisées" était sélectionné mais n'a plus de textes,
    // on revient automatiquement sur "phrases"
    LaunchedEffect(customTextCount) {
        if (customTextCount == 0 && categoryIndex == 2) {
            categoryIndex = 0
            model.savedCategoryIndex = 0
        }
    }
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
                        if (idx == 2 && customTextCount == 0) {
                            model.speak(context.getString(R.string.tts_no_text))
                        } else {
                            categoryIndex = idx
                            model.speak(context.getString(R.string.tts_selected, categoryLabels[idx]))
                            model.speakQueued(ttsChooseSpeed)
                            step = AudioStep.SPEED
                        }
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

    Column(modifier = Modifier.fillMaxSize()) {
        K3TopBar(onBack = { model.stopSpeaking(); onAnnuler() })

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(stringResource(R.string.setup_game), fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
            Spacer(modifier = Modifier.height(32.dp))
            Text(stringResource(R.string.text_type), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = categoryLabels[categoryIndex], onValueChange = {}, readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = Color.White
                ) {
                    categoryLabels.forEachIndexed { idx, label ->
                        val isCustomEmpty = idx == 2 && customTextCount == 0
                        DropdownMenuItem(
                            text = { Text(label, color = if (isCustomEmpty) Color.LightGray else Color.Black) },
                            onClick = { if (!isCustomEmpty) { categoryIndex = idx; expanded = false } },
                            enabled = !isCustomEmpty
                        )
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
            Button(
                onClick = {
                    model.savedCategoryIndex = categoryIndex; model.savedSpeedIndex = speedIndex
                    onConfirmer(categoryDb[categoryIndex], speedValues[speedIndex])
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
            ) { Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.background, modifier = Modifier.padding(vertical = 4.dp)) }
        }
    }
}

enum class AudioStep { CATEGORY, SPEED, CONFIRM }