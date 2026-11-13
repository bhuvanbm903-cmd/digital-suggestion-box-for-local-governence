package com.suggestbox.ui.navigation

/**
 * Centralized navigation routes for the single-activity SuggestBox Compose graph.
 */
object SuggestBoxRoutes {
    // Bottom Tab Routes (Citizen 4-Tab Bar)
    const val TAB_HOME = "citizen/home"
    const val TAB_COMMUNITY = "citizen/community"
    const val TAB_TRACK = "citizen/track"
    const val TAB_PROFILE = "citizen/profile"

    // Pre-Auth & Citizen Flows (Phase 2)
    const val ONBOARDING = "auth/onboarding"
    const val DPDP_CONSENT = "auth/consent"
    const val LOGIN = "auth/login"
    const val SUBMIT_SUGGESTION = "citizen/submit?draftId={draftId}"
    const val SUGGESTION_DETAIL = "citizen/detail/{suggestionId}"

    fun submitSuggestion(draftId: String? = null): String =
        if (draftId.isNullOrBlank()) "citizen/submit" else "citizen/submit?draftId=$draftId"

    fun suggestionDetail(suggestionId: String): String = "citizen/detail/$suggestionId"

    // Official & Super Admin Flows (Phase 3)
    const val OFFICIAL_DASHBOARD = "official/dashboard"
    const val OFFICIAL_INBOX = "official/inbox"
    const val OFFICIAL_DETAIL = "official/detail/{suggestionId}"
    const val BROADCAST_ANNOUNCEMENT = "official/broadcast"
    const val SUPER_ADMIN_OVERVIEW = "admin/overview"

    fun officialDetail(suggestionId: String): String = "official/detail/$suggestionId"

    val citizenTabs = setOf(TAB_HOME, TAB_COMMUNITY, TAB_TRACK, TAB_PROFILE)
}
