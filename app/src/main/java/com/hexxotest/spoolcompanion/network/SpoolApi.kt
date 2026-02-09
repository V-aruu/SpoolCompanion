package com.hexxotest.spoolcompanion.network

import android.util.Log
import com.hexxotest.spoolcompanion.network.data.SpoolItem
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET

class SpoolApi(
    baseUrl: String = ""
) {

    interface SpoolApiService {
        // Spoolman v1 endpoint: /api/v1/spool
        @GET("spool")
        suspend fun getSpoolList(): List<SpoolItem>
    }

    // Tag for logcat entries related to Spoolman API calls.
    private companion object {
        private const val TAG = "SpoolApi"
        private const val MAX_PEEK_BYTES = 2048L
    }

    // Ignore unknown keys to tolerate newer Spoolman fields without breaking parsing.
    private val json = Json { ignoreUnknownKeys = true }

    // Compose the base URL once so logs and Retrofit share the same value.
    private val apiBaseUrl = "$baseUrl/api/v1/"

    // Log the base URL as soon as the API wrapper is created to aid debugging.
    init {
        Log.d(TAG, "SpoolApi initialized with baseUrl=$apiBaseUrl")
    }

    // OkHttp client that logs request/response details for tracing API calls in logcat.
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(ApiLoggingInterceptor())
        .build()

    // baseUrl is expected to be the server root (without trailing /api/v1).
    private val retrofit = Retrofit.Builder()
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .client(okHttpClient)
        .baseUrl(apiBaseUrl)
        .build()

    val retrofitService: SpoolApiService by lazy {
        retrofit.create(SpoolApiService::class.java)
    }

    // Interceptor that logs API request/response metadata and a small body preview.
    private class ApiLoggingInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val startedNs = System.nanoTime()
            Log.d(TAG, "API request -> ${request.method} ${request.url}")
            return try {
                val response = chain.proceed(request)
                val elapsedMs = (System.nanoTime() - startedNs) / 1_000_000
                val responseBody = response.body
                val contentType = responseBody?.contentType()
                val contentLength = responseBody?.contentLength() ?: -1L
                Log.d(
                    TAG,
                    "API response <- ${response.code} ${response.message} " +
                        "(${elapsedMs}ms, type=$contentType, bytes=$contentLength)"
                )
                val preview = response.peekBody(MAX_PEEK_BYTES).string()
                Log.v(TAG, "API response body preview (${MAX_PEEK_BYTES} bytes max): $preview")
                response
            } catch (e: Exception) {
                val elapsedMs = (System.nanoTime() - startedNs) / 1_000_000
                Log.e(TAG, "API call failed after ${elapsedMs}ms", e)
                throw e
            }
        }
    }
}
