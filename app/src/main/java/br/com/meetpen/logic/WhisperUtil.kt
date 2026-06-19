package br.com.meetpen.logic

import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

class WhisperUtil {
    companion object {
        private const val TAG = "WhisperUtil"
        const val WHISPER_SAMPLE_RATE = 16000
        const val WHISPER_N_FFT = 400
        const val WHISPER_N_MEL = 80
        const val WHISPER_HOP_LENGTH = 160
        const val WHISPER_CHUNK_SIZE = 30
        const val WHISPER_MEL_LEN = 3000
    }

    private val vocab = WhisperVocab()
    private val filters = WhisperFilter()

    fun getTokenEOT() = vocab.tokenEOT
    fun getWordFromToken(token: Int) = vocab.tokenToWord[token]

    fun loadFiltersAndVocab(multilingual: Boolean, vocabPath: String): Boolean {
        try {
            val file = File(vocabPath)
            if (!file.exists()) return false
            val bytes = file.readBytes()
            val vocabBuf = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder())

            val magic = vocabBuf.getInt()
            if (magic != 0x5553454e) {
                Log.d(TAG, "Invalid vocab file (bad magic: $magic)")
                return false
            }

            filters.nMel = vocabBuf.getInt()
            filters.nFft = vocabBuf.getInt()
            
            val filterDataSize = filters.nMel * filters.nFft
            filters.data = FloatArray(filterDataSize)
            for (i in 0 until filterDataSize) {
                filters.data[i] = vocabBuf.getFloat()
            }

            val nVocab = vocabBuf.getInt()
            for (i in 0 until nVocab) {
                val len = vocabBuf.getInt()
                val wordBytes = ByteArray(len)
                vocabBuf.get(wordBytes)
                vocab.tokenToWord[i] = String(wordBytes)
            }

            val nVocabAdditional = if (!multilingual) vocab.nVocabEnglish else vocab.nVocabMultilingual
            if (multilingual) {
                vocab.tokenEOT++
                vocab.tokenSOT++
                vocab.tokenPREV++
                vocab.tokenSOLM++
                vocab.tokenNOT++
                vocab.tokenBEG++
            }

            for (i in nVocab until nVocabAdditional) {
                val word = when (i) {
                    vocab.tokenEOT -> "[_EOT_]"
                    vocab.tokenSOT -> "[_SOT_]"
                    vocab.tokenPREV -> "[_PREV_]"
                    vocab.tokenNOT -> "[_NOT_]"
                    vocab.tokenBEG -> "[_BEG_]"
                    else -> if (i > vocab.tokenBEG) "[_TT_${i - vocab.tokenBEG}]" else "[_extra_token_$i]"
                }
                vocab.tokenToWord[i] = word
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading filters: ${e.message}")
            return false
        }
    }

    fun getMelSpectrogram(samples: FloatArray, nThreads: Int): FloatArray {
        val fftSize = WHISPER_N_FFT
        val fftStep = WHISPER_HOP_LENGTH
        val nSamples = samples.size
        val nLen = nSamples / fftStep
        val melData = FloatArray(WHISPER_N_MEL * nLen)

        val hann = FloatArray(fftSize) { i ->
            (0.5 * (1.0 - cos(2.0 * PI * i / fftSize))).toFloat()
        }

        val nFft = 1 + fftSize / 2
        val threads = mutableListOf<Thread>()

        for (ith in 0 until nThreads) {
            val thread = Thread {
                val fftIn = FloatArray(fftSize)
                val fftOut = FloatArray(fftSize * 2)

                for (i in ith until nLen step nThreads) {
                    val offset = i * fftStep
                    for (j in 0 until fftSize) {
                        fftIn[j] = if (offset + j < nSamples) hann[j] * samples[offset + j] else 0.0f
                    }

                    fft(fftIn, fftOut)
                    for (j in 0 until fftSize) {
                        fftOut[j] = fftOut[2 * j] * fftOut[2 * j] + fftOut[2 * j + 1] * fftOut[2 * j + 1]
                    }
                    for (j in 1 until fftSize / 2) {
                        fftOut[j] += fftOut[fftSize - j]
                    }

                    for (j in 0 until WHISPER_N_MEL) {
                        var sum = 0.0
                        for (k in 0 until nFft) {
                            sum += (fftOut[k] * filters.data[j * nFft + k])
                        }
                        if (sum < 1e-10) sum = 1e-10
                        melData[j * nLen + i] = log10(sum).toFloat()
                    }
                }
            }
            threads.add(thread)
            thread.start()
        }

        threads.forEach { it.join() }

        var mmax = -1e20f
        for (i in melData.indices) if (melData[i] > mmax) mmax = melData[i]
        
        mmax -= 8.0f
        for (i in melData.indices) {
            if (melData[i] < mmax) melData[i] = mmax
            melData[i] = (melData[i] + 4.0f) / 4.0f
        }

        return melData
    }

    private fun fft(input: FloatArray, output: FloatArray) {
        val inSize = input.size
        if (inSize == 1) {
            output[0] = input[0]
            output[1] = 0.0f
            return
        }
        if (inSize % 2 == 1) {
            dft(input, output)
            return
        }

        val even = FloatArray(inSize / 2)
        val odd = FloatArray(inSize / 2)
        for (i in 0 until inSize / 2) {
            even[i] = input[i * 2]
            odd[i] = input[i * 2 + 1]
        }

        val evenFft = FloatArray(inSize)
        val oddFft = FloatArray(inSize)
        fft(even, evenFft)
        fft(odd, oddFft)

        for (k in 0 until inSize / 2) {
            val theta = 2.0 * PI * k / inSize
            val re = cos(theta).toFloat()
            val im = -sin(theta).toFloat()
            val reOdd = oddFft[2 * k]
            val imOdd = oddFft[2 * k + 1]
            output[2 * k] = evenFft[2 * k] + re * reOdd - im * imOdd
            output[2 * k + 1] = evenFft[2 * k + 1] + re * imOdd + im * reOdd
            output[2 * (k + inSize / 2)] = evenFft[2 * k] - re * reOdd + im * imOdd
            output[2 * (k + inSize / 2) + 1] = evenFft[2 * k + 1] - re * imOdd - im * reOdd
        }
    }

    private fun dft(input: FloatArray, output: FloatArray) {
        val inSize = input.size
        for (k in 0 until inSize) {
            var re = 0.0f
            var im = 0.0f
            for (n in 0 until inSize) {
                val angle = 2.0 * PI * k * n / inSize
                re += (input[n] * cos(angle)).toFloat()
                im -= (input[n] * sin(angle)).toFloat()
            }
            output[k * 2] = re
            output[k * 2 + 1] = im
        }
    }

    private class WhisperVocab {
        var tokenEOT = 50256
        var tokenSOT = 50257
        var tokenPREV = 50360
        var tokenSOLM = 50361
        var tokenNOT = 50362
        var tokenBEG = 50363
        val tokenTRANSLATE = 50358
        val tokenTRANSCRIBE = 50359
        val nVocabEnglish = 51864
        val nVocabMultilingual = 51865
        val tokenToWord = HashMap<Int, String>()
    }

    private class WhisperFilter {
        var nMel = 0
        var nFft = 0
        var data = FloatArray(0)
    }
}
