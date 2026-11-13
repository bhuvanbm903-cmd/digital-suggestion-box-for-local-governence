package com.suggestbox.core.designsystem

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.suggestbox.core.designsystem.components.EmptyState
import com.suggestbox.core.designsystem.components.PrimaryButton
import com.suggestbox.core.designsystem.components.SegmentedControl
import com.suggestbox.core.designsystem.components.StatusBadge
import com.suggestbox.core.designsystem.components.StatusTone
import com.suggestbox.core.designsystem.icons.SuggestIcons
import com.suggestbox.core.designsystem.theme.SuggestBoxTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DesignSystemComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun primaryButton_invokesCallbackOnClick() {
        var clicked = false
        composeRule.setContent {
            SuggestBoxTheme {
                PrimaryButton(text = "Submit Suggestion", onClick = { clicked = true })
            }
        }
        composeRule.onNodeWithText("Submit Suggestion").assertIsDisplayed().performClick()
        assertTrue(clicked)
    }

    @Test
    fun segmentedControl_updatesSelection() {
        var selectedIndex = 0
        composeRule.setContent {
            SuggestBoxTheme {
                SegmentedControl(
                    options = listOf("Trending", "Recent", "Nearby"),
                    selectedIndex = selectedIndex,
                    onOptionSelected = { selectedIndex = it },
                )
            }
        }
        composeRule.onNodeWithText("Trending").assertIsSelected()
        composeRule.onNodeWithText("Nearby").performClick()
        assertEquals(2, selectedIndex)
    }

    @Test
    fun statusBadge_and_emptyState_renderCleanly() {
        composeRule.setContent {
            SuggestBoxTheme {
                StatusBadge(label = "In Progress", tone = StatusTone.IN_PROGRESS)
                EmptyState(
                    icon = SuggestIcons.Track,
                    title = "No Active Submissions",
                    description = "Submit a suggestion to begin tracking.",
                )
            }
        }
        composeRule.onNodeWithText("In Progress").assertIsDisplayed()
        composeRule.onNodeWithText("No Active Submissions").assertIsDisplayed()
    }
}
