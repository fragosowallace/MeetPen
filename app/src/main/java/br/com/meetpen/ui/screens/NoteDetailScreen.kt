package br.com.meetpen.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.meetpen.data.Recording
import br.com.meetpen.ui.MainViewModel
import org.json.JSONArray
import org.json.JSONObject
import br.com.meetpen.ui.theme.AmberMain
import br.com.meetpen.ui.theme.MidnightBg
import br.com.meetpen.logic.VoiceEffect
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.FileProvider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Int,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val recordings by viewModel.recordings.collectAsState()
    val note = recordings.find { it.id == noteId }
    val currentSpeed by viewModel.currentSpeed
    val isPlaying = viewModel.currentlyPlayingPath.value == note?.filePath
    val transcriptionMode by viewModel.transcriptionMode
 
    var showEditDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf("") }
 
    if (note == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Compartilhar") },
            text = { Text("O que você deseja compartilhar?") },
            confirmButton = {
                TextButton(onClick = {
                    showShareDialog = false
                    val file = File(note.filePath)
                    if (file.exists()) {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "audio/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Compartilhar Áudio"))
                    }
                }) { Text("Áudio") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showShareDialog = false
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, note.transcription)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Compartilhar Texto"))
                }) { Text("Texto") }
            }
        )
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Renomear Nota") },
            text = {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateTitle(note, editedTitle)
                    showEditDialog = false
                }) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancelar") }
            }
        )
    }

    val isTranscribing = note.transcription == "Processando..."
    val isPlaceholder = note.transcription == "Áudio pronto para transcrição" || note.transcription.isEmpty()
    val isFailure = note.transcription.startsWith("Erro") || note.transcription == "Não foi possível entender o áudio."
    val hasTranscription = note.transcription.isNotEmpty() && !isTranscribing && !isFailure && !isPlaceholder
    
    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(note.timestamp))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes da Nota", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartilhar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBg)
            )
        },
        containerColor = MidnightBg
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable {
                    editedTitle = note.title
                    showEditDialog = true
                }
            ) {
                Text(text = note.title, style = MaterialTheme.typography.displaySmall, color = Color.White, fontSize = 28.sp, modifier = Modifier.weight(1f))
                Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
            Text(text = dateStr, color = Color.Gray, style = MaterialTheme.typography.labelLarge)
            
            Spacer(modifier = Modifier.height(16.dp))

            // Categorias
            val categories = listOf("Geral", "Trabalho", "Diário", "Ideias", "Reunião")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { cat ->
                    val isSelected = note.category == cat || (note.category.isEmpty() && cat == "Geral")
                    Surface(
                        modifier = Modifier.clickable { viewModel.updateCategory(note, cat) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) AmberMain else Color.White.copy(alpha = 0.05f),
                    ) {
                        Text(text = cat, color = if (isSelected) MidnightBg else Color.Gray, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Player
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.05f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.playAudio(note.filePath) }) {
                                Icon(imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, tint = AmberMain, modifier = Modifier.size(32.dp))
                            }
                            Text("Ouvir gravação", color = Color.White, fontWeight = FontWeight.Medium)
                        }
                        Surface(modifier = Modifier.clickable { 
                            val speeds = listOf(0.5f, 1.0f, 1.5f, 2.0f)
                            val nextIdx = (speeds.indexOf(currentSpeed) + 1) % speeds.size
                            viewModel.setPlaybackSpeed(speeds[nextIdx])
                        }.padding(4.dp), shape = RoundedCornerShape(20.dp), color = AmberMain.copy(alpha = 0.2f)) {
                            Text(text = "${currentSpeed}x", color = AmberMain, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Efeitos de Voz", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val currentEffect by viewModel.currentVoiceEffect
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VoiceEffect.values().forEach { effect ->
                            val isSelected = currentEffect == effect
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setVoiceEffect(effect) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) AmberMain else Color.White.copy(alpha = 0.05f),
                            ) {
                                Text(
                                    text = effect.label,
                                    color = if (isSelected) MidnightBg else Color.Gray,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Transcrição
            val voskReady by remember { viewModel.voskModelReady }
            val voskProgress by remember { viewModel.voskDownloadProgress }
            TranscriptionCard(
                title = "Transcrição",
                content = note.transcription,
                isLoading = isTranscribing,
                isFailure = isFailure,
                isPlaceholder = isPlaceholder,
                hasContent = hasTranscription,
                onRetry = { viewModel.transcribe(note) },
                onClear = { viewModel.clearTranscription(note) },
                onNativeTranscribe = {
                    if (voskReady) {
                        viewModel.transcribeOffline(note)
                    } else {
                        onNavigateToSettings()
                    }
                },
                voskModelReady = voskReady,
                voskDownloadProgress = voskProgress,
                hasApiKey = viewModel.hasApiKey(),
                onGoToSettings = onNavigateToSettings,
                transcriptionMode = transcriptionMode
            )

            // Resumo
            if (hasTranscription) {
                val isSummaryLoading = note.summary == "Processando..."
                Spacer(modifier = Modifier.height(16.dp))
                TranscriptionCard(
                    "Resumo da IA", 
                    note.summary, 
                    isSummaryLoading, 
                    false, 
                    note.summary.isEmpty(), 
                    note.summary.isNotEmpty() && !isSummaryLoading, 
                    buttonText = "Gerar Resumo", 
                    onRetry = { viewModel.generateSummary(note) }
                )
            }

            // To-Do List Interativo
            if (hasTranscription) {
                Spacer(modifier = Modifier.height(16.dp))
                InteractiveTodoCard(
                    note, 
                    onToggle = { idx -> viewModel.toggleTodo(note, idx) }, 
                    onGenerate = { viewModel.generateTodo(note) }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun InteractiveTodoCard(recording: Recording, onToggle: (Int) -> Unit, onGenerate: () -> Unit) {
    val isProcessing = recording.todoList == "Processando..."
    val todos: List<Pair<String, Boolean>> = remember(recording.todoList) {
        if (isProcessing) return@remember emptyList()
        try {
            val arr = JSONArray(recording.todoList)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                obj.getString("task") to obj.getBoolean("done")
            }
        } catch (e: Exception) { emptyList() }
    }

    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.05f)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Lista de Tarefas", color = AmberMain, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isProcessing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AmberMain)
                    Spacer(Modifier.width(12.dp))
                    Text("Extraindo tarefas...", color = Color.Gray, fontSize = 14.sp)
                }
            } else if (todos.isEmpty()) {
                Button(onClick = onGenerate, colors = ButtonDefaults.buttonColors(containerColor = AmberMain.copy(alpha = 0.1f))) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = AmberMain)
                    Spacer(Modifier.width(8.dp))
                    Text("Extrair Tarefas da IA", color = AmberMain, fontSize = 14.sp)
                }
            } else {
                todos.forEachIndexed { index, (task, done) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onToggle(index) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = done,
                            onCheckedChange = { onToggle(index) },
                            colors = CheckboxDefaults.colors(checkedColor = AmberMain, uncheckedColor = Color.Gray)
                        )
                        Text(
                            text = task,
                            color = if (done) Color.Gray else Color.White,
                            style = if (done) MaterialTheme.typography.bodyLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TranscriptionCard(
    title: String,
    content: String,
    isLoading: Boolean,
    isFailure: Boolean,
    isPlaceholder: Boolean,
    hasContent: Boolean,
    buttonText: String = "Transcrever com IA",
    onRetry: () -> Unit,
    onClear: () -> Unit = {},
    onNativeTranscribe: (() -> Unit)? = null,
    voskModelReady: Boolean = false,
    voskDownloadProgress: Int = -1,
    hasApiKey: Boolean = true,
    onGoToSettings: (() -> Unit)? = null,
    transcriptionMode: String = "offline"
) {
    val context = LocalContext.current
    val isNativeResult = content.startsWith("⚠️")

    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.05f)) {
        Column(modifier = Modifier.padding(20.dp)) {

            // ── Cabeçalho ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = AmberMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (hasContent && !isNativeResult) {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("MeetPen", content))
                    }) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copiar",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when {
                // ── Carregando ─────────────────────────────────────────
                isLoading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = AmberMain
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Processando...", color = Color.Gray, fontSize = 14.sp)
                    }
                }

                // ── Aguardando primeira ação ───────────────────────────
                isPlaceholder || (isFailure && !isNativeResult) -> {
                    if (onNativeTranscribe != null) {
                        DynamicTranscriptionPanel(
                            transcriptionMode = transcriptionMode,
                            hasApiKey = hasApiKey,
                            voskModelReady = voskModelReady,
                            voskDownloadProgress = voskDownloadProgress,
                            onNative = onNativeTranscribe,
                            onAi = onRetry,
                            onGoToSettings = onGoToSettings
                        )
                    } else {
                        // Botão simples para resumo/todo
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberMain.copy(alpha = 0.1f)
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = AmberMain
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(buttonText, color = AmberMain, fontSize = 14.sp)
                        }
                    }
                }

                // ── Resultado de transcrição nativa (com aviso de qualidade) ──
                isNativeResult -> {
                    // Banner de aviso de qualidade
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFA726).copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, Color(0xFFFFA726).copy(alpha = 0.25f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Transcrição local",
                                color = Color(0xFFFFA726),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "O motor nativo do Android tem qualidade limitada. Para resultados precisos, use um provedor de IA com chave de API.",
                                color = Color(0xFFFFA726).copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                            if (!hasApiKey && onGoToSettings != null) {
                                Spacer(Modifier.height(10.dp))
                                TextButton(
                                    onClick = onGoToSettings,
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = AmberMain,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Configurar chave de API →",
                                        color = AmberMain,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Texto transcrito
                    val resultText = content
                        .removePrefix("⚠️ Transcrição local — qualidade limitada pelo motor do Android.\n\nResultado:\n\n")
                        .removePrefix("⚠️ Transcrição local — qualidade limitada pelo motor do Android.\n\nResultado:")
                        .trim()
                    Text(
                        text = resultText,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )

                    // Opção de retranscrever com IA
                    if (hasApiKey) {
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onRetry,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, AmberMain.copy(alpha = 0.4f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberMain),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Melhorar com IA", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // ── Conteúdo normal ────────────────────────────────────
                else -> {
                    Text(text = content, color = Color.White, lineHeight = 22.sp, fontSize = 15.sp)
                    
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (hasApiKey) {
                            OutlinedButton(
                                onClick = onRetry,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AmberMain.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberMain)
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Reprocessar IA", fontSize = 13.sp)
                            }
                        }
                        
                        OutlinedButton(
                            onClick = onClear,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Limpar", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicTranscriptionPanel(
    transcriptionMode: String,
    hasApiKey: Boolean,
    voskModelReady: Boolean,
    voskDownloadProgress: Int,
    onNative: () -> Unit,
    onAi: () -> Unit,
    onGoToSettings: (() -> Unit)?
) {
    val isDownloading = voskDownloadProgress in 0..100

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (transcriptionMode == "offline") {
            // ── PRIORIDADE: OFFLINE ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, Color.White.copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Título da opção
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.OfflinePin,
                            contentDescription = null,
                            tint = AmberMain,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Transcrição local offline",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "Preferência Ativa",
                            color = AmberMain,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    when {
                        // Modelo pronto
                        voskModelReady -> {
                            Text(
                                "O motor local está pronto. O áudio será processado inteiramente no seu aparelho, sem internet.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = onNative,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberMain,
                                    contentColor = MidnightBg
                                )
                            ) {
                                Icon(
                                    Icons.Default.OfflinePin,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Transcrever offline", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Baixando
                        isDownloading -> {
                            Text(
                                "Baixando motor de reconhecimento de voz local…",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = {
                                    if (voskDownloadProgress > 0) voskDownloadProgress / 100f else 0f
                                },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                color = AmberMain,
                                trackColor = Color.White.copy(alpha = 0.08f)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (voskDownloadProgress > 0) "$voskDownloadProgress% concluído"
                                else "Conectando…",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }

                        // Não instalado
                        else -> {
                            Text(
                                "Motor offline selecionado como padrão, mas não está instalado.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { onGoToSettings?.invoke() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberMain.copy(alpha = 0.12f),
                                    contentColor = AmberMain
                                )
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Instalar motor offline", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Opção extra: Usar IA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Ou use a IA para maior precisão",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                if (hasApiKey) {
                    TextButton(
                        onClick = onAi,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            "Usar IA →",
                            color = AmberMain,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    TextButton(
                        onClick = { onGoToSettings?.invoke() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            "Configurar IA →",
                            color = AmberMain.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            // ── PRIORIDADE: IA ONLINE ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, Color.White.copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Título da opção
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AmberMain,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Transcrição com Inteligência Artificial",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "Preferência Ativa",
                            color = AmberMain,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    if (hasApiKey) {
                        Text(
                            "Usará seu modelo de IA online selecionado para gerar uma transcrição altamente precisa e detalhada.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = onAi,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberMain,
                                contentColor = MidnightBg
                            )
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Transcrever com IA", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Chave de API não configurada
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFA726).copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, Color(0xFFFFA726).copy(alpha = 0.25f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Chave de API requerida",
                                    color = Color(0xFFFFA726),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Você escolheu transcrição online como padrão, mas nenhuma chave de API foi inserida nas configurações.",
                                    color = Color(0xFFFFA726).copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { onGoToSettings?.invoke() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberMain.copy(alpha = 0.12f),
                                contentColor = AmberMain
                            )
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Configurar chave de API", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Opção extra: Usar Offline
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.OfflinePin,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Ou use o motor local 100% privado",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                if (voskModelReady) {
                    TextButton(
                        onClick = onNative,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            "Usar offline →",
                            color = AmberMain,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    TextButton(
                        onClick = { onGoToSettings?.invoke() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            "Instalar local →",
                            color = AmberMain.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}



