package com.k3mobile.testk3.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
 * CategorySelectionScreen
 *
 * Écran principal de l'application.
 * Affiche les catégories disponibles et la liste des textes de la catégorie sélectionnée.
 * La navigation vers les autres écrans est déléguée via des lambdas.
 *
 * @param model ViewModel partagé de l'application
 * @param onNavigateToTyping Appelé avec l'ID du texte sélectionné pour démarrer un exercice
 * @param onNavigateToStats Appelé pour ouvrir l'écran des statistiques
 * @param onNavigateToAddText Appelé pour ouvrir l'écran d'ajout de texte
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectionScreen(
    model: MainViewModel,
    onNavigateToTyping: (Long) -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToAddText: () -> Unit
) {
    val categories = listOf("phrases", "histoires", "textes personnalisées")
    val availableTexts by model.texts.collectAsState()
    var currentCategory by remember { mutableStateOf("phrases") }

    // Recharge les textes à chaque changement de catégorie
    LaunchedEffect(currentCategory) {
        model.loadTextsByCategory(currentCategory)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("K3 Typing", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToStats) {
                        Text("📊", fontSize = 20.sp)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddText) {
                Text("+", fontSize = 24.sp)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Mode d'entraînement",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sélecteur de catégorie
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = (currentCategory == category),
                        onClick = { currentCategory = category },
                        label = { Text(category.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Liste des textes disponibles
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(availableTexts) { textEntity ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigateToTyping(textEntity.idText) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = textEntity.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = textEntity.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    color = Color.Gray
                                )
                            }
                            Text("➔", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
