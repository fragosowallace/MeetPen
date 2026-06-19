package br.com.meetpen.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.meetpen.logic.AndroidAudioRecorder
import br.com.meetpen.ui.components.FluidFeatherAnimation
import br.com.meetpen.ui.theme.AmberMain
import br.com.meetpen.ui.theme.MidnightBg
import br.com.meetpen.ui.theme.TextSecondary
import br.com.meetpen.ui.MainViewModel
import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File

@Composable
fun RecordingScreen(
    viewModel: MainViewModel,
    onStopRecording: () -> Unit
) {
    val context = LocalContext.current
    val recorder by remember { mutableStateOf(AndroidAudioRecorder(context)) }

    var audioFile by remember { mutableStateOf<File?>(null) }
    var hasPermission by remember { mutableStateOf(false) }
    var seconds by remember { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    val hasApiKey = remember { viewModel.hasApiKey() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasPermission = isGranted }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && audioFile == null) {
            if (viewModel.canRecord()) {
                val file = File(context.cacheDir, "recording_${System.currentTimeMillis()}.m4a")
                audioFile = file
                recorder.start(file)
            } else {
                onStopRecording()
            }
        }
    }

    LaunchedEffect(hasPermission, isPaused) {
        if (hasPermission && !isPaused) {
            while (true) {
                delay(1000)
                seconds++
            }
        }
    }

    val togglePause = {
        if (isPaused) recorder.resume() else recorder.pause()
        isPaused = !isPaused
    }

    val stopAndSave = {
        if (hasPermission) {
            recorder.stop()
            audioFile?.let { file ->
                viewModel.saveAndTranscribe(title = "Gravação Nova", path = file.absolutePath)
            }
        }
        onStopRecording()
    }

    val discardAndBack = {
        recorder.stop()
        audioFile?.delete()
        onStopRecording()
    }

    val timeFormatted = String.format("%02d:%02d", seconds / 60, seconds % 60)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBg)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // ── Indicador de status ────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isPaused) {
                RecordingDot()
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = if (isPaused) "Pausado" else "Gravando...",
                color = if (isPaused) TextSecondary else AmberMain,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Animação de pena ───────────────────────────────────────────
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(AmberMain.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
            )
            FluidFeatherAnimation()
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Card de timer ──────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = AmberMain)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 56.sp,
                    color = Color.White
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(15) { index -> WaveBar(index, isPaused) }
                }
                Button(
                    onClick = togglePause,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isPaused) "continuar" else "pausar", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Aviso sobre o que acontece ao salvar ───────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.04f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, Color.White.copy(alpha = 0.07f)
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (hasApiKey) Icons.Default.AutoAwesome else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (hasApiKey) AmberMain else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (hasApiKey)
                        "Ao salvar, a transcrição com IA será iniciada automaticamente."
                    else
                        "Ao salvar, você poderá escolher como transcrever na nota.",
                    color = if (hasApiKey) AmberMain.copy(alpha = 0.85f) else TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Botões de ação ─────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Descartar
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = discardAndBack,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Descartar",
                        tint = Color.Red.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text("Descartar", color = Color.Gray, fontSize = 11.sp)
            }

            // Parar e salvar
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingActionButton(
                    onClick = stopAndSave,
                    containerColor = Color.White,
                    contentColor = AmberMain,
                    shape = CircleShape,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Parar e Salvar",
                        modifier = Modifier.size(36.dp),
                        tint = AmberMain
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Salvar",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Espaço para equilíbrio visual
            Box(modifier = Modifier.size(56.dp))
        }
    }
}

@Composable
private fun RecordingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "dot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(AmberMain.copy(alpha = alpha))
    )
}

@Composable
fun WaveBar(index: Int, isPaused: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val heightScale by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400 + (index * 50)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar"
    )
    val finalScale = if (isPaused) 0.2f else heightScale
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .width(4.dp)
            .fillMaxHeight(finalScale * 0.8f)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.7f))
    )
}
