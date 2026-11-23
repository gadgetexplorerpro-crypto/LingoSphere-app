package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdaptiveRecommendationPlan
import com.example.data.model.CurriculumLesson
import com.example.data.model.Language
import com.example.data.model.PlacementTestEvaluation
import com.example.data.model.UserProfile
import com.example.data.repository.InProgressLessonState
import com.example.speech.SpeechManager
import com.example.ui.components.LingoCleanCard
import com.example.ui.components.LingoEmptyOrErrorCard
import com.example.ui.components.LingoGradientProgressBar
import com.example.ui.components.LingoPrimaryGradientButton
import com.example.ui.components.LingoSecondaryChip
import com.example.ui.components.LingoTokens
import com.example.ui.components.extractCefrCodeOnly
import com.example.ui.components.lingoAmbientBackdrop
import com.example.ui.components.springTap
import kotlinx.coroutines.launch

@Composable
fun LearnScreen(
  userProfile: UserProfile,
  lessons: List<CurriculumLesson>,
  onStartLesson: (CurriculumLesson) -> Unit,
  onOpenCulturePassport: (initialTab: Int) -> Unit = {},
  speechManager: SpeechManager? = null,
  bookmarkedReadingIds: Set<String> = emptySet(),
  completedAcademyIds: Set<String> = emptySet(),
  placementEvaluation: PlacementTestEvaluation? = null,
  onToggleReadingBookmark: (String) -> Unit = {},
  onCompleteAcademyItem: (id: String, title: String, scorePercent: Int, xpBonus: Int) -> Unit = { _, _, _, _ -> },
  onSaveVocabToFlashcard: (front: String, back: String, phonetic: String) -> Unit = { _, _, _ -> },
  onApplyPlacementEvaluation: (PlacementTestEvaluation) -> Unit = {},
  inProgressLessonsMap: Map<String, InProgressLessonState> = emptyMap(),
  downloadedLessonIds: Set<String> = emptySet(),
  pendingOfflineSyncCount: Int = 0,
  onToggleLessonOfflineDownload: (CurriculumLesson) -> Unit = {},
  onDownloadAllLessons: (List<CurriculumLesson>) -> Unit = {},
  onSyncOfflineNow: () -> Unit = {},
  adaptivePlan: AdaptiveRecommendationPlan? = null,
  onOpenAccentStudio: () -> Unit = {},
  onOpenMistakeRecovery: () -> Unit = {},
  onOpenImmersionMode: () -> Unit = {},
  onOpenConversationRepairLab: () -> Unit = {},
  onOpenConversationDetective: () -> Unit = {},
  onOpenTeachTheAi: () -> Unit = {},
  todayStudyMinutes: Int = 0,
  onChangeTargetLanguage: (String) -> Unit = {},
  onChangeNativeLanguage: (String) -> Unit = {},
  onUpdateLearningSetup: (level: String, goal: String, dailyMinutes: Int, confidence: String) -> Unit = { _, _, _, _ -> },
  currentLearningGoal: String = "Everyday Communication",
  currentConfidence: String = "Medium",
  hasCompletedLearningSetup: Boolean = false,
  onOpenListeningPractice: () -> Unit = {},
  onOpenWritingStudio: () -> Unit = {},
  onOpenRoleplayStudio: () -> Unit = {},
  onOpenReadingStudio: () -> Unit = {},
  onOpenVocabularyStudio: () -> Unit = {},
  onOpenSpeakingCoach: () -> Unit = {},
  showInlineContinueFab: Boolean = true
) {
  val targetLang = Language.findByCode(userProfile.targetLanguageCode)
  val nativeLang = Language.findByCode(userProfile.nativeLanguageCode.ifBlank { "en" })
  val isVerifiedPro = userProfile.isEntitlementVerified

  val curriculumListState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()

  var inspectedLessonAndState by remember { mutableStateOf<Pair<CurriculumLesson, JourneyNodeState>?>(null) }
  var showLanguageSwitchDialog by remember { mutableStateOf(false) }
  var showLearningSetupModal by remember { mutableStateOf(false) }
  var showLiveAiTutorModal by remember { mutableStateOf(false) }
  var showRoleplayStudioModal by remember { mutableStateOf(false) }
  var showVocabularyStudioModal by remember { mutableStateOf(false) }
  var showWritingStudioModal by remember { mutableStateOf(false) }

  var selectedCefrFilter by rememberSaveable { mutableStateOf("All") }
  var selectedSkillFilter by rememberSaveable { mutableStateOf("All") }

  val cefrTabs = remember { listOf("All", "A1", "A2", "B1", "B2", "C1", "C2") }
  val skillFilters = remember { listOf("All", "Speaking", "Listening", "Vocabulary", "Grammar") }

  val activeCefrCode = extractCefrCodeOnly(
    placementEvaluation?.recommendedCefrBand ?: adaptivePlan?.recommendedCefrDifficulty,
    userProfile.proficiencyLevel
  )

  val allAvailableLessons = remember(lessons) { lessons }
  val filteredLessons = remember(allAvailableLessons, selectedCefrFilter, selectedSkillFilter) {
    allAvailableLessons.filter { lesson ->
      val matchesCefr = selectedCefrFilter == "All" || lesson.cefrLevel.equals(selectedCefrFilter, ignoreCase = true)
      val matchesSkill = when (selectedSkillFilter) {
        "All" -> true
        "Speaking" -> lesson.category.contains("speak", true) || lesson.category.contains("convers", true) || lesson.category.contains("greet", true) || lesson.category.contains("pronun", true) || lesson.title.contains("speak", true)
        "Listening" -> lesson.category.contains("listen", true) || lesson.category.contains("travel", true) || lesson.category.contains("daily", true) || lesson.category.contains("social", true)
        "Vocabulary" -> lesson.category.contains("vocab", true) || lesson.category.contains("food", true) || lesson.category.contains("word", true) || lesson.category.contains("essential", true) || lesson.category.contains("work", true)
        "Grammar" -> lesson.category.contains("grammar", true) || lesson.category.contains("tense", true) || lesson.category.contains("structure", true) || lesson.category.contains("verb", true) || lesson.category.contains("write", true)
        else -> true
      }
      matchesCefr && matchesSkill
    }
  }

  val recommendedLesson = remember(allAvailableLessons, userProfile.completedLessonIds, inProgressLessonsMap) {
    val inProgressFirst = allAvailableLessons.firstOrNull { inProgressLessonsMap.containsKey(it.id) }
    inProgressFirst
      ?: allAvailableLessons.firstOrNull { !userProfile.completedLessonIds.contains(it.id) }
      ?: allAvailableLessons.firstOrNull()
  }

  val cefrSections = remember(filteredLessons) {
    listOf(
      Triple("A1", "Beginner", filteredLessons.filter { it.cefrLevel.equals("A1", ignoreCase = true) }),
      Triple("A2", "Elementary", filteredLessons.filter { it.cefrLevel.equals("A2", ignoreCase = true) }),
      Triple("B1", "Intermediate", filteredLessons.filter { it.cefrLevel.equals("B1", ignoreCase = true) }),
      Triple("B2", "Upper-Intermediate", filteredLessons.filter { it.cefrLevel.equals("B2", ignoreCase = true) }),
      Triple("C1", "Advanced", filteredLessons.filter { it.cefrLevel.equals("C1", ignoreCase = true) }),
      Triple("C2", "Proficient", filteredLessons.filter { it.cefrLevel.equals("C2", ignoreCase = true) })
    ).filter { it.third.isNotEmpty() }
  }

  // Compute exact LazyColumn index of the current lesson so sticky "Continue" button appears only when off-screen
  val currentLessonListIndex = remember(cefrSections, recommendedLesson?.id) {
    val targetId = recommendedLesson?.id ?: return@remember 0
    var idx = 2 // 0 = studios, 1 = daily challenge
    for ((_, _, sectionLessons) in cefrSections) {
      idx += 1 // section header
      val localIdx = sectionLessons.indexOfFirst { it.id == targetId }
      if (localIdx >= 0) {
        return@remember idx + localIdx
      }
      idx += sectionLessons.size
    }
    0
  }

  val isCurrentLessonOffScreen by remember(currentLessonListIndex) {
    derivedStateOf {
      val visible = curriculumListState.layoutInfo.visibleItemsInfo
      if (visible.isEmpty()) false
      else visible.none { it.index == currentLessonListIndex }
    }
  }

  val dailyPhrase = remember(recommendedLesson) {
    recommendedLesson?.exercises?.firstOrNull { it.targetAudioText.isNotBlank() }?.targetAudioText
      ?: "Hello, how are you doing today?"
  }
  val dailyTranslation = remember(recommendedLesson) {
    recommendedLesson?.exercises?.firstOrNull()?.options?.firstOrNull()
      ?: "Hello! How are you doing today?"
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .lingoAmbientBackdrop()
      .testTag("learn_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = LingoTokens.screenMargin)
    ) {
      // =====================================================================
      // 1. HEADER: "Learn" title + language pill ("English · A2") + Plan action
      // =====================================================================
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Learn",
          style = MaterialTheme.typography.headlineLarge,
          fontSize = 26.sp,
          fontWeight = FontWeight.ExtraBold,
          color = LingoTokens.textPrimary
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            color = LingoTokens.bgElevated,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, LingoTokens.borderSubtle),
            modifier = Modifier
              .heightIn(min = 44.dp)
              .springTap { showLanguageSwitchDialog = true }
              .testTag("open_language_switcher_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = "Switch language",
                tint = LingoTokens.brandCyan,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "${targetLang.name} · $activeCefrCode",
                style = MaterialTheme.typography.labelLarge,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = LingoTokens.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("learn_header_title")
              )
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = LingoTokens.textSecondary,
                modifier = Modifier.size(18.dp)
              )
            }
          }

          IconButton(
            onClick = { showLearningSetupModal = true },
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(LingoTokens.bgElevated)
              .border(1.dp, LingoTokens.borderSubtle, CircleShape)
              .testTag("open_learning_setup_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.Tune,
              contentDescription = "Learning plan",
              tint = LingoTokens.textPrimary,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      // =====================================================================
      // 2. CEFR LEVEL TABS (A1 to C2)
      // =====================================================================
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        cefrTabs.forEach { band ->
          val selected = selectedCefrFilter == band
          LingoSecondaryChip(
            text = band,
            selected = selected,
            onClick = { selectedCefrFilter = band },
            testTag = "learn_cefr_tab_${band.lowercase()}"
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // =====================================================================
      // 3. SKILL FILTER CHIPS (Speaking, Listening, Vocabulary, Grammar)
      // =====================================================================
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        skillFilters.forEach { skill ->
          val selected = selectedSkillFilter == skill
          Surface(
            color = if (selected) LingoTokens.brandIndigo.copy(alpha = 0.20f) else LingoTokens.bgElevated,
            shape = RoundedCornerShape(50),
            border = BorderStroke(
              1.dp,
              if (selected) LingoTokens.brandCyan.copy(alpha = 0.5f) else LingoTokens.borderSubtle
            ),
            modifier = Modifier
              .heightIn(min = 38.dp)
              .springTap { selectedSkillFilter = skill }
              .testTag("learn_skill_chip_${skill.lowercase()}")
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
              Text(
                text = skill,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) LingoTokens.brandCyan else LingoTokens.textSecondary,
                maxLines = 1
              )
            }
          }
        }
      }

      // Compact "Edit Learning Plan" bar so the current lesson remains immediately visible
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = LingoTokens.bgElevated,
          shape = RoundedCornerShape(50),
          border = BorderStroke(1.dp, LingoTokens.borderSubtle),
          modifier = Modifier
            .heightIn(min = 40.dp)
            .springTap { showLearningSetupModal = true }
            .testTag("edit_learning_plan_chip")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.Tune,
              contentDescription = "Edit Learning Plan",
              tint = LingoTokens.brandCyan,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = if (hasCompletedLearningSetup) {
                "Edit Learning Plan · ${userProfile.dailyGoalMinutes} min/day"
              } else {
                "Edit Learning Plan"
              },
              style = MaterialTheme.typography.labelMedium,
              fontSize = 12.5.sp,
              fontWeight = FontWeight.SemiBold,
              color = LingoTokens.textPrimary,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      // Modals & Inspector
      if (showLanguageSwitchDialog) {
        SearchableLanguageAndExplanationModal(
          currentTargetCode = userProfile.targetLanguageCode,
          currentNativeCode = nativeLang.code,
          onSelectTargetLanguage = { code -> onChangeTargetLanguage(code) },
          onSelectNativeLanguage = { code -> onChangeNativeLanguage(code) },
          onDismiss = { showLanguageSwitchDialog = false }
        )
      }

      if (showLearningSetupModal) {
        Dialog(
          onDismissRequest = { showLearningSetupModal = false },
          properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 20.dp)
              .verticalScroll(rememberScrollState())
          ) {
            PersonalizedLearningSetupModal(
              currentLevel = userProfile.proficiencyLevel,
              currentGoal = currentLearningGoal,
              currentDailyMinutes = userProfile.dailyGoalMinutes.toInt(),
              currentConfidence = currentConfidence,
              onSaveSetup = { level, goal, mins, conf ->
                onUpdateLearningSetup(level, goal, mins, conf)
              },
              onLaunchPlacementTest = { showLearningSetupModal = false },
              onDismiss = { showLearningSetupModal = false }
            )
          }
        }
      }

      if (showLiveAiTutorModal && recommendedLesson != null && isVerifiedPro) {
        LiveAiLessonTutorModal(
          lesson = recommendedLesson,
          targetLanguageCode = userProfile.targetLanguageCode,
          nativeLanguageCode = nativeLang.code,
          speechManager = speechManager,
          onSaveWordToFlashcards = { front, back, note ->
            onSaveVocabToFlashcard(front, back, note)
          },
          onCompleteLesson = { completed ->
            onCompleteAcademyItem(completed.id, completed.title, 100, completed.xpReward)
            showLiveAiTutorModal = false
          },
          onDismiss = { showLiveAiTutorModal = false }
        )
      }

      if (showRoleplayStudioModal && isVerifiedPro) {
        AiRoleplayStudioDialog(
          targetLang = targetLang,
          nativeLang = nativeLang,
          initialCefr = activeCefrCode,
          speechManager = speechManager,
          onDismiss = { showRoleplayStudioModal = false },
          onCompleteRoleplayReward = { title, score ->
            onCompleteAcademyItem("roleplay_${title.lowercase()}", title, score, 30)
          }
        )
      }

      if (showVocabularyStudioModal) {
        AiVocabularyStudioModal(
          targetLanguageCode = userProfile.targetLanguageCode,
          cefrLevel = activeCefrCode,
          speechManager = speechManager,
          onSaveWord = { front, back, note ->
            onSaveVocabToFlashcard(front, back, note)
          },
          onAwardXp = { xp ->
            onCompleteAcademyItem("vocab_studio_${userProfile.targetLanguageCode}", "Vocabulary Studio", 100, xp)
          },
          onDismiss = { showVocabularyStudioModal = false }
        )
      }

      if (showWritingStudioModal) {
        AiWritingStudioModal(
          targetLanguageCode = userProfile.targetLanguageCode,
          nativeLanguageCode = nativeLang.code,
          cefrLevel = activeCefrCode,
          onAwardXp = { xp ->
            onCompleteAcademyItem("writing_studio_${userProfile.targetLanguageCode}", "Writing Studio", 100, xp)
          },
          onDismiss = { showWritingStudioModal = false }
        )
      }

      inspectedLessonAndState?.let { (lesson, state) ->
        LessonInspectDialog(
          lesson = lesson,
          nodeState = state,
          isCompleted = userProfile.completedLessonIds.contains(lesson.id),
          onDismiss = { inspectedLessonAndState = null },
          onStartLesson = { onStartLesson(lesson) },
          onOpenLiveTutor = {
            if (isVerifiedPro) {
              showLiveAiTutorModal = true
            } else {
              com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.AI_UPGRADE)
            }
          }
        )
      }

      // =====================================================================
      // 4. CEFR ROADMAP LIST
      // =====================================================================
      LazyColumn(
        state = curriculumListState,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .testTag("learn_curriculum_list"),
        contentPadding = PaddingValues(top = 4.dp, bottom = LingoTokens.bottomBarClearance),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item(key = "ai_learning_studio_section_item") {
          AiLearningStudioSection(
            targetLanguageName = targetLang.name,
            isVerifiedPro = isVerifiedPro,
            onOpenSpeakingTutor = onOpenSpeakingCoach,
            onOpenRoleplayStudio = {
              if (isVerifiedPro) {
                onOpenRoleplayStudio()
                showRoleplayStudioModal = true
              } else {
                com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.ROLEPLAY)
              }
            },
            onOpenReadingStories = onOpenReadingStudio,
            onOpenListeningStudio = onOpenListeningPractice,
            onOpenVocabularyStudio = onOpenVocabularyStudio,
            onOpenWritingStudio = onOpenWritingStudio
          )
        }

        item(key = "daily_challenge_card_item") {
          LearnDailyChallengeCard(
            targetLang = targetLang,
            cefrLabel = activeCefrCode,
            dailyPhrase = dailyPhrase,
            dailyTranslation = dailyTranslation,
            isCompletedToday = completedAcademyIds.any { it.startsWith("daily_challenge_") },
            onCompleteChallenge = {
              onCompleteAcademyItem(
                "daily_challenge_${userProfile.targetLanguageCode}",
                "Daily ${targetLang.name} Challenge",
                100,
                25
              )
            },
            speechManager = speechManager
          )
        }

        if (cefrSections.isEmpty()) {
          item(key = "empty_filtered_lessons") {
            LingoEmptyOrErrorCard(
              title = "No matching lessons",
              message = "Try another skill filter or CEFR level to explore all ${targetLang.name} lessons.",
              actionLabel = "Show all lessons",
              onAction = {
                selectedCefrFilter = "All"
                selectedSkillFilter = "All"
              }
            )
          }
        } else {
          cefrSections.forEach { (band, title, sectionLessons) ->
            item(key = "cefr_section_$band") {
              val doneCount = sectionLessons.count { userProfile.completedLessonIds.contains(it.id) }
              val ratio = (doneCount.toFloat() / sectionLessons.size.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

              LingoCleanCard(
                contentPadding = 18.dp,
                modifier = Modifier.padding(top = 6.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    Box(
                      contentAlignment = Alignment.Center,
                      modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(LingoTokens.brandCyan.copy(alpha = 0.16f))
                        .border(1.dp, LingoTokens.brandCyan.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                      Text(
                        text = band,
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = LingoTokens.brandCyan
                      )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = LingoTokens.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      Text(
                        text = "$doneCount of ${sectionLessons.size} complete",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        color = LingoTokens.textSecondary,
                        maxLines = 1
                      )
                    }
                  }

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    IconButton(
                      onClick = { onDownloadAllLessons(sectionLessons) },
                      modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(LingoTokens.bgSubtle)
                        .border(1.dp, LingoTokens.borderSubtle, CircleShape)
                        .testTag("download_all_level_${band.lowercase()}")
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.CloudDownload,
                        contentDescription = "Download all $band lessons offline",
                        tint = LingoTokens.brandCyan,
                        modifier = Modifier.size(20.dp)
                      )
                    }

                    LingoSecondaryChip(
                      text = "Free talk",
                      icon = Icons.Outlined.Mic,
                      onClick = onOpenImmersionMode,
                      testTag = if (band == cefrSections.first().first) "free_talk_teacher_button" else "free_talk_$band"
                    )
                  }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LingoGradientProgressBar(
                  progress = ratio,
                  height = 6.dp
                )
              }
            }

            itemsIndexed(sectionLessons, key = { _, lesson -> lesson.id }) { indexInUnit, lesson ->
              val isCompleted = userProfile.completedLessonIds.contains(lesson.id)
              val isInProgress = inProgressLessonsMap.containsKey(lesson.id) && !isCompleted
              val isCurrent = lesson.id == recommendedLesson?.id

              val nodeState = when {
                isCompleted -> JourneyNodeState.COMPLETED
                isInProgress || isCurrent -> JourneyNodeState.CURRENT
                indexInUnit == sectionLessons.size - 1 && !isCompleted -> JourneyNodeState.ASSESSMENT
                else -> JourneyNodeState.LOCKED
              }

              WindingJourneyLessonRow(
                lesson = lesson,
                indexInUnit = indexInUnit,
                totalInUnit = sectionLessons.size,
                nodeState = nodeState,
                isCompleted = isCompleted,
                inProgressState = inProgressLessonsMap[lesson.id],
                isDownloaded = downloadedLessonIds.contains(lesson.id),
                isVerifiedPro = isVerifiedPro,
                onStart = { onStartLesson(lesson) },
                onToggleOffline = { onToggleLessonOfflineDownload(lesson) },
                onInspectLesson = { inspectedLesson, state ->
                  inspectedLessonAndState = Pair(inspectedLesson, state)
                }
              )
            }
          }
        }
      }
    }

    // Sticky "Continue" button when the current lesson scrolls off screen (hidden if global floating Continue button is enabled)
    AnimatedVisibility(
      visible = showInlineContinueFab && isCurrentLessonOffScreen,
      enter = fadeIn() + slideInVertically { it / 2 },
      exit = fadeOut() + slideOutVertically { it / 2 },
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 20.dp, bottom = 104.dp)
    ) {
      LingoPrimaryGradientButton(
        text = "Continue",
        icon = Icons.Outlined.PlayArrow,
        onClick = {
          coroutineScope.launch {
            curriculumListState.animateScrollToItem(currentLessonListIndex.coerceAtLeast(0))
          }
        },
        testTag = "floating_current_lesson_fab"
      )
    }
  }
}
