package com.k3mobile.testk3.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 * AppDatabase
 *
 * Main Room database of the application.
 * It stores texts used for typing practice and typing session statistics.
 */
@Database(entities = [TextEntity::class, SessionEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    // Provides access to database operations
    abstract fun typingDao(): TypingDao

    companion object {

        // Singleton instance of the database
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton database instance.
         * Ensures thread-safe initialization using synchronized block.
         */
        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "k3_database"
                )
                    // Callback used to prepopulate the database at creation
                    .addCallback(DatabaseCallback(scope)) // Ajout automatique ici
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }


    /**
     * DatabaseCallback
     *
     * This callback is triggered when the database is created for the first time.
     * It inserts initial data to provide example texts to the user.
     */
    private class DatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            // Populate the database in a background thread
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.typingDao()

                    // Insert sample texts for each category
                    dao.insertText(TextEntity(0, "Simple", "Salut je m'appelle Mathis !", "fr", "phrases", 1))
                    dao.insertText(TextEntity(0, "Conte", "Il était une fois...", "fr", "histoires", 1))
                    dao.insertText(TextEntity(0, "Mon Test", "Ceci est à moi.", "fr", "textes personnalisées", 1))
                    dao.insertText(TextEntity(0, "Conte Long", "Un soir d’hiver, Lina trouva une clé rouillée au fond de sa poche. Elle était certaine qu’elle ne lui appartenait pas. Intriguée, elle essaya toutes les portes de son immeuble, sans succès… jusqu’à celle du grenier, qu’elle n’avait jamais vue ouverte.\n" +
                            "\n" +
                            "À l’intérieur, il n’y avait rien, sauf une fenêtre donnant sur un ciel incroyablement bleu, comme en été. Quand Lina passa la tête dehors, elle sentit la chaleur du soleil et entendit des rires au loin.\n" +
                            "\n" +
                            "Elle referma vite la fenêtre, le cœur battant. La clé avait disparu.\n" +
                            "Depuis ce jour, chaque hiver lui semble un peu moins froid.", "fr", "histoires", 2))

                }
            }
        }
    }
}