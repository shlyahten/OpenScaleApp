package com.example.chipseascaleapp.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.chipseascaleapp.model.BmiCategory
import com.example.chipseascaleapp.model.BodyComposition
import com.example.chipseascaleapp.model.DeviceType
import com.example.chipseascaleapp.model.Gender
import com.example.chipseascaleapp.model.ScaleMeasurement
import com.example.chipseascaleapp.model.UserProfile
import com.example.chipseascaleapp.model.WeightUnit
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI tests for [com.example.chipseascaleapp.ui.main.MainScreen]. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    val sampleMeasurement = ScaleMeasurement(
        weightKg = 77.10f,
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
    composeTestRule.setContent {
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

  @Test
  fun weightDisplay_exists() {
    composeTestRule.onNodeWithText("77.10").assertExists()
    composeTestRule.onNodeWithText("Вес зафиксирован").assertExists()
  }
}
