package com.example.mothership

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File

class PwaViewerActivity : ComponentActivity() {
    companion object {
        private const val TAG = "PwaViewerActivity"
    }

    private var webView: WebView? = null
    private var pwaPath: String? = null
    private var isInstallMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the PWA path from intent
        pwaPath = intent.getStringExtra("pwa_path")
        val pwaName = intent.getStringExtra("pwa_name") ?: "PWA Viewer"
        isInstallMode = intent.getBooleanExtra("install_mode", false)

        setContent {
            PwaViewerScreen(
                pwaName = pwaName,
                onBackClicked = { finish() },
                onInstallClicked = { installPwa() }
            )
        }

        // Request storage permissions if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Intent.ACTION_OPEN_DOCUMENT_TREE.equals(intent.action)) {
                requestStoragePermission()
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestStoragePermission()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PwaViewerScreen(
        pwaName: String,
        onBackClicked: () -> Unit,
        onInstallClicked: () -> Unit
    ) {
        var webViewLoaded by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(pwaName) },
                    navigationIcon = {
                        IconButton(onClick = onBackClicked) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (!isInstallMode) {
                            IconButton(onClick = onInstallClicked) {
                                Text("Install")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (!webViewLoaded && pwaPath != null) {
                    Text(
                        text = "Loading PWA...",
                        modifier = Modifier.padding(16.dp)
                    )
                }

                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webView = this
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            settings.allowFileAccess = true
                            settings.allowContentAccess = true
                            settings.allowFileAccessFromFileURLs = true
                            settings.allowUniversalAccessFromFileURLs = true

                            // Enable PWA features
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    webViewLoaded = true
                                    
                                    // Try to trigger PWA installation if in install mode
                                    if (isInstallMode) {
                                        evaluateJavascript(
                                            """
                                            if ('serviceWorker' in navigator) {
                                                navigator.serviceWorker.register('./sw.js')
                                                    .then(function(registration) {
                                                        console.log('Service Worker registered with scope:', registration.scope);
                                                    })
                                                    .catch(function(error) {
                                                        console.log('Service Worker registration failed:', error);
                                                    });
                                            }
                                            """.trimIndent(),
                                            null
                                        )
                                    }
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    errorCode: Int,
                                    description: String?,
                                    failingUrl: String?
                                ) {
                                    Log.e(TAG, "WebView error: $description")
                                }
                            }

                            pwaPath?.let { path ->
                                val htmlFile = File(path, "index.html")
                                if (htmlFile.exists()) {
                                    loadUrl("file://$htmlFile")
                                } else {
                                    loadUrl("file://$path")
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun installPwa() {
        // For installation, we'll show a message to the user
        // In a real implementation, we could create a Trusted Web Activity or use other methods
        Log.d(TAG, "Install PWA clicked")
        
        // Try to trigger the PWA installation prompt
        webView?.evaluateJavascript(
            """
            if ('serviceWorker' in navigator) {
                navigator.serviceWorker.ready.then(function(registration) {
                    if (registration.active) {
                        // Try to show installation prompt
                        if ('beforeinstallprompt' in window) {
                            window.dispatchEvent(new Event('beforeinstallprompt'));
                        } else {
                            // Fallback: Show instructions
                            alert('To install this PWA, use the browser menu and select "Add to Home screen"');
                        }
                    }
                });
            } else {
                alert('PWA installation requires a service worker. Please check if sw.js exists.');
            }
            """.trimIndent(),
            null
        )
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 and above, we need to request MANAGE_EXTERNAL_STORAGE
            if (!android.os.Environment.isExternalStorageManager()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
        } else {
            // For older versions, request READ_EXTERNAL_STORAGE
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    Log.d(TAG, "Storage permission granted")
                } else {
                    Log.d(TAG, "Storage permission denied")
                }
            }
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }
}