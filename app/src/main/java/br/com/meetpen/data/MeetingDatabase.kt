package br.com.meetpen.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {
    @Query("SELECT * FROM meeting_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<MeetingNote>>

    @Insert
    suspend fun insertNote(note: MeetingNote)

    @Delete
    suspend fun deleteNote(note: MeetingNote)
}

@Database(entities = [MeetingNote::class], version = 1, exportSchema = false)
abstract class MeetingDatabase : RoomDatabase() {
    abstract fun meetingDao(): MeetingDao
}
