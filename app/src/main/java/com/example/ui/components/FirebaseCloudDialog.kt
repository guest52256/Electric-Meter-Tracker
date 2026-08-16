package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.AdManager
import com.example.ads.findActivity
import com.example.data.firebase.SyncState
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.MeterViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FirebaseCloudDialog(
    viewModel: MeterViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("firebase_cloud_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElectricBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (syncStatus.lastSyncedAt != null) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                contentDescription = "Cloud Sync",
                                tint = if (syncStatus.lastSyncedAt != null) SuccessGreen else ElectricBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Secure Data Sync",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = Slate900
                            )
                            Text(
                                text = "Encrypted Connection",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = ElectricBlue
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_cloud_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Slate600
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Firebase Auth User Card
                val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
                val context = androidx.compose.ui.platform.LocalContext.current

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (currentUser != null) SuccessGreen else ElectricBlue,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (currentUser != null) "G" else "?",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = currentUser?.displayName ?: if (currentUser != null) "Google Account" else "Offline / Guest Mode",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                        color = Slate900
                                    )
                                    Text(
                                        text = currentUser?.email ?: "Not signed in to Google",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Slate600
                                    )
                                }
                            }

                            if (currentUser != null) {
                                OutlinedButton(
                                    onClick = {
                                        context.findActivity()?.let { activity ->
                                            AdManager.showInterstitialAd(activity) {
                                                viewModel.signOut(context)
                                            }
                                        } ?: viewModel.signOut(context)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Sign Out", fontSize = 11.sp, color = Slate800)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        context.findActivity()?.let { activity ->
                                            AdManager.showInterstitialAd(activity) {
                                                viewModel.signInWithGoogle(context)
                                            }
                                        } ?: viewModel.signInWithGoogle(context)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Sign In", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Network & Sync State Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when (syncStatus.state) {
                                SyncState.SYNCED -> Color(0xFFF0FDF4)
                                SyncState.SYNCING -> Color(0xFFFFFBEB)
                                SyncState.OFFLINE -> Color(0xFFFEF2F2)
                                SyncState.ERROR -> Color(0xFFFFF7ED)
                            }
                        )
                        .border(
                            1.dp,
                            when (syncStatus.state) {
                                SyncState.SYNCED -> Color(0xFFBBF7D0)
                                SyncState.SYNCING -> Color(0xFFFDE68A)
                                SyncState.OFFLINE -> Color(0xFFFECACA)
                                SyncState.ERROR -> Color(0xFFFED7AA)
                            },
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (syncStatus.state) {
                                    SyncState.SYNCED -> SuccessGreen
                                    SyncState.SYNCING -> Color(0xFFD97706)
                                    SyncState.OFFLINE -> Color(0xFFDC2626)
                                    SyncState.ERROR -> Color(0xFFEA580C)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = syncStatus.displayStatusText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = when (syncStatus.state) {
                            SyncState.SYNCED -> Color(0xFF166534)
                            SyncState.SYNCING -> Color(0xFFB45309)
                            SyncState.OFFLINE -> Color(0xFF991B1B)
                            SyncState.ERROR -> Color(0xFFC2410C)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Local-First Info Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate100)
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = "Database",
                                tint = ElectricBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Local-First & Multi-Device Sync",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = Slate900
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Works 100% offline: entries are saved instantly to local Room database.\n• Automatic two-way cloud sync: changes made here or on other devices synchronize seamlessly.\n• Conflict handling: versioned timestamps prevent duplicate readings.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                lineHeight = 17.sp,
                                fontSize = 12.sp
                            ),
                            color = Slate600
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Firestore Collections Checklist
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Cloud Data Synced:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                    )
                    listOf(
                        "Meters" to "Meter names & active states",
                        "Billing" to "Starting bill readings per meter",
                        "Readings" to "Daily entries & limit alert status",
                        "Settings" to "Global alert threshold"
                    ).forEach { (coll, desc) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                tint = SuccessGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$coll: ",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = Slate900
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Slate600
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val timeFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)
                val lastSyncText = syncStatus.lastSyncedAt?.let { "Last cloud sync: ${timeFormat.format(Date(it))}" } ?: "Cloud status: Connected & listening"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = lastSyncText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate600
                    )
                    Text(
                        text = "Device: ${syncStatus.deviceId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate600
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            context.findActivity()?.let { activity ->
                                AdManager.showInterstitialAd(activity) {
                                    viewModel.forcePushAllData(context)
                                }
                            } ?: viewModel.forcePushAllData(context)
                        },
                        enabled = !syncStatus.isSyncing,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("dialog_force_push_button")
                    ) {
                        if (syncStatus.isSyncing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Uploading All Records...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Upload",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Push All Records to Cloud")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                context.findActivity()?.let { activity ->
                                    AdManager.showInterstitialAd(activity) {
                                        viewModel.performCombinedSyncAndUpload(context)
                                    }
                                } ?: viewModel.performCombinedSyncAndUpload(context)
                            },
                            enabled = !syncStatus.isSyncing,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync & Upload",
                                modifier = Modifier.size(16.dp),
                                tint = Slate800
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync & Upload", color = Slate800, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(0.7f)
                                .height(40.dp)
                        ) {
                            Text("Close", color = Slate800, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
