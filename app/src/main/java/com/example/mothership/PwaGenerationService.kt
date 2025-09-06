package com.example.mothership

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class PwaGenerationService : Service() {

    companion object {
        private const val TAG = "PwaGenerationService"
        private const val CHANNEL_ID = "PWA_GENERATION_SERVICE_CHANNEL"
        private const val NOTIFICATION_ID = 1

        const val ACTION_GENERATION_COMPLETE = "com.example.mothership.GENERATION_COMPLETE"
        const val EXTRA_PWA_NAME = "pwa_name"
        const val EXTRA_SUCCESS = "success"
        const val EXTRA_ERROR_MESSAGE = "error_message"

        private const val ACTION_START_GENERATION = "com.example.mothership.START_GENERATION"
        private const val ACTION_STOP_SERVICE = "com.example.mothership.STOP_SERVICE"

        fun startService(context: Context, pwaName: String) {
            val intent = Intent(context, PwaGenerationService::class.java).apply {
                action = ACTION_START_GENERATION
                putExtra(EXTRA_PWA_NAME, pwaName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, PwaGenerationService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "PWA Generation Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            Log.w(TAG, "Service started with a null intent. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent.action) {
            ACTION_START_GENERATION -> {
                val pwaName = intent.getStringExtra(EXTRA_PWA_NAME) ?: "PWA"
                Log.d(TAG, "Starting PWA generation for: $pwaName")
                startForeground(NOTIFICATION_ID, createNotification("Generating PWA", "Generating $pwaName..."))
            }
            ACTION_STOP_SERVICE -> {
                Log.d(TAG, "Stopping PWA generation service")
                stopSelf()
            }
            else -> {
                Log.w(TAG, "Unknown action: ${intent.action}")
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "PWA generation service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PWA Generation Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for PWA generation service notifications"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setShowBadge(false)
                }
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with a proper icon
            .setOngoing(true)
            .build()
    }
}