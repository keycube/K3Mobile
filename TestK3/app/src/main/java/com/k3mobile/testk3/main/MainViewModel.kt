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
import com.k3mobile.testk3.main.K3AppState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val dao = AppDatabase.getDatabase(application, viewModelScope).typingDao()
    private var tts: TextToSpeech? = null

    // -------------------------------------------------------------------------
    // État TTS
    // -------------------------------------------------------------------------

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady = _isTtsReady.asStateFlow()

    // -------------------------------------------------------------------------
    // Luminosité écran
    // -------------------------------------------------------------------------

    private val _screenBrightness = MutableStateFlow(-1f)
    val screenBrightness = _screenBrightness.asStateFlow()

    fun setScreenBrightness(brightness: Float) { _screenBrightness.value = brightness.coerceIn(-1f, 1f) }
    fun dimScreen()    = setScreenBrightness(0f)
    fun normalScreen() = setScreenBrightness(-1f)
    fun brightScreen() = setScreenBrightness(1f)

    // -------------------------------------------------------------------------
    // Navigation clavier — délégué à K3AppState
    // -------------------------------------------------------------------------

    val keyEvent = K3AppState.keyEvent

    var isInTypingMode: Boolean
        get()      = K3AppState.isInTypingMode
        set(value) { K3AppState.isInTypingMode = value }

    // Fallback si le service d'accessibilité n'est pas actif
    fun emitKeyEvent(keyCode: Int) = K3AppState.emitKey(keyCode)

    // -------------------------------------------------------------------------
    // Voix
    // -------------------------------------------------------------------------

    private val _availableVoices = MutableStateFlow<List<Voice>>(emptyList())
    val availableVoices = _availableVoices.asStateFlow()

    private val _selectedVoice = MutableStateFlow<Voice?>(null)
    val selectedVoice = _selectedVoice.asStateFlow()

    init {
        tts = TextToSpeech(application, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(java.util.Locale.FRENCH)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setSpeechRate(0.65f)
                loadAvailableVoices()
                _isTtsReady.value = true
            }
        }
    }

    private fun loadAvailableVoices() {
        val voices = tts?.voices
            ?.filter { it.locale.language == "fr" && !it.isNetworkConnectionRequired }
            ?.sortedBy { it.name }
            ?: emptyList()
        _availableVoices.value = voices
        _selectedVoice.value = tts?.voice
    }

    fun selectVoice(voice: Voice) {
        tts?.voice = voice
        _selectedVoice.value = voice
    }

    fun previewVoice(voice: Voice) {
        if (!_isTtsReady.value) return
        val prev = tts?.voice
        tts?.voice = voice
        tts?.speak("Bonjour, voici un aperçu de ma voix.", TextToSpeech.QUEUE_FLUSH, null, "VoicePreview")
        if (prev != null && prev.name != voice.name && _selectedVoice.value?.name != voice.name) {
            tts?.voice = _selectedVoice.value ?: prev
        }
    }

    // -------------------------------------------------------------------------
    // TTS — lecture
    // -------------------------------------------------------------------------

    fun speak(text: String) {
        if (!_isTtsReady.value) return
        val cleaned = text
            .replace(".", " point ")
            .replace(",", " virgule ")
            .replace("!", " point d'exclamation ")
            .replace("?", " point d'interrogation ")
            .replace(";", " point virgule ")
            .replace(":", " deux points ")
        tts?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, "speak_${System.currentTimeMillis()}")
    }

    fun speakQueued(text: String) {
        if (!_isTtsReady.value) return
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "queued_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        if (_isTtsReady.value) tts?.stop()
    }

    fun setSpeechRate(rate: Float) {
        val ttsRate = 0.4f + ((rate - 1f) / 2f) * 0.8f
        tts?.setSpeechRate(ttsRate)
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        super.onCleared()
    }

    // -------------------------------------------------------------------------
    // Textes
    // -------------------------------------------------------------------------

    private val _texts = MutableStateFlow<List<TextEntity>>(emptyList())
    val texts = _texts.asStateFlow()

    fun loadTextsByCategory(category: String) {
        viewModelScope.launch { _texts.value = dao.getTextsByCategory(category) }
    }

    fun addCustomText(title: String, content: String) {
        viewModelScope.launch {
            dao.insertText(TextEntity(0, title, content, "fr", "textes personnalisées", 1))
            loadTextsByCategory("textes personnalisées")
        }
    }

    fun updateCustomText(id: Long, title: String, content: String) {
        viewModelScope.launch {
            dao.updateText(id, title, content)
            loadTextsByCategory("textes personnalisées")
        }
    }

    // -------------------------------------------------------------------------
    // Sessions
    // -------------------------------------------------------------------------

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
            dao.insertSession(SessionEntity(textId = textId, duration = durationMillis, wpm = wpm, accuracy = accuracy, timeStamp = System.currentTimeMillis()))
        }
    }
}