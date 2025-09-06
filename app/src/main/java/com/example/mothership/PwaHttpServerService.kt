package com.example.mothership

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.util.*

class PwaHttpServerService : Service() {
    companion object {
        private const val TAG = "PwaHttpServerService"
        const val ACTION_START_SERVER = "com.example.mothership.START_SERVER"
        const val ACTION_STOP_SERVER = "com.example.mothership.STOP_SERVER"
        const val EXTRA_PWA_UUID = "pwa_uuid"
        const val EXTRA_PORT = "port"
    }

    // Map to store multiple server instances by port
    private val serverMap = mutableMapOf<Int, PwaHttpServer>()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PWA HTTP Server Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            Log.w(TAG, "Service started with a null intent. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent.action) {
            ACTION_START_SERVER -> {
                val pwaUuid = intent.getStringExtra(EXTRA_PWA_UUID)
                val port = intent.getIntExtra(EXTRA_PORT, 8080)
                
                if (pwaUuid != null) {
                    startHttpServer(pwaUuid, port)
                } else {
                    Log.e(TAG, "No PWA UUID provided to start server")
                }
            }
            ACTION_STOP_SERVER -> {
                val port = intent.getIntExtra(EXTRA_PORT, 8080)
                stopHttpServer(port)
            }
            else -> {
                Log.w(TAG, "Unknown action: ${intent.action}")
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop all servers when service is destroyed
        stopAllServers()
        Log.d(TAG, "PWA HTTP Server Service destroyed")
    }

    private fun startHttpServer(pwaUuid: String, port: Int) {
        try {
            // Stop any existing server on this port
            serverMap[port]?.stop()
            
            val pwaDir = File(getExternalFilesDir(null), pwaUuid)
            if (!pwaDir.exists() || !pwaDir.isDirectory) {
                Log.e(TAG, "PWA directory does not exist: ${pwaDir.absolutePath}")
                return
            }

            val httpServer = PwaHttpServer(port, pwaDir)
            httpServer.start()
            serverMap[port] = httpServer
            Log.d(TAG, "HTTP server started on port $port for PWA $pwaUuid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start HTTP server on port $port", e)
        }
    }

    private fun stopHttpServer(port: Int) {
        try {
            serverMap[port]?.stop()
            serverMap.remove(port)
            Log.d(TAG, "HTTP server stopped on port $port")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping HTTP server on port $port", e)
        }
    }
    
    private fun stopAllServers() {
        try {
            serverMap.values.forEach { server ->
                try {
                    server.stop()
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping server", e)
                }
            }
            serverMap.clear()
            Log.d(TAG, "All HTTP servers stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping all HTTP servers", e)
        }
    }

    class PwaHttpServer(private val port: Int, private val pwaDir: File) : NanoHTTPD(port) {
        companion object {
            private const val TAG = "PwaHttpServer"
        }

        override fun serve(session: IHTTPSession?): Response {
            try {
                val uri = session?.uri ?: "/"
                val filePath = if (uri == "/" || uri.isEmpty()) {
                    File(pwaDir, "index.html")
                } else {
                    // Sanitize the URI to prevent directory traversal attacks
                    val cleanUri = uri.replace("..", "").replace("//", "/")
                    File(pwaDir, cleanUri.trimStart('/'))
                }

                // Check if file exists and is within the PWA directory
                if (!filePath.exists() || !filePath.isFile || !filePath.absolutePath.startsWith(pwaDir.absolutePath)) {
                    Log.w(TAG, "File not found or access denied: ${filePath.absolutePath}")
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found")
                }

                // Determine content type
                val mimeType = when {
                    filePath.name.endsWith(".html", ignoreCase = true) -> "text/html"
                    filePath.name.endsWith(".css", ignoreCase = true) -> "text/css"
                    filePath.name.endsWith(".js", ignoreCase = true) -> "application/javascript"
                    filePath.name.endsWith(".json", ignoreCase = true) -> "application/json"
                    filePath.name.endsWith(".png", ignoreCase = true) -> "image/png"
                    filePath.name.endsWith(".jpg", ignoreCase = true) || filePath.name.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                    filePath.name.endsWith(".gif", ignoreCase = true) -> "image/gif"
                    filePath.name.endsWith(".svg", ignoreCase = true) -> "image/svg+xml"
                    filePath.name.endsWith(".ico", ignoreCase = true) -> "image/x-icon"
                    filePath.name.endsWith(".woff", ignoreCase = true) -> "font/woff"
                    filePath.name.endsWith(".woff2", ignoreCase = true) -> "font/woff2"
                    else -> "application/octet-stream"
                }

                // Serve the file
                val fis = FileInputStream(filePath)
                val fileSize = filePath.length()
                
                Log.d(TAG, "Serving file: ${filePath.name} (Size: $fileSize bytes, Type: $mimeType)")
                
                return newFixedLengthResponse(Response.Status.OK, mimeType, fis, fileSize)
            } catch (e: Exception) {
                Log.e(TAG, "Error serving file", e)
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal server error: ${e.message}")
            }
        }
    }
}