package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedBg
import com.example.ui.theme.AlertRedBorder
import com.example.ui.theme.AlertRedText

@Composable
fun HighUsageAlertBanner(
    units: Double,
    meterName: String? = null,
    modifier: Modifier = Modifier
) {
    val isAlert = units >= 100.0
    val unitsDisplay = if (units % 1.0 == 0.0) units.toInt().toString() else "%.1f".format(units)

    AnimatedVisibility(
        visible = isAlert,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AlertRedBg)
                .border(2.dp, AlertRedBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
                .testTag("high_usage_alert_banner")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AlertRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alert Icon",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⚠️ HIGH USAGE ALERT",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        ),
                        color = AlertRed
                    )
                    Text(
                        text = "$unitsDisplay Units Used Since Last Bill",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = AlertRedText
                    )
                    if (!meterName.isNullOrBlank()) {
                        Text(
                            text = "Meter: $meterName",
                            style = MaterialTheme.typography.labelMedium,
                            color = AlertRedText.copy(alpha = 0.85f)
                        )
                    }
                    Text(
                        text = "حالیہ بل سائیکل میں 100 یا اس سے زیادہ یونٹس استعمال ہو چکے ہیں",
                        style = MaterialTheme.typography.bodySmall,
                        color = AlertRedText.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
