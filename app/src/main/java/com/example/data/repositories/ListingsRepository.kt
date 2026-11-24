package com.example.data.repositories

import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.data.models.AppAnnouncement
import com.example.data.models.EducationResource
import com.example.data.models.JobListing
import com.example.data.models.MarketplaceListing
import com.example.data.models.ReportItem
import com.example.data.models.ServiceListing
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

class ListingsRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing listings.")

    fun observeJobs(): Flow<List<JobListing>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("jobs")
                .whereEqualTo("visibility", "active")
                .limit(40)
                .snapshots()
                .map { snap ->
                    snap.toObjects(JobListing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (auth.currentUser?.uid == uid) handleFirestoreError(error, OperationType.LIST, "jobs")
                    throw error
                }
        )
    }

    fun observeMarketplace(): Flow<List<MarketplaceListing>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("marketplace_listings")
                .whereEqualTo("visibility", "active")
                .limit(40)
                .snapshots()
                .map { snap ->
                    snap.toObjects(MarketplaceListing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (auth.currentUser?.uid == uid) handleFirestoreError(error, OperationType.LIST, "marketplace_listings")
                    throw error
                }
        )
    }

    fun observeServices(): Flow<List<ServiceListing>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("service_listings")
                .whereEqualTo("visibility", "active")
                .limit(40)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ServiceListing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (auth.currentUser?.uid == uid) handleFirestoreError(error, OperationType.LIST, "service_listings")
                    throw error
                }
        )
    }

    fun observeEducationResources(): Flow<List<EducationResource>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("education_resources")
                .whereEqualTo("visibility", "active")
                .limit(40)
                .snapshots()
                .map { snap ->
                    snap.toObjects(EducationResource::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (auth.currentUser?.uid == uid) handleFirestoreError(error, OperationType.LIST, "education_resources")
                    throw error
                }
        )
    }

    fun observeAnnouncements(): Flow<List<AppAnnouncement>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("app_announcements")
                .whereEqualTo("visibility", "active")
                .limit(20)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AppAnnouncement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (auth.currentUser?.uid == uid) handleFirestoreError(error, OperationType.LIST, "app_announcements")
                    throw error
                }
        )
    }

    suspend fun createMarketplaceListing(
        sellerName: String,
        sellerPhotoUrl: String?,
        title: String,
        description: String,
        category: String,
        pricePkr: Long,
        city: String,
        condition: String,
        imageUrl: String?
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanTitle = title.trim().take(120)
        val cleanDesc = description.trim().take(1500)
        if (cleanTitle.length < 2 || cleanDesc.length < 2) {
            return Result.failure(IllegalArgumentException("Please enter a valid title and description."))
        }
        val docRef = db.collection("marketplace_listings").document()
        return try {
            val payload = mutableMapOf<String, Any?>(
                "sellerId" to uid,
                "sellerName" to sellerName.take(80).ifBlank { "Seller" },
                "sellerPhotoUrl" to sellerPhotoUrl?.takeIf { it.isNotBlank() }?.take(249000),
                "title" to cleanTitle,
                "description" to cleanDesc,
                "category" to category.take(60),
                "pricePkr" to pricePkr.coerceIn(0L, 100_000_000L),
                "city" to city.take(60),
                "condition" to condition,
                "imageUrl" to imageUrl?.takeIf { it.isNotBlank() }?.take(249000),
                "visibility" to "active",
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

    suspend fun createServiceListing(
        providerName: String,
        providerPhotoUrl: String?,
        title: String,
        category: String,
        city: String,
        description: String,
        priceEstimate: String
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanTitle = title.trim().take(120)
        val cleanDesc = description.trim().take(1500)
        if (cleanTitle.length < 2 || cleanDesc.length < 2) {
            return Result.failure(IllegalArgumentException("Please enter a valid service title and description."))
        }
        val docRef = db.collection("service_listings").document()
        return try {
            val payload = mutableMapOf<String, Any?>(
                "providerId" to uid,
                "providerName" to providerName.take(80).ifBlank { "Provider" },
                "providerPhotoUrl" to providerPhotoUrl?.takeIf { it.isNotBlank() }?.take(249000),
                "title" to cleanTitle,
                "category" to category.take(60),
                "city" to city.take(60),
                "description" to cleanDesc,
                "priceEstimate" to priceEstimate.trim().take(80).ifBlank { "Contact for quote" },
                "visibility" to "active",
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

    // Admin-only writes (enforced by firestore.rules isAdmin())
    suspend fun adminPublishJob(
        title: String,
        company: String,
        city: String,
        category: String,
        qualification: String,
        salaryRange: String,
        description: String,
        applicationInstructions: String,
        sampleListing: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("jobs").document()
        return try {
            docRef.set(
                mapOf(
                    "publisherId" to uid,
                    "title" to title.trim().take(140),
                    "company" to company.trim().take(100),
                    "city" to city.trim().take(60),
                    "category" to category,
                    "qualification" to qualification.trim().take(100),
                    "salaryRange" to salaryRange.trim().take(80),
                    "description" to description.trim().take(2000),
                    "applicationInstructions" to applicationInstructions.trim().take(1000),
                    "sampleListing" to sampleListing,
                    "reviewStatus" to if (sampleListing) "sample_demo" else "admin_reviewed",
                    "visibility" to "active",
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

    suspend fun adminPublishAnnouncement(title: String, body: String, category: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("app_announcements").document()
        return try {
            docRef.set(
                mapOf(
                    "publisherId" to uid,
                    "title" to title.trim().take(140),
                    "body" to body.trim().take(2000),
                    "category" to category.take(60),
                    "visibility" to "active",
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

    suspend fun adminFetchReports(): Result<List<ReportItem>> {
        return try {
            requireUserId()
            val snap = db.collection("reports").limit(50).get().await()
            val items = snap.toObjects(ReportItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            Result.success(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "reports")
            Result.failure(e)
        }
    }

    suspend fun adminResolveReport(reportId: String, status: String): Result<Unit> {
        val docRef = db.collection("reports").document(reportId)
        return try {
            requireUserId()
            docRef.update(
                mapOf(
                    "status" to status,
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

    suspend fun adminRemoveTargetContent(targetType: String, targetId: String): Result<Unit> {
        val collection = when (targetType) {
            "post" -> "posts"
            "video" -> "videos"
            "marketplace" -> "marketplace_listings"
            "service" -> "service_listings"
            else -> return Result.failure(IllegalArgumentException("Unsupported target type"))
        }
        val docRef = db.collection(collection).document(targetId)
        return try {
            requireUserId()
            docRef.update(
                mapOf(
                    "visibility" to "removed",
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
}
