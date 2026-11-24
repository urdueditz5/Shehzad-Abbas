package com.example.features.profile

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.ApnaDependencies
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.core.firebase.unwrapFirestoreError
import com.example.core.utils.MediaUtils
import com.example.data.models.PrivateUser
import com.example.data.repositories.UserProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class UserProfileUiState(
    val user: PrivateUser? = null,
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isProcessingPhoto: Boolean = false,
    val draftDisplayName: String = "",
    val draftUsername: String = "",
    val draftCity: String = "",
    val draftBio: String = "",
    val draftPhotoUrl: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class UserProfileViewModel(
    private val userRepository: UserProfileRepository,
    private val currentUserId: String,
    private val initialEditMode: Boolean = false
) : ViewModel() {

    constructor(
        deps: ApnaDependencies,
        currentUserId: String,
        initialEditMode: Boolean = false
    ) : this(
        userRepository = deps.userRepository,
        currentUserId = currentUserId,
        initialEditMode = initialEditMode
    )

    private val _uiState = MutableStateFlow(
        UserProfileUiState(
            isLoading = currentUserId.isNotBlank(),
            isEditing = initialEditMode
        )
    )
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    private var hasInitializedDraftFromUser = false

    init {
        if (currentUserId.isNotBlank()) {
            viewModelScope.launch {
                userRepository.observeCurrentUser(currentUserId)
                    .catch { err ->
                        val root = unwrapFirestoreError(err)
                        handleFirestoreError(root, OperationType.GET, "users/$currentUserId")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = root.localizedMessage ?: "Unable to load profile."
                            )
                        }
                    }
                    .collect { profile ->
                        _uiState.update { state ->
                            val shouldSyncDrafts = !state.isEditing || (initialEditMode && !hasInitializedDraftFromUser && profile != null)
                            if (profile != null && shouldSyncDrafts) {
                                hasInitializedDraftFromUser = true
                                state.copy(
                                    user = profile,
                                    isLoading = false,
                                    draftDisplayName = profile.displayName,
                                    draftUsername = profile.username,
                                    draftCity = profile.city.orEmpty(),
                                    draftBio = profile.bio.orEmpty(),
                                    draftPhotoUrl = profile.photoUrl
                                )
                            } else {
                                state.copy(
                                    user = profile,
                                    isLoading = false
                                )
                            }
                        }
                    }
            }
        }
    }

    fun startEditing() {
        _uiState.update { state ->
            val current = state.user
            state.copy(
                isEditing = true,
                errorMessage = null,
                successMessage = null,
                draftDisplayName = current?.displayName ?: state.draftDisplayName,
                draftUsername = current?.username ?: state.draftUsername,
                draftCity = current?.city.orEmpty(),
                draftBio = current?.bio.orEmpty(),
                draftPhotoUrl = current?.photoUrl
            )
        }
    }

    fun cancelEditing() {
        _uiState.update { state ->
            val current = state.user
            state.copy(
                isEditing = false,
                isSaving = false,
                isProcessingPhoto = false,
                errorMessage = null,
                draftDisplayName = current?.displayName.orEmpty(),
                draftUsername = current?.username.orEmpty(),
                draftCity = current?.city.orEmpty(),
                draftBio = current?.bio.orEmpty(),
                draftPhotoUrl = current?.photoUrl
            )
        }
    }

    fun onDisplayNameChanged(value: String) {
        _uiState.update {
            it.copy(
                draftDisplayName = value.take(80),
                errorMessage = null
            )
        }
    }

    fun onUsernameChanged(value: String) {
        _uiState.update {
            it.copy(
                draftUsername = value.take(40),
                errorMessage = null
            )
        }
    }

    fun onCityChanged(value: String) {
        _uiState.update {
            it.copy(
                draftCity = value.take(60),
                errorMessage = null
            )
        }
    }

    fun onBioChanged(value: String) {
        _uiState.update {
            it.copy(
                draftBio = value.take(240),
                errorMessage = null
            )
        }
    }

    fun onPhotoDataUriSelected(dataUri: String?) {
        _uiState.update { state ->
            val baseUser = state.user
            state.copy(
                isEditing = true,
                isProcessingPhoto = false,
                errorMessage = null,
                draftDisplayName = if (state.isEditing) state.draftDisplayName else baseUser?.displayName.orEmpty(),
                draftUsername = if (state.isEditing) state.draftUsername else baseUser?.username.orEmpty(),
                draftCity = if (state.isEditing) state.draftCity else baseUser?.city.orEmpty(),
                draftBio = if (state.isEditing) state.draftBio else baseUser?.bio.orEmpty(),
                draftPhotoUrl = dataUri
            )
        }
    }

    fun onPhotoUriPicked(context: Context, uri: Uri) {
        val appContext = context.applicationContext
        _uiState.update { state ->
            val baseUser = state.user
            state.copy(
                isEditing = true,
                isProcessingPhoto = true,
                errorMessage = null,
                draftDisplayName = if (state.isEditing) state.draftDisplayName else baseUser?.displayName.orEmpty(),
                draftUsername = if (state.isEditing) state.draftUsername else baseUser?.username.orEmpty(),
                draftCity = if (state.isEditing) state.draftCity else baseUser?.city.orEmpty(),
                draftBio = if (state.isEditing) state.draftBio else baseUser?.bio.orEmpty(),
                draftPhotoUrl = if (state.isEditing) state.draftPhotoUrl else baseUser?.photoUrl
            )
        }
        viewModelScope.launch {
            MediaUtils.compressUriToDataUri(appContext, uri, maxDim = 360)
                .onSuccess { compressed ->
                    _uiState.update {
                        it.copy(
                            isProcessingPhoto = false,
                            draftPhotoUrl = compressed,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { err ->
                    if (err is CancellationException) throw err
                    _uiState.update {
                        it.copy(
                            isProcessingPhoto = false,
                            errorMessage = err.localizedMessage ?: "Failed to process selected photo."
                        )
                    }
                }
        }
    }

    fun removePhoto() {
        _uiState.update {
            it.copy(
                draftPhotoUrl = null,
                errorMessage = null
            )
        }
    }

    fun saveProfile(onSaved: () -> Unit = {}) {
        val snapshot = _uiState.value
        val cleanName = snapshot.draftDisplayName.trim()
        val cleanUsername = snapshot.draftUsername.trim().lowercase().replace(Regex("[^a-z0-9_.]"), "")
        if (cleanName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Display name cannot be empty.") }
            return
        }
        if (cleanUsername.length < 2) {
            _uiState.update { it.copy(errorMessage = "Username must be at least 2 valid characters.") }
            return
        }
        if (snapshot.isSaving || snapshot.isProcessingPhoto) return

        _uiState.update {
            it.copy(
                isSaving = true,
                errorMessage = null,
                successMessage = null
            )
        }

        viewModelScope.launch {
            val result = userRepository.updateProfile(
                displayName = cleanName,
                username = cleanUsername,
                city = snapshot.draftCity.trim(),
                bio = snapshot.draftBio.trim(),
                photoUrl = snapshot.draftPhotoUrl,
                markCompleted = true
            )
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isEditing = false,
                        successMessage = "Profile updated successfully."
                    )
                }
                onSaved()
            }.onFailure { err ->
                if (err is CancellationException) throw err
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = err.localizedMessage ?: "Failed to update profile."
                    )
                }
            }
        }
    }

    fun dismissFeedback() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                successMessage = null
            )
        }
    }
}
