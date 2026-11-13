package com.suggestbox.ui.screens.official

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suggestbox.data.export.ReportExporter
import com.suggestbox.domain.model.AdminAnalytics
import com.suggestbox.domain.model.Suggestion
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

data class OfficialDashboardUiState(
    val analytics: AdminAnalytics = AdminAnalytics(0, emptyMap(), emptyMap(), 0, 0.0),
    val exportedUri: Uri? = null,
    val exportedMimeType: String = "text/csv",
    val exportMessage: String? = null,
)

@HiltViewModel
class OfficialDashboardViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val suggestionRepository: SuggestionRepository,
    private val reportExporter: ReportExporter,
) : ViewModel() {

    private val exportState = MutableStateFlow<Triple<Uri?, String, String?>>(Triple(null, "text/csv", null))

    val uiState: StateFlow<OfficialDashboardUiState> = combine(
        adminRepository.observeAnalytics(),
        exportState,
    ) { analytics, (uri, mime, msg) ->
        OfficialDashboardUiState(
            analytics = analytics,
            exportedUri = uri,
            exportedMimeType = mime,
            exportMessage = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OfficialDashboardUiState(),
    )

    fun exportCsv() {
        viewModelScope.launch {
            val csvResult = adminRepository.exportSummaryReportCsv()
            csvResult.onSuccess { csv ->
                val uri = reportExporter.exportCsvFile(csv)
                exportState.value = Triple(uri, "text/csv", "suggestbox_report.csv")
            }
        }
    }

    fun exportPdf() {
        viewModelScope.launch {
            val analytics = adminRepository.observeAnalytics().first()
            val list: List<Suggestion> = suggestionRepository.observeSuggestions().first()
            val uri = reportExporter.exportPdfSummary(analytics, list)
            exportState.value = Triple(uri, "application/pdf", "suggestbox_summary.pdf")
        }
    }

    fun consumeExportEvent() {
        exportState.value = exportState.value.copy(first = null, third = null)
    }
}
