package com.k3mobile.testk3.ui.screens

import android.content.Intent
import android.view.KeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k3mobile.testk3.R
import com.k3mobile.testk3.data.TextEntity
import com.k3mobile.testk3.main.TypingForegroundService
import com.k3mobile.testk3.ui.MainViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Entry point for the typing exercise screen.
 *
 * Waits for the text entity to be available in the ViewModel's text list,
 * showing a loading spinner in the meantime, then delegates to [TypingContent].
 *
 * @param textId Database ID of the text to type.
 * @param model Shared [MainViewModel].
 * @param onBack Callback to abort the session and navigate back.
 * @param onFinished Callback when the session is completed and results are dismissed.
 */
@Composable
fun TypingScreen(textId: Long, model: MainViewModel, onBack: () -> Unit, onFinished: () -> Unit) {
    val texts by model.texts.collectAsState()
    val textEntity = texts.find { it.idText == textId }
    if (textEntity == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    TypingContent(textEntity = textEntity, model = model, onBack = onBack, onFinished = onFinished)
}

/**
 * Core typing exercise composable.
 *
 * Manages the full session lifecycle:
 * 1. **Countdown** (screen-off mode): TTS "Get ready" + 3-2-1 countdown with earcons.
 *    In screen-on mode, typing starts immediately without countdown or TTS.
 * 2. **Typing**: displays sentences one by one, validates input via prefix matching,
 *    tracks typed characters and Levenshtein distance for accuracy.
 * 3. **Results**: shows WPM, accuracy (color-coded), and duration in black cards.
 *    TTS reads the results aloud (screen-off mode only).
 *
 * Text normalization handles typographic variants (curly quotes, ligatures, etc.)
 * so that physical keyboard input matches the displayed text.
 *
 * @param textEntity The text entity containing the content to type.
 * @param model Shared [MainViewModel].
 * @param onBack Callback to abort mid-session.
 * @param onFinished Callback after results are dismissed.
 */
@Composable
private fun TypingContent(textEntity: TextEntity, model: MainViewModel, onBack: () -> Unit, onFinished: () -> Unit) {
    val context = LocalContext.current
    val sentences = remember(textEntity.content) {
        textEntity.content
            .split(Regex("(?<=[.!?])\\s+|\n+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    var currentSentenceIndex by remember { mutableStateOf(0) }
    var userInput            by remember { mutableStateOf("") }
    var totalTypedChars      by remember { mutableStateOf(0) }
    var totalDistance         by remember { mutableStateOf(0) }
    var totalEvaluatedChars  by remember { mutableStateOf(0) }
    val startTime            = remember { System.currentTimeMillis() }
    val focusRequester       = remember { FocusRequester() }
    var hasStarted           by remember { mutableStateOf(false) }
    var isFinishing          by remember { mutableStateOf(false) }
    var showResults          by remember { mutableStateOf(false) }
    var resultWpm            by remember { mutableStateOf(0) }
    var resultAccuracy       by remember { mutableStateOf(0) }
    var resultDurationSec   by remember { mutableStateOf(0L) }
    // 0 = screen on, 1 = black screen, 2 = screen off
    val screenOnMode = remember { model.savedScreenMode }
    
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

    val ttsGetReady     = stringResource(R.string.tts_get_ready)
    val ttsBravo        = stringResource(R.string.tts_bravo)
    val ttsBackToMenu   = stringResource(R.string.tts_back_to_menu)

    fun goToNextSentence() {
        if (isFinishing) return
        val ci = userInput.normalize()
        val ct = sentences[currentSentenceIndex].normalize()
        totalTypedChars += ci.length
        val distance = calculateLevenshteinDistance(ci, ct)
        totalDistance += distance
        totalEvaluatedChars += maxOf(ci.length, ct.length)

        if (currentSentenceIndex < sentences.lastIndex) {
            if (screenOnMode != 0) model.sound.playValidation()
            currentSentenceIndex++
            userInput = ""
        } else {
            isFinishing = true
            if (screenOnMode != 0) {
                model.sound.playVictory()
                model.sound.vibrateVictory()
            }

            val duration = System.currentTimeMillis() - startTime
            val minutes = duration / 60_000.0
            val wpm = if (minutes > 0) (totalTypedChars / 5.0) / minutes else 0.0
            val accuracy = if (totalEvaluatedChars > 0) ((totalEvaluatedChars - totalDistance).toDouble() / totalEvaluatedChars) * 100 else 0.0
            model.saveSession(textEntity.idText, duration, wpm, accuracy)

            val wpmRounded = wpm.roundToInt()
            val accRounded = accuracy.roundToInt()
            val minPart = (duration / 60_000).toInt()
            val secPart = ((duration % 60_000) / 1_000).toInt()
            val durationText = if (minPart > 0)
                context.getString(R.string.duration_min_sec, minPart, if (minPart > 1) "s" else "", secPart)
            else context.getString(R.string.duration_sec, secPart)

            resultWpm = wpmRounded
            resultAccuracy = accRounded
            resultDurationSec = (System.currentTimeMillis() - startTime) / 1000
            showResults = true

            if (screenOnMode != 0) {
                model.speak(ttsBravo)
                model.speakQueued(context.getString(R.string.tts_duration, durationText))
                model.speakQueued(context.getString(R.string.tts_speed_result, wpmRounded))
                model.speakQueued(context.getString(R.string.tts_accuracy_result, accRounded))
                model.speakQueued(ttsBackToMenu)
            }
        }
    }

    LaunchedEffect(Unit) {
        context.startService(Intent(context, TypingForegroundService::class.java).apply {
            putExtra(TypingForegroundService.EXTRA_STATUS, TypingForegroundService.STATUS_READY) })
        if (screenOnMode == 0) {

            context.startService(Intent(context, TypingForegroundService::class.java).apply {
                putExtra(TypingForegroundService.EXTRA_STATUS, TypingForegroundService.STATUS_TYPING) })
            hasStarted = true
            focusRequester.requestFocus()
        } else {

            model.speak(ttsGetReady)
            delay(1_800)
            model.sound.playCountdownTick(); model.speakQueued("3"); delay(900)
            model.sound.playCountdownTick(); model.speakQueued("2"); delay(900)
            model.sound.playCountdownTick(); model.speakQueued("1"); delay(900)
            model.sound.playCountdownGo()
            context.startService(Intent(context, TypingForegroundService::class.java).apply {
                putExtra(TypingForegroundService.EXTRA_STATUS, TypingForegroundService.STATUS_TYPING) })
            hasStarted = true
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(currentSentenceIndex, hasStarted) {
        if (hasStarted && currentSentenceIndex < sentences.size && !isFinishing && screenOnMode != 0) {
            model.speak(sentences[currentSentenceIndex])
        }
    }

    LaunchedEffect("typing_keys") {
        for (event in model.keyChannel) {
            if (showResults) {
                if (event.keyCode == KeyEvent.KEYCODE_ENTER) {
                    model.stopSpeaking(); model.sound.playNavigation(); onFinished()
                }
                continue
            }
            if (!hasStarted || isFinishing) continue
            when {
                event.keyCode == KeyEvent.KEYCODE_ENTER -> goToNextSentence()
                event.keyCode == KeyEvent.KEYCODE_DEL -> { if (userInput.isNotEmpty()) { if (screenOnMode != 0) model.sound.playDelete(); userInput = userInput.dropLast(1) } }
                event.unicodeChar > 0 && !Character.isISOControl(event.unicodeChar.toChar()) -> userInput += event.unicodeChar.toChar()
            }
        }
    }

    // UI
    if (showResults) {

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(stringResource(R.string.tts_bravo), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(textEntity.title, fontSize = 16.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(modifier = Modifier.weight(1f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = Color.Black), elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$resultWpm", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("WPM", fontSize = 13.sp, color = Color.LightGray, modifier = Modifier.padding(start = 2.dp, bottom = 3.dp))
                        }
                        Text(stringResource(R.string.metric_speed), fontSize = 11.sp, color = Color.LightGray)
                    }
                }
                Card(modifier = Modifier.weight(1f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = Color.Black), elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val accuracyColor = when { resultAccuracy >= 90 -> Color(0xFF4CAF50); resultAccuracy >= 70 -> Color(0xFFFF9800); else -> Color(0xFFF44336) }
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$resultAccuracy", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = accuracyColor)
                            Text("%", fontSize = 13.sp, color = accuracyColor.copy(alpha = 0.7f), modifier = Modifier.padding(start = 2.dp, bottom = 3.dp))
                        }
                        Text(stringResource(R.string.metric_accuracy), fontSize = 11.sp, color = Color.LightGray)
                    }
                }
                Card(modifier = Modifier.weight(1f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = Color.Black), elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$resultDurationSec", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("s", fontSize = 13.sp, color = Color.LightGray, modifier = Modifier.padding(start = 2.dp, bottom = 3.dp))
                        }
                        Text(stringResource(R.string.metric_duration), fontSize = 11.sp, color = Color.LightGray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { model.stopSpeaking(); model.sound.playNavigation(); onFinished() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
            ) {
                Text(stringResource(R.string.back_to_menu), color = MaterialTheme.colorScheme.background, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    } else {

        Column(modifier = Modifier.fillMaxSize()) {
            K3TopBar(onBack = { model.stopSpeaking(); onBack() })
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                LinearProgressIndicator(
                    progress = { (currentSentenceIndex + 1).toFloat() / sentences.size },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (!hasStarted) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.get_ready),
                            fontSize = 18.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Text(
                    rawTarget, fontSize = 22.sp, lineHeight = 32.sp,
                    color = when {
                        !hasStarted -> Color.LightGray; isFinishedSentence -> Color(0xFF4CAF50); else -> Color.Black
                    },
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = userInput,
                    onValueChange = {
                        if (hasStarted && !isFinishing) userInput = it.replace("\n", "")
                    },
                    label = {
                        Text(
                            if (hasStarted) stringResource(R.string.type_here) else stringResource(
                                R.string.waiting
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    enabled = hasStarted && !isFinishing,
                    isError = isError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { goToNextSentence() })
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(
                            R.string.sentence_progress,
                            currentSentenceIndex + 1,
                            sentences.size
                        ), fontSize = 12.sp, color = Color.Gray
                    )
                    when {
                        isFinishing -> Text(
                            stringResource(R.string.session_end),
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50)
                        )

                        isError -> Text(
                            stringResource(R.string.correction_needed),
                            fontSize = 12.sp,
                            color = Color(0xFFE53935)
                        )

                        isFinishedSentence -> Text(
                            stringResource(R.string.press_enter),
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Computes the Levenshtein (edit) distance between two strings.
 *
 * Used to calculate typing accuracy: the fewer edits needed to transform
 * the user's input into the target sentence, the higher the accuracy.
 *
 * @param s1 First string (typically user input).
 * @param s2 Second string (typically target sentence).
 * @return The minimum number of single-character edits (insertions, deletions,
 *         substitutions) required to change [s1] into [s2].
 */
fun calculateLevenshteinDistance(s1: String, s2: String): Int {
    if (s1 == s2) return 0; if (s1.isEmpty()) return s2.length; if (s2.isEmpty()) return s1.length
    val d = Array(s1.length + 1) { IntArray(s2.length + 1) }
    for (i in 0..s1.length) d[i][0] = i; for (j in 0..s2.length) d[0][j] = j
    for (i in 1..s1.length) for (j in 1..s2.length) {
        val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
        d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
    }
    return d[s1.length][s2.length]
}