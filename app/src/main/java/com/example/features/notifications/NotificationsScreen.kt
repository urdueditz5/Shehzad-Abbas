package com.example.features.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.example.core.components.ApnaEmptyState
import com.example.core.components.ApnaSubScreenTopBar
import com.example.core.components.SafetyNoticeBanner
import com.example.core.constants.PakistanConstants
import com.example.core.i18n.LocalApnaStrings
import com.example.core.theme.RoyalBlue
import com.example.data.models.AppNotification

@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onMarkRead: (String) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalApnaStrings.current
    var selectedCat by remember { mutableStateOf("all") }

    val filtered = remember(notifications, selectedCat) {
        if (selectedCat == "all") notifications
        else notifications.filter { it.category.equals(selectedCat, ignoreCase = true) }
    }

    Scaffold(
        topBar = { ApnaSubScreenTopBar(title = strings.notificationsTitle, onBack = onBack) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SafetyNoticeBanner(text = strings.pushNotConfiguredBanner)
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PakistanConstants.notificationCategories) { cat ->
                        FilterChip(
                            selected = selectedCat == cat,
                            onClick = { selectedCat = cat },
                            label = { Text(cat.replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.testTag("notif_cat_$cat")
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    ApnaEmptyState(
                        title = "No notifications here",
                        subtitle = "Likes, comments, new followers, messages, and official announcements appear in real time."
                    )
                }
            } else {
                items(filtered, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.readStatus) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!item.readStatus) onMarkRead(item.id)
                            }
                            .testTag("notif_card_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = RoyalBlue)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(item.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(item.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = RoyalBlue)
                            }
                            if (!item.readStatus) {
                                Surface(shape = CircleShape, color = RoyalBlue, modifier = Modifier.size(10.dp)) {}
                            }
                        }
                    }
                }
            }
        }
    }
}
