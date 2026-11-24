package com.example.features.create

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.components.ApnaRemoteOrDataImage
import com.example.core.components.SafetyNoticeBanner
import com.example.core.constants.PakistanConstants
import com.example.core.firebase.ApnaDependencies
import com.example.core.i18n.LocalApnaStrings
import com.example.core.utils.MediaUtils
import com.example.data.models.PrivateUser
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHubScreen(
    user: PrivateUser?,
    deps: ApnaDependencies,
    isAdmin: Boolean,
    onPostCreated: () -> Unit,
    onVideoCreated: () -> Unit,
    onMarketCreated: () -> Unit,
    onServiceCreated: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalApnaStrings.current

    var mode by remember { mutableStateOf("Post") }
    val modes = listOf("Post", "Short Video", "Marketplace", "Local Service")

    var statusMsg by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Post state
    var postCaption by remember { mutableStateOf("") }
    var postCategory by remember { mutableStateOf("Community") }
    var postImageDataUri by remember { mutableStateOf<String?>(null) }

    // Video state
    var videoTitle by remember { mutableStateOf("") }
    var videoDesc by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
    var videoDuration by remember { mutableStateOf("30") }
    var localVideoWarning by remember { mutableStateOf<String?>(null) }

    // Marketplace state
    var marketTitle by remember { mutableStateOf("") }
    var marketDesc by remember { mutableStateOf("") }
    var marketPrice by remember { mutableStateOf("") }
    var marketCategory by remember { mutableStateOf("Electronics") }
    var marketCity by remember { mutableStateOf(user?.city ?: "Lahore") }
    var marketCondition by remember { mutableStateOf("Used") }
    var marketImageUri by remember { mutableStateOf<String?>(null) }

    // Local service state
    var srvTitle by remember { mutableStateOf("") }
    var srvDesc by remember { mutableStateOf("") }
    var srvCategory by remember { mutableStateOf("Education and tutoring") }
    var srvCity by remember { mutableStateOf(user?.city ?: "Lahore") }
    var srvPrice by remember { mutableStateOf("") }

    val postPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                MediaUtils.compressUriToDataUri(context, uri, maxDim = 720)
                    .onSuccess {
                        postImageDataUri = it
                        errorMsg = null
                    }
                    .onFailure { errorMsg = it.localizedMessage }
            }
        }
    }

    val marketPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                MediaUtils.compressUriToDataUri(context, uri, maxDim = 720)
                    .onSuccess {
                        marketImageUri = it
                        errorMsg = null
                    }
                    .onFailure { errorMsg = it.localizedMessage }
            }
        }
    }

    val videoFileChecker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val sizeBytes = MediaUtils.getUriFileSizeBytes(context, uri)
            val mb = if (sizeBytes != null) sizeBytes / (1024f * 1024f) else null
            localVideoWarning = if (sizeBytes != null && sizeBytes > PakistanConstants.MAX_VIDEO_UPLOAD_BYTES) {
                "Selected video is ${"%.1f".format(mb)} MB — exceeds the 15 MB limit. Compress to 720p H.264 before uploading to Firebase Storage, or paste a compressed HTTPS stream URL below."
            } else {
                "Video selected (${mb?.let { "%.1f MB".format(it) } ?: "verified size"}). Raw video file uploads require configuring Firebase Storage in the Firebase Console; enter a hosted HTTPS MP4/HLS stream URL below to publish immediately."
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Create & Publish", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(modes) { item ->
                    FilterChip(
                        selected = mode == item,
                        onClick = {
                            mode = item
                            errorMsg = null
                            statusMsg = null
                        },
                        label = { Text(item) },
                        modifier = Modifier.testTag("create_mode_${item.lowercase().replace(" ", "_")}")
                    )
                }
            }
        }

        if (isAdmin) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Admin Publishing", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Publish official announcements, jobs, or moderate reports.", style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(onClick = onOpenAdmin) {
                            Text("Open")
                        }
                    }
                }
            }
        }

        if (statusMsg != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(statusMsg.orEmpty(), modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (errorMsg != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(errorMsg.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        when (mode) {
            "Post" -> {
                item {
                    OutlinedTextField(
                        value = postCaption,
                        onValueChange = { if (it.length <= 2000) postCaption = it },
                        label = { Text("What's on your mind?") },
                        minLines = 4,
                        supportingText = { Text("${postCaption.length}/2000") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_post_caption_input")
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PakistanConstants.socialCategories.filter { it != "All" }) { cat ->
                            FilterChip(
                                selected = postCategory == cat,
                                onClick = { postCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = {
                            postPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.testTag("create_post_pick_image")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(if (postImageDataUri == null) "Attach Photo (Photo Picker)" else "Change Photo")
                    }
                    if (postImageDataUri != null) {
                        Spacer(Modifier.height(10.dp))
                        ApnaRemoteOrDataImage(
                            source = postImageDataUri!!,
                            contentDescription = "Selected post photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                item {
                    Button(
                        enabled = !isSubmitting,
                        onClick = {
                            isSubmitting = true
                            errorMsg = null
                            scope.launch {
                                val res = deps.socialRepository.createPost(
                                    authorName = user?.displayName ?: "User",
                                    authorUsername = user?.username ?: "user",
                                    authorPhotoUrl = user?.photoUrl,
                                    caption = postCaption,
                                    category = postCategory,
                                    imageUrl = postImageDataUri
                                )
                                isSubmitting = false
                                res.onSuccess {
                                    postCaption = ""
                                    postImageDataUri = null
                                    statusMsg = "Post published to Social Feed!"
                                    onPostCreated()
                                }.onFailure { errorMsg = it.localizedMessage }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("publish_post_button")
                    ) {
                        if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text("Publish Post")
                    }
                }
            }

            "Short Video" -> {
                item {
                    SafetyNoticeBanner(
                        text = "Storage & Upload Policy: Large videos are never auto-uploaded. Keep short clips under 15 MB (720p H.264). Provide a valid HTTPS MP4/HLS URL to stream with Media3 ExoPlayer."
                    )
                }
                item {
                    OutlinedButton(
                        onClick = {
                            videoFileChecker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        },
                        modifier = Modifier.testTag("check_local_video_button")
                    ) {
                        Icon(Icons.Default.VideoFile, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Check Local Video Size")
                    }
                    if (localVideoWarning != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(localVideoWarning.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                item {
                    OutlinedTextField(
                        value = videoTitle,
                        onValueChange = { if (it.length <= 140) videoTitle = it },
                        label = { Text("Video Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = videoUrl,
                        onValueChange = { videoUrl = it },
                        label = { Text("HTTPS Video Stream URL (.mp4 / .m3u8)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = videoDuration,
                            onValueChange = { if (it.all { ch -> ch.isDigit() }) videoDuration = it.take(3) },
                            label = { Text("Duration (sec)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = videoDesc,
                        onValueChange = { if (it.length <= 600) videoDesc = it },
                        label = { Text("Description") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Button(
                        enabled = !isSubmitting,
                        onClick = {
                            isSubmitting = true
                            errorMsg = null
                            scope.launch {
                                val res = deps.videoRepository.publishShortVideo(
                                    creatorName = user?.displayName ?: "Creator",
                                    creatorUsername = user?.username ?: "creator",
                                    creatorPhotoUrl = user?.photoUrl,
                                    title = videoTitle,
                                    description = videoDesc,
                                    videoUrl = videoUrl,
                                    durationSec = videoDuration.toLongOrNull() ?: 30L
                                )
                                isSubmitting = false
                                res.onSuccess {
                                    videoTitle = ""
                                    videoDesc = ""
                                    videoUrl = ""
                                    statusMsg = "Short video published!"
                                    onVideoCreated()
                                }.onFailure { errorMsg = it.localizedMessage }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("publish_video_button")
                    ) {
                        Text("Publish Short Video")
                    }
                }
            }

            "Marketplace" -> {
                item {
                    SafetyNoticeBanner(text = strings.safetyMarketplaceBanner)
                }
                item {
                    OutlinedTextField(
                        value = marketTitle,
                        onValueChange = { if (it.length <= 120) marketTitle = it },
                        label = { Text("Item Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = marketPrice,
                        onValueChange = { if (it.all { c -> c.isDigit() }) marketPrice = it.take(9) },
                        label = { Text("Price (PKR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    SimpleDropdownField(
                        label = "Category",
                        value = marketCategory,
                        options = PakistanConstants.marketplaceCategories.filter { it != "All" },
                        onSelect = { marketCategory = it }
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        SimpleDropdownField(
                            label = "City",
                            value = marketCity,
                            options = PakistanConstants.selectableCities,
                            onSelect = { marketCity = it },
                            modifier = Modifier.weight(1f)
                        )
                        SimpleDropdownField(
                            label = "Condition",
                            value = marketCondition,
                            options = PakistanConstants.marketplaceConditions,
                            onSelect = { marketCondition = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = marketDesc,
                        onValueChange = { if (it.length <= 1500) marketDesc = it },
                        label = { Text("Description & Pickup Area") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedButton(
                        onClick = {
                            marketPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(if (marketImageUri == null) "Add Product Photo" else "Change Photo")
                    }
                }
                item {
                    Button(
                        enabled = !isSubmitting,
                        onClick = {
                            isSubmitting = true
                            errorMsg = null
                            scope.launch {
                                val res = deps.listingsRepository.createMarketplaceListing(
                                    sellerName = user?.displayName ?: "Seller",
                                    sellerPhotoUrl = user?.photoUrl,
                                    title = marketTitle,
                                    description = marketDesc,
                                    category = marketCategory,
                                    pricePkr = marketPrice.toLongOrNull() ?: 0L,
                                    city = marketCity,
                                    condition = marketCondition,
                                    imageUrl = marketImageUri
                                )
                                isSubmitting = false
                                res.onSuccess {
                                    marketTitle = ""
                                    marketDesc = ""
                                    marketPrice = ""
                                    marketImageUri = null
                                    statusMsg = "Marketplace listing published!"
                                    onMarketCreated()
                                }.onFailure { errorMsg = it.localizedMessage }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("publish_marketplace_button")
                    ) {
                        Text("Publish Listing")
                    }
                }
            }

            "Local Service" -> {
                item {
                    OutlinedTextField(
                        value = srvTitle,
                        onValueChange = { if (it.length <= 120) srvTitle = it },
                        label = { Text("Service Title (e.g., FSc Home Tutor)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    SimpleDropdownField(
                        label = "Service Category",
                        value = srvCategory,
                        options = PakistanConstants.serviceCategories.filter { it != "All" },
                        onSelect = { srvCategory = it }
                    )
                }
                item {
                    SimpleDropdownField(
                        label = "City",
                        value = srvCity,
                        options = PakistanConstants.selectableCities,
                        onSelect = { srvCity = it }
                    )
                }
                item {
                    OutlinedTextField(
                        value = srvPrice,
                        onValueChange = { if (it.length <= 80) srvPrice = it },
                        label = { Text("Rate Estimate (e.g., PKR 2,000 / visit)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = srvDesc,
                        onValueChange = { if (it.length <= 1500) srvDesc = it },
                        label = { Text("Service Details & Availability") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Button(
                        enabled = !isSubmitting,
                        onClick = {
                            isSubmitting = true
                            errorMsg = null
                            scope.launch {
                                val res = deps.listingsRepository.createServiceListing(
                                    providerName = user?.displayName ?: "Provider",
                                    providerPhotoUrl = user?.photoUrl,
                                    title = srvTitle,
                                    category = srvCategory,
                                    city = srvCity,
                                    description = srvDesc,
                                    priceEstimate = srvPrice
                                )
                                isSubmitting = false
                                res.onSuccess {
                                    srvTitle = ""
                                    srvDesc = ""
                                    srvPrice = ""
                                    statusMsg = "Service listing published!"
                                    onServiceCreated()
                                }.onFailure { errorMsg = it.localizedMessage }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("publish_service_button")
                    ) {
                        Text("Publish Service")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}
