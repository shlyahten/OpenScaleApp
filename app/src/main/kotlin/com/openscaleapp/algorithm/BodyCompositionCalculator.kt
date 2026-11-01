package com.openscaleapp.algorithm

import com.openscaleapp.model.BmiCategory
import com.openscaleapp.model.BodyComposition
import com.openscaleapp.model.Gender
import com.openscaleapp.model.MetricStatus
import com.openscaleapp.model.ScaleMeasurement
import com.openscaleapp.model.UserProfile
import java.math.BigDecimal
import java.math.RoundingMode

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

        val bmiStatus = when (bmiCategory) {
            BmiCategory.UNDERWEIGHT -> MetricStatus.LOW
            BmiCategory.NORMAL -> MetricStatus.HEALTHY
            BmiCategory.OVERWEIGHT -> MetricStatus.OVERWEIGHT
            BmiCategory.OBESE -> MetricStatus.ALERT
        }

        val hasImpedance = impedanceOhm > 0f && weightKg > 0f && heightCm > 0f && ageYears > 0

        if (!hasImpedance) {
            return BodyComposition(
                bmi = bmi,
                bmiCategory = bmiCategory,
                bmiStatus = bmiStatus
            )
        }

        val isMale = gender == Gender.MALE
        val sexByte = if (isMale) 1 else 0

        // 2. Body Fat % (CsAlgoBuilder.getBFR_Raw & getBFR) and Fat Mass (getFM)
        val rawFatMass = if (isMale) {
            (heightCm * -0.3315f) + (weightKg * 0.6216f) + (ageYears * 0.0183f) + (impedanceOhm * 0.0085f) + 22.554f
        } else {
            (heightCm * -0.3332f) + (weightKg * 0.7509f) + (ageYears * 0.0196f) + (impedanceOhm * 0.0072f) + 22.7193f
        }
        val rawBfr = (rawFatMass / weightKg) * 100.0f
        val clampedFatRate = rawBfr.coerceIn(5.0f, 45.0f)
        val bodyFatRate = roundToOneDecimal(clampedFatRate)
        val bfrScale2 = roundToTwoDecimals(clampedFatRate)
        val calculatedFatMass = (bfrScale2 * weightKg) / 100.0f
        val fatMassKg = roundToOneDecimal(calculatedFatMass)

        val bodyFatStatus = if (isMale) {
            when {
                bodyFatRate < 11.0f -> MetricStatus.LOW
                bodyFatRate <= 17.0f -> MetricStatus.HEALTHY
                bodyFatRate <= 27.0f -> MetricStatus.HIGH
                else -> MetricStatus.ALERT
            }
        } else {
            when {
                bodyFatRate < 21.0f -> MetricStatus.LOW
                bodyFatRate <= 28.0f -> MetricStatus.HEALTHY
                bodyFatRate <= 40.0f -> MetricStatus.HIGH
                else -> MetricStatus.ALERT
            }
        }
        val fatMassStatus = bodyFatStatus

        // Weight Without Fat (CsAlgoBuilder.getLBM)
        val leanMassKg = roundToTwoDecimals((weightKg - calculatedFatMass).coerceAtLeast(0f))

        // 3. Muscle Mass (CsAlgoBuilder.getSLM_Raw) and Muscle % (CsAlgoBuilder.getSLMPercent)
        val slmRaw = if (rawBfr > 45.0f) {
            (weightKg - (0.45f * weightKg)) - 4.0f
        } else if (rawBfr < 5.0f) {
            (weightKg - (0.05f * weightKg)) - 1.0f
        } else {
            val f5 = if (isMale) {
                (((heightCm * 0.2867f) + (weightKg * 0.3894f)) - (ageYears * 0.0408f)) - (impedanceOhm * 0.01235f) - 15.7665f
            } else {
                (((heightCm * 0.3186f) + (weightKg * 0.1934f)) - (ageYears * 0.0206f)) - (impedanceOhm * 0.0132f) - 16.4556f
            }
            f5.coerceIn(20.0f, 70.0f)
        }
        val slmScale2 = roundToTwoDecimals(slmRaw)
        val muscleMassKg = roundToOneDecimal(slmRaw)
        val rawMusclePercent = (slmRaw / weightKg) * 100.0f
        val muscleRate = roundToOneDecimal(rawMusclePercent)
        val muscleStatus = MetricStatus.HEALTHY

        // 4. Skeletal Muscle Mass (CsAlgoBuilder.getGuMuscle) and Skeletal Muscle %
        val rawGuMuscle = ((((heightCm * 0.2573f) + (weightKg * 0.1745f)) - (ageYears * 0.0161f)) + (sexByte * 2.4269f)) - (impedanceOhm * 0.017f) - 20.2165f
        val clampedGuMuscle = rawGuMuscle.coerceIn(20.0f, 70.0f)
        val skeletalMuscleKg = roundToOneDecimal(clampedGuMuscle)
        val skeletalMuscleRate = roundToOneDecimal((clampedGuMuscle / weightKg) * 100.0f)

        val skeletalMuscleStatus = if (isMale) {
            when {
                skeletalMuscleRate < 26.0f -> MetricStatus.LOW
                skeletalMuscleRate < 31.0f -> MetricStatus.HEALTHY
                skeletalMuscleRate < 39.0f -> MetricStatus.HEALTHY
                else -> MetricStatus.PERFECT
            }
        } else {
            when {
                skeletalMuscleRate < 21.0f -> MetricStatus.LOW
                skeletalMuscleRate < 25.0f -> MetricStatus.HEALTHY
                skeletalMuscleRate < 30.0f -> MetricStatus.HEALTHY
                else -> MetricStatus.PERFECT
            }
        }

        // 5. Body Water % (CsAlgoBuilder.getTFR) and Water weight
        val rawWaterPercent = if (ageYears > 17) {
            val f3 = if (isMale) {
                ((heightCm * 0.0939f + weightKg * 0.3758f) - (ageYears * 0.0032f)) - (impedanceOhm * 0.006925f)
            } else {
                ((heightCm * 0.0877f + weightKg * 0.2973f) + (ageYears * 0.0128f)) - (impedanceOhm * 0.00603f)
            }
            val f4 = if (isMale) 0.097f else 0.5175f
            val p = ((f3 + f4) / weightKg) * 100.0f
            p.coerceIn(20.0f, 85.0f)
        } else 0f
        val waterRate = roundToOneDecimal(rawWaterPercent)
        val waterMassKg = roundToOneDecimal((rawWaterPercent * weightKg) / 100.0f)
        val waterStatus = MetricStatus.HEALTHY

        // 6. Visceral Fat (CsAlgoBuilder.getVFR)
        val visceralFatVal = if (ageYears > 17) {
            val f3 = if (isMale) {
                (heightCm * -0.2675f) + (weightKg * 0.42f) + (ageYears * 0.1462f) + (impedanceOhm * 0.0123f)
            } else {
                (heightCm * -0.1651f) + (weightKg * 0.2628f) + (ageYears * 0.0649f) + (impedanceOhm * 0.0024f)
            }
            val f4 = if (isMale) 13.9871f else 12.3445f
            val f5 = f3 + f4
            var f6 = f5.toInt().toFloat()
            if (f5 - f6 >= 0.5f) {
                f6 += 0.5f
            }
            f6.coerceIn(1.0f, 59.0f)
        } else 0f
        val visceralFat = roundToOneDecimal(visceralFatVal)
        val visceralFatStatus = when {
            visceralFat < 1.0f -> MetricStatus.LOW
            visceralFat <= 9.0f -> MetricStatus.HEALTHY
            visceralFat <= 14.0f -> MetricStatus.HIGH
            else -> MetricStatus.ALERT
        }

        // 7. Bone Mass (CsAlgoBuilder.getMSW)
        val rawBoneMass = (weightKg - calculatedFatMass) - slmScale2
        val boneMassKg = roundToOneDecimal(rawBoneMass.coerceIn(1.0f, 4.0f))
        val boneMassStatus = MetricStatus.PERFECT

        // 8. Metabolism BMR (CsAlgoBuilder.getBMR)
        val rawBmr = if (isMale) {
            (((heightCm * 7.5037f) + (weightKg * 13.1523f)) - (ageYears * 4.3376f)) - (impedanceOhm * 0.3486f) - 311.7751f
        } else {
            (((heightCm * 7.5432f) + (weightKg * 9.9474f)) - (ageYears * 3.4382f)) - (impedanceOhm * 0.309f) - 288.2821f
        }
        val bmrKcal = roundToOneDecimal(rawBmr)
        val bmrStatus = MetricStatus.HIGH

        // 9. Protein % (CsAlgoBuilder.getProtein: getSLMPercent - getTFR)
        val proteinRate = roundToOneDecimal((rawMusclePercent - rawWaterPercent).coerceAtLeast(0f))
        val proteinStatus = MetricStatus.HEALTHY

        // 10. Obesity Degree % (CsAlgoBuilder.calOD)
        val standardWeight = if (isMale) (heightCm - 80f) * 0.7f else (heightCm - 70f) * 0.6f
        val rawObesityDegree = if (standardWeight > 0f) {
            ((weightKg - standardWeight) / standardWeight) * 100.0f
        } else 0f
        val obesityDegree = roundToOneDecimal(rawObesityDegree)
        val obesityDegreeStatus = when {
            obesityDegree < -10f -> MetricStatus.LOW
            obesityDegree <= 10f -> MetricStatus.HEALTHY
            obesityDegree <= 20f -> MetricStatus.OVERWEIGHT
            else -> MetricStatus.ALERT
        }

        // 11. Biological Body Age (CsAlgoBuilder.getBodyAge)
        val bodyAge = if (ageYears > 17) {
            val f2 = if (isMale) {
                (heightCm * -0.7471f) + (weightKg * 0.9161f) + (ageYears * 0.4184f) + (impedanceOhm * 0.0517f) + 54.2267f
            } else {
                (heightCm * -1.1165f) + (weightKg * 1.5784f) + (ageYears * 0.4615f) + (impedanceOhm * 0.0415f) + 83.2548f
            }
            var i2 = f2.toInt()
            if (i2 - ageYears > 10) {
                i2 = ageYears + 10
            } else if (ageYears - i2 > 10) {
                i2 = ageYears - 10
            }
            i2.coerceIn(18, 80)
        } else {
            ageYears
        }

        return BodyComposition(
            bmi = bmi,
            bmiCategory = bmiCategory,
            bmiStatus = bmiStatus,
            bodyFatRate = bodyFatRate,
            bodyFatStatus = bodyFatStatus,
            fatMassKg = fatMassKg,
            fatMassStatus = fatMassStatus,
            skeletalMuscleRate = skeletalMuscleRate,
            skeletalMuscleKg = skeletalMuscleKg,
            skeletalMuscleStatus = skeletalMuscleStatus,
            muscleRate = muscleRate,
            muscleMassKg = muscleMassKg,
            muscleStatus = muscleStatus,
            waterRate = waterRate,
            waterMassKg = waterMassKg,
            waterStatus = waterStatus,
            visceralFat = visceralFat,
            visceralFatStatus = visceralFatStatus,
            boneMassKg = boneMassKg,
            boneMassStatus = boneMassStatus,
            bmrKcal = bmrKcal,
            bmrStatus = bmrStatus,
            proteinRate = proteinRate,
            proteinStatus = proteinStatus,
            obesityDegree = obesityDegree,
            obesityDegreeStatus = obesityDegreeStatus,
            bodyAge = bodyAge,
            leanMassKg = leanMassKg
        )
    }

    private fun roundToOneDecimal(value: Float): Float {
        return BigDecimal(value.toDouble()).setScale(1, RoundingMode.HALF_UP).toFloat()
    }

    private fun roundToTwoDecimals(value: Float): Float {
        return BigDecimal(value.toDouble()).setScale(2, RoundingMode.HALF_UP).toFloat()
    }
}
