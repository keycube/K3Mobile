package com.k3mobile.testk3.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.k3mobile.testk3.data.AppDatabase
import com.k3mobile.testk3.data.SessionWithTitle
import com.k3mobile.testk3.data.TextEntity
import com.k3mobile.testk3.data.SessionEntity
import com.k3mobile.testk3.main.K3AppState
import com.k3mobile.testk3.main.K3KeyInput
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val dao        = AppDatabase.getDatabase(application, viewModelScope).typingDao()
    private var tts        : TextToSpeech? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // -------------------------------------------------------------------------
    // Persistance des préférences
    // -------------------------------------------------------------------------
    private val prefs: SharedPreferences =
        application.getSharedPreferences("K3_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_VOICE_NAME = "selected_voice_name"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_CATEGORY_INDEX = "category_index"
        private const val KEY_SPEED_INDEX = "speed_index"
    }

    var savedCategoryIndex: Int
        get() = prefs.getInt(KEY_CATEGORY_INDEX, 0)
        set(value) = prefs.edit().putInt(KEY_CATEGORY_INDEX, value).apply()

    var savedSpeedIndex: Int
        get() = prefs.getInt(KEY_SPEED_INDEX, 1)
        set(value) = prefs.edit().putInt(KEY_SPEED_INDEX, value).apply()

    // -------------------------------------------------------------------------
    // État TTS
    // -------------------------------------------------------------------------

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady = _isTtsReady.asStateFlow()

    // -------------------------------------------------------------------------
    // Navigation clavier — délégué à K3AppState
    // -------------------------------------------------------------------------

    // Propriété calculée : retourne toujours le channel courant de K3AppState.
    // Important : ne pas stocker la référence dans un val, car resetKeyChannel()
    // crée un nouveau channel — il faut toujours lire K3AppState.keyChannel.
    val keyChannel get() = K3AppState.keyChannel

    var isInTypingMode: Boolean
        get()      = K3AppState.isInTypingMode
        set(value) { K3AppState.isInTypingMode = value }

    var pendingTextId: Long? = null

    fun emitKeyEvent(keyCode: Int, unicodeChar: Int = 0) = K3AppState.emitKey(keyCode, unicodeChar)

    /**
     * Réinitialise le channel clavier. À appeler depuis MainActivity juste
     * avant chaque navigation déclenchée par une touche, pour s'assurer que
     * l'ancien écran (dont le LaunchedEffect tourne encore pendant la transition)
     * ne reçoit plus aucune touche destinée au nouvel écran.
     */
    fun resetKeyChannel() = K3AppState.resetKeyChannel()

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
                tts?.setSpeechRate(0.9f)
                loadAvailableVoices()

                val savedVoiceName = prefs.getString(KEY_VOICE_NAME, null)
                if (savedVoiceName != null){
                    val savedVoice = _availableVoices.value.find { it.name == savedVoiceName }
                    if (savedVoice != null){
                        tts?.voice = savedVoice
                        _selectedVoice.value = savedVoice
                    }
                }
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
        prefs.edit().putString(KEY_VOICE_NAME, voice.name).apply()
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
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speak_${System.currentTimeMillis()}")
    }

    fun speakQueued(text: String) {
        if (!_isTtsReady.value) return
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "queued_${System.currentTimeMillis()}")
    }

    /**
     * Parle toutes les [phrases] en séquence, puis appelle [onDone] sur le
     * MAIN THREAD quand la dernière est terminée.
     *
     * UtteranceProgressListener.onDone() est déclenché sur un thread background
     * par le moteur TTS — sans mainHandler.post(), la navigation Compose plante
     * silencieusement sur écran verrouillé.
     */
    fun speakThenDo(phrases: List<String>, onDone: () -> Unit) {
        if (!_isTtsReady.value) { mainHandler.post(onDone); return }
        if (phrases.isEmpty())  { mainHandler.post(onDone); return }

        val lastId = "final_${System.currentTimeMillis()}"

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                if (utteranceId == lastId) {
                    tts?.setOnUtteranceProgressListener(null)
                    mainHandler.post(onDone)   // ← main thread obligatoire
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                tts?.setOnUtteranceProgressListener(null)
                mainHandler.post(onDone)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                tts?.setOnUtteranceProgressListener(null)
                mainHandler.post(onDone)
            }
        })

        phrases.forEachIndexed { index, phrase ->
            val id        = if (index == phrases.lastIndex) lastId else "q_${System.currentTimeMillis()}_$index"
            val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(phrase, queueMode, null, id)
        }
    }

    fun stopSpeaking() {
        if (_isTtsReady.value) {
            tts?.setOnUtteranceProgressListener(null)
            tts?.stop()
        }
    }

    fun setSpeechRate(rate: Float) {
        val ttsRate = 0.7f + ((rate - 1f) / 2f) * 0.8f
        tts?.setSpeechRate(ttsRate)
        prefs.edit().putFloat(KEY_SPEECH_RATE, ttsRate).apply()
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