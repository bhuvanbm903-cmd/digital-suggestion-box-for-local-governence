package com.suggestbox.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import com.suggestbox.core.designsystem.icons.SuggestIcons
import com.suggestbox.domain.model.SuggestionCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun SuggestionCategory.icon(): ImageVector = when (this) {
    SuggestionCategory.ROADS -> SuggestIcons.Roads
    SuggestionCategory.WATER -> SuggestIcons.Water
    SuggestionCategory.SANITATION -> SuggestIcons.Sanitation
    SuggestionCategory.ELECTRICITY -> SuggestIcons.Electricity
    SuggestionCategory.HEALTH -> SuggestIcons.Health
    SuggestionCategory.EDUCATION -> SuggestIcons.Education
    SuggestionCategory.SAFETY -> SuggestIcons.Safety
    SuggestionCategory.PARKS -> SuggestIcons.Parks
    SuggestionCategory.OTHER -> SuggestIcons.OtherCategory
}

fun formatRelativeTime(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val diff = (nowMillis - timestampMillis).coerceAtLeast(0L)
    val minutes = diff / 60_000L
    val hours = diff / 3_600_000L
    val days = diff / 86_400_000L
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestampMillis))
    }
}

fun formatExactDateTime(timestampMillis: Long): String {
    return SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(timestampMillis))
}
