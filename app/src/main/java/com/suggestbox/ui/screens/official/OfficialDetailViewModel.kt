package com.suggestbox.ui.screens.official

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suggestbox.domain.model.Suggestion
import com.suggestbox.domain.model.SuggestionPriority
import com.suggestbox.domain.model.SuggestionStatus
import com.suggestbox.domain.repository.AdminRepository
import com.suggestbox.domain.repository.SuggestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OfficialDetailUiState(
    val suggestion: Suggestion? = null,
    val otherActiveTrackingIds: List<String> = emptyList(),
    val department: String = "Public Works & Roads",
    val officerName: String = "Meera Deshmukh (AEE)",
    val priority: SuggestionPriority = SuggestionPriority.MEDIUM,
    val status: SuggestionStatus = SuggestionStatus.UNDER_REVIEW,
    val officialResponse: String = "",
    val attachProofChecked: Boolean = false,
    val mergeTrackingId: String = "",
    val actionSavedToast: Boolean = false,
)

@HiltViewModel
class OfficialDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val suggestionRepository: SuggestionRepository,
    private val adminRepository: AdminRepository,
) : ViewModel() {

    private val suggestionId: String = checkNotNull(savedStateHandle["suggestionId"])
    private val formState = MutableStateFlow(OfficialDetailUiState())

    val departments = listOf(
        "Public Works & Roads",
        "Water Supply & Sewerage",
        "Solid Waste Management",
        "Electrical & Parks",
        "Health & Sanitation",
        "Safety & Traffic",
        "Education",
    )

    init {
        viewModelScope.launch {
            val item = suggestionRepository.observeSuggestionById(suggestionId).first()
            val others = suggestionRepository.observeSuggestions().first()
                .filter { it.id != suggestionId && it.status != SuggestionStatus.REJECTED }
                .map { it.trackingId }
            if (item != null) {
                formState.value = formState.value.copy(
                    department = item.assignedDepartment ?: departments.first(),
                    officerName = item.assignedOfficerName ?: "Meera Deshmukh (AEE)",
                    priority = item.priority,
                    status = item.status,
                    officialResponse = item.officialResponse.orEmpty(),
                    otherActiveTrackingIds = others,
                )
            }
        }
    }

    val uiState: StateFlow<OfficialDetailUiState> = combine(
        suggestionRepository.observeSuggestionById(suggestionId),
        formState,
    ) { item, state ->
        state.copy(suggestion = item)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OfficialDetailUiState(),
    )

    fun setDepartment(dept: String) {
        formState.value = formState.value.copy(department = dept)
    }

    fun setOfficerName(name: String) {
        formState.value = formState.value.copy(officerName = name)
    }

    fun setPriority(priority: SuggestionPriority) {
        formState.value = formState.value.copy(priority = priority)
    }

    fun setStatus(status: SuggestionStatus) {
        formState.value = formState.value.copy(status = status)
    }

    fun setOfficialResponse(response: String) {
        formState.value = formState.value.copy(officialResponse = response)
    }

    fun setAttachProof(checked: Boolean) {
        formState.value = formState.value.copy(attachProofChecked = checked)
    }

    fun setMergeTrackingId(id: String) {
        formState.value = formState.value.copy(mergeTrackingId = id)
    }

    fun saveAssignment() {
        val s = formState.value
        viewModelScope.launch {
            adminRepository.assignDepartmentAndPriority(
                suggestionId = suggestionId,
                department = s.department,
                officerName = s.officerName.ifBlank { "Assigned Ward Officer" },
                priority = s.priority,
            )
            formState.value = formState.value.copy(actionSavedToast = true)
        }
    }

    fun updateStatusAndResponse() {
        val s = formState.value
        val note = buildString {
            append(s.officialResponse.trim())
            if (s.attachProofChecked && s.status == SuggestionStatus.RESOLVED) {
                if (isNotEmpty()) append(" ")
                append("[Geotagged resolution photo proof verified]")
            }
        }
        viewModelScope.launch {
            adminRepository.updateSuggestionStatus(
                suggestionId = suggestionId,
                newStatus = s.status,
                officialResponse = note,
            )
            formState.value = formState.value.copy(actionSavedToast = true)
        }
    }

    fun mergeAsDuplicate() {
        val targetId = formState.value.mergeTrackingId.trim()
        if (targetId.isEmpty()) return
        viewModelScope.launch {
            adminRepository.markDuplicateAndMerge(suggestionId, targetId)
            formState.value = formState.value.copy(actionSavedToast = true)
        }
    }

    fun consumeToast() {
        formState.value = formState.value.copy(actionSavedToast = false)
    }
}
