package com.openscaleapp.ui.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.openscaleapp.R
import com.openscaleapp.ble.BleScaleScanner
import com.openscaleapp.model.BmiCategory
import com.openscaleapp.model.DeviceType
import com.openscaleapp.model.Gender
import com.openscaleapp.model.ScaleMeasurement
import com.openscaleapp.model.UserProfile
import com.openscaleapp.model.WeighInRecord
import com.openscaleapp.model.WeightUnit
import com.openscaleapp.ui.main.components.BleStatusCard
import com.openscaleapp.ui.main.components.HistorySection
import com.openscaleapp.ui.main.components.MetricsGrid
import com.openscaleapp.ui.main.components.UserProfileCard
import com.openscaleapp.ui.main.components.WeightHeroCard
import com.openscaleapp.ui.theme.AppLanguage
import com.openscaleapp.ui.theme.AppTheme
import com.openscaleapp.ui.theme.OpenScaleAppTheme

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
        if (!uiState.hasPermissions) {
            permissionLauncher.launch(BleScaleScanner.getRequiredPermissions())
        }
    }

    // Lifecycle observer to stop scanning in background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.refreshDeviceState()
                }
                Lifecycle.Event.ON_STOP -> {
                    viewModel.cancelWeighIn()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.cancelWeighIn()
        }
    }

    MainScreenContent(
        uiState = uiState,
        onRequestPermissions = {
            permissionLauncher.launch(BleScaleScanner.getRequiredPermissions())
        },
        onStartWeighIn = { viewModel.startWeighIn() },
        onCancelWeighIn = { viewModel.cancelWeighIn() },
        onClearHistorySelection = { viewModel.clearHistorySelection() },
        onSelectHistoryRecord = { viewModel.selectHistoryRecord(it) },
        onDeleteHistoryRecord = { viewModel.deleteHistoryRecord(it) },
        onClearAllHistory = { viewModel.clearAllHistory() },
        onSaveProfile = { viewModel.updateUserProfile(it) },
        onSetAppTheme = { viewModel.setAppTheme(it) },
        onSetAppLanguage = { viewModel.setAppLanguage(it) },
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
    onStartWeighIn: () -> Unit,
    onCancelWeighIn: () -> Unit,
    onClearHistorySelection: () -> Unit,
    onSelectHistoryRecord: (WeighInRecord) -> Unit,
    onDeleteHistoryRecord: (String) -> Unit,
    onClearAllHistory: () -> Unit,
    onSaveProfile: (UserProfile) -> Unit,
    onSetAppTheme: (AppTheme) -> Unit,
    onSetAppLanguage: (AppLanguage) -> Unit,
    onInjectDemo: (stabilized: Boolean) -> Unit,
    onInjectDemoLb: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showThemeMenu by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }

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
                navigationIcon = {
                    // Language Switcher
                    Box {
                        IconButton(onClick = { showLanguageMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = stringResource(R.string.language_dialog_title),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(lang.titleResId)) },
                                    trailingIcon = {
                                        if (uiState.appLanguage == lang) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        onSetAppLanguage(lang)
                                        showLanguageMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Theme Switcher
                    Box {
                        val themeIcon = when (uiState.appTheme) {
                            AppTheme.LIGHT -> Icons.Default.LightMode
                            AppTheme.DARK -> Icons.Default.DarkMode
                            AppTheme.SYSTEM -> Icons.Default.BrightnessAuto
                        }
                        IconButton(onClick = { showThemeMenu = true }) {
                            Icon(
                                imageVector = themeIcon,
                                contentDescription = stringResource(R.string.theme_dialog_title),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false }
                        ) {
                            AppTheme.entries.forEach { theme ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(theme.titleResId)) },
                                    trailingIcon = {
                                        if (uiState.appTheme == theme) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        onSetAppTheme(theme)
                                        showThemeMenu = false
                                    }
                                )
                            }
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
            // Hero Weight Card with Primary "Взвешивание" action
            WeightHeroCard(
                measurement = uiState.scaleMeasurement,
                isScanning = uiState.isScanning,
                scanRemainingSeconds = uiState.scanRemainingSeconds,
                selectedHistoryRecord = uiState.selectedHistoryRecord,
                onStartWeighIn = onStartWeighIn,
                onCancelWeighIn = onCancelWeighIn,
                onClearHistorySelection = onClearHistorySelection
            )

            // Body Composition Metrics (all 17 metrics)
            MetricsGrid(
                measurement = uiState.scaleMeasurement,
                bodyComposition = uiState.bodyComposition
            )

            // History Section (saved previous weigh-ins)
            HistorySection(
                history = uiState.history,
                selectedRecordId = uiState.selectedHistoryRecord?.id,
                onSelectRecord = onSelectHistoryRecord,
                onDeleteRecord = onDeleteHistoryRecord,
                onClearAll = onClearAllHistory
            )

            // User Profile Section
            UserProfileCard(
                profile = uiState.userProfile,
                onSaveProfile = onSaveProfile
            )

            // BLE State & Diagnostics & Demo testing controls
            BleStatusCard(
                hasPermissions = uiState.hasPermissions,
                isBluetoothEnabled = uiState.isBluetoothEnabled,
                isLocationEnabled = uiState.isLocationEnabled,
                errorMessage = uiState.errorMessage,
                onRequestPermissions = onRequestPermissions,
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
    OpenScaleAppTheme {
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
        MainScreenContent(
            uiState = MainUiState(
                isScanning = false,
                scaleMeasurement = sampleMeasurement,
                userProfile = UserProfile(gender = Gender.MALE, heightCm = 178f, ageYears = 31)
            ),
            onRequestPermissions = {},
            onStartWeighIn = {},
            onCancelWeighIn = {},
            onClearHistorySelection = {},
            onSelectHistoryRecord = {},
            onDeleteHistoryRecord = {},
            onClearAllHistory = {},
            onSaveProfile = {},
            onSetAppTheme = {},
            onSetAppLanguage = {},
            onInjectDemo = {},
            onInjectDemoLb = {},
            onClear = {}
        )
    }
}
