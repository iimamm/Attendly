package com.appsbase.attendly.domain.usecase

import com.appsbase.attendly.domain.model.LocationModel
import com.appsbase.attendly.domain.repository.AttendanceRepository
import javax.inject.Inject

class SaveOfficeLocationUseCase @Inject constructor(
    private val repository: AttendanceRepository
) {
    suspend operator fun invoke(location: LocationModel): Result<Unit> {
        return try {
            repository.saveOfficeLocation(location)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
