package com.example.ui.screens

import android.app.Activity
import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.AdManager
import com.example.ads.BannerAdView
import com.example.ads.findActivity
import com.example.ui.components.HighUsageAlertBanner
import com.example.ui.navigation.Screen
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertAmberBg
import com.example.ui.theme.AlertAmberText
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedBg
import com.example.ui.theme.AlertRedBorder
import com.example.ui.theme.AlertRedText
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EnergyCyan
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenText
import com.example.viewmodel.MeterViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReadingScreen(
    viewModel: MeterViewModel,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val unitThreshold by viewModel.unitThreshold.collectAsStateWithLifecycle()
    val activeMeters by viewModel.activeMeters.collectAsStateWithLifecycle()
    val billingCycles by viewModel.billingCycles.collectAsStateWithLifecycle()

    val selectedMeterId by viewModel.addReadingMeterId.collectAsStateWithLifecycle()
    val currentReadingInput by viewModel.addReadingCurrentInput.collectAsStateWithLifecycle()
    val dateString by viewModel.addReadingDate.collectAsStateWithLifecycle()
    val notes by viewModel.addReadingNotes.collectAsStateWithLifecycle()
    val errorMessage by viewModel.addReadingError.collectAsStateWithLifecycle()
    val successMessage by viewModel.addReadingSuccessMessage.collectAsStateWithLifecycle()
    val duplicateWarning by viewModel.addReadingDuplicateWarning.collectAsStateWithLifecycle()

    // Default select first active meter if none selected
    LaunchedEffect(activeMeters) {
        if (selectedMeterId == null && activeMeters.isNotEmpty()) {
            viewModel.selectMeterForAdd(activeMeters.first().id)
        }
    }

    val selectedMeter = activeMeters.find { it.id == selectedMeterId } ?: activeMeters.firstOrNull()
    val currentCycle = billingCycles.find { it.meterId == selectedMeter?.id }
    val previousBillReading = currentCycle?.previousBillReading ?: 0.0

    val currentReadingVal = currentReadingInput.toDoubleOrNull()
    val calculatedUnits = if (currentReadingVal != null) {
        (currentReadingVal - previousBillReading).coerceAtLeast(0.0)
    } else null

    val isInputLowerThanBill = currentReadingVal != null && currentReadingVal < previousBillReading
    val isAlert = calculatedUnits != null && calculatedUnits >= unitThreshold

    var dropdownExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Calendar for DatePicker
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            val format = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            viewModel.onDateChanged(format.format(cal.time))
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(16.dp)
                .testTag("add_reading_screen")
        ) {
            // Title Header
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
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Daily Reading Entry",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Slate900
                    )
                    Text(
                        text = "روزانہ بجلی کے میٹر کی ریڈنگ درج کریں",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Success Feedback Banner
            AnimatedVisibility(visible = successMessage != null) {
                val msg = successMessage ?: ""
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

            // If no active meters, show helpful prompt to add a meter
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
                            text = "No Meters Added Yet",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = Slate900
                        )

                        Text(
                            text = "Please add at least one electric meter and its initial previous bill reading before recording daily readings.",
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
                                imageVector = Icons.Default.ElectricMeter,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Meter First")
                        }
                    }
                }
            } else {
                // Form Card
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
                    // STEP 1: Select Meter Dropdown
                    Column {
                        Text(
                            text = "1. Select Meter (میٹر منتخب کریں)",
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
                                    .testTag("meter_dropdown_field"),
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
                                            viewModel.selectMeterForAdd(meter.id)
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

                    // STEP 2: Automatic Date (Clickable to change)
                    Column {
                        Text(
                            text = "2. Reading Date (تاریخ)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Slate900
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = dateString,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Pick Date",
                                    tint = ElectricBlue,
                                    modifier = Modifier.clickable { datePickerDialog.show() }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { datePickerDialog.show() }
                                .testTag("date_input_field"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = Slate300
                            )
                        )
                    }

                    // STEP 3: Auto-Populated Previous Bill Reading Display
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "3. Previous Bill Reading (پچھلے بل کی ریڈنگ)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Slate900
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Slate100
                            ) {
                                Text(
                                    text = "Auto-Loaded",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Slate600,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Slate100)
                                .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (previousBillReading % 1.0 == 0.0) previousBillReading.toInt().toString() else previousBillReading.toString(),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Slate900
                                    )
                                    Text(
                                        text = "Base reading for current billing cycle",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate600
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Slate600,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // STEP 4: Current Reading Input (User's Only Manual Input)
                    Column {
                        Text(
                            text = "4. Current Reading (موجودہ ریڈنگ)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = currentReadingInput,
                            onValueChange = { viewModel.onCurrentReadingInputChanged(it) },
                            placeholder = { Text("e.g. 12601") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            isError = isInputLowerThanBill || errorMessage != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("current_reading_input"),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = Slate300,
                                errorBorderColor = AlertRed
                            )
                        )

                        // Error Message for lower reading
                        if (isInputLowerThanBill) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = AlertRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Current reading cannot be lower than Previous Bill ($previousBillReading)",
                                    style = MaterialTheme.typography.bodySmall.copy(color = AlertRed)
                                )
                            }
                        }

                        // Duplicate date warning
                        if (duplicateWarning) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = AlertAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "A reading for this date already exists for ${selectedMeter?.name}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = AlertAmberText)
                                )
                            }
                        }
                    }

                    // STEP 5: Live Calculated Consumption Box
                    if (calculatedUnits != null && !isInputLowerThanBill) {
                        val unitsDisplay = if (calculatedUnits % 1.0 == 0.0) calculatedUnits.toInt().toString() else "%.1f".format(calculatedUnits)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isAlert) AlertRedBg else Slate100)
                                .border(
                                    1.5.dp,
                                    if (isAlert) AlertRedBorder else ElectricBlue.copy(alpha = 0.3f),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(16.dp)
                                .testTag("calculated_units_preview")
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Auto Calculated Formula",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isAlert) AlertRedText else Slate600
                                    )
                                    Text(
                                        text = if (isAlert) "🔴 HIGH ALERT (>= ${unitThreshold.toInt()})" else "🟢 NORMAL USAGE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                        color = if (isAlert) AlertRed else SuccessGreenText
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Units Since Bill = $currentReadingVal − $previousBillReading = $unitsDisplay Units",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAlert) AlertRedText else Slate900
                                    )
                                )
                            }
                        }
                    }

                    // High Usage Alert Banner Preview
                    if (isAlert && calculatedUnits != null) {
                        HighUsageAlertBanner(
                            units = calculatedUnits,
                            unitThreshold = unitThreshold,
                            meterName = selectedMeter?.name
                        )
                    }

                    // Notes (Optional)
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { viewModel.onNotesChanged(it) },
                        label = { Text("Notes / Remarks (Optional)") },
                        placeholder = { Text("e.g. Afternoon reading, AC usage") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = Slate300
                        )
                    )

                    // Big Save Reading Button
                    Button(
                        onClick = {
                            viewModel.submitDailyReading {
                                // On successful reading save, show instant confirmation and handle centralized ad action
                                android.widget.Toast.makeText(context, "Reading Saved Successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                context.findActivity()?.let { activity ->
                                    AdManager.handleAction(activity, "add_reading")
                                }
                            }
                        },
                        enabled = currentReadingInput.isNotBlank() && !isInputLowerThanBill,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("save_reading_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAlert) AlertRed else ElectricBlue,
                            disabledContainerColor = Slate300
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Daily Reading",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            BannerAdView(
                modifier = Modifier.padding(bottom = 80.dp),
                bannerId = "banner_add_reading"
            )
        }
    }
}
}

