package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.AutoScrollLearningService
import com.example.ai.AutoScrollPageSource
import com.example.ai.GeminiTutorService
import com.example.data.model.CountryCultureJourney
import com.example.data.model.CountryJourneyStage
import com.example.data.model.CulturalAdvisorResponse
import com.example.data.model.CulturalJourneyRepository
import com.example.data.model.CulturalVocabCard
import com.example.data.model.Language
import com.example.data.model.SpeakingSession
import com.example.data.model.UserProfile
import com.example.data.model.VirtualTravelDestination
import com.example.data.model.VoiceDiaryAiEvaluation
import com.example.speech.SpeechManager
import com.example.ui.components.AutoScrollTeleprompterView
import com.example.ui.components.LanguagePickerDialog
import com.example.ui.theme.LingoAmber
import com.example.ui.theme.LingoCoral
import com.example.ui.theme.LingoCyan
import com.example.ui.theme.LingoElectricBlue
import com.example.ui.theme.LingoEmerald
import com.example.ui.theme.LingoGold
import com.example.ui.theme.LingoViolet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class CultureSubScreen {
  COUNTRY_JOURNEY,
  PASSPORT_MISSIONS,
  VIRTUAL_TRAVEL,
  ETIQUETTE_TRAINER,
  VOICE_DIARY,
  QUIZ_BATTLE,
  CULTURAL_AUTO_READER
}

@Composable
fun CulturePassportScreen(
  userProfile: UserProfile,
  speakingSessions: List<SpeakingSession>,
  speechManager: SpeechManager,
  initialTab: Int = 0,
  onSelectLanguageCode: (String) -> Unit,
  onSaveSpeakingSession: (SpeakingSession) -> Unit,
  onSaveVocabToFlashcard: (String, String, String, String) -> Unit,
  onUnlockAchievementAndXp: (String, String, String, Int) -> Unit,
  onOpenSpeakingRooms: () -> Unit = {},
  onOpenLanguageExchange: () -> Unit = {},
  onBack: () -> Unit
) {
  var currentSubScreen by remember {
    mutableStateOf<CultureSubScreen?>(
      when (initialTab) {
        1 -> CultureSubScreen.PASSPORT_MISSIONS
        2 -> CultureSubScreen.VIRTUAL_TRAVEL
        3 -> CultureSubScreen.ETIQUETTE_TRAINER
        4 -> CultureSubScreen.VOICE_DIARY
        5 -> CultureSubScreen.QUIZ_BATTLE
        else -> null
      }
    )
  }

  BackHandler {
    if (currentSubScreen != null) {
      currentSubScreen = null
    } else {
      onBack()
    }
  }

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var selectedLanguageCode by remember(userProfile.targetLanguageCode) {
    mutableStateOf(userProfile.targetLanguageCode)
  }

  // Support country-specific selection (e.g. US, GB, CA, AU for English, or ES, MX for Spanish)
  var selectedCountryCode by remember(selectedLanguageCode) {
    val defaultCountry = when (selectedLanguageCode.lowercase().trim()) {
      "en" -> "US"
      "es" -> "ES"
      "ja" -> "JP"
      "fr" -> "FR"
      "de" -> "DE"
      "ko" -> "KR"
      "it" -> "IT"
      "zh" -> "CN"
      else -> selectedLanguageCode.uppercase()
    }
    mutableStateOf(defaultCountry)
  }

  var showLanguagePicker by remember { mutableStateOf(false) }

  val currentJourney = remember(selectedLanguageCode, selectedCountryCode) {
    CulturalJourneyRepository.getJourneyForLanguage(selectedLanguageCode, selectedCountryCode)
  }
  val targetLang = remember(selectedLanguageCode) {
    Language.findByCode(selectedLanguageCode)
  }
  val nativeLang = remember(userProfile.nativeLanguageCode) {
    Language.findByCode(userProfile.nativeLanguageCode)
  }

  val allCuratedJourneys = remember { CulturalJourneyRepository.getAllJourneys() }
  val unlockedStampsCount = remember(userProfile.achievements) {
    userProfile.achievements.count { it.startsWith("PASSPORT_") }
  }

  // Calculate completed stages for current journey
  val completedStagesCount = remember(userProfile.achievements, currentJourney.countryCode) {
    currentJourney.stages.count { stage ->
      userProfile.achievements.contains("STAGE_${currentJourney.countryCode}_${stage.stageNumber}")
    }
  }

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .testTag("culture_passport_screen"),
    color = Color(0xFF060D1D)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF060D1D))
        .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
      if (currentSubScreen == null) {
        // ====================================================================
        // MAIN CULTURE HUB (Redesigned clean, modern, responsive view)
        // ====================================================================
        MainCultureHubView(
          userProfile = userProfile,
          targetLang = targetLang,
          currentJourney = currentJourney,
          allJourneys = allCuratedJourneys,
          selectedCountryCode = selectedCountryCode,
          unlockedStampsCount = unlockedStampsCount,
          completedStagesCount = completedStagesCount,
          onSelectCountry = { journey ->
            selectedCountryCode = journey.countryCode
            selectedLanguageCode = journey.languageCode
            onSelectLanguageCode(journey.languageCode)
          },
          onOpenLanguagePicker = { showLanguagePicker = true },
          onOpenSubScreen = { subScreen -> currentSubScreen = subScreen },
          onOpenSpeakingRooms = onOpenSpeakingRooms,
          onOpenLanguageExchange = onOpenLanguageExchange,
          onBack = onBack
        )
      } else {
        // ====================================================================
        // SUB-FEATURE DETAIL VIEW WITH COMPACT TOP BAR
        // ====================================================================
        SubFeatureHeader(
          subScreen = currentSubScreen!!,
          journey = currentJourney,
          onBackToHub = { currentSubScreen = null }
        )

        Box(
          modifier = Modifier
            .fillMaxSize()
            .weight(1f)
        ) {
          when (currentSubScreen!!) {
            CultureSubScreen.COUNTRY_JOURNEY -> CountryJourneySubScreen(
              journey = currentJourney,
              targetLang = targetLang,
              userProfile = userProfile,
              speechManager = speechManager,
              onSaveVocab = { card ->
                onSaveVocabToFlashcard(card.phrase, card.meaning, card.phonetic, targetLang.code)
                Toast.makeText(context, "Saved '${card.phrase}' to Flashcards!", Toast.LENGTH_SHORT).show()
              },
              onSaveSpeakingSession = onSaveSpeakingSession,
              onUnlockAchievementAndXp = onUnlockAchievementAndXp
            )

            CultureSubScreen.PASSPORT_MISSIONS -> PassportMissionsSubScreen(
              currentJourney = currentJourney,
              allJourneys = allCuratedJourneys,
              userProfile = userProfile,
              onSelectCountry = { journey ->
                selectedCountryCode = journey.countryCode
                selectedLanguageCode = journey.languageCode
                onSelectLanguageCode(journey.languageCode)
              },
              onNavigateToFeature = { targetFeature -> currentSubScreen = targetFeature },
              onUnlockAchievementAndXp = onUnlockAchievementAndXp
            )

            CultureSubScreen.VIRTUAL_TRAVEL -> VirtualTravelSubScreen(
              journey = currentJourney,
              targetLang = targetLang,
              nativeLang = nativeLang,
              userProfile = userProfile,
              speechManager = speechManager,
              onSaveSpeakingSession = onSaveSpeakingSession,
              onUnlockAchievementAndXp = onUnlockAchievementAndXp
            )

            CultureSubScreen.ETIQUETTE_TRAINER -> EtiquetteTrainerSubScreen(
              journey = currentJourney,
              targetLang = targetLang,
              nativeLang = nativeLang,
              speechManager = speechManager,
              onUnlockAchievementAndXp = onUnlockAchievementAndXp
            )

            CultureSubScreen.VOICE_DIARY -> VoiceDiarySubScreen(
              targetLang = targetLang,
              nativeLang = nativeLang,
              userProfile = userProfile,
              speakingSessions = speakingSessions,
              speechManager = speechManager,
              onSaveSpeakingSession = onSaveSpeakingSession,
              onUnlockAchievementAndXp = onUnlockAchievementAndXp
            )

            CultureSubScreen.QUIZ_BATTLE -> CultureQuizBattleSubScreen(
              journey = currentJourney,
              userProfile = userProfile,
              onUnlockAchievementAndXp = onUnlockAchievementAndXp
            )

            CultureSubScreen.CULTURAL_AUTO_READER -> {
              var selectedCultureCefr by remember {
                mutableStateOf(
                  when (userProfile.proficiencyLevel.lowercase()) {
                    "advanced" -> "B2"
                    "intermediate" -> "B1"
                    else -> "A2"
                  }
                )
              }
              var selectedCultureCat by remember {
                mutableStateOf(AutoScrollLearningService.CULTURE_CATEGORIES.first())
              }
              var isGeneratingCulturalStory by remember { mutableStateOf(false) }
              var culturalError by remember { mutableStateOf<String?>(null) }
              var culturalAutoContent by remember(currentJourney.countryCode, targetLang.code) {
                mutableStateOf(
                  AutoScrollLearningService.loadCachedContentList(
                    context = context,
                    userId = userProfile.userId,
                    pageSource = AutoScrollPageSource.CULTURE_JOURNEY,
                    languageCode = targetLang.code
                  ).firstOrNull() ?: AutoScrollLearningService.fromCultureJourney(
                    journey = currentJourney,
                    nativeLanguageCode = nativeLang.code
                  )
                )
              }

              Box(modifier = Modifier.fillMaxSize()) {
                AutoScrollTeleprompterView(
                  userId = userProfile.userId,
                  content = culturalAutoContent,
                  availableContents = currentJourney.stages.map { stage ->
                    AutoScrollLearningService.fromCultureJourney(
                      journey = currentJourney,
                      stage = stage,
                      nativeLanguageCode = nativeLang.code
                    )
                  },
                  categories = AutoScrollLearningService.CULTURE_CATEGORIES,
                  selectedCategory = selectedCultureCat,
                  selectedCefrLevel = selectedCultureCefr,
                  isFullScreen = true,
                  isGenerating = isGeneratingCulturalStory,
                  errorMessage = culturalError,
                  speechManager = speechManager,
                  onSelectContent = { chosen ->
                    culturalError = null
                    culturalAutoContent = chosen
                  },
                  onSelectCategory = { cat ->
                    selectedCultureCat = cat
                    val cachedMatch = AutoScrollLearningService.loadCachedContentList(
                      context = context,
                      userId = userProfile.userId,
                      pageSource = AutoScrollPageSource.CULTURE_JOURNEY,
                      languageCode = targetLang.code
                    ).firstOrNull {
                      it.category.equals(cat, ignoreCase = true) &&
                        it.cefrLevel.equals(selectedCultureCefr, ignoreCase = true)
                    }
                    if (cachedMatch != null) {
                      culturalAutoContent = cachedMatch
                    } else {
                      coroutineScope.launch {
                        isGeneratingCulturalStory = true
                        culturalError = null
                        val res = AutoScrollLearningService.generateFreshAutoScrollContent(
                          context = context,
                          userId = userProfile.userId,
                          pageSource = AutoScrollPageSource.CULTURE_JOURNEY,
                          targetLanguageCode = targetLang.code,
                          nativeLanguageCode = nativeLang.code,
                          cefrLevel = selectedCultureCefr,
                          category = cat,
                          topic = "${currentJourney.countryName} (${currentJourney.capitalCity}) — $cat",
                          isPremiumUser = userProfile.isPremium
                        )
                        isGeneratingCulturalStory = false
                        when (res) {
                          is com.example.ai.AutoScrollGenerationResult.Success -> {
                            culturalAutoContent = res.content
                          }
                          is com.example.ai.AutoScrollGenerationResult.Error -> {
                            culturalError = res.userFriendlyMessage
                          }
                        }
                      }
                    }
                  },
                  onSelectCefrLevel = { cefr -> selectedCultureCefr = cefr },
                  onRequestGenerateNew = { cat, cefr, len, customTopic ->
                    selectedCultureCat = cat
                    selectedCultureCefr = cefr
                    coroutineScope.launch {
                      isGeneratingCulturalStory = true
                      culturalError = null
                      val res = AutoScrollLearningService.generateFreshAutoScrollContent(
                        context = context,
                        userId = userProfile.userId,
                        pageSource = AutoScrollPageSource.CULTURE_JOURNEY,
                        targetLanguageCode = targetLang.code,
                        nativeLanguageCode = nativeLang.code,
                        cefrLevel = cefr,
                        category = cat,
                        topic = customTopic.takeIf { it.isNotBlank() }
                          ?: "${currentJourney.countryName} cultural immersion: $cat",
                        contentLength = len,
                        isPremiumUser = userProfile.isPremium
                      )
                      isGeneratingCulturalStory = false
                      when (res) {
                        is com.example.ai.AutoScrollGenerationResult.Success -> {
                          culturalAutoContent = res.content
                        }
                        is com.example.ai.AutoScrollGenerationResult.Error -> {
                          culturalError = res.userFriendlyMessage
                        }
                      }
                    }
                  },
                  onSaveVocabToFlashcard = { word, meaning, phonetic ->
                    onSaveVocabToFlashcard(word, meaning, phonetic, targetLang.code)
                    Toast.makeText(context, "Saved '$word' to Flashcards!", Toast.LENGTH_SHORT).show()
                  },
                  onCompleteActivity = { _, title, scorePct, xp ->
                    onUnlockAchievementAndXp(
                      "CULTURE_READ_${currentJourney.countryCode}",
                      "Cultural Story Completed",
                      "Completed $title ($scorePct%)",
                      xp
                    )
                  },
                  onBack = {
                    speechManager.stopSpeaking()
                    currentSubScreen = null
                  }
                )
              }
            }
          }
        }
      }
    }
  }

  if (showLanguagePicker) {
    LanguagePickerDialog(
      title = "Select Cultural Target Language",
      currentLanguageCode = selectedLanguageCode,
      onLanguageSelected = { lang: Language ->
        selectedLanguageCode = lang.code
        val defaultCountry = when (lang.code.lowercase()) {
          "en" -> "US"
          "es" -> "ES"
          "ja" -> "JP"
          "fr" -> "FR"
          "de" -> "DE"
          "ko" -> "KR"
          "it" -> "IT"
          "zh" -> "CN"
          else -> lang.code.uppercase()
        }
        selectedCountryCode = defaultCountry
        onSelectLanguageCode(lang.code)
        showLanguagePicker = false
      },
      onDismiss = { showLanguagePicker = false }
    )
  }
}

// ============================================================================
// 1. MAIN CULTURE HUB COMPONENT
// ============================================================================
@Composable
private fun MainCultureHubView(
  userProfile: UserProfile,
  targetLang: Language,
  currentJourney: CountryCultureJourney,
  allJourneys: List<CountryCultureJourney>,
  selectedCountryCode: String,
  unlockedStampsCount: Int,
  completedStagesCount: Int,
  onSelectCountry: (CountryCultureJourney) -> Unit,
  onOpenLanguagePicker: () -> Unit,
  onOpenSubScreen: (CultureSubScreen) -> Unit,
  onOpenSpeakingRooms: () -> Unit,
  onOpenLanguageExchange: () -> Unit,
  onBack: () -> Unit
) {
  val hasCountryStamp = userProfile.achievements.contains(currentJourney.passportStampCode)
  val stageProgressFloat = (completedStagesCount.coerceIn(0, 10).toFloat() / 10f)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
  ) {
    // ------------------------------------------------------------------------
    // Clean Header
    // ------------------------------------------------------------------------
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0xFF0E214A))
            .testTag("culture_back_button")
        ) {
          Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color.White
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "Culture & World Hub",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "Learn Languages. Explore Cultures.",
            color = LingoCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }

      // Language Selector Pill
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clip(RoundedCornerShape(20.dp))
          .background(Color(0xFF102552))
          .border(1.2.dp, LingoCyan, RoundedCornerShape(20.dp))
          .clickable { onOpenLanguagePicker() }
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("culture_language_picker_pill")
      ) {
        Text(text = targetLang.flagEmoji, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = targetLang.name,
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    // ------------------------------------------------------------------------
    // Country Selector Chips (Distinguishing US, GB, CA, AU, JP, ES, MX, etc.)
    // ------------------------------------------------------------------------
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
    ) {
      items(allJourneys, key = { it.countryCode }) { journey ->
        val isSelected = journey.countryCode.equals(selectedCountryCode, ignoreCase = true)
        val hasStamp = userProfile.achievements.contains(journey.passportStampCode)
        FilterChip(
          selected = isSelected,
          onClick = { onSelectCountry(journey) },
          label = {
            Text(
              text = "${journey.flagEmoji} ${journey.countryName}" + (if (hasStamp) " 🛂" else ""),
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = LingoElectricBlue,
            selectedLabelColor = Color.White,
            containerColor = Color(0xFF0B1C3D),
            labelColor = Color(0xFFCBD5E1)
          ),
          border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = if (isSelected) LingoCyan else Color(0xFF1E3A75)
          )
        )
      }
      item {
        FilterChip(
          selected = false,
          onClick = onOpenLanguagePicker,
          label = { Text("🌐 100+ More...", fontSize = 12.sp, color = LingoCyan) },
          colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFF0B1C3D))
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // ------------------------------------------------------------------------
    // Featured Country Banner
    // ------------------------------------------------------------------------
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1E46)),
      border = BorderStroke(1.5.dp, if (hasCountryStamp) LingoGold else LingoCyan),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .testTag("featured_country_banner")
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            Brush.verticalGradient(
              listOf(Color(0xFF132B5E), Color(0xFF091636))
            )
          )
          .padding(18.dp)
      ) {
        Column {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Text(text = currentJourney.flagEmoji, fontSize = 42.sp)
              Spacer(modifier = Modifier.width(14.dp))
              Column {
                Text(
                  text = "${currentJourney.countryName} Journey",
                  color = Color.White,
                  fontSize = 20.sp,
                  fontWeight = FontWeight.ExtraBold
                )
                Text(
                  text = "Capital: ${currentJourney.capitalCity} • ${targetLang.name} (${targetLang.nativeName})",
                  color = LingoCyan,
                  fontSize = 12.5.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            if (hasCountryStamp) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .background(LingoGold.copy(alpha = 0.2f))
                  .border(1.2.dp, LingoGold, RoundedCornerShape(12.dp))
                  .padding(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Text("🛂 STAMPED", color = LingoGold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
              }
            } else {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0xFF0F2654))
                  .border(1.dp, Color(0xFF1E3E7B), RoundedCornerShape(12.dp))
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text("$unlockedStampsCount Stamps", color = Color(0xFF93C5FD), fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "“${currentJourney.motto}”",
            color = LingoGold,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = currentJourney.culturalOverview,
            color = Color(0xFFCBD5E1),
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Cultural Learning Progress
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Cultural Learning Progress: $completedStagesCount / 10 Stages",
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "${(stageProgressFloat * 100).toInt()}%",
              color = LingoCyan,
              fontSize = 12.sp,
              fontWeight = FontWeight.ExtraBold
            )
          }

          Spacer(modifier = Modifier.height(6.dp))
          LinearProgressIndicator(
            progress = { stageProgressFloat },
            color = LingoCyan,
            trackColor = Color(0xFF1B3566),
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp))
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Continue Journey Button
          Button(
            onClick = { onOpenSubScreen(CultureSubScreen.COUNTRY_JOURNEY) },
            colors = ButtonDefaults.buttonColors(
              containerColor = LingoElectricBlue,
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("continue_journey_button")
          ) {
            Text(
              text = if (completedStagesCount >= 10) "Review 10 Stages 🎓" else "Continue Country Journey →",
              fontSize = 14.sp,
              fontWeight = FontWeight.ExtraBold
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedButton(
            onClick = { onOpenSubScreen(CultureSubScreen.CULTURAL_AUTO_READER) },
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = Color(0xFF0A2838),
              contentColor = LingoCyan
            ),
            border = BorderStroke(1.2.dp, LingoCyan.copy(alpha = 0.65f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("culture_open_auto_scroll_reader_btn")
          ) {
            Icon(
              Icons.Default.PlayArrow,
              contentDescription = null,
              tint = LingoEmerald,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Auto-Scrolling Cultural Stories & Audio (${currentJourney.countryName})",
              fontSize = 12.5.sp,
              fontWeight = FontWeight.ExtraBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // ------------------------------------------------------------------------
    // Main Feature Navigation: 2-Column Grid of 8 Compact Cards
    // ------------------------------------------------------------------------
    Text(
      text = "Explore Cultural Features",
      color = Color.White,
      fontSize = 17.sp,
      fontWeight = FontWeight.ExtraBold,
      modifier = Modifier.padding(horizontal = 16.dp)
    )
    Spacer(modifier = Modifier.height(10.dp))

    Column(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
    ) {
      // Row 1: Country Journey + Passport & Missions
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        FeatureGridCard(
          title = "Country Journey",
          subtitle = "10 Immersion Stages",
          badgeText = "10 Stages",
          badgeColor = LingoCyan,
          emoji = "🌍",
          modifier = Modifier.weight(1f),
          onClick = { onOpenSubScreen(CultureSubScreen.COUNTRY_JOURNEY) }
        )
        FeatureGridCard(
          title = "Passport & Missions",
          subtitle = "Visa Stamps & Quests",
          badgeText = "+75 XP",
          badgeColor = LingoGold,
          emoji = "🛂",
          modifier = Modifier.weight(1f),
          onClick = { onOpenSubScreen(CultureSubScreen.PASSPORT_MISSIONS) }
        )
      }

      // Row 2: Virtual Travel + Etiquette Trainer
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        FeatureGridCard(
          title = "Virtual Travel",
          subtitle = "Iconic Cities & Personas",
          badgeText = "Live Guide",
          badgeColor = LingoEmerald,
          emoji = "✈️",
          modifier = Modifier.weight(1f),
          onClick = { onOpenSubScreen(CultureSubScreen.VIRTUAL_TRAVEL) }
        )
        FeatureGridCard(
          title = "Etiquette Trainer",
          subtitle = "Customs, Taboos & AI",
          badgeText = "Advisor",
          badgeColor = LingoViolet,
          emoji = "🤝",
          modifier = Modifier.weight(1f),
          onClick = { onOpenSubScreen(CultureSubScreen.ETIQUETTE_TRAINER) }
        )
      }

      // Row 3: Voice Diary + Quiz Battle
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        FeatureGridCard(
          title = "Voice Diary",
          subtitle = "Spoken Journal & Audio",
          badgeText = "Real Mic",
          badgeColor = LingoCoral,
          emoji = "🎙️",
          modifier = Modifier.weight(1f),
          onClick = { onOpenSubScreen(CultureSubScreen.VOICE_DIARY) }
        )
        FeatureGridCard(
          title = "Quiz Battle",
          subtitle = "Trivia, Idioms & XP",
          badgeText = "+100 XP",
          badgeColor = LingoGold,
          emoji = "⚔️",
          modifier = Modifier.weight(1f),
          onClick = { onOpenSubScreen(CultureSubScreen.QUIZ_BATTLE) }
        )
      }

      // Row 4: Speaking Rooms + Language Exchange
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        FeatureGridCard(
          title = "Speaking Rooms",
          subtitle = "Live Audio Lounges",
          badgeText = "WebRTC",
          badgeColor = LingoElectricBlue,
          emoji = "🗣️",
          modifier = Modifier.weight(1f),
          onClick = onOpenSpeakingRooms
        )
        FeatureGridCard(
          title = "Language Exchange",
          subtitle = "1-on-1 Chat & Audio",
          badgeText = "Partners",
          badgeColor = LingoCyan,
          emoji = "💬",
          modifier = Modifier.weight(1f),
          onClick = onOpenLanguageExchange
        )
      }
    }

    Spacer(modifier = Modifier.height(28.dp))
  }
}

@Composable
private fun FeatureGridCard(
  title: String,
  subtitle: String,
  badgeText: String,
  badgeColor: Color,
  emoji: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1E44)),
    border = BorderStroke(1.dp, Color(0xFF1B3B73)),
    modifier = modifier
      .clickable { onClick() }
      .testTag("feature_card_${title.lowercase().replace(" ", "_")}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(text = emoji, fontSize = 28.sp)
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(badgeColor.copy(alpha = 0.15f))
            .border(1.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = badgeText,
            color = badgeColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = title,
        color = Color.White,
        fontSize = 14.5.sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        color = Color(0xFF93C5FD),
        fontSize = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

// ============================================================================
// SUB-FEATURE HEADER
// ============================================================================
@Composable
private fun SubFeatureHeader(
  subScreen: CultureSubScreen,
  journey: CountryCultureJourney,
  onBackToHub: () -> Unit
) {
  val title = when (subScreen) {
    CultureSubScreen.COUNTRY_JOURNEY -> "Country Journey"
    CultureSubScreen.PASSPORT_MISSIONS -> "Passport & Missions"
    CultureSubScreen.VIRTUAL_TRAVEL -> "Virtual Travel & Dialects"
    CultureSubScreen.ETIQUETTE_TRAINER -> "Cultural Etiquette Trainer"
    CultureSubScreen.VOICE_DIARY -> "Daily Spoken Voice Diary"
    CultureSubScreen.QUIZ_BATTLE -> "Culture Quiz Battle"
    CultureSubScreen.CULTURAL_AUTO_READER -> "Cultural Auto-Scroll Teleprompter"
  }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 14.dp, vertical = 10.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f)
    ) {
      IconButton(
        onClick = onBackToHub,
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(Color(0xFF0E214A))
      ) {
        Icon(
          Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back to Main Hub",
          tint = Color.White
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = title,
          color = Color.White,
          fontSize = 17.5.sp,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "${journey.flagEmoji} ${journey.countryName} • Main Hub",
          color = LingoCyan,
          fontSize = 12.sp
        )
      }
    }

    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF102552))
        .border(1.dp, LingoCyan, RoundedCornerShape(12.dp))
        .clickable { onBackToHub() }
        .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
      Text("Hub Overview", color = LingoCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
  }
}

// ============================================================================
// 2. COUNTRY JOURNEY SUB-SCREEN (10 PROGRESSIVE STAGES)
// ============================================================================
@Composable
private fun CountryJourneySubScreen(
  journey: CountryCultureJourney,
  targetLang: Language,
  userProfile: UserProfile,
  speechManager: SpeechManager,
  onSaveVocab: (CulturalVocabCard) -> Unit,
  onSaveSpeakingSession: (SpeakingSession) -> Unit,
  onUnlockAchievementAndXp: (String, String, String, Int) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var expandedStageNum by remember(journey.countryCode) { mutableIntStateOf(1) }

  val isListening by speechManager.isListening.collectAsState()
  val spokenText by speechManager.spokenText.collectAsState()

  var hasMicPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }
  val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    hasMicPermission = granted
    if (granted) speechManager.startListening(targetLang.code)
  }

  // Track accuracy and answer per stage
  var activeSpeakingAccuracy by remember { mutableStateOf<Int?>(null) }
  var activeListeningAnswer by remember { mutableStateOf<Int?>(null) }

  LaunchedEffect(spokenText) {
    if (spokenText.isNotBlank()) {
      val spoken = spokenText
      speechManager.clearSpokenText()
      val activeStage = journey.stages.firstOrNull { it.stageNumber == expandedStageNum }
      val target = activeStage?.speakingChallenge?.suggestedResponseInTarget ?: ""
      val targetWords = target.lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), "").split(" ").filter { it.isNotBlank() }
      val userWords = spoken.lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), "").split(" ").filter { it.isNotBlank() }
      val hits = targetWords.count { userWords.contains(it) }
      val score = if (targetWords.isEmpty()) 90 else ((hits.toFloat() / targetWords.size) * 100).toInt().coerceIn(60, 100)
      activeSpeakingAccuracy = score
      Toast.makeText(context, "Spoken: \"$spoken\" • Accuracy: $score%", Toast.LENGTH_SHORT).show()
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    Text(
      text = "${journey.flagEmoji} ${journey.countryName} • 10 Immersion Stages",
      color = Color.White,
      fontSize = 18.sp,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = "Progress from foundational greetings to cultural mastery. Complete each stage to unlock the next!",
      color = Color(0xFF93C5FD),
      fontSize = 12.5.sp
    )
    Spacer(modifier = Modifier.height(14.dp))

    journey.stages.forEach { stage ->
      val isCompleted = userProfile.achievements.contains("STAGE_${journey.countryCode}_${stage.stageNumber}")
      val isPreviousCompleted = stage.stageNumber == 1 ||
        userProfile.achievements.contains("STAGE_${journey.countryCode}_${stage.stageNumber - 1}")
      val isUnlocked = isCompleted || isPreviousCompleted
      val isExpanded = expandedStageNum == stage.stageNumber

      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (isExpanded) Color(0xFF0F2654) else Color(0xFF0A1938)
        ),
        border = BorderStroke(
          if (isExpanded) 1.5.dp else 1.dp,
          when {
            isCompleted -> LingoEmerald
            isExpanded -> LingoCyan
            isUnlocked -> Color(0xFF22437F)
            else -> Color(0xFF132347)
          }
        ),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 5.dp)
          .clickable(enabled = isUnlocked) {
            expandedStageNum = if (isExpanded) 0 else stage.stageNumber
            activeSpeakingAccuracy = null
            activeListeningAnswer = null
          }
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(32.dp)
                  .clip(CircleShape)
                  .background(
                    when {
                      isCompleted -> LingoEmerald.copy(alpha = 0.2f)
                      isUnlocked -> LingoCyan.copy(alpha = 0.2f)
                      else -> Color(0xFF1B2F55)
                    }
                  )
              ) {
                if (isCompleted) {
                  Icon(Icons.Default.CheckCircle, contentDescription = "Completed", tint = LingoEmerald, modifier = Modifier.size(20.dp))
                } else if (!isUnlocked) {
                  Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(16.dp))
                } else {
                  Text("${stage.stageNumber}", color = LingoCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = stage.title,
                  color = if (isUnlocked) Color.White else Color.Gray,
                  fontSize = 14.5.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = stage.subtitle,
                  color = if (isUnlocked) Color(0xFF93C5FD) else Color(0xFF64748B),
                  fontSize = 11.5.sp
                )
              }
            }

            // Status Pill
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                  when {
                    isCompleted -> LingoEmerald.copy(alpha = 0.2f)
                    isUnlocked -> LingoCyan.copy(alpha = 0.15f)
                    else -> Color(0xFF14244A)
                  }
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(
                text = when {
                  isCompleted -> "✓ Complete"
                  isUnlocked -> "In Progress"
                  else -> "Locked"
                },
                color = when {
                  isCompleted -> LingoEmerald
                  isUnlocked -> LingoCyan
                  else -> Color.Gray
                },
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          // Expanded Stage Details
          AnimatedVisibility(visible = isExpanded && isUnlocked) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
              Text(
                text = "CULTURAL CONTEXT",
                color = LingoGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = stage.culturalContext,
                color = Color(0xFFCBD5E1),
                fontSize = 12.5.sp,
                lineHeight = 18.sp
              )
              Spacer(modifier = Modifier.height(10.dp))

              // Key Phrases
              Text("KEY PHRASES & AUDIO", color = LingoCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              stage.keyPhrases.forEach { card ->
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF091736))
                    .padding(8.dp)
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(card.phrase, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    Text("${card.phonetic} — ${card.meaning}", color = Color(0xFF93C5FD), fontSize = 11.sp)
                    Text(card.culturalNote, color = Color(0xFFCBD5E1), fontSize = 10.5.sp)
                  }
                  Row {
                    IconButton(onClick = { speechManager.speak(card.phrase, targetLang.code) }) {
                      Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play Native Audio", tint = LingoCyan, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { onSaveVocab(card) }) {
                      Icon(Icons.Default.BookmarkAdd, contentDescription = "Save Flashcard", tint = LingoGold, modifier = Modifier.size(18.dp))
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Speaking Challenge
              Text("SPEAKING CHALLENGE (MIC)", color = LingoCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(Color(0xFF091736))
                  .padding(10.dp)
              ) {
                Text(stage.speakingChallenge.situationTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                Text("Scenario: ${stage.speakingChallenge.scenarioSetting}", color = Color(0xFF93C5FD), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Say: \"${stage.speakingChallenge.suggestedResponseInTarget}\"", color = LingoCoral, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("${stage.speakingChallenge.suggestedPhonetic} (${stage.speakingChallenge.suggestedMeaning})", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                  OutlinedButton(
                    onClick = {
                      if (isListening) speechManager.stopListening()
                      else if (hasMicPermission) speechManager.startListening(targetLang.code)
                      else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isListening) LingoCoral else LingoCyan),
                    modifier = Modifier.weight(1f)
                  ) {
                    Icon(if (isListening) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null, tint = if (isListening) LingoCoral else LingoCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isListening) "Listening..." else "Record Response", color = if (isListening) LingoCoral else LingoCyan, fontSize = 11.5.sp)
                  }

                  if (activeSpeakingAccuracy != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Accuracy: $activeSpeakingAccuracy%",
                      color = if (activeSpeakingAccuracy!! >= 80) LingoEmerald else LingoGold,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Listening Comprehension
              Text("LISTENING COMPREHENSION", color = LingoEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(Color(0xFF091736))
                  .padding(10.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(stage.listeningExercise.settingTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                  IconButton(onClick = { speechManager.speak(stage.listeningExercise.audioScriptInTarget, targetLang.code) }) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play Audio", tint = LingoEmerald, modifier = Modifier.size(18.dp))
                  }
                }
                Text("Question: ${stage.listeningExercise.question}", color = Color(0xFF93C5FD), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                stage.listeningExercise.options.forEachIndexed { optIdx, optText ->
                  val isSelected = activeListeningAnswer == optIdx
                  val isCorrect = optIdx == stage.listeningExercise.correctIndex
                  Card(
                    colors = CardDefaults.cardColors(
                      containerColor = when {
                        isSelected && isCorrect -> LingoEmerald.copy(alpha = 0.2f)
                        isSelected && !isCorrect -> LingoCoral.copy(alpha = 0.2f)
                        else -> Color(0xFF071229)
                      }
                    ),
                    border = BorderStroke(1.dp, if (isSelected) LingoCyan else Color(0xFF142B59)),
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 2.dp)
                      .clickable { activeListeningAnswer = optIdx }
                  ) {
                    Text(
                      text = optText,
                      color = Color.White,
                      fontSize = 11.5.sp,
                      modifier = Modifier.padding(8.dp)
                    )
                  }
                }
                if (activeListeningAnswer != null) {
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = "💡 Insight: ${stage.listeningExercise.culturalExplanation}",
                    color = LingoGold,
                    fontSize = 11.sp
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))
              Text("💡 Cultural Tip: ${stage.culturalInsightTip}", color = LingoGold, fontSize = 11.5.sp)

              Spacer(modifier = Modifier.height(12.dp))

              // Complete Stage Button
              Button(
                onClick = {
                  val stageKey = "STAGE_${journey.countryCode}_${stage.stageNumber}"
                  onUnlockAchievementAndXp(
                    stageKey,
                    "${stage.title} Mastered! 🎓",
                    "You mastered ${stage.title} in ${journey.countryName} (+${stage.xpReward} XP)",
                    stage.xpReward
                  )
                  if (stage.stageNumber == 10) {
                    onUnlockAchievementAndXp(
                      journey.passportStampCode,
                      "Passport Visa Stamp Earned! 🛂",
                      "You mastered the entire cultural journey for ${journey.countryName} (+150 XP)!",
                      150
                    )
                  }
                  Toast.makeText(context, "${stage.title} Completed! +${stage.xpReward} XP", Toast.LENGTH_SHORT).show()
                  if (stage.stageNumber < 10) {
                    expandedStageNum = stage.stageNumber + 1
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isCompleted) LingoEmerald else LingoElectricBlue
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = if (isCompleted) "✓ Stage Completed (+${stage.xpReward} XP Earned)" else "Complete Stage (+${stage.xpReward} XP)",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}

// ============================================================================
// 3. PASSPORT & MISSIONS SUB-SCREEN
// ============================================================================
@Composable
private fun PassportMissionsSubScreen(
  currentJourney: CountryCultureJourney,
  allJourneys: List<CountryCultureJourney>,
  userProfile: UserProfile,
  onSelectCountry: (CountryCultureJourney) -> Unit,
  onNavigateToFeature: (CultureSubScreen) -> Unit,
  onUnlockAchievementAndXp: (String, String, String, Int) -> Unit
) {
  val context = LocalContext.current
  val stampCodes = userProfile.achievements.filter { it.startsWith("PASSPORT_") }
  val rank = when {
    stampCodes.size >= 5 -> "Global Cultural Ambassador 🌐"
    stampCodes.size >= 3 -> "International Diplomat 🎖️"
    stampCodes.size >= 1 -> "Cultural Explorer 🧭"
    else -> "Aspiring Traveler 🛫"
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    // Digital Passport Card
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1F48)),
      border = BorderStroke(1.5.dp, LingoGold),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column {
            Text("🛂 LINGOSPHERE DIGITAL PASSPORT", color = LingoGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(userProfile.displayName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(rank, color = LingoCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(LingoGold.copy(alpha = 0.2f))
              .border(1.dp, LingoGold, RoundedCornerShape(12.dp))
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text("${stampCodes.size} Visa Stamps", color = LingoGold, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text("EARNED COUNTRY VISA STAMPS", color = Color(0xFF93C5FD), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // Visa Stamp Grid
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          items(allJourneys) { journey ->
            val hasStamp = userProfile.achievements.contains(journey.passportStampCode)
            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (hasStamp) Color(0xFF143063) else Color(0xFF08152F)
              ),
              border = BorderStroke(1.2.dp, if (hasStamp) LingoGold else Color(0xFF1C386E)),
              modifier = Modifier
                .width(105.dp)
                .clickable { onSelectCountry(journey) }
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(10.dp)
              ) {
                Text(journey.flagEmoji, fontSize = 28.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = journey.countryName,
                  color = Color.White,
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = if (hasStamp) "STAMPED ✓" else "LOCKED",
                  color = if (hasStamp) LingoGold else Color.Gray,
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.ExtraBold
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Country Missions
    Text(
      text = "${currentJourney.flagEmoji} ${currentJourney.countryName} Missions",
      color = Color.White,
      fontSize = 17.sp,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = "Complete real cultural missions to earn XP and unlock passport stamps.",
      color = Color(0xFF93C5FD),
      fontSize = 12.sp
    )
    Spacer(modifier = Modifier.height(10.dp))

    currentJourney.missions.forEach { mission ->
      val isCompleted = userProfile.achievements.contains(mission.id)
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1C3D)),
        border = BorderStroke(1.dp, if (isCompleted) LingoEmerald else Color(0xFF1E3A75)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(14.dp)
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = mission.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
              Spacer(modifier = Modifier.width(8.dp))
              // FIX BROKEN VERTICAL XP WRAPPING: maxLines=1, softWrap=false, neatly formatted
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (isCompleted) LingoEmerald.copy(alpha = 0.2f) else LingoCyan.copy(alpha = 0.2f))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "+${mission.xpReward} XP",
                  color = if (isCompleted) LingoEmerald else LingoCyan,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.ExtraBold,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = mission.description,
              color = Color(0xFFCBD5E1),
              fontSize = 11.5.sp
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Button(
            onClick = {
              if (isCompleted) {
                Toast.makeText(context, "Mission already completed! ✓", Toast.LENGTH_SHORT).show()
              } else {
                when (mission.missionType) {
                  "QUIZ" -> onNavigateToFeature(CultureSubScreen.QUIZ_BATTLE)
                  "SPEAKING" -> onNavigateToFeature(CultureSubScreen.COUNTRY_JOURNEY)
                  else -> {
                    onUnlockAchievementAndXp(
                      mission.id,
                      "Cultural Mission Complete! 🌍",
                      "You completed '${mission.title}' and earned +${mission.xpReward} XP!",
                      mission.xpReward
                    )
                    Toast.makeText(context, "Mission '${mission.title}' Complete! +${mission.xpReward} XP", Toast.LENGTH_SHORT).show()
                  }
                }
              }
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isCompleted) LingoEmerald else LingoElectricBlue
            ),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text(
              text = if (isCompleted) "✓ Done" else "Start",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}

// ============================================================================
// 4. VIRTUAL TRAVEL & DIALECTS SUB-SCREEN
// ============================================================================
@Composable
private fun VirtualTravelSubScreen(
  journey: CountryCultureJourney,
  targetLang: Language,
  nativeLang: Language,
  userProfile: UserProfile,
  speechManager: SpeechManager,
  onSaveSpeakingSession: (SpeakingSession) -> Unit,
  onUnlockAchievementAndXp: (String, String, String, Int) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var activeDestination by remember(journey.countryCode) { mutableStateOf<VirtualTravelDestination?>(null) }
  val chatHistory = remember { mutableStateListOf<Pair<String, String>>() }
  var userReplyInput by remember { mutableStateOf("") }
  var isAiResponding by remember { mutableStateOf(false) }

  val isListening by speechManager.isListening.collectAsState()
  val spokenText by speechManager.spokenText.collectAsState()
  var hasMicPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }
  val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    hasMicPermission = granted
    if (granted) speechManager.startListening(targetLang.code)
  }

  LaunchedEffect(spokenText) {
    if (spokenText.isNotBlank() && activeDestination != null) {
      userReplyInput = spokenText
      speechManager.clearSpokenText()
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    Text(
      text = "✈️ Virtual Travel & Local Guides • ${journey.countryName}",
      color = Color.White,
      fontSize = 18.sp,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = "Step inside real destinations and converse with local personas in real time.",
      color = Color(0xFF93C5FD),
      fontSize = 12.sp
    )
    Spacer(modifier = Modifier.height(14.dp))

    // Destinations
    journey.virtualLandmarks.forEach { landmark ->
      val isOpen = activeDestination?.id == landmark.id
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1C3D)),
        border = BorderStroke(1.2.dp, if (isOpen) LingoEmerald else LingoCyan.copy(alpha = 0.5f)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 5.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = landmark.badgeEmoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = landmark.landmarkName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
              )
              Text(
                text = "${landmark.city} • Local Host: ${landmark.localPersonaName}",
                color = LingoCyan,
                fontSize = 12.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = landmark.sceneDescription,
            color = Color(0xFFCBD5E1),
            fontSize = 12.5.sp,
            lineHeight = 18.sp
          )

          Spacer(modifier = Modifier.height(10.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF071229))
              .padding(10.dp)
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Local Street Phrase:", color = LingoGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Text(landmark.streetPhraseInTarget, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Text("${landmark.streetPhrasePhonetic} — ${landmark.streetPhraseMeaning}", color = Color(0xFF93C5FD), fontSize = 11.sp)
            }
            IconButton(onClick = { speechManager.speak(landmark.streetPhraseInTarget, targetLang.code) }) {
              Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play Audio", tint = LingoCyan)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Button(
            onClick = {
              if (isOpen) {
                activeDestination = null
              } else {
                activeDestination = landmark
                chatHistory.clear()
                chatHistory.add("GUIDE" to landmark.openingGreetingInTarget)
                speechManager.speak(landmark.openingGreetingInTarget, targetLang.code)
              }
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isOpen) LingoCoral else LingoEmerald,
              contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = if (isOpen) "Leave Simulation" else "Talk to Local Guide (${landmark.localPersonaName}) 🎙️",
              fontWeight = FontWeight.ExtraBold,
              fontSize = 13.sp
            )
          }

          // Active Guide Chat Simulator
          AnimatedVisibility(visible = isOpen) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
              Text("CONVERSATION WITH LOCAL GUIDE", color = LingoCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(6.dp))

              chatHistory.forEach { (sender, msg) ->
                val isMe = sender == "USER"
                Row(
                  horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(12.dp))
                      .background(if (isMe) LingoElectricBlue else Color(0xFF142C59))
                      .padding(horizontal = 12.dp, vertical = 8.dp)
                  ) {
                    Text(text = msg, color = Color.White, fontSize = 13.sp)
                  }
                }
              }

              Spacer(modifier = Modifier.height(8.dp))
              OutlinedTextField(
                value = userReplyInput,
                onValueChange = { userReplyInput = it },
                placeholder = { Text("Speak or type in ${targetLang.name}...", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth()
              )

              Spacer(modifier = Modifier.height(8.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                  onClick = {
                    if (isListening) speechManager.stopListening()
                    else if (hasMicPermission) speechManager.startListening(targetLang.code)
                    else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                  },
                  border = BorderStroke(1.dp, if (isListening) LingoCoral else LingoCyan),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(if (isListening) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null, tint = if (isListening) LingoCoral else LingoCyan, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(if (isListening) "Listening" else "Mic Reply", color = if (isListening) LingoCoral else LingoCyan, fontSize = 11.5.sp)
                }

                Button(
                  enabled = userReplyInput.isNotBlank() && !isAiResponding,
                  onClick = {
                    val input = userReplyInput.trim()
                    userReplyInput = ""
                    chatHistory.add("USER" to input)
                    isAiResponding = true
                    coroutineScope.launch {
                      val prompt = """
                        You are ${landmark.localPersonaName}, a friendly ${landmark.localPersonaRole} in ${landmark.city}, ${journey.countryName}.
                        The learner says: "$input"
                        Reply naturally in ${targetLang.name} (1-2 sentences), keeping it warm and cultural.
                      """.trimIndent()
                      val reply = GeminiTutorService.generateGeminiText(
                        systemInstructionText = null,
                        userPromptText = prompt,
                        historyTurns = emptyList(),
                        temperature = 0.6f
                      ) ?: "Welcome to ${landmark.city}! It is wonderful to meet you."
                      chatHistory.add("GUIDE" to reply)
                      isAiResponding = false
                      speechManager.speak(reply, targetLang.code)
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = LingoElectricBlue),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Text(if (isAiResponding) "Thinking..." else "Send Reply", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Regional Dialect Differences
    Text("Regional Dialect Comparisons", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))

    journey.regionalDialects.forEach { dialect ->
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF091738)),
        border = BorderStroke(1.dp, Color(0xFF1E3A75)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(dialect.conceptTitle, color = LingoCyan, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
          Spacer(modifier = Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
              Text("${dialect.regionAFlag} ${dialect.regionAName}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Text(dialect.regionAPhrase, color = Color(0xFF93C5FD), fontSize = 11.5.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
              Text("${dialect.regionBFlag} ${dialect.regionBName}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Text(dialect.regionBPhrase, color = Color(0xFF93C5FD), fontSize = 11.5.sp)
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text("💡 Cultural Nuance: ${dialect.culturalNuance}", color = Color(0xFFCBD5E1), fontSize = 11.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}

// ============================================================================
// 5. CULTURAL ETIQUETTE TRAINER SUB-SCREEN
// ============================================================================
@Composable
private fun EtiquetteTrainerSubScreen(
  journey: CountryCultureJourney,
  targetLang: Language,
  nativeLang: Language,
  speechManager: SpeechManager,
  onUnlockAchievementAndXp: (String, String, String, Int) -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  var advisorQuery by remember { mutableStateOf("") }
  var advisorResponse by remember { mutableStateOf<CulturalAdvisorResponse?>(null) }
  var isConsulting by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    Text(
      text = "🤝 Cultural Etiquette Trainer • ${journey.countryName}",
      color = Color.White,
      fontSize = 18.sp,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = "Master unspoken social rules, dining customs, and respectful communication.",
      color = Color(0xFF93C5FD),
      fontSize = 12.sp
    )
    Spacer(modifier = Modifier.height(14.dp))

    // Categories
    journey.etiquetteRules.forEach { rule ->
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1C3D)),
        border = BorderStroke(1.dp, Color(0xFF1E3A75)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "${rule.iconEmoji} ${rule.category}",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(text = "✅ DO: ${rule.doRule}", color = LingoEmerald, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
          Spacer(modifier = Modifier.height(3.dp))
          Text(text = "❌ AVOID: ${rule.dontRule}", color = LingoCoral, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
          Spacer(modifier = Modifier.height(6.dp))
          Text(text = "💡 Why it matters: ${rule.culturalWhy}", color = Color(0xFFCBD5E1), fontSize = 11.5.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Cultural Etiquette Advisor Q&A
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1D45)),
      border = BorderStroke(1.2.dp, LingoCyan),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("✨ Cultural Advisor Guidance", color = LingoCyan, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Text("Attending a dinner, meeting, or home visit? Ask what to say and what etiquette to follow.", color = Color(0xFFCBD5E1), fontSize = 11.5.sp)
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = advisorQuery,
          onValueChange = { advisorQuery = it },
          placeholder = { Text("e.g. What gift should I bring when invited to a home?", fontSize = 12.sp) },
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        Button(
          enabled = advisorQuery.isNotBlank() && !isConsulting,
          onClick = {
            isConsulting = true
            coroutineScope.launch {
              advisorResponse = GeminiTutorService.askCulturalEtiquetteAdvisor(
                questionOrScenario = advisorQuery,
                countryName = journey.countryName,
                targetLanguage = targetLang,
                nativeLanguage = nativeLang
              )
              isConsulting = false
              onUnlockAchievementAndXp(
                "ETIQUETTE_${journey.countryCode}",
                "Cultural Etiquette Diplomat! 🤝",
                "You consulted the ${journey.countryName} Etiquette Advisor (+35 XP).",
                35
              )
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = LingoElectricBlue),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          if (isConsulting) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Consulting Cultural Guide...", fontSize = 12.sp)
          } else {
            Text("Get Cultural Guidance & Native Phrase", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
          }
        }

        if (advisorResponse != null) {
          val adv = advisorResponse!!
          Spacer(modifier = Modifier.height(12.dp))
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF071229))
              .padding(12.dp)
          ) {
            Text("🧭 Practical Advice:", color = LingoGold, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            Text(adv.etiquetteAdvice, color = Color.White, fontSize = 12.5.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("Polite Phrase to Speak:", color = LingoCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(adv.essentialPhraseInTarget, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("${adv.phonetics} — ${adv.phraseTranslation}", color = Color(0xFF93C5FD), fontSize = 11.5.sp)
              }
              IconButton(onClick = { speechManager.speak(adv.essentialPhraseInTarget, targetLang.code) }) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Hear Phrase", tint = LingoCyan)
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("⚠️ Taboo to Avoid: ${adv.tabooWarning}", color = LingoCoral, fontSize = 11.5.sp)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}

// ============================================================================
// 6. VOICE DIARY SUB-SCREEN (RECORDING, PLAYBACK, TIMELINE)
// ============================================================================
@Composable
private fun VoiceDiarySubScreen(
  targetLang: Language,
  nativeLang: Language,
  userProfile: UserProfile,
  speakingSessions: List<SpeakingSession>,
  speechManager: SpeechManager,
  onSaveSpeakingSession: (SpeakingSession) -> Unit,
  onUnlockAchievementAndXp: (String, String, String, Int) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  val prompts = remember(targetLang.name) {
    listOf(
      "Daily Reflection: What was the most meaningful part of your day?",
      "Food & Culture: Describe a meal, beverage, or snack you enjoyed recently.",
      "Travel Dream: Which city where ${targetLang.name} is spoken would you love to explore?",
      "Milestones: What inspired you to learn ${targetLang.name} today?"
    )
  }
  var selectedPromptIdx by remember { mutableIntStateOf(0) }
  var diaryInputText by remember { mutableStateOf("") }
  var evaluation by remember { mutableStateOf<VoiceDiaryAiEvaluation?>(null) }
  var isEvaluating by remember { mutableStateOf(false) }

  // Microphone recording timer
  var recordSeconds by remember { mutableIntStateOf(0) }
  val isListening by speechManager.isListening.collectAsState()
  val spokenText by speechManager.spokenText.collectAsState()

  LaunchedEffect(isListening) {
    if (isListening) {
      recordSeconds = 0
      while (isListening) {
        delay(1000)
        recordSeconds++
      }
    }
  }

  var hasMicPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }
  val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    hasMicPermission = granted
    if (granted) speechManager.startListening(targetLang.code)
  }

  LaunchedEffect(spokenText) {
    if (spokenText.isNotBlank()) {
      diaryInputText = if (diaryInputText.isBlank()) spokenText else "$diaryInputText $spokenText"
      speechManager.clearSpokenText()
    }
  }

  // Persistent timeline with edit and delete
  val localDiaryTimeline = remember {
    mutableStateListOf<Pair<String, String>>()
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    Text(
      text = "🎙️ Daily Spoken Voice Diary (${targetLang.flagEmoji} ${targetLang.name})",
      color = Color.White,
      fontSize = 18.sp,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = "Record or write your reflections. The Speech Coach analyzes your phrasing and saves your timeline.",
      color = Color(0xFF93C5FD),
      fontSize = 12.sp
    )
    Spacer(modifier = Modifier.height(14.dp))

    // Prompt Card
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1C3D)),
      border = BorderStroke(1.2.dp, LingoCyan),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("TODAY'S JOURNAL PROMPT", color = LingoCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Text(
            text = "Next Prompt →",
            color = LingoGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable {
              selectedPromptIdx = (selectedPromptIdx + 1) % prompts.size
            }
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = prompts[selectedPromptIdx],
          color = Color.White,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = diaryInputText,
          onValueChange = { diaryInputText = it },
          placeholder = { Text("Tap the mic to record or type your journal entry here...", fontSize = 12.sp) },
          minLines = 3,
          modifier = Modifier.fillMaxWidth()
        )

        // Recording Timer Indicator
        if (isListening) {
          Spacer(modifier = Modifier.height(8.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(LingoCoral)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Recording Live: ${recordSeconds}s", color = LingoCoral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedButton(
            onClick = {
              if (isListening) speechManager.stopListening()
              else if (hasMicPermission) speechManager.startListening(targetLang.code)
              else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            border = BorderStroke(1.dp, if (isListening) LingoCoral else LingoCyan),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              if (isListening) Icons.Default.Stop else Icons.Default.Mic,
              contentDescription = null,
              tint = if (isListening) LingoCoral else LingoCyan,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isListening) "Stop Mic" else "Record Mic",
              color = if (isListening) LingoCoral else LingoCyan,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Button(
            enabled = diaryInputText.isNotBlank() && !isEvaluating,
            onClick = {
              val entry = diaryInputText.trim()
              isEvaluating = true
              coroutineScope.launch {
                val result = GeminiTutorService.evaluateVoiceDiaryEntry(
                  diaryEntryText = entry,
                  targetLanguage = targetLang,
                  nativeLanguage = nativeLang,
                  proficiencyLevel = userProfile.proficiencyLevel,
                  promptTopic = prompts[selectedPromptIdx]
                )
                evaluation = result
                isEvaluating = false
                localDiaryTimeline.add(0, entry to "Score: ${result.fluencyScore}/100 • ${result.correctedText}")
                onSaveSpeakingSession(
                  SpeakingSession(
                    userId = userProfile.userId,
                    languageCode = targetLang.code,
                    mode = "Tutor",
                    scenarioTitle = "Voice Diary: ${targetLang.name}",
                    proficiencyLevel = userProfile.proficiencyLevel,
                    turnsCount = 1L,
                    fluencyScore = result.fluencyScore.toLong(),
                    grammarFeedbackSummary = "Polished: \"${result.correctedText}\" • Note: ${result.grammarExplanation}"
                  )
                )
                onUnlockAchievementAndXp(
                  "VOICE_DIARY_AUTHOR",
                  "Voice Diary Entry Saved! 🎙️",
                  "You recorded a ${targetLang.name} Voice Diary entry (+60 XP).",
                  60
                )
                Toast.makeText(context, "Voice Diary Evaluated & Saved! +60 XP", Toast.LENGTH_SHORT).show()
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = LingoEmerald, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text(
              text = if (isEvaluating) "Evaluating..." else "Analyze & Save",
              color = Color.Black,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 12.sp
            )
          }
        }
      }
    }

    // Evaluation Results
    if (evaluation != null) {
      val res = evaluation!!
      Spacer(modifier = Modifier.height(14.dp))
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2448)),
        border = BorderStroke(1.2.dp, LingoEmerald),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("📊 SPEECH COACH FEEDBACK", color = LingoEmerald, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold)
            Text("Fluency: ${res.fluencyScore}/100", color = LingoGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text("Refined Version:", color = LingoCyan, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
          Text(res.correctedText, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { speechManager.speak(res.correctedText, targetLang.code) }) {
              Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play Pronunciation", tint = LingoCyan)
            }
            Text("Listen to refined pronunciation", color = Color(0xFF93C5FD), fontSize = 11.sp)
          }

          Spacer(modifier = Modifier.height(4.dp))
          Text("💡 Grammar Note: ${res.grammarExplanation}", color = Color(0xFFCBD5E1), fontSize = 12.sp)
          Spacer(modifier = Modifier.height(4.dp))
          Text("🌟 Natural Idiom: ${res.naturalIdiomSuggestion}", color = LingoGold, fontSize = 12.sp)
        }
      }
    }

    // Persistent Timeline with Edit and Delete
    Spacer(modifier = Modifier.height(18.dp))
    Text("Voice Diary History & Timeline", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))

    if (localDiaryTimeline.isEmpty()) {
      Text(
        text = "No saved voice diary entries yet. Record your first reflection above!",
        color = Color(0xFF94A3B8),
        fontSize = 12.sp
      )
    } else {
      localDiaryTimeline.forEachIndexed { idx, (original, note) ->
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF091738)),
          border = BorderStroke(1.dp, Color(0xFF1E3A75)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("\"$original\"", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              Text(note, color = Color(0xFF93C5FD), fontSize = 11.sp)
            }
            IconButton(onClick = { diaryInputText = original }) {
              Icon(Icons.Default.Edit, contentDescription = "Edit Entry", tint = LingoCyan, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { localDiaryTimeline.removeAt(idx) }) {
              Icon(Icons.Default.Delete, contentDescription = "Delete Entry", tint = LingoCoral, modifier = Modifier.size(18.dp))
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}

// ============================================================================
// 7. CULTURE QUIZ BATTLE SUB-SCREEN
// ============================================================================
@Composable
private fun CultureQuizBattleSubScreen(
  journey: CountryCultureJourney,
  userProfile: UserProfile,
  onUnlockAchievementAndXp: (String, String, String, Int) -> Unit
) {
  val questions = journey.quizBattleQuestions
  var currentIndex by remember(journey.countryCode) { mutableIntStateOf(0) }
  var selectedOption by remember(journey.countryCode, currentIndex) { mutableStateOf<Int?>(null) }
  var scoreCount by remember(journey.countryCode) { mutableIntStateOf(0) }
  var battleFinished by remember(journey.countryCode) { mutableStateOf(false) }

  val q = questions.getOrNull(currentIndex)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    Text(
      text = "⚔️ Culture Quiz Battle • ${journey.flagEmoji} ${journey.countryName}",
      color = Color.White,
      fontSize = 18.sp,
      fontWeight = FontWeight.ExtraBold
    )
    Text(
      text = "Test your cultural intelligence, idioms, and etiquette mastery to earn your Country Visa Stamp!",
      color = Color(0xFF93C5FD),
      fontSize = 12.sp
    )
    Spacer(modifier = Modifier.height(14.dp))

    if (battleFinished || q == null) {
      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1D45)),
        border = BorderStroke(1.5.dp, LingoGold),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(22.dp)
        ) {
          Text("🏆", fontSize = 44.sp)
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "${journey.countryName} Quiz Battle Complete!",
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold
          )
          Text(
            text = "Score: $scoreCount / ${questions.size} • +100 XP & Visa Stamp Unlocked",
            color = LingoGold,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(14.dp))
          Button(
            onClick = {
              currentIndex = 0
              scoreCount = 0
              selectedOption = null
              battleFinished = false
            },
            colors = ButtonDefaults.buttonColors(containerColor = LingoElectricBlue),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Play Battle Again", fontWeight = FontWeight.Bold)
          }
        }
      }
    } else {
      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1C3D)),
        border = BorderStroke(1.2.dp, LingoCyan),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "ROUND ${currentIndex + 1} OF ${questions.size}",
              color = LingoCyan,
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Score: $scoreCount",
              color = LingoGold,
              fontSize = 12.sp,
              fontWeight = FontWeight.ExtraBold
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = q.question,
            color = Color.White,
            fontSize = 16.5.sp,
            fontWeight = FontWeight.ExtraBold
          )
          Spacer(modifier = Modifier.height(12.dp))

          q.options.forEachIndexed { idx, opt ->
            val isSelected = selectedOption == idx
            val isCorrect = idx == q.correctIndex
            val cardBg = when {
              selectedOption != null && isCorrect -> LingoEmerald.copy(alpha = 0.25f)
              selectedOption != null && isSelected && !isCorrect -> LingoCoral.copy(alpha = 0.25f)
              else -> Color(0xFF071229)
            }
            val cardBorder = when {
              selectedOption != null && isCorrect -> LingoEmerald
              selectedOption != null && isSelected && !isCorrect -> LingoCoral
              else -> Color(0xFF142B59)
            }

            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = cardBg),
              border = BorderStroke(1.dp, cardBorder),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable(enabled = selectedOption == null) {
                  selectedOption = idx
                  if (idx == q.correctIndex) {
                    scoreCount++
                  }
                }
            ) {
              Text(
                text = opt,
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.padding(12.dp)
              )
            }
          }

          if (selectedOption != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "💡 Explanation: ${q.explanation}",
              color = LingoGold,
              fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Button(
              onClick = {
                if (currentIndex + 1 < questions.size) {
                  currentIndex++
                  selectedOption = null
                } else {
                  battleFinished = true
                  onUnlockAchievementAndXp(
                    journey.passportStampCode,
                    "Country Visa Stamp Earned! 🛂",
                    "You conquered the ${journey.countryName} Quiz Battle (+100 XP)!",
                    100
                  )
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = LingoElectricBlue),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = if (currentIndex + 1 < questions.size) "Next Round →" else "Finish Battle & Claim Stamp 🏆",
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}
