package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide settings preferences managing user-configurable parameters like Unit Threshold,
 * synced across all screens and Firebase Firestore.
 */
class AppSettingsPreferences(private val context: Context) {

    companion object {
        const val PREFS_NAME = "meter_app_settings_prefs"
        const val KEY_UNIT_THRESHOLD = "key_alert_unit_threshold"
        const val KEY_ADMIN_MODE_ENABLED = "key_admin_mode_enabled"
        const val DEFAULT_UNIT_THRESHOLD = 100.0
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _unitThreshold = MutableStateFlow(loadUnitThreshold())
    val unitThreshold: StateFlow<Double> = _unitThreshold.asStateFlow()

    private val _adminModeEnabled = MutableStateFlow(loadAdminModeEnabled())
    val adminModeEnabled: StateFlow<Boolean> = _adminModeEnabled.asStateFlow()

    private fun loadUnitThreshold(): Double {
        val strVal = prefs.getString(KEY_UNIT_THRESHOLD, null)
        return strVal?.toDoubleOrNull() ?: prefs.getFloat(KEY_UNIT_THRESHOLD, DEFAULT_UNIT_THRESHOLD.toFloat()).toDouble()
    }

    private fun loadAdminModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_ADMIN_MODE_ENABLED, false)
    }

    fun setAdminModeEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_ADMIN_MODE_ENABLED, enabled)
            .apply()
        _adminModeEnabled.value = enabled
    }

    fun setUnitThreshold(threshold: Double) {
        val validThreshold = threshold.coerceAtLeast(1.0)
        prefs.edit()
            .putString(KEY_UNIT_THRESHOLD, validThreshold.toString())
            .putFloat(KEY_UNIT_THRESHOLD, validThreshold.toFloat())
            .apply()
        _unitThreshold.value = validThreshold
    }

    fun setFromRemoteSync(threshold: Double) {
        if (threshold >= 1.0 && threshold != _unitThreshold.value) {
            prefs.edit()
                .putString(KEY_UNIT_THRESHOLD, threshold.toString())
                .putFloat(KEY_UNIT_THRESHOLD, threshold.toFloat())
                .apply()
            _unitThreshold.value = threshold
        }
    }
}
