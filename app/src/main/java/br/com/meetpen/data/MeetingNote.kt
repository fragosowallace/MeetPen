package br.com.meetpen.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meeting_notes")
data class MeetingNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val transcription: String,
    val summary: String,
    val actionItems: String, // Stored as a string (can be JSON)
    val timestamp: Long = System.currentTimeMillis()
)
