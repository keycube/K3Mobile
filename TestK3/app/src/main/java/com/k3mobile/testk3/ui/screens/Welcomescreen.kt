package com.k3mobile.testk3.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * WelcomeScreen
 *
 * Premier écran affiché au lancement de l'application.
 * Présente le logo et le nom de l'app, avec un bouton pour continuer.
 *
 * @param onCommencer Appelé quand l'utilisateur appuie sur "Commencer"
 */
@Composable
fun WelcomeScreen(onCommencer: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Bienvenue sur",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            // Logo + nom de l'app
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Icône placeholder — remplace par ton vrai logo avec Image(painterResource(...))
                Text(text = "⌨", fontSize = 32.sp)
                Text(
                    text = "K3AudioType",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onCommencer,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground
                )
            ) {
                Text("Commencer", color = MaterialTheme.colorScheme.background)
            }
        }
    }
}
