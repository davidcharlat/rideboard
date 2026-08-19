package com.example.rideboard.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StravaUploadScreen() {

    AndroidView(
        modifier = Modifier.fillMaxSize(),

        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)


                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true

                settings.userAgentString = settings.userAgentString.replace("; wv", "")

                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                   // setAcceptThirdPartyCookies(this@apply.let { webView }, true) // voir note ci-dessous
                }
                webViewClient = WebViewClient()

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

                loadUrl(
                    "https://www.strava.com/upload/select"
                )
            }
        }
    )
}