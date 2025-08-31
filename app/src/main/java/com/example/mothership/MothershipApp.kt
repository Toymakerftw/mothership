
package com.example.mothership

import android.app.Application
import com.example.mothership.api.MothershipApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MothershipApp : Application() {

    lateinit var mothershipApi: MothershipApi

    override fun onCreate() {
        super.onCreate()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://openrouter.ai/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        mothershipApi = retrofit.create(MothershipApi::class.java)
    }
}
