package com.example.ads

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import com.example.data.firebase.FirebaseInitializer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Enumeration
import java.util.Locale

/**
 * AdAnalyticsTracker tracks daily ad metrics for Guest and Google users:
 * - Date of ad view
 * - Device ID
 * - User Type (Guest vs. Google user)
 * - User ID / Email
 * - IP Address
 * - Ad Type (Banner, Interstitial, Rewarded Video)
 * - Number of ads seen in a day (per type & total)
 * - 10-ad member reward progress (e.g. 3/10, 8/10, 10/10) & 1-month exemption status
 * - Uploads and updates all telemetry in Firebase Firestore.
 */
object AdAnalyticsTracker {

    private const val TAG = "AdAnalyticsTracker"
    private const val PREFS_NAME = "ad_analytics_daily_prefs"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cachedIpAddress: String? = null
    private var lastIpFetchTimestamp = 0L

    /**
     * Resolves the device ID reliably across Android versions.
     */
    fun getDeviceId(context: Context): String {
        return try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (!androidId.isNullOrBlank()) androidId else "device_generic"
        } catch (e: Exception) {
            "device_fallback"
        }
    }

    /**
     * Fetches the device's public IP address asynchronously with network caching and fallback.
     */
    fun getOrFetchIpAddress(context: Context, onResult: (String) -> Unit) {
        val now = System.currentTimeMillis()
        val cached = cachedIpAddress
        if (!cached.isNullOrBlank() && (now - lastIpFetchTimestamp) < 10 * 60 * 1000L) {
            onResult(cached)
            return
        }

        // Check local cached IP from SharedPreferences
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedIp = prefs.getString("last_known_ip", null)
        if (!savedIp.isNullOrBlank() && cached == null) {
            cachedIpAddress = savedIp
        }

        scope.launch {
            var fetchedIp = fetchPublicIpDirectly()
            if (fetchedIp.isNullOrBlank() || fetchedIp == "Unknown") {
                fetchedIp = getLocalIpAddress()
            }
            if (!fetchedIp.isNullOrBlank()) {
                cachedIpAddress = fetchedIp
                lastIpFetchTimestamp = System.currentTimeMillis()
                prefs.edit().putString("last_known_ip", fetchedIp).apply()
                onResult(fetchedIp)
            } else {
                onResult(savedIp ?: "127.0.0.1")
            }
        }
    }

    private fun fetchPublicIpDirectly(): String? {
        return try {
            val url = URL("https://api.ipify.org?format=json")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "GET"
            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                val json = JSONObject(response)
                json.optString("ip", "")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf: NetworkInterface = en.nextElement()
                val enumIpAddr: Enumeration<InetAddress> = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress: InetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress.hostAddress != null) {
                        val host = inetAddress.hostAddress ?: ""
                        if (!host.contains(':')) { // IPv4
                            return host
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            Log.w(TAG, "Could not obtain local IP address: ${ex.message}")
        }
        return "127.0.0.1"
    }

    /**
     * Logs an ad impression for the specified ad type, increments daily stats,
     * gathers reward plan progress, and persists everything to Firebase Firestore.
     *
     * @param context Application or UI Context
     * @param adType "BANNER", "INTERSTITIAL", or "REWARDED_VIDEO"
     */
    fun logAdImpression(context: Context, adType: String) {
        val appContext = context.applicationContext
        val deviceId = getDeviceId(appContext)
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        val isGoogleUser = user != null && !user.isAnonymous
        val userType = if (isGoogleUser) "google" else "guest"
        val userId = if (isGoogleUser) user!!.uid else deviceId
        val userEmail = if (isGoogleUser) (user?.email ?: "google_user") else "guest_user"

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        val now = System.currentTimeMillis()
        val todayStr = dateFormat.format(Date(now))
        val formattedTimeStr = timeFormat.format(Date(now))

        val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Increment daily counters
        val keyBanner = "banner_count_${userId}_$todayStr"
        val keyInterstitial = "interstitial_count_${userId}_$todayStr"
        val keyRewarded = "rewarded_count_${userId}_$todayStr"
        val keyTotal = "total_count_${userId}_$todayStr"

        var bannerCount = prefs.getInt(keyBanner, 0)
        var interstitialCount = prefs.getInt(keyInterstitial, 0)
        var rewardedCount = prefs.getInt(keyRewarded, 0)
        var totalCount = prefs.getInt(keyTotal, 0)

        when (adType.uppercase()) {
            "BANNER" -> bannerCount += 1
            "INTERSTITIAL" -> interstitialCount += 1
            "REWARDED_VIDEO" -> rewardedCount += 1
        }
        totalCount += 1

        prefs.edit()
            .putInt(keyBanner, bannerCount)
            .putInt(keyInterstitial, interstitialCount)
            .putInt(keyRewarded, rewardedCount)
            .putInt(keyTotal, totalCount)
            .apply()

        // Get live 10-ad member reward status
        val adRewardPrefs = AdManager.getAdRewardPreferences(appContext)
        val rewardState = adRewardPrefs.loadState()
        val watchedCount = (AdRewardPreferences.REQUIRED_ADS_COUNT - rewardState.remainingAdsToWatch).coerceAtLeast(0)
        val remainingCount = rewardState.remainingAdsToWatch
        val progressText = "$watchedCount / ${AdRewardPreferences.REQUIRED_ADS_COUNT} ads watched ($remainingCount remaining)"

        // Fetch IP and upload to Firebase Firestore
        getOrFetchIpAddress(appContext) { ip ->
            uploadAdAnalyticsToFirestore(
                deviceId = deviceId,
                userId = userId,
                userType = userType,
                userEmail = userEmail,
                ipAddress = ip,
                date = todayStr,
                timestamp = now,
                formattedTime = formattedTimeStr,
                adType = adType.uppercase(),
                bannerCount = bannerCount,
                interstitialCount = interstitialCount,
                rewardedCount = rewardedCount,
                totalCount = totalCount,
                watchedRewardAds = watchedCount,
                remainingRewardAds = remainingCount,
                rewardProgressText = progressText,
                isMonthExemptActive = rewardState.isMonthExemptActive,
                exemptUntilDate = rewardState.exemptUntilFormattedDate,
                exemptUntilTimestamp = rewardState.exemptUntilTimestamp
            )
        }
    }

    private fun uploadAdAnalyticsToFirestore(
        deviceId: String,
        userId: String,
        userType: String,
        userEmail: String,
        ipAddress: String,
        date: String,
        timestamp: Long,
        formattedTime: String,
        adType: String,
        bannerCount: Int,
        interstitialCount: Int,
        rewardedCount: Int,
        totalCount: Int,
        watchedRewardAds: Int,
        remainingRewardAds: Int,
        rewardProgressText: String,
        isMonthExemptActive: Boolean,
        exemptUntilDate: String,
        exemptUntilTimestamp: Long
    ) {
        scope.launch {
            try {
                val db = try {
                    FirebaseFirestore.getInstance()
                } catch (e: Throwable) {
                    null
                } ?: return@launch

                val dailySummaryData = hashMapOf<String, Any>(
                    "date" to date,
                    "deviceId" to deviceId,
                    "userId" to userId,
                    "userType" to userType,
                    "userEmail" to userEmail,
                    "ipAddress" to ipAddress,
                    "lastAdType" to adType,
                    "lastAdTimestamp" to timestamp,
                    "lastAdTimeFormatted" to formattedTime,
                    "bannerCount" to bannerCount,
                    "interstitialCount" to interstitialCount,
                    "rewardedCount" to rewardedCount,
                    "totalAdsSeenToday" to totalCount,
                    "rewardPlanProgress" to hashMapOf(
                        "watchedCount" to watchedRewardAds,
                        "remainingCount" to remainingRewardAds,
                        "progressText" to rewardProgressText,
                        "isMonthExemptActive" to isMonthExemptActive,
                        "exemptUntilDate" to exemptUntilDate,
                        "exemptUntilTimestamp" to exemptUntilTimestamp
                    ),
                    "updatedAt" to timestamp
                )

                // 1. Root collection `adAnalytics` partitioned by device/user & date
                val docId = "${deviceId}_${date}"
                db.collection("adAnalytics").document(docId)
                    .set(dailySummaryData, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d(TAG, "AdAnalytics updated in Firestore: $docId (Ad: $adType, IP: $ipAddress, Total: $totalCount)")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "AdAnalytics update failed: ${e.message}")
                    }

                // 2. User or Guest specific profile document
                val profilePath = if (userType == "google") "userProfiles/$userId" else "guestProfiles/$deviceId"
                val profileData = hashMapOf<String, Any>(
                    "deviceId" to deviceId,
                    "userId" to userId,
                    "userType" to userType,
                    "userEmail" to userEmail,
                    "lastIpAddress" to ipAddress,
                    "lastActiveDate" to date,
                    "lastActiveTimestamp" to timestamp,
                    "todayAdCount" to totalCount,
                    "todayBannerCount" to bannerCount,
                    "todayInterstitialCount" to interstitialCount,
                    "todayRewardedCount" to rewardedCount,
                    "adRewardPlan" to hashMapOf(
                        "watchedCount" to watchedRewardAds,
                        "remainingCount" to remainingRewardAds,
                        "progressText" to rewardProgressText,
                        "isMonthExemptActive" to isMonthExemptActive,
                        "exemptUntilDate" to exemptUntilDate,
                        "exemptUntilTimestamp" to exemptUntilTimestamp
                    )
                )

                db.document(profilePath)
                    .set(profileData, SetOptions.merge())

                // 3. User subcollection `adDailyLogs`
                val userDailyLogDoc = if (userType == "google") {
                    "users/$userId/adDailyLogs/$date"
                } else {
                    "guests/$deviceId/adDailyLogs/$date"
                }
                db.document(userDailyLogDoc)
                    .set(dailySummaryData, SetOptions.merge())

            } catch (e: Throwable) {
                Log.w(TAG, "Error uploading ad analytics to Firestore: ${e.message}")
            }
        }
    }
}
