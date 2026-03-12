package com.k3mobile.testk3.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k3mobile.testk3.data.TextEntity
import com.k3mobile.testk3.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextListScreen(
    category: String,
    model: MainViewModel,
    onTextSelected: (Long) -> Unit = {},
    onBack: () -> Unit,
    readOnly: Boolean = false
) {
    val texts by model.texts.collectAsState()
    val isCustomCategory = category == "textes personnalisées"

    var showAddDialog by remember { mutableStateOf(false) }
    var textToEdit    by remember { mutableStateOf<TextEntity?>(null) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    var hasNavigated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        model.pendingTextId = null
    }

    LaunchedEffect(category) {
        model.loadTextsByCategory(category)
    }

    if (!readOnly) {
        LaunchedEffect(texts) {
            if (texts.isEmpty()) {
                model.speak("Aucun texte disponible dans cette catégorie")
                return@LaunchedEffect
            }
            announceList(model, texts)
        }

        LaunchedEffect("keys") {
            for (event in model.keyChannel) {
                val keyCode    = event.keyCode
                val digitIndex = keyCodeToIndex(keyCode)
                when {
                    digitIndex != null && digitIndex < texts.size -> {
                        selectedIndex       = digitIndex
                        model.pendingTextId = texts[digitIndex].idText
                        model.speak(
                            "Texte sélectionné ${texts[digitIndex].title}. " +
                                    "Appuyez sur Entrée pour démarrer " +
                                    "ou sur un autre numéro pour changer"
                        )
                    }
                    keyCode == KeyEvent.KEYCODE_ENTER -> {
                        val textId = model.pendingTextId
                        if (textId != null && !hasNavigated) {
                            hasNavigated = true
                            model.pendingTextId = null
                            model.stopSpeaking()
                            onTextSelected(textId)
                        } else if (textId == null) {
                            model.speak("Veuillez d'abord sélectionner un texte avec son numéro")
                        }
                    }
                    keyCode == KeyEvent.KEYCODE_STAR ||
                            (digitIndex != null && digitIndex >= texts.size) -> {
                        announceList(model, texts)
                    }
                    keyCode == KeyEvent.KEYCODE_DEL -> {
                        model.pendingTextId = null
                        model.stopSpeaking()
                        onBack()
                    }
                }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (isCustomCategory) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color.Black,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter un texte")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 8.dp, end = 24.dp, bottom = 8.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = Color.Black)
                }
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "⌨", fontSize = 20.sp)
                    Text(text = "K3AudioType", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = Color.Black, thickness = 1.dp)
            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = category.replaceFirstChar { it.uppercase() },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${texts.size} texte${if (texts.size > 1) "s" else ""} disponible${if (texts.size > 1) "s" else ""}",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (texts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Aucun texte disponible", color = Color.Gray, fontSize = 14.sp)
                        if (isCustomCategory) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Appuyez sur + pour en ajouter un", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(texts.take(9).mapIndexed { i, t -> i to t }) { (idx, textEntity) ->
                        val isSelected = !readOnly && selectedIndex == idx

                        Card(
                            onClick = {
                                if (readOnly) {
                                    if (isCustomCategory) textToEdit = textEntity
                                } else {
                                    model.pendingTextId = null
                                    onTextSelected(textEntity.idText)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFF0F0F0) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(0.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color.Black else Color(0xFFE0E0E0)
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!readOnly) {
                                    Text(
                                        text = "${idx + 1}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(end = 12.dp).width(16.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = textEntity.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = textEntity.content, fontSize = 13.sp, color = Color.Gray, maxLines = 2, lineHeight = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Surface(
                                    shape = MaterialTheme.shapes.extraLarge,
                                    color = Color.Black,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (readOnly && isCustomCategory) "✎" else "→",
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        TextDialog(
            title = "", content = "", dialogTitle = "Nouveau texte", confirmLabel = "Ajouter",
            onConfirm = { t, c -> model.addCustomText(t, c); showAddDialog = false },
            onDismiss = { showAddDialog = false }
        )
    }
    if (textToEdit != null) {
        TextDialog(
            title = textToEdit!!.title, content = textToEdit!!.content,
            dialogTitle = "Modifier le texte", confirmLabel = "Enregistrer",
            onConfirm = { t, c -> model.updateCustomText(textToEdit!!.idText, t, c); textToEdit = null },
            onDismiss = { textToEdit = null }
        )
    }
}

private suspend fun announceList(model: MainViewModel, texts: List<TextEntity>) {
    val count = texts.take(9).size
    val intro = "Vous avez $count texte${if (count > 1) "s" else ""} disponible${if (count > 1) "s" else ""} "
    val items = texts.take(9).mapIndexed { i, t -> "Numéro ${i + 1}  ${t.title}" }.joinToString(". ")
    val hint  = " Appuyez sur le numéro du texte souhaité ou sur Étoile pour répéter la liste"
    model.speak(intro + items + hint)
}

private fun keyCodeToIndex(keyCode: Int): Int? = when (keyCode) {
    KeyEvent.KEYCODE_1 -> 0; KeyEvent.KEYCODE_2 -> 1; KeyEvent.KEYCODE_3 -> 2
    KeyEvent.KEYCODE_4 -> 3; KeyEvent.KEYCODE_5 -> 4; KeyEvent.KEYCODE_6 -> 5
    KeyEvent.KEYCODE_7 -> 6; KeyEvent.KEYCODE_8 -> 7; KeyEvent.KEYCODE_9 -> 8
    else -> null
}

@Composable
fun TextDialog(
    title: String, content: String,
    dialogTitle: String, confirmLabel: String,
    onConfirm: (title: String, content: String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentTitle   by remember { mutableStateOf(title) }
    var currentContent by remember { mutableStateOf(content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentTitle, onValueChange = { currentTitle = it },
                    label = { Text("Titre") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = currentContent, onValueChange = { currentContent = it },
                    label = { Text("Contenu") }, minLines = 4, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (currentTitle.isNotBlank() && currentContent.isNotBlank()) onConfirm(currentTitle, currentContent) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) { Text(confirmLabel, color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = Color.Gray) } }
    )
}