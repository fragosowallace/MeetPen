package br.com.meetpen.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.meetpen.data.AppDatabase
import br.com.meetpen.data.Recording
import br.com.meetpen.logic.AndroidAudioPlayer
import br.com.meetpen.logic.GeminiApi
import br.com.meetpen.logic.OpenAIApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File

import br.com.meetpen.logic.SecurePrefs
import br.com.meetpen.logic.SettingsManager
import br.com.meetpen.logic.VoiceEffect
import br.com.meetpen.logic.VoskTranscriber
import br.com.meetpen.logic.OfflineModel
import br.com.meetpen.logic.WhisperTFLiteTranscriber

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.recordingDao()
    private val settingsManager = SettingsManager(application)
    private val prefs = application.getSharedPreferences("meetpen_prefs", Context.MODE_PRIVATE)
    private val securePrefs = SecurePrefs.get(application)

    val recordings: StateFlow<List<Recording>> = dao.getAllRecordings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentlyPlayingPath = mutableStateOf<String?>(null)
    val currentSpeed = mutableStateOf(1.0f)
    val currentVoiceEffect = mutableStateOf(VoiceEffect.NONE)
    
    val isSubscribed = mutableStateOf(settingsManager.isSubscribed())
    val userEmail = mutableStateOf(settingsManager.getUserEmail())
    val recordingCount = mutableStateOf(0)

    private val gemini = GeminiApi()
    private val openai = OpenAIApi()
    private val player = AndroidAudioPlayer(application)
    val voskTranscriber = VoskTranscriber(application)
    private val whisperTranscriber = WhisperTFLiteTranscriber(application)

    // Estado do modelo offline selecionado
    val currentOfflineModel = mutableStateOf(OfflineModel.fromId(settingsManager.getOfflineModelId()))
    val voskModelReady = mutableStateOf(false)
    val voskDownloadProgress = mutableStateOf(-1) // -1 = idle, 0-100 = downloading
    val voskError = mutableStateOf<String?>(null)

    // Estado do modo padrão de transcrição (lido do SharedPreferences, padrão: offline)
    val transcriptionMode = mutableStateOf(prefs.getString("transcription_mode", "offline") ?: "offline")

    init {
        updateRecordingCount()
        // Se o modelo já existe no disco (download anterior válido), carrega em memória agora
        val savedModel = OfflineModel.fromId(settingsManager.getOfflineModelId())
        if (savedModel.type == "Vosk" && voskTranscriber.isModelReady(savedModel)) {
            voskTranscriber.loadModel(
                offlineModel = savedModel,
                onProgress = {},
                onReady = { voskModelReady.value = true },
                onError = { voskModelReady.value = false }
            )
        } else if (savedModel.type == "Whisper" && whisperTranscriber.isModelReady(savedModel)) {
            viewModelScope.launch {
                try {
                    whisperTranscriber.loadModel(savedModel)
                    voskModelReady.value = true
                } catch (e: Exception) {
                    voskModelReady.value = false
                }
            }
        }
    }

    /**
     * Baixa (se necessário) e carrega o modelo offline selecionado.
     * [onLoaded] é chamado quando o modelo estiver pronto em memória;
     * [onFailed] quando o download/carregamento falhar.
     */
    fun prepareOfflineModel(
        model: OfflineModel,
        onLoaded: (() -> Unit)? = null,
        onFailed: ((String) -> Unit)? = null
    ) {
        if (voskDownloadProgress.value in 0..100) {
            // Já há um download em andamento
            onFailed?.invoke("O modelo offline ainda está sendo baixado. Tente novamente em instantes.")
            return
        }

        settingsManager.saveOfflineModel(model.id)
        currentOfflineModel.value = model

        voskDownloadProgress.value = 0
        voskError.value = null
        voskModelReady.value = false

        if (model.type == "Vosk") {
            voskTranscriber.loadModel(
                offlineModel = model,
                onProgress = { p -> voskDownloadProgress.value = p },
                onReady = {
                    voskModelReady.value = true
                    voskDownloadProgress.value = -1
                    onLoaded?.invoke()
                },
                onError = { err ->
                    voskError.value = err
                    voskDownloadProgress.value = -1
                    voskModelReady.value = false
                    onFailed?.invoke(err)
                }
            )
        } else {
            // Whisper Download
            voskTranscriber.loadModel( // Usamos o mesmo downloader do Vosk que é genérico
                offlineModel = model,
                onProgress = { p -> voskDownloadProgress.value = p },
                onReady = {
                    viewModelScope.launch {
                        try {
                            whisperTranscriber.loadModel(model)
                            voskModelReady.value = true
                            voskDownloadProgress.value = -1
                            onLoaded?.invoke()
                        } catch (e: Exception) {
                            voskError.value = "Erro ao carregar Whisper: ${e.message}"
                            voskModelReady.value = false
                            voskDownloadProgress.value = -1
                            onFailed?.invoke("Erro ao carregar Whisper: ${e.message}")
                        }
                    }
                },
                onError = { err ->
                    voskError.value = err
                    voskDownloadProgress.value = -1
                    voskModelReady.value = false
                    onFailed?.invoke(err)
                }
            )
        }
    }

    /** Transcreve offline. Se o modelo estiver no disco mas não em memória, carrega primeiro. */
    fun transcribeOffline(recording: Recording) {
        viewModelScope.launch {
            dao.insert(recording.copy(transcription = "Processando..."))

            // Garante que o modelo está em memória antes de transcrever.
            // Quando o carregamento terminar, a transcrição é re-disparada
            // automaticamente (a nota não fica presa em "Processando...").
            if (!voskModelReady.value) {
                prepareOfflineModel(
                    model = currentOfflineModel.value,
                    onLoaded = { transcribeOffline(recording) },
                    onFailed = { err ->
                        viewModelScope.launch {
                            dao.insert(recording.copy(transcription = "Erro: $err"))
                        }
                    }
                )
                return@launch
            }

            val model = currentOfflineModel.value
            if (model.type == "Vosk") {
                voskTranscriber.transcribeFile(
                audioFile = File(recording.filePath),
                offlineModel = currentOfflineModel.value,
                onResult = { result ->
                    viewModelScope.launch { dao.insert(recording.copy(transcription = result)) }
                },
                onError = { err ->
                    viewModelScope.launch {
                        dao.insert(recording.copy(transcription = "Erro: $err"))
                    }
                }
            )
            } else {
                try {
                    val result = whisperTranscriber.transcribe(File(recording.filePath))
                    dao.insert(recording.copy(transcription = result))
                } catch (e: Exception) {
                    dao.insert(recording.copy(transcription = "Erro Whisper: ${e.message}"))
                }
            }
        }
    }

    fun updateRecordingCount() {
        viewModelScope.launch {
            recordingCount.value = dao.getCount()
        }
    }

    fun canRecord(): Boolean {
        return recordingCount.value < settingsManager.getUsageLimit()
    }

    fun logout() {
        settingsManager.setUserEmail(null)
        userEmail.value = null
        settingsManager.setSubscribed(false)
        isSubscribed.value = false
    }

    fun subscribe() {
        settingsManager.setSubscribed(true)
        isSubscribed.value = true
    }

    private fun getActiveKeyAndProvider(): Pair<String, String> {
        // "Claude" foi removido da UI; valores antigos salvos caem no Gemini
        val saved = prefs.getString("provider", "Gemini") ?: "Gemini"
        val provider = if (saved == "OpenAI") "OpenAI" else "Gemini"
        val key = when (provider) {
            "OpenAI" -> securePrefs.getString("openai_key", "") ?: ""
            else -> securePrefs.getString("api_key", "") ?: ""
        }
        return key to provider
    }

    private fun getGeminiKey(): String = securePrefs.getString("api_key", "") ?: ""

    /** Salva a gravação. Transcrição é sempre iniciada pelo usuário — nunca automática. */
    fun saveAndTranscribe(title: String, path: String) {
        viewModelScope.launch {
            val newRecording = Recording(
                title = title,
                filePath = path,
                timestamp = System.currentTimeMillis(),
                transcription = "Áudio pronto para transcrição"
            )
            dao.insert(newRecording)
            updateRecordingCount()
        }
    }

    fun hasApiKey(): Boolean {
        val (key, _) = getActiveKeyAndProvider()
        return key.isNotEmpty()
    }

    fun setTranscriptionMode(mode: String) {
        prefs.edit().putString("transcription_mode", mode).apply()
        transcriptionMode.value = mode
    }

    fun transcribeBasedOnSettings(recording: Recording) {
        val mode = transcriptionMode.value
        if (mode == "api") {
            transcribe(recording)
        } else {
            transcribeOffline(recording)
        }
    }

    fun transcribe(recording: Recording) {
        val (apiKey, provider) = getActiveKeyAndProvider()
        if (apiKey.isEmpty()) return

        viewModelScope.launch {
            dao.insert(recording.copy(transcription = "Processando..."))
            if (provider == "OpenAI" || apiKey.startsWith("sk-")) {
                openai.transcribe(File(recording.filePath), apiKey) { result ->
                    viewModelScope.launch { dao.insert(recording.copy(transcription = result)) }
                }
            } else {
                gemini.transcribe(File(recording.filePath), apiKey) { result ->
                    viewModelScope.launch { dao.insert(recording.copy(transcription = result)) }
                }
            }
        }
    }

    fun generateSummary(recording: Recording) {
        if (recording.transcription.isEmpty()) return
        if (noteIsProcessing(recording.summary)) return

        // Resumo é gerado pelo Gemini; exige a chave do Gemini configurada
        val apiKey = getGeminiKey()
        if (apiKey.isEmpty()) {
            viewModelScope.launch {
                dao.insert(recording.copy(summary = "Erro: configure a chave do Google Gemini nas Configurações para gerar resumos."))
            }
            return
        }

        viewModelScope.launch {
            dao.insert(recording.copy(summary = "Processando..."))
            gemini.summarize(recording.transcription, apiKey) { result ->
                viewModelScope.launch { dao.insert(recording.copy(summary = result)) }
            }
        }
    }

    fun generateTodo(recording: Recording) {
        if (recording.transcription.isEmpty()) return
        if (noteIsProcessing(recording.todoList)) return

        // Checklist é gerado pelo Gemini; exige a chave do Gemini configurada
        val apiKey = getGeminiKey()
        if (apiKey.isEmpty()) {
            viewModelScope.launch {
                dao.insert(recording.copy(todoList = "Erro: configure a chave do Google Gemini nas Configurações para extrair tarefas."))
            }
            return
        }

        viewModelScope.launch {
            dao.insert(recording.copy(todoList = "Processando..."))
            gemini.generateTodo(recording.transcription, apiKey) { result ->
                viewModelScope.launch { dao.insert(recording.copy(todoList = result)) }
            }
        }
    }

    private fun noteIsProcessing(field: String) = field == "Processando..."

    fun toggleTodo(recording: Recording, taskIndex: Int) {
        try {
            val jsonArray = JSONArray(recording.todoList)
            val item = jsonArray.getJSONObject(taskIndex)
            item.put("done", !item.getBoolean("done"))
            viewModelScope.launch { dao.insert(recording.copy(todoList = jsonArray.toString())) }
        } catch (e: Exception) {
            android.util.Log.e("MeetPen", "Erro ao alternar tarefa do checklist", e)
        }
    }

    fun updateTitle(recording: Recording, newTitle: String) {
        viewModelScope.launch { dao.insert(recording.copy(title = newTitle)) }
    }

    fun updateCategory(recording: Recording, newCategory: String) {
        viewModelScope.launch { dao.insert(recording.copy(category = newCategory)) }
    }

    fun clearTranscription(recording: Recording) {
        viewModelScope.launch { 
            dao.insert(recording.copy(
                transcription = "Áudio pronto para transcrição",
                summary = "",
                todoList = ""
            )) 
        }
    }

    fun deleteRecording(recording: Recording) {
        viewModelScope.launch {
            dao.delete(recording)
            File(recording.filePath).delete()
            updateRecordingCount()
        }
    }

    fun playAudio(path: String) {
        if (currentlyPlayingPath.value == path) {
            player.stop()
            currentlyPlayingPath.value = null
        } else {
            val file = File(path)
            if (file.exists()) {
                val started = player.playFile(file, currentSpeed.value, currentVoiceEffect.value) {
                    currentlyPlayingPath.value = null
                }
                // Só marca como "tocando" se o MediaPlayer foi criado com sucesso,
                // senão a UI ficaria presa no estado de reprodução
                currentlyPlayingPath.value = if (started) path else null
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        currentSpeed.value = speed
        player.setParams(speed, currentVoiceEffect.value)
    }

    fun setVoiceEffect(effect: VoiceEffect) {
        currentVoiceEffect.value = effect
        player.setParams(currentSpeed.value, effect)
    }

    override fun onCleared() {
        super.onCleared()
        player.stop()
    }
}
