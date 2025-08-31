
package com.example.mothership

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mothership.data.SettingsRepository

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    private val mothershipApp = context.applicationContext as MothershipApp

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(mothershipApp.mothershipApi, SettingsRepository(context), context) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(SettingsRepository(context)) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
