package com.toymakerftw.appsage

import android.content.Intent
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import java.io.File

class PwaViewerActivity : ComponentActivity() {
    
    private lateinit var webView: WebView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        webView = WebView(this)
        setContentView(webView)
        
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        
        // Get the PWA URL from intent
        val pwaUrl = intent.getStringExtra("pwaUrl") ?: ""
        val pwaId = intent.getStringExtra("pwaId") ?: ""
        
        if (pwaUrl.isNotEmpty()) {
            // If it's a file URL, use the file:// scheme
            if (pwaUrl.startsWith("file://")) {
                webView.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                        val url = request.url.toString()
                        
                        // Check if this is a local file request
                        if (url.startsWith("file://")) {
                            try {
                                val file = File(url.substring(7)) // Remove "file://" prefix
                                if (file.exists()) {
                                    val mimeType = when {
                                        url.endsWith(".html", ignoreCase = true) -> "text/html"
                                        url.endsWith(".css", ignoreCase = true) -> "text/css"
                                        url.endsWith(".js", ignoreCase = true) -> "application/javascript"
                                        url.endsWith(".json", ignoreCase = true) -> "application/json"
                                        url.endsWith(".png", ignoreCase = true) -> "image/png"
                                        url.endsWith(".jpg", ignoreCase = true) || url.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                                        url.endsWith(".gif", ignoreCase = true) -> "image/gif"
                                        url.endsWith(".svg", ignoreCase = true) -> "image/svg+xml"
                                        url.endsWith(".ico", ignoreCase = true) -> "image/x-icon"
                                        url.endsWith(".woff", ignoreCase = true) -> "font/woff"
                                        url.endsWith(".woff2", ignoreCase = true) -> "font/woff2"
                                        else -> "application/octet-stream"
                                    }
                                    
                                    val inputStream = java.io.FileInputStream(file)
                                    return WebResourceResponse(mimeType, null, inputStream)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        
                        return super.shouldInterceptRequest(view, request)
                    }
                }
            } else {
                // For network URLs, use the standard WebViewClient
                webView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        // Prevent external URL navigation
                        val url = request.url.toString()
                        if (!url.startsWith("http://127.0.0.1:") && !url.startsWith("http://localhost:") && !url.startsWith("file://")) {
                            // If it's an external URL, open in external browser instead of WebView
                            val intent = Intent(Intent.ACTION_VIEW, request.url)
                            startActivity(intent)
                            return true
                        }
                        return false
                    }
                }
            }
            
            webView.loadUrl(pwaUrl)
        }
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}