package com.k3mobile.testk3.ui.screens


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k3mobile.testk3.R

/**
 * About screen displaying app information, credits, and technologies used.
 *
 * Shows the app name, version, developer info, and a list of
 * libraries/technologies that power the application.
 *
 * @param onBack Callback to navigate back.
 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val version = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
    } catch (_: Exception) { "1.0" }

    Column(modifier = Modifier.fillMaxSize()) {
        K3TopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // App name and version
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.about_version, version),
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.about_description),
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Developer
            AboutSection(title = stringResource(R.string.about_developer)) {
                AboutItem(label = stringResource(R.string.about_developer_name))
                AboutItem(label = stringResource(R.string.about_developer_school))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Technologies
            AboutSection(title = stringResource(R.string.about_technologies)) {
                AboutItem(label = "Kotlin", detail = stringResource(R.string.about_tech_language))
                AboutItem(label = "Jetpack Compose", detail = stringResource(R.string.about_tech_ui))
                AboutItem(label = "Room", detail = stringResource(R.string.about_tech_database))
                AboutItem(label = "Android TTS", detail = stringResource(R.string.about_tech_tts))
                AboutItem(label = "Accessibility Service", detail = stringResource(R.string.about_tech_accessibility))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Acknowledgments
            AboutSection(title = stringResource(R.string.about_acknowledgments)) {
                AboutItem(label = stringResource(R.string.about_thanks_testers))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Section header with a title and content block.
 */
@Composable
private fun AboutSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

/**
 * Single credit item with a label and optional detail text.
 */
@Composable
private fun AboutItem(label: String, detail: String? = null) {
    Column {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        if (detail != null) {
            Text(detail, fontSize = 12.sp, color = Color.Gray)
        }
    }
}