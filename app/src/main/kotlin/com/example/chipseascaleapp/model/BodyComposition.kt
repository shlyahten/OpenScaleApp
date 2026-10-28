package com.example.chipseascaleapp.model

enum class BmiCategory(val label: String) {
    UNDERWEIGHT("Дефицит веса"),
    NORMAL("Нормальный вес"),
    OVERWEIGHT("Избыточный вес"),
    OBESE("Ожирение")
}

data class BodyComposition(
    val bmi: Float,
    val bmiCategory: BmiCategory,
    val bodyFatRate: Float?,
    val fatMassKg: Float?,
    val leanMassKg: Float?
)
