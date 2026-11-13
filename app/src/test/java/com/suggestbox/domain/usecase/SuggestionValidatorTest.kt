package com.suggestbox.domain.usecase

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class SuggestionValidatorTest {

    private lateinit var validator: SuggestionValidator

    @Before
    fun setUp() {
        validator = SuggestionValidator()
    }

    @Test
    fun `short title returns TITLE_TOO_SHORT`() {
        val result = validator.validateSuggestion(
            title = "Pothole",
            description = "Deep pothole right in front of the primary school gate.",
        )
        assertThat(result).isEqualTo(ValidationResult.Invalid(ValidationError.TITLE_TOO_SHORT))
    }

    @Test
    fun `short description returns DESCRIPTION_TOO_SHORT`() {
        val result = validator.validateSuggestion(
            title = "Repair broken streetlamp",
            description = "Broken light",
        )
        assertThat(result).isEqualTo(ValidationResult.Invalid(ValidationError.DESCRIPTION_TOO_SHORT))
    }

    @Test
    fun `spam or profanity term returns PROFANITY_OR_SPAM_DETECTED`() {
        val result = validator.validateSuggestion(
            title = "Buy now cheap lights",
            description = "Click here for free money near Ward 14 main square.",
        )
        assertThat(result).isEqualTo(ValidationResult.Invalid(ValidationError.PROFANITY_OR_SPAM_DETECTED))
    }

    @Test
    fun `rate limit triggers after five rapid submissions`() {
        repeat(5) { validator.recordSuccessfulSubmission() }
        val result = validator.validateSuggestion(
            title = "Repair main drainage cover",
            description = "The drainage cover on 12th cross has collapsed and needs urgent repair.",
        )
        assertThat(result).isEqualTo(ValidationResult.Invalid(ValidationError.RATE_LIMIT_EXCEEDED))
    }

    @Test
    fun `phone and otp validation accept valid Indian inputs`() {
        assertThat(validator.validatePhone("+91 98450 12345")).isEqualTo(ValidationResult.Valid)
        assertThat(validator.validatePhone("12345")).isEqualTo(ValidationResult.Invalid(ValidationError.INVALID_PHONE))
        assertThat(validator.validateOtp("123456")).isEqualTo(ValidationResult.Valid)
        assertThat(validator.validateOtp("12ab")).isEqualTo(ValidationResult.Invalid(ValidationError.INVALID_OTP))
    }

    @Test
    fun `email validation enforces RFC format and government domains`() {
        assertThat(validator.validateEmail("citizen@example.gov.in")).isEqualTo(ValidationResult.Valid)
        assertThat(validator.validateEmail("officer.ward14@bengaluru.gov.in")).isEqualTo(ValidationResult.Valid)
        assertThat(validator.validateEmail("resident.blr@gmail.com")).isEqualTo(ValidationResult.Valid)
        assertThat(validator.validateEmail("plainaddress")).isEqualTo(ValidationResult.Invalid(ValidationError.INVALID_EMAIL))
        assertThat(validator.validateEmail("@missinguser.com")).isEqualTo(ValidationResult.Invalid(ValidationError.INVALID_EMAIL))
        assertThat(validator.validateEmail("missingdomain@")).isEqualTo(ValidationResult.Invalid(ValidationError.INVALID_EMAIL))
        assertThat(validator.validateEmail("")).isEqualTo(ValidationResult.Invalid(ValidationError.INVALID_EMAIL))
    }
}
