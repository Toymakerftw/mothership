package com.toymakerftw.appsage

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AppsageApp : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}
