package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.AiErrorHandler
import com.example.ai.AiUiError
import com.example.ai.DebateRubricEvaluation
import com.example.ai.DebateTurnEntry
import com.example.ai.DynamicDebateTopic
import com.example.ai.DynamicRepairScenario
import com.example.ai.DynamicRepairTurnEvaluation
import com.example.ai.ExploreAiLearningEngine
import com.example.ai.ExploreAiResult
import com.example.ai.ExploreLearnerContext
import com.example.ai.ExploreSessionRecord
import com.example.ai.PracticalFeaturesAiService
import com.example.ai.UpgradeSheetMode
import com.example.data.model.Language
import com.example.data.model.PracticalFeaturesCatalog
import com.example.data.model.TeachAiTurnEvaluation
import com.example.data.model.UserProfile
import com.example.data.repository.AccessPolicyFeatureKeys
import com.example.data.repository.OfflineEmergencyPhrasePack
import com.example.data.repository.OfflineEmergencyPhraseRepository
import com.example.speech.SpeechManager
import com.example.ui.components.LingoCalmAiErrorBanner
import com.example.ui.components.LingoCleanCard
import com.example.ui.components.LingoEmptyOrErrorCard
import com.example.ui.components.LingoGoldLockIcon
import com.example.ui.components.LingoStatusBadge
import com.example.ui.components.LingoTokens
import com.example.ui.components.LingoTopBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun labTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = LingoTokens.textPrimary,
    unfocusedTextColor = LingoTokens.textPrimary,
    focusedBorderColor = LingoTokens.accentPrimary,
    unfocusedBorderColor = LingoTokens.borderSubtle,
    focusedContainerColor = LingoTokens.bgSubtle,
    unfocusedContainerColor = LingoTokens.bgSubtle
)

@Composable
private fun labFilterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = LingoTokens.bgElevated,
    labelColor = LingoTokens.textSecondary,
    selectedContainerColor = LingoTokens.accentPrimary.copy(alpha = 0.16f),
    selectedLabelColor = LingoTokens.accentPrimary
)

private fun <T> exploreResultErrorText(result: ExploreAiResult<T>): String = when (result) {
    is ExploreAiResult.Offline -> "No internet connection detected. Connect to the internet and tap Retry."
    is ExploreAiResult.QuotaExceeded -> result.message
    is ExploreAiResult.Error -> result.message
    is ExploreAiResult.Success -> ""
}

// ═══════════════════════════════════════════════════════════════════════════════
// 11. CONVERSATION REPAIR LAB SCREEN (Dynamic AI Scenarios & Multi-Turn Repair)
// ═══════════════════════════════════════════════════════════════════════════════

private data class RepairDisplayTurn(
    val id: String,
    val speaker: String, // "partner" | "learner"
    val targetText: String,
    val nativeText: String = "",
    val score: Int? = null
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConversationRepairLabScreen(
    userProfile: UserProfile,
    speechManager: SpeechManager,
    completedScenarioIds: Set<String>,
    onScenarioResolved: (scenarioId: String, languageCode: String) -> Unit,
    onRecordMistake: (category: String, original: String, corrected: String, explanation: String) -> Unit = { _, _, _, _ -> },
    onBack: () -> Unit
) {
    BackHandler {
        speechManager.stopSpeaking()
        onBack()
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val targetLang = Language.findByCode(userProfile.targetLanguageCode)
    val learnerCtx = remember(userProfile) { ExploreLearnerContext.fromProfile(userProfile) }

    val breakdownFilters = remember {
        listOf(
            "Fast speech",
            "Unfamiliar vocabulary",
            "Regional accent",
            "Misunderstood instructions",
            "Asking to repeat",
            "Politely correcting",
            "Confirming information"
        )
    }

    var selectedBreakdown by remember(targetLang.code) { mutableStateOf(breakdownFilters.first()) }
    var activeScenario by remember(targetLang.code) { mutableStateOf<DynamicRepairScenario?>(null) }
    var recentSettings by remember(targetLang.code) { mutableStateOf<List<String>>(emptyList()) }
    var isGeneratingScenario by remember { mutableStateOf(false) }
    var scenarioError by remember { mutableStateOf<String?>(null) }

    var conversationTurns by remember(activeScenario?.id) { mutableStateOf<List<RepairDisplayTurn>>(emptyList()) }
    var repairInput by remember(activeScenario?.id) { mutableStateOf("") }
    var lastEvaluation by remember(activeScenario?.id) { mutableStateOf<DynamicRepairTurnEvaluation?>(null) }
    var evaluationError by remember(activeScenario?.id) { mutableStateOf<String?>(null) }
    var isEvaluating by remember { mutableStateOf(false) }
    var resolvedCount by remember { mutableIntStateOf(completedScenarioIds.size) }

    val isListening by speechManager.isListening.collectAsState()
    val spokenText by speechManager.spokenText.collectAsState()
    val speechError by speechManager.speechError.collectAsState()

    LaunchedEffect(spokenText) {
        if (spokenText.isNotBlank()) {
            repairInput = spokenText
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) speechManager.startListening(targetLang.code)
    }

    fun loadNewScenario(requestedBreakdown: String = selectedBreakdown) {
        if (isGeneratingScenario) return
        speechManager.stopSpeaking()
        isGeneratingScenario = true
        scenarioError = null
        evaluationError = null
        lastEvaluation = null
        repairInput = ""
        scope.launch {
            val res = ExploreAiLearningEngine.generateRepairScenario(
                context = context,
                learnerCtx = learnerCtx,
                requestedBreakdownType = requestedBreakdown,
                avoidSettings = recentSettings
            )
            when (res) {
                is ExploreAiResult.Success -> {
                    val sc = res.data
                    activeScenario = sc
                    recentSettings = (recentSettings + sc.settingTitle).takeLast(8)
                    conversationTurns = listOf(
                        RepairDisplayTurn(
                            id = "partner_init_${sc.id}",
                            speaker = "partner",
                            targetText = sc.breakdownPromptTarget,
                            nativeText = sc.breakdownPromptNative
                        )
                    )
                    val speed = if (sc.breakdownType.contains("Fast", ignoreCase = true)) 1.15f else 1.0f
                    speechManager.speakWithGender(
                        text = sc.breakdownPromptTarget,
                        languageCode = targetLang.code,
                        preferMale = sc.preferredVoiceGender.equals("male", ignoreCase = true),
                        speed = speed
                    )
                }
                else -> {
                    scenarioError = exploreResultErrorText(res)
                }
            }
            isGeneratingScenario = false
        }
    }

    LaunchedEffect(targetLang.code) {
        if (activeScenario == null && !isGeneratingScenario) {
            loadNewScenario(selectedBreakdown)
        }
    }

    fun submitRepairAttempt() {
        val sc = activeScenario ?: return
        val candidate = repairInput.trim()
        if (candidate.isEmpty() || isEvaluating) return
        isEvaluating = true
        evaluationError = null
        val priorHistory = conversationTurns.map { turn ->
            (if (turn.speaker == "partner") sc.speakerName else "Learner") to turn.targetText
        }
        scope.launch {
            val res = ExploreAiLearningEngine.evaluateRepairTurn(
                context = context,
                learnerCtx = learnerCtx,
                scenario = sc,
                history = priorHistory,
                learnerRepairAttempt = candidate
            )
            when (res) {
                is ExploreAiResult.Success -> {
                    val eval = res.data
                    lastEvaluation = eval
                    val updatedTurns = conversationTurns + listOf(
                        RepairDisplayTurn(
                            id = "learner_${System.currentTimeMillis()}",
                            speaker = "learner",
                            targetText = candidate,
                            score = eval.score
                        ),
                        RepairDisplayTurn(
                            id = "partner_${System.currentTimeMillis() + 1}",
                            speaker = "partner",
                            targetText = eval.speakerFollowUpTarget,
                            nativeText = eval.speakerFollowUpNative
                        )
                    )
                    conversationTurns = updatedTurns
                    repairInput = ""

                    eval.capturedMistake?.let { m ->
                        onRecordMistake(m.category, m.originalText, m.correctedText, m.explanation)
                    }

                    if (eval.speakerFollowUpTarget.isNotBlank()) {
                        speechManager.speakWithGender(
                            text = eval.speakerFollowUpTarget,
                            languageCode = targetLang.code,
                            preferMale = sc.preferredVoiceGender.equals("male", ignoreCase = true),
                            speed = 1.0f
                        )
                    }

                    if (eval.repairedSuccessfully) {
                        onScenarioResolved(sc.id, targetLang.code)
                        resolvedCount += 1
                    }

                    ExploreAiLearningEngine.saveExploreSession(
                        context = context,
                        record = ExploreSessionRecord(
                            sessionId = sc.id,
                            featureType = "conversation_repair",
                            languageCode = targetLang.code,
                            title = "${sc.settingTitle} (${sc.breakdownType})",
                            summary = eval.feedbackNative,
                            cefrLevel = sc.cefrLevel,
                            turnsCount = updatedTurns.count { it.speaker == "learner" },
                            scorePercent = eval.score
                        )
                    )
                }
                else -> {
                    evaluationError = exploreResultErrorText(res)
                }
            }
            isEvaluating = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LingoTokens.bgCanvas)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
    ) {
        LingoTopBar(
            title = "Conversation Repair Lab",
            subtitle = "${targetLang.name} • Live AI communication breakdown drills",
            onBack = {
                speechManager.stopSpeaking()
                onBack()
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("conversation_repair_lab_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                LingoCleanCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Breakdown Focus",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = LingoTokens.textPrimary
                                )
                                Text(
                                    text = "Resolved: $resolvedCount • CEFR ${learnerCtx.cefrLevel}",
                                    fontSize = 12.sp,
                                    color = LingoTokens.textSecondary
                                )
                            }
                            Button(
                                onClick = { loadNewScenario(selectedBreakdown) },
                                enabled = !isGeneratingScenario,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LingoTokens.accentBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("repair_new_scenario_btn")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isGeneratingScenario) "Generating..." else "New AI Scenario",
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            breakdownFilters.forEachIndexed { idx, filter ->
                                val selected = selectedBreakdown == filter
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        selectedBreakdown = filter
                                        loadNewScenario(filter)
                                    },
                                    label = {
                                        Text(
                                            text = filter,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = labFilterChipColors(),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = LingoTokens.borderSubtle,
                                        selectedBorderColor = LingoTokens.accentPrimary.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.testTag("repair_scenario_chip_$idx")
                                )
                            }
                        }
                    }
                }
            }

            if (isGeneratingScenario && activeScenario == null) {
                item {
                    LingoEmptyOrErrorCard(
                        title = "Generating Real-Life Breakdown",
                        message = "Creating a fresh ${targetLang.name} $selectedBreakdown scenario for CEFR ${learnerCtx.cefrLevel}...",
                        isLoading = true
                    )
                }
            }

            if (!scenarioError.isNullOrBlank()) {
                item {
                    LingoEmptyOrErrorCard(
                        title = "Could Not Load AI Scenario",
                        message = scenarioError!!,
                        isError = true,
                        actionLabel = "Retry",
                        onAction = { loadNewScenario(selectedBreakdown) }
                    )
                }
            }

            val sc = activeScenario
            if (sc != null) {
                item {
                    LingoCleanCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LingoStatusBadge(text = sc.breakdownType, color = LingoTokens.accentPrimary)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(
                                        onClick = {
                                            speechManager.speakWithGender(
                                                text = sc.breakdownPromptTarget,
                                                languageCode = targetLang.code,
                                                preferMale = sc.preferredVoiceGender.equals("male", ignoreCase = true),
                                                speed = 0.72f
                                            )
                                        },
                                        modifier = Modifier.testTag("repair_slow_replay_btn")
                                    ) {
                                        Text("Slow 0.7x", fontSize = 12.sp, color = LingoTokens.accentPrimary, maxLines = 1, softWrap = false)
                                    }
                                    IconButton(
                                        onClick = {
                                            speechManager.speakWithGender(
                                                text = sc.breakdownPromptTarget,
                                                languageCode = targetLang.code,
                                                preferMale = sc.preferredVoiceGender.equals("male", ignoreCase = true),
                                                speed = 1.0f
                                            )
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Hear speaker prompt",
                                            tint = LingoTokens.accentPrimary
                                        )
                                    }
                                }
                            }

                            Text(
                                text = sc.settingTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = LingoTokens.textPrimary
                            )
                            Text(
                                text = "${sc.speakerName} (${sc.speakerRole}) • ${sc.challengeNoteNative}",
                                fontSize = 13.sp,
                                color = LingoTokens.textSecondary
                            )
                            Text(
                                text = "Repair Goal: ${sc.repairGoalNative}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = LingoTokens.accentPrimary
                            )

                            // Live multi-turn dialogue exchange
                            conversationTurns.forEach { turn ->
                                val isPartner = turn.speaker == "partner"
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isPartner) LingoTokens.bgSubtle else LingoTokens.accentBlue.copy(alpha = 0.14f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isPartner) LingoTokens.borderSubtle else LingoTokens.accentBlue.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isPartner) sc.speakerName else "You${turn.score?.let { " • $it/100" } ?: ""}",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPartner) LingoTokens.accentAmber else LingoTokens.accentPrimary
                                            )
                                            IconButton(
                                                onClick = {
                                                    speechManager.speakWithGender(
                                                        text = turn.targetText,
                                                        languageCode = targetLang.code,
                                                        preferMale = isPartner && sc.preferredVoiceGender.equals("male", ignoreCase = true)
                                                    )
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "Hear turn",
                                                    tint = LingoTokens.accentPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = turn.targetText,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.5.sp,
                                            color = LingoTokens.textPrimary
                                        )
                                        if (turn.nativeText.isNotBlank()) {
                                            Text(
                                                text = turn.nativeText,
                                                fontSize = 12.5.sp,
                                                color = LingoTokens.textSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            if (sc.suggestedRepairStartersTarget.isNotEmpty()) {
                                Text(
                                    text = "Repair starters (tap to load & hear)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LingoTokens.textSecondary
                                )
                                sc.suggestedRepairStartersTarget.forEachIndexed { mIdx, starter ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = LingoTokens.accentPrimary.copy(alpha = 0.08f),
                                        border = BorderStroke(1.dp, LingoTokens.accentPrimary.copy(alpha = 0.28f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                repairInput = starter
                                                speechManager.speak(starter, targetLang.code)
                                            }
                                            .testTag("repair_model_phrase_$mIdx")
                                    ) {
                                        Text(
                                            text = starter,
                                            fontSize = 13.sp,
                                            color = LingoTokens.textPrimary,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = repairInput,
                                onValueChange = { repairInput = it },
                                placeholder = {
                                    Text(
                                        "Speak or type how you clarify or repair this in ${targetLang.name}...",
                                        color = LingoTokens.textMuted
                                    )
                                },
                                colors = labTextFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("repair_attempt_input"),
                                maxLines = 3
                            )

                            if (!speechError.isNullOrBlank()) {
                                Text(
                                    text = speechError!!,
                                    fontSize = 12.sp,
                                    color = LingoTokens.accentAmber
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (isListening) {
                                            speechManager.stopListening()
                                        } else {
                                            speechManager.clearSpokenText()
                                            val hasPerm = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (hasPerm) speechManager.startListening(targetLang.code)
                                            else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    border = BorderStroke(1.dp, LingoTokens.borderFocus),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (isListening) LingoTokens.accentAmber else LingoTokens.accentPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isListening) "Stop" else "Speak", maxLines = 1, softWrap = false)
                                }

                                Button(
                                    onClick = { submitRepairAttempt() },
                                    enabled = !isEvaluating && repairInput.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LingoTokens.accentBlue,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("repair_submit_button")
                                ) {
                                    Text(if (isEvaluating) "Evaluating..." else "Send Repair", maxLines = 1, softWrap = false)
                                }
                            }

                            if (!evaluationError.isNullOrBlank()) {
                                LingoEmptyOrErrorCard(
                                    title = "AI Evaluation Unavailable",
                                    message = evaluationError!!,
                                    isError = true,
                                    actionLabel = "Retry",
                                    onAction = { submitRepairAttempt() }
                                )
                            }

                            val eval = lastEvaluation
                            if (eval != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (eval.repairedSuccessfully) LingoTokens.accentEmerald.copy(alpha = 0.14f) else LingoTokens.accentAmber.copy(alpha = 0.14f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (eval.repairedSuccessfully) LingoTokens.accentEmerald.copy(alpha = 0.4f) else LingoTokens.accentAmber.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("repair_evaluation_card")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (eval.repairedSuccessfully) {
                                                "Misunderstanding Resolved (${eval.score}/100)"
                                            } else {
                                                "Keep Refining Your Repair (${eval.score}/100)"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            color = if (eval.repairedSuccessfully) LingoTokens.accentEmerald else LingoTokens.accentAmber
                                        )
                                        Text(
                                            text = eval.feedbackNative,
                                            fontSize = 13.sp,
                                            color = LingoTokens.textPrimary
                                        )
                                        if (eval.naturalRepairModelTarget.isNotBlank()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { speechManager.speak(eval.naturalRepairModelTarget, targetLang.code) },
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "Natural model: ${eval.naturalRepairModelTarget}",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = LingoTokens.accentPrimary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Icon(
                                                    Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "Hear natural model",
                                                    tint = LingoTokens.accentPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        if (eval.nextDifficultyHint.isNotBlank()) {
                                            Text(
                                                text = "Next step: ${eval.nextDifficultyHint}",
                                                fontSize = 12.sp,
                                                color = LingoTokens.textSecondary
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
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 5. LANGUAGE EMERGENCY MODE SCREEN (Real On-Device Offline Packs + Offline TTS)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun LanguageEmergencyModeScreen(
    userProfile: UserProfile,
    speechManager: SpeechManager,
    offlineDownloadedLangs: Set<String>,
    onToggleOfflinePack: (String) -> Unit,
    onPacksChanged: () -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler {
        speechManager.stopSpeaking()
        onBack()
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val targetLang = Language.findByCode(userProfile.targetLanguageCode)
    val learnerCtx = remember(userProfile) { ExploreLearnerContext.fromProfile(userProfile) }

    var savedPack by remember(targetLang.code) {
        mutableStateOf<OfflineEmergencyPhrasePack?>(
            OfflineEmergencyPhraseRepository.loadPack(context, targetLang.code)
        )
    }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember(targetLang.code) { mutableStateOf<String?>(null) }
    var offlineAudioNotice by remember(targetLang.code) { mutableStateOf<String?>(null) }
    var emergencySearchQuery by remember(targetLang.code) { mutableStateOf("") }
    var showFavoritesOnly by remember(targetLang.code) { mutableStateOf(false) }
    val favPrefs = remember(context) {
        context.getSharedPreferences("lingosphere_emergency_favs", android.content.Context.MODE_PRIVATE)
    }
    var favoritePhraseIds by remember(targetLang.code) {
        mutableStateOf(
            favPrefs.getStringSet("favs_${targetLang.code}", emptySet())?.toSet() ?: emptySet()
        )
    }
    var fullScreenPhrase by remember { mutableStateOf<com.example.data.repository.OfflineEmergencyPhrase?>(null) }
    val countryEmergencyOptions = remember {
        listOf(
            Triple("EU / Spain / France", "112", "General Emergency: 112 • Medical/Police/Fire"),
            Triple("US / Canada / Mexico", "911", "All Emergencies: 911"),
            Triple("United Kingdom", "999 / 112", "Emergency: 999 or 112 • Non-emergency medical: 111"),
            Triple("Japan", "110 / 119", "Police: 110 • Ambulance & Fire: 119"),
            Triple("Australia", "000", "Police / Fire / Ambulance: 000")
        )
    }
    var selectedCountryIdx by remember { mutableIntStateOf(0) }

    val offlineTtsAvailable = remember(targetLang.code) {
        speechManager.isOfflineTtsSupportedForLanguage(targetLang.code)
    }
    val allCategories = OfflineEmergencyPhraseRepository.REQUIRED_CATEGORIES
    val displayedPhrases = remember(savedPack, selectedCategory, emergencySearchQuery, showFavoritesOnly, favoritePhraseIds) {
        val list = savedPack?.phrases.orEmpty()
        list.filter { item ->
            val catOk = selectedCategory == null || item.category.equals(selectedCategory, ignoreCase = true)
            val favOk = !showFavoritesOnly || (item.id in favoritePhraseIds)
            val q = emergencySearchQuery.trim().lowercase()
            val searchOk = q.isEmpty() ||
                item.targetPhrase.lowercase().contains(q) ||
                item.nativeTranslation.lowercase().contains(q) ||
                item.category.lowercase().contains(q)
            catOk && favOk && searchOk
        }
    }

    fun downloadOfflinePack() {
        if (isDownloading) return
        isDownloading = true
        downloadError = null
        scope.launch {
            val res = ExploreAiLearningEngine.generateEmergencyOfflinePack(
                context = context,
                learnerCtx = learnerCtx
            )
            when (res) {
                is ExploreAiResult.Success -> {
                    savedPack = res.data
                    onPacksChanged()
                }
                else -> {
                    downloadError = exploreResultErrorText(res)
                }
            }
            isDownloading = false
        }
    }

    fun deleteOfflinePack() {
        OfflineEmergencyPhraseRepository.deletePack(context, targetLang.code)
        savedPack = null
        onPacksChanged()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LingoTokens.bgCanvas)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
    ) {
        LingoTopBar(
            title = "Emergency Phrase Mode",
            subtitle = "${targetLang.name} • On-device offline survival packs",
            onBack = {
                speechManager.stopSpeaking()
                onBack()
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("language_emergency_mode_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Honest Safety & Scope Disclaimer
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = LingoTokens.accentAmber.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, LingoTokens.accentAmber.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emergency_disclaimer_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = LingoTokens.accentAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Language practice aid only — not professional medical, legal, or emergency advice. In a real emergency, contact local emergency services immediately.",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = LingoTokens.textPrimary
                        )
                    }
                }
            }

            // Real On-Device Pack Status & Download Controls
            item {
                LingoCleanCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val pack = savedPack
                        val downloadedDate = remember(pack?.downloadedAtEpochMs) {
                            pack?.downloadedAtEpochMs?.let {
                                SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(it))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${targetLang.name} Offline Survival Pack",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp,
                                    color = LingoTokens.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (pack != null) {
                                        "Stored on device • ${pack.phrases.size} phrases across ${pack.categoryCount} categories${downloadedDate?.let { " • Saved $it" } ?: ""}"
                                    } else {
                                        "Not downloaded yet. Download once to store all 9 emergency categories locally for 100% offline use."
                                    },
                                    fontSize = 12.5.sp,
                                    color = if (pack != null) LingoTokens.accentEmerald else LingoTokens.textSecondary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { downloadOfflinePack() },
                                enabled = !isDownloading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (pack != null) LingoTokens.accentEmerald else LingoTokens.accentBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("emergency_offline_pack_toggle")
                            ) {
                                if (isDownloading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Downloading...", fontSize = 12.5.sp, maxLines = 1, softWrap = false)
                                } else {
                                    Icon(
                                        if (pack != null) Icons.Default.Refresh else Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (pack != null) "Refresh Pack" else "Download Offline Pack",
                                        fontSize = 12.5.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            if (pack != null) {
                                OutlinedButton(
                                    onClick = { deleteOfflinePack() },
                                    border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LingoTokens.textSecondary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("emergency_delete_pack_btn")
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete offline pack", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Honest Offline TTS Capability Indicator
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = LingoTokens.bgSubtle,
                            border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    if (offlineTtsAvailable) Icons.Default.CloudDone else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (offlineTtsAvailable) LingoTokens.accentEmerald else LingoTokens.accentPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (offlineTtsAvailable) {
                                        "Offline device voice for ${targetLang.name} is ready for no-signal audio playback."
                                    } else {
                                        "Offline Android TTS voice data for ${targetLang.name} is not installed on this device. Install ${targetLang.name} in Android Settings > Text-to-speech for offline voice playback; text & phonetic guides still work 100% offline."
                                    },
                                    fontSize = 12.sp,
                                    color = LingoTokens.textSecondary
                                )
                            }
                        }

                        if (!offlineAudioNotice.isNullOrBlank()) {
                            Text(
                                text = offlineAudioNotice!!,
                                fontSize = 12.sp,
                                color = LingoTokens.accentAmber
                            )
                        }
                    }
                }
            }

            if (!downloadError.isNullOrBlank()) {
                item {
                    LingoEmptyOrErrorCard(
                        title = "Offline Pack Download Failed",
                        message = downloadError!!,
                        isError = true,
                        actionLabel = "Retry Download",
                        onAction = { downloadOfflinePack() }
                    )
                }
            }

            // Country Emergency Numbers + Search Bar + Favorites Filter
            item {
                val activeCountry = countryEmergencyOptions[selectedCountryIdx.coerceIn(0, countryEmergencyOptions.lastIndex)]
                LingoCleanCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emergency_country_numbers_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Country Emergency Numbers",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = LingoTokens.textPrimary
                            )
                            LingoStatusBadge(
                                text = "Dial ${activeCountry.second}",
                                color = LingoTokens.accentRose
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            countryEmergencyOptions.forEachIndexed { idx, triple ->
                                FilterChip(
                                    selected = selectedCountryIdx == idx,
                                    onClick = { selectedCountryIdx = idx },
                                    label = { Text("${triple.first} (${triple.second})", fontSize = 11.5.sp) },
                                    colors = labFilterChipColors(),
                                    modifier = Modifier.testTag("emergency_country_chip_$idx")
                                )
                            }
                        }
                        Text(
                            text = activeCountry.third,
                            fontSize = 12.sp,
                            color = LingoTokens.textSecondary
                        )

                        OutlinedTextField(
                            value = emergencySearchQuery,
                            onValueChange = { emergencySearchQuery = it },
                            placeholder = {
                                Text("Search emergency phrases...", fontSize = 12.5.sp, color = LingoTokens.textMuted)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = LingoTokens.textSecondary)
                            },
                            colors = labTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("emergency_search_input")
                        )
                    }
                }
            }

            // All 9 Required Categories + Favorites Filter Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = showFavoritesOnly,
                        onClick = { showFavoritesOnly = !showFavoritesOnly },
                        label = { Text("Favorites (${favoritePhraseIds.size})", fontSize = 12.sp, maxLines = 1, softWrap = false) },
                        colors = labFilterChipColors(),
                        modifier = Modifier.testTag("emergency_fav_filter_chip")
                    )
                    FilterChip(
                        selected = selectedCategory == null && !showFavoritesOnly,
                        onClick = {
                            selectedCategory = null
                            showFavoritesOnly = false
                        },
                        label = { Text("All (9 Categories)", fontSize = 12.sp, maxLines = 1, softWrap = false) },
                        colors = labFilterChipColors(),
                        modifier = Modifier.testTag("emergency_cat_all")
                    )
                    allCategories.forEachIndexed { idx, cat ->
                        val selected = selectedCategory == cat
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp, maxLines = 1, softWrap = false) },
                            colors = labFilterChipColors(),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = LingoTokens.borderSubtle,
                                selectedBorderColor = LingoTokens.accentPrimary.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.testTag("emergency_cat_$idx")
                        )
                    }
                }
            }

            if (savedPack == null) {
                item {
                    LingoEmptyOrErrorCard(
                        title = "Download ${targetLang.name} Pack for Offline Access",
                        message = "Tap 'Download Offline Pack' above while online to save verified ${targetLang.name} survival phrases across all 9 emergency categories directly to your device storage.",
                        isLoading = isDownloading,
                        actionLabel = if (!isDownloading) "Download Now" else null,
                        onAction = if (!isDownloading) { { downloadOfflinePack() } } else null
                    )
                }
            } else if (displayedPhrases.isEmpty()) {
                item {
                    LingoEmptyOrErrorCard(
                        title = "No Phrases in This Category",
                        message = "Tap 'Refresh Pack' to regenerate all 9 categories for ${targetLang.name}."
                    )
                }
            } else {
                items(displayedPhrases, key = { it.id }) { phrase ->
                    LingoCleanCard(
                        borderColor = if (phrase.urgent) LingoTokens.accentAmber.copy(alpha = 0.45f) else LingoTokens.borderSubtle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("emergency_phrase_card_${phrase.id}")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LingoStatusBadge(
                                    text = if (phrase.urgent) "Urgent • ${phrase.category}" else phrase.category,
                                    color = if (phrase.urgent) LingoTokens.accentAmber else LingoTokens.accentPrimary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(
                                        onClick = {
                                            offlineAudioNotice = null
                                            speechManager.speakOfflineLocalOnly(
                                                text = phrase.targetPhrase,
                                                languageCode = targetLang.code,
                                                speed = 0.72f,
                                                onUnavailable = { offlineAudioNotice = it }
                                            )
                                        }
                                    ) {
                                        Text("Slow", fontSize = 12.sp, color = LingoTokens.accentPrimary, maxLines = 1, softWrap = false)
                                    }
                                    Button(
                                        onClick = {
                                            offlineAudioNotice = null
                                            speechManager.speakOfflineLocalOnly(
                                                text = phrase.targetPhrase,
                                                languageCode = targetLang.code,
                                                speed = 1.0f,
                                                onUnavailable = { offlineAudioNotice = it }
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = LingoTokens.accentBlue,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Listen", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                    }
                                }
                            }

                            Text(
                                text = phrase.targetPhrase,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = LingoTokens.textPrimary
                            )
                            if (phrase.phoneticGuide.isNotBlank()) {
                                Text(
                                    text = "Pronunciation: ${phrase.phoneticGuide}",
                                    fontSize = 12.5.sp,
                                    color = LingoTokens.accentPrimary
                                )
                            }
                            Text(
                                text = phrase.nativeTranslation,
                                fontSize = 13.5.sp,
                                color = LingoTokens.textPrimary
                            )
                            if (phrase.usageNote.isNotBlank()) {
                                Text(
                                    text = phrase.usageNote,
                                    fontSize = 12.sp,
                                    color = LingoTokens.textSecondary
                                )
                            }

                            val isFav = phrase.id in favoritePhraseIds
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        val updated = if (isFav) favoritePhraseIds - phrase.id else favoritePhraseIds + phrase.id
                                        favoritePhraseIds = updated
                                        favPrefs.edit().putStringSet("favs_${targetLang.code}", updated).apply()
                                    },
                                    modifier = Modifier.testTag("emergency_fav_btn_${phrase.id}")
                                ) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Favorite phrase",
                                        tint = if (isFav) LingoTokens.goldAccent else LingoTokens.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isFav) "Saved" else "Favorite",
                                        fontSize = 12.sp,
                                        color = if (isFav) LingoTokens.goldAccent else LingoTokens.textSecondary
                                    )
                                }

                                OutlinedButton(
                                    onClick = { fullScreenPhrase = phrase },
                                    border = BorderStroke(1.dp, LingoTokens.brandCyan.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LingoTokens.brandCyan),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("emergency_show_someone_btn_${phrase.id}")
                                ) {
                                    Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Show to someone", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val shownPhrase = fullScreenPhrase
    if (shownPhrase != null) {
        Dialog(
            onDismissRequest = { fullScreenPhrase = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                color = LingoTokens.bgCanvas,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("emergency_show_fullscreen_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LingoStatusBadge(
                            text = "${targetLang.name} • ${shownPhrase.category}",
                            color = LingoTokens.accentAmber
                        )
                        IconButton(
                            onClick = { fullScreenPhrase = null },
                            modifier = Modifier.testTag("emergency_close_fullscreen_btn")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close full screen", tint = LingoTokens.textPrimary)
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = shownPhrase.targetPhrase,
                            fontSize = 32.sp,
                            lineHeight = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = LingoTokens.textPrimary
                        )
                        if (shownPhrase.phoneticGuide.isNotBlank()) {
                            Text(
                                text = shownPhrase.phoneticGuide,
                                fontSize = 18.sp,
                                color = LingoTokens.brandCyan,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = shownPhrase.nativeTranslation,
                            fontSize = 18.sp,
                            color = LingoTokens.textSecondary
                        )
                    }

                    Button(
                        onClick = {
                            speechManager.speakOfflineLocalOnly(
                                text = shownPhrase.targetPhrase,
                                languageCode = targetLang.code,
                                speed = 0.9f,
                                onUnavailable = { offlineAudioNotice = it }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LingoTokens.accentBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Speak Out Loud", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 6. AI DEBATE PARTNER SCREEN (Custom/AI Topics, Multi-Round & Honest Rubric)
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiDebatePartnerScreen(
    userProfile: UserProfile,
    speechManager: SpeechManager,
    onRecordMistake: (category: String, original: String, corrected: String, explanation: String) -> Unit = { _, _, _, _ -> },
    onBack: () -> Unit
) {
    BackHandler {
        speechManager.stopSpeaking()
        onBack()
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val targetLang = Language.findByCode(userProfile.targetLanguageCode)
    val learnerCtx = remember(userProfile) { ExploreLearnerContext.fromProfile(userProfile) }

    var customTopicInput by remember(targetLang.code) { mutableStateOf("") }
    var activeTopic by remember(targetLang.code) { mutableStateOf<DynamicDebateTopic?>(null) }
    var recentTopicTitles by remember(targetLang.code) { mutableStateOf<List<String>>(emptyList()) }
    var learnerStance by remember(targetLang.code) { mutableStateOf("Pro") }
    var isGeneratingTopic by remember { mutableStateOf(false) }
    var topicError by remember { mutableStateOf<String?>(null) }

    var debateHistory by remember(activeTopic?.id) { mutableStateOf<List<DebateTurnEntry>>(emptyList()) }
    var lastQuickCoaching by remember(activeTopic?.id) { mutableStateOf<String?>(null) }
    var lastChallenge by remember(activeTopic?.id) { mutableStateOf<String?>(null) }
    var userArgument by remember(activeTopic?.id) { mutableStateOf("") }
    var currentDraftSpoken by remember(activeTopic?.id) { mutableStateOf(false) }
    var isSubmittingTurn by remember { mutableStateOf(false) }
    var turnError by remember { mutableStateOf<String?>(null) }

    var rubricEval by remember(activeTopic?.id) { mutableStateOf<DebateRubricEvaluation?>(null) }
    var isEvaluatingDebate by remember { mutableStateOf(false) }
    var evaluationError by remember { mutableStateOf<String?>(null) }
    var roundTimerSeconds by remember(activeTopic?.id, debateHistory.size) { mutableIntStateOf(60) }

    LaunchedEffect(activeTopic?.id, debateHistory.size, rubricEval == null) {
        roundTimerSeconds = 60
        while (roundTimerSeconds > 0 && rubricEval == null && activeTopic != null) {
            delay(1000L)
            roundTimerSeconds = (roundTimerSeconds - 1).coerceAtLeast(0)
        }
    }

    val isListening by speechManager.isListening.collectAsState()
    val spokenText by speechManager.spokenText.collectAsState()
    val speechError by speechManager.speechError.collectAsState()

    LaunchedEffect(spokenText) {
        if (spokenText.isNotBlank()) {
            userArgument = spokenText
            currentDraftSpoken = true
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) speechManager.startListening(targetLang.code)
    }

    fun requestTopic(customIdea: String? = null) {
        if (isGeneratingTopic) return
        speechManager.stopSpeaking()
        isGeneratingTopic = true
        topicError = null
        turnError = null
        evaluationError = null
        rubricEval = null
        scope.launch {
            val res = ExploreAiLearningEngine.generateDebateTopic(
                context = context,
                learnerCtx = learnerCtx,
                customTopicIdea = customIdea?.trim()?.takeIf { it.isNotBlank() },
                avoidTitles = recentTopicTitles
            )
            when (res) {
                is ExploreAiResult.Success -> {
                    activeTopic = res.data
                    recentTopicTitles = (recentTopicTitles + res.data.titleTarget).takeLast(8)
                    debateHistory = emptyList()
                    lastQuickCoaching = null
                    lastChallenge = null
                    userArgument = ""
                    currentDraftSpoken = false
                }
                else -> {
                    topicError = exploreResultErrorText(res)
                }
            }
            isGeneratingTopic = false
        }
    }

    LaunchedEffect(targetLang.code) {
        if (activeTopic == null && !isGeneratingTopic) {
            requestTopic(null)
        }
    }

    fun sendDebateTurn(isClarification: Boolean = false) {
        val topic = activeTopic ?: return
        val candidate = if (isClarification) {
            userArgument.trim().ifBlank { "Could you clarify your last point?" }
        } else {
            userArgument.trim()
        }
        if (candidate.isEmpty() || isSubmittingTurn) return
        isSubmittingTurn = true
        turnError = null
        val roundNum = debateHistory.count { it.speaker == "learner" } + 1
        val wasSpoken = currentDraftSpoken && !isClarification

        scope.launch {
            val res = ExploreAiLearningEngine.advanceDebateTurn(
                context = context,
                learnerCtx = learnerCtx,
                topic = topic,
                learnerStance = learnerStance,
                history = debateHistory,
                learnerArgument = candidate,
                isClarificationRequest = isClarification
            )
            when (res) {
                is ExploreAiResult.Success -> {
                    val reply = res.data
                    val learnerEntry = DebateTurnEntry(
                        id = "l_${System.currentTimeMillis()}",
                        speaker = "learner",
                        textTarget = candidate,
                        spokenViaMic = wasSpoken,
                        roundNumber = roundNum
                    )
                    val aiEntry = DebateTurnEntry(
                        id = "a_${System.currentTimeMillis() + 1}",
                        speaker = "ai",
                        textTarget = reply.counterArgumentTarget,
                        translationNative = reply.counterArgumentNative,
                        roundNumber = roundNum
                    )
                    debateHistory = debateHistory + listOf(learnerEntry, aiEntry)
                    lastQuickCoaching = reply.quickCoachingNote.takeIf { it.isNotBlank() }
                    lastChallenge = reply.followUpChallengeTarget.takeIf { it.isNotBlank() }
                    userArgument = ""
                    currentDraftSpoken = false
                    rubricEval = null

                    reply.capturedMistake?.let { m ->
                        onRecordMistake(m.category, m.originalText, m.correctedText, m.explanation)
                    }

                    speechManager.speak(reply.counterArgumentTarget, targetLang.code)
                }
                else -> {
                    turnError = exploreResultErrorText(res)
                }
            }
            isSubmittingTurn = false
        }
    }

    fun finishAndEvaluate() {
        val topic = activeTopic ?: return
        if (isEvaluatingDebate) return
        isEvaluatingDebate = true
        evaluationError = null
        scope.launch {
            val res = ExploreAiLearningEngine.evaluateCompletedDebate(
                context = context,
                learnerCtx = learnerCtx,
                topic = topic,
                learnerStance = learnerStance,
                history = debateHistory
            )
            when (res) {
                is ExploreAiResult.Success -> {
                    val eval = res.data
                    rubricEval = eval
                    eval.capturedMistakes.forEach { m ->
                        onRecordMistake(m.category, m.originalText, m.correctedText, m.explanation)
                    }
                    ExploreAiLearningEngine.saveExploreSession(
                        context = context,
                        record = ExploreSessionRecord(
                            sessionId = topic.id,
                            featureType = "ai_debate",
                            languageCode = targetLang.code,
                            title = topic.titleTarget,
                            summary = "Stance: $learnerStance • Overall ${eval.overallScore}/100 • Grammar ${eval.grammarScore}, Vocab ${eval.vocabularyScore}, Structure ${eval.argumentStructureScore}",
                            cefrLevel = topic.cefrLevel,
                            turnsCount = debateHistory.count { it.speaker == "learner" },
                            scorePercent = eval.overallScore
                        )
                    )
                }
                else -> {
                    evaluationError = exploreResultErrorText(res)
                }
            }
            isEvaluatingDebate = false
        }
    }

    val learnerTurnCount = debateHistory.count { it.speaker == "learner" }
    val hasAiTurn = debateHistory.any { it.speaker == "ai" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LingoTokens.bgCanvas)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
    ) {
        LingoTopBar(
            title = "AI Debate Partner",
            subtitle = "${targetLang.name} • Live multi-round debate & honest rubric",
            onBack = {
                speechManager.stopSpeaking()
                onBack()
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("ai_debate_partner_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Custom Topic + AI Topic Generator + Stance Selector
            item {
                LingoCleanCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Debate Setup (CEFR ${learnerCtx.cefrLevel})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = LingoTokens.textPrimary
                        )
                        OutlinedTextField(
                            value = customTopicInput,
                            onValueChange = { customTopicInput = it },
                            placeholder = {
                                Text(
                                    "Enter any debate topic of your choice...",
                                    color = LingoTokens.textMuted
                                )
                            },
                            colors = labTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("debate_custom_topic_input")
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { requestTopic(customTopicInput) },
                                enabled = !isGeneratingTopic && customTopicInput.isNotBlank(),
                                border = BorderStroke(1.dp, LingoTokens.borderFocus),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LingoTokens.accentPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("debate_use_custom_topic_btn")
                            ) {
                                Text("Use Topic", fontSize = 12.5.sp, maxLines = 1, softWrap = false)
                            }
                            Button(
                                onClick = {
                                    customTopicInput = ""
                                    requestTopic(null)
                                },
                                enabled = !isGeneratingTopic,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LingoTokens.accentBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("debate_generate_topic_btn")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isGeneratingTopic) "Generating..." else "Generate AI Topic",
                                    fontSize = 12.5.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Pro", "Con").forEach { stance ->
                                val selected = learnerStance == stance
                                FilterChip(
                                    selected = selected,
                                    onClick = { learnerStance = stance },
                                    label = {
                                        Text(
                                            text = if (stance == "Pro") "My Stance: Pro" else "My Stance: Con",
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    },
                                    colors = labFilterChipColors(),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = LingoTokens.borderSubtle,
                                        selectedBorderColor = LingoTokens.accentPrimary.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.testTag("debate_stance_${stance.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            if (isGeneratingTopic && activeTopic == null) {
                item {
                    LingoEmptyOrErrorCard(
                        title = "Preparing Debate Motion",
                        message = "Generating a CEFR ${learnerCtx.cefrLevel} ${targetLang.name} debate topic...",
                        isLoading = true
                    )
                }
            }

            if (!topicError.isNullOrBlank()) {
                item {
                    LingoEmptyOrErrorCard(
                        title = "Could Not Prepare Debate Topic",
                        message = topicError!!,
                        isError = true,
                        actionLabel = "Retry",
                        onAction = { requestTopic(customTopicInput.takeIf { it.isNotBlank() }) }
                    )
                }
            }

            val topic = activeTopic
            if (topic != null) {
                item {
                    LingoCleanCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LingoStatusBadge(
                                        text = "Round ${learnerTurnCount + 1} • You: $learnerStance",
                                        color = LingoTokens.accentPrimary
                                    )
                                    LingoStatusBadge(
                                        text = "${roundTimerSeconds}s round timer",
                                        color = if (roundTimerSeconds <= 15) LingoTokens.accentAmber else LingoTokens.accentEmerald,
                                        modifier = Modifier.testTag("debate_round_timer_pill")
                                    )
                                }
                                IconButton(
                                    onClick = { speechManager.speak(topic.titleTarget, targetLang.code) },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Hear debate motion",
                                        tint = LingoTokens.accentPrimary
                                    )
                                }
                            }
                            Text(
                                text = topic.titleTarget,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = LingoTokens.textPrimary
                            )
                            if (topic.titleNative != topic.titleTarget) {
                                Text(
                                    text = topic.titleNative,
                                    fontSize = 13.sp,
                                    color = LingoTokens.textSecondary
                                )
                            }
                            if (topic.contextBriefNative.isNotBlank()) {
                                Text(
                                    text = topic.contextBriefNative,
                                    fontSize = 12.5.sp,
                                    color = LingoTokens.textSecondary
                                )
                            }
                            Text(
                                text = "Pro: ${topic.proPositionNative}\nCon: ${topic.conPositionNative}",
                                fontSize = 12.sp,
                                color = LingoTokens.textPrimary
                            )
                            if (topic.suggestedConnectorsTarget.isNotEmpty()) {
                                Text(
                                    text = "Useful connectors (tap to insert)",
                                    fontSize = 11.5.sp,
                                    color = LingoTokens.textSecondary
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    topic.suggestedConnectorsTarget.forEach { conn ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = LingoTokens.accentPrimary.copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, LingoTokens.accentPrimary.copy(alpha = 0.3f)),
                                            modifier = Modifier.clickable {
                                                userArgument = if (userArgument.isBlank()) "$conn " else "$userArgument $conn "
                                                speechManager.speak(conn, targetLang.code)
                                            }
                                        ) {
                                            Text(
                                                text = conn,
                                                fontSize = 12.sp,
                                                color = LingoTokens.accentPrimary,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Multi-Round Live Debate Transcript
                items(debateHistory, key = { it.id }) { turn ->
                    val isAi = turn.speaker == "ai"
                    LingoCleanCard(
                        containerColor = if (isAi) LingoTokens.bgElevated else LingoTokens.accentBlue.copy(alpha = 0.14f),
                        borderColor = if (isAi) LingoTokens.borderSubtle else LingoTokens.accentBlue.copy(alpha = 0.4f),
                        contentPadding = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isAi) {
                                        "AI Opponent (Round ${turn.roundNumber})"
                                    } else {
                                        "You • ${if (turn.spokenViaMic) "Spoken" else "Typed"} (Round ${turn.roundNumber})"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAi) LingoTokens.accentAmber else LingoTokens.accentPrimary
                                )
                                Row {
                                    IconButton(
                                        onClick = { speechManager.speak(turn.textTarget, targetLang.code) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Hear turn",
                                            tint = LingoTokens.accentPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    if (isAi) {
                                        IconButton(
                                            onClick = { speechManager.stopSpeaking() },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Stop,
                                                contentDescription = "Stop audio",
                                                tint = LingoTokens.textSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = turn.textTarget,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LingoTokens.textPrimary
                            )
                            if (turn.translationNative.isNotBlank()) {
                                Text(
                                    text = turn.translationNative,
                                    fontSize = 12.5.sp,
                                    color = LingoTokens.textSecondary
                                )
                            }
                        }
                    }
                }

                // Composer + Clarification + Finish & Evaluate
                item {
                    LingoCleanCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (!lastQuickCoaching.isNullOrBlank()) {
                                Text(
                                    text = "Coach tip: $lastQuickCoaching",
                                    fontSize = 12.sp,
                                    color = LingoTokens.accentEmerald,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (!lastChallenge.isNullOrBlank()) {
                                Text(
                                    text = "Follow-up challenge: $lastChallenge",
                                    fontSize = 12.5.sp,
                                    color = LingoTokens.accentPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            OutlinedTextField(
                                value = userArgument,
                                onValueChange = {
                                    userArgument = it
                                    if (it.isBlank()) currentDraftSpoken = false
                                },
                                placeholder = {
                                    Text(
                                        "Present your $learnerStance argument in ${targetLang.name}...",
                                        color = LingoTokens.textMuted
                                    )
                                },
                                colors = labTextFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("debate_argument_input"),
                                maxLines = 4
                            )

                            if (!speechError.isNullOrBlank()) {
                                Text(text = speechError!!, fontSize = 12.sp, color = LingoTokens.accentAmber)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (isListening) {
                                            speechManager.stopListening()
                                        } else {
                                            speechManager.clearSpokenText()
                                            val hasPerm = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (hasPerm) speechManager.startListening(targetLang.code)
                                            else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    border = BorderStroke(1.dp, LingoTokens.borderFocus),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (isListening) LingoTokens.accentAmber else LingoTokens.accentPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("debate_mic_btn")
                                ) {
                                    Icon(
                                        if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isListening) "Stop" else "Speak", maxLines = 1, softWrap = false)
                                }

                                Button(
                                    onClick = { sendDebateTurn(isClarification = false) },
                                    enabled = !isSubmittingTurn && userArgument.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LingoTokens.accentBlue,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.4f)
                                        .testTag("debate_submit_button")
                                ) {
                                    Text(
                                        if (isSubmittingTurn) "Responding..." else "Send Argument",
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { sendDebateTurn(isClarification = true) },
                                    enabled = hasAiTurn && !isSubmittingTurn,
                                    border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LingoTokens.textPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("debate_clarify_btn")
                                ) {
                                    Text("Ask for Clarification", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                }

                                Button(
                                    onClick = { finishAndEvaluate() },
                                    enabled = learnerTurnCount > 0 && !isEvaluatingDebate && !isSubmittingTurn,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LingoTokens.accentEmerald,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("debate_finish_evaluate_btn")
                                ) {
                                    Text(
                                        if (isEvaluatingDebate) "Evaluating..." else "Finish & Evaluate ($learnerTurnCount turns)",
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            if (!turnError.isNullOrBlank()) {
                                LingoEmptyOrErrorCard(
                                    title = "Debate Partner Unavailable",
                                    message = turnError!!,
                                    isError = true,
                                    actionLabel = "Retry",
                                    onAction = { sendDebateTurn(isClarification = false) }
                                )
                            }

                            if (!evaluationError.isNullOrBlank()) {
                                LingoEmptyOrErrorCard(
                                    title = "Evaluation Unavailable",
                                    message = evaluationError!!,
                                    isError = true,
                                    actionLabel = if (learnerTurnCount > 0) "Retry" else null,
                                    onAction = if (learnerTurnCount > 0) { { finishAndEvaluate() } } else null
                                )
                            }
                        }
                    }
                }

                // Honest Post-Debate Rubric Card
                val eval = rubricEval
                if (eval != null) {
                    item {
                        LingoCleanCard(
                            borderColor = LingoTokens.accentEmerald.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("debate_evaluation_card")
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Debate Performance Rubric",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = LingoTokens.textPrimary
                                    )
                                    LingoStatusBadge(
                                        text = "Overall ${eval.overallScore}/100",
                                        color = LingoTokens.accentEmerald
                                    )
                                }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    LingoStatusBadge("Grammar ${eval.grammarScore}", LingoTokens.accentPrimary)
                                    LingoStatusBadge("Vocab ${eval.vocabularyScore}", LingoTokens.accentPrimary)
                                    LingoStatusBadge("Clarity ${eval.clarityScore}", LingoTokens.accentPrimary)
                                    LingoStatusBadge("Structure ${eval.argumentStructureScore}", LingoTokens.accentPrimary)
                                    eval.fluencyScoreIfSpoken?.let { fl ->
                                        LingoStatusBadge("Spoken Flow $fl", LingoTokens.accentEmerald)
                                    }
                                }

                                Text(
                                    text = eval.fluencyHonestyNote,
                                    fontSize = 12.sp,
                                    color = LingoTokens.textSecondary
                                )

                                if (eval.connectorsUsed.isNotEmpty()) {
                                    Text(
                                        text = "Connectors you used: ${eval.connectorsUsed.joinToString(", ")}",
                                        fontSize = 12.5.sp,
                                        color = LingoTokens.accentPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Text(
                                        text = "Connectors detected: None yet — try adding transition words in your next round.",
                                        fontSize = 12.5.sp,
                                        color = LingoTokens.textSecondary
                                    )
                                }

                                if (eval.counterargumentHandlingNote.isNotBlank()) {
                                    Text(
                                        text = "Counterargument handling: ${eval.counterargumentHandlingNote}",
                                        fontSize = 12.5.sp,
                                        color = LingoTokens.textPrimary
                                    )
                                }

                                if (eval.strengths.isNotEmpty()) {
                                    Text(
                                        text = "Strengths:\n${eval.strengths.joinToString("\n") { "• $it" }}",
                                        fontSize = 12.5.sp,
                                        color = LingoTokens.accentEmerald
                                    )
                                }
                                if (eval.improvementTips.isNotEmpty()) {
                                    Text(
                                        text = "3 Stronger Arguments & Next Improvements:\n${eval.improvementTips.take(3).joinToString("\n") { "• $it" }}",
                                        fontSize = 12.5.sp,
                                        color = LingoTokens.textPrimary
                                    )
                                }
                                val recommendedTransitions = remember(topic.suggestedConnectorsTarget) {
                                    (topic.suggestedConnectorsTarget + listOf("On the other hand", "That said", "Furthermore")).distinct().take(4)
                                }
                                Text(
                                    text = "New Transition Phrases to Practice: ${recommendedTransitions.joinToString(" • ")}",
                                    fontSize = 12.sp,
                                    color = LingoTokens.brandCyan,
                                    fontWeight = FontWeight.Medium
                                )
                                if (eval.upgradedExampleTarget.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = LingoTokens.bgSubtle,
                                        border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { speechManager.speak(eval.upgradedExampleTarget, targetLang.code) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Upgraded Phrasing Model",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = LingoTokens.accentPrimary
                                                )
                                                Text(
                                                    text = eval.upgradedExampleTarget,
                                                    fontSize = 13.5.sp,
                                                    color = LingoTokens.textPrimary
                                                )
                                            }
                                            Icon(
                                                Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "Hear upgraded example",
                                                tint = LingoTokens.accentPrimary,
                                                modifier = Modifier.size(18.dp)
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
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 10. TEACH THE AI SCREEN (Curious AI Student Persona)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun TeachTheAiScreen(
    userProfile: UserProfile,
    speechManager: SpeechManager,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val targetLang = Language.findByCode(userProfile.targetLanguageCode)
    val nativeLang = Language.findByCode(userProfile.nativeLanguageCode)
    val topics = remember { PracticalFeaturesCatalog.teachAiTopics() }
    var selectedTopic by remember { mutableStateOf(topics.first()) }
    var explanationInput by remember(selectedTopic.id) { mutableStateOf("") }
    var turnEval by remember(selectedTopic.id) { mutableStateOf<TeachAiTurnEvaluation?>(null) }
    var isTeaching by remember { mutableStateOf(false) }
    var teachErrorBanner by remember(selectedTopic.id) { mutableStateOf<String?>(null) }
    val isVerifiedPro = userProfile.isEntitlementVerified

    val isListening by speechManager.isListening.collectAsState()
    val spokenText by speechManager.spokenText.collectAsState()

    LaunchedEffect(spokenText) {
        if (spokenText.isNotBlank()) {
            explanationInput = spokenText
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) speechManager.startListening(targetLang.code)
    }

    val starterQuestion = selectedTopic.starterQuestionFromAiByLanguage[targetLang.code.lowercase()]
        ?: "How does '${selectedTopic.title}' work? Please explain step by step in ${targetLang.name}!"

    fun submitExplanation() {
        if (!isVerifiedPro) {
            AiErrorHandler.requestUpgrade(
                mode = UpgradeSheetMode.AI_UPGRADE,
                featureKey = AccessPolicyFeatureKeys.LABS,
                headline = "Unlock Teach the AI",
                reason = "Upgrade to LingoSphere Pro for interactive Teach the AI speaking practice and clarity feedback."
            )
            return
        }
        val text = explanationInput.trim()
        if (text.isEmpty() || isTeaching) return
        isTeaching = true
        teachErrorBanner = null
        scope.launch {
            try {
                turnEval = PracticalFeaturesAiService.interactWithAiStudent(
                    topic = selectedTopic,
                    languageCode = targetLang.code,
                    learnerExplanation = text,
                    nativeLanguage = nativeLang.name
                )
            } catch (e: Exception) {
                val handled = AiErrorHandler.handle(e, contextTag = "TeachTheAi")
                if (handled is AiUiError.ProRequired) {
                    AiErrorHandler.requestUpgrade(
                        mode = UpgradeSheetMode.AI_UPGRADE,
                        featureKey = AccessPolicyFeatureKeys.LABS
                    )
                } else {
                    teachErrorBanner = handled.userMessage
                }
            } finally {
                isTeaching = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LingoTokens.bgCanvas)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
    ) {
        LingoTopBar(
            title = "Teach the AI",
            subtitle = "${targetLang.name} • Explain clearly to a curious AI student",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("teach_the_ai_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    topics.forEachIndexed { idx, t ->
                        val selected = selectedTopic.id == t.id
                        FilterChip(
                            selected = selected,
                            onClick = { selectedTopic = t },
                            label = {
                                Text(
                                    t.title,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                )
                            },
                            colors = labFilterChipColors(),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = LingoTokens.borderSubtle,
                                selectedBorderColor = LingoTokens.accentPrimary.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.testTag("teach_ai_topic_chip_$idx")
                        )
                    }
                }
            }

            item {
                LingoCleanCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LingoStatusBadge(
                            text = "Curious AI Student Asks",
                            color = LingoTokens.accentPrimary
                        )
                        Text(
                            text = starterQuestion,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            color = LingoTokens.textPrimary
                        )
                        Text(
                            text = selectedTopic.description,
                            fontSize = 12.5.sp,
                            color = LingoTokens.textSecondary
                        )

                        OutlinedTextField(
                            value = explanationInput,
                            onValueChange = { explanationInput = it },
                            placeholder = {
                                Text(
                                    "Explain step-by-step in ${targetLang.name}...",
                                    color = LingoTokens.textMuted
                                )
                            },
                            colors = labTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("teach_ai_explanation_input"),
                            maxLines = 4
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (isListening) {
                                        speechManager.stopListening()
                                    } else {
                                        speechManager.clearSpokenText()
                                        val hasPerm = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasPerm) speechManager.startListening(targetLang.code)
                                        else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                border = BorderStroke(1.dp, LingoTokens.borderFocus),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isListening) LingoTokens.accentAmber else LingoTokens.accentPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isListening) "Stop" else "Speak", maxLines = 1, softWrap = false)
                            }

                            Button(
                                onClick = { submitExplanation() },
                                enabled = !isTeaching && explanationInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LingoTokens.accentBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.4f)
                                    .testTag("teach_ai_submit_button")
                            ) {
                                if (!isVerifiedPro) {
                                    LingoGoldLockIcon(size = 14.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(if (isTeaching) "Thinking..." else "Explain to AI", maxLines = 1, softWrap = false)
                            }
                        }

                        if (!teachErrorBanner.isNullOrBlank()) {
                            LingoCalmAiErrorBanner(
                                message = teachErrorBanner,
                                onRetry = { submitExplanation() },
                                onDismiss = { teachErrorBanner = null }
                            )
                        }

                        val eval = turnEval
                        if (eval != null) {
                            if (!eval.aiErrorMessage.isNullOrBlank()) {
                                LingoCalmAiErrorBanner(
                                    message = AiErrorHandler.sanitizeUserFacingMessage(eval.aiErrorMessage),
                                    onRetry = { submitExplanation() },
                                    onDismiss = { turnEval = null }
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = LingoTokens.bgSubtle,
                                    border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("teach_ai_evaluation_card")
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
                                            Text(
                                                text = "What the AI Student Understood",
                                                fontWeight = FontWeight.Bold,
                                                color = LingoTokens.accentEmerald,
                                                fontSize = 13.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { speechManager.speak(eval.aiStudentFollowUpQuestionTarget, targetLang.code) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "Hear follow-up question",
                                                    tint = LingoTokens.accentPrimary
                                                )
                                            }
                                        }
                                        Text(
                                            text = eval.whatAiStudentUnderstoodSummary,
                                            fontSize = 12.5.sp,
                                            color = LingoTokens.textPrimary
                                        )
                                        Text(
                                            text = "Follow-Up Question: ${eval.aiStudentFollowUpQuestionTarget}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = LingoTokens.accentPrimary
                                        )
                                        Text(
                                            text = eval.aiStudentFollowUpTranslation,
                                            fontSize = 12.sp,
                                            color = LingoTokens.textSecondary
                                        )
                                        Text(
                                            text = "Clarity Feedback: ${eval.clarityAndCoherenceFeedback}",
                                            fontSize = 12.5.sp,
                                            color = LingoTokens.textPrimary
                                        )
                                        Text(
                                            text = "Model Phrasing: ${eval.suggestedClearerPhrasingTarget}",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = LingoTokens.accentEmerald
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
}
