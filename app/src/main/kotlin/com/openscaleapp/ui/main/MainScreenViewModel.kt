package com.openscaleapp.ui.main

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.openscaleapp.algorithm.BodyCompositionCalculator
import com.openscaleapp.ble.BleScaleScanner
import com.openscaleapp.ble.BleScannerState
import com.openscaleapp.ble.ScaleScanner
import com.openscaleapp.data.AppSettingsRepository
import com.openscaleapp.data.HistoryRepository
import com.openscaleapp.data.ProfileRepository
import com.openscaleapp.data.SettingsRepository
import com.openscaleapp.data.UserProfileRepository
import com.openscaleapp.data.WeighInHistoryRepository
import com.openscaleapp.model.BodyComposition
import com.openscaleapp.model.DeviceType
import com.openscaleapp.model.ScaleMeasurement
import com.openscaleapp.model.UserProfile
import com.openscaleapp.model.WeighInRecord
import com.openscaleapp.model.WeightUnit
import com.openscaleapp.ui.theme.AppLanguage
import com.openscaleapp.ui.theme.AppTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val isScanning: Boolean = false,
    val scanRemainingSeconds: Int = 0,
    val isBluetoothEnabled: Boolean = true,
    val isLocationEnabled: Boolean = true,
    val hasPermissions: Boolean = false,
    val scaleMeasurement: ScaleMeasurement? = null,
    val bodyComposition: BodyComposition? = null,
    val userProfile: UserProfile = UserProfile(),
    val errorMessage: String? = null,
    val isDemoMode: Boolean = false,
    val history: List<WeighInRecord> = emptyList(),
    val selectedHistoryRecord: WeighInRecord? = null,
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM
)

class MainScreenViewModel(
    private val scanner: ScaleScanner,
    private val userProfileRepository: ProfileRepository,
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MainUiState(
            hasPermissions = scanner.hasRequiredPermissions(),
            isBluetoothEnabled = scanner.isBluetoothEnabled(),
            isLocationEnabled = scanner.isLocationEnabled(),
            userProfile = userProfileRepository.userProfile.value,
            history = historyRepository.history.value,
            appTheme = settingsRepository?.appTheme?.value ?: AppTheme.SYSTEM,
            appLanguage = settingsRepository?.appLanguage?.value ?: AppLanguage.SYSTEM
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var scanTimerJob: Job? = null
    private var stabilizationJob: Job? = null

    init {
        // 1. Observe user profile changes
        viewModelScope.launch {
            userProfileRepository.userProfile.collect { profile ->
                _uiState.update { current ->
                    // If viewing history record, keep historical composition
                    if (current.selectedHistoryRecord != null) {
                        current.copy(userProfile = profile)
                    } else {
                        val recomputed = current.scaleMeasurement?.let {
                            BodyCompositionCalculator.calculate(it, profile)
                        }
                        current.copy(userProfile = profile, bodyComposition = recomputed)
                    }
                }
            }
        }

        // 2. Observe history repository
        viewModelScope.launch {
            historyRepository.history.collect { list ->
                _uiState.update { current ->
                    // If currently showing nothing, pre-populate with latest history record for instant display
                    if (current.scaleMeasurement == null && current.selectedHistoryRecord == null && list.isNotEmpty()) {
                        val latest = list.first()
                        current.copy(
                            history = list,
                            scaleMeasurement = latest.toScaleMeasurement(),
                            bodyComposition = latest.bodyComposition
                        )
                    } else {
                        current.copy(history = list)
                    }
                }
            }
        }

        // 3. Observe incoming scale measurements
        viewModelScope.launch {
            scanner.measurements.collect { measurement ->
                handleIncomingMeasurement(measurement)
            }
        }

        // 4. Observe scanner state
        viewModelScope.launch {
            scanner.scannerState.collect { state ->
                when (state) {
                    is BleScannerState.Idle -> {
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                isBluetoothEnabled = scanner.isBluetoothEnabled(),
                                isLocationEnabled = scanner.isLocationEnabled()
                            )
                        }
                    }
                    is BleScannerState.Scanning -> {
                        _uiState.update {
                            it.copy(
                                isScanning = true,
                                errorMessage = null,
                                isBluetoothEnabled = true,
                                isLocationEnabled = true
                            )
                        }
                    }
                    is BleScannerState.BluetoothDisabled -> {
                        cancelScanJobs()
                        _uiState.update { it.copy(isScanning = false, scanRemainingSeconds = 0, isBluetoothEnabled = false) }
                    }
                    is BleScannerState.LocationDisabled -> {
                        cancelScanJobs()
                        _uiState.update { it.copy(isScanning = false, scanRemainingSeconds = 0, isLocationEnabled = false) }
                    }
                    is BleScannerState.MissingPermissions -> {
                        cancelScanJobs()
                        _uiState.update { it.copy(isScanning = false, scanRemainingSeconds = 0, hasPermissions = false) }
                    }
                    is BleScannerState.Error -> {
                        cancelScanJobs()
                        _uiState.update { it.copy(isScanning = false, scanRemainingSeconds = 0, errorMessage = state.message) }
                    }
                }
            }
        }

        // 5. Observe theme and language settings
        settingsRepository?.let { settings ->
            viewModelScope.launch {
                settings.appTheme.collect { theme ->
                    _uiState.update { it.copy(appTheme = theme) }
                }
            }
            viewModelScope.launch {
                settings.appLanguage.collect { lang ->
                    _uiState.update { it.copy(appLanguage = lang) }
                }
            }
        }
    }

    private fun handleIncomingMeasurement(measurement: ScaleMeasurement) {
        // If we are not currently searching for a weigh-in, ignore extraneous packets
        if (!_uiState.value.isScanning) {
            return
        }

        val profile = _uiState.value.userProfile
        val composition = BodyCompositionCalculator.calculate(measurement, profile)

        _uiState.update { current ->
            current.copy(
                scaleMeasurement = measurement,
                bodyComposition = composition,
                selectedHistoryRecord = null, // Switched to live measurement
                errorMessage = null
            )
        }

        if (measurement.isStabilized) {
            Log.i("MainViewModel", "Stabilized measurement detected: ${measurement.displayWeight} ${measurement.unit}, Z=${measurement.impedanceOhm}")

            if (measurement.deviceType == DeviceType.BODY_FAT_SCALE) {
                if (measurement.impedanceOhm > 0f) {
                    // Full bioimpedance measurement locked! Finalize immediately
                    finalizeAndSaveMeasurement(measurement, composition)
                } else {
                    // Weight locked, waiting up to 2.5s for bioimpedance contact
                    if (stabilizationJob == null || stabilizationJob?.isActive != true) {
                        stabilizationJob = viewModelScope.launch {
                            delay(2500)
                            // If still scanning and no impedance packet arrived, save weight-only
                            if (_uiState.value.isScanning) {
                                val currentM = _uiState.value.scaleMeasurement ?: measurement
                                val currentC = _uiState.value.bodyComposition ?: composition
                                finalizeAndSaveMeasurement(currentM, currentC)
                            }
                        }
                    }
                }
            } else {
                // Simple scale: finalize immediately
                finalizeAndSaveMeasurement(measurement, composition)
            }
        }
    }

    private fun finalizeAndSaveMeasurement(measurement: ScaleMeasurement, composition: BodyComposition) {
        cancelScanJobs()
        scanner.stopScan()

        val record = WeighInRecord(
            timestampMs = System.currentTimeMillis(),
            weightKg = measurement.weightKg,
            displayWeight = measurement.displayWeight,
            unit = measurement.unit,
            impedanceOhm = measurement.impedanceOhm,
            deviceType = measurement.deviceType,
            macAddress = measurement.macAddress,
            bodyComposition = composition
        )
        historyRepository.saveRecord(record)

        _uiState.update {
            it.copy(
                isScanning = false,
                scanRemainingSeconds = 0,
                scaleMeasurement = measurement,
                bodyComposition = composition
            )
        }
    }

    private fun cancelScanJobs() {
        scanTimerJob?.cancel()
        scanTimerJob = null
        stabilizationJob?.cancel()
        stabilizationJob = null
    }

    fun refreshDeviceState() {
        _uiState.update {
            it.copy(
                hasPermissions = scanner.hasRequiredPermissions(),
                isBluetoothEnabled = scanner.isBluetoothEnabled(),
                isLocationEnabled = scanner.isLocationEnabled()
            )
        }
    }

    /**
     * Start a dedicated weigh-in session: scans for up to 30 seconds for the scale.
     */
    fun startWeighIn(timeoutSeconds: Int = 30) {
        val hasPerms = scanner.hasRequiredPermissions()
        val btEnabled = scanner.isBluetoothEnabled()
        val locEnabled = scanner.isLocationEnabled()

        if (!hasPerms || !btEnabled || !locEnabled) {
            _uiState.update {
                it.copy(
                    hasPermissions = hasPerms,
                    isBluetoothEnabled = btEnabled,
                    isLocationEnabled = locEnabled
                )
            }
            return
        }

        cancelScanJobs()
        _uiState.update {
            it.copy(
                isScanning = true,
                scanRemainingSeconds = timeoutSeconds,
                hasPermissions = hasPerms,
                isBluetoothEnabled = btEnabled,
                isLocationEnabled = locEnabled,
                errorMessage = null,
                selectedHistoryRecord = null
            )
        }
        scanner.startScan()

        scanTimerJob = viewModelScope.launch {
            for (sec in timeoutSeconds downTo 1) {
                _uiState.update { it.copy(scanRemainingSeconds = sec) }
                delay(1000)
            }
            if (_uiState.value.isScanning) {
                scanner.stopScan()
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanRemainingSeconds = 0,
                        errorMessage = "Весы не найдены за $timeoutSeconds сек. Встаньте на весы и нажмите «Взвешивание» снова."
                    )
                }
            }
        }
    }

    /**
     * Cancel an ongoing weigh-in session.
     */
    fun cancelWeighIn() {
        cancelScanJobs()
        scanner.stopScan()
        _uiState.update { it.copy(isScanning = false, scanRemainingSeconds = 0) }
    }

    fun selectHistoryRecord(record: WeighInRecord) {
        cancelWeighIn()
        _uiState.update {
            it.copy(
                selectedHistoryRecord = record,
                scaleMeasurement = record.toScaleMeasurement(),
                bodyComposition = record.bodyComposition
            )
        }
    }

    fun clearHistorySelection() {
        _uiState.update { current ->
            // Switch back to latest record if available
            val latest = current.history.firstOrNull()
            current.copy(
                selectedHistoryRecord = null,
                scaleMeasurement = latest?.toScaleMeasurement(),
                bodyComposition = latest?.bodyComposition
            )
        }
    }

    fun deleteHistoryRecord(id: String) {
        historyRepository.deleteRecord(id)
        if (_uiState.value.selectedHistoryRecord?.id == id) {
            clearHistorySelection()
        }
    }

    fun clearAllHistory() {
        historyRepository.clearHistory()
        clearHistorySelection()
    }

    fun onPermissionsResult(allGranted: Boolean) {
        _uiState.update { it.copy(hasPermissions = allGranted) }
        if (allGranted) {
            startWeighIn()
        }
    }

    fun updateUserProfile(profile: UserProfile) {
        userProfileRepository.saveProfile(profile)
    }

    fun clearMeasurement() {
        _uiState.update {
            it.copy(
                scaleMeasurement = null,
                bodyComposition = null,
                selectedHistoryRecord = null,
                isDemoMode = false
            )
        }
    }

    fun injectDemoMeasurement(
        isStabilized: Boolean = true,
        weightKg: Float = 76.65f,
        impedanceOhm: Float = 600.0f,
        unit: WeightUnit = WeightUnit.KG
    ) {
        val displayWeight = if (unit == WeightUnit.LB) weightKg / 0.45359237f else weightKg
        val demoMeasurement = ScaleMeasurement(
            weightKg = weightKg,
            displayWeight = displayWeight,
            impedanceOhm = impedanceOhm,
            isStabilized = isStabilized,
            unit = unit,
            deviceType = DeviceType.BODY_FAT_SCALE,
            sequenceIndex = 42,
            productId = 0x0A11,
            macAddress = "80:F4:16:01:47:50"
        )
        _uiState.update { it.copy(isDemoMode = true, isScanning = true) }
        scanner.injectMeasurementForTesting(demoMeasurement)
    }

    fun setAppTheme(theme: AppTheme) {
        settingsRepository?.setAppTheme(theme)
        _uiState.update { it.copy(appTheme = theme) }
    }

    fun setAppLanguage(language: AppLanguage) {
        settingsRepository?.setAppLanguage(language)
        _uiState.update { it.copy(appLanguage = language) }
    }

    override fun onCleared() {
        super.onCleared()
        cancelScanJobs()
        scanner.stopScan()
        if (scanner is BleScaleScanner) {
            scanner.unregisterStateReceiver()
        }
    }

    companion object {
        @Volatile
        private var singletonScanner: BleScaleScanner? = null

        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                val scanner = singletonScanner ?: synchronized(this) {
                    singletonScanner ?: BleScaleScanner(appContext).also { singletonScanner = it }
                }
                val profileRepo = UserProfileRepository(appContext)
                val historyRepo = WeighInHistoryRepository(appContext)
                val settingsRepo = AppSettingsRepository(appContext)
                return MainScreenViewModel(scanner, profileRepo, historyRepo, settingsRepo) as T
            }
        }
    }
}
