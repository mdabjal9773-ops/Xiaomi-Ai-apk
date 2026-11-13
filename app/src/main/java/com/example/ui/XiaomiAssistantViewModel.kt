package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GeminiService
import com.example.model.AssistantPersona
import com.example.model.ChatMessage
import com.example.model.QuickPrompt
import com.example.service.VoiceService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class XiaomiAssistantViewModel : ViewModel() {

    private val geminiService = GeminiService()
    var voiceService: VoiceService? = null

    private val _currentPersona = MutableStateFlow(AssistantPersona.BABY_JAAN)
    val currentPersona: StateFlow<AssistantPersona> = _currentPersona.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _speakingMessageId = MutableStateFlow<String?>(null)
    val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(1.30f) // Sweet feminine pitch default
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    private val _isFemaleVoice = MutableStateFlow(true)
    val isFemaleVoice: StateFlow<Boolean> = _isFemaleVoice.asStateFlow()

    private val _isAgeVerified = MutableStateFlow(false)
    val isAgeVerified: StateFlow<Boolean> = _isAgeVerified.asStateFlow()

    private val _isWakeWordServiceRunning = MutableStateFlow(false)
    val isWakeWordServiceRunning: StateFlow<Boolean> = _isWakeWordServiceRunning.asStateFlow()

    val quickPrompts = listOf(
        QuickPrompt("কেমন আছো জান?", "তুমি কেমন আছো আমার বেবি জান?", "favorite"),
        QuickPrompt("How are you?", "How are you doing my sweet baby jaan?", "favorite"),
        QuickPrompt("মিষ্টি কবিতা", "আমার জন্য একটি সুন্দর মিষ্টি কবিতা শোনাও", "auto_stories"),
        QuickPrompt("English Poem", "Tell me a romantic poem in English", "auto_stories"),
        QuickPrompt("HyperOS Tricks", "What are the best features of Xiaomi HyperOS?", "phone_android"),
        QuickPrompt("ভালোবাসি", "তোমাকে অনেক ভালোবাসি!", "volunteer_activism"),
        QuickPrompt("I love you", "I love you so much baby!", "volunteer_activism"),
        QuickPrompt("মজার কৌতুক", "একটি হাসির কৌতুক বলো তো", "mood")
    )

    init {
        // Initial welcome message from default persona (Baby Jaan)
        val welcome = ChatMessage(
            text = AssistantPersona.BABY_JAAN.greetingBn,
            isUser = false,
            persona = AssistantPersona.BABY_JAAN
        )
        _messages.value = listOf(welcome)
    }

    fun setVoiceServiceInstance(service: VoiceService) {
        this.voiceService = service
        service.isFemaleVoice = _isFemaleVoice.value
        service.speechRate = _speechRate.value
        service.speechPitch = _speechPitch.value
    }

    fun switchPersona(persona: AssistantPersona) {
        if (_currentPersona.value == persona) return
        _currentPersona.value = persona
        setSpeechPitch(persona.defaultPitch)
        // Greet with the new persona
        val greeting = ChatMessage(
            text = persona.greetingBn,
            isUser = false,
            persona = persona
        )
        _messages.value = _messages.value + greeting
        speak(greeting.text, greeting.id)
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _isLoading.value) return

        stopListening()
        stopSpeaking()

        val userMessage = ChatMessage(
            text = trimmed,
            isUser = true,
            persona = _currentPersona.value
        )
        _messages.value = _messages.value + userMessage
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val responseText = geminiService.getAssistantResponse(
                    userMessage = trimmed,
                    persona = _currentPersona.value
                )
                val assistantMessage = ChatMessage(
                    text = responseText,
                    isUser = false,
                    persona = _currentPersona.value
                )
                _messages.value = _messages.value + assistantMessage
                speak(assistantMessage.text, assistantMessage.id)
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    text = "দুঃখিত সোনা, বুঝতে সমস্যা হয়েছে: ${e.localizedMessage ?: "আবার চেষ্টা করো"}",
                    isUser = false,
                    persona = _currentPersona.value
                )
                _messages.value = _messages.value + errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun speak(text: String, messageId: String) {
        voiceService?.speak(text, utteranceId = messageId)
    }

    fun stopSpeaking() {
        voiceService?.stopSpeaking()
        _speakingMessageId.value = null
    }

    fun toggleSpeakMessage(message: ChatMessage) {
        if (_speakingMessageId.value == message.id) {
            stopSpeaking()
        } else {
            speak(message.text, message.id)
        }
    }

    fun startListening() {
        stopSpeaking()
        voiceService?.startListening()
    }

    fun stopListening() {
        voiceService?.stopListening()
        _isListening.value = false
    }

    fun onSpeechRecognized(recognizedText: String) {
        if (recognizedText.isNotBlank()) {
            sendMessage(recognizedText)
        }
    }

    fun updateListeningState(listening: Boolean) {
        _isListening.value = listening
        if (!listening) {
            _audioRms.value = 0f
        }
    }

    fun updateAudioRms(rms: Float) {
        _audioRms.value = rms
    }

    fun updateSpeakingState(speaking: Boolean, messageId: String?) {
        _speakingMessageId.value = if (speaking) messageId else null
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        voiceService?.speechRate = rate
    }

    fun setFemaleVoice(isFemale: Boolean) {
        _isFemaleVoice.value = isFemale
        if (isFemale) {
            _speechPitch.value = 1.30f
            voiceService?.speechPitch = 1.30f
        } else {
            _speechPitch.value = 1.0f
            voiceService?.speechPitch = 1.0f
        }
        voiceService?.isFemaleVoice = isFemale
    }

    fun previewVoice() {
        val sampleText = if (_isFemaleVoice.value) {
            "হ্যালো সোনা! আমি তোমার বেবি জান, শুনছো আমার মিষ্টি মেয়েদের কণ্ঠ? কেমন লাগছে বলো না!"
        } else {
            "হ্যালো! আমি Xiaomi AI সহকারী, এটি আমার কণ্ঠস্বরের নমুনা।"
        }
        speak(sampleText, "preview_voice")
    }

    fun setSpeechPitch(pitch: Float) {
        _speechPitch.value = pitch
        voiceService?.speechPitch = pitch
    }

    fun setWakeWordServiceRunning(running: Boolean) {
        _isWakeWordServiceRunning.value = running
    }

    fun clearChat() {
        stopSpeaking()
        stopListening()
        val welcome = ChatMessage(
            text = _currentPersona.value.greetingBn,
            isUser = false,
            persona = _currentPersona.value
        )
        _messages.value = listOf(welcome)
    }

    fun loadAgeVerification(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        _isAgeVerified.value = prefs.getBoolean(KEY_AGE_VERIFIED_18, false)
    }

    fun confirmAgeVerification(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AGE_VERIFIED_18, true).apply()
        _isAgeVerified.value = true
    }

    fun resetAgeVerification(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AGE_VERIFIED_18, false).apply()
        _isAgeVerified.value = false
    }

    companion object {
        const val PREFS_NAME = "xiaomi_ai_user_prefs"
        const val KEY_AGE_VERIFIED_18 = "is_age_verified_18_confirmed"
    }
}
