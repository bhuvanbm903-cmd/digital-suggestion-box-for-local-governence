package com.suggestbox.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suggestbox.domain.model.Announcement
import com.suggestbox.domain.model.Suggestion
import com.suggestbox.domain.model.UserRole
import com.suggestbox.domain.model.WardInfo
import com.suggestbox.domain.repository.PreferencesRepository
import com.suggestbox.domain.repository.SuggestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val activeWard: WardInfo? = null,
    val availableWards: List<WardInfo> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    val trendingSuggestions: List<Suggestion> = emptyList(),
    val activeRole: UserRole = UserRole.CITIZEN,
    val isLoading: Boolean = true,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel @Inject constructor(
    private val suggestionRepository: SuggestionRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val announcementsFlow = preferencesRepository.preferences.flatMapLatest { prefs ->
        suggestionRepository.observeAnnouncements(prefs.selectedWardId)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        preferencesRepository.preferences,
        suggestionRepository.observeWards(),
        announcementsFlow,
        suggestionRepository.observeSuggestions(),
    ) { prefs, wards, announcements, suggestions ->
        val ward = wards.firstOrNull { it.id == prefs.selectedWardId } ?: wards.firstOrNull()
        val trending = suggestions
            .sortedByDescending { it.upvoteCount }
            .take(5)
        HomeUiState(
            activeWard = ward,
            availableWards = wards,
            announcements = announcements,
            trendingSuggestions = trending,
            activeRole = prefs.previewRole,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun selectWard(wardId: String) {
        viewModelScope.launch {
            preferencesRepository.setSelectedWard(wardId)
        }
    }

    fun toggleUpvote(suggestionId: String) {
        viewModelScope.launch {
            suggestionRepository.toggleUpvote(suggestionId)
        }
    }
}
