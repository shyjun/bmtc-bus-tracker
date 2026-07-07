package com.bmtc.bustracker.ui

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bmtc.bustracker.data.repository.BmtcRepository
import com.bmtc.bustracker.data.repository.TrackingStatus
import com.bmtc.bustracker.service.TrackingService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BusTrackerViewModel(private val application: Application) : AndroidViewModel(application) {

    private val repository = BmtcRepository.getInstance(application)
    val uiState: StateFlow<com.bmtc.bustracker.data.repository.TrackingUiState> = repository.uiState

    var busInput by mutableStateOf("")
        private set

    init {
        busInput = repository.getSavedBusNumber()
        
        // Start foreground service if monitoring is enabled on startup
        if (repository.getMonitoringEnabled()) {
            startTrackingService()
        }
    }

    fun onBusInputChange(newValue: String) {
        busInput = newValue.uppercase()
    }

    fun onTrackClicked() {
        if (busInput.isBlank()) return

        val vehicleId = 25597 // Hardcoded vehicle ID for Version 1
        repository.saveTrackedBus(busInput, vehicleId)

        viewModelScope.launch {
            repository.fetchTrackingUpdate(busInput, vehicleId)
            
            // If monitoring is enabled, restart the service to apply new bus info
            if (repository.getMonitoringEnabled()) {
                startTrackingService()
            }
        }
    }

    fun onRefreshClicked() {
        val state = uiState.value
        viewModelScope.launch {
            val result = repository.fetchTrackingUpdate(state.busNumber, state.vehicleId)
            
            if (result.isSuccess) {
                // Cancel stale notification and reset notification flag
                cancelStaleNotification()
                repository.setStaleNotificationSent(false)
            }
        }
    }

    fun onMonitoringToggled(enabled: Boolean) {
        repository.setMonitoringEnabled(enabled)
        if (enabled) {
            startTrackingService()
        } else {
            stopTrackingService()
            cancelStaleNotification()
            repository.setStaleNotificationSent(false)
    }

    fun parseDate(dateStr: String?): java.util.Date? {
        return repository.parseDate(dateStr)
    }

    private fun startTrackingService() {
        val intent = Intent(application, TrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            application.startForegroundService(intent)
        } else {
            application.startService(intent)
        }
    }

    private fun stopTrackingService() {
        val intent = Intent(application, TrackingService::class.java)
        application.stopService(intent)
    }

    private fun cancelStaleNotification() {
        val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1002) // STALE_NOTIFICATION_ID is 1002
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BusTrackerViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BusTrackerViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
