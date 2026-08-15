package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Meter
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.EditMeterDialog
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedBg
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenText
import com.example.viewmodel.MeterViewModel

@Composable
fun MetersScreen(
    viewModel: MeterViewModel,
    modifier: Modifier = Modifier
) {
    val allMeters by viewModel.allMeters.collectAsStateWithLifecycle()
    val billingCycles by viewModel.billingCycles.collectAsStateWithLifecycle()
    val allReadings by viewModel.allReadings.collectAsStateWithLifecycle()

    val showAddDialog by viewModel.showAddMeterDialog.collectAsStateWithLifecycle()
    val newMeterName by viewModel.newMeterName.collectAsStateWithLifecycle()
    val newMeterInitialBill by viewModel.newMeterInitialBillReading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.meterManagementError.collectAsStateWithLifecycle()
    val successMessage by viewModel.meterManagementSuccess.collectAsStateWithLifecycle()

    val editingMeter by viewModel.editingMeter.collectAsStateWithLifecycle()
    val meterToDelete by viewModel.meterToDelete.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize().testTag("meters_screen")) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricMeter,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Meter Management",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Slate900
                        )
                        Text(
                            text = "میٹر کی فہرست، ترمیم، ڈیلیٹ اور ایکٹو کا انتظام کریں",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    }
                }
            }

            // Success feedback
            if (successMessage != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SuccessGreenBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = successMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = SuccessGreenText
                            )
                        }
                    }
                }
            }

            // Explainer notice
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Slate100,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Use Edit to update name or status. Deactivating hides a meter from daily entry while preserving history. Delete removes the meter and all its records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    }
                }
            }

            // List of Meters or Empty State
            if (allMeters.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(ElectricBlue.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ElectricMeter,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Text(
                                text = "No Meters Added",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = Slate900
                            )

                            Text(
                                text = "Create your first electric meter with its initial bill reading to start tracking daily readings.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate600,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = { viewModel.openAddMeterDialog() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Meter")
                            }
                        }
                    }
                }
            } else {
                items(allMeters, key = { "meter_item_${it.id}" }) { meter ->
                    val cycle = billingCycles.find { it.meterId == meter.id }
                    val readingsCount = allReadings.count { it.meterId == meter.id }

                    MeterListItemCard(
                        meter = meter,
                        cycleReading = cycle?.previousBillReading ?: 0.0,
                        readingsCount = readingsCount,
                        onToggleActive = { viewModel.toggleMeterStatus(meter) },
                        onEdit = { viewModel.startEditingMeter(meter) },
                        onDelete = { viewModel.requestDeleteMeter(meter) }
                    )
                }
            }
        }

        // FAB to add meter
        FloatingActionButton(
            onClick = { viewModel.openAddMeterDialog() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_add_meter"),
            containerColor = ElectricBlue,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Meter")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add New Meter", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Add New Meter Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAddMeterDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ElectricMeter,
                        contentDescription = null,
                        tint = ElectricBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add New Meter",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Enter meter details to start tracking its daily readings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )

                    OutlinedTextField(
                        value = newMeterName,
                        onValueChange = { viewModel.onNewMeterNameChanged(it) },
                        label = { Text("Meter Name / Owner Name *") },
                        placeholder = { Text("e.g. Ali Ahmed S/O Tariq") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_meter_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = Slate300
                        )
                    )

                    OutlinedTextField(
                        value = newMeterInitialBill,
                        onValueChange = { viewModel.onNewMeterInitialBillReadingChanged(it) },
                        label = { Text("Initial Previous Bill Reading") },
                        placeholder = { Text("e.g. 10500") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_meter_bill_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = Slate300
                        )
                    )

                    errorMessage?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveNewMeter {
                            // Meter saved
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    modifier = Modifier.testTag("btn_save_new_meter")
                ) {
                    Text("Save Meter", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAddMeterDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Meter Dialog
    editingMeter?.let { meter ->
        EditMeterDialog(
            meter = meter,
            viewModel = viewModel,
            onDismiss = { viewModel.dismissEditingMeter() },
            onMeterUpdated = {
                // Meter updated successfully
            }
        )
    }

    // Delete Meter Confirmation Dialog
    meterToDelete?.let { meter ->
        val readingsCount = allReadings.count { it.meterId == meter.id }
        DeleteConfirmDialog(
            title = "Delete Meter?",
            message = "Are you sure you want to delete '${meter.name}'?",
            warningNote = "Warning: Deleting this meter will permanently remove its baseline billing cycle and all $readingsCount logged daily readings from both local storage and cloud sync.",
            confirmButtonText = "Delete Meter",
            onConfirm = {
                viewModel.confirmDeleteMeter()
            },
            onDismiss = { viewModel.dismissDeleteMeterDialog() },
            testTag = "delete_meter_dialog"
        )
    }
}

@Composable
fun MeterListItemCard(
    meter: Meter,
    cycleReading: Double,
    readingsCount: Int,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("meter_list_card_${meter.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (meter.isActive) MaterialTheme.colorScheme.surface else Slate100
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = if (meter.isActive) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (meter.isActive) ElectricBlue.copy(alpha = 0.1f) else Slate200),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricMeter,
                            contentDescription = null,
                            tint = if (meter.isActive) ElectricBlue else Slate600,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = meter.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (meter.isActive) Slate900 else Slate600
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Base Bill: $cycleReading • $readingsCount readings logged",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    }
                }

                // Active toggle switch
                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked = meter.isActive,
                        onCheckedChange = { onToggleActive() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ElectricBlue,
                            uncheckedThumbColor = Slate600,
                            uncheckedTrackColor = Slate200
                        ),
                        modifier = Modifier.testTag("switch_meter_active_${meter.id}")
                    )
                    Text(
                        text = if (meter.isActive) "Active" else "Inactive",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (meter.isActive) SuccessGreenText else Slate600
                        )
                    )
                }
            }

            // Action row with Edit and Delete buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit button
                Surface(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.testTag("btn_edit_meter_${meter.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Meter",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Delete button
                Surface(
                    onClick = onDelete,
                    shape = RoundedCornerShape(8.dp),
                    color = AlertRedBg,
                    modifier = Modifier.testTag("btn_delete_meter_${meter.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Meter",
                            tint = AlertRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = AlertRed
                        )
                    }
                }
            }
        }
    }
}

