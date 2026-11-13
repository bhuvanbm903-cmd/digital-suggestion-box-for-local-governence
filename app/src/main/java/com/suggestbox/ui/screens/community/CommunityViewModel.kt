package com.suggestbox.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suggestbox.domain.model.SubmissionKind
import com.suggestbox.domain.model.Suggestion
import com.suggestbox.domain.model.SuggestionCategory
import com.suggestbox.domain.model.SuggestionStatus
import com.suggestbox.domain.repository.SuggestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CommunitySortSegment { TRENDING, RECENT, NEARBY }
enum class DistanceFilter(val maxMeters: Int?) { ALL(null), WITHIN_500M(500), WITHIN_2KM(2000) }

data class CommunityUiState(
    val query: String = "",
    val segment: CommunitySortSegment = CommunitySortSegment.TRENDING,
    val selectedCategory: SuggestionCategory? = null,
    val selectedStatus: SuggestionStatus? = null,
    val selectedKind: SubmissionKind? = null,
    val distanceFilter: DistanceFilter = DistanceFilter.ALL,
    val items: List<Suggestion> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val suggestionRepository: SuggestionRepository,
) : ViewModel() {

    private val filterState = MutableStateFlow(CommunityUiState(isLoading = false))

    val uiState: StateFlow<CommunityUiState> = combine(
        suggestionRepository.observeSuggestions(),
        filterState,
    ) { all, filters ->
        val filtered = all
            .asSequence()
            .filter { item ->
                filters.selectedCategory == null || item.category == filters.selectedCategory
            }
            .filter { item ->
                filters.selectedStatus == null || item.status == filters.selectedStatus
            }
            .filter { item ->
                filters.selectedKind == null || item.kind == filters.selectedKind
            }
            .filter { item ->
                val max = filters.distanceFilter.maxMeters
                max == null || item.location.distanceMeters <= max
            }
            .filter { item ->
                val q = filters.query.trim()
                q.isEmpty() ||
                    item.title.contains(q, ignoreCase = true) ||
                    item.description.contains(q, ignoreCase = true) ||
                    item.trackingId.contains(q, ignoreCase = true) ||
                    item.location.addressLabel.contains(q, ignoreCase = true)
            }
            .toList()

        val sorted = when (filters.segment) {
            CommunitySortSegment.TRENDING -> filtered.sortedByDescending { it.upvoteCount }
            CommunitySortSegment.RECENT -> filtered.sortedByDescending { it.createdAtMillis }
            CommunitySortSegment.NEARBY -> filtered.sortedBy { it.location.distanceMeters }
        }

        filters.copy(items = sorted, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CommunityUiState(),
    )

    fun onQueryChange(query: String) {
        filterState.value = filterState.value.copy(query = query)
    }

    fun onSegmentChange(index: Int) {
        val seg = CommunitySortSegment.entries.getOrElse(index) { CommunitySortSegment.TRENDING }
        filterState.value = filterState.value.copy(segment = seg)
    }

    fun toggleCategory(category: SuggestionCategory) {
        val current = filterState.value.selectedCategory
        filterState.value = filterState.value.copy(selectedCategory = if (current == category) null else category)
    }

    fun setStatusFilter(status: SuggestionStatus?) {
        filterState.value = filterState.value.copy(selectedStatus = status)
    }

    fun setDistanceFilter(distance: DistanceFilter) {
        filterState.value = filterState.value.copy(distanceFilter = distance)
    }

    fun setKindFilter(kind: SubmissionKind?) {
        filterState.value = filterState.value.copy(selectedKind = kind)
    }

    fun clearAllFilters() {
        filterState.value = CommunityUiState(isLoading = false)
    }

    fun toggleUpvote(suggestionId: String) {
        viewModelScope.launch {
            suggestionRepository.toggleUpvote(suggestionId)
        }
    }
}
