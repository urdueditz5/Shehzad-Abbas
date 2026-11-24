package com.example.features.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.components.UserAvatar
import com.example.core.i18n.LocalApnaStrings
import com.example.core.theme.DeepNavy
import com.example.core.theme.ElectricCyan
import com.example.core.theme.RoyalBlue
import com.example.data.models.AppAnnouncement
import com.example.data.models.AppNotification
import com.example.data.models.PostItem
import com.example.data.models.PrivateUser
import com.example.data.models.SavedItem
import com.example.data.models.ShortVideo

enum class ServiceCategory(val tag: String, val label: String) {
    ALL("all", "All"),
    CONNECT("connect", "Connect"),
    LEARN_WORK("learn_work", "Learn & Work"),
    LOCAL_SHOP("local_shop", "Local & Shop"),
    TOOLS_AI("tools_ai", "Tools & AI")
}

data class HomeServiceItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: ServiceCategory,
    val icon: ImageVector,
    val accent: Color,
    val onClick: () -> Unit
)

@Composable
fun HomeDashboardScreen(
    user: PrivateUser?,
    announcements: List<AppAnnouncement>,
    notifications: List<AppNotification>,
    savedItems: List<SavedItem>,
    recentPosts: List<PostItem>,
    recentVideos: List<ShortVideo>,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onOpenSocial: () -> Unit,
    onOpenVideos: () -> Unit,
    onOpenMessaging: () -> Unit,
    onOpenEducation: () -> Unit,
    onOpenJobs: () -> Unit,
    onOpenMarketplace: () -> Unit,
    onOpenLocalServices: () -> Unit,
    onOpenNews: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenAiAssistant: () -> Unit
) {
    val strings = LocalApnaStrings.current

    var selectedCategory by rememberSaveable { mutableStateOf(ServiceCategory.ALL) }

    val services = remember(
        strings,
        onOpenSocial,
        onOpenVideos,
        onOpenMessaging,
        onOpenEducation,
        onOpenJobs,
        onOpenMarketplace,
        onOpenLocalServices,
        onOpenNews,
        onOpenTools,
        onOpenAiAssistant
    ) {
        listOf(
            HomeServiceItem("social", strings.socialTitle, "Posts & community", ServiceCategory.CONNECT, Icons.Default.Public, RoyalBlue, onOpenSocial),
            HomeServiceItem("videos", strings.videosTitle, "Reels & creators", ServiceCategory.CONNECT, Icons.Default.PlayCircle, Color(0xFFE11D48), onOpenVideos),
            HomeServiceItem("messaging", strings.messagingTitle, "1-to-1 secure chat", ServiceCategory.CONNECT, Icons.AutoMirrored.Filled.Chat, Color(0xFF0EA5E9), onOpenMessaging),
            HomeServiceItem("education", strings.educationTitle, "Matric to University", ServiceCategory.LEARN_WORK, Icons.Default.School, Color(0xFF10B981), onOpenEducation),
            HomeServiceItem("jobs", strings.jobsTitle, "Govt, Private & Intern", ServiceCategory.LEARN_WORK, Icons.Default.Work, Color(0xFFF59E0B), onOpenJobs),
            HomeServiceItem("marketplace", strings.marketplaceTitle, "Buy & sell locally", ServiceCategory.LOCAL_SHOP, Icons.Default.Storefront, Color(0xFF8B5CF6), onOpenMarketplace),
            HomeServiceItem("services", strings.localServicesTitle, "Tutors & technicians", ServiceCategory.LOCAL_SHOP, Icons.Default.Handyman, Color(0xFF14B8A6), onOpenLocalServices),
            HomeServiceItem("news", strings.newsTitle, "Pakistan updates", ServiceCategory.LOCAL_SHOP, Icons.Default.Newspaper, Color(0xFF6366F1), onOpenNews),
            HomeServiceItem("tools", strings.toolsTitle, "Calc, Notes, QR & more", ServiceCategory.TOOLS_AI, Icons.Default.Build, Color(0xFFEC4899), onOpenTools),
            HomeServiceItem("ai", strings.aiTitle, "Urdu & English Gemini", ServiceCategory.TOOLS_AI, Icons.Default.AutoAwesome, ElectricCyan, onOpenAiAssistant)
        )
    }

    val filteredServices = remember(services, selectedCategory) {
        if (selectedCategory == ServiceCategory.ALL) {
            services
        } else {
            services.filter { it.category == selectedCategory }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Horizontal Scrollable Category Filter Chip Row at Top
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp)
                .testTag("home_category_chip_row"),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ServiceCategory.entries, key = { it.tag }) { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = {
                        Text(
                            text = category.label,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .testTag("home_cat_chip_${category.tag}")
                        .semantics { selected = isSelected }
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("home_dashboard_grid"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

        // Dashboard Header & Search Bar
        item(span = { GridItemSpan(maxLineSpan) }) {
            HomeHeaderCard(
                user = user,
                greeting = strings.welcomeGreeting,
                tagline = strings.tagline,
                searchHint = strings.searchHint,
                onSearchClick = onSearchClick
            )
        }

        // Trending Quick Shortcuts
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.trendingTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        listOf(
                            "Matric & FSc Prep" to onOpenEducation,
                            "Ask Urdu AI" to onOpenAiAssistant,
                            "Tech & Govt Jobs" to onOpenJobs,
                            "Marla/Kanal Tool" to onOpenTools,
                            "Local Services" to onOpenLocalServices
                        )
                    ) { (label, action) ->
                        AssistChip(
                            onClick = action,
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        // Section Header for Service Grid
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedCategory == ServiceCategory.ALL) {
                        strings.servicesTitle
                    } else {
                        "${strings.servicesTitle} • ${selectedCategory.label}"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("home_services_title")
                )
                if (selectedCategory != ServiceCategory.ALL) {
                    TextButton(
                        onClick = { selectedCategory = ServiceCategory.ALL },
                        modifier = Modifier.testTag("home_clear_category_filter")
                    ) {
                        Text("Show all")
                    }
                }
            }
        }

        // Lazy Grid of Material 3 Service Cards
        items(filteredServices, key = { it.id }) { service ->
            ServiceTileCard(item = service)
        }

        // Official Announcements Preview
        if (announcements.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.announcementsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    announcements.take(2).forEach { ann ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = RoyalBlue
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(ann.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        ann.body,
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

        // Notifications Preview Card
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.notificationsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onNotificationsClick) {
                        Text("View all (${notifications.count { !it.readStatus }})")
                    }
                }
                if (notifications.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = RoyalBlue)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "You're all caught up! Activity from likes, comments, chats, and announcements will appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val latest = notifications.first()
                    ElevatedCard(
                        onClick = onNotificationsClick,
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(latest.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                latest.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Recent Activity & Quick Stats
        if (recentPosts.isNotEmpty() || recentVideos.isNotEmpty() || savedItems.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.recentSavedTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryStatCard("Posts", recentPosts.size.toString(), onOpenSocial, Modifier.weight(1f))
                        SummaryStatCard("Videos", recentVideos.size.toString(), onOpenVideos, Modifier.weight(1f))
                        SummaryStatCard("Saved", savedItems.size.toString(), onOpenEducation, Modifier.weight(1f))
                    }
                }
            }
        }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun HomeHeaderCard(
    user: PrivateUser?,
    greeting: String,
    tagline: String,
    searchHint: String,
    onSearchClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_header_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(178.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner_1795522450345),
                contentDescription = "ApnaHub Hero Banner",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                DeepNavy.copy(alpha = 0.90f),
                                DeepNavy.copy(alpha = 0.50f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$greeting, ${user?.displayName?.split(" ")?.firstOrNull() ?: "Friend"}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = listOfNotNull(
                                user?.username?.takeIf { it.isNotBlank() }?.let { "@$it" },
                                user?.city?.takeIf { it.isNotBlank() }
                            ).joinToString(" • ").ifBlank { tagline },
                            style = MaterialTheme.typography.bodySmall,
                            color = ElectricCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    UserAvatar(
                        name = user?.displayName ?: "A",
                        photoUrl = user?.photoUrl,
                        size = 48.dp
                    )
                }

                // Material 3 Search Bar Pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.96f),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onSearchClick)
                        .testTag("home_search_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search ApnaHub",
                                tint = DeepNavy
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = searchHint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF475569),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = RoyalBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceTileCard(
    item: HomeServiceItem,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = item.onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("service_card_${item.id}")
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
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(item.accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = item.accent,
                        modifier = Modifier.size(23.dp)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SummaryStatCard(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = RoyalBlue)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
