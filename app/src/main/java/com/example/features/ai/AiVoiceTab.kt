package com.example.features.ai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class PendingAudioAction {
    TRANSCRIBE_RECORD,
    LIVE_VOICE
}

@Composable
fun AiVoiceTab(
    transcriptionState: TranscriptionUiState,
    liveVoiceState: LiveVoiceUiState,
    onRecordingStarted: () -> Unit,
    onRecordingError: (String) -> Unit,
    onTranscribeBytes: (ByteArray) -> Unit,
    onSendTranscriptToChat: (String) -> Unit,
    onStartLiveVoice: () -> Unit,
    onStopLiveVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }
    var pendingAction by remember { mutableStateOf<PendingAudioAction?>(null) }

    fun beginAudioRecording() {
        permissionDeniedMessage = null
        val outFile = File(context.cacheDir, "apna_voice_${System.currentTimeMillis()}.m4a")
        try {
            val recorder = createMediaRecorder(context).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16_000)
                setAudioEncodingBitRate(64_000)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            activeRecorder = recorder
            recordingFile = outFile
            onRecordingStarted()
        } catch (e: Exception) {
            runCatching { outFile.delete() }
            activeRecorder = null
            recordingFile = null
            onRecordingError(e.localizedMessage ?: "Unable to access microphone for recording.")
        }
    }

    fun stopAndTranscribeRecording() {
        val recorder = activeRecorder
        val outFile = recordingFile
        activeRecorder = null
        recordingFile = null
        if (recorder == null || outFile == null) return

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    recorder.stop()
                    recorder.release()
                    outFile.readBytes()
                }.also {
                    runCatching { outFile.delete() }
                }
            }
            result.onSuccess { bytes ->
                onTranscribeBytes(bytes)
            }.onFailure { err ->
                runCatching { recorder.release() }
                onRecordingError(err.localizedMessage ?: "Failed to save audio recording.")
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val action = pendingAction
        pendingAction = null
        if (!granted) {
            permissionDeniedMessage = "Microphone permission is required for voice transcription and Gemini Live conversations."
            return@rememberLauncherForActivityResult
        }
        permissionDeniedMessage = null
        when (action) {
            PendingAudioAction.TRANSCRIBE_RECORD -> beginAudioRecording()
            PendingAudioAction.LIVE_VOICE -> onStartLiveVoice()
            null -> Unit
        }
    }

    fun runWithMicPermission(action: PendingAudioAction) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            permissionDeniedMessage = null
            when (action) {
                PendingAudioAction.TRANSCRIBE_RECORD -> beginAudioRecording()
                PendingAudioAction.LIVE_VOICE -> onStartLiveVoice()
            }
        } else {
            pendingAction = action
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activeRecorder?.let { rec ->
                runCatching { rec.stop() }
                runCatching { rec.release() }
            }
            recordingFile?.let { file -> runCatching { file.delete() } }
            activeRecorder = null
            recordingFile = null
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (permissionDeniedMessage != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.testTag("ai_mic_permission_error")
                ) {
                    Text(
                        text = permissionDeniedMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // 1. Speech-to-Text Transcription
            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "Audio Transcription",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Record English, Urdu, or Roman Urdu speech and transcribe it cleanly with Gemini 3.8 Flash.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val isRecording = transcriptionState is TranscriptionUiState.Recording
                    val isBusy = transcriptionState is TranscriptionUiState.Transcribing

                    Button(
                        enabled = !isBusy,
                        onClick = {
                            if (isRecording) {
                                stopAndTranscribeRecording()
                            } else {
                                runWithMicPermission(PendingAudioAction.TRANSCRIBE_RECORD)
                            }
                        },
                        colors = if (isRecording) {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        } else {
                            ButtonDefaults.buttonColors()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ai_transcribe_button")
                    ) {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            when {
                                isRecording -> "Stop & Transcribe"
                                isBusy -> "Transcribing Audio..."
                                else -> "Record Audio to Transcribe"
                            }
                        )
                    }

                    when (transcriptionState) {
                        is TranscriptionUiState.Idle -> Unit
                        is TranscriptionUiState.Recording -> {
                            Text(
                                text = "Listening via microphone... tap Stop when finished.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.testTag("ai_transcription_status")
                            )
                        }

                        is TranscriptionUiState.Transcribing -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.testTag("ai_transcription_status")
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text("Transcribing recorded audio...", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        is TranscriptionUiState.Success -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_transcription_result")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = transcriptionState.transcript,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    OutlinedButton(
                                        onClick = { onSendTranscriptToChat(transcriptionState.transcript) },
                                        modifier = Modifier.testTag("ai_transcription_send_to_chat")
                                    ) {
                                        Text("Ask in AI Chat")
                                    }
                                }
                            }
                        }

                        is TranscriptionUiState.Error -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.testTag("ai_transcription_error")
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = transcriptionState.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // 2. Real-time Bidirectional Gemini Live Voice Conversation
            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "Gemini Live Voice Chat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Real-time two-way spoken conversation with instant audio replies and live transcripts (gemini-3.1-flash-live-preview).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val isLiveActive = liveVoiceState is LiveVoiceUiState.Active
                    val isLiveConnecting = liveVoiceState is LiveVoiceUiState.Connecting

                    Button(
                        enabled = !isLiveConnecting,
                        onClick = {
                            if (isLiveActive) {
                                onStopLiveVoice()
                            } else {
                                runWithMicPermission(PendingAudioAction.LIVE_VOICE)
                            }
                        },
                        colors = if (isLiveActive) {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        } else {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ai_live_voice_button")
                    ) {
                        Icon(
                            if (isLiveActive) Icons.Default.CallEnd else Icons.Default.GraphicEq,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            when {
                                isLiveConnecting -> "Connecting Live Session..."
                                isLiveActive -> "End Live Voice Call"
                                else -> "Start Live Voice Conversation"
                            }
                        )
                    }

                    when (liveVoiceState) {
                        is LiveVoiceUiState.Idle -> Unit
                        is LiveVoiceUiState.Connecting -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text("Establishing bidirectional audio stream...", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        is LiveVoiceUiState.Active -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_live_transcript_panel")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (liveVoiceState.turns.isEmpty()) {
                                            "Live session active. Speak now — Gemini is listening."
                                        } else {
                                            "Live Transcript"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    liveVoiceState.turns.forEach { turn ->
                                        Text(
                                            text = "${if (turn.isUser) "You" else "Gemini"}: ${turn.text}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (turn.isUser) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        is LiveVoiceUiState.Error -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.testTag("ai_live_voice_error")
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = liveVoiceState.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun createMediaRecorder(context: Context): MediaRecorder {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
    } else {
        MediaRecorder()
    }
}
