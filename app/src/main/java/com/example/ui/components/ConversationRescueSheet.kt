package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Support
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.PracticalFeaturesAiService
import com.example.data.model.ConversationRescueResult
import com.example.data.model.ConversationRescueSuggestion
import kotlinx.coroutines.launch

/**
 * Optional assistance button for AI speaking sessions (Voice Call, Video Tutor, Speak Tutor).
 * Opens Conversation Rescue without restarting or interrupting the ongoing session.
 */
@Composable
fun ConversationRescueButton(
    isOpen: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "conversation_rescue_button"
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(50),
        color = if (isOpen) Color(0xFFF59E0B) else Color(0xFF1E293B),
        modifier = modifier
            .border(
                width = 1.dp,
                color = Color(0xFFF59E0B).copy(alpha = 0.7f),
                shape = RoundedCornerShape(50)
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Support,
                contentDescription = "Conversation Rescue Assistance",
                tint = if (isOpen) Color(0xFF0F172A) else Color(0xFFFBBF24),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isOpen) "Hide Rescue" else "Conversation Rescue",
                color = if (isOpen) Color(0xFF0F172A) else Color(0xFFFDE68A),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Expandable in-session Conversation Rescue overlay/card.
 * Generates 3 contextually appropriate phrases, native-language explanations, TTS audio preview,
 * and allows the user to retry right in place without restarting the conversation.
 */
@Composable
fun ConversationRescuePanel(
    visible: Boolean,
    languageCode: String,
    lastPartnerPrompt: String,
    learnerStruggledDraft: String = "",
    sessionTitle: String = "Live AI Speaking Session",
    onSpeakPreview: (String) -> Unit,
    onRetryWithPhrase: (String) -> Unit,
    onPracticeLoadedIntoDraft: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var rescueError by remember { mutableStateOf<String?>(null) }
    var rescueResult by remember { mutableStateOf<ConversationRescueResult?>(null) }
    var selectedRescueTab by remember { mutableStateOf("Hint") }

    fun loadRescueSuggestions() {
        scope.launch {
            isLoading = true
            rescueError = null
            try {
                rescueResult = PracticalFeaturesAiService.generateConversationRescue(
                    languageCode = languageCode,
                    lastPartnerPrompt = lastPartnerPrompt,
                    learnerPartialOrStruggledDraft = learnerStruggledDraft,
                    sessionTopic = sessionTitle
                )
            } catch (e: Exception) {
                rescueError = com.example.ai.AiErrorHandler.handle(e, contextTag = "ConversationRescue").userMessage
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(visible, languageCode, lastPartnerPrompt) {
        if (visible) {
            loadRescueSuggestions()
        }
    }

    AnimatedVisibility(visible = visible) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .testTag("conversation_rescue_panel"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color(0xFFF59E0B).copy(alpha = 0.55f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Conversation Rescue",
                                color = Color(0xFFFDE68A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Hint, simpler rephrase, or translation without restarting",
                                color = Color(0xFFA8B3CF),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { loadRescueSuggestions() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "Refresh rescue suggestions",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Conversation Rescue",
                                tint = Color(0xFFA8B3CF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Clear 3-option Rescue Mode Bar: Hint | Simpler Rephrase | Translation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rescue_mode_selector"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Hint", "Simpler Rephrase", "Translation").forEach { mode ->
                        val isSelected = selectedRescueTab == mode
                        Surface(
                            onClick = { selectedRescueTab = mode },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFFF59E0B) else Color(0xFF1E293B),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rescue_tab_${mode.lowercase().replace(" ", "_")}")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode,
                                    color = if (isSelected) Color(0xFF0B0F1A) else Color(0xFFE2E8F0),
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFFFBBF24),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Building rescue help...",
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp
                        )
                    }
                } else if (!rescueError.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = rescueError!!,
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(onClick = { loadRescueSuggestions() }) {
                                Text("Retry", color = Color(0xFFFBBF24), fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    val result = rescueResult
                    if (result != null) {
                        when (selectedRescueTab) {
                            "Translation" -> {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Meaning & Context",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = result.conversationContextSummary,
                                            color = Color(0xFFF8FAFC),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        result.suggestions.firstOrNull()?.let { first ->
                                            Text(
                                                text = "Suggested Reply Meaning: ${first.nativeMeaning}",
                                                color = Color(0xFFA8B3CF),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                            "Simpler Rephrase" -> {
                                val simplePick = result.suggestions.minByOrNull { it.targetPhrase.length }
                                    ?: result.suggestions.firstOrNull()
                                if (simplePick != null) {
                                    RescueSuggestionCard(
                                        index = 0,
                                        suggestion = simplePick,
                                        onSpeakPreview = { onSpeakPreview(simplePick.targetPhrase) },
                                        onRetryNow = { onRetryWithPhrase(simplePick.targetPhrase) },
                                        onLoadDraft = onPracticeLoadedIntoDraft?.let { loader ->
                                            { loader(simplePick.targetPhrase) }
                                        }
                                    )
                                }
                            }
                            else -> {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1E293B)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = result.conversationContextSummary,
                                            color = Color(0xFFF8FAFC),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Hint: ${result.grammarBridgeTip}",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                result.suggestions.take(2).forEachIndexed { idx, suggestion ->
                                    RescueSuggestionCard(
                                        index = idx,
                                        suggestion = suggestion,
                                        onSpeakPreview = { onSpeakPreview(suggestion.targetPhrase) },
                                        onRetryNow = { onRetryWithPhrase(suggestion.targetPhrase) },
                                        onLoadDraft = onPracticeLoadedIntoDraft?.let { loader ->
                                            { loader(suggestion.targetPhrase) }
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
}

@Composable
private fun RescueSuggestionCard(
    index: Int,
    suggestion: ConversationRescueSuggestion,
    onSpeakPreview: () -> Unit,
    onRetryNow: () -> Unit,
    onLoadDraft: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.85f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rescue_suggestion_card_$index")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = suggestion.RegisterAndNuanceNote,
                        color = Color(0xFFFBBF24),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                IconButton(
                    onClick = onSpeakPreview,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Hear suggested phrase",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = suggestion.targetPhrase,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            if (suggestion.romanization.isNotBlank()) {
                Text(
                    text = suggestion.romanization,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
            Text(
                text = "Meaning: ${suggestion.nativeMeaning}",
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp
            )
            if (suggestion.whenToUseExplanation.isNotBlank()) {
                Text(
                    text = suggestion.whenToUseExplanation,
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRetryNow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B),
                        contentColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rescue_retry_button_$index")
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Retry in Conversation",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (onLoadDraft != null) {
                    OutlinedButton(
                        onClick = onLoadDraft,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("rescue_edit_draft_button_$index")
                    ) {
                        Text(
                            text = "Edit First",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
