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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k3mobile.testk3.R
import com.k3mobile.testk3.data.SessionWithTitle
import com.k3mobile.testk3.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun StatsScreen(model: MainViewModel, onBack: () -> Unit) {
    val sessions     by model.sessionsWithTitle.collectAsState()
    val totalCount   by model.totalSessionCount.collectAsState()
    val hasMore      by model.hasMoreSessions.collectAsState()
    val chartSessions by model.chartSessions.collectAsState()

    // Stats globales — toujours calculées sur TOUTE la BDD
    val bestWpm      by model.globalBestWpm.collectAsState()
    val avgAccuracy  by model.globalAvgAccuracy.collectAsState()
    val totalDurMs   by model.globalTotalDuration.collectAsState()

    LaunchedEffect(Unit) { model.loadStats() }

    // Durée totale formatée
    val totalDurSec = totalDurMs / 1000
    val totalDurFormatted = when {
        totalDurSec >= 3600 -> "${totalDurSec / 3600}h ${(totalDurSec % 3600) / 60}min"
        totalDurSec >= 60   -> "${totalDurSec / 60}min ${totalDurSec % 60}s"
        else                -> "${totalDurSec}s"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── App bar ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 8.dp, end = 24.dp, bottom = 8.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.Black)
            }
            Text(
                stringResource(R.string.app_name),
                modifier = Modifier.align(Alignment.Center),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        HorizontalDivider(color = Color.Black, thickness = 1.dp)

        if (sessions.isEmpty() && totalCount == 0) {
            // ── État vide ─────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(R.string.no_session), fontSize = 16.sp, color = Color.Gray)
                    Text(
                        stringResource(R.string.no_session_hint),
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
                // ── Titre ─────────────────────────────────────────────────────
                item {
                    Text(stringResource(R.string.stats_title), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    val s = if (totalCount > 1) "s" else ""
                    Text(
                        stringResource(R.string.sessions_count, totalCount, s, s),
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // ── Grille 2×2 (Alignée avec IntrinsicSize) ──────────────────────
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Première ligne
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SummaryCard(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                value = "$bestWpm",
                                unit = "WPM",
                                label = stringResource(R.string.best_speed)
                            )
                            SummaryCard(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                value = "$avgAccuracy",
                                unit = "%",
                                label = stringResource(R.string.avg_accuracy)
                            )
                        }
                        // Seconde ligne
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SummaryCard(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                value = "$totalCount",
                                unit = "",
                                label = stringResource(R.string.total_sessions)
                            )
                            SummaryCard(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                value = totalDurFormatted,
                                unit = "",
                                label = stringResource(R.string.total_time)
                            )
                        }
                    }
                }

                // ── Graphique de progression ──────────────────────────────────
                if (chartSessions.size >= 2) {
                    item { ProgressChart(sessions = chartSessions) }
                }

                // ── Historique ────────────────────────────────────────────────
                item {
                    Text(
                        stringResource(R.string.history),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(sessions, key = { it.timeStamp }) { session ->
                    SessionCard(session = session)
                }

                if (hasMore) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            OutlinedButton(onClick = { model.loadMoreSessions() }) {
                                Text(stringResource(R.string.load_more), color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// Graphique
// =============================================================================

@Composable
fun ProgressChart(sessions: List<SessionWithTitle>) {
    var showWpm by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 12.dp)) {

            // ── En-tête ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        stringResource(R.string.progression),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        stringResource(R.string.last_sessions, sessions.size),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ChartToggleButton(label = "WPM", selected = showWpm, onClick = { showWpm = true })
                    ChartToggleButton(label = "%",   selected = !showWpm, onClick = { showWpm = false })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val values = sessions.map { if (showWpm) it.wpm.toFloat() else it.accuracy.toFloat() }
            val lastValue = values.last().roundToInt()
            val unitLabel = if (showWpm) "WPM" else "%"

            // Tendance : différence entre moyenne des 3 dernières et des 3 premières
            val trendDelta = run {
                val window = minOf(3, values.size / 2).coerceAtLeast(1)
                val recent = values.takeLast(window).average()
                val old    = values.take(window).average()
                (recent - old).roundToInt()
            }
            val trendText = when {
                trendDelta > 0 -> "↑ +$trendDelta"
                trendDelta < 0 -> "↓ $trendDelta"
                else           -> "→ stable"
            }
            val trendColor = when {
                trendDelta > 0 -> Color(0xFF4CAF50)
                trendDelta < 0 -> Color(0xFFF44336)
                else           -> Color.Gray
            }

            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("$lastValue $unitLabel", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(trendText, fontSize = 13.sp, color = trendColor, modifier = Modifier.padding(bottom = 3.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Canvas ───────────────────────────────────────────────────────
            val lineColor = Color.White
            val fillColor = Color.White.copy(alpha = 0.08f)
            val dotColor  = Color.White
            val gridColor = Color.White.copy(alpha = 0.1f)

            // Moyenne mobile (fenêtre 3) pour la ligne de tendance
            val movingAvg: List<Float> = values.mapIndexed { i, _ ->
                val window = values.subList(maxOf(0, i - 1), minOf(values.size, i + 2))
                window.average().toFloat()
            }

            val gridPaint = remember {
                android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(120, 255, 255, 255)
                    textSize = 22f
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
            }
            val labelPaint = remember {
                android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(120, 255, 255, 255)
                    textSize = 22f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            }

            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                val w = size.width; val h = size.height
                val paddingLeft   = 50f; val paddingRight = 16f
                val paddingTop    = 8f;  val paddingBottom = 28f
                val chartW = w - paddingLeft - paddingRight
                val chartH = h - paddingTop  - paddingBottom

                val minVal = (values.min() * 0.85f).coerceAtLeast(0f)
                val maxVal = values.max() * 1.1f
                val range  = (maxVal - minVal).coerceAtLeast(1f)

                // Grille horizontale
                for (i in 0..3) {
                    val y = paddingTop + chartH - (i.toFloat() / 3) * chartH
                    drawLine(gridColor, Offset(paddingLeft, y), Offset(w - paddingRight, y), 1f)
                    drawContext.canvas.nativeCanvas.drawText(
                        "${(minVal + (i.toFloat() / 3) * range).roundToInt()}",
                        paddingLeft - 6f, y + 5f, gridPaint
                    )
                }

                fun xFor(i: Int) =
                    paddingLeft + if (values.size > 1) i.toFloat() / (values.size - 1) * chartW else chartW / 2f
                fun yFor(v: Float) =
                    paddingTop + chartH - ((v - minVal) / range) * chartH

                val points = values.mapIndexed { i, v -> Offset(xFor(i), yFor(v)) }

                // Aire sous la courbe
                val fillPath = Path().apply {
                    moveTo(points.first().x, paddingTop + chartH)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, paddingTop + chartH)
                    close()
                }
                drawPath(fillPath, fillColor)

                // Courbe principale (cubique)
                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]; val curr = points[i]
                        val cx = (prev.x + curr.x) / 2f
                        cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                    }
                }
                drawPath(linePath, lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

                // Ligne de tendance (moyenne mobile) — en jaune discret
                val trendColor2 = Color(0xFFFFD54F).copy(alpha = 0.6f)
                val trendPoints = movingAvg.mapIndexed { i, v -> Offset(xFor(i), yFor(v)) }
                if (trendPoints.size >= 2) {
                    val trendPath = Path().apply {
                        moveTo(trendPoints.first().x, trendPoints.first().y)
                        for (i in 1 until trendPoints.size) {
                            val prev = trendPoints[i - 1]; val curr = trendPoints[i]
                            val cx = (prev.x + curr.x) / 2f
                            cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                        }
                    }
                    drawPath(trendPath, trendColor2, style = Stroke(width = 2f, cap = StrokeCap.Round, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 6f))))
                }

                // Points + numéros
                points.forEachIndexed { i, pt ->
                    drawCircle(Color.Black, 6f, pt)
                    drawCircle(dotColor, 4f, pt)
                    drawContext.canvas.nativeCanvas.drawText("${i + 1}", pt.x, h - 4f, labelPaint)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.chart_legend), fontSize = 10.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Canvas(modifier = Modifier.size(15.dp, 2.dp)) {
                        drawLine(Color(0xFFFFD54F).copy(alpha = 0.6f), Offset(0f, 1f), Offset(size.width, 1f), 2f)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartToggleButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (selected) Color.White else Color.White.copy(alpha = 0.1f)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.Black else Color.Gray,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// =============================================================================
// Cartes résumé — grille 2×2 (plus espace, texte lisible)
// =============================================================================

@Composable
fun SummaryCard(modifier: Modifier = Modifier, value: String, unit: String, label: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(label, fontSize = 11.sp, color = Color.Gray, lineHeight = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                if (unit.isNotEmpty()) {
                    Text(
                        unit,
                        fontSize = 14.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

// =============================================================================
// Carte historique
// =============================================================================

@Composable
fun SessionCard(session: SessionWithTitle) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()) }
    val formattedDate = dateFormatter.format(Date(session.timeStamp))
    val durationSec = session.duration / 1000
    val formattedDuration = if (durationSec >= 60) "${durationSec / 60}min ${durationSec % 60}s" else "${durationSec}s"
    val accuracyColor = when {
        session.accuracy >= 90 -> Color(0xFF4CAF50)
        session.accuracy >= 70 -> Color(0xFFFF9800)
        else                   -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
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
                MetricItem("${session.wpm.roundToInt()}", "WPM", stringResource(R.string.metric_speed))
                MetricVerticalDivider()
                MetricItem("${session.accuracy.roundToInt()}", "%", stringResource(R.string.metric_accuracy), accuracyColor)
                MetricVerticalDivider()
                MetricItem(formattedDuration, "", stringResource(R.string.metric_duration))
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