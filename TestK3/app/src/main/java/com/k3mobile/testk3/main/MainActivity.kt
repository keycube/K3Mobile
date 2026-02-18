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
// ... gardez vos imports existants et ajoutez :
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

    // Split the full text into sentences based on punctuation
    // Each sentence will be read aloud separately by TextToSpeech
    val sentences = remember(textEntity) {
        textEntity.content
            .split("(?<=[.!?])\\s+".toRegex())
            .filter { it.isNotBlank() }
    }

    // Index of the currently displayed sentence
    var currentSentenceIndex by remember { mutableStateOf(0) }
    // User input for the current sentence
    var userInput by remember { mutableStateOf("") }
    // Session start time (used to compute typing speed)
    val startTime = remember { System.currentTimeMillis() }
    // Focus controller to automatically focus the text field
    val focusRequester = remember { FocusRequester() }

    // Automatically speak the current sentence when it changes
    LaunchedEffect(currentSentenceIndex) {
        if (currentSentenceIndex < sentences.size) {
            model.speak(sentences[currentSentenceIndex])
        }
    }

    // Normalizes text to avoid false typing errors caused by typography differences
    // This allows fair comparison between the displayed text and user input
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
            .replace("\n", " ")
            .replace("\r", "")
            .trim()
    }

    // Original sentence displayed to the user
    val rawTarget = sentences[currentSentenceIndex].trim()

    // Normalized versions used for comparison
    val cleanTarget = rawTarget.normalize()
    val cleanInput = userInput.normalize()

    // Detects a typing error as soon as the input diverges from the target
    val isError = userInput.isNotEmpty() && !cleanTarget.startsWith(cleanInput)
    // Checks if the current sentence is fully typed
    val isFinishedSentence = cleanInput == cleanTarget
    // Checks if this is the last sentence of the text
    val isLastSentence = currentSentenceIndex == sentences.size - 1

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        IconButton(onClick = onBack) { Text("⬅ Quitter") }

        LinearProgressIndicator(
            progress = (currentSentenceIndex.toFloat() / sentences.size),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Affichage (Texte original pour la lecture)
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
                userInput = newValue

                // Si la phrase est finie (en utilisant ton nettoyage), on passe à la suite
                if (newValue.normalize() == cleanTarget && !isLastSentence) {
                    currentSentenceIndex++
                    userInput = ""
                }
            },
            label = { Text("Tapez le texte...") },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            isError = isError,
            supportingText = { if (isError) Text("Erreur de caractère") }
        )

        if (isLastSentence && isFinishedSentence) {
            // Save the typing session when the last sentence is completed
            Button(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                onClick = {
                    val duration = System.currentTimeMillis() - startTime
                    val words = textEntity.content.split("\\s+".toRegex()).size
                    val wpm = words / (duration / 60000.0)

                    // Save session statistics to the database
                    model.saveSession(textEntity.idText, duration, wpm, 100.0)
                    onBack()
                }
            ) {
                Text("ENREGISTRER LA SESSION")
            }
        }
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