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
import com.bmtc.bustracker.data.remote.VehicleData
import com.bmtc.bustracker.data.repository.BmtcRepository
import com.bmtc.bustracker.data.repository.TrackingUiState
import com.bmtc.bustracker.service.TrackingService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BusTrackerViewModel(private val application: Application) : AndroidViewModel(application) {

    private val repository = BmtcRepository.getInstance(application)
    val uiState: StateFlow<TrackingUiState> = repository.uiState

    var busInput by mutableStateOf("")
        private set

    var searchResults by mutableStateOf<List<VehicleData>>(emptyList())
        private set

    var showSuggestions by mutableStateOf(false)
        private set

    var isSearching by mutableStateOf(false)
        private set

    var searchError by mutableStateOf<String?>(null)
        private set

    var resolvedVehicleId by mutableStateOf<Int?>(null)
        private set

    val monitoringInterval: Int get() = repository.getMonitoringInterval()
    val offlineNotificationInterval: Int get() = repository.getOfflineNotificationInterval()
    val monitoringEnabled: Boolean get() = repository.getMonitoringEnabled()
    val notificationsEnabled: Boolean get() = repository.getNotificationsEnabled()

    var showSettingsDialog by mutableStateOf(false)
        private set
    var dialogMonitoringSecs by mutableStateOf(300)
        private set
    var dialogOfflineMins by mutableStateOf(15)
        private set

    private var searchJob: Job? = null

    init {
        val savedBus = repository.getSavedBusNumber()
        val savedVehicleId = repository.getSavedVehicleId()
        busInput = savedBus
        if (savedBus.isNotBlank() && savedVehicleId > 0) {
            resolvedVehicleId = savedVehicleId
        }
        if (repository.getMonitoringEnabled() && savedVehicleId > 0) {
            startTrackingService()
        }
    }

    fun onBusInputChange(newValue: String) {
        busInput = newValue.uppercase()
        resolvedVehicleId = null
        searchError = null

        searchJob?.cancel()
        if (busInput.isBlank()) {
            searchResults = emptyList()
            showSuggestions = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            performSearch(busInput)
        }
    }

    fun onSuggestionSelected(vehicle: VehicleData) {
        busInput = vehicle.vehicleRegNo
        resolvedVehicleId = vehicle.vehicleId
        showSuggestions = false
        searchResults = emptyList()

        repository.saveTrackedBus(vehicle.vehicleRegNo, vehicle.vehicleId)

        viewModelScope.launch {
            repository.fetchTrackingUpdate(vehicle.vehicleRegNo, vehicle.vehicleId)
            if (repository.getMonitoringEnabled()) {
                startTrackingService()
            }
        }
    }

    fun dismissSuggestions() {
        showSuggestions = false
    }

    fun onTrackClicked() {
        if (busInput.isBlank()) return

        val existingId = resolvedVehicleId
        if (existingId != null) {
            repository.saveTrackedBus(busInput, existingId)
            viewModelScope.launch {
                repository.fetchTrackingUpdate(busInput, existingId)
                if (repository.getMonitoringEnabled()) {
                    startTrackingService()
                }
            }
        } else {
            viewModelScope.launch {
                isSearching = true
                val result = repository.searchVehicles(busInput)
                isSearching = false
                result.onSuccess { vehicles ->
                    val exactMatch = vehicles.find { it.vehicleRegNo == busInput }
                    if (exactMatch != null) {
                        resolvedVehicleId = exactMatch.vehicleId
                        repository.saveTrackedBus(busInput, exactMatch.vehicleId)
                        repository.fetchTrackingUpdate(busInput, exactMatch.vehicleId)
                        if (repository.getMonitoringEnabled()) {
                            startTrackingService()
                        }
                    } else {
                        searchError = "Bus Not Found"
                    }
                }.onFailure {
                    searchError = "Bus Not Found"
                }
            }
        }
    }

    fun onRefreshClicked() {
        val state = uiState.value
        if (state.busNumber.isBlank() || state.vehicleId <= 0) return
        viewModelScope.launch {
            val result = repository.fetchTrackingUpdate(state.busNumber, state.vehicleId)
            if (result.isSuccess) {
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
    }

    fun onNotificationsToggled(enabled: Boolean) {
        repository.setNotificationsEnabled(enabled)
        if (!enabled) {
            cancelStaleNotification()
            repository.setStaleNotificationSent(false)
        }
    }

    fun openSettings() {
        dialogMonitoringSecs = repository.getMonitoringInterval()
        dialogOfflineMins = repository.getOfflineNotificationInterval()
        showSettingsDialog = true
    }

    fun onSettingsDismiss() {
        showSettingsDialog = false
    }

    fun onSettingsConfirm() {
        repository.setMonitoringInterval(dialogMonitoringSecs)
        repository.setOfflineNotificationInterval(dialogOfflineMins)
        showSettingsDialog = false
    }

    fun onMonitoringSecsDecrement() {
        val newVal = dialogMonitoringSecs - 60
        if (newVal >= 60) {
            dialogMonitoringSecs = newVal
        }
    }

    fun onMonitoringSecsIncrement() {
        val newVal = dialogMonitoringSecs + 60
        if (newVal <= 1800) {
            dialogMonitoringSecs = newVal
        }
    }

    fun onOfflineMinsDecrement() {
        val newVal = dialogOfflineMins - 5
        if (newVal >= 5) {
            dialogOfflineMins = newVal
        }
    }

    fun onOfflineMinsIncrement() {
        val newVal = dialogOfflineMins + 5
        if (newVal <= 60) {
            dialogOfflineMins = newVal
        }
    }

    fun parseDate(dateStr: String?): java.util.Date? {
        return repository.parseDate(dateStr)
    }

    private suspend fun performSearch(query: String) {
        isSearching = true
        searchError = null
        val result = repository.searchVehicles(query)
        isSearching = false
        result.onSuccess { vehicles ->
            searchResults = vehicles
            showSuggestions = vehicles.isNotEmpty()
        }.onFailure {
            searchResults = emptyList()
            showSuggestions = false
        }
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
        val notificationManager =
            application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1002)
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