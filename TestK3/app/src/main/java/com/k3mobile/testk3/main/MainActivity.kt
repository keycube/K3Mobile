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
import com.k3mobile.testk3.ui.screens.WelcomeScreen
import com.k3mobile.testk3.ui.screens.HomeScreen
import com.k3mobile.testk3.ui.screens.CustomGameScreen
import com.k3mobile.testk3.ui.screens.TextListScreen
import com.k3mobile.testk3.ui.screens.AddTextScreen
import com.k3mobile.testk3.ui.screens.StatsScreen
import com.k3mobile.testk3.ui.screens.TypingScreen

/**
 * MainActivity
 *
 * Flux de navigation :
 *   welcome → home → custom_game → text_list/{category} → typing/{textId}
 *                  → stats
 *                  → add_text
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {

                    val navController = rememberNavController()
                    val viewModel: MainViewModel = viewModel()

                    NavHost(
                        navController = navController,
                        startDestination = "welcome"
                    ) {

                        // 1. Écran de bienvenue
                        composable("welcome") {
                            WelcomeScreen(
                                onCommencer = { navController.navigate("home") }
                            )
                        }

                        // 2. Choix du type de partie
                        composable("home") {
                            HomeScreen(
                                onPartieRapide = {
                                    // TODO : à implémenter
                                },
                                onPartiePersonnalisee = {
                                    navController.navigate("custom_game")
                                }
                            )
                        }

                        // 3. Paramétrage de la partie
                        composable("custom_game") {
                            CustomGameScreen(
                                onConfirmer = { category, _ ->
                                    // Navigue vers la liste des textes de la catégorie choisie
                                    navController.navigate("text_list/$category")
                                },
                                onAnnuler = { navController.popBackStack() }
                            )
                        }

                        // 4. Liste des textes de la catégorie choisie
                        composable("text_list/{category}") { backStackEntry ->
                            val category = backStackEntry.arguments?.getString("category") ?: "phrases"
                            TextListScreen(
                                category = category,
                                model = viewModel,
                                onTextSelected = { textId ->
                                    navController.navigate("typing/$textId")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // 5. Exercice de frappe
                        composable("typing/{textId}") { backStackEntry ->
                            val textId = backStackEntry.arguments?.getString("textId")?.toLongOrNull()
                                ?: return@composable
                            TypingScreen(
                                textId = textId,
                                model = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // 6. Statistiques
                        composable("stats") {
                            StatsScreen(
                                model = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // 7. Ajout d'un texte personnalisé
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
