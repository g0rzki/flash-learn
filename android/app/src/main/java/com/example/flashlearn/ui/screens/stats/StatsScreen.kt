package com.example.flashlearn.ui.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flashlearn.data.remote.dto.StatsDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statystyki") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadStats() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Odśwież")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is StatsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is StatsUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadStats() }) {
                            Text("Spróbuj ponownie")
                        }
                    }
                }
                is StatsUiState.Success -> {
                    StatsContent(stats = state.stats)
                }
            }
        }
    }
}

@Composable
fun StatsContent(stats: StatsDto) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        StreakCard(stats.currentStreak, stats.longestStreak)
        MasteryCard(stats)
        BarChartCard(stats.cardsPerDayLast7)
    }
}

@Composable
fun StreakCard(currentStreak: Int, longestStreak: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Streak nauki", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$currentStreak", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Obecny", style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$longestStreak", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.secondary)
                    Text("Najlepszy", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun MasteryCard(stats: StatsDto) {
    val total = stats.totalReviewed
    val correct = stats.correctAnswers
    val masteryPercent = if (total > 0) (correct.toFloat() / total) * 100 else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Opanowanie materiału", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "Na podstawie ostatnich 30 dni",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Oceny: Złe: ${stats.wrongAnswers}, Trudne: ${stats.hardAnswers}, Dobre: ${stats.correctAnswers}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Box(contentAlignment = Alignment.Center) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                Canvas(modifier = Modifier.size(80.dp)) {
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = 360f * (masteryPercent / 100f),
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${masteryPercent.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BarChartCard(dataMap: Map<String, Long>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Fiszki w ostatnich 7 dniach", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (dataMap.isEmpty()) {
                Text("Brak danych za ostatnie 7 dni.", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }

            // Fill missing days
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val today = LocalDate.now()
            val last7Days = (6 downTo 0).map { today.minusDays(it.toLong()) }
            val data = last7Days.map { date ->
                val dateStr = date.format(formatter)
                val count = dataMap[dateStr] ?: 0L
                val label = date.dayOfWeek.name.take(3)
                Pair(label, count)
            }

            val maxCount = data.maxOfOrNull { it.second }?.coerceAtLeast(5L) ?: 5L
            val barColor = MaterialTheme.colorScheme.primary

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(top = 16.dp)
            ) {
                val barWidth = size.width / (data.size * 2f)
                val maxBarHeight = size.height - 40.dp.toPx()

                data.forEachIndexed { index, pair ->
                    val (label, count) = pair
                    val barHeight = (count.toFloat() / maxCount) * maxBarHeight
                    val x = (index * 2 * barWidth) + (barWidth / 2f)
                    val y = size.height - 40.dp.toPx() - barHeight

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )

                    // Draw text (simplified by not using TextMeasurer for compatibility, just drawing basic shape if needed,
                    // but Compose Canvas doesn't have a simple drawText without TextMeasurer.
                    // We can use native canvas or just rely on an overlay.
                    // Actually, a better approach for text is to use Row/Column overlay or TextMeasurer.
                }
            }
            
            // Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.forEach { pair ->
                    Text(
                        text = pair.first,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
