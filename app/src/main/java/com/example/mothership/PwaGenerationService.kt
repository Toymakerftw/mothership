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
    private val TAG = "PwaGenerationService"
    
    companion object {
        private const val CHANNEL_ID = "PWA_GENERATION_SERVICE_CHANNEL"
        private const val NOTIFICATION_ID = 1
        const val ACTION_GENERATION_COMPLETE = "com.example.mothership.GENERATION_COMPLETE"
        const val EXTRA_PWA_NAME = "pwa_name"
        const val EXTRA_SUCCESS = "success"
        const val EXTRA_ERROR_MESSAGE = "error_message"
        private const val ACTION_NOTIFY_COMPLETION = "NOTIFY_COMPLETION"
        
        fun startService(context: Context, pwaName: String) {
            val startIntent = Intent(context, PwaGenerationService::class.java)
            startIntent.putExtra(EXTRA_PWA_NAME, pwaName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent)
            } else {
                context.startService(startIntent)
            }
        }
        
        fun stopService(context: Context) {
            val stopIntent = Intent(context, PwaGenerationService::class.java)
            context.stopService(stopIntent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("PWA Generation Service", "Service is running..."))
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { 
            when (it.action) {
                ACTION_NOTIFY_COMPLETION -> {
                    val pwaName = it.getStringExtra(EXTRA_PWA_NAME) ?: "PWA"
                    val success = it.getBooleanExtra(EXTRA_SUCCESS, false)
                    val errorMessage = it.getStringExtra(EXTRA_ERROR_MESSAGE)
                    notifyGenerationComplete(pwaName, success, errorMessage)
                    return START_NOT_STICKY
                }
                else -> {
                    val pwaName = it.getStringExtra(EXTRA_PWA_NAME) ?: "PWA"
                    startForeground(NOTIFICATION_ID, createNotification("Generating PWA", "Generating $pwaName..."))
                    
                    // For now, we'll just keep the service running
                    // The actual generation happens in the ViewModel, but the service keeps the app alive
                    Log.d(TAG, "PWA generation service started for $pwaName")
                }
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "PWA generation service destroyed")
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    private fun notifyGenerationComplete(pwaName: String, success: Boolean, errorMessage: String? = null) {
        // Update the notification to show completion
        val notification = if (success) {
            createNotification("PWA Generation Complete", "$pwaName has been generated successfully!")
        } else {
            createNotification("PWA Generation Failed", "Failed to generate $pwaName: ${errorMessage ?: "Unknown error"}")
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
        
        // Send broadcast to notify the app
        val broadcastIntent = Intent(ACTION_GENERATION_COMPLETE).apply {
            setPackage(packageName) // Set the package to make it explicit
            putExtra(EXTRA_PWA_NAME, pwaName)
            putExtra(EXTRA_SUCCESS, success)
            putExtra(EXTRA_ERROR_MESSAGE, errorMessage)
        }
        sendBroadcast(broadcastIntent)
        
        // Stop the service after a delay to allow user to see the notification
        Thread {
            Thread.sleep(5000) // Show notification for 5 seconds
            stopSelf()
        }.start()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PWA Generation Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
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
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using a system icon for now
            .setOngoing(true)
            .build()
    }
}