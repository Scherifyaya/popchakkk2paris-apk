package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val category: String, // "Personal", "Work", "Ideas", "Tasks", "Lists"
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val colorHex: String = "#FFF59D", // Default elegant yellow pastel
    val checklistJson: String = "" // Underline checkable list items
)
