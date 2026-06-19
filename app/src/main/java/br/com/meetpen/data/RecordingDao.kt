package br.com.meetpen.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<Recording>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: Recording)

    @Delete
    suspend fun delete(recording: Recording)

    @Query("SELECT COUNT(*) FROM recordings")
    suspend fun getCount(): Int

    @Query("SELECT * FROM recordings WHERE filePath = :path LIMIT 1")
    suspend fun getByFilePath(path: String): Recording?
}
