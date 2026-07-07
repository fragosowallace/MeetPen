package br.com.meetpen.logic

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class AndroidAudioRecorder(private val context: Context) {

    companion object {
        // Garante que apenas um gravador (tela ou widget/serviço) use o microfone por vez
        private val micInUse = AtomicBoolean(false)
    }

    private var recorder: MediaRecorder? = null
    private var acquiredMic = false

    fun start(outputFile: File) {
        check(micInUse.compareAndSet(false, true)) {
            "Já existe uma gravação em andamento"
        }
        acquiredMic = true

        val newRecorder = createRecorder()
        try {
            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)

                // Mudança para MPEG_4 / AAC: Formato universal e altamente compatível
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

                // Grava em 16kHz → compatível diretamente com o modelo Vosk (sem resampling)
                // Qualidade de voz é idêntica; o overhead de resampling é eliminado
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(64000)

                setOutputFile(outputFile.absolutePath)

                prepare()
                start()
            }
            recorder = newRecorder
        } catch (e: Exception) {
            newRecorder.release()
            releaseMic()
            throw e
        }
    }

    fun pause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recorder?.pause()
        }
    }

    fun resume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recorder?.resume()
        }
    }

    fun stop() {
        try {
            recorder?.stop()
        } catch (e: Exception) {
            // Caso o áudio seja muito curto
        }
        recorder?.reset()
        recorder?.release()
        recorder = null
        releaseMic()
    }

    private fun releaseMic() {
        if (acquiredMic) {
            micInUse.set(false)
            acquiredMic = false
        }
    }

    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }
}
