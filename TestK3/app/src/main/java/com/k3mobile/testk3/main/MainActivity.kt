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
import com.k3mobile.testk3.ui.screens.HomeScreen
import com.k3mobile.testk3.ui.screens.CustomGameScreen
import com.k3mobile.testk3.ui.screens.TextListScreen
import com.k3mobile.testk3.ui.screens.SettingsScreen
import com.k3mobile.testk3.ui.screens.VoiceScreen
import com.k3mobile.testk3.ui.screens.StatsScreen
import com.k3mobile.testk3.ui.screens.TypingScreen

/**
 * MainActivity
 *
 * Flux de navigation :
 *   welcome → home → custom_game → text_list/{category} → typing/{textId}
 *                  → settings → stats
 *                             → text_list_readonly/textes personnalisées
 *                             → voices
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {

                    val navController = rememberNavController()
                    val viewModel: MainViewModel = viewModel()

                    NavHost(navController = navController, startDestination = "home") {

                        composable("home") {
                            HomeScreen(
                                onPartieRapide = { /* TODO */ },
                                onPartiePersonnalisee = { navController.navigate("custom_game") },
                                onSettings = { navController.navigate("settings") }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                onVoix = { navController.navigate("voices") },
                                onTextesPersonnalises = {
                                    navController.navigate("text_list_readonly/textes personnalisées")
                                },
                                onStatistiques = { navController.navigate("stats") },
                                onQuitter = { navController.popBackStack() }
                            )
                        }

                        // Sélection et aperçu des voix TTS
                        composable("voices") {
                            VoiceScreen(
                                model = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("custom_game") {
                            CustomGameScreen(
                                onConfirmer = { category, speed ->
                                    viewModel.setSpeechRate(speed)
                                    navController.navigate("text_list/$category")
                                },
                                onAnnuler = { navController.popBackStack() },
                                onSettings = { navController.navigate("settings") }
                            )
                        }

                        // Mode jeu
                        composable("text_list/{category}") { backStackEntry ->
                            val category = backStackEntry.arguments?.getString("category") ?: "phrases"
                            TextListScreen(
                                category = category,
                                model = viewModel,
                                onTextSelected = { textId -> navController.navigate("typing/$textId") },
                                onBack = { navController.popBackStack() },
                                readOnly = false
                            )
                        }

                        // Mode consultation / édition (depuis paramètres)
                        composable("text_list_readonly/{category}") { backStackEntry ->
                            val category = backStackEntry.arguments?.getString("category") ?: "textes personnalisées"
                            TextListScreen(
                                category = category,
                                model = viewModel,
                                onBack = { navController.popBackStack() },
                                readOnly = true
                            )
                        }

                        composable("typing/{textId}") { backStackEntry ->
                            val textId = backStackEntry.arguments?.getString("textId")?.toLongOrNull()
                                ?: return@composable
                            TypingScreen(
                                textId = textId,
                                model = viewModel,
                                onBack = { navController.popBackStack() },
                                onFinished = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = false }
                                    }
                                }
                            )
                        }

                        composable("stats") {
                            StatsScreen(
                                model = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}