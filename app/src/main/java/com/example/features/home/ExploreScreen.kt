package com.example.features.home

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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.example.core.components.ApnaEmptyState
import com.example.core.components.UserAvatar
import com.example.core.theme.RoyalBlue
import com.example.data.models.JobListing
import com.example.data.models.MarketplaceListing
import com.example.data.models.PostItem
import com.example.data.models.PublicProfile
import com.example.data.models.ServiceListing

@Composable
fun ExploreScreen(
    posts: List<PostItem>,
    jobs: List<JobListing>,
    marketplace: List<MarketplaceListing>,
    services: List<ServiceListing>,
    profiles: List<PublicProfile>,
    onOpenSocial: () -> Unit,
    onOpenJobs: () -> Unit,
    onOpenMarketplace: () -> Unit,
    onOpenServices: () -> Unit,
    onOpenProfile: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("All") }
    val tabs = listOf("All", "People", "Posts", "Jobs", "Market", "Services")

    val q = query.trim().lowercase()
    val filteredProfiles = remember(profiles, q) {
        profiles.filter {
            q.isBlank() || it.displayName.lowercase().contains(q) || it.username.lowercase().contains(q) ||
                (it.city?.lowercase()?.contains(q) == true)
        }
    }
    val filteredPosts = remember(posts, q) {
        posts.filter {
            q.isBlank() || it.caption.lowercase().contains(q) || it.authorName.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
        }
    }
    val filteredJobs = remember(jobs, q) {
        jobs.filter {
            q.isBlank() || it.title.lowercase().contains(q) || it.company.lowercase().contains(q) ||
                it.city.lowercase().contains(q)
        }
    }
    val filteredMarket = remember(marketplace, q) {
        marketplace.filter {
            q.isBlank() || it.title.lowercase().contains(q) || it.city.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
        }
    }
    val filteredServices = remember(services, q) {
        services.filter {
            q.isBlank() || it.title.lowercase().contains(q) || it.city.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search across ApnaHub...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_search_input")
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tabs) { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab) },
                        modifier = Modifier.testTag("explore_tab_$tab")
                    )
                }
            }
        }

        if (selectedTab in listOf("All", "People") && filteredProfiles.isNotEmpty()) {
            item {
                Text("Community Members", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(filteredProfiles.take(6)) { profile ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenProfile(profile.userId) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(name = profile.displayName, photoUrl = profile.photoUrl, size = 42.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "@${profile.username}" + (profile.city?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (selectedTab in listOf("All", "Posts") && filteredPosts.isNotEmpty()) {
            item {
                Text("Social Posts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(filteredPosts.take(5)) { post ->
                ExploreCard(
                    title = post.authorName,
                    subtitle = post.caption,
                    badge = post.category,
                    onClick = onOpenSocial
                )
            }
        }

        if (selectedTab in listOf("All", "Jobs") && filteredJobs.isNotEmpty()) {
            item {
                Text("Jobs & Careers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(filteredJobs.take(5)) { job ->
                ExploreCard(
                    title = job.title,
                    subtitle = "${job.company} • ${job.city}",
                    badge = if (job.sampleListing) "Sample" else job.category,
                    onClick = onOpenJobs
                )
            }
        }

        if (selectedTab in listOf("All", "Market") && filteredMarket.isNotEmpty()) {
            item {
                Text("Marketplace", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(filteredMarket.take(5)) { item ->
                ExploreCard(
                    title = item.title,
                    subtitle = "PKR ${item.pricePkr} • ${item.city}",
                    badge = item.category,
                    onClick = onOpenMarketplace
                )
            }
        }

        if (selectedTab in listOf("All", "Services") && filteredServices.isNotEmpty()) {
            item {
                Text("Local Services", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(filteredServices.take(5)) { srv ->
                ExploreCard(
                    title = srv.title,
                    subtitle = "${srv.providerName} • ${srv.city} • ${srv.priceEstimate}",
                    badge = srv.category,
                    onClick = onOpenServices
                )
            }
        }

        val hasAny = filteredProfiles.isNotEmpty() || filteredPosts.isNotEmpty() ||
            filteredJobs.isNotEmpty() || filteredMarket.isNotEmpty() || filteredServices.isNotEmpty()

        if (!hasAny) {
            item {
                Spacer(Modifier.height(20.dp))
                ApnaEmptyState(
                    title = "No matching results",
                    subtitle = "Try another keyword or publish the first post, listing, or local service from the Create tab."
                )
            }
        }
    }
}

@Composable
private fun ExploreCard(
    title: String,
    subtitle: String,
    badge: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                color = RoyalBlue,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
