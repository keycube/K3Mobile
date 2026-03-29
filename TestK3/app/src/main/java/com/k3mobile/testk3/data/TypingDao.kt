package com.k3mobile.testk3.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * Résultat de la jointure session + titre du texte associé.
 * Utilisé pour afficher les statistiques avec le nom du texte pratiqué.
 */
data class SessionWithTitle(
    val idSession: Long,
    val textId: Long,
    val textTitle: String,
    val timeStamp: Long,
    val duration: Long,
    val wpm: Double,
    val accuracy: Double
)

@Dao
interface TypingDao {

    @Insert
    suspend fun insertText(text: TextEntity)

    @Query("SELECT * FROM texts WHERE category = :cat")
    suspend fun getTextsByCategory(cat: String): List<TextEntity>

    @Query("SELECT * FROM session_table ORDER BY timeStamp DESC")
    suspend fun getAllSessions(): List<SessionEntity>

    /**
     * Récupère toutes les sessions avec le titre du texte associé.
     * LEFT JOIN pour garder les sessions même si le texte a été supprimé.
     */
    @Query("""
        SELECT 
            s.idSession,
            s.textId,
            COALESCE(t.title, 'Texte supprimé') AS textTitle,
            s.timeStamp,
            s.duration,
            s.wpm,
            s.accuracy
        FROM session_table s
        LEFT JOIN texts t ON s.textId = t.idText
        ORDER BY s.timeStamp DESC
    """)
    suspend fun getAllSessionsWithTitle(): List<SessionWithTitle>

    @Insert
    suspend fun insertSession(session: SessionEntity)

    @Query("UPDATE texts SET title = :title, content = :content WHERE idText = :id")
    suspend fun updateText(id: Long, title: String, content: String)

    @Query("DELETE FROM texts WHERE idText = :id")
    suspend fun deleteText(id: Long)
}