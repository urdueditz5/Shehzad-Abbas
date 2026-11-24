package com.example.features.ai

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.ApnaDependencies
import com.example.data.repositories.AiAssistantRepository
import com.example.data.repositories.AiCitation
import com.example.data.repositories.AiGroundingMode
import com.example.data.repositories.AiImageAspectRatio
import com.example.data.repositories.AiLiveSessionHandle
import com.example.data.repositories.AiModelTier
import com.example.data.repositories.AiPersona
import com.example.data.repositories.LiveVoiceCallbacks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AiStudioTab(val label: String) {
    CHAT("Chat"),
    IMAGE("Images"),
    VOICE("Audio & Live")
}

data class AiChatBubble(
    val id: String,
    val fromUser: Boolean,
    val text: String,
    val citations: List<AiCitation> = emptyList(),
    val isError: Boolean = false
)

sealed interface ImageGenUiState {
    data object Idle : ImageGenUiState
    data object Loading : ImageGenUiState
    data class Success(
        val bitmap: Bitmap,
        val prompt: String,
        val isEdit: Boolean
    ) : ImageGenUiState

    data class Error(val message: String) : ImageGenUiState
}

sealed interface TranscriptionUiState {
    data object Idle : TranscriptionUiState
    data object Recording : TranscriptionUiState
    data object Transcribing : TranscriptionUiState
    data class Success(val transcript: String) : TranscriptionUiState
    data class Error(val message: String) : TranscriptionUiState
}

data class LiveVoiceTurn(
    val id: String,
    val isUser: Boolean,
    val text: String
)

sealed interface LiveVoiceUiState {
    data object Idle : LiveVoiceUiState
    data object Connecting : LiveVoiceUiState
    data class Active(val turns: List<LiveVoiceTurn>) : LiveVoiceUiState
    data class Error(val message: String) : LiveVoiceUiState
}

class AiViewModel(
    private val aiRepository: AiAssistantRepository
) : ViewModel() {

    constructor(deps: ApnaDependencies) : this(deps.aiRepository)

    private val _selectedTab = MutableStateFlow(AiStudioTab.CHAT)
    val selectedTab: StateFlow<AiStudioTab> = _selectedTab.asStateFlow()

    private val _selectedModelTier = MutableStateFlow(AiModelTier.BALANCED)
    val selectedModelTier: StateFlow<AiModelTier> = _selectedModelTier.asStateFlow()

    private val _selectedPersona = MutableStateFlow(AiPersona.GENERAL)
    val selectedPersona: StateFlow<AiPersona> = _selectedPersona.asStateFlow()

    private val _selectedGrounding = MutableStateFlow(AiGroundingMode.NONE)
    val selectedGrounding: StateFlow<AiGroundingMode> = _selectedGrounding.asStateFlow()

    private val _messages = MutableStateFlow<List<AiChatBubble>>(emptyList())
    val messages: StateFlow<List<AiChatBubble>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _imageEditSource = MutableStateFlow<Bitmap?>(null)
    val imageEditSource: StateFlow<Bitmap?> = _imageEditSource.asStateFlow()

    private val _imageAspectRatio = MutableStateFlow(AiImageAspectRatio.SQUARE)
    val imageAspectRatio: StateFlow<AiImageAspectRatio> = _imageAspectRatio.asStateFlow()

    private val _imageGenState = MutableStateFlow<ImageGenUiState>(ImageGenUiState.Idle)
    val imageGenState: StateFlow<ImageGenUiState> = _imageGenState.asStateFlow()

    private val _transcriptionState = MutableStateFlow<TranscriptionUiState>(TranscriptionUiState.Idle)
    val transcriptionState: StateFlow<TranscriptionUiState> = _transcriptionState.asStateFlow()

    private val _liveVoiceState = MutableStateFlow<LiveVoiceUiState>(LiveVoiceUiState.Idle)
    val liveVoiceState: StateFlow<LiveVoiceUiState> = _liveVoiceState.asStateFlow()

    private var chatJob: Job? = null
    private var imageJob: Job? = null
    private var transcriptionJob: Job? = null
    private var liveConnectJob: Job? = null
    private var liveSessionHandle: AiLiveSessionHandle? = null

    fun selectTab(tab: AiStudioTab) {
        _selectedTab.value = tab
    }

    fun selectModelTier(tier: AiModelTier) {
        _selectedModelTier.value = tier
    }

    fun selectPersona(persona: AiPersona) {
        _selectedPersona.value = persona
    }

    fun selectGrounding(mode: AiGroundingMode) {
        _selectedGrounding.value = mode
    }

    fun sendPrompt(prompt: String) {
        val clean = prompt.trim()
        if (clean.isBlank() || _isGenerating.value) return

        _selectedTab.value = AiStudioTab.CHAT
        _lastError.value = null
        val now = System.currentTimeMillis()
        val userBubble = AiChatBubble(
            id = "u_$now",
            fromUser = true,
            text = clean
        )
        val aiId = "ai_${now + 1}"
        _messages.value = _messages.value + userBubble + AiChatBubble(aiId, fromUser = false, text = "")
        _isGenerating.value = true

        val tier = _selectedModelTier.value
        val persona = _selectedPersona.value
        val grounding = _selectedGrounding.value

        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            try {
                aiRepository.sendMessageStream(
                    prompt = clean,
                    modelTier = tier,
                    persona = persona,
                    groundingMode = grounding
                ).collect { chunk ->
                    _messages.value = _messages.value.map {
                        if (it.id == aiId) {
                            it.copy(
                                text = chunk.text,
                                citations = chunk.citations,
                                isError = false
                            )
                        } else {
                            it
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "AI Assistant could not respond right now. Please try again."
                _lastError.value = msg
                _messages.value = _messages.value.map {
                    if (it.id == aiId && it.text.isBlank()) {
                        it.copy(text = msg, isError = true)
                    } else {
                        it
                    }
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun clearConversation() {
        chatJob?.cancel()
        _isGenerating.value = false
        _lastError.value = null
        _messages.value = emptyList()
        aiRepository.resetConversation()
    }

    fun setImageEditSource(bitmap: Bitmap?) {
        _imageEditSource.value = bitmap
    }

    fun selectImageAspectRatio(ratio: AiImageAspectRatio) {
        _imageAspectRatio.value = ratio
    }

    fun generateOrEditImage(prompt: String) {
        val clean = prompt.trim()
        if (clean.isBlank()) {
            _imageGenState.value = ImageGenUiState.Error("Please enter a prompt describing the image.")
            return
        }
        val sourceBitmap = _imageEditSource.value
        val ratio = _imageAspectRatio.value
        imageJob?.cancel()
        _imageGenState.value = ImageGenUiState.Loading
        imageJob = viewModelScope.launch {
            try {
                val result = aiRepository.generateOrEditImage(
                    prompt = clean,
                    inputBitmap = sourceBitmap,
                    aspectRatio = ratio
                )
                _imageGenState.value = ImageGenUiState.Success(
                    bitmap = result,
                    prompt = clean,
                    isEdit = sourceBitmap != null
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _imageGenState.value = ImageGenUiState.Error(
                    e.localizedMessage ?: "Image generation failed."
                )
            }
        }
    }

    fun onAudioRecordingStarted() {
        _transcriptionState.value = TranscriptionUiState.Recording
    }

    fun onAudioRecordingError(message: String) {
        _transcriptionState.value = TranscriptionUiState.Error(message)
    }

    fun transcribeAudioBytes(audioBytes: ByteArray, mimeType: String = "audio/mp4") {
        transcriptionJob?.cancel()
        _transcriptionState.value = TranscriptionUiState.Transcribing
        transcriptionJob = viewModelScope.launch {
            try {
                val transcript = aiRepository.transcribeAudio(audioBytes, mimeType)
                _transcriptionState.value = TranscriptionUiState.Success(transcript)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _transcriptionState.value = TranscriptionUiState.Error(
                    e.localizedMessage ?: "Could not transcribe audio recording."
                )
            }
        }
    }

    fun sendTranscriptToChat(transcript: String) {
        if (transcript.isBlank()) return
        _selectedTab.value = AiStudioTab.CHAT
        sendPrompt(transcript)
    }

    fun startLiveVoiceConversation() {
        if (_liveVoiceState.value is LiveVoiceUiState.Connecting ||
            _liveVoiceState.value is LiveVoiceUiState.Active
        ) {
            return
        }
        liveConnectJob?.cancel()
        _liveVoiceState.value = LiveVoiceUiState.Connecting
        val persona = _selectedPersona.value
        liveConnectJob = viewModelScope.launch {
            try {
                val handle = aiRepository.startLiveVoiceSession(
                    persona = persona,
                    callbacks = LiveVoiceCallbacks(
                        onTranscript = { userText, modelText ->
                            val currentTurns = (_liveVoiceState.value as? LiveVoiceUiState.Active)
                                ?.turns.orEmpty().toMutableList()
                            if (!userText.isNullOrBlank()) {
                                currentTurns.add(
                                    LiveVoiceTurn(
                                        id = "lv_u_${System.nanoTime()}",
                                        isUser = true,
                                        text = userText.trim()
                                    )
                                )
                            }
                            if (!modelText.isNullOrBlank()) {
                                currentTurns.add(
                                    LiveVoiceTurn(
                                        id = "lv_m_${System.nanoTime()}",
                                        isUser = false,
                                        text = modelText.trim()
                                    )
                                )
                            }
                            _liveVoiceState.value = LiveVoiceUiState.Active(currentTurns)
                        },
                        onError = { message ->
                            liveSessionHandle = null
                            _liveVoiceState.value = LiveVoiceUiState.Error(message)
                        },
                        onEnded = {
                            liveSessionHandle = null
                            if (_liveVoiceState.value !is LiveVoiceUiState.Error) {
                                _liveVoiceState.value = LiveVoiceUiState.Idle
                            }
                        }
                    )
                )
                liveSessionHandle = handle
                if (_liveVoiceState.value is LiveVoiceUiState.Connecting) {
                    _liveVoiceState.value = LiveVoiceUiState.Active(emptyList())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                liveSessionHandle = null
                _liveVoiceState.value = LiveVoiceUiState.Error(
                    e.localizedMessage ?: "Failed to start Gemini Live voice conversation."
                )
            }
        }
    }

    fun stopLiveVoiceConversation() {
        liveConnectJob?.cancel()
        liveSessionHandle?.stop()
        liveSessionHandle = null
        _liveVoiceState.value = LiveVoiceUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        liveSessionHandle?.stop()
        liveSessionHandle = null
    }
}
