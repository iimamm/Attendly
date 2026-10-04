package com.appsbase.attendly.ui.attendance

import com.appsbase.attendly.domain.model.AttendanceStatus
import com.appsbase.attendly.domain.model.LocationModel
import com.appsbase.attendly.domain.model.OfficeLocation
import com.appsbase.attendly.domain.usecase.MarkAttendanceUseCase
import com.appsbase.attendly.domain.usecase.SaveOfficeLocationUseCase
import com.appsbase.attendly.domain.usecase.ValidateAttendanceEligibilityUseCase
import com.appsbase.attendly.domain.util.TimeValidator
import com.appsbase.attendly.testutil.FakeAttendanceRepository
import com.appsbase.attendly.testutil.FakeLocationTracker
import com.appsbase.attendly.testutil.FakeTimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeRepository: FakeAttendanceRepository
    private lateinit var fakeLocationTracker: FakeLocationTracker
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var timeValidator: TimeValidator
    private lateinit var validateEligibilityUseCase: ValidateAttendanceEligibilityUseCase
    private lateinit var markAttendanceUseCase: MarkAttendanceUseCase
    private lateinit var saveOfficeLocationUseCase: SaveOfficeLocationUseCase

    private lateinit var viewModel: AttendanceViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakeRepository = FakeAttendanceRepository()
        fakeLocationTracker = FakeLocationTracker()
        fakeTimeProvider = FakeTimeProvider()
        fakeTimeProvider.setTime(LocalTime.of(11, 0)) // During work hours

        timeValidator = TimeValidator(fakeTimeProvider)
        validateEligibilityUseCase = ValidateAttendanceEligibilityUseCase(timeValidator)
        markAttendanceUseCase = MarkAttendanceUseCase(fakeRepository, validateEligibilityUseCase)
        saveOfficeLocationUseCase = SaveOfficeLocationUseCase(fakeRepository)

        viewModel = AttendanceViewModel(
            repository = fakeRepository,
            locationTracker = fakeLocationTracker,
            validateEligibilityUseCase = validateEligibilityUseCase,
            markAttendanceUseCase = markAttendanceUseCase,
            saveOfficeLocationUseCase = saveOfficeLocationUseCase,
            timeValidator = timeValidator
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasPermissionsAndTracksLocation() = runTest {
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.hasLocationPermission)
        assertTrue(state.isGpsEnabled)
        assertNotNull(state.currentLocation)
        assertEquals(23.8103, state.currentLocation!!.latitude, 0.001)
    }

    @Test
    fun refreshLocationState_detectsGpsDisabled() = runTest {
        advanceUntilIdle()

        fakeLocationTracker.gpsEnabled = false
        viewModel.onIntent(AttendanceIntent.RefreshLocationState)
        advanceUntilIdle()

        org.junit.Assert.assertFalse(viewModel.state.value.isGpsEnabled)

        fakeLocationTracker.gpsEnabled = true
        viewModel.onIntent(AttendanceIntent.RefreshLocationState)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isGpsEnabled)
    }

    @Test
    fun pauseLocationTracking_stopsAndResumeRestartsTracking() = runTest {
        advanceUntilIdle()
        val callsAfterInit = fakeLocationTracker.getCurrentLocationCalls

        viewModel.onIntent(AttendanceIntent.PauseLocationTracking)
        viewModel.onIntent(AttendanceIntent.RefreshLocationState)
        advanceUntilIdle()

        assertEquals(callsAfterInit + 1, fakeLocationTracker.getCurrentLocationCalls)
    }

    @Test
    fun onMapCameraMoved_updatesTargetOfficeLocation() = runTest {
        advanceUntilIdle()

        val newTarget = LocationModel(23.7500, 90.3900)
        viewModel.onIntent(AttendanceIntent.MapCameraMoved(newTarget))

        assertEquals(newTarget, viewModel.state.value.targetOfficeLocation)
    }

    @Test
    fun onSaveOfficeLocationClicked_persistsOfficeLocation() = runTest {
        advanceUntilIdle()

        val candidate = LocationModel(23.8103, 90.4125)
        viewModel.onIntent(AttendanceIntent.MapCameraMoved(candidate))
        viewModel.onIntent(AttendanceIntent.SaveOfficeLocationClicked)

        advanceUntilIdle()

        assertEquals(
            OfficeLocation(candidate.latitude, candidate.longitude, isSet = true),
            fakeRepository.officeLocationFlow.value
        )
    }

    @Test
    fun onToggleTimeSimulation_togglesSimulationState() = runTest {
        advanceUntilIdle()

        viewModel.onIntent(AttendanceIntent.ToggleTimeSimulation)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.simulationConfig.bypassTimeValidation)

        viewModel.onIntent(AttendanceIntent.ToggleTimeSimulation)
        advanceUntilIdle()

        org.junit.Assert.assertFalse(viewModel.state.value.simulationConfig.bypassTimeValidation)
    }

    @Test
    fun markAttendance_whenInsideGeofenceAndWorkingHours_marksSuccessfully() = runTest {
        advanceUntilIdle()

        // Set office location to same as current location
        val loc = LocationModel(23.8103, 90.4125)
        viewModel.onIntent(AttendanceIntent.MapCameraMoved(loc))
        viewModel.onIntent(AttendanceIntent.SaveOfficeLocationClicked)
        advanceUntilIdle()

        assertEquals(AttendanceStatus.ELIGIBLE, viewModel.state.value.eligibilityStatus)

        // Mark attendance
        viewModel.onIntent(AttendanceIntent.MarkAttendanceClicked)
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.todayAttendance)
        assertEquals(1, viewModel.state.value.attendanceHistory.size)
    }

    @Test
    fun onConfirmResetAll_clearsAllDataAndOffice() = runTest {
        advanceUntilIdle()

        // Set office location
        viewModel.onIntent(AttendanceIntent.MapCameraMoved(LocationModel(23.8103, 90.4125)))
        viewModel.onIntent(AttendanceIntent.SaveOfficeLocationClicked)
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.officeLocation)

        // Reset All
        viewModel.onIntent(AttendanceIntent.ConfirmResetAll)
        advanceUntilIdle()

        assertNull(viewModel.state.value.officeLocation)
        assertEquals(0, viewModel.state.value.attendanceHistory.size)
    }
}
