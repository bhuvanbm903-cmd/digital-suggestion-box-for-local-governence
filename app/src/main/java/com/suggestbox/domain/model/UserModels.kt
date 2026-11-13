package com.suggestbox.domain.model

import androidx.annotation.StringRes
import com.suggestbox.R

/**
 * Supported user roles across the local governance platform.
 */
enum class UserRole(@StringRes val labelRes: Int) {
    CITIZEN(R.string.role_citizen),
    OFFICIAL(R.string.role_official),
    SUPER_ADMIN(R.string.role_super_admin),
}

/**
 * Supported application locales structured for straightforward expansion.
 */
enum class LanguageOption(
    val code: String,
    @StringRes val labelRes: Int,
) {
    ENGLISH("en", R.string.lang_english),
    HINDI("hi", R.string.lang_hindi),
    KANNADA("kn", R.string.lang_kannada),
    ;

    companion object {
        fun fromCode(code: String): LanguageOption =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

/**
 * Authenticated or anonymous citizen/official profile.
 *
 * @property id Unique identifier (empty/hashed when operating in anonymous citizen mode).
 * @property email Verified citizen or official email address.
 * @property phoneNumber Optional legacy phone number preserved for account migration.
 * @property hasVerifiedEmail Whether the email has been confirmed via OTP.
 * @property displayName Citizen display name or "Anonymous Citizen".
 * @property wardId Assigned local governance ward/panchayat ID.
 * @property wardName Human-readable ward or panchayat name.
 * @property department Department assigned to an [UserRole.OFFICIAL] user.
 * @property role Current workspace role.
 * @property isAnonymous Whether the citizen chose anonymous participation.
 */
data class UserProfile(
    val id: String,
    val email: String = "",
    val phoneNumber: String? = null,
    val hasVerifiedEmail: Boolean = true,
    val displayName: String,
    val wardId: String,
    val wardName: String,
    val department: String? = null,
    val role: UserRole = UserRole.CITIZEN,
    val isAnonymous: Boolean = false,
)

/**
 * User-configurable appearance, privacy, and persistent authenticated session settings.
 */
data class AppPreferences(
    val darkTheme: Boolean = false,
    val language: LanguageOption = LanguageOption.ENGLISH,
    val notificationsEnabled: Boolean = true,
    val dpdpConsentGranted: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val selectedWardId: String = "ward-14",
    val previewRole: UserRole = UserRole.CITIZEN,
    val isLoggedIn: Boolean = false,
    val loggedInUserId: String? = null,
    val loggedInEmail: String? = null,
    val loggedInPhoneNumber: String? = null,
    val loggedInDisplayName: String? = null,
    val isAnonymousUser: Boolean = false,
    val needsEmailMigration: Boolean = false,
)
