package com.k3mobile.testk3.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k3mobile.testk3.R
import com.k3mobile.testk3.data.TextEntity
import com.k3mobile.testk3.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextListScreen(
    category: String, model: MainViewModel,
    onTextSelected: (Long) -> Unit = {}, onBack: () -> Unit, readOnly: Boolean = false
) {
    val context = LocalContext.current
    val texts by model.texts.collectAsState()
    val isCustomCategory = category == "textes personnalisées"

    var showAddDialog  by remember { mutableStateOf(false) }
    var textToEdit     by remember { mutableStateOf<TextEntity?>(null) }
    var textToDelete   by remember { mutableStateOf<TextEntity?>(null) }
    var selectedIndex  by remember { mutableStateOf<Int?>(null) }
    var hasNavigated   by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { model.pendingTextId = null }
    LaunchedEffect(category) { model.loadTextsByCategory(category) }

    if (!readOnly) {
        LaunchedEffect(texts) {
            if (texts.isEmpty()) { model.speak(context.getString(R.string.tts_no_text)); return@LaunchedEffect }
            announceList(context, model, texts)
        }

        LaunchedEffect("keys") {
            for (event in model.keyChannel) {
                val keyCode = event.keyCode
                val digitIndex = keyCodeToIndex(keyCode)
                when {
                    digitIndex != null && digitIndex < texts.size -> {
                        selectedIndex = digitIndex
                        model.pendingTextId = texts[digitIndex].idText
                        model.speak(context.getString(R.string.tts_text_selected, texts[digitIndex].title))
                    }
                    keyCode == KeyEvent.KEYCODE_ENTER -> {
                        val textId = model.pendingTextId
                        if (textId != null && !hasNavigated) {
                            hasNavigated = true; model.pendingTextId = null; model.stopSpeaking()
                            model.sound.playNavigation(); onTextSelected(textId)
                        } else if (textId == null) {
                            model.speak(context.getString(R.string.tts_select_first))
                        }
                    }
                    keyCode == KeyEvent.KEYCODE_STAR || (digitIndex != null && digitIndex >= texts.size) -> {
                        announceList(context, model, texts)
                    }
                    keyCode == KeyEvent.KEYCODE_DEL -> {
                        model.pendingTextId = null; model.stopSpeaking(); model.sound.playNavigation(); onBack()
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
                    contentColor = Color.White,
                    modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_text))
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 8.dp, end = 24.dp, bottom = 8.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.Black)
                }
                Row(modifier = Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = stringResource(R.string.app_name), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = Color.Black, thickness = 1.dp)
            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(category.replaceFirstChar { it.uppercase() }, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                val s = if (texts.size > 1) "s" else ""
                Text(stringResource(R.string.texts_count, texts.size, s, s), fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))

            if (texts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.no_text_available), color = Color.Gray, fontSize = 14.sp)
                        if (isCustomCategory) { Spacer(modifier = Modifier.height(8.dp)); Text(stringResource(R.string.press_plus_to_add), color = Color.Gray, fontSize = 13.sp) }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(
                        items = texts.take(9).mapIndexed { i, t -> i to t },
                        key = { (_, t) -> t.idText }
                    ) { (idx, textEntity) ->
                        val isSelected = !readOnly && selectedIndex == idx
                        val canSwipeDelete = readOnly && isCustomCategory

                        if (canSwipeDelete) {
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        textToDelete = textEntity
                                    }
                                    false
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFFE53935), MaterialTheme.shapes.medium)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                                    }
                                },
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = true
                            ) {
                                TextItemCard(textEntity, idx, readOnly, isSelected, isCustomCategory,
                                    onClick = { if (isCustomCategory) textToEdit = textEntity })
                            }
                        } else {
                            TextItemCard(textEntity, idx, readOnly, isSelected, isCustomCategory,
                                onClick = {
                                    if (readOnly) { if (isCustomCategory) textToEdit = textEntity }
                                    else { model.pendingTextId = null; onTextSelected(textEntity.idText) }
                                })
                        }
                    }
                }
            }
        }
    }

    // Dialog ajout
    if (showAddDialog) {
        TextDialog(title = "", content = "", dialogTitle = stringResource(R.string.new_text), confirmLabel = stringResource(R.string.add),
            onConfirm = { t, c -> model.addCustomText(t, c); showAddDialog = false }, onDismiss = { showAddDialog = false })
    }

    // Dialog édition
    if (textToEdit != null) {
        TextDialog(title = textToEdit!!.title, content = textToEdit!!.content, dialogTitle = stringResource(R.string.edit_text), confirmLabel = stringResource(R.string.save),
            onConfirm = { t, c -> model.updateCustomText(textToEdit!!.idText, t, c); textToEdit = null }, onDismiss = { textToEdit = null })
    }

    // Dialog confirmation suppression
    if (textToDelete != null) {
        AlertDialog(
            onDismissRequest = { textToDelete = null },
            title = { Text(stringResource(R.string.delete_confirm_title), fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = { Text(stringResource(R.string.delete_confirm_message, textToDelete!!.title)) },
            confirmButton = {
                Button(
                    onClick = { model.deleteCustomText(textToDelete!!.idText); textToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) { Text(stringResource(R.string.delete), color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { textToDelete = null }) {
                    Text(stringResource(R.string.cancel), color = Color.Gray)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextItemCard(
    textEntity: TextEntity, idx: Int, readOnly: Boolean,
    isSelected: Boolean, isCustomCategory: Boolean, onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFF0F0F0) else Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Color.Black else Color(0xFFE0E0E0))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!readOnly) { Text("${idx + 1}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(end = 12.dp).width(16.dp)) }
            Column(modifier = Modifier.weight(1f)) {
                Text(textEntity.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(textEntity.content, fontSize = 13.sp, color = Color.Gray, maxLines = 2, lineHeight = 18.sp)
            }
            if (!(readOnly && isCustomCategory)) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun announceList(context: android.content.Context, model: MainViewModel, texts: List<TextEntity>) {
    val count = texts.take(9).size
    val s = if (count > 1) "s" else ""
    val items = texts.take(9).mapIndexed { i, t -> context.getString(R.string.tts_number, i + 1, t.title) }.joinToString(". ")
    model.speak(context.getString(R.string.tts_text_list, count, s, s, items))
}

private fun keyCodeToIndex(keyCode: Int): Int? = when (keyCode) {
    KeyEvent.KEYCODE_1 -> 0; KeyEvent.KEYCODE_2 -> 1; KeyEvent.KEYCODE_3 -> 2
    KeyEvent.KEYCODE_4 -> 3; KeyEvent.KEYCODE_5 -> 4; KeyEvent.KEYCODE_6 -> 5
    KeyEvent.KEYCODE_7 -> 6; KeyEvent.KEYCODE_8 -> 7; KeyEvent.KEYCODE_9 -> 8
    else -> null
}

@Composable
fun TextDialog(title: String, content: String, dialogTitle: String, confirmLabel: String,
               onConfirm: (title: String, content: String) -> Unit, onDismiss: () -> Unit) {
    var currentTitle   by remember { mutableStateOf(title) }
    var currentContent by remember { mutableStateOf(content) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = currentTitle, onValueChange = { currentTitle = it },
                    label = { Text(stringResource(R.string.title_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = currentContent, onValueChange = { currentContent = it },
                    label = { Text(stringResource(R.string.content_label)) }, minLines = 4, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { if (currentTitle.isNotBlank() && currentContent.isNotBlank()) onConfirm(currentTitle, currentContent) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) { Text(confirmLabel, color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Color.Gray) } }
    )
}