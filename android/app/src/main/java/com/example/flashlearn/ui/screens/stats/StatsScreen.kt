package com.example.flashlearn.ui.screens.stats

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flashlearn.R
import com.example.flashlearn.data.remote.dto.StatsDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pdfState by viewModel.pdfState.collectAsState()   // NOWE
    val context = LocalContext.current                     // NOWE

    // NOWE — obsługa wyników pobierania
    LaunchedEffect(pdfState) {
        when (pdfState) {
            is PdfDownloadState.Success -> {
                Toast.makeText(context, "PDF zapisany w Pobrane", Toast.LENGTH_SHORT).show()
                viewModel.resetPdfState()
            }
            is PdfDownloadState.Error -> {
                Toast.makeText(context, (pdfState as PdfDownloadState.Error).message, Toast.LENGTH_LONG).show()
                viewModel.resetPdfState()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_desc_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadStats() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.content_desc_refresh))
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
                            Text(stringResource(R.string.stats_btn_retry))
                        }
                    }
                }
                is StatsUiState.Success -> {
                    StatsContent(
                        stats = state.stats,
                        pdfState = pdfState,                          // NOWE
                        onDownloadPdf = { viewModel.downloadStatsPdf(context) }  // NOWE
                    )
                }
            }
        }
    }
}

@Composable
fun StatsContent(
    stats: StatsDto,
    pdfState: PdfDownloadState,           // NOWE
    onDownloadPdf: () -> Unit             // NOWE
) {
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

        // NOWE — przycisk PDF na dole listy
        Button(
            onClick = onDownloadPdf,
            modifier = Modifier.fillMaxWidth(),
            enabled = pdfState !is PdfDownloadState.Loading
        ) {
            if (pdfState is PdfDownloadState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text("Pobieranie...")
            } else {
                Text("Pobierz raport PDF")
            }
        }
    }
}

// --- reszta composables BEZ ZMIAN ---

@Composable
fun StreakCard(currentStreak: Int, longestStreak: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.stats_streak_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StreakItem(currentStreak, stringResource(R.string.stats_streak_current), MaterialTheme.colorScheme.primary)
                HorizontalDivider(
                    modifier = Modifier.height(50.dp).width(1.dp).align(Alignment.CenterVertically),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )
                StreakItem(longestStreak, stringResource(R.string.stats_streak_best), MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
fun StreakItem(value: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MasteryCard(stats: StatsDto) {
    val total = stats.totalReviewed
    val correct = stats.correctAnswers
    val hard = stats.hardAnswers
    val wrong = stats.wrongAnswers
    val masteryPercent = if (total > 0) (correct.toFloat() / total) * 100 else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.stats_mastery_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(R.string.stats_mastery_subtitle), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val goodColor = Color(0xFF4CAF50)
                    RatingDot(color = goodColor, label = stringResource(R.string.stats_rate_good, correct))
                    RatingDot(color = MaterialTheme.colorScheme.secondary, label = stringResource(R.string.stats_rate_hard, hard))
                    RatingDot(color = MaterialTheme.colorScheme.error, label = stringResource(R.string.stats_rate_wrong, wrong))
                }
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(start = 16.dp)) {
                val goodColor = Color(0xFF4CAF50)
                val secondaryColor = MaterialTheme.colorScheme.secondary
                val errorColor = MaterialTheme.colorScheme.error
                val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                Canvas(modifier = Modifier.size(100.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    drawArc(color = trackColor, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                    if (total > 0) {
                        var start = -90f
                        if (correct > 0) { val sweep = (correct.toFloat() / total) * 360f; drawArc(color = goodColor, startAngle = start, sweepAngle = sweep, useCenter = false, style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)); start += sweep }
                        if (hard > 0)    { val sweep = (hard.toFloat() / total) * 360f;    drawArc(color = secondaryColor, startAngle = start, sweepAngle = sweep, useCenter = false, style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)); start += sweep }
                        if (wrong > 0)   { val sweep = (wrong.toFloat() / total) * 360f;   drawArc(color = errorColor, startAngle = start, sweepAngle = sweep, useCenter = false, style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)) }
                    }
                }
                Text("${masteryPercent.toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun RatingDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, androidx.compose.foundation.shape.CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun BarChartCard(dataMap: Map<String, Long>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.stats_history_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            if (dataMap.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.stats_history_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                return@Column
            }
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val today = LocalDate.now()
            val data = (6 downTo 0).map { today.minusDays(it.toLong()) }.map { date ->
                val label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase()
                Pair(label, dataMap[date.format(formatter)] ?: 0L)
            }
            val maxCount = data.maxOfOrNull { it.second }?.coerceAtLeast(5L) ?: 5L
            Row(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { BarChartItem(label = it.first, count = it.second, maxCount = maxCount, modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun BarChartItem(label: String, count: Long, maxCount: Long, modifier: Modifier = Modifier) {
    val heightFraction = if (maxCount > 0) count.toFloat() / maxCount.toFloat() else 0f
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
        Text(
            text = if (count > 0) count.toString() else "0",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (count > 0) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (count > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier.fillMaxWidth(0.5f).height(120.dp).background(primaryColor.copy(alpha = 0.1f), androidx.compose.foundation.shape.RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (count > 0) {
                Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(heightFraction).background(Brush.verticalGradient(listOf(primaryColor, secondaryColor)), androidx.compose.foundation.shape.RoundedCornerShape(6.dp)))
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}