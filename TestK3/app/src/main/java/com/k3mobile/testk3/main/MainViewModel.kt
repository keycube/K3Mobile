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

/**
 * Shared ViewModel for the entire application.
 *
 * Centralizes TTS engine management, user preferences, keyboard event routing,
 * Room database access (texts and sessions), and sound effects.
 * Survives configuration changes and is scoped to the Activity lifecycle.
 */
class MainViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val dao = AppDatabase.getDatabase(application, viewModelScope).typingDao()
    private var tts: TextToSpeech? = null

    /** Handler for posting callbacks to the main thread from TTS background threads. */
    private val mainHandler = Handler(Looper.getMainLooper())

    // -------------------------------------------------------------------------
    // Sound & haptic feedback
    // -------------------------------------------------------------------------

    /** Shared sound manager for earcons and vibration across all screens. */
    val sound = K3SoundManager(application)

    // -------------------------------------------------------------------------
    // User preferences (persisted via SharedPreferences)
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

    /** Last selected text category index (0=phrases, 1=stories, 2=custom). */
    var savedCategoryIndex: Int
        get() = prefs.getInt(KEY_CATEGORY_INDEX, 0)
        set(value) = prefs.edit().putInt(KEY_CATEGORY_INDEX, value).apply()

    /** Last selected speed index (0=slow .. 4=max). */
    var savedSpeedIndex: Int
        get() = prefs.getInt(KEY_SPEED_INDEX, 1)
        set(value) = prefs.edit().putInt(KEY_SPEED_INDEX, value).apply()

    /** TTS volume 0–100, default 50. Also updates the internal float used by [speak]. */
    var savedTtsVolume: Int
        get() = prefs.getInt(KEY_TTS_VOLUME, 50)
        set(value) {
            prefs.edit().putInt(KEY_TTS_VOLUME, value).apply()
            _ttsVolume = value / 100f
        }

    /** Effects volume 0–100, default 50. Immediately updates [K3SoundManager]. */
    var savedEffectsVolume: Int
        get() = prefs.getInt(KEY_EFFECTS_VOLUME, 50)
        set(value) {
            prefs.edit().putInt(KEY_EFFECTS_VOLUME, value).apply()
            sound.setVolume(value)
        }

    /** Whether haptic feedback is enabled. */
    var savedVibrationEnabled: Boolean
        get() = prefs.getBoolean("vibration_enabled", true)
        set(value) = prefs.edit().putBoolean("vibration_enabled", value).apply()

    /** Saved language code: "fr", "en", or "es". */
    var savedLanguage: String
        get() = prefs.getString(KEY_LANGUAGE, "fr") ?: "fr"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    /**
     * Screen-on mode: when enabled, disables TTS, earcons and countdown
     * for sighted users who don't need audio feedback.
     */
    var savedScreenMode: Boolean
        get() = prefs.getBoolean("screen_on_mode", false)
        set(value) = prefs.edit().putBoolean("screen_on_mode", value).apply()

    /** Current TTS volume as a float 0.0–1.0, used in the speak Bundle. */
    private var _ttsVolume: Float = prefs.getInt(KEY_TTS_VOLUME, 50) / 100f

    // -------------------------------------------------------------------------
    // TTS state
    // -------------------------------------------------------------------------

    private val _isTtsReady = MutableStateFlow(false)

    /** Emits `true` once the TTS engine is initialized and ready to speak. */
    val isTtsReady = _isTtsReady.asStateFlow()

    // -------------------------------------------------------------------------
    // Keyboard event routing (delegated to K3AppState)
    // -------------------------------------------------------------------------

    /**
     * Current keyboard event channel.
     *
     * Computed property — always reads from [K3AppState.keyChannel] to get
     * the latest channel after [resetKeyChannel] calls.
     */
    val keyChannel get() = K3AppState.keyChannel

    /** Proxied typing mode flag from [K3AppState]. */
    var isInTypingMode: Boolean
        get() = K3AppState.isInTypingMode
        set(value) { K3AppState.isInTypingMode = value }

    /** ID of the text selected by the user in [TextListScreen], awaiting ENTER confirmation. */
    var pendingTextId: Long? = null

    /** Emits a key event to the current channel. */
    fun emitKeyEvent(keyCode: Int, unicodeChar: Int = 0) = K3AppState.emitKey(keyCode, unicodeChar)

    /** Resets the key channel before navigation. See [K3AppState.resetKeyChannel]. */
    fun resetKeyChannel() = K3AppState.resetKeyChannel()

    // -------------------------------------------------------------------------
    // Voice management
    // -------------------------------------------------------------------------

    private val _availableVoices = MutableStateFlow<List<Voice>>(emptyList())

    /** List of TTS voices available for the current language, sorted by name. */
    val availableVoices = _availableVoices.asStateFlow()

    private val _selectedVoice = MutableStateFlow<Voice?>(null)

    /** Currently selected TTS voice. */
    val selectedVoice = _selectedVoice.asStateFlow()

    init {
        tts = TextToSpeech(application, this)
    }

    /**
     * Called when the TTS engine finishes initialization.
     *
     * Restores the saved language, voice, and volume settings.
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = localeForCode(savedLanguage)
            val result = tts?.setLanguage(locale)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setSpeechRate(0.9f)
                loadAvailableVoices()

                val savedVoiceName = prefs.getString(KEY_VOICE_NAME, null)
                if (savedVoiceName != null) {
                    val savedVoice = _availableVoices.value.find { it.name == savedVoiceName }
                    if (savedVoice != null) {
                        tts?.voice = savedVoice
                        _selectedVoice.value = savedVoice
                    }
                }

                sound.setVolume(savedEffectsVolume)
                sound.vibrationEnabled = savedVibrationEnabled

                _isTtsReady.value = true
            }
        }
    }

    /** Converts a language code ("fr", "en", "es") to a [java.util.Locale]. */
    private fun localeForCode(code: String): java.util.Locale = when (code) {
        "en" -> java.util.Locale.ENGLISH
        "es" -> java.util.Locale("es")
        else -> java.util.Locale.FRENCH
    }

    /** Loads offline voices matching the current language and sorts them by name. */
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
     * Changes the TTS language and reloads available voices.
     *
     * Automatically selects the first available voice in the new language.
     *
     * @param code Language code: "fr", "en", or "es".
     */
    fun setLanguage(code: String) {
        savedLanguage = code
        val locale = localeForCode(code)
        tts?.setLanguage(locale)
        loadAvailableVoices()
        val firstVoice = _availableVoices.value.firstOrNull()
        if (firstVoice != null) {
            tts?.voice = firstVoice
            _selectedVoice.value = firstVoice
            prefs.edit().putString(KEY_VOICE_NAME, firstVoice.name).apply()
        }
    }

    /**
     * Selects and persists a TTS voice.
     *
     * @param voice The voice to activate.
     */
    fun selectVoice(voice: Voice) {
        tts?.voice = voice
        _selectedVoice.value = voice
        prefs.edit().putString(KEY_VOICE_NAME, voice.name).apply()
    }

    /**
     * Plays a preview of the given voice without changing the current selection.
     *
     * Temporarily switches the TTS voice, speaks a sample sentence,
     * then restores the previously selected voice.
     */
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
    // TTS — speech output
    // -------------------------------------------------------------------------

    /** Creates a Bundle containing the current TTS volume setting. */
    private fun volumeBundle(): Bundle = Bundle().apply {
        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, _ttsVolume)
    }

    /**
     * Speaks text immediately, interrupting any ongoing speech.
     *
     * Does nothing if TTS is not ready or screen-on mode is enabled.
     */
    fun speak(text: String) {
        if (!_isTtsReady.value || savedScreenMode) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, volumeBundle(), "speak_${System.currentTimeMillis()}")
    }

    /**
     * Queues text to be spoken after any currently playing speech.
     *
     * Does nothing if TTS is not ready or screen-on mode is enabled.
     */
    fun speakQueued(text: String) {
        if (!_isTtsReady.value || savedScreenMode) return
        tts?.speak(text, TextToSpeech.QUEUE_ADD, volumeBundle(), "queued_${System.currentTimeMillis()}")
    }

    /**
     * Speaks a sequence of phrases, then executes [onDone] on the main thread.
     *
     * The callback is posted via [mainHandler] because [UtteranceProgressListener.onDone]
     * fires on a TTS background thread — Compose navigation would crash
     * if triggered directly from there (especially on locked screens).
     *
     * @param phrases List of texts to speak sequentially.
     * @param onDone Callback invoked on the main thread after the last phrase finishes.
     */
    fun speakThenDo(phrases: List<String>, onDone: () -> Unit) {
        if (!_isTtsReady.value || savedScreenMode) { mainHandler.post(onDone); return }
        if (phrases.isEmpty()) { mainHandler.post(onDone); return }

        val lastId = "final_${System.currentTimeMillis()}"

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                if (utteranceId == lastId) {
                    tts?.setOnUtteranceProgressListener(null)
                    mainHandler.post(onDone)
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
            val id = if (index == phrases.lastIndex) lastId else "q_${System.currentTimeMillis()}_$index"
            val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(phrase, queueMode, volumeBundle(), id)
        }
    }

    /** Immediately stops any ongoing TTS speech and clears the utterance listener. */
    fun stopSpeaking() {
        if (_isTtsReady.value) {
            tts?.setOnUtteranceProgressListener(null)
            tts?.stop()
        }
    }

    /**
     * Sets the TTS speech rate based on the user's speed selection.
     *
     * Maps the game speed range (1.0–3.0) to a TTS rate range (0.7–1.5).
     *
     * @param rate Game speed value from the speed slider.
     */
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
    // Texts (Room database)
    // -------------------------------------------------------------------------

    private val _texts = MutableStateFlow<List<TextEntity>>(emptyList())

    /** Observable list of texts for the currently loaded category. */
    val texts = _texts.asStateFlow()

    /**
     * Loads all texts matching a category from the database.
     *
     * Runs on [Dispatchers.IO] to avoid blocking the main thread.
     *
     * @param category Category name: "phrases", "histoires", or "textes personnalisées".
     */
    fun loadTextsByCategory(category: String) {
        viewModelScope.launch(Dispatchers.IO) { _texts.value = dao.getTextsByCategory(category) }
    }

    /**
     * Inserts a new custom text and refreshes the custom texts list.
     *
     * @param title Display title for the text.
     * @param content The text content to be typed by the user.
     */
    fun addCustomText(title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertText(TextEntity(0, title, content, "fr", "textes personnalisées", 1))
            loadTextsByCategory("textes personnalisées")
        }
    }

    /**
     * Updates an existing custom text's title and content.
     *
     * @param id The text's database ID.
     * @param title New title.
     * @param content New content.
     */
    fun updateCustomText(id: Long, title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateText(id, title, content)
            loadTextsByCategory("textes personnalisées")
        }
    }

    // -------------------------------------------------------------------------
    // Sessions (Room database)
    // -------------------------------------------------------------------------

    private val _sessionsWithTitle = MutableStateFlow<List<SessionWithTitle>>(emptyList())

    /** Paginated list of sessions with their associated text titles. */
    val sessionsWithTitle = _sessionsWithTitle.asStateFlow()

    private val _totalSessionCount = MutableStateFlow(0)

    /** Total number of sessions in the database (independent of pagination). */
    val totalSessionCount = _totalSessionCount.asStateFlow()

    private val _hasMoreSessions = MutableStateFlow(false)

    /** `true` if more session pages are available to load. */
    val hasMoreSessions = _hasMoreSessions.asStateFlow()

    // -------------------------------------------------------------------------
    // Global statistics (computed from entire database, not paginated data)
    // -------------------------------------------------------------------------

    private val _globalBestWpm = MutableStateFlow(0)

    /** Highest WPM score across all recorded sessions. */
    val globalBestWpm = _globalBestWpm.asStateFlow()

    private val _globalAvgAccuracy = MutableStateFlow(0)

    /** Average accuracy percentage across all recorded sessions. */
    val globalAvgAccuracy = _globalAvgAccuracy.asStateFlow()

    private val _globalTotalDuration = MutableStateFlow(0L)

    /** Cumulative duration (ms) of all recorded sessions. */
    val globalTotalDuration = _globalTotalDuration.asStateFlow()

    private val _chartSessions = MutableStateFlow<List<SessionWithTitle>>(emptyList())

    /** Last 10 sessions in chronological order, used for the progress chart. */
    val chartSessions = _chartSessions.asStateFlow()

    private var currentOffset = 0
    private val pageSize = 5

    /**
     * Loads global statistics and the first page of session history.
     *
     * Global stats (best WPM, avg accuracy) are computed via SQL aggregation
     * on the entire table, ensuring accuracy regardless of pagination state.
     */
    fun loadStats() {
        currentOffset = 0
        viewModelScope.launch(Dispatchers.IO) {
            _totalSessionCount.value = dao.getSessionCount()
            _globalBestWpm.value = dao.getGlobalBestWpm()?.toInt() ?: 0
            _globalAvgAccuracy.value = dao.getGlobalAvgAccuracy()?.toInt() ?: 0
            _globalTotalDuration.value = dao.getGlobalTotalDuration() ?: 0L

            _chartSessions.value = dao.getLastSessionsForChart(10).reversed()

            val page = dao.getSessionsWithTitlePaged(pageSize, 0)
            _sessionsWithTitle.value = page
            currentOffset = page.size
            _hasMoreSessions.value = currentOffset < _totalSessionCount.value
        }
    }

    /** Loads the next page of sessions and appends it to the existing list. */
    fun loadMoreSessions() {
        viewModelScope.launch(Dispatchers.IO) {
            val page = dao.getSessionsWithTitlePaged(pageSize, currentOffset)
            _sessionsWithTitle.value = _sessionsWithTitle.value + page
            currentOffset += page.size
            _hasMoreSessions.value = currentOffset < _totalSessionCount.value
        }
    }

    /**
     * Records a completed typing session to the database.
     *
     * @param textId ID of the text that was typed.
     * @param durationMillis Session duration in milliseconds.
     * @param wpm Words per minute achieved.
     * @param accuracy Accuracy percentage (0–100).
     */
    fun saveSession(textId: Long, durationMillis: Long, wpm: Double, accuracy: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertSession(SessionEntity(textId = textId, duration = durationMillis, wpm = wpm, accuracy = accuracy, timeStamp = System.currentTimeMillis()))
        }
    }

    /**
     * Deletes a custom text and all its associated sessions (CASCADE).
     *
     * Refreshes the custom texts list after deletion.
     *
     * @param id The text's database ID.
     */
    fun deleteCustomText(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteText(id)
            loadTextsByCategory("textes personnalisées")
        }
    }
}