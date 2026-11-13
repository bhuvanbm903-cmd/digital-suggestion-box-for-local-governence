package com.suggestbox.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Room entity storing offline citizen drafts and cached pending submissions.
 */
@Entity(tableName = "suggestion_drafts")
data class DraftEntity(
    @PrimaryKey val localId: String,
    val kindName: String,
    val categoryName: String,
    val title: String,
    val description: String,
    val mediaUrisJson: String,
    val latitude: Double?,
    val longitude: Double?,
    val addressLabel: String?,
    val isAnonymous: Boolean,
    val updatedAtMillis: Long,
    val pendingSync: Boolean,
)

@Dao
interface DraftDao {
    @Query("SELECT * FROM suggestion_drafts ORDER BY updatedAtMillis DESC")
    fun observeAllDrafts(): Flow<List<DraftEntity>>

    @Query("SELECT * FROM suggestion_drafts WHERE pendingSync = 1")
    suspend fun getPendingSyncDrafts(): List<DraftEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraft(draft: DraftEntity)

    @Query("DELETE FROM suggestion_drafts WHERE localId = :localId")
    suspend fun deleteById(localId: String)
}

/**
 * Local Room database for SuggestBox offline drafts and future offline caching.
 */
@Database(
    entities = [DraftEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SuggestBoxDatabase : RoomDatabase() {
    abstract fun draftDao(): DraftDao
}
