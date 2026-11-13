package com.suggestbox.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suggestbox.domain.model.UserProfile
import com.suggestbox.domain.repository.AuthRepository
import com.suggestbox.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val currentUser: StateFlow<UserProfile?> = authRepository.currentUser
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setNotificationsEnabled(enabled)
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onSignedOut()
        }
    }

    fun deleteAccountAndData(onDeleted: () -> Unit) {
        viewModelScope.launch {
            authRepository.deleteAccountAndData()
            preferencesRepository.setDpdpConsent(false)
            onDeleted()
        }
    }

    fun dismissEmailMigration() {
        viewModelScope.launch {
            preferencesRepository.dismissEmailMigrationPrompt()
        }
    }

    fun linkVerifiedEmail(email: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            preferencesRepository.linkVerifiedEmail(email)
            onDone()
        }
    }
}
