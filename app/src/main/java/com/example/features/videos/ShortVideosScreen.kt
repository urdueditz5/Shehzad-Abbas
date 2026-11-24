package com.example.features.videos

import android.content.Intent
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.core.components.ApnaEmptyState
import com.example.core.components.ApnaErrorState
import com.example.core.components.ApnaSubScreenTopBar
import com.example.core.components.SkeletonLoadingList
import com.example.core.components.UserAvatar
import com.example.core.firebase.ApnaDependencies
import com.example.core.firebase.OperationType
import com.example.core.firebase.UiState
import com.example.core.firebase.handleFirestoreError
import com.example.core.firebase.unwrapFirestoreError
import com.example.core.theme.ElectricCyan
import com.example.core.theme.ErrorRed
import com.example.data.models.PrivateUser
import com.example.data.models.SavedItem
import com.example.data.models.ShortVideo
import com.example.features.social.CommentsBottomSheet
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShortVideosViewModel(
    private val deps: ApnaDependencies
) : ViewModel() {
    val videosState: StateFlow<UiState<List<ShortVideo>>> = deps.videoRepository.observeVideos()
        .map<List<ShortVideo>, UiState<List<ShortVideo>>> { UiState.Success(it) }
        .catch { err ->
            val root = unwrapFirestoreError(err)
            handleFirestoreError(root, OperationType.LIST, "videos")
            emit(UiState.Error(root.localizedMessage ?: "Failed to load short videos."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
}

@Composable
fun ShortVideosScreen(
    viewModel: ShortVideosViewModel,
    currentUser: PrivateUser?,
    savedItems: List<SavedItem>,
    followingIds: Set<String>,
    deps: ApnaDependencies,
    onBack: () -> Unit,
    onUploadClick: () -> Unit,
    onOpenCreator: (String) -> Unit,
    onToggleSave: (ShortVideo) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.videosState.collectAsStateWithLifecycle()
    var activeCommentVideo by remember { mutableStateOf<ShortVideo?>(null) }
    val savedIds = savedItems.filter { it.itemType == "video" }.map { it.itemId }.toSet()

    when (val ui = state) {
        is UiState.Loading -> {
            Scaffold(topBar = { ApnaSubScreenTopBar(title = "Short Videos", onBack = onBack) }) {
                Box(Modifier.padding(it)) { SkeletonLoadingList(3) }
            }
        }
        is UiState.Error -> {
            Scaffold(topBar = { ApnaSubScreenTopBar(title = "Short Videos", onBack = onBack) }) {
                Box(Modifier.padding(it)) { ApnaErrorState(ui.message) }
            }
        }
        is UiState.Success -> {
            val videos = ui.data
            if (videos.isEmpty()) {
                Scaffold(topBar = { ApnaSubScreenTopBar(title = "Short Videos", onBack = onBack) }) { inner ->
                    ApnaEmptyState(
                        title = "No short videos yet",
                        subtitle = "Publish a short educational or community video clip (under 15 MB / HTTPS stream) from Create.",
                        actionLabel = "Publish Short Video",
                        onAction = onUploadClick,
                        modifier = Modifier.padding(inner)
                    )
                }
            } else {
                val pagerState = rememberPagerState(pageCount = { videos.size })
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val video = videos[page]
                        val isCurrentPage = pagerState.currentPage == page
                        LaunchedEffect(isCurrentPage, video.id) {
                            if (isCurrentPage) {
                                deps.videoRepository.incrementView(video.id)
                            }
                        }
                        ShortVideoPage(
                            video = video,
                            isActive = isCurrentPage,
                            isSaved = video.id in savedIds,
                            isFollowing = video.creatorId in followingIds,
                            isOwnVideo = video.creatorId == currentUser?.userId,
                            onCreatorClick = { onOpenCreator(video.creatorId) },
                            onFollowToggle = {
                                scope.launch {
                                    deps.socialRepository.toggleFollow(
                                        video.creatorId,
                                        video.creatorName,
                                        video.creatorPhotoUrl
                                    )
                                }
                            },
                            onLikeClick = {
                                scope.launch { deps.videoRepository.toggleLikeVideo(video.id) }
                            },
                            onCommentClick = { activeCommentVideo = video },
                            onSaveClick = { onToggleSave(video) },
                            onShareClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "${video.title} — ${video.videoUrl}")
                                }
                                context.startActivity(Intent.createChooser(intent, "Share video"))
                            }
                        )
                    }

                    // Top Overlay Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, start = 12.dp, end = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                .testTag("video_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text(
                            text = "ApnaHub Shorts",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = onUploadClick,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                .testTag("video_upload_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Upload short video", tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    activeCommentVideo?.let { video ->
        CommentsBottomSheet(
            commentsFlow = deps.videoRepository.observeVideoComments(video.id),
            onDismiss = { activeCommentVideo = null },
            onSendComment = { text ->
                scope.launch {
                    deps.videoRepository.addVideoComment(
                        videoId = video.id,
                        authorName = currentUser?.displayName ?: "User",
                        authorPhotoUrl = currentUser?.photoUrl,
                        text = text
                    )
                }
            }
        )
    }
}

@Composable
private fun ShortVideoPage(
    video: ShortVideo,
    isActive: Boolean,
    isSaved: Boolean,
    isFollowing: Boolean,
    isOwnVideo: Boolean,
    onCreatorClick: () -> Unit,
    onFollowToggle: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val context = LocalContext.current
    var isPaused by remember(video.id) { mutableStateOf(false) }

    val exoPlayer = remember(video.id, video.videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.videoUrl))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
        }
    }

    LaunchedEffect(isActive, isPaused) {
        exoPlayer.playWhenReady = isActive && !isPaused
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { isPaused = !isPaused }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isPaused) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Paused",
                    tint = Color.White,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        // Right-side action bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconButton(onClick = onLikeClick) {
                Icon(Icons.Default.Favorite, contentDescription = "Like video", tint = ErrorRed)
            }
            Text(video.likesCount.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall)

            IconButton(onClick = onCommentClick) {
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comment", tint = Color.White)
            }
            Text(video.commentsCount.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall)

            IconButton(onClick = onSaveClick) {
                Icon(
                    if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Save video",
                    tint = if (isSaved) ElectricCyan else Color.White
                )
            }
            IconButton(onClick = onShareClick) {
                Icon(Icons.Default.Share, contentDescription = "Share video", tint = Color.White)
            }
            IconButton(onClick = { isPaused = !isPaused }) {
                Icon(
                    if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = "Play or pause",
                    tint = Color.White
                )
            }
        }

        // Bottom-left creator & video caption
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 84.dp, bottom = 36.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onCreatorClick)
                ) {
                    UserAvatar(name = video.creatorName, photoUrl = video.creatorPhotoUrl, size = 36.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "@${video.creatorUsername}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!isOwnVideo) {
                    TextButton(onClick = onFollowToggle) {
                        Text(if (isFollowing) "Following" else "Follow", color = ElectricCyan)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(video.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (video.description.isNotBlank()) {
                Text(video.description, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("${video.viewsCount} views", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
