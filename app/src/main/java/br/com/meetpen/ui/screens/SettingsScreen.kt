package br.com.meetpen.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import java.io.File
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import br.com.meetpen.logic.BiometricAuthenticator
import br.com.meetpen.ui.theme.AmberMain
import br.com.meetpen.ui.theme.MidnightBg
import br.com.meetpen.ui.theme.MidnightSurface
import br.com.meetpen.ui.MainViewModel
import br.com.meetpen.ui.theme.TextSecondary
import br.com.meetpen.logic.OfflineModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val prefs = remember { context.getSharedPreferences("meetpen_prefs", Context.MODE_PRIVATE) }
    val securePrefs = remember { br.com.meetpen.logic.SecurePrefs.get(context) }
    val biometricAuthenticator = remember { BiometricAuthenticator(context) }

    var isAuthorized by remember { mutableStateOf(!prefs.getBoolean("biometric_enabled", false)) }
    var authError by remember { mutableStateOf<String?>(null) }

    // Estados do modelo Vosk vindos do ViewModel
    val voskModelReady by viewModel.voskModelReady
    val voskDownloadProgress by viewModel.voskDownloadProgress
    val voskError by viewModel.voskError

    LaunchedEffect(Unit) {
        if (!isAuthorized && activity != null) {
            biometricAuthenticator.authenticate(
                activity = activity,
                onSuccess = { isAuthorized = true },
                onError = { error ->
                    authError = error
                    if (error.contains("cancel", ignoreCase = true)) onBack()
                }
            )
        }
    }

    if (!isAuthorized) {
        Box(
            modifier = Modifier.fillMaxSize().background(MidnightBg),
            contentAlignment = Alignment.Center
        ) {
            if (authError != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = AmberMain,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Acesso Negado", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        activity?.let {
                            biometricAuthenticator.authenticate(it, { isAuthorized = true }, { authError = it })
                        }
                    }) { Text("Tentar Novamente", color = AmberMain) }
                    TextButton(onClick = onBack) {
                        Text("Voltar", color = Color.Gray)
                    }
                }
            }
        }
        return
    }

    // ── Estado dos campos ──────────────────────────────────────────────────
    // "Claude" foi removido enquanto não há suporte; valores antigos caem no Gemini
    var selectedProvider by remember {
        val saved = prefs.getString("provider", "Gemini") ?: "Gemini"
        mutableStateOf(if (saved == "OpenAI") "OpenAI" else "Gemini")
    }
    var geminiKey by remember { mutableStateOf(securePrefs.getString("api_key", "") ?: "") }
    var openaiKey by remember { mutableStateOf(securePrefs.getString("openai_key", "") ?: "") }
    var biometricEnabled by remember { mutableStateOf(prefs.getBoolean("biometric_enabled", false)) }
    val currentMode by viewModel.transcriptionMode

    val activeKey = when (selectedProvider) {
        "OpenAI" -> openaiKey
        else -> geminiKey
    }
    val hasActiveKey = activeKey.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Configurações",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color.White
                        )
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // ── Seção Especial: Método de Transcrição Padrão ───────────────
            SettingsSectionHeader(
                icon = Icons.Default.Mic,
                title = "Motor de Transcrição Padrão",
                subtitle = "Escolha como o app deve processar seus áudios por padrão"
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Modo Offline
                val isOfflineSelected = currentMode == "offline"
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.setTranscriptionMode("offline") }
                        .height(80.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isOfflineSelected) AmberMain.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isOfflineSelected) AmberMain.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.07f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.OfflinePin,
                            contentDescription = null,
                            tint = if (isOfflineSelected) AmberMain else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Offline (Local)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Modo Online (IA)
                val isApiSelected = currentMode == "api"
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.setTranscriptionMode("api") }
                        .height(80.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isApiSelected) AmberMain.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isApiSelected) AmberMain.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.07f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isApiSelected) AmberMain else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Online (IA Nuvem)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(bottom = 24.dp))

            // ── Seção 0: Transcrição Offline ──────────────────────────────
            SettingsSectionHeader(
                icon = Icons.Default.OfflinePin,
                title = "Transcrição Offline",
                subtitle = "Escolha o motor de reconhecimento local"
            )

            Spacer(Modifier.height(16.dp))

            // Lista de modelos disponíveis
            OfflineModel.ALL.forEach { model ->
                val isSelected = viewModel.currentOfflineModel.value.id == model.id
                val isInstalled = if (model.type == "Vosk") {
                    viewModel.voskTranscriber.isModelReady(model)
                } else {
                    // Para o Whisper TFLite, precisamos de um cheque similar
                    // Como não queremos duplicar lógica complexa, usamos o MainViewModel/Transcriber
                    val modelDir = File(context.filesDir, model.modelFileName)
                    val modelFile = File(modelDir, model.modelFileName)
                    modelFile.exists()
                }
                
                val isDownloading = isSelected && viewModel.voskDownloadProgress.value in 0..100

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable { if (!isDownloading) viewModel.prepareOfflineModel(model) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) AmberMain.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, 
                        if (isSelected) AmberMain.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.07f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when {
                                    isInstalled -> Icons.Default.CheckCircle
                                    model.type == "Whisper" -> Icons.Default.AutoAwesome
                                    else -> Icons.Default.CloudDownload
                                },
                                contentDescription = null,
                                tint = if (isInstalled) Color(0xFF66BB6A) else if (isSelected) AmberMain else Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = model.name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${model.type} • ${model.size}",
                                    color = if (isSelected) AmberMain.copy(alpha = 0.7f) else TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            
                            if (isSelected && !isInstalled && !isDownloading) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null,
                                    tint = AmberMain,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = model.description,
                            color = TextSecondary.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        if (isDownloading) {
                            Spacer(Modifier.height(16.dp))
                            Column {
                                LinearProgressIndicator(
                                    progress = { viewModel.voskDownloadProgress.value / 100f },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                    color = AmberMain,
                                    trackColor = Color.White.copy(alpha = 0.1f)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Baixando: ${viewModel.voskDownloadProgress.value}%",
                                    color = AmberMain,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (voskError != null) {
                Spacer(Modifier.height(8.dp))
                Text(voskError!!, color = Color.Red, fontSize = 12.sp)
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            Spacer(Modifier.height(32.dp))

            // ── Seção 1: Transcrição com IA ───────────────────────────────
            SettingsSectionHeader(
                icon = Icons.Default.AutoAwesome,
                title = "Transcrição com IA",
                subtitle = "Configure um provedor para obter transcrições precisas"
            )

            Spacer(Modifier.height(16.dp))

            // Status atual
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (hasActiveKey) AmberMain.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.04f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (hasActiveKey) AmberMain.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.07f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (hasActiveKey) AmberMain else Color.Gray)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (hasActiveKey)
                            "Usando $selectedProvider — IA ativa"
                        else
                            "Nenhuma chave configurada — usando motor local",
                        color = if (hasActiveKey) AmberMain else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Seletor de provedor
            Text(
                "Escolha o provedor",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Gemini", "OpenAI").forEach { provider ->
                    val isSelected = selectedProvider == provider
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedProvider = provider
                                prefs.edit().putString("provider", provider).apply()
                            }
                            .height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) AmberMain else Color.White.copy(alpha = 0.04f),
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
                            1.dp, Color.White.copy(alpha = 0.08f)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = provider,
                                color = if (isSelected) MidnightBg else Color.White.copy(alpha = 0.7f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Campo de chave — só mostra o do provedor selecionado
            when (selectedProvider) {
                "Gemini" -> ApiKeyField(
                    label = "Chave do Google Gemini",
                    hint = "AIzaSy...",
                    value = geminiKey,
                    onValueChange = {
                        geminiKey = it
                        securePrefs.edit().putString("api_key", it).apply()
                    }
                )
                "OpenAI" -> ApiKeyField(
                    label = "Chave da OpenAI (Whisper / GPT)",
                    hint = "sk-...",
                    value = openaiKey,
                    onValueChange = {
                        openaiKey = it
                        securePrefs.edit().putString("openai_key", it).apply()
                    }
                )
            }

            // Dica contextual de onde obter a chave
            Spacer(Modifier.height(10.dp))
            val docsUrl = when (selectedProvider) {
                "OpenAI" -> "platform.openai.com/api-keys"
                else -> "aistudio.google.com"
            }
            Text(
                text = "Obtenha sua chave em: $docsUrl",
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp)
            )

            Spacer(Modifier.height(32.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            Spacer(Modifier.height(32.dp))

            // ── Seção 2: Segurança ────────────────────────────────────────
            SettingsSectionHeader(
                icon = Icons.Default.Security,
                title = "Segurança",
                subtitle = "Proteja o acesso às suas chaves de API"
            )

            Spacer(Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.04f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = AmberMain,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                "Bloquear com biometria",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Exige digital ou face para abrir Configurações",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = {
                            biometricEnabled = it
                            prefs.edit().putBoolean("biometric_enabled", it).apply()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AmberMain,
                            checkedTrackColor = AmberMain.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Aviso de privacidade
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.02f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp).padding(top = 2.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Suas chaves são armazenadas somente neste aparelho e enviadas apenas ao provedor de IA escolhido.",
                        color = TextSecondary.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
            Text(
                "Meet Pen v${br.com.meetpen.BuildConfig.VERSION_NAME}",
                color = Color.White.copy(alpha = 0.15f),
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AmberMain,
            modifier = Modifier.size(22.dp).padding(top = 2.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ApiKeyField(
    label: String,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    var showKey by remember { mutableStateOf(false) }
    val hasValue = value.isNotBlank()

    Column {
        Text(
            label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(hint, color = Color.White.copy(alpha = 0.2f), fontSize = 14.sp)
            },
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    if (hasValue) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AmberMain,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.White.copy(alpha = 0.03f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                focusedBorderColor = AmberMain,
                unfocusedBorderColor = Color.White.copy(alpha = 0.08f)
            ),
            singleLine = true
        )
    }
}

