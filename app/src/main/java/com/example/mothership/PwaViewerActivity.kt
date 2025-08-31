package com.example.mothership

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity

class PwaViewerActivity : ComponentActivity() {
    private lateinit var webView: WebView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val pwaUrl = intent.getStringExtra("pwaUrl")
        val pwaName = intent.getStringExtra("pwaName") ?: "PWA App"
        
        if (pwaUrl == null || !pwaUrl.startsWith("file://")) {
            Toast.makeText(this, "Invalid PWA URL", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        webView = WebView(this)
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
        webSettings.databaseEnabled = true
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        
        // Set the app name as the activity title
        title = pwaName
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d("PwaViewer", "Loading page: $url")
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("PwaViewer", "Page loaded: $url")
                // Try to hide the address bar by scrolling
                view?.scrollTo(0, 1)
            }
            
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.e("PwaViewer", "Web error: ${error?.description}")
                Toast.makeText(this@PwaViewerActivity, "Error loading PWA: ${error?.description}", Toast.LENGTH_LONG).show()
            }
        }
        
        Log.d("PwaViewer", "Loading PWA from: $pwaUrl")
        webView.loadUrl(pwaUrl)
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}