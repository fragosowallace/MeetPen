package br.com.meetpen.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val filePath: String,
    val timestamp: Long,
    val transcription: String = "",
    val category: String = "Geral",
    val summary: String = "",
    val todoList: String = ""
)
