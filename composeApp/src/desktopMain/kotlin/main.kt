import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ajinkyabadve.kmmmywatchlist.App
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.MoviesTab
import java.awt.Dimension

@OptIn(ExperimentalMaterial3Api::class)
fun main() = application {
    val windowState = rememberWindowState(width = 800.dp, height = 600.dp)
    Window(
        title = "MyWatchList",
        state = windowState,
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(650, 900)
        App(windowSize = MoviesTab.WindowSize.basedOnWidth(windowState.size.width))
    }
}


