package com.example.timelineplanner.data.remote

import android.content.SharedPreferences
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object SyncClient {

    private const val DEFAULT_SERVER_URL = "http://115.190.253.67:5000/"

    fun getServerUrl(prefs: SharedPreferences? = null): String {
        val configured = prefs?.getString("server_url", "")?.trim()
        return if (!configured.isNullOrEmpty()) configured else DEFAULT_SERVER_URL
    }

    private fun buildApi(serverUrl: String): SyncApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(serverUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SyncApi::class.java)
    }

    private var cachedApi: SyncApi? = null
    private var cachedUrl: String? = null
    private var prefsRef: SharedPreferences? = null

    fun createApi(prefs: SharedPreferences? = null): SyncApi {
        prefsRef = prefs
        val url = getServerUrl(prefs)
        if (cachedApi != null && cachedUrl == url) return cachedApi!!
        cachedApi = buildApi(url)
        cachedUrl = url
        return cachedApi!!
    }

    fun refresh(): SyncApi {
        cachedApi = null
        cachedUrl = null
        return createApi(prefsRef)
    }
}
