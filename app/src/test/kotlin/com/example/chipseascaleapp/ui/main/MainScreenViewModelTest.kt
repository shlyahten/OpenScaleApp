package com.example.chipseascaleapp.ui.main

import com.example.chipseascaleapp.ble.BleScannerState
import com.example.chipseascaleapp.ble.ScaleScanner
import com.example.chipseascaleapp.data.ProfileRepository
import com.example.chipseascaleapp.model.DeviceType
import com.example.chipseascaleapp.model.Gender
import com.example.chipseascaleapp.model.ScaleMeasurement
import com.example.chipseascaleapp.model.UserProfile
import com.example.chipseascaleapp.model.WeightUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeScanner: FakeScaleScanner
    private lateinit var fakeRepo: FakeProfileRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeScanner = FakeScaleScanner()
        fakeRepo = FakeProfileRepository(
            UserProfile(gender = Gender.MALE, heightCm = 180f, ageYears = 30)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_reflectsScannerAndProfile() = runTest(testDispatcher) {
        val viewModel = MainScreenViewModel(fakeScanner, fakeRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasPermissions)
        assertTrue(state.isBluetoothEnabled)
        assertTrue(state.isLocationEnabled)
        assertFalse(state.isScanning)
        assertNull(state.scaleMeasurement)
        assertEquals(180f, state.userProfile.heightCm, 0.01f)
    }

    @Test
    fun onMeasurementReceived_updatesStateAndCalculatesBodyComposition() = runTest(testDispatcher) {
        val viewModel = MainScreenViewModel(fakeScanner, fakeRepo)
        advanceUntilIdle()

        val sample = ScaleMeasurement(
            weightKg = 77.10f,
            displayWeight = 77.10f,
            impedanceOhm = 600.0f,
            isStabilized = true,
            unit = WeightUnit.KG,
            deviceType = DeviceType.BODY_FAT_SCALE,
            sequenceIndex = 1,
            productId = 0x0A11,
            macAddress = "80:F4:16:01:47:50"
        )

        fakeScanner.injectMeasurementForTesting(sample)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val measurement = state.scaleMeasurement
        assertNotNull(measurement)
        assertEquals(77.10f, measurement?.weightKg ?: 0f, 0.01f)
        assertTrue(measurement?.isStabilized == true)

        // Verify body composition calculated
        val composition = state.bodyComposition
        assertNotNull(composition)
        assertEquals(23.8f, composition?.bmi ?: 0f, 0.1f)
        assertNotNull(composition?.bodyFatRate)
    }

    @Test
    fun injectDemoMeasurement_supportsLbUnitProperly() = runTest(testDispatcher) {
        val viewModel = MainScreenViewModel(fakeScanner, fakeRepo)
        advanceUntilIdle()

        // Inject 77.10 kg in LB
        viewModel.injectDemoMeasurement(
            isStabilized = true,
            weightKg = 77.10f,
            impedanceOhm = 600.0f,
            unit = WeightUnit.LB
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val m = state.scaleMeasurement
        assertNotNull(m)
        assertEquals(WeightUnit.LB, m!!.unit)
        assertEquals(77.10f, m.weightKg, 0.01f)
        assertEquals(169.98f, m.displayWeight, 0.1f)
        // BMI is still based on 77.10 kg (not 169.98!)
        assertEquals(23.8f, state.bodyComposition!!.bmi, 0.1f)
    }

    @Test
    fun updateUserProfile_recalculatesBodyComposition() = runTest(testDispatcher) {
        val viewModel = MainScreenViewModel(fakeScanner, fakeRepo)
        advanceUntilIdle()

        val sample = ScaleMeasurement(
            weightKg = 80.0f,
            displayWeight = 80.0f,
            impedanceOhm = 550.0f,
            isStabilized = true,
            unit = WeightUnit.KG,
            deviceType = DeviceType.BODY_FAT_SCALE,
            sequenceIndex = 1,
            productId = 0x0A11,
            macAddress = "80:F4:16:01:47:50"
        )
        fakeScanner.injectMeasurementForTesting(sample)
        advanceUntilIdle()

        val initialBmi = viewModel.uiState.value.bodyComposition!!.bmi

        // Update height from 180 to 200 cm
        viewModel.updateUserProfile(UserProfile(gender = Gender.MALE, heightCm = 200f, ageYears = 30))
        advanceUntilIdle()

        val updatedBmi = viewModel.uiState.value.bodyComposition!!.bmi
        assertTrue(updatedBmi < initialBmi)
        assertEquals(20.0f, updatedBmi, 0.1f)
    }

    @Test
    fun scannerControls_startStopScan() = runTest(testDispatcher) {
        val viewModel = MainScreenViewModel(fakeScanner, fakeRepo)
        advanceUntilIdle()

        viewModel.startScan()
        advanceUntilIdle()
        assertTrue(fakeScanner.isScanningActive)
        assertTrue(viewModel.uiState.value.isScanning)

        viewModel.stopScan()
        advanceUntilIdle()
        assertFalse(fakeScanner.isScanningActive)
        assertFalse(viewModel.uiState.value.isScanning)
    }

    @Test
    fun scannerState_handlesBluetoothAndLocationDisabled() = runTest(testDispatcher) {
        val viewModel = MainScreenViewModel(fakeScanner, fakeRepo)
        advanceUntilIdle()

        fakeScanner.setScannerState(BleScannerState.BluetoothDisabled)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isBluetoothEnabled)
        assertFalse(viewModel.uiState.value.isScanning)

        fakeScanner.setScannerState(BleScannerState.LocationDisabled)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLocationEnabled)
        assertFalse(viewModel.uiState.value.isScanning)
    }

    @Test
    fun clearMeasurement_resetsMeasurementData() = runTest(testDispatcher) {
        val viewModel = MainScreenViewModel(fakeScanner, fakeRepo)
        advanceUntilIdle()

        viewModel.injectDemoMeasurement()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.scaleMeasurement)

        viewModel.clearMeasurement()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.scaleMeasurement)
        assertNull(viewModel.uiState.value.bodyComposition)
    }
}

private class FakeScaleScanner : ScaleScanner {
    private val _scannerState = MutableStateFlow<BleScannerState>(BleScannerState.Idle)
    override val scannerState: StateFlow<BleScannerState> = _scannerState.asStateFlow()

    private val _measurements = MutableSharedFlow<ScaleMeasurement>(extraBufferCapacity = 64)
    override val measurements: SharedFlow<ScaleMeasurement> = _measurements.asSharedFlow()

    var isScanningActive = false
    var fakeBluetoothEnabled = true
    var fakeLocationEnabled = true

    override fun hasRequiredPermissions(): Boolean = true
    override fun isBluetoothEnabled(): Boolean = fakeBluetoothEnabled
    override fun isLocationEnabled(): Boolean = fakeLocationEnabled

    fun setScannerState(state: BleScannerState) {
        _scannerState.value = state
    }

    override fun startScan() {
        isScanningActive = true
        _scannerState.value = BleScannerState.Scanning
    }

    override fun stopScan() {
        isScanningActive = false
        _scannerState.value = BleScannerState.Idle
    }

    override fun injectMeasurementForTesting(measurement: ScaleMeasurement) {
        _measurements.tryEmit(measurement)
    }
}

private class FakeProfileRepository(initialProfile: UserProfile) : ProfileRepository {
    private val _userProfile = MutableStateFlow(initialProfile)
    override val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    override fun saveProfile(profile: UserProfile) {
        _userProfile.value = profile
    }
}
