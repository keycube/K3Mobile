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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

/**
 * Main entry point of the K3AudioType application.
 *
 * Responsibilities:
 * - Applies the saved language locale before the Activity is created.
 * - Configures the window to remain visible on the lock screen.
 * - Sets up the Compose navigation graph with all application screens.
 * - Acts as a fallback keyboard event dispatcher when [K3AccessibilityService] is unavailable.
 */
class MainActivity : ComponentActivity() {

    private val sharedViewModel: MainViewModel by viewModels()

    /**
     * Overrides the base context to apply the user's saved language.
     *
     * Called before [onCreate], ensuring that [stringResource] and
     * [Context.getString] return translations in the correct language
     * from the very first frame.
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

        // Allow the app to be displayed on the lock screen
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
                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    val navController = rememberNavController()
                    val viewModel = sharedViewModel

                    // Observe screen mode for black screen overlay
                    val currentScreenMode by viewModel.screenMode.collectAsState()
                    LaunchedEffect(currentScreenMode) {
                        if (currentScreenMode == 1) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }

                    // Cross-fade transitions (100ms) prevent the "flash" of
                    // the new screen being constructed during navigation.
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        enterTransition = { fadeIn(animationSpec = androidx.compose.animation.core.tween(100)) },
                        exitTransition = { fadeOut(animationSpec = androidx.compose.animation.core.tween(100)) },
                        popEnterTransition = { fadeIn(animationSpec = androidx.compose.animation.core.tween(100)) },
                        popExitTransition = { fadeOut(animationSpec = androidx.compose.animation.core.tween(100)) }
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
                                onAbout = { navController.navigate("about") },
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

                        composable("about") {
                            AboutScreen(onBack = { navController.popBackStack() })
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

                            // Start the foreground service to keep the app alive
                            // when the screen is off during a typing session
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

                    // Full black overlay when black screen mode is active
                    if (currentScreenMode == 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        )
                    }
                }
            }
        }
    }

    /**
     * Fallback keyboard handler for when [K3AccessibilityService] is not active.
     *
     * When the accessibility service IS connected, it has already emitted the
     * key event via [K3AppState]. Returning `true` here consumes the duplicate
     * event that MIUI/Android generates through the lock screen interaction.
     *
     * When the service is NOT connected, this method emits the key event
     * directly — but only outside of typing mode, where the soft keyboard
     * should handle input instead.
     */
    @SuppressLint("GestureBackNavigation")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event)
        if (event.repeatCount > 0) return super.dispatchKeyEvent(event)
        if (event.keyCode == KeyEvent.KEYCODE_BACK) return super.dispatchKeyEvent(event)

        if (K3AppState.isServiceConnected) {
            return true
        }

        sharedViewModel.emitKeyEvent(event.keyCode, event.getUnicodeChar(event.metaState))
        return true
    }
}