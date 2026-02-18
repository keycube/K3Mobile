package com.k3mobile.testk3.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.k3mobile.testk3.data.AppDatabase
import com.k3mobile.testk3.data.TextEntity
import com.k3mobile.testk3.data.SessionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    private val dao = AppDatabase.getDatabase(application, viewModelScope).typingDao()

    // Moteur TTS
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        // Initialisation du moteur au lancement du ViewModel
        tts = TextToSpeech(application, this)
    }

    override fun onInit(status: Int) {
        tts?.setSpeechRate(0.65f)
        if (status == TextToSpeech.SUCCESS) {
            // Configuration de la langue (Français)
            val result = tts?.setLanguage(Locale.FRENCH)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
            }
        }
    }

    fun speak(text: String) {
        if (isTtsReady) {
            val textToSpeak = text
                .replace(".", " point ")
                .replace(",", " virgule ")
                .replace("!", " point d'exclamation ")
                .replace("?", " point d'interrogation ")
                .replace(";", " point virgule ")
                .replace(":", " deux points ")

            // On envoie le texte modifié au moteur TTS
            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "UtteranceID")
        }
    }

    // --- GESTION DU CYCLE DE VIE ---
    // Très important : libérer les ressources quand le ViewModel est détruit
    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        super.onCleared()
    }

    private val _texts = MutableStateFlow<List<TextEntity>>(emptyList())
    val texts = _texts.asStateFlow()

    private val _sessions = MutableStateFlow<List<SessionEntity>>(emptyList())
    val sessions = _sessions.asStateFlow()

    // Fonction pour charger les textes selon la catégorie choisie
    fun loadTextsByCategory(category: String) {
        viewModelScope.launch {
            _texts.value = dao.getTextsByCategory(category)
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            _sessions.value = dao.getAllSessions()
        }
    }

    fun addCustomText(title: String, content: String) {
        viewModelScope.launch {
            val newText = TextEntity(
                idText = 0, // Room générera l'ID automatiquement
                title = title,
                content = content,
                language = "fr",
                category = "textes personnalisées",
                difficulty = 1
            )
            dao.insertText(newText)
            // Optionnel : recharger la liste si on est déjà dans la catégorie
            loadTextsByCategory("textes personnalisées")
        }
    }

    fun saveSession(textId: Long, durationMillis: Long, wpm: Double, accuracy: Double) {
        viewModelScope.launch {
            val session = SessionEntity(
                textId = textId,
                duration = durationMillis,
                wpm = wpm,
                accuracy = accuracy,
                timeStamp = System.currentTimeMillis()
            )
            dao.insertSession(session)
        }
    }
}