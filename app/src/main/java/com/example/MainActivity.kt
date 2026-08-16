package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.AdManager
import com.example.ui.MainAppScreen
import com.example.ui.navigation.Screen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MeterViewModel
import com.example.viewmodel.MeterViewModelFactory

class MainActivity : ComponentActivity() {

  private val meterViewModel: MeterViewModel by viewModels {
    MeterViewModelFactory(application)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: Intent?) {
    when (intent?.action) {
      "ACTION_ADD_BILL_CYCLE" -> {
          meterViewModel.selectedNavigationScreen.value = Screen.BILL_CYCLE
      }
      "ACTION_ADD_READING" -> {
          meterViewModel.selectedNavigationScreen.value = Screen.ADD_READING
      }
      "ACTION_DEV_INFO" -> {
          meterViewModel.showDeveloperDialog.value = true
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Google Mobile Ads SDK (AdMob)
    AdManager.initialize(this)

    // Handle intents from notification
    handleIntent(intent)

    setContent {
      val currentThemeMode by meterViewModel.themeMode.collectAsStateWithLifecycle()
      val currentColorPalette by meterViewModel.colorPalette.collectAsStateWithLifecycle()

      MyApplicationTheme(
        themeMode = currentThemeMode,
        colorPalette = currentColorPalette
      ) {
        Surface(modifier = Modifier.fillMaxSize()) {
          MainAppScreen(viewModel = meterViewModel)
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    // Start 3-minute recurring Interstitial Ad loop for both guest and Google users
    AdManager.startPeriodicInterstitialTimer(this)
  }

  override fun onPause() {
    super.onPause()
    AdManager.stopPeriodicInterstitialTimer()
  }
}
