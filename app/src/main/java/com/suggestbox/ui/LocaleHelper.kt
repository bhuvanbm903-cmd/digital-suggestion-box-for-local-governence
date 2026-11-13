package com.suggestbox.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Wraps the composable hierarchy with localized [Resources] and [Configuration] while preserving
 * the underlying [android.app.Activity] in the [ContextWrapper] chain so `hiltViewModel()`
 * and ActivityResult launchers continue to resolve the host Activity.
 */
@Composable
fun ProvideAppLocale(
    languageCode: String,
    content: @Composable () -> Unit,
) {
    val baseContext = LocalContext.current
    val wrappedContext = remember(baseContext, languageCode) {
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(baseContext.resources.configuration).apply {
            setLocale(locale)
        }
        val localizedContext = baseContext.createConfigurationContext(config)
        LocalizedContextWrapper(
            base = baseContext,
            localizedResources = localizedContext.resources,
        )
    }

    CompositionLocalProvider(
        LocalContext provides wrappedContext,
        LocalConfiguration provides wrappedContext.resources.configuration,
    ) {
        content()
    }
}

private class LocalizedContextWrapper(
    base: Context,
    private val localizedResources: Resources,
) : ContextWrapper(base) {
    override fun getResources(): Resources = localizedResources
}
