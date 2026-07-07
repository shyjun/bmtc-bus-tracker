package com.bmtc.bustracker.data.repository

import android.content.Context
import com.bmtc.bustracker.data.local.PreferencesHelper
import com.bmtc.bustracker.data.remote.BmtcApiService
import com.bmtc.bustracker.data.remote.LiveLocationDetails
import com.bmtc.bustracker.data.remote.RetrofitClient
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
    val busNumber: String = "KA57F4864",
    val vehicleId: Int = 25597,
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
        // Restore saved state on startup
        val busNum = prefsHelper.getBusNumber()
        val vehId = prefsHelper.getVehicleId()
        val monEnabled = prefsHelper.isMonitoringEnabled()
        val lastLoc = prefsHelper.getLastLocation()

        val status = if (lastLoc != null) {
            calculateTrackingStatus(lastLoc.lastRefreshOn)
        } else {
            TrackingStatus.ACTIVE
        }

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

    fun getSavedBusNumber(): String = prefsHelper.getBusNumber()
    fun getSavedVehicleId(): Int = prefsHelper.getVehicleId()
    fun getMonitoringEnabled(): Boolean = prefsHelper.isMonitoringEnabled()
    fun isStaleNotificationSent(): Boolean = prefsHelper.isStaleNotificationSent()
    fun setStaleNotificationSent(sent: Boolean) = prefsHelper.saveStaleNotificationSent(sent)

    fun setMonitoringEnabled(enabled: Boolean) {
        prefsHelper.saveMonitoringEnabled(enabled)
        _uiState.update { it.copy(monitoringEnabled = enabled) }
    }

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

    suspend fun fetchTrackingUpdate(busNumber: String, vehicleId: Int): Result<LiveLocationDetails?> {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val response = apiService.getVehicleTripDetails(VehicleTripDetailsRequest(vehicleId))
            if (response.isSuccess) {
                val location = response.liveLocation?.firstOrNull()
                
                // If we got a location, update preferences and state
                if (location != null) {
                    prefsHelper.saveLastLocation(location)
                }
                
                val status = calculateTrackingStatus(location?.lastRefreshOn ?: _uiState.value.locationDetails?.lastRefreshOn)

                _uiState.update {
                    it.copy(
                        busNumber = busNumber,
                        vehicleId = vehicleId,
                        locationDetails = location ?: it.locationDetails,
                        trackingStatus = status,
                        isLoading = false,
                        error = null
                    )
                }
                return Result.success(location)
            } else {
                val errorMsg = response.message ?: "API returned failure response"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                }
                return Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            // Internet unavailable
            _uiState.update {
                it.copy(
                    isLoading = false,
                    trackingStatus = TrackingStatus.NO_INTERNET,
                    error = null // Do not show API error message, just update status
                )
            }
            return Result.failure(e)
        } catch (e: Exception) {
            val errorMsg = e.message ?: e.toString()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = errorMsg
                )
            }
            return Result.failure(e)
        }
    }

    fun calculateTrackingStatus(lastRefreshOnStr: String?): TrackingStatus {
        val lastRefreshDate = parseDate(lastRefreshOnStr) ?: return TrackingStatus.ACTIVE
        val diffMs = System.currentTimeMillis() - lastRefreshDate.time
        val diffMins = diffMs / (1000 * 60)
        return if (diffMins <= 15) {
            TrackingStatus.ACTIVE
        } else {
            TrackingStatus.OFFLINE
        }
    }

    fun parseDate(dateStr: String?): Date? {
        if (dateStr.isNullOrBlank()) return null
        val normalized = dateStr.trim()
        val istZone = TimeZone.getTimeZone("Asia/Kolkata")

        // Try every known format; skip any result whose year < 2020
        // (guards against 2-digit years being read as e.g. year 26 AD)
        val formats = listOf(
            "dd-MMM-yy HH:mm:ss",   // 2-digit year, alpha month  e.g. 07-Jul-26 20:10:33
            "dd-MMM-yyyy HH:mm:ss", // 4-digit year, alpha month  e.g. 07-Jul-2026 20:10:33
            "dd-MM-yy HH:mm:ss",    // 2-digit year, numeric month e.g. 07-07-26 20:10:33
            "dd-MM-yyyy HH:mm:ss",  // 4-digit year, numeric month e.g. 07-07-2026 20:10:33
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "dd/MM/yyyy HH:mm:ss"
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.ENGLISH)
                sdf.timeZone = istZone
                val date = sdf.parse(normalized) ?: continue
                // Reject obviously wrong years caused by 2-digit year mis-parses
                val cal = Calendar.getInstance(istZone)
                cal.time = date
                if (cal.get(Calendar.YEAR) < 2020) continue
                return date
            } catch (_: Exception) {}
        }

        // Fallback: extract HH:mm:ss from anywhere in the string and combine with TODAY's IST date.
        // This handles completely unknown date formats while still giving a correct freshness check.
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
            // If the resulting time is more than 1 hour in the future, assume it belongs to yesterday
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
