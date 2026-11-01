package com.openscaleapp

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.openscaleapp.data.AppSettingsRepository
import com.openscaleapp.ui.theme.AppLanguage
import com.openscaleapp.ui.theme.AppTheme
import com.openscaleapp.ui.theme.OpenScaleAppTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val settingsRepository = AppSettingsRepository(applicationContext)

        enableEdgeToEdge()
        setContent {
            val appTheme by settingsRepository.appTheme.collectAsState()
            val appLanguage by settingsRepository.appLanguage.collectAsState()

            val isDark = when (appTheme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }

            val targetLocale = when (appLanguage) {
                AppLanguage.SYSTEM -> null
                AppLanguage.EN -> Locale.ENGLISH
                AppLanguage.RU -> Locale.forLanguageTag("ru")
            }

            val context = LocalContext.current
            val localizedContext = remember(context, targetLocale) {
                if (targetLocale == null) {
                    context
                } else {
                    val config = Configuration(context.resources.configuration)
                    config.setLocale(targetLocale)
                    context.createConfigurationContext(config)
                }
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration
            ) {
                OpenScaleAppTheme(darkTheme = isDark) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainNavigation()
                    }
                }
            }
        }
    }
}
