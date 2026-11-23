package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.AiErrorHandler
import com.example.ai.AiUiError
import com.example.ai.PracticalFeaturesAiService
import com.example.ai.UpgradeSheetMode
import com.example.data.model.Language
import com.example.data.model.LingoSphere3Catalog
import com.example.data.model.PortfolioSpeakingSample
import com.example.data.repository.AccessPolicyFeatureKeys
import com.example.speech.SpeechManager
import com.example.ui.LingoViewModel
import com.example.ui.components.LingoCalmAiErrorBanner
import com.example.ui.components.LingoCleanCard
import com.example.ui.components.LingoEmptyOrErrorCard
import com.example.ui.components.LingoGoldLockIcon
import com.example.ui.components.LingoSectionHeader
import com.example.ui.components.LingoStatusBadge
import com.example.ui.components.LingoTokens
import com.example.ui.components.LingoTopBar
import kotlinx.coroutines.launch

@Composable
private fun portfolioChipColors() = FilterChipDefaults.filterChipColors(
  containerColor = LingoTokens.bgElevated,
  labelColor = LingoTokens.textSecondary,
  selectedContainerColor = LingoTokens.accentPrimary.copy(alpha = 0.16f),
  selectedLabelColor = LingoTokens.accentPrimary
)

/**
 * Capability 8: Accent Understanding Studio
 */
@Composable
fun AccentUnderstandingStudioScreen(
  viewModel: LingoViewModel,
  speechManager: SpeechManager,
  onBack: () -> Unit
) {
  BackHandler { onBack() }
  val userProfile by viewModel.userProfileState.collectAsState()
  val targetLang = Language.findByCode(userProfile.targetLanguageCode)

  val context = LocalContext.current
  val repeatedMistakes by viewModel.repeatedMistakesState.collectAsState()
  val isListening by speechManager.isListening.collectAsState()
  val spokenText by speechManager.spokenText.collectAsState()

  var selectedLanguageName by remember(targetLang.name) { mutableStateOf(targetLang.name) }
  var selectedAccentRegion by remember { mutableStateOf("US") }
  val accentRegionOptions = remember { listOf("US", "UK", "Australian", "Indian", "Canadian") }
  val samples = remember(selectedLanguageName) {
    LingoSphere3Catalog.regionalAccentsForLanguage(selectedLanguageName)
  }
  var playbackRate by remember { mutableFloatStateOf(1.0f) }
  var selectedOptionBySample by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
  var activeShadowSampleId by remember(samples) { mutableStateOf(samples.firstOrNull()?.id.orEmpty()) }
  var shadowAttemptText by remember(activeShadowSampleId) { mutableStateOf("") }
  var shadowEstimateScore by remember(activeShadowSampleId) { mutableStateOf<Int?>(null) }
  var selectedMinimalPairIdx by remember { mutableStateOf(0) }

  LaunchedEffect(spokenText) {
    if (spokenText.isNotBlank()) {
      shadowAttemptText = spokenText
    }
  }

  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      speechManager.startListening(targetLang.code)
    }
  }

  val languageTabs = remember(targetLang.name) {
    listOf(targetLang.name, "Spanish", "French", "English", "Japanese").distinct()
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(LingoTokens.bgCanvas)
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
  ) {
    LingoTopBar(
      title = "Accent & Listening Studio",
      subtitle = "Regional accents, playback speed & local expressions",
      onBack = onBack
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(vertical = 16.dp)
    ) {
      item {
        LingoCleanCard(
          containerColor = LingoTokens.accentBlue.copy(alpha = 0.1f),
          borderColor = LingoTokens.accentPrimary.copy(alpha = 0.3f),
          contentPadding = 14.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "Linguistic Diversity Principle",
              fontWeight = FontWeight.Bold,
              fontSize = 13.5.sp,
              color = LingoTokens.accentPrimary
            )
            Text(
              text = "Every regional variety reflects living cultural heritage. Building ear fluency across accents helps you understand real conversations everywhere.",
              fontSize = 12.5.sp,
              lineHeight = 18.sp,
              color = LingoTokens.textSecondary
            )
          }
        }
      }

      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
          ) {
            languageTabs.forEach { langName ->
              val selected = selectedLanguageName.equals(langName, ignoreCase = true)
              FilterChip(
                selected = selected,
                onClick = { selectedLanguageName = langName },
                label = {
                  Text(
                    langName,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                  )
                },
                colors = portfolioChipColors(),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = selected,
                  borderColor = LingoTokens.borderSubtle,
                  selectedBorderColor = LingoTokens.accentPrimary.copy(alpha = 0.6f)
                )
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
          ) {
            Text(
              "Accent:",
              fontWeight = FontWeight.SemiBold,
              fontSize = 12.5.sp,
              color = LingoTokens.textSecondary
            )
            accentRegionOptions.forEach { region ->
              val selected = selectedAccentRegion == region
              FilterChip(
                selected = selected,
                onClick = { selectedAccentRegion = region },
                label = { Text(region, fontSize = 12.sp) },
                colors = portfolioChipColors(),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = selected,
                  borderColor = LingoTokens.borderSubtle,
                  selectedBorderColor = LingoTokens.accentPrimary.copy(alpha = 0.6f)
                ),
                modifier = Modifier.testTag("accent_region_chip_${region.lowercase()}")
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
          ) {
            Text(
              "Speed:",
              fontWeight = FontWeight.SemiBold,
              fontSize = 12.5.sp,
              color = LingoTokens.textSecondary
            )
            listOf(0.5f, 0.75f, 1.0f, 1.25f).forEach { speed ->
              val selected = playbackRate == speed
              FilterChip(
                selected = selected,
                onClick = {
                  playbackRate = speed
                  speechManager.setSpeechRate(speed)
                },
                label = { Text("${speed}x", fontSize = 12.sp) },
                colors = portfolioChipColors(),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = selected,
                  borderColor = LingoTokens.borderSubtle,
                  selectedBorderColor = LingoTokens.accentPrimary.copy(alpha = 0.6f)
                ),
                modifier = Modifier.testTag("accent_speed_chip_${speed}")
              )
            }
          }
        }
      }

      // Shadowing Mode & Minimal-Pair Drills Card
      item {
        val activeShadowSample = samples.firstOrNull { it.id == activeShadowSampleId } ?: samples.firstOrNull()
        val langCode = Language.SUPPORTED_LANGUAGES.firstOrNull {
          it.name.equals(selectedLanguageName, ignoreCase = true)
        }?.code ?: targetLang.code
        val minimalPairs = remember(selectedLanguageName) {
          when {
            selectedLanguageName.equals("Spanish", ignoreCase = true) -> listOf(
              Triple("pero / perro", "single tap /ɾ/ vs trilled /r/", "Pero quiero un perro pequeño."),
              Triple("caro / carro", "expensive vs car (/ɾ/ vs /r/)", "Ese carro no es muy caro."),
              Triple("casa / caza", "/s/ vs /θ/ (Spain) or seseo (LatAm)", "Sale de casa temprano.")
            )
            selectedLanguageName.equals("French", ignoreCase = true) -> listOf(
              Triple("tu / tout", "front rounded /y/ vs back /u/", "Tu as tout compris aujourd'hui."),
              Triple("vin / vent", "nasal /ɛ̃/ vs nasal /ɑ̃/", "Le vent souffle sur les vignes."),
              Triple("dessus / dessous", "above (/y/) vs below (/u/)", "Regarde dessus et dessous.")
            )
            else -> listOf(
              Triple("ship / sheep", "short lax /ɪ/ vs long tense /iː/", "The sheep boarded the big ship."),
              Triple("think / sink", "voiceless dental /θ/ vs alveolar /s/", "Think carefully before you sink."),
              Triple("bet / bat", "mid front /ɛ/ vs open front /æ/", "I bet the bat flew past.")
            )
          }
        }
        val activePair = minimalPairs[selectedMinimalPairIdx.coerceIn(0, minimalPairs.lastIndex)]

        LingoCleanCard(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("accent_shadowing_and_drills_card")
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Shadowing Mode ($selectedAccentRegion • ${playbackRate}x)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = LingoTokens.textPrimary
              )
              if (shadowEstimateScore != null) {
                LingoStatusBadge(
                  text = "Similarity ${shadowEstimateScore}% (estimate)",
                  color = if ((shadowEstimateScore ?: 0) >= 75) LingoTokens.accentEmerald else LingoTokens.accentAmber
                )
              }
            }

            if (activeShadowSample != null) {
              Text(
                text = "1. Listen to \"${activeShadowSample.transcriptInTargetLang}\" at ${playbackRate}x, then repeat aloud.",
                fontSize = 12.5.sp,
                color = LingoTokens.textSecondary
              )
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                androidx.compose.material3.Button(
                  onClick = {
                    speechManager.speakWithRate(
                      text = activeShadowSample.transcriptInTargetLang,
                      languageCode = langCode,
                      speechRate = playbackRate
                    )
                  },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = LingoTokens.accentBlue,
                    contentColor = Color.White
                  ),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("accent_shadow_listen_btn")
                ) {
                  Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Listen (${playbackRate}x)", fontSize = 12.sp)
                }

                androidx.compose.material3.OutlinedButton(
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
                        speechManager.startListening(langCode)
                      } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                      }
                    }
                  },
                  border = BorderStroke(1.dp, LingoTokens.accentPrimary.copy(alpha = 0.6f)),
                  colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isListening) LingoTokens.accentAmber else LingoTokens.accentPrimary
                  ),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("accent_shadow_record_btn")
                ) {
                  Icon(
                    if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(if (isListening) "Stop" else "Repeat", fontSize = 12.sp)
                }
              }

              OutlinedTextField(
                value = shadowAttemptText,
                onValueChange = { shadowAttemptText = it },
                placeholder = {
                  Text("Spoken or typed shadowing attempt...", fontSize = 12.sp, color = LingoTokens.textMuted)
                },
                colors = OutlinedTextFieldDefaults.colors(
                  focusedTextColor = LingoTokens.textPrimary,
                  unfocusedTextColor = LingoTokens.textPrimary,
                  focusedBorderColor = LingoTokens.accentPrimary,
                  unfocusedBorderColor = LingoTokens.borderSubtle,
                  focusedContainerColor = LingoTokens.bgSubtle,
                  unfocusedContainerColor = LingoTokens.bgSubtle
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("accent_shadow_input")
              )

              if (shadowAttemptText.isNotBlank()) {
                val targetWords = activeShadowSample.transcriptInTargetLang
                  .lowercase()
                  .split(Regex("[^\\p{L}0-9']+"))
                  .filter { it.isNotBlank() }
                val userWords = shadowAttemptText
                  .lowercase()
                  .split(Regex("[^\\p{L}0-9']+"))
                  .filter { it.isNotBlank() }
                val matched = userWords.count { it in targetWords.toSet() }
                val computedSimilarity = if (targetWords.isEmpty()) 0 else ((matched.toFloat() / targetWords.size) * 100f).toInt().coerceIn(15, 98)
                LaunchedEffect(shadowAttemptText) {
                  shadowEstimateScore = computedSimilarity
                }

                // Visual waveform envelope comparison
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = "Waveform Rhythm & Cadence Comparison (Similarity: $computedSimilarity% • estimate)",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LingoTokens.accentPrimary
                  )
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    targetWords.take(12).forEachIndexed { idx, w ->
                      val matchedWord = userWords.getOrNull(idx) == w || w in userWords
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (matchedWord) LingoTokens.accentEmerald else LingoTokens.accentAmber,
                        modifier = Modifier
                          .weight(1f)
                          .height(if (idx % 2 == 0) 16.dp else 11.dp)
                      ) {}
                    }
                  }
                }
              }
            }

            HorizontalDivider(color = LingoTokens.borderSubtle)

            Text(
              text = "Minimal-Pair Phoneme Drills",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = LingoTokens.textPrimary
            )
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
            ) {
              minimalPairs.forEachIndexed { idx, pair ->
                FilterChip(
                  selected = selectedMinimalPairIdx == idx,
                  onClick = { selectedMinimalPairIdx = idx },
                  label = { Text(pair.first, fontSize = 12.sp) },
                  colors = portfolioChipColors(),
                  modifier = Modifier.testTag("minimal_pair_chip_$idx")
                )
              }
            }
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = LingoTokens.bgSubtle,
              border = BorderStroke(1.dp, LingoTokens.borderSubtle),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${activePair.first} — ${activePair.second}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = LingoTokens.accentPrimary
                  )
                  Text(
                    text = activePair.third,
                    fontSize = 12.5.sp,
                    color = LingoTokens.textPrimary
                  )
                }
                IconButton(
                  onClick = {
                    speechManager.speakWithRate(activePair.third, langCode, playbackRate)
                  }
                ) {
                  Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Hear minimal pair",
                    tint = LingoTokens.accentPrimary
                  )
                }
              }
            }

            if (repeatedMistakes.isNotEmpty()) {
              Text(
                text = "Target Sounds From Your Past Mistakes: ${repeatedMistakes.take(2).joinToString(" • ") { "${it.exampleWrong} → ${it.exampleCorrected}" }}",
                fontSize = 12.sp,
                color = LingoTokens.accentAmber
              )
            }
          }
        }
      }

      items(samples, key = { it.id }) { sample ->
        val selectedAnswer = selectedOptionBySample[sample.id]
        val langCode = Language.SUPPORTED_LANGUAGES.firstOrNull {
          it.name.equals(sample.language, ignoreCase = true)
        }?.code ?: targetLang.code

        LingoCleanCard(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("accent_sample_card_${sample.id}")
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${sample.countryFlag} ${sample.regionName}",
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp,
                  color = LingoTokens.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = sample.accentCharacteristics,
                  fontSize = 12.5.sp,
                  color = LingoTokens.textSecondary
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                onClick = {
                  speechManager.speakWithRate(
                    text = sample.transcriptInTargetLang,
                    languageCode = langCode,
                    speechRate = playbackRate
                  )
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = LingoTokens.accentBlue,
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("accent_play_btn_${sample.id}")
              ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("${playbackRate}x", fontSize = 12.sp)
              }
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = LingoTokens.bgSubtle,
              border = BorderStroke(1.dp, LingoTokens.borderSubtle),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "\"${sample.transcriptInTargetLang}\"",
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 14.sp,
                  color = LingoTokens.textPrimary
                )
                Text(
                  text = sample.englishTranslation,
                  fontSize = 12.5.sp,
                  color = LingoTokens.textSecondary
                )
              }
            }

            Text(
              text = "Regional Vocabulary & Expressions",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = LingoTokens.accentPrimary
            )
            sample.regionalVocabulary.forEach { vocab ->
              Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "• ${vocab.regionalTerm} (≈ ${vocab.standardEquivalent}) — ${vocab.meaning}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = LingoTokens.textPrimary
                  )
                  Text(
                    text = "  ${vocab.exampleUsage}",
                    fontSize = 12.sp,
                    color = LingoTokens.textSecondary
                  )
                }
                TextButton(
                  onClick = {
                    viewModel.saveVocabularyToFlashcards(
                      frontText = vocab.regionalTerm,
                      backText = "${vocab.meaning} (${sample.regionName})",
                      pronunciation = vocab.standardEquivalent
                    )
                  }
                ) {
                  Text("+ Save", fontSize = 12.sp, color = LingoTokens.accentPrimary)
                }
              }
            }

            HorizontalDivider(color = LingoTokens.borderSubtle)

            Text(
              text = "Comprehension Check: ${sample.comprehensionQuestion}",
              fontWeight = FontWeight.SemiBold,
              fontSize = 13.sp,
              color = LingoTokens.textPrimary
            )
            sample.comprehensionOptions.forEachIndexed { optIdx, optionText ->
              val isChosen = selectedAnswer == optIdx
              val isCorrect = optIdx == sample.correctOptionIndex
              OutlinedButton(
                onClick = {
                  selectedOptionBySample = selectedOptionBySample + (sample.id to optIdx)
                },
                border = BorderStroke(
                  1.dp,
                  when {
                    !isChosen -> LingoTokens.borderSubtle
                    isCorrect -> LingoTokens.accentEmerald
                    else -> LingoTokens.accentRose
                  }
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = when {
                    !isChosen -> LingoTokens.bgSubtle
                    isCorrect -> LingoTokens.accentEmerald.copy(alpha = 0.14f)
                    else -> LingoTokens.accentRose.copy(alpha = 0.14f)
                  },
                  contentColor = LingoTokens.textPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("accent_comp_opt_${sample.id}_$optIdx")
              ) {
                Text(optionText, fontSize = 12.5.sp)
              }
            }

            if (selectedAnswer != null) {
              val correct = selectedAnswer == sample.correctOptionIndex
              Text(
                text = if (correct) "Correct! ${sample.comprehensionExplanation}" else sample.comprehensionExplanation,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (correct) LingoTokens.accentEmerald else LingoTokens.accentAmber
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Capability 9: Personal Language Portfolio
 */
@Composable
fun PersonalLanguagePortfolioScreen(
  viewModel: LingoViewModel,
  speechManager: SpeechManager,
  onBack: () -> Unit
) {
  BackHandler { onBack() }
  val context = LocalContext.current
  val userProfile by viewModel.userProfileState.collectAsState()
  val targetLang = Language.findByCode(userProfile.targetLanguageCode)

  val speakingSamples by viewModel.portfolioSpeakingSamplesState.collectAsState()
  val writingSamples by viewModel.portfolioWritingSamplesState.collectAsState()
  val completedMissions by viewModel.completedDailyMissionsState.collectAsState()
  val flashcards by viewModel.flashcardProgressState.collectAsState()
  val privacySettings by viewModel.portfolioPrivacySettingsState.collectAsState()
  val mediaPrivacyConsent by viewModel.mediaPrivacyConsentState.collectAsState()
  val futureMeComparisons by viewModel.futureMeComparisonsState.collectAsState()
  val livingWorldMap by viewModel.livingWorldProgressState.collectAsState()
  val habitCompletions by viewModel.verifiedHabitCompletionsState.collectAsState()
  val repairScenarios by viewModel.completedRepairScenariosState.collectAsState()

  val skillPassport = remember(
    targetLang.code,
    speakingSamples.size,
    completedMissions.size,
    flashcards.size,
    livingWorldMap.size,
    habitCompletions.size,
    repairScenarios.size
  ) {
    viewModel.computeLanguageSkillPassport(targetLang.code)
  }

  val isListening by speechManager.isListening.collectAsState()
  val spokenText by speechManager.spokenText.collectAsState()
  val coroutineScope = rememberCoroutineScope()
  var newSampleDraft by remember(targetLang.code) { mutableStateOf("") }
  var statusToast by remember(targetLang.code) { mutableStateOf<String?>(null) }
  var isGeneratingFutureMe by remember { mutableStateOf(false) }
  var futureMeErrorBanner by remember { mutableStateOf<String?>(null) }
  val isVerifiedPro = userProfile.isEntitlementVerified

  LaunchedEffect(spokenText) {
    if (spokenText.isNotBlank()) {
      newSampleDraft = spokenText
    }
  }

  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      speechManager.startListening(targetLang.code)
    }
  }

  val earlierSample = speakingSamples.minByOrNull { it.recordedAtEpochMs }
  val recentSample = speakingSamples.maxByOrNull { it.recordedAtEpochMs }
  val masteredVocabCount = flashcards.count { it.boxLevel >= 3L }
  val futureMeInput = recentSample?.spokenTranscript?.takeIf { it.isNotBlank() } ?: newSampleDraft.trim()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(LingoTokens.bgCanvas)
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .imePadding()
  ) {
    LingoTopBar(
      title = "Language Portfolio",
      subtitle = "${targetLang.name} • Skill Passport, recordings & growth",
      onBack = onBack
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(vertical = 16.dp)
    ) {
      // 1. Portfolio Progress & Mastery Summary
      item {
        LingoCleanCard(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("portfolio_summary_card")
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "${targetLang.flagEmoji} ${targetLang.name} Mastery Portfolio",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = LingoTokens.textPrimary
              )
              LingoStatusBadge(
                text = userProfile.proficiencyLevel,
                color = LingoTokens.accentPrimary
              )
            }
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              PortfolioStatMetric("${speakingSamples.size}", "Speaking", LingoTokens.accentPrimary)
              PortfolioStatMetric("${writingSamples.size}", "Writing", LingoTokens.accentEmerald)
              PortfolioStatMetric("${completedMissions.size}", "Missions", LingoTokens.accentAmber)
              PortfolioStatMetric("$masteredVocabCount/${flashcards.size.coerceAtLeast(1)}", "Mastered", LingoTokens.accentBlue)
            }
          }
        }
      }

      // 1B. Language Skill Passport
      item {
        LingoCleanCard(
          borderColor = LingoTokens.accentEmerald.copy(alpha = 0.4f),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("language_skill_passport_card")
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "Language Skill Passport • Practical Competencies",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = LingoTokens.accentEmerald
            )
            Text(
              text = skillPassport.officialDisclaimer,
              fontSize = 11.5.sp,
              color = LingoTokens.textSecondary
            )

            skillPassport.competencies.forEachIndexed { idx, comp ->
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = LingoTokens.bgSubtle,
                border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("passport_competency_$idx")
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = comp.domainTitle,
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.5.sp,
                      color = if (comp.isDemonstrated) LingoTokens.accentEmerald else LingoTokens.accentAmber,
                      modifier = Modifier.weight(1f)
                    )
                    Text(
                      text = "${comp.verifiedEvidenceCount} verified",
                      fontSize = 11.5.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = LingoTokens.accentPrimary
                    )
                  }
                  Text(
                    text = comp.practicalCanDoStatement,
                    fontSize = 12.5.sp,
                    color = LingoTokens.textPrimary
                  )
                  Text(
                    text = comp.latestEvidenceSummary,
                    fontSize = 12.sp,
                    color = LingoTokens.textSecondary
                  )
                }
              }
            }
          }
        }
      }

      // 1C. Future Me Speaking (Consent-Preserving Synthetic Fluent Comparison)
      item {
        LingoCleanCard(
          borderColor = LingoTokens.accentBlue.copy(alpha = 0.4f),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("future_me_speaking_card")
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "Future Me Speaking • Fluent Reference",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = LingoTokens.accentPrimary
            )
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = LingoTokens.accentAmber.copy(alpha = 0.12f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "Synthetic Fluent Reference — Not voice cloning. Compares your transcript with a clearly labeled fluent phrasing example.",
                fontSize = 11.5.sp,
                color = LingoTokens.accentAmber,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(10.dp)
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  "Synthetic Comparison Consent",
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.sp,
                  color = LingoTokens.textPrimary
                )
                Text(
                  "Enable to compare your speaking transcript with a fluent target model.",
                  fontSize = 11.5.sp,
                  color = LingoTokens.textSecondary
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Switch(
                checked = mediaPrivacyConsent.syntheticFluentReferenceConsent,
                onCheckedChange = { enabled ->
                  viewModel.updateMediaPrivacyConsent(
                    mediaPrivacyConsent.copy(syntheticFluentReferenceConsent = enabled)
                  )
                },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = Color.White,
                  checkedTrackColor = LingoTokens.accentPrimary
                ),
                modifier = Modifier.testTag("future_me_consent_switch")
              )
            }

            if (mediaPrivacyConsent.syntheticFluentReferenceConsent) {
              if (futureMeInput.isBlank()) {
                Text(
                  text = "Record or type a ${targetLang.name} speaking sample below first to generate a Future Me comparison.",
                  fontSize = 12.sp,
                  color = LingoTokens.textSecondary
                )
              }

              Button(
                onClick = {
                  if (!isVerifiedPro) {
                    AiErrorHandler.requestUpgrade(
                      mode = UpgradeSheetMode.AI_UPGRADE,
                      featureKey = AccessPolicyFeatureKeys.PERSONALIZED_MATERIAL,
                      headline = "Unlock Future Me Coaching",
                      reason = "Upgrade to LingoSphere Pro for fluent reference comparisons and AI speech feedback."
                    )
                    return@Button
                  }
                  isGeneratingFutureMe = true
                  futureMeErrorBanner = null
                  coroutineScope.launch {
                    try {
                      val comp = PracticalFeaturesAiService.generateFutureMeComparison(
                        languageCode = targetLang.code,
                        learnerTranscript = futureMeInput,
                        consentVerified = true
                      )
                      viewModel.recordFutureMeComparison(comp)
                    } catch (e: Exception) {
                      val handled = AiErrorHandler.handle(e, contextTag = "FutureMeComparison")
                      if (handled is AiUiError.ProRequired) {
                        AiErrorHandler.requestUpgrade(
                          mode = UpgradeSheetMode.AI_UPGRADE,
                          featureKey = AccessPolicyFeatureKeys.PERSONALIZED_MATERIAL
                        )
                      } else {
                        futureMeErrorBanner = handled.userMessage
                      }
                    } finally {
                      isGeneratingFutureMe = false
                    }
                  }
                },
                enabled = !isGeneratingFutureMe && futureMeInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                  containerColor = LingoTokens.accentBlue,
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("future_me_generate_button")
              ) {
                if (!isVerifiedPro) {
                  LingoGoldLockIcon(size = 14.dp)
                  Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                  if (isGeneratingFutureMe) "Comparing..."
                  else "Compare Latest Speech with Fluent Model",
                  fontSize = 12.5.sp
                )
              }

              if (!futureMeErrorBanner.isNullOrBlank()) {
                LingoCalmAiErrorBanner(
                  message = futureMeErrorBanner,
                  onDismiss = { futureMeErrorBanner = null }
                )
              }

              val latestComp = futureMeComparisons.firstOrNull()
              if (latestComp != null) {
                if (!latestComp.aiErrorMessage.isNullOrBlank()) {
                  LingoCalmAiErrorBanner(
                    message = AiErrorHandler.sanitizeUserFacingMessage(latestComp.aiErrorMessage)
                  )
                } else {
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LingoTokens.bgSubtle,
                    border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("future_me_result_box")
                  ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      Text(
                        text = latestComp.explicitSyntheticLabel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = LingoTokens.accentAmber
                      )
                      Text(
                        text = "Your Speech: \"${latestComp.learnerTranscript}\"",
                        fontSize = 12.5.sp,
                        color = LingoTokens.textPrimary
                      )
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = "Fluent Example: \"${latestComp.syntheticFluentReferenceText}\"",
                          fontSize = 13.sp,
                          fontWeight = FontWeight.Bold,
                          color = LingoTokens.accentEmerald,
                          modifier = Modifier.weight(1f)
                        )
                        IconButton(
                          onClick = { speechManager.speak(latestComp.syntheticFluentReferenceText, targetLang.code) },
                          modifier = Modifier.size(48.dp)
                        ) {
                          Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Hear synthetic fluent reference",
                            tint = LingoTokens.accentPrimary
                          )
                        }
                      }
                      Text(
                        text = latestComp.pacingAndFlowComparison,
                        fontSize = 12.sp,
                        color = LingoTokens.textSecondary
                      )
                      Text(
                        text = latestComp.phrasingUpgradeExplanation,
                        fontSize = 12.sp,
                        color = LingoTokens.accentPrimary
                      )
                      latestComp.actionableDrills.forEach { drill ->
                        Text(
                          text = "• ${drill.focusArea}: ${drill.drillInstruction}",
                          fontSize = 12.sp,
                          fontWeight = FontWeight.Medium,
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

      // 2. Compare Earlier vs. Recent Speaking Recordings
      if (earlierSample != null && recentSample != null && speakingSamples.size >= 2) {
        item {
          LingoCleanCard(
            borderColor = LingoTokens.accentPrimary.copy(alpha = 0.4f),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("portfolio_comparison_card")
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text(
                text = "Earlier vs. Recent Speaking Growth",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = LingoTokens.accentPrimary
              )
              val pronDelta = recentSample.pronunciationScore - earlierSample.pronunciationScore
              val fluencyDelta = recentSample.fluencyScore - earlierSample.fluencyScore
              Text(
                text = "Pronunciation ${if (pronDelta >= 0) "+$pronDelta%" else "$pronDelta%"} • Fluency ${if (fluencyDelta >= 0) "+$fluencyDelta%" else "$fluencyDelta%"}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = LingoTokens.accentEmerald
              )

              Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = LingoTokens.bgSubtle,
                  border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("EARLIER", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = LingoTokens.accentAmber)
                    Text(earlierSample.dateLabel, fontSize = 11.sp, color = LingoTokens.textSecondary)
                    Text("Pron ${earlierSample.pronunciationScore}% • Flu ${earlierSample.fluencyScore}%", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = LingoTokens.textPrimary)
                    Text("\"${earlierSample.spokenTranscript}\"", fontSize = 11.5.sp, maxLines = 3, color = LingoTokens.textSecondary)
                    OutlinedButton(
                      onClick = {
                        speechManager.playPortfolioRecording(
                          filePath = earlierSample.audioFilePath,
                          fallbackText = earlierSample.spokenTranscript,
                          fallbackLanguageCode = targetLang.code
                        )
                      },
                      border = BorderStroke(1.dp, LingoTokens.borderFocus),
                      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("play_earlier_sample_btn")
                    ) {
                      Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(15.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Play", fontSize = 11.5.sp)
                    }
                  }
                }

                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = LingoTokens.bgSubtle,
                  border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("RECENT", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = LingoTokens.accentEmerald)
                    Text(recentSample.dateLabel, fontSize = 11.sp, color = LingoTokens.textSecondary)
                    Text("Pron ${recentSample.pronunciationScore}% • Flu ${recentSample.fluencyScore}%", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = LingoTokens.textPrimary)
                    Text("\"${recentSample.spokenTranscript}\"", fontSize = 11.5.sp, maxLines = 3, color = LingoTokens.textSecondary)
                    Button(
                      onClick = {
                        speechManager.playPortfolioRecording(
                          filePath = recentSample.audioFilePath,
                          fallbackText = recentSample.spokenTranscript,
                          fallbackLanguageCode = targetLang.code
                        )
                      },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = LingoTokens.accentEmerald,
                        contentColor = Color.White
                      ),
                      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("play_recent_sample_btn")
                    ) {
                      Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(15.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Play", fontSize = 11.5.sp)
                    }
                  }
                }
              }
            }
          }
        }
      }

      // 3. Record New Speaking Sample
      item {
        LingoCleanCard(modifier = Modifier.fillMaxWidth()) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              "Record New Speaking Sample",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = LingoTokens.textPrimary
            )
            OutlinedTextField(
              value = newSampleDraft,
              onValueChange = { newSampleDraft = it },
              placeholder = {
                Text(
                  "Tap Record Voice or enter your ${targetLang.name} transcript...",
                  fontSize = 12.5.sp,
                  color = LingoTokens.textMuted
                )
              },
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = LingoTokens.textPrimary,
                unfocusedTextColor = LingoTokens.textPrimary,
                focusedBorderColor = LingoTokens.accentPrimary,
                unfocusedBorderColor = LingoTokens.borderSubtle,
                focusedContainerColor = LingoTokens.bgSubtle,
                unfocusedContainerColor = LingoTokens.bgSubtle
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("portfolio_new_sample_input")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isListening) "Stop" else "Record", fontSize = 12.sp)
              }

              Button(
                onClick = {
                  val transcript = newSampleDraft.trim()
                  if (transcript.isEmpty()) return@Button
                  val sampleId = "port_${System.currentTimeMillis()}"
                  val wavPath = speechManager.exportLastRecordingToPortfolio(sampleId)
                  viewModel.savePortfolioSpeakingSample(
                    PortfolioSpeakingSample(
                      id = sampleId,
                      title = "Portfolio Recording (${userProfile.proficiencyLevel})",
                      targetLanguage = targetLang.name,
                      cefrLevel = userProfile.proficiencyLevel,
                      spokenTranscript = transcript,
                      improvedVersion = transcript,
                      pronunciationScore = 88,
                      fluencyScore = 86,
                      durationSeconds = 20,
                      recordedAtEpochMs = System.currentTimeMillis(),
                      dateLabel = "Recent • Just Now",
                      audioFilePath = wavPath,
                      isPrivate = privacySettings.defaultPrivateRecordings
                    )
                  )
                  newSampleDraft = ""
                  statusToast = "Saved speaking recording to your ${targetLang.name} portfolio."
                },
                enabled = newSampleDraft.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                  containerColor = LingoTokens.accentBlue,
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("portfolio_save_recording_btn")
              ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Sample", fontSize = 12.sp)
              }
            }

            if (statusToast != null) {
              Text(
                statusToast!!,
                fontSize = 12.5.sp,
                color = LingoTokens.accentEmerald,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
      }

      // 4. Privacy Controls & Recording Deletion
      item {
        LingoCleanCard(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("portfolio_privacy_controls_card")
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Security, contentDescription = null, tint = LingoTokens.accentPrimary)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                "Privacy Controls & Storage",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = LingoTokens.textPrimary
              )
            }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                "Keep new recordings Private by default",
                fontSize = 13.sp,
                color = LingoTokens.textPrimary,
                modifier = Modifier.weight(1f)
              )
              Switch(
                checked = privacySettings.defaultPrivateRecordings,
                onCheckedChange = {
                  viewModel.updatePortfolioPrivacySettings(privacySettings.copy(defaultPrivateRecordings = it))
                },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = Color.White,
                  checkedTrackColor = LingoTokens.accentPrimary
                ),
                modifier = Modifier.testTag("portfolio_default_private_switch")
              )
            }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                "Store audio on-device only",
                fontSize = 13.sp,
                color = LingoTokens.textPrimary,
                modifier = Modifier.weight(1f)
              )
              Switch(
                checked = privacySettings.storeAudioOnDeviceOnly,
                onCheckedChange = {
                  viewModel.updatePortfolioPrivacySettings(privacySettings.copy(storeAudioOnDeviceOnly = it))
                },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = Color.White,
                  checkedTrackColor = LingoTokens.accentPrimary
                )
              )
            }
            OutlinedButton(
              onClick = {
                speechManager.deleteAllPortfolioRecordingFiles()
                viewModel.deleteAllPortfolioSpeakingSamples()
                statusToast = "All stored speaking recordings deleted from device."
              },
              enabled = speakingSamples.isNotEmpty(),
              border = BorderStroke(1.dp, LingoTokens.accentRose.copy(alpha = 0.7f)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("portfolio_delete_all_recordings_btn")
            ) {
              Icon(Icons.Default.DeleteForever, contentDescription = null, tint = LingoTokens.accentRose, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Delete All Recordings (${speakingSamples.size})", color = LingoTokens.accentRose, fontSize = 12.5.sp)
            }
          }
        }
      }

      // 5. Stored Speaking Samples
      item {
        LingoSectionHeader(title = "Stored Speaking Samples (${speakingSamples.size})")
      }

      if (speakingSamples.isEmpty()) {
        item {
          LingoEmptyOrErrorCard(
            title = "No ${targetLang.name} Speaking Samples Yet",
            message = "Record a sample above or complete speaking sessions in Living Immersion or Roleplay to build your ${targetLang.name} timeline."
          )
        }
      } else {
        items(speakingSamples, key = { it.id }) { sample ->
          LingoCleanCard(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("portfolio_sample_item_${sample.id}")
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(sample.title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = LingoTokens.textPrimary)
                  Text(
                    "${sample.dateLabel} • Pron ${sample.pronunciationScore}% • Fluency ${sample.fluencyScore}%",
                    fontSize = 11.5.sp,
                    color = LingoTokens.accentPrimary
                  )
                }
                FilterChip(
                  selected = sample.isPrivate,
                  onClick = { viewModel.togglePortfolioSpeakingSamplePrivacy(sample.id, sample.targetLanguage) },
                  label = { Text(if (sample.isPrivate) "Private" else "Shareable", fontSize = 11.sp) },
                  colors = portfolioChipColors()
                )
              }
              Text(
                "\"${sample.spokenTranscript}\"",
                fontSize = 13.sp,
                color = LingoTokens.textPrimary
              )
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                  onClick = {
                    speechManager.playPortfolioRecording(
                      filePath = sample.audioFilePath,
                      fallbackText = sample.spokenTranscript,
                      fallbackLanguageCode = targetLang.code
                    )
                  },
                  border = BorderStroke(1.dp, LingoTokens.borderFocus),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                  Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(15.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Play", fontSize = 12.sp)
                }
                OutlinedButton(
                  onClick = {
                    speechManager.deletePortfolioRecordingFile(sample.audioFilePath)
                    viewModel.deletePortfolioSpeakingSample(sample.id, sample.targetLanguage)
                  },
                  border = BorderStroke(1.dp, LingoTokens.accentRose.copy(alpha = 0.7f)),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                  modifier = Modifier.testTag("delete_portfolio_sample_${sample.id}")
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete recording", tint = LingoTokens.accentRose, modifier = Modifier.size(15.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Delete", fontSize = 12.sp, color = LingoTokens.accentRose)
                }
              }
            }
          }
        }
      }

      // 6. Writing Improvements
      item {
        LingoSectionHeader(title = "Writing Improvements (${writingSamples.size})")
      }

      if (writingSamples.isEmpty()) {
        item {
          LingoEmptyOrErrorCard(
            title = "No ${targetLang.name} Writing Samples Yet",
            message = "Practice writing revisions in Writing Studio to see your grammar gains tracked here."
          )
        }
      } else {
        items(writingSamples, key = { it.id }) { wr ->
          LingoCleanCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
              Text(
                "${wr.promptTitle} (+${wr.grammarGainScore}% grammar gain)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = LingoTokens.accentEmerald
              )
              Text(
                "Before: \"${wr.originalDraft}\"",
                fontSize = 12.5.sp,
                color = LingoTokens.textSecondary
              )
              Text(
                "Improved: \"${wr.improvedRevision}\"",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = LingoTokens.textPrimary
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PortfolioStatMetric(
  value: String,
  label: String,
  color: Color
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
    Text(label, fontSize = 11.5.sp, color = LingoTokens.textSecondary)
  }
}
