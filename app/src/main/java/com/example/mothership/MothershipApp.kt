
package com.example.mothership

import android.app.Application
import com.example.mothership.api.MothershipApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MothershipApp : Application() {

    lateinit var mothershipApi: MothershipApi

    override fun onCreate() {
        super.onCreate()

        // Configure OkHttp with longer timeouts for better network reliability
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://openrouter.ai/api/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        mothershipApi = retrofit.create(MothershipApi::class.java)
    }
}
