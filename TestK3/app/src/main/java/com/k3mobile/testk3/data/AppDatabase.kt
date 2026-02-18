package com.k3mobile.testk3.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [TextEntity::class, SessionEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun typingDao(): TypingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "k3_database"
                )
                    .addCallback(DatabaseCallback(scope)) // Ajout automatique ici
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.typingDao()

                    // Ajout d'un exemple par catégorie
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