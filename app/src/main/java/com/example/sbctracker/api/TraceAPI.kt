package com.example.sbctracker.api

import com.example.sbctracker.models.User
import com.google.gson.JsonObject
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Json
import kotlinx.coroutines.Deferred
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

/**
 * A reftrofit service to get the list of available tasks
 * */

interface TraceNetworkService {

    @POST("maps/real")
    fun postLocationUpdates(@Header("security_key") key: String, @Body request: RequestBody): Call<String>

    @POST("fetch/userdetails/")
    fun getUserDetails(@Header("security_key") key: String, @Body request: RequestBody): Deferred<NetworkUser>

    @POST("push/staff")
    fun login(@Header("security_key") key: String, @Body request: RequestBody): Call<String>

    @POST("push/staff")
    fun logout(@Header("security_key") key: String, @Body request: RequestBody): Call<String>
}

/**
 * Main entry point for API access
 * */
object TraceNetwork {
    // Configure retrofit to parse JSON and use coroutines
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://165.22.51.149:4000/")
        .addConverterFactory(MoshiConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

    val api = retrofit.create(TraceNetworkService::class.java)

}
