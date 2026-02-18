package com.k3mobile.testk3.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query


/**
 * TypingDao
 *
 * Data Access Object (DAO) for the typing application.
 * Defines all database operations related to texts and typing sessions.
 */
@Dao
interface TypingDao {

    /**
     * Inserts a new text into the database.
     *
     * @param text The text entity to be stored.
     */
    @Insert
    suspend fun insertText(text: TextEntity)


    /**
     * Retrieves all texts belonging to a specific category.
     *
     * @param cat The category used to filter texts.
     * @return A list of texts matching the selected category.
     */
    @Query("SELECT * FROM texts WHERE category = :cat")
    suspend fun getTextsByCategory(cat: String): List<TextEntity>

    /**
     * Retrieves all typing sessions, ordered by most recent first.
     *
     * @return A list of typing session entities.
     */
    @Query("SELECT * FROM session_table ORDER BY timeStamp DESC")
    suspend fun getAllSessions(): List<SessionEntity>


    /**
     * Inserts a completed typing session into the database.
     *
     * @param session The session entity containing performance data.
     */
    @Insert
    suspend fun insertSession(session: SessionEntity)
}