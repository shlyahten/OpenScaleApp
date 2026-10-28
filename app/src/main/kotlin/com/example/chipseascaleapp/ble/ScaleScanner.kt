package com.example.chipseascaleapp.ble

import com.example.chipseascaleapp.model.ScaleMeasurement
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ScaleScanner {
    val scannerState: StateFlow<BleScannerState>
    val measurements: SharedFlow<ScaleMeasurement>
    fun hasRequiredPermissions(): Boolean
    fun isBluetoothEnabled(): Boolean
    fun isLocationEnabled(): Boolean = true
    fun startScan()
    fun stopScan()
    fun injectMeasurementForTesting(measurement: ScaleMeasurement)
}
