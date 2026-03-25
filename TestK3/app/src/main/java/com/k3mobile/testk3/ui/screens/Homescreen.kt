package com.k3mobile.testk3.ui.screens

import android.content.Intent
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.k3mobile.testk3.R
import com.k3mobile.testk3.main.K3AccessibilityService
import com.k3mobile.testk3.ui.MainViewModel

private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val am = context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    return am.getEnabledAccessibilityServiceList(
        android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
    ).any { info ->
        info.resolveInfo.serviceInfo.packageName == context.packageName &&
                info.resolveInfo.serviceInfo.name == K3AccessibilityService::class.java.name
    }
}

@Composable
fun HomeScreen(
    model: MainViewModel,
    onPartiePersonnalisee: () -> Unit,
    onSettings: () -> Unit = {}
) {
    val context        = LocalContext.current
    val isTtsReady     by model.isTtsReady.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var serviceEnabled   by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var hasSpokenWelcome by remember { mutableStateOf(false) }
    val welcomeTts = stringResource(R.string.welcome_tts)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { serviceEnabled = isAccessibilityServiceEnabled(context) }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isTtsReady) {
        if (isTtsReady && !hasSpokenWelcome) { hasSpokenWelcome = true; model.speak(welcomeTts) }
    }

    LaunchedEffect(Unit) {
        for (event in model.keyChannel) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_ENTER -> { model.stopSpeaking(); model.sound.playNavigation(); onPartiePersonnalisee() }
                KeyEvent.KEYCODE_S     -> { model.stopSpeaking(); model.sound.playNavigation(); onSettings() }
            }
        }
    }

    if (!serviceEnabled) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.permission_required), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.permission_description), textAlign = TextAlign.Center, fontSize = 14.sp)
                    Text(stringResource(R.string.permission_steps), fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
                ) { Text(stringResource(R.string.open_settings), color = MaterialTheme.colorScheme.background) }
            }
        )
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