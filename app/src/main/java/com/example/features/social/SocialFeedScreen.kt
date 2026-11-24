package com.example.features.social

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.components.ApnaEmptyState
import com.example.core.components.ApnaErrorState
import com.example.core.components.ApnaRemoteOrDataImage
import com.example.core.components.ApnaSubScreenTopBar
import com.example.core.components.SkeletonLoadingList
import com.example.core.components.UserAvatar
import com.example.core.constants.PakistanConstants
import com.example.core.firebase.UiState
import com.example.core.theme.ErrorRed
import com.example.data.models.CommentItem
import com.example.data.models.PostItem
import com.example.data.models.PrivateUser
import com.example.data.models.PublicProfile
import com.example.data.models.SavedItem
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialFeedScreen(
    viewModel: SocialViewModel,
    currentUser: PrivateUser?,
    savedItems: List<SavedItem>,
    onBack: () -> Unit,
    onCreatePost: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onToggleSave: (PostItem) -> Unit,
    onBlockUser: (String) -> Unit
) {
    val context = LocalContext.current
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val following by viewModel.following.collectAsStateWithLifecycle()
    val likedPostIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
    val likingPostIds by viewModel.likingPostIds.collectAsStateWithLifecycle()
    val optimisticLikeDeltas by viewModel.optimisticLikeDeltas.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var activeCommentsPost by remember { mutableStateOf<PostItem?>(null) }
    var reportTargetPost by remember { mutableStateOf<PostItem?>(null) }
    var reportReason by remember { mutableStateOf("") }
    var fullscreenImageSource by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(actionError) {
        val message = actionError
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearActionError()
        }
    }

    val blockedIds = remember(currentUser?.blockedUserIds) {
        currentUser?.blockedUserIds.orEmpty().toSet()
    }
    val followingIds = remember(following) {
        following.map { it.targetUserId }.toSet()
    }
    val savedPostIds = remember(savedItems) {
        savedItems.filter { it.itemType == "post" }.map { it.itemId }.toSet()
    }

    Scaffold(
        topBar = { ApnaSubScreenTopBar(title = "Social Feed", onBack = onBack) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreatePost,
                modifier = Modifier.testTag("social_create_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Post")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("social_category_row")
                ) {
                    items(PakistanConstants.socialCategories) { cat ->
                        val catTag = cat.lowercase().replace(" ", "_")
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { viewModel.selectCategory(cat) },
                            label = { Text(cat) },
                            modifier = Modifier.testTag("social_cat_chip_$catTag")
                        )
                    }
                }

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.weight(1f)
                ) {
                    when (val state = postsState) {
                        is UiState.Loading -> SkeletonLoadingList(count = 4)
                        is UiState.Error -> ApnaErrorState(
                            message = state.message,
                            onRetry = { viewModel.refresh() },
                            modifier = Modifier.testTag("social_feed_error")
                        )

                        is UiState.Success -> {
                            val visiblePosts = state.data
                                .filter { it.authorId !in blockedIds }
                                .filter { selectedCategory == "All" || it.category == selectedCategory }

                            if (visiblePosts.isEmpty()) {
                                ApnaEmptyState(
                                    title = "No posts yet",
                                    subtitle = "Be the first to share an update, photo, or story with the ApnaHub community.",
                                    actionLabel = "Create Post",
                                    onAction = onCreatePost,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("social_feed_empty")
                                )
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("social_feed_list")
                                ) {
                                    items(visiblePosts, key = { it.id }) { post ->
                                        val isLiked = post.id in likedPostIds
                                        val isLikeInFlight = post.id in likingPostIds
                                        val displayLikes = (post.likesCount + (optimisticLikeDeltas[post.id] ?: 0L)).coerceAtLeast(0L)

                                        PostCard(
                                            post = post,
                                            displayLikesCount = displayLikes,
                                            isLikedByUser = isLiked,
                                            isLikeInFlight = isLikeInFlight,
                                            isOwnPost = post.authorId == currentUser?.userId,
                                            isFollowingAuthor = post.authorId in followingIds,
                                            isBookmarked = post.id in savedPostIds,
                                            onAuthorClick = { onOpenUserProfile(post.authorId) },
                                            onImageClick = { img -> fullscreenImageSource = img },
                                            onFollowToggle = {
                                                viewModel.toggleFollow(
                                                    post.authorId,
                                                    post.authorName,
                                                    post.authorPhotoUrl
                                                )
                                            },
                                            onLikeToggle = { viewModel.toggleLike(post) },
                                            onCommentClick = { activeCommentsPost = post },
                                            onShareClick = {
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, "${post.authorName} on ApnaHub:\n${post.caption}")
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share post"))
                                            },
                                            onSaveClick = { onToggleSave(post) },
                                            onReportClick = { reportTargetPost = post },
                                            onBlockClick = { onBlockUser(post.authorId) },
                                            onDeleteClick = { viewModel.deletePost(post.id) }
                                        )
                                    }
                                    if (state.data.size >= 20) {
                                        item {
                                            OutlinedButton(
                                                onClick = { viewModel.loadMore() },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("social_load_more_button")
                                            ) {
                                                Text("Load More Posts")
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
    }

    fullscreenImageSource?.let { source ->
        Dialog(
            onDismissRequest = { fullscreenImageSource = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { fullscreenImageSource = null }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                ApnaRemoteOrDataImage(
                    source = source,
                    contentDescription = "Full size post image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                )
                IconButton(
                    onClick = { fullscreenImageSource = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close image preview", tint = Color.White)
                }
            }
        }
    }

    activeCommentsPost?.let { post ->
        CommentsBottomSheet(
            commentsFlow = viewModel.observeComments(post.id),
            onDismiss = { activeCommentsPost = null },
            onSendComment = { text ->
                viewModel.addComment(
                    postId = post.id,
                    authorName = currentUser?.displayName ?: "User",
                    authorPhotoUrl = currentUser?.photoUrl,
                    text = text
                )
            }
        )
    }

    reportTargetPost?.let { post ->
        AlertDialog(
            onDismissRequest = { reportTargetPost = null },
            title = { Text("Report Post") },
            text = {
                OutlinedTextField(
                    value = reportReason,
                    onValueChange = { reportReason = it },
                    label = { Text("Reason (spam, harassment, misleading)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val reason = reportReason
                    reportTargetPost = null
                    reportReason = ""
                    viewModel.submitReport(post.id, reason)
                }) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = { reportTargetPost = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PostCard(
    post: PostItem,
    displayLikesCount: Long,
    isLikedByUser: Boolean,
    isLikeInFlight: Boolean,
    isOwnPost: Boolean,
    isFollowingAuthor: Boolean,
    isBookmarked: Boolean,
    onAuthorClick: () -> Unit,
    onImageClick: (String) -> Unit,
    onFollowToggle: () -> Unit,
    onLikeToggle: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onReportClick: () -> Unit,
    onBlockClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val dateStr = remember(post.createdAt) {
        post.createdAt?.toDate()?.let {
            SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(it)
        } ?: "Just now"
    }
    val heartScale by animateFloatAsState(
        targetValue = if (isLikedByUser) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "like_scale"
    )
    val heartColor by animateColorAsState(
        targetValue = if (isLikedByUser) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "like_color"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("social_post_card_${post.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onAuthorClick)
                ) {
                    UserAvatar(name = post.authorName, photoUrl = post.authorPhotoUrl, size = 42.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = post.authorName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = post.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "@${post.authorUsername} • $dateStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!isOwnPost) {
                    TextButton(
                        onClick = onFollowToggle,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text(if (isFollowingAuthor) "Following" else "Follow")
                    }
                }
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Post options")
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (isOwnPost) {
                            DropdownMenuItem(
                                text = { Text("Delete Post") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    onDeleteClick()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Report Post") },
                                onClick = {
                                    menuOpen = false
                                    onReportClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Block User") },
                                onClick = {
                                    menuOpen = false
                                    onBlockClick()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = post.caption,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("social_post_caption_${post.id}")
            )

            if (!post.imageUrl.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { onImageClick(post.imageUrl) }
                        .testTag("social_post_image_${post.id}")
                ) {
                    ApnaRemoteOrDataImage(
                        source = post.imageUrl,
                        contentDescription = "Post image by ${post.authorName}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(enabled = !isLikeInFlight, onClick = onLikeToggle)
                            .minimumInteractiveComponentSize()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("social_like_btn_${post.id}")
                    ) {
                        Icon(
                            imageVector = if (isLikedByUser) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isLikedByUser) "Unlike post" else "Like post",
                            tint = heartColor,
                            modifier = Modifier
                                .size(22.dp)
                                .scale(heartScale)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = displayLikesCount.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isLikedByUser) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("social_like_count_${post.id}")
                        )
                    }

                    Spacer(Modifier.width(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onCommentClick)
                            .minimumInteractiveComponentSize()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("social_comment_btn_${post.id}")
                    ) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = "Comments",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = post.commentsCount.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("social_share_btn_${post.id}")
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share post",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onSaveClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("social_save_btn_${post.id}")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Remove saved post" else "Save post",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    commentsFlow: Flow<List<CommentItem>>,
    onDismiss: () -> Unit,
    onSendComment: (String) -> Unit
) {
    val comments by commentsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var commentText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Comments (${comments.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            if (comments.isEmpty()) {
                Text(
                    text = "No comments yet. Start the conversation!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(comments, key = { it.id }) { comment ->
                        Row(verticalAlignment = Alignment.Top) {
                            UserAvatar(name = comment.authorName, photoUrl = comment.authorPhotoUrl, size = 32.dp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(comment.authorName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { if (it.length <= 500) commentText = it },
                    placeholder = { Text("Write a comment...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            onSendComment(commentText)
                            commentText = ""
                        }
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send comment",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun PublicProfileDetailScreen(
    targetUserId: String,
    profiles: List<PublicProfile>,
    posts: List<PostItem>,
    followingIds: Set<String>,
    currentUserId: String,
    onBack: () -> Unit,
    onFollowToggle: (PublicProfile) -> Unit,
    onMessageUser: (PublicProfile) -> Unit,
    onBlockUser: (String) -> Unit
) {
    val profile = profiles.firstOrNull { it.userId == targetUserId }
    val userPosts = posts.filter { it.authorId == targetUserId }
    val isFollowing = targetUserId in followingIds

    Scaffold(
        topBar = {
            ApnaSubScreenTopBar(
                title = profile?.displayName ?: "Member Profile",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        if (profile == null) {
            ApnaEmptyState(
                title = "Profile unavailable",
                subtitle = "This member profile could not be loaded.",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            UserAvatar(name = profile.displayName, photoUrl = profile.photoUrl, size = 76.dp)
                            Spacer(Modifier.height(10.dp))
                            Text(profile.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                text = "@${profile.username}" + (profile.city?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!profile.bio.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(profile.bio, style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(profile.followersCount.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Followers", style = MaterialTheme.typography.labelSmall)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(userPosts.size.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Posts", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (profile.userId != currentUserId) {
                                Spacer(Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(onClick = { onFollowToggle(profile) }) {
                                        Text(if (isFollowing) "Following" else "Follow")
                                    }
                                    OutlinedButton(onClick = { onMessageUser(profile) }) {
                                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Message")
                                    }
                                    TextButton(onClick = { onBlockUser(profile.userId) }) {
                                        Text("Block")
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Text("Recent Posts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(userPosts, key = { it.id }) { post ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(post.caption, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
