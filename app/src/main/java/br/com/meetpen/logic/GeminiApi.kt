package br.com.meetpen.logic

import android.util.Base64
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun callGemini(prompt: String, audioFile: File?, apiKey: String, onResult: (String) -> Unit) {
        val cleanKey = apiKey.trim()
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=$cleanKey"

        val parts = JSONArray()
        audioFile?.let {
            val audioBytes = it.readBytes()
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            parts.put(JSONObject().apply {
                put("inline_data", JSONObject().apply {
                    put("mime_type", "audio/3gpp")
                    put("data", base64Audio)
                })
            })
        }
        parts.put(JSONObject().apply { put("text", prompt) })

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply { put(JSONObject().apply { put("parts", parts) }) })
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { onResult("Erro: ${e.message}") }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                try {
                    if (response.isSuccessful && body != null) {
                        val json = JSONObject(body)
                        var text = json.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                        
                        Log.d("MeetPen", "Resposta RAW do Gemini: $text")
                        
                        // Limpeza profunda de Markdown
                        text = text.replace("```json", "")
                                   .replace("```", "")
                                   .trim()
                        
                        onResult(text)
                    } else {
                        onResult("Erro API: ${response.code}")
                    }
                } catch (e: Exception) { 
                    Log.e("MeetPen", "Erro ao processar JSON: ${e.message}")
                    onResult("Erro no processamento") 
                }
            }
        })
    }

    fun transcribe(file: File, apiKey: String, onResult: (String) -> Unit) {
        val prompt = "Transcreva este áudio em português. Retorne APENAS o texto transcrito."
        callGemini(prompt, file, apiKey, onResult)
    }

    fun summarize(text: String, apiKey: String, onResult: (String) -> Unit) {
        val prompt = "Resuma este texto em 3 pontos curtos e diretos:\n\n$text"
        callGemini(prompt, null, apiKey, onResult)
    }

    fun generateTodo(text: String, apiKey: String, onResult: (String) -> Unit) {
        val prompt = """
            Extraia as tarefas do texto abaixo. 
            Responda EXCLUSIVAMENTE com um array JSON válido.
            Não inclua nenhuma introdução, explicação ou formatação markdown.
            Formato: [{"task": "o que fazer", "done": false}]
            Se vazio, responda: []
            Texto: $text
        """.trimIndent()
        callGemini(prompt, null, apiKey) { rawResult ->
            var cleaned = rawResult.trim()
            val startIndex = cleaned.indexOf("[")
            val endIndex = cleaned.lastIndexOf("]")
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                cleaned = cleaned.substring(startIndex, endIndex + 1)
            }
            onResult(cleaned)
        }
    }
}
