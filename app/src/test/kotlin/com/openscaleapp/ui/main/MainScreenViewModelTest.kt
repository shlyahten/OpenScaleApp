package com.openscaleapp.ui.main

import com.openscaleapp.ble.BleScannerState
import com.openscaleapp.ble.ScaleScanner
import com.openscaleapp.data.HistoryRepository
import com.openscaleapp.data.ProfileRepository
import com.openscaleapp.model.DeviceType
import com.openscaleapp.model.Gender
import com.openscaleapp.model.ScaleMeasurement
import com.openscaleapp.model.UserProfile
import com.openscaleapp.model.WeighInRecord
import com.openscaleapp.model.WeightUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
    private lateinit var fakeProfileRepo: FakeProfileRepository
    private lateinit var fakeHistoryRepo: FakeHistoryRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeScanner = FakeScaleScanner()
        fakeProfileRepo = FakeProfileRepository(
            UserProfile(gender = Gender.MALE, heightCm = 180f, ageYears = 30)
        )
        fakeHistoryRepo = FakeHistoryRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_reflectsScannerAndProfile() = runTest(testDispatcher) {
        val viewModel = MainScreenViewModel(fakeScanner, fakeProfileRepo, fakeHistoryRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasPermissions)
        assertTrue(state.isBluetoothEnabled)
        assertTrue(state.isLocationEnabled)
        assertFalse(state.isScanning)
        assertNull(state.scaleMeasurement)
        assertEquals(180f, state.userProfile.heightCm, 0.01f)
        assertTrue(state.history.isEmpty())
    }

    @Test
    fun onMeasurementReceived_whenScanning_updatesStateAndCalculatesBodyComposition() = runTest(testDispatcher) {
        val viewModel = MainScreenViewModel(fakeScanner, fakeProfileRepo, fakeHistoryRepo)
        runCurrent()

        viewModel.startWeighIn()
        runCurrent()
        assertTrue(viewModel.uiState.value.isScanning)

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
        runCurrent()

        val state = viewModel.uiState.value
        // When stabilized with impedance > 0, scanner stops automatically!
        assertFalse(state.isScanning)
        assertNotNull(state.scaleMeasurement)
        assertEquals(77.10f, state.scaleMeasurement?.weightKg ?: 0f, 0.01f)
        assertTrue(state.scaleMeasurement?.isStabilized == true)

        // Verify body composition calculated
        val composition = state.bodyComposition
        assertNotNull(composition)
        assertEquals(23.8f, composition?.bmi ?: 0f, 0.1f)
        assertNotNull(composition?.bodyFatRate)

        // Verify saved to history
        assertEquals(1, state.history.size)
        assertEquals(77.10f, state.history[0].weightKg, 0.01f)
    }

    @Test
    fun injectDemoMeasurement_supportsLbUnitProperly() = runTest(testDispatcher) {
        val viewModel = MainScreenViewModel(fakeScanner, fakeProfileRepo, fakeHistoryRepo)
        runCurrent()

        viewModel.injectDemoMeasurement(
            isStabilized = true,
            weightKg = 77.10f,
            impedanceOhm = 600.0f,
            unit = WeightUnit.LB
        )
        runCurrent()

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
        val viewModel = MainScreenViewModel(fakeScanner, fakeProfileRepo, fakeHistoryRepo)
        runCurrent()

        viewModel.startWeighIn()
        runCurrent()

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
        runCurrent()

        val initialBmi = viewModel.uiState.value.bodyComposition!!.bmi

        // Update height from 180 to 200 cm
        viewModel.updateUserProfile(UserProfile(gender = Gender.MALE, heightCm = 200f, ageYears = 30))
        runCurrent()

        val updatedBmi = viewModel.uiState.value.bodyComposition!!.bmi
        assertTrue(updatedBmi < initialBmi)
        assertEquals(20.0f, updatedBmi, 0.1f)
    }

    @Test
    fun startWeighIn_and_cancelWeighIn_controlsScanner() = runTest(testDispatcher) {
        val viewModel = MainScreenViewModel(fakeScanner, fakeProfileRepo, fakeHistoryRepo)
        runCurrent()

        viewModel.startWeighIn()
        runCurrent()
        assertTrue(fakeScanner.isScanningActive)
        assertTrue(viewModel.uiState.value.isScanning)

        viewModel.cancelWeighIn()
        runCurrent()
        assertFalse(fakeScanner.isScanningActive)
        assertFalse(viewModel.uiState.value.isScanning)
    }

    @Test
    fun startWeighIn_timeoutExpires_stopsScanAndShowsError() = runTest(testDispatcher) {
        val viewModel = MainScreenViewModel(fakeScanner, fakeProfileRepo, fakeHistoryRepo)
        runCurrent()

        viewModel.startWeighIn(timeoutSeconds = 5)
        runCurrent()
        assertTrue(viewModel.uiState.value.isScanning)

        // Advance past 5-second timeout
        advanceTimeBy(6000)
        runCurrent()

        assertFalse(viewModel.uiState.value.isScanning)
        assertFalse(fakeScanner.isScanningActive)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun scannerState_handlesBluetoothAndLocationDisabled() = runTest(testDispatcher) {
        val viewModel = MainScreenViewModel(fakeScanner, fakeProfileRepo, fakeHistoryRepo)
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
    fun history_selectAndReturnOperations() = runTest(testDispatcher) {
        val viewModel = MainScreenViewModel(fakeScanner, fakeProfileRepo, fakeHistoryRepo)
        runCurrent()

        viewModel.injectDemoMeasurement(weightKg = 76.65f, impedanceOhm = 600f)
        runCurrent()
        assertEquals(1, viewModel.uiState.value.history.size)
        val record = viewModel.uiState.value.history[0]

        // Select record
        viewModel.selectHistoryRecord(record)
        runCurrent()
        assertEquals(record, viewModel.uiState.value.selectedHistoryRecord)
        assertEquals(76.65f, viewModel.uiState.value.scaleMeasurement?.weightKg ?: 0f, 0.01f)

        // Clear selection
        viewModel.clearHistorySelection()
        runCurrent()
        assertNull(viewModel.uiState.value.selectedHistoryRecord)

        // Delete record
        viewModel.deleteHistoryRecord(record.id)
        runCurrent()
        assertTrue(viewModel.uiState.value.history.isEmpty())
    }

    @Test
    fun clearMeasurement_resetsMeasurementData() = runTest(testDispatcher) {
        val viewModel = MainScreenViewModel(fakeScanner, fakeProfileRepo, fakeHistoryRepo)
        runCurrent()

        viewModel.injectDemoMeasurement()
        runCurrent()
        assertNotNull(viewModel.uiState.value.scaleMeasurement)

        viewModel.clearMeasurement()
        runCurrent()
        assertNull(viewModel.uiState.value.scaleMeasurement)
        assertNull(viewModel.uiState.value.bodyComposition)
    }

    @Test
    fun setAppTheme_updatesStateAndPersists() = runTest {
        val fakeSettings = FakeSettingsRepository()
        val vm = MainScreenViewModel(fakeScanner, fakeProfileRepo, fakeHistoryRepo, fakeSettings)
        runCurrent()
        assertEquals(com.openscaleapp.ui.theme.AppTheme.SYSTEM, vm.uiState.value.appTheme)

        vm.setAppTheme(com.openscaleapp.ui.theme.AppTheme.DARK)
        runCurrent()
        assertEquals(com.openscaleapp.ui.theme.AppTheme.DARK, vm.uiState.value.appTheme)
        assertEquals(com.openscaleapp.ui.theme.AppTheme.DARK, fakeSettings.appTheme.value)

        vm.setAppTheme(com.openscaleapp.ui.theme.AppTheme.LIGHT)
        runCurrent()
        assertEquals(com.openscaleapp.ui.theme.AppTheme.LIGHT, vm.uiState.value.appTheme)
        assertEquals(com.openscaleapp.ui.theme.AppTheme.LIGHT, fakeSettings.appTheme.value)
    }

    @Test
    fun setAppLanguage_updatesStateAndPersists() = runTest {
        val fakeSettings = FakeSettingsRepository()
        val vm = MainScreenViewModel(fakeScanner, fakeProfileRepo, fakeHistoryRepo, fakeSettings)
        runCurrent()
        assertEquals(com.openscaleapp.ui.theme.AppLanguage.SYSTEM, vm.uiState.value.appLanguage)

        vm.setAppLanguage(com.openscaleapp.ui.theme.AppLanguage.EN)
        runCurrent()
        assertEquals(com.openscaleapp.ui.theme.AppLanguage.EN, vm.uiState.value.appLanguage)
        assertEquals(com.openscaleapp.ui.theme.AppLanguage.EN, fakeSettings.appLanguage.value)

        vm.setAppLanguage(com.openscaleapp.ui.theme.AppLanguage.RU)
        runCurrent()
        assertEquals(com.openscaleapp.ui.theme.AppLanguage.RU, vm.uiState.value.appLanguage)
        assertEquals(com.openscaleapp.ui.theme.AppLanguage.RU, fakeSettings.appLanguage.value)
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

private class FakeHistoryRepository : HistoryRepository {
    private val _history = MutableStateFlow<List<WeighInRecord>>(emptyList())
    override val history: StateFlow<List<WeighInRecord>> = _history.asStateFlow()

    override fun saveRecord(record: WeighInRecord) {
        val current = _history.value.toMutableList()
        current.removeAll { it.id == record.id }
        current.add(0, record)
        _history.value = current
    }

    override fun deleteRecord(id: String) {
        val current = _history.value.toMutableList()
        current.removeAll { it.id == id }
        _history.value = current
    }

    override fun clearHistory() {
        _history.value = emptyList()
    }
}

private class FakeSettingsRepository : com.openscaleapp.data.SettingsRepository {
    private val _theme = MutableStateFlow(com.openscaleapp.ui.theme.AppTheme.SYSTEM)
    override val appTheme: StateFlow<com.openscaleapp.ui.theme.AppTheme> = _theme.asStateFlow()

    private val _lang = MutableStateFlow(com.openscaleapp.ui.theme.AppLanguage.SYSTEM)
    override val appLanguage: StateFlow<com.openscaleapp.ui.theme.AppLanguage> = _lang.asStateFlow()

    override fun setAppTheme(theme: com.openscaleapp.ui.theme.AppTheme) {
        _theme.value = theme
    }

    override fun setAppLanguage(language: com.openscaleapp.ui.theme.AppLanguage) {
        _lang.value = language
    }
}
