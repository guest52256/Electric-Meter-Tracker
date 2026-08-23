package com.example.data.models

data class AdminSettings(
    val adminEmail: String = "TechSadaqat@gmail.com",
    val adsEnabled: Boolean = true,
    val guestAdsEnabled: Boolean = true,
    val googleUserAdsEnabled: Boolean = true,
    val bannerAdsEnabled: Boolean = true,
    val interstitialAdsEnabled: Boolean = true,
    val rewardedAdsEnabled: Boolean = true,
    val bannerAdUnitId: String = "",
    val interstitialAdUnitId: String = "",
    val rewardedAdUnitId: String = "",
    val interstitialIntervalMinutes: Int = 3,
    val oneMonthMemberEnabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

data class ButtonAdConfig(
    val buttonId: String = "",
    val buttonName: String = "",
    val screenName: String = "",
    val adType: String = "none", // none, banner, interstitial, rewarded
    val enabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
