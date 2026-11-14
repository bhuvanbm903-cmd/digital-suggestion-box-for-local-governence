package com.suggestbox.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.suggestbox.R
import com.suggestbox.core.designsystem.components.BottomBarItem
import com.suggestbox.core.designsystem.components.TranslucentBottomBar
import com.suggestbox.core.designsystem.icons.SuggestIcons
import com.suggestbox.core.designsystem.motion.SuggestMotion
import com.suggestbox.domain.model.AppPreferences
import com.suggestbox.domain.model.LanguageOption
import com.suggestbox.domain.model.UserRole
import com.suggestbox.ui.screens.auth.LoginScreen
import com.suggestbox.ui.screens.community.CommunityScreen
import com.suggestbox.ui.screens.detail.SuggestionDetailScreen
import com.suggestbox.ui.screens.home.HomeScreen
import com.suggestbox.ui.screens.official.BroadcastAnnouncementScreen
import com.suggestbox.ui.screens.official.OfficialDashboardScreen
import com.suggestbox.ui.screens.official.OfficialDetailScreen
import com.suggestbox.ui.screens.official.OfficialInboxScreen
import com.suggestbox.ui.screens.official.SuperAdminScreen
import com.suggestbox.ui.screens.onboarding.DpdpConsentScreen
import com.suggestbox.ui.screens.onboarding.OnboardingScreen
import com.suggestbox.ui.screens.profile.ProfileScreen
import com.suggestbox.ui.screens.submit.SubmitSuggestionScreen
import com.suggestbox.ui.screens.track.TrackScreen

/**
 * Root Navigation Compose host configured with SharedTransitionLayout, iOS-inspired slide
 * animations, and the 4-tab translucent bottom bar.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SuggestBoxNavHost(
    preferences: AppPreferences,
    onToggleDarkTheme: (Boolean) -> Unit,
    onSelectLanguage: (LanguageOption) -> Unit,
    onSelectRole: (UserRole) -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val startDestination = remember {
        when {
            preferences.isLoggedIn -> SuggestBoxRoutes.TAB_HOME
            preferences.onboardingCompleted && preferences.dpdpConsentGranted -> SuggestBoxRoutes.LOGIN
            else -> SuggestBoxRoutes.ONBOARDING
        }
    }

    val tabItems = listOf(
        BottomBarItem(SuggestBoxRoutes.TAB_HOME, stringResource(R.string.tab_home), SuggestIcons.Home),
        BottomBarItem(SuggestBoxRoutes.TAB_COMMUNITY, stringResource(R.string.tab_community), SuggestIcons.Community),
        BottomBarItem(SuggestBoxRoutes.TAB_TRACK, stringResource(R.string.tab_track), SuggestIcons.Track),
        BottomBarItem(SuggestBoxRoutes.TAB_PROFILE, stringResource(R.string.tab_profile), SuggestIcons.Profile),
    )

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val bottomBar: @Composable () -> Unit = {
        TranslucentBottomBar(
            items = tabItems,
            selectedRoute = currentRoute,
            onItemSelected = { item -> navigateToTab(item.route) },
        )
    }

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                if (targetState.destination.route in SuggestBoxRoutes.citizenTabs &&
                    initialState.destination.route in SuggestBoxRoutes.citizenTabs
                ) {
                    fadeIn()
                } else {
                    SuggestMotion.slideEnter()
                }
            },
            exitTransition = {
                if (targetState.destination.route in SuggestBoxRoutes.citizenTabs &&
                    initialState.destination.route in SuggestBoxRoutes.citizenTabs
                ) {
                    fadeOut()
                } else {
                    SuggestMotion.slideExit()
                }
            },
            popEnterTransition = {
                if (targetState.destination.route in SuggestBoxRoutes.citizenTabs &&
                    initialState.destination.route in SuggestBoxRoutes.citizenTabs
                ) {
                    fadeIn()
                } else {
                    SuggestMotion.slidePopEnter()
                }
            },
            popExitTransition = {
                if (targetState.destination.route in SuggestBoxRoutes.citizenTabs &&
                    initialState.destination.route in SuggestBoxRoutes.citizenTabs
                ) {
                    fadeOut()
                } else {
                    SuggestMotion.slidePopExit()
                }
            },
        ) {
            composable(SuggestBoxRoutes.ONBOARDING) {
                OnboardingScreen(
                    selectedLanguage = preferences.language,
                    onContinueToConsent = { navController.navigate(SuggestBoxRoutes.DPDP_CONSENT) },
                )
            }

            composable(SuggestBoxRoutes.DPDP_CONSENT) {
                DpdpConsentScreen(
                    onConsentAccepted = { navController.navigate(SuggestBoxRoutes.LOGIN) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(SuggestBoxRoutes.LOGIN) {
                LoginScreen(
                    onAuthenticated = {
                        navController.navigate(SuggestBoxRoutes.TAB_HOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(SuggestBoxRoutes.TAB_HOME) {
                HomeScreen(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    onNavigateSubmit = { navController.navigate(SuggestBoxRoutes.submitSuggestion()) },
                    onNavigateTrack = { navigateToTab(SuggestBoxRoutes.TAB_TRACK) },
                    onNavigateCommunity = { navigateToTab(SuggestBoxRoutes.TAB_COMMUNITY) },
                    onNavigateDetail = { id -> navController.navigate(SuggestBoxRoutes.suggestionDetail(id)) },
                    onNavigateOfficial = { navController.navigate(SuggestBoxRoutes.OFFICIAL_DASHBOARD) },
                    bottomBar = bottomBar,
                )
            }

            composable(SuggestBoxRoutes.TAB_COMMUNITY) {
                CommunityScreen(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    onSuggestionClick = { id -> navController.navigate(SuggestBoxRoutes.suggestionDetail(id)) },
                    bottomBar = bottomBar,
                )
            }

            composable(SuggestBoxRoutes.TAB_TRACK) {
                TrackScreen(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    onOpenSuggestion = { id -> navController.navigate(SuggestBoxRoutes.suggestionDetail(id)) },
                    onResumeDraft = { draftId -> navController.navigate(SuggestBoxRoutes.submitSuggestion(draftId)) },
                    onCreateNew = { navController.navigate(SuggestBoxRoutes.submitSuggestion()) },
                    bottomBar = bottomBar,
                )
            }

            composable(SuggestBoxRoutes.TAB_PROFILE) {
                ProfileScreen(
                    preferences = preferences,
                    onToggleDarkTheme = onToggleDarkTheme,
                    onSelectLanguage = onSelectLanguage,
                    onSelectRole = onSelectRole,
                    onOpenOfficialConsole = { navController.navigate(SuggestBoxRoutes.OFFICIAL_DASHBOARD) },
                    onOpenSuperAdmin = { navController.navigate(SuggestBoxRoutes.SUPER_ADMIN_OVERVIEW) },
                    onOpenWebPortal = { navController.navigate(SuggestBoxRoutes.WEB_PORTAL) },
                    onLoggedOut = {
                        navController.navigate(SuggestBoxRoutes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    bottomBar = bottomBar,
                )
            }

            composable(
                route = SuggestBoxRoutes.SUBMIT_SUGGESTION,
                arguments = listOf(
                    navArgument("draftId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                SubmitSuggestionScreen(
                    onBack = { navController.popBackStack() },
                    onTrackSubmitted = { createdId ->
                        navController.navigate(SuggestBoxRoutes.suggestionDetail(createdId)) {
                            popUpTo(SuggestBoxRoutes.TAB_HOME)
                        }
                    },
                )
            }

            composable(
                route = SuggestBoxRoutes.SUGGESTION_DETAIL,
                arguments = listOf(navArgument("suggestionId") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("suggestionId").orEmpty()
                SuggestionDetailScreen(
                    suggestionId = id,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    onBack = { navController.popBackStack() },
                )
            }

            // Phase 3 Official & Super Admin Flows
            composable(SuggestBoxRoutes.OFFICIAL_DASHBOARD) {
                OfficialDashboardScreen(
                    onBack = { navController.popBackStack() },
                    onOpenInbox = { navController.navigate(SuggestBoxRoutes.OFFICIAL_INBOX) },
                    onOpenBroadcast = { navController.navigate(SuggestBoxRoutes.BROADCAST_ANNOUNCEMENT) },
                )
            }

            composable(SuggestBoxRoutes.OFFICIAL_INBOX) {
                OfficialInboxScreen(
                    onBack = { navController.popBackStack() },
                    onOpenOfficialDetail = { id -> navController.navigate(SuggestBoxRoutes.officialDetail(id)) },
                )
            }

            composable(
                route = SuggestBoxRoutes.OFFICIAL_DETAIL,
                arguments = listOf(navArgument("suggestionId") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("suggestionId").orEmpty()
                OfficialDetailScreen(
                    suggestionId = id,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(SuggestBoxRoutes.BROADCAST_ANNOUNCEMENT) {
                BroadcastAnnouncementScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(SuggestBoxRoutes.SUPER_ADMIN_OVERVIEW) {
                SuperAdminScreen(
                    onBack = { navController.popBackStack() },
                    onOpenOfficialDashboard = { navController.navigate(SuggestBoxRoutes.OFFICIAL_DASHBOARD) },
                )
            }

            composable(SuggestBoxRoutes.WEB_PORTAL) {
                com.suggestbox.ui.screens.webportal.WebPortalScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
