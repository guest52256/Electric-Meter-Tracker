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
import com.example.util.NotificationHelper
import com.example.viewmodel.MeterViewModel
import com.example.viewmodel.MeterViewModelFactory

class MainActivity : ComponentActivity() {

  private val meterViewModel: MeterViewModel by viewModels {
    MeterViewModelFactory(application)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    NotificationHelper.createNotificationChannel(this)

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


