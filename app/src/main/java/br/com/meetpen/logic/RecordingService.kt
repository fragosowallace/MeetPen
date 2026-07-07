package br.com.meetpen.logic

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.edit
import br.com.meetpen.MainActivity
import br.com.meetpen.R
import br.com.meetpen.data.AppDatabase
import br.com.meetpen.data.Recording
import br.com.meetpen.widget.MeetPenWidget
import br.com.meetpen.widget.MeetPenWidgetStateDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class RecordingService : Service() {
    private var recorder: AndroidAudioRecorder? = null
    private var audioFile: File? = null
    private var startTime: Long = 0
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val CHANNEL_ID = "recording_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // CHAMADA OBRIGATÓRIA IMEDIATA PARA EVITAR CRASH NO ANDROID 12+
        showNotification("Preparando...")

        when (intent?.action) {
            "START" -> {
                serviceScope.launch {
                    startRecording()
                }
            }
            "STOP" -> {
                serviceScope.launch {
                    stopRecording()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun showNotification(text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Meet Pen")
            .setContentText(text)
            .setSmallIcon(R.drawable.app_icon)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }

    private suspend fun startRecording() {
        try {
            setRecordingState("PREPARING")
            delay(500) // Simula um tempo de preparação visual
            
            audioFile = File(cacheDir, "recording_${System.currentTimeMillis()}.m4a")
            recorder = AndroidAudioRecorder(this).apply {
                start(audioFile!!)
            }
            startTime = System.currentTimeMillis()
            showNotification("Gravando áudio...")
            setRecordingState("RECORDING")
        } catch (e: Exception) {
            Log.e("MeetPen", "Erro ao iniciar: ${e.message}")
            setRecordingState("IDLE")
            stopSelf()
        }
    }

    private suspend fun stopRecording() {
        // Se for clicar rápido demais, esperamos o tempo mínimo de 1.5s
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed < 1500) {
            delay(1500 - elapsed)
        }

        setRecordingState("SAVING")
        showNotification("Salvando gravação...")
        
        try {
            recorder?.stop()
            delay(500) // Feedback visual do estado SAVING
        } catch (e: Exception) {
            Log.e("MeetPen", "Erro ao parar: ${e.message}")
        }
        
        audioFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                val db = AppDatabase.getDatabase(this@RecordingService)
                val newRecording = Recording(
                    title = "Gravação via Widget",
                    filePath = file.absolutePath,
                    timestamp = System.currentTimeMillis(),
                    transcription = "Áudio gravado em background"
                )
                db.recordingDao().insert(newRecording)
            }
        }
        
        setRecordingState("IDLE")
        stopForeground(true)
        stopSelf()
    }

    // suspend (em vez de launch em paralelo) garante que a atualização do widget
    // termina antes de stopSelf()/onDestroy() cancelarem o escopo
    private suspend fun setRecordingState(state: String) {
        val manager = GlanceAppWidgetManager(this@RecordingService)
        val ids = manager.getGlanceIds(MeetPenWidget::class.java)
        ids.forEach { id ->
            updateAppWidgetState(this@RecordingService, MeetPenWidgetStateDefinition, id) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[MeetPenWidgetStateDefinition.RECORDING_STATE_KEY] = state
                }
            }
            MeetPenWidget().update(this@RecordingService, id)
        }
    }

    override fun onDestroy() {
        // Garante que o MediaRecorder/microfone não fica retido se o serviço morrer
        recorder?.stop()
        recorder = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Gravação MeetPen", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
