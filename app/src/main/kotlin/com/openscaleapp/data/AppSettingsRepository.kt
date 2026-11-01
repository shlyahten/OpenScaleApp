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
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("openscale_settings", Context.MODE_PRIVATE)

    init {
        initialize(appContext)
    }

    override val appTheme: StateFlow<AppTheme> = _sharedAppTheme.asStateFlow()
    override val appLanguage: StateFlow<AppLanguage> = _sharedAppLanguage.asStateFlow()

    override fun setAppTheme(theme: AppTheme) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
        _sharedAppTheme.value = theme
    }

    override fun setAppLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.name).apply()
        _sharedAppLanguage.value = language
    }

    companion object {
        private const val KEY_THEME = "app_theme"
        private const val KEY_LANGUAGE = "app_language"

        private val _sharedAppTheme = MutableStateFlow(AppTheme.SYSTEM)
        private val _sharedAppLanguage = MutableStateFlow(AppLanguage.SYSTEM)
        private var isInitialized = false

        @Synchronized
        fun initialize(context: Context) {
            if (!isInitialized) {
                val p = context.getSharedPreferences("openscale_settings", Context.MODE_PRIVATE)
                val themeName = p.getString(KEY_THEME, AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
                _sharedAppTheme.value = try {
                    AppTheme.valueOf(themeName)
                } catch (_: Exception) {
                    AppTheme.SYSTEM
                }
                val langName = p.getString(KEY_LANGUAGE, AppLanguage.SYSTEM.name) ?: AppLanguage.SYSTEM.name
                _sharedAppLanguage.value = try {
                    AppLanguage.valueOf(langName)
                } catch (_: Exception) {
                    AppLanguage.SYSTEM
                }
                isInitialized = true
            }
        }
    }
}
