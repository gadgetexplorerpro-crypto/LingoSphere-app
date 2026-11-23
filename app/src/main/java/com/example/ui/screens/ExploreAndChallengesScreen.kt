package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.AiErrorHandler
import com.example.ai.AiUiError
import com.example.ai.GeminiTutorService
import com.example.ai.UpgradeSheetMode
import com.example.data.model.CommunityProfile
import com.example.data.model.FlashcardProgress
import com.example.data.model.Language
import com.example.data.model.LessonProgress
import com.example.data.model.SpeakingSession
import com.example.data.model.UserNotification
import com.example.data.model.UserProfile
import com.example.data.repository.AccessPolicyFeatureKeys
import com.example.speech.SpeechManager
import com.example.ui.components.LingoCalmAiErrorBanner
import com.example.ui.components.LingoCleanCard
import com.example.ui.components.LingoEmptyOrErrorCard
import com.example.ui.components.LingoGoldLockIcon
import com.example.ui.components.LingoStatusBadge
import com.example.ui.components.LingoTokens
import com.example.ui.components.LingoTopBar
import kotlinx.coroutines.launch

// ============================================================================
// 1. EXPLORE LANGUAGES SCREEN
// ============================================================================

@Composable
fun ExploreLanguagesScreen(
  userProfile: UserProfile,
  communityProfiles: List<CommunityProfile>,
  speechManager: SpeechManager,
  onSelectTargetLanguage: (String) -> Unit,
  onSelectNativeLanguage: (String) -> Unit,
  onBack: () -> Unit
) {
  BackHandler { onBack() }
  var searchQuery by remember { mutableStateOf("") }
  val filteredLanguages = remember(searchQuery) {
    val q = searchQuery.trim()
    if (q.isBlank()) Language.SUPPORTED_LANGUAGES
    else Language.SUPPORTED_LANGUAGES.filter {
      it.name.contains(q, ignoreCase = true) ||
        it.nativeName.contains(q, ignoreCase = true) ||
        it.writingSystem.contains(q, ignoreCase = true) ||
        it.code.contains(q, ignoreCase = true)
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
      title = "Explore Languages",
      subtitle = "${Language.SUPPORTED_LANGUAGES.size} world languages • Audio & script support",
      onBack = onBack
    )

    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = {
            Text(
              "Search language, native name, or script...",
              style = MaterialTheme.typography.bodyMedium,
              color = LingoTokens.textSecondary
            )
          },
          leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = LingoTokens.accentPrimary)
          },
          trailingIcon = {
            if (searchQuery.isNotBlank()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Close, contentDescription = "Clear search", tint = LingoTokens.textSecondary)
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LingoTokens.accentPrimary,
            unfocusedBorderColor = LingoTokens.borderSubtle,
            focusedContainerColor = LingoTokens.bgElevated,
            unfocusedContainerColor = LingoTokens.bgElevated,
            focusedTextColor = LingoTokens.textPrimary,
            unfocusedTextColor = LingoTokens.textPrimary
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("explore_languages_search_input")
        )
      }

      if (filteredLanguages.isEmpty()) {
        item {
          LingoEmptyOrErrorCard(
            title = "No matching languages",
            message = "Try searching for \"Urdu\", \"German\", \"Japanese\", or \"Arabic\".",
            actionLabel = "Show All",
            onAction = { searchQuery = "" }
          )
        }
      } else {
        items(filteredLanguages, key = { it.code }) { lang ->
          val isTarget = userProfile.targetLanguageCode == lang.code
          val isNative = userProfile.nativeLanguageCode == lang.code
          val activeLearnersCount = communityProfiles.count { it.targetLanguageCode == lang.code } + (if (isTarget) 1 else 0)
          val sampleGreeting = lang.sampleGreeting

          LingoCleanCard(
            containerColor = if (isTarget) LingoTokens.accentPrimary.copy(alpha = 0.09f) else LingoTokens.bgElevated,
            borderColor = if (isTarget) LingoTokens.accentPrimary.copy(alpha = 0.6f) else LingoTokens.borderSubtle,
            contentPadding = 16.dp
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(text = lang.flagEmoji, fontSize = 32.sp)
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    text = "${lang.name} (${lang.nativeName})",
                    style = MaterialTheme.typography.titleSmall,
                    color = LingoTokens.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                  )
                  if (lang.isRtl) {
                    LingoStatusBadge(text = "RTL", color = LingoTokens.accentAmber)
                  }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "${lang.writingSystem} • \"$sampleGreeting\"",
                  style = MaterialTheme.typography.bodySmall,
                  color = LingoTokens.textSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                if (activeLearnersCount > 0) {
                  Text(
                    text = "$activeLearnersCount active learner(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = LingoTokens.accentPrimary
                  )
                }
              }
              IconButton(
                onClick = { speechManager.speak(sampleGreeting, lang.code) },
                modifier = Modifier.size(48.dp)
              ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Hear Sample", tint = LingoTokens.accentPrimary)
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Button(
                onClick = { onSelectTargetLanguage(lang.code) },
                enabled = !isTarget,
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isTarget) LingoTokens.accentEmerald else LingoTokens.accentBlue,
                  contentColor = Color.White,
                  disabledContainerColor = LingoTokens.accentEmerald.copy(alpha = 0.22f),
                  disabledContentColor = LingoTokens.accentEmerald
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text(
                  text = if (isTarget) "Active Target" else "Learn ${lang.name}",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1
                )
              }

              OutlinedButton(
                onClick = { onSelectNativeLanguage(lang.code) },
                enabled = !isNative,
                border = BorderStroke(1.dp, if (isNative) LingoTokens.accentEmerald.copy(alpha = 0.5f) else LingoTokens.borderFocus),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text(
                  text = if (isNative) "Native Tongue" else "Set as Native",
                  fontSize = 12.sp,
                  color = if (isNative) LingoTokens.accentEmerald else LingoTokens.accentPrimary,
                  maxLines = 1
                )
              }
            }
          }
        }
      }
    }
  }
}

// ============================================================================
// 2. DAILY CHALLENGES SCREEN
// ============================================================================

@Composable
fun DailyChallengesScreen(
  userProfile: UserProfile,
  lessonProgress: List<LessonProgress>,
  flashcards: List<FlashcardProgress>,
  speakingSessions: List<SpeakingSession>,
  onNavigateToLearn: () -> Unit,
  onNavigateToSpeak: () -> Unit,
  onNavigateToPractice: () -> Unit,
  onBack: () -> Unit,
  speechManager: SpeechManager? = null,
  completedMissions: List<com.example.data.model.DailyLifeMissionEvaluation> = emptyList(),
  onCompleteMission: (com.example.data.model.DailyLifeMissionEvaluation, Int) -> Unit = { _, _ -> }
) {
  BackHandler { onBack() }
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val targetLang = Language.findByCode(userProfile.targetLanguageCode)

  val missions = remember(targetLang.name, userProfile.proficiencyLevel) {
    com.example.data.model.LingoSphere3Catalog.dailyLifeMissionsForLanguage(
      language = targetLang.name,
      cefrLevel = userProfile.proficiencyLevel
    )
  }
  var activeMissionId by remember(missions) { mutableStateOf(missions.first().id) }
  var missionDrafts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
  var missionModes by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
  var missionEvaluations by remember(completedMissions) {
    mutableStateOf(completedMissions.associateBy { it.missionId })
  }
  var evaluatingMissionId by remember { mutableStateOf<String?>(null) }
  var missionErrorBanner by remember { mutableStateOf<String?>(null) }
  val isVerifiedPro = userProfile.isEntitlementVerified

  val isListening = speechManager?.isListening?.collectAsState()?.value ?: false
  val spokenText = speechManager?.spokenText?.collectAsState()?.value.orEmpty()

  LaunchedEffect(spokenText, activeMissionId) {
    if (spokenText.isNotBlank()) {
      missionDrafts = missionDrafts + (activeMissionId to spokenText)
      missionModes = missionModes + (activeMissionId to "Spoken")
    }
  }

  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      speechManager?.startListening(targetLang.code)
    }
  }

  val completedLessonsCount = userProfile.completedLessonIds.size
  val speakingCount = userProfile.dailyAiSpeakingCount.toInt()
  val masteredCardsCount = flashcards.count { it.boxLevel >= 2L }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(LingoTokens.bgCanvas)
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .imePadding()
  ) {
    LingoTopBar(
      title = "Daily Missions & Quests",
      subtitle = "Speech & written missions tailored to ${userProfile.proficiencyLevel}",
      onBack = onBack
    )

    Column(
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      LingoCleanCard(borderColor = LingoTokens.accentAmber.copy(alpha = 0.45f), contentPadding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = LingoTokens.accentAmber.copy(alpha = 0.16f),
            modifier = Modifier.size(48.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = LingoTokens.accentAmber, modifier = Modifier.size(26.dp))
            }
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "${userProfile.currentStreak}-Day Active Streak",
              style = MaterialTheme.typography.titleMedium,
              color = LingoTokens.textPrimary,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Longest: ${userProfile.longestStreak} days • Daily goal: ${userProfile.dailyGoalMinutes} mins",
              style = MaterialTheme.typography.bodySmall,
              color = LingoTokens.textSecondary
            )
          }
        }
      }

      val habitMissions = remember(targetLang.code) {
        com.example.data.model.PracticalFeaturesCatalog.habitMissionsForLanguage(targetLang.code)
      }
      var selectedRoutineIds by remember {
        mutableStateOf(
          setOf(
            com.example.data.model.HabitRoutineSlot.MORNING_COFFEE.id,
            com.example.data.model.HabitRoutineSlot.EVENING_REFLECTION.id
          )
        )
      }
      var activeHabitMission by remember(habitMissions) { mutableStateOf(habitMissions.first()) }
      var habitTranscriptInput by remember(activeHabitMission.id) { mutableStateOf("") }
      var verifiedHabitCompletedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
      var habitFeedbackText by remember(activeHabitMission.id) { mutableStateOf<String?>(null) }

      LingoCleanCard(
        borderColor = LingoTokens.accentPrimary.copy(alpha = 0.4f),
        contentPadding = 16.dp,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("language_habit_coach_card")
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Language Habit Coach",
            style = MaterialTheme.typography.titleMedium,
            color = LingoTokens.textPrimary,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Practice short ${targetLang.name} check-ins anchored to daily routines.",
            style = MaterialTheme.typography.bodySmall,
            color = LingoTokens.textSecondary
          )

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            habitMissions.forEachIndexed { hIdx, hm ->
              val isDone = hm.id in verifiedHabitCompletedIds
              FilterChip(
                selected = activeHabitMission.id == hm.id,
                onClick = {
                  activeHabitMission = hm
                  selectedRoutineIds = selectedRoutineIds + hm.routineSlot.id
                },
                label = {
                  Text(
                    text = if (isDone) "✓ ${hm.routineSlot.label}" else hm.routineSlot.label,
                    fontSize = 12.sp
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = LingoTokens.accentPrimary.copy(alpha = 0.18f),
                  selectedLabelColor = LingoTokens.accentPrimary,
                  containerColor = LingoTokens.bgSubtle,
                  labelColor = if (isDone) LingoTokens.accentEmerald else LingoTokens.textSecondary
                ),
                modifier = Modifier.testTag("habit_routine_chip_$hIdx")
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = LingoTokens.bgSubtle,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "${activeHabitMission.missionTitle} (${activeHabitMission.routineSlot.defaultTimeWindow})",
                color = LingoTokens.accentAmber,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              )
              Text(
                text = activeHabitMission.speakingPromptTarget,
                color = LingoTokens.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
              )
              Text(
                text = activeHabitMission.speakingPromptEnglish,
                color = LingoTokens.textSecondary,
                fontSize = 12.sp
              )
              Text(
                text = "Suggested words: ${activeHabitMission.targetKeywords.joinToString(" • ")}",
                color = LingoTokens.accentPrimary,
                fontSize = 11.5.sp
              )

              OutlinedTextField(
                value = habitTranscriptInput,
                onValueChange = { habitTranscriptInput = it },
                placeholder = {
                  Text("Type or speak your ${targetLang.name} check-in (3+ words)...", fontSize = 12.sp, color = LingoTokens.textSecondary)
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("habit_coach_input"),
                maxLines = 3
              )

              Button(
                onClick = {
                  val candidate = habitTranscriptInput.trim()
                  val wordCount = candidate.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                  if (wordCount >= 3) {
                    verifiedHabitCompletedIds = verifiedHabitCompletedIds + activeHabitMission.id
                    habitFeedbackText = "✓ Habit Practice Logged ($wordCount words for ${activeHabitMission.routineSlot.label})"
                  } else {
                    habitFeedbackText = "Enter or speak at least 3 words in ${targetLang.name} to log this routine."
                  }
                },
                enabled = habitTranscriptInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = LingoTokens.accentBlue, contentColor = Color.White),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("habit_coach_verify_button")
              ) {
                Text("Verify Routine Practice", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }

              if (habitFeedbackText != null) {
                Text(
                  text = habitFeedbackText!!,
                  color = if (activeHabitMission.id in verifiedHabitCompletedIds) LingoTokens.accentEmerald else LingoTokens.accentAmber,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.testTag("habit_coach_verified_banner")
                )
              }
            }
          }
        }
      }

      // LingoSphere 3.0: Daily Life Language Missions (Assessed via Real Speech or Written Response)
      Text(
        text = "Daily Life Missions (${targetLang.flagEmoji} ${targetLang.name})",
        style = MaterialTheme.typography.titleMedium,
        color = LingoTokens.textPrimary,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "Assessed using your spoken voice or written ${targetLang.name} responses.",
        style = MaterialTheme.typography.bodySmall,
        color = LingoTokens.textSecondary
      )

      missions.forEachIndexed { idx, mission ->
        val isExpanded = activeMissionId == mission.id
        val draft = missionDrafts[mission.id].orEmpty()
        val eval = missionEvaluations[mission.id]
        val isDone = eval?.completedSuccessfully == true

        LingoCleanCard(
          onClick = { activeMissionId = mission.id },
          borderColor = if (isDone) LingoTokens.accentEmerald else if (isExpanded) LingoTokens.accentPrimary else LingoTokens.borderSubtle,
          contentPadding = 16.dp,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_life_mission_card_$idx")
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = mission.title,
                  color = LingoTokens.textPrimary,
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp
                )
                Text(
                  text = "${mission.cefrLevel} • ~${mission.recommendedDurationSeconds}s • +${mission.xpReward} XP",
                  color = LingoTokens.accentPrimary,
                  fontSize = 11.5.sp
                )
              }
              LingoStatusBadge(
                text = if (isDone) "PASSED ${eval?.overallScore}%" else "MISSION",
                color = if (isDone) LingoTokens.accentEmerald else LingoTokens.accentPrimary
              )
            }

            Text(
              text = mission.promptDescription,
              color = LingoTokens.textSecondary,
              fontSize = 12.5.sp
            )

            if (isExpanded) {
              Text(
                text = "Checkpoints: ${mission.requiredElements.joinToString(" • ")}",
                color = LingoTokens.accentAmber,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "Starter prompts: ${mission.starterPhrasesInTargetLang.joinToString("  |  ")}",
                color = LingoTokens.accentPrimary,
                fontSize = 11.5.sp
              )

              OutlinedTextField(
                value = draft,
                onValueChange = {
                  missionDrafts = missionDrafts + (mission.id to it)
                  if (missionModes[mission.id] == null) {
                    missionModes = missionModes + (mission.id to "Written")
                  }
                },
                placeholder = {
                  Text(
                    "Speak with Mic or write your full ${targetLang.name} response...",
                    color = LingoTokens.textSecondary,
                    fontSize = 12.sp
                  )
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("daily_mission_response_input_$idx")
              )

              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                OutlinedButton(
                  onClick = {
                    if (speechManager != null) {
                      if (isListening) {
                        speechManager.stopListening()
                      } else {
                        speechManager.clearSpokenText()
                        missionModes = missionModes + (mission.id to "Spoken")
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
                    }
                  },
                  border = BorderStroke(1.dp, LingoTokens.borderFocus),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("daily_mission_speak_btn_$idx")
                ) {
                  Icon(
                    if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null,
                    tint = LingoTokens.accentPrimary,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(if (isListening) "Stop Mic" else "Speak Response", color = LingoTokens.textPrimary, fontSize = 12.sp)
                }

                Button(
                  onClick = {
                    if (!isVerifiedPro) {
                      AiErrorHandler.requestUpgrade(
                        mode = UpgradeSheetMode.AI_UPGRADE,
                        featureKey = AccessPolicyFeatureKeys.PERSONALIZED_MATERIAL,
                        headline = "Unlock AI Mission Feedback",
                        reason = "Upgrade to LingoSphere Pro for AI scoring and personalized feedback on Daily Life Missions."
                      )
                      return@Button
                    }
                    val responseText = draft.trim()
                    if (responseText.isNotBlank() && evaluatingMissionId == null) {
                      evaluatingMissionId = mission.id
                      missionErrorBanner = null
                      val mode = missionModes[mission.id] ?: "Written"
                      coroutineScope.launch {
                        try {
                          val audioPath = if (mode == "Spoken") {
                            speechManager?.exportLastRecordingToPortfolio("mission_${mission.id}")
                          } else null
                          val result = GeminiTutorService.evaluateDailyLifeMissionResponse(
                            mission = mission,
                            userResponseText = responseText,
                            submissionMode = mode,
                            audioSampleFilePath = audioPath
                          )
                          missionEvaluations = missionEvaluations + (mission.id to result)
                          onCompleteMission(result, mission.xpReward)
                        } catch (e: Exception) {
                          val handled = AiErrorHandler.handle(e, contextTag = "DailyLifeMission")
                          if (handled is AiUiError.ProRequired) {
                            AiErrorHandler.requestUpgrade(
                              mode = UpgradeSheetMode.AI_UPGRADE,
                              featureKey = AccessPolicyFeatureKeys.PERSONALIZED_MATERIAL
                            )
                          } else {
                            missionErrorBanner = handled.userMessage
                          }
                        } finally {
                          evaluatingMissionId = null
                        }
                      }
                    }
                  },
                  enabled = draft.isNotBlank() && evaluatingMissionId != mission.id,
                  colors = ButtonDefaults.buttonColors(containerColor = LingoTokens.accentBlue, contentColor = Color.White),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("daily_mission_submit_btn_$idx")
                ) {
                  if (!isVerifiedPro) {
                    LingoGoldLockIcon(size = 14.dp)
                  } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                  }
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    if (evaluatingMissionId == mission.id) "Assessing..." else "Assess Mission",
                    fontSize = 12.sp
                  )
                }
              }

              if (!missionErrorBanner.isNullOrBlank()) {
                LingoCalmAiErrorBanner(
                  message = missionErrorBanner,
                  onDismiss = { missionErrorBanner = null }
                )
              }

              if (eval != null) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = LingoTokens.bgSubtle,
                  border = BorderStroke(1.dp, if (eval.completedSuccessfully) LingoTokens.accentEmerald else LingoTokens.accentAmber),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_mission_eval_box_$idx")
                ) {
                  Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                      text = "${eval.submissionMode} Assessment: ${eval.overallScore}% • Fluency ${eval.fluencyAndCoherenceScore}% • Grammar ${eval.grammarAndVocabScore}%",
                      color = if (eval.completedSuccessfully) LingoTokens.accentEmerald else LingoTokens.accentAmber,
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.sp
                    )
                    Text(eval.aiFeedback, color = LingoTokens.textPrimary, fontSize = 12.sp)
                    Text(
                      text = "Model Response: ${eval.improvedModelResponse}",
                      color = LingoTokens.accentPrimary,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                  }
                }
              }
            }
          }
        }
      }

      ChallengeQuestCard(
        title = "1. Complete a Curriculum Lesson",
        description = "Finish any Vocabulary, Grammar, Reading, or Listening module.",
        current = completedLessonsCount.coerceAtMost(1),
        target = 1,
        xpReward = 50,
        actionLabel = "Go to Lessons",
        onAction = onNavigateToLearn
      )

      ChallengeQuestCard(
        title = "2. Practice 3 Speaking Turns with AI Tutor",
        description = "Speak aloud with the AI Voice Tutor or Roleplay partner today.",
        current = speakingCount.coerceAtMost(3),
        target = 3,
        xpReward = 75,
        actionLabel = "Speak Now",
        onAction = onNavigateToSpeak
      )

      ChallengeQuestCard(
        title = "3. Level Up 3 Vocabulary Flashcards",
        description = "Review spaced-repetition flashcards to Box 2 or higher.",
        current = masteredCardsCount.coerceAtMost(3),
        target = 3,
        xpReward = 40,
        actionLabel = "Review Cards",
        onAction = onNavigateToPractice
      )

      ChallengeQuestCard(
        title = "4. Reach 200+ Total XP",
        description = "Accumulate XP across lessons, quizzes, and voice sessions.",
        current = userProfile.xpPoints.toInt().coerceAtMost(200),
        target = 200,
        xpReward = 100,
        actionLabel = "Earn XP",
        onAction = onNavigateToLearn
      )
    }
  }
}

@Composable
private fun ChallengeQuestCard(
  title: String,
  description: String,
  current: Int,
  target: Int,
  xpReward: Int,
  actionLabel: String,
  onAction: () -> Unit
) {
  val completed = current >= target
  val progress = (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)

  LingoCleanCard(
    borderColor = if (completed) LingoTokens.accentEmerald.copy(alpha = 0.6f) else LingoTokens.borderSubtle,
    contentPadding = 16.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = title,
        color = LingoTokens.textPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        modifier = Modifier.weight(1f)
      )
      LingoStatusBadge(
        text = if (completed) "COMPLETED" else "+$xpReward XP",
        color = if (completed) LingoTokens.accentEmerald else LingoTokens.accentPrimary
      )
    }

    Spacer(modifier = Modifier.height(6.dp))
    Text(description, color = LingoTokens.textSecondary, fontSize = 12.5.sp)
    Spacer(modifier = Modifier.height(10.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
      LinearProgressIndicator(
        progress = { progress },
        color = if (completed) LingoTokens.accentEmerald else LingoTokens.accentPrimary,
        trackColor = LingoTokens.bgSubtle,
        modifier = Modifier
          .weight(1f)
          .height(8.dp)
          .clip(RoundedCornerShape(50))
      )
      Spacer(modifier = Modifier.width(12.dp))
      Text(
        text = "$current / $target",
        color = LingoTokens.textPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
      )
    }

    if (!completed) {
      Spacer(modifier = Modifier.height(10.dp))
      Button(
        onClick = onAction,
        colors = ButtonDefaults.buttonColors(containerColor = LingoTokens.accentBlue, contentColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.align(Alignment.End)
      ) {
        Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

// ============================================================================
// 3. PRONUNCIATION PRACTICE SCREEN
// ============================================================================

@Composable
fun PronunciationPracticeScreen(
  userProfile: UserProfile,
  speechManager: SpeechManager,
  onSaveSession: (SpeakingSession) -> Unit,
  onBack: () -> Unit,
  pronunciationHistory: List<com.example.data.model.PronunciationDiagnosticResult> = emptyList(),
  onRecordDiagnostic: (com.example.data.model.PronunciationDiagnosticResult) -> Unit = {},
  initialWordOfTheDay: com.example.notifications.WordOfTheDayEntry? = null,
  onOpenSubscription: () -> Unit = {}
) {
  PronunciationCoachScreen(
    userProfile = userProfile,
    speechManager = speechManager,
    onSaveSession = onSaveSession,
    onBack = onBack,
    pronunciationHistory = pronunciationHistory,
    onRecordDiagnostic = onRecordDiagnostic,
    initialWordOfTheDay = initialWordOfTheDay,
    onOpenSubscription = onOpenSubscription
  )
}

// ============================================================================
// 4. PROGRESS DASHBOARD SCREEN
// ============================================================================

@Composable
fun ProgressDashboardScreen(
  userProfile: UserProfile,
  lessonProgress: List<LessonProgress>,
  flashcards: List<FlashcardProgress>,
  speakingSessions: List<SpeakingSession>,
  onBack: () -> Unit,
  weeklyReport: com.example.data.repository.WeeklyLearningReportSummary? = null,
  onSyncOfflineNow: () -> Unit = {},
  onOpenLanguagePortfolio: () -> Unit = {}
) {
  BackHandler { onBack() }

  val avgFluency = if (speakingSessions.isNotEmpty()) {
    speakingSessions.map { it.fluencyScore }.average().toInt()
  } else 0

  val masteredVocab = flashcards.count { it.mastered || it.boxLevel >= 4L }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(LingoTokens.bgCanvas)
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
  ) {
    LingoTopBar(
      title = "Progress & Analytics",
      subtitle = "Fluency, vocabulary mastery & weekly study insights",
      onBack = onBack
    )

    Column(
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        AnalyticsMetricCard("Total XP", "${userProfile.xpPoints}", LingoTokens.accentPrimary, Modifier.weight(1f))
        AnalyticsMetricCard("Avg Fluency", "$avgFluency%", LingoTokens.accentEmerald, Modifier.weight(1f))
        AnalyticsMetricCard("Words Learned", "${flashcards.size}", LingoTokens.accentAmber, Modifier.weight(1f))
      }

      LingoCleanCard(
        borderColor = LingoTokens.accentPrimary.copy(alpha = 0.4f),
        contentPadding = 16.dp,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("skill_passport_progress_card")
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Language Skill Passport",
            color = LingoTokens.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.5.sp
          )
          Text(
            text = "Practice Portfolio • Spoken Samples • Practical Competencies",
            color = LingoTokens.accentPrimary,
            fontSize = 12.sp
          )
          Text(
            text = "Track documented speaking recordings, writing revisions, and CEFR practice evidence.",
            color = LingoTokens.textSecondary,
            fontSize = 12.5.sp,
            lineHeight = 18.sp
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            Button(
              onClick = onOpenLanguagePortfolio,
              colors = ButtonDefaults.buttonColors(containerColor = LingoTokens.accentBlue, contentColor = Color.White),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("open_skill_passport_btn")
            ) {
              Text("Open Skill Passport", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }
        }
      }

      LingoCleanCard(contentPadding = 16.dp) {
        Text(
          text = "Recent AI Voice & Roleplay Sessions (${speakingSessions.size})",
          color = LingoTokens.textPrimary,
          fontWeight = FontWeight.Bold,
          fontSize = 15.5.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (speakingSessions.isEmpty()) {
          Text(
            text = "No speaking sessions recorded yet. Start a Voice Call or AI Tutor conversation to track your fluency.",
            color = LingoTokens.textSecondary,
            fontSize = 12.5.sp
          )
        } else {
          speakingSessions.take(6).forEach { sess ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${sess.scenarioTitle} (${sess.mode})",
                  color = LingoTokens.textPrimary,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.5.sp
                )
                Text(
                  text = sess.grammarFeedbackSummary,
                  color = LingoTokens.textSecondary,
                  fontSize = 11.5.sp,
                  maxLines = 2
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "${sess.fluencyScore}%",
                color = LingoTokens.accentEmerald,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
            }
          }
        }
      }

      LingoCleanCard(contentPadding = 16.dp) {
        Text(
          text = "Curriculum Mastery & Vocabulary",
          color = LingoTokens.textPrimary,
          fontWeight = FontWeight.Bold,
          fontSize = 15.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "• Completed Modules: ${userProfile.completedLessonIds.size}\n" +
            "• High-Mastery Flashcards (Box 4–5): $masteredVocab / ${flashcards.size}\n" +
            "• Current Level: Level ${userProfile.level} (${userProfile.proficiencyLevel})",
          color = LingoTokens.textSecondary,
          fontSize = 13.sp,
          lineHeight = 20.sp
        )
      }

      val thisWeekMins = weeklyReport?.thisWeekMinutes
        ?: (speakingSessions.size * 5 + userProfile.completedLessonIds.size * 5)
      val lastWeekMins = weeklyReport?.lastWeekMinutes ?: 0
      val wowDelta = weeklyReport?.weekOverWeekDeltaPercent ?: 0

      LingoCleanCard(
        borderColor = LingoTokens.accentPrimary.copy(alpha = 0.35f),
        contentPadding = 16.dp
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Weekly Learning Report", color = LingoTokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
              Text(
                text = "Last week: ${lastWeekMins}m • ${if (wowDelta >= 0) "+$wowDelta%" else "$wowDelta%"} vs prior week",
                color = LingoTokens.accentPrimary,
                fontSize = 12.sp
              )
            }
            Text("$thisWeekMins mins", color = LingoTokens.accentEmerald, fontWeight = FontWeight.Bold, fontSize = 19.sp)
          }

          val dailyBars = weeklyReport?.dailyMinutesLast7Days
          if (!dailyBars.isNullOrEmpty()) {
            val maxDayMins = (dailyBars.maxOfOrNull { it.second } ?: 15).coerceAtLeast(15)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.Bottom
            ) {
              dailyBars.forEach { (dayLabel, mins) ->
                val barRatio = (mins.toFloat() / maxDayMins).coerceIn(0.08f, 1f)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(text = "${mins}m", color = LingoTokens.textSecondary, fontSize = 10.sp)
                  Spacer(modifier = Modifier.height(4.dp))
                  Box(
                    modifier = Modifier
                      .width(20.dp)
                      .height((48 * barRatio).dp)
                      .background(
                        color = if (mins > 0) LingoTokens.accentPrimary else LingoTokens.bgSubtle,
                        shape = RoundedCornerShape(6.dp)
                      )
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(text = dayLabel, color = LingoTokens.textPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }

          HorizontalDivider(color = LingoTokens.borderSubtle)

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Skill Progress & Accuracy Trends", color = LingoTokens.accentPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Speaking & Pronunciation Precision", color = LingoTokens.textPrimary, fontSize = 12.sp)
              val pronAcc = weeklyReport?.averagePronunciationAccuracy?.takeIf { it > 0 } ?: avgFluency
              val pronDelta = weeklyReport?.pronunciationImprovementDelta ?: 0
              val deltaSuffix = if (pronDelta != 0) " (${if (pronDelta > 0) "+$pronDelta%" else "$pronDelta%"})" else ""
              Text("${pronAcc}%$deltaSuffix", color = LingoTokens.accentEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Vocabulary Mastered / Due Now", color = LingoTokens.textPrimary, fontSize = 12.sp)
              val dueNow = weeklyReport?.vocabularyDueCount ?: 0
              Text("$masteredVocab mastered • $dueNow due", color = LingoTokens.accentAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            if (!weeklyReport?.difficultSounds.isNullOrEmpty()) {
              Text(
                text = "Target Phonemes: ${weeklyReport!!.difficultSounds.joinToString(", ")}",
                color = LingoTokens.accentPrimary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          if (!weeklyReport?.topGrammarMistakes.isNullOrEmpty()) {
            HorizontalDivider(color = LingoTokens.borderSubtle)
            Text("Recurring Grammar Patterns:", color = LingoTokens.accentAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            weeklyReport!!.topGrammarMistakes.take(3).forEach { m ->
              Text(
                text = "• [${m.category}] \"${m.originalText}\" → \"${m.correctedText}\"",
                color = LingoTokens.textSecondary,
                fontSize = 11.5.sp
              )
            }
          }

          if (!weeklyReport?.personalizedRecommendations.isNullOrEmpty()) {
            HorizontalDivider(color = LingoTokens.borderSubtle)
            Text("Recommended Next Steps:", color = LingoTokens.accentEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            weeklyReport!!.personalizedRecommendations.forEach { rec ->
              Text(text = "• $rec", color = LingoTokens.textSecondary, fontSize = 11.5.sp, lineHeight = 16.sp)
            }
          }

          if (weeklyReport != null && (weeklyReport.downloadedOfflineLessonsCount > 0 || weeklyReport.pendingOfflineSyncCount > 0)) {
            HorizontalDivider(color = LingoTokens.borderSubtle)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Offline: ${weeklyReport.downloadedOfflineLessonsCount} lessons • ${weeklyReport.pendingOfflineSyncCount} pending sync",
                color = LingoTokens.textSecondary,
                fontSize = 11.5.sp,
                modifier = Modifier.weight(1f)
              )
              if (weeklyReport.pendingOfflineSyncCount > 0) {
                OutlinedButton(
                  onClick = onSyncOfflineNow,
                  border = BorderStroke(1.dp, LingoTokens.borderFocus)
                ) {
                  Text("Sync Now", color = LingoTokens.accentPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
private fun AnalyticsMetricCard(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier
) {
  LingoCleanCard(
    borderColor = accent.copy(alpha = 0.35f),
    contentPadding = 14.dp,
    modifier = modifier
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(value, color = accent, fontWeight = FontWeight.Bold, fontSize = 20.sp)
      Spacer(modifier = Modifier.height(4.dp))
      Text(label, color = LingoTokens.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  }
}

// ============================================================================
// 5. NOTIFICATIONS SCREEN
// ============================================================================

@Composable
fun NotificationsScreen(
  notifications: List<UserNotification>,
  onMarkRead: (String) -> Unit,
  onBack: () -> Unit,
  accountSettings: com.example.data.model.UserAccountSettings = com.example.data.model.UserAccountSettings(),
  onUpdateAccountSettings: (com.example.data.model.UserAccountSettings) -> Unit = {},
  onOpenNotificationDeepLink: (com.example.notifications.SmartNotificationDeepLink) -> Unit = {}
) {
  BackHandler { onBack() }
  val context = LocalContext.current
  val featuredWord = remember { com.example.notifications.SmartEnglishContentCatalog.WORDS_OF_THE_DAY.first() }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(LingoTokens.bgCanvas)
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
  ) {
    LingoTopBar(
      title = "Notifications",
      subtitle = "Daily reminders, partner invites & call alerts",
      onBack = onBack
    )

    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier
        .fillMaxSize()
        .testTag("notifications_list")
    ) {
      item {
        com.example.ui.components.DailyStreakReminderCard(
          accountSettings = accountSettings,
          onUpdateAccountSettings = onUpdateAccountSettings
        )
      }

      item {
        LingoCleanCard(
          onClick = {
            onOpenNotificationDeepLink(
              com.example.notifications.SmartNotificationDeepLink(
                route = "word_of_the_day",
                word = featuredWord.word,
                meaning = featuredWord.meaning,
                pronunciation = featuredWord.pronunciation,
                exampleSentence = featuredWord.exampleSentence,
                lessonId = featuredWord.lessonId
              )
            )
          },
          borderColor = LingoTokens.accentAmber.copy(alpha = 0.45f),
          contentPadding = 16.dp,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("wod_notification_preview_card")
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "${featuredWord.word} — Word of the Day",
                color = LingoTokens.accentAmber,
                fontWeight = FontWeight.Bold,
                fontSize = 15.5.sp
              )
              Text(
                text = featuredWord.pronunciation,
                color = LingoTokens.accentPrimary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
            Text(text = "Meaning: ${featuredWord.meaning}", color = LingoTokens.textPrimary, fontSize = 13.5.sp)
            Text(text = "Example: \"${featuredWord.exampleSentence}\"", color = LingoTokens.textSecondary, fontSize = 13.sp)
            Text(text = "Tap to learn and practice pronunciation.", color = LingoTokens.accentEmerald, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
              Button(
                onClick = {
                  onOpenNotificationDeepLink(
                    com.example.notifications.SmartNotificationDeepLink(
                      route = "word_of_the_day",
                      word = featuredWord.word,
                      meaning = featuredWord.meaning,
                      pronunciation = featuredWord.pronunciation,
                      exampleSentence = featuredWord.exampleSentence,
                      lessonId = featuredWord.lessonId
                    )
                  )
                },
                colors = ButtonDefaults.buttonColors(containerColor = LingoTokens.accentBlue, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("practice_wod_from_notifications_btn")
              ) {
                Text("Practice Pronunciation", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
              }
              OutlinedButton(
                onClick = {
                  val delivered = com.example.notifications.SmartNotificationManager.showWordOfTheDayNotification(
                    context = context,
                    entry = featuredWord,
                    uniqueKey = "manual_wod_${System.currentTimeMillis() / 15_000L}",
                    bypassCooldown = true
                  )
                  Toast.makeText(
                    context,
                    if (delivered) "Sent Word of the Day push notification!" else "Enable notification permission or Word of the Day category first.",
                    Toast.LENGTH_SHORT
                  ).show()
                },
                border = BorderStroke(1.dp, LingoTokens.borderFocus),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("send_wod_push_btn")
              ) {
                Text("Send Push", color = LingoTokens.accentPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      item {
        LingoCleanCard(
          contentPadding = 16.dp,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_notification_categories_card")
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "Notification Categories",
              color = LingoTokens.textPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 15.5.sp
            )
            Text(
              text = "Choose which learning and partner alerts you want to receive.",
              color = LingoTokens.textSecondary,
              fontSize = 12.sp
            )

            SmartCategoryToggleRow(
              label = "Daily Word of the Day",
              subtitle = "Word, meaning, pronunciation & example sentence",
              checked = accountSettings.wordOfTheDayNotificationsEnabled,
              onCheckedChange = {
                onUpdateAccountSettings(accountSettings.copy(wordOfTheDayNotificationsEnabled = it))
              }
            )
            SmartCategoryToggleRow(
              label = "Daily Vocabulary Challenges",
              subtitle = "60-second vocabulary context quizzes",
              checked = accountSettings.dailyVocabChallengeNotificationsEnabled,
              onCheckedChange = {
                onUpdateAccountSettings(accountSettings.copy(dailyVocabChallengeNotificationsEnabled = it))
              }
            )
            SmartCategoryToggleRow(
              label = "Grammar Tips",
              subtitle = "Daily grammar rules & real-world examples",
              checked = accountSettings.grammarTipNotificationsEnabled,
              onCheckedChange = {
                onUpdateAccountSettings(accountSettings.copy(grammarTipNotificationsEnabled = it))
              }
            )
            SmartCategoryToggleRow(
              label = "Speaking Practice Reminders",
              subtitle = "Daily fluency & pronunciation warmup reminders",
              checked = accountSettings.speakingReminderNotificationsEnabled,
              onCheckedChange = {
                onUpdateAccountSettings(accountSettings.copy(speakingReminderNotificationsEnabled = it))
              }
            )
            SmartCategoryToggleRow(
              label = "Speaking Partner Invitations",
              subtitle = "Alerts when someone sends a speaking invitation",
              checked = accountSettings.speakingInvitationNotificationsEnabled,
              onCheckedChange = {
                onUpdateAccountSettings(accountSettings.copy(speakingInvitationNotificationsEnabled = it))
              }
            )
            SmartCategoryToggleRow(
              label = "Incoming & Missed Call Alerts",
              subtitle = "Real-time partner call & missed call notifications",
              checked = accountSettings.callNotificationsEnabled,
              onCheckedChange = {
                onUpdateAccountSettings(accountSettings.copy(callNotificationsEnabled = it))
              }
            )
          }
        }
      }

      if (notifications.isEmpty()) {
        item {
          LingoEmptyOrErrorCard(
            title = "All Caught Up",
            message = "Friend requests, speaking invitations, missed call alerts, and unlocked milestones appear here."
          )
        }
      } else {
        items(notifications, key = { it.id }) { notif ->
          LingoCleanCard(
            onClick = {
              onMarkRead(notif.id)
              val resolvedRoute = notif.targetRoute.ifBlank {
                when (notif.type.lowercase()) {
                  "word_of_the_day" -> "word_of_the_day"
                  "vocab_challenge" -> "vocab_challenge"
                  "grammar_tip" -> "grammar_tip"
                  "speaking_reminder" -> "speaking_reminder"
                  "speaking_invitation", "friend_request", "call_invite" -> "speaking_invitation"
                  "missed_call" -> "missed_call"
                  else -> ""
                }
              }
              if (resolvedRoute.isNotBlank()) {
                onOpenNotificationDeepLink(
                  com.example.notifications.SmartNotificationDeepLink(
                    route = resolvedRoute,
                    word = notif.word,
                    meaning = notif.meaning,
                    pronunciation = notif.pronunciation,
                    exampleSentence = notif.exampleSentence,
                    lessonId = notif.targetId,
                    partnerId = notif.senderId.ifBlank { notif.targetId },
                    partnerName = notif.senderName,
                    roomId = notif.targetId
                  )
                )
              }
            },
            containerColor = if (notif.isRead) LingoTokens.bgElevated else LingoTokens.accentPrimary.copy(alpha = 0.08f),
            borderColor = if (notif.isRead) LingoTokens.borderSubtle else LingoTokens.accentPrimary.copy(alpha = 0.5f),
            contentPadding = 16.dp
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = if (notif.isRead) LingoTokens.textSecondary else LingoTokens.accentPrimary,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(notif.title, color = LingoTokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(notif.message, color = LingoTokens.textSecondary, fontSize = 12.5.sp)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SmartCategoryToggleRow(
  label: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(label, color = LingoTokens.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
      Text(subtitle, color = LingoTokens.textSecondary, fontSize = 11.5.sp)
    }
    Spacer(modifier = Modifier.width(8.dp))
    androidx.compose.material3.Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
fun HelpSupportScreen(
  onBack: () -> Unit
) {
  BackHandler { onBack() }
  val context = LocalContext.current

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(LingoTokens.bgCanvas)
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
  ) {
    LingoTopBar(
      title = "Help & Support",
      subtitle = "FAQs, voice/video diagnostics & contact",
      onBack = onBack
    )

    Column(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      FaqCard(
        question = "How does Real-Time AI Voice Calling work?",
        answer = "LingoSphere uses native Speech Recognition, Text-to-Speech, and AI coaching to deliver natural, hands-free two-way conversations. Grant Microphone permission when prompted and speak naturally."
      )
      FaqCard(
        question = "How do Partner Voice & Video Calls work?",
        answer = "When you tap Random Match, Country Match, Interest Match, or call a partner directly, LingoSphere creates a live signaling room and activates your microphone and camera preview."
      )
      FaqCard(
        question = "How do I manage or cancel my Premium Subscription?",
        answer = "You can manage or cancel your subscription anytime from Google Play's Payments & Subscriptions settings."
      )
      FaqCard(
        question = "How do I delete my account and data?",
        answer = "Go to Profile > Account > Delete Account & All Data. Confirming deletion permanently removes your profile, lessons, flashcards, speaking sessions, and community records."
      )

      LingoCleanCard(borderColor = LingoTokens.accentPrimary.copy(alpha = 0.45f), contentPadding = 16.dp) {
        Text("Contact Support", color = LingoTokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          "Need help with your account or language learning tools? Tap below to copy our support email:",
          color = LingoTokens.textSecondary,
          fontSize = 12.5.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
          onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Support Email", "support@lingosphere.app"))
            Toast.makeText(context, "Copied: support@lingosphere.app", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = LingoTokens.accentBlue, contentColor = Color.White),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("support@lingosphere.app")
        }
      }
    }
  }
}

@Composable
private fun FaqCard(question: String, answer: String) {
  LingoCleanCard(contentPadding = 16.dp) {
    Text(question, color = LingoTokens.accentPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
    Spacer(modifier = Modifier.height(6.dp))
    Text(answer, color = LingoTokens.textSecondary, fontSize = 12.5.sp, lineHeight = 18.sp)
  }
}
