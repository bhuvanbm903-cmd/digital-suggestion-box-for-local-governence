package com.suggestbox.ui.screens.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suggestbox.domain.model.Suggestion
import com.suggestbox.domain.model.SuggestionDraft
import com.suggestbox.domain.repository.AuthRepository
import com.suggestbox.domain.repository.DraftRepository
import com.suggestbox.domain.repository.SuggestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrackUiState(
    val lookupQuery: String = "",
    val lookupNotFound: Boolean = false,
    val foundSuggestionId: String? = null,
    val mySubmissions: List<Suggestion> = emptyList(),
    val offlineDrafts: List<SuggestionDraft> = emptyList(),
    val syncedCountNotice: Int? = null,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TrackViewModel @Inject constructor(
    private val suggestionRepository: SuggestionRepository,
    private val draftRepository: DraftRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val lookupQuery = MutableStateFlow("")
    private val lookupError = MutableStateFlow(false)
    private val foundId = MutableStateFlow<String?>(null)
    private val syncedNotice = MutableStateFlow<Int?>(null)

    private val mySuggestionsFlow = authRepository.currentUser.flatMapLatest { user ->
        suggestionRepository.observeMySuggestions(user?.id ?: "user-citizen-1")
    }

    val uiState: StateFlow<TrackUiState> = combine(
        lookupQuery,
        lookupError,
        foundId,
        mySuggestionsFlow,
        draftRepository.observeDrafts(),
        syncedNotice,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        TrackUiState(
            lookupQuery = values[0] as String,
            lookupNotFound = values[1] as Boolean,
            foundSuggestionId = values[2] as String?,
            mySubmissions = values[3] as List<Suggestion>,
            offlineDrafts = values[4] as List<SuggestionDraft>,
            syncedCountNotice = values[5] as Int?,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrackUiState(),
    )

    fun onLookupQueryChange(value: String) {
        lookupQuery.value = value
        lookupError.value = false
    }

    fun findByTrackingId() {
        val code = lookupQuery.value.trim()
        if (code.isEmpty()) return
        viewModelScope.launch {
            val match = suggestionRepository.findByTrackingId(code)
            if (match != null) {
                foundId.value = match.id
            } else {
                lookupError.value = true
            }
        }
    }

    fun consumeFoundSuggestion() {
        foundId.value = null
    }

    fun toggleUpvote(suggestionId: String) {
        viewModelScope.launch {
            suggestionRepository.toggleUpvote(suggestionId)
        }
    }

    fun syncDraftsNow() {
        viewModelScope.launch {
            val count = draftRepository.syncPendingDrafts()
            syncedNotice.value = count
        }
    }

    fun deleteDraft(localId: String) {
        viewModelScope.launch {
            draftRepository.deleteDraft(localId)
        }
    }
}
