package com.example.features.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.core.components.ApnaEmptyState
import com.example.core.components.ApnaErrorState
import com.example.core.components.SafetyNoticeBanner
import com.example.core.components.SkeletonLoadingList
import com.example.core.components.UserAvatar
import com.example.core.firebase.ApnaDependencies
import com.example.core.firebase.OperationType
import com.example.core.firebase.UiState
import com.example.core.firebase.handleFirestoreError
import com.example.core.firebase.unwrapFirestoreError
import com.example.data.models.ChatThread
import com.example.data.models.PrivateUser
import com.example.data.models.PublicProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InboxViewModel(
    deps: ApnaDependencies,
    currentUserId: String
) : ViewModel() {
    val chatsState: StateFlow<UiState<List<ChatThread>>> = deps.chatRepository.observeChats(currentUserId)
        .map<List<ChatThread>, UiState<List<ChatThread>>> { UiState.Success(it) }
        .catch { err ->
            val root = unwrapFirestoreError(err)
            handleFirestoreError(root, OperationType.LIST, "chats")
            emit(UiState.Error(root.localizedMessage ?: "Failed to load conversations."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
}

@Composable
fun InboxScreen(
    viewModel: InboxViewModel,
    currentUser: PrivateUser?,
    publicProfiles: List<PublicProfile>,
    deps: ApnaDependencies,
    onOpenChat: (chatId: String, otherUserId: String, otherUserName: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val state by viewModel.chatsState.collectAsStateWithLifecycle()
    var search by remember { mutableStateOf("") }
    var showNewChatDialog by remember { mutableStateOf(false) }

    val uid = currentUser?.userId.orEmpty()
    val blocked = currentUser?.blockedUserIds.orEmpty().toSet()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChatDialog = true },
                modifier = Modifier.testTag("inbox_new_chat_fab")
            ) {
                Icon(Icons.Default.AddComment, contentDescription = "Start new chat")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search conversations...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inbox_search_input")
            )
            Spacer(Modifier.height(10.dp))
            SafetyNoticeBanner(
                text = "Private In-App Messaging: ApnaHub never reads your phone SMS inbox or contacts."
            )
            Spacer(Modifier.height(10.dp))

            when (val ui = state) {
                is UiState.Loading -> SkeletonLoadingList(3)
                is UiState.Error -> ApnaErrorState(ui.message)
                is UiState.Success -> {
                    val q = search.trim().lowercase()
                    val filtered = ui.data.filter { chat ->
                        val otherId = chat.participantIds.firstOrNull { it != uid }.orEmpty()
                        val otherName = chat.participantNames[otherId] ?: "Member"
                        otherId !in blocked && (q.isBlank() || otherName.lowercase().contains(q) || chat.lastMessage.lowercase().contains(q))
                    }
                    if (filtered.isEmpty()) {
                        ApnaEmptyState(
                            title = "No conversations yet",
                            subtitle = "Tap the message icon below to start a 1-to-1 chat with another ApnaHub member.",
                            actionLabel = "New Chat",
                            onAction = { showNewChatDialog = true }
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.id }) { chat ->
                                val otherId = chat.participantIds.firstOrNull { it != uid }.orEmpty()
                                val otherName = chat.participantNames[otherId] ?: "Member"
                                val isUnread = uid in chat.unreadBy
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenChat(chat.id, otherId, otherName) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        UserAvatar(name = otherName, photoUrl = null, size = 44.dp)
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(otherName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = chat.lastMessage.ifBlank { "Tap to send a message" },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (isUnread) {
                                            Badge { Text("New") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        val others = publicProfiles.filter { it.userId != uid && it.userId !in blocked }
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text("Start New Conversation") },
            text = {
                if (others.isEmpty()) {
                    Text("No other members found yet.")
                } else {
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(others, key = { it.userId }) { profile ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showNewChatDialog = false
                                        scope.launch {
                                            deps.chatRepository.getOrCreateChat(
                                                otherUserId = profile.userId,
                                                currentUserName = currentUser?.displayName ?: "You",
                                                otherUserName = profile.displayName
                                            ).onSuccess { thread ->
                                                onOpenChat(thread.id, profile.userId, profile.displayName)
                                            }
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(name = profile.displayName, photoUrl = profile.photoUrl, size = 36.dp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(profile.displayName, style = MaterialTheme.typography.titleSmall)
                                    Text("@${profile.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNewChatDialog = false }) { Text("Close") }
            }
        )
    }
}
