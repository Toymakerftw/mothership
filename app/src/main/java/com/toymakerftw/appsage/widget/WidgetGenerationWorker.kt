package com.toymakerftw.appsage.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.google.ai.client.generativeai.GenerativeModel
import com.toymakerftw.appsage.data.SettingsRepository
import com.google.gson.Gson

class WidgetGenerationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_PROMPT = "prompt"
        const val KEY_APP_WIDGET_ID = "appWidgetId"
        const val KEY_WIDGET_UUID = "widget_uuid"
        const val KEY_ERROR_MESSAGE = "error_message"
    }

    override suspend fun doWork(): Result {
        val prompt = inputData.getString(KEY_PROMPT) ?: return Result.failure()
        val appWidgetId = inputData.getInt(KEY_APP_WIDGET_ID, -1)

        val context = applicationContext
        val settingsRepo = SettingsRepository(context)
        val apiKey = settingsRepo.getApiKey()
        val modelId = settingsRepo.getSelectedModel() ?: "gemini-2.5-flash"

        if (apiKey.isNullOrEmpty()) {
            return Result.failure(
                Data.Builder().putString(KEY_ERROR_MESSAGE, "API key not configured").build()
            )
        }

        try {
            val fullPrompt = """
                You are a UI layout engine. Your job is to output a single JSON object that represents a native Android Widget UI layout matching the user's prompt: "$prompt".
                
                The JSON must PERFECTLY match this structure:
                {
                  "root": {
                     "type": "column" | "row" | "text" | "button" | "spacer",
                     "text": "String for text/button",
                     "color": "#HEXCOLOR",
                     "backgroundColor": "#HEXCOLOR",
                     "fontSize": Int,
                     "fontWeight": "bold" | "normal",
                     "align": "start" | "center" | "end",
                     "cornerRadius": Int,
                     "fillMaxWidth": Boolean,
                     "fillMaxHeight": Boolean,
                     "padding": Int,
                     "actionPrompt": "String to send back if this button is tapped",
                     "children": [ array of child WidgetNode objects ]
                  },
                  "backgroundColor": "#HEXCOLOR"
                }
                
                Always return ONLY the JSON block. Do not include markdown formatting or explanations. Make the UI gorgeous and useful. You can use standard hex colors.
            """.trimIndent()

            val generativeModel = GenerativeModel(
                modelName = modelId,
                apiKey = apiKey
            )

            val response = generativeModel.generateContent(fullPrompt)
            var generatedJson = response.text ?: ""
            
            // Clean up possible markdown tags
            if (generatedJson.startsWith("```json")) {
                generatedJson = generatedJson.substringAfter("```json").substringBeforeLast("```").trim()
            } else if (generatedJson.startsWith("```")) {
                generatedJson = generatedJson.substringAfter("```").substringBeforeLast("```").trim()
            }

            // Verify it parses
            Gson().fromJson(generatedJson, WidgetLayout::class.java)

            // Always save to WidgetManager for persistence
            val widgetManager = WidgetManager(context)
            val stored = widgetManager.saveWidget(prompt, generatedJson)

            // If we have a live appWidgetId, update the on-screen widget too
            if (appWidgetId != -1) {
                val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
                updateAppWidgetState(context, glanceId) { prefs ->
                    prefs[DynamicGlanceWidget.KEY_LAYOUT_JSON] = generatedJson
                    prefs[DynamicGlanceWidget.KEY_PROMPT] = prompt
                }
                DynamicGlanceWidget().update(context, glanceId)
            }

            return Result.success(
                Data.Builder().putString(KEY_WIDGET_UUID, stored.uuid).build()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            
            // If we have a live widget, show the error on it
            if (appWidgetId != -1) {
                try {
                    val errorMessage = if (e.message?.contains("high demand", ignoreCase = true) == true) {
                        "AI Server is currently busy. Tap to try again later."
                    } else {
                        "Error: ${e.localizedMessage}"
                    }
                    
                    val errorJson = """
                    {
                      "root": {
                         "type": "column",
                         "padding": 16,
                         "align": "center",
                         "children": [
                             {
                                "type": "text",
                                "text": "Generation Failed",
                                "color": "#FFFFFF",
                                "fontWeight": "bold",
                                "padding": 8
                             },
                             {
                                "type": "text",
                                "text": "$errorMessage",
                                "color": "#FFEEEE",
                                "fontSize": 12,
                                "align": "center"
                             },
                             {
                                "type": "button",
                                "text": "Retry",
                                "color": "#FFFFFF",
                                "backgroundColor": "#FF5555",
                                "actionPrompt": "$prompt",
                                "padding": 8
                             }
                         ]
                      },
                      "backgroundColor": "#AA3333"
                    }
                    """.trimIndent()

                    val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
                    updateAppWidgetState(context, glanceId) { prefs ->
                        prefs[DynamicGlanceWidget.KEY_LAYOUT_JSON] = errorJson
                    }
                    DynamicGlanceWidget().update(context, glanceId)
                } catch (inner: Exception) {
                    inner.printStackTrace()
                }
            }
            
            return Result.failure(
                Data.Builder().putString(KEY_ERROR_MESSAGE, e.localizedMessage ?: "Unknown error").build()
            )
        }
    }
}
