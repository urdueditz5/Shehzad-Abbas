package com.example.data.repositories

import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.data.models.ChatMessage
import com.example.data.models.ChatThread
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

interface ChatMessagingRepository {
    fun observeMessages(chatId: String, uid: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(
        chatId: String,
        participantIds: List<String>,
        senderName: String,
        text: String,
        imageUrl: String? = null
    ): Result<Unit>
    suspend fun markChatRead(chatId: String, currentUnread: List<String>)
    suspend fun hideConversationLocally(chatId: String, currentHidden: List<String>): Result<Unit>
}

class ChatRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : ChatMessagingRepository {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing chats.")

    fun deterministicChatId(uid1: String, uid2: String): String =
        listOf(uid1, uid2).sorted().joinToString("_")

    fun observeChats(uid: String): Flow<List<ChatThread>> = flow {
        if (uid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("chats")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { uid !in it.hiddenFor }
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(error, OperationType.LIST, "chats")
                    }
                    throw error
                }
        )
    }

    override fun observeMessages(chatId: String, uid: String): Flow<List<ChatMessage>> = flow {
        if (uid.isBlank() || chatId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "chats/$chatId/messages"
        emitAll(
            db.collection("chats").document(chatId).collection("messages")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getOrCreateChat(
        otherUserId: String,
        currentUserName: String,
        otherUserName: String
    ): Result<ChatThread> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (otherUserId.isBlank() || otherUserId == uid) {
            return Result.failure(IllegalArgumentException("Cannot start a chat with yourself."))
        }
        val chatId = deterministicChatId(uid, otherUserId)
        val chatRef = db.collection("chats").document(chatId)
        return try {
            val snap = chatRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(ChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: ChatThread(id = chatId, participantIds = listOf(uid, otherUserId).sorted())
                if (uid in existing.hiddenFor) {
                    chatRef.update(
                        mapOf(
                            "hiddenFor" to (existing.hiddenFor - uid),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                Result.success(existing)
            } else {
                val sortedIds = listOf(uid, otherUserId).sorted()
                val namesMap = mapOf(
                    uid to currentUserName.take(60).ifBlank { "You" },
                    otherUserId to otherUserName.take(60).ifBlank { "Member" }
                )
                val payload = mapOf(
                    "participantIds" to sortedIds,
                    "participantNames" to namesMap,
                    "lastMessage" to "",
                    "lastSenderId" to "",
                    "unreadBy" to emptyList<String>(),
                    "hiddenFor" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                chatRef.set(payload).await()
                Result.success(
                    ChatThread(
                        id = chatId,
                        participantIds = sortedIds,
                        participantNames = namesMap
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, chatRef.path)
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(
        chatId: String,
        participantIds: List<String>,
        senderName: String,
        text: String,
        imageUrl: String?
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanText = text.trim().take(2000)
        if (cleanText.isBlank() && imageUrl.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Message cannot be empty."))
        }
        val otherIds = participantIds.filter { it != uid }
        val chatRef = db.collection("chats").document(chatId)
        val msgRef = chatRef.collection("messages").document()
        return try {
            val msgPayload = mutableMapOf<String, Any?>(
                "chatId" to chatId,
                "senderId" to uid,
                "senderName" to senderName.take(80).ifBlank { "User" },
                "text" to cleanText,
                "imageUrl" to imageUrl?.takeIf { it.isNotBlank() }?.take(249000),
                "participantIds" to participantIds,
                "status" to "sent",
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            val preview = if (cleanText.isNotBlank()) cleanText.take(120) else "[Photo]"
            val batch = db.batch()
            batch.set(msgRef, msgPayload)
            batch.update(
                chatRef,
                mapOf(
                    "lastMessage" to preview,
                    "lastSenderId" to uid,
                    "unreadBy" to otherIds,
                    "hiddenFor" to emptyList<String>(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            Result.failure(e)
        }
    }

    override suspend fun markChatRead(chatId: String, currentUnread: List<String>) {
        val uid = auth.currentUser?.uid ?: return
        if (uid !in currentUnread) return
        try {
            db.collection("chats").document(chatId).update(
                mapOf(
                    "unreadBy" to (currentUnread - uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId")
        }
    }

    override suspend fun hideConversationLocally(chatId: String, currentHidden: List<String>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("chats").document(chatId).update(
                mapOf(
                    "hiddenFor" to (currentHidden + uid).distinct().take(2),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId")
            Result.failure(e)
        }
    }
}
