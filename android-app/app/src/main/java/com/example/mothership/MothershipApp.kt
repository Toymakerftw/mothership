package com.example.mothership

import android.app.Application
import com.example.mothership.api.MothershipApi

class MothershipApp : Application() {

    lateinit var api: MothershipApi

    override fun onCreate() {
        super.onCreate()
        api = MothershipApi.create()
    }
}