package com.example.data.repositories

import com.example.core.constants.PakistanConstants
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.data.models.AppNotification
import com.example.data.models.PrivateUser
import com.example.data.models.PublicProfile
import com.example.data.models.SavedItem
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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

interface UserProfileRepository {
    fun observeCurrentUser(uid: String): Flow<PrivateUser?>
    suspend fun updateProfile(
        displayName: String,
        username: String,
        city: String,
        bio: String,
        photoUrl: String?,
        markCompleted: Boolean = true
    ): Result<Unit>
}

class UserRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : UserProfileRepository {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    suspend fun ensureUserBootstrapped(firebaseUser: FirebaseUser): Result<PrivateUser> {
        val uid = firebaseUser.uid
        val userPath = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val existing = snapshot.toObject(
                    PrivateUser::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: PrivateUser(userId = uid)
                Result.success(existing)
            } else {
                val baseName = firebaseUser.displayName?.take(80)?.takeIf { it.isNotBlank() } ?: "ApnaHub User"
                val email = firebaseUser.email.orEmpty().take(200)
                val baseHandle = (email.substringBefore("@")
                    .ifBlank { baseName }
                    .lowercase()
                    .replace(Regex("[^a-z0-9_]"), "")
                    .take(20)
                    .ifBlank { "user" }) + "_" + uid.take(4).lowercase()
                val photo = firebaseUser.photoUrl?.toString()?.take(2000)

                val privatePayload = mutableMapOf<String, Any?>(
                    "userId" to uid,
                    "displayName" to baseName,
                    "username" to baseHandle,
                    "email" to email,
                    "photoUrl" to photo,
                    "city" to "Lahore",
                    "bio" to "",
                    "language" to "en",
                    "themeMode" to "system",
                    "notificationsEnabled" to true,
                    "profileCompleted" to false,
                    "blockedUserIds" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>

                val publicPayload = mutableMapOf<String, Any?>(
                    "userId" to uid,
                    "displayName" to baseName,
                    "username" to baseHandle,
                    "photoUrl" to photo,
                    "city" to "Lahore",
                    "bio" to "",
                    "visibility" to "public",
                    "followersCount" to 0L,
                    "followingCount" to 0L,
                    "postsCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>

                val batch = db.batch()
                batch.set(docRef, privatePayload)
                batch.set(db.collection("public_profiles").document(uid), publicPayload)
                batch.commit().await()

                val created = PrivateUser(
                    userId = uid,
                    displayName = baseName,
                    username = baseHandle,
                    email = email,
                    photoUrl = photo,
                    city = "Lahore",
                    bio = "",
                    profileCompleted = false
                )
                Result.success(created)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userPath)
            Result.failure(e)
        }
    }

    override fun observeCurrentUser(uid: String): Flow<PrivateUser?> = flow {
        if (uid.isBlank()) {
            emit(null)
            return@flow
        }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(PrivateUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observePublicProfiles(): Flow<List<PublicProfile>> = flow {
        val uid = requireUserId()
        val path = "public_profiles"
        emitAll(
            db.collection("public_profiles")
                .whereEqualTo("visibility", "public")
                .limit(50)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    override suspend fun updateProfile(
        displayName: String,
        username: String,
        city: String,
        bio: String,
        photoUrl: String?,
        markCompleted: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanName = displayName.trim().take(80)
        val cleanUsername = username.trim().lowercase().replace(Regex("[^a-z0-9_.]"), "").take(40)
        if (cleanName.isBlank()) return Result.failure(IllegalArgumentException("Display name is required."))
        if (cleanUsername.length < 2) return Result.failure(IllegalArgumentException("Username must be at least 2 characters."))

        val cleanCity = city.trim().take(60)
        val cleanBio = bio.trim().take(240)
        val cleanPhoto = photoUrl?.takeIf { it.isNotBlank() }?.take(249000)

        return try {
            val photoFieldValue: Any = cleanPhoto ?: FieldValue.delete()
            val privateUpdates = mapOf<String, Any>(
                "displayName" to cleanName,
                "username" to cleanUsername,
                "city" to cleanCity,
                "bio" to cleanBio,
                "photoUrl" to photoFieldValue,
                "profileCompleted" to markCompleted,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val publicUpdates = mapOf<String, Any>(
                "displayName" to cleanName,
                "username" to cleanUsername,
                "city" to cleanCity,
                "bio" to cleanBio,
                "photoUrl" to photoFieldValue,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.update(db.collection("users").document(uid), privateUpdates)
            batch.update(db.collection("public_profiles").document(uid), publicUpdates)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun syncUserPreferences(
        languageCode: String? = null,
        themeModeCode: String? = null,
        notificationsEnabled: Boolean? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val updates = mutableMapOf<String, Any?>(
            "language" to languageCode,
            "themeMode" to themeModeCode,
            "notificationsEnabled" to notificationsEnabled,
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
        return try {
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun toggleBlockUser(targetUserId: String, currentlyBlocked: List<String>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (targetUserId == uid || targetUserId.isBlank()) {
            return Result.failure(IllegalArgumentException("Invalid target user"))
        }
        val updated = if (targetUserId in currentlyBlocked) {
            currentlyBlocked - targetUserId
        } else {
            (currentlyBlocked + targetUserId).distinct().take(100)
        }
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "blockedUserIds" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    fun observeSavedItems(uid: String): Flow<List<SavedItem>> = flow {
        if (uid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$uid/saved_items"
        emitAll(
            db.collection("users").document(uid).collection("saved_items")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SavedItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun toggleSaveItem(
        itemType: String,
        itemId: String,
        title: String,
        subtitle: String
    ): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docId = "${itemType}_${itemId}".take(128).replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val docRef = db.collection("users").document(uid).collection("saved_items").document(docId)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.delete().await()
                Result.success(false)
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "itemType" to itemType,
                        "itemId" to itemId.take(128),
                        "title" to title.take(160).ifBlank { "Saved Item" },
                        "subtitle" to subtitle.take(240),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeNotifications(uid: String): Flow<List<AppNotification>> = flow {
        if (uid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$uid/notifications"
        emitAll(
            db.collection("users").document(uid).collection("notifications")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun sendNotification(
        targetUserId: String,
        actorName: String,
        category: String,
        title: String,
        body: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("users").document(targetUserId).collection("notifications").document()
        return try {
            docRef.set(
                mapOf(
                    "userId" to targetUserId,
                    "actorId" to uid,
                    "actorName" to actorName.take(80),
                    "category" to category,
                    "title" to title.take(140),
                    "body" to body.take(400),
                    "readStatus" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("users").document(uid).collection("notifications").document(notificationId)
        return try {
            docRef.update(
                mapOf(
                    "readStatus" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun isUserAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        if (user.email == PakistanConstants.ADMIN_BOOTSTRAP_EMAIL && user.isEmailVerified) {
            return true
        }
        return try {
            db.collection("admins").document(user.uid).get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteAccountDataAndSignOut(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val uid = user.uid
        return try {
            val batch = db.batch()
            batch.delete(db.collection("users").document(uid))
            batch.delete(db.collection("public_profiles").document(uid))
            batch.commit().await()
            user.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }
}
