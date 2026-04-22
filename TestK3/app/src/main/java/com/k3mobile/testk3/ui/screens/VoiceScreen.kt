package com.k3mobile.testk3.ui.screens

import android.speech.tts.Voice
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k3mobile.testk3.R
import com.k3mobile.testk3.ui.MainViewModel

/**
 * TTS voice selection screen.
 *
 * Displays all offline voices available for the current language as
 * radio-button cards. Tapping a card immediately selects and persists
 * the voice (no "Accept" button needed). Each card also has a preview
 * button (▶) that speaks a sample sentence with that voice without
 * changing the selection.
 *
 * Voice names and details are cached via `remember` to avoid
 * recomputing string parsing on every scroll frame.
 *
 * @param model Shared [MainViewModel].
 * @param onBack Callback to navigate back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(model: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val voices by model.availableVoices.collectAsState()
    val selectedVoice by model.selectedVoice.collectAsState()
    var previewingVoiceName by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        K3TopBar(onBack = onBack)

        if (voices.isEmpty()) {
            // Empty state when no voices are installed for the current language
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(32.dp)) {
                    Text("\uD83D\uDD07", fontSize = 40.sp)
                    Text(stringResource(R.string.no_voice_found), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.no_voice_hint), fontSize = 13.sp, color = Color.Gray, lineHeight = 20.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(stringResource(R.string.voices_title), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    val s = if (voices.size > 1) "s" else ""
                    Text(stringResource(R.string.voices_count, voices.size, s), fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                }
                items(voices, key = { it.name }) { voice ->
                    val isSelected = voice.name == selectedVoice?.name
                    val displayName = remember(voice.name) { formatVoiceName(voice.name) }
                    val displayDetails = remember(voice) { formatVoiceDetails(context, voice) }

                    Card(
                        onClick = { model.selectVoice(voice) },
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
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Radio button indicator
                            Box(modifier = Modifier.size(20.dp).padding(2.dp), contentAlignment = Alignment.Center) {
                                if (isSelected) {
                                    Surface(shape = MaterialTheme.shapes.extraLarge, color = Color.Black, modifier = Modifier.size(16.dp)) {}
                                } else {
                                    Surface(shape = MaterialTheme.shapes.extraLarge, color = Color.Transparent,
                                        border = BorderStroke(1.5.dp, Color.LightGray), modifier = Modifier.size(16.dp)) {}
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(displayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 15.sp)
                                Text(displayDetails, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            // Preview button — speaks a sample without changing selection
                            TextButton(
                                onClick = { previewingVoiceName = voice.name; model.previewVoice(voice) },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (previewingVoiceName == voice.name) Color(0xFF4CAF50) else Color.Black
                                )
                            ) {
                                Text(if (previewingVoiceName == voice.name) "\u25B6 ..." else "\u25B6", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Extracts a human-readable name from a raw TTS voice identifier.
 *
 * Removes language codes and platform-specific tokens, capitalizes
 * remaining parts, and appends "(network)" or "(local)" if applicable.
 *
 * @param rawName The raw voice name (e.g. "fr-fr-x-frd-local").
 * @return A cleaned display name (e.g. "Frd (local)").
 */
private fun formatVoiceName(rawName: String): String {
    val parts = rawName.lowercase().split("-", "_")
    val cleaned = parts.filterNot { it in listOf("fr", "en", "es", "frd", "x", "local", "network") }
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }.ifBlank { rawName }
    val suffix = when {
        rawName.contains("network", ignoreCase = true) -> " (network)"
        rawName.contains("local", ignoreCase = true)   -> " (local)"
        else -> ""
    }
    return cleaned + suffix
}

/**
 * Builds a detail string showing voice quality and region.
 *
 * @param context Context for accessing string resources.
 * @param voice The TTS voice to describe.
 * @return A string like "High quality · France".
 */
private fun formatVoiceDetails(context: android.content.Context, voice: Voice): String {
    val quality = when (voice.quality) {
        Voice.QUALITY_VERY_HIGH -> context.getString(R.string.quality_very_high)
        Voice.QUALITY_HIGH      -> context.getString(R.string.quality_high)
        Voice.QUALITY_NORMAL    -> context.getString(R.string.quality_normal)
        Voice.QUALITY_LOW       -> context.getString(R.string.quality_low)
        else                    -> context.getString(R.string.quality_unknown)
    }
    val region = voice.locale.displayCountry.ifBlank { voice.locale.displayLanguage }
    return "$quality \u00B7 $region"
}