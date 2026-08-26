package sadaqat.kinzadigitalhub.electricmetertracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import sadaqat.kinzadigitalhub.electricmetertracker.model.BillingSettings

@Composable
fun TariffSettingsDialog(
    settings: BillingSettings,
    onDismiss: () -> Unit,
    onSave: (BillingSettings) -> Unit
) {
    var fixedChargesText by remember { mutableStateOf(settings.fixedCharges.toString()) }
    var taxPercentText by remember { mutableStateOf(settings.taxPercentage.toString()) }
    var currencyText by remember { mutableStateOf(settings.currencySymbol) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Tariff & Billing Settings")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = currencyText,
                    onValueChange = { currencyText = it },
                    label = { Text("Currency Symbol (e.g. $, ₹, £)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = fixedChargesText,
                    onValueChange = { fixedChargesText = it },
                    label = { Text("Fixed Base Charges") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = taxPercentText,
                    onValueChange = { taxPercentText = it },
                    label = { Text("Tax / Duty Percentage (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fixed = fixedChargesText.toDoubleOrNull() ?: settings.fixedCharges
                    val tax = taxPercentText.toDoubleOrNull() ?: settings.taxPercentage
                    onSave(
                        settings.copy(
                            currencySymbol = currencyText.trim().ifEmpty { "$" },
                            fixedCharges = fixed,
                            taxPercentage = tax
                        )
                    )
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
