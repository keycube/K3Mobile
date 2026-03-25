package com.k3mobile.testk3.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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

/**
 * LanguageScreen
 *
 * Permet de choisir la langue de l'application (TTS + textes).
 * La sélection est sauvegardée et appliquée immédiatement au TTS.
 */
@Composable
fun LanguageScreen(
    model: MainViewModel,
    onBack: () -> Unit
) {
    val languages = listOf(
        "fr" to "Français",
        "en" to "Anglais",
        "es" to "Espagnol"
    )

    var selectedCode by remember { mutableStateOf(model.savedLanguage) }

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
                text = "Langues :",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            )

            // --- Dropdown label ---
            Text(
                text = "Langue",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            // --- Liste des langues ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                languages.forEach { (code, label) ->
                    val isSelected = selectedCode == code

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCode = code },
                        color = if (isSelected) Color(0xFFF0F0F0) else Color.White,
                        border = BorderStroke(
                            width = 0.5.dp,
                            color = Color(0xFFE0E0E0)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

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
                        model.setLanguage(selectedCode)
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