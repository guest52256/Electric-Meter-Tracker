package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.AdManager
import com.example.ads.BannerAdView
import com.example.ads.findActivity
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.EditBillCycleDialog
import com.example.ui.navigation.Screen
import com.example.ui.theme.AlertAmberBg
import com.example.ui.theme.AlertAmberText
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedBg
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenText
import com.example.viewmodel.MeterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillCycleScreen(
    viewModel: MeterViewModel,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeMeters by viewModel.activeMeters.collectAsStateWithLifecycle()
    val allMeters by viewModel.allMeters.collectAsStateWithLifecycle()
    val billingCycles by viewModel.billingCycles.collectAsStateWithLifecycle()

    val selectedMeterId by viewModel.billCycleMeterId.collectAsStateWithLifecycle()
    val newBillReadingInput by viewModel.billCycleNewReadingInput.collectAsStateWithLifecycle()
    val statusMessage by viewModel.billCycleStatusMessage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.billCycleError.collectAsStateWithLifecycle()

    val editingBillingCycle by viewModel.editingBillingCycle.collectAsStateWithLifecycle()
    val cycleToDelete by viewModel.cycleToDelete.collectAsStateWithLifecycle()

    // Default select first meter if none
    LaunchedEffect(activeMeters) {
        if (selectedMeterId == null && activeMeters.isNotEmpty()) {
            viewModel.selectMeterForCycleUpdate(activeMeters.first().id)
        }
    }

    val selectedMeter = activeMeters.find { it.id == selectedMeterId } ?: activeMeters.firstOrNull()
    val currentCycle = billingCycles.find { it.meterId == selectedMeter?.id }

    var dropdownExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("bill_cycle_screen")
    ) {
        // Header
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
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Update Previous Bill Reading",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Slate900
                )
                Text(
                    text = "نیا بجلی کا بل آنے پر پچھلی بل ریڈنگ اپ ڈیٹ کریں",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Success banner
        AnimatedVisibility(visible = statusMessage != null) {
            statusMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SuccessGreenBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
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
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = SuccessGreenText
                        )
                    }
                }
            }
        }

        // Info Explainer Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AlertAmberBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = AlertAmberText,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "How Billing Cycles Work",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = AlertAmberText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "When your new monthly electricity bill arrives, enter the bill's reading here. Future daily readings will automatically calculate units from this new baseline, resetting the alert cycle to zero.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AlertAmberText.copy(alpha = 0.9f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Form Card or Empty State
        if (activeMeters.isEmpty()) {
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(ElectricBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricMeter,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "No Meters Found",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = Slate900
                    )

                    Text(
                        text = "Please add an electric meter first to configure and update billing cycle readings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate600,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            viewModel.openAddMeterDialog()
                            onNavigate(Screen.METERS)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Meter First")
                    }
                }
            }
        } else {
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
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Meter Dropdown
                    Column {
                        Text(
                            text = "Select Meter to Update",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Slate900
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedMeter?.name ?: "Select a meter",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ElectricMeter,
                                        contentDescription = null,
                                        tint = ElectricBlue
                                    )
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("cycle_meter_dropdown"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricBlue,
                                    unfocusedBorderColor = Slate300
                                )
                            )

                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                activeMeters.forEach { meter ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = meter.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (meter.id == selectedMeter?.id) FontWeight.Bold else FontWeight.Normal
                                                )
                                            )
                                        },
                                        onClick = {
                                            viewModel.selectMeterForCycleUpdate(meter.id)
                                            dropdownExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.ElectricMeter,
                                                contentDescription = null,
                                                tint = if (meter.id == selectedMeter?.id) ElectricBlue else Slate600
                                            )
                                        },
                                        trailingIcon = {
                                            if (meter.id == selectedMeter?.id) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = ElectricBlue
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Current baseline view
                    currentCycle?.let { cycle ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Slate100)
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Current Baseline in Database",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate600
                                    )
                                    Text(
                                        text = "${cycle.previousBillReading} Units",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Slate900
                                    )
                                }
                                Text(
                                    text = "Started: ${cycle.cycleStartFormattedDate.ifBlank { "Initial" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate600
                                )
                            }
                        }
                    }

                    // New Previous Bill Reading Input
                    Column {
                        Text(
                            text = "New Previous Bill Reading (نئے بل کی ریڈنگ)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = newBillReadingInput,
                            onValueChange = { viewModel.onBillCycleReadingInputChanged(it) },
                            placeholder = { Text("e.g. 12680") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_bill_reading_input"),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = Slate300
                            )
                        )

                        errorMessage?.let { err ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Save button
                    Button(
                        onClick = {
                            viewModel.updateBillingCycle {
                                // Cycle updated - show Interstitial Ad
                                context.findActivity()?.let { activity ->
                                    AdManager.showInterstitialAd(activity)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("save_bill_cycle_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Update Bill Cycle Reading",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // All Meters Current Cycles Status
        Text(
            text = "Active Meter Baselines & Controls",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Slate900
        )
        Text(
            text = "Manage, edit baseline units, or reset cycles per meter",
            style = MaterialTheme.typography.bodySmall,
            color = Slate600
        )
        Spacer(modifier = Modifier.height(10.dp))

        billingCycles.forEach { cycle ->
            val meter = allMeters.find { it.id == cycle.meterId }
            if (meter != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("cycle_card_${cycle.meterId}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = meter.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Slate900
                                )
                                Text(
                                    text = "Cycle Start: ${cycle.cycleStartFormattedDate.ifBlank { "Active Cycle" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate600
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Slate100
                            ) {
                                Text(
                                    text = "${cycle.previousBillReading} Units",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = ElectricBlue
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Edit & Reset Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = { viewModel.startEditingBillingCycle(cycle) },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.testTag("btn_edit_cycle_${cycle.meterId}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Baseline",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Edit Baseline",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                onClick = { viewModel.requestDeleteBillingCycle(cycle) },
                                shape = RoundedCornerShape(8.dp),
                                color = AlertRedBg,
                                modifier = Modifier.testTag("btn_reset_cycle_${cycle.meterId}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = "Reset Baseline",
                                        tint = AlertRed,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Reset to 0",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = AlertRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        BannerAdView(modifier = Modifier.padding(top = 12.dp, bottom = 80.dp))
    }

    // Direct Edit Billing Cycle Dialog
    editingBillingCycle?.let { cycle ->
        val meter = allMeters.find { it.id == cycle.meterId }
        val meterName = meter?.name ?: "Meter #${cycle.meterId}"
        EditBillCycleDialog(
            cycle = cycle,
            meterName = meterName,
            viewModel = viewModel,
            onDismiss = { viewModel.dismissEditingBillingCycle() },
            onCycleUpdated = {
                // Cycle updated
            }
        )
    }

    // Reset Billing Cycle Confirmation Dialog
    cycleToDelete?.let { cycle ->
        val meter = allMeters.find { it.id == cycle.meterId }
        val meterName = meter?.name ?: "Meter #${cycle.meterId}"
        DeleteConfirmDialog(
            title = "Reset Billing Cycle Baseline?",
            message = "Are you sure you want to reset the baseline reading for '$meterName' to 0.0 units?",
            warningNote = "Note: Daily readings logged in this cycle will now calculate units relative to 0.0 units.",
            confirmButtonText = "Reset to 0.0",
            onConfirm = {
                viewModel.confirmDeleteBillingCycle()
            },
            onDismiss = { viewModel.dismissDeleteBillingCycleDialog() },
            testTag = "reset_cycle_dialog"
        )
    }
}

