package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Language
import com.example.data.model.PlacementTestEvaluation
import com.example.data.model.UserProfile
import com.example.data.model.normalizeCefrCodeOrNull
import com.example.data.repository.CurriculumData

private enum class LanguageSheetStep {
  SELECT_LANGUAGE,
  SELECT_LEVEL,
  PLACEMENT_TEST
}

private data class CefrLevelChoice(
  val code: String,
  val title: String,
  val description: String
)

private val CEFR_LEVEL_CHOICES = listOf(
  CefrLevelChoice("A1", "Beginner", "Greetings, essentials & first phrases"),
  CefrLevelChoice("A2", "Elementary", "Everyday routines, shopping & travel"),
  CefrLevelChoice("B1", "Intermediate", "Work, stories, opinions & past/future"),
  CefrLevelChoice("B2", "Upper-Intermediate", "Fluent discussions, nuance & debate"),
  CefrLevelChoice("C1", "Advanced", "Formal register, precision & mastery")
)

/**
 * Single shared "Change Learning Language" bottom sheet reused by both Home and Profile.
 * - Free for all users (no Pro lock, no ads).
 * - Preserves LTR chrome layout even when an RTL language is active.
 * - Supports selecting a new language -> choosing CEFR level (A1–C1 or quick level test),
 *   as well as changing the level for the currently selected language at any time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeLearningLanguageBottomSheet(
  userProfile: UserProfile,
  cefrStageLabel: String = "",
  onLanguageAndLevelSelected: (languageCode: String, cefrCode: String) -> Unit,
  onApplyPlacementEvaluation: (languageCode: String, evaluation: PlacementTestEvaluation) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val currentLang = Language.findByCode(userProfile.targetLanguageCode)
  val currentCefr = userProfile.effectiveCefrForLanguage(
    langCode = userProfile.targetLanguageCode,
    fallbackPlacementCefr = cefrStageLabel
  )

  var step by remember { mutableStateOf(LanguageSheetStep.SELECT_LANGUAGE) }
  var searchQuery by remember { mutableStateOf("") }
  var pendingLanguage by remember { mutableStateOf(currentLang) }

  // Placement test state for pendingLanguage
  val placementQuestions = remember(pendingLanguage.code) {
    CurriculumData.getPlacementQuestions(pendingLanguage.code)
  }
  var placementIndex by remember(pendingLanguage.code, step) { mutableIntStateOf(0) }
  var placementCorrectCount by remember(pendingLanguage.code, step) { mutableIntStateOf(0) }
  var placementEvaluation by remember(pendingLanguage.code, step) {
    mutableStateOf<PlacementTestEvaluation?>(null)
  }

  val filteredLanguages = remember(searchQuery) {
    val q = searchQuery.trim().lowercase()
    if (q.isEmpty()) {
      Language.SUPPORTED_LANGUAGES
    } else {
      Language.SUPPORTED_LANGUAGES.filter { lang ->
        lang.name.lowercase().contains(q) ||
          lang.nativeName.lowercase().contains(q) ||
          lang.code.lowercase().contains(q)
      }
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = Color(0xFF0E1525),
    contentColor = LingoTokens.textPrimary,
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(top = 10.dp, bottom = 6.dp)
          .width(40.dp)
          .height(4.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.22f))
      )
    }
  ) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 20.dp)
          .padding(bottom = 20.dp)
          .testTag("change_learning_language_sheet")
      ) {
        when (step) {
          LanguageSheetStep.SELECT_LANGUAGE -> {
            // Header
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Change Learning Language",
                  fontSize = 19.sp,
                  fontWeight = FontWeight.Bold,
                  color = LingoTokens.textPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.testTag("language_sheet_title")
                )
                Text(
                  text = "Progress & level are saved per language",
                  fontSize = 12.5.sp,
                  color = LingoTokens.textSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              IconButton(
                onClick = onDismiss,
                modifier = Modifier
                  .size(48.dp)
                  .testTag("language_sheet_close_button")
              ) {
                Icon(
                  imageVector = Icons.Outlined.Close,
                  contentDescription = "Close language selector",
                  tint = LingoTokens.textSecondary
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Active language + quick "Change level" button
            Surface(
              color = LingoTokens.bgGlassCard,
              shape = RoundedCornerShape(16.dp),
              border = BorderStroke(1.dp, LingoTokens.brandCyan.copy(alpha = 0.35f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  Text(
                    text = currentLang.flagEmoji,
                    fontSize = 24.sp
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = currentLang.name,
                      fontSize = 15.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = LingoTokens.textPrimary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "Current level: $currentCefr",
                      fontSize = 12.5.sp,
                      color = LingoTokens.brandCyan,
                      fontWeight = FontWeight.Medium,
                      maxLines = 1
                    )
                  }
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                  onClick = {
                    pendingLanguage = currentLang
                    step = LanguageSheetStep.SELECT_LEVEL
                  },
                  border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                  colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White.copy(alpha = 0.05f),
                    contentColor = LingoTokens.textPrimary
                  ),
                  shape = RoundedCornerShape(12.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                  modifier = Modifier
                    .heightIn(min = 40.dp)
                    .testTag("language_sheet_change_level_button")
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = LingoTokens.brandCyan,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Level",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search bar
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              singleLine = true,
              placeholder = {
                Text(
                  text = "Search languages...",
                  color = LingoTokens.textSecondary,
                  fontSize = 14.sp
                )
              },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Outlined.Search,
                  contentDescription = "Search languages",
                  tint = LingoTokens.textSecondary,
                  modifier = Modifier.size(20.dp)
                )
              },
              shape = RoundedCornerShape(14.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = LingoTokens.textPrimary,
                unfocusedTextColor = LingoTokens.textPrimary,
                focusedBorderColor = LingoTokens.brandCyan,
                unfocusedBorderColor = LingoTokens.borderSubtle,
                focusedContainerColor = LingoTokens.bgGlassCard,
                unfocusedContainerColor = LingoTokens.bgGlassCard,
                cursorColor = LingoTokens.brandCyan
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("language_sheet_search_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Language list
            LazyColumn(
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .testTag("language_sheet_list"),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              items(filteredLanguages, key = { it.code }) { lang ->
                val isSelected = lang.code.equals(userProfile.targetLanguageCode, ignoreCase = true)
                val savedCefr = if (isSelected) {
                  currentCefr
                } else {
                  normalizeCefrCodeOrNull(userProfile.languageLevels[lang.code])
                }
                Surface(
                  onClick = {
                    pendingLanguage = lang
                    step = LanguageSheetStep.SELECT_LEVEL
                  },
                  color = if (isSelected) LingoTokens.brandIndigo.copy(alpha = 0.22f) else LingoTokens.bgGlassCard,
                  shape = RoundedCornerShape(14.dp),
                  border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) LingoTokens.brandCyan.copy(alpha = 0.6f) else LingoTokens.borderSubtle
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("language_sheet_item_${lang.code}")
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = lang.flagEmoji,
                      fontSize = 22.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = lang.name,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = LingoTokens.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      Text(
                        text = lang.nativeName,
                        fontSize = 12.5.sp,
                        color = LingoTokens.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                    if (!savedCefr.isNullOrBlank()) {
                      Box(
                        modifier = Modifier
                          .clip(RoundedCornerShape(8.dp))
                          .background(Color.White.copy(alpha = 0.08f))
                          .border(1.dp, LingoTokens.borderSubtle, RoundedCornerShape(8.dp))
                          .padding(horizontal = 8.dp, vertical = 4.dp)
                      ) {
                        Text(
                          text = savedCefr,
                          fontSize = 12.sp,
                          fontWeight = FontWeight.SemiBold,
                          color = if (isSelected) LingoTokens.brandCyan else LingoTokens.textPrimary
                        )
                      }
                    }
                    if (isSelected) {
                      Spacer(modifier = Modifier.width(8.dp))
                      Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Selected language",
                        tint = LingoTokens.brandCyan,
                        modifier = Modifier.size(20.dp)
                      )
                    }
                  }
                }
              }
            }
          }

          LanguageSheetStep.SELECT_LEVEL -> {
            val savedPendingCefr = if (pendingLanguage.code.equals(userProfile.targetLanguageCode, ignoreCase = true)) {
              currentCefr
            } else {
              normalizeCefrCodeOrNull(userProfile.languageLevels[pendingLanguage.code])
            }

            Column(
              modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .testTag("language_sheet_level_step")
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                IconButton(
                  onClick = { step = LanguageSheetStep.SELECT_LANGUAGE },
                  modifier = Modifier
                    .size(48.dp)
                    .testTag("language_sheet_back_button")
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back to language list",
                    tint = LingoTokens.textPrimary
                  )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Choose your ${pendingLanguage.name} level",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LingoTokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = "${pendingLanguage.flagEmoji} ${pendingLanguage.nativeName} · Change anytime",
                    fontSize = 12.5.sp,
                    color = LingoTokens.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CEFR_LEVEL_CHOICES.forEach { choice ->
                  val isCurrentChoice = savedPendingCefr == choice.code
                  Surface(
                    onClick = {
                      onLanguageAndLevelSelected(pendingLanguage.code, choice.code)
                      onDismiss()
                    },
                    color = if (isCurrentChoice) LingoTokens.brandIndigo.copy(alpha = 0.24f) else LingoTokens.bgGlassCard,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                      width = 1.dp,
                      color = if (isCurrentChoice) LingoTokens.brandCyan.copy(alpha = 0.65f) else LingoTokens.borderSubtle
                    ),
                    modifier = Modifier
                      .fillMaxWidth()
                      .heightIn(min = 56.dp)
                      .testTag("language_sheet_level_${choice.code}")
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Box(
                        modifier = Modifier
                          .size(40.dp)
                          .clip(RoundedCornerShape(12.dp))
                          .background(
                            if (isCurrentChoice) {
                              Brush.linearGradient(listOf(LingoTokens.brandIndigo, LingoTokens.brandCyan))
                            } else {
                              Brush.linearGradient(
                                listOf(
                                  Color.White.copy(alpha = 0.08f),
                                  Color.White.copy(alpha = 0.04f)
                                )
                              )
                            }
                          ),
                        contentAlignment = Alignment.Center
                      ) {
                        Text(
                          text = choice.code,
                          fontSize = 14.sp,
                          fontWeight = FontWeight.Bold,
                          color = Color.White
                        )
                      }
                      Spacer(modifier = Modifier.width(12.dp))
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = choice.title,
                          fontSize = 15.sp,
                          fontWeight = FontWeight.SemiBold,
                          color = LingoTokens.textPrimary,
                          maxLines = 1
                        )
                        Text(
                          text = choice.description,
                          fontSize = 12.5.sp,
                          color = LingoTokens.textSecondary,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                      }
                      if (isCurrentChoice) {
                        Icon(
                          imageVector = Icons.Outlined.Check,
                          contentDescription = "Current level",
                          tint = LingoTokens.brandCyan,
                          modifier = Modifier.size(20.dp)
                        )
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              // "Not sure — test my level" option
              Surface(
                onClick = {
                  placementIndex = 0
                  placementCorrectCount = 0
                  placementEvaluation = null
                  step = LanguageSheetStep.PLACEMENT_TEST
                },
                color = LingoTokens.brandCyan.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, LingoTokens.brandCyan.copy(alpha = 0.50f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 52.dp)
                  .testTag("language_sheet_placement_test_button")
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Quiz,
                    contentDescription = null,
                    tint = LingoTokens.brandCyan,
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = "Not sure — test my level",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LingoTokens.textPrimary
                  )
                }
              }
            }
          }

          LanguageSheetStep.PLACEMENT_TEST -> {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .testTag("language_sheet_placement_step")
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                IconButton(
                  onClick = { step = LanguageSheetStep.SELECT_LEVEL },
                  modifier = Modifier
                    .size(48.dp)
                    .testTag("language_sheet_placement_back_button")
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back to level selection",
                    tint = LingoTokens.textPrimary
                  )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${pendingLanguage.flagEmoji} ${pendingLanguage.name} Level Check",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LingoTokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = "5 quick questions to match your starting point",
                    fontSize = 12.5.sp,
                    color = LingoTokens.textSecondary,
                    maxLines = 1
                  )
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              val eval = placementEvaluation
              if (eval == null) {
                val safeIndex = placementIndex.coerceIn(0, (placementQuestions.size - 1).coerceAtLeast(0))
                val question = placementQuestions.getOrNull(safeIndex)
                if (question != null) {
                  val progress = ((safeIndex + 1).toFloat() / placementQuestions.size.coerceAtLeast(1).toFloat())
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "Question ${safeIndex + 1} of ${placementQuestions.size}",
                      fontSize = 12.5.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = LingoTokens.brandCyan
                    )
                    Text(
                      text = "${question.cefrBand} · ${question.skillArea}",
                      fontSize = 12.sp,
                      color = LingoTokens.textSecondary
                    )
                  }
                  Spacer(modifier = Modifier.height(8.dp))
                  LinearProgressIndicator(
                    progress = { progress },
                    color = LingoTokens.brandCyan,
                    trackColor = Color.White.copy(alpha = 0.10f),
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(6.dp)
                      .clip(CircleShape)
                  )

                  Spacer(modifier = Modifier.height(14.dp))

                  Text(
                    text = question.prompt,
                    fontSize = 15.5.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LingoTokens.textPrimary
                  )

                  Spacer(modifier = Modifier.height(12.dp))

                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    question.options.forEachIndexed { idx, optionText ->
                      Surface(
                        onClick = {
                          val nextCorrect = placementCorrectCount + if (idx == question.correctIndex) 1 else 0
                          placementCorrectCount = nextCorrect
                          if (safeIndex + 1 < placementQuestions.size) {
                            placementIndex = safeIndex + 1
                          } else {
                            placementEvaluation = CurriculumData.evaluatePlacementScore(
                              correctCount = nextCorrect,
                              totalCount = placementQuestions.size
                            )
                          }
                        },
                        color = LingoTokens.bgGlassCard,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                        modifier = Modifier
                          .fillMaxWidth()
                          .heightIn(min = 50.dp)
                          .testTag("language_sheet_placement_option_$idx")
                      ) {
                        Box(
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                          contentAlignment = Alignment.CenterStart
                        ) {
                          Text(
                            text = optionText,
                            fontSize = 14.sp,
                            color = LingoTokens.textPrimary
                          )
                        }
                      }
                    }
                  }
                }
              } else {
                Surface(
                  color = LingoTokens.bgGlassCard,
                  shape = RoundedCornerShape(18.dp),
                  border = BorderStroke(1.dp, LingoTokens.brandCyan.copy(alpha = 0.45f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Text(
                      text = "Recommended level: ${eval.recommendedCefrBand}",
                      fontSize = 18.sp,
                      fontWeight = FontWeight.Bold,
                      color = LingoTokens.brandCyan,
                      modifier = Modifier.testTag("language_sheet_placement_result_band")
                    )
                    Text(
                      text = "Score: ${eval.scorePercent}% · ${eval.mappedProficiencyLevel}",
                      fontSize = 13.5.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = LingoTokens.textPrimary
                    )
                    Text(
                      text = eval.summaryFeedback,
                      fontSize = 13.5.sp,
                      lineHeight = 20.sp,
                      color = LingoTokens.textSecondary
                    )
                  }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                  onClick = {
                    onApplyPlacementEvaluation(pendingLanguage.code, eval)
                    onDismiss()
                  },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = LingoTokens.brandIndigo,
                    contentColor = Color.White
                  ),
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("language_sheet_apply_placement_button")
                ) {
                  Text(
                    text = "Apply ${eval.recommendedCefrBand} & Start ${pendingLanguage.name}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
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
