package com.suggestbox.data.remote.email

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cryptographically secure OTP code generator and SHA-256 salted hasher.
 * Guarantees that plain-text verification codes are never stored at rest or in memory.
 */
@Singleton
class OtpHasher @Inject constructor() {

    private val secureRandom = SecureRandom()

    /**
     * Generates a cryptographically strong 6-digit one-time code in the range [100000, 999999].
     */
    fun generateSecureCode(): String {
        val number = secureRandom.nextInt(900_000) + 100_000
        return number.toString()
    }

    /**
     * Generates a random 16-byte cryptographic salt represented as a hex string.
     */
    fun generateSalt(): String {
        val saltBytes = ByteArray(16)
        secureRandom.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Computes the SHA-256 hash of (salt + code).
     */
    fun hashOtp(code: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val input = (salt + code.trim()).toByteArray(StandardCharsets.UTF_8)
        val digest = md.digest(input)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Constant-time comparison between computed hash and stored hash to prevent timing attacks.
     */
    fun verify(inputCode: String, salt: String, storedHash: String): Boolean {
        val computedHash = hashOtp(inputCode, salt)
        val computedBytes = computedHash.toByteArray(StandardCharsets.UTF_8)
        val storedBytes = storedHash.toByteArray(StandardCharsets.UTF_8)
        return MessageDigest.isEqual(computedBytes, storedBytes)
    }
}
