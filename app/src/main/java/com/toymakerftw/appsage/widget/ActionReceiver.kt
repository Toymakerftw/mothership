package com.toymakerftw.appsage.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class ActionReceiver : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val prompt = parameters[ActionParameters.Key<String>("actionPrompt")] ?: return
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        
        if (appWidgetId != -1) {
            val workRequest = OneTimeWorkRequestBuilder<WidgetGenerationWorker>()
                .setInputData(
                    Data.Builder()
                        .putString("prompt", prompt)
                        .putInt("appWidgetId", appWidgetId)
                        .build()
                )
                .build()
                
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
