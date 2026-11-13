package com.suggestbox.ui.screens.official

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import javax.inject.Inject

enum class InboxSegment { ALL, OVERDUE, UNASSIGNED, RESOLVED }

data class OfficialInboxUiState(
    val query: String = "",
    val segment: InboxSegment = InboxSegment.ALL,
    val selectedCategory: SuggestionCategory? = null,
    val items: List<Suggestion> = emptyList(),
)

@HiltViewModel
class OfficialInboxViewModel @Inject constructor(
    suggestionRepository: SuggestionRepository,
) : ViewModel() {

    private val filters = MutableStateFlow(OfficialInboxUiState())

    val uiState: StateFlow<OfficialInboxUiState> = combine(
        suggestionRepository.observeSuggestions(),
        filters,
    ) { all, state ->
        val filtered = all
            .asSequence()
            .filter { item ->
                when (state.segment) {
                    InboxSegment.ALL -> true
                    InboxSegment.OVERDUE -> item.isOverdue
                    InboxSegment.UNASSIGNED -> item.assignedDepartment.isNullOrBlank()
                    InboxSegment.RESOLVED -> item.status == SuggestionStatus.RESOLVED
                }
            }
            .filter { item -> state.selectedCategory == null || item.category == state.selectedCategory }
            .filter { item ->
                val q = state.query.trim()
                q.isEmpty() ||
                    item.title.contains(q, ignoreCase = true) ||
                    item.trackingId.contains(q, ignoreCase = true) ||
                    item.location.addressLabel.contains(q, ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<Suggestion> { it.isOverdue }
                    .thenByDescending { it.priority.ordinal }
                    .thenByDescending { it.createdAtMillis },
            )
            .toList()

        state.copy(items = filtered)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OfficialInboxUiState(),
    )

    fun onQueryChange(value: String) {
        filters.value = filters.value.copy(query = value)
    }

    fun onSegmentChange(index: Int) {
        filters.value = filters.value.copy(
            segment = InboxSegment.entries.getOrElse(index) { InboxSegment.ALL },
        )
    }

    fun toggleCategory(category: SuggestionCategory) {
        val current = filters.value.selectedCategory
        filters.value = filters.value.copy(
            selectedCategory = if (current == category) null else category,
        )
    }
}
