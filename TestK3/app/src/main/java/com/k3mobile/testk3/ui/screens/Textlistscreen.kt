package com.k3mobile.testk3.ui.screens

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

/**
 * TextListScreen
 *
 * Affiche la liste des textes d'une catégorie.
 *
 * @param readOnly Si true (depuis les paramètres) : pas de navigation vers la partie,
 *                 les textes personnalisés sont modifiables via un dialog.
 *                 Si false (depuis custom_game) : clic sur un texte lance la partie.
 */
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
    var textToEdit by remember { mutableStateOf<TextEntity?>(null) }

    LaunchedEffect(category) {
        model.loadTextsByCategory(category)
    }

    Scaffold(
        floatingActionButton = {
            // Bouton + visible uniquement pour les textes personnalisés
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // --- Header ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 8.dp, end = 24.dp, bottom = 8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
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

            // --- Titre + compteur ---
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

            // --- Liste ou message vide ---
            if (texts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Aucun texte disponible.", color = Color.Gray, fontSize = 14.sp)
                        if (isCustomCategory) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Appuyez sur + pour en ajouter un.", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(texts) { textEntity ->
                        Card(
                            onClick = {
                                if (readOnly) {
                                    // Mode consultation : ouvre le dialog d'édition pour les textes perso
                                    if (isCustomCategory) textToEdit = textEntity
                                } else {
                                    // Mode jeu : lance la partie
                                    onTextSelected(textEntity.idText)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = textEntity.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = textEntity.content,
                                        fontSize = 13.sp,
                                        color = Color.Gray,
                                        maxLines = 2,
                                        lineHeight = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // En mode readOnly textes perso : icône crayon, sinon flèche
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

    // --- Dialog d'ajout ---
    if (showAddDialog) {
        TextDialog(
            title = "",
            content = "",
            dialogTitle = "Nouveau texte",
            confirmLabel = "Ajouter",
            onConfirm = { t, c ->
                model.addCustomText(t, c)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // --- Dialog d'édition ---
    if (textToEdit != null) {
        TextDialog(
            title = textToEdit!!.title,
            content = textToEdit!!.content,
            dialogTitle = "Modifier le texte",
            confirmLabel = "Enregistrer",
            onConfirm = { t, c ->
                model.updateCustomText(textToEdit!!.idText, t, c)
                textToEdit = null
            },
            onDismiss = { textToEdit = null }
        )
    }
}

/**
 * TextDialog
 *
 * Dialog réutilisable pour ajouter ou modifier un texte personnalisé.
 */
@Composable
fun TextDialog(
    title: String,
    content: String,
    dialogTitle: String,
    confirmLabel: String,
    onConfirm: (title: String, content: String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentTitle by remember { mutableStateOf(title) }
    var currentContent by remember { mutableStateOf(content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentTitle,
                    onValueChange = { currentTitle = it },
                    label = { Text("Titre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = currentContent,
                    onValueChange = { currentContent = it },
                    label = { Text("Contenu") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentTitle.isNotBlank() && currentContent.isNotBlank()) {
                        onConfirm(currentTitle, currentContent)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text(confirmLabel, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = Color.Gray)
            }
        }
    )
}