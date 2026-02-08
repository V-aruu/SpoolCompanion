package com.hexxotest.spoolcompanion.network

import com.hexxotest.spoolcompanion.network.data.SpoolItem
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
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

    // Ignore unknown keys to tolerate newer Spoolman fields without breaking parsing.
    private val json = Json { ignoreUnknownKeys = true }

    // baseUrl is expected to be the server root (without trailing /api/v1).
    private val retrofit = Retrofit.Builder()
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .baseUrl("$baseUrl/api/v1/")
        .build()

    val retrofitService: SpoolApiService by lazy {
        retrofit.create(SpoolApiService::class.java)
    }
}
