package com.k3mobile.testk3.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.k3mobile.testk3.ui.MainViewModel
import com.k3mobile.testk3.data.TextEntity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction


// Main entry point of the application
// Sets up the Material theme and displays the main screen
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // Root surface filling the entire screen
                Surface(modifier = Modifier.fillMaxSize()) {
                    CategorySelectionScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectionScreen(model: MainViewModel = viewModel()) {

    // Available text categories for typing practice
    val categories = listOf("phrases", "histoires", "textes personnalisées")

    // Observes the list of texts from the ViewModel (StateFlow)
    val availableTexts by model.texts.collectAsState()

    // UI navigation states
    var selectedText by remember { mutableStateOf<TextEntity?>(null) }
    var currentCategory by remember { mutableStateOf("phrases") }
    var showAddScreen by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }


    // Reload texts whenever the selected category changes
    LaunchedEffect(currentCategory) {
        model.loadTextsByCategory(currentCategory)
    }

    // Simple navigation logic between screens
    when {
        showStats -> {
            StatsScreen(model = model, onBack = { showStats = false })
        }
        showAddScreen -> {
            AddTextScreen(
                // Save custom text and switch to the custom category
                onSave = { title, content ->
                    model.addCustomText(title, content)
                    currentCategory = "textes personnalisées"
                    showAddScreen = false
                },
                onCancel = { showAddScreen = false }
            )
        }
        selectedText != null -> {
            TypingTest(
                textEntity = selectedText!!,
                model = model,
                onBack = {
                    // Return to the text list after finishing or quitting
                    selectedText = null
                    model.loadTextsByCategory(currentCategory)
                }
            )
        }
        else -> {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("K3 Typing", fontWeight = FontWeight.Bold) },
                        actions = {
                            // Stats button in the top bar
                            IconButton(onClick = { showStats = true }) {
                                Text("📊", fontSize = 20.sp)
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { showAddScreen = true }) {
                        Text("+", fontSize = 24.sp)
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Mode d'entraînement",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Category selector (FilterChips)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            FilterChip(
                                selected = (currentCategory == category),
                                onClick = { currentCategory = category },
                                label = { Text(category.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Texts list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(availableTexts) { textEntity ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { selectedText = textEntity },
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
    }
}

@Composable
fun AddTextScreen(onSave: (String, String) -> Unit, onCancel: () -> Unit) {

    // Local state for user input
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Screen title
        Text("Ajouter un texte perso", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        // Text title input
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Titre du texte") },
            modifier = Modifier.fillMaxWidth()
        )

        // Text content input
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Contenu du texte") },
            modifier = Modifier.fillMaxWidth().weight(1f), // Takes remaining vertical space
            minLines = 5
        )

        // Action buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Annuler") }
            Button(
                onClick = { if(title.isNotBlank() && content.isNotBlank()) onSave(title, content) },
                modifier = Modifier.weight(1f),
                enabled = title.isNotBlank() && content.isNotBlank()
            ) { Text("Enregistrer") }
        }
    }
}

@Composable
fun TypingTest(
    textEntity: TextEntity,
    model: MainViewModel,
    onBack: () -> Unit
) {
    // Découpage du texte en phrases
    val sentences = remember(textEntity) {
        textEntity.content
            .split("(?<=[.!?])\\s+".toRegex())
            .filter { it.isNotBlank() }
    }

    var currentSentenceIndex by remember { mutableStateOf(0) }
    var userInput by remember { mutableStateOf("") }
    var totalTypedChars by remember { mutableStateOf(0) }
    var totalDistance by remember { mutableStateOf(0) } // Cumule les erreurs Levenshtein
    var totalEvaluatedChars by remember { mutableStateOf(0) } // Cumule la longueur max évaluée
    val startTime = remember { System.currentTimeMillis() }
    val focusRequester = remember { FocusRequester() }

    // Lecture audio automatique
    LaunchedEffect(currentSentenceIndex) {
        if (currentSentenceIndex < sentences.size) {
            model.speak(sentences[currentSentenceIndex])
        }
    }

    // Normalisation
    fun String.normalize(): String {
        return this
            .replace("’", "'")
            .replace("‘", "'")
            .replace("…", "...")
            .replace("œ", "oe")
            .replace("Œ", "Oe")
            .replace("æ", "ae")
            .replace("Æ", "Ae")
            .replace("ß", "ss")
            .replace("\n", "")
            .replace("\r", "")
            .trim()
    }

    val rawTarget = sentences[currentSentenceIndex]
    val cleanTarget = rawTarget.normalize()
    val cleanInput = userInput.normalize()

    val isError = userInput.isNotEmpty() && !cleanTarget.startsWith(cleanInput)
    val isFinishedSentence = cleanInput == cleanTarget
    val isLastSentence = currentSentenceIndex == sentences.lastIndex

    // Fonction centrale de validation
    fun goToNextSentence() {
        val cleanInput = userInput.normalize()
        val cleanTarget = rawTarget.normalize()

        // 1. WPM : Compter les caractères tapés
        totalTypedChars += cleanInput.length

        // 2. PRÉCISION : Calculer la distance de Levenshtein
        val distance = calculateLevenshteinDistance(cleanInput, cleanTarget)
        totalDistance += distance
        // On se base sur la longueur maximale pour avoir un pourcentage cohérent
        totalEvaluatedChars += maxOf(cleanInput.length, cleanTarget.length)

        if (currentSentenceIndex < sentences.lastIndex) {
            currentSentenceIndex++
            userInput = ""
        } else {
            // Fin du test
            val duration = System.currentTimeMillis() - startTime
            val minutes = duration / 60000.0

            // 🔥 Calcul réel WPM (5 caractères = 1 mot standard)
            val wpm = if (minutes > 0) (totalTypedChars / 5.0) / minutes else 0.0

            // 🔥 Calcul précision avec Levenshtein
            val accuracy = if (totalEvaluatedChars > 0) {
                // (Caractères totaux - erreurs) / Caractères totaux
                ((totalEvaluatedChars - totalDistance).toDouble() / totalEvaluatedChars) * 100
            } else 0.0

            // Sauvegarde de la session (déjà bien câblée vers ton ViewModel)
            model.saveSession(
                textEntity.idText,
                duration,
                wpm,
                accuracy
            )

            model.stopSpeaking()
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Bouton Quitter
        IconButton(onClick = {
            model.stopSpeaking()
            onBack()
        }) {
            Text("⬅ Quitter")
        }

        // Progression correcte (atteint 100%)
        LinearProgressIndicator(
            progress = (currentSentenceIndex + 1).toFloat() / sentences.size,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = rawTarget,
            fontSize = 22.sp,
            lineHeight = 32.sp,
            color = if (isFinishedSentence) Color(0xFF4CAF50) else Color.Black,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(20.dp))

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
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    goToNextSentence()   // 🔥 PAS de validation
                }
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(model: MainViewModel, onBack: () -> Unit) {

    // Observes all recorded typing sessions
    val sessions by model.sessions.collectAsState()

    // Load statistics when the screen appears
    LaunchedEffect(Unit) {
        model.loadStats()
    }

    // Displays either a message or the list of statistics
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
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Aucune session enregistrée pour le moment.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
                items(sessions) { session ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Vitesse: ${session.wpm.toInt()} WPM", fontWeight = FontWeight.Bold)
                                Text("Précision: ${session.accuracy.toInt()}%", style = MaterialTheme.typography.bodySmall)
                            }
                            // Formater la date simplement
                            Text("ID Texte: ${session.textId}", color = Color.Gray)
                        }
                    }
                }
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

    for (i in 1..s1.length) {
        for (j in 1..s2.length) {
            val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
            d[i][j] = minOf(
                d[i - 1][j] + 1,       // Suppression
                d[i][j - 1] + 1,       // Insertion
                d[i - 1][j - 1] + cost // Substitution
            )
        }
    }
    return d[s1.length][s2.length]
}