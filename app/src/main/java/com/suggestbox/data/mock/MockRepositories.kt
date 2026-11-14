package com.suggestbox.data.mock

import com.suggestbox.domain.model.AdminAnalytics
import com.suggestbox.domain.model.Announcement
import com.suggestbox.domain.model.Comment
import com.suggestbox.domain.model.GeoLocation
import com.suggestbox.domain.model.ResolutionRating
import com.suggestbox.domain.model.StatusUpdateEvent
import com.suggestbox.domain.model.Suggestion
import com.suggestbox.domain.model.SuggestionDraft
import com.suggestbox.domain.model.SuggestionPriority
import com.suggestbox.domain.model.SuggestionStatus
import com.suggestbox.domain.model.UserProfile
import com.suggestbox.domain.model.UserRole
import com.suggestbox.domain.model.WardInfo
import com.suggestbox.domain.repository.AdminRepository
import com.suggestbox.domain.repository.AuthRepository
import com.suggestbox.domain.repository.DraftRepository
import com.suggestbox.domain.repository.SuggestionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import com.suggestbox.data.remote.email.EmailOtpManager
import com.suggestbox.domain.repository.OtpDeliveryEvent
import com.suggestbox.domain.repository.PreferencesRepository

/**
 * In-memory mock implementation of [AuthRepository] allowing instant testing of Citizen,
 * Anonymous, Official, and Super Admin journeys without Firebase credentials.
 * Backed by EmailOtpManager (SHA-256 salted hashes, 10-min expiry, 5-attempt rate limits)
 * and encrypted local preferences so user sessions survive process restarts and never log out
 * until explicit user permission.
 */
@Singleton
class MockAuthRepository @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val emailOtpManager: EmailOtpManager,
) : AuthRepository {

    private val _latestDispatchedOtp = MutableStateFlow<OtpDeliveryEvent?>(null)
    override val latestDispatchedOtp: Flow<OtpDeliveryEvent?> = _latestDispatchedOtp.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile?>(restoreSessionFromPrefs())
    override val currentUser: Flow<UserProfile?> = _currentUser.asStateFlow()

    private fun restoreSessionFromPrefs(): UserProfile? {
        val prefs = preferencesRepository.currentPreferences
        if (!prefs.isLoggedIn) {
            return null
        }
        val email = prefs.loggedInEmail ?: "citizen@example.gov.in"
        val displayName = prefs.loggedInDisplayName
            ?: if (prefs.isAnonymousUser) "Anonymous Citizen"
            else email.substringBefore("@").replaceFirstChar { it.titlecase() }

        return UserProfile(
            id = prefs.loggedInUserId ?: "user-citizen-01",
            email = email,
            phoneNumber = prefs.loggedInPhoneNumber,
            hasVerifiedEmail = true,
            displayName = displayName,
            wardId = prefs.selectedWardId,
            wardName = "Ward 14 • Indiranagar",
            role = prefs.previewRole,
            isAnonymous = prefs.isAnonymousUser,
        )
    }

    override suspend fun signInWithPassword(mobileNumber: String, password: String): Result<UserProfile> {
        delay(300)
        val cleanNumber = mobileNumber.replace(Regex("[^0-9]"), "").takeLast(10)
        if (cleanNumber.length != 10) {
            return Result.failure(IllegalArgumentException("Please enter a valid 10-digit mobile number"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters long"))
        }
        val user = UserProfile(
            id = "user-${cleanNumber.takeLast(4)}",
            email = "$cleanNumber@phone.suggestbox.gov.in",
            phoneNumber = "+91$cleanNumber",
            hasVerifiedEmail = true,
            displayName = "Citizen ($cleanNumber)",
            wardId = preferencesRepository.currentPreferences.selectedWardId,
            wardName = "Ward 14 • Indiranagar",
            role = UserRole.CITIZEN,
            isAnonymous = false,
        )
        _currentUser.value = user
        preferencesRepository.saveUserSession(user)
        return Result.success(user)
    }

    override suspend fun registerWithPassword(fullName: String, mobileNumber: String, password: String): Result<UserProfile> {
        delay(300)
        val cleanNumber = mobileNumber.replace(Regex("[^0-9]"), "").takeLast(10)
        if (fullName.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your full name"))
        }
        if (cleanNumber.length != 10) {
            return Result.failure(IllegalArgumentException("Please enter a valid 10-digit mobile number"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters long"))
        }
        val user = UserProfile(
            id = "user-${cleanNumber.takeLast(4)}",
            email = "$cleanNumber@phone.suggestbox.gov.in",
            phoneNumber = "+91$cleanNumber",
            hasVerifiedEmail = true,
            displayName = fullName.trim(),
            wardId = preferencesRepository.currentPreferences.selectedWardId,
            wardName = "Ward 14 • Indiranagar",
            role = UserRole.CITIZEN,
            isAnonymous = false,
        )
        _currentUser.value = user
        preferencesRepository.saveUserSession(user)
        return Result.success(user)
    }

    override suspend fun requestOtp(email: String): Result<String> {
        delay(250)
        val normalized = email.trim().lowercase()
        return emailOtpManager.requestOtp(normalized).fold(
            onSuccess = { (verId, code) ->
                _latestDispatchedOtp.value = OtpDeliveryEvent(
                    email = normalized,
                    code = code,
                    verificationId = verId,
                )
                Result.success(verId)
            },
            onFailure = { Result.failure(it) },
        )
    }

    override suspend fun resendOtp(email: String): Result<String> {
        return requestOtp(email)
    }

    override suspend fun verifyOtp(verificationId: String, otpCode: String): Result<UserProfile> {
        delay(250)
        return emailOtpManager.verifyOtp(verificationId, otpCode).fold(
            onSuccess = { verifiedEmail ->
                val name = verifiedEmail.substringBefore("@")
                    .replace(".", " ")
                    .split(" ")
                    .joinToString(" ") { word -> word.replaceFirstChar { it.titlecase() } }

                val user = UserProfile(
                    id = "citizen-${UUID.randomUUID().toString().take(8)}",
                    email = verifiedEmail,
                    phoneNumber = preferencesRepository.currentPreferences.loggedInPhoneNumber,
                    hasVerifiedEmail = true,
                    displayName = name.ifBlank { "Citizen" },
                    wardId = preferencesRepository.currentPreferences.selectedWardId,
                    wardName = "Ward 14 • Indiranagar",
                    role = UserRole.CITIZEN,
                    isAnonymous = false,
                )

                _currentUser.value = user
                preferencesRepository.saveUserSession(user)
                Result.success(user)
            },
            onFailure = { Result.failure(it) },
        )
    }

    override suspend fun linkVerifiedEmail(
        email: String,
        verificationId: String,
        otpCode: String,
    ): Result<UserProfile> {
        delay(250)
        return emailOtpManager.verifyOtp(verificationId, otpCode).fold(
            onSuccess = { verifiedEmail ->
                val existing = _currentUser.value ?: MockSeedData.citizenUser
                val updated = existing.copy(
                    email = verifiedEmail,
                    hasVerifiedEmail = true,
                )
                _currentUser.value = updated
                preferencesRepository.linkVerifiedEmail(verifiedEmail)
                Result.success(updated)
            },
            onFailure = { Result.failure(it) },
        )
    }

    override suspend fun signInAnonymously(wardId: String): Result<UserProfile> {
        delay(200)
        val ward = MockSeedData.wards.firstOrNull { it.id == wardId } ?: MockSeedData.wards.first()
        val anon = UserProfile(
            id = "anon-${UUID.randomUUID().toString().take(8)}",
            email = "",
            phoneNumber = null,
            hasVerifiedEmail = false,
            displayName = "Anonymous Citizen",
            wardId = ward.id,
            wardName = ward.name,
            role = UserRole.CITIZEN,
            isAnonymous = true,
        )
        _currentUser.value = anon
        preferencesRepository.saveUserSession(anon)
        return Result.success(anon)
    }

    override suspend fun switchPreviewRole(role: UserRole): Result<UserProfile> {
        val current = _currentUser.value ?: MockSeedData.citizenUser
        val target = when (role) {
            UserRole.CITIZEN -> current.copy(role = UserRole.CITIZEN)
            UserRole.OFFICIAL -> MockSeedData.officialUser
            UserRole.SUPER_ADMIN -> MockSeedData.superAdminUser
        }
        _currentUser.value = target
        preferencesRepository.saveUserSession(target)
        return Result.success(target)
    }

    override suspend fun signOut() {
        _currentUser.value = null
        preferencesRepository.clearUserSession()
    }

    override suspend fun deleteAccountAndData(): Result<Unit> {
        delay(250)
        _currentUser.value = null
        preferencesRepository.clearUserSession()
        preferencesRepository.setDpdpConsent(false)
        return Result.success(Unit)
    }
}

/**
 * In-memory mock implementation of [SuggestionRepository] and [AdminRepository]
 * providing live reactive streams across Citizen and Official UIs.
 */
@Singleton
class MockGovernanceRepository @Inject constructor(
    private val authRepository: AuthRepository,
) : SuggestionRepository, AdminRepository {

    private val suggestions = MutableStateFlow(MockSeedData.initialSuggestions())
    private val announcements = MutableStateFlow(MockSeedData.initialAnnouncements())
    private val wards = MutableStateFlow(MockSeedData.wards)

    override fun observeSuggestions(): Flow<List<Suggestion>> = suggestions.asStateFlow()

    override fun observeSuggestionById(suggestionId: String): Flow<Suggestion?> =
        suggestions.map { list -> list.firstOrNull { it.id == suggestionId } }

    override fun observeMySuggestions(userId: String): Flow<List<Suggestion>> =
        suggestions.map { list -> list.filter { it.authorId == userId } }

    override fun observeAnnouncements(wardId: String): Flow<List<Announcement>> =
        announcements.map { list -> list.filter { it.wardId == wardId } }

    override fun observeWards(): Flow<List<WardInfo>> = wards.asStateFlow()

    override suspend fun findByTrackingId(trackingId: String): Suggestion? =
        suggestions.value.firstOrNull { it.trackingId.equals(trackingId.trim(), ignoreCase = true) }

    override suspend fun submitSuggestion(draft: SuggestionDraft): Result<Suggestion> {
        delay(450)
        val now = System.currentTimeMillis()
        val id = "sug-${UUID.randomUUID().toString().take(6)}"
        val trackingId = "SBX-W14-${(2100..9999).random()}"
        val ward = wards.value.first()
        val newItem = Suggestion(
            id = id,
            trackingId = trackingId,
            kind = draft.kind,
            title = draft.title.trim(),
            description = draft.description.trim(),
            category = draft.category,
            status = SuggestionStatus.SUBMITTED,
            priority = SuggestionPriority.MEDIUM,
            wardId = ward.id,
            wardName = ward.name,
            location = draft.location ?: GeoLocation(ward.centerLat, ward.centerLng, ward.name),
            mediaUrls = draft.localMediaUris,
            isAnonymous = draft.isAnonymous,
            authorId = if (draft.isAnonymous) "anon" else "user-citizen-1",
            authorDisplayName = if (draft.isAnonymous) "Anonymous Citizen" else "Aarav Kulkarni",
            assignedDepartment = null,
            assignedOfficerName = null,
            upvoteCount = 1,
            upvotedByCurrentUser = true,
            comments = emptyList(),
            timeline = listOf(
                StatusUpdateEvent(
                    id = "evt-${UUID.randomUUID().toString().take(5)}",
                    status = SuggestionStatus.SUBMITTED,
                    timestampMillis = now,
                    note = "Suggestion submitted and queued for ward review",
                    actorName = "System",
                ),
            ),
            createdAtMillis = now,
            slaDeadlineMillis = now + SuggestionPriority.MEDIUM.slaHours * 3_600_000L,
            officialResponse = null,
        )
        suggestions.value = listOf(newItem) + suggestions.value
        return Result.success(newItem)
    }

    override suspend fun toggleUpvote(suggestionId: String): Result<Unit> {
        suggestions.value = suggestions.value.map { item ->
            if (item.id == suggestionId) {
                val nextUpvoted = !item.upvotedByCurrentUser
                item.copy(
                    upvotedByCurrentUser = nextUpvoted,
                    upvoteCount = (item.upvoteCount + if (nextUpvoted) 1 else -1).coerceAtLeast(0),
                )
            } else {
                item
            }
        }
        return Result.success(Unit)
    }

    override suspend fun addComment(suggestionId: String, body: String): Result<Unit> {
        val clean = body.trim()
        if (clean.isEmpty()) return Result.failure(IllegalArgumentException("Comment cannot be empty"))
        val newComment = Comment(
            id = "c-${UUID.randomUUID().toString().take(6)}",
            authorLabel = "Citizen Participant",
            isOfficial = false,
            body = clean,
            createdAtMillis = System.currentTimeMillis(),
        )
        suggestions.value = suggestions.value.map { item ->
            if (item.id == suggestionId) item.copy(comments = item.comments + newComment) else item
        }
        return Result.success(Unit)
    }

    override suspend fun submitRating(suggestionId: String, stars: Int, feedback: String): Result<Unit> {
        suggestions.value = suggestions.value.map { item ->
            if (item.id == suggestionId) {
                item.copy(
                    rating = ResolutionRating(
                        stars = stars.coerceIn(1, 5),
                        feedback = feedback.trim(),
                        submittedAtMillis = System.currentTimeMillis(),
                    ),
                )
            } else {
                item
            }
        }
        return Result.success(Unit)
    }

    override fun observeAnalytics(wardId: String?): Flow<AdminAnalytics> =
        suggestions.map { all ->
            val filtered = if (wardId == null) all else all.filter { it.wardId == wardId }
            AdminAnalytics(
                totalCount = filtered.size,
                countByStatus = SuggestionStatus.entries.associateWith { status ->
                    filtered.count { it.status == status }
                },
                countByCategory = filtered.groupingBy { it.category }.eachCount(),
                overdueCount = filtered.count { it.isOverdue },
                averageResolutionHours = 38.5,
            )
        }

    override suspend fun updateSuggestionStatus(
        suggestionId: String,
        newStatus: SuggestionStatus,
        officialResponse: String,
    ): Result<Unit> {
        val now = System.currentTimeMillis()
        suggestions.value = suggestions.value.map { item ->
            if (item.id == suggestionId) {
                val event = StatusUpdateEvent(
                    id = "evt-${UUID.randomUUID().toString().take(6)}",
                    status = newStatus,
                    timestampMillis = now,
                    note = officialResponse.ifBlank { "Status updated to ${newStatus.name}" },
                    actorName = "Meera Deshmukh (AEE)",
                )
                item.copy(
                    status = newStatus,
                    officialResponse = officialResponse.ifBlank { item.officialResponse },
                    timeline = item.timeline + event,
                )
            } else {
                item
            }
        }
        return Result.success(Unit)
    }

    override suspend fun assignDepartmentAndPriority(
        suggestionId: String,
        department: String,
        officerName: String,
        priority: SuggestionPriority,
    ): Result<Unit> {
        suggestions.value = suggestions.value.map { item ->
            if (item.id == suggestionId) {
                item.copy(
                    assignedDepartment = department,
                    assignedOfficerName = officerName,
                    priority = priority,
                    slaDeadlineMillis = item.createdAtMillis + priority.slaHours * 3_600_000L,
                )
            } else {
                item
            }
        }
        return Result.success(Unit)
    }

    override suspend fun markDuplicateAndMerge(
        duplicateSuggestionId: String,
        primaryTrackingId: String,
    ): Result<Unit> {
        suggestions.value = suggestions.value.map { item ->
            if (item.id == duplicateSuggestionId) {
                item.copy(
                    status = SuggestionStatus.REJECTED,
                    duplicateOfTrackingId = primaryTrackingId,
                    officialResponse = "Merged as duplicate of $primaryTrackingId.",
                )
            } else {
                item
            }
        }
        return Result.success(Unit)
    }

    override suspend fun broadcastAnnouncement(
        wardId: String,
        title: String,
        body: String,
        department: String,
    ): Result<Announcement> {
        val newItem = Announcement(
            id = "ann-${UUID.randomUUID().toString().take(6)}",
            wardId = wardId,
            title = title.trim(),
            body = body.trim(),
            department = department.trim(),
            publishedAtMillis = System.currentTimeMillis(),
            isPinned = true,
        )
        announcements.value = listOf(newItem) + announcements.value
        return Result.success(newItem)
    }

    override suspend fun exportSummaryReportCsv(wardId: String?): Result<String> {
        val header = "TrackingId,Title,Category,Status,Priority,Ward,Upvotes"
        val rows = suggestions.value
            .filter { wardId == null || it.wardId == wardId }
            .joinToString("\n") { s ->
                "${s.trackingId},\"${s.title.replace("\"", "'")}\",${s.category},${s.status},${s.priority},${s.wardName},${s.upvoteCount}"
            }
        return Result.success("$header\n$rows")
    }
}

/**
 * In-memory placeholder for [DraftRepository] in Phase 1 (replaced with Room DAO in Phase 2/4).
 */
@Singleton
class InMemoryDraftRepository @Inject constructor() : DraftRepository {
    private val drafts = MutableStateFlow<List<SuggestionDraft>>(emptyList())

    override fun observeDrafts(): Flow<List<SuggestionDraft>> = drafts.asStateFlow()

    override suspend fun saveDraft(draft: SuggestionDraft) {
        drafts.value = drafts.value.filterNot { it.localId == draft.localId } + draft
    }

    override suspend fun deleteDraft(localId: String) {
        drafts.value = drafts.value.filterNot { it.localId == localId }
    }

    override suspend fun syncPendingDrafts(): Int = 0
}
