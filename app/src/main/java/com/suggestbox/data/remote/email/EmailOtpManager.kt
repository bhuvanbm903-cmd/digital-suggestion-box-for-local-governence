package com.suggestbox.data.remote.email

import com.suggestbox.domain.usecase.SuggestionValidator
import com.suggestbox.domain.usecase.ValidationError
import com.suggestbox.domain.usecase.ValidationResult
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

sealed class OtpException(message: String) : Exception(message) {
    data class InvalidEmail(val email: String) : OtpException("Invalid email format: $email")
    data class ResendCooldownActive(val secondsRemaining: Long) :
        OtpException("Please wait $secondsRemaining seconds before requesting another code.")
    data class HourlyRateLimitExceeded(val maxPerHour: Int = 5) :
        OtpException("Maximum $maxPerHour verification requests per hour reached. Please try again later.")
    data object Expired : OtpException("Verification code has expired. Please request a new code.")
    data object MaxAttemptsExceeded : OtpException("Maximum verification attempts exceeded. Code has been invalidated.")
    data class IncorrectCode(val attemptsRemaining: Int) :
        OtpException("Invalid code. $attemptsRemaining attempt(s) remaining.")
    data object SessionNotFound : OtpException("Verification session not found or already completed. Please request a new code.")
}

data class EmailOtpSession(
    val verificationId: String,
    val email: String,
    val salt: String,
    val codeHash: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = createdAt + EmailOtpManager.EXPIRY_DURATION_MS,
    var attemptsRemaining: Int = EmailOtpManager.MAX_ATTEMPTS,
    var isUsed: Boolean = false,
)

/**
 * Manages email OTP creation, cryptographic verification, 10-minute expiry,
 * 5-attempt brute-force protection, and 60-second / 5-per-hour rate limiting.
 */
@Singleton
class EmailOtpManager @Inject constructor(
    private val hasher: OtpHasher,
    private val emailService: EmailService,
    private val validator: SuggestionValidator,
) {
    companion object {
        const val EXPIRY_DURATION_MS = 10 * 60 * 1000L // 10 minutes
        const val MAX_ATTEMPTS = 5
        const val RESEND_COOLDOWN_MS = 60 * 1000L // 60 seconds
        const val HOURLY_WINDOW_MS = 60 * 60 * 1000L // 1 hour
        const val MAX_HOURLY_REQUESTS = 5
    }

    private data class RateLimitRecord(
        var lastRequestTimestamp: Long = 0L,
        val hourlyRequestTimestamps: MutableList<Long> = mutableListOf(),
    )

    private val sessions = ConcurrentHashMap<String, EmailOtpSession>()
    private val rateLimits = ConcurrentHashMap<String, RateLimitRecord>()

    /**
     * Creates a new OTP session, enforces rate limits, hashes the generated code,
     * dispatches the email, and returns the verificationId and generated code.
     */
    suspend fun requestOtp(email: String): Result<Pair<String, String>> {
        val normalizedEmail = email.trim().lowercase()

        // 1. Format validation
        val validation = validator.validateEmail(normalizedEmail)
        if (validation is ValidationResult.Invalid) {
            return Result.failure(OtpException.InvalidEmail(normalizedEmail))
        }

        // 2. Rate limit check
        val now = System.currentTimeMillis()
        val rateRecord = rateLimits.computeIfAbsent(normalizedEmail) { RateLimitRecord() }

        synchronized(rateRecord) {
            // Check 60s cooldown
            val timeSinceLast = now - rateRecord.lastRequestTimestamp
            if (timeSinceLast < RESEND_COOLDOWN_MS) {
                val secondsRemaining = kotlin.math.ceil((RESEND_COOLDOWN_MS - timeSinceLast) / 1000.0)
                    .toLong()
                    .coerceIn(1L, 60L)
                return Result.failure(OtpException.ResendCooldownActive(secondsRemaining))
            }

            // Prune timestamps older than 1 hour
            rateRecord.hourlyRequestTimestamps.removeAll { it < (now - HOURLY_WINDOW_MS) }

            // Check hourly cap
            if (rateRecord.hourlyRequestTimestamps.size >= MAX_HOURLY_REQUESTS) {
                return Result.failure(OtpException.HourlyRateLimitExceeded(MAX_HOURLY_REQUESTS))
            }

            // Record request
            rateRecord.lastRequestTimestamp = now
            rateRecord.hourlyRequestTimestamps.add(now)
        }

        // 3. Cryptographic code generation
        val plainCode = hasher.generateSecureCode()
        val salt = hasher.generateSalt()
        val codeHash = hasher.hashOtp(plainCode, salt)
        val verificationId = "eml-ver-${UUID.randomUUID().toString().take(12)}"

        val session = EmailOtpSession(
            verificationId = verificationId,
            email = normalizedEmail,
            salt = salt,
            codeHash = codeHash,
            createdAt = now,
            expiresAt = now + EXPIRY_DURATION_MS,
            attemptsRemaining = MAX_ATTEMPTS,
            isUsed = false,
        )
        sessions[verificationId] = session

        // 4. Deliver via SMTP service
        val deliveryResult = emailService.sendVerificationEmail(
            recipientEmail = normalizedEmail,
            otpCode = plainCode,
            expiresInMinutes = (EXPIRY_DURATION_MS / 60_000L).toInt(),
        )

        return if (deliveryResult.isSuccess) {
            Result.success(Pair(verificationId, plainCode))
        } else {
            // Invalidate session if delivery failed outright
            sessions.remove(verificationId)
            Result.failure(deliveryResult.exceptionOrNull() ?: Exception("Failed to deliver email"))
        }
    }

    /**
     * Verifies the user-entered code against the salted SHA-256 hash.
     * Enforces attempt limits, checks expiry, and invalidates code on success or max attempts.
     */
    fun verifyOtp(verificationId: String, enteredCode: String): Result<String> {
        val session = sessions[verificationId]
            ?: return Result.failure(OtpException.SessionNotFound)

        synchronized(session) {
            if (session.isUsed) {
                return Result.failure(OtpException.SessionNotFound)
            }

            val now = System.currentTimeMillis()
            if (now > session.expiresAt) {
                sessions.remove(verificationId)
                return Result.failure(OtpException.Expired)
            }

            if (session.attemptsRemaining <= 0) {
                sessions.remove(verificationId)
                return Result.failure(OtpException.MaxAttemptsExceeded)
            }

            // Check code
            val isValid = hasher.verify(enteredCode.trim(), session.salt, session.codeHash)
            if (isValid) {
                // Invalidate session immediately upon success
                session.isUsed = true
                sessions.remove(verificationId)
                return Result.success(session.email)
            } else {
                session.attemptsRemaining -= 1
                if (session.attemptsRemaining <= 0) {
                    session.isUsed = true
                    sessions.remove(verificationId)
                    return Result.failure(OtpException.MaxAttemptsExceeded)
                } else {
                    return Result.failure(OtpException.IncorrectCode(session.attemptsRemaining))
                }
            }
        }
    }

    fun getSession(verificationId: String): EmailOtpSession? = sessions[verificationId]

    fun clearAllSessions() {
        sessions.clear()
        rateLimits.clear()
    }
}
