package com.openscaleapp.model

import com.openscaleapp.R

enum class BmiCategory(val stringResId: Int, val label: String) {
    UNDERWEIGHT(R.string.bmi_underweight, "Дефицит веса"),
    NORMAL(R.string.bmi_normal, "Нормальный вес"),
    OVERWEIGHT(R.string.bmi_overweight, "Избыточный вес"),
    OBESE(R.string.bmi_obese, "Ожирение")
}

enum class MetricStatus(
    val stringResId: Int,
    val label: String,
    val textColorArgb: Long,
    val bgColorArgb: Long
) {
    LOW(R.string.status_low, "Ниже нормы", 0xFF0277BD, 0xFFE1F5FE),
    HEALTHY(R.string.status_healthy, "В норме", 0xFF2E7D32, 0xFFE8F5E9),
    PERFECT(R.string.status_perfect, "Отлично", 0xFF1B5E20, 0xFFC8E6C9),
    HIGH(R.string.status_high, "Выше нормы", 0xFFE65100, 0xFFFFF3E0),
    OVERWEIGHT(R.string.status_overweight, "Избыток", 0xFFE65100, 0xFFFFF3E0),
    ALERT(R.string.status_alert, "Опасно", 0xFFC62828, 0xFFFFEBEE)
}

data class BodyComposition(
    val bmi: Float,
    val bmiCategory: BmiCategory = BmiCategory.NORMAL,
    val bmiStatus: MetricStatus = MetricStatus.HEALTHY,
    val bodyFatRate: Float? = null,
    val bodyFatStatus: MetricStatus? = null,
    val fatMassKg: Float? = null,
    val fatMassStatus: MetricStatus? = null,
    val skeletalMuscleRate: Float? = null,
    val skeletalMuscleKg: Float? = null,
    val skeletalMuscleStatus: MetricStatus? = null,
    val muscleRate: Float? = null,
    val muscleMassKg: Float? = null,
    val muscleStatus: MetricStatus? = null,
    val waterRate: Float? = null,
    val waterMassKg: Float? = null,
    val waterStatus: MetricStatus? = null,
    val visceralFat: Float? = null,
    val visceralFatStatus: MetricStatus? = null,
    val boneMassKg: Float? = null,
    val boneMassStatus: MetricStatus? = null,
    val bmrKcal: Float? = null,
    val bmrStatus: MetricStatus? = null,
    val proteinRate: Float? = null,
    val proteinStatus: MetricStatus? = null,
    val obesityDegree: Float? = null,
    val obesityDegreeStatus: MetricStatus? = null,
    val bodyAge: Int? = null,
    val leanMassKg: Float? = null
)
