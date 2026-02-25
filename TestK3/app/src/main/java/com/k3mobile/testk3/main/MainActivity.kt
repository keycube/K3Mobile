package com.k3mobile.testk3.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.k3mobile.testk3.ui.MainViewModel
import com.k3mobile.testk3.ui.screens.AddTextScreen
import com.k3mobile.testk3.ui.screens.StatsScreen
import com.k3mobile.testk3.ui.screens.CategorySelectionScreen
import com.k3mobile.testk3.ui.screens.TypingScreen

/**
 * MainActivity
 *
 * Point d'entrée de l'application.
 * Son seul rôle est de configurer le thème Material et le graphe de navigation.
 * Toute la logique UI est déléguée aux écrans dans ui/screens/.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {

                    val navController = rememberNavController()
                    // Le ViewModel est créé ici pour être partagé entre tous les écrans
                    val viewModel: MainViewModel = viewModel()

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        // Écran principal : sélection de catégorie et liste des textes
                        composable("home") {
                            CategorySelectionScreen(
                                model = viewModel,
                                onNavigateToTyping = { textId ->
                                    navController.navigate("typing/$textId")
                                },
                                onNavigateToStats = {
                                    navController.navigate("stats")
                                },
                                onNavigateToAddText = {
                                    navController.navigate("add_text")
                                }
                            )
                        }

                        // Écran de saisie : exercice de frappe sur un texte donné
                        composable("typing/{textId}") { backStackEntry ->
                            val textId = backStackEntry.arguments?.getString("textId")?.toLongOrNull() ?: return@composable
                            TypingScreen(
                                textId = textId,
                                model = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // Écran de statistiques : historique des sessions
                        composable("stats") {
                            StatsScreen(
                                model = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // Écran d'ajout : création d'un texte personnalisé
                        composable("add_text") {
                            AddTextScreen(
                                model = viewModel,
                                onSaved = { navController.popBackStack() },
                                onCancel = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
