
package com.example.mothership

import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity

class PwaViewerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val pwaUrl = intent.getStringExtra("pwaUrl")
        val pwaName = intent.getStringExtra("pwaName") ?: "PWA App"
        
        if (pwaUrl == null || !pwaUrl.startsWith("file://")) {
            finish()
            return
        }
        
        val webView = WebView(this)
        setContentView(webView)
        
        // Configure WebView for PWA experience
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        
        // Set the app name as the activity title
        title = pwaName
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Try to hide the address bar by scrolling
                view?.scrollTo(0, 1)
            }
        }
        
        webView.loadUrl(pwaUrl)
    }
}
