package com.k3mobile.testk3.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val K3ColorScheme = lightColorScheme(
    primary = Grey20,
    onPrimary = Color.White,
    primaryContainer = Grey90,
    onPrimaryContainer = Color.Black,
    secondary = Grey40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEEEEE),
    onSecondaryContainer = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Grey30
)

@Composable
fun TestK3Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = K3ColorScheme,
        typography = Typography,
        content = content
    )
}