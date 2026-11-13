package com.suggestbox.domain.usecase

import com.suggestbox.domain.model.GeoLocation
import com.suggestbox.domain.model.Suggestion
import com.suggestbox.domain.model.SuggestionCategory
import com.suggestbox.domain.model.SuggestionStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Candidate duplicate detected within a 150m spatial radius with semantic keyword overlap,
 * implementing the synopsis deduplication pipeline (`ST_DWithin(150m)` + text similarity).
 */
data class DuplicateMatch(
    val suggestion: Suggestion,
    val distanceMeters: Int,
    val similarityScore: Double,
)

/**
 * Evaluates spatial proximity and token similarity to prevent redundant duplicate tickets
 * and encourage citizens to upvote existing active reports instead.
 */
@Singleton
class DuplicateDetectorUseCase @Inject constructor() {

    fun findPotentialDuplicates(
        title: String,
        description: String,
        category: SuggestionCategory,
        location: GeoLocation?,
        existingSuggestions: List<Suggestion>,
    ): List<DuplicateMatch> {
        val cleanInput = "$title $description".trim()
        if (cleanInput.length < 6) return emptyList()

        val inputTokens = tokenize(cleanInput)
        if (inputTokens.isEmpty()) return emptyList()

        return existingSuggestions
            .asSequence()
            .filter { it.status != SuggestionStatus.RESOLVED && it.status != SuggestionStatus.REJECTED }
            .mapNotNull { candidate ->
                val distance = if (location != null) {
                    haversineMeters(
                        location.latitude,
                        location.longitude,
                        candidate.location.latitude,
                        candidate.location.longitude,
                    )
                } else {
                    candidate.location.distanceMeters
                }

                val candidateTokens = tokenize("${candidate.title} ${candidate.description}")
                val jaccard = jaccardSimilarity(inputTokens, candidateTokens)
                val sameCategoryBoost = if (candidate.category == category) 0.18 else 0.0
                val score = (jaccard + sameCategoryBoost).coerceAtMost(1.0)

                val isNearbyMatch = distance <= RADIUS_METERS && (jaccard >= 0.15 || candidate.category == category)
                val isHighTextMatch = score >= 0.42

                if (isNearbyMatch || isHighTextMatch) {
                    DuplicateMatch(
                        suggestion = candidate,
                        distanceMeters = distance,
                        similarityScore = score,
                    )
                } else {
                    null
                }
            }
            .sortedByDescending { it.similarityScore }
            .take(2)
            .toList()
    }

    private fun tokenize(text: String): Set<String> {
        return text.lowercase()
            .split(Regex("[^a-z0-9\\u0900-\\u097F\\u0C80-\\u0CFF]+"))
            .filter { it.length >= 3 && it !in stopWords }
            .toSet()
    }

    private fun jaccardSimilarity(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val intersection = a.intersect(b).size.toDouble()
        val union = a.union(b).size.toDouble()
        return if (union == 0.0) 0.0 else intersection / union
    }

    companion object {
        const val RADIUS_METERS = 150

        private val stopWords = setOf(
            "the", "and", "for", "with", "near", "this", "that", "from", "have", "been",
            "are", "was", "were", "into", "very", "please", "kindly", "issue", "problem",
        )

        fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
            val earthRadius = 6_371_000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val sinLat = sin(dLat / 2).pow(2)
            val sinLon = sin(dLon / 2).pow(2)
            val a = sinLat + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sinLon
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return (earthRadius * c).toInt()
        }
    }
}
