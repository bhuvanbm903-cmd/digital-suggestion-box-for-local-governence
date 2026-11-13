package com.suggestbox.data.mock

import com.suggestbox.domain.model.Announcement
import com.suggestbox.domain.model.Comment
import com.suggestbox.domain.model.GeoLocation
import com.suggestbox.domain.model.StatusUpdateEvent
import com.suggestbox.domain.model.SubmissionKind
import com.suggestbox.domain.model.Suggestion
import com.suggestbox.domain.model.SuggestionCategory
import com.suggestbox.domain.model.SuggestionPriority
import com.suggestbox.domain.model.SuggestionStatus
import com.suggestbox.domain.model.UserProfile
import com.suggestbox.domain.model.UserRole
import com.suggestbox.domain.model.WardInfo

/**
 * Realistic Indian local governance seed data enabling immediate offline/demo execution
 * without requiring Firebase credentials.
 */
object MockSeedData {

    private const val HOUR_MS = 3_600_000L
    private const val DAY_MS = 86_400_000L

    val wards = listOf(
        WardInfo(
            id = "ward-14",
            name = "Ward 14 • Indiranagar",
            municipalityName = "Bengaluru City Municipal Council",
            centerLat = 12.9784,
            centerLng = 77.6408,
        ),
        WardInfo(
            id = "ward-08",
            name = "Ward 08 • Malleswaram",
            municipalityName = "Bengaluru City Municipal Council",
            centerLat = 13.0031,
            centerLng = 77.5643,
        ),
        WardInfo(
            id = "gp-kengeri",
            name = "Kengeri Gram Panchayat",
            municipalityName = "Bengaluru South Zilla Panchayat",
            centerLat = 12.9077,
            centerLng = 77.4855,
        ),
    )

    val citizenUser = UserProfile(
        id = "user-citizen-1",
        email = "aarav.kulkarni@example.gov.in",
        phoneNumber = "+91 98450 12345",
        hasVerifiedEmail = true,
        displayName = "Aarav Kulkarni",
        wardId = "ward-14",
        wardName = "Ward 14 • Indiranagar",
        role = UserRole.CITIZEN,
        isAnonymous = false,
    )

    val officialUser = UserProfile(
        id = "user-official-1",
        email = "meera.deshmukh@bbmp.gov.in",
        phoneNumber = "+91 94480 55432",
        hasVerifiedEmail = true,
        displayName = "Meera Deshmukh (AEE)",
        wardId = "ward-14",
        wardName = "Ward 14 • Indiranagar",
        department = "Public Works & Roads",
        role = UserRole.OFFICIAL,
        isAnonymous = false,
    )

    val superAdminUser = UserProfile(
        id = "user-admin-1",
        email = "commissioner.urban@bbmp.gov.in",
        phoneNumber = "+91 80 2222 1100",
        hasVerifiedEmail = true,
        displayName = "R. Venkatesh, IAS (Commissioner)",
        wardId = "ward-14",
        wardName = "Municipal Head Office",
        department = "Executive Administration",
        role = UserRole.SUPER_ADMIN,
        isAnonymous = false,
    )

    fun initialAnnouncements(now: Long = System.currentTimeMillis()): List<Announcement> = listOf(
        Announcement(
            id = "ann-1",
            wardId = "ward-14",
            title = "Scheduled Water Pipeline Maintenance",
            body = "Potable water supply in 12th Main and adjoining blocks will resume by 5:00 PM today after valve replacement.",
            department = "Water Supply & Sewerage Board",
            publishedAtMillis = now - (3 * HOUR_MS),
            isPinned = true,
        ),
        Announcement(
            id = "ann-2",
            wardId = "ward-14",
            title = "Ward Sabha & Participatory Budget Meet",
            body = "Join Ward 14 open consultation this Saturday at 10:30 AM at the Community Hall to vote on top citizen proposals.",
            department = "Ward Committee",
            publishedAtMillis = now - DAY_MS,
        ),
    )

    fun initialSuggestions(now: Long = System.currentTimeMillis()): List<Suggestion> = listOf(
        Suggestion(
            id = "sug-101",
            trackingId = "SBX-W14-2041",
            title = "Repair sunken stormwater drain grate on 100ft Road",
            description = "The iron stormwater cover near the pedestrian crossing has caved in by 15cm, posing a severe hazard for two-wheelers and school pedestrians.",
            category = SuggestionCategory.ROADS,
            status = SuggestionStatus.IN_PROGRESS,
            priority = SuggestionPriority.URGENT,
            wardId = "ward-14",
            wardName = "Ward 14 • Indiranagar",
            location = GeoLocation(12.9789, 77.6412, "100ft Road, near Metro Pillar 42", 180),
            mediaUrls = emptyList(),
            isAnonymous = false,
            authorId = "user-citizen-1",
            authorDisplayName = "Aarav Kulkarni",
            assignedDepartment = "Public Works & Roads",
            assignedOfficerName = "Meera Deshmukh (AEE)",
            upvoteCount = 47,
            upvotedByCurrentUser = true,
            comments = listOf(
                Comment(
                    id = "c-1",
                    authorLabel = "Priya Nair",
                    isOfficial = false,
                    body = "Almost slipped here yesterday evening. Urgent barricading needed.",
                    createdAtMillis = now - (14 * HOUR_MS),
                ),
                Comment(
                    id = "c-2",
                    authorLabel = "Meera Deshmukh (AEE)",
                    isOfficial = true,
                    body = "Safety cones placed. Precast concrete grate fabrication scheduled for tomorrow morning.",
                    createdAtMillis = now - (5 * HOUR_MS),
                ),
            ),
            timeline = listOf(
                StatusUpdateEvent("e-1", SuggestionStatus.SUBMITTED, now - (26 * HOUR_MS), "Logged via Citizen PWA with GPS tag", "System"),
                StatusUpdateEvent("e-2", SuggestionStatus.UNDER_REVIEW, now - (20 * HOUR_MS), "Verified site hazard and marked Urgent", "Ward Desk"),
                StatusUpdateEvent("e-3", SuggestionStatus.IN_PROGRESS, now - (5 * HOUR_MS), "Barricaded; precast slab dispatched", "Meera Deshmukh (AEE)"),
            ),
            createdAtMillis = now - (26 * HOUR_MS),
            slaDeadlineMillis = now - (2 * HOUR_MS), // Overdue example for SLA indicator
            officialResponse = "Safety cones placed. Precast concrete grate installation underway.",
        ),
        Suggestion(
            id = "sug-102",
            trackingId = "SBX-W14-2038",
            kind = SubmissionKind.IDEA,
            title = "Install solar LED lamps along Lake Walking Trail",
            description = "Adding 12 solar-powered pathway lights along the southern walking loop will improve evening safety for senior citizens and save grid electricity.",
            category = SuggestionCategory.PARKS,
            status = SuggestionStatus.UNDER_REVIEW,
            priority = SuggestionPriority.MEDIUM,
            wardId = "ward-14",
            wardName = "Ward 14 • Indiranagar",
            location = GeoLocation(12.9752, 77.6380, "Domlur-Indiranagar Neighborhood Park", 420),
            mediaUrls = emptyList(),
            isAnonymous = false,
            authorId = "user-citizen-2",
            authorDisplayName = "Kavitha Rao",
            assignedDepartment = "Electrical & Parks",
            assignedOfficerName = "Suresh Babu (JE)",
            upvoteCount = 89,
            upvotedByCurrentUser = false,
            comments = emptyList(),
            timeline = listOf(
                StatusUpdateEvent("e-4", SuggestionStatus.SUBMITTED, now - (3 * DAY_MS), "Submitted as community improvement idea", "System"),
                StatusUpdateEvent("e-5", SuggestionStatus.UNDER_REVIEW, now - (2 * DAY_MS), "Shortlisted for Saturday Ward Sabha budget review", "Ward Committee"),
            ),
            createdAtMillis = now - (3 * DAY_MS),
            slaDeadlineMillis = now + (2 * DAY_MS),
            officialResponse = "Shortlisted for Ward Committee participatory budget evaluation.",
        ),
        Suggestion(
            id = "sug-103",
            trackingId = "SBX-W14-2029",
            title = "Irregular dry-waste collection in 6th Cross",
            description = "The dry-waste auto tipper has skipped 6th Cross lane for three consecutive collection days.",
            category = SuggestionCategory.SANITATION,
            status = SuggestionStatus.RESOLVED,
            priority = SuggestionPriority.HIGH,
            wardId = "ward-14",
            wardName = "Ward 14 • Indiranagar",
            location = GeoLocation(12.9801, 77.6442, "6th Cross, HAL 2nd Stage", 650),
            mediaUrls = emptyList(),
            afterPhotoUrl = "verified://after-clean-6th-cross",
            isAnonymous = true,
            authorId = "user-citizen-1",
            authorDisplayName = "Anonymous Citizen",
            assignedDepartment = "Solid Waste Management",
            assignedOfficerName = "Lakshmi N. (Health Inspector)",
            upvoteCount = 24,
            upvotedByCurrentUser = false,
            comments = emptyList(),
            timeline = listOf(
                StatusUpdateEvent("e-6", SuggestionStatus.SUBMITTED, now - (5 * DAY_MS), "Anonymous submission received", "System"),
                StatusUpdateEvent("e-7", SuggestionStatus.IN_PROGRESS, now - (4 * DAY_MS), "Route supervisor alerted", "Lakshmi N."),
                StatusUpdateEvent("e-8", SuggestionStatus.RESOLVED, now - (3 * DAY_MS), "Waste cleared and morning route GPS verified", "Lakshmi N."),
            ),
            createdAtMillis = now - (5 * DAY_MS),
            slaDeadlineMillis = now - (3 * DAY_MS),
            officialResponse = "Collection vehicle reassigned; GPS route compliance verified for 7:30 AM daily.",
        ),
        Suggestion(
            id = "sug-104",
            trackingId = "SBX-W14-2050",
            title = "Contaminated water pressure drop near Primary School",
            description = "Low pressure and slight turbidity noticed in morning tap supply outside Govt Primary School gate.",
            category = SuggestionCategory.WATER,
            status = SuggestionStatus.SUBMITTED,
            priority = SuggestionPriority.HIGH,
            wardId = "ward-14",
            wardName = "Ward 14 • Indiranagar",
            location = GeoLocation(12.9765, 77.6395, "Govt Primary School Road", 310),
            mediaUrls = emptyList(),
            isAnonymous = false,
            authorId = "user-citizen-3",
            authorDisplayName = "Rohan Hegde",
            assignedDepartment = null,
            assignedOfficerName = null,
            upvoteCount = 31,
            upvotedByCurrentUser = false,
            comments = emptyList(),
            timeline = listOf(
                StatusUpdateEvent("e-9", SuggestionStatus.SUBMITTED, now - (2 * HOUR_MS), "Received via mobile app", "System"),
            ),
            createdAtMillis = now - (2 * HOUR_MS),
            slaDeadlineMillis = now + (46 * HOUR_MS),
            officialResponse = null,
        ),
    )
}
