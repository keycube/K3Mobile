package com.k3mobile.testk3.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k3mobile.testk3.R
import com.k3mobile.testk3.main.K3AccessibilityService
import com.k3mobile.testk3.ui.MainViewModel

/**
 * Screen mode selection screen.
 *
 * Allows the user to choose between four display modes:
 * - Screen on: normal display, no TTS or sounds
 * - Low brightness: TTS active, screen dimmed to minimum
 * - Black screen: TTS active, fully black overlay
 * - Screen off: TTS active, screen locked, requires accessibility service
 *
 * Selection is saved immediately on tap (no Accept button needed).
 *
 * @param model Shared [MainViewModel].
 * @param onBack Callback to navigate back.
 * @param onHome Callback to navigate directly to home (used when activating black screen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenModeScreen(model: MainViewModel, onBack: () -> Unit, onHome: () -> Unit = onBack) {
    val context = LocalContext.current
    val screenMode by model.screenMode.collectAsState()
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    val modeLabels = listOf(
        stringResource(R.string.screen_mode_on),
        stringResource(R.string.screen_mode_dim),
        stringResource(R.string.screen_mode_black),
        stringResource(R.string.screen_mode_off)
    )
    val modeDescs = listOf(
        stringResource(R.string.screen_mode_on_desc),
        stringResource(R.string.screen_mode_dim_desc),
        stringResource(R.string.screen_mode_black_desc),
        stringResource(R.string.screen_mode_off_desc)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        K3TopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.screen_mode_title), fontSize = 28.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            Text(stringResource(R.string.screen_mode_subtitle), fontSize = 13.sp, color = Color.Gray,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                modeLabels.forEachIndexed { idx, label ->
                    val isSelected = screenMode == idx
                    Card(
                        onClick = {
                            val previousMode = model.savedScreenMode
                            model.savedScreenMode = idx
                            if (idx == 3 && !isAccessibilityServiceEnabled(context)) {
                                showAccessibilityDialog = true
                            }
                            if (idx == 2 && previousMode != 2) {
                                model.speak(context.getString(R.string.black_screen_activated))
                                onHome()
                            }
                        },
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
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
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
                            Column {
                                Text(label, fontSize = 15.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                Text(modeDescs[idx], fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Accessibility service dialog
    if (showAccessibilityDialog) {
        AlertDialog(
            onDismissRequest = { showAccessibilityDialog = false },
            title = { Text(stringResource(R.string.permission_required), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.permission_description), textAlign = TextAlign.Center, fontSize = 14.sp)
                    Text(stringResource(R.string.permission_steps), fontSize = 13.sp, lineHeight = 20.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccessibilityDialog = false
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
                ) { Text(stringResource(R.string.open_settings), color = MaterialTheme.colorScheme.background) }
            },
        )
    }
}

/**
 * Checks whether [K3AccessibilityService] is currently enabled in system settings.
 */
private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val expectedComponent = android.content.ComponentName(
        context, K3AccessibilityService::class.java
    ).flattenToString()
    val enabledServices = android.provider.Settings.Secure.getString(
        context.contentResolver,
        android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.split(':').any { it.equals(expectedComponent, ignoreCase = true) }
}