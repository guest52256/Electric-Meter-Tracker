package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.SyncState
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.Slate600
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.MeterViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FirebaseSyncBanner(
    viewModel: MeterViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    val (bgColor, borderColor, iconColor, titleColor, iconVector) = when (syncStatus.state) {
        SyncState.SYNCED -> Quintuple(
            Color(0xFFF0FDF4),
            Color(0xFFBBF7D0),
            SuccessGreen,
            Color(0xFF166534),
            Icons.Default.CloudDone
        )
        SyncState.SYNCING -> Quintuple(
            Color(0xFFFFFBEB),
            Color(0xFFFDE68A),
            Color(0xFFD97706),
            Color(0xFFB45309),
            Icons.Default.CloudSync
        )
        SyncState.OFFLINE -> Quintuple(
            Color(0xFFFEF2F2),
            Color(0xFFFECACA),
            Color(0xFFDC2626),
            Color(0xFF991B1B),
            Icons.Default.CloudOff
        )
        SyncState.ERROR -> Quintuple(
            Color(0xFFFFF7ED),
            Color(0xFFFED7AA),
            Color(0xFFEA580C),
            Color(0xFFC2410C),
            Icons.Default.SyncProblem
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("firebase_sync_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = "Cloud Sync Status",
                        tint = iconColor,
                        modifier = Modifier
                            .size(20.dp)
                            .then(if (syncStatus.state == SyncState.SYNCING) Modifier.rotate(rotation) else Modifier)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    val titleText = when (syncStatus.state) {
                        SyncState.SYNCED -> "🟢 All data synced"
                        SyncState.SYNCING -> "🟠 Syncing..."
                        SyncState.OFFLINE -> "🔴 Offline — Data saved locally"
                        SyncState.ERROR -> "⚠️ Sync issue detected"
                    }

                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = titleColor
                    )

                    val timeFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
                    val subtitleText = when {
                        !syncStatus.isOnline -> "Offline mode active. Readings are safely saved to device Room database."
                        syncStatus.state == SyncState.SYNCING -> "Uploading and merging records securely..."
                        syncStatus.pendingCount > 0 -> "${syncStatus.pendingCount} record(s) queued for synchronization."
                        syncStatus.lastSyncedAt != null -> "Synced at ${timeFormat.format(Date(syncStatus.lastSyncedAt!!))}"
                        else -> "Secure Cloud connected"
                    }

                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp
                        ),
                        color = Slate600
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Sync Details",
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
