package com.example.chipseascaleapp.data

import com.example.chipseascaleapp.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface ProfileRepository {
    val userProfile: StateFlow<UserProfile>
    fun saveProfile(profile: UserProfile)
}
