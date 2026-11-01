package com.openscaleapp.data

import android.content.Context
import com.openscaleapp.ui.theme.AppLanguage
import com.openscaleapp.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface SettingsRepository {
    val appTheme: StateFlow<AppTheme>
    val appLanguage: StateFlow<AppLanguage>
    fun setAppTheme(theme: AppTheme)
    fun setAppLanguage(language: AppLanguage)
}

class AppSettingsRepository(context: Context) : SettingsRepository {
    private val prefs = context.getSharedPreferences("openscale_settings", Context.MODE_PRIVATE)

    private val _appTheme = MutableStateFlow(loadTheme())
    override val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    private val _appLanguage = MutableStateFlow(loadLanguage())
    override val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private fun loadTheme(): AppTheme {
        val name = prefs.getString(KEY_THEME, AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        return try {
            AppTheme.valueOf(name)
        } catch (_: Exception) {
            AppTheme.SYSTEM
        }
    }

    private fun loadLanguage(): AppLanguage {
        val name = prefs.getString(KEY_LANGUAGE, AppLanguage.SYSTEM.name) ?: AppLanguage.SYSTEM.name
        return try {
            AppLanguage.valueOf(name)
        } catch (_: Exception) {
            AppLanguage.SYSTEM
        }
    }

    override fun setAppTheme(theme: AppTheme) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
        _appTheme.value = theme
    }

    override fun setAppLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.name).apply()
        _appLanguage.value = language
    }

    companion object {
        private const val KEY_THEME = "app_theme"
        private const val KEY_LANGUAGE = "app_language"
    }
}
