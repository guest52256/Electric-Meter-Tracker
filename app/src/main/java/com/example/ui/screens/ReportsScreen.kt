package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.BannerAdView
import com.example.ui.components.ExportBackupDialog
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedBg
import com.example.ui.theme.AlertRedBorder
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EnergyCyan
import com.example.ui.theme.EnergyCyanLight
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.MeterViewModel

@Composable
fun ReportsScreen(
    viewModel: MeterViewModel,
    modifier: Modifier = Modifier
) {
    val allMeters by viewModel.allMeters.collectAsStateWithLifecycle()
    val allReadings by viewModel.allReadings.collectAsStateWithLifecycle()
    val dashboardOverview by viewModel.dashboardOverview.collectAsStateWithLifecycle()
    val unitThreshold by viewModel.unitThreshold.collectAsStateWithLifecycle()

    var selectedMeterId by remember { mutableStateOf<Long?>(null) } // null = All
    var showExportBackupDialog by remember { mutableStateOf(false) }

    val filteredReadings = if (selectedMeterId == null) {
        allReadings
    } else {
        allReadings.filter { it.meterId == selectedMeterId }
    }

    // Calculations
    val totalUnits = dashboardOverview.meterCards
        .filter { selectedMeterId == null || it.meter.id == selectedMeterId }
        .sumOf { it.unitsSinceBill }

    val activeAlerts = dashboardOverview.meterCards
        .filter { selectedMeterId == null || it.meter.id == selectedMeterId }
        .count { it.isAlert }

    val recentDaysReadings = filteredReadings
        .sortedBy { it.timestamp }
        .takeLast(7)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Consumption Analytics",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Slate900
                        )
                        Text(
                            text = "ماہانہ اور میٹر کے لحاظ سے استعمال کا تفصیلی تجزیہ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    }
                }

                IconButton(
                    onClick = { showExportBackupDialog = true },
                    modifier = Modifier.testTag("btn_reports_export_csv")
                ) {
                    Icon(
                        Icons.Default.FileDownload,
                        contentDescription = "Export All CSV Data",
                        tint = ElectricBlue
                    )
                }
            }
        }

        // CSV Export & Backup Banner Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_reports_export_banner"),
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📁 CSV Data Backup (Local Storage)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Slate900
                        )
                        Text(
                            text = "Export complete meter readings and baseline history for offline backup or Google Sheets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { showExportBackupDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_card_export_csv")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export CSV", fontSize = 12.sp)
                    }
                }
            }
        }

        // Meter Filter Chips
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedMeterId == null,
                        onClick = { selectedMeterId = null },
                        label = { Text("Combined (All)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                items(allMeters, key = { "report_filter_meter_${it.id}" }) { meter ->
                    FilterChip(
                        selected = selectedMeterId == meter.id,
                        onClick = { selectedMeterId = meter.id },
                        label = { Text(meter.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Key KPI Stat Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Units Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Insights,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cycle Total",
                                style = MaterialTheme.typography.labelMedium,
                                color = Slate600
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${totalUnits.toInt()} Units",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = if (totalUnits >= unitThreshold) AlertRed else ElectricBlue
                            )
                        )
                    }
                }

                // Alerts Status Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (activeAlerts > 0) AlertRedBorder else Slate200
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (activeAlerts > 0) Icons.Default.Warning else Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = if (activeAlerts > 0) AlertRed else SuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Alert Status",
                                style = MaterialTheme.typography.labelMedium,
                                color = Slate600
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (activeAlerts > 0) "$activeAlerts Exceeded ${unitThreshold.toInt()}" else "Normal",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (activeAlerts > 0) AlertRed else SuccessGreen
                            )
                        )
                    }
                }
            }
        }

        // Daily Consumption Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Units Consumption Trend",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Slate900
                    )
                    Text(
                        text = "Progressive units consumed in current billing cycle",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (recentDaysReadings.isNotEmpty()) {
                        ConsumptionBarChart(
                            readings = recentDaysReadings,
                            unitThreshold = unitThreshold
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No readings available for chart",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate600
                            )
                        }
                    }
                }
            }
        }

        // Meter Breakdown List
        item {
            Text(
                text = "Meter-by-Meter Summary",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Slate900
            )
        }

        items(dashboardOverview.meterCards, key = { "report_meter_card_${it.meter.id}" }) { card ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (card.isAlert) AlertRedBorder else Slate200
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = card.meter.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Slate900
                        )
                        Text(
                            text = "Current: ${card.currentReading} • Prev Bill: ${card.previousBillReading}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${card.unitsSinceBill.toInt()} Units",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = if (card.isAlert) AlertRed else ElectricBlue
                            )
                        )
                        Text(
                            text = if (card.isAlert) "🔴 Alert (>= ${unitThreshold.toInt()})" else "🟢 Normal",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (card.isAlert) AlertRed else SuccessGreen
                        )
                    }
                }
            }
        }

        // Anchored Banner Ad
        item {
            BannerAdView(
                modifier = Modifier.padding(top = 12.dp, bottom = 80.dp),
                bannerId = "banner_reports"
            )
        }
    }

    if (showExportBackupDialog) {
        ExportBackupDialog(
            viewModel = viewModel,
            onDismiss = { showExportBackupDialog = false }
        )
    }
}

@Composable
fun ConsumptionBarChart(
    readings: List<com.example.model.DailyReading>,
    unitThreshold: Double,
    modifier: Modifier = Modifier
) {
    val maxUnits = (readings.maxOfOrNull { it.unitsSinceBill } ?: unitThreshold).coerceAtLeast(unitThreshold * 1.2)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val barCount = readings.size
            val barSpacing = 16.dp.toPx()
            val totalSpacing = barSpacing * (barCount + 1)
            val availableWidth = size.width - totalSpacing
            val barWidth = (availableWidth / barCount).coerceAtMost(48.dp.toPx())

            // threshold units threshold line
            val thresholdY = size.height * (1f - (unitThreshold / maxUnits).toFloat())
            drawLine(
                color = AlertRed.copy(alpha = 0.6f),
                start = Offset(0f, thresholdY),
                end = Offset(size.width, thresholdY),
                strokeWidth = 2.dp.toPx()
            )

            readings.forEachIndexed { index, reading ->
                val barHeight = (size.height * (reading.unitsSinceBill / maxUnits).toFloat()).coerceAtLeast(8.dp.toPx())
                val left = barSpacing + index * (barWidth + barSpacing)
                val top = size.height - barHeight

                val isExceeded = reading.unitsSinceBill >= unitThreshold

                drawRoundRect(
                    color = if (isExceeded) AlertRed else ElectricBlue,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chart Legend & X Axis Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            readings.forEach { reading ->
                Text(
                    text = reading.dateString.split(" ").take(2).joinToString(" "),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Slate600
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AlertRed)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Red Dashed Line: ${unitThreshold.toInt()} Units Alert Threshold",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = AlertRed
            )
        }
    }
}
