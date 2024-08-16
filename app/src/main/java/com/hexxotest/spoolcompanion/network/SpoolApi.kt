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
        @GET("spool")
        suspend fun getSpoolList(): List<SpoolItem>
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val retrofit = Retrofit.Builder()
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .baseUrl("$baseUrl/api/v1/")
        .build()

    val retrofitService: SpoolApiService by lazy {
        retrofit.create(SpoolApiService::class.java)
    }
}
