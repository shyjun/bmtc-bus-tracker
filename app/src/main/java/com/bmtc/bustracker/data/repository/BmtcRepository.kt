package com.bmtc.bustracker.data.repository

import android.content.Context
import com.bmtc.bustracker.data.local.PreferencesHelper
import com.bmtc.bustracker.data.remote.BmtcApiService
import com.bmtc.bustracker.data.remote.ListVehiclesRequest
import com.bmtc.bustracker.data.remote.LiveLocationDetails
import com.bmtc.bustracker.data.remote.RetrofitClient
import com.bmtc.bustracker.data.remote.VehicleData
import com.bmtc.bustracker.data.remote.VehicleTripDetailsRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class TrackingUiState(
    val busNumber: String = "",
    val vehicleId: Int = 0,
    val monitoringEnabled: Boolean = true,
    val locationDetails: LiveLocationDetails? = null,
    val trackingStatus: TrackingStatus = TrackingStatus.OFFLINE,
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class TrackingStatus {
    ACTIVE,
    OFFLINE,
    NO_INTERNET
}

class BmtcRepository private constructor(context: Context) {

    private val apiService: BmtcApiService = RetrofitClient.apiService
    private val prefsHelper = PreferencesHelper(context.applicationContext)

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    init {
        val busNum = prefsHelper.getBusNumber()
        val vehId = prefsHelper.getVehicleId()
        val monEnabled = prefsHelper.isMonitoringEnabled()
        val lastLoc = prefsHelper.getLastLocation()

        val status = if (lastLoc != null) {
            calculateTrackingStatus(lastLoc.lastRefreshOn)
        } else {
            TrackingStatus.OFFLINE
        }

        if (busNum.isNotBlank() && vehId > 0) {
            _uiState.update {
                TrackingUiState(
                    busNumber = busNum,
                    vehicleId = vehId,
                    monitoringEnabled = monEnabled,
                    locationDetails = lastLoc,
                    trackingStatus = status
                )
            }
        }
    }

    fun getSavedBusNumber(): String = prefsHelper.getBusNumber()
    fun getSavedVehicleId(): Int = prefsHelper.getVehicleId()
    fun getMonitoringEnabled(): Boolean = prefsHelper.isMonitoringEnabled()
    fun isStaleNotificationSent(): Boolean = prefsHelper.isStaleNotificationSent()
    fun setStaleNotificationSent(sent: Boolean) = prefsHelper.saveStaleNotificationSent(sent)
    fun getMonitoringInterval(): Int = prefsHelper.getMonitoringInterval()
    fun getOfflineNotificationInterval(): Int = prefsHelper.getOfflineNotificationInterval()
    fun setMonitoringInterval(seconds: Int) = prefsHelper.saveMonitoringInterval(seconds)
    fun setOfflineNotificationInterval(minutes: Int) = prefsHelper.saveOfflineNotificationInterval(minutes)

    fun setMonitoringEnabled(enabled: Boolean) {
        prefsHelper.saveMonitoringEnabled(enabled)
        _uiState.update { it.copy(monitoringEnabled = enabled) }
    }

    fun getNotificationsEnabled(): Boolean = prefsHelper.isNotificationsEnabled()
    fun setNotificationsEnabled(enabled: Boolean) = prefsHelper.saveNotificationsEnabled(enabled)

    fun saveTrackedBus(busNumber: String, vehicleId: Int) {
        prefsHelper.saveBusNumber(busNumber)
        prefsHelper.saveVehicleId(vehicleId)
        _uiState.update {
            it.copy(
                busNumber = busNumber,
                vehicleId = vehicleId
            )
        }
    }

    suspend fun searchVehicles(regNo: String): Result<List<VehicleData>> {
        try {
            val response = apiService.listVehicles(ListVehiclesRequest(regNo))
            val vehicles = response.data ?: emptyList()
            return Result.success(vehicles)
        } catch (e: IOException) {
            return Result.failure(e)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun fetchTrackingUpdate(busNumber: String, vehicleId: Int): Result<LiveLocationDetails?> {
        _uiState.update { it.copy(isLoading = true, error = null) }
        return try {
            val response = apiService.getVehicleTripDetails(VehicleTripDetailsRequest(vehicleId))
            if (response.isSuccess && !response.liveLocation.isNullOrEmpty()) {
                val location = response.liveLocation.first()
                prefsHelper.saveLastLocation(location)

                val status = calculateTrackingStatus(location.lastRefreshOn)

                _uiState.update {
                    it.copy(
                        busNumber = busNumber,
                        vehicleId = vehicleId,
                        locationDetails = location,
                        trackingStatus = status,
                        isLoading = false,
                        error = null
                    )
                }
                Result.success(location)
            } else if (response.isSuccess && response.liveLocation.isNullOrEmpty()) {
                _uiState.update {
                    it.copy(
                        busNumber = busNumber,
                        vehicleId = vehicleId,
                        isLoading = false,
                        error = "No Active Trip Available"
                    )
                }
                Result.failure(Exception("No Active Trip Available"))
            } else {
                val errorMsg = response.message ?: "API returned failure response"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    trackingStatus = TrackingStatus.NO_INTERNET,
                    error = "No Internet Connection"
                )
            }
            Result.failure(e)
        } catch (e: Exception) {
            val errorMsg = e.message ?: e.toString()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = errorMsg
                )
            }
            Result.failure(e)
        }
    }

    fun calculateTrackingStatus(lastRefreshOnStr: String?): TrackingStatus {
        val lastRefreshDate = parseDate(lastRefreshOnStr) ?: return TrackingStatus.ACTIVE
        val diffMs = System.currentTimeMillis() - lastRefreshDate.time
        val diffMins = diffMs / (1000 * 60)
        val threshold = getOfflineNotificationInterval()
        return if (diffMins <= threshold) {
            TrackingStatus.ACTIVE
        } else {
            TrackingStatus.OFFLINE
        }
    }

    fun parseDate(dateStr: String?): Date? {
        if (dateStr.isNullOrBlank()) return null
        val normalized = dateStr.trim()
        val istZone = TimeZone.getTimeZone("Asia/Kolkata")

        val formats = listOf(
            "dd-MMM-yy HH:mm:ss",
            "dd-MMM-yyyy HH:mm:ss",
            "dd-MM-yy HH:mm:ss",
            "dd-MM-yyyy HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "dd/MM/yyyy HH:mm:ss"
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.ENGLISH)
                sdf.timeZone = istZone
                val date = sdf.parse(normalized) ?: continue
                val cal = Calendar.getInstance(istZone)
                cal.time = date
                if (cal.get(Calendar.YEAR) < 2020) continue
                return date
            } catch (_: Exception) {}
        }

        val timeRegex = Regex("""(\d{1,2}):(\d{2}):(\d{2})""")
        val m = timeRegex.find(normalized)
        if (m != null) {
            val hour = m.groupValues[1].toIntOrNull() ?: return null
            val min  = m.groupValues[2].toIntOrNull() ?: return null
            val sec  = m.groupValues[3].toIntOrNull() ?: return null
            val cal = Calendar.getInstance(istZone)
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, min)
            cal.set(Calendar.SECOND, sec)
            cal.set(Calendar.MILLISECOND, 0)
            if (cal.timeInMillis > System.currentTimeMillis() + 60 * 60 * 1000L) {
                cal.add(Calendar.DAY_OF_MONTH, -1)
            }
            return cal.time
        }

        return null
    }

    companion object {
        @Volatile
        private var INSTANCE: BmtcRepository? = null

        fun getInstance(context: Context): BmtcRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = BmtcRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}