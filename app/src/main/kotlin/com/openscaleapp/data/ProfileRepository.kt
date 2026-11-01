package com.openscaleapp.data

import com.openscaleapp.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface ProfileRepository {
    val userProfile: StateFlow<UserProfile>
    fun saveProfile(profile: UserProfile)
}
