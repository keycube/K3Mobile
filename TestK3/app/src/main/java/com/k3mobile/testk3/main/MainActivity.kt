package com.k3mobile.testk3.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import com.k3mobile.testk3.ui.theme.TestK3Theme
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.k3mobile.testk3.ui.MainViewModel
import com.k3mobile.testk3.ui.screens.*
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val sharedViewModel: MainViewModel by viewModels()

    /**
     * Applique la langue sauvegardée AVANT que l'Activity ne soit créée.
     * Cela garantit que stringResource() et context.getString() retournent
     * les traductions dans la bonne langue dès le premier affichage.
     */
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("K3_prefs", Context.MODE_PRIVATE)
        val langCode = prefs.getString("language_code", "fr") ?: "fr"
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        setContent {
            TestK3Theme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {                    val navController = rememberNavController()
                    val viewModel = sharedViewModel

                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None }
                    ) {

                        composable("home") {
                            HomeScreen(
                                model = viewModel,
                                onPartiePersonnalisee = { viewModel.resetKeyChannel(); navController.navigate("custom_game") },
                                onSettings = { viewModel.resetKeyChannel(); navController.navigate("settings") }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                onLangue = { navController.navigate("language") },
                                onSon = { navController.navigate("sound") },
                                onVoix = { navController.navigate("voices") },
                                onTextesPersonnalises = { navController.navigate("text_list_readonly/textes personnalisées") },
                                onStatistiques = { navController.navigate("stats") },
                                onQuitter = { navController.popBackStack() }
                            )
                        }

                        composable("voices") {
                            VoiceScreen(model = viewModel, onBack = { navController.popBackStack() })
                        }

                        composable("sound") {
                            SoundScreen(model = viewModel, onBack = { navController.popBackStack() })
                        }

                        composable("language") {
                            LanguageScreen(model = viewModel, onBack = { navController.popBackStack() })
                        }

                        composable("custom_game") {
                            CustomGameScreen(
                                model = viewModel,
                                onConfirmer = { category, speed ->
                                    viewModel.setSpeechRate(speed)
                                    viewModel.resetKeyChannel()
                                    navController.navigate("text_list/$category")
                                },
                                onAnnuler = { viewModel.resetKeyChannel(); navController.popBackStack() },
                                onSettings = { navController.navigate("settings") }
                            )
                        }

                        composable("text_list/{category}") { backStackEntry ->
                            val category = backStackEntry.arguments?.getString("category") ?: "phrases"
                            TextListScreen(
                                category = category, model = viewModel,
                                onTextSelected = { textId ->
                                    viewModel.resetKeyChannel()
                                    navController.navigate("typing/$textId")
                                },
                                onBack = { viewModel.resetKeyChannel(); navController.popBackStack() },
                                readOnly = false
                            )
                        }

                        composable("text_list_readonly/{category}") { backStackEntry ->
                            val category = backStackEntry.arguments?.getString("category") ?: "textes personnalisées"
                            TextListScreen(
                                category = category, model = viewModel,
                                onBack = { navController.popBackStack() }, readOnly = true
                            )
                        }

                        composable("typing/{textId}") { backStackEntry ->
                            val textId = backStackEntry.arguments?.getString("textId")?.toLongOrNull()
                                ?: return@composable
                            val context = LocalContext.current

                            LaunchedEffect(Unit) {
                                ContextCompat.startForegroundService(
                                    context,
                                    Intent(context, TypingForegroundService::class.java).apply {
                                        putExtra(TypingForegroundService.EXTRA_STATUS, TypingForegroundService.STATUS_READY)
                                    }
                                )
                            }

                            TypingScreen(
                                textId = textId, model = viewModel,
                                onBack = {
                                    context.stopService(Intent(context, TypingForegroundService::class.java))
                                    viewModel.resetKeyChannel()
                                    navController.popBackStack()
                                },
                                onFinished = {
                                    context.stopService(Intent(context, TypingForegroundService::class.java))
                                    viewModel.resetKeyChannel()
                                    navController.navigate("home") { popUpTo("home") { inclusive = false } }
                                }
                            )
                        }

                        composable("stats") {
                            StatsScreen(model = viewModel, onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("GestureBackNavigation")
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.repeatCount > 0) return super.onKeyDown(keyCode, event)

        if (keyCode == KeyEvent.KEYCODE_BACK) return super.onKeyDown(keyCode, event)

        if (K3AppState.isServiceConnected) {
            // Le service d'accessibilité a DÉJÀ émis cet événement dans K3AppState.
            // On retourne true pour CONSOMMER la touche et empêcher MIUI/Android
            // de générer un second event (via interaction avec l'écran de verrouillage)
            // qui arriverait dans ce même onKeyDown et produirait un doublon dans le flux.
            return true
        }

        // Fallback : service inactif, on émet nous-mêmes
        if (!sharedViewModel.isInTypingMode) {
            sharedViewModel.emitKeyEvent(keyCode)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}