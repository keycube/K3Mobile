package com.k3mobile.testk3.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * SettingsScreen
 *
 * Écran des paramètres de l'application.
 * Affiche les sections navigables et un bouton pour quitter l'écran.
 *
 * @param onLangue Ouvre les paramètres de langue
 * @param onSon Ouvre les paramètres de son
 * @param onVoix Ouvre les paramètres de voix
 * @param onTextesPersonnalises Ouvre la liste des textes personnalisés
 * @param onStatistiques Ouvre l'écran des statistiques
 * @param onQuitter Ferme l'écran des paramètres
 */
@Composable
fun SettingsScreen(
    onLangue: () -> Unit = {},
    onSon: () -> Unit = {},
    onVoix: () -> Unit = {},
    onTextesPersonnalises: () -> Unit,
    onStatistiques: () -> Unit,
    onQuitter: () -> Unit
) {
    val items = listOf(
        "Langue" to onLangue,
        "Son" to onSon,
        "Voix" to onVoix,
        "Textes personnalisés" to onTextesPersonnalises,
        "Statistiques" to onStatistiques
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // Icône paramètres en haut à droite (décorative, indique l'écran actif)
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
                Text(
                    text = "K3AudioType",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // --- Titre ---
            Text(
                text = "Paramètres :",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            // --- Liste des entrées ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { (label, action) ->
                    SettingsItem(label = label, onClick = action)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- Bouton Quitter ---
            TextButton(
                onClick = onQuitter,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Quitter",
                    color = Color.Black,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * SettingsItem
 *
 * Ligne cliquable d'un paramètre avec son label et une flèche à droite.
 */
@Composable
fun SettingsItem(label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 15.sp)
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}