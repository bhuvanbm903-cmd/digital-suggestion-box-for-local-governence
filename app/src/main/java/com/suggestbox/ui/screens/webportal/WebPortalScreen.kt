package com.suggestbox.ui.screens.webportal

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.suggestbox.core.designsystem.components.LargeTitleScaffold
import com.suggestbox.core.designsystem.theme.SuggestTheme

/**
 * Native Android Screen that renders the SuggestBox Civic Web App directly
 * inside the Android application with full JavaScript, session persistence,
 * and responsive scaling.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPortalScreen(
    onBack: (() -> Unit)? = null,
    portalUrl: String = "http://10.0.2.2:5173", // In Android Emulator, 10.0.2.2 points to host localhost:5173
) {
    var webProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LargeTitleScaffold(
        title = "SuggestBox Web Portal",
        subtitle = "Civic Governance Platform",
        onBackClick = onBack,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (isLoading) {
                LinearProgressIndicator(
                    progress = { webProgress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = SuggestTheme.colors.primary,
                )
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowFileAccess = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                webProgress = newProgress
                                if (newProgress == 100) {
                                    isLoading = false
                                }
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }
                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?,
                            ) {
                                // Fallback handling if server is unreachable
                            }
                        }
                        loadUrl(portalUrl)
                    }
                },
            )
        }
    }
}
