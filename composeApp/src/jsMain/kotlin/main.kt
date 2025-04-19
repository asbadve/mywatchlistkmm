import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.ajinkyabadve.kmmmywatchlist.App
import org.jetbrains.skiko.wasm.onWasmReady

@ExperimentalMaterial3Api
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
fun main() {
    onWasmReady {
        CanvasBasedWindow("MyWatchList") {
            //todo pass actual size by calculating it
            App(calculateWindowSizeClass())
        }
    }
}
