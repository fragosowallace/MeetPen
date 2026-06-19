package br.com.meetpen.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.state.GlanceStateDefinition
import java.io.File

object MeetPenWidgetStateDefinition : GlanceStateDefinition<Preferences> {
    private val Context.dataStore by preferencesDataStore(name = "meetpen_widget_state")
    
    val RECORDING_STATE_KEY = stringPreferencesKey("recording_state")

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<Preferences> {
        return context.dataStore
    }

    override fun getLocation(context: Context, fileKey: String): File {
        return File(context.filesDir, "datastore/$fileKey")
    }
}
