package com.example.network

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val TAG = "ApiClient"

    private var appContext: Context? = null
    private var retrofitInstance: Retrofit? = null
    private var apiServiceInstance: ApiService? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
        NetworkConfig.initialize(context)
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val apiService: ApiService
        get() {
            val targetBaseUrl = NetworkConfig.getBaseUrl(appContext)
            val currentRetrofitUrl = retrofitInstance?.baseUrl()?.toString()
            if (currentRetrofitUrl != null && currentRetrofitUrl != targetBaseUrl) {
                Log.i(TAG, "ApiClient baseUrl changed from $currentRetrofitUrl to $targetBaseUrl. Rebuilding.")
                rebuild()
            }
            return apiServiceInstance ?: synchronized(this) {
                val activeTarget = NetworkConfig.getBaseUrl(appContext)
                val activeRetrofit = retrofitInstance?.baseUrl()?.toString()
                if (activeRetrofit != null && activeRetrofit != activeTarget) {
                    retrofitInstance = null
                    apiServiceInstance = null
                }
                apiServiceInstance ?: createRetrofit().create(ApiService::class.java).also {
                    apiServiceInstance = it
                }
            }
        }

    private fun createRetrofit(): Retrofit {
        val baseUrl = NetworkConfig.getBaseUrl(appContext)
        Log.i(TAG, "Initializing Retrofit with Base URL: $baseUrl")

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        retrofitInstance = retrofit
        return retrofit
    }

    fun rebuild() {
        synchronized(this) {
            retrofitInstance = null
            apiServiceInstance = null
        }
        Log.i(TAG, "ApiClient rebuilt with Base URL: ${NetworkConfig.getBaseUrl(appContext)}")
    }

    data class HealthCheckResult(
        val isSuccessful: Boolean,
        val url: String,
        val statusCode: Int? = null,
        val details: String? = null
    )

    suspend fun testHealthConnection(context: Context? = null): HealthCheckResult = withContext(Dispatchers.IO) {
        val targetUrl = NetworkConfig.getBaseUrl(context ?: appContext)
        try {
            val response = apiService.checkHealth()
            if (response.isSuccessful) {
                val body = response.body()
                HealthCheckResult(
                    isSuccessful = true,
                    url = targetUrl,
                    statusCode = response.code(),
                    details = body?.get("service")?.toString() ?: "Connected successfully"
                )
            } else {
                HealthCheckResult(
                    isSuccessful = false,
                    url = targetUrl,
                    statusCode = response.code(),
                    details = "HTTP ${response.code()}: ${response.message()}"
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Health check failed for $targetUrl: ${e.message}")
            HealthCheckResult(
                isSuccessful = false,
                url = targetUrl,
                statusCode = null,
                details = e.message ?: "Connection failed"
            )
        }
    }

    suspend fun isServerReachable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.checkHealth()
            response.isSuccessful
        } catch (e: Exception) {
            Log.w(TAG, "Backend server unreachable at ${NetworkConfig.getBaseUrl(appContext)}: ${e.message}")
            false
        }
    }
}
