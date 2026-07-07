package br.com.meetpen.widget

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import br.com.meetpen.MainActivity
import br.com.meetpen.logic.RecordingService

class ToggleRecordingCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefs = getAppWidgetState(context, MeetPenWidgetStateDefinition, glanceId)
        val currentState = prefs[MeetPenWidgetStateDefinition.RECORDING_STATE_KEY] ?: "IDLE"
        
        val intent = Intent(context, RecordingService::class.java)
        val newState: String
        
        when (currentState) {
            "IDLE" -> {
                // Sem RECORD_AUDIO o foreground service de microfone falharia
                // (SecurityException no Android 14+): abre o app para o usuário
                // conceder a permissão na tela de gravação
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    context.startActivity(
                        Intent(context, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                    return
                }
                newState = "PREPARING"
                intent.action = "START"
            }
            "RECORDING" -> {
                newState = "SAVING"
                intent.action = "STOP"
            }
            else -> return // Ignora se estiver transicionando
        }
        
        // Atualiza o estado no DataStore
        updateAppWidgetState(context, MeetPenWidgetStateDefinition, glanceId) { p ->
            p.toMutablePreferences().apply {
                this[MeetPenWidgetStateDefinition.RECORDING_STATE_KEY] = newState
            }
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        
        MeetPenWidget().update(context, glanceId)
    }
}
