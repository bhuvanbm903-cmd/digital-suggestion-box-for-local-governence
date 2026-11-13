package com.suggestbox.domain.model

import androidx.annotation.StringRes
import com.suggestbox.R
import com.suggestbox.core.designsystem.components.PriorityTone
import com.suggestbox.core.designsystem.components.StatusTone

/**
 * Distinguishes infrastructural complaints ("Fix-It") from constructive neighborhood proposals ("Build-It"),
 * enabling participatory budgeting alongside grievance redressal.
 */
enum class SubmissionKind(@StringRes val labelRes: Int) {
    ISSUE(R.string.kind_issue),
    IDEA(R.string.kind_idea),
}

/**
 * Civic categories aligned with municipal and Gram Panchayat departments.
 */
enum class SuggestionCategory(@StringRes val labelRes: Int) {
    ROADS(R.string.cat_roads),
    WATER(R.string.cat_water),
    SANITATION(R.string.cat_sanitation),
    ELECTRICITY(R.string.cat_electricity),
    HEALTH(R.string.cat_health),
    EDUCATION(R.string.cat_education),
    SAFETY(R.string.cat_safety),
    PARKS(R.string.cat_parks),
    OTHER(R.string.cat_other),
}

/**
 * Lifecycle state of a civic suggestion or complaint.
 */
enum class SuggestionStatus(
    @StringRes val labelRes: Int,
    val tone: StatusTone,
) {
    SUBMITTED(R.string.status_submitted, StatusTone.SUBMITTED),
    UNDER_REVIEW(R.string.status_under_review, StatusTone.UNDER_REVIEW),
    IN_PROGRESS(R.string.status_in_progress, StatusTone.IN_PROGRESS),
    RESOLVED(R.string.status_resolved, StatusTone.RESOLVED),
    REJECTED(R.string.status_rejected, StatusTone.REJECTED),
}

/**
 * Official urgency level mapped to SLA deadlines.
 */
enum class SuggestionPriority(
    @StringRes val labelRes: Int,
    val tone: PriorityTone,
    val slaHours: Int,
) {
    LOW(R.string.priority_low, PriorityTone.LOW, 168),
    MEDIUM(R.string.priority_medium, PriorityTone.MEDIUM, 72),
    HIGH(R.string.priority_high, PriorityTone.HIGH, 48),
    URGENT(R.string.priority_urgent, PriorityTone.URGENT, 24),
}

/**
 * Geographic location pinned by a citizen or retrieved via GPS.
 */
data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val addressLabel: String,
    val distanceMeters: Int = 0,
)

/**
 * Timeline entry recording status changes and official remarks.
 */
data class StatusUpdateEvent(
    val id: String,
    val status: SuggestionStatus,
    val timestampMillis: Long,
    val note: String,
    val actorName: String,
)

/**
 * Public comment posted on a community suggestion.
 */
data class Comment(
    val id: String,
    val authorLabel: String,
    val isOfficial: Boolean,
    val body: String,
    val createdAtMillis: Long,
)

/**
 * Official broadcast announcement from the local government body.
 */
data class Announcement(
    val id: String,
    val wardId: String,
    val title: String,
    val body: String,
    val department: String,
    val publishedAtMillis: Long,
    val isPinned: Boolean = false,
)

/**
 * Local administrative ward or Gram Panchayat metadata.
 */
data class WardInfo(
    val id: String,
    val name: String,
    val municipalityName: String,
    val centerLat: Double,
    val centerLng: Double,
)

/**
 * Citizen satisfaction rating captured after a suggestion reaches [SuggestionStatus.RESOLVED].
 */
data class ResolutionRating(
    val stars: Int,
    val feedback: String,
    val submittedAtMillis: Long,
)

/**
 * Core suggestion/complaint/idea aggregate.
 */
data class Suggestion(
    val id: String,
    val trackingId: String,
    val kind: SubmissionKind = SubmissionKind.ISSUE,
    val title: String,
    val description: String,
    val category: SuggestionCategory,
    val status: SuggestionStatus,
    val priority: SuggestionPriority,
    val wardId: String,
    val wardName: String,
    val location: GeoLocation,
    val mediaUrls: List<String>,
    val afterPhotoUrl: String? = null,
    val isAnonymous: Boolean,
    val authorId: String,
    val authorDisplayName: String,
    val assignedDepartment: String?,
    val assignedOfficerName: String?,
    val upvoteCount: Int,
    val upvotedByCurrentUser: Boolean,
    val comments: List<Comment>,
    val timeline: List<StatusUpdateEvent>,
    val createdAtMillis: Long,
    val slaDeadlineMillis: Long,
    val officialResponse: String?,
    val duplicateOfTrackingId: String? = null,
    val rating: ResolutionRating? = null,
) {
    val isOverdue: Boolean
        get() = status != SuggestionStatus.RESOLVED &&
            status != SuggestionStatus.REJECTED &&
            System.currentTimeMillis() > slaDeadlineMillis
}

/**
 * Offline-first draft saved in Room before network submission.
 */
data class SuggestionDraft(
    val localId: String,
    val kind: SubmissionKind = SubmissionKind.ISSUE,
    val category: SuggestionCategory,
    val title: String,
    val description: String,
    val localMediaUris: List<String>,
    val location: GeoLocation?,
    val isAnonymous: Boolean,
    val updatedAtMillis: Long,
    val pendingSync: Boolean = false,
)

/**
 * Aggregated metrics for the Official / Admin dashboard.
 */
data class AdminAnalytics(
    val totalCount: Int,
    val countByStatus: Map<SuggestionStatus, Int>,
    val countByCategory: Map<SuggestionCategory, Int>,
    val overdueCount: Int,
    val averageResolutionHours: Double,
)
