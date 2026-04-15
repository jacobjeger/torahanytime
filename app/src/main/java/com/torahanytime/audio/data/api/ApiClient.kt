package com.torahanytime.audio.data.api

import com.squareup.moshi.Moshi
import com.torahanytime.audio.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://api.torahanytime.com/"

    private val moshi = Moshi.Builder()
        .add(LenientBooleanAdapter())
        .build()

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = AuthManager.getToken()
        val request = if (token != null) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }
        chain.proceed(request)
    }

    /** Retries GET requests up to 2 times on network failure with exponential backoff */
    private val retryInterceptor = Interceptor { chain ->
        val request = chain.request()
        val isGet = request.method == "GET"
        var lastException: IOException? = null

        val maxRetries = if (isGet) 2 else 0
        for (attempt in 0..maxRetries) {
            try {
                val response = chain.proceed(request)
                // Handle 401 unauthorized
                if (response.code == 401) {
                    response.close()
                    AuthManager.logout()
                }
                return@Interceptor response
            } catch (e: IOException) {
                lastException = e
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep((attempt + 1) * 1000L) // 1s, 2s backoff
                    } catch (_: InterruptedException) {
                        throw e
                    }
                }
            }
        }
        throw lastException!!
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(retryInterceptor)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
            }
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: TATApiService = retrofit.create(TATApiService::class.java)
}
