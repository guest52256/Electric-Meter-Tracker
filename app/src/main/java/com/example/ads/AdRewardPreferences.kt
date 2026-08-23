package com.example.ads

import android.content.Context
import android.content.SharedPreferences
import com.example.data.firebase.FirebaseInitializer
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * State representing the user's Reward Ads progress and Month-long Ad-free status.
 */
data class AdRewardState(
    val remainingAdsToWatch: Int = 10,
    val isMonthExemptActive: Boolean = false,
    val exemptUntilTimestamp: Long = 0L,
    val exemptUntilFormattedDate: String = "",
    val totalAdsWatchedThisCycle: Int = 0,
    val isGoogleUser: Boolean = false
)

/**
 * Manages the monthly 10-ad watching feature and checks whether
 * interstitial & rewarded video ads are currently exempted.
 * 
 * Rules:
 * - Guest users (not signed in to Google): Ads run everywhere without restriction.
 * - Google signed-in users: Can watch 10 rewarded ads to unlock 1 month of no interstitial/rewarded video ads (banners only).
 */
class AdRewardPreferences(private val context: Context) {

    companion object {
        val REQUIRED_ADS_COUNT: Int
            get() = AdManager.oneMonthMemberTargetAds
        private const val KEY_REMAINING_ADS = "remaining_ads_to_watch"
        private const val KEY_EXEMPT_UNTIL = "exempt_until_timestamp"
        private const val KEY_WATCHED_COUNT = "total_watched_this_cycle"
    }

    private fun getPrefs(): SharedPreferences {
        val user = FirebaseInitializer.getAuth(context)?.currentUser
        val prefName = if (user != null) {
            "ad_reward_prefs_${user.uid}"
        } else {
            "ad_reward_prefs_guest"
        }
        return context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
    }

    private val _adRewardState = MutableStateFlow(loadState())
    val adRewardState: StateFlow<AdRewardState> = _adRewardState.asStateFlow()

    init {
        checkAndRefreshMonthlyCycle()
    }

    fun loadState(): AdRewardState {
        return try {
            val prefs = getPrefs()
            val user = FirebaseInitializer.getAuth(context)?.currentUser
            val isGoogleUser = user != null

            val now = System.currentTimeMillis()
            val exemptUntil = prefs.getLong(KEY_EXEMPT_UNTIL, 0L)
            // Exemption is only valid for Google logged-in users who earned it
            val isExempt = isGoogleUser && (exemptUntil > now)

            val maxTarget = REQUIRED_ADS_COUNT.coerceAtLeast(1)
            val remaining = if (isExempt) {
                0
            } else {
                val savedRemaining = prefs.getInt(KEY_REMAINING_ADS, maxTarget)
                savedRemaining.coerceIn(0, maxTarget)
            }

            val watched = prefs.getInt(KEY_WATCHED_COUNT, 0)

            val formattedDate = if (exemptUntil > 0L) {
                SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(exemptUntil))
            } else {
                ""
            }

            AdRewardState(
                remainingAdsToWatch = remaining,
                isMonthExemptActive = isExempt,
                exemptUntilTimestamp = exemptUntil,
                exemptUntilFormattedDate = formattedDate,
                totalAdsWatchedThisCycle = watched,
                isGoogleUser = isGoogleUser
            )
        } catch (e: Exception) {
            AdRewardState(
                remainingAdsToWatch = 10,
                isMonthExemptActive = false,
                exemptUntilTimestamp = 0L,
                exemptUntilFormattedDate = "",
                totalAdsWatchedThisCycle = 0,
                isGoogleUser = false
            )
        }
    }

    /**
     * Checks if the 1-month exemption period has passed. If it has expired, resets the counter to 10 ads for the new month.
     */
    fun checkAndRefreshMonthlyCycle() {
        val prefs = getPrefs()
        val now = System.currentTimeMillis()
        val exemptUntil = prefs.getLong(KEY_EXEMPT_UNTIL, 0L)

        if (exemptUntil in 1..now) {
            // Exemption period expired! Reset for the new month cycle.
            prefs.edit()
                .putInt(KEY_REMAINING_ADS, REQUIRED_ADS_COUNT)
                .putInt(KEY_WATCHED_COUNT, 0)
                .putLong(KEY_EXEMPT_UNTIL, 0L)
                .apply()
        }
        _adRewardState.value = loadState()
    }

    /**
     * Records one successfully watched rewarded video ad.
     * Decreases the counter by 1.
     * When reaching 0, grants 1 full month of exemption from video reward & interstitial ads for Google user.
     */
    fun recordAdWatched(): AdRewardState {
        checkAndRefreshMonthlyCycle()
        val current = _adRewardState.value
        if (current.isMonthExemptActive) {
            return current
        }

        val prefs = getPrefs()
        val newWatched = current.totalAdsWatchedThisCycle + 1
        val newRemaining = (current.remainingAdsToWatch - 1).coerceAtLeast(0)

        if (newRemaining == 0) {
            // User reached 10 ads! Grant 1 calendar month exemption
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, 1)
            val exemptUntil = cal.timeInMillis

            prefs.edit()
                .putInt(KEY_REMAINING_ADS, 0)
                .putInt(KEY_WATCHED_COUNT, newWatched)
                .putLong(KEY_EXEMPT_UNTIL, exemptUntil)
                .apply()
        } else {
            prefs.edit()
                .putInt(KEY_REMAINING_ADS, newRemaining)
                .putInt(KEY_WATCHED_COUNT, newWatched)
                .apply()
        }

        val newState = loadState()
        _adRewardState.value = newState
        AdAnalyticsTracker.logAdImpression(context, "REWARDED_VIDEO")
        return newState
    }

    /**
     * Returns whether Interstitial and Video Reward ads are currently muted/exempt for the user.
     * Guest users NEVER have exemption (ads run everywhere for guests).
     * Google users have exemption ONLY if they completed 10 ads this month.
     */
    fun isExemptFromIntrusiveAds(): Boolean {
        checkAndRefreshMonthlyCycle()
        val state = loadState()
        _adRewardState.value = state
        return state.isGoogleUser && state.isMonthExemptActive
    }
}
