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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.ai.DetectiveResponseEvaluation
import com.example.ai.DynamicDetectiveCase
import com.example.ai.ExplanationMiniPracticeEvaluation
import com.example.ai.ExploreAiLearningEngine
import com.example.ai.ExploreAiResult
import com.example.ai.ExploreLearnerContext
import com.example.ai.ExploreSessionRecord
import com.example.ai.ThreeWayExplanationResult
import com.example.data.model.Language
import com.example.data.model.UserProfile
import com.example.speech.SpeechManager
import com.example.ui.components.LingoCleanCard
import com.example.ui.components.LingoEmptyOrErrorCard
import com.example.ui.components.LingoStatusBadge
import com.example.ui.components.LingoTokens
import com.example.ui.components.LingoTopBar
import kotlinx.coroutines.launch

@Composable
private fun pragmaticsInputColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = LingoTokens.textPrimary,
    unfocusedTextColor = LingoTokens.textPrimary,
    focusedBorderColor = LingoTokens.accentPrimary,
    unfocusedBorderColor = LingoTokens.borderSubtle,
    focusedContainerColor = LingoTokens.bgSubtle,
    unfocusedContainerColor = LingoTokens.bgSubtle
)

private fun <T> pragmaticsErrorText(result: ExploreAiResult<T>): String = when (result) {
    is ExploreAiResult.Offline -> "No internet connection detected. Connect to the internet and tap Retry."
    is ExploreAiResult.QuotaExceeded -> result.message
    is ExploreAiResult.Error -> result.message
    is ExploreAiResult.Success -> ""
}

// ═══════════════════════════════════════════════════════════════════════════════
// 7. EXPLAIN IT THREE WAYS SCREEN (Any Concept, 3 Real Depths, Follow-Up & Drill)
// ═══════════════════════════════════════════════════════════════════════════════

private data class FollowUpQaEntry(
    val question: String,
    val answerNative: String,
    val exampleTarget: String
)

@Composable
fun ExplainItThreeWaysScreen(
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

    val quickTopics = remember(targetLang.code) {
        when (targetLang.code.lowercase()) {
            "es" -> listOf("Por vs. Para", "Ser vs. Estar", "Subjunctive in Polite Requests", "Preterite vs. Imperfect")
            "fr" -> listOf("Passé Composé vs. Imparfait", "Tu vs. Vous Nuances", "Subjonctif Présent", "En vs. Y Pronouns")
            "ja" -> listOf("Wa (は) vs. Ga (が)", "Te-form Polite Requests", "Keigo Politeness Levels", "Giving & Receiving")
            "de" -> listOf("Der/Die/Das & Cases", "Weil vs. Denn Word Order", "Sie vs. Du Formality", "Separable Verbs")
            "ur" -> listOf("Aap vs. Tum Politeness", "Gendered Verb Agreement", "Postpositions (Ko, Se, Mein)", "Polite Requests")
            "ar" -> listOf("Root & Pattern System", "Nominal vs. Verbal Sentences", "Polite Greetings", "Dual & Plural Agreement")
            "it" -> listOf("Essere vs. Avere", "Lei vs. Tu Formality", "Preposizioni Articolate", "Subjunctive Opinions")
            "pt" -> listOf("Ser vs. Estar", "Por vs. Para", "Você vs. Tu Usage", "Preterite vs. Imperfect")
            "ko" -> listOf("Eun/Neun vs. I/Ga", "Yo vs. Hamnida Levels", "Honorific -Si- Marker", "Natural Connectors")
            "zh" -> listOf("Le (了) Aspect Particle", "Measure Words", "Ba (把) Construction", "Polite Softeners")
            else -> listOf("Polite Requests vs. Direct Commands", "Present Perfect vs. Past Simple", "Natural Discourse Connectors", "Formal vs. Casual Tone")
        }
    }

    var conceptQuery by remember(targetLang.code) { mutableStateOf(quickTopics.first()) }
    var explanationResult by remember(targetLang.code) { mutableStateOf<ThreeWayExplanationResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }

    var followUpInput by remember(explanationResult?.conceptTitle) { mutableStateOf("") }
    var followUpEntries by remember(explanationResult?.conceptTitle) { mutableStateOf<List<FollowUpQaEntry>>(emptyList()) }
    var isAskingFollowUp by remember { mutableStateOf(false) }
    var followUpError by remember { mutableStateOf<String?>(null) }

    var miniPracticeInput by remember(explanationResult?.conceptTitle) { mutableStateOf("") }
    var miniPracticeEval by remember(explanationResult?.conceptTitle) { mutableStateOf<ExplanationMiniPracticeEvaluation?>(null) }
    var isCheckingPractice by remember { mutableStateOf(false) }
    var practiceError by remember { mutableStateOf<String?>(null) }
    var showHint by remember(explanationResult?.conceptTitle) { mutableStateOf(false) }
    var selectedRegisterTab by remember(explanationResult?.conceptTitle) { mutableStateOf("All 3") }
    var savedBestVersionNotice by remember(explanationResult?.conceptTitle) { mutableStateOf<String?>(null) }

    var savedHistory by remember(targetLang.code) {
        mutableStateOf(
            ExploreAiLearningEngine.loadLocalExploreSessions(
                context = context,
                featureType = "explain_three_ways",
                languageCode = targetLang.code
            )
        )
    }

    fun loadExplanation(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty() || isLoading) return
        isLoading = true
        aiError = null
        followUpError = null
        practiceError = null
        scope.launch {
            val res = ExploreAiLearningEngine.generateThreeWayExplanation(
                context = context,
                learnerCtx = learnerCtx,
                userConceptQuery = trimmed
            )
            when (res) {
                is ExploreAiResult.Success -> {
                    val data = res.data
                    explanationResult = data
                    followUpEntries = emptyList()
                    miniPracticeInput = ""
                    miniPracticeEval = null
                    showHint = false

                    val sessionId = "exp3_${targetLang.code}_${trimmed.lowercase().replace(Regex("[^a-z0-9]+"), "_").take(40)}"
                    ExploreAiLearningEngine.saveExploreSession(
                        context = context,
                        record = ExploreSessionRecord(
                            sessionId = sessionId,
                            featureType = "explain_three_ways",
                            languageCode = targetLang.code,
                            title = data.conceptTitle,
                            summary = data.beginnerExplanationNative.take(220),
                            cefrLevel = learnerCtx.cefrLevel,
                            turnsCount = 1,
                            scorePercent = 100
                        )
                    )
                    savedHistory = ExploreAiLearningEngine.loadLocalExploreSessions(
                        context = context,
                        featureType = "explain_three_ways",
                        languageCode = targetLang.code
                    )
                }
                else -> {
                    aiError = pragmaticsErrorText(res)
                }
            }
            isLoading = false
        }
    }

    fun askFollowUp() {
        val concept = explanationResult ?: return
        val q = followUpInput.trim()
        if (q.isEmpty() || isAskingFollowUp) return
        isAskingFollowUp = true
        followUpError = null
        scope.launch {
            val res = ExploreAiLearningEngine.askExplanationFollowUp(
                context = context,
                learnerCtx = learnerCtx,
                concept = concept,
                followUpQuestion = q
            )
            when (res) {
                is ExploreAiResult.Success -> {
                    val (answer, example) = res.data
                    followUpEntries = followUpEntries + FollowUpQaEntry(
                        question = q,
                        answerNative = answer,
                        exampleTarget = example
                    )
                    followUpInput = ""
                }
                else -> {
                    followUpError = pragmaticsErrorText(res)
                }
            }
            isAskingFollowUp = false
        }
    }

    fun submitMiniPractice() {
        val concept = explanationResult ?: return
        val ans = miniPracticeInput.trim()
        if (ans.isEmpty() || isCheckingPractice) return
        isCheckingPractice = true
        practiceError = null
        scope.launch {
            val res = ExploreAiLearningEngine.evaluateExplanationMiniExercise(
                context = context,
                learnerCtx = learnerCtx,
                concept = concept,
                learnerAnswer = ans
            )
            when (res) {
                is ExploreAiResult.Success -> {
                    val eval = res.data
                    miniPracticeEval = eval
                    eval.capturedMistake?.let { m ->
                        onRecordMistake(m.category, m.originalText, m.correctedText, m.explanation)
                    }
                }
                else -> {
                    practiceError = pragmaticsErrorText(res)
                }
            }
            isCheckingPractice = false
        }
    }

    LaunchedEffect(targetLang.code) {
        if (explanationResult == null && !isLoading) {
            loadExplanation(conceptQuery)
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
            title = "Explain It Three Ways",
            subtitle = "${targetLang.name} • Beginner, Intermediate & Advanced AI Teacher",
            onBack = {
                speechManager.stopSpeaking()
                onBack()
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("explain_it_three_ways_screen"),
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
                    quickTopics.forEachIndexed { idx, topic ->
                        val selected = conceptQuery.equals(topic, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                conceptQuery = topic
                                loadExplanation(topic)
                            },
                            label = {
                                Text(
                                    topic,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = LingoTokens.bgElevated,
                                labelColor = LingoTokens.textSecondary,
                                selectedContainerColor = LingoTokens.accentPrimary.copy(alpha = 0.16f),
                                selectedLabelColor = LingoTokens.accentPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = LingoTokens.borderSubtle,
                                selectedBorderColor = LingoTokens.accentPrimary.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.testTag("three_way_topic_chip_$idx")
                        )
                    }
                }
            }

            item {
                LingoCleanCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Ask About Any ${targetLang.name} Concept",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = LingoTokens.textPrimary
                        )
                        OutlinedTextField(
                            value = conceptQuery,
                            onValueChange = { conceptQuery = it },
                            placeholder = {
                                Text(
                                    "Type any grammar rule, idiom, tense, phrase, or nuance...",
                                    color = LingoTokens.textMuted
                                )
                            },
                            colors = pragmaticsInputColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("three_way_query_input"),
                            singleLine = true
                        )
                        Button(
                            onClick = { loadExplanation(conceptQuery) },
                            enabled = !isLoading && conceptQuery.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LingoTokens.accentBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("three_way_generate_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isLoading) "Generating 3 Levels..." else "Explain at All 3 Levels")
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    LingoEmptyOrErrorCard(
                        title = "Teaching \"${conceptQuery.trim()}\"",
                        message = "Building distinct Beginner, Intermediate, and Advanced ${targetLang.name} breakdowns...",
                        isLoading = true
                    )
                }
            }

            if (!aiError.isNullOrBlank()) {
                item {
                    LingoEmptyOrErrorCard(
                        title = "AI Explanation Unavailable",
                        message = aiError!!,
                        isError = true,
                        actionLabel = "Retry",
                        onAction = { loadExplanation(conceptQuery) }
                    )
                }
            }

            val res = explanationResult
            if (res != null && !isLoading) {
                item {
                    val keyVocabDiff = remember(res) {
                        val bWords = res.beginnerExampleTarget.split(Regex("[^\\p{L}0-9']+")).filter { it.length >= 4 }
                        val iWords = res.intermediateExampleTarget.split(Regex("[^\\p{L}0-9']+")).filter { it.length >= 4 }
                        val aWords = res.advancedExampleTarget.split(Regex("[^\\p{L}0-9']+")).filter { it.length >= 4 }
                        (bWords.take(2) + iWords.take(2) + aWords.take(2)).distinct()
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("All 3", "Casual", "Intermediate", "Professional").forEach { tab ->
                                val selected = selectedRegisterTab == tab
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedRegisterTab = tab },
                                    label = { Text(tab, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LingoTokens.brandCyan.copy(alpha = 0.18f),
                                        selectedLabelColor = LingoTokens.brandCyan,
                                        containerColor = LingoTokens.bgElevated,
                                        labelColor = LingoTokens.textSecondary
                                    ),
                                    modifier = Modifier.testTag("three_way_tab_${tab.lowercase().replace(' ', '_')}")
                                )
                            }
                        }
                        if (keyVocabDiff.isNotEmpty()) {
                            Text(
                                text = "Key Vocabulary Shifts Across Registers: ${keyVocabDiff.joinToString(" → ")}",
                                fontSize = 12.sp,
                                color = LingoTokens.brandCyan,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                if (selectedRegisterTab == "All 3" || selectedRegisterTab == "Casual") {
                    item {
                        DepthExplanationCard(
                            badgeText = "Casual / Beginner (A1–A2) • ${res.conceptTitle}",
                            explanation = res.beginnerExplanationNative,
                            exampleTarget = res.beginnerExampleTarget,
                            exampleNative = res.beginnerExampleNative,
                            accentColor = LingoTokens.accentEmerald,
                            onSpeak = { speechManager.speak(res.beginnerExampleTarget, targetLang.code) },
                            testTag = "three_way_beginner_card"
                        )
                    }
                }
                if (selectedRegisterTab == "All 3" || selectedRegisterTab == "Intermediate") {
                    item {
                        DepthExplanationCard(
                            badgeText = "Intermediate (B1–B2)",
                            explanation = res.intermediateExplanationNative,
                            exampleTarget = res.intermediateExampleTarget,
                            exampleNative = res.intermediateExampleNative,
                            accentColor = LingoTokens.accentPrimary,
                            onSpeak = { speechManager.speak(res.intermediateExampleTarget, targetLang.code) },
                            testTag = "three_way_intermediate_card"
                        )
                    }
                }
                if (selectedRegisterTab == "All 3" || selectedRegisterTab == "Professional") {
                    item {
                        DepthExplanationCard(
                            badgeText = "Professional / Advanced (C1–C2)",
                            explanation = res.advancedExplanationNative,
                            exampleTarget = res.advancedExampleTarget,
                            exampleNative = res.advancedExampleNative,
                            accentColor = LingoTokens.accentAmber,
                            onSpeak = { speechManager.speak(res.advancedExampleTarget, targetLang.code) },
                            testTag = "three_way_advanced_card"
                        )
                    }
                }

                if (res.commonPitfallNative.isNotBlank()) {
                    item {
                        LingoCleanCard(
                            borderColor = LingoTokens.accentAmber.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Common Pitfall to Avoid",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = LingoTokens.accentAmber
                                )
                                Text(
                                    text = res.commonPitfallNative,
                                    fontSize = 13.sp,
                                    color = LingoTokens.textPrimary
                                )
                            }
                        }
                    }
                }

                // Interactive Mini Practice Exercise
                if (res.miniPracticePromptNative.isNotBlank()) {
                    item {
                        LingoCleanCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("three_way_comprehension_card")
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = LingoTokens.accentPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Mini Practice Exercise",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = LingoTokens.textPrimary
                                    )
                                }
                                Text(
                                    text = res.miniPracticePromptNative,
                                    fontSize = 14.sp,
                                    color = LingoTokens.textPrimary
                                )
                                if (res.miniPracticeHintNative.isNotBlank()) {
                                    TextButton(onClick = { showHint = !showHint }) {
                                        Text(
                                            if (showHint) "Hide Hint" else "Show Hint",
                                            fontSize = 12.sp,
                                            color = LingoTokens.accentPrimary
                                        )
                                    }
                                    if (showHint) {
                                        Text(
                                            text = "Hint: ${res.miniPracticeHintNative}",
                                            fontSize = 12.5.sp,
                                            color = LingoTokens.textSecondary
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = miniPracticeInput,
                                    onValueChange = { miniPracticeInput = it },
                                    placeholder = {
                                        Text("Write your answer in ${targetLang.name}...", color = LingoTokens.textMuted)
                                    },
                                    colors = pragmaticsInputColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("three_way_mini_practice_input"),
                                    maxLines = 3
                                )
                                Button(
                                    onClick = { submitMiniPractice() },
                                    enabled = !isCheckingPractice && miniPracticeInput.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LingoTokens.accentBlue,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("three_way_mini_practice_submit")
                                ) {
                                    Text(if (isCheckingPractice) "Checking..." else "Check Practice Answer")
                                }

                                if (!practiceError.isNullOrBlank()) {
                                    LingoEmptyOrErrorCard(
                                        title = "Could Not Grade Exercise",
                                        message = practiceError!!,
                                        isError = true,
                                        actionLabel = "Retry",
                                        onAction = { submitMiniPractice() }
                                    )
                                }

                                val pEval = miniPracticeEval
                                if (pEval != null) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (pEval.isCorrect) LingoTokens.accentEmerald.copy(alpha = 0.14f) else LingoTokens.accentAmber.copy(alpha = 0.14f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (pEval.isCorrect) LingoTokens.accentEmerald.copy(alpha = 0.4f) else LingoTokens.accentAmber.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (pEval.isCorrect) {
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = LingoTokens.accentEmerald,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Text(
                                                    text = if (pEval.isCorrect) "Great Job! (${pEval.score}/100)" else "Almost There (${pEval.score}/100)",
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (pEval.isCorrect) LingoTokens.accentEmerald else LingoTokens.accentAmber
                                                )
                                            }
                                            Text(text = pEval.feedbackNative, fontSize = 13.sp, color = LingoTokens.textPrimary)
                                            if (pEval.modelAnswerTarget.isNotBlank()) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { speechManager.speak(pEval.modelAnswerTarget, targetLang.code) },
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "Model answer: ${pEval.modelAnswerTarget}",
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = LingoTokens.accentPrimary,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.VolumeUp,
                                                        contentDescription = "Hear model answer",
                                                        tint = LingoTokens.accentPrimary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            androidx.compose.material3.TextButton(
                                                onClick = {
                                                    val bestText = pEval.modelAnswerTarget.ifBlank { miniPracticeInput }
                                                    scope.launch {
                                                        ExploreAiLearningEngine.saveExploreSession(
                                                            context = context,
                                                            record = ExploreSessionRecord(
                                                                sessionId = "exp3_best_${targetLang.code}_${System.currentTimeMillis()}",
                                                                featureType = "explain_three_ways",
                                                                languageCode = targetLang.code,
                                                                title = "${res.conceptTitle} (${selectedRegisterTab})",
                                                                summary = "Best version: $bestText • Score ${pEval.score}/100",
                                                                cefrLevel = learnerCtx.cefrLevel,
                                                                turnsCount = 1,
                                                                scorePercent = pEval.score
                                                            )
                                                        )
                                                        savedHistory = ExploreAiLearningEngine.loadLocalExploreSessions(
                                                            context = context,
                                                            featureType = "explain_three_ways",
                                                            languageCode = targetLang.code
                                                        )
                                                        savedBestVersionNotice = "Saved your best version & key vocabulary to recent sessions."
                                                    }
                                                },
                                                modifier = Modifier.testTag("three_way_save_best_btn")
                                            ) {
                                                Text(
                                                    text = "Save Best Version & New Words",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = LingoTokens.brandCyan
                                                )
                                            }
                                            if (!savedBestVersionNotice.isNullOrBlank()) {
                                                Text(
                                                    text = savedBestVersionNotice!!,
                                                    fontSize = 11.5.sp,
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

                // Ask a Follow-Up Question Card
                item {
                    LingoCleanCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Ask a Follow-Up Question",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = LingoTokens.textPrimary
                            )
                            followUpEntries.forEach { entry ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = LingoTokens.bgSubtle,
                                    border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Q: ${entry.question}",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp,
                                            color = LingoTokens.accentPrimary
                                        )
                                        Text(
                                            text = entry.answerNative,
                                            fontSize = 13.sp,
                                            color = LingoTokens.textPrimary
                                        )
                                        if (entry.exampleTarget.isNotBlank()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { speechManager.speak(entry.exampleTarget, targetLang.code) },
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "Example: ${entry.exampleTarget}",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = LingoTokens.accentEmerald,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Icon(
                                                    Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "Hear follow-up example",
                                                    tint = LingoTokens.accentEmerald,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = followUpInput,
                                onValueChange = { followUpInput = it },
                                placeholder = {
                                    Text("Ask about an exception, casual use, or extra example...", color = LingoTokens.textMuted)
                                },
                                colors = pragmaticsInputColors(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("three_way_followup_input"),
                                maxLines = 2
                            )
                            Button(
                                onClick = { askFollowUp() },
                                enabled = !isAskingFollowUp && followUpInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LingoTokens.accentPrimary,
                                    contentColor = Color(0xFF0B0F1A)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("three_way_followup_btn")
                            ) {
                                Text(if (isAskingFollowUp) "Answering..." else "Ask AI Teacher")
                            }
                            if (!followUpError.isNullOrBlank()) {
                                Text(text = followUpError!!, fontSize = 12.sp, color = LingoTokens.accentAmber)
                            }
                        }
                    }
                }
            }

            // Saved Explanation History
            if (savedHistory.isNotEmpty()) {
                item {
                    LingoCleanCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = LingoTokens.accentPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Recent Saved Explanations",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = LingoTokens.textPrimary
                                )
                            }
                            savedHistory.take(6).forEach { item ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = LingoTokens.bgSubtle,
                                    border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            conceptQuery = item.title
                                            loadExplanation(item.title)
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = LingoTokens.textPrimary
                                        )
                                        if (item.summary.isNotBlank()) {
                                            Text(
                                                text = item.summary,
                                                fontSize = 12.sp,
                                                color = LingoTokens.textSecondary,
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
        }
    }
}

@Composable
private fun DepthExplanationCard(
    badgeText: String,
    explanation: String,
    exampleTarget: String,
    exampleNative: String,
    accentColor: Color,
    onSpeak: () -> Unit,
    testTag: String
) {
    LingoCleanCard(
        borderColor = accentColor.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LingoStatusBadge(text = badgeText, color = accentColor)
                if (exampleTarget.isNotBlank()) {
                    IconButton(onClick = onSpeak, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Hear example",
                            tint = accentColor
                        )
                    }
                }
            }
            Text(
                text = explanation,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = LingoTokens.textPrimary
            )
            if (exampleTarget.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LingoTokens.bgSubtle,
                    border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = exampleTarget,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = LingoTokens.textPrimary
                        )
                        if (exampleNative.isNotBlank()) {
                            Text(
                                text = exampleNative,
                                fontSize = 12.5.sp,
                                color = LingoTokens.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 8. CONVERSATION DETECTIVE SCREEN (Dynamic AI Pragmatics & Multi-Turn Reply)
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConversationDetectiveScreen(
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

    val pragmaticFocusOptions = remember {
        listOf(
            "Indirect refusal",
            "Sarcasm",
            "Hesitation",
            "Uncertainty",
            "Politeness",
            "Humor",
            "Context-dependent"
        )
    }

    var selectedPragmatic by remember(targetLang.code) { mutableStateOf(pragmaticFocusOptions.first()) }
    var activeCase by remember(targetLang.code) { mutableStateOf<DynamicDetectiveCase?>(null) }
    var recentPhrases by remember(targetLang.code) { mutableStateOf<List<String>>(emptyList()) }
    var isGeneratingCase by remember { mutableStateOf(false) }
    var caseError by remember { mutableStateOf<String?>(null) }

    var selectedOptionIdx by remember(activeCase?.id) { mutableStateOf<Int?>(null) }
    var customExplanationInput by remember(activeCase?.id) { mutableStateOf("") }
    var naturalReplyInput by remember(activeCase?.id) { mutableStateOf("") }
    var isEvaluating by remember { mutableStateOf(false) }
    var evalError by remember { mutableStateOf<String?>(null) }
    var evaluation by remember(activeCase?.id) { mutableStateOf<DetectiveResponseEvaluation?>(null) }
    var continuationTurns by remember(activeCase?.id) { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    val isListening by speechManager.isListening.collectAsState()
    val spokenText by speechManager.spokenText.collectAsState()
    val speechError by speechManager.speechError.collectAsState()

    LaunchedEffect(spokenText) {
        if (spokenText.isNotBlank()) {
            naturalReplyInput = spokenText
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) speechManager.startListening(targetLang.code)
    }

    fun generateNewCase(pragmatic: String = selectedPragmatic) {
        if (isGeneratingCase) return
        speechManager.stopSpeaking()
        isGeneratingCase = true
        caseError = null
        evalError = null
        evaluation = null
        scope.launch {
            val res = ExploreAiLearningEngine.generateDetectiveCase(
                context = context,
                learnerCtx = learnerCtx,
                requestedPragmaticType = pragmatic,
                avoidPhrases = recentPhrases
            )
            when (res) {
                is ExploreAiResult.Success -> {
                    val data = res.data
                    activeCase = data
                    recentPhrases = (recentPhrases + data.literalPhraseTarget).takeLast(8)
                    selectedOptionIdx = null
                    customExplanationInput = ""
                    naturalReplyInput = ""
                    continuationTurns = emptyList()
                    speechManager.speakWithGender(
                        text = data.literalPhraseTarget,
                        languageCode = targetLang.code,
                        preferMale = data.preferredVoiceGender.equals("male", ignoreCase = true)
                    )
                }
                else -> {
                    caseError = pragmaticsErrorText(res)
                }
            }
            isGeneratingCase = false
        }
    }

    LaunchedEffect(targetLang.code) {
        if (activeCase == null && !isGeneratingCase) {
            generateNewCase(selectedPragmatic)
        }
    }

    fun submitInvestigation() {
        val c = activeCase ?: return
        val chosenOption = selectedOptionIdx?.let { c.interpretationOptions.getOrNull(it) }
        if (chosenOption == null && customExplanationInput.isBlank() && naturalReplyInput.isBlank()) return
        if (isEvaluating) return
        isEvaluating = true
        evalError = null
        val replyText = naturalReplyInput.trim()

        scope.launch {
            val res = ExploreAiLearningEngine.evaluateDetectiveResponse(
                context = context,
                learnerCtx = learnerCtx,
                detectiveCase = c,
                selectedInterpretation = chosenOption,
                learnerCustomExplanation = customExplanationInput.trim(),
                learnerReplyInTargetLang = replyText
            )
            when (res) {
                is ExploreAiResult.Success -> {
                    val eval = res.data
                    evaluation = eval
                    if (replyText.isNotBlank() && eval.speakerContinuationTarget.isNotBlank()) {
                        continuationTurns = continuationTurns + listOf(
                            "You" to replyText,
                            c.speakerName to "${eval.speakerContinuationTarget}\n(${eval.speakerContinuationNative})"
                        )
                        naturalReplyInput = ""
                        speechManager.speakWithGender(
                            text = eval.speakerContinuationTarget,
                            languageCode = targetLang.code,
                            preferMale = c.preferredVoiceGender.equals("male", ignoreCase = true)
                        )
                    }
                    eval.capturedMistake?.let { m ->
                        onRecordMistake(m.category, m.originalText, m.correctedText, m.explanation)
                    }
                    ExploreAiLearningEngine.saveExploreSession(
                        context = context,
                        record = ExploreSessionRecord(
                            sessionId = c.id,
                            featureType = "conversation_detective",
                            languageCode = targetLang.code,
                            title = "${c.settingTitle} (${c.pragmaticType})",
                            summary = eval.feedbackNative,
                            cefrLevel = learnerCtx.cefrLevel,
                            turnsCount = continuationTurns.size.coerceAtLeast(1),
                            scorePercent = eval.score
                        )
                    )
                }
                else -> {
                    evalError = pragmaticsErrorText(res)
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
            title = "Conversation Detective",
            subtitle = "${targetLang.name} • Live AI pragmatics, tone & subtext",
            onBack = {
                speechManager.stopSpeaking()
                onBack()
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("conversation_detective_screen"),
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
                            Text(
                                text = "Pragmatic Focus",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = LingoTokens.textPrimary
                            )
                            Button(
                                onClick = { generateNewCase(selectedPragmatic) },
                                enabled = !isGeneratingCase,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LingoTokens.accentBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("detective_new_case_btn")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isGeneratingCase) "Generating..." else "New AI Case",
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
                            pragmaticFocusOptions.forEachIndexed { idx, focus ->
                                val selected = selectedPragmatic == focus
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        selectedPragmatic = focus
                                        generateNewCase(focus)
                                    },
                                    label = {
                                        Text(
                                            focus,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = LingoTokens.bgElevated,
                                        labelColor = LingoTokens.textSecondary,
                                        selectedContainerColor = LingoTokens.accentPrimary.copy(alpha = 0.16f),
                                        selectedLabelColor = LingoTokens.accentPrimary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = LingoTokens.borderSubtle,
                                        selectedBorderColor = LingoTokens.accentPrimary.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.testTag("detective_case_chip_$idx")
                                )
                            }
                        }
                    }
                }
            }

            if (isGeneratingCase && activeCase == null) {
                item {
                    LingoEmptyOrErrorCard(
                        title = "Generating Pragmatic Case",
                        message = "Creating a realistic ${targetLang.name} $selectedPragmatic dialogue for CEFR ${learnerCtx.cefrLevel}...",
                        isLoading = true
                    )
                }
            }

            if (!caseError.isNullOrBlank()) {
                item {
                    LingoEmptyOrErrorCard(
                        title = "Could Not Load AI Case",
                        message = caseError!!,
                        isError = true,
                        actionLabel = "Retry",
                        onAction = { generateNewCase(selectedPragmatic) }
                    )
                }
            }

            val c = activeCase
            if (c != null) {
                item {
                    LingoCleanCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    LingoStatusBadge(text = c.pragmaticType, color = LingoTokens.accentPrimary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = c.settingTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = LingoTokens.textPrimary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        speechManager.speakWithGender(
                                            text = c.literalPhraseTarget,
                                            languageCode = targetLang.code,
                                            preferMale = c.preferredVoiceGender.equals("male", ignoreCase = true)
                                        )
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Hear speaker utterance",
                                        tint = LingoTokens.accentPrimary
                                    )
                                }
                            }

                            Text(
                                text = c.contextBriefNative,
                                fontSize = 13.sp,
                                color = LingoTokens.textSecondary
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = LingoTokens.bgSubtle,
                                border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = "${c.speakerName} says:",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LingoTokens.accentAmber
                                    )
                                    Text(
                                        text = c.literalPhraseTarget,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.5.sp,
                                        color = LingoTokens.textPrimary
                                    )
                                    Text(
                                        text = "Literal words: ${c.literalTranslationNative}",
                                        fontSize = 12.5.sp,
                                        color = LingoTokens.textSecondary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = LingoTokens.accentPrimary.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, LingoTokens.accentPrimary.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Nuance Principle: ${c.nuancedCulturalNote}",
                                    fontSize = 12.5.sp,
                                    color = LingoTokens.accentPrimary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }

                            Text(
                                text = "1. Pick the most likely meaning (or explain in your own words)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = LingoTokens.textPrimary
                            )

                            c.interpretationOptions.forEachIndexed { idx, opt ->
                                val isSelected = selectedOptionIdx == idx
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) LingoTokens.accentPrimary.copy(alpha = 0.16f) else LingoTokens.bgSubtle,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) LingoTokens.accentPrimary else LingoTokens.borderSubtle
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedOptionIdx = idx }
                                        .testTag("detective_interpretation_$idx")
                                ) {
                                    Text(
                                        text = opt,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 13.5.sp,
                                        color = LingoTokens.textPrimary,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = customExplanationInput,
                                onValueChange = { customExplanationInput = it },
                                placeholder = {
                                    Text("Optional: explain the nuance in your own words...", color = LingoTokens.textMuted)
                                },
                                colors = pragmaticsInputColors(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detective_custom_explanation_input"),
                                maxLines = 2
                            )

                            Text(
                                text = "2. Write or speak a natural reply in ${targetLang.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = LingoTokens.textPrimary
                            )

                            OutlinedTextField(
                                value = naturalReplyInput,
                                onValueChange = { naturalReplyInput = it },
                                placeholder = {
                                    Text("Reply naturally to ${c.speakerName} in ${targetLang.name}...", color = LingoTokens.textMuted)
                                },
                                colors = pragmaticsInputColors(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detective_reply_input"),
                                maxLines = 3
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
                                        .testTag("detective_mic_btn")
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
                                    onClick = { submitInvestigation() },
                                    enabled = !isEvaluating && (selectedOptionIdx != null || customExplanationInput.isNotBlank() || naturalReplyInput.isNotBlank()),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LingoTokens.accentBlue,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.4f)
                                        .testTag("detective_submit_btn")
                                ) {
                                    Text(if (isEvaluating) "Analyzing..." else "Check & Reply", maxLines = 1, softWrap = false)
                                }
                            }

                            if (!evalError.isNullOrBlank()) {
                                LingoEmptyOrErrorCard(
                                    title = "AI Analysis Unavailable",
                                    message = evalError!!,
                                    isError = true,
                                    actionLabel = "Retry",
                                    onAction = { submitInvestigation() }
                                )
                            }

                            val eval = evaluation
                            if (eval != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (eval.understoodNuance) LingoTokens.accentEmerald.copy(alpha = 0.14f) else LingoTokens.accentAmber.copy(alpha = 0.14f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (eval.understoodNuance) LingoTokens.accentEmerald.copy(alpha = 0.4f) else LingoTokens.accentAmber.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("detective_evaluation_card")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (eval.understoodNuance) {
                                                "Sharp Pragmatic Read (${eval.score}/100)"
                                            } else {
                                                "Nuance Worth Noting (${eval.score}/100)"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            color = if (eval.understoodNuance) LingoTokens.accentEmerald else LingoTokens.accentAmber
                                        )
                                        Text(
                                            text = eval.feedbackNative,
                                            fontSize = 13.sp,
                                            color = LingoTokens.textPrimary
                                        )
                                        Text(
                                            text = eval.pragmaticExplanationNative,
                                            fontSize = 12.5.sp,
                                            color = LingoTokens.textSecondary
                                        )
                                        if (eval.naturalResponseModelTarget.isNotBlank()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { speechManager.speak(eval.naturalResponseModelTarget, targetLang.code) },
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "Natural reply model: ${eval.naturalResponseModelTarget}",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = LingoTokens.accentPrimary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Icon(
                                                    Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "Hear natural reply",
                                                    tint = LingoTokens.accentPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (continuationTurns.isNotEmpty()) {
                                Text(
                                    text = "Conversation Continuation",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = LingoTokens.textPrimary
                                )
                                continuationTurns.forEach { (who, msg) ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = LingoTokens.bgSubtle,
                                        border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = who,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (who == "You") LingoTokens.accentPrimary else LingoTokens.accentAmber
                                            )
                                            Text(
                                                text = msg,
                                                fontSize = 13.sp,
                                                color = LingoTokens.textPrimary
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
