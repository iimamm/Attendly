package com.appsbase.attendly.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.appsbase.attendly.domain.model.AttendanceRecord
import com.appsbase.attendly.domain.model.OfficeLocation
import com.appsbase.attendly.domain.model.SimulationConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "attendly_prefs")

@Singleton
class AttendanceDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        private val KEY_OFFICE_LAT = doublePreferencesKey("office_latitude")
        private val KEY_OFFICE_LNG = doublePreferencesKey("office_longitude")
        private val KEY_OFFICE_IS_SET = booleanPreferencesKey("office_is_set")
        private val KEY_ATTENDANCE_HISTORY = stringPreferencesKey("attendance_history")
        private val KEY_BYPASS_TIME_SIMULATION = booleanPreferencesKey("bypass_time_simulation")
    }

    val officeLocationFlow: Flow<OfficeLocation?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val isSet = prefs[KEY_OFFICE_IS_SET] ?: false
            if (isSet) {
                val lat = prefs[KEY_OFFICE_LAT] ?: 0.0
                val lng = prefs[KEY_OFFICE_LNG] ?: 0.0
                OfficeLocation(latitude = lat, longitude = lng, isSet = true)
            } else {
                null
            }
        }

    suspend fun saveOfficeLocation(latitude: Double, longitude: Double) {
        dataStore.edit { prefs ->
            prefs[KEY_OFFICE_LAT] = latitude
            prefs[KEY_OFFICE_LNG] = longitude
            prefs[KEY_OFFICE_IS_SET] = true
        }
    }

    val attendanceHistoryFlow: Flow<List<AttendanceRecord>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val jsonString = prefs[KEY_ATTENDANCE_HISTORY] ?: "[]"
            parseAttendanceRecords(jsonString)
        }

    suspend fun addAttendanceRecord(record: AttendanceRecord) {
        dataStore.edit { prefs ->
            val currentJson = prefs[KEY_ATTENDANCE_HISTORY] ?: "[]"
            val currentList = parseAttendanceRecords(currentJson).toMutableList()
            currentList.add(0, record) // Most recent first
            prefs[KEY_ATTENDANCE_HISTORY] = serializeAttendanceRecords(currentList)
        }
    }

    suspend fun undoTodayAttendance(startOfDayMillis: Long) {
        dataStore.edit { prefs ->
            val currentJson = prefs[KEY_ATTENDANCE_HISTORY] ?: "[]"
            val currentList = parseAttendanceRecords(currentJson)
            // Filter out records created today
            val filtered = currentList.filter { it.timestamp < startOfDayMillis }
            prefs[KEY_ATTENDANCE_HISTORY] = serializeAttendanceRecords(filtered)
        }
    }

    val simulationConfigFlow: Flow<SimulationConfig> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            SimulationConfig(
                bypassTimeValidation = prefs[KEY_BYPASS_TIME_SIMULATION] ?: false
            )
        }

    suspend fun setBypassTimeSimulation(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_BYPASS_TIME_SIMULATION] = enabled
        }
    }

    suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    private fun parseAttendanceRecords(jsonString: String): List<AttendanceRecord> {
        return try {
            val array = JSONArray(jsonString)
            val list = mutableListOf<AttendanceRecord>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AttendanceRecord(
                        id = obj.optString("id", ""),
                        timestamp = obj.optLong("timestamp", 0L),
                        latitude = obj.optDouble("lat", 0.0),
                        longitude = obj.optDouble("lng", 0.0),
                        distanceMeters = obj.optInt("dist", 0)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeAttendanceRecords(records: List<AttendanceRecord>): String {
        val array = JSONArray()
        records.forEach { record ->
            val obj = JSONObject().apply {
                put("id", record.id)
                put("timestamp", record.timestamp)
                put("lat", record.latitude)
                put("lng", record.longitude)
                put("dist", record.distanceMeters)
            }
            array.put(obj)
        }
        return array.toString()
    }
}
