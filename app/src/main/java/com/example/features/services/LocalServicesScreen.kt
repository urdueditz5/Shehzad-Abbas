package com.example.features.services

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.core.components.ApnaEmptyState
import com.example.core.components.ApnaErrorState
import com.example.core.components.ApnaSubScreenTopBar
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
import com.example.data.models.ServiceListing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class LocalServicesViewModel(private val deps: ApnaDependencies) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<ServiceListing>>>(UiState.Loading)
    val state: StateFlow<UiState<List<ServiceListing>>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            deps.listingsRepository.observeServices()
                .catch { err ->
                    val root = unwrapFirestoreError(err)
                    handleFirestoreError(root, OperationType.LIST, "service_listings")
                    _state.value = UiState.Error(root.localizedMessage ?: "Failed to load local services.")
                }
                .collect { items ->
                    _state.value = UiState.Success(items)
                }
        }
    }
}

@Composable
fun LocalServicesScreen(
    viewModel: LocalServicesViewModel,
    deps: ApnaDependencies,
    currentUserId: String,
    blockedUserIds: List<String>,
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
    var reportService by remember { mutableStateOf<ServiceListing?>(null) }
    var reportReason by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            ApnaSubScreenTopBar(title = strings.localServicesTitle, onBack = onBack)
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
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search tutors, plumbers, designers, repairs...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("services_search_input")
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PakistanConstants.serviceCategories) { cat ->
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
                    val filtered = current.data.filter { srv ->
                        srv.providerId !in blockedUserIds &&
                            (selectedCategory == "All" || srv.category == selectedCategory) &&
                            (selectedCity == "All Cities" || srv.city == selectedCity) &&
                            (q.isBlank() || srv.title.lowercase().contains(q) ||
                                srv.description.lowercase().contains(q) || srv.city.lowercase().contains(q))
                    }

                    if (filtered.isEmpty()) {
                        item {
                            ApnaEmptyState(
                                title = "No local services listed yet",
                                subtitle = "Offer tutoring, computer repair, design, or maintenance skills from the Create tab.",
                                actionLabel = "Offer a Service",
                                onAction = onOpenCreate
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { srv ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AssistChip(onClick = {}, label = { Text(srv.category) })
                                        Text(
                                            srv.priceEstimate,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = RoyalBlue,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(srv.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${srv.city} • ${srv.description}", style = MaterialTheme.typography.bodySmall)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            UserAvatar(name = srv.providerName, photoUrl = srv.providerPhotoUrl, size = 32.dp)
                                            Spacer(Modifier.width(8.dp))
                                            Text(srv.providerName, style = MaterialTheme.typography.labelMedium)
                                        }
                                        Row {
                                            IconButton(onClick = { reportService = srv }) {
                                                Icon(Icons.Default.Flag, contentDescription = "Report service")
                                            }
                                            if (srv.providerId != currentUserId) {
                                                OutlinedButton(
                                                    onClick = { onStartChat(srv.providerId, srv.providerName) },
                                                    modifier = Modifier.testTag("service_chat_${srv.id}")
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Contact")
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

    reportService?.let { srv ->
        AlertDialog(
            onDismissRequest = { reportService = null },
            title = { Text("Report Service") },
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
                    val id = srv.id
                    reportService = null
                    reportReason = ""
                    scope.launch {
                        deps.socialRepository.submitReport("service", id, reason)
                    }
                }) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = { reportService = null }) { Text("Cancel") }
            }
        )
    }
}
