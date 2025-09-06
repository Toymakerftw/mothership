package com.example.mothership

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.*
import kotlin.concurrent.thread

class PwaViewerActivity : ComponentActivity() {
    companion object {
        private const val TAG = "PwaViewer"
        private const val SERVER_PORT_BASE = 8080
        private const val SERVER_PORT_RANGE = 1000 // Ports 8080-9079
        private const val SERVER_START_DELAY = 1000L // 1 second delay to allow server to start
    }
    
    private lateinit var webView: WebView
    private var pwaUuid: String? = null
    private var serverPort: Int = SERVER_PORT_BASE
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        var pwaUrl = intent.getStringExtra("pwaUrl")
        val pwaName = intent.getStringExtra("pwaName") ?: "PWA App"
        
        if (pwaUrl == null || !pwaUrl.startsWith("file://")) {
            Toast.makeText(this, "Invalid PWA URL", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Extract UUID from the PWA URL
        try {
            // Decode the URL to handle any encoded characters
            val decodedUrl = URLDecoder.decode(pwaUrl, "UTF-8")
            // Remove "file://" prefix to get the file path
            val filePath = decodedUrl.substring(7) // "file://".length = 7
            val file = File(filePath)
            
            // Extract UUID from the path
            pwaUuid = file.parentFile?.name
            
            // Validate that we have a UUID
            if (pwaUuid == null) {
                Toast.makeText(this, "Invalid PWA path", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            
            // Validate UUID format
            try {
                UUID.fromString(pwaUuid)
            } catch (e: IllegalArgumentException) {
                Toast.makeText(this, "Invalid PWA UUID", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting PWA UUID: ${e.message}")
            Toast.makeText(this, "Error processing PWA URL", Toast.LENGTH_SHORT).show()
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
                Log.d(TAG, "Loading page: $url")
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page loaded: $url")
                // Try to hide the address bar by scrolling
                view?.scrollTo(0, 1)
            }
            
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.e(TAG, "Web error: ${error?.description}")
                Toast.makeText(this@PwaViewerActivity, "Error loading PWA: ${error?.description}", Toast.LENGTH_LONG).show()
            }
        }
        
        // Start the HTTP server and load the PWA
        startHttpServerAndLoadPwa()
    }
    
    private fun startHttpServerAndLoadPwa() {
        if (pwaUuid == null) {
            Toast.makeText(this, "PWA UUID not available", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        try {
            // Generate a unique port based on the PWA UUID to avoid conflicts
            serverPort = generateUniquePort(pwaUuid!!)
            
            // Start the HTTP server service
            val serverIntent = Intent(this, PwaHttpServerService::class.java).apply {
                action = PwaHttpServerService.ACTION_START_SERVER
                putExtra(PwaHttpServerService.EXTRA_PWA_UUID, pwaUuid)
                putExtra(PwaHttpServerService.EXTRA_PORT, serverPort)
            }
            startService(serverIntent)
            
            // Add a delay to allow the server to start before loading the URL
            handler.postDelayed({
                // Check if the server is responding before loading the URL
                checkServerAndLoadPwa()
            }, SERVER_START_DELAY)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting HTTP server or loading PWA: ${e.message}")
            Toast.makeText(this, "Error loading PWA", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun generateUniquePort(uuid: String): Int {
        // Generate a deterministic port based on the UUID to ensure consistency
        // but still be unique for different PWAs
        val hash = uuid.hashCode()
        // Ensure the port is within our range and >= 0
        val portOffset = Math.abs(hash) % SERVER_PORT_RANGE
        return SERVER_PORT_BASE + portOffset
    }
    
    private fun checkServerAndLoadPwa() {
        thread {
            try {
                val url = URL("http://localhost:$serverPort/")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000 // 5 seconds
                connection.readTimeout = 5000 // 5 seconds
                
                val responseCode = connection.responseCode
                connection.disconnect()
                
                if (responseCode == 200) {
                    // Server is responding, load the PWA
                    handler.post {
                        val serverUrl = "http://localhost:$serverPort/"
                        Log.d(TAG, "Loading PWA from local server: $serverUrl")
                        webView.loadUrl(serverUrl)
                    }
                } else {
                    // Server responded with an error
                    Log.e(TAG, "Server responded with error code: $responseCode")
                    handler.post {
                        Toast.makeText(this, "Server error: $responseCode", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                // Server is not responding
                Log.e(TAG, "Server not responding: ${e.message}")
                handler.post {
                    Toast.makeText(this, "Failed to connect to PWA server", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Remove any pending callbacks
        handler.removeCallbacksAndMessages(null)
        // Stop the HTTP server when the activity is destroyed
        stopHttpServer()
    }
    
    private fun stopHttpServer() {
        try {
            val serverIntent = Intent(this, PwaHttpServerService::class.java).apply {
                action = PwaHttpServerService.ACTION_STOP_SERVER
                putExtra(PwaHttpServerService.EXTRA_PORT, serverPort)
            }
            startService(serverIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping HTTP server: ${e.message}")
        }
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}