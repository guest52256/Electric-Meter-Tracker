package sadaqat.kinzadigitalhub.electricmetertracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import sadaqat.kinzadigitalhub.electricmetertracker.state.MeterTrackerState
import sadaqat.kinzadigitalhub.electricmetertracker.ui.AppTheme
import sadaqat.kinzadigitalhub.electricmetertracker.ui.DashboardScreen

@Composable
fun App() {
    val state = remember { MeterTrackerState() }
    AppTheme {
        DashboardScreen(state = state)
    }
}
