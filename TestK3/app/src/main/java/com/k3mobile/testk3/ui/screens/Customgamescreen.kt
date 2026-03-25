package com.k3mobile.testk3.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val categoryLabels = listOf("Phrases", "Histoires", "Textes personnalisés")
    val categoryDb     = listOf("phrases", "histoires", "textes personnalisées")
    val speedLabels    = listOf("lente", "normale", "rapide", "très rapide", "maximale")
    val speedValues    = listOf(1f, 1.5f, 2f, 2.5f, 3f)

    var categoryIndex by remember { mutableStateOf(0) }
    var speedIndex    by remember { mutableStateOf(1) }
    var step          by remember { mutableStateOf(AudioStep.CATEGORY) }

    val speed = speedValues[speedIndex]

    LaunchedEffect(Unit) {
        model.speak(
            "Choisissez le type de texte" +
                    "1 pour Phrases 2 pour Histoires 3 pour Textes personnalisés"
        )
    }

    LaunchedEffect("keys") {
        for (event in model.keyChannel) {

            val keyCode = event.keyCode
            when (step) {

                AudioStep.CATEGORY -> {
                    val idx = when (keyCode) {
                        KeyEvent.KEYCODE_1 -> 0
                        KeyEvent.KEYCODE_2 -> 1
                        KeyEvent.KEYCODE_3 -> 2
                        else -> -1
                    }
                    if (idx >= 0) {
                        categoryIndex = idx
                        model.speak("${categoryLabels[idx]} sélectionné")
                        model.speakQueued(
                            "Choisissez la vitesse audio" +
                                    "1 pour lente 2 normale 3 rapide 4 très rapide 5 maximale"
                        )
                        step = AudioStep.SPEED
                    } else if (keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_BACK) {
                        model.stopSpeaking()
                        onAnnuler()
                    }
                }

                AudioStep.SPEED -> {
                    val idx = when (keyCode) {
                        KeyEvent.KEYCODE_1 -> 0
                        KeyEvent.KEYCODE_2 -> 1
                        KeyEvent.KEYCODE_3 -> 2
                        KeyEvent.KEYCODE_4 -> 3
                        KeyEvent.KEYCODE_5 -> 4
                        else -> -1
                    }
                    if (idx >= 0) {
                        speedIndex = idx
                        model.speak("Vitesse ${speedLabels[idx]} sélectionnée")
                        model.speakQueued(
                            "Récapitulatif " +
                                    "Type de texte ${categoryLabels[categoryIndex]}" +
                                    "Vitesse ${speedLabels[idx]}" +
                                    "Appuyez sur Entrée pour confirmer" +
                                    "ou sur Retour arrière pour recommencer"
                        )
                        step = AudioStep.CONFIRM
                    } else if (keyCode == KeyEvent.KEYCODE_DEL) {
                        model.speak(
                            "Choisissez le type de texte" +
                                    "1 pour Phrases 2 pour Histoires 3 pour Textes personnalisés"
                        )
                        step = AudioStep.CATEGORY
                    }
                }

                AudioStep.CONFIRM -> {
                    when (keyCode) {
                        KeyEvent.KEYCODE_ENTER -> {
                            model.stopSpeaking()
                            model.sound.playNavigation()
                            model.savedCategoryIndex = categoryIndex
                            model.savedSpeedIndex = speedIndex
                            onConfirmer(categoryDb[categoryIndex], speedValues[speedIndex])
                        }
                        KeyEvent.KEYCODE_DEL -> {
                            model.speak(
                                "Choisissez le type de texte " +
                                        "1 pour Phrases 2 pour Histoires 3 pour Textes personnalisés"
                            )
                            step = AudioStep.CATEGORY
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = onSettings,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Paramètres")
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "⌨", fontSize = 28.sp)
                Text(text = "K3AudioType", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Paramétrer une partie !",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 30.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            StepIndicator(currentStep = step)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Type de texte :",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            )

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = categoryLabels[categoryIndex],
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categoryLabels.forEachIndexed { idx, label ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { categoryIndex = idx; expanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Vitesse de l'audio :",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            )

            Slider(
                value = speed,
                onValueChange = { newVal ->
                    speedIndex = speedValues.indexOfFirst { it >= newVal }.coerceAtLeast(0)
                },
                valueRange = 1f..3f,
                steps = 3,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.onBackground,
                    activeTrackColor = MaterialTheme.colorScheme.onBackground
                )
            )

            Text(
                text = "${speedLabels[speedIndex]} — ${(speed * 10).roundToInt() / 10f} mots/sec",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onAnnuler) { Text("Annuler") }
                Button(
                    onClick = { onConfirmer(categoryDb[categoryIndex], speedValues[speedIndex]) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground
                    )
                ) {
                    Text("Confirmer", color = MaterialTheme.colorScheme.background)
                }
            }
        }
    }
}

enum class AudioStep { CATEGORY, SPEED, CONFIRM }

@Composable
private fun StepIndicator(currentStep: AudioStep) {
    val steps = listOf("Type de texte", "Vitesse", "Confirmation")
    val currentIdx = currentStep.ordinal

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { idx, label ->
            val active = idx == currentIdx
            val done   = idx < currentIdx
            Surface(
                shape = MaterialTheme.shapes.small,
                color = when {
                    active -> MaterialTheme.colorScheme.onBackground
                    done   -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    else   -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${idx + 1}. $label",
                    fontSize = 11.sp,
                    color = when {
                        active || done -> MaterialTheme.colorScheme.background
                        else           -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}