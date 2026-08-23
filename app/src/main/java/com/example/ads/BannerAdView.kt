package com.example.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Jetpack Compose Composable for displaying an AdMob Banner Ad.
 * Respects dynamic admin switches and unit ID overrides.
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    bannerId: String = "banner_general",
    adSize: AdSize = AdSize.BANNER
) {
    val isInPreview = LocalInspectionMode.current
    val context = LocalContext.current

    val showBanner = remember { AdManager.shouldShowBannerAdForScreen(context, bannerId) }
    if (!showBanner) {
        // Return an empty box that takes no space if banners are disabled by Admin
        return
    }

    if (isInPreview) {
        // Preview placeholder in Studio / Tools
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AdMob Banner Ad Preview ($bannerId)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val adView = remember {
        AdView(context).apply {
            setAdSize(adSize)
            adUnitId = AdManager.getResolvedBannerAdUnitId()
            adListener = object : AdListener() {
                override fun onAdImpression() {
                    super.onAdImpression()
                    AdAnalyticsTracker.logAdImpression(context, "BANNER")
                }

                override fun onAdLoaded() {
                    super.onAdLoaded()
                    AdAnalyticsTracker.logAdImpression(context, "BANNER")
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    AdAnalyticsTracker.logAdImpression(context, "BANNER_CLICK")
                    context.findActivity()?.let { activity ->
                        val clickActionKey = "click_$bannerId"
                        AdManager.handleAction(activity, clickActionKey)
                    }
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { adView }
        )
    }
}
