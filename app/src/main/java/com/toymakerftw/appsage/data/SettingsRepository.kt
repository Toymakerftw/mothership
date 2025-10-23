package com.toymakerftw.appsage.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(val context: Context) {

    private val API_KEY_KEY = stringPreferencesKey("api_key")
    private val THEME_PREFERENCE_KEY = booleanPreferencesKey("theme_preference")
    private val SELECTED_MODEL_KEY = stringPreferencesKey("selected_model")

    suspend fun saveApiKey(apiKey: String) {
        // Validate API key format (basic validation - should start with 'sk-' for most API providers)
        if (apiKey.isNotEmpty() && !apiKey.startsWith("sk-")) {
            android.util.Log.w("SettingsRepository", "API key does not follow standard format (should start with 'sk-')")
        }
        
        try {
            context.dataStore.edit { preferences ->
                preferences[API_KEY_KEY] = apiKey
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error saving API key", e)
            // Don't expose the API key in logs - just log generic error
            throw e
        }
    }

    suspend fun getApiKey(): String? {
        return try {
            val preferences = context.dataStore.data.first()
            preferences[API_KEY_KEY]
        } catch (e: Exception) {
            // Don't log sensitive information
            android.util.Log.e("SettingsRepository", "Error reading API key from DataStore", e)
            null
        }
    }

    fun getApiKeyFlow() = context.dataStore.data.map { preferences ->
        preferences[API_KEY_KEY]
    }

    suspend fun setThemePreference(isDark: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[THEME_PREFERENCE_KEY] = isDark
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error setting theme preference", e)
            throw e
        }
    }

    fun getThemePreferenceFlow() = context.dataStore.data.map { preferences ->
        preferences[THEME_PREFERENCE_KEY] ?: false // Default to light theme
    }

    suspend fun setSelectedModel(modelId: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[SELECTED_MODEL_KEY] = modelId
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error setting selected model", e)
            throw e
        }
    }

    fun getSelectedModelFlow() = context.dataStore.data.map { preferences ->
        preferences[SELECTED_MODEL_KEY] ?: "x-ai/grok-4-fast" // Default to the same as MainViewModel
    }

    suspend fun getSelectedModel(): String? {
        return try {
            val preferences = context.dataStore.data.first()
            preferences[SELECTED_MODEL_KEY]
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error reading selected model from DataStore", e)
            null
        }
    }
}