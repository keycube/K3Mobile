package com.k3mobile.testk3.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * CustomGameScreen
 *
 * Écran de paramétrage d'une partie personnalisée.
 * L'utilisateur choisit le type de texte et la vitesse de lecture audio.
 *
 * @param onConfirmer Appelé avec la catégorie choisie et la vitesse (mots/sec) pour lancer la partie
 * @param onAnnuler Appelé si l'utilisateur annule et revient en arrière
 * @param onSettings Appelé pour ouvrir les paramètres de l'application
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomGameScreen(
    onConfirmer: (category: String, speed: Float) -> Unit,
    onAnnuler: () -> Unit,
    onSettings: () -> Unit = {}
) {
    // Catégories disponibles en base de données
    val categories = listOf("Phrases", "Histoires", "Textes Personnalisés")

    // État du dropdown
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(categories[0]) }

    // Vitesse audio : 1 à 3 mots/sec, valeur par défaut 1
    var speed by remember { mutableStateOf(1f) }

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
                Text(text = "⌨", fontSize = 28.sp)
                Text(
                    text = "K3AudioType",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Paramétrer une partie !",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 30.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- Dropdown : Type de texte ---
            Text(
                text = "Type de texte :",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                selectedCategory = category
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Slider : Vitesse de l'audio ---
            Text(
                text = "Vitesse de l'audio :",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )

            Slider(
                value = speed,
                onValueChange = { speed = it },
                valueRange = 1f..3f,
                steps = 3, // Valeurs : 1, 1.5, 2, 2.5, 3
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.onBackground,
                    activeTrackColor = MaterialTheme.colorScheme.onBackground
                )
            )

            Text(
                text = "${(speed * 10).roundToInt() / 10f} Mots/sec",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // --- Boutons d'action ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onAnnuler) {
                    Text("Annuler")
                }

                Button(
                    onClick = {
                        // Convertit le nom affiché vers la valeur en base de données
                        val categoryDb = when (selectedCategory) {
                            "Phrases" -> "phrases"
                            "Histoires" -> "histoires"
                            "Textes Personnalisés" -> "textes personnalisées"
                            else -> "phrases"
                        }
                        onConfirmer(categoryDb, speed)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground
                    )
                ) {
                    Text(
                        "Confirmer",
                        color = MaterialTheme.colorScheme.background
                    )
                }
            }
        }
    }
}
