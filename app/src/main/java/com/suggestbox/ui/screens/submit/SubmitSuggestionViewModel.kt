package com.suggestbox.ui.screens.submit

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suggestbox.data.media.ImageCompressor
import com.suggestbox.domain.model.GeoLocation
import com.suggestbox.domain.model.SubmissionKind
import com.suggestbox.domain.model.Suggestion
import com.suggestbox.domain.model.SuggestionCategory
import com.suggestbox.domain.model.SuggestionDraft
import com.suggestbox.domain.repository.DraftRepository
import com.suggestbox.domain.repository.PreferencesRepository
import com.suggestbox.domain.repository.SuggestionRepository
import com.suggestbox.domain.usecase.DuplicateDetectorUseCase
import com.suggestbox.domain.usecase.DuplicateMatch
import com.suggestbox.domain.usecase.SuggestionValidator
import com.suggestbox.domain.usecase.ValidationError
import com.suggestbox.domain.usecase.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * UI state backing the 5-step citizen submission wizard.
 */
data class SubmitUiState(
    val draftId: String = "draft-${UUID.randomUUID().toString().take(8)}",
    val step: Int = 1,
    val totalSteps: Int = 5,
    val kind: SubmissionKind = SubmissionKind.ISSUE,
    val category: SuggestionCategory = SuggestionCategory.ROADS,
    val title: String = "",
    val description: String = "",
    val mediaUris: List<String> = emptyList(),
    val location: GeoLocation = GeoLocation(12.9784, 77.6408, "100ft Road, Ward 14"),
    val isAnonymous: Boolean = false,
    val duplicateMatches: List<DuplicateMatch> = emptyList(),
    val validationError: ValidationError? = null,
    val isCompressingMedia: Boolean = false,
    val isSubmitting: Boolean = false,
    val draftSavedToast: Boolean = false,
    val submittedSuggestion: Suggestion? = null,
)

@HiltViewModel
class SubmitSuggestionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val suggestionRepository: SuggestionRepository,
    private val draftRepository: DraftRepository,
    private val preferencesRepository: PreferencesRepository,
    private val validator: SuggestionValidator,
    private val duplicateDetector: DuplicateDetectorUseCase,
    private val imageCompressor: ImageCompressor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubmitUiState())
    val uiState: StateFlow<SubmitUiState> = _uiState.asStateFlow()

    private var cachedExisting: List<Suggestion> = emptyList()

    init {
        val resumeDraftId: String? = savedStateHandle["draftId"]
        viewModelScope.launch {
            cachedExisting = suggestionRepository.observeSuggestions().first()
            if (!resumeDraftId.isNullOrBlank()) {
                val draft = draftRepository.observeDrafts().first().firstOrNull { it.localId == resumeDraftId }
                if (draft != null) {
                    _uiState.value = _uiState.value.copy(
                        draftId = draft.localId,
                        kind = draft.kind,
                        category = draft.category,
                        title = draft.title,
                        description = draft.description,
                        mediaUris = draft.localMediaUris,
                        location = draft.location ?: _uiState.value.location,
                        isAnonymous = draft.isAnonymous,
                    )
                }
            } else {
                val prefs = preferencesRepository.preferences.first()
                val wards = suggestionRepository.observeWards().first()
                val ward = wards.firstOrNull { it.id == prefs.selectedWardId } ?: wards.firstOrNull()
                if (ward != null) {
                    _uiState.value = _uiState.value.copy(
                        location = GeoLocation(ward.centerLat, ward.centerLng, ward.name),
                    )
                }
            }
        }
    }

    fun setKind(kind: SubmissionKind) {
        _uiState.value = _uiState.value.copy(kind = kind)
    }

    fun setCategory(category: SuggestionCategory) {
        _uiState.value = _uiState.value.copy(category = category)
        refreshDuplicates()
    }

    fun onTitleChange(value: String) {
        _uiState.value = _uiState.value.copy(title = value, validationError = null)
        refreshDuplicates()
    }

    fun onDescriptionChange(value: String) {
        _uiState.value = _uiState.value.copy(description = value, validationError = null)
        refreshDuplicates()
    }

    fun appendVoiceTranscript(transcript: String) {
        val clean = transcript.trim()
        if (clean.isEmpty()) return
        val existingTitle = _uiState.value.title.trim()
        val existingDesc = _uiState.value.description.trim()

        if (existingTitle.isBlank()) {
            val words = clean.split("\\s+".toRegex())
            val titleCandidate = words.take(8).joinToString(" ")
            val formattedTitle = titleCandidate.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
            _uiState.value = _uiState.value.copy(
                title = formattedTitle,
                description = if (existingDesc.isBlank()) clean else "$existingDesc. $clean",
                validationError = null,
            )
        } else {
            val nextDesc = if (existingDesc.isBlank()) clean else "$existingDesc. $clean"
            _uiState.value = _uiState.value.copy(description = nextDesc, validationError = null)
        }
        refreshDuplicates()
    }

    fun addMediaUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCompressingMedia = true)
            val compressed = uris.map { imageCompressor.compressIfImage(it) }
            val merged = (_uiState.value.mediaUris + compressed).distinct().take(4)
            _uiState.value = _uiState.value.copy(mediaUris = merged, isCompressingMedia = false)
        }
    }

    fun removeMedia(uri: String) {
        _uiState.value = _uiState.value.copy(
            mediaUris = _uiState.value.mediaUris.filterNot { it == uri },
        )
    }

    fun updateCoordinates(lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(
            location = _uiState.value.location.copy(latitude = lat, longitude = lng),
        )
        refreshDuplicates()
    }

    fun updateAddressLabel(label: String) {
        _uiState.value = _uiState.value.copy(
            location = _uiState.value.location.copy(addressLabel = label),
        )
    }

    fun setAnonymous(anonymous: Boolean) {
        _uiState.value = _uiState.value.copy(isAnonymous = anonymous)
    }

    fun nextStep() {
        val current = _uiState.value
        if (current.step == 2) {
            val result = validator.validateSuggestion(
                title = current.title,
                description = current.description,
                checkRateLimit = false,
            )
            if (result is ValidationResult.Invalid) {
                _uiState.value = current.copy(validationError = result.errorReason)
                return
            }
        }
        if (current.step < current.totalSteps) {
            _uiState.value = current.copy(step = current.step + 1, validationError = null)
        }
    }

    fun previousStep() {
        val current = _uiState.value
        if (current.step > 1) {
            _uiState.value = current.copy(step = current.step - 1, validationError = null)
        }
    }

    fun upvoteExistingDuplicate(suggestionId: String) {
        viewModelScope.launch {
            suggestionRepository.toggleUpvote(suggestionId)
        }
    }

    fun saveOfflineDraft(pendingSync: Boolean = false) {
        val s = _uiState.value
        viewModelScope.launch {
            draftRepository.saveDraft(
                SuggestionDraft(
                    localId = s.draftId,
                    kind = s.kind,
                    category = s.category,
                    title = s.title,
                    description = s.description,
                    localMediaUris = s.mediaUris,
                    location = s.location,
                    isAnonymous = s.isAnonymous,
                    updatedAtMillis = System.currentTimeMillis(),
                    pendingSync = pendingSync,
                ),
            )
            _uiState.value = _uiState.value.copy(draftSavedToast = true)
        }
    }

    fun consumeDraftSavedFlag() {
        _uiState.value = _uiState.value.copy(draftSavedToast = false)
    }

    fun submit() {
        val s = _uiState.value
        val validation = validator.validateSuggestion(
            title = s.title,
            description = s.description,
            checkRateLimit = true,
        )
        if (validation is ValidationResult.Invalid) {
            _uiState.value = s.copy(validationError = validation.errorReason)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, validationError = null)
            val draft = SuggestionDraft(
                localId = s.draftId,
                kind = s.kind,
                category = s.category,
                title = s.title,
                description = s.description,
                localMediaUris = s.mediaUris,
                location = s.location,
                isAnonymous = s.isAnonymous,
                updatedAtMillis = System.currentTimeMillis(),
                pendingSync = false,
            )
            suggestionRepository.submitSuggestion(draft)
                .onSuccess { created ->
                    validator.recordSuccessfulSubmission()
                    draftRepository.deleteDraft(s.draftId)
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submittedSuggestion = created,
                    )
                }
                .onFailure {
                    draftRepository.saveDraft(draft.copy(pendingSync = true))
                    _uiState.value = _uiState.value.copy(isSubmitting = false, draftSavedToast = true)
                }
        }
    }

    private fun refreshDuplicates() {
        val s = _uiState.value
        val found = duplicateDetector.findPotentialDuplicates(
            title = s.title,
            description = s.description,
            category = s.category,
            location = s.location,
            existingSuggestions = cachedExisting,
        )
        _uiState.value = s.copy(duplicateMatches = found)
    }
}
