package com.example.data.repositories

import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.data.models.CommentItem
import com.example.data.models.FollowRecord
import com.example.data.models.PostItem
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

interface SocialFeedRepository {
    fun observePosts(limitCount: Long = 20L): Flow<List<PostItem>>
    fun observeFollowing(uid: String): Flow<List<FollowRecord>>
    fun observeComments(postId: String): Flow<List<CommentItem>>
    suspend fun hasLikedPost(postId: String): Boolean
    suspend fun toggleLikePost(postId: String): Result<Boolean>
    suspend fun createPost(
        authorName: String,
        authorUsername: String,
        authorPhotoUrl: String?,
        caption: String,
        category: String,
        imageUrl: String?
    ): Result<String>
    suspend fun deletePost(postId: String): Result<Unit>
    suspend fun addComment(
        postId: String,
        authorName: String,
        authorPhotoUrl: String?,
        text: String
    ): Result<Unit>
    suspend fun toggleFollow(
        targetUserId: String,
        targetName: String,
        targetPhotoUrl: String?
    ): Result<Boolean>
    suspend fun submitReport(targetType: String, targetId: String, reason: String): Result<Unit>
}

class SocialRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : SocialFeedRepository {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing posts.")

    override fun observePosts(limitCount: Long): Flow<List<PostItem>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("posts")
                .whereEqualTo("visibility", "public")
                .limit(limitCount)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PostItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(error, OperationType.LIST, "posts")
                    }
                    throw error
                }
        )
    }

    override suspend fun createPost(
        authorName: String,
        authorUsername: String,
        authorPhotoUrl: String?,
        caption: String,
        category: String,
        imageUrl: String?
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanCaption = caption.trim().take(2000)
        if (cleanCaption.isBlank()) return Result.failure(IllegalArgumentException("Post caption cannot be empty."))
        val docRef = db.collection("posts").document()
        return try {
            val payload = mutableMapOf<String, Any?>(
                "authorId" to uid,
                "authorName" to authorName.take(80).ifBlank { "ApnaHub User" },
                "authorUsername" to authorUsername.take(40).ifBlank { "user" },
                "authorPhotoUrl" to authorPhotoUrl?.takeIf { it.isNotBlank() }?.take(249000),
                "caption" to cleanCaption,
                "imageUrl" to imageUrl?.takeIf { it.isNotBlank() }?.take(249000),
                "category" to category.take(40).ifBlank { "Community" },
                "visibility" to "public",
                "likesCount" to 0L,
                "commentsCount" to 0L,
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

    override suspend fun deletePost(postId: String): Result<Unit> {
        val docRef = db.collection("posts").document(postId)
        return try {
            requireUserId()
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun toggleLikePost(postId: String): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val postRef = db.collection("posts").document(postId)
        val likeRef = postRef.collection("likes").document(uid)
        return try {
            val likeSnap = likeRef.get().await()
            val batch = db.batch()
            val likedNow = if (likeSnap.exists()) {
                batch.delete(likeRef)
                batch.update(postRef, mapOf("likesCount" to FieldValue.increment(-1), "updatedAt" to FieldValue.serverTimestamp()))
                false
            } else {
                batch.set(
                    likeRef,
                    mapOf(
                        "parentId" to postId,
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(postRef, mapOf("likesCount" to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()))
                true
            }
            batch.commit().await()
            Result.success(likedNow)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, postRef.path)
            Result.failure(e)
        }
    }

    override suspend fun hasLikedPost(postId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            db.collection("posts").document(postId).collection("likes").document(uid).get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    override fun observeComments(postId: String): Flow<List<CommentItem>> = flow {
        val uid = requireUserId()
        val path = "posts/$postId/comments"
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("parentId", postId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CommentItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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

    override suspend fun addComment(
        postId: String,
        authorName: String,
        authorPhotoUrl: String?,
        text: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanText = text.trim().take(500)
        if (cleanText.isBlank()) return Result.failure(IllegalArgumentException("Comment cannot be empty."))
        val postRef = db.collection("posts").document(postId)
        val commentRef = postRef.collection("comments").document()
        return try {
            val payload = mutableMapOf<String, Any?>(
                "parentId" to postId,
                "authorId" to uid,
                "authorName" to authorName.take(80).ifBlank { "User" },
                "authorPhotoUrl" to authorPhotoUrl?.takeIf { it.isNotBlank() }?.take(249000),
                "text" to cleanText,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            val batch = db.batch()
            batch.set(commentRef, payload)
            batch.update(
                postRef,
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

    override fun observeFollowing(uid: String): Flow<List<FollowRecord>> = flow {
        if (uid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("follows")
                .whereEqualTo("followerId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FollowRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(error, OperationType.LIST, "follows")
                    }
                    throw error
                }
        )
    }

    override suspend fun toggleFollow(
        targetUserId: String,
        targetName: String,
        targetPhotoUrl: String?
    ): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (targetUserId == uid || targetUserId.isBlank()) {
            return Result.failure(IllegalArgumentException("Cannot follow yourself"))
        }
        val followId = "${uid}_${targetUserId}"
        val followRef = db.collection("follows").document(followId)
        val targetProfileRef = db.collection("public_profiles").document(targetUserId)
        return try {
            val existing = followRef.get().await()
            val batch = db.batch()
            val nowFollowing = if (existing.exists()) {
                batch.delete(followRef)
                batch.update(
                    targetProfileRef,
                    mapOf("followersCount" to FieldValue.increment(-1), "updatedAt" to FieldValue.serverTimestamp())
                )
                false
            } else {
                val payload = mutableMapOf<String, Any?>(
                    "followerId" to uid,
                    "targetUserId" to targetUserId,
                    "targetName" to targetName.take(80).ifBlank { "User" },
                    "targetPhotoUrl" to targetPhotoUrl?.takeIf { it.isNotBlank() }?.take(249000),
                    "createdAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                batch.set(followRef, payload)
                batch.update(
                    targetProfileRef,
                    mapOf("followersCount" to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp())
                )
                true
            }
            batch.commit().await()
            Result.success(nowFollowing)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, followRef.path)
            Result.failure(e)
        }
    }

    override suspend fun submitReport(targetType: String, targetId: String, reason: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanReason = reason.trim().take(500)
        if (cleanReason.length < 2) {
            return Result.failure(IllegalArgumentException("Please enter a reason for reporting."))
        }
        val reportRef = db.collection("reports").document()
        return try {
            reportRef.set(
                mapOf(
                    "reporterId" to uid,
                    "targetType" to targetType,
                    "targetId" to targetId.take(128),
                    "reason" to cleanReason,
                    "status" to "open",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, reportRef.path)
            Result.failure(e)
        }
    }
}
