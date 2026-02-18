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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
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
    val categories = listOf("phrases", "histoires", "textes personnalisées")
    val availableTexts by model.texts.collectAsState()

    var selectedText by remember { mutableStateOf<TextEntity?>(null) }
    var currentCategory by remember { mutableStateOf("phrases") }
    var showAddScreen by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }

    LaunchedEffect(currentCategory) {
        model.loadTextsByCategory(currentCategory)
    }

    when {
        showStats -> {
            StatsScreen(model = model, onBack = { showStats = false })
        }
        showAddScreen -> {
            AddTextScreen(
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
                            // Bouton Stats dans la barre du haut
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

                    // Sélecteur de catégories (FilterChips)
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

                    // Liste des textes
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
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Ajouter un texte perso", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Titre du texte") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Contenu du texte") },
            modifier = Modifier.fillMaxWidth().weight(1f), // Prend l'espace restant
            minLines = 5
        )

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
    val sentences = remember(textEntity) {
        textEntity.content
            .split("(?<=[.!?])\\s+".toRegex())
            .filter { it.isNotBlank() }
    }

    var currentSentenceIndex by remember { mutableStateOf(0) }
    var userInput by remember { mutableStateOf("") }
    val startTime = remember { System.currentTimeMillis() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(currentSentenceIndex) {
        if (currentSentenceIndex < sentences.size) {
            model.speak(sentences[currentSentenceIndex])
        }
    }

    // --- TON BLOC DE NETTOYAGE PERSONNALISÉ ---
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

    val rawTarget = sentences[currentSentenceIndex].trim()
    val cleanTarget = rawTarget.normalize()
    val cleanInput = userInput.normalize()

    val isError = userInput.isNotEmpty() && !cleanTarget.startsWith(cleanInput)
    val isFinishedSentence = cleanInput == cleanTarget
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
            Button(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                onClick = {
                    val duration = System.currentTimeMillis() - startTime
                    val words = textEntity.content.split("\\s+".toRegex()).size
                    val wpm = words / (duration / 60000.0)

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
    val sessions by model.sessions.collectAsState()

    LaunchedEffect(Unit) {
        model.loadStats()
    }

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