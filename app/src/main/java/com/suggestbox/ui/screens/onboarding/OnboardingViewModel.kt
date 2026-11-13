package com.suggestbox.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suggestbox.domain.model.LanguageOption
import com.suggestbox.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    fun selectLanguage(language: LanguageOption) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(language)
        }
    }

    fun grantDpdpConsentAndFinish() {
        viewModelScope.launch {
            preferencesRepository.setDpdpConsent(true)
            preferencesRepository.setOnboardingCompleted(true)
        }
    }
}
