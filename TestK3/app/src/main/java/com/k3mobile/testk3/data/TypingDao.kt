package com.k3mobile.testk3.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TypingDao {

    @Insert
    suspend fun insertText(text: TextEntity)

    @Query("SELECT * FROM texts WHERE category = :cat")
    suspend fun getTextsByCategory(cat: String): List<TextEntity>

    @Query("SELECT * FROM session_table ORDER BY timeStamp DESC")
    suspend fun getAllSessions(): List<SessionEntity>

    @Insert
    suspend fun insertSession(session: SessionEntity)

    /** Met à jour le titre et le contenu d'un texte existant. */
    @Query("UPDATE texts SET title = :title, content = :content WHERE idText = :id")
    suspend fun updateText(id: Long, title: String, content: String)
}