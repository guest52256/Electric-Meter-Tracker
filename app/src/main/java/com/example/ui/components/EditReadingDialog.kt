package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.model.DailyReading
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedBg
import com.example.ui.theme.AlertRedBorder
import com.example.ui.theme.AlertRedText
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenBorder
import com.example.ui.theme.SuccessGreenText
import com.example.viewmodel.MeterViewModel

@Composable
fun EditReadingDialog(
    reading: DailyReading,
    viewModel: MeterViewModel,
    onDismiss: () -> Unit,
    onReadingUpdated: (DailyReading) -> Unit
) {
    val currentInput by viewModel.editReadingCurrentInput.collectAsStateWithLifecycle()
    val dateInput by viewModel.editReadingDate.collectAsStateWithLifecycle()
    val notesInput by viewModel.editReadingNotes.collectAsStateWithLifecycle()
    val errorMsg by viewModel.editReadingError.collectAsStateWithLifecycle()

    val currentNum = currentInput.toDoubleOrNull()
    val previousBill = reading.previousBillReading
    val liveUnits = if (currentNum != null) (currentNum - previousBill).coerceAtLeast(0.0) else 0.0
    val liveAlert = liveUnits >= 100.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Edit Meter Reading",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = reading.meterName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info Summary Card (Previous Bill reference)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Base / Prev Bill Reading",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$previousBill",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Live calculated units tag
                        if (currentNum != null && currentNum >= previousBill) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (liveAlert) AlertRedBg else SuccessGreenBg,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (liveAlert) AlertRedBorder else SuccessGreenBorder
                                )
                            ) {
                                Text(
                                    text = if (liveAlert) "🔴 ${liveUnits.toInt()} Units (ALERT)" else "🟢 ${liveUnits.toInt()} Units",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (liveAlert) AlertRedText else SuccessGreenText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Current Reading Input
                OutlinedTextField(
                    value = currentInput,
                    onValueChange = { viewModel.onEditReadingInputChanged(it) },
                    label = { Text("Current Meter Reading") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_reading_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Date Input
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { viewModel.onEditReadingDateChanged(it) },
                    label = { Text("Reading Date (e.g. 15 Aug 2026)") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Slate500
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_reading_date_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Notes Input
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { viewModel.onEditReadingNotesChanged(it) },
                    label = { Text("Notes / Remarks (Optional)") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Notes,
                            contentDescription = null,
                            tint = Slate500
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_reading_notes_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                // Error Message Display
                errorMsg?.let { msg ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = AlertRedBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AlertRedBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = AlertRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = AlertRedText
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.submitEditReading { updated ->
                        onReadingUpdated(updated)
                    }
                },
                modifier = Modifier.testTag("btn_save_edit_reading"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_edit_reading")
            ) {
                Text("Cancel")
            }
        }
    )
}
