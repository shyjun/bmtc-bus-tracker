package com.bmtc.bustracker.data.local

import android.content.Context
import android.content.SharedPreferences
import com.bmtc.bustracker.data.remote.LiveLocationDetails
import com.google.gson.Gson

class PreferencesHelper(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveBusNumber(busNumber: String) {
        sharedPreferences.edit().putString(KEY_BUS_NUMBER, busNumber).apply()
    }

    fun getBusNumber(): String {
        return sharedPreferences.getString(KEY_BUS_NUMBER, "") ?: ""
    }

    fun saveVehicleId(vehicleId: Int) {
        sharedPreferences.edit().putInt(KEY_VEHICLE_ID, vehicleId).apply()
    }

    fun getVehicleId(): Int {
        return sharedPreferences.getInt(KEY_VEHICLE_ID, 0)
    }

    fun saveMonitoringEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_MONITORING_ENABLED, enabled).apply()
    }

    fun isMonitoringEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_MONITORING_ENABLED, true)
    }

    fun saveLastLocation(location: LiveLocationDetails?) {
        val json = if (location != null) gson.toJson(location) else null
        sharedPreferences.edit().putString(KEY_LAST_LOCATION, json).apply()
    }

    fun getLastLocation(): LiveLocationDetails? {
        val json = sharedPreferences.getString(KEY_LAST_LOCATION, null) ?: return null
        return try {
            gson.fromJson(json, LiveLocationDetails::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun saveStaleNotificationSent(sent: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_STALE_NOTIFICATION_SENT, sent).apply()
    }

    fun isStaleNotificationSent(): Boolean {
        return sharedPreferences.getBoolean(KEY_STALE_NOTIFICATION_SENT, false)
    }

    companion object {
        private const val PREFS_NAME = "bmtc_bus_tracker_prefs"
        private const val KEY_BUS_NUMBER = "bus_number"
        private const val KEY_VEHICLE_ID = "vehicle_id"
        private const val KEY_MONITORING_ENABLED = "monitoring_enabled"
        private const val KEY_LAST_LOCATION = "last_location"
        private const val KEY_STALE_NOTIFICATION_SENT = "stale_notification_sent"
    }
}
