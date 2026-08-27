package com.example.rideboard.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StravaUploadScreen(
    onContinueRide: () -> Unit,
    onExportElsewhere: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    var uploadDetected by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.userAgentString = settings.userAgentString.replace("; wv", "")

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Strava redirige vers /activities/{id} une fois l'upload traité avec succès
                            if (url != null && url.contains("strava.com/activities/")) {
                                uploadDetected = true
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onShowFileChooser(
                            webView: WebView?,
                            filePathCallback: ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            val fitFile = File(context.getExternalFilesDir(null), "ride.fit")
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                fitFile
                            )
                            filePathCallback?.onReceiveValue(arrayOf(uri))
                            return true
                        }
                    }

                    loadUrl("https://www.strava.com/upload/select")
                }
            }
        )

        // --- Barre de boutons superposée, toujours accessible ---
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.95f))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onContinueRide) {
                Text("Reprendre")
            }
            Button(onClick = onExportElsewhere) {
                Text("Export fit")
            }
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uploadDetected) Color(0xFF4CAF50) else Color.Gray
                )
            ) {
                Text(if (uploadDetected) "✓ Ok" else "Exit")
            }
        }

        // Bandeau indicatif si upload détecté
        if (uploadDetected) {
            Text(
                text = "Upload Strava détecté ✓",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .background(Color(0xFF4CAF50))
                    .padding(8.dp),
                color = Color.White
            )
        }
    }
}