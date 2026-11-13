package com.suggestbox.data.local

import com.suggestbox.domain.model.GeoLocation
import com.suggestbox.domain.model.SubmissionKind
import com.suggestbox.domain.model.SuggestionCategory
import com.suggestbox.domain.model.SuggestionDraft
import com.suggestbox.domain.repository.DraftRepository
import com.suggestbox.domain.repository.SuggestionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed [DraftRepository] enabling offline drafting and automatic background sync
 * when connectivity returns.
 */
@Singleton
class RoomDraftRepository @Inject constructor(
    private val draftDao: DraftDao,
    private val suggestionRepository: SuggestionRepository,
) : DraftRepository {

    override fun observeDrafts(): Flow<List<SuggestionDraft>> =
        draftDao.observeAllDrafts().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun saveDraft(draft: SuggestionDraft) {
        draftDao.upsertDraft(draft.toEntity())
    }

    override suspend fun deleteDraft(localId: String) {
        draftDao.deleteById(localId)
    }

    override suspend fun syncPendingDrafts(): Int {
        val pending = draftDao.getPendingSyncDrafts()
        var syncedCount = 0
        for (entity in pending) {
            val result = suggestionRepository.submitSuggestion(entity.toDomain())
            if (result.isSuccess) {
                draftDao.deleteById(entity.localId)
                syncedCount++
            }
        }
        return syncedCount
    }

    private fun DraftEntity.toDomain(): SuggestionDraft {
        val uris = runCatching { Json.decodeFromString<List<String>>(mediaUrisJson) }
            .getOrDefault(emptyList())
        val location = if (latitude != null && longitude != null) {
            GeoLocation(latitude, longitude, addressLabel.orEmpty())
        } else {
            null
        }
        return SuggestionDraft(
            localId = localId,
            kind = runCatching { SubmissionKind.valueOf(kindName) }.getOrDefault(SubmissionKind.ISSUE),
            category = runCatching { SuggestionCategory.valueOf(categoryName) }.getOrDefault(SuggestionCategory.OTHER),
            title = title,
            description = description,
            localMediaUris = uris,
            location = location,
            isAnonymous = isAnonymous,
            updatedAtMillis = updatedAtMillis,
            pendingSync = pendingSync,
        )
    }

    private fun SuggestionDraft.toEntity(): DraftEntity = DraftEntity(
        localId = localId,
        kindName = kind.name,
        categoryName = category.name,
        title = title,
        description = description,
        mediaUrisJson = Json.encodeToString(localMediaUris),
        latitude = location?.latitude,
        longitude = location?.longitude,
        addressLabel = location?.addressLabel,
        isAnonymous = isAnonymous,
        updatedAtMillis = updatedAtMillis,
        pendingSync = pendingSync,
    )
}
