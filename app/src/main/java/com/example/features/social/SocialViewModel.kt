package com.example.features.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.ApnaDependencies
import com.example.core.firebase.OperationType
import com.example.core.firebase.UiState
import com.example.core.firebase.handleFirestoreError
import com.example.core.firebase.unwrapFirestoreError
import com.example.data.models.CommentItem
import com.example.data.models.FollowRecord
import com.example.data.models.PostItem
import com.example.data.repositories.SocialFeedRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SocialViewModel(
    private val socialRepository: SocialFeedRepository,
    private val currentUserId: String
) : ViewModel() {

    constructor(deps: ApnaDependencies, currentUserId: String) : this(
        socialRepository = deps.socialRepository,
        currentUserId = currentUserId
    )

    private val pageLimit = MutableStateFlow(20L)
    private val refreshNonce = MutableStateFlow(0)
    private val checkedLikePostIds = mutableSetOf<String>()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _likedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val likedPostIds: StateFlow<Set<String>> = _likedPostIds.asStateFlow()

    private val _likingPostIds = MutableStateFlow<Set<String>>(emptySet())
    val likingPostIds: StateFlow<Set<String>> = _likingPostIds.asStateFlow()

    private val _optimisticLikeDeltas = MutableStateFlow<Map<String, Long>>(emptyMap())
    val optimisticLikeDeltas: StateFlow<Map<String, Long>> = _optimisticLikeDeltas.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val postsState: StateFlow<UiState<List<PostItem>>> = refreshNonce
        .flatMapLatest {
            pageLimit.flatMapLatest { limit ->
                socialRepository.observePosts(limit)
                    .onEach { posts ->
                        _isRefreshing.value = false
                        _optimisticLikeDeltas.update { deltas ->
                            val currentIds = posts.map { it.id }.toSet()
                            deltas.filterKeys { it in currentIds }
                        }
                        hydrateLikeStates(posts)
                    }
                    .map<List<PostItem>, UiState<List<PostItem>>> { UiState.Success(it) }
                    .catch { err ->
                        _isRefreshing.value = false
                        val root = unwrapFirestoreError(err)
                        handleFirestoreError(root, OperationType.LIST, "posts")
                        emit(UiState.Error(root.localizedMessage ?: "Failed to load social feed."))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val following: StateFlow<List<FollowRecord>> = socialRepository.observeFollowing(currentUserId)
        .catch { err ->
            handleFirestoreError(unwrapFirestoreError(err), OperationType.LIST, "follows")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private fun hydrateLikeStates(posts: List<PostItem>) {
        val unchecked = posts.filter { it.id.isNotBlank() && it.id !in checkedLikePostIds }
        if (unchecked.isEmpty()) return

        unchecked.forEach { checkedLikePostIds.add(it.id) }
        viewModelScope.launch {
            try {
                val results = coroutineScope {
                    unchecked.map { post ->
                        async {
                            post.id to runCatching { socialRepository.hasLikedPost(post.id) }.getOrDefault(false)
                        }
                    }.awaitAll()
                }
                val liked = results.filter { it.second }.map { it.first }.toSet()
                if (liked.isNotEmpty()) {
                    _likedPostIds.update { it + liked }
                }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun refresh() {
        _isRefreshing.value = true
        checkedLikePostIds.clear()
        refreshNonce.value += 1
    }

    fun loadMore() {
        pageLimit.value += 20L
    }

    fun toggleLike(post: PostItem) {
        val postId = post.id
        if (postId.isBlank() || postId in _likingPostIds.value) return

        val wasLiked = postId in _likedPostIds.value
        val delta = if (wasLiked) -1L else 1L

        _actionError.value = null
        _likingPostIds.update { it + postId }
        _likedPostIds.update { if (wasLiked) it - postId else it + postId }
        _optimisticLikeDeltas.update { it + (postId to (it[postId] ?: 0L) + delta) }

        viewModelScope.launch {
            try {
                socialRepository.toggleLikePost(postId)
                    .onSuccess { likedNow ->
                        checkedLikePostIds.add(postId)
                        _likedPostIds.update { if (likedNow) it + postId else it - postId }
                        _optimisticLikeDeltas.update { it - postId }
                    }
                    .onFailure { err ->
                        revertOptimisticLike(postId, wasLiked, delta)
                        _actionError.value = err.localizedMessage ?: "Could not update like right now."
                    }
            } catch (e: CancellationException) {
                revertOptimisticLike(postId, wasLiked, delta)
                throw e
            } finally {
                _likingPostIds.update { it - postId }
            }
        }
    }

    private fun revertOptimisticLike(postId: String, wasLiked: Boolean, delta: Long) {
        _likedPostIds.update { if (wasLiked) it + postId else it - postId }
        _optimisticLikeDeltas.update { current ->
            val adjusted = (current[postId] ?: 0L) - delta
            if (adjusted == 0L) current - postId else current + (postId to adjusted)
        }
    }

    fun toggleFollow(targetUserId: String, targetName: String, targetPhotoUrl: String?) {
        viewModelScope.launch {
            socialRepository.toggleFollow(targetUserId, targetName, targetPhotoUrl)
                .onFailure { err ->
                    _actionError.value = err.localizedMessage ?: "Failed to update follow status."
                }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            socialRepository.deletePost(postId)
                .onFailure { err ->
                    _actionError.value = err.localizedMessage ?: "Failed to delete post."
                }
        }
    }

    fun observeComments(postId: String): Flow<List<CommentItem>> =
        socialRepository.observeComments(postId)

    fun addComment(
        postId: String,
        authorName: String,
        authorPhotoUrl: String?,
        text: String
    ) {
        viewModelScope.launch {
            socialRepository.addComment(postId, authorName, authorPhotoUrl, text)
                .onFailure { err ->
                    _actionError.value = err.localizedMessage ?: "Failed to post comment."
                }
        }
    }

    fun submitReport(postId: String, reason: String) {
        viewModelScope.launch {
            socialRepository.submitReport("post", postId, reason)
                .onFailure { err ->
                    _actionError.value = err.localizedMessage ?: "Failed to submit report."
                }
        }
    }

    fun clearActionError() {
        _actionError.value = null
    }
}
