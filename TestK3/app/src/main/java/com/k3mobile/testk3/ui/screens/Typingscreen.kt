package com.k3mobile.testk3.ui.screens

import android.content.Intent
import android.view.KeyEvent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k3mobile.testk3.data.TextEntity
import com.k3mobile.testk3.main.TypingForegroundService
import com.k3mobile.testk3.ui.MainViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun TypingScreen(
    textId: Long,
    model: MainViewModel,
    onBack: () -> Unit,
    onFinished: () -> Unit
) {
    val texts by model.texts.collectAsState()
    val textEntity = texts.find { it.idText == textId }

    if (textEntity == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    TypingContent(textEntity = textEntity, model = model, onBack = onBack, onFinished = onFinished)
}

@Composable
private fun TypingContent(
    textEntity: TextEntity,
    model: MainViewModel,
    onBack: () -> Unit,
    onFinished: () -> Unit
) {
    val context = LocalContext.current

    val sentences = remember(textEntity.content) {
        textEntity.content
            .split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    var currentSentenceIndex by remember { mutableStateOf(0) }
    var userInput            by remember { mutableStateOf("") }
    var totalTypedChars      by remember { mutableStateOf(0) }
    var totalDistance        by remember { mutableStateOf(0) }
    var totalEvaluatedChars  by remember { mutableStateOf(0) }
    val startTime            = remember { System.currentTimeMillis() }
    val focusRequester       = remember { FocusRequester() }
    var hasStarted           by remember { mutableStateOf(false) }
    var isFinishing          by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        model.isInTypingMode = true
        onDispose { model.isInTypingMode = false }
    }

    fun String.normalize(): String = this
        .replace("\u2019", "'").replace("\u2018", "'")
        .replace("\u2026", "...").replace("œ", "oe").replace("Œ", "Oe")
        .replace("æ", "ae").replace("Æ", "Ae").replace("ß", "ss")
        .replace("\n", "").replace("\r", "").trim()

    val rawTarget          = sentences[currentSentenceIndex]
    val cleanTarget        = rawTarget.normalize()
    val cleanInput         = userInput.normalize()
    val isError            = userInput.isNotEmpty() && !cleanTarget.startsWith(cleanInput)
    val isFinishedSentence = cleanInput == cleanTarget

    fun goToNextSentence() {
        if (isFinishing) return
        val ci = userInput.normalize()
        val ct = sentences[currentSentenceIndex].normalize()

        totalTypedChars     += ci.length
        val distance         = calculateLevenshteinDistance(ci, ct)
        totalDistance       += distance
        totalEvaluatedChars += maxOf(ci.length, ct.length)

        if (currentSentenceIndex < sentences.lastIndex) {
            model.sound.playValidation()
            currentSentenceIndex++
            userInput = ""
        } else {
            isFinishing = true
            model.sound.playVictory()
            model.sound.vibrateVictory()

            val duration    = System.currentTimeMillis() - startTime
            val minutes     = duration / 60_000.0
            val wpm         = if (minutes > 0) (totalTypedChars / 5.0) / minutes else 0.0
            val accuracy    = if (totalEvaluatedChars > 0)
                ((totalEvaluatedChars - totalDistance).toDouble() / totalEvaluatedChars) * 100
            else 0.0

            model.saveSession(textEntity.idText, duration, wpm, accuracy)

            val wpmRounded   = wpm.roundToInt()
            val accRounded   = accuracy.roundToInt()
            val minPart      = (duration / 60_000).toInt()
            val secPart      = ((duration % 60_000) / 1_000).toInt()
            val durationText = if (minPart > 0)
                "$minPart minute${if (minPart > 1) "s" else ""} et $secPart secondes"
            else "$secPart secondes"

            model.speakThenDo(
                phrases = listOf(
                    "Bravo, exercice terminé.",
                    "Durée : $durationText.",
                    "Vitesse : $wpmRounded mots par minute.",
                    "Précision : $accRounded pourcent.",
                    "Retour au menu principal."
                ),
                onDone = { onFinished() }
            )
        }
    }

    // Compte à rebours
    LaunchedEffect(Unit) {
        context.startService(
            Intent(context, TypingForegroundService::class.java).apply {
                putExtra(TypingForegroundService.EXTRA_STATUS, TypingForegroundService.STATUS_READY)
            }
        )
        model.speak("Préparez-vous à écrire")
        delay(1_800)
        model.sound.playCountdownTick(); model.speakQueued("3"); delay(900)
        model.sound.playCountdownTick(); model.speakQueued("2"); delay(900)
        model.sound.playCountdownTick(); model.speakQueued("1"); delay(900)
        model.sound.playCountdownGo()

        context.startService(
            Intent(context, TypingForegroundService::class.java).apply {
                putExtra(TypingForegroundService.EXTRA_STATUS, TypingForegroundService.STATUS_TYPING)
            }
        )
        hasStarted = true
        focusRequester.requestFocus()
    }

    // Annonce de chaque nouvelle phrase
    LaunchedEffect(currentSentenceIndex, hasStarted) {
        if (hasStarted && currentSentenceIndex < sentences.size && !isFinishing) {
            model.speak(sentences[currentSentenceIndex])
        }
    }

    LaunchedEffect("typing_keys") {
        for (event in model.keyChannel) {
            if (!hasStarted || isFinishing) continue

            when {
                event.keyCode == KeyEvent.KEYCODE_ENTER -> {
                    goToNextSentence()
                }
                event.keyCode == KeyEvent.KEYCODE_DEL -> {
                    if (userInput.isNotEmpty()) {
                        model.sound.playDelete()
                        userInput = userInput.dropLast(1)
                    }
                }
                event.unicodeChar > 0 && !Character.isISOControl(event.unicodeChar.toChar()) -> {
                    userInput += event.unicodeChar.toChar()
                }
            }
        }
    }

    // UI
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        IconButton(onClick = { model.stopSpeaking(); onBack() }) {
            Text("⬅ Quitter")
        }

        LinearProgressIndicator(
            progress = { (currentSentenceIndex + 1).toFloat() / sentences.size },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!hasStarted) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Préparez-vous…", fontSize = 18.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        Text(
            text = rawTarget,
            fontSize = 22.sp,
            lineHeight = 32.sp,
            color = when {
                !hasStarted        -> Color.LightGray
                isFinishedSentence -> Color(0xFF4CAF50)
                else               -> Color.Black
            },
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = userInput,
            onValueChange = { if (hasStarted && !isFinishing) userInput = it.replace("\n", "") },
            label = { Text(if (hasStarted) "Tapez le texte…" else "En attente…") },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            enabled = hasStarted && !isFinishing,
            isError = isError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { goToNextSentence() })
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Phrase ${currentSentenceIndex + 1} / ${sentences.size}", fontSize = 12.sp, color = Color.Gray)
            when {
                isFinishing        -> Text("✅ Fin de session…", fontSize = 12.sp, color = Color(0xFF4CAF50))
                isError            -> Text("❌ Correction nécessaire", fontSize = 12.sp, color = Color(0xFFE53935))
                isFinishedSentence -> Text("✅ Appuyez sur Entrée", fontSize = 12.sp, color = Color(0xFF4CAF50))
            }
        }
    }
}

fun calculateLevenshteinDistance(s1: String, s2: String): Int {
    if (s1 == s2) return 0
    if (s1.isEmpty()) return s2.length
    if (s2.isEmpty()) return s1.length
    val d = Array(s1.length + 1) { IntArray(s2.length + 1) }
    for (i in 0..s1.length) d[i][0] = i
    for (j in 0..s2.length) d[0][j] = j
    for (i in 1..s1.length) for (j in 1..s2.length) {
        val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
        d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
    }
    return d[s1.length][s2.length]
}