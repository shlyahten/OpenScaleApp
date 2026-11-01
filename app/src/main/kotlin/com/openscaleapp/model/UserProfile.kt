package com.openscaleapp.model

enum class Gender {
    MALE,
    FEMALE
}

data class UserProfile(
    val gender: Gender = Gender.MALE,
    val heightCm: Float = 178f,
    val ageYears: Int = 31
)
