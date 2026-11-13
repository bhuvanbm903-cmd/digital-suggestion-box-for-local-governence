package com.suggestbox.ui.screens.community

import com.google.common.truth.Truth.assertThat
import com.suggestbox.data.mock.MockGovernanceRepository
import com.suggestbox.domain.model.SubmissionKind
import com.suggestbox.domain.model.SuggestionCategory
import com.suggestbox.testing.FakeAuthRepository
import com.suggestbox.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class CommunityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `filtering by category and kind narrows community list`() = runTest {
        val repo = MockGovernanceRepository(FakeAuthRepository())
        val viewModel = CommunityViewModel(repo)

        val initial = viewModel.uiState.first { !it.isLoading }
        assertThat(initial.items).hasSize(4)

        viewModel.setKindFilter(SubmissionKind.IDEA)
        val ideasOnly = viewModel.uiState.first { it.selectedKind == SubmissionKind.IDEA }
        assertThat(ideasOnly.items).hasSize(1)
        assertThat(ideasOnly.items.first().category).isEqualTo(SuggestionCategory.PARKS)

        viewModel.clearAllFilters()
        val reset = viewModel.uiState.first { it.selectedKind == null }
        assertThat(reset.items).hasSize(4)
    }

    @Test
    fun `toggleUpvote updates suggestion upvote count`() = runTest {
        val repo = MockGovernanceRepository(FakeAuthRepository())
        val viewModel = CommunityViewModel(repo)

        val before = viewModel.uiState.first { !it.isLoading }
            .items.first { it.id == "sug-102" }
        val initialVotes = before.upvoteCount

        viewModel.toggleUpvote("sug-102")
        val after = viewModel.uiState.first { state ->
            state.items.firstOrNull { it.id == "sug-102" }?.upvoteCount == initialVotes + 1
        }
        assertThat(after.items.first { it.id == "sug-102" }.upvotedByCurrentUser).isTrue()
    }
}
