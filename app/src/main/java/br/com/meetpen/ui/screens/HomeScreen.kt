package br.com.meetpen.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import br.com.meetpen.ui.theme.AmberMain
import br.com.meetpen.ui.theme.MidnightBg
import br.com.meetpen.data.Recording
import br.com.meetpen.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.FileProvider
import java.io.File

import br.com.meetpen.ui.components.AccountDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onStartRecording: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onNavigateToDetail: (Int) -> Unit
) {
    val context = LocalContext.current
    val recordings by viewModel.recordings.collectAsState()
    val isSubscribed by remember { viewModel.isSubscribed }
    val userEmail by remember { viewModel.userEmail }
    val recordingCount by remember { viewModel.recordingCount }

    var showAccountDialog by remember { mutableStateOf(false) }
    var showLimitAlert by remember { mutableStateOf(false) }
    val categories = listOf("Todas", "Trabalho", "Diário", "Ideias", "Reuniões")
    var selectedCategory by remember { mutableStateOf("Todas") }
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("Data") } 
    
    var recordingToRename by remember { mutableStateOf<Recording?>(null) }
    var recordingToDelete by remember { mutableStateOf<Recording?>(null) }
    var recordingToShare by remember { mutableStateOf<Recording?>(null) }
    var newTitle by remember { mutableStateOf("") }

    if (recordingToShare != null) {
        val note = recordingToShare!!
        AlertDialog(
            onDismissRequest = { recordingToShare = null },
            title = { Text("Compartilhar") },
            text = { Text("O que você deseja compartilhar de \"${note.title}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    recordingToShare = null
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
                    recordingToShare = null
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.transcription}")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Compartilhar Texto"))
                }) { Text("Texto") }
            }
        )
    }

    if (recordingToRename != null) {
        AlertDialog(
            onDismissRequest = { recordingToRename = null },
            title = { Text("Renomear Nota") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Novo título") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    recordingToRename?.let { viewModel.updateTitle(it, newTitle) }
                    recordingToRename = null
                }) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { recordingToRename = null }) { Text("Cancelar") }
            }
        )
    }

    if (recordingToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordingToDelete = null },
            title = { Text("Excluir Nota?") },
            text = { Text("Tem certeza que deseja apagar esta gravação?") },
            confirmButton = {
                Button(onClick = {
                    recordingToDelete?.let { viewModel.deleteRecording(it) }
                    recordingToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Excluir", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { recordingToDelete = null }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meet Pen", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { showAccountDialog = true }) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = "Perfil", modifier = Modifier.size(20.dp), tint = if (isSubscribed) AmberMain else Color.White)
                        }
                    }
                },
                actions = {
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(imageVector = Icons.Default.Sort, contentDescription = "Ordenar", tint = Color.Gray)
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }, modifier = Modifier.background(MidnightBg)) {
                            DropdownMenuItem(
                                text = { Text("Data (Mais recentes)", color = if(sortBy == "Data") AmberMain else Color.White) },
                                onClick = { sortBy = "Data"; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Título (A-Z)", color = if(sortBy == "Título") AmberMain else Color.White) },
                                onClick = { sortBy = "Título"; showSortMenu = false }
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Configurações", tint = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (viewModel.canRecord()) {
                        onStartRecording()
                    } else {
                        showLimitAlert = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(72.dp).padding(4.dp)
            ) { Icon(imageVector = Icons.Default.Mic, contentDescription = "Gravar", modifier = Modifier.size(32.dp), tint = Color.White) }
        },
        floatingActionButtonPosition = FabPosition.Center,
        containerColor = MidnightBg
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyRow(modifier = Modifier.padding(vertical = 16.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = Color.White, containerColor = Color.White.copy(alpha = 0.05f), labelColor = Color.Gray),
                        border = null
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery, 
                onValueChange = { searchQuery = it }, 
                placeholder = { Text("buscar notas...", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.05f)),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent, focusedBorderColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            val filteredRecordings = recordings.filter { recording ->
                val matchesSearch = recording.title.contains(searchQuery, ignoreCase = true) || 
                                   recording.transcription.contains(searchQuery, ignoreCase = true)
                
                val matchesCategory = if (selectedCategory == "Todas") {
                    true
                } else {
                    recording.category == selectedCategory || (recording.category.isEmpty() && selectedCategory == "Geral")
                }
                
                matchesSearch && matchesCategory
            }.sortedWith { a, b ->
                if (sortBy == "Data") b.timestamp.compareTo(a.timestamp)
                else a.title.compareTo(b.title, ignoreCase = true)
            }

            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (filteredRecordings.isEmpty()) {
                    item { Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { Text("Nenhuma nota encontrada.", color = Color.Gray) } }
                }
                items(filteredRecordings) { recording ->
                    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(recording.timestamp))
                    val isTranscribing = recording.transcription == "Processando..."
                    val isPlaying = viewModel.currentlyPlayingPath.value == recording.filePath

                    val transcriptionMode by viewModel.transcriptionMode
                    NoteCard(
                        title = recording.title,
                        date = dateStr,
                        snippet = if (recording.transcription.isEmpty()) "Áudio pronto para transcrição" else recording.transcription,
                        isTranscribing = isTranscribing,
                        isPlaying = isPlaying,
                        onClick = { onNavigateToDetail(recording.id) },
                        onPlay = { viewModel.playAudio(recording.filePath) },
                        onEdit = {
                            newTitle = recording.title
                            recordingToRename = recording
                        },
                        onDelete = { recordingToDelete = recording },
                        onShare = { recordingToShare = recording },
                        onTranscribe = { viewModel.transcribeBasedOnSettings(recording) },
                        transcriptionMode = transcriptionMode
                    )
                }
            }
        }
    }

    if (showAccountDialog) {
        AccountDialog(
            userEmail = userEmail,
            isSubscribed = isSubscribed,
            recordingCount = recordingCount,
            onDismiss = { showAccountDialog = false },
            onLogin = { viewModel.loginTest() },
            onLogout = { viewModel.logout() },
            onUpgrade = { 
                showAccountDialog = false
                onNavigateToPaywall() 
            }
        )
    }

    if (showLimitAlert) {
        AlertDialog(
            onDismissRequest = { showLimitAlert = false },
            title = { Text("Limite Atingido", color = Color.White) },
            text = { Text("Você atingiu o limite de gravações gratuitas. Assine o Prime para continuar gravando sem limites!", color = Color.Gray) },
            confirmButton = {
                Button(
                    onClick = { 
                        showLimitAlert = false
                        onNavigateToPaywall() 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberMain)
                ) { Text("Ver Planos") }
            },
            dismissButton = {
                TextButton(onClick = { showLimitAlert = false }) { Text("Agora não", color = Color.Gray) }
            },
            containerColor = br.com.meetpen.ui.theme.MidnightSurface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun NoteCard(
    title: String, 
    date: String, 
    snippet: String, 
    isTranscribing: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit, 
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onTranscribe: () -> Unit,
    transcriptionMode: String = "offline"
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(text = title, style = MaterialTheme.typography.headlineMedium, fontSize = 20.sp, color = Color.White, modifier = Modifier.weight(1f))
                Row {
                    IconButton(onClick = onPlay, modifier = Modifier.size(40.dp)) {
                        Icon(imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, tint = AmberMain, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(40.dp)) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Compartilhar", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = snippet, style = MaterialTheme.typography.bodyLarge, color = if(isTranscribing) AmberMain else Color.Gray, maxLines = 3)
            
            if (snippet == "Áudio pronto para transcrição" || isTranscribing) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onTranscribe() },
                    enabled = !isTranscribing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberMain.copy(alpha = 0.1f), contentColor = AmberMain),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isTranscribing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AmberMain)
                        Spacer(Modifier.width(8.dp))
                        Text("Processando...")
                    } else {
                        val icon = if (transcriptionMode == "api") Icons.Default.AutoAwesome else Icons.Default.OfflinePin
                        val text = if (transcriptionMode == "api") "Gerar Transcrição com IA" else "Gerar Transcrição Offline"
                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if(isTranscribing) AmberMain else Color.DarkGray))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = date, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }
        }
    }
}
