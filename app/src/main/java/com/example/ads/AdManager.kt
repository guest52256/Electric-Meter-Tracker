package com.example.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
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
 * Singleton AdManager for loading and presenting Interstitial and Rewarded Ads.
 * - Handles 3-minute recurring interstitial ad timer.
 * - Handles monthly 10-ad reward exemption for Google signed-in users.
 * - Guarantees ads run everywhere for Guest users with no restrictions.
 * - Safe thread dispatching on the Main UI thread to prevent any crashes.
 */
object AdManager {

    private const val TAG = "AdManager"
    const val THREE_MINUTES_MS = 3 * 60 * 1000L // 3 minutes

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
                Log.d(TAG, "3-minute interval reached. Checking and showing interstitial ad...")
                showInterstitialAd(activity) {
                    // Schedule next interval after current ad closes or skips
                    mainHandler.postDelayed(this, THREE_MINUTES_MS)
                }
            } else {
                // If activity is currently paused/null, re-check in 30 seconds
                mainHandler.postDelayed(this, 30_000L)
            }
        }
    }

    /**
     * Initializes Google Mobile Ads SDK and reward tracker. Call once in MainActivity.
     */
    fun initialize(context: Context) {
        if (adRewardPreferences == null) {
            adRewardPreferences = AdRewardPreferences(context.applicationContext)
        }

        if (isInitialized) return
        try {
            MobileAds.initialize(context) { initializationStatus ->
                Log.d(TAG, "AdMob MobileAds initialized: ${initializationStatus.adapterStatusMap}")
                isInitialized = true
                loadInterstitialAd(context.applicationContext)
                loadRewardedAd(context.applicationContext)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds", e)
        }
    }

    /**
     * Starts the 3-minute recurring Interstitial Ad timer for the active activity.
     */
    fun startPeriodicInterstitialTimer(activity: Activity) {
        periodicActivityRef = WeakReference(activity)
        if (!isTimerRunning) {
            isTimerRunning = true
            mainHandler.removeCallbacks(periodicAdRunnable)
            mainHandler.postDelayed(periodicAdRunnable, THREE_MINUTES_MS)
            Log.d(TAG, "Periodic 3-minute interstitial timer started")
        }
    }

    fun stopPeriodicInterstitialTimer() {
        isTimerRunning = false
        mainHandler.removeCallbacks(periodicAdRunnable)
        Log.d(TAG, "Periodic 3-minute interstitial timer stopped")
    }

    fun getAdRewardPreferences(context: Context): AdRewardPreferences {
        if (adRewardPreferences == null) {
            adRewardPreferences = AdRewardPreferences(context.applicationContext)
        }
        return adRewardPreferences!!
    }

    /**
     * Preloads an Interstitial Ad on the Main UI thread.
     */
    fun loadInterstitialAd(context: Context) {
        mainHandler.post {
            if (interstitialAd != null || isInterstitialLoading) return@post

            isInterstitialLoading = true
            val adRequest = AdRequest.Builder().build()
            val adUnitId = AdUnits.INTERSTITIAL_AD_UNIT_ID

            try {
                InterstitialAd.load(
                    context,
                    adUnitId,
                    adRequest,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            Log.d(TAG, "Interstitial ad loaded successfully")
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
     * Displays an Interstitial Ad (e.g. 3-minute timer, Bill Cycle add/update).
     * - Guest users: Always shown (no restrictions).
     * - Google users: Shown unless user has watched 10 ads for 1-month exemption.
     */
    fun showInterstitialAd(
        activity: Activity,
        onDismiss: () -> Unit = {}
    ) {
        activity.runOnUiThread {
            try {
                if (activity.isFinishing || activity.isDestroyed) {
                    onDismiss()
                    return@runOnUiThread
                }

                val prefs = getAdRewardPreferences(activity)
                if (prefs.isExemptFromIntrusiveAds()) {
                    Log.d(TAG, "Google user has 1-month ad exemption active. Skipping Interstitial Ad.")
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
                    // Not ready yet — trigger load and proceed
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
     * Preloads a Rewarded Video Ad on the Main UI thread.
     */
    fun loadRewardedAd(context: Context) {
        mainHandler.post {
            if (rewardedAd != null || isRewardedLoading) return@post

            isRewardedLoading = true
            val adRequest = AdRequest.Builder().build()
            val adUnitId = AdUnits.REWARDED_AD_UNIT_ID

            try {
                RewardedAd.load(
                    context,
                    adUnitId,
                    adRequest,
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) {
                            Log.d(TAG, "Rewarded ad loaded successfully")
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
     * Shows a Rewarded Video Ad safely on the UI thread.
     * @param ignoreExemption If true (e.g. from Profile dialog "Watch Ad"), plays even if user is exempt.
     * @param onUserEarnedReward Called when the user watches the ad to completion.
     * @param onAdClosed Called when ad closes (whether rewarded or closed early).
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

                val prefs = getAdRewardPreferences(activity)
                if (!ignoreExemption && prefs.isExemptFromIntrusiveAds()) {
                    Log.d(TAG, "Google user has 1-month ad exemption active. Skipping Rewarded Video Ad.")
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
