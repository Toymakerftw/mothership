package com.toymakerftw.appsage

import android.app.Application
import com.toymakerftw.appsage.api.AppsageApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AppsageApp : Application() {

    lateinit var appsageApi: AppsageApi

    override fun onCreate() {
        super.onCreate()

        // Configure OkHttp with longer timeouts and logging for better debugging
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)  // Increased timeout
            .readTimeout(120, TimeUnit.SECONDS)    // Increased timeout for potentially large responses
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)    // Add logging to help debug network issues
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://openrouter.ai/api/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        appsageApi = retrofit.create(AppsageApi::class.java)
    }
}