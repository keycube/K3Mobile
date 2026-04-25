package com.k3mobile.testk3.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k3mobile.testk3.R

/**
 * Settings hub screen listing all configurable options.
 *
 * @param onLangue Navigate to language selection.
 * @param onSon Navigate to sound settings.
 * @param onVoix Navigate to voice selection.
 * @param onScreenMode Navigate to screen mode selection.
 * @param onTextesPersonnalises Navigate to custom texts list.
 * @param onStatistiques Navigate to statistics screen.
 * @param onAbout Navigate to about screen.
 * @param onQuitter Navigate back to home.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLangue: () -> Unit = {},
    onSon: () -> Unit = {},
    onVoix: () -> Unit = {},
    onScreenMode: () -> Unit = {},
    onTextesPersonnalises: () -> Unit,
    onStatistiques: () -> Unit,
    onAbout: () -> Unit = {},
    onQuitter: () -> Unit
) {
    val items = listOf(
        stringResource(R.string.menu_language) to onLangue,
        stringResource(R.string.menu_sound) to onSon,
        stringResource(R.string.menu_voice) to onVoix,
        stringResource(R.string.menu_screen_mode) to onScreenMode,
        stringResource(R.string.menu_custom_texts) to onTextesPersonnalises,
        stringResource(R.string.menu_stats) to onStatistiques,
        stringResource(R.string.menu_about) to onAbout
    )

    Column(modifier = Modifier.fillMaxSize()) {
        K3TopBar(onBack = onQuitter)

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.settings_title), fontSize = 28.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp))

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { (label, action) -> SettingsItem(label = label, onClick = action) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsItem(label: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 15.sp)
            Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
        }
    }
}