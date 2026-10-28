package com.example.chipseascaleapp.model

enum class Gender {
    MALE,
    FEMALE
}

data class UserProfile(
    val gender: Gender = Gender.MALE,
    val heightCm: Float = 175f,
    val ageYears: Int = 28
)
