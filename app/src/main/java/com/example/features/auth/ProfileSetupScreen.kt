package com.example.features.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.components.ApnaSubScreenTopBar
import com.example.core.components.UserAvatar
import com.example.core.constants.PakistanConstants
import com.example.core.utils.MediaUtils
import com.example.data.models.PrivateUser
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    initialUser: PrivateUser,
    isEditMode: Boolean,
    onSave: (displayName: String, username: String, city: String, bio: String, photoUrl: String?, onDone: (Result<Unit>) -> Unit) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var displayName by remember(initialUser.userId) { mutableStateOf(initialUser.displayName) }
    var username by remember(initialUser.userId) { mutableStateOf(initialUser.username) }
    var city by remember(initialUser.userId) { mutableStateOf(initialUser.city ?: "Lahore") }
    var bio by remember(initialUser.userId) { mutableStateOf(initialUser.bio ?: "") }
    var photoUrl by remember(initialUser.userId) { mutableStateOf(initialUser.photoUrl) }
    var cityExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                MediaUtils.compressUriToDataUri(context, uri, maxDim = 360)
                    .onSuccess { photoUrl = it }
                    .onFailure { errorMsg = it.localizedMessage }
            }
        }
    }

    Scaffold(
        topBar = {
            if (isEditMode && onBack != null) {
                ApnaSubScreenTopBar(title = "Edit Profile", onBack = onBack)
            }
        }
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
                if (!isEditMode) {
                    Text(
                        text = "Complete Your ApnaHub Profile",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Only minimal profile details are collected. City and bio are optional.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                UserAvatar(name = displayName, photoUrl = photoUrl, size = 84.dp)
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.testTag("pick_avatar_button")
                ) {
                    Text("Choose Profile Photo")
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    prefix = { Text("@") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_username_input")
                )

                ExposedDropdownMenuBox(
                    expanded = cityExpanded,
                    onExpandedChange = { cityExpanded = !cityExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("City (Optional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = cityExpanded,
                        onDismissRequest = { cityExpanded = false }
                    ) {
                        PakistanConstants.selectableCities.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    city = option
                                    cityExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 240) bio = it },
                    label = { Text("Short Bio (Optional)") },
                    minLines = 3,
                    maxLines = 4,
                    supportingText = { Text("${bio.length}/240") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_bio_input")
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            errorMsg = null
                            isSaving = true
                            onSave(displayName, username, city, bio, photoUrl) { res ->
                                isSaving = false
                                res.onSuccess {
                                    onBack?.invoke()
                                }.onFailure {
                                    errorMsg = it.localizedMessage ?: "Failed to save profile"
                                }
                            }
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("save_profile_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (isEditMode) "Save Changes" else "Get Started")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
