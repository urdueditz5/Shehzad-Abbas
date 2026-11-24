package com.example.data.models

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class PrivateUser(
    val userId: String = "",
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val city: String? = null,
    val bio: String? = null,
    val language: String = "en",
    val themeMode: String = "system",
    val notificationsEnabled: Boolean = true,
    val profileCompleted: Boolean = false,
    val blockedUserIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PublicProfile(
    val userId: String = "",
    val displayName: String = "",
    val username: String = "",
    val photoUrl: String? = null,
    val city: String? = null,
    val bio: String? = null,
    val visibility: String = "public",
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val postsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostItem(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorPhotoUrl: String? = null,
    val caption: String = "",
    val imageUrl: String? = null,
    val category: String = "Community",
    val visibility: String = "public",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommentItem(
    @DocumentId val id: String = "",
    val parentId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String? = null,
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ShortVideo(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val creatorUsername: String = "",
    val creatorPhotoUrl: String? = null,
    val title: String = "",
    val description: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String? = null,
    val durationSec: Long = 30L,
    val visibility: String = "public",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val viewsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FollowRecord(
    @DocumentId val id: String = "",
    val followerId: String = "",
    val targetUserId: String = "",
    val targetName: String = "",
    val targetPhotoUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatThread(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val unreadBy: List<String> = emptyList(),
    val hiddenFor: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val participantIds: List<String> = emptyList(),
    val status: String = "sent",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class JobListing(
    @DocumentId val id: String = "",
    val publisherId: String = "",
    val title: String = "",
    val company: String = "",
    val city: String = "",
    val category: String = "Private",
    val qualification: String = "Bachelor",
    val salaryRange: String = "",
    val description: String = "",
    val applicationInstructions: String = "",
    val sampleListing: Boolean = true,
    val reviewStatus: String = "sample_demo",
    val visibility: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MarketplaceListing(
    @DocumentId val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerPhotoUrl: String? = null,
    val title: String = "",
    val description: String = "",
    val category: String = "Electronics",
    val pricePkr: Long = 0L,
    val city: String = "Lahore",
    val condition: String = "Used",
    val imageUrl: String? = null,
    val visibility: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ServiceListing(
    @DocumentId val id: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val providerPhotoUrl: String? = null,
    val title: String = "",
    val category: String = "Education and tutoring",
    val city: String = "Lahore",
    val description: String = "",
    val priceEstimate: String = "",
    val visibility: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class EducationResource(
    @DocumentId val id: String = "",
    val title: String = "",
    val category: String = "Matric",
    val subject: String = "",
    val description: String = "",
    val content: String = "",
    val sampleContent: Boolean = true,
    val visibility: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppAnnouncement(
    @DocumentId val id: String = "",
    val publisherId: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "General",
    val visibility: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val itemType: String = "post",
    val itemId: String = "",
    val title: String = "",
    val subtitle: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val category: String = "announcements",
    val title: String = "",
    val body: String = "",
    val readStatus: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserTodo(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val reminderLabel: String = "",
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReportItem(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "post",
    val targetId: String = "",
    val reason: String = "",
    val status: String = "open",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
