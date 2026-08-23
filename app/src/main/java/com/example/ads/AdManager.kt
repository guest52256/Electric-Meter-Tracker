package com.example.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.firebase.FirebaseInitializer
import com.example.data.models.ButtonAdConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

/**
 * Extension helper to safely find Activity from any Context or ContextWrapper in Compose.
 */
fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * Centralized AdManager for managing dynamic remote configurations,
 * button-level settings, AdMob overrides, and global interstitial cooldowns.
 */
object AdManager {

    private const val TAG = "AdManager"

    // Dynamic Admin Settings (with default fallbacks)
    var adsEnabled = true
    var guestAdsEnabled = true
    var googleUserAdsEnabled = true
    var bannerAdsEnabled = true
    var interstitialAdsEnabled = true
    var rewardedAdsEnabled = true

    var bannerAdUnitId = "ca-app-pub-3950731807963252/6891016409"
    var interstitialAdUnitId = "ca-app-pub-3950731807963252/5589492691"
    var rewardedAdUnitId = "ca-app-pub-3950731807963252/6108367443"
    var admobAppId = "ca-app-pub-3950731807963252~4887066950"

    var interstitialIntervalMinutes = 3
    var oneMonthMemberEnabled = true
    var oneMonthMemberTargetAds = 10
    var hasShownSplash = false

    // Real-time listener registrations
    private var adminSettingsListener: ListenerRegistration? = null
    private var buttonAdConfigsListener: ListenerRegistration? = null

    data class AdminSettings(
        val adsEnabled: Boolean,
        val guestAdsEnabled: Boolean,
        val googleUserAdsEnabled: Boolean,
        val bannerAdsEnabled: Boolean,
        val interstitialAdsEnabled: Boolean,
        val rewardedAdsEnabled: Boolean,
        val bannerAdUnitId: String,
        val interstitialAdUnitId: String,
        val rewardedAdUnitId: String,
        val admobAppId: String,
        val interstitialIntervalMinutes: Int,
        val oneMonthMemberEnabled: Boolean,
        val oneMonthMemberTargetAds: Int
    )

    private val _adminSettingsFlow = MutableStateFlow<AdminSettings?>(null)
    val adminSettingsFlow: StateFlow<AdminSettings?> = _adminSettingsFlow.asStateFlow()

    // Map of button configs loaded from Firestore
    private val buttonConfigs = mutableMapOf<String, ButtonAdConfig>()
    private val _buttonConfigsFlow = MutableStateFlow<Map<String, ButtonAdConfig>>(emptyMap())
    val buttonConfigsFlow: StateFlow<Map<String, ButtonAdConfig>> = _buttonConfigsFlow.asStateFlow()

    fun getButtonConfigs(): Map<String, ButtonAdConfig> = buttonConfigs

    // Local default button configurations
    private val defaultButtonConfigs = mapOf(
        "add_reading" to ButtonAdConfig("add_reading", "Save Daily Reading", "Add Reading Screen", "rewarded", true),
        "save_meter" to ButtonAdConfig("save_meter", "Save New Meter", "Meters Screen", "rewarded", true),
        "save_bill_cycle" to ButtonAdConfig("save_bill_cycle", "Save Bill Cycle", "Bill Cycle Screen", "interstitial", true),
        "history_actions" to ButtonAdConfig("history_actions", "History Export/Share", "History Screen", "interstitial", true),
        "profile_actions" to ButtonAdConfig("profile_actions", "Profile Actions", "Profile Dialog", "interstitial", true),
        "cloud_sync_actions" to ButtonAdConfig("cloud_sync_actions", "Cloud Sync Actions", "Data Sync Dialog", "interstitial", true)
    )

    // Cooldown state
    private var lastInterstitialShownTime = 0L

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading = false

    private var isInitialized = false
    private var adRewardPreferences: AdRewardPreferences? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var periodicActivityRef: WeakReference<Activity>? = null
    private var isTimerRunning = false

    private val periodicAdRunnable = object : Runnable {
        override fun run() {
            val activity = periodicActivityRef?.get()
            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                Log.d(TAG, "Interval reached. Checking and showing interstitial ad...")
                // Explicit periodic timer trigger
                handleAction(activity, "periodic_timer") {
                    mainHandler.postDelayed(this, getInterstitialIntervalMs())
                }
            } else {
                mainHandler.postDelayed(this, 30_000L)
            }
        }
    }

    private fun getInterstitialIntervalMs(): Long {
        return interstitialIntervalMinutes.coerceAtLeast(1) * 60 * 1000L
    }

    /**
     * Initializes Google Mobile Ads SDK, listeners, and trackers.
     */
    fun initialize(context: Context) {
        val appContext = context.applicationContext ?: context
        FirebaseInitializer.ensureInitialized(appContext)

        if (adRewardPreferences == null) {
            adRewardPreferences = AdRewardPreferences(appContext)
        }

        if (isInitialized) return
        try {
            MobileAds.initialize(context) { initializationStatus ->
                Log.d(TAG, "AdMob MobileAds initialized: ${initializationStatus.adapterStatusMap}")
                isInitialized = true
                loadInterstitialAd(appContext)
                loadRewardedAd(appContext)
            }
            listenToRemoteConfig(appContext)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds", e)
        }
    }

    /**
     * Set up real-time Firestore listeners for both global settings and button-level configurations.
     */
    fun listenToRemoteConfig(context: Context? = null) {
        try {
            val db = if (context != null) {
                FirebaseInitializer.getFirestore(context)
            } else {
                try {
                    FirebaseFirestore.getInstance()
                } catch (e: Throwable) {
                    null
                }
            } ?: return

            // 1. Listen to admin settings
            adminSettingsListener?.remove()
            adminSettingsListener = db.collection("config").document("admin_settings")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Failed to listen to admin settings from Firestore: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        adsEnabled = snapshot.getBoolean("ads_enabled") ?: true
                        guestAdsEnabled = snapshot.getBoolean("guest_ads_enabled") ?: true
                        googleUserAdsEnabled = snapshot.getBoolean("google_user_ads_enabled") ?: true
                        bannerAdsEnabled = snapshot.getBoolean("banner_ads_enabled") ?: true
                        interstitialAdsEnabled = snapshot.getBoolean("interstitial_ads_enabled") ?: true
                        rewardedAdsEnabled = snapshot.getBoolean("rewarded_ads_enabled") ?: true

                        val snapBanner = snapshot.getString("banner_ad_unit_id")
                        bannerAdUnitId = if (!snapBanner.isNullOrBlank()) snapBanner.trim() else "ca-app-pub-3950731807963252/6891016409"

                        val snapInterstitial = snapshot.getString("interstitial_ad_unit_id")
                        interstitialAdUnitId = if (!snapInterstitial.isNullOrBlank()) snapInterstitial.trim() else "ca-app-pub-3950731807963252/5589492691"

                        val snapRewarded = snapshot.getString("rewarded_ad_unit_id")
                        rewardedAdUnitId = if (!snapRewarded.isNullOrBlank()) snapRewarded.trim() else "ca-app-pub-3950731807963252/6108367443"

                        val snapAppId = snapshot.getString("admob_app_id")
                        admobAppId = if (!snapAppId.isNullOrBlank()) snapAppId.trim() else "ca-app-pub-3950731807963252~4887066950"

                        val interval = snapshot.getLong("interstitial_interval_minutes")?.toInt() ?: 3
                        if (interval != interstitialIntervalMinutes) {
                            interstitialIntervalMinutes = interval
                            val activity = periodicActivityRef?.get()
                            if (activity != null && isTimerRunning) {
                                stopPeriodicInterstitialTimer()
                                startPeriodicInterstitialTimer(activity)
                            }
                        }

                        oneMonthMemberEnabled = snapshot.getBoolean("one_month_member_enabled") ?: true
                        oneMonthMemberTargetAds = (snapshot.getLong("one_month_member_target_ads") ?: 10L).toInt()

                        _adminSettingsFlow.value = AdminSettings(
                            adsEnabled = adsEnabled,
                            guestAdsEnabled = guestAdsEnabled,
                            googleUserAdsEnabled = googleUserAdsEnabled,
                            bannerAdsEnabled = bannerAdsEnabled,
                            interstitialAdsEnabled = interstitialAdsEnabled,
                            rewardedAdsEnabled = rewardedAdsEnabled,
                            bannerAdUnitId = bannerAdUnitId,
                            interstitialAdUnitId = interstitialAdUnitId,
                            rewardedAdUnitId = rewardedAdUnitId,
                            admobAppId = admobAppId,
                            interstitialIntervalMinutes = interstitialIntervalMinutes,
                            oneMonthMemberEnabled = oneMonthMemberEnabled,
                            oneMonthMemberTargetAds = oneMonthMemberTargetAds
                        )
                        Log.d(TAG, "Real-time admin settings synchronized successfully: adsEnabled=$adsEnabled")
                    }
                }

            // 2. Listen to button configs
            buttonAdConfigsListener?.remove()
            buttonAdConfigsListener = db.collection("button_ad_configs")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Failed to listen to button configs: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        for (doc in snapshots.documents) {
                            val buttonId = doc.getString("button_id") ?: continue
                            val buttonName = doc.getString("button_name") ?: ""
                            val screenName = doc.getString("screen_name") ?: ""
                            val adType = doc.getString("ad_type") ?: "none"
                            val enabled = doc.getBoolean("enabled") ?: true
                            buttonConfigs[buttonId] = ButtonAdConfig(
                                buttonId = buttonId,
                                buttonName = buttonName,
                                screenName = screenName,
                                adType = adType,
                                enabled = enabled
                            )
                        }
                        _buttonConfigsFlow.value = buttonConfigs.toMap()
                        Log.d(TAG, "Button configurations updated dynamically: ${buttonConfigs.keys}")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Firestore listener inside AdManager", e)
        }
    }

    /**
     * Starts the recurring Interstitial Ad timer for the active activity.
     */
    fun startPeriodicInterstitialTimer(activity: Activity) {
        periodicActivityRef = WeakReference(activity)
        if (!isTimerRunning) {
            isTimerRunning = true
            mainHandler.removeCallbacks(periodicAdRunnable)
            mainHandler.postDelayed(periodicAdRunnable, getInterstitialIntervalMs())
            Log.d(TAG, "Periodic interstitial timer started with interval: $interstitialIntervalMinutes minutes")
        }
    }

    fun stopPeriodicInterstitialTimer() {
        isTimerRunning = false
        mainHandler.removeCallbacks(periodicAdRunnable)
        Log.d(TAG, "Periodic interstitial timer stopped")
    }

    fun getAdRewardPreferences(context: Context): AdRewardPreferences {
        if (adRewardPreferences == null) {
            adRewardPreferences = AdRewardPreferences(context.applicationContext)
        }
        return adRewardPreferences!!
    }

    /**
     * Resolves the current Ad Unit ID for a specific format.
     */
    fun getResolvedBannerAdUnitId(): String {
        return if (bannerAdUnitId.isNotBlank()) bannerAdUnitId.trim() else AdUnits.BANNER_AD_UNIT_ID
    }

    fun getResolvedInterstitialAdUnitId(): String {
        return if (interstitialAdUnitId.isNotBlank()) interstitialAdUnitId.trim() else AdUnits.INTERSTITIAL_AD_UNIT_ID
    }

    fun getResolvedRewardedAdUnitId(): String {
        return if (rewardedAdUnitId.isNotBlank()) rewardedAdUnitId.trim() else AdUnits.REWARDED_AD_UNIT_ID
    }

    /**
     * Preloads an Interstitial Ad safely.
     */
    fun loadInterstitialAd(context: Context) {
        mainHandler.post {
            if (interstitialAd != null || isInterstitialLoading) return@post

            isInterstitialLoading = true
            val adRequest = AdRequest.Builder().build()
            val adUnitId = getResolvedInterstitialAdUnitId()

            try {
                InterstitialAd.load(
                    context,
                    adUnitId,
                    adRequest,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            Log.d(TAG, "Interstitial ad loaded successfully using: $adUnitId")
                            interstitialAd = ad
                            isInterstitialLoading = false
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.w(TAG, "Interstitial ad failed to load: ${error.message}")
                            interstitialAd = null
                            isInterstitialLoading = false
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading interstitial ad", e)
                isInterstitialLoading = false
            }
        }
    }

    /**
     * Preloads a Rewarded Video Ad safely.
     */
    fun loadRewardedAd(context: Context) {
        mainHandler.post {
            if (rewardedAd != null || isRewardedLoading) return@post

            isRewardedLoading = true
            val adRequest = AdRequest.Builder().build()
            val adUnitId = getResolvedRewardedAdUnitId()

            try {
                RewardedAd.load(
                    context,
                    adUnitId,
                    adRequest,
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) {
                            Log.d(TAG, "Rewarded ad loaded successfully using: $adUnitId")
                            rewardedAd = ad
                            isRewardedLoading = false
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.w(TAG, "Rewarded ad failed to load: ${error.message}")
                            rewardedAd = null
                            isRewardedLoading = false
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading rewarded ad", e)
                isRewardedLoading = false
            }
        }
    }

    /**
     * Helper to check if a banner ad should be shown globally.
     */
    fun shouldShowBannerAd(context: Context): Boolean {
        if (!adsEnabled) return false
        if (!bannerAdsEnabled) return false

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            if (!googleUserAdsEnabled) return false
        } else {
            if (!guestAdsEnabled) return false
        }
        return true
    }

    /**
     * Helper to check if a banner ad should be shown for a specific screen/tab.
     */
    fun shouldShowBannerAdForScreen(context: Context, bannerId: String): Boolean {
        if (!adsEnabled) return false
        if (!bannerAdsEnabled) return false

        val currentUser = FirebaseAuth.getInstance().currentUser
        val isGoogleUser = currentUser != null

        if (isGoogleUser) {
            if (!googleUserAdsEnabled) return false
        } else {
            if (!guestAdsEnabled) return false
        }

        val userSuffix = if (isGoogleUser) "google_user" else "guest"
        val docId = "${bannerId}_${userSuffix}"

        val config = buttonConfigs[docId] ?: buttonConfigs[bannerId]
        if (config != null) {
            return config.enabled && config.adType == "banner"
        }
        return true
    }

    /**
     * Evaluates whether an ad should be displayed for a given action.
     * Follows the exact logical steps defined in PRD Section 11.
     * Returns: "none", "banner", "interstitial", "rewarded" or null.
     */
    fun getResolvedAdTypeForAction(context: Context, buttonId: String): String? {
        // Determine user type
        val currentUser = FirebaseAuth.getInstance().currentUser
        val isGoogleUser = currentUser != null
        val userSuffix = if (isGoogleUser) "google_user" else "guest"
        val docId = "${buttonId}_${userSuffix}"

        // Step 1: Resolve configuration for the button
        val config = buttonConfigs[docId] ?: buttonConfigs[buttonId] ?: defaultButtonConfigs[buttonId] ?: return null

        // Step 2: Check if global ads are enabled
        if (!adsEnabled) return null

        // Step 3: Check User Type Allowed
        if (isGoogleUser) {
            if (!googleUserAdsEnabled) return null
            
            // Check One Month Member feature (if enabled globally)
            if (oneMonthMemberEnabled) {
                val prefs = getAdRewardPreferences(context)
                if (prefs.isExemptFromIntrusiveAds()) {
                    // Intrusive formats (interstitial/rewarded) are blocked for exempt users. Banners are allowed.
                    if (config.adType == "interstitial" || config.adType == "rewarded") {
                        Log.d(TAG, "User has active 1-month ad exemption. Intrusive ad format skipped.")
                        return null
                    }
                }
            }
        } else {
            if (!guestAdsEnabled) return false.toString().let { null } // Guest ads disabled
        }

        // Step 4: Check if the button configuration itself is enabled
        if (!config.enabled) return null

        // Step 5: Check Button Ad Type
        val adType = config.adType
        if (adType == "none") return null

        // Step 6: Selected Ad Type Enabled?
        when (adType) {
            "banner" -> if (!bannerAdsEnabled) return null
            "interstitial" -> if (!interstitialAdsEnabled) return null
            "rewarded" -> if (!rewardedAdsEnabled) return null
            else -> return null
        }

        // Step 7: Required Ad Unit ID Available?
        val unitId = when (adType) {
            "banner" -> getResolvedBannerAdUnitId()
            "interstitial" -> getResolvedInterstitialAdUnitId()
            "rewarded" -> getResolvedRewardedAdUnitId()
            else -> ""
        }
        if (unitId.isBlank()) return null

        // Step 8: Frequency Rules
        if (adType == "interstitial") {
            val now = System.currentTimeMillis()
            val elapsed = now - lastInterstitialShownTime
            val intervalMs = getInterstitialIntervalMs()
            if (elapsed < intervalMs) {
                Log.d(TAG, "Interstitial action '$buttonId' suppressed. Cooldown active: ${((intervalMs - elapsed) / 1000)}s remaining.")
                return null
            }
        }

        return adType
    }

    /**
     * Executes the centralized decision pipeline and handles display.
     */
    fun handleAction(
        activity: Activity,
        buttonId: String,
        onAdDismissed: () -> Unit = {}
    ) {
        val resolvedFormat = getResolvedAdTypeForAction(activity, buttonId)
        Log.d(TAG, "Centralized Ad Decision for '$buttonId': Resolved Format = $resolvedFormat")

        when (resolvedFormat) {
            "interstitial" -> {
                showInterstitialAd(activity) {
                    lastInterstitialShownTime = System.currentTimeMillis()
                    onAdDismissed()
                }
            }
            "rewarded" -> {
                showRewardedAd(
                    activity = activity,
                    ignoreExemption = false,
                    onUserEarnedReward = { _, _ ->
                        adRewardPreferences?.recordAdWatched()
                    },
                    onAdClosed = { _ ->
                        onAdDismissed()
                    }
                )
            }
            else -> {
                // If "none", "banner" (which is displayed inline), or null, execute the callback immediately
                onAdDismissed()
            }
        }
    }

    /**
     * Safe internal interstitial show call.
     */
    fun showInterstitialAd(activity: Activity, onDismiss: () -> Unit) {
        activity.runOnUiThread {
            try {
                if (activity.isFinishing || activity.isDestroyed) {
                    onDismiss()
                    return@runOnUiThread
                }

                val currentAd = interstitialAd
                if (currentAd != null) {
                    currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            super.onAdShowedFullScreenContent()
                            Log.d(TAG, "Interstitial ad showed full screen")
                            AdAnalyticsTracker.logAdImpression(activity, "INTERSTITIAL")
                        }

                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Interstitial ad dismissed")
                            interstitialAd = null
                            loadInterstitialAd(activity.applicationContext)
                            onDismiss()
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            Log.w(TAG, "Interstitial ad failed to show: ${error.message}")
                            interstitialAd = null
                            loadInterstitialAd(activity.applicationContext)
                            onDismiss()
                        }
                    }
                    interstitialAd = null
                    currentAd.show(activity)
                } else {
                    Log.d(TAG, "Interstitial ad not ready, preloading for next event")
                    loadInterstitialAd(activity.applicationContext)
                    onDismiss()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception showing interstitial ad", e)
                loadInterstitialAd(activity.applicationContext)
                onDismiss()
            }
        }
    }

    /**
     * Safe internal rewarded show call.
     */
    fun showRewardedAd(
        activity: Activity,
        ignoreExemption: Boolean = false,
        onUserEarnedReward: (amount: Int, type: String) -> Unit = { _, _ -> },
        onAdClosed: (earned: Boolean) -> Unit = {}
    ) {
        activity.runOnUiThread {
            try {
                if (activity.isFinishing || activity.isDestroyed) {
                    onAdClosed(false)
                    return@runOnUiThread
                }

                val currentAd = rewardedAd
                if (currentAd != null) {
                    var rewardEarned = false

                    currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            super.onAdShowedFullScreenContent()
                            Log.d(TAG, "Rewarded video ad showed full screen")
                            AdAnalyticsTracker.logAdImpression(activity, "REWARDED_VIDEO")
                        }

                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Rewarded ad dismissed")
                            rewardedAd = null
                            loadRewardedAd(activity.applicationContext)
                            onAdClosed(rewardEarned)
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            Log.w(TAG, "Rewarded ad failed to show: ${error.message}")
                            rewardedAd = null
                            loadRewardedAd(activity.applicationContext)
                            onAdClosed(false)
                        }
                    }

                    rewardedAd = null
                    currentAd.show(activity) { rewardItem ->
                        rewardEarned = true
                        Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                        onUserEarnedReward(rewardItem.amount, rewardItem.type)
                    }
                } else {
                    Log.d(TAG, "Rewarded ad not ready yet, preloading for next event")
                    loadRewardedAd(activity.applicationContext)
                    onAdClosed(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception showing rewarded ad", e)
                loadRewardedAd(activity.applicationContext)
                onAdClosed(false)
            }
        }
    }

    fun isRewardedAdReady(): Boolean = rewardedAd != null
}