package com.k3mobile.testk3.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.k3mobile.testk3.ui.MainViewModel

/**
 * StatsScreen
 *
 * Affiche l'historique de toutes les sessions de frappe enregistrées.
 * Les sessions sont chargées depuis le ViewModel au montage de l'écran.
 *
 * @param model ViewModel partagé de l'application
 * @param onBack Appelé pour revenir à l'écran précédent
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(model: MainViewModel, onBack: () -> Unit) {

    val sessions by model.sessions.collectAsState()

    // Charge les statistiques à l'ouverture de l'écran
    LaunchedEffect(Unit) {
        model.loadStats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes Statistiques") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("< Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("Aucune session enregistrée pour le moment.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                items(sessions) { session ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "Vitesse: ${session.wpm.toInt()} WPM",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Précision: ${session.accuracy.toInt()}%",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text("ID Texte: ${session.textId}", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
