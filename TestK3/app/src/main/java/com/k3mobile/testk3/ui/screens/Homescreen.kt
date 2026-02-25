package com.k3mobile.testk3.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * HomeScreen
 *
 * Écran de choix du type de partie, affiché après le WelcomeScreen.
 * Propose une partie rapide (paramètres par défaut) ou une partie personnalisée.
 *
 * @param onPartieRapide Appelé pour lancer une partie avec les paramètres par défaut
 * @param onPartiePersonnalisee Appelé pour ouvrir l'écran de paramétrage
 * @param onSettings Appelé pour ouvrir les paramètres de l'application
 */
@Composable
fun HomeScreen(
    onPartieRapide: () -> Unit,
    onPartiePersonnalisee: () -> Unit,
    onSettings: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // Bouton paramètres en haut à droite
        IconButton(
            onClick = onSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Paramètres")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Logo + nom de l'app
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Icône placeholder — remplace par ton vrai logo avec Image(painterResource(...))
                Text(text = "⌨", fontSize = 32.sp)
                Text(
                    text = "K3AudioType",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            // Bouton principal : partie rapide
            Button(
                onClick = onPartieRapide,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground
                )
            ) {
                Text(
                    "Lancer une partie rapide",
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lien texte : partie personnalisée
            TextButton(onClick = onPartiePersonnalisee) {
                Text(
                    text = "Lancer une partie personnalisée",
                    color = Color.Gray,
                    textDecoration = TextDecoration.None
                )
            }
        }
    }
}
