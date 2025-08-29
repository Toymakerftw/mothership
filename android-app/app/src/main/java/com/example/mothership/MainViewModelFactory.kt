package com.example.mothership

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mothership.api.MothershipApi

class MainViewModelFactory(private val api: MothershipApi, private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(api, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}