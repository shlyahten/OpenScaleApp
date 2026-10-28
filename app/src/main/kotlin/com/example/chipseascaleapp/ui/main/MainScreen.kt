package com.example.chipseascaleapp.ui.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chipseascaleapp.R
import com.example.chipseascaleapp.ble.BleScaleScanner
import com.example.chipseascaleapp.model.BmiCategory
import com.example.chipseascaleapp.model.BodyComposition
import com.example.chipseascaleapp.model.DeviceType
import com.example.chipseascaleapp.model.Gender
import com.example.chipseascaleapp.model.ScaleMeasurement
import com.example.chipseascaleapp.model.UserProfile
import com.example.chipseascaleapp.model.WeightUnit
import com.example.chipseascaleapp.ui.main.components.BleStatusCard
import com.example.chipseascaleapp.ui.main.components.MetricsGrid
import com.example.chipseascaleapp.ui.main.components.UserProfileCard
import com.example.chipseascaleapp.ui.main.components.WeightHeroCard
import com.example.chipseascaleapp.ui.theme.ChipseaScaleAppTheme

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel(factory = MainScreenViewModel.provideFactory(LocalContext.current))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        viewModel.onPermissionsResult(allGranted)
    }

    LaunchedEffect(Unit) {
        if (uiState.hasPermissions) {
            viewModel.startScan()
        } else {
            permissionLauncher.launch(BleScaleScanner.getRequiredPermissions())
        }
    }

    // Lifecycle observer to stop scanning in background and resume on return
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.refreshDeviceState()
                    if (viewModel.uiState.value.hasPermissions &&
                        viewModel.uiState.value.isBluetoothEnabled &&
                        viewModel.uiState.value.isLocationEnabled
                    ) {
                        viewModel.startScan()
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    viewModel.stopScan()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopScan()
        }
    }

    MainScreenContent(
        uiState = uiState,
        onRequestPermissions = {
            permissionLauncher.launch(BleScaleScanner.getRequiredPermissions())
        },
        onStartScan = { viewModel.startScan() },
        onStopScan = { viewModel.stopScan() },
        onSaveProfile = { viewModel.updateUserProfile(it) },
        onInjectDemo = { stabilized ->
            viewModel.injectDemoMeasurement(isStabilized = stabilized, unit = WeightUnit.KG)
        },
        onInjectDemoLb = {
            viewModel.injectDemoMeasurement(isStabilized = true, unit = WeightUnit.LB)
        },
        onClear = { viewModel.clearMeasurement() },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    uiState: MainUiState,
    onRequestPermissions: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onSaveProfile: (UserProfile) -> Unit,
    onInjectDemo: (stabilized: Boolean) -> Unit,
    onInjectDemoLb: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.isScanning) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFF4CAF50), CircleShape)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Weight Card
            WeightHeroCard(
                measurement = uiState.scaleMeasurement,
                isScanning = uiState.isScanning
            )

            // Body Composition Metrics
            MetricsGrid(
                measurement = uiState.scaleMeasurement,
                bodyComposition = uiState.bodyComposition
            )

            // User Profile Section
            UserProfileCard(
                profile = uiState.userProfile,
                onSaveProfile = onSaveProfile
            )

            // BLE Scanner State & Demo controls
            BleStatusCard(
                hasPermissions = uiState.hasPermissions,
                isBluetoothEnabled = uiState.isBluetoothEnabled,
                isLocationEnabled = uiState.isLocationEnabled,
                errorMessage = uiState.errorMessage,
                isScanning = uiState.isScanning,
                isDemoMode = uiState.isDemoMode,
                onRequestPermissions = onRequestPermissions,
                onStartScan = onStartScan,
                onStopScan = onStopScan,
                onInjectDemo = onInjectDemo,
                onInjectDemoLb = onInjectDemoLb,
                onClear = onClear
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenContentPreview() {
    ChipseaScaleAppTheme {
        val sampleMeasurement = ScaleMeasurement(
            weightKg = 77.10f,
            displayWeight = 77.10f,
            impedanceOhm = 600.0f,
            isStabilized = true,
            unit = WeightUnit.KG,
            deviceType = DeviceType.BODY_FAT_SCALE,
            sequenceIndex = 42,
            productId = 0x0A11,
            macAddress = "80:F4:16:01:47:50"
        )
        val sampleComposition = BodyComposition(
            bmi = 24.3f,
            bmiCategory = BmiCategory.NORMAL,
            bodyFatRate = 18.5f,
            fatMassKg = 14.3f,
            leanMassKg = 62.8f
        )
        MainScreenContent(
            uiState = MainUiState(
                isScanning = true,
                scaleMeasurement = sampleMeasurement,
                bodyComposition = sampleComposition,
                userProfile = UserProfile(gender = Gender.MALE, heightCm = 178f, ageYears = 30)
            ),
            onRequestPermissions = {},
            onStartScan = {},
            onStopScan = {},
            onSaveProfile = {},
            onInjectDemo = {},
            onInjectDemoLb = {},
            onClear = {}
        )
    }
}
