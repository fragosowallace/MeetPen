package br.com.meetpen.logic

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import java.io.File

enum class VoiceEffect(val pitch: Float, val speed: Float, val label: String) {
    NONE(1.0f, 1.0f, "Original"),
    SQUIRREL(2.0f, 1.0f, "Esquilo"),
    MONSTER(0.6f, 0.9f, "Monstro"),
    ROBOT(1.5f, 0.8f, "Robô"),
    DRUNK(0.8f, 0.7f, "Lento")
}

class AndroidAudioPlayer(private val context: Context) {
    private var player: MediaPlayer? = null
    private var currentEffect: VoiceEffect = VoiceEffect.NONE

    fun playFile(file: File, speed: Float = 1.0f, effect: VoiceEffect = VoiceEffect.NONE, onFinished: () -> Unit) {
        stop()
        currentEffect = effect
        
        MediaPlayer.create(context, android.net.Uri.fromFile(file))?.apply {
            player = this
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Combina a velocidade do usuário com a do efeito
                val finalSpeed = speed * effect.speed
                playbackParams = PlaybackParams()
                    .setPitch(effect.pitch)
                    .setSpeed(finalSpeed)
            }
            setOnCompletionListener { 
                onFinished()
                stop()
            }
            start()
        }
    }

    fun stop() {
        player?.stop()
        player?.release()
        player = null
    }
    
    fun setParams(speed: Float, effect: VoiceEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            player?.let {
                if (it.isPlaying) {
                    val finalSpeed = speed * effect.speed
                    it.playbackParams = it.playbackParams
                        .setPitch(effect.pitch)
                        .setSpeed(finalSpeed)
                }
            }
        }
    }
}
