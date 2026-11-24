package com.example.features.marketplace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.core.components.ApnaEmptyState
import com.example.core.components.ApnaErrorState
import com.example.core.components.ApnaRemoteOrDataImage
import com.example.core.components.ApnaSubScreenTopBar
import com.example.core.components.SafetyNoticeBanner
import com.example.core.components.SkeletonLoadingList
import com.example.core.components.UserAvatar
import com.example.core.constants.PakistanConstants
import com.example.core.firebase.ApnaDependencies
import com.example.core.firebase.OperationType
import com.example.core.firebase.UiState
import com.example.core.firebase.handleFirestoreError
import com.example.core.firebase.unwrapFirestoreError
import com.example.core.i18n.LocalApnaStrings
import com.example.core.theme.RoyalBlue
import com.example.data.models.MarketplaceListing
import com.example.data.models.SavedItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MarketplaceViewModel(private val deps: ApnaDependencies) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<MarketplaceListing>>>(UiState.Loading)
    val state: StateFlow<UiState<List<MarketplaceListing>>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            deps.listingsRepository.observeMarketplace()
                .catch { err ->
                    val root = unwrapFirestoreError(err)
                    handleFirestoreError(root, OperationType.LIST, "marketplace_listings")
                    _state.value = UiState.Error(root.localizedMessage ?: "Failed to load marketplace.")
                }
                .collect { items ->
                    _state.value = UiState.Success(items)
                }
        }
    }
}

@Composable
fun MarketplaceScreen(
    viewModel: MarketplaceViewModel,
    deps: ApnaDependencies,
    currentUserId: String,
    blockedUserIds: List<String>,
    savedItems: List<SavedItem>,
    onToggleSave: (String, String, String, String) -> Unit,
    onStartChat: (String, String) -> Unit,
    onOpenCreate: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalApnaStrings.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedCity by remember { mutableStateOf("All Cities") }
    var detailItem by remember { mutableStateOf<MarketplaceListing?>(null) }
    var reportItem by remember { mutableStateOf<MarketplaceListing?>(null) }
    var reportReason by remember { mutableStateOf("") }

    val savedIds = remember(savedItems) {
        savedItems.filter { it.itemType == "marketplace" }.map { it.itemId }.toSet()
    }

    Scaffold(
        topBar = {
            ApnaSubScreenTopBar(title = strings.marketplaceTitle, onBack = onBack)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SafetyNoticeBanner(text = strings.safetyMarketplaceBanner)
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search phones, laptops, books, furniture...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("marketplace_search_input")
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PakistanConstants.marketplaceCategories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PakistanConstants.cities.take(8)) { city ->
                        FilterChip(
                            selected = selectedCity == city,
                            onClick = { selectedCity = city },
                            label = { Text(city) }
                        )
                    }
                }
            }

            when (val current = state) {
                UiState.Loading -> item { SkeletonLoadingList(3) }
                is UiState.Error -> item { ApnaErrorState(current.message, onRetry = { viewModel.refresh() }) }
                is UiState.Success -> {
                    val q = query.trim().lowercase()
                    val filtered = current.data.filter { item ->
                        item.sellerId !in blockedUserIds &&
                            (selectedCategory == "All" || item.category == selectedCategory) &&
                            (selectedCity == "All Cities" || item.city == selectedCity) &&
                            (q.isBlank() || item.title.lowercase().contains(q) ||
                                item.description.lowercase().contains(q) || item.city.lowercase().contains(q))
                    }

                    if (filtered.isEmpty()) {
                        item {
                            ApnaEmptyState(
                                title = "No Marketplace listings yet",
                                subtitle = "List an item from the Create tab to sell locally in Pakistan.",
                                actionLabel = "Create Listing",
                                onAction = onOpenCreate
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { listing ->
                            val isSaved = listing.id in savedIds
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { detailItem = listing }
                                    .testTag("market_card_${listing.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (!listing.imageUrl.isNullOrBlank()) {
                                        ApnaRemoteOrDataImage(
                                            source = listing.imageUrl,
                                            contentDescription = listing.title,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(listing.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(
                                                "PKR ${listing.pricePkr} • ${listing.condition} • ${listing.city}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = RoyalBlue
                                            )
                                        }
                                        IconButton(onClick = {
                                            onToggleSave("marketplace", listing.id, listing.title, "PKR ${listing.pricePkr} • ${listing.city}")
                                        }) {
                                            Icon(
                                                if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = "Save listing",
                                                tint = if (isSaved) RoyalBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        listing.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            UserAvatar(name = listing.sellerName, photoUrl = listing.sellerPhotoUrl, size = 30.dp)
                                            Spacer(Modifier.width(8.dp))
                                            Text(listing.sellerName, style = MaterialTheme.typography.labelMedium)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            IconButton(onClick = { reportItem = listing }) {
                                                Icon(Icons.Default.Flag, contentDescription = "Report listing")
                                            }
                                            if (listing.sellerId != currentUserId) {
                                                OutlinedButton(
                                                    onClick = { onStartChat(listing.sellerId, listing.sellerName) },
                                                    modifier = Modifier.testTag("market_chat_${listing.id}")
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Message")
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
    }

    detailItem?.let { listing ->
        AlertDialog(
            onDismissRequest = { detailItem = null },
            title = { Text(listing.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PKR ${listing.pricePkr} (${listing.condition})", style = MaterialTheme.typography.titleSmall, color = RoyalBlue)
                    Text("${listing.category} • ${listing.city}", style = MaterialTheme.typography.bodySmall)
                    Text(listing.description, style = MaterialTheme.typography.bodyMedium)
                    Text("Seller: ${listing.sellerName}", style = MaterialTheme.typography.labelMedium)
                }
            },
            confirmButton = {
                if (listing.sellerId != currentUserId) {
                    Button(onClick = {
                        val sellerId = listing.sellerId
                        val sellerName = listing.sellerName
                        detailItem = null
                        onStartChat(sellerId, sellerName)
                    }) {
                        Text("Message Seller")
                    }
                } else {
                    Button(onClick = { detailItem = null }) { Text("Close") }
                }
            },
            dismissButton = {
                TextButton(onClick = { detailItem = null }) { Text("Dismiss") }
            }
        )
    }

    reportItem?.let { item ->
        AlertDialog(
            onDismissRequest = { reportItem = null },
            title = { Text("Report Listing") },
            text = {
                OutlinedTextField(
                    value = reportReason,
                    onValueChange = { reportReason = it },
                    label = { Text("Reason for report") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val reason = reportReason
                    val id = item.id
                    reportItem = null
                    reportReason = ""
                    scope.launch {
                        deps.socialRepository.submitReport("marketplace", id, reason)
                    }
                }) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = { reportItem = null }) { Text("Cancel") }
            }
        )
    }
}
