package com.openscaleapp.algorithm

import com.openscaleapp.model.BmiCategory
import com.openscaleapp.model.DeviceType
import com.openscaleapp.model.Gender
import com.openscaleapp.model.MetricStatus
import com.openscaleapp.model.ScaleMeasurement
import com.openscaleapp.model.UserProfile
import com.openscaleapp.model.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyCompositionCalculatorTest {

    @Test
    fun calculate_okokGroundTruthScreenshotMatch() {
        // Test vector from user's actual weigh-in in OKOK International app:
        // Weight: 76.65 kg, Height: 178 cm, Age: 31, Impedance: 600.0 Ohm, Male
        val measurement = ScaleMeasurement(
            weightKg = 76.65f,
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
            ageYears = 31
        )

        val result = BodyCompositionCalculator.calculate(measurement, profile)

        // 1. BMI: OKOK = 24.2, Status = Healthy
        assertEquals(24.2f, result.bmi, 0.05f)
        assertEquals(MetricStatus.HEALTHY, result.bmiStatus)

        // 2. Fat %: OKOK = 22.0 %, Status = High
        assertNotNull(result.bodyFatRate)
        assertEquals(22.0f, result.bodyFatRate!!, 0.05f)
        assertEquals(MetricStatus.HIGH, result.bodyFatStatus)

        // 3. Body fat weight: OKOK = 16.9 kg, Status = High
        assertNotNull(result.fatMassKg)
        assertEquals(16.9f, result.fatMassKg!!, 0.05f)
        assertEquals(MetricStatus.HIGH, result.fatMassStatus)

        // 4. Skeletal muscle mass %: OKOK = 40.0 %, Status = Perfect
        assertNotNull(result.skeletalMuscleRate)
        assertEquals(40.0f, result.skeletalMuscleRate!!, 0.05f)
        assertEquals(MetricStatus.PERFECT, result.skeletalMuscleStatus)

        // 5. Skeletal muscle weight: OKOK = 30.7 kg, Status = Perfect
        assertNotNull(result.skeletalMuscleKg)
        assertEquals(30.7f, result.skeletalMuscleKg!!, 0.05f)

        // 6. Muscle %: OKOK = 73.6 %, Status = Healthy
        assertNotNull(result.muscleRate)
        assertEquals(73.6f, result.muscleRate!!, 0.05f)
        assertEquals(MetricStatus.HEALTHY, result.muscleStatus)

        // 7. Muscle weight: OKOK = 56.4 kg, Status = Healthy
        assertNotNull(result.muscleMassKg)
        assertEquals(56.4f, result.muscleMassKg!!, 0.05f)

        // 8. Water %: OKOK = 54.0 %, Status = Healthy
        assertNotNull(result.waterRate)
        assertEquals(54.0f, result.waterRate!!, 0.05f)
        assertEquals(MetricStatus.HEALTHY, result.waterStatus)

        // 9. Weight of water: OKOK = 41.4 kg, Status = Healthy
        assertNotNull(result.waterMassKg)
        assertEquals(41.4f, result.waterMassKg!!, 0.05f)

        // 10. Visceral Fat: OKOK = 10.0, Status = High
        assertNotNull(result.visceralFat)
        assertEquals(10.0f, result.visceralFat!!, 0.05f)
        assertEquals(MetricStatus.HIGH, result.visceralFatStatus)

        // 11. Bone Mass: OKOK = 3.3 kg, Status = Perfect
        assertNotNull(result.boneMassKg)
        assertEquals(3.3f, result.boneMassKg!!, 0.05f)
        assertEquals(MetricStatus.PERFECT, result.boneMassStatus)

        // 12. Metabolism (BMR): OKOK = 1688.0 kcal, Status = High
        assertNotNull(result.bmrKcal)
        assertEquals(1688.0f, result.bmrKcal!!, 0.5f)
        assertEquals(MetricStatus.HIGH, result.bmrStatus)

        // 13. Protein %: OKOK = 19.7 %, Status = Healthy
        assertNotNull(result.proteinRate)
        assertEquals(19.7f, result.proteinRate!!, 0.05f)
        assertEquals(MetricStatus.HEALTHY, result.proteinStatus)

        // 14. Obesity Degree %: OKOK = 11.7 %, Status = Overweight
        assertNotNull(result.obesityDegree)
        assertEquals(11.7f, result.obesityDegree!!, 0.05f)
        assertEquals(MetricStatus.OVERWEIGHT, result.obesityDegreeStatus)

        // 15. Body age: OKOK = 35.0 (35 years)
        assertNotNull(result.bodyAge)
        assertEquals(35, result.bodyAge)

        // 16. Weight Without Fat: OKOK = 59.79 kg
        assertNotNull(result.leanMassKg)
        assertEquals(59.79f, result.leanMassKg!!, 0.05f)
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
        assertEquals(MetricStatus.HEALTHY, result.bmiStatus)

        // Expected Fat Mass & Rate
        assertNotNull(result.fatMassKg)
        assertEquals(18.0f, result.fatMassKg!!, 0.2f)
        assertNotNull(result.bodyFatRate)
        assertEquals(30.0f, result.bodyFatRate!!, 0.2f)

        // Lean Mass: 60.0 - 18.0 = 42.0 kg
        assertNotNull(result.leanMassKg)
        assertEquals(42.0f, result.leanMassKg!!, 0.2f)

        // Water, BMR, Muscle present
        assertNotNull(result.waterRate)
        assertNotNull(result.muscleRate)
        assertNotNull(result.bmrKcal)
        assertNotNull(result.boneMassKg)
    }

    @Test
    fun calculate_zeroImpedance_bodyFatUnavailable() {
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

        assertEquals(26.2f, result.bmi, 0.1f)
        assertEquals(BmiCategory.OVERWEIGHT, result.bmiCategory)
        assertEquals(MetricStatus.OVERWEIGHT, result.bmiStatus)

        assertNull(result.bodyFatRate)
        assertNull(result.fatMassKg)
        assertNull(result.skeletalMuscleRate)
        assertNull(result.muscleRate)
        assertNull(result.waterRate)
        assertNull(result.visceralFat)
        assertNull(result.boneMassKg)
        assertNull(result.bmrKcal)
        assertNull(result.proteinRate)
        assertNull(result.obesityDegree)
        assertNull(result.bodyAge)
        assertNull(result.leanMassKg)
    }

    @Test
    fun calculate_extremeImpedance_clampedWithinBounds() {
        val measurementLow = ScaleMeasurement(
            weightKg = 70.0f,
            impedanceOhm = 10.0f,
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
            impedanceOhm = 2000.0f,
            isStabilized = true,
            unit = WeightUnit.KG,
            deviceType = DeviceType.BODY_FAT_SCALE,
            sequenceIndex = 1,
            productId = 0x0A11,
            macAddress = "80:F4:16:01:47:50"
        )
        val resultHigh = BodyCompositionCalculator.calculate(measurementHigh, profile)
        assertTrue(resultHigh.bodyFatRate!! <= 45.0f)
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
