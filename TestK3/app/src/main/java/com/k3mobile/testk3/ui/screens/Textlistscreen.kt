package com.k3mobile.testk3.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.k3mobile.testk3.ui.MainViewModel

/**
 * TextListScreen
 *
 * Affiche la liste des textes disponibles pour une catégorie donnée.
 * L'utilisateur choisit le texte qu'il veut pratiquer.
 *
 * @param category Catégorie à afficher (transmise depuis CustomGameScreen)
 * @param model ViewModel partagé de l'application
 * @param onTextSelected Appelé avec l'ID du texte choisi pour lancer la partie
 * @param onBack Appelé pour revenir à l'écran précédent
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextListScreen(
    category: String,
    model: MainViewModel,
    onTextSelected: (Long) -> Unit,
    onBack: () -> Unit
) {
    val texts by model.texts.collectAsState()

    // Charge les textes de la catégorie à l'ouverture de l'écran
    LaunchedEffect(category) {
        model.loadTextsByCategory(category)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category.replaceFirstChar { it.uppercase() }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->

        if (texts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucun texte disponible dans cette catégorie.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(texts) { textEntity ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onTextSelected(textEntity.idText) },
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
