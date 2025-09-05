package com.example.mothership.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mothership.MainActivity

class PwaNotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "pwa_generation_channel"
        private const val CHANNEL_NAME = "PWA Generation"
        private const val CHANNEL_DESCRIPTION = "Notifications for PWA generation progress and results"
        const val NOTIFICATION_ID_PROGRESS = 1
        const val NOTIFICATION_ID_RESULT = 2
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(false)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showProgressNotification(pwaName: String) {
        // Create intent to bring existing app instance to foreground
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Generating PWA")
            .setContentText("Generating $pwaName...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setProgress(0, 0, true) // Indeterminate progress
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID_PROGRESS, notification)
    }

    fun showSuccessNotification(pwaName: String) {
        // Create intent to bring existing app instance to foreground and navigate to app list
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Add extra to indicate we want to go to the app list
            putExtra("navigate_to", "appList")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("PWA Generation Complete")
            .setContentText("$pwaName has been generated successfully!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID_RESULT, notification)
        
        // Cancel progress notification
        NotificationManagerCompat.from(context)
            .cancel(NOTIFICATION_ID_PROGRESS)
    }

    fun showErrorNotification(pwaName: String, errorMessage: String) {
        // Create intent to bring existing app instance to foreground
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("PWA Generation Failed")
            .setContentText("Failed to generate $pwaName: $errorMessage")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID_RESULT, notification)
        
        // Cancel progress notification
        NotificationManagerCompat.from(context)
            .cancel(NOTIFICATION_ID_PROGRESS)
    }

    fun cancelProgressNotification() {
        NotificationManagerCompat.from(context)
            .cancel(NOTIFICATION_ID_PROGRESS)
    }
}