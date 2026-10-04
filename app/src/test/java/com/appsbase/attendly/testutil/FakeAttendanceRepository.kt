package com.appsbase.attendly.testutil

import com.appsbase.attendly.domain.model.AttendanceRecord
import com.appsbase.attendly.domain.model.LocationModel
import com.appsbase.attendly.domain.model.OfficeLocation
import com.appsbase.attendly.domain.model.SimulationConfig
import com.appsbase.attendly.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

class FakeAttendanceRepository : AttendanceRepository {

    val officeLocationFlow = MutableStateFlow<OfficeLocation?>(null)
    val attendanceHistoryFlow = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val todayAttendanceFlow = MutableStateFlow<AttendanceRecord?>(null)
    val simulationConfigFlow = MutableStateFlow(SimulationConfig())

    override val officeLocation: Flow<OfficeLocation?> = officeLocationFlow
    override val attendanceHistory: Flow<List<AttendanceRecord>> = attendanceHistoryFlow
    override val todayAttendance: Flow<AttendanceRecord?> = todayAttendanceFlow
    override val simulationConfig: Flow<SimulationConfig> = simulationConfigFlow

    override suspend fun saveOfficeLocation(location: LocationModel) {
        officeLocationFlow.value = OfficeLocation(location.latitude, location.longitude, isSet = true)
    }

    override suspend fun markAttendance(location: LocationModel, distanceMeters: Int): AttendanceRecord {
        val record = AttendanceRecord(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            latitude = location.latitude,
            longitude = location.longitude,
            distanceMeters = distanceMeters
        )
        val currentList = attendanceHistoryFlow.value.toMutableList()
        currentList.add(0, record)
        attendanceHistoryFlow.value = currentList
        todayAttendanceFlow.value = record
        return record
    }

    override suspend fun resetAllData() {
        officeLocationFlow.value = null
        attendanceHistoryFlow.value = emptyList()
        todayAttendanceFlow.value = null
        simulationConfigFlow.value = SimulationConfig()
    }

    override suspend fun setTimeBypassSimulation(enabled: Boolean) {
        simulationConfigFlow.value = SimulationConfig(bypassTimeValidation = enabled)
    }
}
