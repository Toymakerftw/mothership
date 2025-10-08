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

class SettingsRepository(private val context: Context) {

    private val API_KEY_KEY = stringPreferencesKey("api_key")
    private val THEME_PREFERENCE_KEY = booleanPreferencesKey("theme_preference")

    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY_KEY] = apiKey
        }
    }

    suspend fun getApiKey(): String? {
        val preferences = context.dataStore.data.first()
        return preferences[API_KEY_KEY]
    }

    suspend fun setThemePreference(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[THEME_PREFERENCE_KEY] = isDark
        }
    }

    suspend fun getThemePreference(): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[THEME_PREFERENCE_KEY] ?: false // Default to light theme
    }
}