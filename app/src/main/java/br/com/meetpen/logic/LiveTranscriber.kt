package br.com.meetpen.logic

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Transcritor ao-vivo usando SpeechRecognizer do Android.
 * Deve ser iniciado JUNTO com a gravação de áudio para capturar a fala em tempo real.
 */
class LiveTranscriber(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false
    private var onPartialUpdate: ((String) -> Unit)? = null
    private var onFinalResult: ((String) -> Unit)? = null
    private val fullTranscript = StringBuilder()

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit
    ) {
        if (!isAvailable()) {
            onFinal("")
            return
        }
        onPartialUpdate = onPartial
        onFinalResult = onFinal
        fullTranscript.clear()
        isListening = true
        startListeningCycle()
    }

    private fun startListeningCycle() {
        if (!isListening) return.also {
            // mark as started before posting
        }

        mainHandler.post {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
            }

            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull() ?: return
                    val preview = buildString {
                        if (fullTranscript.isNotEmpty()) {
                            append(fullTranscript)
                            append(" ")
                        }
                        append(partial)
                    }
                    onPartialUpdate?.invoke(preview)
                }

                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    Log.w("LiveTranscriber", "Erro $error — reiniciando ciclo se ainda ativo")
                    // Reinicia automaticamente em caso de timeout ou erro leve
                    if (isListening && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        mainHandler.postDelayed({ startListeningCycle() }, 300)
                    }
                }

                override fun onResults(results: Bundle?) {
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (!text.isNullOrBlank()) {
                        if (fullTranscript.isNotEmpty()) fullTranscript.append(" ")
                        fullTranscript.append(text)
                        onPartialUpdate?.invoke(fullTranscript.toString())
                    }
                    // Reinicia para capturar mais fala
                    if (isListening) {
                        mainHandler.postDelayed({ startListeningCycle() }, 100)
                    }
                }
            })

            recognizer?.startListening(intent)
        }
    }

    fun stop(): String {
        isListening = false
        mainHandler.post {
            recognizer?.stopListening()
            recognizer?.destroy()
            recognizer = null
        }
        val result = fullTranscript.toString().trim()
        onFinalResult?.invoke(result)
        return result
    }

    fun beginSession() {
        isListening = true
        startListeningCycle()
    }

    fun pauseListening() {
        mainHandler.post { recognizer?.stopListening() }
    }

    fun resumeListening() {
        if (isListening) startListeningCycle()
    }
}
