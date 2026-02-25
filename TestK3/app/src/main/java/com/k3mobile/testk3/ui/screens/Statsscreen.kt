package com.k3mobile.testk3.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k3mobile.testk3.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * StatsScreen
 *
 * Affiche un résumé global (meilleure vitesse, précision moyenne, sessions totales)
 * puis la liste détaillée de chaque session avec titre du texte, date, durée, WPM et précision.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(model: MainViewModel, onBack: () -> Unit) {

    val sessions by model.sessionsWithTitle.collectAsState()

    LaunchedEffect(Unit) {
        model.loadStats()
    }

    // --- Calculs du résumé global ---
    val bestWpm = sessions.maxOfOrNull { it.wpm }?.roundToInt() ?: 0
    val avgAccuracy = if (sessions.isNotEmpty())
        sessions.map { it.accuracy }.average().roundToInt() else 0
    val totalSessions = sessions.size

    Column(modifier = Modifier.fillMaxSize()) {

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
                Text("⌨", fontSize = 20.sp)
                Text("K3AudioType", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(color = Color.Black, thickness = 1.dp)

        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⌨", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Aucune session enregistrée.",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                    Text(
                        "Lancez une partie pour voir vos stats !",
                        fontSize = 13.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // --- Titre ---
                item {
                    Text("Statistiques", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "$totalSessions session${if (totalSessions > 1) "s" else ""} enregistrée${if (totalSessions > 1) "s" else ""}",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // --- Résumé global en 3 cartes ---
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryCard(
                            modifier = Modifier.weight(1f),
                            value = "$bestWpm",
                            unit = "WPM",
                            label = "Meilleure\nvitesse"
                        )
                        SummaryCard(
                            modifier = Modifier.weight(1f),
                            value = "$avgAccuracy",
                            unit = "%",
                            label = "Précision\nmoyenne"
                        )
                        SummaryCard(
                            modifier = Modifier.weight(1f),
                            value = "$totalSessions",
                            unit = "",
                            label = "Sessions\ntotales"
                        )
                    }
                }

                // --- Séparateur ---
                item {
                    Text(
                        "Historique",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // --- Liste des sessions ---
                items(sessions) { session ->
                    SessionCard(session = session)
                }
            }
        }
    }
}

/**
 * Carte de résumé global (WPM max, précision moyenne, total sessions).
 */
@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    value: String,
    unit: String,
    label: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (unit.isNotEmpty()) {
                    Text(
                        unit,
                        fontSize = 13.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(start = 2.dp, bottom = 3.dp)
                    )
                }
            }
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.LightGray,
                lineHeight = 15.sp
            )
        }
    }
}

/**
 * Carte détaillée d'une session : titre du texte, date, durée, WPM, précision.
 */
@Composable
fun SessionCard(session: com.k3mobile.testk3.data.SessionWithTitle) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.FRENCH) }
    val formattedDate = dateFormatter.format(Date(session.timeStamp))
    val durationSec = session.duration / 1000
    val formattedDuration = if (durationSec >= 60) {
        "${durationSec / 60}min ${durationSec % 60}s"
    } else {
        "${durationSec}s"
    }

    // Couleur de la précision : vert si ≥ 90%, orange si ≥ 70%, rouge sinon
    val accuracyColor = when {
        session.accuracy >= 90 -> Color(0xFF4CAF50)
        session.accuracy >= 70 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Titre du texte + date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = session.textTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(12.dp))

            // Métriques : WPM · Précision · Durée
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricItem(value = "${session.wpm.roundToInt()}", unit = "WPM", label = "Vitesse")
                VerticalDivider()
                MetricItem(
                    value = "${session.accuracy.roundToInt()}",
                    unit = "%",
                    label = "Précision",
                    valueColor = accuracyColor
                )
                VerticalDivider()
                MetricItem(value = formattedDuration, unit = "", label = "Durée")
            }
        }
    }
}

/**
 * Séparateur vertical léger entre les métriques.
 */
@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .height(36.dp)
            .width(1.dp)
            .padding(vertical = 4.dp),
    ) {
        Divider(
            modifier = Modifier.fillMaxHeight(),
            color = Color(0xFFE0E0E0),
            thickness = 1.dp
        )
    }
}

/**
 * Affiche une valeur avec son unité et un label en dessous.
 */
@Composable
private fun MetricItem(
    value: String,
    unit: String,
    label: String,
    valueColor: Color = Color.Black
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = valueColor)
            if (unit.isNotEmpty()) {
                Text(
                    unit,
                    fontSize = 12.sp,
                    color = valueColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
                )
            }
        }
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}