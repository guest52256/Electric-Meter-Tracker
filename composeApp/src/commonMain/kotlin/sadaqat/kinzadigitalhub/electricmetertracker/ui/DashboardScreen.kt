package sadaqat.kinzadigitalhub.electricmetertracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import sadaqat.kinzadigitalhub.electricmetertracker.model.MeterReading
import sadaqat.kinzadigitalhub.electricmetertracker.state.MeterTrackerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(state: MeterTrackerState) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showTariffDialog by remember { mutableStateOf(false) }

    val currentMeter = state.selectedMeter
    val readings = state.currentMeterReadings
    val summary = state.getSummary()
    val currency = state.billingSettings.currencySymbol

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Electric Meter Tracker",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (currentMeter != null) {
                                Text(
                                    text = "${currentMeter.name} (${currentMeter.meterNumber})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showTariffDialog = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Tariff Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Reading") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
        ) {
            // Meters Selector
            if (state.meters.size > 1) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.meters.forEach { meter ->
                            val isSelected = meter.id == state.selectedMeterId
                            FilterChip(
                                selected = isSelected,
                                onClick = { state.selectedMeterId = meter.id },
                                label = { Text(meter.name) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            // Summary Metrics Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Units Used",
                        value = "${formatDec(summary.totalUnitsConsumed)} kWh",
                        subtitle = "Avg ${formatDec(summary.averageDailyUnits)} /day",
                        icon = Icons.Default.ElectricMeter,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Est. Total Bill",
                        value = "$currency${formatDec(summary.totalBillAmount)}",
                        subtitle = "Proj: $currency${formatDec(summary.projectedBillAmount)}",
                        icon = Icons.Default.ReceiptLong,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Cost Breakdown Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Billing Breakdown",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Divider()
                        CostRow("Energy Charges", "$currency${formatDec(summary.energyCharges)}")
                        CostRow("Fixed Charges", "$currency${formatDec(summary.fixedCharges)}")
                        CostRow("Taxes (${state.billingSettings.taxPercentage}%)", "$currency${formatDec(summary.taxAmount)}")
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Current Bill Total",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "$currency${formatDec(summary.totalBillAmount)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Reading History Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reading History (${readings.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (readings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No readings logged yet.\nClick 'New Reading' to get started.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(readings, key = { it.id }) { reading ->
                    ReadingItemCard(
                        reading = reading,
                        onDelete = { state.deleteReading(reading.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        val lastValue = readings.firstOrNull()?.readingValue ?: currentMeter?.initialReading
        AddReadingDialog(
            lastReading = lastValue,
            onDismiss = { showAddDialog = false },
            onConfirm = { value, notes ->
                state.addReading(value, System.currentTimeMillis(), notes)
                showAddDialog = false
            }
        )
    }

    if (showTariffDialog) {
        TariffSettingsDialog(
            settings = state.billingSettings,
            onDismiss = { showTariffDialog = false },
            onSave = { updated ->
                state.billingSettings = updated
                showTariffDialog = false
            }
        )
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.labelMedium, color = contentColor)
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = contentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = contentColor.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun CostRow(label: String, amount: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = amount, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ReadingItemCard(
    reading: MeterReading,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column {
                    Text(
                        text = "${formatDec(reading.readingValue)} kWh",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (reading.notes.isNotBlank()) {
                        Text(
                            text = reading.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete Reading",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatDec(v: Double): String {
    val rounded = (v * 100).toLong() / 100.0
    return rounded.toString()
}
