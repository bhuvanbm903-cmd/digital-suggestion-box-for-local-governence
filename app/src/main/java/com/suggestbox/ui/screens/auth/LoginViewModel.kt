package com.suggestbox.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suggestbox.data.remote.email.OtpException
import com.suggestbox.domain.repository.AuthRepository
import com.suggestbox.domain.repository.OtpDeliveryEvent
import com.suggestbox.domain.repository.PreferencesRepository
import com.suggestbox.domain.usecase.SuggestionValidator
import com.suggestbox.domain.usecase.ValidationError
import com.suggestbox.domain.usecase.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val otpCode: String = "",
    val verificationId: String? = null,
    val isLoading: Boolean = false,
    val error: ValidationError? = null,
    val serverError: String? = null,
    val authenticated: Boolean = false,
    val deliveredOtp: OtpDeliveryEvent? = null,
    val resendCountdown: Int = 0,
    val attemptsRemaining: Int = 5,
    val expiryMinutes: Int = 10,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val validator: SuggestionValidator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        // Collect real-time dispatched OTP events for email delivery banner and 1-tap autofill
        viewModelScope.launch {
            authRepository.latestDispatchedOtp.collect { event ->
                if (event != null && event.verificationId == _uiState.value.verificationId) {
                    _uiState.value = _uiState.value.copy(deliveredOtp = event)
                }
            }
        }
    }

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(
            email = value.trim(),
            error = null,
            serverError = null,
        )
    }

    fun onOtpChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(6)
        _uiState.value = _uiState.value.copy(otpCode = digits, error = null, serverError = null)
        if (digits.length == 6) {
            verifyOtp()
        }
    }

    fun requestOtp() {
        val state = _uiState.value
        val validation = validator.validateEmail(state.email)
        if (validation is ValidationResult.Invalid) {
            _uiState.value = state.copy(error = validation.errorReason)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                serverError = null,
                deliveredOtp = null,
            )
            authRepository.requestOtp(state.email.trim())
                .onSuccess { verId ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        verificationId = verId,
                        otpCode = "",
                        attemptsRemaining = 5,
                        error = null,
                        serverError = null,
                    )
                    startResendCountdown(60)
                }
                .onFailure { err ->
                    val errorReason = when (err) {
                        is OtpException.InvalidEmail -> ValidationError.INVALID_EMAIL
                        else -> null
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorReason,
                        serverError = err.message ?: "Failed to send verification email.",
                    )
                }
        }
    }

    fun resendOtp() {
        if (_uiState.value.resendCountdown > 0) return
        val email = _uiState.value.email.trim()
        val validation = validator.validateEmail(email)
        if (validation is ValidationResult.Invalid) {
            _uiState.value = _uiState.value.copy(error = validation.errorReason)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                serverError = null,
                deliveredOtp = null,
            )
            authRepository.resendOtp(email)
                .onSuccess { verId ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        verificationId = verId,
                        otpCode = "",
                        attemptsRemaining = 5,
                    )
                    startResendCountdown(60)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        serverError = err.message ?: "Failed to resend verification code.",
                    )
                }
        }
    }

    fun autoFillAndVerify(code: String) {
        _uiState.value = _uiState.value.copy(otpCode = code, error = null)
        verifyOtp()
    }

    fun changeEmail() {
        countdownJob?.cancel()
        _uiState.value = _uiState.value.copy(
            verificationId = null,
            otpCode = "",
            deliveredOtp = null,
            resendCountdown = 0,
            serverError = null,
            error = null,
            attemptsRemaining = 5,
        )
    }

    fun verifyOtp() {
        val state = _uiState.value
        val verId = state.verificationId ?: return
        val validation = validator.validateOtp(state.otpCode)
        if (validation is ValidationResult.Invalid) {
            _uiState.value = state.copy(error = validation.errorReason)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, serverError = null)
            authRepository.verifyOtp(verId, state.otpCode)
                .onSuccess { user ->
                    preferencesRepository.saveUserSession(user)
                    _uiState.value = _uiState.value.copy(isLoading = false, authenticated = true)
                }
                .onFailure { err ->
                    val errorReason = when (err) {
                        is OtpException.Expired -> ValidationError.CODE_EXPIRED
                        is OtpException.MaxAttemptsExceeded -> ValidationError.MAX_ATTEMPTS_EXCEEDED
                        else -> ValidationError.INVALID_OTP
                    }
                    val attemptsRemaining = when (err) {
                        is OtpException.IncorrectCode -> err.attemptsRemaining
                        is OtpException.MaxAttemptsExceeded -> 0
                        else -> state.attemptsRemaining
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorReason,
                        attemptsRemaining = attemptsRemaining,
                        serverError = err.message ?: "Verification failed. Please try again.",
                    )
                }
        }
    }

    fun continueAnonymously() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, serverError = null)
            val wardId = preferencesRepository.preferences.first().selectedWardId
            authRepository.signInAnonymously(wardId)
                .onSuccess { user ->
                    preferencesRepository.saveUserSession(user)
                    _uiState.value = _uiState.value.copy(isLoading = false, authenticated = true)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(isLoading = false, serverError = err.message)
                }
        }
    }

    private fun startResendCountdown(seconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (remaining in seconds downTo 0) {
                _uiState.value = _uiState.value.copy(resendCountdown = remaining)
                delay(1000L)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
