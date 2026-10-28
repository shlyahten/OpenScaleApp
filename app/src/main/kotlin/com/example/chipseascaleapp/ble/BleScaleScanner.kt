package com.example.chipseascaleapp.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.chipseascaleapp.model.ScaleMeasurement
import com.example.chipseascaleapp.parser.ChipseaScaleParser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface BleScannerState {
    object Idle : BleScannerState
    object Scanning : BleScannerState
    object BluetoothDisabled : BleScannerState
    object MissingPermissions : BleScannerState
    data class Error(val message: String) : BleScannerState
}

class BleScaleScanner(private val context: Context) : ScaleScanner {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter
    private val leScanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    private val _scannerState = MutableStateFlow<BleScannerState>(BleScannerState.Idle)
    override val scannerState: StateFlow<BleScannerState> = _scannerState.asStateFlow()

    private val _measurements = MutableSharedFlow<ScaleMeasurement>(extraBufferCapacity = 64)
    override val measurements: SharedFlow<ScaleMeasurement> = _measurements.asSharedFlow()

    private var activeCallback: ScanCallback? = null

    override fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    override fun startScan() {
        if (!hasRequiredPermissions()) {
            _scannerState.value = BleScannerState.MissingPermissions
            return
        }

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _scannerState.value = BleScannerState.BluetoothDisabled
            return
        }

        val scanner = leScanner
        if (scanner == null) {
            _scannerState.value = BleScannerState.Error("BLE сканер недоступен на этом устройстве")
            return
        }

        if (activeCallback != null) {
            // Already scanning
            _scannerState.value = BleScannerState.Scanning
            return
        }

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.scanRecord?.let { record ->
                    val measurement = ChipseaScaleParser.parse(record)
                    if (measurement != null) {
                        _measurements.tryEmit(measurement)
                    }
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results?.forEach { result ->
                    result.scanRecord?.let { record ->
                        val measurement = ChipseaScaleParser.parse(record)
                        if (measurement != null) {
                            _measurements.tryEmit(measurement)
                        }
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                _scannerState.value = BleScannerState.Error("Ошибка сканирования BLE (код: $errorCode)")
                activeCallback = null
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        try {
            scanner.startScan(null, settings, scanCallback)
            activeCallback = scanCallback
            _scannerState.value = BleScannerState.Scanning
        } catch (e: SecurityException) {
            _scannerState.value = BleScannerState.MissingPermissions
            activeCallback = null
        } catch (e: Exception) {
            _scannerState.value = BleScannerState.Error(e.message ?: "Не удалось запустить сканирование")
            activeCallback = null
        }
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    override fun stopScan() {
        val callback = activeCallback ?: return
        try {
            if (hasRequiredPermissions() && bluetoothAdapter?.isEnabled == true) {
                leScanner?.stopScan(callback)
            }
        } catch (_: Exception) {
            // Ignore clean-up errors
        } finally {
            activeCallback = null
            _scannerState.value = BleScannerState.Idle
        }
    }

    override fun injectMeasurementForTesting(measurement: ScaleMeasurement) {
        _measurements.tryEmit(measurement)
    }

    companion object {
        fun getRequiredPermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
        }
    }
}
