package com.k3mobile.testk3.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k3mobile.testk3.data.SessionWithTitle
import com.k3mobile.testk3.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun StatsScreen(model: MainViewModel, onBack: () -> Unit) {

    val sessions by model.sessionsWithTitle.collectAsState()

    LaunchedEffect(Unit) { model.loadStats() }

    val bestWpm = sessions.maxOfOrNull { it.wpm }?.roundToInt() ?: 0
    val avgAccuracy = if (sessions.isNotEmpty()) sessions.map { it.accuracy }.average().roundToInt() else 0
    val totalSessions = sessions.size

    // Les 10 dernières sessions dans l'ordre chronologique pour le graphe
    val last10 = sessions.take(10).reversed()

    Column(modifier = Modifier.fillMaxSize()) {

        // --- Header ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 8.dp, end = 24.dp, bottom = 8.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
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
                    Text("Aucune session enregistrée.", fontSize = 16.sp, color = Color.Gray)
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

                // Titre
                item {
                    Text("Statistiques", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "$totalSessions session${if (totalSessions > 1) "s" else ""} enregistrée${if (totalSessions > 1) "s" else ""}",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Résumé 3 cartes
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryCard(Modifier.weight(1f), "$bestWpm", "WPM", "Meilleure\nvitesse")
                        SummaryCard(Modifier.weight(1f), "$avgAccuracy", "%", "Précision\nmoyenne")
                        SummaryCard(Modifier.weight(1f), "$totalSessions", "", "Sessions\ntotales")
                    }
                }

                // Graphe des 10 dernières sessions
                if (last10.size >= 2) {
                    item {
                        ProgressChart(sessions = last10)
                    }
                }

                // Titre historique
                item {
                    Text(
                        "Historique",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Liste sessions
                items(sessions) { session ->
                    SessionCard(session = session)
                }
            }
        }
    }
}

/**
 * Graphe des 10 dernières sessions.
 * Affiche deux courbes : WPM (blanc sur fond noir) et précision (gris clair).
 * Un onglet permet de basculer entre les deux métriques.
 */
@Composable
fun ProgressChart(sessions: List<SessionWithTitle>) {

    var showWpm by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // En-tête du graphe
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Progression",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        "${sessions.size} dernières sessions",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                // Toggle WPM / Précision
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartToggleButton(
                        label = "WPM",
                        selected = showWpm,
                        onClick = { showWpm = true }
                    )
                    ChartToggleButton(
                        label = "%",
                        selected = !showWpm,
                        onClick = { showWpm = false }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Valeur actuelle (dernière session)
            val lastValue = if (showWpm) sessions.last().wpm.roundToInt()
            else sessions.last().accuracy.roundToInt()
            val unit = if (showWpm) " WPM" else "%"
            Text(
                "$lastValue$unit",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text("Dernière session", fontSize = 11.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(20.dp))

            // Canvas du graphe
            val values = sessions.map { if (showWpm) it.wpm.toFloat() else it.accuracy.toFloat() }
            val lineColor = Color.White
            val fillColor = Color.White.copy(alpha = 0.08f)
            val dotColor = Color.White
            val gridColor = Color.White.copy(alpha = 0.08f)
            val labelColor = android.graphics.Color.parseColor("#888888")

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val w = size.width
                val h = size.height
                val paddingLeft = 40f
                val paddingRight = 16f
                val paddingTop = 12f
                val paddingBottom = 28f

                val chartW = w - paddingLeft - paddingRight
                val chartH = h - paddingTop - paddingBottom

                val minVal = (values.min() * 0.85f).coerceAtLeast(0f)
                val maxVal = values.max() * 1.1f
                val range = maxVal - minVal

                // Lignes de grille horizontales (3 niveaux)
                val gridSteps = 3
                for (i in 0..gridSteps) {
                    val y = paddingTop + chartH - (i.toFloat() / gridSteps) * chartH
                    drawLine(
                        color = gridColor,
                        start = Offset(paddingLeft, y),
                        end = Offset(w - paddingRight, y),
                        strokeWidth = 1f
                    )
                    // Label axe Y
                    val labelVal = (minVal + (i.toFloat() / gridSteps) * range).roundToInt()
                    drawContext.canvas.nativeCanvas.drawText(
                        "$labelVal",
                        paddingLeft - 6f,
                        y + 5f,
                        android.graphics.Paint().apply {
                            color = labelColor
                            textSize = 22f
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                    )
                }

                // Calcul des points
                val points = values.mapIndexed { i, v ->
                    val x = paddingLeft + if (values.size > 1)
                        i.toFloat() / (values.size - 1) * chartW
                    else chartW / 2f
                    val y = paddingTop + chartH - ((v - minVal) / range) * chartH
                    Offset(x, y)
                }

                // Aire sous la courbe (remplissage)
                val fillPath = Path().apply {
                    moveTo(points.first().x, paddingTop + chartH)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, paddingTop + chartH)
                    close()
                }
                drawPath(fillPath, color = fillColor)

                // Courbe lissée
                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val cx = (prev.x + curr.x) / 2f
                        cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                    }
                }
                drawPath(linePath, color = lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

                // Points + labels axe X
                points.forEachIndexed { i, pt ->
                    // Point blanc
                    drawCircle(color = Color.Black, radius = 6f, center = pt)
                    drawCircle(color = dotColor, radius = 4f, center = pt)

                    // Numéro de session en bas
                    drawContext.canvas.nativeCanvas.drawText(
                        "${i + 1}",
                        pt.x,
                        h - 4f,
                        android.graphics.Paint().apply {
                            color = labelColor
                            textSize = 22f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Sessions (de la plus ancienne à la plus récente)",
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

/**
 * Bouton de bascule WPM / Précision dans le graphe.
 */
@Composable
private fun ChartToggleButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (selected) Color.White else Color.White.copy(alpha = 0.1f)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.Black else Color.Gray,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// --- Composables réutilisés depuis avant ---

@Composable
fun SummaryCard(modifier: Modifier = Modifier, value: String, unit: String, label: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (unit.isNotEmpty()) {
                    Text(unit, fontSize = 13.sp, color = Color.LightGray,
                        modifier = Modifier.padding(start = 2.dp, bottom = 3.dp))
                }
            }
            Text(text = label, fontSize = 11.sp, color = Color.LightGray, lineHeight = 15.sp)
        }
    }
}

@Composable
fun SessionCard(session: SessionWithTitle) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.FRENCH) }
    val formattedDate = dateFormatter.format(Date(session.timeStamp))
    val durationSec = session.duration / 1000
    val formattedDuration = if (durationSec >= 60) "${durationSec / 60}min ${durationSec % 60}s" else "${durationSec}s"
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(session.textTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text(formattedDate, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                MetricItem("${session.wpm.roundToInt()}", "WPM", "Vitesse")
                MetricVerticalDivider()
                MetricItem("${session.accuracy.roundToInt()}", "%", "Précision", accuracyColor)
                MetricVerticalDivider()
                MetricItem(formattedDuration, "", "Durée")
            }
        }
    }
}

@Composable
private fun MetricVerticalDivider() {
    Box(modifier = Modifier.height(36.dp).width(1.dp)) {
        Divider(modifier = Modifier.fillMaxHeight(), color = Color(0xFFE0E0E0), thickness = 1.dp)
    }
}

@Composable
private fun MetricItem(value: String, unit: String, label: String, valueColor: Color = Color.Black) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = valueColor)
            if (unit.isNotEmpty()) {
                Text(unit, fontSize = 12.sp, color = valueColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
            }
        }
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}