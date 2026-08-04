import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ajinkyabadve.kmmmywatchlist.App
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.app_icon
import mywatchlist.composeapp.generated.resources.app_name
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.Dimension

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
fun main() =
    application {
        val windowState = rememberWindowState(width = 800.dp, height = 600.dp)
        Window(
            title = stringResource(Res.string.app_name),
            // Without this the running process keeps the JVM's default Duke icon in the Windows
            // taskbar / Alt-Tab switcher - the .ico wired into nativeDistributions only decorates
            // the packaged launcher .exe, not the live window.
            icon = painterResource(Res.drawable.app_icon),
            state = windowState,
            onCloseRequest = ::exitApplication,
        ) {
            window.minimumSize = Dimension(650, 900)
            App(calculateWindowSizeClass())
        }
    }
