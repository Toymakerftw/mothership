package com.example.mothership.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("mothership_prefs", Context.MODE_PRIVATE)

    fun saveOpenRouterApiKey(apiKey: String) {
        sharedPreferences.edit().putString(OPENROUTER_API_KEY, apiKey).apply()
    }

    fun getOpenRouterApiKey(): String? {
        return sharedPreferences.getString(OPENROUTER_API_KEY, null)
    }

    companion object {
        private const val OPENROUTER_API_KEY = "openrouter_api_key"
    }
}