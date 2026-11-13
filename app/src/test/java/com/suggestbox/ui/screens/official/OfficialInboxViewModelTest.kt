package com.suggestbox.ui.screens.official

import com.google.common.truth.Truth.assertThat
import com.suggestbox.data.mock.MockGovernanceRepository
import com.suggestbox.testing.FakeAuthRepository
import com.suggestbox.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class OfficialInboxViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `overdue segment returns only overdue tickets`() = runTest {
        val repo = MockGovernanceRepository(FakeAuthRepository())
        val viewModel = OfficialInboxViewModel(repo)

        viewModel.onSegmentChange(InboxSegment.OVERDUE.ordinal)
        val overdueState = viewModel.uiState.first { it.segment == InboxSegment.OVERDUE }

        assertThat(overdueState.items).isNotEmpty()
        assertThat(overdueState.items.all { it.isOverdue }).isTrue()
    }

    @Test
    fun `unassigned segment returns tickets without assigned department`() = runTest {
        val repo = MockGovernanceRepository(FakeAuthRepository())
        val viewModel = OfficialInboxViewModel(repo)

        viewModel.onSegmentChange(InboxSegment.UNASSIGNED.ordinal)
        val unassignedState = viewModel.uiState.first { it.segment == InboxSegment.UNASSIGNED }

        assertThat(unassignedState.items).hasSize(1)
        assertThat(unassignedState.items.first().id).isEqualTo("sug-104")
    }
}
