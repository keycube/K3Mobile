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
 * Main Room database for the K3AudioType application.
 *
 * Stores typing practice texts and session statistics.
 * Pre-populated with sample phrases and stories on first creation
 * via [DatabaseCallback].
 */
@Database(entities = [TextEntity::class, SessionEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    /** Provides access to all database operations. */
    abstract fun typingDao(): TypingDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton database instance with thread-safe initialization.
         *
         * @param context Application context.
         * @param scope Coroutine scope for the pre-population callback.
         */
        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "k3_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Callback triggered on first database creation.
     *
     * Inserts sample phrases and stories so the user has content
     * to practice with immediately after installation.
     */
    private class DatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.typingDao()

                    // =============================================================
                    // Phrases — unrelated sentences for quick practice
                    // =============================================================

                    dao.insertText(TextEntity(0, "Vie quotidienne",
                        "Le train de huit heures est toujours en retard. " +
                                "Ma voisine cultive des tomates sur son balcon. " +
                                "Il faut penser a acheter du pain en rentrant. " +
                                "Le chat dort sur le clavier depuis ce matin. " +
                                "La meteo annonce de la pluie pour demain.",
                        "fr", "phrases", 1))

                    dao.insertText(TextEntity(0, "Technologie",
                        "Le wifi ne fonctionne plus depuis hier soir. " +
                                "Mon telephone a besoin d'une mise a jour. " +
                                "Les mots de passe doivent contenir au moins huit caracteres. " +
                                "Le fichier est trop lourd pour etre envoye par mail. " +
                                "Il faut redemarrer le serveur pour appliquer les changements.",
                        "fr", "phrases", 1))

                    dao.insertText(TextEntity(0, "Nature",
                        "Les feuilles des arbres changent de couleur en automne. " +
                                "La riviere coule doucement entre les rochers. " +
                                "Les oiseaux chantent des le lever du soleil. " +
                                "La neige a recouvert les sommets des montagnes. " +
                                "Un arc-en-ciel est apparu apres la pluie.",
                        "fr", "phrases", 1))

                    dao.insertText(TextEntity(0, "Cuisine",
                        "La recette demande trois oeufs et du beurre. " +
                                "Il faut laisser la pate reposer pendant une heure. " +
                                "Le four doit etre prechauffe a cent quatre-vingts degres. " +
                                "Ajoutez une pincee de sel et du poivre. " +
                                "Le gateau sera pret dans trente minutes.",
                        "fr", "phrases", 1))

                    dao.insertText(TextEntity(0, "Voyage",
                        "L'avion decolle a quatorze heures trente. " +
                                "N'oubliez pas votre passeport et votre carte d'embarquement. " +
                                "L'hotel se trouve a dix minutes de la gare. " +
                                "Le musee est ferme le mardi. " +
                                "La vue depuis le sommet de la tour est magnifique.",
                        "fr", "phrases", 1))

                    dao.insertText(TextEntity(0, "Sport",
                        "Le match commence a vingt et une heures. " +
                                "Il faut s'echauffer avant de courir. " +
                                "L'equipe a remporte la victoire en prolongation. " +
                                "La piscine municipale ouvre a sept heures. " +
                                "Le marathon de Paris a lieu chaque annee en avril.",
                        "fr", "phrases", 1))

                    dao.insertText(TextEntity(0, "Ecole",
                        "Les cours reprennent lundi a huit heures. " +
                                "Le devoir de mathematiques est a rendre pour jeudi. " +
                                "La bibliotheque ferme a dix-huit heures. " +
                                "Le professeur a distribue les copies corrigees. " +
                                "La reunion des parents aura lieu vendredi soir.",
                        "fr", "phrases", 1))

                    dao.insertText(TextEntity(0, "Animaux",
                        "Le chien aboie quand quelqu'un sonne a la porte. " +
                                "Les poissons rouges tournent dans leur bocal. " +
                                "Le perroquet repete tout ce qu'on lui dit. " +
                                "Les abeilles sont essentielles pour la pollinisation. " +
                                "Le renard traverse le jardin chaque nuit.",
                        "fr", "phrases", 1))

                    // =============================================================
                    // Histoires — short stories with paragraphs
                    // =============================================================

                    dao.insertText(TextEntity(0, "La cle du grenier",
                        "Un soir d'hiver, Lina trouva une cle rouillee au fond de sa poche. Elle etait certaine qu'elle ne lui appartenait pas. Intriguee, elle essaya toutes les portes de son immeuble, sans succes, jusqu'a celle du grenier, qu'elle n'avait jamais vue ouverte.\n" +
                                "A l'interieur, il n'y avait rien, sauf une fenetre donnant sur un ciel incroyablement bleu, comme en ete. Quand Lina passa la tete dehors, elle sentit la chaleur du soleil et entendit des rires au loin.\n" +
                                "Elle referma vite la fenetre, le coeur battant. La cle avait disparu.\n" +
                                "Depuis ce jour, chaque hiver lui semble un peu moins froid.",
                        "fr", "histoires", 2))

                    dao.insertText(TextEntity(0, "Le phare",
                        "Au bout de la jetee, le vieux phare ne s'allumait plus depuis des annees. Les pecheurs disaient qu'il etait hante, mais personne n'osait verifier.\n" +
                                "Un soir de tempete, un garcon de douze ans poussa la porte rougie par le sel. L'escalier en spirale grincait sous ses pas. En haut, il trouva une lampe a huile et un carnet couvert de poussiere.\n" +
                                "La premiere page disait simplement : Si tu lis ces mots, allume la lumiere. Quelqu'un en a besoin ce soir.\n" +
                                "Le garcon craqua une allumette. Au loin, un bateau changea de cap.",
                        "fr", "histoires", 2))

                    dao.insertText(TextEntity(0, "Le jardin secret",
                        "Derriere la maison de sa grand-mere, Hugo decouvrit une porte cachee sous le lierre. Elle menait a un jardin que personne ne semblait connaitre.\n" +
                                "Les fleurs y poussaient dans un desordre magnifique. Des papillons aux couleurs impossibles volaient entre les roses. Au centre, un banc de pierre portait une inscription : Cet endroit existe tant que quelqu'un y croit.\n" +
                                "Hugo revint chaque ete. Le jardin etait toujours la, un peu different a chaque fois, comme s'il grandissait avec lui.",
                        "fr", "histoires", 2))

                    dao.insertText(TextEntity(0, "Le robot",
                        "Dans un laboratoire oublie, un petit robot s'alluma tout seul un mardi matin. Il ne savait pas qui l'avait construit ni pourquoi. Ses capteurs detectaient la poussiere, le silence et une araignee dans le coin de la fenetre.\n" +
                                "Il decida de ranger. Pendant des jours, il nettoya, repara et classa. Quand le laboratoire fut impeccable, il s'assit et attendit.\n" +
                                "Personne ne vint. Alors il ouvrit la porte et sortit decouvrir le monde, un pas mecanique apres l'autre.",
                        "fr", "histoires", 2))

                    dao.insertText(TextEntity(0, "La lettre",
                        "Camille trouva une lettre glissee sous sa porte un dimanche matin. Pas de nom, pas de timbre. Juste une phrase ecrite a la main : Regarde le ciel a vingt-deux heures ce soir.\n" +
                                "Elle hesita toute la journee. A vingt-deux heures, elle monta sur le toit de son immeuble. Le ciel etait clair et les etoiles brillaient comme jamais.\n" +
                                "Elle ne sut jamais qui avait ecrit cette lettre. Mais depuis, elle regarde le ciel chaque soir, au cas ou.",
                        "fr", "histoires", 2))

                    dao.insertText(TextEntity(0, "Le boulanger",
                        "Monsieur Duval se levait chaque matin a quatre heures pour faire le pain. C'etait le meme rituel depuis trente ans : la farine, l'eau, le sel, et la patience.\n" +
                                "Un jour, un enfant lui demanda pourquoi son pain etait meilleur que celui des autres. Il repondit : Parce que je le fais pour les gens, pas pour l'argent.\n" +
                                "L'enfant ne comprit pas tout de suite. Mais des annees plus tard, devenu boulanger a son tour, il repensa a cette phrase chaque matin a quatre heures.",
                        "fr", "histoires", 2))
                }
            }
        }
    }
}