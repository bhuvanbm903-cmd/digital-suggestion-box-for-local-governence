package com.suggestbox.ui.screens.official

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suggestbox.domain.model.WardInfo
import com.suggestbox.domain.repository.AdminRepository
import com.suggestbox.domain.repository.SuggestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BroadcastUiState(
    val wards: List<WardInfo> = emptyList(),
    val selectedWardId: String = "ward-14",
    val department: String = "Municipal Administration",
    val headline: String = "",
    val body: String = "",
    val isPublishing: Boolean = false,
    val publishedSuccess: Boolean = false,
)

@HiltViewModel
class BroadcastAnnouncementViewModel @Inject constructor(
    suggestionRepository: SuggestionRepository,
    private val adminRepository: AdminRepository,
) : ViewModel() {

    private val formState = MutableStateFlow(BroadcastUiState())

    val departments = listOf(
        "Municipal Administration",
        "Water Supply & Sewerage",
        "Public Works & Roads",
        "Solid Waste Management",
        "Health & Sanitation",
    )

    val uiState: StateFlow<BroadcastUiState> = combine(
        suggestionRepository.observeWards(),
        formState,
    ) { wards, state ->
        state.copy(wards = wards)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BroadcastUiState(),
    )

    fun selectWard(wardId: String) {
        formState.value = formState.value.copy(selectedWardId = wardId)
    }

    fun selectDepartment(dept: String) {
        formState.value = formState.value.copy(department = dept)
    }

    fun onHeadlineChange(value: String) {
        formState.value = formState.value.copy(headline = value)
    }

    fun onBodyChange(value: String) {
        formState.value = formState.value.copy(body = value)
    }

    fun publish() {
        val s = formState.value
        if (s.headline.isBlank() || s.body.isBlank()) return
        viewModelScope.launch {
            formState.value = formState.value.copy(isPublishing = true)
            adminRepository.broadcastAnnouncement(
                wardId = s.selectedWardId,
                title = s.headline,
                body = s.body,
                department = s.department,
            ).onSuccess {
                formState.value = formState.value.copy(isPublishing = false, publishedSuccess = true)
            }.onFailure {
                formState.value = formState.value.copy(isPublishing = false)
            }
        }
    }
}
