package com.appsbase.attendly.data.repository

import com.appsbase.attendly.core.time.TimeProvider
import com.appsbase.attendly.data.local.AttendanceDataStore
import com.appsbase.attendly.domain.model.AttendanceRecord
import com.appsbase.attendly.domain.model.LocationModel
import com.appsbase.attendly.domain.model.OfficeLocation
import com.appsbase.attendly.domain.model.SimulationConfig
import com.appsbase.attendly.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepositoryImpl @Inject constructor(
    private val dataStore: AttendanceDataStore,
    private val timeProvider: TimeProvider
) : AttendanceRepository {

    override val officeLocation: Flow<OfficeLocation?> = dataStore.officeLocationFlow

    override val attendanceHistory: Flow<List<AttendanceRecord>> = dataStore.attendanceHistoryFlow

    override val todayAttendance: Flow<AttendanceRecord?> = dataStore.attendanceHistoryFlow.map { records ->
        val startOfDay = timeProvider.now().toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val endOfDay = startOfDay + 86_400_000L

        records.firstOrNull { it.timestamp in startOfDay until endOfDay }
    }

    override val simulationConfig: Flow<SimulationConfig> = dataStore.simulationConfigFlow

    override suspend fun saveOfficeLocation(location: LocationModel) {
        dataStore.saveOfficeLocation(location.latitude, location.longitude)
    }

    override suspend fun markAttendance(
        location: LocationModel,
        distanceMeters: Int
    ): AttendanceRecord {
        val record = AttendanceRecord(
            id = UUID.randomUUID().toString(),
            timestamp = timeProvider.currentTimeMillis(),
            latitude = location.latitude,
            longitude = location.longitude,
            distanceMeters = distanceMeters
        )
        dataStore.addAttendanceRecord(record)
        return record
    }

    override suspend fun resetAllData() {
        dataStore.clearAll()
    }

    override suspend fun setTimeBypassSimulation(enabled: Boolean) {
        dataStore.setBypassTimeSimulation(enabled)
    }
}
