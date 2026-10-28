package com.example.chipseascaleapp.algorithm

import com.example.chipseascaleapp.model.BmiCategory
import com.example.chipseascaleapp.model.DeviceType
import com.example.chipseascaleapp.model.Gender
import com.example.chipseascaleapp.model.ScaleMeasurement
import com.example.chipseascaleapp.model.UserProfile
import com.example.chipseascaleapp.model.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyCompositionCalculatorTest {

    @Test
    fun calculate_maleProfileWithImpedance() {
        val measurement = ScaleMeasurement(
            weightKg = 77.10f,
            impedanceOhm = 600.0f,
            isStabilized = true,
            unit = WeightUnit.KG,
            deviceType = DeviceType.BODY_FAT_SCALE,
            sequenceIndex = 1,
            productId = 0x0A11,
            macAddress = "80:F4:16:01:47:50"
        )
        val profile = UserProfile(
            gender = Gender.MALE,
            heightCm = 178f,
            ageYears = 30
        )

        val result = BodyCompositionCalculator.calculate(measurement, profile)

        // Expected BMI: 77.10 / (1.78^2) = 24.33 -> 24.3
        assertEquals(24.3f, result.bmi, 0.1f)
        assertEquals(BmiCategory.NORMAL, result.bmiCategory)

        // Expected Fat Mass: ~17.1 kg
        assertNotNull(result.fatMassKg)
        assertEquals(17.1f, result.fatMassKg!!, 0.2f)

        // Expected Body Fat %: ~22.2 %
        assertNotNull(result.bodyFatRate)
        assertEquals(22.2f, result.bodyFatRate!!, 0.2f)

        // Expected Lean Mass: 77.1 - 17.1 = 60.0 kg
        assertNotNull(result.leanMassKg)
        assertEquals(60.0f, result.leanMassKg!!, 0.2f)

        // Verify total mass consistency
        assertEquals(measurement.weightKg, result.fatMassKg + result.leanMassKg, 0.2f)
    }

    @Test
    fun calculate_femaleProfileWithImpedance() {
        val measurement = ScaleMeasurement(
            weightKg = 60.0f,
            impedanceOhm = 650.0f,
            isStabilized = true,
            unit = WeightUnit.KG,
            deviceType = DeviceType.BODY_FAT_SCALE,
            sequenceIndex = 1,
            productId = 0x0A11,
            macAddress = "80:F4:16:01:47:50"
        )
        val profile = UserProfile(
            gender = Gender.FEMALE,
            heightCm = 165f,
            ageYears = 28
        )

        val result = BodyCompositionCalculator.calculate(measurement, profile)

        // Expected BMI: 60.0 / (1.65^2) = 22.0
        assertEquals(22.0f, result.bmi, 0.1f)
        assertEquals(BmiCategory.NORMAL, result.bmiCategory)

        // Expected Fat Mass: ~18.0 kg
        assertNotNull(result.fatMassKg)
        assertEquals(18.0f, result.fatMassKg!!, 0.2f)

        // Expected Body Fat Rate: ~30.0 %
        assertNotNull(result.bodyFatRate)
        assertEquals(30.0f, result.bodyFatRate!!, 0.2f)

        // Expected Lean Mass: ~42.0 kg
        assertNotNull(result.leanMassKg)
        assertEquals(42.0f, result.leanMassKg!!, 0.2f)

        // Verify total mass consistency
        assertEquals(measurement.weightKg, result.fatMassKg + result.leanMassKg, 0.2f)
    }

    @Test
    fun calculate_zeroImpedance_bodyFatUnavailable() {
        // User stood on scale with socks or simple scale without impedance
        val measurement = ScaleMeasurement(
            weightKg = 85.0f,
            impedanceOhm = 0f,
            isStabilized = true,
            unit = WeightUnit.KG,
            deviceType = DeviceType.SIMPLE_SCALE,
            sequenceIndex = 1,
            productId = 0x0A11,
            macAddress = "80:F4:16:01:47:50"
        )
        val profile = UserProfile(
            gender = Gender.MALE,
            heightCm = 180f,
            ageYears = 25
        )

        val result = BodyCompositionCalculator.calculate(measurement, profile)

        // BMI: 85.0 / (1.8^2) = 26.2 -> OVERWEIGHT
        assertEquals(26.2f, result.bmi, 0.1f)
        assertEquals(BmiCategory.OVERWEIGHT, result.bmiCategory)

        // Body fat and lean mass should be null
        assertNull(result.bodyFatRate)
        assertNull(result.fatMassKg)
        assertNull(result.leanMassKg)
    }

    @Test
    fun calculate_extremeImpedance_clampedWithinBounds() {
        val measurementLow = ScaleMeasurement(
            weightKg = 70.0f,
            impedanceOhm = 10.0f, // extremely low
            isStabilized = true,
            unit = WeightUnit.KG,
            deviceType = DeviceType.BODY_FAT_SCALE,
            sequenceIndex = 1,
            productId = 0x0A11,
            macAddress = "80:F4:16:01:47:50"
        )
        val profile = UserProfile(gender = Gender.MALE, heightCm = 200f, ageYears = 18)

        val resultLow = BodyCompositionCalculator.calculate(measurementLow, profile)
        assertTrue(resultLow.bodyFatRate!! >= 5.0f)

        val measurementHigh = ScaleMeasurement(
            weightKg = 150.0f,
            impedanceOhm = 2000.0f, // extremely high
            isStabilized = true,
            unit = WeightUnit.KG,
            deviceType = DeviceType.BODY_FAT_SCALE,
            sequenceIndex = 1,
            productId = 0x0A11,
            macAddress = "80:F4:16:01:47:50"
        )
        val resultHigh = BodyCompositionCalculator.calculate(measurementHigh, profile)
        assertTrue(resultHigh.bodyFatRate!! <= 55.0f)
    }

    @Test
    fun calculate_edgeCases_noCrash() {
        val measurementZeroWeight = ScaleMeasurement(
            weightKg = 0f,
            impedanceOhm = 0f,
            isStabilized = false,
            unit = WeightUnit.KG,
            deviceType = DeviceType.SIMPLE_SCALE,
            sequenceIndex = 0,
            productId = 0,
            macAddress = "00:00:00:00:00:00"
        )
        val profileZeroHeight = UserProfile(
            gender = Gender.MALE,
            heightCm = 0f,
            ageYears = 0
        )

        val result = BodyCompositionCalculator.calculate(measurementZeroWeight, profileZeroHeight)
        assertEquals(0f, result.bmi, 0.001f)
        assertNull(result.bodyFatRate)
    }
}
