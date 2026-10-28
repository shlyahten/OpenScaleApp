package com.example.chipseascaleapp.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chipseascaleapp.algorithm.BodyCompositionCalculator
import com.example.chipseascaleapp.ble.BleScaleScanner
import com.example.chipseascaleapp.ble.BleScannerState
import com.example.chipseascaleapp.data.UserProfileRepository
import com.example.chipseascaleapp.model.BodyComposition
import com.example.chipseascaleapp.model.DeviceType
import com.example.chipseascaleapp.model.ScaleMeasurement
import com.example.chipseascaleapp.model.UserProfile
import com.example.chipseascaleapp.model.WeightUnit
import com.example.chipseascaleapp.ble.ScaleScanner
import com.example.chipseascaleapp.data.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val isScanning: Boolean = false,
    val isBluetoothEnabled: Boolean = true,
    val hasPermissions: Boolean = false,
    val scaleMeasurement: ScaleMeasurement? = null,
    val bodyComposition: BodyComposition? = null,
    val userProfile: UserProfile = UserProfile(),
    val errorMessage: String? = null,
    val isDemoMode: Boolean = false
)

class MainScreenViewModel(
    private val scanner: ScaleScanner,
    private val userProfileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MainUiState(
            hasPermissions = scanner.hasRequiredPermissions(),
            isBluetoothEnabled = scanner.isBluetoothEnabled(),
            userProfile = userProfileRepository.userProfile.value
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // Observe user profile changes
        viewModelScope.launch {
            userProfileRepository.userProfile.collect { profile ->
                _uiState.update { current ->
                    val recomputed = current.scaleMeasurement?.let {
                        BodyCompositionCalculator.calculate(it, profile)
                    }
                    current.copy(userProfile = profile, bodyComposition = recomputed)
                }
            }
        }

        // Observe incoming scale measurements
        viewModelScope.launch {
            scanner.measurements.collect { measurement ->
                _uiState.update { current ->
                    val composition = BodyCompositionCalculator.calculate(measurement, current.userProfile)
                    current.copy(
                        scaleMeasurement = measurement,
                        bodyComposition = composition,
                        errorMessage = null
                    )
                }
            }
        }

        // Observe scanner state
        viewModelScope.launch {
            scanner.scannerState.collect { state ->
                when (state) {
                    is BleScannerState.Idle -> {
                        _uiState.update { it.copy(isScanning = false) }
                    }
                    is BleScannerState.Scanning -> {
                        _uiState.update { it.copy(isScanning = true, errorMessage = null) }
                    }
                    is BleScannerState.BluetoothDisabled -> {
                        _uiState.update { it.copy(isScanning = false, isBluetoothEnabled = false) }
                    }
                    is BleScannerState.MissingPermissions -> {
                        _uiState.update { it.copy(isScanning = false, hasPermissions = false) }
                    }
                    is BleScannerState.Error -> {
                        _uiState.update { it.copy(isScanning = false, errorMessage = state.message) }
                    }
                }
            }
        }
    }

    fun startScan() {
        val hasPerms = scanner.hasRequiredPermissions()
        val btEnabled = scanner.isBluetoothEnabled()
        _uiState.update { it.copy(hasPermissions = hasPerms, isBluetoothEnabled = btEnabled) }
        if (hasPerms && btEnabled) {
            scanner.startScan()
        }
    }

    fun stopScan() {
        scanner.stopScan()
    }

    fun onPermissionsResult(allGranted: Boolean) {
        _uiState.update { it.copy(hasPermissions = allGranted) }
        if (allGranted) {
            startScan()
        }
    }

    fun updateUserProfile(profile: UserProfile) {
        userProfileRepository.saveProfile(profile)
    }

    fun clearMeasurement() {
        _uiState.update { it.copy(scaleMeasurement = null, bodyComposition = null, isDemoMode = false) }
    }

    fun injectDemoMeasurement(isStabilized: Boolean = true, weightKg: Float = 77.10f, impedanceOhm: Float = 600.0f) {
        val demoMeasurement = ScaleMeasurement(
            weightKg = weightKg,
            impedanceOhm = impedanceOhm,
            isStabilized = isStabilized,
            unit = WeightUnit.KG,
            deviceType = DeviceType.BODY_FAT_SCALE,
            sequenceIndex = 42,
            productId = 0x0A11,
            macAddress = "80:F4:16:01:47:50"
        )
        _uiState.update { it.copy(isDemoMode = true) }
        scanner.injectMeasurementForTesting(demoMeasurement)
    }

    override fun onCleared() {
        super.onCleared()
        scanner.stopScan()
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                val scanner = BleScaleScanner(appContext)
                val repo = UserProfileRepository(appContext)
                return MainScreenViewModel(scanner, repo) as T
            }
        }
    }
}
