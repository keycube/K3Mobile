package com.k3mobile.testk3.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.k3mobile.testk3.data.AppDatabase
import com.k3mobile.testk3.data.SessionWithTitle
import com.k3mobile.testk3.data.TextEntity
import com.k3mobile.testk3.data.SessionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val dao = AppDatabase.getDatabase(application, viewModelScope).typingDao()

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    // --- Voix disponibles ---

    // Liste des voix françaises disponibles sur l'appareil
    private val _availableVoices = MutableStateFlow<List<Voice>>(emptyList())
    val availableVoices = _availableVoices.asStateFlow()

    // Voix actuellement sélectionnée (null = voix par défaut)
    private val _selectedVoice = MutableStateFlow<Voice?>(null)
    val selectedVoice = _selectedVoice.asStateFlow()

    init {
        tts = TextToSpeech(application, this)
    }

    override fun onInit(status: Int) {
        tts?.setSpeechRate(0.65f)
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.FRENCH)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
                loadAvailableVoices()
            }
        }
    }

    /**
     * Récupère toutes les voix françaises disponibles sur l'appareil
     * et les trie par nom pour un affichage cohérent.
     */
    private fun loadAvailableVoices() {
        val voices = tts?.voices
            ?.filter { voice ->
                voice.locale.language == "fr" && !voice.isNetworkConnectionRequired
            }
            ?.sortedBy { it.name }
            ?: emptyList()

        _availableVoices.value = voices

        // Pré-sélectionne la voix active du moteur TTS si disponible
        _selectedVoice.value = tts?.voice
    }

    /**
     * Applique une voix choisie par l'utilisateur.
     * La mémorise dans le ViewModel pour la session en cours.
     */
    fun selectVoice(voice: Voice) {
        tts?.voice = voice
        _selectedVoice.value = voice
    }

    /**
     * Joue un aperçu de la voix donnée avec une phrase d'exemple,
     * puis restaure la voix sélectionnée.
     */
    fun previewVoice(voice: Voice) {
        if (!isTtsReady) return
        val previousVoice = tts?.voice
        tts?.voice = voice
        tts?.speak(
            "Bonjour, voici un aperçu de ma voix.",
            TextToSpeech.QUEUE_FLUSH,
            null,
            "VoicePreview"
        )
        // Restaure la voix sélectionnée après la lecture si ce n'est pas celle qu'on prévisualise
        if (previousVoice != null && previousVoice.name != voice.name && _selectedVoice.value?.name != voice.name) {
            tts?.voice = _selectedVoice.value ?: previousVoice
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
            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "UtteranceID")
        }
    }

    fun stopSpeaking() {
        if (isTtsReady) tts?.stop()
    }

    /**
     * Modifie la vitesse de lecture du TTS.
     * @param rate vitesse en mots/sec (1.0 = normal, 0.5 = lent, 2.0 = rapide)
     *             Converti en speechRate TTS : 1 mot/sec ≈ 0.65 de speechRate
     */
    fun setSpeechRate(rate: Float) {
        // On mappe la plage 1–3 mots/sec vers une plage TTS 0.4–1.2
        val ttsRate = 0.4f + ((rate - 1f) / 2f) * 0.8f
        tts?.setSpeechRate(ttsRate)
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        super.onCleared()
    }

    // --- Texts ---

    private val _texts = MutableStateFlow<List<TextEntity>>(emptyList())
    val texts = _texts.asStateFlow()

    fun loadTextsByCategory(category: String) {
        viewModelScope.launch {
            _texts.value = dao.getTextsByCategory(category)
        }
    }

    fun addCustomText(title: String, content: String) {
        viewModelScope.launch {
            dao.insertText(
                TextEntity(
                    idText = 0,
                    title = title,
                    content = content,
                    language = "fr",
                    category = "textes personnalisées",
                    difficulty = 1
                )
            )
            loadTextsByCategory("textes personnalisées")
        }
    }

    fun updateCustomText(id: Long, title: String, content: String) {
        viewModelScope.launch {
            dao.updateText(id, title, content)
            loadTextsByCategory("textes personnalisées")
        }
    }

    // --- Sessions ---

    private val _sessions = MutableStateFlow<List<SessionEntity>>(emptyList())
    val sessions = _sessions.asStateFlow()

    private val _sessionsWithTitle = MutableStateFlow<List<SessionWithTitle>>(emptyList())
    val sessionsWithTitle = _sessionsWithTitle.asStateFlow()

    fun loadStats() {
        viewModelScope.launch {
            _sessions.value = dao.getAllSessions()
            _sessionsWithTitle.value = dao.getAllSessionsWithTitle()
        }
    }

    fun saveSession(textId: Long, durationMillis: Long, wpm: Double, accuracy: Double) {
        viewModelScope.launch {
            dao.insertSession(
                SessionEntity(
                    textId = textId,
                    duration = durationMillis,
                    wpm = wpm,
                    accuracy = accuracy,
                    timeStamp = System.currentTimeMillis()
                )
            )
        }
    }
}