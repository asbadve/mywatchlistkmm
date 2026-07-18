import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.ajinkyabadve.kmmmywatchlist.App

@ExperimentalMaterial3Api
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
fun main() {
    ComposeViewport("ComposeTarget") {
        //todo pass actual size by calculating it
        App(calculateWindowSizeClass())
    }
}
