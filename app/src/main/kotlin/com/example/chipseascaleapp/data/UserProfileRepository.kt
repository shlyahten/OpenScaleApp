package com.example.chipseascaleapp.data

import android.content.Context
import com.example.chipseascaleapp.model.Gender
import com.example.chipseascaleapp.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserProfileRepository(context: Context) : ProfileRepository {
    private val prefs = context.getSharedPreferences("chipsea_user_profile", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadProfile())
    override val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private fun loadProfile(): UserProfile {
        val genderStr = prefs.getString(KEY_GENDER, Gender.MALE.name) ?: Gender.MALE.name
        val gender = try {
            Gender.valueOf(genderStr)
        } catch (_: Exception) {
            Gender.MALE
        }
        val height = prefs.getFloat(KEY_HEIGHT, 175f)
        val age = prefs.getInt(KEY_AGE, 28)
        return UserProfile(gender = gender, heightCm = height, ageYears = age)
    }

    override fun saveProfile(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_GENDER, profile.gender.name)
            .putFloat(KEY_HEIGHT, profile.heightCm)
            .putInt(KEY_AGE, profile.ageYears)
            .apply()
        _userProfile.value = profile
    }

    companion object {
        private const val KEY_GENDER = "gender"
        private const val KEY_HEIGHT = "height_cm"
        private const val KEY_AGE = "age_years"
    }
}
