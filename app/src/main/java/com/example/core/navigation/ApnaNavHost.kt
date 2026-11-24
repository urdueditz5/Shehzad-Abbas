package com.example.core.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.core.components.ApnaErrorState
import com.example.core.components.ApnaMainTopBar
import com.example.core.firebase.ApnaDependencies
import com.example.core.firebase.UiState
import com.example.core.firebase.authStateFlow
import com.example.core.firebase.signOutUser
import com.example.core.i18n.LocalApnaStrings
import com.example.core.preferences.UserPrefsState
import com.example.core.theme.ApnaHubTheme
import com.example.features.ai.AiAssistantScreen
import com.example.features.ai.AiViewModel
import com.example.features.auth.AuthScreen
import com.example.features.auth.ProfileSetupScreen
import com.example.features.auth.SessionViewModel
import com.example.features.chat.ChatScreen
import com.example.features.chat.ChatViewModel
import com.example.features.chat.InboxScreen
import com.example.features.chat.InboxViewModel
import com.example.features.create.CreateHubScreen
import com.example.features.education.EducationScreen
import com.example.features.home.ExploreScreen
import com.example.features.home.HomeDashboardScreen
import com.example.features.jobs.JobsScreen
import com.example.features.jobs.JobsViewModel
import com.example.features.marketplace.MarketplaceScreen
import com.example.features.marketplace.MarketplaceViewModel
import com.example.features.news.NewsScreen
import com.example.features.notifications.NotificationsScreen
import com.example.features.profile.AdminScreen
import com.example.features.profile.EditProfileRouteScreen
import com.example.features.profile.LegalScreen
import com.example.features.profile.ProfileScreen
import com.example.features.profile.SettingsScreen
import com.example.features.profile.UserProfileViewModel
import com.example.features.services.LocalServicesScreen
import com.example.features.services.LocalServicesViewModel
import com.example.features.social.PublicProfileDetailScreen
import com.example.features.social.SocialFeedScreen
import com.example.features.social.SocialViewModel
import com.example.features.tools.ToolsScreen
import com.example.features.tools.ToolsViewModel
import com.example.features.videos.ShortVideosScreen
import com.example.features.videos.ShortVideosViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

private inline fun <reified VM : ViewModel> simpleFactory(crossinline creator: () -> VM): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
    }

private data class MainTabSpec(
    val route: Any,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun ApnaAppRoot(deps: ApnaDependencies) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { androidx.credentials.CredentialManager.create(context) }

    val prefs by deps.preferencesRepository.prefsFlow.collectAsStateWithLifecycle(initialValue = UserPrefsState())
    val firebaseUser by remember { Firebase.auth.authStateFlow() }.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    var authLegalPrivacy by remember { mutableStateOf<Boolean?>(null) }

    ApnaHubTheme(
        themeMode = prefs.themeMode,
        language = prefs.language
    ) {
        if (firebaseUser == null) {
            val showPrivacy = authLegalPrivacy
            if (showPrivacy != null) {
                BackHandler { authLegalPrivacy = null }
                LegalScreen(
                    isPrivacy = showPrivacy,
                    onBack = { authLegalPrivacy = null }
                )
            } else {
                AuthScreen(
                    selectedLanguage = prefs.language,
                    onLanguageChange = { lang ->
                        scope.launch { deps.preferencesRepository.setLanguage(lang) }
                    },
                    onAuthSuccess = {},
                    onShowPrivacy = { authLegalPrivacy = true },
                    onShowTerms = { authLegalPrivacy = false }
                )
            }
        } else {
            val currentAuthUser = firebaseUser!!
            val uid = currentAuthUser.uid
            key(uid) {
                val sessionViewModel: SessionViewModel = viewModel(
                    key = "session_$uid",
                    factory = simpleFactory { SessionViewModel(deps, uid) }
                )
                LaunchedEffect(uid) {
                    sessionViewModel.bootstrapSignedInUser(currentAuthUser)
                }

                val currentUser by sessionViewModel.currentUserProfile.collectAsStateWithLifecycle()
                val bootstrapError by sessionViewModel.bootstrapError.collectAsStateWithLifecycle()

                val activeUser = currentUser
                if (activeUser == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bootstrapError != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ApnaErrorState(
                                    message = bootstrapError.orEmpty(),
                                    onRetry = { sessionViewModel.bootstrapSignedInUser(currentAuthUser) }
                                )
                                Button(onClick = { signOutUser(credentialManager, scope) }) {
                                    Text("Sign Out")
                                }
                            }
                        } else {
                            CircularProgressIndicator()
                        }
                    }
                } else if (!activeUser.profileCompleted) {
                    ProfileSetupScreen(
                        initialUser = activeUser,
                        isEditMode = false,
                        onSave = { name, uname, city, bio, photo, cb ->
                            sessionViewModel.completeProfile(name, uname, city, bio, photo, cb)
                        }
                    )
                } else {
                    ApnaMainShell(
                        uid = uid,
                        deps = deps,
                        sessionViewModel = sessionViewModel,
                        prefs = prefs
                    )
                }
            }
        }
    }
}

@Composable
private fun ApnaMainShell(
    uid: String,
    deps: ApnaDependencies,
    sessionViewModel: SessionViewModel,
    prefs: UserPrefsState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { androidx.credentials.CredentialManager.create(context) }
    val strings = LocalApnaStrings.current
    val navController = rememberNavController()

    val currentUser by sessionViewModel.currentUserProfile.collectAsStateWithLifecycle()
    val isAdmin by sessionViewModel.isAdmin.collectAsStateWithLifecycle()
    val publicProfiles by sessionViewModel.publicProfiles.collectAsStateWithLifecycle()
    val savedItems by sessionViewModel.savedItems.collectAsStateWithLifecycle()
    val notifications by sessionViewModel.notifications.collectAsStateWithLifecycle()

    val socialViewModel: SocialViewModel = viewModel(
        key = "social_$uid",
        factory = simpleFactory { SocialViewModel(deps, uid) }
    )
    val videosViewModel: ShortVideosViewModel = viewModel(
        key = "videos_$uid",
        factory = simpleFactory { ShortVideosViewModel(deps) }
    )
    val inboxViewModel: InboxViewModel = viewModel(
        key = "inbox_$uid",
        factory = simpleFactory { InboxViewModel(deps, uid) }
    )
    val jobsViewModel: JobsViewModel = viewModel(
        key = "jobs_$uid",
        factory = simpleFactory { JobsViewModel(deps) }
    )
    val marketplaceViewModel: MarketplaceViewModel = viewModel(
        key = "market_$uid",
        factory = simpleFactory { MarketplaceViewModel(deps) }
    )
    val servicesViewModel: LocalServicesViewModel = viewModel(
        key = "services_$uid",
        factory = simpleFactory { LocalServicesViewModel(deps) }
    )
    val toolsViewModel: ToolsViewModel = viewModel(
        key = "tools_$uid",
        factory = simpleFactory { ToolsViewModel(deps, uid) }
    )
    val aiViewModel: AiViewModel = viewModel(
        key = "ai_$uid",
        factory = simpleFactory { AiViewModel(deps) }
    )
    val profileViewModel: UserProfileViewModel = viewModel(
        key = "profile_$uid",
        factory = simpleFactory { UserProfileViewModel(deps, uid) }
    )

    val postsState by socialViewModel.postsState.collectAsStateWithLifecycle()
    val following by socialViewModel.following.collectAsStateWithLifecycle()
    val videosState by videosViewModel.videosState.collectAsStateWithLifecycle()
    val jobsState by jobsViewModel.state.collectAsStateWithLifecycle()
    val marketState by marketplaceViewModel.state.collectAsStateWithLifecycle()
    val servicesState by servicesViewModel.state.collectAsStateWithLifecycle()
    val announcements by deps.listingsRepository.observeAnnouncements()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val posts = (postsState as? UiState.Success)?.data.orEmpty()
    val videos = (videosState as? UiState.Success)?.data.orEmpty()
    val jobs = (jobsState as? UiState.Success)?.data.orEmpty()
    val marketItems = (marketState as? UiState.Success)?.data.orEmpty()
    val serviceItems = (servicesState as? UiState.Success)?.data.orEmpty()
    val unreadNotifCount = notifications.count { !it.readStatus }

    val mainTabs = listOf(
        MainTabSpec(HomeRoute, strings.navHome, Icons.Default.Home, "nav_home"),
        MainTabSpec(ExploreRoute, strings.navExplore, Icons.Default.Explore, "nav_explore"),
        MainTabSpec(CreateRoute, strings.navCreate, Icons.Default.AddCircle, "nav_create"),
        MainTabSpec(InboxRoute, strings.navInbox, Icons.AutoMirrored.Filled.Chat, "nav_inbox"),
        MainTabSpec(ProfileRoute, strings.navProfile, Icons.Default.Person, "nav_profile")
    )

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination
    val isMainTab = currentDest?.let { dest ->
        dest.hasRoute<HomeRoute>() ||
            dest.hasRoute<ExploreRoute>() ||
            dest.hasRoute<CreateRoute>() ||
            dest.hasRoute<InboxRoute>() ||
            dest.hasRoute<ProfileRoute>()
    } ?: true

    fun navigateTopTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openDirectChat(targetUid: String, targetName: String) {
        if (targetUid.isBlank() || targetUid == uid) return
        scope.launch {
            deps.chatRepository.getOrCreateChat(
                otherUserId = targetUid,
                currentUserName = currentUser?.displayName ?: "You",
                otherUserName = targetName
            ).onSuccess { thread ->
                navController.navigate(
                    ChatDetailRoute(
                        chatId = thread.id,
                        otherUserId = targetUid,
                        otherUserName = targetName
                    )
                )
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            topBar = {
                if (isMainTab) {
                    ApnaMainTopBar(
                        userName = currentUser?.displayName ?: "ApnaHub",
                        photoUrl = currentUser?.photoUrl,
                        unreadNotifications = unreadNotifCount,
                        onSearchClick = { navigateTopTab(ExploreRoute) },
                        onNotificationsClick = { navController.navigate(NotificationsRoute) },
                        onProfileClick = { navigateTopTab(ProfileRoute) }
                    )
                }
            },
            bottomBar = {
                if (isMainTab && !useRail) {
                    NavigationBar {
                        mainTabs.forEach { tab ->
                            val selected = when (tab.route) {
                                HomeRoute -> currentDest?.hasRoute<HomeRoute>() == true
                                ExploreRoute -> currentDest?.hasRoute<ExploreRoute>() == true
                                CreateRoute -> currentDest?.hasRoute<CreateRoute>() == true
                                InboxRoute -> currentDest?.hasRoute<InboxRoute>() == true
                                ProfileRoute -> currentDest?.hasRoute<ProfileRoute>() == true
                                else -> false
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isMainTab && useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        Spacer(Modifier.height(12.dp))
                        mainTabs.forEach { tab ->
                            val selected = when (tab.route) {
                                HomeRoute -> currentDest?.hasRoute<HomeRoute>() == true
                                ExploreRoute -> currentDest?.hasRoute<ExploreRoute>() == true
                                CreateRoute -> currentDest?.hasRoute<CreateRoute>() == true
                                InboxRoute -> currentDest?.hasRoute<InboxRoute>() == true
                                ProfileRoute -> currentDest?.hasRoute<ProfileRoute>() == true
                                else -> false
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = HomeRoute,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    composable<HomeRoute> {
                        HomeDashboardScreen(
                            user = currentUser,
                            announcements = announcements,
                            notifications = notifications,
                            savedItems = savedItems,
                            recentPosts = posts,
                            recentVideos = videos,
                            onSearchClick = { navigateTopTab(ExploreRoute) },
                            onNotificationsClick = { navController.navigate(NotificationsRoute) },
                            onOpenSocial = { navController.navigate(SocialFeedRoute) },
                            onOpenVideos = { navController.navigate(ShortVideosRoute) },
                            onOpenMessaging = { navigateTopTab(InboxRoute) },
                            onOpenEducation = { navController.navigate(EducationRoute) },
                            onOpenJobs = { navController.navigate(JobsRoute) },
                            onOpenMarketplace = { navController.navigate(MarketplaceRoute) },
                            onOpenLocalServices = { navController.navigate(LocalServicesRoute) },
                            onOpenNews = { navController.navigate(NewsRoute) },
                            onOpenTools = { navController.navigate(ToolsRoute) },
                            onOpenAiAssistant = { navController.navigate(AiAssistantRoute) }
                        )
                    }

                    composable<ExploreRoute> {
                        ExploreScreen(
                            posts = posts,
                            jobs = jobs,
                            marketplace = marketItems,
                            services = serviceItems,
                            profiles = publicProfiles,
                            onOpenSocial = { navController.navigate(SocialFeedRoute) },
                            onOpenJobs = { navController.navigate(JobsRoute) },
                            onOpenMarketplace = { navController.navigate(MarketplaceRoute) },
                            onOpenServices = { navController.navigate(LocalServicesRoute) },
                            onOpenProfile = { targetId -> navController.navigate(PublicProfileRoute(targetId)) }
                        )
                    }

                    composable<CreateRoute> {
                        CreateHubScreen(
                            user = currentUser,
                            deps = deps,
                            isAdmin = isAdmin,
                            onPostCreated = { navController.navigate(SocialFeedRoute) },
                            onVideoCreated = { navController.navigate(ShortVideosRoute) },
                            onMarketCreated = { navController.navigate(MarketplaceRoute) },
                            onServiceCreated = { navController.navigate(LocalServicesRoute) },
                            onOpenAdmin = { navController.navigate(AdminRoute) }
                        )
                    }

                    composable<InboxRoute> {
                        InboxScreen(
                            viewModel = inboxViewModel,
                            currentUser = currentUser,
                            publicProfiles = publicProfiles,
                            deps = deps,
                            onOpenChat = { chatId, otherUid, otherName ->
                                navController.navigate(ChatDetailRoute(chatId, otherUid, otherName))
                            }
                        )
                    }

                    composable<ProfileRoute> {
                        val myPosts = remember(posts, uid) { posts.filter { it.authorId == uid } }
                        ProfileScreen(
                            viewModel = profileViewModel,
                            myPosts = myPosts,
                            savedItems = savedItems,
                            following = following,
                            isAdmin = isAdmin,
                            onOpenSettings = { navController.navigate(SettingsRoute) },
                            onOpenAdmin = { navController.navigate(AdminRoute) },
                            onOpenPrivacy = { navController.navigate(LegalRoute(isPrivacy = true)) },
                            onOpenTerms = { navController.navigate(LegalRoute(isPrivacy = false)) },
                            onOpenUserProfile = { targetId -> navController.navigate(PublicProfileRoute(targetId)) }
                        )
                    }

                    composable<SocialFeedRoute> {
                        SocialFeedScreen(
                            viewModel = socialViewModel,
                            currentUser = currentUser,
                            savedItems = savedItems,
                            onBack = { navController.popBackStack() },
                            onCreatePost = { navigateTopTab(CreateRoute) },
                            onOpenUserProfile = { targetId -> navController.navigate(PublicProfileRoute(targetId)) },
                            onToggleSave = { post ->
                                sessionViewModel.toggleSaveItem(
                                    itemType = "post",
                                    itemId = post.id,
                                    title = post.authorName,
                                    subtitle = post.caption.take(100)
                                )
                            },
                            onBlockUser = { sessionViewModel.toggleBlockUser(it) }
                        )
                    }

                    composable<ShortVideosRoute> {
                        val followingIds = remember(following) { following.map { it.targetUserId }.toSet() }
                        ShortVideosScreen(
                            viewModel = videosViewModel,
                            currentUser = currentUser,
                            savedItems = savedItems,
                            followingIds = followingIds,
                            deps = deps,
                            onBack = { navController.popBackStack() },
                            onUploadClick = { navigateTopTab(CreateRoute) },
                            onOpenCreator = { creatorId -> navController.navigate(PublicProfileRoute(creatorId)) },
                            onToggleSave = { video ->
                                sessionViewModel.toggleSaveItem(
                                    itemType = "video",
                                    itemId = video.id,
                                    title = video.title,
                                    subtitle = "@${video.creatorUsername}"
                                )
                            }
                        )
                    }

                    composable<EducationRoute> {
                        EducationScreen(
                            deps = deps,
                            userPrefs = prefs,
                            savedItems = savedItems,
                            onToggleLessonCompleted = { sessionViewModel.toggleLessonCompleted(it) },
                            onRecordQuizScore = { sessionViewModel.recordQuizScore(it) },
                            onToggleSave = { type, id, title, sub ->
                                sessionViewModel.toggleSaveItem(type, id, title, sub)
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<JobsRoute> {
                        JobsScreen(
                            viewModel = jobsViewModel,
                            savedItems = savedItems,
                            onToggleSave = { type, id, title, sub ->
                                sessionViewModel.toggleSaveItem(type, id, title, sub)
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<MarketplaceRoute> {
                        MarketplaceScreen(
                            viewModel = marketplaceViewModel,
                            deps = deps,
                            currentUserId = uid,
                            blockedUserIds = currentUser?.blockedUserIds.orEmpty(),
                            savedItems = savedItems,
                            onToggleSave = { type, id, title, sub ->
                                sessionViewModel.toggleSaveItem(type, id, title, sub)
                            },
                            onStartChat = { sellerId, sellerName -> openDirectChat(sellerId, sellerName) },
                            onOpenCreate = { navigateTopTab(CreateRoute) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<LocalServicesRoute> {
                        LocalServicesScreen(
                            viewModel = servicesViewModel,
                            deps = deps,
                            currentUserId = uid,
                            blockedUserIds = currentUser?.blockedUserIds.orEmpty(),
                            onStartChat = { providerId, providerName -> openDirectChat(providerId, providerName) },
                            onOpenCreate = { navigateTopTab(CreateRoute) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<NewsRoute> {
                        NewsScreen(
                            deps = deps,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<ToolsRoute> {
                        ToolsScreen(
                            viewModel = toolsViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<AiAssistantRoute> {
                        AiAssistantScreen(
                            viewModel = aiViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<NotificationsRoute> {
                        NotificationsScreen(
                            notifications = notifications,
                            onMarkRead = { sessionViewModel.markNotificationRead(it) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<SettingsRoute> {
                        SettingsScreen(
                            user = currentUser,
                            userPrefs = prefs,
                            publicProfiles = publicProfiles,
                            onLanguageChange = { sessionViewModel.updateLanguage(it) },
                            onThemeChange = { sessionViewModel.updateThemeMode(it) },
                            onNotificationsToggle = { sessionViewModel.updateNotificationsEnabled(it) },
                            onUnblockUser = { sessionViewModel.toggleBlockUser(it) },
                            onOpenPrivacy = { navController.navigate(LegalRoute(isPrivacy = true)) },
                            onOpenTerms = { navController.navigate(LegalRoute(isPrivacy = false)) },
                            onSignOut = { signOutUser(credentialManager, scope) },
                            onDeleteAccount = { cb -> sessionViewModel.deleteAccount(cb) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<EditProfileRoute> {
                        val editProfileViewModel: UserProfileViewModel = viewModel(
                            key = "edit_profile_$uid",
                            factory = simpleFactory { UserProfileViewModel(deps, uid, initialEditMode = true) }
                        )
                        EditProfileRouteScreen(
                            viewModel = editProfileViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<AdminRoute> {
                        AdminScreen(
                            deps = deps,
                            isAdmin = isAdmin,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<LegalRoute> { backStack ->
                        val args = backStack.toRoute<LegalRoute>()
                        LegalScreen(
                            isPrivacy = args.isPrivacy,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<ChatDetailRoute> { backStack ->
                        val args = backStack.toRoute<ChatDetailRoute>()
                        val chatViewModel: ChatViewModel = viewModel(
                            key = "chat_${uid}_${args.chatId}",
                            factory = simpleFactory {
                                ChatViewModel(
                                    deps = deps,
                                    chatId = args.chatId,
                                    currentUserId = uid,
                                    otherUserId = args.otherUserId,
                                    currentUserNameProvider = { currentUser?.displayName ?: "You" }
                                )
                            }
                        )
                        val otherProfile = remember(publicProfiles, args.otherUserId) {
                            publicProfiles.firstOrNull { it.userId == args.otherUserId }
                        }
                        ChatScreen(
                            viewModel = chatViewModel,
                            currentUserId = uid,
                            otherUserId = args.otherUserId,
                            otherUserName = args.otherUserName,
                            otherUserPhotoUrl = otherProfile?.photoUrl,
                            onBack = { navController.popBackStack() },
                            onOpenProfile = { targetId -> navController.navigate(PublicProfileRoute(targetId)) },
                            onReportUser = { reason ->
                                scope.launch {
                                    deps.socialRepository.submitReport("user", args.otherUserId, reason)
                                }
                            },
                            onBlockUser = { sessionViewModel.toggleBlockUser(it) }
                        )
                    }

                    composable<PublicProfileRoute> { backStack ->
                        val args = backStack.toRoute<PublicProfileRoute>()
                        val followingIds = remember(following) { following.map { it.targetUserId }.toSet() }
                        PublicProfileDetailScreen(
                            targetUserId = args.userId,
                            profiles = publicProfiles,
                            posts = posts,
                            followingIds = followingIds,
                            currentUserId = uid,
                            onBack = { navController.popBackStack() },
                            onFollowToggle = { profile ->
                                scope.launch {
                                    deps.socialRepository.toggleFollow(
                                        profile.userId,
                                        profile.displayName,
                                        profile.photoUrl
                                    )
                                }
                            },
                            onMessageUser = { profile ->
                                openDirectChat(profile.userId, profile.displayName)
                            },
                            onBlockUser = {
                                sessionViewModel.toggleBlockUser(it)
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
