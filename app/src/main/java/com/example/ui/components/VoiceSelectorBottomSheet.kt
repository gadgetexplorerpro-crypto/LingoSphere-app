package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.data.model.Language
import com.example.data.model.TutorVoice
import com.example.speech.SpeechManager

enum class VoiceGenderFilter(val label: String, val tagSuffix: String) {
  ALL("All", "all"),
  FEMALE("Female", "female"),
  MALE("Male", "male")
}

private val SheetBg = Color(0xFF0B1328)
private val SheetCard = Color(0xFF13203F)
private val SheetElevated = Color(0xFF1A2B54)
private val SheetBorder = Color(0xFF283E73)
private val AccentCyan = Color(0xFF06B6D4)
private val AccentEmerald = Color(0xFF10B981)
private val AccentAmber = Color(0xFFF59E0B)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentRose = Color(0xFFF43F5E)

/**
 * Unified AI Voice & Narrator Selector bottom sheet reused across:
 * - AI Partner (SpeakScreen, Live Call Settings)
 * - Audiobook Listening & Reading (BookDetailScreen, AudiobookPlayerScreen, CreateMyListeningSheet)
 *
 * Uses one consistent preview sentence in the active language so users can accurately compare
 * genuine acoustic differences across all supported provider voices.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedVoiceSelectorBottomSheet(
  title: String = "Choose AI Voice",
  subtitle: String = "Tap Preview to compare voices on the same sentence",
  selectedVoice: TutorVoice,
  isHumanNarratorSelected: Boolean = false,
  includeHumanNarratorOption: Boolean = false,
  humanNarratorAvailable: Boolean = true,
  targetLang: Language,
  speechManager: SpeechManager,
  onSelectHumanNarrator: (() -> Unit)? = null,
  onApplyVoice: (TutorVoice) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var draftVoice by remember(selectedVoice.id) { mutableStateOf(selectedVoice) }
  var genderFilter by remember { mutableStateOf(VoiceGenderFilter.ALL) }

  val previewingVoiceId by speechManager.previewingVoiceId.collectAsState()
  val synthesizingVoiceId by speechManager.synthesizingVoiceId.collectAsState()
  val voiceError by speechManager.voiceError.collectAsState()

  val supportedVoices = remember(targetLang.code) {
    speechManager.getSupportedVoicesForLanguage(targetLang.code)
  }
  val filteredVoices = remember(supportedVoices, genderFilter) {
    when (genderFilter) {
      VoiceGenderFilter.ALL -> supportedVoices
      VoiceGenderFilter.FEMALE -> supportedVoices.filter { it.gender.equals("Female", ignoreCase = true) }
      VoiceGenderFilter.MALE -> supportedVoices.filter { it.gender.equals("Male", ignoreCase = true) }
    }
  }
  val consistentSample = remember(targetLang.code) {
    TutorVoice.consistentPreviewSentence(targetLang.code)
  }

  DisposableEffect(Unit) {
    onDispose {
      speechManager.clearVoiceError()
    }
  }

  ModalBottomSheet(
    onDismissRequest = {
      speechManager.stopSpeaking()
      speechManager.clearVoiceError()
      onDismiss()
    },
    sheetState = sheetState,
    containerColor = SheetBg,
    contentColor = Color.White,
    modifier = Modifier.testTag("shared_voice_selector_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "$subtitle (${supportedVoices.size} distinct voices • ${targetLang.name})",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
          )
        }
        IconButton(
          onClick = {
            speechManager.stopSpeaking()
            speechManager.clearVoiceError()
            onDismiss()
          },
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("close_voice_selector_btn")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close voice selector",
            tint = Color(0xFFCBD5E1)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Gender filter chips: All | Female | Male
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        val femaleCount = supportedVoices.count { it.gender.equals("Female", ignoreCase = true) }
        val maleCount = supportedVoices.count { it.gender.equals("Male", ignoreCase = true) }
        VoiceGenderFilter.entries.forEach { filter ->
          val count = when (filter) {
            VoiceGenderFilter.ALL -> supportedVoices.size
            VoiceGenderFilter.FEMALE -> femaleCount
            VoiceGenderFilter.MALE -> maleCount
          }
          val isSelected = genderFilter == filter
          FilterChip(
            selected = isSelected,
            onClick = { genderFilter = filter },
            label = {
              Text(
                text = "${filter.label} ($count)",
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = AccentCyan.copy(alpha = 0.22f),
              selectedLabelColor = AccentCyan,
              containerColor = SheetCard,
              labelColor = Color(0xFFCBD5E1)
            ),
            modifier = Modifier
              .height(48.dp)
              .testTag("voice_filter_${filter.tagSuffix}")
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Consistent preview sentence callout
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = SheetCard,
        border = BorderStroke(1.dp, SheetBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = null,
            tint = AccentCyan,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Preview phrase: \"$consistentSample\"",
            color = Color(0xFFCBD5E1),
            fontSize = 11.5.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("consistent_preview_phrase_text")
          )
        }
      }

      // Preview error banner if TTS provider unavailable
      if (!voiceError.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = AccentRose.copy(alpha = 0.16f),
          border = BorderStroke(1.dp, AccentRose.copy(alpha = 0.65f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("voice_preview_error_banner")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.ErrorOutline,
              contentDescription = "Preview error",
              tint = AccentRose,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = voiceError.orEmpty(),
              color = Color.White,
              fontSize = 12.sp,
              modifier = Modifier.weight(1f)
            )
            IconButton(
              onClick = { speechManager.clearVoiceError() },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(Icons.Default.Close, contentDescription = "Dismiss error", tint = Color.White, modifier = Modifier.size(16.dp))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Scrollable voice catalog
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f, fill = false)
          .heightIn(max = 360.dp)
          .testTag("voice_selector_list"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
      ) {
        if (includeHumanNarratorOption && onSelectHumanNarrator != null) {
          item(key = "human_librivox_option") {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (isHumanNarratorSelected) AccentAmber.copy(alpha = 0.16f) else SheetCard,
              border = BorderStroke(
                width = if (isHumanNarratorSelected) 1.5.dp else 1.dp,
                color = if (isHumanNarratorSelected) AccentAmber else SheetBorder
              ),
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .clickable(enabled = humanNarratorAvailable) {
                  speechManager.stopSpeaking()
                  speechManager.clearVoiceError()
                  onSelectHumanNarrator()
                }
                .testTag("voice_option_human_narrator")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.RecordVoiceOver,
                  contentDescription = "Human Narrator",
                  tint = if (humanNarratorAvailable) AccentAmber else Color(0xFF64748B),
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Human Narrator",
                    color = if (humanNarratorAvailable) Color.White else Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = if (humanNarratorAvailable) {
                      "Original human-narrated audiobook recording"
                    } else {
                      "Human narration unavailable for this chapter"
                    },
                    color = if (humanNarratorAvailable) AccentAmber else Color(0xFF64748B),
                    fontSize = 11.5.sp
                  )
                }
                if (isHumanNarratorSelected) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = AccentAmber,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }
          }
        }

        items(filteredVoices, key = { "shared_voice_${it.id}" }) { voice ->
          val isSelected = !isHumanNarratorSelected && draftVoice.id == voice.id
          val isPreviewing = previewingVoiceId == voice.id
          val isSynthesizing = synthesizingVoiceId == voice.id
          val isMale = voice.gender.equals("Male", ignoreCase = true)

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isSelected) SheetElevated else SheetCard,
            border = BorderStroke(
              width = if (isSelected) 1.5.dp else 1.dp,
              color = if (isSelected) AccentCyan else SheetBorder
            ),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 68.dp)
              .clickable {
                draftVoice = voice
                speechManager.clearVoiceError()
              }
              .testTag("voice_chip_${voice.id}")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Text(
                    text = "${voice.personaName} (${voice.id})",
                    color = Color.White,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                  )
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isMale) AccentCyan.copy(alpha = 0.16f) else AccentPurple.copy(alpha = 0.18f)
                  ) {
                    Text(
                      text = voice.gender,
                      color = if (isMale) AccentCyan else Color(0xFFC4B5FD),
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.08f)
                  ) {
                    Text(
                      text = voice.styleCategory,
                      color = Color(0xFFCBD5E1),
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Medium,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                  text = "${voice.accentLabel} • ${voice.styleTag}",
                  color = AccentEmerald,
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = voice.description,
                  color = Color(0xFF94A3B8),
                  fontSize = 11.5.sp,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
              }

              Spacer(modifier = Modifier.width(8.dp))

              // Preview button (>= 48dp touch target)
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(
                    when {
                      isPreviewing -> AccentEmerald.copy(alpha = 0.22f)
                      isSynthesizing -> AccentAmber.copy(alpha = 0.18f)
                      else -> AccentCyan.copy(alpha = 0.16f)
                    }
                  )
                  .clickable {
                    draftVoice = voice
                    speechManager.previewVoice(
                      voice = voice,
                      languageCode = targetLang.code,
                      customSampleText = consistentSample
                    )
                  }
                  .testTag("voice_preview_btn_${voice.id}"),
                contentAlignment = Alignment.Center
              ) {
                if (isSynthesizing && !isPreviewing) {
                  CircularProgressIndicator(
                    color = AccentAmber,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                  )
                } else {
                  Icon(
                    imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPreviewing) "Stop preview of ${voice.personaName}" else "Preview ${voice.personaName}",
                    tint = if (isPreviewing) AccentEmerald else AccentCyan,
                    modifier = Modifier.size(22.dp)
                  )
                }
              }

              if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = "Selected voice",
                  tint = AccentCyan,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = SheetBorder)
      Spacer(modifier = Modifier.height(10.dp))

      // Selected Voice Summary + Apply Voice Button
      Text(
        text = "Selected: ${draftVoice.personaName} (${draftVoice.id}) • ${draftVoice.gender} • ${draftVoice.accentLabel}",
        color = AccentCyan,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(8.dp))
      Button(
        onClick = {
          speechManager.stopSpeaking()
          speechManager.clearVoiceError()
          onApplyVoice(draftVoice)
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = AccentCyan,
          contentColor = Color(0xFF041220)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("apply_voice_btn")
      ) {
        Text(
          text = "Apply Voice (${draftVoice.personaName})",
          fontSize = 15.sp,
          fontWeight = FontWeight.ExtraBold
        )
      }
      Spacer(modifier = Modifier.height(14.dp))
    }
  }
}
