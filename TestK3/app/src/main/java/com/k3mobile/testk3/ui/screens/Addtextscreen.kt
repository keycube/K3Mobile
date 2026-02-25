package com.k3mobile.testk3.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k3mobile.testk3.ui.MainViewModel

/**
 * AddTextScreen
 *
 * Écran de création d'un texte personnalisé.
 * L'utilisateur saisit un titre et un contenu, puis sauvegarde
 * directement via le ViewModel.
 *
 * @param model ViewModel partagé de l'application
 * @param onSaved Appelé après la sauvegarde réussie (retour à l'écran précédent)
 * @param onCancel Appelé si l'utilisateur annule
 */
@Composable
fun AddTextScreen(
    model: MainViewModel,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            minLines = 5
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Annuler")
            }
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        // Sauvegarde via le ViewModel puis retour à l'écran précédent
                        model.addCustomText(title, content)
                        onSaved()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Enregistrer")
            }
        }
    }
}
