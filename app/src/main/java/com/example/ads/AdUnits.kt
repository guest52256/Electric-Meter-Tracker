package com.example.ads

/**
 * AdMob Configuration & Ad Unit IDs.
 *
 * =============================================================================================
 * INSTRUCTIONS FOR APPLYING ORIGINAL / PRODUCTION AD UNITS:
 * =============================================================================================
 * 1. Set `USE_TEST_ADS = false` when you are ready to publish your app to the Play Store.
 * 2. Replace the `PROD_*` constants below with your real AdMob Ad Unit IDs from your AdMob console.
 * 3. Also update `com.google.android.gms.ads.APPLICATION_ID` in `AndroidManifest.xml` with your
 *    official AdMob App ID (e.g. ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY).
 * =============================================================================================
 */
object AdUnits {

    /**
     * Set this to FALSE when deploying the application to production with real AdMob ads.
     * When TRUE, official Google test ad unit IDs will be used to prevent policy violations.
     */
    const val USE_TEST_ADS = false

    // =========================================================================================
    // 1. GOOGLE OFFICIAL TEST AD UNIT IDS (Safe for development & testing)
    // =========================================================================================
    private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // =========================================================================================
    // 2. PRODUCTION AD UNIT IDS (Put your original AdMob Ad Unit IDs here)
    // =========================================================================================
    private const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-3950731807963252/6891016409"
    private const val PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3950731807963252/5589492691"
    private const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-3950731807963252/6108367443"

    // =========================================================================================
    // RESOLVED AD UNIT GETTERS (Automatically selects Test or Production based on USE_TEST_ADS)
    // =========================================================================================
    val BANNER_AD_UNIT_ID: String
        get() = if (USE_TEST_ADS) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID

    val INTERSTITIAL_AD_UNIT_ID: String
        get() = if (USE_TEST_ADS) TEST_INTERSTITIAL_AD_UNIT_ID else PROD_INTERSTITIAL_AD_UNIT_ID

    val REWARDED_AD_UNIT_ID: String
        get() = if (USE_TEST_ADS) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID
}
