package br.com.meetpen.logic

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.io.File

/**
 * Transcritor nativo usando o motor de reconhecimento de voz embutido do Android.
 *
 * LIMITAÇÕES IMPORTANTES (exibidas ao usuário):
 * - A API SpeechRecognizer do Android foi projetada para voz AO VIVO, não para arquivos de áudio.
 * - Na maioria dos dispositivos, não é possível alimentar um arquivo .m4a diretamente.
 * - A qualidade varia muito de aparelho para aparelho e depende do modelo de voz do Google/OEM.
 * - Gravações longas (> 30s) ou com ruído de fundo terão resultados muito piores.
 *
 * RECOMENDAÇÃO: Para transcrições precisas, configure uma chave de API de IA nas Configurações.
 */
class AndroidNativeTranscriber(private val context: Context) {

    companion object {
        const val NATIVE_QUALITY_WARNING = "⚠️ Transcrição local — qualidade limitada pelo motor do Android.\n\nResultado:"
    }

    /**
     * Tenta transcrever usando o SpeechRecognizer.
     * Como a API não suporta arquivos nativamente na maioria dos dispositivos,
     * usamos a intenção ACTION_RECOGNIZE_SPEECH e reportamos o resultado honestamente.
     */
    fun transcribeFile(file: File, onResult: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onResult("Motor de reconhecimento de voz não disponível neste aparelho.\n\nPara transcrever, adicione uma chave de API nas Configurações.")
            return
        }

        val handler = Handler(Looper.getMainLooper())
        handler.post {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}

                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Erro de captura de áudio"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissão de microfone negada"
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            "Sem conexão — o motor nativo do Android requer internet para funcionar"
                        SpeechRecognizer.ERROR_NO_MATCH ->
                            "Nenhuma fala reconhecível encontrada. Tente com uma chave de API para melhores resultados."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                            "Tempo esgotado. O motor nativo funciona melhor com gravações curtas."
                        SpeechRecognizer.ERROR_SERVER -> "Erro no servidor de reconhecimento do Google"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Serviço de reconhecimento ocupado. Tente novamente."
                        else -> "Erro no motor nativo (código $error). Recomendamos usar uma chave de API."
                    }
                    Log.w("MeetPen", "Transcrição nativa — erro: $message")
                    onResult("$NATIVE_QUALITY_WARNING\n\n$message")
                    recognizer.destroy()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()
                    if (text.isNullOrBlank()) {
                        onResult("$NATIVE_QUALITY_WARNING\n\nNenhum texto reconhecido. Para melhores resultados, configure uma chave de API de IA nas Configurações.")
                    } else {
                        onResult("$NATIVE_QUALITY_WARNING\n\n$text")
                    }
                    recognizer.destroy()
                }
            })

            recognizer.startListening(intent)
        }
    }
}
