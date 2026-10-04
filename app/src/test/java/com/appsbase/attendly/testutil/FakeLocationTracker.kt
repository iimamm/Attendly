package com.appsbase.attendly.testutil

import com.appsbase.attendly.data.location.LocationTracker
import com.appsbase.attendly.domain.model.LocationModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeLocationTracker(
    initialLocation: LocationModel = LocationModel(23.8103, 90.4125),
    var hasPermission: Boolean = true,
    var gpsEnabled: Boolean = true
) : LocationTracker {

    val locationFlow = MutableStateFlow(initialLocation)

    var getCurrentLocationCalls = 0
        private set

    override val locationUpdates: Flow<LocationModel> = locationFlow

    override suspend fun getCurrentLocation(): LocationModel? {
        getCurrentLocationCalls++
        return locationFlow.value
    }

    override fun hasLocationPermission(): Boolean = hasPermission

    override fun isGpsEnabled(): Boolean = gpsEnabled

    fun emitLocation(location: LocationModel) {
        locationFlow.value = location
    }
}
