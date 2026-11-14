package com.suggestbox.domain.repository

import com.suggestbox.domain.model.AdminAnalytics
import com.suggestbox.domain.model.Announcement
import com.suggestbox.domain.model.AppPreferences
import com.suggestbox.domain.model.LanguageOption
import com.suggestbox.domain.model.Suggestion
import com.suggestbox.domain.model.SuggestionCategory
import com.suggestbox.domain.model.SuggestionDraft
import com.suggestbox.domain.model.SuggestionPriority
import com.suggestbox.domain.model.SuggestionStatus
import com.suggestbox.domain.model.UserProfile
import com.suggestbox.domain.model.UserRole
import com.suggestbox.domain.model.WardInfo
import kotlinx.coroutines.flow.Flow

/**
 * Delivery metadata for real-time email verification codes and in-app autofill.
 */
data class OtpDeliveryEvent(
    val email: String,
    val code: String,
    val verificationId: String,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Swappable authentication repository interface supporting Email OTP, Anonymous citizen mode,
 * role switching for governance demos, DPDP Act data deletion, and legacy account migration.
 */
interface AuthRepository {
    val currentUser: Flow<UserProfile?>
    val latestDispatchedOtp: Flow<OtpDeliveryEvent?> get() = kotlinx.coroutines.flow.emptyFlow()

    suspend fun signInWithPassword(mobileNumber: String, password: String): Result<UserProfile> =
        Result.failure(NotImplementedError("Password authentication not implemented"))
    suspend fun registerWithPassword(fullName: String, mobileNumber: String, password: String): Result<UserProfile> =
        Result.failure(NotImplementedError("Password registration not implemented"))
    suspend fun requestOtp(email: String): Result<String>
    suspend fun verifyOtp(verificationId: String, otpCode: String): Result<UserProfile>
    suspend fun resendOtp(email: String): Result<String>
    suspend fun linkVerifiedEmail(email: String, verificationId: String, otpCode: String): Result<UserProfile>
    suspend fun signInAnonymously(wardId: String): Result<UserProfile>
    suspend fun switchPreviewRole(role: UserRole): Result<UserProfile>
    suspend fun signOut()
    suspend fun deleteAccountAndData(): Result<Unit>
}

/**
 * Swappable civic suggestion repository (backed by Mock repository out-of-the-box or Firebase).
 */
interface SuggestionRepository {
    fun observeSuggestions(): Flow<List<Suggestion>>
    fun observeSuggestionById(suggestionId: String): Flow<Suggestion?>
    fun observeMySuggestions(userId: String): Flow<List<Suggestion>>
    fun observeAnnouncements(wardId: String): Flow<List<Announcement>>
    fun observeWards(): Flow<List<WardInfo>>

    suspend fun findByTrackingId(trackingId: String): Suggestion?
    suspend fun submitSuggestion(draft: SuggestionDraft): Result<Suggestion>
    suspend fun toggleUpvote(suggestionId: String): Result<Unit>
    suspend fun addComment(suggestionId: String, body: String): Result<Unit>
    suspend fun submitRating(suggestionId: String, stars: Int, feedback: String): Result<Unit>
}

/**
 * Official and Super Admin operations repository.
 */
interface AdminRepository {
    fun observeAnalytics(wardId: String? = null): Flow<AdminAnalytics>
    suspend fun updateSuggestionStatus(
        suggestionId: String,
        newStatus: SuggestionStatus,
        officialResponse: String,
    ): Result<Unit>

    suspend fun assignDepartmentAndPriority(
        suggestionId: String,
        department: String,
        officerName: String,
        priority: SuggestionPriority,
    ): Result<Unit>

    suspend fun markDuplicateAndMerge(
        duplicateSuggestionId: String,
        primaryTrackingId: String,
    ): Result<Unit>

    suspend fun broadcastAnnouncement(
        wardId: String,
        title: String,
        body: String,
        department: String,
    ): Result<Announcement>

    suspend fun exportSummaryReportCsv(wardId: String? = null): Result<String>
}

/**
 * Local Room-backed offline draft persistence and background sync queue.
 */
interface DraftRepository {
    fun observeDrafts(): Flow<List<SuggestionDraft>>
    suspend fun saveDraft(draft: SuggestionDraft)
    suspend fun deleteDraft(localId: String)
    suspend fun syncPendingDrafts(): Int
}

/**
 * Encrypted local preferences repository for theme, localization, DPDP consent,
 * and persistent user session management across process restarts.
 */
interface PreferencesRepository {
    val preferences: Flow<AppPreferences>
    val currentPreferences: AppPreferences

    suspend fun setDarkTheme(enabled: Boolean)
    suspend fun setLanguage(language: LanguageOption)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setDpdpConsent(granted: Boolean)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setSelectedWard(wardId: String)
    suspend fun setPreviewRole(role: UserRole)

    suspend fun saveUserSession(user: UserProfile)
    suspend fun clearUserSession()
    suspend fun linkVerifiedEmail(email: String)
    suspend fun dismissEmailMigrationPrompt()
}
