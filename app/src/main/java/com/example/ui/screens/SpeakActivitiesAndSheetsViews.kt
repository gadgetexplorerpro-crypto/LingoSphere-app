package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import com.example.ai.GeminiTutorService
import com.example.data.model.AiConversationPartner
import com.example.data.model.CallMode
import com.example.data.model.ChatMessage
import com.example.data.model.CorrectionMode
import com.example.data.model.InterviewEvaluationResult
import com.example.data.model.Language
import com.example.data.model.MessageSender
import com.example.data.model.MistakeNotebookEntry
import com.example.data.model.RoleplayEvaluationReport
import com.example.data.model.RoleplayScenario
import com.example.data.model.SavedAiConversationRecord
import com.example.data.model.SpeakingAssessmentResult
import com.example.data.model.SpeakingSession
import com.example.data.model.TutorVoice
import com.example.data.model.UserProfile
import com.example.speech.SpeechManager
import com.example.ui.components.EmojiVoice
import com.example.ui.components.Expressive2dPartnerAvatar
import com.example.ui.components.PartnerSpeakingWaveform
import com.example.ui.components.rememberLiveEmojiVoiceState
import com.example.ui.components.LingoTokens
import com.example.ui.theme.LingoAmber
import com.example.ui.theme.LingoCoral
import com.example.ui.theme.LingoCyan
import com.example.ui.theme.LingoElectricBlue
import com.example.ui.theme.LingoEmerald
import kotlinx.coroutines.launch
import java.util.UUID

internal val SpeakBgTop: Color get() = LingoTokens.bgCanvas
internal val SpeakBgBottom: Color get() = LingoTokens.bgCanvas
internal val SpeakHeaderSurface: Color get() = LingoTokens.bgElevated
internal val SpeakCardSurface: Color get() = LingoTokens.bgElevated
internal val SpeakElevatedSurface: Color get() = LingoTokens.bgSubtle
internal val SpeakSubtleBorder: Color get() = LingoTokens.borderSubtle
internal val SpeakTextPrimary: Color get() = LingoTokens.textPrimary
internal val SpeakTextSecondary: Color get() = LingoTokens.textSecondary

@Composable
internal fun SpeakSubScreenQuickBar(
  activeSection: SpeakActiveSection,
  onSelectSection: (SpeakActiveSection) -> Unit,
  onOpenMoreSheet: () -> Unit = {}
) {
  val haptics = LocalHapticFeedback.current
  val isPracticeFamily = activeSection != SpeakActiveSection.EXAM && activeSection != SpeakActiveSection.INTERVIEW

  Column(modifier = Modifier.fillMaxWidth().background(LingoTokens.bgCanvas)) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp)
        .testTag("speak_sub_segmented_bar"),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(16.dp))
          .background(LingoTokens.bgElevated)
          .border(1.dp, LingoTokens.borderSubtle, RoundedCornerShape(16.dp))
          .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        val segments = listOf(
          Triple("Practice", isPracticeFamily, "speak_subbar_overview") to { onSelectSection(SpeakActiveSection.HOME) },
          Triple("Exams", activeSection == SpeakActiveSection.EXAM, "speak_tab_assessment") to { onSelectSection(SpeakActiveSection.EXAM) },
          Triple("Interview", activeSection == SpeakActiveSection.INTERVIEW, "speak_tab_interview") to { onSelectSection(SpeakActiveSection.INTERVIEW) }
        )
        segments.forEach { (meta, onTap) ->
          val (label, selected, tag) = meta
          val textColor by animateColorAsState(
            targetValue = if (selected) LingoTokens.onGradientInk else LingoTokens.textSecondary,
            animationSpec = tween(200),
            label = "speakSubSeg_$tag"
          )
          Box(
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .clip(RoundedCornerShape(12.dp))
              .then(
                if (selected) Modifier.background(LingoTokens.brandHorizontalGradient)
                else Modifier
              )
              .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onTap()
              }
              .padding(vertical = 10.dp, horizontal = 6.dp)
              .testTag(tag),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = label,
              color = textColor,
              fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
              fontSize = 13.sp,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      Surface(
        onClick = {
          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onOpenMoreSheet()
        },
        color = LingoTokens.bgElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LingoTokens.borderSubtle),
        modifier = Modifier
          .size(48.dp)
          .testTag("speak_sub_more_btn")
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Outlined.MoreHoriz,
            contentDescription = "More speaking tools",
            tint = LingoTokens.textPrimary,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
    HorizontalDivider(color = SpeakSubtleBorder, thickness = 1.dp)
  }
}

@Composable
internal fun SpeakCompactTabPill(
  label: String,
  selected: Boolean,
  testTag: String,
  onClick: () -> Unit
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(50),
    color = if (selected) LingoCyan else SpeakCardSurface,
    border = BorderStroke(1.dp, if (selected) LingoCyan else SpeakSubtleBorder),
    modifier = Modifier.testTag(testTag)
  ) {
    Text(
      text = label,
      color = if (selected) Color(0xFF04101E) else SpeakTextSecondary,
      fontSize = 11.5.sp,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      maxLines = 1,
      softWrap = false,
      modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
    )
  }
}

@Composable
internal fun SpeakConversationSection(
  currentSelectedPartner: AiConversationPartner,
  targetLang: Language,
  nativeLangCode: String = "en",
  selectedVoice: TutorVoice,
  selectedCorrectionMode: CorrectionMode,
  activeConversationMode: com.example.ai.TutorConversationMode = com.example.ai.TutorConversationMode.FREE_CONVERSATION,
  onSelectConversationMode: (com.example.ai.TutorConversationMode) -> Unit = {},
  autoCorrectionsEnabled: Boolean,
  onToggleAutoCorrections: (Boolean) -> Unit,
  conversationMessages: List<ChatMessage>,
  conversationListState: LazyListState,
  isAiGenerating: Boolean,
  conversationErrorMessage: String? = null,
  onRetryLastMessage: () -> Unit = {},
  isListening: Boolean,
  partialSpokenText: String,
  conversationInputText: String,
  onConversationInputChange: (String) -> Unit,
  onSendMessage: (String) -> Unit,
  onToggleMic: () -> Unit,
  onOpenRescueSheet: () -> Unit,
  onOpenVoiceSheet: () -> Unit,
  onRecordGrammarMistake: (MistakeNotebookEntry) -> Unit,
  speechManager: SpeechManager,
  onBack: () -> Unit
) {
  val isSpeaking by speechManager.isSpeaking.collectAsState()
  val speechAperture by speechManager.speechAperture.collectAsState()
  val currentViseme by speechManager.currentViseme.collectAsState()

  var expandedFeedbackIds by androidx.compose.runtime.remember {
    androidx.compose.runtime.mutableStateOf<Set<String>>(emptySet())
  }
  var expandedTranslationIds by androidx.compose.runtime.remember {
    androidx.compose.runtime.mutableStateOf<Set<String>>(emptySet())
  }

  Column(modifier = Modifier.fillMaxSize()) {
    Surface(
      color = SpeakHeaderSurface,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize()
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SpeakTextPrimary)
          }

          Expressive2dPartnerAvatar(
            partner = currentSelectedPartner,
            size = 40.dp,
            isSpeaking = isSpeaking,
            speechAperture = speechAperture,
            viseme = currentViseme,
            showOnlineBadge = true,
            showWaveformBadge = isSpeaking,
            borderColor = if (isSpeaking) LingoEmerald else LingoCyan.copy(alpha = 0.55f),
            borderWidth = 1.5.dp,
            badgeBorderColor = SpeakHeaderSurface
          )

          Spacer(modifier = Modifier.width(10.dp))

          Column(
            modifier = Modifier
              .weight(1f)
              .clickable { onOpenVoiceSheet() }
          ) {
            Text(
              text = currentSelectedPartner.name,
              color = SpeakTextPrimary,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "${targetLang.name} • ${selectedVoice.displayName}",
              color = SpeakTextSecondary,
              fontSize = 11.5.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          val (speakEmojiState, speakAudioLevel) = rememberLiveEmojiVoiceState(
            speechManager = speechManager,
            isThinking = isAiGenerating
          )
          EmojiVoice(
            state = speakEmojiState,
            size = 32.dp,
            audioLevel = speakAudioLevel,
            showStateLabel = false
          )
          Spacer(modifier = Modifier.width(4.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tips", color = SpeakTextSecondary, fontSize = 11.sp)
            Switch(
              checked = autoCorrectionsEnabled,
              onCheckedChange = onToggleAutoCorrections,
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = LingoEmerald,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF334155)
              ),
              modifier = Modifier.scale(0.72f)
            )
          }
        }

        // Clean Mode Selector Strip (Free Chat / Learn / Practice / Roleplay)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          com.example.ai.TutorConversationMode.entries.forEach { mode ->
            val selected = activeConversationMode == mode
            Surface(
              onClick = { onSelectConversationMode(mode) },
              shape = RoundedCornerShape(50),
              color = if (selected) LingoElectricBlue.copy(alpha = 0.18f) else SpeakElevatedSurface,
              border = BorderStroke(1.dp, if (selected) LingoCyan.copy(alpha = 0.7f) else SpeakSubtleBorder),
              modifier = Modifier.testTag("speak_mode_chip_${mode.id}")
            ) {
              Text(
                text = mode.label,
                color = if (selected) LingoCyan else SpeakTextSecondary,
                fontSize = 11.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
              )
            }
          }
        }
      }
    }
    HorizontalDivider(color = SpeakSubtleBorder, thickness = 1.dp)

    LazyColumn(
      state = conversationListState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 14.dp),
      contentPadding = PaddingValues(vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(conversationMessages, key = { it.id }) { msg ->
        val isUser = msg.sender == MessageSender.USER
        val hasDistinctTranslation = !isUser &&
          !msg.translation.isNullOrBlank() &&
          !targetLang.code.equals(nativeLangCode, ignoreCase = true) &&
          !msg.translation.trim().equals(msg.text.trim(), ignoreCase = true)
        val isTranslationExpanded = expandedTranslationIds.contains(msg.id) ||
          (activeConversationMode == com.example.ai.TutorConversationMode.LEARN && hasDistinctTranslation)
        val hasCorrection = !isUser && autoCorrectionsEnabled && !msg.grammarFeedback.isNullOrBlank()
        val isCorrectionExpanded = if (activeConversationMode == com.example.ai.TutorConversationMode.LEARN) {
          // Learn Mode: expanded by default unless user explicitly toggles it off
          !expandedFeedbackIds.contains("collapsed_${msg.id}")
        } else {
          // Free Conversation / Practice / Roleplay: collapsed by default
          expandedFeedbackIds.contains(msg.id)
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
          Surface(
            shape = RoundedCornerShape(
              topStart = 18.dp,
              topEnd = 18.dp,
              bottomStart = if (isUser) 18.dp else 4.dp,
              bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = if (isUser) LingoElectricBlue else SpeakCardSurface,
            modifier = Modifier.widthIn(max = 316.dp)
          ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = if (isUser) "You" else currentSelectedPartner.name,
                  color = if (isUser) Color.White.copy(alpha = 0.85f) else LingoCyan,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold
                )
                if (!isUser) {
                  IconButton(
                    onClick = {
                      if (isSpeaking) {
                        speechManager.stopSpeaking()
                      } else {
                        speechManager.speakWithTutorVoice(
                          text = msg.text,
                          voice = selectedVoice,
                          languageCode = targetLang.code
                        )
                      }
                    },
                    modifier = Modifier.size(32.dp)
                  ) {
                    Icon(
                      imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                      contentDescription = if (isSpeaking) "Stop Audio" else "Listen",
                      tint = LingoCyan,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(2.dp))
              if (isUser) {
                Text(
                  text = msg.text,
                  color = Color.White,
                  fontSize = 14.5.sp,
                  lineHeight = 20.sp
                )
              } else {
                com.example.ui.components.SynchronizedAiSpeechText(
                  text = msg.text,
                  speechManager = speechManager,
                  baseTextColor = SpeakTextPrimary,
                  highlightColor = LingoCyan,
                  fontSizeSp = 15
                )
              }

              // Translation toggle pill (only shown when native & target differ and translation differs from reply)
              if (hasDistinctTranslation) {
                Spacer(modifier = Modifier.height(6.dp))
                if (isTranslationExpanded) {
                  Text(
                    text = msg.translation!!,
                    color = SpeakTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.clickable {
                      expandedTranslationIds = expandedTranslationIds - msg.id
                    }
                  )
                } else {
                  Text(
                    text = "Translate",
                    color = LingoCyan.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                      .clickable { expandedTranslationIds = expandedTranslationIds + msg.id }
                      .padding(vertical = 2.dp)
                  )
                }
              }

              // Error-only Feedback (collapsed by default in Free Chat, expanded in Learn Mode)
              if (hasCorrection) {
                Spacer(modifier = Modifier.height(8.dp))
                if (!isCorrectionExpanded) {
                  Surface(
                    onClick = { expandedFeedbackIds = expandedFeedbackIds + msg.id },
                    shape = RoundedCornerShape(10.dp),
                    color = LingoAmber.copy(alpha = 0.14f),
                    modifier = Modifier.testTag("feedback_pill_${msg.id}")
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LingoAmber, modifier = Modifier.size(13.dp))
                      Spacer(modifier = Modifier.width(5.dp))
                      Text("Quick tip • Tap to view", color = LingoAmber, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                  }
                } else {
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.25f),
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable {
                        if (activeConversationMode == com.example.ai.TutorConversationMode.LEARN) {
                          expandedFeedbackIds = expandedFeedbackIds + "collapsed_${msg.id}"
                        } else {
                          expandedFeedbackIds = expandedFeedbackIds - msg.id
                        }
                      }
                      .testTag("feedback_expanded_${msg.id}")
                  ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LingoEmerald, modifier = Modifier.size(13.dp))
                          Spacer(modifier = Modifier.width(4.dp))
                          Text("Teacher Tip", color = LingoEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                          text = "Save tip",
                          color = LingoCyan,
                          fontSize = 11.sp,
                          fontWeight = FontWeight.SemiBold,
                          modifier = Modifier.clickable {
                            onRecordGrammarMistake(
                              MistakeNotebookEntry(
                                languageCode = targetLang.code,
                                originalText = msg.text,
                                correctedText = msg.grammarFeedback!!,
                                category = "Speaking Feedback",
                                explanationNative = msg.grammarFeedback
                              )
                            )
                          }
                        )
                      }
                      Spacer(modifier = Modifier.height(3.dp))
                      Text(text = msg.grammarFeedback!!, color = SpeakTextPrimary, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                  }
                }
              }
            }
          }
        }
      }

      if (isAiGenerating) {
        item(key = "generating_indicator") {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            CircularProgressIndicator(color = LingoCyan, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("${currentSelectedPartner.name} is replying...", color = SpeakTextSecondary, fontSize = 12.sp)
          }
        }
      }
    }

    if (!conversationErrorMessage.isNullOrBlank()) {
      Surface(
        color = LingoCoral.copy(alpha = 0.16f),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 6.dp)
          .testTag("speak_ai_error_banner"),
        shape = RoundedCornerShape(12.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = conversationErrorMessage,
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f)
          )
          Spacer(modifier = Modifier.width(8.dp))
          androidx.compose.material3.TextButton(
            onClick = onRetryLastMessage,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("speak_ai_retry_button")
          ) {
            Text("Retry", color = LingoCyan, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
          }
        }
      }
    }

    HorizontalDivider(color = SpeakSubtleBorder, thickness = 1.dp)
    Surface(
      color = SpeakHeaderSurface,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp)
      ) {
        if (isListening) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            CircularProgressIndicator(color = LingoEmerald, strokeWidth = 2.dp, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (partialSpokenText.isNotBlank()) "Hearing: \"$partialSpokenText\"" else "Listening... tap mic when done",
              color = LingoEmerald,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = onOpenRescueSheet,
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(SpeakCardSurface)
              .testTag("conversation_rescue_btn")
          ) {
            Icon(Icons.Default.Lightbulb, contentDescription = "Conversation Hints", tint = LingoAmber, modifier = Modifier.size(20.dp))
          }

          Spacer(modifier = Modifier.width(6.dp))

          IconButton(
            onClick = onToggleMic,
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(if (isListening) LingoCoral else LingoElectricBlue)
              .testTag("conversation_mic_btn")
          ) {
            Icon(
              imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
              contentDescription = "Speak",
              tint = Color.White,
              modifier = Modifier.size(21.dp)
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          OutlinedTextField(
            value = conversationInputText,
            onValueChange = onConversationInputChange,
            placeholder = {
              Text(
                text = if (isListening) "Listening..." else "Message ${currentSelectedPartner.name}...",
                color = SpeakTextSecondary,
                fontSize = 13.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = SpeakTextPrimary,
              unfocusedTextColor = SpeakTextPrimary,
              focusedBorderColor = LingoCyan,
              unfocusedBorderColor = SpeakSubtleBorder,
              focusedContainerColor = SpeakCardSurface,
              unfocusedContainerColor = SpeakCardSurface
            ),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("conversation_input_field")
          )

          Spacer(modifier = Modifier.width(6.dp))

          val canSend = conversationInputText.isNotBlank() && !isAiGenerating
          IconButton(
            onClick = { onSendMessage(conversationInputText) },
            enabled = canSend,
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(if (canSend) LingoCyan else SpeakCardSurface)
              .testTag("conversation_send_btn")
          ) {
            Icon(
              Icons.AutoMirrored.Filled.Send,
              contentDescription = "Send",
              tint = if (canSend) Color(0xFF04101E) else SpeakTextSecondary,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
internal fun SpeakMissionsSection(
  activeMission: RoleplayScenario?,
  onSelectMission: (RoleplayScenario?) -> Unit,
  missionSearch: String,
  onMissionSearchChange: (String) -> Unit,
  selectedMissionCategory: String,
  onSelectMissionCategory: (String) -> Unit,
  missionMessages: List<ChatMessage>,
  missionInputText: String,
  onMissionInputChange: (String) -> Unit,
  onSendMissionMessage: (String) -> Unit,
  missionEvaluationReport: RoleplayEvaluationReport?,
  onUpdateMissionEvaluationReport: (RoleplayEvaluationReport?) -> Unit,
  isEvaluatingMission: Boolean,
  onSetEvaluatingMission: (Boolean) -> Unit,
  isAiGenerating: Boolean,
  isListening: Boolean,
  onToggleMic: () -> Unit,
  currentSelectedPartner: AiConversationPartner,
  targetLang: Language,
  nativeLang: Language,
  userProfile: UserProfile,
  onSaveSession: (SpeakingSession) -> Unit,
  onBackHome: () -> Unit
) {
  val coroutineScope = rememberCoroutineScope()

  if (activeMission == null) {
    val missionCategories = listOf("All", "Everyday Life", "Travel", "Restaurants", "Shopping", "Work", "Emergencies")
    val filteredMissions = RoleplayScenario.SCENARIOS.filter { s ->
      val catMatch = when (selectedMissionCategory) {
        "Everyday Life" -> s.id in setOf("friendly_coffee", "apartment_rental", "asking_directions")
        "Travel" -> s.id in setOf("airport_employee", "hotel_receptionist", "train_station_transit")
        "Restaurants" -> s.id in setOf("restaurant_waiter", "street_market_bargaining")
        "Shopping" -> s.id in setOf("customer_service", "bank_and_sim_setup")
        "Work" -> s.id in setOf("job_interviewer", "business_negotiation", "workplace_discussion")
        "Emergencies" -> s.id in setOf("doctor_consultation", "emergency_services", "pharmacy_urgent_care")
        else -> true
      }
      val searchMatch = missionSearch.isBlank() ||
        s.title.contains(missionSearch, ignoreCase = true) ||
        s.description.contains(missionSearch, ignoreCase = true)
      catMatch && searchMatch
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("speak_missions_list"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      item(key = "missions_top") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(onClick = onBackHome) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SpeakTextPrimary)
          }
          Column {
            Text("Speaking Missions", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = SpeakTextPrimary)
            Text("Real-world goal scenarios with ${currentSelectedPartner.name}", fontSize = 12.sp, color = SpeakTextSecondary)
          }
        }
      }

      item(key = "missions_search") {
        OutlinedTextField(
          value = missionSearch,
          onValueChange = onMissionSearchChange,
          placeholder = { Text("Search scenarios (airport, cafe, hotel...)", fontSize = 13.sp, color = SpeakTextSecondary) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LingoCyan, modifier = Modifier.size(18.dp)) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = SpeakTextPrimary,
            unfocusedTextColor = SpeakTextPrimary,
            focusedBorderColor = LingoCyan,
            unfocusedBorderColor = SpeakSubtleBorder,
            focusedContainerColor = SpeakCardSurface,
            unfocusedContainerColor = SpeakCardSurface
          ),
          modifier = Modifier.fillMaxWidth()
        )
      }

      item(key = "missions_categories") {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(missionCategories) { cat ->
            val isSel = selectedMissionCategory == cat
            FilterChip(
              selected = isSel,
              onClick = { onSelectMissionCategory(cat) },
              label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = LingoElectricBlue,
                selectedLabelColor = Color.White
              )
            )
          }
        }
      }

      items(filteredMissions, key = { it.id }) { mission ->
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = SpeakCardSurface),
          border = BorderStroke(1.dp, SpeakSubtleBorder),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectMission(mission) }
            .testTag("roleplay_card_${mission.id}")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = SpeakElevatedSurface,
              modifier = Modifier.size(44.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(text = mission.iconEmoji, fontSize = 22.sp)
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(mission.title, color = SpeakTextPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
              Text(mission.roleName, color = LingoCyan, fontSize = 11.5.sp, maxLines = 1)
              Spacer(modifier = Modifier.height(2.dp))
              Text(mission.description, color = SpeakTextSecondary, fontSize = 12.sp, maxLines = 2, lineHeight = 16.sp, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = { onSelectMission(mission) },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = LingoElectricBlue, contentColor = Color.White),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.minimumInteractiveComponentSize()
            ) {
              Text("Start", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    }
  } else {
    Column(modifier = Modifier.fillMaxSize()) {
      Surface(color = SpeakHeaderSurface, modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = { onSelectMission(null) },
            modifier = Modifier.testTag("exit_scenario_button")
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SpeakTextPrimary)
          }
          Text(text = activeMission.iconEmoji, fontSize = 20.sp)
          Spacer(modifier = Modifier.width(8.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(activeMission.title, color = SpeakTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${activeMission.roleName} • ${currentSelectedPartner.name}", color = LingoCyan, fontSize = 11.sp, maxLines = 1)
          }
          Button(
            onClick = {
              onSetEvaluatingMission(true)
              coroutineScope.launch {
                try {
                  val userTurns = missionMessages.filter { it.sender == MessageSender.USER }.map { it.text }
                  val report = GeminiTutorService.evaluateRoleplayCompletion(
                    scenarioId = activeMission.id,
                    scenarioTitle = activeMission.title,
                    taskObjective = activeMission.taskObjective,
                    userTurns = userTurns,
                    targetLanguage = targetLang,
                    nativeLanguage = nativeLang
                  )
                  onUpdateMissionEvaluationReport(report)
                  onSaveSession(
                    SpeakingSession(
                      languageCode = targetLang.code,
                      mode = "Mission",
                      scenarioTitle = activeMission.title,
                      proficiencyLevel = userProfile.proficiencyLevel,
                      turnsCount = missionMessages.size.toLong(),
                      fluencyScore = report.scorePercent.toLong(),
                      grammarFeedbackSummary = report.taskFeedback
                    )
                  )
                } finally {
                  onSetEvaluatingMission(false)
                }
              }
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LingoEmerald, contentColor = Color(0xFF04101E)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text(if (isEvaluatingMission) "Scoring..." else "Finish", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      Surface(color = SpeakElevatedSurface, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LingoEmerald, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Goal: ${activeMission.taskObjective}", color = SpeakTextPrimary, fontSize = 12.sp, lineHeight = 16.sp)
        }
      }

      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(missionMessages, key = { it.id }) { msg ->
          val isUser = msg.sender == MessageSender.USER
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
          ) {
            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = if (isUser) LingoElectricBlue else SpeakCardSurface),
              border = if (isUser) null else BorderStroke(1.dp, SpeakSubtleBorder),
              modifier = Modifier.widthIn(max = 300.dp)
            ) {
              Column(modifier = Modifier.padding(11.dp)) {
                Text(
                  text = if (isUser) "You" else activeMission.roleName,
                  color = if (isUser) Color.White.copy(alpha = 0.85f) else LingoCyan,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  msg.text,
                  color = if (isUser) Color.White else SpeakTextPrimary,
                  fontSize = 14.sp,
                  lineHeight = 19.sp
                )
              }
            }
          }
        }

        missionEvaluationReport?.let { report ->
          item(key = "mission_report") {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = SpeakCardSurface),
              border = BorderStroke(1.dp, LingoEmerald.copy(alpha = 0.6f))
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text("Mission Complete", color = LingoEmerald, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Score: ${report.scorePercent}% • ${if (report.objectiveAchieved) "Goal Achieved" else "Keep Practicing"}",
                  color = SpeakTextPrimary,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.SemiBold
                )
                Text(report.taskFeedback, color = SpeakTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                  onClick = {
                    onSelectMission(null)
                    onUpdateMissionEvaluationReport(null)
                  },
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = LingoElectricBlue)
                ) {
                  Text("Done", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }

      HorizontalDivider(color = SpeakSubtleBorder, thickness = 1.dp)
      Surface(color = SpeakHeaderSurface, modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = onToggleMic,
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(if (isListening) LingoCoral else LingoElectricBlue)
          ) {
            Icon(
              imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
              contentDescription = "Mic",
              tint = Color.White,
              modifier = Modifier.size(19.dp)
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          OutlinedTextField(
            value = missionInputText,
            onValueChange = onMissionInputChange,
            placeholder = { Text("Speak or type response...", fontSize = 13.sp, color = SpeakTextSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = SpeakTextPrimary,
              unfocusedTextColor = SpeakTextPrimary,
              focusedBorderColor = LingoCyan,
              unfocusedBorderColor = SpeakSubtleBorder,
              focusedContainerColor = SpeakCardSurface,
              unfocusedContainerColor = SpeakCardSurface
            ),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 44.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          val canSend = missionInputText.isNotBlank() && !isAiGenerating
          IconButton(
            onClick = { onSendMissionMessage(missionInputText) },
            enabled = canSend,
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(if (canSend) LingoCyan else SpeakCardSurface)
          ) {
            Icon(
              Icons.AutoMirrored.Filled.Send,
              contentDescription = "Send",
              tint = if (canSend) Color(0xFF04101E) else SpeakTextSecondary,
              modifier = Modifier.size(17.dp)
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpeakExamSection(
  examPrompts: List<String>,
  selectedExamLevel: String,
  onSelectExamLevel: (String) -> Unit,
  currentExamQuestionIndex: Int,
  onSelectExamQuestionIndex: (Int) -> Unit = {},
  onPrevExamQuestion: () -> Unit = {},
  onNextExamQuestion: () -> Unit,
  examAnswerText: String,
  onExamAnswerChange: (String) -> Unit,
  examAnswersByIndex: Map<Int, String> = emptyMap(),
  examVoiceUsedByIndex: Map<Int, Boolean> = emptyMap(),
  examSpeechSecsByIndex: Map<Int, Int> = emptyMap(),
  onRecordSpeechDuration: (Int, Int) -> Unit = { _, _ -> },
  examResult: SpeakingAssessmentResult?,
  onUpdateExamResult: (SpeakingAssessmentResult?) -> Unit,
  onRetakeExam: () -> Unit = {},
  onOpenLabById: (String) -> Unit = {},
  isEvaluatingExam: Boolean,
  onSetEvaluatingExam: (Boolean) -> Unit,
  isListening: Boolean,
  onToggleMic: () -> Unit,
  selectedVoice: TutorVoice,
  targetLang: Language,
  nativeLang: Language,
  speechManager: SpeechManager,
  onSaveSession: (SpeakingSession) -> Unit,
  onBackHome: () -> Unit
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val haptics = LocalHapticFeedback.current
  val speechError by speechManager.speechError.collectAsState()
  var examEvalError by remember { mutableStateOf<String?>(null) }
  var unscoredValidationNotice by remember { mutableStateOf<String?>(null) }
  var recordingSeconds by remember { mutableIntStateOf(0) }
  var savedToHistoryConfirmation by remember { mutableStateOf(false) }

  LaunchedEffect(isListening, currentExamQuestionIndex) {
    if (isListening) {
      recordingSeconds = 0
      while (true) {
        delay(1000L)
        recordingSeconds += 1
        onRecordSpeechDuration(currentExamQuestionIndex, 1)
      }
    } else {
      recordingSeconds = 0
    }
  }

  val promptTotal = examPrompts.size.coerceAtLeast(1)
  val minWordsForLevel = remember(selectedExamLevel) {
    com.example.data.model.SpeakingExamPromptBank.minWordsForLevel(selectedExamLevel)
  }

  // Build effective map combining per-index answers and current text field
  val effectiveAnswers = remember(examAnswersByIndex, currentExamQuestionIndex, examAnswerText) {
    val map = examAnswersByIndex.toMutableMap()
    if (examAnswerText.isNotBlank()) {
      map[currentExamQuestionIndex] = examAnswerText
    }
    map.filterValues { it.trim().isNotBlank() }
  }

  val answeredCount = effectiveAnswers.size.coerceIn(0, promptTotal)
  val answeredProgress = (answeredCount.toFloat() / promptTotal.toFloat()).coerceIn(0f, 1f)
  val currentWordCount = remember(examAnswerText) {
    examAnswerText.trim().split(Regex("\\s+")).count { it.isNotBlank() }
  }
  val allPromptsAnswered = answeredCount >= promptTotal
  val canSubmitFullExam = allPromptsAnswered && !isEvaluatingExam
  val canSubmitPartialExam = answeredCount in 1 until promptTotal && !isEvaluatingExam

  fun runExamEvaluation(isPartial: Boolean) {
    if (!com.example.data.repository.AccessPolicyManager.entitlementState.value.hasProEntitlement) {
      com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.AI_UPGRADE)
      return
    }
    if (effectiveAnswers.isEmpty() || isEvaluatingExam) return
    examEvalError = null
    unscoredValidationNotice = null
    savedToHistoryConfirmation = false
    onSetEvaluatingExam(true)
    coroutineScope.launch {
      try {
        val evaluated = GeminiTutorService.evaluateFullSpeakingExam(
          prompts = examPrompts,
          answersByPromptIndex = effectiveAnswers,
          voiceFlagsByPromptIndex = examVoiceUsedByIndex,
          speechSecondsByPromptIndex = examSpeechSecsByIndex,
          targetLanguage = targetLang,
          nativeLanguage = nativeLang,
          selectedExamLevel = selectedExamLevel,
          isPartialSubmission = isPartial
        )
        if (!evaluated.isScored) {
          onUpdateExamResult(null)
          unscoredValidationNotice = evaluated.unscoredReason
            ?: "This answer doesn't address the question. Try again with at least $minWordsForLevel words."
        } else {
          onUpdateExamResult(evaluated)
          // Only award XP / save rated CEFR session for valid full 4-prompt exams
          if (!evaluated.isPartialUnrated) {
            onSaveSession(
              SpeakingSession(
                languageCode = targetLang.code,
                mode = "Exam",
                scenarioTitle = "CEFR Speaking Exam (${evaluated.cefrBand})",
                proficiencyLevel = evaluated.cefrBand,
                turnsCount = answeredCount.toLong(),
                fluencyScore = evaluated.overallScore.toLong(),
                grammarFeedbackSummary = evaluated.grammarFeedback
              )
            )
            savedToHistoryConfirmation = true
          }
        }
      } catch (e: Exception) {
        examEvalError = com.example.ai.AiErrorHandler.handle(e).message
      } finally {
        onSetEvaluatingExam(false)
      }
    }
  }

  // Dedicated Full-Screen Result View when a valid exam result is ready
  val activeScoredResult = examResult?.takeIf { it.isScored }
  if (activeScoredResult != null) {
    val res = activeScoredResult
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("exam_result_screen"),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      item(key = "result_top_bar") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = { onUpdateExamResult(null) },
              modifier = Modifier
                .size(48.dp)
                .testTag("exam_result_back_btn")
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to exam", tint = SpeakTextPrimary)
            }
            Column {
              Text(
                text = if (res.isPartialUnrated) "Partial Exam Feedback" else "CEFR Speaking Result",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SpeakTextPrimary
              )
              Text(
                text = "${targetLang.name} · ${res.totalWordsCount} words analyzed",
                fontSize = 12.sp,
                color = SpeakTextSecondary
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(999.dp),
            color = if (res.isPartialUnrated) SpeakElevatedSurface else LingoTokens.accentGold.copy(alpha = 0.16f),
            border = BorderStroke(
              1.dp,
              if (res.isPartialUnrated) SpeakSubtleBorder else LingoTokens.accentGold.copy(alpha = 0.55f)
            )
          ) {
            Text(
              text = if (res.isPartialUnrated) "Not Rated · 0 XP" else "+45 XP Earned",
              color = if (res.isPartialUnrated) SpeakTextSecondary else LingoTokens.accentGold,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
          }
        }
      }

      // Hero Score Ring + CEFR Badge + Confidence Label + Criteria Bars
      item(key = "exam_result_card") {
        Card(
          shape = RoundedCornerShape(LingoTokens.cardRadius),
          colors = CardDefaults.cardColors(containerColor = SpeakCardSurface),
          border = BorderStroke(1.dp, LingoTokens.brandCyan.copy(alpha = 0.55f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("exam_result_card")
        ) {
          Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              SpeakScoreRing(
                scorePercent = res.overallScore.coerceIn(0, 100),
                accentColor = LingoTokens.brandCyan
              )
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    shape = RoundedCornerShape(50),
                    color = if (res.isPartialUnrated) SpeakElevatedSurface else LingoTokens.accentGold.copy(alpha = 0.16f),
                    border = BorderStroke(
                      1.dp,
                      if (res.isPartialUnrated) SpeakSubtleBorder else LingoTokens.accentGold.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("exam_result_cefr_badge")
                  ) {
                    Text(
                      text = if (res.isPartialUnrated) "Not Rated (Partial)" else "CEFR ${res.cefrBand}",
                      color = if (res.isPartialUnrated) SpeakTextSecondary else LingoTokens.accentGold,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.ExtraBold,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                  }

                  Surface(
                    shape = RoundedCornerShape(50),
                    color = LingoTokens.brandCyan.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, LingoTokens.brandCyan.copy(alpha = 0.45f)),
                    modifier = Modifier.testTag("exam_result_confidence_badge")
                  ) {
                    Text(
                      text = "Confidence: ${res.confidenceLabel}",
                      color = LingoTokens.brandCyan,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                  }
                }

                Text(
                  text = "Overall Score: ${res.overallScore}%",
                  color = SpeakTextPrimary,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.ExtraBold
                )
                Text(
                  text = if (res.showPronunciationScore) {
                    "Weighted average across Fluency, Grammar, Vocabulary, Coherence & Pronunciation"
                  } else {
                    "Typed response · Pronunciation score hidden (voice required)"
                  },
                  color = SpeakTextSecondary,
                  fontSize = 12.sp
                )
              }
            }

            HorizontalDivider(color = SpeakSubtleBorder)

            // Criteria Bars
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              val criteriaList = buildList {
                add(Triple("Fluency", res.fluencyScore, LingoTokens.brandCyan))
                add(Triple("Grammar", res.grammarAccuracyScore, LingoEmerald))
                add(Triple("Vocabulary", res.vocabularyRichnessScore, LingoTokens.brandIndigo))
                add(Triple("Coherence", res.coherenceScore, LingoAmber))
                if (res.showPronunciationScore) {
                  add(Triple("Pronunciation", res.pronunciationScore, LingoCoral))
                }
              }
              criteriaList.forEach { (label, score, barColor) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(label, color = SpeakTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("$score%", color = barColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                  }
                  LinearProgressIndicator(
                    progress = { (score.coerceIn(0, 100) / 100f) },
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(6.dp)
                      .clip(RoundedCornerShape(999.dp)),
                    color = barColor,
                    trackColor = SpeakElevatedSurface
                  )
                }
              }
            }
          }
        }
      }

      // Your Answer vs Improved Answer
      item(key = "exam_answer_comparison_section") {
        Card(
          shape = RoundedCornerShape(LingoTokens.cardRadius),
          colors = CardDefaults.cardColors(containerColor = SpeakCardSurface),
          border = BorderStroke(1.dp, SpeakSubtleBorder),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("exam_answer_comparison_section")
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "Your Answer vs Improved Answer",
              color = SpeakTextPrimary,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
            if (res.promptComparisons.isNotEmpty()) {
              res.promptComparisons.forEach { comp ->
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = SpeakElevatedSurface,
                  border = BorderStroke(1.dp, SpeakSubtleBorder),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                      text = "Prompt ${comp.promptIndex + 1}: ${comp.prompt}",
                      color = LingoTokens.brandCyan,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = "You said: \"${comp.userAnswer}\"",
                      color = SpeakTextSecondary,
                      fontSize = 13.sp
                    )
                  }
                }
              }
            }
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = LingoEmerald.copy(alpha = 0.1f),
              border = BorderStroke(1.dp, LingoEmerald.copy(alpha = 0.4f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "Improved Native Version",
                  color = LingoEmerald,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = res.improvedAnswer.ifBlank { res.correctedTranscript },
                  color = SpeakTextPrimary,
                  fontSize = 14.sp,
                  lineHeight = 20.sp
                )
              }
            }
          }
        }
      }

      // Mistakes (said -> better -> why) quoting real user words
      item(key = "exam_mistakes_section") {
        Card(
          shape = RoundedCornerShape(LingoTokens.cardRadius),
          colors = CardDefaults.cardColors(containerColor = SpeakCardSurface),
          border = BorderStroke(1.dp, SpeakSubtleBorder),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("exam_mistakes_section")
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "Mistakes & Upgrades (From Your Transcript)",
              color = LingoAmber,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
            res.mistakes.forEach { mistake ->
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = SpeakElevatedSurface,
                border = BorderStroke(1.dp, LingoAmber.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = "You said: \"${mistake.said}\"",
                    color = LingoCoral,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                  )
                  Text(
                    text = "Better: \"${mistake.better}\"",
                    color = LingoEmerald,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "Why: ${mistake.reason}",
                    color = SpeakTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                  )
                }
              }
            }
          }
        }
      }

      // Strengths with verbatim quotes
      item(key = "exam_strengths_section") {
        Card(
          shape = RoundedCornerShape(LingoTokens.cardRadius),
          colors = CardDefaults.cardColors(containerColor = SpeakCardSurface),
          border = BorderStroke(1.dp, SpeakSubtleBorder),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("exam_strengths_section")
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "Strengths (With Your Quotes)",
              color = LingoEmerald,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
            res.strengths.forEach { strength ->
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = SpeakElevatedSurface,
                border = BorderStroke(1.dp, LingoEmerald.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = strength.point,
                    color = SpeakTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "Quote: \"${strength.quote}\"",
                    color = LingoEmerald,
                    fontSize = 12.sp
                  )
                }
              }
            }
          }
        }
      }

      // Next-Step Speaking Lab Buttons
      item(key = "exam_next_steps_section") {
        Card(
          shape = RoundedCornerShape(LingoTokens.cardRadius),
          colors = CardDefaults.cardColors(containerColor = SpeakCardSurface),
          border = BorderStroke(1.dp, LingoTokens.brandCyan.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "Recommended Speaking Labs",
              color = LingoTokens.brandCyan,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
            res.nextSteps.forEach { step ->
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = SpeakElevatedSurface,
                border = BorderStroke(1.dp, LingoTokens.brandCyan.copy(alpha = 0.45f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onOpenLabById(step.labId) }
                  .testTag("exam_next_step_lab_${step.labId}")
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = step.text,
                      color = SpeakTextPrimary,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                    Text(
                      text = "Tap to open ${step.labId.replaceFirstChar { it.uppercase() }} Lab",
                      color = LingoTokens.brandCyan,
                      fontSize = 11.sp
                    )
                  }
                  Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Open lab",
                    tint = LingoTokens.brandCyan,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }
          }
        }
      }

      // Action Buttons: Retake, Save to history, Share
      item(key = "exam_result_actions") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = onRetakeExam,
            colors = ButtonDefaults.buttonColors(
              containerColor = LingoTokens.brandCyan,
              contentColor = LingoTokens.onGradientInk
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("exam_retake_btn")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Retake", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }

          OutlinedButton(
            onClick = {
              if (!savedToHistoryConfirmation) {
                onSaveSession(
                  SpeakingSession(
                    languageCode = targetLang.code,
                    mode = "Exam",
                    scenarioTitle = "CEFR Speaking Exam (${res.cefrBand})",
                    proficiencyLevel = res.cefrBand,
                    turnsCount = answeredCount.toLong(),
                    fluencyScore = res.overallScore.toLong(),
                    grammarFeedbackSummary = res.grammarFeedback
                  )
                )
                savedToHistoryConfirmation = true
                android.widget.Toast.makeText(context, "Exam result saved to history", android.widget.Toast.LENGTH_SHORT).show()
              }
            },
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, if (savedToHistoryConfirmation) LingoEmerald else SpeakSubtleBorder),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("exam_save_history_btn")
          ) {
            Icon(
              imageVector = if (savedToHistoryConfirmation) Icons.Default.Check else Icons.Default.History,
              contentDescription = null,
              tint = if (savedToHistoryConfirmation) LingoEmerald else SpeakTextPrimary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (savedToHistoryConfirmation) "Saved" else "Save",
              color = if (savedToHistoryConfirmation) LingoEmerald else SpeakTextPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }

          OutlinedButton(
            onClick = {
              val shareSummary = "LingoSphere ${targetLang.name} Speaking Exam: ${res.cefrBand} (${res.overallScore}%)"
              val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, shareSummary)
              }
              runCatching {
                context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Exam Result"))
              }
            },
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, SpeakSubtleBorder),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("exam_share_btn")
          ) {
            Text("Share", color = SpeakTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }
      }
    }
    return
  }

  // --- Exam Prompt & Answer Flow Screen (One prompt at a time) ---
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("speak_assessment_list"),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item(key = "exam_header") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBackHome,
          modifier = Modifier.size(48.dp)
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SpeakTextPrimary)
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Speaking Exam",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = SpeakTextPrimary,
            maxLines = 2
          )
          Text(
            text = "Answer all 4 prompts for your official CEFR A1–C2 score · ${targetLang.name}",
            fontSize = 12.sp,
            color = SpeakTextSecondary
          )
        }
      }
    }

    // Wrapping FlowRow Grid for Level Selector (zero horizontal overflow)
    item(key = "exam_level_chips") {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Select CEFR Level (Min $minWordsForLevel words per prompt)",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = SpeakTextSecondary
        )
        FlowRow(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("exam_level_grid"),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("Placement", "A1", "A2", "B1", "B2", "C1", "C2").forEach { lvl ->
            val isSel = selectedExamLevel == lvl
            FilterChip(
              selected = isSel,
              onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectExamLevel(lvl)
              },
              label = {
                Text(
                  text = lvl,
                  fontSize = 12.sp,
                  fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = LingoTokens.brandCyan,
                selectedLabelColor = LingoTokens.onGradientInk,
                containerColor = SpeakCardSurface,
                labelColor = SpeakTextPrimary
              ),
              modifier = Modifier.testTag("exam_level_chip_$lvl")
            )
          }
        }
      }
    }

    item(key = "exam_question_card") {
      val question = examPrompts.getOrElse(currentExamQuestionIndex) { examPrompts.first() }
      Card(
        shape = RoundedCornerShape(LingoTokens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = SpeakCardSurface),
        border = BorderStroke(1.dp, SpeakSubtleBorder)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Prompt ${currentExamQuestionIndex + 1} of $promptTotal · $answeredCount of $promptTotal answered",
                color = LingoTokens.brandCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("exam_prompt_counter")
              )
            }
            IconButton(
              onClick = { speechManager.speakWithTutorVoice(question, selectedVoice, targetLang.code) },
              modifier = Modifier.size(48.dp)
            ) {
              Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen to prompt", tint = LingoCyan, modifier = Modifier.size(20.dp))
            }
          }

          // Progress bar reflects ANSWERED prompts (not current index)
          LinearProgressIndicator(
            progress = { answeredProgress },
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(999.dp))
              .testTag("exam_progress_bar"),
            color = LingoTokens.brandCyan,
            trackColor = SpeakElevatedSurface
          )

          // Step pills 1..4 showing which prompts are answered
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            for (idx in 0 until promptTotal) {
              val isCurrent = idx == currentExamQuestionIndex
              val isAnswered = !effectiveAnswers[idx].isNullOrBlank()
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = when {
                  isCurrent -> LingoTokens.brandCyan.copy(alpha = 0.2f)
                  isAnswered -> LingoEmerald.copy(alpha = 0.16f)
                  else -> SpeakElevatedSurface
                },
                border = BorderStroke(
                  1.dp,
                  when {
                    isCurrent -> LingoTokens.brandCyan
                    isAnswered -> LingoEmerald
                    else -> SpeakSubtleBorder
                  }
                ),
                modifier = Modifier
                  .weight(1f)
                  .clickable { onSelectExamQuestionIndex(idx) }
                  .testTag("exam_prompt_pill_${idx + 1}")
              ) {
                Row(
                  modifier = Modifier.padding(vertical = 6.dp),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = if (isAnswered) "P${idx + 1} ✓" else "P${idx + 1}",
                    color = when {
                      isCurrent -> LingoTokens.brandCyan
                      isAnswered -> LingoEmerald
                      else -> SpeakTextSecondary
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }

          Text(
            text = question,
            color = SpeakTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 22.sp
          )
        }
      }
    }

    item(key = "exam_answer_card") {
      val meetsMinWords = currentWordCount >= minWordsForLevel
      Card(
        shape = RoundedCornerShape(LingoTokens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = SpeakCardSurface),
        border = BorderStroke(1.dp, SpeakSubtleBorder)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Your Response (${targetLang.name})",
              color = SpeakTextSecondary,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "$currentWordCount / $minWordsForLevel min words",
              color = if (meetsMinWords) LingoEmerald else LingoAmber,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.testTag("exam_word_count_badge")
            )
          }

          if (isListening) {
            val mins = recordingSeconds / 60
            val secs = recordingSeconds % 60
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SpeakElevatedSurface)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("exam_recording_waveform"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                PartnerSpeakingWaveform(
                  isSpeaking = true,
                  activeColor = LingoCoral,
                  modifier = Modifier.width(56.dp).height(22.dp)
                )
                Text(
                  text = "Listening in ${targetLang.name}…",
                  color = SpeakTextPrimary,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium
                )
              }
              Surface(
                shape = RoundedCornerShape(50),
                color = LingoCoral.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, LingoCoral.copy(alpha = 0.45f)),
                modifier = Modifier.testTag("exam_recording_timer")
              ) {
                Text(
                  text = String.format("REC %02d:%02d", mins, secs),
                  color = LingoCoral,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }
          }

          if (!speechError.isNullOrBlank()) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = LingoAmber.copy(alpha = 0.12f),
              border = BorderStroke(1.dp, LingoAmber.copy(alpha = 0.4f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "Microphone note: ${speechError}. You can type your answer below instead.",
                color = LingoAmber,
                fontSize = 12.sp,
                modifier = Modifier.padding(10.dp)
              )
            }
          }

          OutlinedTextField(
            value = examAnswerText,
            onValueChange = {
              unscoredValidationNotice = null
              onExamAnswerChange(it)
            },
            placeholder = {
              Text(
                "Tap Speak Answer or type at least $minWordsForLevel words in ${targetLang.name}...",
                fontSize = 14.sp,
                color = SpeakTextSecondary
              )
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = SpeakTextPrimary,
              unfocusedTextColor = SpeakTextPrimary,
              focusedBorderColor = LingoCyan,
              unfocusedBorderColor = SpeakSubtleBorder,
              focusedContainerColor = SpeakElevatedSurface,
              unfocusedContainerColor = SpeakElevatedSurface
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 104.dp)
              .testTag("speaking_assessment_input")
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Button(
              onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggleMic()
              },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = if (isListening) LingoCoral else LingoElectricBlue),
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("exam_mic_btn")
            ) {
              Icon(if (isListening) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (isListening) "Stop Mic" else "Speak Answer", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
              onClick = onPrevExamQuestion,
              enabled = currentExamQuestionIndex > 0,
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, SpeakSubtleBorder),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = SpeakTextPrimary),
              modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("exam_prev_btn")
            ) {
              Text("Prev", fontSize = 13.sp)
            }

            OutlinedButton(
              onClick = onNextExamQuestion,
              enabled = currentExamQuestionIndex < promptTotal - 1,
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, SpeakSubtleBorder),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = SpeakTextPrimary),
              modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("exam_next_btn")
            ) {
              Text("Next", fontSize = 13.sp)
            }
          }
        }
      }
    }

    // Kind validation message when an answer is off-topic, gibberish, copied, or too short
    if (!unscoredValidationNotice.isNullOrBlank()) {
      item(key = "exam_unscored_notice") {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF281C12)),
          border = BorderStroke(1.dp, LingoAmber.copy(alpha = 0.65f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("exam_unscored_notice")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = LingoAmber, modifier = Modifier.size(22.dp))
            Text(
              text = unscoredValidationNotice!!,
              color = SpeakTextPrimary,
              fontSize = 13.sp,
              lineHeight = 18.sp,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }

    if (!examEvalError.isNullOrBlank()) {
      item(key = "exam_error_card") {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF2A141C)),
          border = BorderStroke(1.dp, LingoCoral.copy(alpha = 0.55f))
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = LingoCoral, modifier = Modifier.size(20.dp))
            Text(examEvalError!!, color = SpeakTextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
            OutlinedButton(
              onClick = { runExamEvaluation(isPartial = !allPromptsAnswered) },
              modifier = Modifier.heightIn(min = 48.dp)
            ) {
              Text("Retry", color = LingoCoral, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }
        }
      }
    }

    // Primary Submit & View CEFR Score (stays disabled until all 4 prompts are answered)
    item(key = "submit_exam_btn") {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            runExamEvaluation(isPartial = false)
          },
          enabled = canSubmitFullExam,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = LingoTokens.brandCyan,
            contentColor = LingoTokens.onGradientInk,
            disabledContainerColor = SpeakElevatedSurface,
            disabledContentColor = SpeakTextSecondary
          ),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag("evaluate_speaking_btn")
        ) {
          if (isEvaluatingExam) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = LingoTokens.brandCyan
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Evaluating CEFR Score...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
          } else {
            Text(
              text = if (allPromptsAnswered) {
                "Submit & View CEFR Score"
              } else {
                "Submit & View CEFR Score ($answeredCount/$promptTotal answered)"
              },
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          }
        }

        // Secondary "Submit partial (not rated)" option when 1..3 prompts are answered
        OutlinedButton(
          onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            runExamEvaluation(isPartial = true)
          },
          enabled = canSubmitPartialExam,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(
            1.dp,
            if (canSubmitPartialExam) LingoTokens.brandCyan.copy(alpha = 0.5f) else SpeakSubtleBorder
          ),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("submit_partial_exam_btn")
        ) {
          Text(
            text = "Submit partial (not rated)",
            color = if (canSubmitPartialExam) LingoTokens.brandCyan else SpeakTextSecondary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
          )
        }
      }
    }
  }
}

@Composable
private fun SpeakScoreRing(
  scorePercent: Int,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier.size(74.dp),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val strokePx = 7.dp.toPx()
      drawArc(
        color = Color.White.copy(alpha = 0.1f),
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        style = Stroke(width = strokePx, cap = StrokeCap.Round)
      )
      drawArc(
        color = accentColor,
        startAngle = -90f,
        sweepAngle = 360f * (scorePercent.coerceIn(0, 100) / 100f),
        useCenter = false,
        style = Stroke(width = strokePx, cap = StrokeCap.Round)
      )
    }
    Text(
      text = "$scorePercent%",
      color = SpeakTextPrimary,
      fontSize = 16.sp,
      fontWeight = FontWeight.ExtraBold
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpeakInterviewSection(
  interviewIndustry: String,
  onSelectIndustry: (String) -> Unit,
  interviewRole: String,
  interviewDifficulty: String,
  onSelectDifficulty: (String) -> Unit,
  isInterviewStarted: Boolean,
  onStartInterview: () -> Unit,
  onResetInterview: () -> Unit = {},
  speakingSessions: List<SpeakingSession> = emptyList(),
  interviewMessages: MutableList<ChatMessage>,
  interviewAnswerText: String,
  onInterviewAnswerChange: (String) -> Unit,
  interviewResultReport: InterviewEvaluationResult?,
  onUpdateInterviewReport: (InterviewEvaluationResult?) -> Unit,
  isEvaluatingInterview: Boolean,
  onSetEvaluatingInterview: (Boolean) -> Unit,
  isListening: Boolean,
  onToggleMic: () -> Unit,
  targetLang: Language,
  nativeLang: Language,
  userProfile: UserProfile,
  speechManager: SpeechManager,
  onSaveSession: (SpeakingSession) -> Unit = {},
  onBackHome: () -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  val haptics = LocalHapticFeedback.current
  val speechError by speechManager.speechError.collectAsState()
  var isInterviewerTyping by remember { mutableStateOf(false) }
  var interviewError by remember { mutableStateOf<String?>(null) }
  var lastFailedInterviewAnswer by remember { mutableStateOf<String?>(null) }

  val recentInterviews = remember(speakingSessions) {
    speakingSessions.filter { it.mode.equals("Interview", ignoreCase = true) }.take(3)
  }
  val hasUserAnswered = interviewMessages.any { it.sender == MessageSender.USER }

  fun sendInterviewTurn(answerText: String) {
    val ans = answerText.trim()
    if (ans.isBlank() || isInterviewerTyping) return
    onInterviewAnswerChange("")
    interviewError = null
    lastFailedInterviewAnswer = null
    if (interviewMessages.lastOrNull()?.text != ans || interviewMessages.lastOrNull()?.sender != MessageSender.USER) {
      interviewMessages.add(ChatMessage(id = UUID.randomUUID().toString(), sender = MessageSender.USER, text = ans))
    }
    isInterviewerTyping = true
    coroutineScope.launch {
      try {
        val styleNote = when (interviewDifficulty) {
          "Friendly" -> "Warm, encouraging interviewer tone with supportive follow-ups."
          "Rigorous" -> "Senior hiring panel tone: probe deeply for metrics, trade-offs, and concrete examples."
          else -> "Professional, balanced corporate interviewer tone."
        }
        val reply = GeminiTutorService.chatWithTutor(
          userSpeechInput = ans,
          targetLanguage = targetLang,
          nativeLanguage = nativeLang,
          proficiencyLevel = userProfile.proficiencyLevel,
          scenarioTitle = "Job Interview ($interviewRole in $interviewIndustry · Style: $interviewDifficulty — $styleNote)",
          partnerName = "Michael"
        )
        interviewMessages.add(ChatMessage(id = UUID.randomUUID().toString(), sender = MessageSender.AI_TUTOR, text = reply.replyInTargetLanguage))
        speechManager.speakWithTutorVoice(reply.replyInTargetLanguage, TutorVoice.FENRIR, targetLang.code)
      } catch (e: Exception) {
        interviewError = com.example.ai.AiErrorHandler.handle(e).message
        lastFailedInterviewAnswer = ans
      } finally {
        isInterviewerTyping = false
      }
    }
  }

  fun requestInterviewReport() {
    if (!com.example.data.repository.AccessPolicyManager.entitlementState.value.hasProEntitlement) {
      com.example.ai.AiErrorHandler.requestUpgrade(com.example.ai.UpgradeSheetMode.AI_UPGRADE)
      return
    }
    val userTurns = interviewMessages.filter { it.sender == MessageSender.USER }
    if (userTurns.isEmpty() || isEvaluatingInterview) return
    interviewError = null
    onSetEvaluatingInterview(true)
    coroutineScope.launch {
      try {
        val turns = userTurns.joinToString("\n") { it.text }
        val report = GeminiTutorService.evaluateInterviewAnswer(
          profession = interviewIndustry,
          roleTitle = interviewRole,
          interviewQuestion = "$interviewDifficulty interview for $interviewRole in $interviewIndustry",
          spokenAnswer = turns,
          targetLanguage = targetLang,
          nativeLanguage = nativeLang
        )
        onUpdateInterviewReport(report)
        onSaveSession(
          SpeakingSession(
            languageCode = targetLang.code,
            mode = "Interview",
            scenarioTitle = "$interviewIndustry · $interviewDifficulty",
            proficiencyLevel = userProfile.proficiencyLevel,
            turnsCount = userTurns.size.toLong(),
            fluencyScore = report.scorePercent.toLong(),
            grammarFeedbackSummary = report.clarityAdvice
          )
        )
      } catch (e: Exception) {
        interviewError = com.example.ai.AiErrorHandler.handle(e).message
      } finally {
        onSetEvaluatingInterview(false)
      }
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("speak_interview_list"),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item(key = "interview_header") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          IconButton(
            onClick = onBackHome,
            modifier = Modifier.size(48.dp)
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SpeakTextPrimary)
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Interview Coach",
              fontSize = 22.sp,
              fontWeight = FontWeight.ExtraBold,
              color = SpeakTextPrimary,
              maxLines = 2
            )
            Text(
              text = "Practice job interviews with Michael (${targetLang.name})",
              fontSize = 12.sp,
              color = SpeakTextSecondary
            )
          }
        }
        if (isInterviewStarted) {
          OutlinedButton(
            onClick = {
              onResetInterview()
              interviewError = null
            },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SpeakSubtleBorder),
            modifier = Modifier.heightIn(min = 48.dp)
          ) {
            Text("Setup", color = SpeakTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    if (!isInterviewStarted) {
      item(key = "interview_setup_card") {
        Card(
          shape = RoundedCornerShape(LingoTokens.cardRadius),
          colors = CardDefaults.cardColors(containerColor = SpeakCardSurface),
          border = BorderStroke(1.dp, SpeakSubtleBorder)
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
              text = "Configure Mock Interview",
              color = SpeakTextPrimary,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text("Industry", color = SpeakTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              FlowRow(
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("interview_industry_flow"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                listOf("Technology & Software", "Business & Finance", "Healthcare", "Hospitality", "Sales & Marketing").forEach { ind ->
                  val isSel = interviewIndustry == ind
                  FilterChip(
                    selected = isSel,
                    onClick = {
                      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      onSelectIndustry(ind)
                    },
                    label = { Text(ind, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = LingoTokens.brandIndigo,
                      selectedLabelColor = Color.White,
                      containerColor = SpeakElevatedSurface,
                      labelColor = SpeakTextPrimary
                    )
                  )
                }
              }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text("Interviewer Style", color = SpeakTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              FlowRow(
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("interview_style_flow"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                listOf("Friendly", "Standard", "Rigorous").forEach { diff ->
                  val isSel = interviewDifficulty == diff
                  FilterChip(
                    selected = isSel,
                    onClick = {
                      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      onSelectDifficulty(diff)
                    },
                    label = { Text(diff, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = LingoTokens.brandCyan,
                      selectedLabelColor = LingoTokens.onGradientInk,
                      containerColor = SpeakElevatedSurface,
                      labelColor = SpeakTextPrimary
                    )
                  )
                }
              }
            }

            Button(
              onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onStartInterview()
              },
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = LingoTokens.brandCyan, contentColor = LingoTokens.onGradientInk),
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("start_interview_btn")
            ) {
              Text("Start Interview", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
          }
        }
      }

      // Recent Interviews (fills setup screen purposefully with real session data or helpful empty state)
      item(key = "recent_interviews_card") {
        Card(
          shape = RoundedCornerShape(LingoTokens.cardRadius),
          colors = CardDefaults.cardColors(containerColor = SpeakCardSurface),
          border = BorderStroke(1.dp, SpeakSubtleBorder),
          modifier = Modifier.testTag("recent_interviews_card")
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Recent Interviews", color = SpeakTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
              Text("${recentInterviews.size} saved", color = SpeakTextSecondary, fontSize = 12.sp)
            }
            if (recentInterviews.isEmpty()) {
              Text(
                text = "No mock interviews recorded yet. Start a $interviewDifficulty interview in $interviewIndustry to earn your first scored report.",
                color = SpeakTextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
              )
            } else {
              recentInterviews.forEach { session ->
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = SpeakElevatedSurface,
                  border = BorderStroke(1.dp, SpeakSubtleBorder),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                      Text(
                        text = session.scenarioTitle.ifBlank { "Mock Interview" },
                        color = SpeakTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                      )
                      Text(
                        text = "${session.turnsCount} answer${if (session.turnsCount == 1L) "" else "s"} · ${session.proficiencyLevel}",
                        color = SpeakTextSecondary,
                        fontSize = 12.sp
                      )
                    }
                    Surface(
                      shape = RoundedCornerShape(10.dp),
                      color = LingoTokens.brandCyan.copy(alpha = 0.14f)
                    ) {
                      Text(
                        text = "${session.fluencyScore}%",
                        color = LingoTokens.brandCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Tailored Interview Tip Card
      item(key = "interview_tip_card") {
        val tipBody = when (interviewDifficulty) {
          "Rigorous" -> "Structure answers with STAR (Situation, Task, Action, Result) and quantify your impact with numbers."
          "Friendly" -> "Focus on clear pacing, natural transitions, and connecting your past experience to $interviewIndustry."
          else -> "Keep each response to 60–90 seconds: answer the core question first, then give one concrete example."
        }
        Card(
          shape = RoundedCornerShape(LingoTokens.cardRadius),
          colors = CardDefaults.cardColors(containerColor = SpeakCardSurface),
          border = BorderStroke(1.dp, LingoTokens.brandIndigo.copy(alpha = 0.35f)),
          modifier = Modifier.testTag("interview_tip_card")
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
          ) {
            Icon(
              imageVector = Icons.Default.Lightbulb,
              contentDescription = null,
              tint = LingoTokens.accentGold,
              modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "Coach Tip · $interviewDifficulty Style",
                color = SpeakTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = tipBody,
                color = SpeakTextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
              )
            }
          }
        }
      }
    } else {
      items(interviewMessages, key = { it.id }) { msg ->
        val isUser = msg.sender == MessageSender.USER
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
          Card(
            shape = RoundedCornerShape(
              topStart = 18.dp,
              topEnd = 18.dp,
              bottomStart = if (isUser) 18.dp else 6.dp,
              bottomEnd = if (isUser) 6.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(
              containerColor = if (isUser) LingoTokens.brandIndigo.copy(alpha = 0.28f) else SpeakCardSurface
            ),
            border = BorderStroke(1.dp, if (isUser) LingoTokens.brandIndigo.copy(alpha = 0.55f) else SpeakSubtleBorder),
            modifier = Modifier.widthIn(max = 300.dp)
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = if (isUser) "You" else "Michael · $interviewDifficulty",
                  color = if (isUser) LingoTokens.brandCyan else LingoTokens.accentGold,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
                if (!isUser) {
                  IconButton(
                    onClick = { speechManager.speakWithTutorVoice(msg.text, TutorVoice.FENRIR, targetLang.code) },
                    modifier = Modifier.size(36.dp)
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                      contentDescription = "Listen to interviewer",
                      tint = LingoCyan,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = msg.text,
                color = SpeakTextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
              )
            }
          }
        }
      }

      if (isInterviewerTyping) {
        item(key = "interview_typing_indicator") {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
          ) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = SpeakCardSurface,
              border = BorderStroke(1.dp, SpeakSubtleBorder)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = LingoCyan)
                Text("Michael is preparing a follow-up…", color = SpeakTextSecondary, fontSize = 12.sp)
              }
            }
          }
        }
      }

      if (!interviewError.isNullOrBlank()) {
        item(key = "interview_error_banner") {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A141C)),
            border = BorderStroke(1.dp, LingoCoral.copy(alpha = 0.55f))
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Text(interviewError!!, color = SpeakTextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
              OutlinedButton(
                onClick = {
                  val retryMsg = lastFailedInterviewAnswer
                  if (retryMsg != null) sendInterviewTurn(retryMsg) else requestInterviewReport()
                },
                modifier = Modifier.heightIn(min = 48.dp)
              ) {
                Text("Retry", color = LingoCoral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      item(key = "interview_input_dock") {
        Card(
          shape = RoundedCornerShape(LingoTokens.cardRadius),
          colors = CardDefaults.cardColors(containerColor = SpeakCardSurface),
          border = BorderStroke(1.dp, SpeakSubtleBorder)
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!speechError.isNullOrBlank()) {
              Text(
                text = "Mic unavailable: ${speechError}. Type your answer below.",
                color = LingoAmber,
                fontSize = 12.sp
              )
            }
            OutlinedTextField(
              value = interviewAnswerText,
              onValueChange = onInterviewAnswerChange,
              placeholder = { Text("Speak or type your answer in ${targetLang.name}...", fontSize = 14.sp, color = SpeakTextSecondary) },
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SpeakTextPrimary,
                unfocusedTextColor = SpeakTextPrimary,
                focusedBorderColor = LingoCyan,
                unfocusedBorderColor = SpeakSubtleBorder,
                focusedContainerColor = SpeakElevatedSurface,
                unfocusedContainerColor = SpeakElevatedSurface
              ),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("interview_answer_input")
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = {
                  haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  onToggleMic()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isListening) LingoCoral else LingoElectricBlue),
                modifier = Modifier.heightIn(min = 48.dp)
              ) {
                Icon(if (isListening) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isListening) "Stop" else "Speak", fontSize = 13.sp, fontWeight = FontWeight.Bold)
              }
              Button(
                onClick = { sendInterviewTurn(interviewAnswerText) },
                enabled = interviewAnswerText.isNotBlank() && !isInterviewerTyping,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LingoTokens.brandCyan, contentColor = LingoTokens.onGradientInk),
                modifier = Modifier
                  .weight(1f)
                  .heightIn(min = 48.dp)
                  .testTag("interview_send_btn")
              ) {
                Text("Send", fontSize = 13.sp, fontWeight = FontWeight.Bold)
              }
              OutlinedButton(
                onClick = { requestInterviewReport() },
                enabled = hasUserAnswered && !isEvaluatingInterview,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (hasUserAnswered) LingoTokens.brandCyan.copy(alpha = 0.6f) else SpeakSubtleBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SpeakTextPrimary),
                modifier = Modifier
                  .heightIn(min = 48.dp)
                  .testTag("interview_feedback_btn")
              ) {
                Text(if (isEvaluatingInterview) "Scoring..." else "Report", fontSize = 13.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      interviewResultReport?.let { rep ->
        item(key = "interview_report_card") {
          Card(
            shape = RoundedCornerShape(LingoTokens.cardRadius),
            colors = CardDefaults.cardColors(containerColor = SpeakCardSurface),
            border = BorderStroke(1.dp, LingoCyan.copy(alpha = 0.55f)),
            modifier = Modifier.testTag("interview_report_card")
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
              ) {
                SpeakScoreRing(
                  scorePercent = rep.scorePercent.coerceIn(0, 100),
                  accentColor = LingoTokens.brandCyan
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text("Interview Report", color = LingoCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                  Text(rep.professionalRating, color = SpeakTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                  Text("$interviewIndustry · $interviewDifficulty", color = SpeakTextSecondary, fontSize = 12.sp)
                }
              }

              Text(rep.clarityAdvice, color = SpeakTextPrimary, fontSize = 14.sp, lineHeight = 20.sp)

              if (rep.enhancedProfessionalVersion.isNotBlank()) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = LingoTokens.brandCyan.copy(alpha = 0.1f),
                  border = BorderStroke(1.dp, LingoTokens.brandCyan.copy(alpha = 0.35f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Stronger Phrasing", color = LingoCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(rep.enhancedProfessionalVersion, color = SpeakTextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
                  }
                }
              }

              OutlinedButton(
                onClick = onResetInterview,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, LingoTokens.brandCyan.copy(alpha = 0.6f))
              ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = LingoCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start New Interview", color = LingoCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun SpeakPhoneModesSection(
  currentSelectedPartner: AiConversationPartner,
  onSelectPhoneMode: (CallMode) -> Unit,
  onBackHome: () -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("speak_phone_calls_list"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    item(key = "phone_header") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onBackHome) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SpeakTextPrimary)
        }
        Column {
          Text("Phone Calls", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = SpeakTextPrimary)
          Text("15 realistic phone & topic conversations", fontSize = 12.sp, color = SpeakTextSecondary)
        }
      }
    }

    items(CallMode.entries.chunked(2)) { pair ->
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        pair.forEach { mode ->
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SpeakCardSurface),
            border = BorderStroke(1.dp, SpeakSubtleBorder),
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(16.dp))
              .clickable { onSelectPhoneMode(mode) }
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(text = mode.emoji, fontSize = 22.sp)
              Spacer(modifier = Modifier.height(6.dp))
              Text(text = mode.title, color = SpeakTextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
              Spacer(modifier = Modifier.height(2.dp))
              Text(text = mode.description, color = SpeakTextSecondary, fontSize = 11.sp, maxLines = 2, lineHeight = 15.sp, overflow = TextOverflow.Ellipsis)
              Spacer(modifier = Modifier.height(8.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Call, contentDescription = null, tint = LingoCyan, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Call ${currentSelectedPartner.name}", color = LingoCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
              }
            }
          }
        }
        if (pair.size == 1) {
          Spacer(modifier = Modifier.weight(1f))
        }
      }
    }
  }
}

private data class SpeakingLabDescriptor(
  val id: String,
  val title: String,
  val description: String,
  val category: String,
  val icon: androidx.compose.ui.graphics.vector.ImageVector,
  val accentColor: Color,
  val isFeatured: Boolean = false,
  val onOpen: () -> Unit
)

@Composable
internal fun SpeakLabsSection(
  onOpenImmersionMode: () -> Unit,
  onOpenCameraLens: () -> Unit,
  onOpenConversationReplay: () -> Unit,
  onOpenAccentStudio: () -> Unit,
  onOpenLanguageEmergencyMode: () -> Unit,
  onOpenAiDebatePartner: () -> Unit,
  onOpenExplainItThreeWays: () -> Unit,
  onBackHome: () -> Unit
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val labPrefs = remember {
    context.applicationContext.getSharedPreferences("lingosphere_labs_prefs", android.content.Context.MODE_PRIVATE)
  }
  var lastUsedLabId by remember {
    mutableStateOf(labPrefs.getString("last_used_lab_id", "immersion") ?: "immersion")
  }
  var usedLabIds by remember {
    val raw = labPrefs.getString("used_lab_ids", "immersion") ?: "immersion"
    mutableStateOf(raw.split(",").filter { it.isNotBlank() }.toSet())
  }
  var selectedCategory by remember { mutableStateOf("All") }
  val categories = remember { listOf("All", "Real-life", "Skills", "Review") }

  fun launchLab(id: String, action: () -> Unit) {
    lastUsedLabId = id
    usedLabIds = usedLabIds + id
    runCatching {
      labPrefs.edit()
        .putString("last_used_lab_id", id)
        .putString("used_lab_ids", usedLabIds.joinToString(","))
        .apply()
    }
    action()
  }

  val allLabs = remember(
    onOpenImmersionMode,
    onOpenCameraLens,
    onOpenConversationReplay,
    onOpenAccentStudio,
    onOpenLanguageEmergencyMode,
    onOpenAiDebatePartner,
    onOpenExplainItThreeWays
  ) {
    listOf(
      SpeakingLabDescriptor(
        id = "immersion",
        title = "AI Immersion Mode",
        description = "Real-world scenarios with character missions & live hints",
        category = "Real-life",
        icon = Icons.Default.AutoAwesome,
        accentColor = LingoTokens.brandCyan,
        isFeatured = true,
        onOpen = { launchLab("immersion", onOpenImmersionMode) }
      ),
      SpeakingLabDescriptor(
        id = "camera",
        title = "Camera Speaking Lens",
        description = "Point at objects or scenes and describe them aloud",
        category = "Real-life",
        icon = Icons.Default.Search,
        accentColor = LingoEmerald,
        onOpen = { launchLab("camera", onOpenCameraLens) }
      ),
      SpeakingLabDescriptor(
        id = "replay",
        title = "Call Replay Studio",
        description = "Review call transcripts, better phrasing & fluency trend",
        category = "Review",
        icon = Icons.Default.History,
        accentColor = LingoTokens.brandCyan,
        onOpen = { launchLab("replay", onOpenConversationReplay) }
      ),
      SpeakingLabDescriptor(
        id = "accent",
        title = "Accent Studio",
        description = "Shadow regional voices, waveforms & minimal-pair drills",
        category = "Skills",
        icon = Icons.Default.RecordVoiceOver,
        accentColor = LingoTokens.brandIndigo,
        onOpen = { launchLab("accent", onOpenAccentStudio) }
      ),
      SpeakingLabDescriptor(
        id = "emergency",
        title = "Emergency Phrases",
        description = "Offline travel, medical & police phrases with big-text display",
        category = "Real-life",
        icon = Icons.Default.Warning,
        accentColor = LingoCoral,
        onOpen = { launchLab("emergency", onOpenLanguageEmergencyMode) }
      ),
      SpeakingLabDescriptor(
        id = "debate",
        title = "AI Debate Partner",
        description = "Defend Pro or Con positions in timed 60-second rounds",
        category = "Skills",
        icon = Icons.Default.Groups,
        accentColor = LingoAmber,
        onOpen = { launchLab("debate", onOpenAiDebatePartner) }
      ),
      SpeakingLabDescriptor(
        id = "explain",
        title = "Explain It Three Ways",
        description = "Adapt explanations across Casual, Intermediate & Pro registers",
        category = "Review",
        icon = Icons.Default.Lightbulb,
        accentColor = LingoEmerald,
        onOpen = { launchLab("explain", onOpenExplainItThreeWays) }
      )
    )
  }

  val featuredLab = allLabs.first { it.isFeatured }
  val gridLabs = remember(selectedCategory, allLabs) {
    allLabs.filter { !it.isFeatured && (selectedCategory == "All" || it.category == selectedCategory) }
  }
  val showFeaturedCard = selectedCategory == "All" || featuredLab.category == selectedCategory

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("speak_labs_list"),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item(key = "labs_header") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBackHome,
          modifier = Modifier.size(48.dp)
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SpeakTextPrimary)
        }
        Column {
          Text("Speaking Labs", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = SpeakTextPrimary)
          Text("Specialized real-life fluency and pronunciation studios", fontSize = 12.sp, color = SpeakTextSecondary)
        }
      }
    }

    // Category Chip Row (All, Real-life, Skills, Review)
    item(key = "labs_category_chips") {
      LazyRow(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("speak_labs_category_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(categories, key = { it }) { cat ->
          val isSelected = selectedCategory == cat
          FilterChip(
            selected = isSelected,
            onClick = { selectedCategory = cat },
            label = {
              Text(
                text = cat,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = LingoTokens.brandCyan,
              selectedLabelColor = LingoTokens.onGradientInk,
              containerColor = SpeakCardSurface,
              labelColor = SpeakTextPrimary
            ),
            modifier = Modifier.testTag("speak_lab_category_chip_$cat")
          )
        }
      }
    }

    // One Featured Card (AI Immersion Mode)
    if (showFeaturedCard) {
      item(key = "featured_lab_card") {
        val isLastUsed = lastUsedLabId == featuredLab.id
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF101C36)),
          border = BorderStroke(1.5.dp, LingoTokens.brandCyan.copy(alpha = 0.65f)),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { featuredLab.onOpen() }
            .testTag("lab_card_${featuredLab.id}")
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
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = LingoTokens.brandCyan.copy(alpha = 0.18f),
                  modifier = Modifier.size(44.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = featuredLab.icon,
                      contentDescription = null,
                      tint = LingoTokens.brandCyan,
                      modifier = Modifier.size(22.dp)
                    )
                  }
                }
                Column {
                  Text(
                    text = "FEATURED STUDIO",
                    color = LingoTokens.brandCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                  )
                  Text(
                    text = featuredLab.title,
                    color = SpeakTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (isLastUsed) LingoEmerald.copy(alpha = 0.16f) else LingoTokens.accentGold.copy(alpha = 0.16f),
                border = BorderStroke(
                  1.dp,
                  if (isLastUsed) LingoEmerald.copy(alpha = 0.5f) else LingoTokens.accentGold.copy(alpha = 0.5f)
                )
              ) {
                Text(
                  text = if (isLastUsed) "Last used" else "Featured",
                  color = if (isLastUsed) LingoEmerald else LingoTokens.accentGold,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
              }
            }

            Text(
              text = featuredLab.description,
              color = SpeakTextSecondary,
              fontSize = 13.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }

    // 2-Column Grid of Lab Cards (whole card tappable, vector icons, 1-line description, "Last used" or "New" tag)
    val rows = gridLabs.chunked(2)
    items(rows, key = { row -> row.joinToString("_") { it.id } }) { rowLabs ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        rowLabs.forEach { lab ->
          val tagLabel = when {
            lastUsedLabId == lab.id -> "Last used"
            lab.id !in usedLabIds -> "New"
            else -> lab.category
          }
          val tagColor = when (tagLabel) {
            "Last used" -> LingoEmerald
            "New" -> LingoTokens.brandCyan
            else -> SpeakTextSecondary
          }
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SpeakCardSurface),
            border = BorderStroke(1.dp, SpeakSubtleBorder),
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(16.dp))
              .clickable { lab.onOpen() }
              .testTag("lab_card_${lab.id}")
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
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = lab.accentColor.copy(alpha = 0.16f),
                  modifier = Modifier.size(40.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = lab.icon,
                      contentDescription = null,
                      tint = lab.accentColor,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }

                Surface(
                  shape = RoundedCornerShape(999.dp),
                  color = tagColor.copy(alpha = 0.14f),
                  border = BorderStroke(1.dp, tagColor.copy(alpha = 0.45f))
                ) {
                  Text(
                    text = tagLabel,
                    color = tagColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                  )
                }
              }

              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = lab.title,
                  color = SpeakTextPrimary,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = lab.description,
                  color = SpeakTextSecondary,
                  fontSize = 12.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
        if (rowLabs.size == 1) {
          Spacer(modifier = Modifier.weight(1f))
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpeakTutorDetailSheet(
  partner: AiConversationPartner,
  targetLang: Language,
  selectedVoice: TutorVoice,
  previewingVoiceId: String?,
  selectedTopicMode: CallMode,
  onSelectTopicMode: (CallMode) -> Unit,
  selectedCorrectionMode: CorrectionMode,
  onSelectCorrectionMode: (CorrectionMode) -> Unit,
  speechManager: SpeechManager,
  onOpenVoicePicker: () -> Unit,
  onStartVoiceConversation: (AiConversationPartner) -> Unit,
  onStartTextConversation: (AiConversationPartner) -> Unit,
  onDismiss: () -> Unit
) {
  val isSpeaking by speechManager.isSpeaking.collectAsState()
  val speechAperture by speechManager.speechAperture.collectAsState()
  val currentViseme by speechManager.currentViseme.collectAsState()
  val isPreviewing = previewingVoiceId == selectedVoice.id && isSpeaking

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = SpeakHeaderSurface,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 10.dp)
        .testTag("partner_detail_sheet"),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Expressive2dPartnerAvatar(
        partner = partner,
        size = 92.dp,
        isSpeaking = isPreviewing,
        speechAperture = speechAperture,
        viseme = currentViseme,
        showOnlineBadge = true,
        showWaveformBadge = isPreviewing,
        borderColor = if (isPreviewing) LingoEmerald else LingoCyan.copy(alpha = 0.7f),
        borderWidth = 2.5.dp,
        badgeBorderColor = SpeakHeaderSurface
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "${partner.name} • ${partner.roleTitle}",
        color = SpeakTextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
      )

      Text(
        text = "${targetLang.flagEmoji} ${targetLang.name} • ${partner.accentFlag} ${partner.accent} • CEFR ${partner.cefrRange}",
        color = LingoCyan,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 2.dp)
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "${partner.shortDescription} (${partner.personality})",
        color = SpeakTextSecondary,
        fontSize = 12.5.sp,
        lineHeight = 18.sp
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = {
            speechManager.setTutorVoice(selectedVoice)
            speechManager.previewVoice(
              voice = selectedVoice,
              languageCode = targetLang.code,
              customSampleText = partner.sampleGreeting
            )
          },
          border = BorderStroke(1.dp, if (isPreviewing) LingoEmerald else SpeakSubtleBorder),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("detail_voice_sample_btn")
        ) {
          Icon(
            imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.RecordVoiceOver,
            contentDescription = null,
            tint = if (isPreviewing) LingoEmerald else LingoCyan,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          if (isPreviewing) {
            PartnerSpeakingWaveform(
              isSpeaking = true,
              activeColor = LingoEmerald,
              barCount = 4,
              maxHeight = 12.dp
            )
            Spacer(modifier = Modifier.width(5.dp))
          }
          Text(
            text = if (isPreviewing) "Playing..." else "Play Voice",
            color = SpeakTextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp
          )
        }

        OutlinedButton(
          onClick = onOpenVoicePicker,
          border = BorderStroke(1.dp, SpeakSubtleBorder),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f)
        ) {
          Text(
            text = "Voice: ${selectedVoice.displayName}",
            color = LingoCyan,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "Topic",
        color = SpeakTextPrimary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        modifier = Modifier.align(Alignment.Start)
      )
      Spacer(modifier = Modifier.height(6.dp))
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(CallMode.CORE_15_TOPICS, key = { "det_${it.id}" }) { topic ->
          val selected = selectedTopicMode == topic
          FilterChip(
            selected = selected,
            onClick = { onSelectTopicMode(topic) },
            label = { Text("${topic.emoji} ${topic.title}", fontSize = 12.sp) }
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "Feedback Style",
        color = SpeakTextPrimary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        modifier = Modifier.align(Alignment.Start)
      )
      Spacer(modifier = Modifier.height(6.dp))
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(CorrectionMode.entries, key = { it.id }) { cm ->
          FilterChip(
            selected = selectedCorrectionMode == cm,
            onClick = { onSelectCorrectionMode(cm) },
            label = { Text(cm.title, fontSize = 11.5.sp) }
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = { onStartVoiceConversation(partner) },
        colors = ButtonDefaults.buttonColors(containerColor = LingoCyan, contentColor = Color(0xFF04101E)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("detail_start_voice_btn")
      ) {
        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Start Speaking", fontWeight = FontWeight.Bold, fontSize = 14.sp)
      }

      Spacer(modifier = Modifier.height(8.dp))

      OutlinedButton(
        onClick = { onStartTextConversation(partner) },
        border = BorderStroke(1.dp, SpeakSubtleBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(46.dp)
          .testTag("detail_start_text_btn")
      ) {
        Icon(Icons.Default.Keyboard, contentDescription = null, tint = SpeakTextPrimary, modifier = Modifier.size(17.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Start Text Chat", color = SpeakTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@Composable
internal fun SpeakPartnersDirectoryDialog(
  filteredPartners: List<AiConversationPartner>,
  currentSelectedPartner: AiConversationPartner,
  selectedVoice: TutorVoice,
  partnerSearchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  selectedLevelFilter: String,
  onSelectLevelFilter: (String) -> Unit,
  selectedAccentFilter: String,
  onSelectAccentFilter: (String) -> Unit,
  previewingVoiceId: String?,
  targetLang: Language,
  speechManager: SpeechManager,
  onSelectPartner: (AiConversationPartner) -> Unit,
  onInspectPartner: (AiConversationPartner) -> Unit,
  onOpenVoicesDialog: () -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = SpeakHeaderSurface),
      border = BorderStroke(1.dp, SpeakSubtleBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp)
        .widthIn(max = 520.dp)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Choose AI Tutor",
              color = SpeakTextPrimary,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false
            )
            Text(
              text = "12 tutors • Voice: ${selectedVoice.displayName}",
              color = SpeakTextSecondary,
              fontSize = 11.5.sp,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            onClick = onOpenVoicesDialog,
            shape = RoundedCornerShape(10.dp),
            color = SpeakCardSurface,
            border = BorderStroke(1.dp, SpeakSubtleBorder)
          ) {
            Text(
              text = "15 Voices",
              color = LingoCyan,
              fontSize = 11.5.sp,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SpeakTextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = partnerSearchQuery,
          onValueChange = onSearchQueryChange,
          placeholder = {
            Text(
              text = "Search name, accent, or role...",
              fontSize = 12.5.sp,
              color = SpeakTextSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LingoCyan, modifier = Modifier.size(18.dp)) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = SpeakTextPrimary,
            unfocusedTextColor = SpeakTextPrimary,
            focusedBorderColor = LingoCyan,
            unfocusedBorderColor = SpeakSubtleBorder,
            focusedContainerColor = SpeakCardSurface,
            unfocusedContainerColor = SpeakCardSurface
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("speak_partner_search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          items(listOf("All", "Beginner", "Intermediate", "Advanced")) { lvl ->
            val active = selectedLevelFilter == lvl
            FilterChip(
              selected = active,
              onClick = { onSelectLevelFilter(lvl) },
              label = {
                Text(
                  text = lvl,
                  fontSize = 11.sp,
                  maxLines = 1,
                  softWrap = false
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = LingoCyan.copy(alpha = 0.2f),
                selectedLabelColor = LingoCyan
              )
            )
          }
          items(listOf("American", "British", "Australian")) { acc ->
            val active = selectedAccentFilter == acc
            FilterChip(
              selected = active,
              onClick = { onSelectAccentFilter(if (active) "All" else acc) },
              label = {
                Text(
                  text = acc,
                  fontSize = 11.sp,
                  maxLines = 1,
                  softWrap = false
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = LingoElectricBlue.copy(alpha = 0.25f),
                selectedLabelColor = SpeakTextPrimary
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val isSpeaking by speechManager.isSpeaking.collectAsState()
        val speechAperture by speechManager.speechAperture.collectAsState()
        val currentViseme by speechManager.currentViseme.collectAsState()

        LazyColumn(
          modifier = Modifier
            .heightIn(max = 420.dp)
            .testTag("partners_directory_list"),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(filteredPartners, key = { "dir_${it.id}" }) { partner ->
            val isSelected = currentSelectedPartner.id == partner.id
            val isPreviewing = previewingVoiceId == partner.voice.id && isSpeaking
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (isSelected) SpeakElevatedSurface else SpeakCardSurface
              ),
              border = BorderStroke(
                width = if (isSelected || isPreviewing) 1.5.dp else 1.dp,
                color = when {
                  isPreviewing -> LingoEmerald
                  isSelected -> LingoCyan
                  else -> SpeakSubtleBorder
                }
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  speechManager.setTutorVoice(partner.voice)
                  onSelectPartner(partner)
                }
                .testTag("partner_card_${partner.id}")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.Top
              ) {
                Expressive2dPartnerAvatar(
                  partner = partner,
                  size = 52.dp,
                  isSpeaking = isPreviewing,
                  speechAperture = speechAperture,
                  viseme = currentViseme,
                  showOnlineBadge = true,
                  showWaveformBadge = isPreviewing,
                  borderColor = when {
                    isPreviewing -> LingoEmerald
                    isSelected -> LingoCyan
                    else -> SpeakSubtleBorder
                  },
                  borderWidth = if (isSelected || isPreviewing) 2.dp else 1.dp,
                  badgeBorderColor = if (isSelected) SpeakElevatedSurface else SpeakCardSurface,
                  modifier = Modifier
                    .padding(top = 2.dp)
                    .clickable { onInspectPartner(partner) }
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                  // Row 1: Tutor Name + CEFR Level Badge on left, Voice Preview button on right
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                      modifier = Modifier.weight(1f)
                    ) {
                      Text(
                        text = partner.name,
                        color = SpeakTextPrimary,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                      )
                      Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = Color.White.copy(alpha = 0.08f)
                      ) {
                        Text(
                          text = partner.cefrRange,
                          color = SpeakTextPrimary.copy(alpha = 0.9f),
                          fontSize = 10.sp,
                          fontWeight = FontWeight.SemiBold,
                          maxLines = 1,
                          softWrap = false,
                          modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                      }
                      if (isSelected) {
                        Icon(
                          imageVector = Icons.Default.CheckCircle,
                          contentDescription = "Selected Partner",
                          tint = LingoCyan,
                          modifier = Modifier.size(15.dp)
                        )
                      }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                      onClick = {
                        speechManager.setTutorVoice(partner.voice)
                        speechManager.previewVoice(
                          voice = partner.voice,
                          languageCode = targetLang.code,
                          customSampleText = partner.sampleGreeting
                        )
                      },
                      shape = RoundedCornerShape(9.dp),
                      color = if (isPreviewing) LingoEmerald.copy(alpha = 0.18f) else SpeakElevatedSurface,
                      border = BorderStroke(
                        1.dp,
                        if (isPreviewing) LingoEmerald else LingoCyan.copy(alpha = 0.45f)
                      ),
                      modifier = Modifier.testTag("preview_voice_btn_${partner.id}")
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Icon(
                          imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                          contentDescription = "Play Voice",
                          tint = if (isPreviewing) LingoEmerald else LingoCyan,
                          modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        if (isPreviewing) {
                          PartnerSpeakingWaveform(
                            isSpeaking = true,
                            activeColor = LingoEmerald,
                            barCount = 4,
                            maxHeight = 10.dp
                          )
                        } else {
                          Text(
                            text = "Voice",
                            color = SpeakTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                          )
                        }
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(4.dp))

                  // Row 2: Role badge with full column width so words never break mid-word
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = LingoCyan.copy(alpha = 0.14f)
                  ) {
                    Text(
                      text = partner.roleTitle,
                      color = LingoCyan,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold,
                      maxLines = 1,
                      softWrap = false,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                  }

                  Spacer(modifier = Modifier.height(4.dp))

                  // Row 3: Supported Accent + Configured Voice + Gender Presentation
                  Text(
                    text = "${partner.accentFlag} ${partner.accent} • ${partner.voice.displayName} (${partner.gender})",
                    color = LingoCyan.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )

                  Spacer(modifier = Modifier.height(3.dp))

                  // Row 4: Personality & Teaching Style Description
                  Text(
                    text = partner.shortDescription,
                    color = SpeakTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 14.5.sp,
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

@Composable
internal fun SpeakVoicesSelectionDialog(
  selectedVoice: TutorVoice,
  @Suppress("UNUSED_PARAMETER") previewingVoiceId: String?,
  targetLang: Language,
  speechManager: SpeechManager,
  onSelectVoice: (TutorVoice) -> Unit,
  onDismiss: () -> Unit
) {
  com.example.ui.components.SharedVoiceSelectorBottomSheet(
    title = "AI Partner Voice",
    subtitle = "Compare voices on one sample sentence and apply to your AI partner",
    selectedVoice = selectedVoice,
    targetLang = targetLang,
    speechManager = speechManager,
    onApplyVoice = onSelectVoice,
    onDismiss = onDismiss
  )
}

@Composable
internal fun SpeakGroupPartnersDialog(
  selectedGroupPartnerIds: List<String>,
  onToggleGroupPartner: (String) -> Unit,
  onStartGroupCall: () -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = SpeakHeaderSurface),
      border = BorderStroke(1.dp, SpeakSubtleBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Group Conversation", color = SpeakTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Choose 2 or 3 AI tutors (${selectedGroupPartnerIds.size} selected)", color = SpeakTextSecondary, fontSize = 12.sp)
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SpeakTextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
          modifier = Modifier.heightIn(max = 320.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(AiConversationPartner.ALL_PARTNERS, key = { "grp_${it.id}" }) { partner ->
            val checked = partner.id in selectedGroupPartnerIds
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (checked) SpeakElevatedSurface else SpeakCardSurface,
              border = BorderStroke(1.dp, if (checked) LingoCyan.copy(alpha = 0.6f) else SpeakSubtleBorder),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleGroupPartner(partner.id) }
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Expressive2dPartnerAvatar(
                  partner = partner,
                  size = 42.dp,
                  isSpeaking = false,
                  borderColor = if (checked) LingoCyan else SpeakSubtleBorder,
                  borderWidth = 1.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(partner.name, color = SpeakTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                  Text("${partner.accentFlag} ${partner.accent} • ${partner.voice.displayName}", color = SpeakTextSecondary, fontSize = 11.sp)
                }
                if (checked) {
                  Icon(Icons.Default.CheckCircle, contentDescription = "Included", tint = LingoCyan, modifier = Modifier.size(20.dp))
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
          onClick = onStartGroupCall,
          enabled = selectedGroupPartnerIds.size >= 2,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = LingoCyan, contentColor = Color(0xFF04101E)),
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
        ) {
          Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Start Group Session", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    }
  }
}

@Composable
internal fun SpeakLanguagePickerDialog(
  currentLanguageCode: String,
  onSelectLanguageCode: (String) -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = SpeakHeaderSurface),
      border = BorderStroke(1.dp, SpeakSubtleBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Speaking Language", color = SpeakTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SpeakTextSecondary)
          }
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
          modifier = Modifier.heightIn(max = 340.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(Language.SUPPORTED_LANGUAGES, key = { it.code }) { lang ->
            val isSel = lang.code.equals(currentLanguageCode, ignoreCase = true)
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSel) SpeakElevatedSurface else SpeakCardSurface,
              border = BorderStroke(1.dp, if (isSel) LingoCyan.copy(alpha = 0.6f) else SpeakSubtleBorder),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectLanguageCode(lang.code) }
            ) {
              Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = lang.flagEmoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = lang.name,
                  color = SpeakTextPrimary,
                  fontSize = 14.sp,
                  fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(modifier = Modifier.weight(1f))
                if (isSel) {
                  Icon(Icons.Default.Check, contentDescription = "Selected", tint = LingoCyan, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpeakConversationRescueSheet(
  currentSelectedPartner: AiConversationPartner,
  targetLang: Language,
  lastTutorPrompt: String,
  isGeneratingRescue: Boolean,
  onSetGeneratingRescue: (Boolean) -> Unit,
  rescueSheetContent: String?,
  onUpdateRescueContent: (String?) -> Unit,
  onUsePhrase: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = SpeakHeaderSurface
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Lightbulb, contentDescription = null, tint = LingoAmber, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Speaking Assist", color = SpeakTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = SpeakTextSecondary)
        }
      }

      val rescueOptions = listOf(
        "Suggest a natural reply",
        "Simplify the question",
        "Translate to my language",
        "Give a speaking hint"
      )

      rescueOptions.forEach { opt ->
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = SpeakCardSurface,
          border = BorderStroke(1.dp, SpeakSubtleBorder),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              onSetGeneratingRescue(true)
              coroutineScope.launch {
                try {
                  val prompt = "The learner needs help: '$opt' for ${currentSelectedPartner.name}'s prompt: '$lastTutorPrompt' in ${targetLang.name}."
                  val res = GeminiTutorService.generateGeminiText(
                    systemInstructionText = "You are a concise, supportive language tutor.",
                    userPromptText = prompt,
                    temperature = 0.5f
                  )
                  onUpdateRescueContent(res?.trim() ?: "Try saying a short greeting in ${targetLang.name}!")
                } finally {
                  onSetGeneratingRescue(false)
                }
              }
            }
        ) {
          Text(
            text = opt,
            color = SpeakTextPrimary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
          )
        }
      }

      if (isGeneratingRescue) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
          CircularProgressIndicator(color = LingoCyan, strokeWidth = 2.dp, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Preparing suggestion...", color = SpeakTextSecondary, fontSize = 12.sp)
        }
      }

      rescueSheetContent?.let { content ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = SpeakCardSurface),
          border = BorderStroke(1.dp, LingoCyan.copy(alpha = 0.4f))
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(content, color = SpeakTextPrimary, fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
              onClick = { onUsePhrase(content) },
              colors = ButtonDefaults.buttonColors(containerColor = LingoCyan, contentColor = Color(0xFF04101E)),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("Use in Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
    }
  }
}

@Composable
internal fun SpeakSavedSessionsDialog(
  savedAiConversations: List<SavedAiConversationRecord>,
  speakingSessions: List<SpeakingSession>,
  onResumeSavedRecord: (SavedAiConversationRecord) -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = SpeakHeaderSurface),
      border = BorderStroke(1.dp, SpeakSubtleBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, contentDescription = null, tint = LingoCyan, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Recent Speaking Sessions", color = SpeakTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SpeakTextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
          modifier = Modifier.heightIn(max = 320.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (savedAiConversations.isNotEmpty()) {
            items(savedAiConversations, key = { "rec_${it.id}" }) { record ->
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = SpeakCardSurface,
                border = BorderStroke(1.dp, SpeakSubtleBorder),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onResumeSavedRecord(record) }
              ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text("${record.partnerName} • ${record.topicTitle}", color = SpeakTextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                    Text("${record.languageName} • ${record.cefrLevel}", color = SpeakTextSecondary, fontSize = 11.5.sp)
                  }
                  Text("Resume", color = LingoCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          } else {
            items(speakingSessions.take(10), key = { "${it.scenarioTitle}_${it.turnsCount}_${it.hashCode()}" }) { sess ->
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = SpeakCardSurface,
                border = BorderStroke(1.dp, SpeakSubtleBorder),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(sess.scenarioTitle, color = SpeakTextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${sess.mode} • Fluency ${sess.fluencyScore}%", color = SpeakTextSecondary, fontSize = 11.5.sp)
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
