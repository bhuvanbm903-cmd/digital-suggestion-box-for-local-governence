package com.suggestbox.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.suggestbox.domain.model.AppPreferences
import com.suggestbox.domain.model.LanguageOption
import com.suggestbox.domain.model.UserRole
import com.suggestbox.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted SharedPreferences-backed implementation of [PreferencesRepository].
 * Protects citizen consent, language, ward selection, and role settings at rest.
 */
@Singleton
@Suppress("DEPRECATION")
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : PreferencesRepository {

    private val prefs: SharedPreferences = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREF_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }.getOrElse {
        context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
    }

    private val _preferences = MutableStateFlow(readPreferences())
    override val preferences: Flow<AppPreferences> = _preferences.asStateFlow()
    override val currentPreferences: AppPreferences get() = _preferences.value

    private fun readPreferences(): AppPreferences {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val email = prefs.getString(KEY_LOGGED_IN_EMAIL, null)
        val phone = prefs.getString(KEY_LOGGED_IN_PHONE, null)
        val isAnon = prefs.getBoolean(KEY_LOGGED_IN_ANON, false)
        val needsMigration = isLoggedIn && !isAnon && email.isNullOrBlank() && !phone.isNullOrBlank()

        return AppPreferences(
            darkTheme = prefs.getBoolean(KEY_DARK_THEME, false),
            language = LanguageOption.fromCode(prefs.getString(KEY_LANGUAGE, "en") ?: "en"),
            notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true),
            dpdpConsentGranted = prefs.getBoolean(KEY_DPDP_CONSENT, false),
            onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_DONE, false),
            selectedWardId = prefs.getString(KEY_WARD_ID, "ward-14") ?: "ward-14",
            previewRole = runCatching {
                UserRole.valueOf(prefs.getString(KEY_PREVIEW_ROLE, UserRole.CITIZEN.name) ?: UserRole.CITIZEN.name)
            }.getOrDefault(UserRole.CITIZEN),
            isLoggedIn = isLoggedIn,
            loggedInUserId = prefs.getString(KEY_LOGGED_IN_USER_ID, null),
            loggedInEmail = email,
            loggedInPhoneNumber = phone,
            loggedInDisplayName = prefs.getString(KEY_LOGGED_IN_NAME, null),
            isAnonymousUser = isAnon,
            needsEmailMigration = needsMigration,
        )
    }

    override suspend fun setDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
        _preferences.value = _preferences.value.copy(darkTheme = enabled)
    }

    override suspend fun setLanguage(language: LanguageOption) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
        _preferences.value = _preferences.value.copy(language = language)
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
        _preferences.value = _preferences.value.copy(notificationsEnabled = enabled)
    }

    override suspend fun setDpdpConsent(granted: Boolean) {
        prefs.edit().putBoolean(KEY_DPDP_CONSENT, granted).apply()
        _preferences.value = _preferences.value.copy(dpdpConsentGranted = granted)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, completed).apply()
        _preferences.value = _preferences.value.copy(onboardingCompleted = completed)
    }

    override suspend fun setSelectedWard(wardId: String) {
        prefs.edit().putString(KEY_WARD_ID, wardId).apply()
        _preferences.value = _preferences.value.copy(selectedWardId = wardId)
    }

    override suspend fun setPreviewRole(role: UserRole) {
        prefs.edit().putString(KEY_PREVIEW_ROLE, role.name).apply()
        _preferences.value = _preferences.value.copy(previewRole = role)
    }

    override suspend fun saveUserSession(user: com.suggestbox.domain.model.UserProfile) {
        val editor = prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putBoolean(KEY_ONBOARDING_DONE, true)
            .putBoolean(KEY_DPDP_CONSENT, true)
            .putString(KEY_LOGGED_IN_USER_ID, user.id)
            .putString(KEY_LOGGED_IN_NAME, user.displayName)
            .putBoolean(KEY_LOGGED_IN_ANON, user.isAnonymous)
            .putString(KEY_PREVIEW_ROLE, user.role.name)
            .putString(KEY_WARD_ID, user.wardId)

        if (user.email.isNotBlank()) {
            editor.putString(KEY_LOGGED_IN_EMAIL, user.email)
        } else {
            editor.remove(KEY_LOGGED_IN_EMAIL)
        }

        if (!user.phoneNumber.isNullOrBlank()) {
            editor.putString(KEY_LOGGED_IN_PHONE, user.phoneNumber)
        }

        editor.apply()

        val needsMigration = !user.isAnonymous && user.email.isBlank() && !user.phoneNumber.isNullOrBlank()

        _preferences.value = _preferences.value.copy(
            isLoggedIn = true,
            onboardingCompleted = true,
            dpdpConsentGranted = true,
            loggedInUserId = user.id,
            loggedInEmail = user.email.ifBlank { null },
            loggedInPhoneNumber = user.phoneNumber,
            loggedInDisplayName = user.displayName,
            isAnonymousUser = user.isAnonymous,
            previewRole = user.role,
            selectedWardId = user.wardId,
            needsEmailMigration = needsMigration,
        )
    }

    override suspend fun linkVerifiedEmail(email: String) {
        prefs.edit()
            .putString(KEY_LOGGED_IN_EMAIL, email)
            .apply()

        _preferences.value = _preferences.value.copy(
            loggedInEmail = email,
            needsEmailMigration = false,
        )
    }

    override suspend fun dismissEmailMigrationPrompt() {
        _preferences.value = _preferences.value.copy(needsEmailMigration = false)
    }

    override suspend fun clearUserSession() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_LOGGED_IN_USER_ID)
            .remove(KEY_LOGGED_IN_EMAIL)
            .remove(KEY_LOGGED_IN_PHONE)
            .remove(KEY_LOGGED_IN_NAME)
            .remove(KEY_LOGGED_IN_ANON)
            .apply()

        _preferences.value = _preferences.value.copy(
            isLoggedIn = false,
            loggedInUserId = null,
            loggedInEmail = null,
            loggedInPhoneNumber = null,
            loggedInDisplayName = null,
            isAnonymousUser = false,
            needsEmailMigration = false,
        )
    }

    private companion object {
        const val PREF_FILE_NAME = "suggestbox_secure_prefs"
        const val KEY_DARK_THEME = "dark_theme"
        const val KEY_LANGUAGE = "language_code"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_DPDP_CONSENT = "dpdp_consent"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
        const val KEY_WARD_ID = "selected_ward_id"
        const val KEY_PREVIEW_ROLE = "preview_role"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_LOGGED_IN_USER_ID = "logged_in_user_id"
        const val KEY_LOGGED_IN_EMAIL = "logged_in_email"
        const val KEY_LOGGED_IN_PHONE = "logged_in_phone"
        const val KEY_LOGGED_IN_NAME = "logged_in_name"
        const val KEY_LOGGED_IN_ANON = "logged_in_anon"
    }
}
