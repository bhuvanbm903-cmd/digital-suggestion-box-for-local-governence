package com.suggestbox.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suggestbox.domain.model.AppPreferences
import com.suggestbox.domain.model.LanguageOption
import com.suggestbox.domain.model.UserRole
import com.suggestbox.domain.repository.AuthRepository
import com.suggestbox.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Root ViewModel managing global theme, locale, consent state, and workspace role preview.
 */
@HiltViewModel
class AppStateViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val preferences: StateFlow<AppPreferences> = preferencesRepository.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = preferencesRepository.currentPreferences,
        )

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDarkTheme(enabled)
        }
    }

    fun setLanguage(language: LanguageOption) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(language)
        }
    }

    fun setPreviewRole(role: UserRole) {
        viewModelScope.launch {
            preferencesRepository.setPreviewRole(role)
            authRepository.switchPreviewRole(role)
        }
    }
}
