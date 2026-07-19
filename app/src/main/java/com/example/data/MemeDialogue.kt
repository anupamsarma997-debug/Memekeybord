package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meme_dialogues")
data class MemeDialogue(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val emoji: String,
    val dialogue: String,
    val category: String,
    val mood: String,
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false
)
