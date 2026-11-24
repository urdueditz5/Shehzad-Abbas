package com.example.core.navigation

import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object ExploreRoute
@Serializable data object CreateRoute
@Serializable data object InboxRoute
@Serializable data object ProfileRoute

@Serializable data object SocialFeedRoute
@Serializable data object ShortVideosRoute
@Serializable data object EducationRoute
@Serializable data object JobsRoute
@Serializable data object MarketplaceRoute
@Serializable data object LocalServicesRoute
@Serializable data object NewsRoute
@Serializable data object ToolsRoute
@Serializable data object AiAssistantRoute
@Serializable data object NotificationsRoute
@Serializable data object SettingsRoute
@Serializable data object EditProfileRoute
@Serializable data object AdminRoute

@Serializable data class LegalRoute(val isPrivacy: Boolean)
@Serializable data class ChatDetailRoute(
    val chatId: String,
    val otherUserId: String,
    val otherUserName: String
)
@Serializable data class PublicProfileRoute(val userId: String)
