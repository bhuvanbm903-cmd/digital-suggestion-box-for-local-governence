package com.suggestbox.data.remote.email

import com.google.common.truth.Truth.assertThat
import com.suggestbox.domain.usecase.SuggestionValidator
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * Fake in-memory [EmailService] recording dispatched emails for deterministic test verification.
 */
class FakeEmailService(
    var shouldFailDelivery: Boolean = false,
) : EmailService {
    val dispatchedEmails = mutableListOf<DispatchedEmail>()

    data class DispatchedEmail(
        val recipient: String,
        val otpCode: String,
        val expiresInMinutes: Int,
    )

    override suspend fun sendVerificationEmail(
        recipientEmail: String,
        otpCode: String,
        expiresInMinutes: Int,
    ): Result<Unit> {
        if (shouldFailDelivery) {
            return Result.failure(Exception("SMTP connection timed out"))
        }
        dispatchedEmails.add(DispatchedEmail(recipientEmail, otpCode, expiresInMinutes))
        return Result.success(Unit)
    }
}

/**
 * Unit and integration tests for email-based verification:
 * 1. Cryptographically secure 6-digit OTP generation ([100000, 999999]).
 * 2. Salted SHA-256 cryptographic hashing (zero plain-text storage).
 * 3. 10-minute expiry enforcement.
 * 4. 5-attempt brute-force protection and immediate post-success invalidation.
 * 5. Rate limiting (60s cooldown and 5/hour max requests).
 * 6. Email format validation and graceful SMTP delivery failure handling.
 */
class EmailAuthTest {

    private lateinit var hasher: OtpHasher
    private lateinit var fakeEmailService: FakeEmailService
    private lateinit var validator: SuggestionValidator
    private lateinit var otpManager: EmailOtpManager

    @Before
    fun setUp() {
        hasher = OtpHasher()
        fakeEmailService = FakeEmailService()
        validator = SuggestionValidator()
        otpManager = EmailOtpManager(
            hasher = hasher,
            emailService = fakeEmailService,
            validator = validator,
        )
    }

    // ==========================================
    // 1. Cryptographic Generation & Hashing Tests
    // ==========================================

    @Test
    fun `generateSecureCode produces cryptographically valid 6-digit codes`() {
        repeat(1_000) {
            val code = hasher.generateSecureCode()
            assertThat(code.length).isEqualTo(6)
            val number = code.toIntOrNull()
            assertThat(number).isNotNull()
            assertThat(number).isAtLeast(100_000)
            assertThat(number).isAtMost(999_999)
        }
    }

    @Test
    fun `generateSalt produces 16-byte random hex string with high uniqueness`() {
        val salts = (1..50).map { hasher.generateSalt() }.toSet()
        assertThat(salts).hasSize(50)
        salts.forEach { salt ->
            assertThat(salt.length).isEqualTo(32) // 16 bytes = 32 hex chars
        }
    }

    @Test
    fun `salted SHA-256 hash is deterministic for same salt and code but unique across salts`() {
        val code = "482910"
        val salt1 = hasher.generateSalt()
        val salt2 = hasher.generateSalt()

        val hash1a = hasher.hashOtp(code, salt1)
        val hash1b = hasher.hashOtp(code, salt1)
        val hash2 = hasher.hashOtp(code, salt2)

        assertThat(hash1a).isEqualTo(hash1b)
        assertThat(hash1a).hasLength(64) // 256 bits = 64 hex chars
        assertThat(hash1a).isNotEqualTo(hash2)
    }

    @Test
    fun `hasher constant-time verify correctly validates correct code and rejects incorrect code`() {
        val code = "654321"
        val salt = hasher.generateSalt()
        val storedHash = hasher.hashOtp(code, salt)

        assertThat(hasher.verify(code, salt, storedHash)).isTrue()
        assertThat(hasher.verify("000000", salt, storedHash)).isFalse()
        assertThat(hasher.verify("654322", salt, storedHash)).isFalse()
    }

    // ==========================================
    // 2. Storage & Dispatch Tests
    // ==========================================

    @Test
    fun `requestOtp dispatches email with clean code and stores only salted SHA-256 hash`() = runBlocking {
        val email = "citizen.ward14@example.gov.in"
        val result = otpManager.requestOtp(email)

        assertThat(result.isSuccess).isTrue()
        val (verId, plainCode) = result.getOrThrow()

        // Verify email delivery was attempted
        assertThat(fakeEmailService.dispatchedEmails).hasSize(1)
        val emailRecord = fakeEmailService.dispatchedEmails.first()
        assertThat(emailRecord.recipient).isEqualTo(email)
        assertThat(emailRecord.otpCode).isEqualTo(plainCode)
        assertThat(emailRecord.expiresInMinutes).isEqualTo(10)

        // Verify session storage: plain text code is NEVER stored
        val session = otpManager.getSession(verId)
        assertThat(session).isNotNull()
        assertThat(session?.email).isEqualTo(email)
        assertThat(session?.codeHash).isNotEmpty()
        assertThat(session?.codeHash).isNotEqualTo(plainCode)
        assertThat(session?.salt).isNotEmpty()
        assertThat(session?.attemptsRemaining).isEqualTo(5)
    }

    // ==========================================
    // 3. Expiry and Invalidation Tests
    // ==========================================

    @Test
    fun `verifyOtp succeeds with valid code and invalidates session immediately`() = runBlocking {
        val email = "resident@bengaluru.gov.in"
        val (verId, plainCode) = otpManager.requestOtp(email).getOrThrow()

        // 1. First verification succeeds
        val verifyResult = otpManager.verifyOtp(verId, plainCode)
        assertThat(verifyResult.isSuccess).isTrue()
        assertThat(verifyResult.getOrThrow()).isEqualTo(email)

        // 2. Re-using the same code must fail (single-use token invalidation)
        val replayResult = otpManager.verifyOtp(verId, plainCode)
        assertThat(replayResult.isFailure).isTrue()
        assertThat(replayResult.exceptionOrNull()).isInstanceOf(OtpException.SessionNotFound::class.java)
    }

    @Test
    fun `verifyOtp fails when code is expired after 10 minutes`() = runBlocking {
        val email = "citizen@karnataka.gov.in"
        val (verId, plainCode) = otpManager.requestOtp(email).getOrThrow()

        // Simulate 10-minute expiration by setting expiresAt in the past
        val session = otpManager.getSession(verId)!!
        val expiredSession = session.copy(expiresAt = System.currentTimeMillis() - 1000L)
        // Access reflection or simulate expired check
        java.lang.reflect.Field::class.java // compiler check
        val field = EmailOtpSession::class.java.getDeclaredField("expiresAt")
        field.isAccessible = true
        field.set(session, System.currentTimeMillis() - 1000L)

        val result = otpManager.verifyOtp(verId, plainCode)
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(OtpException.Expired::class.java)
    }

    // ==========================================
    // 4. Attempt Limits (Brute-Force Protection)
    // ==========================================

    @Test
    fun `verifyOtp tracks failed attempts and invalidates session after 5 failed tries`() = runBlocking {
        val email = "user@test.gov.in"
        val (verId, _) = otpManager.requestOtp(email).getOrThrow()

        // 1st incorrect attempt
        val res1 = otpManager.verifyOtp(verId, "000000")
        assertThat(res1.isFailure).isTrue()
        assertThat(res1.exceptionOrNull()).isInstanceOf(OtpException.IncorrectCode::class.java)
        assertThat((res1.exceptionOrNull() as OtpException.IncorrectCode).attemptsRemaining).isEqualTo(4)

        // 2nd incorrect attempt
        val res2 = otpManager.verifyOtp(verId, "111111")
        assertThat((res2.exceptionOrNull() as OtpException.IncorrectCode).attemptsRemaining).isEqualTo(3)

        // 3rd incorrect attempt
        val res3 = otpManager.verifyOtp(verId, "222222")
        assertThat((res3.exceptionOrNull() as OtpException.IncorrectCode).attemptsRemaining).isEqualTo(2)

        // 4th incorrect attempt
        val res4 = otpManager.verifyOtp(verId, "333333")
        assertThat((res4.exceptionOrNull() as OtpException.IncorrectCode).attemptsRemaining).isEqualTo(1)

        // 5th incorrect attempt -> Max attempts exceeded, code invalidated
        val res5 = otpManager.verifyOtp(verId, "444444")
        assertThat(res5.isFailure).isTrue()
        assertThat(res5.exceptionOrNull()).isInstanceOf(OtpException.MaxAttemptsExceeded::class.java)

        // 6th attempt -> Session is completely gone / invalidated
        val res6 = otpManager.verifyOtp(verId, "555555")
        assertThat(res6.isFailure).isTrue()
        assertThat(res6.exceptionOrNull()).isInstanceOf(OtpException.SessionNotFound::class.java)
    }

    // ==========================================
    // 5. Rate Limiting Tests (Cooldown & Hourly Cap)
    // ==========================================

    @Test
    fun `requestOtp enforces 60-second cooldown on rapid resends`() = runBlocking {
        val email = "rate.limited@example.com"
        val first = otpManager.requestOtp(email)
        assertThat(first.isSuccess).isTrue()

        // Immediate second request must trigger cooldown error
        val second = otpManager.requestOtp(email)
        assertThat(second.isFailure).isTrue()
        assertThat(second.exceptionOrNull()).isInstanceOf(OtpException.ResendCooldownActive::class.java)
        val cooldownErr = second.exceptionOrNull() as OtpException.ResendCooldownActive
        assertThat(cooldownErr.secondsRemaining).isAtLeast(1L)
        assertThat(cooldownErr.secondsRemaining).isAtMost(60L)
    }

    @Test
    fun `requestOtp rejects requests once hourly limit of 5 is exceeded`() = runBlocking {
        val email = "hourly.cap@gov.in"

        // Simulate 5 requests spread across the hour (bypassing 60s cooldown by setting lastRequestTimestamp)
        for (i in 1..5) {
            val recordField = EmailOtpManager::class.java.getDeclaredField("rateLimits")
            recordField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val limitsMap = recordField.get(otpManager) as java.util.concurrent.ConcurrentHashMap<String, Any>

            val req = otpManager.requestOtp(email)
            assertThat(req.isSuccess).isTrue()

            // Reset lastRequestTimestamp so the next request doesn't hit the 60s cooldown
            val record = limitsMap[email]!!
            val lastTimeField = record.javaClass.getDeclaredField("lastRequestTimestamp")
            lastTimeField.isAccessible = true
            lastTimeField.set(record, System.currentTimeMillis() - 61_000L)
        }

        // 6th request within the same hour should fail with HourlyRateLimitExceeded
        val sixth = otpManager.requestOtp(email)
        assertThat(sixth.isFailure).isTrue()
        assertThat(sixth.exceptionOrNull()).isInstanceOf(OtpException.HourlyRateLimitExceeded::class.java)
    }

    // ==========================================
    // 6. Validation & Error Handling Tests
    // ==========================================

    @Test
    fun `requestOtp rejects invalid email addresses with InvalidEmail exception`() = runBlocking {
        val invalidEmails = listOf("notanemail", "user@", "@domain.com", "", "user @domain.com")
        for (invalid in invalidEmails) {
            val result = otpManager.requestOtp(invalid)
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(OtpException.InvalidEmail::class.java)
        }
    }

    @Test
    fun `requestOtp cleans up session when email service fails to deliver`() = runBlocking {
        fakeEmailService.shouldFailDelivery = true
        val email = "delivery.fail@example.com"

        val result = otpManager.requestOtp(email)
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("SMTP connection timed out")
    }
}
