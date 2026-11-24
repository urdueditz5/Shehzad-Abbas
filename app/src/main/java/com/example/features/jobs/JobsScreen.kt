package com.example.features.jobs

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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BusinessCenter
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.core.components.ApnaEmptyState
import com.example.core.components.ApnaErrorState
import com.example.core.components.ApnaSubScreenTopBar
import com.example.core.components.SafetyNoticeBanner
import com.example.core.components.SkeletonLoadingList
import com.example.core.constants.PakistanConstants
import com.example.core.firebase.ApnaDependencies
import com.example.core.firebase.OperationType
import com.example.core.firebase.UiState
import com.example.core.firebase.handleFirestoreError
import com.example.core.firebase.unwrapFirestoreError
import com.example.core.i18n.LocalApnaStrings
import com.example.core.theme.RoyalBlue
import com.example.core.theme.WarningAmber
import com.example.data.models.JobListing
import com.example.data.models.SavedItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

private val sampleDemoJobs = listOf(
    JobListing(
        id = "sample_job_1",
        title = "Junior Android Developer (Sample)",
        company = "Tech Bridge Pakistan (Demo)",
        city = "Lahore",
        category = "Private",
        qualification = "Bachelor",
        salaryRange = "PKR 80,000 - 120,000 (Sample)",
        description = "Sample demo listing illustrating Kotlin & Jetpack Compose internship-to-junior role details.",
        applicationInstructions = "Sample demo only: in live listings, review employer domain and apply through official portals.",
        sampleListing = true,
        reviewStatus = "sample_demo"
    ),
    JobListing(
        id = "sample_job_2",
        title = "IT Support & Lab Assistant (Sample)",
        company = "Provincial Public College (Demo)",
        city = "Islamabad",
        category = "Government",
        qualification = "Intermediate",
        salaryRange = "BPS-11 Scale (Sample)",
        description = "Sample demo listing showing how government/public-sector notices appear when published by an admin.",
        applicationInstructions = "Sample demo only: always confirm government roles on official .gov.pk portals.",
        sampleListing = true,
        reviewStatus = "sample_demo"
    ),
    JobListing(
        id = "sample_job_3",
        title = "UI/UX & Graphic Design Intern (Sample)",
        company = "Karachi Digital Studio (Demo)",
        city = "Karachi",
        category = "Internship",
        qualification = "Bachelor",
        salaryRange = "PKR 35,000 Stipend (Sample)",
        description = "Sample demo internship focused on mobile design systems and Figma prototyping.",
        applicationInstructions = "Sample demo only — never pay registration fees for job interviews.",
        sampleListing = true,
        reviewStatus = "sample_demo"
    )
)

class JobsViewModel(private val deps: ApnaDependencies) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<JobListing>>>(UiState.Loading)
    val state: StateFlow<UiState<List<JobListing>>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            deps.listingsRepository.observeJobs()
                .catch { err ->
                    val root = unwrapFirestoreError(err)
                    handleFirestoreError(root, OperationType.LIST, "jobs")
                    _state.value = UiState.Error(root.localizedMessage ?: "Failed to load jobs.")
                }
                .collect { items ->
                    _state.value = UiState.Success(items)
                }
        }
    }
}

@Composable
fun JobsScreen(
    viewModel: JobsViewModel,
    savedItems: List<SavedItem>,
    onToggleSave: (String, String, String, String) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalApnaStrings.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedCity by remember { mutableStateOf("All Cities") }
    var selectedQual by remember { mutableStateOf("All") }
    var showSampleDemos by remember { mutableStateOf(true) }
    var activeJob by remember { mutableStateOf<JobListing?>(null) }

    val savedJobIds = remember(savedItems) {
        savedItems.filter { it.itemType == "job" }.map { it.itemId }.toSet()
    }

    Scaffold(
        topBar = {
            ApnaSubScreenTopBar(title = strings.jobsTitle, onBack = onBack)
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
                SafetyNoticeBanner(
                    text = "Career Safety: ApnaHub never charges fees for jobs. Verify official government roles on .gov.pk websites; sample listings are clearly labeled as unverified demos."
                )
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search job title, company, or city...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("jobs_search_input")
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PakistanConstants.jobCategories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            modifier = Modifier.testTag("jobs_cat_$cat")
                        )
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PakistanConstants.jobQualifications) { qual ->
                        FilterChip(
                            selected = selectedQual == qual,
                            onClick = { selectedQual = qual },
                            label = { Text(qual) }
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

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Include Sample Demo Listings", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = showSampleDemos,
                        onCheckedChange = { showSampleDemos = it },
                        modifier = Modifier.testTag("jobs_sample_toggle")
                    )
                }
            }

            when (val current = state) {
                UiState.Loading -> {
                    item { SkeletonLoadingList(3) }
                }
                is UiState.Error -> {
                    item { ApnaErrorState(message = current.message, onRetry = { viewModel.refresh() }) }
                }
                is UiState.Success -> {
                    val combined = if (showSampleDemos && current.data.isEmpty()) {
                        sampleDemoJobs
                    } else if (showSampleDemos) {
                        current.data + sampleDemoJobs
                    } else {
                        current.data
                    }

                    val q = searchQuery.trim().lowercase()
                    val filtered = combined.filter { job ->
                        (selectedCategory == "All" || job.category.equals(selectedCategory, ignoreCase = true)) &&
                            (selectedCity == "All Cities" || job.city.equals(selectedCity, ignoreCase = true)) &&
                            (selectedQual == "All" || job.qualification.equals(selectedQual, ignoreCase = true)) &&
                            (q.isBlank() || job.title.lowercase().contains(q) ||
                                job.company.lowercase().contains(q) || job.city.lowercase().contains(q))
                    }

                    if (filtered.isEmpty()) {
                        item {
                            ApnaEmptyState(
                                title = "No jobs match your filters",
                                subtitle = "Adjust category, city, or qualification filters, or ask an administrator to publish new job opportunities."
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { job ->
                            val isSaved = job.id in savedJobIds
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeJob = job }
                                    .testTag("job_card_${job.id}")
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
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            AssistChip(onClick = {}, label = { Text(job.category) })
                                            if (job.sampleListing || job.reviewStatus == "sample_demo") {
                                                Surface(
                                                    color = WarningAmber.copy(alpha = 0.16f),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = "Sample Demo — Unverified",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = WarningAmber,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                        IconButton(onClick = {
                                            onToggleSave("job", job.id, job.title, "${job.company} • ${job.city}")
                                        }) {
                                            Icon(
                                                imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = "Save job",
                                                tint = if (isSaved) RoyalBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(job.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.BusinessCenter, contentDescription = null, tint = RoyalBlue)
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "${job.company} • ${job.city} • ${job.qualification}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (job.salaryRange.isNotBlank()) {
                                        Text(job.salaryRange, style = MaterialTheme.typography.labelMedium, color = RoyalBlue)
                                    }
                                    Text(
                                        text = job.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
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

    activeJob?.let { job ->
        AlertDialog(
            onDismissRequest = { activeJob = null },
            title = { Text(job.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${job.company} • ${job.city}", style = MaterialTheme.typography.labelLarge, color = RoyalBlue)
                    Text("Category: ${job.category} | Qualification: ${job.qualification}", style = MaterialTheme.typography.bodySmall)
                    if (job.salaryRange.isNotBlank()) {
                        Text("Compensation: ${job.salaryRange}", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(job.description, style = MaterialTheme.typography.bodyMedium)
                    if (job.applicationInstructions.isNotBlank()) {
                        Text("How to Apply:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(job.applicationInstructions, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onToggleSave("job", job.id, job.title, "${job.company} • ${job.city}")
                    activeJob = null
                }) {
                    Text(if (job.id in savedJobIds) "Saved" else "Save Job")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeJob = null }) {
                    Text("Close")
                }
            }
        )
    }
}
