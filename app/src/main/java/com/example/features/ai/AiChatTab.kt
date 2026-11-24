package com.example.features.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.repositories.AiCitationType
import com.example.data.repositories.AiGroundingMode
import com.example.data.repositories.AiModelTier
import com.example.data.repositories.AiPersona

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiChatTab(
    messages: List<AiChatBubble>,
    isGenerating: Boolean,
    selectedModelTier: AiModelTier,
    selectedPersona: AiPersona,
    selectedGrounding: AiGroundingMode,
    onSelectModelTier: (AiModelTier) -> Unit,
    onSelectPersona: (AiPersona) -> Unit,
    onSelectGrounding: (AiGroundingMode) -> Unit,
    onSendPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var prompt by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current

    val quickPrompts = remember(selectedGrounding) {
        when (selectedGrounding) {
            AiGroundingMode.GOOGLE_SEARCH -> listOf(
                "Latest PSL & Pakistan cricket schedule",
                "Current USD to PKR exchange rate trends",
                "Top scholarships in Pakistan open right now"
            )

            AiGroundingMode.GOOGLE_MAPS -> listOf(
                "Best family restaurants in Gulberg Lahore",
                "Libraries and study cafes in Islamabad F-6",
                "Hospitals near Clifton Karachi"
            )

            AiGroundingMode.NONE -> listOf(
                "Explain Newton's 2nd Law in simple Urdu + English",
                "Write a formal job application for a junior software role",
                "Translate to Roman Urdu: Consistency brings success",
                "Give 5 study tips for FSc board exams"
            )
        }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("ai_grounding_row")
                ) {
                    items(AiGroundingMode.entries) { mode ->
                        FilterChip(
                            selected = selectedGrounding == mode,
                            onClick = { onSelectGrounding(mode) },
                            label = { Text(mode.label) },
                            leadingIcon = when (mode) {
                                AiGroundingMode.GOOGLE_SEARCH -> {
                                    { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                }
                                AiGroundingMode.GOOGLE_MAPS -> {
                                    { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                }
                                AiGroundingMode.NONE -> null
                            },
                            modifier = Modifier.testTag("ai_grounding_chip_${mode.name.lowercase()}")
                        )
                    }
                    items(AiModelTier.entries) { tier ->
                        FilterChip(
                            selected = selectedModelTier == tier,
                            onClick = { onSelectModelTier(tier) },
                            label = { Text("${tier.label} (${tier.subtitle})") },
                            modifier = Modifier.testTag("ai_model_chip_${tier.name.lowercase()}")
                        )
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("ai_persona_row")
                ) {
                    items(AiPersona.entries) { persona ->
                        FilterChip(
                            selected = selectedPersona == persona,
                            onClick = { onSelectPersona(persona) },
                            label = { Text(persona.label) },
                            modifier = Modifier.testTag("ai_persona_chip_${persona.name.lowercase()}")
                        )
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("ai_chat_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    "ApnaHub Gemini Studio",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "Chat in English, اردو, or Roman Urdu. Switch between Standard, Google Search, or Google Maps grounding and choose Fast, Balanced, or Reasoning models.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickPrompts) { qp ->
                            AssistChip(
                                onClick = { onSendPrompt(qp) },
                                label = { Text(qp) }
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { bubble ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (bubble.fromUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when {
                            bubble.isError -> MaterialTheme.colorScheme.errorContainer
                            bubble.fromUser -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surface
                        },
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .widthIn(max = 320.dp)
                            .testTag("ai_bubble_${bubble.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!bubble.fromUser && bubble.text.isBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.size(8.dp))
                                    Text("Thinking...", style = MaterialTheme.typography.bodySmall)
                                }
                            } else {
                                Text(
                                    text = bubble.text,
                                    color = when {
                                        bubble.isError -> MaterialTheme.colorScheme.onErrorContainer
                                        bubble.fromUser -> MaterialTheme.colorScheme.onPrimary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            if (bubble.citations.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    bubble.citations.take(5).forEach { citation ->
                                        AssistChip(
                                            onClick = {
                                                runCatching { uriHandler.openUri(citation.uri) }
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = if (citation.type == AiCitationType.MAPS) {
                                                        Icons.Default.LocationOn
                                                    } else {
                                                        Icons.Default.Public
                                                    },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = citation.title,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(tonalElevation = 3.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("Ask anything in English, Urdu, or Roman Urdu...") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_prompt_input")
                )
                IconButton(
                    enabled = !isGenerating && prompt.isNotBlank(),
                    onClick = {
                        val q = prompt
                        prompt = ""
                        onSendPrompt(q)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("ai_send_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send prompt",
                        tint = if (!isGenerating && prompt.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}
