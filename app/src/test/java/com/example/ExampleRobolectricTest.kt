package com.example

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.core.preferences.AppLanguage
import com.example.core.preferences.ThemeMode
import com.example.core.theme.ApnaHubTheme
import com.example.data.models.ChatMessage
import com.example.data.models.CommentItem
import com.example.data.models.FollowRecord
import com.example.data.models.PostItem
import com.example.data.models.PrivateUser
import com.example.data.repositories.AiAssistantRepository
import com.example.data.repositories.AiCitation
import com.example.data.repositories.AiCitationType
import com.example.data.repositories.AiGroundingMode
import com.example.data.repositories.AiImageAspectRatio
import com.example.data.repositories.AiLiveSessionHandle
import com.example.data.repositories.AiModelTier
import com.example.data.repositories.AiPersona
import com.example.data.repositories.AiStreamChunk
import com.example.data.repositories.ChatMessagingRepository
import com.example.data.repositories.LiveVoiceCallbacks
import com.example.data.repositories.SocialFeedRepository
import com.example.data.repositories.UserProfileRepository
import com.example.features.ai.AiAssistantScreen
import com.example.features.ai.AiViewModel
import com.example.features.ai.ImageGenUiState
import com.example.features.ai.LiveVoiceUiState
import com.example.features.ai.TranscriptionUiState
import com.example.features.chat.ChatScreen
import com.example.features.chat.ChatViewModel
import com.example.features.home.HomeDashboardScreen
import com.example.features.profile.ProfileScreen
import com.example.features.profile.UserProfileViewModel
import com.example.features.social.SocialFeedScreen
import com.example.features.social.SocialViewModel
import com.example.features.tools.QrHelper
import java.io.ByteArrayOutputStream
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `app resources declare ApnaHub and named firestore database id`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("ApnaHub", context.getString(R.string.app_name))
        assertTrue(context.getString(R.string.firestore_database_id).isNotBlank())
    }

    @Test
    fun `qr helper generates and decodes bitmap locally`() {
        val payload = "https://apnahub.example/tool-check"
        val bitmap = QrHelper.generateQrBitmap(payload, sizePx = 256)
        assertNotNull(bitmap)
        val decoded = QrHelper.decodeQrFromBitmap(bitmap!!).getOrThrow()
        assertEquals(payload, decoded)
    }

    @Test
    fun `home dashboard renders header, search bar, and lazy grid of service cards`() {
        var aiOpened = false
        composeRule.setContent {
            ApnaHubTheme(themeMode = ThemeMode.LIGHT, language = AppLanguage.ENGLISH) {
                HomeDashboardScreen(
                    user = PrivateUser(
                        userId = "u1",
                        displayName = "Ali Raza",
                        username = "ali_pk",
                        city = "Lahore",
                        profileCompleted = true
                    ),
                    announcements = emptyList(),
                    notifications = emptyList(),
                    savedItems = emptyList(),
                    recentPosts = emptyList(),
                    recentVideos = emptyList(),
                    onSearchClick = {},
                    onNotificationsClick = {},
                    onOpenSocial = {},
                    onOpenVideos = {},
                    onOpenMessaging = {},
                    onOpenEducation = {},
                    onOpenJobs = {},
                    onOpenMarketplace = {},
                    onOpenLocalServices = {},
                    onOpenNews = {},
                    onOpenTools = {},
                    onOpenAiAssistant = { aiOpened = true }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_category_chip_row").assertIsDisplayed()
        composeRule.onNodeWithTag("home_header_card").assertIsDisplayed()
        composeRule.onNodeWithTag("home_search_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("home_dashboard_grid").performScrollToNode(hasTestTag("service_card_social"))
        composeRule.onNodeWithTag("service_card_social").assertIsDisplayed()
        composeRule.onNodeWithTag("home_dashboard_grid").performScrollToNode(hasTestTag("service_card_ai"))
        composeRule.onNodeWithTag("service_card_ai").performClick()
        composeRule.waitForIdle()
        assertTrue(aiOpened)
    }

    @Test
    fun `home dashboard category chip row filters visible service cards`() {
        composeRule.setContent {
            ApnaHubTheme(themeMode = ThemeMode.LIGHT, language = AppLanguage.ENGLISH) {
                HomeDashboardScreen(
                    user = PrivateUser(
                        userId = "u1",
                        displayName = "Ali Raza",
                        username = "ali_pk",
                        city = "Lahore",
                        profileCompleted = true
                    ),
                    announcements = emptyList(),
                    notifications = emptyList(),
                    savedItems = emptyList(),
                    recentPosts = emptyList(),
                    recentVideos = emptyList(),
                    onSearchClick = {},
                    onNotificationsClick = {},
                    onOpenSocial = {},
                    onOpenVideos = {},
                    onOpenMessaging = {},
                    onOpenEducation = {},
                    onOpenJobs = {},
                    onOpenMarketplace = {},
                    onOpenLocalServices = {},
                    onOpenNews = {},
                    onOpenTools = {},
                    onOpenAiAssistant = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_category_chip_row").assertIsDisplayed()

        // Scroll horizontally to Tools & AI chip and select it
        composeRule.onNodeWithTag("home_category_chip_row").performScrollToNode(hasTestTag("home_cat_chip_tools_ai"))
        composeRule.onNodeWithTag("home_cat_chip_tools_ai").performClick()
        composeRule.waitForIdle()

        // Tools & AI cards remain visible; Social card is filtered out
        composeRule.onNodeWithTag("home_dashboard_grid").performScrollToNode(hasTestTag("service_card_ai"))
        composeRule.onNodeWithTag("service_card_ai").assertIsDisplayed()
        composeRule.onNodeWithTag("service_card_tools").assertIsDisplayed()
        composeRule.onNodeWithTag("service_card_social").assertDoesNotExist()

        // Scroll back to All chip and select it to restore all service cards
        composeRule.onNodeWithTag("home_category_chip_row").performScrollToNode(hasTestTag("home_cat_chip_all"))
        composeRule.onNodeWithTag("home_cat_chip_all").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_dashboard_grid").performScrollToNode(hasTestTag("service_card_social"))
        composeRule.onNodeWithTag("service_card_social").assertIsDisplayed()
    }

    @Test
    fun `user profile screen displays and edits display name, bio, and profile picture via ViewModel`() {
        val fakeRepo = FakeUserProfileRepository(
            PrivateUser(
                userId = "u1",
                displayName = "Ali Raza",
                username = "ali_pk",
                city = "Lahore",
                bio = "Android dev in Lahore",
                photoUrl = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
                profileCompleted = true
            )
        )
        val viewModel = UserProfileViewModel(userRepository = fakeRepo, currentUserId = "u1")

        composeRule.setContent {
            ApnaHubTheme(themeMode = ThemeMode.LIGHT, language = AppLanguage.ENGLISH) {
                ProfileScreen(
                    viewModel = viewModel,
                    myPosts = emptyList(),
                    savedItems = emptyList(),
                    following = emptyList(),
                    isAdmin = false,
                    onOpenSettings = {},
                    onOpenAdmin = {},
                    onOpenPrivacy = {},
                    onOpenTerms = {},
                    onOpenUserProfile = {}
                )
            }
        }

        composeRule.waitForIdle()
        // View mode renders existing name & bio
        composeRule.onNodeWithTag("profile_display_name").assertTextContains("Ali Raza")
        composeRule.onNodeWithTag("profile_bio_text").assertTextContains("Android dev in Lahore")

        // Enter edit mode
        composeRule.onNodeWithTag("profile_edit_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_edit_form").assertIsDisplayed()

        // Blank display name triggers validation error
        composeRule.onNodeWithTag("profile_edit_name_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("profile_save_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_edit_error").performScrollTo().assertIsDisplayed()

        // Update display name, bio, and profile photo via ViewModel
        composeRule.onNodeWithTag("profile_edit_name_input").performScrollTo().performTextInput("Ayesha Khan")
        composeRule.onNodeWithTag("profile_edit_bio_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("profile_edit_bio_input").performScrollTo().performTextInput("Building ApnaHub in Islamabad")
        viewModel.onPhotoDataUriSelected("data:image/jpeg;base64,UPDATED_PHOTO")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("profile_save_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Returned to view mode with updated values and success feedback
        assertFalse(viewModel.uiState.value.isEditing)
        assertEquals("Ayesha Khan", fakeRepo.userFlow.value?.displayName)
        assertEquals("Building ApnaHub in Islamabad", fakeRepo.userFlow.value?.bio)
        assertEquals("data:image/jpeg;base64,UPDATED_PHOTO", fakeRepo.userFlow.value?.photoUrl)
        composeRule.onNodeWithTag("profile_success_banner").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("profile_display_name").performScrollTo().assertTextContains("Ayesha Khan")
        composeRule.onNodeWithTag("profile_bio_text").performScrollTo().assertTextContains("Building ApnaHub in Islamabad")

        // Verify removing profile picture clears photoUrl
        composeRule.onNodeWithTag("profile_edit_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_remove_photo_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("profile_save_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertNull(fakeRepo.userFlow.value?.photoUrl)
    }

    @Test
    fun `chat screen displays real-time messages and sends new message via input field and send button`() {
        val fakeChatRepo = FakeChatMessagingRepository(
            initialMessages = listOf(
                ChatMessage(
                    id = "m1",
                    chatId = "u1_u2",
                    senderId = "u2",
                    senderName = "Bilal",
                    text = "Salam! How is the project going?",
                    participantIds = listOf("u1", "u2"),
                    status = "read",
                    createdAt = Timestamp.now()
                )
            )
        )
        val chatViewModel = ChatViewModel(
            chatRepository = fakeChatRepo,
            chatId = "u1_u2",
            currentUserId = "u1",
            otherUserId = "u2",
            currentUserNameProvider = { "Ali Raza" }
        )

        composeRule.setContent {
            ApnaHubTheme(themeMode = ThemeMode.LIGHT, language = AppLanguage.ENGLISH) {
                ChatScreen(
                    viewModel = chatViewModel,
                    currentUserId = "u1",
                    otherUserId = "u2",
                    otherUserName = "Bilal",
                    onBack = {},
                    onOpenProfile = {},
                    onReportUser = {},
                    onBlockUser = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat_messages_list").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_message_m1").assertIsDisplayed()
        composeRule.onNodeWithText("Salam! How is the project going?").assertIsDisplayed()

        // Send button disabled while input is blank
        composeRule.onNodeWithTag("chat_send_button").assertIsNotEnabled()

        // Type a message and send
        composeRule.onNodeWithTag("chat_message_input").performTextInput("Going great, almost done!")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat_send_button").assertIsEnabled()
        composeRule.onNodeWithTag("chat_send_button").performClick()
        composeRule.waitForIdle()

        // Draft cleared and sent message rendered in real-time list
        assertEquals("", chatViewModel.uiState.value.draftMessage)
        composeRule.onNodeWithText("Going great, almost done!").assertIsDisplayed()

        // Real-time incoming message emitted from repository appears immediately
        fakeChatRepo.pushIncomingMessage(
            ChatMessage(
                id = "m_incoming",
                chatId = "u1_u2",
                senderId = "u2",
                senderName = "Bilal",
                text = "Awesome, let's sync soon!",
                participantIds = listOf("u1", "u2"),
                status = "delivered",
                createdAt = Timestamp.now()
            )
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat_messages_list").performScrollToNode(hasTestTag("chat_message_m_incoming"))
        composeRule.onNodeWithText("Awesome, let's sync soon!").assertIsDisplayed()
    }

    @Test
    fun `ai assistant supports model tiers, search and maps grounding, image gen, and audio transcription`() {
        val fakeAi = FakeAiAssistantRepository()
        val aiViewModel = AiViewModel(fakeAi)

        composeRule.setContent {
            ApnaHubTheme(themeMode = ThemeMode.LIGHT, language = AppLanguage.ENGLISH) {
                AiAssistantScreen(
                    viewModel = aiViewModel,
                    onBack = {}
                )
            }
        }

        composeRule.waitForIdle()
        // Switch grounding to Google Maps and model tier to Reasoning
        composeRule.onNodeWithTag("ai_grounding_chip_google_maps").performClick()
        composeRule.onNodeWithTag("ai_grounding_row").performScrollToNode(hasTestTag("ai_model_chip_deep_reasoning"))
        composeRule.onNodeWithTag("ai_model_chip_deep_reasoning").performClick()
        composeRule.onNodeWithTag("ai_prompt_input").performTextInput("Cafes in Gulberg Lahore")
        composeRule.onNodeWithTag("ai_send_button").performClick()
        composeRule.waitForIdle()

        assertEquals(AiGroundingMode.GOOGLE_MAPS, fakeAi.lastGroundingMode)
        assertEquals(AiModelTier.DEEP_REASONING, fakeAi.lastModelTier)
        composeRule.onNodeWithText("Here are top cafes in Gulberg, Lahore.").assertIsDisplayed()
        composeRule.onNodeWithText("Gulberg Coffee House").assertIsDisplayed()

        // Switch to Images tab and generate an image
        composeRule.onNodeWithTag("ai_tab_image").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ai_aspect_chip_landscape").performScrollTo().performClick()
        composeRule.onNodeWithTag("ai_image_prompt_input").performScrollTo().performTextInput("Badshahi Mosque at sunset")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ai_image_generate_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertTrue(aiViewModel.imageGenState.value is ImageGenUiState.Success)
        assertEquals(AiImageAspectRatio.LANDSCAPE, fakeAi.lastAspectRatio)
        composeRule.onNodeWithTag("ai_image_result").performScrollTo().assertIsDisplayed()

        // Test audio transcription & send-to-chat workflow
        composeRule.onNodeWithTag("ai_tab_voice").performClick()
        composeRule.waitForIdle()
        aiViewModel.transcribeAudioBytes(byteArrayOf(1, 2, 3, 4))
        composeRule.waitForIdle()

        val txState = aiViewModel.transcriptionState.value
        assertTrue(txState is TranscriptionUiState.Success)
        composeRule.onNodeWithTag("ai_transcription_result").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Salam, please summarize FSc Physics chapter 2.").assertIsDisplayed()

        composeRule.onNodeWithTag("ai_transcription_send_to_chat").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ai_chat_list")
            .performScrollToNode(androidx.compose.ui.test.hasText("Salam, please summarize FSc Physics chapter 2."))
        composeRule.onNodeWithText("Salam, please summarize FSc Physics chapter 2.").assertIsDisplayed()

        // Test Live Voice session callbacks
        aiViewModel.startLiveVoiceConversation()
        composeRule.waitForIdle()
        assertTrue(aiViewModel.liveVoiceState.value is LiveVoiceUiState.Active)
        fakeAi.emitLiveTranscript("Hello Gemini", "Wa alaikum assalam! How can I help?")
        composeRule.waitForIdle()
        val activeLive = aiViewModel.liveVoiceState.value as LiveVoiceUiState.Active
        assertEquals(2, activeLive.turns.size)
        aiViewModel.stopLiveVoiceConversation()
        assertTrue(aiViewModel.liveVoiceState.value is LiveVoiceUiState.Idle)
    }

    @Test
    fun `social feed displays posts with text and images and updates like state on tap`() {
        val sampleBmp = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        val out = ByteArrayOutputStream()
        sampleBmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        val dataUri = "data:image/png;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)

        val fakeSocialRepo = FakeSocialFeedRepository(
            initialPosts = listOf(
                PostItem(
                    id = "p1",
                    authorId = "u2",
                    authorName = "Sara Tariq",
                    authorUsername = "sara_t",
                    caption = "Morning chai view from Margalla Hills!",
                    imageUrl = dataUri,
                    category = "Campus Life",
                    likesCount = 3L,
                    commentsCount = 1L,
                    createdAt = Timestamp.now()
                ),
                PostItem(
                    id = "p2",
                    authorId = "u3",
                    authorName = "Hamza Ali",
                    authorUsername = "hamza_dev",
                    caption = "Anyone joining the Lahore Android meetup?",
                    imageUrl = null,
                    category = "Tech & AI",
                    likesCount = 1L,
                    commentsCount = 0L,
                    createdAt = Timestamp.now()
                )
            ),
            initiallyLikedIds = setOf("p2")
        )
        val socialViewModel = SocialViewModel(fakeSocialRepo, currentUserId = "u1")

        composeRule.setContent {
            ApnaHubTheme(themeMode = ThemeMode.LIGHT, language = AppLanguage.ENGLISH) {
                SocialFeedScreen(
                    viewModel = socialViewModel,
                    currentUser = PrivateUser(
                        userId = "u1",
                        displayName = "Ali Raza",
                        username = "ali_pk",
                        profileCompleted = true
                    ),
                    savedItems = emptyList(),
                    onBack = {},
                    onCreatePost = {},
                    onOpenUserProfile = {},
                    onToggleSave = {},
                    onBlockUser = {}
                )
            }
        }

        composeRule.waitForIdle()
        // Both text & image posts render cleanly
        composeRule.onNodeWithTag("social_feed_list").assertIsDisplayed()
        composeRule.onNodeWithTag("social_post_card_p1").assertIsDisplayed()
        composeRule.onNodeWithTag("social_post_caption_p1").assertTextContains("Morning chai view from Margalla Hills!")
        composeRule.onNodeWithTag("social_post_image_p1").assertIsDisplayed()
        composeRule.onNodeWithTag("social_like_btn_p1").performScrollTo().assertTextContains("3")

        // Tap like on p1: increments count 3 -> 4 and marks p1 liked
        composeRule.onNodeWithTag("social_like_btn_p1").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("social_like_btn_p1").performScrollTo().assertTextContains("4")
        assertTrue("p1" in socialViewModel.likedPostIds.value)

        // Tap like again on p1: unlikes and decrements 4 -> 3
        composeRule.onNodeWithTag("social_like_btn_p1").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("social_like_btn_p1").performScrollTo().assertTextContains("3")
        assertFalse("p1" in socialViewModel.likedPostIds.value)

        // Scroll to p2 (already liked by current user) and verify category filter
        composeRule.onNodeWithTag("social_feed_list").performScrollToNode(hasTestTag("social_post_card_p2"))
        composeRule.onNodeWithTag("social_post_caption_p2").assertIsDisplayed()
        assertTrue("p2" in socialViewModel.likedPostIds.value)

        composeRule.onNodeWithTag("social_category_row").performScrollToNode(hasTestTag("social_cat_chip_tech_&_ai"))
        composeRule.onNodeWithTag("social_cat_chip_tech_&_ai").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("social_post_card_p2").assertIsDisplayed()
        composeRule.onNodeWithTag("social_post_card_p1").assertDoesNotExist()
    }
}

private class FakeSocialFeedRepository(
    initialPosts: List<PostItem>,
    initiallyLikedIds: Set<String> = emptySet()
) : SocialFeedRepository {
    private val postsFlow = MutableStateFlow(initialPosts)
    private val likedIds = initiallyLikedIds.toMutableSet()

    override fun observePosts(limitCount: Long): Flow<List<PostItem>> = postsFlow

    override fun observeFollowing(uid: String): Flow<List<FollowRecord>> = flowOf(emptyList())

    override fun observeComments(postId: String): Flow<List<CommentItem>> = flowOf(emptyList())

    override suspend fun hasLikedPost(postId: String): Boolean = postId in likedIds

    override suspend fun toggleLikePost(postId: String): Result<Boolean> {
        val nowLiked = if (postId in likedIds) {
            likedIds.remove(postId)
            false
        } else {
            likedIds.add(postId)
            true
        }
        postsFlow.value = postsFlow.value.map { post ->
            if (post.id == postId) {
                val nextCount = if (nowLiked) post.likesCount + 1L else (post.likesCount - 1L).coerceAtLeast(0L)
                post.copy(likesCount = nextCount)
            } else {
                post
            }
        }
        return Result.success(nowLiked)
    }

    override suspend fun createPost(
        authorName: String,
        authorUsername: String,
        authorPhotoUrl: String?,
        caption: String,
        category: String,
        imageUrl: String?
    ): Result<String> = Result.success("new_post")

    override suspend fun deletePost(postId: String): Result<Unit> = Result.success(Unit)

    override suspend fun addComment(
        postId: String,
        authorName: String,
        authorPhotoUrl: String?,
        text: String
    ): Result<Unit> = Result.success(Unit)

    override suspend fun toggleFollow(
        targetUserId: String,
        targetName: String,
        targetPhotoUrl: String?
    ): Result<Boolean> = Result.success(true)

    override suspend fun submitReport(targetType: String, targetId: String, reason: String): Result<Unit> =
        Result.success(Unit)
}

private class FakeAiAssistantRepository : AiAssistantRepository {
    var lastModelTier: AiModelTier? = null
    var lastPersona: AiPersona? = null
    var lastGroundingMode: AiGroundingMode? = null
    var lastAspectRatio: AiImageAspectRatio? = null
    private var liveCallbacks: LiveVoiceCallbacks? = null

    override fun resetConversation() {}

    override fun sendMessageStream(
        prompt: String,
        modelTier: AiModelTier,
        persona: AiPersona,
        groundingMode: AiGroundingMode
    ): Flow<AiStreamChunk> {
        lastModelTier = modelTier
        lastPersona = persona
        lastGroundingMode = groundingMode
        val citations = if (groundingMode == AiGroundingMode.GOOGLE_MAPS) {
            listOf(AiCitation("Gulberg Coffee House", "https://maps.google.com/?cid=123", AiCitationType.MAPS))
        } else {
            emptyList()
        }
        return flowOf(
            AiStreamChunk(
                text = "Here are top cafes in Gulberg, Lahore.",
                citations = citations
            )
        )
    }

    override suspend fun generateOrEditImage(
        prompt: String,
        inputBitmap: Bitmap?,
        aspectRatio: AiImageAspectRatio
    ): Bitmap {
        lastAspectRatio = aspectRatio
        return Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
    }

    override suspend fun transcribeAudio(audioBytes: ByteArray, mimeType: String): String {
        return "Salam, please summarize FSc Physics chapter 2."
    }

    override suspend fun startLiveVoiceSession(
        persona: AiPersona,
        callbacks: LiveVoiceCallbacks
    ): AiLiveSessionHandle {
        liveCallbacks = callbacks
        return object : AiLiveSessionHandle {
            override fun stop() {
                liveCallbacks = null
            }
        }
    }

    fun emitLiveTranscript(userText: String?, modelText: String?) {
        liveCallbacks?.onTranscript(userText, modelText)
    }
}

private class FakeUserProfileRepository(initialUser: PrivateUser) : UserProfileRepository {
    val userFlow = MutableStateFlow<PrivateUser?>(initialUser)

    override fun observeCurrentUser(uid: String): Flow<PrivateUser?> = userFlow

    override suspend fun updateProfile(
        displayName: String,
        username: String,
        city: String,
        bio: String,
        photoUrl: String?,
        markCompleted: Boolean
    ): Result<Unit> {
        val current = userFlow.value ?: PrivateUser(userId = "u1")
        userFlow.value = current.copy(
            displayName = displayName,
            username = username,
            city = city,
            bio = bio,
            photoUrl = photoUrl,
            profileCompleted = markCompleted
        )
        return Result.success(Unit)
    }
}

private class FakeChatMessagingRepository(
    initialMessages: List<ChatMessage>
) : ChatMessagingRepository {
    private val messagesFlow = MutableStateFlow(initialMessages)
    private var seq = initialMessages.size + 1

    override fun observeMessages(chatId: String, uid: String): Flow<List<ChatMessage>> = messagesFlow

    override suspend fun sendMessage(
        chatId: String,
        participantIds: List<String>,
        senderName: String,
        text: String,
        imageUrl: String?
    ): Result<Unit> {
        val next = ChatMessage(
            id = "m_${seq++}",
            chatId = chatId,
            senderId = "u1",
            senderName = senderName,
            text = text,
            imageUrl = imageUrl,
            participantIds = participantIds,
            status = "sent",
            createdAt = Timestamp.now()
        )
        messagesFlow.value = messagesFlow.value + next
        return Result.success(Unit)
    }

    override suspend fun markChatRead(chatId: String, currentUnread: List<String>) {}

    override suspend fun hideConversationLocally(chatId: String, currentHidden: List<String>): Result<Unit> =
        Result.success(Unit)

    fun pushIncomingMessage(message: ChatMessage) {
        messagesFlow.value = messagesFlow.value + message
    }
}
