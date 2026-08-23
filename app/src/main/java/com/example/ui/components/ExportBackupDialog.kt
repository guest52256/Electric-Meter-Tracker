package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EnergyCyan
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenBorder
import com.example.util.CsvBackupManager
import com.example.viewmodel.MeterViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportBackupDialog(
    viewModel: MeterViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allReadings by viewModel.allReadings.collectAsStateWithLifecycle()
    val allMeters by viewModel.allMeters.collectAsStateWithLifecycle()
    val allCycles by viewModel.allBillingCycles.collectAsStateWithLifecycle()

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    val dateStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ENGLISH).format(Date())
    val defaultFilename = "meter_readings_all_backup_$dateStamp.csv"

    // Storage Access Framework Document Creator
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            val result = viewModel.exportAllDataToUri(context, uri)
            result.onSuccess {
                isSuccess = true
                statusMessage = "Saved to storage successfully!"
                Toast.makeText(context, "CSV Backup saved to selected storage location", Toast.LENGTH_LONG).show()
            }.onFailure { err ->
                isSuccess = false
                statusMessage = "Save failed: ${err.message}"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .padding(16.dp)
            .testTag("export_backup_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElectricBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Export All Data (CSV)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Slate900
                        )
                        Text(
                            text = "Local Storage Backup & Sheets",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${allReadings.size}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = ElectricBlue
                            )
                            Text(
                                text = "Total Readings",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate600
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(36.dp)
                                .background(Slate300)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${allMeters.size}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = Slate800
                            )
                            Text(
                                text = "Meters Included",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate600
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(36.dp)
                                .background(Slate300)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "100%",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = SuccessGreen
                            )
                            Text(
                                text = "All History",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate600
                            )
                        }
                    }
                }

                if (statusMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSuccess) SuccessGreenBg else Color(0xFFFEE2E2))
                            .border(
                                1.dp,
                                if (isSuccess) SuccessGreenBorder else Color(0xFFFCA5A5),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (isSuccess) SuccessGreen else Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusMessage ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = if (isSuccess) Slate800 else Color(0xFF991B1B)
                            )
                        }
                    }
                }

                // Primary Action: Save to user chosen local directory (Downloads/SD Card)
                Button(
                    onClick = {
                        createDocumentLauncher.launch(defaultFilename)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_save_csv_storage"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save CSV to Phone Storage (Folder)")
                }

                // Secondary Action: Quick Save to Local App Documents
                FilledTonalButton(
                    onClick = {
                        val result = viewModel.exportAllDataToLocalFile(context)
                        result.onSuccess { file ->
                            isSuccess = true
                            statusMessage = "Saved to: ${file.name} in Documents"
                            Toast.makeText(context, "Backup saved to ${file.name}", Toast.LENGTH_SHORT).show()
                        }.onFailure { err ->
                            isSuccess = false
                            statusMessage = "Save failed: ${err.message}"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_quick_save_backup"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Quick Save to Documents Folder")
                }

                // Action Row: Share File + Copy CSV
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val result = viewModel.exportAllDataToLocalFile(context)
                            result.onSuccess { file ->
                                val shareIntent = CsvBackupManager.createShareCsvIntent(context, file)
                                context.startActivity(
                                    android.content.Intent.createChooser(shareIntent, "Share Meter CSV Backup")
                                )
                            }.onFailure {
                                Toast.makeText(context, "Could not create share file", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("btn_share_csv_file"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share File", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val csv = viewModel.getAllDataCsvContent()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = ClipData.newPlainText("Meter All Data CSV", csv)
                            clipboard?.setPrimaryClip(clip)
                            isSuccess = true
                            statusMessage = "CSV copied to clipboard!"
                            Toast.makeText(context, "Copied for Google Sheets / Excel", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("btn_copy_all_csv"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy CSV", fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_close_backup_dialog")
            ) {
                Text("Done")
            }
        }
    )
}
