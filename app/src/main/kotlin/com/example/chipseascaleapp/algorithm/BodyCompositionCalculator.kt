package com.example.chipseascaleapp.algorithm

import com.example.chipseascaleapp.model.BmiCategory
import com.example.chipseascaleapp.model.BodyComposition
import com.example.chipseascaleapp.model.Gender
import com.example.chipseascaleapp.model.ScaleMeasurement
import com.example.chipseascaleapp.model.UserProfile
import kotlin.math.roundToInt

object BodyCompositionCalculator {

    fun calculate(measurement: ScaleMeasurement, profile: UserProfile): BodyComposition {
        val weightKg = measurement.weightKg
        val heightCm = profile.heightCm
        val ageYears = profile.ageYears
        val gender = profile.gender
        val impedanceOhm = measurement.impedanceOhm

        // 1. BMI calculation
        val heightM = heightCm / 100.0f
        val bmi = if (heightM > 0f && weightKg > 0f) {
            roundToOneDecimal(weightKg / (heightM * heightM))
        } else {
            0f
        }

        val bmiCategory = when {
            bmi < 18.5f -> BmiCategory.UNDERWEIGHT
            bmi < 25.0f -> BmiCategory.NORMAL
            bmi < 30.0f -> BmiCategory.OVERWEIGHT
            else -> BmiCategory.OBESE
        }

        // 2. Body Fat calculation using reverse-engineered CsAlgoBuilder formula
        val hasImpedance = impedanceOhm > 0f && weightKg > 0f

        val (fatMassKg, bodyFatRate, leanMassKg) = if (hasImpedance) {
            val rawFatMass = when (gender) {
                Gender.MALE -> {
                    val f7 = heightCm * -0.3315f
                    val f3 = f7 + (0.6216f * weightKg) + (ageYears * 0.0183f) + (impedanceOhm * 0.0085f)
                    val f4 = 22.554f
                    f3 + f4
                }
                Gender.FEMALE -> {
                    val f8 = heightCm * -0.3332f
                    val f3 = f8 + (0.7509f * weightKg) + (ageYears * 0.0196f) + (impedanceOhm * 0.0072f)
                    val f4 = 22.7193f
                    f3 + f4
                }
            }

            // Exactly as implemented in OKOK CsAlgoBuilder:
            // Calculate raw BFR (%), clamp between 5.0% and 55.0%, then derive fat mass and lean mass.
            val rawBfr = (rawFatMass / weightKg) * 100.0f
            val clampedFatRate = rawBfr.coerceIn(5.0f, 55.0f)
            val calculatedFatMass = (clampedFatRate * weightKg) / 100.0f
            val calculatedLeanMass = (weightKg - calculatedFatMass).coerceAtLeast(0f)

            Triple(
                roundToOneDecimal(calculatedFatMass),
                roundToOneDecimal(clampedFatRate),
                roundToOneDecimal(calculatedLeanMass)
            )
        } else {
            Triple(null, null, null)
        }

        return BodyComposition(
            bmi = bmi,
            bmiCategory = bmiCategory,
            bodyFatRate = bodyFatRate,
            fatMassKg = fatMassKg,
            leanMassKg = leanMassKg
        )
    }

    private fun roundToOneDecimal(value: Float): Float {
        return (value * 10f).roundToInt() / 10f
    }
}
