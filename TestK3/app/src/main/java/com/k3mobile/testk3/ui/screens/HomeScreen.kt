package com.k3mobile.testk3.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.layout.*
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
 * Home screen — the main entry point of the application.
 *
 * Keyboard shortcuts:
 * - ENTER: start a game
 * - S: open settings
 * - M: cycle through screen modes (on / dim / black / off)
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
    val context = LocalContext.current
    val isTtsReady by model.isTtsReady.collectAsState()
    var hasSpokenWelcome by remember { mutableStateOf(false) }
    val welcomeTts = stringResource(R.string.welcome_tts)
    val screenMode by model.screenMode.collectAsState()

    LaunchedEffect(isTtsReady) {
        if (isTtsReady && !hasSpokenWelcome) {
            hasSpokenWelcome = true
            model.speak(welcomeTts)
            if (model.savedScreenMode != 0) {
                model.speakQueued(context.getString(R.string.home_controls_hint))
            }
        }
    }

    LaunchedEffect(Unit) {
        for (event in model.keyChannel) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_ENTER -> { model.stopSpeaking(); model.sound.playNavigation(); onPartiePersonnalisee() }
                KeyEvent.KEYCODE_S     -> { model.stopSpeaking(); model.sound.playNavigation(); onSettings() }
                KeyEvent.KEYCODE_M     -> {
                    val newMode = (model.savedScreenMode + 1) % 4
                    model.savedScreenMode = newMode
                    val modeNames = listOf(
                        context.getString(R.string.screen_mode_on),
                        context.getString(R.string.screen_mode_dim),
                        context.getString(R.string.screen_mode_black),
                        context.getString(R.string.screen_mode_off)
                    )
                    model.speak(modeNames[newMode])
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onPartiePersonnalisee,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
            ) {
                Text(stringResource(R.string.start_game), color = MaterialTheme.colorScheme.background, modifier = Modifier.padding(vertical = 4.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onSettings,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
            ) {
                Text(stringResource(R.string.settings), modifier = Modifier.padding(vertical = 4.dp))
            }
        }

        val modeLabels = listOf(
            stringResource(R.string.screen_mode_on),
            stringResource(R.string.screen_mode_dim),
            stringResource(R.string.screen_mode_black),
            stringResource(R.string.screen_mode_off)
        )
        Text(
            text = modeLabels[screenMode],
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}