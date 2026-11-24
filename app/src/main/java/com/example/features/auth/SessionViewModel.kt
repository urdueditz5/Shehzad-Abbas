package com.example.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.firebase.ApnaDependencies
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.core.firebase.unwrapFirestoreError
import com.example.core.preferences.AppLanguage
import com.example.core.preferences.ThemeMode
import com.example.core.preferences.UserPrefsState
import com.example.data.models.AppNotification
import com.example.data.models.PrivateUser
import com.example.data.models.PublicProfile
import com.example.data.models.SavedItem
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SessionViewModel(
    private val deps: ApnaDependencies,
    private val currentUserId: String
) : ViewModel() {

    private val _bootstrapError = MutableStateFlow<String?>(null)
    val bootstrapError: StateFlow<String?> = _bootstrapError.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    val userPrefs: StateFlow<UserPrefsState> = deps.preferencesRepository.prefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UserPrefsState())

    val currentUserProfile: StateFlow<PrivateUser?> = deps.userRepository.observeCurrentUser(currentUserId)
        .catch { err ->
            val root = unwrapFirestoreError(err)
            handleFirestoreError(root, OperationType.GET, "users/$currentUserId")
            _bootstrapError.value = root.localizedMessage ?: "Failed to load user profile."
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val publicProfiles: StateFlow<List<PublicProfile>> = deps.userRepository.observePublicProfiles()
        .catch { err ->
            val root = unwrapFirestoreError(err)
            handleFirestoreError(root, OperationType.LIST, "public_profiles")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val savedItems: StateFlow<List<SavedItem>> = deps.userRepository.observeSavedItems(currentUserId)
        .catch { err ->
            val root = unwrapFirestoreError(err)
            handleFirestoreError(root, OperationType.LIST, "users/$currentUserId/saved_items")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<AppNotification>> = deps.userRepository.observeNotifications(currentUserId)
        .catch { err ->
            val root = unwrapFirestoreError(err)
            handleFirestoreError(root, OperationType.LIST, "users/$currentUserId/notifications")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun bootstrapSignedInUser(firebaseUser: FirebaseUser) {
        viewModelScope.launch {
            _bootstrapError.value = null
            deps.userRepository.ensureUserBootstrapped(firebaseUser)
                .onFailure { _bootstrapError.value = it.localizedMessage ?: "Failed to initialize profile." }
            _isAdmin.value = deps.userRepository.isUserAdmin()
        }
    }

    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            deps.preferencesRepository.setLanguage(language)
            deps.userRepository.syncUserPreferences(languageCode = language.code)
        }
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            deps.preferencesRepository.setThemeMode(themeMode)
            deps.userRepository.syncUserPreferences(themeModeCode = themeMode.code)
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            deps.preferencesRepository.setNotificationsEnabled(enabled)
            deps.userRepository.syncUserPreferences(notificationsEnabled = enabled)
        }
    }

    fun completeProfile(
        displayName: String,
        username: String,
        city: String,
        bio: String,
        photoUrl: String?,
        onDone: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val res = deps.userRepository.updateProfile(displayName, username, city, bio, photoUrl, markCompleted = true)
            onDone(res)
        }
    }

    fun toggleBlockUser(targetUserId: String) {
        val currentBlocked = currentUserProfile.value?.blockedUserIds ?: emptyList()
        viewModelScope.launch {
            deps.userRepository.toggleBlockUser(targetUserId, currentBlocked)
        }
    }

    fun toggleSaveItem(itemType: String, itemId: String, title: String, subtitle: String, onDone: (Result<Boolean>) -> Unit = {}) {
        viewModelScope.launch {
            onDone(deps.userRepository.toggleSaveItem(itemType, itemId, title, subtitle))
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            deps.userRepository.markNotificationRead(id)
        }
    }

    fun toggleLessonCompleted(lessonId: String) {
        viewModelScope.launch {
            deps.preferencesRepository.toggleLessonCompleted(lessonId)
        }
    }

    fun recordQuizScore(percent: Int) {
        viewModelScope.launch {
            deps.preferencesRepository.recordQuizResult(percent)
        }
    }

    fun deleteAccount(onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                onResult(deps.userRepository.deleteAccountDataAndSignOut())
            } catch (e: CancellationException) {
                throw e
            }
        }
    }
}
