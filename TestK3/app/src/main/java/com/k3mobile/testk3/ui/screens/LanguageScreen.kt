package com.k3mobile.testk3.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import com.k3mobile.testk3.ui.MainViewModel

@Composable
fun LanguageScreen(
    model: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val languages = listOf(
        "fr" to stringResource(R.string.lang_french),
        "en" to stringResource(R.string.lang_english),
        "es" to stringResource(R.string.lang_spanish)
    )

    var selectedCode by remember { mutableStateOf(model.savedLanguage) }

    Box(modifier = Modifier.fillMaxSize()) {

        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(22.dp),
            tint = Color.Black
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = stringResource(R.string.app_name), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = stringResource(R.string.language_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            )

            Text(
                text = stringResource(R.string.language_label),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                languages.forEach { (code, label) ->
                    val isSelected = selectedCode == code
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { selectedCode = code },
                        color = if (isSelected) Color(0xFFF0F0F0) else Color.White,
                        border = BorderStroke(0.5.dp, Color(0xFFE0E0E0))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.back), color = Color.Black, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(24.dp))
                Button(
                    onClick = {
                        if (selectedCode != model.savedLanguage) {
                            model.setLanguage(selectedCode)
                            // Recréer l'Activity pour appliquer la nouvelle locale
                            (context as? Activity)?.recreate()
                        } else {
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground
                    )
                ) {
                    Text(stringResource(R.string.accept), color = MaterialTheme.colorScheme.background)
                }
            }
        }
    }
}