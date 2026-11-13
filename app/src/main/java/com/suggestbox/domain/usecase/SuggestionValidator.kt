package com.suggestbox.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of validating a citizen suggestion or comment against quality, spam,
 * profanity, and rate-limiting rules.
 */
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val errorReason: ValidationError) : ValidationResult
}

enum class ValidationError {
    TITLE_TOO_SHORT,
    DESCRIPTION_TOO_SHORT,
    PROFANITY_OR_SPAM_DETECTED,
    RATE_LIMIT_EXCEEDED,
    INVALID_EMAIL,
    INVALID_PHONE,
    INVALID_OTP,
    CODE_EXPIRED,
    MAX_ATTEMPTS_EXCEEDED,
}

/**
 * Enforces input validation, profanity/spam screening, and submission rate limiting
 * to keep local governance queues clean and trustworthy.
 */
@Singleton
class SuggestionValidator @Inject constructor() {

    private val submissionTimestamps = mutableListOf<Long>()

    private val blockedTerms = setOf(
        "spam", "scam", "buy now", "click here", "free money",
        "stupid", "idiot", "fool", "abuse",
    )

    fun validateSuggestion(
        title: String,
        description: String,
        checkRateLimit: Boolean = true,
    ): ValidationResult {
        val cleanTitle = title.trim()
        val cleanDesc = description.trim()

        if (cleanTitle.length < 8) {
            return ValidationResult.Invalid(ValidationError.TITLE_TOO_SHORT)
        }
        if (cleanDesc.length < 20) {
            return ValidationResult.Invalid(ValidationError.DESCRIPTION_TOO_SHORT)
        }

        val combined = "$cleanTitle $cleanDesc".lowercase()
        if (blockedTerms.any { term -> combined.contains(term) }) {
            return ValidationResult.Invalid(ValidationError.PROFANITY_OR_SPAM_DETECTED)
        }

        if (checkRateLimit && isRateLimited()) {
            return ValidationResult.Invalid(ValidationError.RATE_LIMIT_EXCEEDED)
        }

        return ValidationResult.Valid
    }

    fun recordSuccessfulSubmission(nowMillis: Long = System.currentTimeMillis()) {
        pruneOldTimestamps(nowMillis)
        submissionTimestamps.add(nowMillis)
    }

    fun isRateLimited(nowMillis: Long = System.currentTimeMillis()): Boolean {
        pruneOldTimestamps(nowMillis)
        return submissionTimestamps.size >= MAX_SUBMISSIONS_PER_WINDOW
    }

    fun validateEmail(email: String): ValidationResult {
        val trimmed = email.trim()
        if (trimmed.length < 5 || !EMAIL_REGEX.matches(trimmed)) {
            return ValidationResult.Invalid(ValidationError.INVALID_EMAIL)
        }
        return ValidationResult.Valid
    }

    fun validatePhone(phone: String): ValidationResult {
        val digits = phone.filter { it.isDigit() }
        return if (digits.length in 10..13) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(ValidationError.INVALID_PHONE)
        }
    }

    fun validateOtp(otp: String): ValidationResult {
        val digits = otp.filter { it.isDigit() }
        return if (digits.length == 6) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(ValidationError.INVALID_OTP)
        }
    }

    private fun pruneOldTimestamps(nowMillis: Long) {
        val cutoff = nowMillis - RATE_WINDOW_MS
        submissionTimestamps.removeAll { it < cutoff }
    }

    private companion object {
        val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        const val RATE_WINDOW_MS = 60 * 60 * 1000L // 1 hour window
        const val MAX_SUBMISSIONS_PER_WINDOW = 5
    }
}
