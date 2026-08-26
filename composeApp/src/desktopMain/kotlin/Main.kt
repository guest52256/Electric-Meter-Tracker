import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import sadaqat.kinzadigitalhub.electricmetertracker.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Electric Meter Tracker",
        state = rememberWindowState(width = 900.dp, height = 700.dp)
    ) {
        App()
    }
}
