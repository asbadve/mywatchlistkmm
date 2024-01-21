import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.ajinkyabadve.kmmmywatchlist.App
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.MoviesTab
import org.jetbrains.skiko.wasm.onWasmReady

@ExperimentalMaterial3Api
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    onWasmReady {
        CanvasBasedWindow("MyWatchList") {
            //todo pass actual size by calculating it
            App(MoviesTab.WindowSize.EXPANDED)
        }
    }
}
