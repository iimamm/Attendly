package com.appsbase.attendly.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoFenceCalculatorTest {

    @Test
    fun calculateDistanceMeters_sameCoordinates_returnsZero() {
        val lat = 23.8103
        val lng = 90.4125

        val distance = GeoFenceCalculator.calculateDistanceMeters(lat, lng, lat, lng)

        assertEquals(0, distance)
    }

    @Test
    fun isWithinGeofence_whenWithin50Meters_returnsTrue() {
        // Points ~20 meters apart
        val officeLat = 23.810300
        val officeLng = 90.412500
        val userLat = 23.810450
        val userLng = 90.412500

        val distance = GeoFenceCalculator.calculateDistanceMeters(officeLat, officeLng, userLat, userLng)

        assertTrue("Distance ($distance m) should be within 50m", distance <= 50)
        assertTrue(GeoFenceCalculator.isWithinGeofence(distance))
    }

    @Test
    fun isWithinGeofence_exactBoundary_returnsTrue() {
        assertTrue(GeoFenceCalculator.isWithinGeofence(50))
    }

    @Test
    fun isWithinGeofence_whenBeyond50Meters_returnsFalse() {
        // Point ~120m away
        val officeLat = 23.810300
        val officeLng = 90.412500
        val userLat = 23.811400
        val userLng = 90.412500

        val distance = GeoFenceCalculator.calculateDistanceMeters(officeLat, officeLng, userLat, userLng)

        assertTrue("Distance ($distance m) should be greater than 50m", distance > 50)
        assertFalse(GeoFenceCalculator.isWithinGeofence(distance))
    }

    @Test
    fun isWithinGeofence_customRadiusThreshold() {
        assertTrue(GeoFenceCalculator.isWithinGeofence(75, radiusMeters = 100.0))
        assertFalse(GeoFenceCalculator.isWithinGeofence(105, radiusMeters = 100.0))
    }
}
