package com.appsbase.attendly.domain.repository

import com.appsbase.attendly.domain.model.AttendanceRecord
import com.appsbase.attendly.domain.model.LocationModel
import com.appsbase.attendly.domain.model.OfficeLocation
import com.appsbase.attendly.domain.model.SimulationConfig
import kotlinx.coroutines.flow.Flow

interface AttendanceRepository {
    val officeLocation: Flow<OfficeLocation?>
    val attendanceHistory: Flow<List<AttendanceRecord>>
    val todayAttendance: Flow<AttendanceRecord?>
    val simulationConfig: Flow<SimulationConfig>

    suspend fun saveOfficeLocation(location: LocationModel)
    suspend fun markAttendance(location: LocationModel, distanceMeters: Int): AttendanceRecord
    suspend fun resetAllData()
    suspend fun setTimeBypassSimulation(enabled: Boolean)
}
