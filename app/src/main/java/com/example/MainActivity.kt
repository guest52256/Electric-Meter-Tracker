package com.example

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
import com.example.ui.MainAppScreen
import com.example.ui.theme.MyApplicationTheme

import com.example.viewmodel.MeterViewModel
import com.example.viewmodel.MeterViewModelFactory

class MainActivity : ComponentActivity() {

  private val meterViewModel: MeterViewModel by viewModels {
    MeterViewModelFactory(application)
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: android.content.Intent?) {
    when (intent?.action) {
      "ACTION_ADD_BILL_CYCLE" -> {
          meterViewModel.selectedNavigationScreen.value = com.example.ui.navigation.Screen.BILL_CYCLE
      }
      "ACTION_ADD_READING" -> {
          meterViewModel.selectedNavigationScreen.value = com.example.ui.navigation.Screen.ADD_READING
      }
      "ACTION_DEV_INFO" -> {
          meterViewModel.showDeveloperDialog.value = true
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

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
}


