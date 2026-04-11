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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k3mobile.testk3.R
import com.k3mobile.testk3.data.TextEntity
import com.k3mobile.testk3.ui.MainViewModel
import kotlinx.coroutines.launch

/**
 * Screen displaying a list of available texts for a given category.
 *
 * Operates in two modes:
 * - **Game mode** (`readOnly = false`): texts are numbered 1–9 for keyboard
 *   selection, with TTS announcement. User selects a text and presses ENTER.
 * - **Settings mode** (`readOnly = true`): for custom texts only. Supports
 *   swipe-to-delete with a Snackbar undo mechanism, tap-to-edit, and a FAB
 *   to add new texts.
 *
 * Keyboard shortcuts (game mode only):
 * - 1–9: select text by number
 * - ENTER: confirm selection and start typing
 * - DEL: go back
 * - *: repeat the list announcement
 *
 * @param category Database category name ("phrases", "histoires", "textes personnalisées").
 * @param model Shared [MainViewModel].
 * @param onTextSelected Callback with the selected text ID (game mode).
 * @param onBack Callback to navigate back.
 * @param readOnly If `true`, disables game keyboard controls and enables edit/delete.
 */
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
    var selectedIndex  by remember { mutableStateOf<Int?>(null) }
    var hasNavigated   by remember { mutableStateOf(false) }

    // Snackbar-based undo delete: text is hidden immediately, then either
    // restored (undo) or permanently deleted after the snackbar timeout.
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var hiddenTextId by remember { mutableStateOf<Long?>(null) }
    val deleteLabel = stringResource(R.string.text_deleted)
    val undoLabel = stringResource(R.string.undo)

    LaunchedEffect(Unit) { model.pendingTextId = null }
    LaunchedEffect(category) { model.loadTextsByCategory(category) }

    // Game mode: TTS announcements and keyboard event loop
    if (!readOnly) {
        // Announce the list via TTS when texts are loaded
        LaunchedEffect(texts) {
            if (texts.isEmpty()) { model.speak(context.getString(R.string.tts_no_text)); return@LaunchedEffect }
            announceList(context, model, texts)
        }

        // Physical keyboard event loop
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
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color.Black,
                    contentColor = Color.White,
                    actionColor = Color.White
                )
            }
        },
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
            K3TopBar(onBack = onBack)

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(category.replaceFirstChar { it.uppercase() }, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                val visibleCount = texts.count { it.idText != hiddenTextId }
                val s = if (visibleCount > 1) "s" else ""
                Text(stringResource(R.string.texts_count, visibleCount, s, s), fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))

            val visibleTexts = texts.filter { it.idText != hiddenTextId }

            if (visibleTexts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.no_text_available), color = Color.Gray, fontSize = 14.sp)
                        if (isCustomCategory) { Spacer(modifier = Modifier.height(8.dp)); Text(stringResource(R.string.press_plus_to_add), color = Color.Gray, fontSize = 13.sp) }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(
                        items = visibleTexts.take(9).mapIndexed { i, t -> i to t },
                        key = { (_, t) -> t.idText }
                    ) { (idx, textEntity) ->
                        val isSelected = !readOnly && selectedIndex == idx
                        val canSwipeDelete = readOnly && isCustomCategory

                        if (canSwipeDelete) {
                            // Swipe-to-delete with undo snackbar
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        val deletedId = textEntity.idText
                                        hiddenTextId = deletedId
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = deleteLabel,
                                                actionLabel = undoLabel,
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                hiddenTextId = null
                                            } else {
                                                hiddenTextId = null
                                                model.deleteCustomText(deletedId)
                                            }
                                        }
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

    // Add custom text dialog
    if (showAddDialog) {
        TextDialog(title = "", content = "", dialogTitle = stringResource(R.string.new_text), confirmLabel = stringResource(R.string.add),
            onConfirm = { t, c -> model.addCustomText(t, c); showAddDialog = false }, onDismiss = { showAddDialog = false })
    }

    // Edit custom text dialog
    if (textToEdit != null) {
        TextDialog(title = textToEdit!!.title, content = textToEdit!!.content, dialogTitle = stringResource(R.string.edit_text), confirmLabel = stringResource(R.string.save),
            onConfirm = { t, c -> model.updateCustomText(textToEdit!!.idText, t, c); textToEdit = null }, onDismiss = { textToEdit = null })
    }
}

/**
 * Single text item card with number, title, content preview, and optional arrow.
 *
 * In game mode: shows the keyboard number and a right arrow.
 * In read-only custom mode: hides the arrow to encourage swipe gestures.
 *
 * @param textEntity The text data to display.
 * @param idx Zero-based index in the visible list.
 * @param readOnly Whether the screen is in settings (read-only) mode.
 * @param isSelected Whether this item is currently selected (game mode).
 * @param isCustomCategory Whether the current category is custom texts.
 * @param onClick Callback when the card is tapped.
 */
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
                Text(textEntity.content, fontSize = 13.sp, color = Color.Gray, maxLines = 2, lineHeight = 18.sp, overflow = TextOverflow.Ellipsis)
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

/**
 * Speaks the text list via TTS for visually impaired users.
 *
 * Announces the count and each text's number + title.
 */
private fun announceList(context: android.content.Context, model: MainViewModel, texts: List<TextEntity>) {
    val count = texts.take(9).size
    val s = if (count > 1) "s" else ""
    val items = texts.take(9).mapIndexed { i, t -> context.getString(R.string.tts_number, i + 1, t.title) }.joinToString(". ")
    model.speak(context.getString(R.string.tts_text_list, count, s, s, items))
}

/**
 * Maps a physical key code to a zero-based list index (1→0, 2→1, ..., 9→8).
 *
 * @return The index, or `null` if the key code is not a digit 1–9.
 */
private fun keyCodeToIndex(keyCode: Int): Int? = when (keyCode) {
    KeyEvent.KEYCODE_1 -> 0; KeyEvent.KEYCODE_2 -> 1; KeyEvent.KEYCODE_3 -> 2
    KeyEvent.KEYCODE_4 -> 3; KeyEvent.KEYCODE_5 -> 4; KeyEvent.KEYCODE_6 -> 5
    KeyEvent.KEYCODE_7 -> 6; KeyEvent.KEYCODE_8 -> 7; KeyEvent.KEYCODE_9 -> 8
    else -> null
}

/**
 * Dialog for creating or editing a custom text.
 *
 * @param title Initial title value (empty for new texts).
 * @param content Initial content value.
 * @param dialogTitle Title displayed at the top of the dialog.
 * @param confirmLabel Label for the confirm button ("Add" or "Save").
 * @param onConfirm Callback with the edited title and content.
 * @param onDismiss Callback when the dialog is dismissed.
 */
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