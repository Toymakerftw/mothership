package com.toymakerftw.appsage.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ActionReceiver : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val prompt = parameters[ActionParameters.Key<String>("actionPrompt")] ?: return
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        
        if (appWidgetId != -1) {
            if (prompt.startsWith("local:")) {
                handleLocalAction(context, glanceId, prompt)
            } else {
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

    private suspend fun handleLocalAction(context: Context, glanceId: GlanceId, action: String) {
        // Syntax: local:toggle:key, local:set:key:value, local:inc:key, local:dec:key
        val parts = action.split(":")
        if (parts.size < 3) return
        
        val command = parts[1]
        val key = parts[2]
        
        updateAppWidgetState(context, glanceId) { prefs ->
            val stateJson = prefs[DynamicGlanceWidget.KEY_STATE_JSON] ?: "{}"
            val type = object : TypeToken<MutableMap<String, Any>>() {}.type
            val state: MutableMap<String, Any> = try {
                Gson().fromJson(stateJson, type)
            } catch (e: Exception) {
                mutableMapOf()
            }

            when (command) {
                "toggle" -> {
                    val current = state[key]
                    state[key] = !(current == true || current == "true")
                }
                "set" -> {
                    if (parts.size >= 4) {
                        val value = parts[3]
                        state[key] = when {
                            value == "true" -> true
                            value == "false" -> false
                            value.toIntOrNull() != null -> value.toInt()
                            else -> value
                        }
                    }
                }
                "inc" -> {
                    val current = (state[key] as? Number)?.toInt() ?: 0
                    state[key] = current + 1
                }
                "dec" -> {
                    val current = (state[key] as? Number)?.toInt() ?: 0
                    state[key] = current - 1
                }
            }
            
            prefs[DynamicGlanceWidget.KEY_STATE_JSON] = Gson().toJson(state)
        }
        DynamicGlanceWidget().update(context, glanceId)
    }
}
