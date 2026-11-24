package com.example.features.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.components.ApnaSubScreenTopBar
import com.example.core.i18n.LocalApnaStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    viewModel: AiViewModel,
    onBack: () -> Unit
) {
    val strings = LocalApnaStrings.current
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val selectedModelTier by viewModel.selectedModelTier.collectAsStateWithLifecycle()
    val selectedPersona by viewModel.selectedPersona.collectAsStateWithLifecycle()
    val selectedGrounding by viewModel.selectedGrounding.collectAsStateWithLifecycle()
    val imageGenState by viewModel.imageGenState.collectAsStateWithLifecycle()
    val editSourceBitmap by viewModel.imageEditSource.collectAsStateWithLifecycle()
    val aspectRatio by viewModel.imageAspectRatio.collectAsStateWithLifecycle()
    val transcriptionState by viewModel.transcriptionState.collectAsStateWithLifecycle()
    val liveVoiceState by viewModel.liveVoiceState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            ApnaSubScreenTopBar(
                title = strings.aiTitle,
                onBack = onBack,
                actions = {
                    if (selectedTab == AiStudioTab.CHAT && messages.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearConversation() },
                            modifier = Modifier.testTag("ai_clear_chat_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear AI conversation")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_studio_tabs")
            ) {
                AiStudioTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab.label) },
                        modifier = Modifier.testTag("ai_tab_${tab.name.lowercase()}")
                    )
                }
            }

            when (selectedTab) {
                AiStudioTab.CHAT -> AiChatTab(
                    messages = messages,
                    isGenerating = isGenerating,
                    selectedModelTier = selectedModelTier,
                    selectedPersona = selectedPersona,
                    selectedGrounding = selectedGrounding,
                    onSelectModelTier = viewModel::selectModelTier,
                    onSelectPersona = viewModel::selectPersona,
                    onSelectGrounding = viewModel::selectGrounding,
                    onSendPrompt = viewModel::sendPrompt
                )

                AiStudioTab.IMAGE -> AiImageTab(
                    imageGenState = imageGenState,
                    editSourceBitmap = editSourceBitmap,
                    selectedAspectRatio = aspectRatio,
                    onSelectEditSource = viewModel::setImageEditSource,
                    onSelectAspectRatio = viewModel::selectImageAspectRatio,
                    onGenerateOrEdit = viewModel::generateOrEditImage
                )

                AiStudioTab.VOICE -> AiVoiceTab(
                    transcriptionState = transcriptionState,
                    liveVoiceState = liveVoiceState,
                    onRecordingStarted = viewModel::onAudioRecordingStarted,
                    onRecordingError = viewModel::onAudioRecordingError,
                    onTranscribeBytes = viewModel::transcribeAudioBytes,
                    onSendTranscriptToChat = viewModel::sendTranscriptToChat,
                    onStartLiveVoice = viewModel::startLiveVoiceConversation,
                    onStopLiveVoice = viewModel::stopLiveVoiceConversation
                )
            }
        }
    }
}
