package com.bmtc.bustracker.data.remote

import com.google.gson.annotations.SerializedName

data class ListVehiclesRequest(
    @SerializedName("vehicleRegNo")
    val vehicleRegNo: String
)

data class VehicleData(
    @SerializedName("vehicleid")
    val vehicleId: Int,
    @SerializedName("vehicleregno")
    val vehicleRegNo: String
)

data class ListVehiclesResponse(
    @SerializedName("data")
    val data: List<VehicleData>?
)

data class VehicleTripDetailsRequest(
    @SerializedName("vehicleId")
    val vehicleId: Int
)

data class VehicleTripDetailsResponse(
    @SerializedName("Message")
    val message: String?,
    @SerializedName("Issuccess")
    val isSuccess: Boolean,
    @SerializedName("exception")
    val exception: String?,
    @SerializedName("RowCount")
    val rowCount: Int,
    @SerializedName("responsecode")
    val responseCode: Int,
    @SerializedName("LiveLocation")
    val liveLocation: List<LiveLocationDetails>?
)

data class LiveLocationDetails(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("location")
    val location: String?,
    @SerializedName("lastrefreshon")
    val lastRefreshOn: String?,
    @SerializedName("nextstop")
    val nextStop: String?,
    @SerializedName("previousstop")
    val previousStop: String?,
    @SerializedName("vehicleid")
    val vehicleId: Int,
    @SerializedName("vehiclenumber")
    val vehicleNumber: String?,
    @SerializedName("routeno")
    val routeNo: String?,
    @SerializedName("servicetypeid")
    val serviceTypeId: Int,
    @SerializedName("servicetype")
    val serviceType: String?,
    @SerializedName("heading")
    val heading: Double?,
    @SerializedName("trip_status")
    val tripStatus: Int?
)
