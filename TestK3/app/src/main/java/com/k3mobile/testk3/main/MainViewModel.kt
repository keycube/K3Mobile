package com.k3mobile.testk3.ui

import android.app.Application
import android.speech.tts.TextToSpeech
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

    init {
        tts = TextToSpeech(application, this)
    }

    override fun onInit(status: Int) {
        tts?.setSpeechRate(0.65f)
        if (status == TextToSpeech.SUCCESS) {
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
            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "UtteranceID")
        }
    }

    fun stopSpeaking() {
        if (isTtsReady) tts?.stop()
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

    // Gardé pour la compatibilité si besoin
    private val _sessions = MutableStateFlow<List<SessionEntity>>(emptyList())
    val sessions = _sessions.asStateFlow()

    // Sessions enrichies avec le titre du texte
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