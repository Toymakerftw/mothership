package com.example.mothership

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
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
        private const val RELOAD_DELAY = 1000L // 1 second delay before reloading on ORB error
    }

    private lateinit var webView: WebView
    private var pwaUuid: String? = null
    private var serverPort: Int = SERVER_PORT_BASE
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pwa_splash) // Show splash screen initially

        var pwaUrl = intent.getStringExtra("pwaUrl")
        val pwaName = intent.getStringExtra("pwaName") ?: "PWA App"

        if (pwaUrl == null || !pwaUrl.startsWith("file://")) {
            Toast.makeText(this, "Invalid PWA URL", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Extract UUID from the PWA URL
        try {
            val decodedUrl = URLDecoder.decode(pwaUrl, "UTF-8")
            val filePath = decodedUrl.substring(7)
            val file = File(filePath)
            pwaUuid = file.parentFile?.name

            if (pwaUuid == null) {
                Toast.makeText(this, "Invalid PWA path", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            UUID.fromString(pwaUuid)
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting PWA UUID: ${e.message}")
            Toast.makeText(this, "Error processing PWA URL", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize WebView but don't set it as content view yet
        webView = WebView(this)
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
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH)

        title = pwaName

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d(TAG, "Loading page: $url")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page loaded: $url")
                view?.scrollTo(0, 1)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.e(TAG, "Web error: ${error?.description}")
                if (error?.description?.contains("ERR_BLOCKED_BY_ORB") == true) {
                    Log.w(TAG, "ORB error detected, reloading...")
                    handler.postDelayed({ view?.reload() }, RELOAD_DELAY)
                } else {
                    Toast.makeText(this@PwaViewerActivity, "Error loading PWA: ${error?.description}", Toast.LENGTH_LONG).show()
                }
            }
        }

        startHttpServerAndLoadPwa()
    }

    private fun startHttpServerAndLoadPwa() {
        if (pwaUuid == null) {
            Toast.makeText(this, "PWA UUID not available", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            serverPort = generateUniquePort(pwaUuid!!)

            val serverIntent = Intent(this, PwaHttpServerService::class.java).apply {
                action = PwaHttpServerService.ACTION_START_SERVER
                putExtra(PwaHttpServerService.EXTRA_PWA_UUID, pwaUuid)
                putExtra(PwaHttpServerService.EXTRA_PORT, serverPort)
            }
            startService(serverIntent)

            handler.postDelayed({
                checkServerAndLoadPwa()
            }, SERVER_START_DELAY)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting HTTP server or loading PWA: ${e.message}")
            Toast.makeText(this, "Error loading PWA", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun checkServerAndLoadPwa() {
        thread {
            try {
                val url = URL("http://localhost:$serverPort/")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                connection.disconnect()

                if (responseCode == 200) {
                    handler.post {
                        setContentView(webView) // Switch to WebView after server is ready
                        val serverUrl = "http://localhost:$serverPort/"
                        Log.d(TAG, "Loading PWA from local server: $serverUrl")
                        webView.loadUrl(serverUrl)
                    }
                } else {
                    Log.e(TAG, "Server responded with error code: $responseCode")
                    handler.post {
                        Toast.makeText(this, "Server error: $responseCode", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server not responding: ${e.message}")
                handler.post {
                    Toast.makeText(this, "Failed to connect to PWA server", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun generateUniquePort(uuid: String): Int {
        val hash = uuid.hashCode()
        val portOffset = Math.abs(hash) % SERVER_PORT_RANGE
        return SERVER_PORT_BASE + portOffset
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
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
