package com.example.data.repositories

import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.data.models.CommentItem
import com.example.data.models.ShortVideo
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

class VideoRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing videos.")

    fun observeVideos(limitCount: Long = 20L): Flow<List<ShortVideo>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("videos")
                .whereEqualTo("visibility", "public")
                .limit(limitCount)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ShortVideo::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(error, OperationType.LIST, "videos")
                    }
                    throw error
                }
        )
    }

    suspend fun publishShortVideo(
        creatorName: String,
        creatorUsername: String,
        creatorPhotoUrl: String?,
        title: String,
        description: String,
        videoUrl: String,
        durationSec: Long
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanTitle = title.trim().take(140)
        val cleanUrl = videoUrl.trim().take(1000)
        if (cleanTitle.isBlank()) return Result.failure(IllegalArgumentException("Title is required."))
        if (!cleanUrl.startsWith("https://")) {
            return Result.failure(IllegalArgumentException("Enter a valid HTTPS video URL (or connect Firebase Storage for raw file uploads)."))
        }

        val docRef = db.collection("videos").document()
        return try {
            val payload = mutableMapOf<String, Any?>(
                "creatorId" to uid,
                "creatorName" to creatorName.take(80).ifBlank { "Creator" },
                "creatorUsername" to creatorUsername.take(40).ifBlank { "creator" },
                "creatorPhotoUrl" to creatorPhotoUrl?.takeIf { it.isNotBlank() }?.take(249000),
                "title" to cleanTitle,
                "description" to description.trim().take(600),
                "videoUrl" to cleanUrl,
                "durationSec" to durationSec.coerceIn(1L, 180L),
                "visibility" to "public",
                "likesCount" to 0L,
                "commentsCount" to 0L,
                "viewsCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleLikeVideo(videoId: String): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val videoRef = db.collection("videos").document(videoId)
        val likeRef = videoRef.collection("likes").document(uid)
        return try {
            val snap = likeRef.get().await()
            val batch = db.batch()
            val liked = if (snap.exists()) {
                batch.delete(likeRef)
                batch.update(videoRef, mapOf("likesCount" to FieldValue.increment(-1), "updatedAt" to FieldValue.serverTimestamp()))
                false
            } else {
                batch.set(
                    likeRef,
                    mapOf(
                        "parentId" to videoId,
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(videoRef, mapOf("likesCount" to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()))
                true
            }
            batch.commit().await()
            Result.success(liked)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, videoRef.path)
            Result.failure(e)
        }
    }

    suspend fun incrementView(videoId: String) {
        try {
            requireUserId()
            db.collection("videos").document(videoId).update(
                mapOf(
                    "viewsCount" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "videos/$videoId")
        }
    }

    fun observeVideoComments(videoId: String): Flow<List<CommentItem>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("videos").document(videoId).collection("comments")
                .whereEqualTo("parentId", videoId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CommentItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(error, OperationType.LIST, "videos/$videoId/comments")
                    }
                    throw error
                }
        )
    }

    suspend fun addVideoComment(
        videoId: String,
        authorName: String,
        authorPhotoUrl: String?,
        text: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanText = text.trim().take(500)
        if (cleanText.isBlank()) return Result.failure(IllegalArgumentException("Comment cannot be empty."))
        val videoRef = db.collection("videos").document(videoId)
        val commentRef = videoRef.collection("comments").document()
        return try {
            val payload = mutableMapOf<String, Any?>(
                "parentId" to videoId,
                "authorId" to uid,
                "authorName" to authorName.take(80).ifBlank { "User" },
                "authorPhotoUrl" to authorPhotoUrl?.takeIf { it.isNotBlank() }?.take(249000),
                "text" to cleanText,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            val batch = db.batch()
            batch.set(commentRef, payload)
            batch.update(
                videoRef,
                mapOf(
                    "commentsCount" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentRef.path)
            Result.failure(e)
        }
    }
}
