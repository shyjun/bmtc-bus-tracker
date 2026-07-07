package com.bmtc.bustracker.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface BmtcApiService {
    @Headers(
        "Accept: application/json, text/plain, */*",
        "Content-Type: application/json",
        "Origin: https://nammabmtcapp.karnataka.gov.in",
        "Referer: https://nammabmtcapp.karnataka.gov.in/",
        "User-Agent: Mozilla/5.0",
        "deviceType: WEB",
        "lan: en"
    )
    @POST("WebAPI/ListVehicles")
    suspend fun listVehicles(
        @Body request: ListVehiclesRequest
    ): ListVehiclesResponse

    @Headers(
        "Accept: application/json, text/plain, */*",
        "Content-Type: application/json",
        "Origin: https://nammabmtcapp.karnataka.gov.in",
        "Referer: https://nammabmtcapp.karnataka.gov.in/",
        "User-Agent: Mozilla/5.0",
        "deviceType: WEB",
        "lan: en"
    )
    @POST("WebAPI/VehicleTripDetails_v2")
    suspend fun getVehicleTripDetails(
        @Body request: VehicleTripDetailsRequest
    ): VehicleTripDetailsResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://bmtcmobileapi.karnataka.gov.in/"

    val apiService: BmtcApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BmtcApiService::class.java)
    }
}
