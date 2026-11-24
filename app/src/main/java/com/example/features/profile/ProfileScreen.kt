package com.example.features.profile

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.components.ApnaEmptyState
import com.example.core.components.ApnaErrorState
import com.example.core.components.ApnaSubScreenTopBar
import com.example.core.components.SkeletonLoadingList
import com.example.core.components.UserAvatar
import com.example.core.constants.PakistanConstants
import com.example.core.theme.DeepNavy
import com.example.core.theme.ElectricCyan
import com.example.core.theme.RoyalBlue
import com.example.data.models.FollowRecord
import com.example.data.models.PostItem
import com.example.data.models.SavedItem

@Composable
fun ProfileScreen(
    viewModel: UserProfileViewModel,
    myPosts: List<PostItem>,
    savedItems: List<SavedItem>,
    following: List<FollowRecord>,
    isAdmin: Boolean,
    onOpenSettings: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenUserProfile: (String) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    BackHandler(enabled = uiState.isEditing) {
        viewModel.cancelEditing()
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onPhotoUriPicked(context, uri)
        }
    }

    val launchImagePicker = {
        photoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    if (uiState.isLoading && uiState.user == null) {
        SkeletonLoadingList(count = 3)
        return
    }

    val user = uiState.user
    val displayedName = if (uiState.isEditing) {
        uiState.draftDisplayName.ifBlank { user?.displayName ?: "ApnaHub User" }
    } else {
        user?.displayName ?: "ApnaHub User"
    }
    val displayedPhoto = if (uiState.isEditing) uiState.draftPhotoUrl else user?.photoUrl

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("user_profile_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (uiState.successMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_success_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = uiState.successMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.dismissFeedback() },
                            modifier = Modifier.testTag("profile_dismiss_success")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss message"
                            )
                        }
                    }
                }
            }
        }

        // Main Profile Card (View + Inline Edit)
        item {
            ElevatedCard(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_header_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Decorative gradient header & overlapping avatar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(126.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(84.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(DeepNavy, RoyalBlue)
                                    )
                                )
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .size(92.dp)
                        ) {
                            UserAvatar(
                                name = displayedName,
                                photoUrl = displayedPhoto,
                                size = 88.dp,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    .testTag("profile_avatar")
                            )
                            Surface(
                                onClick = launchImagePicker,
                                shape = CircleShape,
                                color = RoyalBlue,
                                tonalElevation = 4.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 4.dp, y = 4.dp)
                                    .size(48.dp)
                                    .padding(6.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    .testTag("profile_avatar_edit_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (uiState.isProcessingPhoto) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Edit profile picture",
                                            tint = Color.White,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AnimatedVisibility(visible = !uiState.isEditing) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = user?.displayName ?: "ApnaHub User",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.testTag("profile_display_name")
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "@${user?.username ?: "member"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = RoyalBlue,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.testTag("profile_username_text")
                                    )
                                    val cityText = user?.city?.takeIf { it.isNotBlank() }
                                    if (cityText != null) {
                                        Text(
                                            text = " • ",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(2.dp))
                                        Text(
                                            text = cityText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.testTag("profile_city_text")
                                        )
                                    }
                                }

                                val bioText = user?.bio?.takeIf { it.isNotBlank() }
                                Text(
                                    text = bioText ?: "No bio added yet. Tap Edit Profile to introduce yourself.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (bioText != null) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.testTag("profile_bio_text")
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    ProfileStat("Posts", myPosts.size.toString())
                                    ProfileStat("Following", following.size.toString())
                                    ProfileStat("Saved", savedItems.size.toString())
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = { viewModel.startEditing() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("profile_edit_button")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Edit Profile")
                                    }
                                    OutlinedButton(
                                        onClick = onOpenSettings,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("profile_settings_button")
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Settings")
                                    }
                                }

                                if (uiState.errorMessage != null) {
                                    ApnaErrorState(message = uiState.errorMessage.orEmpty())
                                }
                            }
                        }

                        AnimatedVisibility(visible = uiState.isEditing) {
                            ProfileEditForm(
                                uiState = uiState,
                                onPickPhoto = launchImagePicker,
                                onRemovePhoto = { viewModel.removePhoto() },
                                onDisplayNameChanged = viewModel::onDisplayNameChanged,
                                onUsernameChanged = viewModel::onUsernameChanged,
                                onCityChanged = viewModel::onCityChanged,
                                onBioChanged = viewModel::onBioChanged,
                                onCancel = { viewModel.cancelEditing() },
                                onSave = { viewModel.saveProfile() }
                            )
                        }
                    }
                }
            }
        }

        // Quick Legal / Admin links
        item {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    ProfileMenuRow(
                        icon = Icons.Default.PrivacyTip,
                        label = "Privacy Policy",
                        onClick = onOpenPrivacy,
                        testTag = "profile_privacy_link"
                    )
                    ProfileMenuRow(
                        icon = Icons.Default.Gavel,
                        label = "Terms, Help & Monetization Info",
                        onClick = onOpenTerms,
                        testTag = "profile_terms_link"
                    )
                    if (isAdmin) {
                        ProfileMenuRow(
                            icon = Icons.Default.AdminPanelSettings,
                            label = "Admin Console",
                            onClick = onOpenAdmin,
                            testTag = "profile_admin_link"
                        )
                    }
                }
            }
        }

        item {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("My Posts") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Saved") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Following") })
            }
        }

        when (selectedTab) {
            0 -> {
                if (myPosts.isEmpty()) {
                    item {
                        ApnaEmptyState(
                            title = "No posts yet",
                            subtitle = "Posts you publish from Create appear here."
                        )
                    }
                } else {
                    items(myPosts, key = { it.id }) { post ->
                        ElevatedCard(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(post.category, style = MaterialTheme.typography.labelSmall, color = RoyalBlue)
                                Spacer(Modifier.height(4.dp))
                                Text(post.caption, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${post.likesCount} likes • ${post.commentsCount} comments",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                if (savedItems.isEmpty()) {
                    item {
                        ApnaEmptyState(
                            title = "No saved items",
                            subtitle = "Bookmark jobs, study notes, posts, or marketplace listings to review later."
                        )
                    }
                } else {
                    items(savedItems, key = { it.id }) { item ->
                        ElevatedCard(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, tint = RoyalBlue)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${item.itemType.uppercase()} • ${item.subtitle}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                if (following.isEmpty()) {
                    item {
                        ApnaEmptyState(
                            title = "Not following anyone yet",
                            subtitle = "Follow creators and community members from Social Feed or Short Videos."
                        )
                    }
                } else {
                    items(following, key = { it.id }) { follow ->
                        ElevatedCard(
                            onClick = { onOpenUserProfile(follow.targetUserId) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(name = follow.targetName, photoUrl = follow.targetPhotoUrl, size = 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = follow.targetName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditProfileRouteScreen(
    viewModel: UserProfileViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onPhotoUriPicked(context, uri)
        }
    }

    Scaffold(
        topBar = { ApnaSubScreenTopBar(title = "Edit Profile", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 540.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                UserAvatar(
                    name = uiState.draftDisplayName.ifBlank { uiState.user?.displayName ?: "A" },
                    photoUrl = uiState.draftPhotoUrl,
                    size = 88.dp,
                    modifier = Modifier.border(2.dp, ElectricCyan.copy(alpha = 0.6f), CircleShape)
                )

                ProfileEditForm(
                    uiState = uiState,
                    onPickPhoto = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemovePhoto = { viewModel.removePhoto() },
                    onDisplayNameChanged = viewModel::onDisplayNameChanged,
                    onUsernameChanged = viewModel::onUsernameChanged,
                    onCityChanged = viewModel::onCityChanged,
                    onBioChanged = viewModel::onBioChanged,
                    onCancel = onBack,
                    onSave = { viewModel.saveProfile(onSaved = onBack) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditForm(
    uiState: UserProfileUiState,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onCityChanged: (String) -> Unit,
    onBioChanged: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    var cityExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_edit_form"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onPickPhoto,
                enabled = !uiState.isProcessingPhoto && !uiState.isSaving,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("profile_pick_photo_button")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (uiState.isProcessingPhoto) "Processing..." else "Change Photo")
            }
            if (!uiState.draftPhotoUrl.isNullOrBlank()) {
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = onRemovePhoto,
                    enabled = !uiState.isProcessingPhoto && !uiState.isSaving,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("profile_remove_photo_button")
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        OutlinedTextField(
            value = uiState.draftDisplayName,
            onValueChange = onDisplayNameChanged,
            label = { Text("Display Name") },
            singleLine = true,
            isError = uiState.draftDisplayName.isBlank() && uiState.errorMessage != null,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_edit_name_input")
        )

        OutlinedTextField(
            value = uiState.draftUsername,
            onValueChange = onUsernameChanged,
            label = { Text("Username") },
            prefix = { Text("@") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_edit_username_input")
        )

        ExposedDropdownMenuBox(
            expanded = cityExpanded,
            onExpandedChange = { cityExpanded = !cityExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = uiState.draftCity,
                onValueChange = {},
                readOnly = true,
                label = { Text("City (Optional)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .testTag("profile_edit_city_input")
            )
            ExposedDropdownMenu(
                expanded = cityExpanded,
                onDismissRequest = { cityExpanded = false }
            ) {
                PakistanConstants.selectableCities.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onCityChanged(option)
                            cityExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = uiState.draftBio,
            onValueChange = onBioChanged,
            label = { Text("Bio") },
            minLines = 3,
            maxLines = 4,
            supportingText = { Text("${uiState.draftBio.length}/240") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_edit_bio_input")
        )

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("profile_edit_error")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("profile_cancel_edit_button")
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onSave,
                enabled = !uiState.isSaving && !uiState.isProcessingPhoto,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("profile_save_button")
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Changes")
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RoyalBlue)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = RoyalBlue)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
