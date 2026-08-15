@file:JvmName("WebAuthLauncherDesktop")

package com.ajinkyabadve.kmmmywatchlist.core.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.URI

class DesktopWebAuthLauncher : WebAuthLauncher {
    override fun launchAuth(
        authUrl: String,
        redirectScheme: String,
        onResult: (requestToken: String?, approved: Boolean) -> Unit,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            var serverSocket: ServerSocket? = null
            try {
                serverSocket = ServerSocket(0) // Bind to any free local port
                val localPort = serverSocket.localPort
                val redirectUrl = "http://127.0.0.1:$localPort/auth-callback"
                val fullAuthUrl =
                    if (authUrl.contains("?")) {
                        "$authUrl&redirect_to=$redirectUrl"
                    } else {
                        "$authUrl?redirect_to=$redirectUrl"
                    }

                Napier.d(tag = "WebAuthLauncherDesktop") { "Opening browser to $fullAuthUrl, listening on port $localPort" }
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create(fullAuthUrl))
                } else {
                    val runtime = Runtime.getRuntime()
                    val os = System.getProperty("os.name").lowercase()
                    when {
                        os.contains("mac") -> runtime.exec(arrayOf("open", fullAuthUrl))
                        os.contains("nix") || os.contains("nux") -> runtime.exec(arrayOf("xdg-open", fullAuthUrl))
                        os.contains("win") -> runtime.exec(arrayOf("rundll32", "url.dll,FileProtocolHandler", fullAuthUrl))
                    }
                }

                // Accept incoming redirect request
                serverSocket.soTimeout = 180000 // 3 minutes timeout
                val socket = serverSocket.accept()
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val requestLine = reader.readLine().orEmpty()
                Napier.d(tag = "WebAuthLauncherDesktop") { "Received HTTP request: $requestLine" }

                // Parse query parameters from GET /auth-callback?request_token=...&approved=true
                var requestToken: String? = null
                var approved = true
                if (requestLine.contains("?")) {
                    val queryString = requestLine.substringAfter("?").substringBefore(" ")
                    val params =
                        queryString.split("&").associate {
                            val parts = it.split("=")
                            val key = parts.getOrNull(0).orEmpty()
                            val value = parts.getOrNull(1).orEmpty()
                            key to value
                        }
                    requestToken = params["request_token"]
                    approved = params["approved"] != "false"
                }

                val htmlResponse =
                    """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="utf-8">
                        <title>MyWatchList Authorization</title>
                        <script type="text/javascript">
                            function handleClose() {
                                if (window.history && window.history.replaceState) {
                                    window.history.replaceState({}, document.title, "/auth-callback");
                                }
                                try {
                                    window.close();
                                } catch (e) {}
                            }
                            function manualClose() {
                                handleClose();
                                var btn = document.getElementById('close-btn');
                                if (btn) { btn.innerText = "✓ You can close this tab now"; }
                            }
                            window.onload = handleClose;
                        </script>
                    </head>
                    <body style="font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; text-align: center; padding-top: 60px; background-color: #121212; color: #ffffff;">
                        <div style="max-width: 400px; margin: 0 auto; padding: 32px; background-color: #1e1e1e; border-radius: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.5);">
                            <div style="font-size: 48px; color: #4CAF50; margin-bottom: 16px;">✓</div>
                            <h2 style="margin: 0 0 12px 0; font-size: 22px; font-weight: 600;">Authentication Successful</h2>
                            <p style="color: #b0b0b0; font-size: 14px; line-height: 1.5; margin-bottom: 24px;">
                                Your TMDB account has been connected. Return to the MyWatchList desktop app to continue.
                            </p>
                            <button id="close-btn" onclick="manualClose()" style="padding: 12px 24px; font-size: 14px; font-weight: 600; background-color: #2563EB; color: white; border: none; border-radius: 8px; cursor: pointer;">
                                Close Tab
                            </button>
                        </div>
                    </body>
                    </html>
                    """.trimIndent()

                val htmlBytes = htmlResponse.toByteArray(Charsets.UTF_8)
                val os = socket.getOutputStream()
                val headers =
                    "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html; charset=utf-8\r\n" +
                        "Content-Length: ${htmlBytes.size}\r\n" +
                        "Connection: close\r\n\r\n"
                os.write(headers.toByteArray(Charsets.UTF_8))
                os.write(htmlBytes)
                os.flush()
                socket.close()

                onResult(requestToken, approved)
            } catch (e: java.net.SocketTimeoutException) {
                Napier.e(tag = "WebAuthLauncherDesktop", throwable = e) { "Desktop authentication timed out waiting for callback" }
                onResult(null, false)
            } catch (e: java.io.IOException) {
                Napier.e(tag = "WebAuthLauncherDesktop", throwable = e) { "I/O error during desktop web authentication flow" }
                onResult(null, false)
            } catch (e: IllegalArgumentException) {
                Napier.e(tag = "WebAuthLauncherDesktop", throwable = e) { "Invalid argument during desktop web authentication flow" }
                onResult(null, false)
            } finally {
                try {
                    serverSocket?.close()
                } catch (ignored: java.io.IOException) {
                }
            }
        }
    }
}

@Composable
actual fun rememberWebAuthLauncher(): WebAuthLauncher = remember { DesktopWebAuthLauncher() }
