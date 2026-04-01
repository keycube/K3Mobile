package com.k3mobile.testk3.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

    data class LangItem(val code: String, val label: String, val flag: String)

    val languages = listOf(
        LangItem("fr", stringResource(R.string.lang_french), "\uD83C\uDDEB\uD83C\uDDF7"),
        LangItem("en", stringResource(R.string.lang_english), "\uD83C\uDDEC\uD83C\uDDE7"),
        LangItem("es", stringResource(R.string.lang_spanish), "\uD83C\uDDEA\uD83C\uDDF8")
    )

    var selectedCode by remember { mutableStateOf(model.savedLanguage) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 8.dp, end = 24.dp, bottom = 8.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.Black)
            }
            Row(modifier = Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = stringResource(R.string.app_name), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        HorizontalDivider(color = Color.Black, thickness = 1.dp)

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.language_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.language_label),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 20.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                languages.forEach { lang ->
                    val isSelected = selectedCode == lang.code
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedCode = lang.code },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFF5F5F5) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) Color.Black else Color(0xFFE0E0E0)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(20.dp).padding(2.dp), contentAlignment = Alignment.Center) {
                                if (isSelected) {
                                    Surface(shape = MaterialTheme.shapes.extraLarge, color = Color.Black, modifier = Modifier.size(16.dp)) {}
                                } else {
                                    Surface(shape = MaterialTheme.shapes.extraLarge, color = Color.Transparent,
                                        border = BorderStroke(1.5.dp, Color.LightGray), modifier = Modifier.size(16.dp)) {}
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(text = lang.flag, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = lang.label,
                                fontSize = 16.sp,
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