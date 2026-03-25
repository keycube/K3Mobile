package com.k3mobile.testk3.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k3mobile.testk3.R

@Composable
fun WelcomeScreen(onCommencer: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.welcome_title), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.app_name), fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(48.dp))
            Button(onClick = onCommencer, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)) {
                Text(stringResource(R.string.start), color = MaterialTheme.colorScheme.background)
            }
        }
    }
}