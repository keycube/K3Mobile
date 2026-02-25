package com.k3mobile.testk3.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k3mobile.testk3.data.TextEntity
import com.k3mobile.testk3.ui.MainViewModel

/**
 * TypingScreen
 *
 * Écran d'exercice de frappe.
 * Charge le texte correspondant à [textId] depuis le ViewModel,
 * découpe le contenu en phrases, puis gère la progression phrase par phrase.
 *
 * @param textId ID du texte à taper, transmis via la route de navigation
 * @param model ViewModel partagé de l'application
 * @param onBack Appelé à la fin de l'exercice ou si l'utilisateur quitte
 */
@Composable
fun TypingScreen(
    textId: Long,
    model: MainViewModel,
    onBack: () -> Unit
) {
    // Récupère le texte correspondant à l'ID depuis la liste déjà chargée
    val texts by model.texts.collectAsState()
    val textEntity = texts.find { it.idText == textId }

    // Si le texte n'est pas encore disponible, on affiche un indicateur de chargement
    if (textEntity == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    TypingContent(textEntity = textEntity, model = model, onBack = onBack)
}

/**
 * TypingContent
 *
 * Contenu principal de l'écran de frappe une fois le texte chargé.
 * Gère la logique de progression, la validation et le calcul des performances.
 */
@Composable
private fun TypingContent(
    textEntity: TextEntity,
    model: MainViewModel,
    onBack: () -> Unit
) {
    // Découpe le texte en phrases selon les points et points d'exclamation/interrogation
    val sentences = remember(textEntity.content) {
        textEntity.content
            .split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    var currentSentenceIndex by remember { mutableStateOf(0) }
    var userInput by remember { mutableStateOf("") }
    var totalTypedChars by remember { mutableStateOf(0) }
    var totalDistance by remember { mutableStateOf(0) }
    var totalEvaluatedChars by remember { mutableStateOf(0) }
    val startTime = remember { System.currentTimeMillis() }
    val focusRequester = remember { FocusRequester() }

    // Lecture audio automatique à chaque nouvelle phrase
    LaunchedEffect(currentSentenceIndex) {
        if (currentSentenceIndex < sentences.size) {
            model.speak(sentences[currentSentenceIndex])
        }
    }

    // Donne le focus au champ de saisie au démarrage
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Normalise le texte pour une comparaison tolérante (typographie, retours à la ligne…)
    fun String.normalize(): String = this
        .replace("'", "'").replace("'", "'")
        .replace("…", "...")
        .replace("œ", "oe").replace("Œ", "Oe")
        .replace("æ", "ae").replace("Æ", "Ae")
        .replace("ß", "ss")
        .replace("\n", "").replace("\r", "")
        .trim()

    val rawTarget = sentences[currentSentenceIndex]
    val cleanTarget = rawTarget.normalize()
    val cleanInput = userInput.normalize()

    val isError = userInput.isNotEmpty() && !cleanTarget.startsWith(cleanInput)
    val isFinishedSentence = cleanInput == cleanTarget

    // Valide la phrase en cours et passe à la suivante (ou termine l'exercice)
    fun goToNextSentence() {
        val ci = userInput.normalize()
        val ct = rawTarget.normalize()

        totalTypedChars += ci.length

        val distance = calculateLevenshteinDistance(ci, ct)
        totalDistance += distance
        totalEvaluatedChars += maxOf(ci.length, ct.length)

        if (currentSentenceIndex < sentences.lastIndex) {
            currentSentenceIndex++
            userInput = ""
        } else {
            val duration = System.currentTimeMillis() - startTime
            val minutes = duration / 60000.0
            val wpm = if (minutes > 0) (totalTypedChars / 5.0) / minutes else 0.0
            val accuracy = if (totalEvaluatedChars > 0) {
                ((totalEvaluatedChars - totalDistance).toDouble() / totalEvaluatedChars) * 100
            } else 0.0

            model.saveSession(textEntity.idText, duration, wpm, accuracy)
            model.stopSpeaking()
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Bouton de retour
        IconButton(onClick = {
            model.stopSpeaking()
            onBack()
        }) {
            Text("⬅ Quitter")
        }

        // Barre de progression
        LinearProgressIndicator(
            progress = (currentSentenceIndex + 1).toFloat() / sentences.size,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Texte cible à taper
        Text(
            text = rawTarget,
            fontSize = 22.sp,
            lineHeight = 32.sp,
            color = if (isFinishedSentence) Color(0xFF4CAF50) else Color.Black,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Champ de saisie de l'utilisateur
        OutlinedTextField(
            value = userInput,
            onValueChange = { newValue ->
                userInput = newValue.replace("\n", "")
            },
            label = { Text("Tapez le texte...") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            isError = isError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { goToNextSentence() })
        )
    }
}

/**
 * Calcule la distance de Levenshtein entre deux chaînes.
 * Utilisée pour mesurer la précision de frappe de l'utilisateur.
 */
fun calculateLevenshteinDistance(s1: String, s2: String): Int {
    if (s1 == s2) return 0
    if (s1.isEmpty()) return s2.length
    if (s2.isEmpty()) return s1.length

    val d = Array(s1.length + 1) { IntArray(s2.length + 1) }
    for (i in 0..s1.length) d[i][0] = i
    for (j in 0..s2.length) d[0][j] = j

    for (i in 1..s1.length) {
        for (j in 1..s2.length) {
            val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
            d[i][j] = minOf(
                d[i - 1][j] + 1,
                d[i][j - 1] + 1,
                d[i - 1][j - 1] + cost
            )
        }
    }
    return d[s1.length][s2.length]
}
