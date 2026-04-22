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

/**
 * Home screen — the main entry point of the application.
 *
 * Displays the app name, a "Start" button, and a settings icon.
 *
 * Keyboard shortcuts:
 * - ENTER: start a game
 * - S: open settings
 *
 * On first display (when TTS is ready), speaks a welcome message.
 *
 * @param model Shared [MainViewModel].
 * @param onPartiePersonnalisee Callback to navigate to game setup.
 * @param onSettings Callback to navigate to settings.
 */
@Composable
fun HomeScreen(
    model: MainViewModel,
    onPartiePersonnalisee: () -> Unit,
    onSettings: () -> Unit = {}
) {
    val context        = LocalContext.current
    val isTtsReady     by model.isTtsReady.collectAsState()
    var hasSpokenWelcome by remember { mutableStateOf(false) }
    val welcomeTts = stringResource(R.string.welcome_tts)

    // Speak welcome message once when TTS becomes ready
    LaunchedEffect(isTtsReady) {
        if (isTtsReady && !hasSpokenWelcome) {
            hasSpokenWelcome = true
            model.speak(welcomeTts)
            if (model.savedScreenMode != 0) {
                model.speakQueued(context.getString(R.string.home_controls_hint))
            }
        }
    }

    // Physical keyboard navigation
    LaunchedEffect(Unit) {
        for (event in model.keyChannel) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_ENTER -> { model.stopSpeaking(); model.sound.playNavigation(); onPartiePersonnalisee() }
                KeyEvent.KEYCODE_S     -> { model.stopSpeaking(); model.sound.playNavigation(); onSettings() }
                KeyEvent.KEYCODE_M     -> {
                    val newMode = (model.savedScreenMode + 1) % 3
                    model.savedScreenMode = newMode
                    val modeNames = listOf(
                        context.getString(R.string.screen_mode_on),
                        context.getString(R.string.screen_mode_black),
                        context.getString(R.string.screen_mode_off)
                    )
                    model.speak(modeNames[newMode])
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
                Text(text = stringResource(R.string.app_name), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(64.dp))
            Button(
                onClick = onPartiePersonnalisee, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
            ) { Text(stringResource(R.string.start_game), color = MaterialTheme.colorScheme.background, modifier = Modifier.padding(vertical = 4.dp)) }
        }
    }
}