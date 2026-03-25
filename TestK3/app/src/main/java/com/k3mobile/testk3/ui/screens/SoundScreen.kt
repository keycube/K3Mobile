package com.k3mobile.testk3.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k3mobile.testk3.ui.MainViewModel
import kotlin.math.roundToInt

/**
 * SoundScreen
 *
 * Permet de régler le volume de l'audio (TTS) et le volume des effets sonores.
 * Les valeurs sont sauvegardées dans les SharedPreferences via le ViewModel.
 */
@Composable
fun SoundScreen(
    model: MainViewModel,
    onBack: () -> Unit
) {
    var ttsVolume     by remember { mutableStateOf(model.savedTtsVolume) }
    var effectsVolume by remember { mutableStateOf(model.savedEffectsVolume) }

    Box(modifier = Modifier.fillMaxSize()) {

        // Icône paramètres en haut à droite
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(22.dp),
            tint = Color.Black
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- Logo ---
            Spacer(modifier = Modifier.height(40.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "⌨", fontSize = 24.sp)
                Text(text = "K3AudioType", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(40.dp))

            // --- Titre ---
            Text(
                text = "Son :",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
            )

            // --- Volume de l'audio (TTS) ---
            Text(
                text = "Volume de l'audio :",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            )

            Slider(
                value = ttsVolume.toFloat(),
                onValueChange = { ttsVolume = it.roundToInt() },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.onBackground,
                    activeTrackColor = MaterialTheme.colorScheme.onBackground
                )
            )

            Text(
                text = "$ttsVolume%",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 24.dp)
            )

            // --- Volume des effets ---
            Text(
                text = "Volume des effets :",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            )

            Slider(
                value = effectsVolume.toFloat(),
                onValueChange = { effectsVolume = it.roundToInt() },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.onBackground,
                    activeTrackColor = MaterialTheme.colorScheme.onBackground
                )
            )

            Text(
                text = "$effectsVolume%",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.weight(1f))

            // --- Boutons ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("Retour", color = Color.Black, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(24.dp))
                Button(
                    onClick = {
                        model.savedTtsVolume = ttsVolume
                        model.savedEffectsVolume = effectsVolume
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground
                    )
                ) {
                    Text("Accepter", color = MaterialTheme.colorScheme.background)
                }
            }
        }
    }
}