package com.suggestbox.ui.components

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

enum class VoiceErrorType {
    NONE,
    NO_MATCH,
    NETWORK,
    PERMISSION_DENIED,
    UNAVAILABLE,
}

/**
 * Manages Android in-app [SpeechRecognizer] lifecycle with live partial results,
 * real-time microphone RMS audio level, automatic fallback to Google Voice Input dialog,
 * and multi-language support.
 */
@Stable
class VoiceInputController(
    private val context: Context,
    private val onFinalTranscript: (String) -> Unit,
    private val onFallbackToSystemDialog: (Intent) -> Unit,
) {
    var isListening by mutableStateOf(false)
        private set

    var partialText by mutableStateOf("")
        private set

    var audioLevel by mutableFloatStateOf(0f)
        private set

    var errorType by mutableStateOf(VoiceErrorType.NONE)
        private set

    private var recognizer: SpeechRecognizer? = null

    fun createRecognitionIntent(languageTag: String = resolveBcp47Tag()): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 6000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
        }
    }

    fun launchSystemVoiceDialog() {
        onFallbackToSystemDialog(createRecognitionIntent())
    }

    fun startListening() {
        errorType = VoiceErrorType.NONE
        partialText = ""
        audioLevel = 0f

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            launchSystemVoiceDialog()
            return
        }

        runCatching {
            recognizer?.destroy()
            val sr = SpeechRecognizer.createSpeechRecognizer(context)
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                }

                override fun onBeginningOfSpeech() {
                    isListening = true
                }

                override fun onRmsChanged(rmsdB: Float) {
                    audioLevel = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                }

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    audioLevel = 0f
                }

                override fun onError(error: Int) {
                    isListening = false
                    audioLevel = 0f
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                        -> {
                            if (partialText.isNotBlank()) {
                                onFinalTranscript(partialText)
                                partialText = ""
                            } else {
                                // Fallback to Google Voice Input dialog if in-app recognizer missed words
                                launchSystemVoiceDialog()
                            }
                        }

                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                        -> {
                            errorType = VoiceErrorType.NETWORK
                        }

                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            errorType = VoiceErrorType.PERMISSION_DENIED
                        }

                        SpeechRecognizer.ERROR_CLIENT,
                        SpeechRecognizer.ERROR_AUDIO,
                        SpeechRecognizer.ERROR_SERVER,
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
                        -> {
                            launchSystemVoiceDialog()
                        }

                        else -> {
                            launchSystemVoiceDialog()
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    audioLevel = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull().orEmpty().trim()
                    partialText = ""
                    if (best.isNotEmpty()) {
                        onFinalTranscript(best)
                    } else {
                        launchSystemVoiceDialog()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull().orEmpty()
                    if (text.isNotBlank()) {
                        partialText = text
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            recognizer = sr
            isListening = true
            sr.startListening(createRecognitionIntent())
        }.onFailure {
            isListening = false
            launchSystemVoiceDialog()
        }
    }

    fun stopListening() {
        isListening = false
        audioLevel = 0f
        runCatching { recognizer?.stopListening() }
    }

    fun reportExternalError(type: VoiceErrorType) {
        isListening = false
        errorType = type
    }

    fun destroy() {
        isListening = false
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    private fun resolveBcp47Tag(): String {
        val locale = Locale.getDefault()
        return when (locale.language) {
            "hi" -> "hi-IN"
            "kn" -> "kn-IN"
            else -> if (locale.country.isNotBlank()) locale.toLanguageTag() else "en-US"
        }
    }
}

@Composable
fun rememberVoiceInputController(
    onFinalTranscript: (String) -> Unit,
    onFallbackToSystemDialog: (Intent) -> Unit,
): VoiceInputController {
    val context = LocalContext.current
    val controller = remember(context) {
        VoiceInputController(
            context = context,
            onFinalTranscript = onFinalTranscript,
            onFallbackToSystemDialog = onFallbackToSystemDialog,
        )
    }
    DisposableEffect(controller) {
        onDispose { controller.destroy() }
    }
    return controller
}
