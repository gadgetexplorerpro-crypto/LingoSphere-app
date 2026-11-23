package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ai.AiErrorHandler
import com.example.ai.GeminiTutorService
import com.example.ai.UpgradeSheetMode
import com.example.data.model.Language
import com.example.data.model.PronunciationDiagnosticResult
import com.example.data.model.SpeakingSession
import com.example.data.model.TutorVoice
import com.example.data.model.UserProfile
import com.example.speech.SpeechManager
import com.example.ui.components.EmojiVoice
import com.example.ui.components.EmojiVoiceState
import com.example.ui.components.LingoCalmAiErrorBanner
import com.example.ui.components.LingoGoldLockIcon
import com.example.ui.components.rememberLiveEmojiVoiceState
import com.example.ui.theme.LingoThemeController
import kotlinx.coroutines.launch

private val CoachNavyTop = Color(0xFF060D1D)
private val CoachNavyBottom = Color(0xFF0D162B)
private val CoachElectricBlue = Color(0xFF3B82F6)
private val CoachCyan = Color(0xFF38BDF8)
private val CoachEmerald = Color(0xFF10B981)
private val CoachAmber = Color(0xFFF59E0B)
private val CoachGold = Color(0xFFFBBF24)

@Composable
fun PronunciationCoachScreen(
  userProfile: UserProfile,
  speechManager: SpeechManager,
  onSaveSession: (SpeakingSession) -> Unit,
  onBack: () -> Unit,
  pronunciationHistory: List<PronunciationDiagnosticResult> = emptyList(),
  onRecordDiagnostic: (PronunciationDiagnosticResult) -> Unit = {},
  initialWordOfTheDay: com.example.notifications.WordOfTheDayEntry? = null,
  onOpenSubscription: () -> Unit = {}
) {
  BackHandler { onBack() }
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val isDark = LingoThemeController.isDarkActive
  val targetLang = remember(userProfile.targetLanguageCode) { Language.findByCode(userProfile.targetLanguageCode) }
  val nativeLang = remember(userProfile.nativeLanguageCode) { Language.findByCode(userProfile.nativeLanguageCode) }

  // 0: Practice, 1: Sounds, 2: Feedback
  var activeTab by remember { mutableIntStateOf(0) }
  var showSettingsDialog by remember { mutableStateOf(false) }

  val defaultPhrases = remember(targetLang.code) {
    GeminiTutorService.getFallbackPronunciationPhrases(targetLang.code)
  }
  var practiceDrills by remember(targetLang.code) { mutableStateOf(defaultPhrases) }
  var drillIndex by remember(targetLang.code) { mutableIntStateOf(0) }
  var isGeneratingDrills by remember { mutableStateOf(false) }
  var drillGenerationError by remember { mutableStateOf<String?>(null) }

  val drillTopics = remember {
    listOf(
      "Daily Communication & Introductions",
      "Travel & Asking Directions",
      "Café & Restaurant Ordering",
      "Shopping & Local Markets",
      "Work, Business & Meetings",
      "Interviews & Professional Fluency",
      "Expressing Opinions & Feelings"
    )
  }
  var topicIndex by remember { mutableIntStateOf(0) }

  val pastWeakSounds = remember(pronunciationHistory) {
    pronunciationHistory.flatMap { it.problematicSounds }.filter { it.isNotBlank() }.distinct().take(4)
  }

  var customInputText by remember { mutableStateOf("") }
  var activeCustomPhrase by remember(initialWordOfTheDay) {
    mutableStateOf(initialWordOfTheDay?.exampleSentence ?: "")
  }
  var diagnosticResult by remember { mutableStateOf<PronunciationDiagnosticResult?>(null) }

  val isVerifiedPro = userProfile.isEntitlementVerified

  fun loadDynamicAiDrills(advanceTopic: Boolean = false, userInitiated: Boolean = false) {
    if (!isVerifiedPro) {
      if (practiceDrills.isEmpty()) {
        practiceDrills = GeminiTutorService.getFallbackPronunciationPhrases(targetLang.code)
      }
      if (userInitiated) {
        AiErrorHandler.requestUpgrade(
          mode = UpgradeSheetMode.AI_UPGRADE,
          featureKey = "pronunciation_coach",
          headline = "Unlock AI Pronunciation Coach",
          reason = "Upgrade to LingoSphere Pro for personalized acoustic feedback, syllable breakdown, and unlimited fresh AI drills."
        )
      }
      return
    }
    if (isGeneratingDrills) return
    isGeneratingDrills = true
    drillGenerationError = null
    if (advanceTopic) {
      topicIndex = (topicIndex + 1) % drillTopics.size
    }
    val currentTopic = drillTopics[topicIndex % drillTopics.size]
    scope.launch {
      try {
        val aiDrills = GeminiTutorService.generatePronunciationPracticeDrills(
          targetLanguage = targetLang,
          nativeLanguage = nativeLang,
          cefrLevel = userProfile.proficiencyLevel.ifBlank { "B1" },
          topic = currentTopic,
          weakSounds = pastWeakSounds
        )
        if (aiDrills.isNotEmpty()) {
          practiceDrills = aiDrills
          drillIndex = 0
          activeCustomPhrase = ""
          diagnosticResult = null
        }
      } catch (e: Exception) {
        drillGenerationError = AiErrorHandler.handle(e, contextTag = "PronunciationDrills").userMessage
        if (practiceDrills.isEmpty()) {
          practiceDrills = GeminiTutorService.getFallbackPronunciationPhrases(targetLang.code)
        }
      } finally {
        isGeneratingDrills = false
      }
    }
  }

  LaunchedEffect(targetLang.code, nativeLang.code, userProfile.proficiencyLevel, isVerifiedPro) {
    if (isVerifiedPro) {
      loadDynamicAiDrills(advanceTopic = false, userInitiated = false)
    } else if (practiceDrills.isEmpty()) {
      practiceDrills = GeminiTutorService.getFallbackPronunciationPhrases(targetLang.code)
    }
  }

  val regionalAccents = remember(targetLang.code) {
    when (targetLang.code) {
      "es" -> listOf("Standard (Castilian)", "Latin American (Mexico)", "Rioplatense (Argentina)")
      "fr" -> listOf("Standard (Paris)", "Canadian (Québec)", "Belgian / Swiss")
      "en" -> listOf("General American", "British RP", "Australian")
      "de" -> listOf("Standard High German", "Austrian", "Swiss German")
      "pt" -> listOf("Brazilian (São Paulo)", "European (Lisbon)")
      "ar" -> listOf("Modern Standard (MSA)", "Levantine", "Egyptian")
      else -> listOf("Standard Native", "Conversational Regional")
    }
  }
  var selectedAccent by remember(targetLang.code) { mutableStateOf(regionalAccents.first()) }

  val activePhrasePair = remember(drillIndex, practiceDrills, activeCustomPhrase) {
    if (activeCustomPhrase.isNotBlank()) {
      activeCustomPhrase to (initialWordOfTheDay?.meaning ?: "Custom practice phrase")
    } else {
      practiceDrills[drillIndex % practiceDrills.size]
    }
  }
  val currentPhrase = activePhrasePair.first
  val currentTranslation = activePhrasePair.second

  val isListening by speechManager.isListening.collectAsState()
  val isUserSpeaking by speechManager.isUserSpeaking.collectAsState()
  val isSpeaking by speechManager.isSpeaking.collectAsState()
  val rmsLevel by speechManager.rmsLevel.collectAsState()
  val spokenText by speechManager.spokenText.collectAsState()
  val activeTutorVoice by speechManager.activeTutorVoice.collectAsState()

  var hasMicPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }
  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasMicPermission = granted
    if (granted) {
      speechManager.startListening(targetLang.code)
    }
  }

  var isAnalyzing by remember { mutableStateOf(false) }
  var localHistory by remember { mutableStateOf(pronunciationHistory) }

  // Map 8-state EmojiVoice accurately to real voice events
  val effectiveCoachEmojiState = when {
    isAnalyzing -> EmojiVoiceState.PROCESSING
    isSpeaking -> EmojiVoiceState.REPLYING
    isListening && (isUserSpeaking || rmsLevel > 0.04f) -> EmojiVoiceState.SPEAKING
    isListening -> EmojiVoiceState.LISTENING
    isGeneratingDrills -> EmojiVoiceState.THINKING
    diagnosticResult != null -> if ((diagnosticResult?.accuracyPercent ?: 0) >= 70) EmojiVoiceState.DONE else EmojiVoiceState.ERROR
    else -> EmojiVoiceState.IDLE
  }

  LaunchedEffect(spokenText) {
    if (spokenText.isNotBlank()) {
      val recognized = spokenText.trim()
      speechManager.clearSpokenText()
      if (!isVerifiedPro) {
        AiErrorHandler.requestUpgrade(
          mode = UpgradeSheetMode.AI_UPGRADE,
          featureKey = "pronunciation_coach",
          headline = "Unlock AI Pronunciation Coach",
          reason = "Upgrade to LingoSphere Pro for AI pronunciation scoring and mouth-position coaching."
        )
        return@LaunchedEffect
      }
      isAnalyzing = true

      scope.launch {
        try {
          val diag = GeminiTutorService.analyzePronunciationDetailed(
            targetPhrase = currentPhrase,
            recognizedSpeech = recognized,
            targetLanguage = targetLang,
            nativeLanguage = nativeLang,
            regionalAccent = selectedAccent
          )
          diagnosticResult = diag
          localHistory = (listOf(diag) + localHistory).take(20)
          onRecordDiagnostic(diag)
          onSaveSession(
            SpeakingSession(
              userId = userProfile.userId,
              languageCode = targetLang.code,
              mode = "Pronunciation",
              scenarioTitle = "Pronunciation Coach: ${currentPhrase.take(30)}",
              proficiencyLevel = userProfile.proficiencyLevel,
              turnsCount = 1L,
              fluencyScore = diag.accuracyPercent.toLong(),
              grammarFeedbackSummary = "${diag.tongueAndMouthTip} • ${diag.intonationAdvice}"
            )
          )
        } catch (_: Exception) {
          val cleanTargetWords = currentPhrase.replace(Regex("[¿?¡!.,;:\"]"), "").trim().split(Regex("\\s+")).filter { it.isNotBlank() }
          val cleanSpokenWords = recognized.replace(Regex("[¿?¡!.,;:\"]"), "").trim().split(Regex("\\s+")).filter { it.isNotBlank() }
          val items = cleanTargetWords.mapIndexed { idx, tWord ->
            val exact = cleanSpokenWords.any { it.equals(tWord, ignoreCase = true) }
            val close = !exact && cleanSpokenWords.any { it.startsWith(tWord.take(3), ignoreCase = true) }
            when {
              exact -> com.example.data.model.WordPronunciationItem(tWord, tWord, "MATCH", "Clear articulation")
              close -> com.example.data.model.WordPronunciationItem(tWord, cleanSpokenWords.getOrNull(idx) ?: "", "CLOSE", "Check ending vowels")
              else -> com.example.data.model.WordPronunciationItem(tWord, cleanSpokenWords.getOrNull(idx) ?: "(missed)", "NEEDS_WORK", "Focus on syllable stress")
            }
          }
          val score = if (cleanSpokenWords.isEmpty()) 0 else {
            val pts = items.sumOf { if (it.status == "MATCH") 100 else if (it.status == "CLOSE") 65 else 20 }
            (pts / items.size.coerceAtLeast(1)).coerceIn(0, 100)
          }
          val fallbackDiag = PronunciationDiagnosticResult(
            targetPhrase = currentPhrase,
            recognizedTranscript = recognized,
            accuracyPercent = score,
            ipaTranscription = "/${currentPhrase.lowercase().take(18)}/",
            stressedSyllablesGuide = "Clear native intonation",
            problematicSounds = listOf("Vowels", "Stress"),
            tongueAndMouthTip = "Keep your mouth relaxed and articulate the stressed vowel clearly.",
            intonationAdvice = "Maintain a steady, confident rhythm throughout the phrase.",
            regionalAccentNote = "$selectedAccent native standard",
            wordBreakdown = items
          )
          diagnosticResult = fallbackDiag
          localHistory = (listOf(fallbackDiag) + localHistory).take(20)
          onRecordDiagnostic(fallbackDiag)
        } finally {
          isAnalyzing = false
        }
      }
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "coach_mic_pulse")
  val micScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = 1.12f,
    animationSpec = infiniteRepeatable(
      animation = tween(600),
      repeatMode = RepeatMode.Reverse
    ),
    label = "coach_mic_scale"
  )

  val allCoachVoices = remember { TutorVoice.entries }
  val sheetBg = if (isDark) Color(0xFF060D1D) else Color.White

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(CoachNavyTop)
  ) {
    // -------------------------------------------------------------------------
    // COMPACT COLLAPSIBLE PREMIUM TOP BAR & TUTOR HEADER
    // -------------------------------------------------------------------------
    Surface(
      color = Color.Transparent,
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.statusBars)
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
          ) {
            IconButton(
              onClick = onBack,
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .testTag("pronunciation_back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
              )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Tutor Avatar (keeps existing tutor portrait)
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.5.dp, CoachCyan.copy(alpha = 0.7f), CircleShape)
            ) {
              Image(
                painter = painterResource(id = R.drawable.img_pronunciation_tutor),
                contentDescription = "Pronunciation Tutor",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
              Text(
                text = "Pronunciation Coach",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("pronunciation_screen_title")
              )
              Text(
                text = "${targetLang.name} • Speak clearly & naturally",
                color = CoachCyan,
                fontSize = 11.5.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
          ) {
            if (!isVerifiedPro) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = CoachGold.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, CoachGold.copy(alpha = 0.55f)),
                modifier = Modifier
                  .padding(end = 6.dp)
                  .clickable { onOpenSubscription() }
                  .testTag("pro_subscription_button")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    Icons.Default.WorkspacePremium,
                    contentDescription = "Upgrade to Pro",
                    tint = CoachGold,
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    "Upgrade",
                    color = CoachGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            IconButton(
              onClick = { showSettingsDialog = true },
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .testTag("pronunciation_settings_button")
            ) {
              Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Pronunciation Settings",
                tint = Color.White
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // -------------------------------------------------------------------------
    // LOWER WHITE / DARK ELEVATED SHEET WITH SMOOTH SCROLLING
    // -------------------------------------------------------------------------
    Surface(
      shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
      color = sheetBg,
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 18.dp, vertical = 14.dp)
          .windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // ---------------------------------------------------------------------
        // 3 SEGMENTED MODE TABS: Practice, Sounds, Feedback
        // ---------------------------------------------------------------------
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
            .padding(3.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          // Tab 0: Practice
          val isPracticeActive = activeTab == 0
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isPracticeActive) CoachElectricBlue else Color.Transparent,
            modifier = Modifier
              .weight(1f)
              .clickable { activeTab = 0 }
              .testTag("tab_coach_practice")
          ) {
            Row(
              modifier = Modifier.padding(vertical = 9.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.Mic,
                contentDescription = null,
                tint = if (isPracticeActive) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                "Practice",
                color = if (isPracticeActive) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          // Tab 1: Sounds
          val isSoundsActive = activeTab == 1
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isSoundsActive) CoachElectricBlue else Color.Transparent,
            modifier = Modifier
              .weight(1f)
              .clickable { activeTab = 1 }
              .testTag("tab_coach_sounds")
          ) {
            Row(
              modifier = Modifier.padding(vertical = 9.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.GraphicEq,
                contentDescription = null,
                tint = if (isSoundsActive) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                "Sounds",
                color = if (isSoundsActive) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          // Tab 2: Feedback
          val isFeedbackActive = activeTab == 2
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isFeedbackActive) CoachElectricBlue else Color.Transparent,
            modifier = Modifier
              .weight(1f)
              .clickable { activeTab = 2 }
              .testTag("tab_coach_feedback")
          ) {
            Row(
              modifier = Modifier.padding(vertical = 9.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.TrendingUp,
                contentDescription = null,
                tint = if (isFeedbackActive) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                "Feedback",
                color = if (isFeedbackActive) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ---------------------------------------------------------------------
        // HORIZONTALLY SCROLLABLE VOICE SELECTOR WITH PREVIEW (Preserved on tabs)
        // ---------------------------------------------------------------------
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Voice:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
          )
          allCoachVoices.forEach { v ->
            val isSelected = activeTutorVoice.id == v.id
            FilterChip(
              selected = isSelected,
              onClick = {
                speechManager.setTutorVoice(v)
                speechManager.speakWithTutorVoice(currentPhrase, v, targetLang.code)
              },
              label = {
                Text(
                  text = "${v.id} (${v.styleTag})",
                  fontSize = 11.5.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CoachElectricBlue,
                selectedLabelColor = Color.White
              ),
              modifier = Modifier.testTag("coach_voice_chip_${v.id}")
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // =====================================================================
        // TAB 0: PRACTICE
        // =====================================================================
        when (activeTab) {
          0 -> {
            // Target Practice Header Chip: Switch sentence
            Surface(
              onClick = {
                drillIndex = (drillIndex + 1) % practiceDrills.size
                activeCustomPhrase = ""
                diagnosticResult = null
              },
              shape = RoundedCornerShape(16.dp),
              color = CoachElectricBlue.copy(alpha = 0.12f),
              border = BorderStroke(1.dp, CoachElectricBlue.copy(alpha = 0.35f)),
              modifier = Modifier.testTag("pronunciation_next_sentence_chip")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  Icons.Default.Refresh,
                  contentDescription = "Switch sentence",
                  tint = CoachElectricBlue,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Practice this sentence",
                  color = CoachElectricBlue,
                  fontSize = 12.5.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Large Target Phrase
            Text(
              text = currentPhrase,
              color = if (isDark) Color.White else Color(0xFF0F172A),
              fontSize = 23.sp,
              fontWeight = FontWeight.ExtraBold,
              textAlign = TextAlign.Center,
              lineHeight = 31.sp,
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("pronunciation_target_phrase_text")
            )

            if (currentTranslation.isNotBlank()) {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = currentTranslation,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Blue Pill: [▶ Listen (Voice)]
            Button(
              onClick = {
                speechManager.speakWithTutorVoice(currentPhrase, activeTutorVoice, targetLang.code)
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = CoachElectricBlue,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(20.dp),
              modifier = Modifier.testTag("coach_listen_button")
            ) {
              Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                "Listen (${activeTutorVoice.id})",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
              )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Center Recording Control with Integrated 8-State EmojiVoice
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(86.dp)
                .then(if (isListening) Modifier.scale(micScale) else Modifier)
                .clip(CircleShape)
                .background(
                  Brush.radialGradient(
                    colors = if (isListening) {
                      listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                    } else {
                      listOf(Color(0xFF38BDF8), CoachElectricBlue)
                    }
                  )
                )
                .clickable {
                  if (!isVerifiedPro) {
                    AiErrorHandler.requestUpgrade(
                      mode = UpgradeSheetMode.AI_UPGRADE,
                      featureKey = "pronunciation_coach",
                      headline = "Unlock AI Pronunciation Coach",
                      reason = "Upgrade to LingoSphere Pro for live pronunciation scoring and syllable feedback."
                    )
                  } else if (isListening) {
                    speechManager.finishListeningNow()
                  } else {
                    if (hasMicPermission) {
                      diagnosticResult = null
                      speechManager.startListening(targetLang.code)
                    } else {
                      micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                  }
                }
                .testTag("coach_mic_button")
            ) {
              EmojiVoice(
                state = effectiveCoachEmojiState,
                size = 56.dp,
                audioLevel = rmsLevel,
                isRecording = isListening,
                isPlaying = isSpeaking,
                showStateLabel = false,
                onClick = {
                  if (!isVerifiedPro) {
                    AiErrorHandler.requestUpgrade(
                      mode = UpgradeSheetMode.AI_UPGRADE,
                      featureKey = "pronunciation_coach",
                      headline = "Unlock AI Pronunciation Coach",
                      reason = "Upgrade to LingoSphere Pro for live pronunciation scoring and syllable feedback."
                    )
                  } else if (isListening) {
                    speechManager.finishListeningNow()
                  } else {
                    if (hasMicPermission) {
                      diagnosticResult = null
                      speechManager.startListening(targetLang.code)
                    } else {
                      micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                  }
                }
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = when {
                isAnalyzing -> "Processing speech & acoustics..."
                isListening && isUserSpeaking -> "Speaking detected..."
                isListening -> "Listening... speak now"
                isSpeaking -> "Tutor speaking..."
                diagnosticResult != null -> "Evaluation complete!"
                else -> "Tap emoji mic to practice"
              },
              color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
            )

            if (isListening) {
              Spacer(modifier = Modifier.height(8.dp))
              LinearProgressIndicator(
                progress = { (rmsLevel.coerceIn(0f, 10f) / 10f).coerceIn(0.1f, 1f) },
                color = CoachElectricBlue,
                trackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                modifier = Modifier
                  .width(140.dp)
                  .height(4.dp)
                  .clip(RoundedCornerShape(2.dp))
              )
              Spacer(modifier = Modifier.height(6.dp))
              TextButton(onClick = { speechManager.finishListeningNow() }) {
                Text("Finish & Evaluate", color = CoachElectricBlue, fontWeight = FontWeight.Bold)
              }
            }

            if (isAnalyzing) {
              Spacer(modifier = Modifier.height(14.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
              ) {
                CircularProgressIndicator(
                  color = CoachElectricBlue,
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  "Evaluating pronunciation acoustics...",
                  color = CoachElectricBlue,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pronunciation Feedback Card
            if (diagnosticResult == null && !isListening && !isAnalyzing) {
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                  containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)
                ),
                border = BorderStroke(1.dp, CoachElectricBlue.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CoachElectricBlue.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = CoachElectricBlue,
                        modifier = Modifier.size(22.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                    text = "Get instant pronunciation feedback with tips to sound more natural.",
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF1E3A8A),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                  )
                }
              }
            } else if (diagnosticResult != null) {
              val diag = diagnosticResult!!
              val isGood = diag.accuracyPercent >= 75
              val statusColor = if (isGood) CoachEmerald else CoachAmber

              Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                  containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                ),
                border = BorderStroke(1.5.dp, statusColor.copy(alpha = 0.5f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("pronunciation_feedback_card")
              ) {
                Column(
                  modifier = Modifier.padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = CoachEmerald,
                        modifier = Modifier.size(20.dp)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        "AI Feedback",
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                      )
                    }

                    Surface(
                      shape = RoundedCornerShape(10.dp),
                      color = statusColor,
                      modifier = Modifier.testTag("pronunciation_score_badge")
                    ) {
                      Text(
                        text = "${diag.accuracyPercent}%",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                      )
                    }
                  }

                  Text(
                    text = if (isGood) "Good pronunciation!" else "Clear intent! A little more articulation will make it shine.",
                    color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                  )

                  val tipText = diag.tongueAndMouthTip.ifBlank {
                    "Practice connecting syllables smoothly with relaxed mouth placement."
                  }
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CoachAmber.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, CoachAmber.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier.padding(10.dp),
                      verticalAlignment = Alignment.Top
                    ) {
                      Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = CoachAmber,
                        modifier = Modifier.size(16.dp)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = "Tip: $tipText",
                        color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium
                      )
                    }
                  }

                  if (diag.wordBreakdown.isNotEmpty()) {
                    Row(
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                      modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                    ) {
                      diag.wordBreakdown.forEach { item ->
                        val wordColor = when (item.status) {
                          "MATCH" -> CoachEmerald
                          "CLOSE" -> CoachAmber
                          else -> Color(0xFFEF4444)
                        }
                        Surface(
                          shape = RoundedCornerShape(8.dp),
                          color = wordColor.copy(alpha = 0.15f),
                          border = BorderStroke(1.dp, wordColor),
                          modifier = Modifier.clickable {
                            speechManager.speak(item.targetWord, targetLang.code, speechRate = 0.8f)
                          }
                        ) {
                          Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            Text(
                              item.targetWord,
                              color = if (isDark) Color.White else Color(0xFF0F172A),
                              fontSize = 12.sp,
                              fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                              Icons.AutoMirrored.Filled.VolumeUp,
                              contentDescription = "Listen word",
                              tint = wordColor,
                              modifier = Modifier.size(12.dp)
                            )
                          }
                        }
                      }
                    }
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    OutlinedButton(
                      onClick = {
                        speechManager.speak(currentPhrase, targetLang.code, speechRate = 0.85f)
                      },
                      shape = RoundedCornerShape(12.dp),
                      modifier = Modifier
                        .weight(1f)
                        .testTag("pronunciation_relisten_button")
                    ) {
                      Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Listen Again", fontSize = 12.sp)
                    }

                    OutlinedButton(
                      onClick = {
                        diagnosticResult = null
                        if (hasMicPermission) {
                          speechManager.startListening(targetLang.code)
                        } else {
                          micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                      },
                      shape = RoundedCornerShape(12.dp),
                      modifier = Modifier
                        .weight(1f)
                        .testTag("pronunciation_retry_button")
                    ) {
                      Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Try Again", fontSize = 12.sp)
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Primary Action Button: Next Practice →
            Button(
              onClick = {
                if (drillIndex + 1 < practiceDrills.size) {
                  drillIndex++
                  activeCustomPhrase = ""
                  diagnosticResult = null
                } else {
                  loadDynamicAiDrills(advanceTopic = true, userInitiated = true)
                }
              },
              enabled = !isGeneratingDrills,
              colors = ButtonDefaults.buttonColors(
                containerColor = CoachElectricBlue,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(24.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("coach_next_practice_button")
            ) {
              if (isGeneratingDrills) {
                CircularProgressIndicator(
                  color = Color.White,
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  "Personalizing Next Drills...",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold
                )
              } else {
                Text(
                  "Next Practice →",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            if (drillGenerationError != null) {
              Spacer(modifier = Modifier.height(10.dp))
              LingoCalmAiErrorBanner(
                message = drillGenerationError,
                onRetry = { loadDynamicAiDrills(advanceTopic = false, userInitiated = true) },
                onDismiss = { drillGenerationError = null }
              )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Clean, non-wrapping Word Cards Section
            TopMispronouncedWordsSection(
              targetLangCode = targetLang.code,
              speechManager = speechManager,
              onSelectWordForPractice = { chosenWord ->
                activeCustomPhrase = chosenWord
                diagnosticResult = null
                speechManager.speak(chosenWord, targetLang.code)
              }
            )
          }

          // =====================================================================
          // TAB 1: SOUNDS
          // =====================================================================
          1 -> {
            Text(
              "Practice Difficult Sounds",
              color = if (isDark) Color.White else Color(0xFF0F172A),
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              "Interactive phoneme lessons with accurate IPA, example words, and speaking exercises.",
              color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
              fontSize = 12.sp,
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text("Regional Accent Preference", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155), modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              regionalAccents.forEach { accent ->
                FilterChip(
                  selected = selectedAccent == accent,
                  onClick = { selectedAccent = accent },
                  label = { Text(accent, fontSize = 12.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CoachElectricBlue,
                    selectedLabelColor = Color.White
                  )
                )
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val soundDrills = remember(targetLang.code) {
              when (targetLang.code) {
                "es" -> listOf(
                  Triple("Rolling R (/r/)", "Ferrocarril", "Vibrate the tip of the tongue against the alveolar ridge behind your front teeth."),
                  Triple("Intervocalic D (/ð/)", "Cuidado", "Place tongue lightly between teeth like 'th' in 'feather'."),
                  Triple("Soft G / J (/x/)", "Gente", "Gentle velar friction from the back of the palate.")
                )
                "fr" -> listOf(
                  Triple("Nasal Vowel (/ɑ̃/)", "Enchanteur", "Lower the soft palate so air flows smoothly through the nose."),
                  Triple("Uvular R (/ʁ/)", "Grenouille", "Light friction between back of tongue and uvula."),
                  Triple("Front Rounded (/y/)", "Lune", "Say 'ee' while pursing lips tightly into an 'oo' circle.")
                )
                "de" -> listOf(
                  Triple("Ach-Laut (/x/)", "Achtung", "Back of tongue raises toward velum."),
                  Triple("Ich-Laut (/ç/)", "Ich liebe dich", "Tongue arches near hard palate like in 'huge'."),
                  Triple("Umlaut (/øː/)", "Schön", "Form lips for 'o' while tongue produces 'e'.")
                )
                else -> listOf(
                  Triple("Voiced Dental Fricative (/ð/)", "Weather", "Place tongue gently between teeth and vibrate vocal cords."),
                  Triple("Voiceless Dental Fricative (/θ/)", "Thought", "Breath flows freely between teeth without vocal vibration."),
                  Triple("Liquid Alveolar Approximant (/ɹ/)", "Resilience", "Curl tongue slightly back without touching palate.")
                )
              }
            }

            soundDrills.forEach { (soundName, sampleWord, instruction) ->
              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                  containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                ),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(soundName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CoachElectricBlue)
                    Text("Word: \"$sampleWord\"", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(instruction, fontSize = 11.5.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                  }
                  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                      onClick = {
                        speechManager.speak(sampleWord, targetLang.code, speechRate = 0.85f)
                      }
                    ) {
                      Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = CoachCyan)
                    }
                    IconButton(
                      onClick = {
                        activeCustomPhrase = sampleWord
                        activeTab = 0
                        speechManager.speak(sampleWord, targetLang.code, speechRate = 0.85f)
                      }
                    ) {
                      Icon(Icons.Default.Mic, contentDescription = "Practice this sound", tint = CoachElectricBlue)
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Text Practice
            Text("Custom Text Practice", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155), modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
              value = customInputText,
              onValueChange = { customInputText = it },
              placeholder = { Text("Type any word or sentence in ${targetLang.name}...") },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth(),
              singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
              onClick = {
                if (customInputText.isNotBlank()) {
                  activeCustomPhrase = customInputText.trim()
                  activeTab = 0
                }
              },
              enabled = customInputText.isNotBlank(),
              colors = ButtonDefaults.buttonColors(containerColor = CoachElectricBlue),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Practice My Custom Text")
            }

            Spacer(modifier = Modifier.height(18.dp))
            TopMispronouncedWordsSection(
              targetLangCode = targetLang.code,
              speechManager = speechManager,
              onSelectWordForPractice = { chosenWord ->
                activeCustomPhrase = chosenWord
                diagnosticResult = null
                activeTab = 0
                speechManager.speak(chosenWord, targetLang.code)
              }
            )
          }

          // =====================================================================
          // TAB 2: FEEDBACK
          // =====================================================================
          2 -> {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(
                modifier = Modifier
                  .weight(1f)
                  .padding(end = 12.dp)
              ) {
                Text(
                  "Pronunciation History",
                  color = if (isDark) Color.White else Color(0xFF0F172A),
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  "Real results from completed exercises: accuracy, detected mistakes, and progress history.",
                  color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                  fontSize = 12.sp
                )
              }

              Button(
                onClick = {
                  if (!isVerifiedPro) {
                    AiErrorHandler.requestUpgrade(
                      mode = UpgradeSheetMode.AI_UPGRADE,
                      featureKey = "pronunciation_drills",
                      headline = "Unlock Fresh AI Drills",
                      reason = "Upgrade to LingoSphere Pro for custom AI pronunciation drills."
                    )
                    return@Button
                  }
                  isGeneratingDrills = true
                  drillGenerationError = null
                  scope.launch {
                    try {
                      val fresh = GeminiTutorService.generatePronunciationPracticeDrills(
                        targetLanguage = targetLang,
                        nativeLanguage = nativeLang,
                        cefrLevel = userProfile.proficiencyLevel.ifBlank { "B1" },
                        topic = "Conversational Fluency",
                        weakSounds = pastWeakSounds
                      )
                      if (fresh.isNotEmpty()) {
                        practiceDrills = fresh
                        drillIndex = 0
                        activeTab = 0
                      }
                    } catch (e: Exception) {
                      drillGenerationError = AiErrorHandler.handle(e, contextTag = "FreshDrills").userMessage
                    } finally {
                      isGeneratingDrills = false
                    }
                  }
                },
                enabled = !isGeneratingDrills,
                colors = ButtonDefaults.buttonColors(containerColor = CoachElectricBlue),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier
                  .testTag("coach_generate_drills_button")
                  .widthIn(min = 110.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center
                ) {
                  if (!isVerifiedPro) {
                    LingoGoldLockIcon(size = 13.dp)
                  } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
                  }
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (isGeneratingDrills) "Generating..." else "Fresh Drills",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (localHistory.isEmpty()) {
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                  containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                ),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = CoachElectricBlue,
                    modifier = Modifier.size(36.dp)
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text("No practices recorded yet", fontWeight = FontWeight.Bold)
                  Text(
                    "Speak your first sentence in the Practice tab to unlock instant feedback and history.",
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                  )
                }
              }
            } else {
              localHistory.forEach { item ->
                Card(
                  shape = RoundedCornerShape(14.dp),
                  colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                  ),
                  border = BorderStroke(
                    1.dp,
                    if (item.accuracyPercent >= 75) CoachEmerald.copy(alpha = 0.4f) else CoachAmber.copy(alpha = 0.4f)
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        item.targetPhrase,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                      )
                      Text(
                        "Spoken: \"${item.recognizedTranscript}\"",
                        fontSize = 12.sp,
                        color = CoachElectricBlue
                      )
                      if (item.tongueAndMouthTip.isNotBlank()) {
                        Text(
                          item.tongueAndMouthTip.take(90) + if (item.tongueAndMouthTip.length > 90) "..." else "",
                          fontSize = 11.sp,
                          color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                      }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = if (item.accuracyPercent >= 75) CoachEmerald else CoachAmber
                    ) {
                      Text(
                        "${item.accuracyPercent}%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
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

  if (showSettingsDialog) {
    AlertDialog(
      onDismissRequest = { showSettingsDialog = false },
      title = { Text("Pronunciation Coach Settings", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Target Language: ${targetLang.name} (${targetLang.flagEmoji})", fontWeight = FontWeight.SemiBold)
          Text("Accent Dialect:", fontSize = 13.sp, color = CoachElectricBlue)
          regionalAccents.forEach { accent ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedAccent = accent; showSettingsDialog = false }
                .padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                if (selectedAccent == accent) Icons.Default.Check else Icons.Default.GraphicEq,
                contentDescription = null,
                tint = if (selectedAccent == accent) CoachElectricBlue else Color.Gray,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(accent, fontSize = 13.sp)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showSettingsDialog = false }) {
          Text("Close")
        }
      }
    )
  }
}

/**
 * Clean, responsive Word Card component that formats:
 *   [Word] 🔊
 *   /IPA/ (full-width line, never in a narrow column)
 *   Meaning: ...
 *   [Practice]
 *   [Show pronunciation tips] (expandable)
 */
@Composable
fun TopMispronouncedWordsSection(
  targetLangCode: String,
  speechManager: SpeechManager,
  onSelectWordForPractice: (String) -> Unit
) {
  val baseWords = remember(targetLangCode) {
    com.example.data.repository.InteractiveGamesCatalogAndAiService.getTopMispronouncedWords(targetLangCode)
  }
  var customAddedWords by remember { mutableStateOf(listOf<com.example.data.repository.MispronouncedWordItem>()) }
  var newWordInput by remember { mutableStateOf("") }
  var newWordTipInput by remember { mutableStateOf("") }
  var showAddWordForm by remember { mutableStateOf(false) }

  val allWords = remember(baseWords, customAddedWords) { customAddedWords + baseWords }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D162B)),
    border = BorderStroke(1.dp, CoachCyan.copy(alpha = 0.35f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("top_mispronounced_words_section")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Top Mispronounced Words",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp
          )
          Text(
            text = "Correct your pronunciation with tricky words & phoneme guidance",
            color = Color(0xFF94A3B8),
            fontSize = 11.5.sp
          )
        }
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = CoachCyan.copy(alpha = 0.18f),
          border = BorderStroke(1.dp, CoachCyan),
          modifier = Modifier
            .clickable { showAddWordForm = !showAddWordForm }
            .testTag("speech_coach_add_word_toggle")
        ) {
          Text(
            text = if (showAddWordForm) "Close" else "+ Add Word",
            color = CoachCyan,
            fontWeight = FontWeight.Bold,
            fontSize = 11.5.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          )
        }
      }

      AnimatedVisibility(visible = showAddWordForm) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF071126))
            .padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = newWordInput,
            onValueChange = { newWordInput = it },
            placeholder = { Text("Enter tricky word or phrase...", fontSize = 12.sp) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("speech_coach_custom_word_input")
          )
          OutlinedTextField(
            value = newWordTipInput,
            onValueChange = { newWordTipInput = it },
            placeholder = { Text("Pronunciation note or reminder (optional)...", fontSize = 12.sp) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          )
          Button(
            onClick = {
              val trimmed = newWordInput.trim()
              if (trimmed.isNotEmpty()) {
                val item = com.example.data.repository.MispronouncedWordItem(
                  id = "custom_${System.currentTimeMillis()}",
                  word = trimmed,
                  phonetic = "/${trimmed.lowercase()}/",
                  commonMistakeNote = newWordTipInput.ifBlank { "Focus on clear syllable stress and smooth vowel transitions." },
                  tip = "Listen to the native coach voice first, then record at a steady pace.",
                  exampleSentence = "I am practicing how to pronounce $trimmed clearly.",
                  translation = "Custom Practice Word",
                  difficulty = "Intermediate"
                )
                customAddedWords = listOf(item) + customAddedWords
                newWordInput = ""
                newWordTipInput = ""
                showAddWordForm = false
                onSelectWordForPractice(trimmed)
              }
            },
            enabled = newWordInput.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
              containerColor = CoachCyan,
              contentColor = Color(0xFF04101E)
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("speech_coach_save_custom_word_btn")
          ) {
            Text("Add & Practice Now", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
          }
        }
      }

      allWords.forEachIndexed { idx, item ->
        CleanPronunciationWordCard(
          item = item,
          idx = idx,
          targetLangCode = targetLangCode,
          speechManager = speechManager,
          onPractice = { onSelectWordForPractice(item.word) }
        )
      }
    }
  }
}

/**
 * Individual Word Card formatted cleanly:
 *   [Word] 🔊
 *   /IPA/ on its own full-width line
 *   Meaning: ...
 *   [Practice]
 *   [Show pronunciation tips] (expandable)
 */
@Composable
private fun CleanPronunciationWordCard(
  item: com.example.data.repository.MispronouncedWordItem,
  idx: Int,
  targetLangCode: String,
  speechManager: SpeechManager,
  onPractice: () -> Unit
) {
  var isExpanded by remember { mutableStateOf(false) }

  // Clean raw IPA extraction (removing nested parenthetical notes from the IPA line so it's clean and readable)
  val cleanIpa = remember(item.phonetic) {
    val raw = item.phonetic.trim()
    val match = Regex("/[^/]+(?=/|\\)|$)").find(raw)?.value ?: raw
    if (match.startsWith("/")) "$match/" else "/$match/"
  }

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Color(0xFF0D162B),
    border = BorderStroke(1.dp, Color(0xFF1D2E52)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("mispronounced_word_card_$idx")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      // Line 1: Word prominently + Speaker button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = item.word,
          color = Color.White,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 18.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false)
        )

        IconButton(
          onClick = { speechManager.speak(item.word, targetLangCode, speechRate = 0.85f) },
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFF102342))
            .testTag("mispronounced_listen_btn_$idx")
        ) {
          Icon(
            Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = "Listen to ${item.word}",
            tint = CoachCyan,
            modifier = Modifier.size(19.dp)
          )
        }
      }

      // Line 2: IPA transcription on its OWN full-width line (never in a narrow column)
      Text(
        text = cleanIpa,
        color = CoachCyan,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 13.5.sp,
        modifier = Modifier.fillMaxWidth()
      )

      // Line 3: Meaning
      Text(
        text = "Meaning: ${item.translation}",
        color = Color(0xFF94A3B8),
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(2.dp))

      // Line 4: Practice button + Expand tips toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Button(
          onClick = onPractice,
          colors = ButtonDefaults.buttonColors(
            containerColor = CoachCyan,
            contentColor = Color(0xFF04101E)
          ),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
          modifier = Modifier
            .height(36.dp)
            .testTag("mispronounced_practice_btn_$idx")
        ) {
          Icon(
            Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Practice", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
        }

        Surface(
          onClick = { isExpanded = !isExpanded },
          shape = RoundedCornerShape(8.dp),
          color = Color.Transparent,
          modifier = Modifier
            .padding(start = 6.dp)
            .testTag("mispronounced_tips_toggle_btn_$idx")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp)
          ) {
            Text(
              text = if (isExpanded) "Hide tips" else "Show pronunciation tips",
              color = CoachGold,
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
              imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = null,
              tint = CoachGold,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }

      // Expandable section with detailed pronunciation tips and examples
      AnimatedVisibility(visible = isExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF071126))
            .padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = "Common mistake: ${item.commonMistakeNote}",
            color = Color(0xFFFBBF24),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "Tip: ${item.tip}",
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp
          )
          Text(
            text = "Example: \"${item.exampleSentence}\"",
            color = Color(0xFF94A3B8),
            fontSize = 11.5.sp
          )
        }
      }
    }
  }
}
