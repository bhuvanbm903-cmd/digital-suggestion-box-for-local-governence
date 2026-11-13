package com.suggestbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suggestbox.core.designsystem.theme.SuggestBoxTheme
import com.suggestbox.ui.AppStateViewModel
import com.suggestbox.ui.ProvideAppLocale
import com.suggestbox.ui.navigation.SuggestBoxNavHost
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host for the SuggestBox application.
 * Configures edge-to-edge rendering, dynamic light/dark system bars, locale wrapping, and Compose navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appStateViewModel: AppStateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val prefs by appStateViewModel.preferences.collectAsStateWithLifecycle()

            DisposableEffect(prefs.darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (prefs.darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        )
                    },
                    navigationBarStyle = if (prefs.darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        )
                    },
                )
                onDispose {}
            }

            ProvideAppLocale(languageCode = prefs.language.code) {
                SuggestBoxTheme(darkTheme = prefs.darkTheme) {
                    SuggestBoxNavHost(
                        preferences = prefs,
                        onToggleDarkTheme = appStateViewModel::setDarkTheme,
                        onSelectLanguage = appStateViewModel::setLanguage,
                        onSelectRole = appStateViewModel::setPreviewRole,
                    )
                }
            }
        }
    }
}
