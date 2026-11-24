package com.example.data.repositories

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.liveAudioConversationConfig
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

enum class AiModelTier(
    val modelId: String,
    val label: String,
    val subtitle: String
) {
    FAST("gemini-3.5-flash-lite", "Fast", "3.5 Flash-Lite"),
    BALANCED("gemini-3.8-flash", "Balanced", "3.8 Flash"),
    DEEP_REASONING("gemini-4-argon", "Reasoning", "4 Argon")
}

enum class AiPersona(
    val label: String,
    val systemInstruction: String
) {
    GENERAL(
        label = "Assistant",
        systemInstruction = "You are ApnaHub AI Assistant, a helpful, respectful, concise assistant for users in Pakistan. " +
            "You fluently support English, Urdu (اردو script), and Roman Urdu. " +
            "Help with everyday questions, writing, study explanations, and productivity. Never fabricate facts."
    ),
    TUTOR(
        label = "Study Tutor",
        systemInstruction = "You are ApnaHub Study Tutor for Pakistani students (Matric, FSc, ICS, O/A Levels, University). " +
            "Explain concepts step by step with clear examples in English and simple Urdu/Roman Urdu where helpful."
    ),
    CAREER(
        label = "Career Coach",
        systemInstruction = "You are ApnaHub Career & Freelancing Coach. Help craft strong CVs, cover letters, " +
            "Upwork/Fiverr proposals, and interview answers tailored to Pakistan and remote tech markets."
    ),
    TRANSLATOR(
        label = "Translator",
        systemInstruction = "You are ApnaHub Bilingual Translator specializing in English, Urdu (اردو), and Roman Urdu. " +
            "Provide accurate, natural translations preserving tone and cultural nuance."
    )
}

enum class AiGroundingMode(val label: String) {
    NONE("Standard"),
    GOOGLE_SEARCH("Google Search"),
    GOOGLE_MAPS("Google Maps")
}

enum class AiImageAspectRatio(
    val label: String,
    val sdkValue: AspectRatio
) {
    SQUARE("1:1", AspectRatio.SQUARE_1x1),
    LANDSCAPE("16:9", AspectRatio.LANDSCAPE_16x9),
    PORTRAIT("9:16", AspectRatio.PORTRAIT_9x16)
}

enum class AiCitationType {
    WEB,
    MAPS
}

data class AiCitation(
    val title: String,
    val uri: String,
    val type: AiCitationType,
    val subtitle: String? = null
)

data class AiStreamChunk(
    val text: String,
    val citations: List<AiCitation> = emptyList()
)

data class LiveVoiceCallbacks(
    val onTranscript: (userText: String?, modelText: String?) -> Unit,
    val onError: (String) -> Unit,
    val onEnded: () -> Unit
)

interface AiLiveSessionHandle {
    fun stop()
}

interface AiAssistantRepository {
    fun resetConversation()

    fun sendMessageStream(
        prompt: String,
        modelTier: AiModelTier = AiModelTier.BALANCED,
        persona: AiPersona = AiPersona.GENERAL,
        groundingMode: AiGroundingMode = AiGroundingMode.NONE
    ): Flow<AiStreamChunk>

    suspend fun generateOrEditImage(
        prompt: String,
        inputBitmap: Bitmap?,
        aspectRatio: AiImageAspectRatio
    ): Bitmap

    suspend fun transcribeAudio(
        audioBytes: ByteArray,
        mimeType: String = "audio/mp4"
    ): String

    suspend fun startLiveVoiceSession(
        persona: AiPersona,
        callbacks: LiveVoiceCallbacks
    ): AiLiveSessionHandle
}

class AiRepository : AiAssistantRepository {

    private data class ChatConfigKey(
        val modelTier: AiModelTier,
        val persona: AiPersona,
        val groundingMode: AiGroundingMode
    )

    private var activeConfig: ChatConfigKey? = null
    private var activeChat: Chat? = null
    private val conversationHistory = mutableListOf<Content>()

    private val transcriptionModel: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = AiModelTier.BALANCED.modelId
        )
    }

    private fun buildChatModel(config: ChatConfigKey): GenerativeModel {
        val tools = when (config.groundingMode) {
            AiGroundingMode.NONE -> emptyList()
            AiGroundingMode.GOOGLE_SEARCH -> listOf(Tool.googleSearch())
            AiGroundingMode.GOOGLE_MAPS -> listOf(Tool.googleMaps())
        }
        return Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = config.modelTier.modelId,
            tools = tools,
            systemInstruction = content { text(config.persona.systemInstruction) }
        )
    }

    private fun getOrCreateChat(config: ChatConfigKey): Chat {
        val existing = activeChat
        if (existing != null && activeConfig == config) {
            return existing
        }
        val model = buildChatModel(config)
        val chat = model.startChat(history = conversationHistory.toList())
        activeConfig = config
        activeChat = chat
        return chat
    }

    override fun resetConversation() {
        conversationHistory.clear()
        activeChat = null
        activeConfig = null
    }

    override fun sendMessageStream(
        prompt: String,
        modelTier: AiModelTier,
        persona: AiPersona,
        groundingMode: AiGroundingMode
    ): Flow<AiStreamChunk> = flow {
        val cleanPrompt = prompt.trim()
        require(cleanPrompt.isNotBlank()) { "Please enter a question or prompt." }

        val config = ChatConfigKey(modelTier, persona, groundingMode)
        val chatSession = getOrCreateChat(config)

        val accumulated = StringBuilder()
        val citationsByUri = linkedMapOf<String, AiCitation>()

        try {
            withTimeout(60_000L) {
                chatSession.sendMessageStream(cleanPrompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                    }
                    chunk.candidates.firstOrNull()?.groundingMetadata?.groundingChunks?.forEach { gChunk ->
                        gChunk.web?.let { web ->
                            val uri = web.uri?.takeIf { it.isNotBlank() } ?: return@let
                            citationsByUri[uri] = AiCitation(
                                title = web.title?.takeIf { it.isNotBlank() } ?: web.domain ?: "Web Source",
                                uri = uri,
                                type = AiCitationType.WEB,
                                subtitle = web.domain
                            )
                        }
                        gChunk.maps?.let { maps ->
                            val uri = maps.uri?.takeIf { it.isNotBlank() } ?: return@let
                            citationsByUri[uri] = AiCitation(
                                title = maps.title?.takeIf { it.isNotBlank() } ?: "Map Location",
                                uri = uri,
                                type = AiCitationType.MAPS
                            )
                        }
                    }
                    if (piece.isNotEmpty() || citationsByUri.isNotEmpty()) {
                        emit(
                            AiStreamChunk(
                                text = accumulated.toString(),
                                citations = citationsByUri.values.toList()
                            )
                        )
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("AI Assistant returned an empty response. Please try again.")
            }
            conversationHistory.add(content("user") { text(cleanPrompt) })
            conversationHistory.add(content("model") { text(accumulated.toString()) })
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI stream timed out", e)
            throw IllegalStateException("Request timed out after 60 seconds. Please check your connection and try again.")
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "AI quota exceeded", e)
            throw IllegalStateException(
                e.localizedMessage ?: "Usage limit reached for ${modelTier.label}. Try Balanced mode or try again later."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            throw IllegalStateException(
                e.localizedMessage ?: "AI service is currently unavailable. Please try again later."
            )
        }
    }

    override suspend fun generateOrEditImage(
        prompt: String,
        inputBitmap: Bitmap?,
        aspectRatio: AiImageAspectRatio
    ): Bitmap {
        val cleanPrompt = prompt.trim()
        require(cleanPrompt.isNotBlank()) { "Please describe the image you want to create or edit." }

        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = ImageConfig.builder()
                .setAspectRatio(aspectRatio.sdkValue)
                .setImageSize(ImageSize.SIZE_1K)
                .build()
        }
        val imageModel = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = IMAGE_MODEL_ID,
            generationConfig = config
        )

        return try {
            val response = withTimeout(60_000L) {
                if (inputBitmap != null) {
                    imageModel.generateContent(
                        content {
                            image(inputBitmap)
                            text(cleanPrompt)
                        }
                    )
                } else {
                    imageModel.generateContent(cleanPrompt)
                }
            }
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
            imagePart?.image ?: throw IllegalStateException("The model did not return an image. Try refining your prompt.")
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out", e)
            throw IllegalStateException("Image generation timed out after 60 seconds. Please try again.")
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Image generation quota exceeded", e)
            throw IllegalStateException(
                e.localizedMessage ?: "Image generation requires an active Blaze plan or available quota."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            throw IllegalStateException(
                e.localizedMessage ?: "Image generation failed. Please verify App Check & Blaze billing."
            )
        }
    }

    override suspend fun transcribeAudio(
        audioBytes: ByteArray,
        mimeType: String
    ): String {
        require(audioBytes.isNotEmpty()) { "Recorded audio was empty. Please speak clearly and try again." }
        require(audioBytes.size <= MAX_INLINE_AUDIO_BYTES) {
            "Audio clip is too large (over 18 MB). Please record a shorter clip."
        }

        return try {
            val response = withTimeout(60_000L) {
                transcriptionModel.generateContent(
                    content {
                        inlineData(audioBytes, mimeType)
                        text(
                            "Transcribe the spoken audio accurately in its original language " +
                                "(English, Urdu, or Roman Urdu). Return only the clean transcription text without extra commentary."
                        )
                    }
                )
            }
            val transcript = response.text?.trim().orEmpty()
            if (transcript.isBlank()) {
                throw IllegalStateException("No speech could be transcribed from the recording.")
            }
            transcript
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Audio transcription timed out", e)
            throw IllegalStateException("Transcription timed out after 60 seconds. Please try a shorter clip.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Audio transcription failed", e)
            throw IllegalStateException(
                e.localizedMessage ?: "Audio transcription failed. Please try again."
            )
        }
    }

    @OptIn(PublicPreviewAPI::class)
    override suspend fun startLiveVoiceSession(
        persona: AiPersona,
        callbacks: LiveVoiceCallbacks
    ): AiLiveSessionHandle {
        val liveModel = Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
            modelName = LIVE_VOICE_MODEL_ID,
            generationConfig = liveGenerationConfig {
                responseModality = ResponseModality.AUDIO
                speechConfig = SpeechConfig(voice = Voice("Aoede"))
                inputAudioTranscription = AudioTranscriptionConfig()
                outputAudioTranscription = AudioTranscriptionConfig()
            },
            systemInstruction = content {
                text(
                    persona.systemInstruction +
                        " Keep spoken responses natural, friendly, and concise for voice conversation."
                )
            }
        )

        val session: LiveSession = try {
            withTimeout(30_000L) {
                liveModel.connect()
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Live voice connection timed out", e)
            throw IllegalStateException("Live voice connection timed out after 30 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Live voice connect failed", e)
            throw IllegalStateException(
                e.localizedMessage ?: "Could not connect to Gemini Live voice service."
            )
        }

        val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val conversationJob: Job = sessionScope.launch {
            try {
                session.startAudioConversation(
                    liveAudioConversationConfig {
                        enableInterruptions = true
                        transcriptHandler = { inputTx, outputTx ->
                            callbacks.onTranscript(inputTx?.text, outputTx?.text)
                        }
                        goAwayHandler = {
                            callbacks.onEnded()
                        }
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Live voice session error", e)
                callbacks.onError(e.localizedMessage ?: "Live voice conversation interrupted.")
            } finally {
                callbacks.onEnded()
            }
        }

        return object : AiLiveSessionHandle {
            override fun stop() {
                sessionScope.launch {
                    try {
                        session.stopAudioConversation()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error stopping audio conversation", e)
                    }
                    try {
                        session.close()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error closing live session", e)
                    }
                    conversationJob.cancel()
                    sessionScope.cancel()
                }
            }
        }
    }

    companion object {
        private const val TAG = "AiRepository"
        const val IMAGE_MODEL_ID = "gemini-nano-banana-2.1"
        const val LIVE_VOICE_MODEL_ID = "gemini-3.1-flash-live-preview"
        private const val MAX_INLINE_AUDIO_BYTES = 18 * 1024 * 1024
    }
}
