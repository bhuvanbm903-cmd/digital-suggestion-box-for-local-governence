package com.suggestbox

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point enabling Hilt dependency injection across SuggestBox.
 */
@HiltAndroidApp
class SuggestBoxApplication : Application()
