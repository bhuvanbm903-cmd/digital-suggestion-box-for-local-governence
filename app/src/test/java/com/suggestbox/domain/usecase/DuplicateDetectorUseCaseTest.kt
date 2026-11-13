package com.suggestbox.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.suggestbox.data.mock.MockSeedData
import com.suggestbox.domain.model.GeoLocation
import com.suggestbox.domain.model.SuggestionCategory
import org.junit.Test

class DuplicateDetectorUseCaseTest {

    private val detector = DuplicateDetectorUseCase()

    @Test
    fun `detects nearby active suggestion within 150 meters`() {
        val existing = MockSeedData.initialSuggestions()
        // Within ~45m of sug-101 (12.9789, 77.6412)
        val nearby = GeoLocation(12.9792, 77.6414, "100ft Road", 40)

        val matches = detector.findPotentialDuplicates(
            title = "Stormwater drain grate sunken on 100ft Road",
            description = "The iron grate cover near metro pillar has caved in.",
            category = SuggestionCategory.ROADS,
            location = nearby,
            existingSuggestions = existing,
        )

        assertThat(matches).isNotEmpty()
        assertThat(matches.first().suggestion.id).isEqualTo("sug-101")
        assertThat(matches.first().distanceMeters).isAtMost(150)
    }

    @Test
    fun `ignores resolved suggestions even if nearby`() {
        val existing = MockSeedData.initialSuggestions()
        // Near resolved sug-103 (12.9801, 77.6442)
        val nearResolved = GeoLocation(12.9801, 77.6442, "6th Cross", 10)

        val matches = detector.findPotentialDuplicates(
            title = "Irregular dry waste collection",
            description = "Dry waste auto tipper skipped 6th cross lane again.",
            category = SuggestionCategory.SANITATION,
            location = nearResolved,
            existingSuggestions = existing,
        )

        assertThat(matches.map { it.suggestion.id }).doesNotContain("sug-103")
    }
}
