package com.k3mobile.testk3.ui.screens

import android.speech.tts.Voice
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k3mobile.testk3.ui.MainViewModel

/**
 * VoiceScreen
 *
 * Liste toutes les voix françaises disponibles sur l'appareil.
 * L'utilisateur peut écouter un aperçu de chaque voix et en sélectionner une.
 *
 * @param model ViewModel partagé
 * @param onBack Retour à l'écran précédent
 */
@Composable
fun VoiceScreen(
    model: MainViewModel,
    onBack: () -> Unit
) {
    val voices by model.availableVoices.collectAsState()
    val selectedVoice by model.selectedVoice.collectAsState()
    var previewingVoiceName by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {

        // --- Header ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 8.dp, end = 24.dp, bottom = 8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = Color.Black)
            }
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("⌨", fontSize = 20.sp)
                Text("K3AudioType", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(color = Color.Black, thickness = 1.dp)

        if (voices.isEmpty()) {
            // Aucune voix trouvée sur l'appareil
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("🔇", fontSize = 40.sp)
                    Text(
                        "Aucune voix française trouvée.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Vérifiez que des voix françaises sont installées dans les paramètres TTS de votre appareil.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Titre + compteur
                item {
                    Text("Voix", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${voices.size} voix disponible${if (voices.size > 1) "s" else ""}",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }

                // Voix active mise en avant
                selectedVoice?.let { active ->
                    item {
                        ActiveVoiceBanner(
                            voice = active,
                            onPreview = {
                                previewingVoiceName = active.name
                                model.previewVoice(active)
                            }
                        )
                    }
                    item {
                        Text(
                            "Changer de voix",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Liste de toutes les voix
                items(voices) { voice ->
                    val isSelected = voice.name == selectedVoice?.name
                    VoiceItem(
                        voice = voice,
                        isSelected = isSelected,
                        isPreviewing = previewingVoiceName == voice.name,
                        onSelect = {
                            model.selectVoice(voice)
                        },
                        onPreview = {
                            previewingVoiceName = voice.name
                            model.previewVoice(voice)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Bandeau de la voix actuellement active, affiché en noir en haut de la liste.
 */
@Composable
private fun ActiveVoiceBanner(voice: Voice, onPreview: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Voix active",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    formatVoiceName(voice.name),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    formatVoiceDetails(voice),
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            // Bouton aperçu
            OutlinedButton(
                onClick = onPreview,
                border = BorderStroke(1.dp, Color.White),
                colors = ButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = Color.Gray
                )
            ) {
                Text("▶ Écouter", fontSize = 13.sp)
            }
        }
    }
}

/**
 * Carte d'une voix dans la liste avec bouton aperçu et sélection.
 */
@Composable
private fun VoiceItem(
    voice: Voice,
    isSelected: Boolean,
    isPreviewing: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFF5F5F5) else Color.White
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color.Black else Color(0xFFE0E0E0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicateur de sélection
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = Color.Black,
                        modifier = Modifier.size(16.dp)
                    ) {}
                } else {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = Color.Transparent,
                        border = BorderStroke(1.5.dp, Color.LightGray),
                        modifier = Modifier.size(16.dp)
                    ) {}
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Infos voix
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatVoiceName(voice.name),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 15.sp
                )
                Text(
                    text = formatVoiceDetails(voice),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Bouton aperçu
            TextButton(
                onClick = onPreview,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isPreviewing) Color(0xFF4CAF50) else Color.Black
                )
            ) {
                Text(
                    if (isPreviewing) "▶ ..." else "▶",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bouton sélectionner (masqué si déjà sélectionné)
            if (!isSelected) {
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = onSelect,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Choisir", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

/**
 * Formate le nom brut d'une voix TTS en quelque chose de lisible.
 * Ex: "fr-fr-x-frd-local" → "Frd (locale)"
 */
private fun formatVoiceName(rawName: String): String {
    val parts = rawName.lowercase().split("-", "_")
    // Retire les préfixes de langue courants
    val cleaned = parts
        .filterNot { it in listOf("fr", "frd", "x", "local", "network") }
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        .ifBlank { rawName }

    val suffix = when {
        rawName.contains("network", ignoreCase = true) -> " (réseau)"
        rawName.contains("local", ignoreCase = true) -> " (locale)"
        else -> ""
    }
    return cleaned + suffix
}

/**
 * Construit une description courte de la voix : qualité et région.
 */
private fun formatVoiceDetails(voice: Voice): String {
    val quality = when (voice.quality) {
        Voice.QUALITY_VERY_HIGH -> "Très haute qualité"
        Voice.QUALITY_HIGH -> "Haute qualité"
        Voice.QUALITY_NORMAL -> "Qualité normale"
        Voice.QUALITY_LOW -> "Qualité basse"
        else -> "Qualité inconnue"
    }
    val region = voice.locale.displayCountry.ifBlank { voice.locale.displayLanguage }
    return "$quality · $region"
}