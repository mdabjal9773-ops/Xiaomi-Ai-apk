package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

class VoiceService(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onAudioRmsChanged: (Float) -> Unit,
    private val onSpeakingStateChanged: (Boolean, String?) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var currentSpeakingId: String? = null

    var isFemaleVoice: Boolean = true
        set(value) {
            field = value
            applyVoiceConfiguration()
        }

    var speechRate: Float = 1.0f
        set(value) {
            field = value
            tts?.setSpeechRate(value)
        }

    var speechPitch: Float = 1.30f // Sweet feminine pitch default
        set(value) {
            field = value
            tts?.setPitch(value)
        }

    init {
        tts = TextToSpeech(context.applicationContext, this)
        initSpeechRecognizer()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            applyVoiceConfiguration()

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onSpeakingStateChanged(true, utteranceId)
                }

                override fun onDone(utteranceId: String?) {
                    currentSpeakingId = null
                    onSpeakingStateChanged(false, null)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    currentSpeakingId = null
                    onSpeakingStateChanged(false, null)
                }
            })
            Log.i(TAG, "TextToSpeech initialized with English + Bengali and female voice")
        } else {
            Log.e(TAG, "TextToSpeech initialization failed: $status")
        }
    }

    fun applyVoiceConfiguration(forEnglish: Boolean = false) {
        if (!isTtsReady || tts == null) return

        if (forEnglish) {
            tts?.language = Locale.US
        } else {
            val bnBd = Locale.forLanguageTag("bn-BD")
            val bnIn = Locale.forLanguageTag("bn-IN")
            val bn = Locale.forLanguageTag("bn")

            var langResult = tts?.setLanguage(bnBd)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                langResult = tts?.setLanguage(bnIn)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(bn)
                }
            }
        }

        // Try to select an authentic female voice from TTS engine
        try {
            val targetLang = if (forEnglish) "en" else "bn"
            val availableVoices = tts?.voices
            if (!availableVoices.isNullOrEmpty()) {
                val matchedVoice = if (isFemaleVoice) {
                    availableVoices.firstOrNull { voice ->
                        voice.locale.language == targetLang && isFemaleVoiceName(voice.name)
                    } ?: availableVoices.firstOrNull { voice ->
                        voice.locale.language == targetLang
                    } ?: availableVoices.firstOrNull { voice ->
                        isFemaleVoiceName(voice.name)
                    }
                } else {
                    availableVoices.firstOrNull { voice ->
                        voice.locale.language == targetLang && !isFemaleVoiceName(voice.name)
                    } ?: availableVoices.firstOrNull { voice ->
                        voice.locale.language == targetLang
                    }
                }

                if (matchedVoice != null) {
                    tts?.voice = matchedVoice
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error selecting specific TTS voice", e)
        }

        // Apply feminine vocal frequency
        val finalPitch = if (isFemaleVoice) speechPitch.coerceAtLeast(1.25f) else speechPitch
        tts?.setPitch(finalPitch)
        tts?.setSpeechRate(speechRate)
    }

    private fun isFemaleVoiceName(name: String): Boolean {
        val lower = name.lowercase()
        return lower.contains("female") ||
                lower.contains("bif") ||
                lower.contains("ban") ||
                lower.contains("woman") ||
                lower.contains("girl") ||
                lower.contains("sfg") ||
                lower.contains("f0")
    }

    fun isEnglishText(text: String): Boolean {
        val englishCharCount = text.count { it in 'a'..'z' || it in 'A'..'Z' }
        val bengaliCharCount = text.count { it in '\u0980'..'\u09FF' }
        return englishCharCount > bengaliCharCount
    }

    fun speak(text: String, utteranceId: String = "xiaomi_speech") {
        if (isTtsReady && tts != null) {
            val isEng = isEnglishText(text)
            applyVoiceConfiguration(forEnglish = isEng)
            currentSpeakingId = utteranceId
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isListening = true
                            onListeningStateChanged(true)
                        }

                        override fun onBeginningOfSpeech() {}

                        override fun onRmsChanged(rmsdB: Float) {
                            onAudioRmsChanged(rmsdB)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            isListening = false
                            onListeningStateChanged(false)
                        }

                        override fun onError(error: Int) {
                            Log.w(TAG, "SpeechRecognizer error: $error")
                            isListening = false
                            onListeningStateChanged(false)
                        }

                        override fun onResults(results: Bundle?) {
                            isListening = false
                            onListeningStateChanged(false)
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                onSpeechResult(matches[0])
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                onSpeechResult(matches[0])
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not create SpeechRecognizer", e)
            }
        }
    }

    fun startListening() {
        stopSpeaking()
        if (speechRecognizer != null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                // Support Bengali and English simultaneously
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                putExtra(
                    "android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES",
                    arrayOf("en-US", "en-IN", "bn-IN", "bn")
                )
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "বাংলা বা ইংরেজিতে বলুন / Speak in English or Bengali...")
            }
            try {
                speechRecognizer?.startListening(intent)
                isListening = true
                onListeningStateChanged(true)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start listening", e)
                onListeningStateChanged(false)
            }
        } else {
            onListeningStateChanged(false)
        }
    }

    fun stopListening() {
        if (isListening) {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping recognizer", e)
            }
            isListening = false
            onListeningStateChanged(false)
        }
    }

    fun stopSpeaking() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
            currentSpeakingId = null
            onSpeakingStateChanged(false, null)
        }
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking == true

    fun destroy() {
        stopSpeaking()
        tts?.shutdown()
        tts = null
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying speech recognizer", e)
        }
        speechRecognizer = null
    }

    companion object {
        private const val TAG = "VoiceService"
    }
}
