package sadaqat.kinzadigitalhub.electricmetertracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AddReadingDialog(
    lastReading: Double?,
    onDismiss: () -> Unit,
    onConfirm: (readingValue: Double, notes: String) -> Unit
) {
    var readingText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Record Meter Reading")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (lastReading != null) {
                    Text(
                        text = "Previous Reading: $lastReading kWh",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = readingText,
                    onValueChange = {
                        readingText = it
                        errorText = null
                    },
                    label = { Text("Current Unit / kWh Value") },
                    leadingIcon = {
                        Icon(Icons.Default.Speed, contentDescription = null)
                    },
                    isError = errorText != null,
                    supportingText = errorText?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes (Optional)") },
                    leadingIcon = {
                        Icon(Icons.Default.Notes, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = readingText.toDoubleOrNull()
                    if (value == null) {
                        errorText = "Please enter a valid numeric reading"
                    } else if (lastReading != null && value < lastReading) {
                        errorText = "Reading cannot be lower than previous ($lastReading)"
                    } else {
                        onConfirm(value, notesText.trim())
                    }
                }
            ) {
                Text("Save Reading")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
