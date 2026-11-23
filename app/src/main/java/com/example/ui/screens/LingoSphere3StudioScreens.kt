package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.DynamicImmersionMission
import com.example.ai.DynamicImmersionScene
import com.example.ai.DynamicMistakeDrill
import com.example.ai.ExploreAiLearningEngine
import com.example.ai.ExploreAiResult
import com.example.ai.ExploreLearnerContext
import com.example.ai.ExploreSessionRecord
import com.example.ai.GeminiTutorService
import com.example.ai.PracticalFeaturesAiService
import com.example.data.model.ConversationReplaySession
import com.example.data.model.ImmersionDialogueTurn
import com.example.data.model.Language
import com.example.data.model.LingoSphere3Catalog
import com.example.data.model.LivingWorldCharacterProgress
import com.example.data.model.MistakeRecoveryExercise
import com.example.data.model.PortfolioSpeakingSample
import com.example.data.model.PracticalFeaturesCatalog
import com.example.data.model.ReplayTurnItem
import com.example.speech.SpeechManager
import com.example.ui.LingoViewModel
import com.example.ui.components.ConversationRescueButton
import com.example.ui.components.ConversationRescuePanel
import com.example.ui.components.LingoCleanCard
import com.example.ui.components.LingoEmptyOrErrorCard
import com.example.ui.components.LingoSectionHeader
import com.example.ui.components.LingoStatusBadge
import com.example.ui.components.LingoTokens
import com.example.ui.components.LingoTopBar
import kotlinx.coroutines.launch

@Composable
private fun studioInputColors(container: Color = LingoTokens.bgSubtle) = OutlinedTextFieldDefaults.colors(
  focusedTextColor = LingoTokens.textPrimary,
  unfocusedTextColor = LingoTokens.textPrimary,
  focusedBorderColor = LingoTokens.accentPrimary,
  unfocusedBorderColor = LingoTokens.borderSubtle,
  focusedContainerColor = container,
  unfocusedContainerColor = container
)

@Composable
private fun studioChipColors() = FilterChipDefaults.filterChipColors(
  containerColor = LingoTokens.bgElevated,
  labelColor = LingoTokens.textSecondary,
  selectedContainerColor = LingoTokens.accentPrimary.copy(alpha = 0.16f),
  selectedLabelColor = LingoTokens.accentPrimary
)

/**
 * Capability 4: AI Immersion Mode
 * Simulated everyday situations entirely in the target language where AI characters
 * respond dynamically to user speech and decisions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiImmersionModeScreen(
  viewModel: LingoViewModel,
  speechManager: SpeechManager,
  onOpenReplayStudio: () -> Unit,
  onBack: () -> Unit
) {
  BackHandler { onBack() }
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val userProfile by viewModel.userProfileState.collectAsState()
  val learningGoals by viewModel.learningGoalsState.collectAsState()
  val repeatedMistakes by viewModel.repeatedMistakesState.collectAsState()
  val targetLang = Language.findByCode(userProfile.targetLanguageCode)
  val learnerCtx = remember(userProfile, learningGoals, repeatedMistakes) {
    ExploreLearnerContext.fromProfile(userProfile, learningGoals, repeatedMistakes)
  }

  val livingWorldProgressMap by viewModel.livingWorldProgressState.collectAsState()
  val locationCategories = remember {
    listOf("Café", "Airport", "Hotel", "Hospital", "Shop", "Office", "Neighborhood", "Train Station")
  }
  val livingLocations = remember(targetLang.code) {
    PracticalFeaturesCatalog.livingWorldLocationsForLanguage(targetLang.code)
  }
  var selectedLivingLocation by remember(livingLocations) { mutableStateOf(livingLocations.first()) }
  var dynamicScene by remember(targetLang.code) { mutableStateOf<DynamicImmersionScene?>(null) }
  var recentSceneTitles by remember(targetLang.code) { mutableStateOf<List<String>>(emptyList()) }
  var isGeneratingScene by remember { mutableStateOf(false) }
  var sceneGenerationError by remember { mutableStateOf<String?>(null) }
  var selectedLocationCategory by remember { mutableStateOf<String?>(null) }

  // Active character/scene metadata (dynamic AI scene takes priority over initial catalog seed)
  val activeSceneId = dynamicScene?.sceneId ?: selectedLivingLocation.id
  val activePlaceTitle = dynamicScene?.placeNameTarget ?: selectedLivingLocation.nameTargetLanguage
  val activeCharacterName = dynamicScene?.characterName ?: selectedLivingLocation.characterName
  val activeCharacterRole = dynamicScene?.characterRole ?: selectedLivingLocation.characterRole
  val activeDistrict = dynamicScene?.cityAndNeighborhood ?: selectedLivingLocation.districtDescription
  val activeVoiceMale = dynamicScene?.preferredVoiceGender.equals("male", ignoreCase = true)

  val activeCharacterProgress = livingWorldProgressMap[activeSceneId]
    ?: LivingWorldCharacterProgress(
      locationId = activeSceneId,
      characterName = activeCharacterName
    )
  var lastLivingOutcomeNote by remember(activeSceneId) { mutableStateOf<String?>(null) }
  var immersionAiError by remember(activeSceneId) { mutableStateOf<String?>(null) }
  var lastFailedUtterance by remember(activeSceneId) { mutableStateOf<String?>(null) }

  var dialogueTurns by remember(activeSceneId) {
    val openText = dynamicScene?.openingGreetingTarget ?: selectedLivingLocation.openingGreetingTarget
    val openNative = dynamicScene?.openingGreetingNative ?: selectedLivingLocation.openingGreetingEnglish
    val brief = dynamicScene?.situationBriefNative ?: selectedLivingLocation.districtDescription
    mutableStateOf(
      listOf(
        ImmersionDialogueTurn(
          id = "open_$activeSceneId",
          speaker = "character",
          targetLanguageText = openText,
          phoneticHint = openNative,
          decisionImpactNote = brief
        )
      )
    )
  }
  var userDraftText by remember { mutableStateOf("") }
  var isAiResponding by remember { mutableStateOf(false) }
  var savedReplayBanner by remember { mutableStateOf<String?>(null) }
  var showImmersionRescue by remember { mutableStateOf(false) }
  var missionsExpanded by remember(activeSceneId) { mutableStateOf(false) }
  var showTrustTooltip by remember(activeSceneId) { mutableStateOf(false) }
  var revealedTranslationTurnIds by remember(activeSceneId) { mutableStateOf<Set<String>>(emptySet()) }
  var expandedCorrectionTurnIds by remember(activeSceneId) { mutableStateOf<Set<String>>(emptySet()) }
  var micDeniedNotice by remember { mutableStateOf<String?>(null) }
  var liveCaptionsAllEnabled by remember(activeSceneId) { mutableStateOf(false) }
  var showImmersionSummary by remember(activeSceneId) { mutableStateOf(false) }
  val sessionStartedAtEpochMs = remember(activeSceneId) { System.currentTimeMillis() }
  val haptics = LocalHapticFeedback.current

  fun generateNewAiScene(requestedCategory: String? = null) {
    if (isGeneratingScene) return
    selectedLocationCategory = requestedCategory
    isGeneratingScene = true
    sceneGenerationError = null
    immersionAiError = null
    savedReplayBanner = null
    coroutineScope.launch {
      when (val res = ExploreAiLearningEngine.generateDynamicImmersionScene(
        context = context,
        learnerCtx = learnerCtx,
        requestedLocationType = requestedCategory,
        avoidSceneTitles = recentSceneTitles
      )) {
        is ExploreAiResult.Success -> {
          val scene = res.data
          dynamicScene = scene
          recentSceneTitles = (recentSceneTitles + "${scene.placeNameTarget} (${scene.characterName})").takeLast(8)
          dialogueTurns = listOf(
            ImmersionDialogueTurn(
              id = "open_${scene.sceneId}",
              speaker = "character",
              targetLanguageText = scene.openingGreetingTarget,
              phoneticHint = scene.openingGreetingNative,
              decisionImpactNote = scene.situationBriefNative
            )
          )
          lastLivingOutcomeNote = null
          speechManager.speakWithGender(
            text = scene.openingGreetingTarget,
            languageCode = targetLang.code,
            preferMale = scene.preferredVoiceGender.equals("male", ignoreCase = true)
          )
        }
        ExploreAiResult.Offline -> {
          sceneGenerationError = "You appear to be offline. Reconnect and tap Retry to generate a new AI scene."
        }
        is ExploreAiResult.QuotaExceeded -> {
          sceneGenerationError = res.message
        }
        is ExploreAiResult.Error -> {
          sceneGenerationError = res.message
        }
      }
      isGeneratingScene = false
    }
  }

  // Automatically generate a fresh dynamic scene on first entry so users don't always see the same café with Elena
  LaunchedEffect(targetLang.code) {
    if (dynamicScene == null && !android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) {
      generateNewAiScene(null)
    }
  }

  val isListening by speechManager.isListening.collectAsState()
  val spokenText by speechManager.spokenText.collectAsState()
  val speechError by speechManager.speechError.collectAsState()

  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      micDeniedNotice = null
      speechManager.startListening(targetLang.code)
    } else {
      micDeniedNotice = "Microphone permission was denied. You can type your response below or enable mic access in settings."
    }
  }

  fun submitImmersionTurn(utterance: String, fromVoice: Boolean = false) {
    val clean = utterance.trim()
    if (clean.isBlank() || isAiResponding) return
    val effectiveScene = dynamicScene ?: DynamicImmersionScene(
      sceneId = selectedLivingLocation.id,
      locationType = selectedLivingLocation.nameEnglish,
      placeNameTarget = selectedLivingLocation.nameTargetLanguage,
      placeNameNative = selectedLivingLocation.nameEnglish,
      cityAndNeighborhood = selectedLivingLocation.districtDescription,
      characterName = selectedLivingLocation.characterName,
      characterRole = selectedLivingLocation.characterRole,
      characterPersonality = "Warm local host",
      preferredVoiceGender = "female",
      cefrLevel = learnerCtx.cefrLevel,
      situationBriefNative = selectedLivingLocation.districtDescription,
      openingGreetingTarget = selectedLivingLocation.openingGreetingTarget,
      openingGreetingNative = selectedLivingLocation.openingGreetingEnglish,
      missions = selectedLivingLocation.tasks.map {
        DynamicImmersionMission(
          id = it.id,
          title = it.title,
          objectiveDescription = it.communicativeGoal,
          completed = it.id in activeCharacterProgress.completedTaskIds
        )
      },
      starterHintsTarget = emptyList()
    )

    userDraftText = ""
    immersionAiError = null
    isAiResponding = true
    coroutineScope.launch {
      val historyPairs = dialogueTurns.map { t ->
        (if (t.speaker == "user") "Learner" else effectiveScene.characterName) to t.targetLanguageText
      }
      when (val outcomeRes = ExploreAiLearningEngine.advanceDynamicImmersionTurn(
        context = context,
        learnerCtx = learnerCtx,
        scene = effectiveScene,
        conversationHistory = historyPairs,
        rememberedFacts = activeCharacterProgress.rememberedLearnerFacts,
        userUtterance = clean
      )) {
        is ExploreAiResult.Success -> {
          val out = outcomeRes.data
          val userTurn = ImmersionDialogueTurn(
            speaker = "user",
            targetLanguageText = clean,
            naturalnessScore = out.naturalnessScore,
            subtleCorrectionInTargetLang = out.grammarCorrection.ifBlank { null }
          )
          val charTurn = ImmersionDialogueTurn(
            speaker = "character",
            targetLanguageText = out.characterReplyTarget,
            phoneticHint = out.characterReplyNative,
            decisionImpactNote = listOf(out.coachingFeedback, out.vocabularyTip)
              .filter { it.isNotBlank() }
              .joinToString(" • ")
          )
          val updatedTurns = dialogueTurns + userTurn + charTurn
          dialogueTurns = updatedTurns

          // Update mission completion & character memory from real AI evaluation
          val completedId = out.completedMissionId
          if (completedId != null && dynamicScene != null) {
            dynamicScene = dynamicScene!!.copy(
              missions = dynamicScene!!.missions.map { m ->
                if (m.id == completedId) m.copy(completed = true) else m
              }
            )
          }
          val updatedCompletedIds = if (completedId != null) {
            (activeCharacterProgress.completedTaskIds + completedId).distinct()
          } else {
            activeCharacterProgress.completedTaskIds
          }
          val updatedFacts = if (!out.newlyRememberedFact.isNullOrBlank()) {
            (activeCharacterProgress.rememberedLearnerFacts + out.newlyRememberedFact).distinct()
          } else {
            activeCharacterProgress.rememberedLearnerFacts
          }
          val updatedProgress = activeCharacterProgress.copy(
            trustScore = (activeCharacterProgress.trustScore + out.trustDelta).coerceIn(0, 100),
            completedTaskIds = updatedCompletedIds,
            rememberedLearnerFacts = updatedFacts,
            lastConversationSummary = clean.take(90),
            totalSpokenTurns = activeCharacterProgress.totalSpokenTurns + 1
          )
          viewModel.updateLivingWorldCharacterProgress(updatedProgress)
          lastLivingOutcomeNote = listOf(out.coachingFeedback, out.vocabularyTip)
            .filter { it.isNotBlank() }
            .joinToString(" • ")
          lastFailedUtterance = null

          // Log real grammar/vocabulary mistake into Mistake Recovery + Firestore
          out.capturedMistake?.let { mistake ->
            viewModel.recordExploreMistake(
              activitySource = "Living Immersion",
              category = mistake.category,
              originalText = mistake.originalText,
              correctedText = mistake.correctedText,
              explanation = mistake.explanation,
              languageCode = targetLang.code
            )
          }

          speechManager.speakWithGender(
            text = out.characterReplyTarget,
            languageCode = targetLang.code,
            preferMale = activeVoiceMale,
            onDone = {
              if (fromVoice && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                speechManager.startListening(targetLang.code)
              }
            }
          )

          val userTurnCount = updatedTurns.count { it.speaker == "user" }
          ExploreAiLearningEngine.saveExploreSession(
            context = context,
            record = ExploreSessionRecord(
              sessionId = "imm_${effectiveScene.sceneId}",
              featureType = "living_immersion",
              languageCode = targetLang.code,
              title = "${effectiveScene.placeNameTarget} • ${effectiveScene.characterName}",
              summary = "Trust ${updatedProgress.trustScore}% • ${updatedCompletedIds.size} tasks • ${userTurnCount} turns",
              cefrLevel = learnerCtx.cefrLevel,
              turnsCount = updatedTurns.size,
              scorePercent = out.naturalnessScore
            )
          )

          if (userTurnCount >= 2) {
            val replayTurns = updatedTurns.mapIndexed { idx, t ->
              ReplayTurnItem(
                turnId = "imm_t_$idx",
                speaker = if (t.speaker == "user") "user" else "tutor",
                originalText = t.targetLanguageText,
                improvedNativeVersion = t.subtleCorrectionInTargetLang ?: t.targetLanguageText,
                pronunciationIpaOrPhonetic = t.phoneticHint.ifBlank { "Natural ${targetLang.name} phrasing" },
                correctionExplanation = t.decisionImpactNote.ifBlank { "Living Immersion at $activePlaceTitle" },
                initialScore = t.naturalnessScore ?: 85
              )
            }
            viewModel.saveConversationReplaySession(
              ConversationReplaySession(
                id = "replay_${effectiveScene.sceneId}",
                scenarioTitle = "Living World: $activePlaceTitle",
                targetLanguage = targetLang.name,
                cefrLevel = learnerCtx.cefrLevel,
                timestampLabel = "Just now • ${replayTurns.size} turns",
                overallScore = out.naturalnessScore,
                turns = replayTurns
              )
            )
            savedReplayBanner = "Session saved to Replay Studio & Cloud"
          }
        }
        ExploreAiResult.Offline -> {
          immersionAiError = "You are offline. Reconnect and tap Retry to send your message."
          lastFailedUtterance = clean
        }
        is ExploreAiResult.QuotaExceeded -> {
          immersionAiError = outcomeRes.message
          lastFailedUtterance = clean
        }
        is ExploreAiResult.Error -> {
          immersionAiError = outcomeRes.message
          lastFailedUtterance = clean
        }
      }
      isAiResponding = false
    }
  }

  LaunchedEffect(spokenText) {
    val cleanSpoken = spokenText.trim()
    if (cleanSpoken.isNotBlank() && !isAiResponding) {
      speechManager.clearSpokenText()
      submitImmersionTurn(cleanSpoken, fromVoice = true)
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
      title = "Living Immersion Mode",
      subtitle = "${targetLang.name} (${learnerCtx.cefrLevel}) • Live AI characters & missions",
      onBack = onBack,
      actions = {
        TextButton(
          onClick = { generateNewAiScene(null) },
          enabled = !isGeneratingScene,
          modifier = Modifier.testTag("immersion_new_scene_btn")
        ) {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LingoTokens.accentPrimary, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("New Scene", fontSize = 12.sp, color = LingoTokens.accentPrimary, maxLines = 1, softWrap = false)
        }
        TextButton(
          onClick = onOpenReplayStudio,
          modifier = Modifier.testTag("immersion_open_replay_btn")
        ) {
          Icon(Icons.Default.Replay, contentDescription = null, tint = LingoTokens.accentPrimary, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Replay", fontSize = 12.sp, color = LingoTokens.accentPrimary, maxLines = 1, softWrap = false)
        }
      }
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)
    ) {
      // Horizontal Scenario Picker Cards
      val scenarioMeta = remember {
        listOf(
          "Café" to "Order & chat",
          "Airport" to "Check-in & gate",
          "Hotel" to "Stay & requests",
          "Doctor" to "Clinic & symptoms",
          "Job" to "Interview & work",
          "Market" to "Fresh goods & price",
          "Hospital" to "Health visit",
          "Shop" to "Browse & buy",
          "Office" to "Workday talk",
          "Neighborhood" to "Local chat",
          "Train Station" to "Tickets & routes"
        )
      }
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("immersion_scenario_cards_row")
      ) {
        itemsIndexed(scenarioMeta, key = { _, pair -> pair.first }) { idx, (cat, vibe) ->
          val selected = selectedLocationCategory == cat || (selectedLocationCategory == null && idx == 0)
          Surface(
            onClick = {
              if (!isGeneratingScene) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                generateNewAiScene(cat)
              }
            },
            shape = RoundedCornerShape(16.dp),
            color = if (selected) LingoTokens.brandCyan.copy(alpha = 0.16f) else LingoTokens.bgElevated,
            border = BorderStroke(
              width = 1.dp,
              color = if (selected) LingoTokens.brandCyan.copy(alpha = 0.65f) else LingoTokens.borderSubtle
            ),
            modifier = Modifier
              .widthIn(min = 116.dp)
              .heightIn(min = 56.dp)
              .testTag("living_world_location_chip_$idx")
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
              Text(
                text = cat,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) LingoTokens.brandCyan else LingoTokens.textPrimary,
                maxLines = 1
              )
              Text(
                text = vibe,
                fontSize = 11.5.sp,
                color = LingoTokens.textSecondary,
                maxLines = 1
              )
            }
          }
        }
      }

      if (isGeneratingScene) {
        LingoEmptyOrErrorCard(
          title = "Creating Live ${targetLang.name} Scene",
          message = "Generating a fresh character, setting, and CEFR ${learnerCtx.cefrLevel} missions...",
          isLoading = true,
          modifier = Modifier.padding(bottom = 8.dp)
        )
      } else if (!sceneGenerationError.isNullOrBlank()) {
        LingoEmptyOrErrorCard(
          title = "Could Not Generate New Scene",
          message = sceneGenerationError!!,
          isError = true,
          actionLabel = "Retry",
          onAction = { generateNewAiScene(selectedLocationCategory) },
          modifier = Modifier.padding(bottom = 8.dp)
        )
      }

      val activeMissions = dynamicScene?.missions ?: selectedLivingLocation.tasks.map {
        DynamicImmersionMission(
          id = it.id,
          title = it.title,
          objectiveDescription = it.communicativeGoal,
          completed = it.id in activeCharacterProgress.completedTaskIds
        )
      }
      val doneMissionsCount = activeMissions.count { it.completed || it.id in activeCharacterProgress.completedTaskIds }
      val totalMissionsCount = activeMissions.size
      val trustPercent = activeCharacterProgress.trustScore.coerceIn(0, 100)

      LingoCleanCard(
        contentPadding = 12.dp,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("living_world_header_card")
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "$activePlaceTitle • $activeCharacterName",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = LingoTokens.textPrimary,
                maxLines = 2
              )
              Text(
                text = "$activeCharacterRole • $activeDistrict",
                fontSize = 12.sp,
                color = LingoTokens.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
              onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val openText = dynamicScene?.openingGreetingTarget ?: selectedLivingLocation.openingGreetingTarget
                val openNative = dynamicScene?.openingGreetingNative ?: selectedLivingLocation.openingGreetingEnglish
                val brief = dynamicScene?.situationBriefNative ?: selectedLivingLocation.districtDescription
                dialogueTurns = listOf(
                  ImmersionDialogueTurn(
                    id = "restart_${System.currentTimeMillis()}",
                    speaker = "character",
                    targetLanguageText = openText,
                    phoneticHint = openNative,
                    decisionImpactNote = brief
                  )
                )
                immersionAiError = null
                lastLivingOutcomeNote = null
                revealedTranslationTurnIds = emptySet()
              },
              modifier = Modifier.size(48.dp).testTag("immersion_restart_btn")
            ) {
              Icon(Icons.Default.Refresh, contentDescription = "Restart scene", tint = LingoTokens.textSecondary, modifier = Modifier.size(20.dp))
            }
          }

          // Compact Trust Progress Bar + Tooltip Trigger + Collapsible "Missions X/Y" Chip
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showTrustTooltip = !showTrustTooltip
              },
              shape = RoundedCornerShape(12.dp),
              color = LingoTokens.bgSubtle,
              border = BorderStroke(1.dp, LingoTokens.accentEmerald.copy(alpha = 0.45f)),
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("immersion_trust_info_btn")
            ) {
              Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Trust $trustPercent%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = LingoTokens.accentEmerald
                  )
                  Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Explain trust score",
                    tint = LingoTokens.textSecondary,
                    modifier = Modifier.size(15.dp)
                  )
                }
                LinearProgressIndicator(
                  progress = { trustPercent / 100f },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .testTag("immersion_trust_progress"),
                  color = LingoTokens.accentEmerald,
                  trackColor = LingoTokens.bgElevated
                )
              }
            }

            Surface(
              onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                missionsExpanded = !missionsExpanded
              },
              shape = RoundedCornerShape(12.dp),
              color = if (missionsExpanded) LingoTokens.brandCyan.copy(alpha = 0.16f) else LingoTokens.bgSubtle,
              border = BorderStroke(
                1.dp,
                if (missionsExpanded) LingoTokens.brandCyan.copy(alpha = 0.55f) else LingoTokens.borderSubtle
              ),
              modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("immersion_missions_toggle")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Text(
                  text = "Missions $doneMissionsCount/$totalMissionsCount",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (doneMissionsCount == totalMissionsCount && totalMissionsCount > 0) LingoTokens.accentEmerald else LingoTokens.textPrimary
                )
                Icon(
                  imageVector = if (missionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                  contentDescription = if (missionsExpanded) "Collapse missions" else "Expand missions",
                  tint = LingoTokens.textSecondary,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }

          AnimatedVisibility(
            visible = showTrustTooltip,
            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = LingoTokens.accentEmerald.copy(alpha = 0.12f),
              border = BorderStroke(1.dp, LingoTokens.accentEmerald.copy(alpha = 0.35f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("immersion_trust_tooltip")
            ) {
              Text(
                text = "Trust measures how natural and polite you sound to $activeCharacterName. Complete missions and reply naturally to raise trust and earn bonus XP.",
                fontSize = 12.sp,
                color = LingoTokens.textPrimary,
                lineHeight = 17.sp,
                modifier = Modifier.padding(10.dp)
              )
            }
          }

          AnimatedVisibility(
            visible = missionsExpanded,
            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
          ) {
            Column(
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .testTag("immersion_missions_list")
            ) {
              activeMissions.forEachIndexed { tIdx, task ->
                val done = task.completed || task.id in activeCharacterProgress.completedTaskIds
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.testTag("living_world_task_$tIdx")
                ) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (done) LingoTokens.accentEmerald else LingoTokens.textMuted,
                    modifier = Modifier.size(16.dp)
                  )
                  Text(
                    text = "${task.title} — ${task.objectiveDescription}",
                    fontSize = 12.sp,
                    fontWeight = if (done) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (done) LingoTokens.accentEmerald else LingoTokens.textSecondary
                  )
                }
              }
            }
          }

          if (activeCharacterProgress.rememberedLearnerFacts.isNotEmpty()) {
            Text(
              text = "Remembered: ${activeCharacterProgress.rememberedLearnerFacts.takeLast(2).joinToString(" • ")}",
              fontSize = 12.sp,
              color = LingoTokens.accentPrimary,
              fontWeight = FontWeight.Medium
            )
          }
          if (!micDeniedNotice.isNullOrBlank()) {
            Text(
              text = micDeniedNotice!!,
              fontSize = 12.sp,
              color = LingoTokens.accentAmber
            )
          } else if (!speechError.isNullOrBlank()) {
            Text(
              text = speechError!!,
              fontSize = 12.sp,
              color = LingoTokens.accentAmber
            )
          }
        }
      }

      if (savedReplayBanner != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = LingoTokens.accentEmerald.copy(alpha = 0.14f),
          border = BorderStroke(1.dp, LingoTokens.accentEmerald.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = savedReplayBanner!!,
              fontSize = 12.sp,
              color = LingoTokens.accentEmerald,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onOpenReplayStudio) {
              Text("Open", fontSize = 12.sp, color = LingoTokens.accentPrimary)
            }
          }
        }
      }

      // Real Chat Bubbles: AI left, User right, single target text + Translate toggle + Tap-to-correct
      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(vertical = 8.dp)
          .testTag("immersion_chat_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(dialogueTurns, key = { it.id }) { turn ->
          val isCharacter = turn.speaker == "character"
          val hasDistinctTranslation = isCharacter &&
            turn.phoneticHint.isNotBlank() &&
            turn.phoneticHint.trim().lowercase() != turn.targetLanguageText.trim().lowercase()
          val isTranslationShown = liveCaptionsAllEnabled || (turn.id in revealedTranslationTurnIds)
          val hasCorrection = !isCharacter &&
            !turn.subtleCorrectionInTargetLang.isNullOrBlank() &&
            turn.subtleCorrectionInTargetLang.trim() != turn.targetLanguageText.trim()
          val isCorrectionShown = turn.id in expandedCorrectionTurnIds

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isCharacter) Arrangement.Start else Arrangement.End
          ) {
            Surface(
              shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isCharacter) 6.dp else 20.dp,
                bottomEnd = if (isCharacter) 20.dp else 6.dp
              ),
              color = if (isCharacter) LingoTokens.bgElevated else LingoTokens.brandIndigo.copy(alpha = 0.24f),
              border = BorderStroke(
                width = 1.dp,
                color = if (isCharacter) LingoTokens.borderSubtle else LingoTokens.brandIndigo.copy(alpha = 0.5f)
              ),
              modifier = Modifier
                .widthIn(max = 304.dp)
                .then(
                  if (hasCorrection) {
                    Modifier.clickable {
                      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      expandedCorrectionTurnIds = if (isCorrectionShown) {
                        expandedCorrectionTurnIds - turn.id
                      } else {
                        expandedCorrectionTurnIds + turn.id
                      }
                    }
                  } else Modifier
                )
                .testTag("immersion_bubble_${turn.id}")
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = if (isCharacter) activeCharacterName else "You · ${turn.naturalnessScore ?: 80}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isCharacter) LingoTokens.brandCyan else LingoTokens.textSecondary
                  )
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasDistinctTranslation) {
                      Surface(
                        onClick = {
                          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                          revealedTranslationTurnIds = if (isTranslationShown) {
                            revealedTranslationTurnIds - turn.id
                          } else {
                            revealedTranslationTurnIds + turn.id
                          }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isTranslationShown) LingoTokens.brandCyan.copy(alpha = 0.18f) else LingoTokens.bgSubtle,
                        modifier = Modifier.testTag("immersion_translate_toggle_${turn.id}")
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                          Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Toggle translation",
                            tint = LingoTokens.brandCyan,
                            modifier = Modifier.size(14.dp)
                          )
                          Text(
                            text = if (isTranslationShown) "Hide" else "Translate",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = LingoTokens.brandCyan
                          )
                        }
                      }
                    }
                    IconButton(
                      onClick = {
                        speechManager.speakWithGender(
                          text = turn.targetLanguageText,
                          languageCode = targetLang.code,
                          preferMale = isCharacter && activeVoiceMale
                        )
                      },
                      modifier = Modifier
                        .size(40.dp)
                        .testTag("immersion_speak_btn_${turn.id}")
                    ) {
                      Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Listen to message",
                        tint = LingoTokens.brandCyan,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }
                }

                // Target-language message shown ONCE
                Text(
                  text = turn.targetLanguageText,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Medium,
                  color = LingoTokens.textPrimary,
                  lineHeight = 20.sp
                )

                // On-demand native translation (never duplicated automatically)
                AnimatedVisibility(
                  visible = hasDistinctTranslation && isTranslationShown,
                  enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                  exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                ) {
                  Text(
                    text = turn.phoneticHint,
                    fontSize = 13.sp,
                    color = LingoTokens.textSecondary,
                    lineHeight = 18.sp,
                    modifier = Modifier.testTag("immersion_translation_text_${turn.id}")
                  )
                }

                // Tap-to-correct indicator & inline grammar fix on User bubbles
                if (hasCorrection) {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LingoTokens.accentEmerald.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, LingoTokens.accentEmerald.copy(alpha = 0.35f)),
                    modifier = Modifier.testTag("immersion_tap_to_correct_${turn.id}")
                  ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                      Text(
                        text = if (isCorrectionShown) "Natural phrasing (tap to hide)" else "Tap to see native correction",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = LingoTokens.accentEmerald
                      )
                      AnimatedVisibility(
                        visible = isCorrectionShown,
                        enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                        exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                      ) {
                        Text(
                          text = turn.subtleCorrectionInTargetLang!!,
                          fontSize = 13.sp,
                          color = LingoTokens.textPrimary,
                          fontWeight = FontWeight.Medium,
                          modifier = Modifier.padding(top = 4.dp)
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }

        if (isAiResponding) {
          item(key = "immersion_typing_indicator") {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .testTag("immersion_typing_indicator"),
              horizontalArrangement = Arrangement.Start
            ) {
              Surface(
                shape = RoundedCornerShape(18.dp),
                color = LingoTokens.bgElevated,
                border = BorderStroke(1.dp, LingoTokens.borderSubtle)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = LingoTokens.brandCyan,
                    strokeWidth = 2.dp
                  )
                  Text(
                    text = "$activeCharacterName is typing…",
                    fontSize = 12.sp,
                    color = LingoTokens.textSecondary
                  )
                }
              }
            }
          }
        }
      }

      if (!immersionAiError.isNullOrBlank()) {
        LingoEmptyOrErrorCard(
          title = "AI Character Unavailable",
          message = immersionAiError!!,
          isError = true,
          actionLabel = if (lastFailedUtterance != null) "Retry" else null,
          onAction = lastFailedUtterance?.let { msg -> { submitImmersionTurn(msg) } },
          modifier = Modifier.padding(bottom = 8.dp)
        )
      }

      val userTurnsInSession = remember(dialogueTurns) {
        dialogueTurns.filter { it.speaker == "user" }
      }
      val distinctWordsUsed = remember(userTurnsInSession) {
        userTurnsInSession
          .flatMap { it.targetLanguageText.lowercase().split(Regex("[^\\p{L}0-9']+")) }
          .filter { it.length >= 3 }
          .distinct()
      }
      val elapsedSessionSec = ((System.currentTimeMillis() - sessionStartedAtEpochMs) / 1000L).coerceAtLeast(1L)
      val isValidImmersionSession = userTurnsInSession.size >= 3 && elapsedSessionSec >= 30L

      if (showImmersionSummary) {
        LingoCleanCard(
          borderColor = LingoTokens.accentEmerald.copy(alpha = 0.5f),
          contentPadding = 12.dp,
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .testTag("immersion_end_summary_card")
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Immersion Session Summary",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = LingoTokens.textPrimary
              )
              LingoStatusBadge(
                text = if (isValidImmersionSession) "Valid Session • +25 XP" else "Practice Preview (<3 turns or <30s)",
                color = if (isValidImmersionSession) LingoTokens.accentEmerald else LingoTokens.accentAmber
              )
            }
            Text(
              text = "Turns completed: ${userTurnsInSession.size} • Duration: ${elapsedSessionSec}s • Missions: $doneMissionsCount/$totalMissionsCount • Trust: $trustPercent%",
              fontSize = 12.sp,
              color = LingoTokens.textSecondary
            )
            if (distinctWordsUsed.isNotEmpty()) {
              Text(
                text = "Vocabulary you used (${distinctWordsUsed.size}): ${distinctWordsUsed.take(8).joinToString(", ")}",
                fontSize = 12.sp,
                color = LingoTokens.brandCyan,
                fontWeight = FontWeight.Medium
              )
            }
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              TextButton(onClick = { showImmersionSummary = false }) {
                Text("Continue Scene", fontSize = 12.sp, color = LingoTokens.accentPrimary)
              }
              if (userTurnsInSession.size >= 2) {
                TextButton(onClick = onOpenReplayStudio) {
                  Text("Review in Call Replay", fontSize = 12.sp, color = LingoTokens.accentEmerald)
                }
              }
            }
          }
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        FilterChip(
          selected = showImmersionRescue,
          onClick = { showImmersionRescue = !showImmersionRescue },
          label = { Text("Hint", fontSize = 11.5.sp) },
          colors = studioChipColors(),
          modifier = Modifier.testTag("living_world_rescue_button")
        )
        FilterChip(
          selected = false,
          onClick = {
            val lastAi = dialogueTurns.lastOrNull { it.speaker == "character" }?.targetLanguageText.orEmpty()
            if (lastAi.isNotBlank() && !isAiResponding) {
              submitImmersionTurn("Could you please say that more simply?")
            }
          },
          label = { Text("Simplify", fontSize = 11.5.sp) },
          colors = studioChipColors(),
          modifier = Modifier.testTag("immersion_simplify_btn")
        )
        FilterChip(
          selected = liveCaptionsAllEnabled,
          onClick = { liveCaptionsAllEnabled = !liveCaptionsAllEnabled },
          label = { Text(if (liveCaptionsAllEnabled) "Captions On" else "Live Captions", fontSize = 11.5.sp) },
          colors = studioChipColors(),
          modifier = Modifier.testTag("immersion_live_captions_btn")
        )
        FilterChip(
          selected = showImmersionSummary,
          onClick = { showImmersionSummary = !showImmersionSummary },
          label = { Text("End Session", fontSize = 11.5.sp) },
          colors = studioChipColors(),
          modifier = Modifier.testTag("immersion_end_session_btn")
        )
      }

      ConversationRescuePanel(
        visible = showImmersionRescue,
        languageCode = targetLang.code,
        lastPartnerPrompt = dialogueTurns.lastOrNull { it.speaker == "character" }?.targetLanguageText ?: "",
        learnerStruggledDraft = userDraftText,
        sessionTitle = "Living World: $activePlaceTitle",
        onSpeakPreview = { phrase -> speechManager.speak(phrase, targetLang.code) },
        onRetryWithPhrase = { phrase ->
          showImmersionRescue = false
          submitImmersionTurn(phrase)
        },
        onPracticeLoadedIntoDraft = { phrase ->
          userDraftText = phrase
          showImmersionRescue = false
        },
        onDismiss = { showImmersionRescue = false },
        modifier = Modifier.padding(top = 6.dp)
      )

      Spacer(modifier = Modifier.height(6.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp)
      ) {
        IconButton(
          onClick = {
            if (isListening) {
              speechManager.stopListening()
            } else {
              speechManager.clearSpokenText()
              val hasMic = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
              ) == PackageManager.PERMISSION_GRANTED
              if (hasMic) {
                speechManager.startListening(targetLang.code)
              } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
              }
            }
          },
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isListening) LingoTokens.accentRose else LingoTokens.accentPrimary.copy(alpha = 0.16f))
            .testTag("immersion_mic_btn")
        ) {
          Icon(
            if (isListening) Icons.Default.Stop else Icons.Default.Mic,
            contentDescription = "Speak in Immersion",
            tint = if (isListening) Color.White else LingoTokens.accentPrimary
          )
        }

        OutlinedTextField(
          value = userDraftText,
          onValueChange = { userDraftText = it },
          placeholder = {
            Text(
              "Respond in ${targetLang.name}...",
              fontSize = 13.sp,
              color = LingoTokens.textMuted
            )
          },
          colors = studioInputColors(LingoTokens.bgElevated),
          shape = RoundedCornerShape(14.dp),
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .testTag("immersion_text_input")
        )

        IconButton(
          onClick = { submitImmersionTurn(userDraftText) },
          enabled = userDraftText.isNotBlank() && !isAiResponding,
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (userDraftText.isNotBlank() && !isAiResponding) LingoTokens.accentBlue else LingoTokens.bgInteractive)
            .testTag("immersion_send_btn")
        ) {
          if (isAiResponding) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              color = Color.White,
              strokeWidth = 2.dp
            )
          } else {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
          }
        }
      }
    }
  }
}

/**
 * Capability 7: AI Conversation Replay
 */
@Composable
fun AiConversationReplayScreen(
  viewModel: LingoViewModel,
  speechManager: SpeechManager,
  onBack: () -> Unit
) {
  BackHandler { onBack() }
  val context = LocalContext.current
  val userProfile by viewModel.userProfileState.collectAsState()
  val targetLang = Language.findByCode(userProfile.targetLanguageCode)
  val sessions by viewModel.conversationReplaySessionsState.collectAsState()

  var selectedSessionId by remember(sessions) {
    mutableStateOf(sessions.firstOrNull()?.id.orEmpty())
  }
  val activeSession = sessions.firstOrNull { it.id == selectedSessionId } ?: sessions.firstOrNull()
  var selectedTurnId by remember(activeSession?.id) {
    mutableStateOf(activeSession?.turns?.firstOrNull { it.speaker == "user" }?.turnId ?: activeSession?.turns?.firstOrNull()?.turnId)
  }
  var practiceInputText by remember(selectedTurnId) { mutableStateOf("") }

  val isListening by speechManager.isListening.collectAsState()
  val spokenText by speechManager.spokenText.collectAsState()

  LaunchedEffect(spokenText) {
    if (spokenText.isNotBlank()) {
      practiceInputText = spokenText
    }
  }

  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      speechManager.startListening(targetLang.code)
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
      title = "Conversation Replay",
      subtitle = "${targetLang.name} • Review corrections & re-practice turns",
      onBack = onBack
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(vertical = 16.dp)
    ) {
      if (sessions.isEmpty()) {
        item {
          LingoEmptyOrErrorCard(
            title = "No calls yet, start your first call.",
            message = "Complete 2 or more turns in Speak, Roleplay, or AI Immersion Mode to unlock turn-by-turn ${targetLang.name} call replays."
          )
        }
      } else {
        if (sessions.size >= 3) {
          item {
            LingoCleanCard(
              modifier = Modifier
                .fillMaxWidth()
                .testTag("replay_fluency_trend_card")
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Fluency Trend Across Calls",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = LingoTokens.textPrimary
                  )
                  val avgScore = sessions.take(6).map { it.overallScore }.average().toInt()
                  LingoStatusBadge(
                    text = "Avg $avgScore%",
                    color = LingoTokens.accentEmerald
                  )
                }
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.Bottom
                ) {
                  sessions.take(6).reversed().forEachIndexed { idx, s ->
                    val ratio = (s.overallScore.coerceIn(10, 100) / 100f)
                    Column(
                      modifier = Modifier.weight(1f),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.Bottom
                    ) {
                      Text(
                        text = "${s.overallScore}%",
                        fontSize = 10.sp,
                        color = LingoTokens.textSecondary
                      )
                      Spacer(modifier = Modifier.height(3.dp))
                      Box(
                        modifier = Modifier
                          .fillMaxWidth(0.6f)
                          .height((40.dp * ratio).coerceAtLeast(6.dp))
                          .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                          .background(
                            if (s.id == activeSession?.id) LingoTokens.brandCyan
                            else LingoTokens.accentPrimary.copy(alpha = 0.55f)
                          )
                      )
                      Text(
                        text = "#${idx + 1}",
                        fontSize = 10.sp,
                        color = LingoTokens.textMuted
                      )
                    }
                  }
                }
              }
            }
          }
        }
        item {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
          ) {
            sessions.forEach { s ->
              val selected = s.id == activeSession?.id
              FilterChip(
                selected = selected,
                onClick = { selectedSessionId = s.id },
                label = {
                  Text(
                    "${s.scenarioTitle} (${s.overallScore}%)",
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                  )
                },
                colors = studioChipColors(),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = selected,
                  borderColor = LingoTokens.borderSubtle,
                  selectedBorderColor = LingoTokens.accentPrimary.copy(alpha = 0.6f)
                ),
                modifier = Modifier.testTag("replay_session_chip_${s.id}")
              )
            }
          }
        }

        if (activeSession != null) {
          items(activeSession.turns, key = { it.turnId }) { turn ->
            val isSelected = turn.turnId == selectedTurnId
            val isUserTurn = turn.speaker == "user"

            LingoCleanCard(
              containerColor = if (isSelected) LingoTokens.accentBlue.copy(alpha = 0.12f) else LingoTokens.bgElevated,
              borderColor = if (isSelected) LingoTokens.accentPrimary else LingoTokens.borderSubtle,
              onClick = { selectedTurnId = turn.turnId },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("replay_turn_card_${turn.turnId}")
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = if (isUserTurn) "You Said • ${turn.initialScore}%" else "AI Tutor",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isUserTurn) LingoTokens.accentPrimary else LingoTokens.accentEmerald,
                    modifier = Modifier.weight(1f)
                  )
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    if (turn.practicedRetryScore != null) {
                      LingoStatusBadge(
                        text = "Retried ${turn.practicedRetryScore}%",
                        color = LingoTokens.accentEmerald
                      )
                    }
                    OutlinedButton(
                      onClick = {
                        speechManager.speakWithRate(turn.improvedNativeVersion, targetLang.code, 0.95f)
                      },
                      border = BorderStroke(1.dp, LingoTokens.borderFocus),
                      colors = ButtonDefaults.outlinedButtonColors(contentColor = LingoTokens.accentPrimary),
                      shape = RoundedCornerShape(10.dp),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                      modifier = Modifier.testTag("replay_hear_improved_${turn.turnId}")
                    ) {
                      Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(15.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Hear", fontSize = 11.5.sp)
                    }
                  }
                }

                Text(
                  text = turn.originalText,
                  fontSize = 14.5.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = LingoTokens.textPrimary
                )

                if (isSelected) {
                  HorizontalDivider(color = LingoTokens.borderSubtle)
                  Text(
                    text = "Improved Native Phrasing",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = LingoTokens.accentEmerald
                  )
                  Text(
                    text = turn.improvedNativeVersion,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = LingoTokens.textPrimary
                  )
                  if (turn.pronunciationIpaOrPhonetic.isNotBlank()) {
                    Text(
                      text = "Pronunciation: / ${turn.pronunciationIpaOrPhonetic} /",
                      fontSize = 12.sp,
                      color = LingoTokens.accentPrimary
                    )
                  }
                  Text(
                    text = turn.correctionExplanation,
                    fontSize = 12.sp,
                    color = LingoTokens.textSecondary
                  )

                  OutlinedTextField(
                    value = practiceInputText,
                    onValueChange = { practiceInputText = it },
                    placeholder = {
                      Text(
                        "Speak or type the improved version...",
                        fontSize = 12.sp,
                        color = LingoTokens.textMuted
                      )
                    },
                    colors = studioInputColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("replay_practice_input_${turn.turnId}")
                  )

                  if (practiceInputText.isBlank()) {
                    TextButton(
                      onClick = { practiceInputText = turn.improvedNativeVersion }
                    ) {
                      Text("Load Improved Phrasing", fontSize = 12.sp, color = LingoTokens.accentPrimary)
                    }
                  }

                  Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    OutlinedButton(
                      onClick = {
                        if (isListening) {
                          speechManager.stopListening()
                        } else {
                          speechManager.clearSpokenText()
                          val hasMic = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                          ) == PackageManager.PERMISSION_GRANTED
                          if (hasMic) {
                            speechManager.startListening(targetLang.code)
                          } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                          }
                        }
                      },
                      border = BorderStroke(1.dp, LingoTokens.borderFocus),
                      colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isListening) LingoTokens.accentAmber else LingoTokens.accentPrimary
                      ),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.weight(1f)
                    ) {
                      Icon(
                        if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(if (isListening) "Stop" else "Speak", fontSize = 12.sp)
                    }

                    Button(
                      onClick = {
                        val attempt = practiceInputText.trim()
                        val targetTokens = turn.improvedNativeVersion.lowercase()
                          .split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
                        val attemptTokens = attempt.lowercase()
                          .split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
                        val overlap = if (targetTokens.isEmpty()) 0f
                        else attemptTokens.intersect(targetTokens).size.toFloat() / targetTokens.size.toFloat()
                        val newScore = when {
                          attempt.equals(turn.improvedNativeVersion, ignoreCase = true) -> 98
                          overlap >= 0.7f -> (turn.initialScore + 12).coerceIn(85, 96)
                          overlap >= 0.4f -> 78
                          else -> 65
                        }
                        viewModel.updateReplayTurnPracticeScore(
                          sessionId = activeSession.id,
                          turnId = turn.turnId,
                          practicedTranscript = attempt,
                          newScore = newScore,
                          targetLanguage = activeSession.targetLanguage
                        )
                      },
                      enabled = practiceInputText.isNotBlank(),
                      colors = ButtonDefaults.buttonColors(
                        containerColor = LingoTokens.accentEmerald,
                        contentColor = Color.White
                      ),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier
                        .weight(1f)
                        .testTag("replay_score_retry_btn_${turn.turnId}")
                    ) {
                      Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Score", fontSize = 12.sp)
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

/**
 * Capability 1 & 5: AI Language Memory & Intelligent Mistake Recovery
 */
@Composable
fun IntelligentMistakeRecoveryScreen(
  viewModel: LingoViewModel,
  speechManager: SpeechManager,
  onBack: () -> Unit
) {
  BackHandler { onBack() }
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val userProfile by viewModel.userProfileState.collectAsState()
  val targetLang = Language.findByCode(userProfile.targetLanguageCode)

  val memoryProfile by viewModel.aiLanguageMemoryState.collectAsState()
  val repeatedMistakes by viewModel.repeatedMistakesState.collectAsState()
  val learningGoals by viewModel.learningGoalsState.collectAsState()
  val learnerCtx = remember(userProfile, learningGoals, repeatedMistakes) {
    ExploreLearnerContext.fromProfile(userProfile, learningGoals, repeatedMistakes)
  }

  var newGoalText by remember { mutableStateOf("") }
  var editingGoalIndex by remember { mutableStateOf<Int?>(null) }
  var recoveryDrills by remember { mutableStateOf<List<DynamicMistakeDrill>>(emptyList()) }
  var isLoadingExercises by remember { mutableStateOf(false) }
  var drillsError by remember { mutableStateOf<String?>(null) }
  var activeExerciseIdx by remember(targetLang.code) { mutableIntStateOf(0) }
  var exerciseAnswers by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
  var exerciseFeedbackMap by remember { mutableStateOf<Map<String, Pair<Boolean, String>>>(emptyMap()) }
  var isEvaluatingAnswer by remember { mutableStateOf(false) }

  val isListening by speechManager.isListening.collectAsState()
  val spokenText by speechManager.spokenText.collectAsState()

  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) speechManager.startListening(targetLang.code)
  }

  fun loadDrillsFromActualMistakes() {
    if (repeatedMistakes.isEmpty()) {
      recoveryDrills = emptyList()
      drillsError = null
      isLoadingExercises = false
      return
    }
    isLoadingExercises = true
    drillsError = null
    coroutineScope.launch {
      when (val res = ExploreAiLearningEngine.generateMistakeRecoveryDrills(context, learnerCtx, repeatedMistakes)) {
        is ExploreAiResult.Success -> {
          recoveryDrills = res.data
          activeExerciseIdx = 0
        }
        ExploreAiResult.Offline -> {
          // Offline deterministic drill from the learner's own real recorded mistakes (never fabricated mistakes)
          recoveryDrills = repeatedMistakes.take(5).mapIndexed { idx, m ->
            DynamicMistakeDrill(
              id = "local_drill_${m.id}_$idx",
              mistakeId = m.id,
              category = m.category,
              promptNative = "Fix this ${targetLang.name} sentence from your recorded mistakes:",
              flawedSentenceTarget = m.exampleWrong,
              expectedCorrectedTarget = m.exampleCorrected,
              ruleExplanationNative = m.ruleSummary
            )
          }
          drillsError = "Offline mode: practicing directly from your saved mistakes."
        }
        is ExploreAiResult.QuotaExceeded -> {
          drillsError = res.message
        }
        is ExploreAiResult.Error -> {
          drillsError = res.message
        }
      }
      isLoadingExercises = false
    }
  }

  LaunchedEffect(targetLang.name, repeatedMistakes.size) {
    viewModel.refreshAiLanguageMemoryProfile(targetLang.name)
    loadDrillsFromActualMistakes()
  }

  LaunchedEffect(spokenText, recoveryDrills, activeExerciseIdx) {
    val activeEx = recoveryDrills.getOrNull(activeExerciseIdx)
    if (activeEx != null && spokenText.isNotBlank()) {
      exerciseAnswers = exerciseAnswers + (activeEx.id to spokenText)
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
      title = "Mistake Recovery",
      subtitle = "${targetLang.name} • Focus areas, drills & progress",
      onBack = onBack
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      contentPadding = PaddingValues(vertical = 16.dp)
    ) {
      // 1. Your Learning Focus
      item {
        LingoSectionHeader(
          title = "Your Learning Focus",
          subtitle = "Patterns remembered from your ${targetLang.name} sessions"
        )
      }

      item {
        LingoCleanCard(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_language_memory_card")
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Psychology, contentDescription = null, tint = LingoTokens.accentPrimary)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "${targetLang.name} Memory Summary",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = LingoTokens.textPrimary
              )
            }

            if (memoryProfile.vocabularyWeaknesses.isEmpty() &&
              memoryProfile.pronunciationDifficulties.isEmpty() &&
              memoryProfile.conversationHistorySummaries.isEmpty()
            ) {
              Text(
                text = "No ${targetLang.name} patterns logged yet. Speak or complete a lesson to build your focus areas.",
                fontSize = 13.sp,
                color = LingoTokens.textSecondary
              )
            } else {
              if (memoryProfile.vocabularyWeaknesses.isNotEmpty()) {
                Text(
                  text = "• Vocabulary: ${memoryProfile.vocabularyWeaknesses.take(3).joinToString(", ")}",
                  fontSize = 13.sp,
                  color = LingoTokens.textPrimary
                )
              }
              if (memoryProfile.pronunciationDifficulties.isNotEmpty()) {
                Text(
                  text = "• Pronunciation: ${memoryProfile.pronunciationDifficulties.take(3).joinToString(", ")}",
                  fontSize = 13.sp,
                  color = LingoTokens.textPrimary
                )
              }
              if (memoryProfile.conversationHistorySummaries.isNotEmpty()) {
                Text(
                  text = "• Recent memory: ${memoryProfile.conversationHistorySummaries.take(2).joinToString(" • ")}",
                  fontSize = 13.sp,
                  color = LingoTokens.textSecondary
                )
              }
            }
          }
        }
      }

      // 2. Practice Your Mistakes (1 active drill card at a time)
      item {
        LingoSectionHeader(
          title = "Practice Your Mistakes",
          subtitle = if (recoveryDrills.isNotEmpty()) {
            "Exercise ${activeExerciseIdx.coerceAtMost(recoveryDrills.lastIndex) + 1} of ${recoveryDrills.size}"
          } else null
        )
      }

      if (!drillsError.isNullOrBlank()) {
        item {
          LingoEmptyOrErrorCard(
            title = "Recovery Drill Notice",
            message = drillsError!!,
            isError = recoveryDrills.isEmpty(),
            actionLabel = if (repeatedMistakes.isNotEmpty()) "Retry AI Drills" else null,
            onAction = if (repeatedMistakes.isNotEmpty()) ({ loadDrillsFromActualMistakes() }) else null
          )
        }
      }

      if (isLoadingExercises) {
        item {
          LingoEmptyOrErrorCard(
            title = "Building Recovery Drills",
            message = "Generating personalized ${targetLang.name} spaced-repetition drills from your recorded mistakes...",
            isLoading = true
          )
        }
      } else if (repeatedMistakes.isEmpty() || recoveryDrills.isEmpty()) {
        item {
          LingoEmptyOrErrorCard(
            title = "No ${targetLang.name} Mistakes Recorded Yet",
            message = "Complete Speaking, Roleplay, Debate, Living Immersion, Camera Lens, Conversation Repair, or Writing practice. Real grammar and phrasing mistakes will be saved here for spaced-repetition recovery."
          )
        }
      } else {
        val safeIdx = activeExerciseIdx.coerceIn(0, recoveryDrills.lastIndex)
        val ex = recoveryDrills[safeIdx]
        val matchingMistake = repeatedMistakes.firstOrNull { it.id == ex.mistakeId }
        val draft = exerciseAnswers[ex.id].orEmpty()
        val feedback = exerciseFeedbackMap[ex.id]

        item {
          LingoCleanCard(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("mistake_recovery_exercise_$safeIdx")
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                LingoStatusBadge(
                  text = ex.category,
                  color = LingoTokens.accentPrimary
                )
                IconButton(
                  onClick = { speechManager.speak(ex.expectedCorrectedTarget, targetLang.code) },
                  modifier = Modifier.size(44.dp)
                ) {
                  Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Hear correct sentence",
                    tint = LingoTokens.accentPrimary
                  )
                }
              }

              Text(
                text = ex.promptNative,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = LingoTokens.textPrimary
              )

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
                  val wrongText = ex.flawedSentenceTarget.ifBlank { matchingMistake?.exampleWrong.orEmpty() }
                  if (wrongText.isNotBlank()) {
                    Text(
                      text = "Habit to fix: \"$wrongText\"",
                      fontSize = 12.5.sp,
                      color = LingoTokens.accentAmber
                    )
                  }
                  Text(
                    text = "Rule: ${ex.ruleExplanationNative.ifBlank { matchingMistake?.ruleSummary.orEmpty() }}",
                    fontSize = 12.sp,
                    color = LingoTokens.textSecondary
                  )
                }
              }

              OutlinedTextField(
                value = draft,
                onValueChange = { exerciseAnswers = exerciseAnswers + (ex.id to it) },
                placeholder = {
                  Text(
                    "Type or speak the corrected ${targetLang.name} sentence...",
                    fontSize = 12.5.sp,
                    color = LingoTokens.textMuted
                  )
                },
                colors = studioInputColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("mistake_recovery_input_$safeIdx")
              )

              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                OutlinedButton(
                  onClick = {
                    if (isListening) {
                      speechManager.stopListening()
                    } else {
                      speechManager.clearSpokenText()
                      val hasMic = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                      ) == PackageManager.PERMISSION_GRANTED
                      if (hasMic) speechManager.startListening(targetLang.code)
                      else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                  },
                  border = BorderStroke(1.dp, LingoTokens.borderFocus),
                  colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isListening) LingoTokens.accentAmber else LingoTokens.accentPrimary
                  ),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(
                    if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(if (isListening) "Stop" else "Speak", fontSize = 12.sp, maxLines = 1, softWrap = false)
                }

                Button(
                  onClick = {
                    val userAns = draft.trim()
                    if (userAns.isBlank() || isEvaluatingAnswer) return@Button
                    isEvaluatingAnswer = true
                    coroutineScope.launch {
                      when (val evalRes = ExploreAiLearningEngine.evaluateMistakeRecoveryResponse(
                        context = context,
                        learnerCtx = learnerCtx,
                        drill = ex,
                        learnerAnswer = userAns
                      )) {
                        is ExploreAiResult.Success -> {
                          val ev = evalRes.data
                          viewModel.recordMistakeRecoveryAttempt(ex.mistakeId, ev.score, targetLang.name)
                          exerciseFeedbackMap = exerciseFeedbackMap + (
                            ex.id to (ev.passed to "Score ${ev.score}% • ${ev.feedbackNative} • Model: \"${ev.correctedModelTarget}\"")
                          )
                        }
                        else -> {
                          // Honest exact-match check if offline/AI unavailable
                          val exact = userAns.equals(ex.expectedCorrectedTarget, ignoreCase = true)
                          val fallbackScore = if (exact) 95 else 60
                          viewModel.recordMistakeRecoveryAttempt(ex.mistakeId, fallbackScore, targetLang.name)
                          exerciseFeedbackMap = exerciseFeedbackMap + (
                            ex.id to (exact to "Score $fallbackScore% • Model: \"${ex.expectedCorrectedTarget}\" — ${ex.ruleExplanationNative}")
                          )
                        }
                      }
                      isEvaluatingAnswer = false
                    }
                  },
                  enabled = draft.isNotBlank() && !isEvaluatingAnswer,
                  colors = ButtonDefaults.buttonColors(
                    containerColor = LingoTokens.accentEmerald,
                    contentColor = Color.White
                  ),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .weight(1.3f)
                    .testTag("mistake_recovery_verify_btn_$safeIdx")
                ) {
                  if (isEvaluatingAnswer) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                  } else {
                    Text("Check Answer", fontSize = 12.sp, maxLines = 1, softWrap = false)
                  }
                }
              }

              if (feedback != null) {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = (if (feedback.first) LingoTokens.accentEmerald else LingoTokens.accentAmber).copy(alpha = 0.14f),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = feedback.second,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (feedback.first) LingoTokens.accentEmerald else LingoTokens.accentAmber,
                    modifier = Modifier.padding(10.dp)
                  )
                }
              }

              if (recoveryDrills.size > 1) {
                HorizontalDivider(color = LingoTokens.borderSubtle)
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  OutlinedButton(
                    onClick = { if (safeIdx > 0) activeExerciseIdx = safeIdx - 1 },
                    enabled = safeIdx > 0,
                    border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                    shape = RoundedCornerShape(10.dp)
                  ) {
                    Text("Previous", fontSize = 12.sp, maxLines = 1, softWrap = false)
                  }
                  Text(
                    text = "${safeIdx + 1} / ${recoveryDrills.size}",
                    fontSize = 12.sp,
                    color = LingoTokens.textSecondary
                  )
                  OutlinedButton(
                    onClick = { if (safeIdx < recoveryDrills.lastIndex) activeExerciseIdx = safeIdx + 1 },
                    enabled = safeIdx < recoveryDrills.lastIndex,
                    border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                    shape = RoundedCornerShape(10.dp)
                  ) {
                    Text("Next", fontSize = 12.sp, maxLines = 1, softWrap = false)
                  }
                }
              }
            }
          }
        }
      }

      // 3. Your Goals (Add, Edit, and Delete personal learning goals)
      item {
        LingoSectionHeader(title = "Your Goals")
      }

      item {
        LingoCleanCard(modifier = Modifier.fillMaxWidth()) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (learningGoals.isEmpty()) {
              Text(
                text = "Add a personal ${targetLang.name} speaking goal to guide your AI tutor.",
                fontSize = 12.5.sp,
                color = LingoTokens.textSecondary
              )
            } else {
              learningGoals.forEachIndexed { gIdx, goal ->
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = "• $goal",
                    fontSize = 13.sp,
                    color = LingoTokens.textPrimary,
                    modifier = Modifier.weight(1f)
                  )
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                      onClick = {
                        editingGoalIndex = gIdx
                        newGoalText = goal
                      },
                      modifier = Modifier.size(36.dp).testTag("ai_memory_edit_goal_$gIdx")
                    ) {
                      Icon(Icons.Default.Edit, contentDescription = "Edit goal", tint = LingoTokens.accentPrimary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                      onClick = {
                        val updated = learningGoals.toMutableList().apply { removeAt(gIdx) }
                        if (editingGoalIndex == gIdx) {
                          editingGoalIndex = null
                          newGoalText = ""
                        }
                        viewModel.updateLearningGoals(updated, targetLang.name)
                      },
                      modifier = Modifier.size(36.dp).testTag("ai_memory_delete_goal_$gIdx")
                    ) {
                      Icon(Icons.Default.Delete, contentDescription = "Delete goal", tint = LingoTokens.textSecondary, modifier = Modifier.size(16.dp))
                    }
                  }
                }
              }
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              OutlinedTextField(
                value = newGoalText,
                onValueChange = { newGoalText = it },
                placeholder = {
                  Text(
                    if (editingGoalIndex != null) "Update your goal..." else "Add a ${targetLang.name} goal...",
                    fontSize = 12.5.sp,
                    color = LingoTokens.textMuted
                  )
                },
                colors = studioInputColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                  .weight(1f)
                  .testTag("ai_memory_new_goal_input")
              )
              Button(
                onClick = {
                  val clean = newGoalText.trim()
                  if (clean.isNotBlank()) {
                    val idxToEdit = editingGoalIndex
                    val updated = if (idxToEdit != null && idxToEdit in learningGoals.indices) {
                      learningGoals.toMutableList().apply { set(idxToEdit, clean) }
                    } else {
                      learningGoals + clean
                    }
                    viewModel.updateLearningGoals(updated, targetLang.name)
                    newGoalText = ""
                    editingGoalIndex = null
                  }
                },
                enabled = newGoalText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                  containerColor = LingoTokens.accentBlue,
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("ai_memory_add_goal_btn")
              ) {
                Text(if (editingGoalIndex != null) "Save" else "Add", fontSize = 12.5.sp, maxLines = 1, softWrap = false)
              }
            }
          }
        }
      }

      // 4. Improvement History
      item {
        LingoSectionHeader(title = "Improvement History")
      }

      if (repeatedMistakes.isEmpty()) {
        item {
          LingoEmptyOrErrorCard(
            title = "No Mistake History Yet",
            message = "Your ${targetLang.name} mastery progress over recurring grammar habits will appear here."
          )
        }
      } else {
        items(repeatedMistakes, key = { it.id }) { mistake ->
          LingoCleanCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "${mistake.category} (${mistake.occurrenceCount}x)",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = LingoTokens.textPrimary,
                  modifier = Modifier.weight(1f)
                )
                LingoStatusBadge(
                  text = if (mistake.improved) "Improved • ${mistake.masteryScore}%" else "Mastery ${mistake.masteryScore}%",
                  color = if (mistake.improved) LingoTokens.accentEmerald else LingoTokens.accentAmber
                )
              }
              Text(
                text = "\"${mistake.exampleWrong}\" → \"${mistake.exampleCorrected}\"",
                fontSize = 13.sp,
                color = LingoTokens.textPrimary
              )
              Text(
                text = mistake.ruleSummary,
                fontSize = 12.sp,
                color = LingoTokens.textSecondary
              )
              LinearProgressIndicator(
                progress = { (mistake.masteryScore / 100f).coerceIn(0.05f, 1f) },
                color = if (mistake.improved) LingoTokens.accentEmerald else LingoTokens.accentBlue,
                trackColor = LingoTokens.bgSubtle,
                modifier = Modifier.fillMaxWidth()
              )
            }
          }
        }
      }
    }
  }
}
