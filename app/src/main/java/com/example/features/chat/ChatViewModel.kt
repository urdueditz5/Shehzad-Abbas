package com.example.features.chat

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.ApnaDependencies
import com.example.core.firebase.OperationType
import com.example.core.firebase.UiState
import com.example.core.firebase.handleFirestoreError
import com.example.core.firebase.unwrapFirestoreError
import com.example.core.utils.MediaUtils
import com.example.data.models.ChatMessage
import com.example.data.repositories.ChatMessagingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class ChatUiState(
    val messagesState: UiState<List<ChatMessage>> = UiState.Loading,
    val draftMessage: String = "",
    val isSending: Boolean = false,
    val isUploadingImage: Boolean = false,
    val errorMessage: String? = null
)

class ChatViewModel(
    private val chatRepository: ChatMessagingRepository,
    private val chatId: String,
    private val currentUserId: String,
    otherUserId: String,
    private val currentUserNameProvider: () -> String
) : ViewModel() {

    constructor(
        deps: ApnaDependencies,
        chatId: String,
        currentUserId: String,
        otherUserId: String,
        currentUserNameProvider: () -> String
    ) : this(
        chatRepository = deps.chatRepository,
        chatId = chatId,
        currentUserId = currentUserId,
        otherUserId = otherUserId,
        currentUserNameProvider = currentUserNameProvider
    )

    private val participantIds: List<String> = listOf(currentUserId, otherUserId).sorted()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        if (chatId.isBlank() || currentUserId.isBlank()) {
            _uiState.update {
                it.copy(messagesState = UiState.Error("Conversation unavailable."))
            }
        } else {
            viewModelScope.launch {
                chatRepository.markChatRead(chatId, listOf(currentUserId))
            }
            viewModelScope.launch {
                chatRepository.observeMessages(chatId, currentUserId)
                    .catch { err ->
                        val root = unwrapFirestoreError(err)
                        handleFirestoreError(root, OperationType.LIST, "chats/$chatId/messages")
                        _uiState.update {
                            it.copy(
                                messagesState = UiState.Error(
                                    root.localizedMessage ?: "Failed to load messages."
                                )
                            )
                        }
                    }
                    .collect { messages ->
                        _uiState.update {
                            it.copy(messagesState = UiState.Success(messages))
                        }
                        if (messages.any { it.senderId != currentUserId }) {
                            chatRepository.markChatRead(chatId, listOf(currentUserId))
                        }
                    }
            }
        }
    }

    fun onDraftChanged(text: String) {
        _uiState.update {
            it.copy(
                draftMessage = text.take(2000),
                errorMessage = null
            )
        }
    }

    fun sendTextMessage() {
        val currentState = _uiState.value
        val trimmed = currentState.draftMessage.trim()
        if (trimmed.isBlank() || currentState.isSending) return

        _uiState.update {
            it.copy(
                draftMessage = "",
                isSending = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val result = chatRepository.sendMessage(
                chatId = chatId,
                participantIds = participantIds,
                senderName = currentUserNameProvider().ifBlank { "You" },
                text = trimmed
            )
            result.onSuccess {
                _uiState.update { it.copy(isSending = false) }
            }.onFailure { err ->
                if (err is CancellationException) throw err
                _uiState.update { state ->
                    state.copy(
                        isSending = false,
                        draftMessage = state.draftMessage.ifBlank { trimmed },
                        errorMessage = err.localizedMessage ?: "Failed to send message."
                    )
                }
            }
        }
    }

    fun sendImageUri(context: Context, uri: Uri) {
        if (_uiState.value.isUploadingImage) return
        val appContext = context.applicationContext
        _uiState.update {
            it.copy(
                isUploadingImage = true,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            MediaUtils.compressUriToDataUri(appContext, uri, maxDim = 640)
                .onSuccess { dataUri ->
                    val sendRes = chatRepository.sendMessage(
                        chatId = chatId,
                        participantIds = participantIds,
                        senderName = currentUserNameProvider().ifBlank { "You" },
                        text = "",
                        imageUrl = dataUri
                    )
                    sendRes.onFailure { err ->
                        if (err is CancellationException) throw err
                        _uiState.update {
                            it.copy(errorMessage = err.localizedMessage ?: "Failed to send photo.")
                        }
                    }
                }
                .onFailure { err ->
                    if (err is CancellationException) throw err
                    _uiState.update {
                        it.copy(errorMessage = err.localizedMessage ?: "Failed to prepare image.")
                    }
                }
            _uiState.update { it.copy(isUploadingImage = false) }
        }
    }

    fun hideConversation(onDone: () -> Unit) {
        viewModelScope.launch {
            chatRepository.hideConversationLocally(chatId, emptyList())
                .onSuccess { onDone() }
                .onFailure { err ->
                    if (err is CancellationException) throw err
                    _uiState.update {
                        it.copy(errorMessage = err.localizedMessage ?: "Unable to hide conversation.")
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
