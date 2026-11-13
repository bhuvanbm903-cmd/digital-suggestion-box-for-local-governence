package com.suggestbox.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suggestbox.domain.model.Suggestion
import com.suggestbox.domain.repository.SuggestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val suggestion: Suggestion? = null,
    val commentDraft: String = "",
    val selectedStars: Int = 5,
    val ratingFeedback: String = "",
    val isSubmittingComment: Boolean = false,
    val isSubmittingRating: Boolean = false,
)

@HiltViewModel
class SuggestionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val suggestionRepository: SuggestionRepository,
) : ViewModel() {

    private val suggestionId: String = checkNotNull(savedStateHandle["suggestionId"])
    private val localDrafts = MutableStateFlow(DetailUiState())

    val uiState: StateFlow<DetailUiState> = combine(
        suggestionRepository.observeSuggestionById(suggestionId),
        localDrafts,
    ) { suggestion, drafts ->
        drafts.copy(suggestion = suggestion)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState(),
    )

    fun toggleUpvote() {
        viewModelScope.launch {
            suggestionRepository.toggleUpvote(suggestionId)
        }
    }

    fun onCommentDraftChange(value: String) {
        localDrafts.value = localDrafts.value.copy(commentDraft = value)
    }

    fun postComment() {
        val body = localDrafts.value.commentDraft.trim()
        if (body.isEmpty()) return
        viewModelScope.launch {
            localDrafts.value = localDrafts.value.copy(isSubmittingComment = true)
            suggestionRepository.addComment(suggestionId, body)
                .onSuccess {
                    localDrafts.value = localDrafts.value.copy(commentDraft = "", isSubmittingComment = false)
                }
                .onFailure {
                    localDrafts.value = localDrafts.value.copy(isSubmittingComment = false)
                }
        }
    }

    fun selectStars(stars: Int) {
        localDrafts.value = localDrafts.value.copy(selectedStars = stars.coerceIn(1, 5))
    }

    fun onRatingFeedbackChange(value: String) {
        localDrafts.value = localDrafts.value.copy(ratingFeedback = value)
    }

    fun submitRating() {
        val current = localDrafts.value
        viewModelScope.launch {
            localDrafts.value = current.copy(isSubmittingRating = true)
            suggestionRepository.submitRating(
                suggestionId = suggestionId,
                stars = current.selectedStars,
                feedback = current.ratingFeedback,
            )
            localDrafts.value = localDrafts.value.copy(isSubmittingRating = false)
        }
    }
}
