package com.toymakerftw.appsage

import android.app.Application
import com.toymakerftw.appsage.api.AppsageApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AppsageApp : Application() {

    lateinit var appsageApi: AppsageApi

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

        appsageApi = retrofit.create(AppsageApi::class.java)
    }
}