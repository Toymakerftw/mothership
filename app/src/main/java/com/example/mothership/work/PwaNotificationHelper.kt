package com.example.mothership.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mothership.R

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
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Generating PWA")
            .setContentText("Generating $pwaName...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setProgress(0, 0, true) // Indeterminate progress
            .build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID_PROGRESS, notification)
    }

    fun showSuccessNotification(pwaName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("PWA Generation Complete")
            .setContentText("$pwaName has been generated successfully!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID_RESULT, notification)
        
        // Cancel progress notification
        NotificationManagerCompat.from(context)
            .cancel(NOTIFICATION_ID_PROGRESS)
    }

    fun showErrorNotification(pwaName: String, errorMessage: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("PWA Generation Failed")
            .setContentText("Failed to generate $pwaName: $errorMessage")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
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