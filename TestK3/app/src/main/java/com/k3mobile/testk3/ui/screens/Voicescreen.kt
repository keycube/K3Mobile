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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k3mobile.testk3.R
import com.k3mobile.testk3.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(model: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val voices by model.availableVoices.collectAsState()
    val selectedVoice by model.selectedVoice.collectAsState()
    var pendingVoice by remember { mutableStateOf(selectedVoice) }
    var previewingVoiceName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedVoice) {
        if (pendingVoice == null) pendingVoice = selectedVoice
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 8.dp, end = 24.dp, bottom = 8.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.Black)
            }
            Row(modifier = Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.app_name), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        HorizontalDivider(color = Color.Black, thickness = 1.dp)

        if (voices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(32.dp)) {
                    Text("\uD83D\uDD07", fontSize = 40.sp)
                    Text(stringResource(R.string.no_voice_found), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.no_voice_hint), fontSize = 13.sp, color = Color.Gray, lineHeight = 20.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(stringResource(R.string.voices_title), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    val s = if (voices.size > 1) "s" else ""
                    Text(stringResource(R.string.voices_count, voices.size, s), fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                }
                items(voices, key = { it.name }) { voice ->
                    val isSelected = voice.name == pendingVoice?.name
                    val displayName = remember(voice.name) { formatVoiceName(voice.name) }
                    val displayDetails = remember(voice) { formatVoiceDetails(context, voice) }

                    Card(
                        onClick = { pendingVoice = voice },
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

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        pendingVoice?.let { model.selectVoice(it) }
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(stringResource(R.string.accept), color = MaterialTheme.colorScheme.background)
                }
            }
        }
    }
}

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