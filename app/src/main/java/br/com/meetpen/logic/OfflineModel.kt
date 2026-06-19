package br.com.meetpen.logic

sealed class OfflineModel(
    val id: String,
    val name: String,
    val type: String,
    val size: String,
    val description: String,
    val url: String,
    val modelFileName: String,
    val vocabUrl: String? = null // Adicionado para o Whisper
) {
    object VoskSmall : OfflineModel(
        id = "vosk_small",
        name = "Vosk Standard (PT-BR)",
        type = "Vosk",
        size = "~45MB",
        description = "Rápido e leve. Ideal para a maioria dos aparelhos.",
        url = "https://alphacephei.com/vosk/models/vosk-model-small-pt-0.3.zip",
        modelFileName = "vosk-model-small-pt-0.3"
    )

    object WhisperTiny : OfflineModel(
        id = "whisper_tiny",
        name = "Whisper Tiny (Multi)",
        type = "Whisper",
        size = "~40MB",
        description = "Tecnologia da OpenAI leve. Boa para comandos rápidos.",
        url = "https://huggingface.co/cik009/whisper/resolve/main/whisper-tiny.tflite",
        modelFileName = "whisper-tiny.tflite",
        vocabUrl = "https://raw.githubusercontent.com/vilassn/whisper_android/master/whisper_java/app/src/main/assets/filters_vocab_multilingual.bin"
    )

    object WhisperBase : OfflineModel(
        id = "whisper_base",
        name = "Whisper Base (Multi) ⭐",
        type = "Whisper",
        size = "~145MB",
        description = "Opção Intermediária. Muito mais precisa que o Tiny.",
        url = "https://huggingface.co/cik009/whisper/resolve/main/whisper-base.tflite",
        modelFileName = "whisper-base.tflite",
        vocabUrl = "https://raw.githubusercontent.com/vilassn/whisper_android/master/whisper_java/app/src/main/assets/filters_vocab_multilingual.bin"
    )

    companion object {
        val ALL = listOf(VoskSmall, WhisperTiny, WhisperBase)
        fun fromId(id: String) = ALL.find { it.id == id } ?: VoskSmall
    }
}
