package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemeDao {
    @Query("SELECT * FROM meme_dialogues ORDER BY id ASC")
    fun getAllDialogues(): Flow<List<MemeDialogue>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDialogue(dialogue: MemeDialogue)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDialogues(dialogues: List<MemeDialogue>)

    @Update
    suspend fun updateDialogue(dialogue: MemeDialogue)

    @Query("DELETE FROM meme_dialogues WHERE id = :id")
    suspend fun deleteDialogueById(id: Int)

    @Query("DELETE FROM meme_dialogues")
    suspend fun deleteAll()
}
