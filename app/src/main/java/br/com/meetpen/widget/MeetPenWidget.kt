package br.com.meetpen.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import br.com.meetpen.R

class MeetPenWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = MeetPenWidgetStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val state = prefs[MeetPenWidgetStateDefinition.RECORDING_STATE_KEY] ?: "IDLE"
            MeetPenWidgetContent(state)
        }
    }
}

@Composable
fun MeetPenWidgetContent(state: String) {
    val bgColorRes = when (state) {
        "PREPARING" -> R.drawable.bg_widget_circle_amber
        "RECORDING" -> R.drawable.bg_widget_circle_red
        "SAVING" -> R.drawable.bg_widget_circle_gray
        else -> R.drawable.bg_widget_circle
    }
    
    val label = when (state) {
        "PREPARING" -> "..."
        "RECORDING" -> "PARAR"
        "SAVING" -> "OK"
        else -> "GRAVAR"
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier
                .size(100.dp)
                .background(ImageProvider(bgColorRes))
                .clickable(actionRunCallback<ToggleRecordingCallback>()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.app_icon),
                contentDescription = null,
                modifier = GlanceModifier.size(32.dp)
            )
            Text(
                text = label,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
