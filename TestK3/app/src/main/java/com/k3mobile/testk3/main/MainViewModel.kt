package com.k3mobile.testk3.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
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
import com.k3mobile.testk3.main.K3SoundManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val dao        = AppDatabase.getDatabase(application, viewModelScope).typingDao()
    private var tts        : TextToSpeech? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // -------------------------------------------------------------------------
    // Sons et haptique
    // -------------------------------------------------------------------------

    val sound = K3SoundManager(application)

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
        private const val KEY_TTS_VOLUME = "tts_volume"
        private const val KEY_EFFECTS_VOLUME = "effects_volume"
        private const val KEY_LANGUAGE = "language_code"
    }

    var savedCategoryIndex: Int
        get() = prefs.getInt(KEY_CATEGORY_INDEX, 0)
        set(value) = prefs.edit().putInt(KEY_CATEGORY_INDEX, value).apply()

    var savedSpeedIndex: Int
        get() = prefs.getInt(KEY_SPEED_INDEX, 1)
        set(value) = prefs.edit().putInt(KEY_SPEED_INDEX, value).apply()

    /** Volume TTS 0–100, par défaut 50. */
    var savedTtsVolume: Int
        get() = prefs.getInt(KEY_TTS_VOLUME, 50)
        set(value) {
            prefs.edit().putInt(KEY_TTS_VOLUME, value).apply()
            _ttsVolume = value / 100f
        }

    /** Volume effets sonores 0–100, par défaut 50. */
    var savedEffectsVolume: Int
        get() = prefs.getInt(KEY_EFFECTS_VOLUME, 50)
        set(value) {
            prefs.edit().putInt(KEY_EFFECTS_VOLUME, value).apply()
            sound.setVolume(value)
        }

    /** Vibrations activées ou non. */
    var savedVibrationEnabled: Boolean
        get() = prefs.getBoolean("vibration_enabled", true)
        set(value) = prefs.edit().putBoolean("vibration_enabled", value).apply()

    /** Code langue sauvegardé : "fr", "en", "es". */
    var savedLanguage: String
        get() = prefs.getString(KEY_LANGUAGE, "fr") ?: "fr"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    /** Volume TTS courant en float 0.0–1.0 pour le Bundle speak(). */
    private var _ttsVolume: Float = prefs.getInt(KEY_TTS_VOLUME, 50) / 100f

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
            // Restaurer la langue sauvegardée
            val locale = localeForCode(savedLanguage)
            val result = tts?.setLanguage(locale)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setSpeechRate(0.9f)
                loadAvailableVoices()

                // Restaurer la voix sauvegardée
                val savedVoiceName = prefs.getString(KEY_VOICE_NAME, null)
                if (savedVoiceName != null) {
                    val savedVoice = _availableVoices.value.find { it.name == savedVoiceName }
                    if (savedVoice != null) {
                        tts?.voice = savedVoice
                        _selectedVoice.value = savedVoice
                    }
                }

                // Restaurer le volume des effets
                sound.setVolume(savedEffectsVolume)
                sound.vibrationEnabled = savedVibrationEnabled

                _isTtsReady.value = true
            }
        }
    }

    /** Convertit un code langue en Locale. */
    private fun localeForCode(code: String): java.util.Locale = when (code) {
        "en" -> java.util.Locale.ENGLISH
        "es" -> java.util.Locale("es")
        else -> java.util.Locale.FRENCH
    }

    private fun loadAvailableVoices() {
        val langCode = savedLanguage
        val voices = tts?.voices
            ?.filter { it.locale.language == langCode && !it.isNetworkConnectionRequired }
            ?.sortedBy { it.name }
            ?: emptyList()
        _availableVoices.value = voices
        _selectedVoice.value = tts?.voice
    }

    /**
     * Change la langue du TTS et recharge les voix disponibles.
     * Réinitialise la voix sélectionnée vers la voix par défaut de la nouvelle langue.
     */
    fun setLanguage(code: String) {
        savedLanguage = code
        val locale = localeForCode(code)
        tts?.setLanguage(locale)
        loadAvailableVoices()
        // Sélectionner la première voix disponible dans la nouvelle langue
        val firstVoice = _availableVoices.value.firstOrNull()
        if (firstVoice != null) {
            tts?.voice = firstVoice
            _selectedVoice.value = firstVoice
            prefs.edit().putString(KEY_VOICE_NAME, firstVoice.name).apply()
        }
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
        tts?.speak("Bonjour, voici un aperçu de ma voix.", TextToSpeech.QUEUE_FLUSH, volumeBundle(), "VoicePreview")
        if (prev != null && prev.name != voice.name && _selectedVoice.value?.name != voice.name) {
            tts?.voice = _selectedVoice.value ?: prev
        }
    }

    // -------------------------------------------------------------------------
    // TTS — lecture
    // -------------------------------------------------------------------------

    /** Crée un Bundle avec le volume TTS courant. */
    private fun volumeBundle(): Bundle = Bundle().apply {
        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, _ttsVolume)
    }

    fun speak(text: String) {
        if (!_isTtsReady.value) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, volumeBundle(), "speak_${System.currentTimeMillis()}")
    }

    fun speakQueued(text: String) {
        if (!_isTtsReady.value) return
        tts?.speak(text, TextToSpeech.QUEUE_ADD, volumeBundle(), "queued_${System.currentTimeMillis()}")
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
            tts?.speak(phrase, queueMode, volumeBundle(), id)
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
        sound.release()
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

    private val _sessionsWithTitle = MutableStateFlow<List<SessionWithTitle>>(emptyList())
    val sessionsWithTitle = _sessionsWithTitle.asStateFlow()

    private val _totalSessionCount = MutableStateFlow(0)
    val totalSessionCount = _totalSessionCount.asStateFlow()

    private val _hasMoreSessions = MutableStateFlow(false)
    val hasMoreSessions = _hasMoreSessions.asStateFlow()

    private var currentOffset = 0
    private val pageSize = 5

    /** Charge la première page de sessions (reset). */
    fun loadStats() {
        currentOffset = 0
        viewModelScope.launch(Dispatchers.IO) {
            _totalSessionCount.value = dao.getSessionCount()
            val page = dao.getSessionsWithTitlePaged(pageSize, 0)
            _sessionsWithTitle.value = page
            currentOffset = page.size
            _hasMoreSessions.value = currentOffset < _totalSessionCount.value
        }
    }

    /** Charge la page suivante et l'ajoute à la liste existante. */
    fun loadMoreSessions() {
        viewModelScope.launch(Dispatchers.IO) {
            val page = dao.getSessionsWithTitlePaged(pageSize, currentOffset)
            _sessionsWithTitle.value = _sessionsWithTitle.value + page
            currentOffset += page.size
            _hasMoreSessions.value = currentOffset < _totalSessionCount.value
        }
    }

    fun saveSession(textId: Long, durationMillis: Long, wpm: Double, accuracy: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertSession(SessionEntity(textId = textId, duration = durationMillis, wpm = wpm, accuracy = accuracy, timeStamp = System.currentTimeMillis()))
        }
    }

    fun deleteCustomText(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteText(id)
            loadTextsByCategory("textes personnalisées")
        }
    }
}