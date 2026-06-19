package br.com.meetpen.logic

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipInputStream
import br.com.meetpen.logic.OfflineModel
import kotlinx.coroutines.CompletableDeferred

class VoskTranscriber(private val context: Context) {

    private val TAG = "VoskTranscriber"
    private var currentModelId: String? = null
    private var model: Model? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun isValidModelDir(dir: File, model: OfflineModel): Boolean {
        if (model.type == "Vosk") {
            return dir.exists() && dir.isDirectory && File(dir, "am").exists()
        } else {
            val modelFile = File(dir, model.modelFileName)
            return modelFile.exists() && modelFile.length() > 1024 * 1024 // Pelo menos 1MB
        }
    }

    fun isModelReady(model: OfflineModel): Boolean {
        val dir = File(context.filesDir, model.modelFileName)
        return (this.model != null && currentModelId == model.id) || isValidModelDir(dir, model)
    }

    fun loadModel(
        offlineModel: OfflineModel,
        onProgress: (Int) -> Unit,
        onReady: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (model != null && currentModelId == offlineModel.id) { 
            mainHandler.post { onReady() }
            return 
        }

        val modelDir = File(context.filesDir, offlineModel.modelFileName)

        Thread {
            try {
                if (model != null && currentModelId != offlineModel.id) {
                    model?.close()
                    model = null
                }

                if (modelDir.exists() && !isValidModelDir(modelDir, offlineModel)) {
                    Log.w(TAG, "Diretório inválido encontrado — apagando para re-download")
                    modelDir.deleteRecursively()
                }

                if (!modelDir.exists()) {
                    Log.i(TAG, "Baixando modelo ${offlineModel.name}…")
                    downloadAndExtract(offlineModel, onProgress)
                }

                if (offlineModel.type == "Vosk") {
                    Log.i(TAG, "Carregando modelo ${offlineModel.name} em memória…")
                    model = Model(modelDir.absolutePath)
                }
                
                currentModelId = offlineModel.id
                Log.i(TAG, "Modelo ${offlineModel.name} pronto!")
                mainHandler.post { onReady() }

            } catch (e: Exception) {
                Log.e(TAG, "Falha: ${e.message}", e)
                if (modelDir.exists()) modelDir.deleteRecursively()
                mainHandler.post { onError(e.message ?: "Erro desconhecido") }
            }
        }.start()
    }

    private fun downloadAndExtract(model: OfflineModel, onProgress: (Int) -> Unit) {
        val modelDir = File(context.filesDir, model.modelFileName)
        val isZip = model.url.endsWith(".zip")
        val tempFile = File(context.cacheDir, if (isZip) "${model.modelFileName}.zip" else model.modelFileName)
        
        val conn = URL(model.url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 120_000
        conn.connect()

        val total = conn.contentLength.toLong()
        var done = 0L

        conn.inputStream.use { inp ->
            FileOutputStream(tempFile).use { out ->
                val buf = ByteArray(8192)
                var n: Int
                while (inp.read(buf).also { n = it } != -1) {
                    out.write(buf, 0, n)
                    done += n
                    if (total > 0) mainHandler.post { onProgress((done * 100 / total).toInt()) }
                }
            }
        }
        conn.disconnect()

        if (isZip) {
            ZipInputStream(tempFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val f = File(context.filesDir, entry.name)
                    if (entry.isDirectory) { f.mkdirs() }
                    else {
                        f.parentFile?.mkdirs()
                        FileOutputStream(f).use { fos ->
                            val buf = ByteArray(8192); var n: Int
                            while (zis.read(buf).also { n = it } != -1) fos.write(buf, 0, n)
                        }
                    }
                    zis.closeEntry(); entry = zis.nextEntry
                }
            }
            tempFile.delete()
        } else {
            modelDir.mkdirs()
            val finalFile = File(modelDir, model.modelFileName)
            tempFile.renameTo(finalFile)
            
            if (model.vocabUrl != null) {
                val vocabFile = File(modelDir, "filters_vocab_gen.bin")
                if (!vocabFile.exists()) {
                    var currentUrl = model.vocabUrl
                    var connection = URL(currentUrl).openConnection() as HttpURLConnection
                    var redirect = true
                    while (redirect) {
                        connection.connectTimeout = 15000
                        connection.readTimeout = 120000
                        connection.instanceFollowRedirects = true
                        val status = connection.responseCode
                        if (status == HttpURLConnection.HTTP_MOVED_TEMP || 
                            status == HttpURLConnection.HTTP_MOVED_PERM || 
                            status == HttpURLConnection.HTTP_SEE_OTHER) {
                            currentUrl = connection.getHeaderField("Location")
                            connection = URL(currentUrl).openConnection() as HttpURLConnection
                        } else {
                            redirect = false
                        }
                    }
                    connection.inputStream.use { vInp ->
                        FileOutputStream(vocabFile).use { vOut ->
                            vInp.copyTo(vOut)
                        }
                    }
                    connection.disconnect()
                }
            }
        }
    }

    suspend fun transcribeFile(
        audioFile: File,
        offlineModel: OfflineModel,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val m = model
        if (m == null) {
            withContext(Dispatchers.Main) { onError("Modelo não carregado.") }
            return@withContext
        }
        if (!audioFile.exists()) {
            withContext(Dispatchers.Main) { onError("Arquivo não encontrado.") }
            return@withContext
        }

        val wavFile = File(context.cacheDir, "vosk_${System.currentTimeMillis()}.wav")
        try {
            val srcRate = decodeToWav(audioFile, wavFile)
            val recognizer = Recognizer(m, 16000.0f)
            val sb = StringBuilder()

            wavFile.inputStream().use { stream ->
                stream.skip(44)
                val raw = stream.readBytes()
                val pcm = if (srcRate != 16000) resample(raw, srcRate, 16000) else raw

                var off = 0
                while (off < pcm.size) {
                    val end = minOf(off + 4096, pcm.size)
                    val chunk = pcm.copyOfRange(off, end)
                    if (recognizer.acceptWaveForm(chunk, chunk.size)) {
                        val t = jsonText(recognizer.result)
                        if (t.isNotBlank()) sb.append(t).append(' ')
                    }
                    off = end
                }
            }
            val last = jsonText(recognizer.finalResult)
            if (last.isNotBlank()) sb.append(last)
            recognizer.close()

            val result = sb.toString().trim()
            withContext(Dispatchers.Main) {
                if (result.isBlank()) onError("Silêncio detectado.")
                else onResult(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro: ${e.message}", e)
            withContext(Dispatchers.Main) { onError("Erro: ${e.message}") }
        } finally {
            wavFile.delete()
        }
    }

    suspend fun transcribeFileSync(audioFile: File, offlineModel: OfflineModel): String {
        val deferred = CompletableDeferred<String>()
        transcribeFile(audioFile, offlineModel,
            onResult = { deferred.complete(it) },
            onError = { deferred.complete("Erro: $it") }
        )
        return deferred.await()
    }

    private fun resample(pcm: ByteArray, srcHz: Int, dstHz: Int): ByteArray {
        val srcFrames = pcm.size / 2
        val dstFrames = (srcFrames.toLong() * dstHz / srcHz).toInt()
        val out = ByteArray(dstFrames * 2)
        for (i in 0 until dstFrames) {
            val pos = i.toDouble() * srcHz / dstHz
            val idx = pos.toInt()
            val frac = pos - idx
            val s0 = s16le(pcm, idx * 2)
            val s1 = if ((idx + 1) * 2 + 1 < pcm.size) s16le(pcm, (idx + 1) * 2) else s0
            val v = (s0 + frac * (s1 - s0)).toInt().coerceIn(-32768, 32767).toShort()
            out[i * 2] = (v.toInt() and 0xFF).toByte()
            out[i * 2 + 1] = ((v.toInt() shr 8) and 0xFF).toByte()
        }
        return out
    }

    private fun s16le(buf: ByteArray, off: Int): Int {
        if (off + 1 >= buf.size) return 0
        val raw = (buf[off].toInt() and 0xFF) or ((buf[off + 1].toInt() and 0xFF) shl 8)
        return if (raw > 32767) raw - 65536 else raw
    }

    private fun jsonText(json: String): String {
        val m = "\"text\" : \""
        val i = json.indexOf(m); if (i < 0) return ""
        val s = i + m.length; val e = json.indexOf('"', s)
        return if (e > s) json.substring(s, e).trim() else ""
    }

    internal fun decodeToWav(input: File, output: File): Int {
        val ext = MediaExtractor()
        ext.setDataSource(input.absolutePath)
        var trackIdx = -1; var fmt: MediaFormat? = null
        for (i in 0 until ext.trackCount) {
            val f = ext.getTrackFormat(i)
            if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                trackIdx = i; fmt = f; break
            }
        }
        if (trackIdx < 0 || fmt == null) throw IllegalStateException("Sem áudio")
        ext.selectTrack(trackIdx)
        val mime = fmt.getString(MediaFormat.KEY_MIME)!!
        val sr = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val ch = if (fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 1

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(fmt, null, null, 0); codec.start()
        val pcm = mutableListOf<ByteArray>()
        val info = MediaCodec.BufferInfo(); var eos = false
        while (!eos) {
            val ii = codec.dequeueInputBuffer(10_000)
            if (ii >= 0) {
                val b = codec.getInputBuffer(ii)!!
                val sz = ext.readSampleData(b, 0)
                if (sz < 0) { codec.queueInputBuffer(ii, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); eos = true }
                else { codec.queueInputBuffer(ii, 0, sz, ext.sampleTime, 0); ext.advance() }
            }
            var oi = codec.dequeueOutputBuffer(info, 10_000)
            while (oi >= 0) {
                val b = codec.getOutputBuffer(oi)!!
                val c = ByteArray(info.size); b.get(c); pcm.add(c)
                codec.releaseOutputBuffer(oi, false); oi = codec.dequeueOutputBuffer(info, 0)
            }
        }
        codec.stop(); codec.release(); ext.release()
        val total = pcm.sumOf { it.size }
        FileOutputStream(output).use { fos -> writeWavHeader(fos, total, sr, ch); pcm.forEach { fos.write(it) } }
        return sr
    }

    private fun writeWavHeader(fos: FileOutputStream, sz: Int, sr: Int, ch: Int) {
        val bps: Short = 16; val br = sr * ch * 2; val ba = (ch * 2).toShort()
        fos.write("RIFF".toByteArray()); fos.write(i32(sz + 36))
        fos.write("WAVE".toByteArray()); fos.write("fmt ".toByteArray()); fos.write(i32(16))
        fos.write(i16(1)); fos.write(i16(ch.toShort())); fos.write(i32(sr)); fos.write(i32(br))
        fos.write(i16(ba)); fos.write(i16(bps)); fos.write("data".toByteArray()); fos.write(i32(sz))
    }

    private fun i32(v: Int) = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array()
    private fun i16(v: Short) = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(v).array()

    fun release() { model?.close(); model = null }
}
