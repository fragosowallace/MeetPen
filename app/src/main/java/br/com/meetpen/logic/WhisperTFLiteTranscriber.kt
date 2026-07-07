package br.com.meetpen.logic

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class WhisperTFLiteTranscriber(private val context: Context) {

    private var interpreter: Interpreter? = null
    private val whisperUtil = WhisperUtil()
    private var isInitialized = false

    fun isModelReady(model: OfflineModel): Boolean {
        val modelDir = File(context.filesDir, model.modelFileName)
        val modelFile = File(modelDir, model.modelFileName)
        val vocabFile = File(modelDir, "filters_vocab_gen.bin")
        return modelFile.exists() && vocabFile.exists()
    }

    suspend fun loadModel(model: OfflineModel) = withContext(Dispatchers.IO) {
        val modelDir = File(context.filesDir, model.modelFileName)
        val modelFile = File(modelDir, model.modelFileName)
        val vocabFile = File(modelDir, "filters_vocab_gen.bin")
        
        if (!modelFile.exists() || !vocabFile.exists()) throw Exception("Arquivos do modelo incompleto.")

        try {
            interpreter?.close()
            val options = Interpreter.Options().apply {
                setNumThreads(Runtime.getRuntime().availableProcessors())
            }
            val modelBuffer = FileInputStream(modelFile).channel.map(FileChannel.MapMode.READ_ONLY, 0, modelFile.length())
            interpreter = Interpreter(modelBuffer, options)
            
            val loaded = whisperUtil.loadFiltersAndVocab(true, vocabFile.absolutePath)
            if (!loaded) throw Exception("Erro ao carregar filtros/vocabulário.")
            
            isInitialized = true
            Log.i("WhisperTFLite", "Modelo ${model.name} carregado com sucesso.")
        } catch (e: Exception) {
            Log.e("WhisperTFLite", "Erro ao carregar: ${e.message}")
            throw e
        }
    }

    suspend fun transcribe(audioFile: File): String = withContext(Dispatchers.Default) {
        val interp = interpreter ?: throw Exception("Motor não inicializado")
        if (!isInitialized) throw Exception("Vocabulário não carregado")

        // Nome único por chamada para evitar corrida entre transcrições concorrentes
        val pcmFile = File.createTempFile("whisper_", ".wav", context.cacheDir)
        try {
            val voskTemp = VoskTranscriber(context)
            voskTemp.decodeToWav(audioFile, pcmFile)

            val pcmBytes = pcmFile.readBytes()
            val pcmData = FloatArray((pcmBytes.size - 44) / 2)
            val buffer = ByteBuffer.wrap(pcmBytes, 44, pcmBytes.size - 44).order(ByteOrder.LITTLE_ENDIAN)
            for (i in pcmData.indices) {
                pcmData[i] = buffer.short / 32768.0f
            }

            // Padronizar tamanho de entrada (30s)
            val fixedInputSize = WhisperUtil.WHISPER_SAMPLE_RATE * WhisperUtil.WHISPER_CHUNK_SIZE
            val inputSamples = FloatArray(fixedInputSize)
            pcmData.copyInto(inputSamples, 0, 0, minOf(pcmData.size, fixedInputSize))

            // 1. Calcular Mel Spectrogram
            val melData = whisperUtil.getMelSpectrogram(inputSamples, Runtime.getRuntime().availableProcessors())
            
            // 2. Preparar Tensores
            val inputTensor = interp.getInputTensor(0)
            val inputBuffer = TensorBuffer.createFixedSize(inputTensor.shape(), inputTensor.dataType())
            val byteBuffer = ByteBuffer.allocateDirect(inputTensor.numBytes()).order(ByteOrder.nativeOrder())
            melData.forEach { byteBuffer.putFloat(it) }
            inputBuffer.loadBuffer(byteBuffer)

            val outputTensor = interp.getOutputTensor(0)
            val outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), DataType.FLOAT32)

            // 3. Inferência
            interp.run(inputBuffer.getBuffer(), outputBuffer.getBuffer())

            // 4. Decodificar Tokens
            val resBuffer = outputBuffer.getBuffer()
            resBuffer.rewind()
            val result = StringBuilder()
            val outputLen = outputBuffer.flatSize
            
            for (i in 0 until outputLen) {
                val token = resBuffer.int
                if (token == whisperUtil.getTokenEOT()) break
                
                if (token < whisperUtil.getTokenEOT()) {
                    val word = whisperUtil.getWordFromToken(token)
                    if (word != null) result.append(word)
                }
            }

            val finalResult = result.toString().trim()
            if (finalResult.isEmpty()) {
                return@withContext "Não foi possível reconhecer fala no áudio."
            }
            return@withContext finalResult

        } catch (e: Exception) {
            Log.e("WhisperTFLite", "Erro na transcrição: ${e.message}")
            throw e
        } finally {
            pcmFile.delete()
        }
    }
}
