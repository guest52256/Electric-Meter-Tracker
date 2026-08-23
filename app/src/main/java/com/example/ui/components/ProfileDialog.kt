package com.example.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ads.AdAnalyticsTracker
import com.example.ads.AdManager
import com.example.ads.AdRewardPreferences
import com.example.ads.findActivity
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.MeterViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileDialog(
    viewModel: MeterViewModel,
    onDismiss: () -> Unit,
    onSignOut: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val unitThreshold by viewModel.unitThreshold.collectAsState()
    val context = LocalContext.current
    val adRewardPrefs = remember { AdManager.getAdRewardPreferences(context) }
    val adRewardState by adRewardPrefs.adRewardState.collectAsState()

    var thresholdInput by remember(unitThreshold) {
        mutableStateOf(if (unitThreshold % 1.0 == 0.0) unitThreshold.toInt().toString() else unitThreshold.toString())
    }
    var thresholdSavedMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Picture
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val photoUrl = currentUser?.photoUrl
                    if (photoUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(photoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val initial = currentUser?.displayName?.firstOrNull()?.toString() ?: "G"
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Name
                Text(
                    text = currentUser?.displayName ?: "Google User",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Email
                Text(
                    text = currentUser?.email ?: currentUser?.uid ?: "Anonymous",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sign Up Date
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    val creationTime = currentUser?.metadata?.creationTimestamp
                    val dateString = if (creationTime != null) {
                        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(creationTime))
                    } else {
                        "Active Account"
                    }
                    Text(
                        text = "Member since: $dateString",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ----------------------------------------------------
                // 10 REWARD VIDEO ADS FOR 1 MONTH AD-FREE PASS
                // ----------------------------------------------------
                if (AdManager.oneMonthMemberEnabled) {
                    Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (adRewardState.isMonthExemptActive) {
                            SuccessGreen.copy(alpha = 0.10f)
                        } else {
                            ElectricBlue.copy(alpha = 0.08f)
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (adRewardState.isMonthExemptActive) SuccessGreen.copy(alpha = 0.4f) else ElectricBlue.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (adRewardState.isMonthExemptActive) Icons.Default.Stars else Icons.Default.OndemandVideo,
                                contentDescription = null,
                                tint = if (adRewardState.isMonthExemptActive) SuccessGreen else ElectricBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "1-Month Ad-Free Reward Pass",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (adRewardState.isMonthExemptActive) {
                            // Active Exemption Status
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SuccessGreen.copy(alpha = 0.15f))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "1-Month Ad-Free Mode is ACTIVE!",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = SuccessGreen
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "• Video Reward and Interstitial ads are completely stopped for one month.\n• Only bottom banner ads will run.\n• Pass resets on: ${adRewardState.exemptUntilFormattedDate}.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Reset Date: ${adRewardState.exemptUntilFormattedDate}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = ElectricBlue
                                )
                            }
                        } else {
                            // In-progress ad counter
                            Text(
                                text = "Watch ${AdManager.oneMonthMemberTargetAds} short video ads to stop all Interstitial and Video Reward ads for 1 month! Only small banners will run.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Start
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Counter Badge & Progress
                            val watched = (AdRewardPreferences.REQUIRED_ADS_COUNT - adRewardState.remainingAdsToWatch).coerceAtLeast(0)
                            val progress = (watched.toFloat() / AdRewardPreferences.REQUIRED_ADS_COUNT.toFloat()).coerceIn(0f, 1f)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ads Counter: $watched / ${AdRewardPreferences.REQUIRED_ADS_COUNT}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ElectricBlue
                                )
                                Text(
                                    text = "${adRewardState.remainingAdsToWatch} remaining",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = ElectricBlue,
                                trackColor = ElectricBlue.copy(alpha = 0.2f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Watch Ad Button
                            Button(
                                onClick = {
                                    context.findActivity()?.let { activity ->
                                        AdManager.showRewardedAd(
                                            activity = activity,
                                            ignoreExemption = true,
                                            onUserEarnedReward = { _, _ ->
                                                val newState = adRewardPrefs.recordAdWatched()
                                                if (newState.isMonthExemptActive) {
                                                    Toast.makeText(
                                                        context,
                                                        "🎉 Congratulations! You watched ${AdManager.oneMonthMemberTargetAds} ads. Video & interstitial ads are now disabled for 1 month until ${newState.exemptUntilFormattedDate}!",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Ad completed! Counter decreased: ${newState.remainingAdsToWatch} ad(s) left to unlock 1 month ad-free.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            onAdClosed = { earned ->
                                                if (!earned) {
                                                    Toast.makeText(context, "Ad was closed early. Please watch full ad to count toward your ${AdManager.oneMonthMemberTargetAds}-ad goal.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Watch Ad (${adRewardState.remainingAdsToWatch} left)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ----------------------------------------------------
                // GLOBAL UNIT ALERT THRESHOLD CONFIGURATION
                // ----------------------------------------------------
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = AlertAmber.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AlertAmber,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Alert Unit Threshold",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Set the threshold units to trigger alerts, dashboard notifications, and red tags across the entire app. Synced with cloud.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Current Value & Quick Step Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    val currentVal = thresholdInput.toDoubleOrNull() ?: unitThreshold
                                    val newVal = (currentVal - 10.0).coerceAtLeast(1.0)
                                    thresholdInput = if (newVal % 1.0 == 0.0) newVal.toInt().toString() else newVal.toString()
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "-10 Units", tint = MaterialTheme.colorScheme.primary)
                            }

                            OutlinedTextField(
                                value = thresholdInput,
                                onValueChange = { thresholdInput = it },
                                label = { Text("Threshold (Units)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )

                            IconButton(
                                onClick = {
                                    val currentVal = thresholdInput.toDoubleOrNull() ?: unitThreshold
                                    val newVal = currentVal + 10.0
                                    thresholdInput = if (newVal % 1.0 == 0.0) newVal.toInt().toString() else newVal.toString()
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "+10 Units", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(50.0, 100.0, 150.0, 200.0).forEach { preset ->
                                OutlinedButton(
                                    onClick = {
                                        thresholdInput = preset.toInt().toString()
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${preset.toInt()}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Save & Sync Button
                        Button(
                            onClick = {
                                val parsed = thresholdInput.toDoubleOrNull()
                                if (parsed != null && parsed >= 1.0) {
                                    viewModel.updateUnitThreshold(parsed)
                                    thresholdSavedMessage = "Unit threshold set to ${parsed.toInt()} units & synced to cloud!"
                                    Toast.makeText(context, "Threshold saved: ${parsed.toInt()} units", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please enter a valid positive unit threshold", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AlertAmber),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Save & Sync Threshold",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (thresholdSavedMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = thresholdSavedMessage ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = SuccessGreen,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ----------------------------------------------------
                // AD & DEVICE ANALYTICS CLOUD STATUS
                // ----------------------------------------------------
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = "Ad Tracking",
                                tint = ElectricBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ad & Device Sync Status",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val deviceId = remember { AdAnalyticsTracker.getDeviceId(context) }
                        Text(
                            text = "• Device ID: $deviceId\n• Account Type: ${if (currentUser != null && !currentUser!!.isAnonymous) "Google Member" else "Guest"}\n• Daily ad counts, ad types, device ID & IP are actively tracked & synced to cloud.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "Developer",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Developer Information",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Developed by Sadaqat Ali\n0318-6036054",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        context.findActivity()?.let { activity ->
                            AdManager.showInterstitialAd(activity) { onDismiss() }
                        } ?: onDismiss()
                    }) {
                        Text("Close")
                    }

                    Button(
                        onClick = {
                            context.findActivity()?.let { activity ->
                                AdManager.showInterstitialAd(activity) { onSignOut() }
                            } ?: onSignOut()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
