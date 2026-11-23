package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ListeningAcademyItem
import com.example.data.model.ReadingAcademyPassage
import com.example.data.model.ReadingWordGloss
import com.example.speech.SpeechManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val AcadBgCard: Color
  get() = if (com.example.ui.theme.LingoThemeController.isDarkActive) Color(0xFF0E172A) else Color(0xFFFFFFFF)
private val AcadBgElevated: Color
  get() = if (com.example.ui.theme.LingoThemeController.isDarkActive) Color(0xFF15223C) else Color(0xFFF8FAFC)
private val AcadBorder: Color
  get() = if (com.example.ui.theme.LingoThemeController.isDarkActive) Color(0xFF1E3054) else Color(0xFFCBD5E1)
private val AcadCyan: Color
  get() = if (com.example.ui.theme.LingoThemeController.isDarkActive) Color(0xFF38BDF8) else Color(0xFF0284C7)
private val AcadIndigo = Color(0xFF818CF8)
private val AcadEmerald = Color(0xFF34D399)
private val AcadAmber = Color(0xFFFBBF24)
private val AcadRose = Color(0xFFF87171)
private val AcadTextPrimary: Color
  get() = if (com.example.ui.theme.LingoThemeController.isDarkActive) Color(0xFFF8FAFC) else Color(0xFF0F172A)
private val AcadTextSecondary: Color
  get() = if (com.example.ui.theme.LingoThemeController.isDarkActive) Color(0xFF94A3B8) else Color(0xFF475569)

enum class ReadingFontScale(val label: String, val bodySizeSp: Int) {
  SMALL("A-", 14),
  MEDIUM("A", 16),
  LARGE("A+", 19),
  EXTRA_LARGE("A++", 22)
}

enum class ReadingLineSpacing(val label: String, val lineHeightMultiplier: Float, val wordRowSpacingDp: Int) {
  COMPACT("Compact", 1.25f, 3),
  COMFORTABLE("Normal", 1.45f, 5),
  SPACIOUS("Wide", 1.7f, 8)
}

enum class ReadingWidthMode(val label: String, val horizontalPaddingDp: Int) {
  COMPACT("Focused", 20),
  STANDARD("Balanced", 12),
  WIDE("Full Width", 6)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListeningAcademySection(
  items: List<ListeningAcademyItem>,
  completedIds: Set<String>,
  speechManager: SpeechManager?,
  listState: LazyListState,
  selectedItemId: String,
  onSelectItemId: (String) -> Unit,
  savedLinePositions: Map<String, Int>,
  onUpdateLinePosition: (itemId: String, lineIdx: Int) -> Unit,
  onCompleteItem: (id: String, title: String, scorePercent: Int, xpBonus: Int) -> Unit,
  onSaveVocabToFlashcard: (front: String, back: String, phonetic: String) -> Unit,
  onGenerateNewListening: ((cefrLevel: String, category: String, topic: String) -> Unit)? = null,
  isGeneratingListening: Boolean = false,
  generationErrorMessage: String? = null
) {
  var selectedCefrFilter by remember { mutableStateOf("All") }
  var searchListeningQuery by remember { mutableStateOf("") }
  var showGenerateDialog by remember { mutableStateOf(false) }
  var genCefr by remember { mutableStateOf("B1") }
  var genCategory by remember { mutableStateOf("Everyday Dialogue") }
  var genScenarioTopic by remember { mutableStateOf("") }

  val filteredItems = remember(items, selectedCefrFilter, searchListeningQuery) {
    items.filter { item ->
      val matchesCefr = selectedCefrFilter == "All" || item.cefrLevel.equals(selectedCefrFilter, ignoreCase = true)
      val q = searchListeningQuery.trim()
      val matchesSearch = q.isBlank() || item.title.contains(q, ignoreCase = true) || item.category.contains(q, ignoreCase = true) || item.lines.any { it.targetText.contains(q, ignoreCase = true) }
      matchesCefr && matchesSearch
    }
  }
  val activeItem = filteredItems.firstOrNull { it.id == selectedItemId }
    ?: filteredItems.firstOrNull()
    ?: items.firstOrNull()

  var selectedAccent by remember(activeItem?.id) {
    mutableStateOf(activeItem?.regionalAccents?.firstOrNull() ?: "Standard")
  }
  var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
  var showTranslation by remember { mutableStateOf(true) }
  var isPlaying by remember(activeItem?.id) { mutableStateOf(false) }

  val totalLines = activeItem?.lines?.size?.coerceAtLeast(1) ?: 1
  val currentLineIndex = if (activeItem != null) {
    (savedLinePositions[activeItem.id] ?: 0).coerceIn(0, totalLines - 1)
  } else {
    0
  }

  var quizAnswers by remember(activeItem?.id) { mutableStateOf<Map<Int, Int>>(emptyMap()) }
  var quizSubmitted by remember(activeItem?.id) { mutableStateOf(false) }
  var savedTerms by remember { mutableStateOf(setOf<String>()) }

  val speakLineAtIndex: (Int) -> Unit = { idx ->
    if (activeItem != null) {
      val line = activeItem.lines.getOrNull(idx)
      if (line != null) {
        val accentPitchDelta = when {
          selectedAccent.contains("Mexico", ignoreCase = true) || selectedAccent.contains("Canada", ignoreCase = true) -> 0.04f
          selectedAccent.contains("Colombia", ignoreCase = true) || selectedAccent.contains("Belgium", ignoreCase = true) -> -0.04f
          else -> 0.0f
        }
        val speakerPitch = (if (idx % 2 == 0) 1.0f else 0.94f) + accentPitchDelta
        speechManager?.speakWithVoice(
          text = line.targetText,
          languageCode = activeItem.languageCode,
          pitch = speakerPitch.coerceIn(0.8f, 1.2f),
          speed = playbackSpeed
        )
      }
    }
  }

  // Automatic sentence-by-sentence playback loop when isPlaying is true
  LaunchedEffect(isPlaying, currentLineIndex, activeItem?.id, playbackSpeed, selectedAccent) {
    if (isPlaying && activeItem != null) {
      val line = activeItem.lines.getOrNull(currentLineIndex)
      if (line != null) {
        speakLineAtIndex(currentLineIndex)
        val wordCount = line.targetText.split(Regex("\\s+")).size.coerceAtLeast(3)
        val estimatedMs = ((wordCount * 420L) / playbackSpeed).toLong().coerceIn(1600L, 5200L)
        delay(estimatedMs)
        if (isPlaying) {
          if (currentLineIndex + 1 < activeItem.lines.size) {
            onUpdateLinePosition(activeItem.id, currentLineIndex + 1)
          } else {
            isPlaying = false
          }
        }
      } else {
        isPlaying = false
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      speechManager?.stopSpeaking()
    }
  }

  LazyColumn(
    state = listState,
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 14.dp)
      .testTag("learn_listening_list"),
    contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Header & CEFR Filter
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AcadBgCard),
        border = BorderStroke(1.dp, AcadBorder)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Surface(
                shape = CircleShape,
                color = AcadCyan.copy(alpha = 0.16f),
                modifier = Modifier.size(36.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = null,
                    tint = AcadCyan,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Listening Academy",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = AcadTextPrimary
                )
                Text(
                  text = "Synced transcript • Tap any sentence to replay",
                  style = MaterialTheme.typography.bodySmall,
                  color = AcadTextSecondary
                )
              }
            }
            Surface(
              color = AcadEmerald.copy(alpha = 0.14f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.WifiOff,
                  contentDescription = null,
                  tint = AcadEmerald,
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Offline Ready",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = AcadEmerald
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // CEFR Level Filter Chips
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf("All", "A1", "B1", "C1").forEach { level ->
              FilterChip(
                selected = selectedCefrFilter == level,
                onClick = { selectedCefrFilter = level },
                label = {
                  Text(
                    text = if (level == "All") "All Levels" else level,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = AcadCyan.copy(alpha = 0.22f),
                  selectedLabelColor = AcadCyan,
                  containerColor = AcadBgElevated,
                  labelColor = AcadTextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = selectedCefrFilter == level,
                  borderColor = AcadBorder,
                  selectedBorderColor = AcadCyan
                )
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Search & Generate AI Listening Dialogues
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = searchListeningQuery,
              onValueChange = { searchListeningQuery = it },
              placeholder = { Text("Search dialogues...", style = MaterialTheme.typography.bodySmall, color = AcadTextSecondary) },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AcadCyan, modifier = Modifier.size(16.dp)) },
              singleLine = true,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("search_listening_dialogues_input")
            )

            if (onGenerateNewListening != null) {
              Button(
                onClick = { showGenerateDialog = true },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AcadCyan, contentColor = Color(0xFF04101E)),
                modifier = Modifier
                  .height(52.dp)
                  .testTag("generate_new_listening_dialogue_button")
              ) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Dialogue", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
              }
            }
          }

          if (isGeneratingListening) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = AcadCyan)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Gemini is composing a fresh dialogue script with real pronunciation...",
              style = MaterialTheme.typography.labelSmall,
              color = AcadCyan,
              fontWeight = FontWeight.Medium
            )
          }

          if (!generationErrorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
              color = AcadAmber.copy(alpha = 0.16f),
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, AcadAmber.copy(alpha = 0.4f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = generationErrorMessage,
                  style = MaterialTheme.typography.labelSmall,
                  color = AcadAmber,
                  modifier = Modifier.weight(1f)
                )
                Text(
                  text = "Retry",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = AcadAmber,
                  modifier = Modifier
                    .clickable { onGenerateNewListening?.invoke(genCefr, genCategory, genScenarioTopic) }
                    .padding(4.dp)
                )
              }
            }
          }

          if (showGenerateDialog) {
            AlertDialog(
              onDismissRequest = { showGenerateDialog = false },
              title = {
                Text(
                  text = "Generate AI Listening Dialogue",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = AcadTextPrimary
                )
              },
              text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Text(
                    text = "Select CEFR Level:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AcadTextPrimary
                  )
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    listOf("A1", "A2", "B1", "B2", "C1", "C2").forEach { level ->
                      FilterChip(
                        selected = genCefr == level,
                        onClick = { genCefr = level },
                        label = { Text(level, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                          selectedContainerColor = AcadCyan.copy(alpha = 0.25f),
                          selectedLabelColor = AcadCyan
                        )
                      )
                    }
                  }

                  Text(
                    text = "Dialogue Category:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AcadTextPrimary
                  )
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    listOf("Everyday Dialogue", "Travel", "Workplace", "Interview", "Short Conversation").forEach { cat ->
                      FilterChip(
                        selected = genCategory == cat,
                        onClick = { genCategory = cat },
                        label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                          selectedContainerColor = AcadCyan.copy(alpha = 0.25f),
                          selectedLabelColor = AcadCyan
                        )
                      )
                    }
                  }

                  OutlinedTextField(
                    value = genScenarioTopic,
                    onValueChange = { genScenarioTopic = it },
                    label = { Text("Specific Scenario (Optional)") },
                    placeholder = { Text("e.g., Asking for recommendations at a bakery") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                  )
                }
              },
              confirmButton = {
                Button(
                  onClick = {
                    showGenerateDialog = false
                    onGenerateNewListening?.invoke(genCefr, genCategory, genScenarioTopic)
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = AcadCyan, contentColor = Color(0xFF04101E))
                ) {
                  Text("Generate with Gemini", fontWeight = FontWeight.Bold)
                }
              },
              dismissButton = {
                OutlinedButton(onClick = { showGenerateDialog = false }) {
                  Text("Cancel")
                }
              }
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Story Selector Pills
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            filteredItems.forEach { item ->
              val isSelected = item.id == activeItem?.id
              val isDone = completedIds.contains(item.id)
              val savedLine = savedLinePositions[item.id] ?: 0
              Surface(
                color = if (isSelected) AcadCyan.copy(alpha = 0.18f) else AcadBgElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                  1.dp,
                  if (isSelected) AcadCyan else if (isDone) AcadEmerald.copy(alpha = 0.5f) else AcadBorder
                ),
                modifier = Modifier
                  .clickable {
                    speechManager?.stopSpeaking()
                    isPlaying = false
                    onSelectItemId(item.id)
                  }
                  .testTag("listening_story_chip_${item.id}")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    color = AcadCyan.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Text(
                      text = item.cefrLevel,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.ExtraBold,
                      color = AcadCyan,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      text = item.title,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      color = AcadTextPrimary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = when {
                        isDone -> "Completed ✓"
                        savedLine > 0 -> "Resume line ${savedLine + 1}/${item.lines.size}"
                        else -> "${item.category} • ${item.lines.size} lines"
                      },
                      style = MaterialTheme.typography.labelSmall,
                      color = if (isDone) AcadEmerald else AcadTextSecondary
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    if (activeItem != null) {
      // Professional Audio Player Card
      item {
        val progressFraction = ((currentLineIndex + 1).toFloat() / totalLines.toFloat()).coerceIn(0f, 1f)
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("listening_player_card"),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = AcadBgCard),
          border = BorderStroke(1.dp, AcadCyan.copy(alpha = 0.35f))
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Surface(
                    color = AcadCyan.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Text(
                      text = "${activeItem.cefrLevel} • ${activeItem.category}",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.ExtraBold,
                      color = AcadCyan,
                      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                  }
                  if (completedIds.contains(activeItem.id)) {
                    Surface(
                      color = AcadEmerald.copy(alpha = 0.16f),
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text(
                        text = "Completed ✓",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AcadEmerald,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                      )
                    }
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = activeItem.title,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = AcadTextPrimary
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Progress Bar & Line Counter
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (isPlaying) "Playing line ${currentLineIndex + 1} of $totalLines" else "Line ${currentLineIndex + 1} of $totalLines",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isPlaying) AcadCyan else AcadTextSecondary
              )
              Text(
                text = "${(progressFraction * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AcadCyan
              )
            }
            Spacer(modifier = Modifier.height(5.dp))
            LinearProgressIndicator(
              progress = { progressFraction },
              modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
              color = AcadCyan,
              trackColor = AcadBgElevated
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Playback Controls: Prev | Play/Pause | Next | Replay Line
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically
            ) {
              IconButton(
                onClick = {
                  val prevIdx = (currentLineIndex - 1).coerceAtLeast(0)
                  onUpdateLinePosition(activeItem.id, prevIdx)
                  speakLineAtIndex(prevIdx)
                },
                enabled = currentLineIndex > 0,
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(AcadBgElevated)
                  .testTag("listen_prev_line_button")
              ) {
                Icon(
                  imageVector = Icons.Default.SkipPrevious,
                  contentDescription = "Previous sentence",
                  tint = if (currentLineIndex > 0) AcadTextPrimary else AcadTextSecondary.copy(alpha = 0.4f)
                )
              }

              Button(
                onClick = {
                  if (isPlaying) {
                    isPlaying = false
                    speechManager?.stopSpeaking()
                  } else {
                    isPlaying = true
                  }
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isPlaying) AcadAmber else AcadCyan,
                  contentColor = Color(0xFF04101E)
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                modifier = Modifier.testTag("listen_play_pause_button")
              ) {
                Icon(
                  imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                  contentDescription = if (isPlaying) "Pause audio" else "Play audio",
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (isPlaying) "Pause" else "Play Dialogue",
                  fontWeight = FontWeight.ExtraBold,
                  style = MaterialTheme.typography.labelLarge
                )
              }

              IconButton(
                onClick = {
                  val nextIdx = (currentLineIndex + 1).coerceAtMost(totalLines - 1)
                  onUpdateLinePosition(activeItem.id, nextIdx)
                  speakLineAtIndex(nextIdx)
                },
                enabled = currentLineIndex < totalLines - 1,
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(AcadBgElevated)
                  .testTag("listen_next_line_button")
              ) {
                Icon(
                  imageVector = Icons.Default.SkipNext,
                  contentDescription = "Next sentence",
                  tint = if (currentLineIndex < totalLines - 1) AcadTextPrimary else AcadTextSecondary.copy(alpha = 0.4f)
                )
              }

              IconButton(
                onClick = {
                  speakLineAtIndex(currentLineIndex)
                },
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(AcadBgElevated)
                  .testTag("listen_replay_line_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Replay,
                  contentDescription = "Replay current sentence",
                  tint = AcadCyan
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = AcadBorder)
            Spacer(modifier = Modifier.height(10.dp))

            // Speed, Regional Accent, and Translation Toggles
            FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              listOf(0.75f to "0.75x", 1.0f to "1.0x", 1.25f to "1.25x").forEach { (speed, label) ->
                val selected = playbackSpeed == speed
                Surface(
                  color = if (selected) AcadIndigo.copy(alpha = 0.22f) else AcadBgElevated,
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, if (selected) AcadIndigo else AcadBorder),
                  modifier = Modifier
                    .clickable { playbackSpeed = speed }
                    .testTag("listen_speed_${label.replace(".", "_")}")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.Speed,
                      contentDescription = null,
                      tint = if (selected) AcadIndigo else AcadTextSecondary,
                      modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = label,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = if (selected) AcadIndigo else AcadTextSecondary
                    )
                  }
                }
              }

              Surface(
                color = if (showTranslation) AcadCyan.copy(alpha = 0.16f) else AcadBgElevated,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (showTranslation) AcadCyan.copy(alpha = 0.5f) else AcadBorder),
                modifier = Modifier
                  .clickable { showTranslation = !showTranslation }
                  .testTag("listen_toggle_translation")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    tint = if (showTranslation) AcadCyan else AcadTextSecondary,
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (showTranslation) "Translations On" else "Translations Off",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (showTranslation) AcadCyan else AcadTextSecondary
                  )
                }
              }
            }

            if (activeItem.regionalAccents.isNotEmpty()) {
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Accent:",
                  style = MaterialTheme.typography.labelSmall,
                  color = AcadTextSecondary,
                  fontWeight = FontWeight.SemiBold
                )
                activeItem.regionalAccents.forEach { accent ->
                  val isAccSelected = selectedAccent == accent
                  Surface(
                    color = if (isAccSelected) AcadCyan.copy(alpha = 0.18f) else AcadBgElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isAccSelected) AcadCyan else AcadBorder),
                    modifier = Modifier.clickable {
                      selectedAccent = accent
                      speakLineAtIndex(currentLineIndex)
                    }
                  ) {
                    Text(
                      text = accent,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = if (isAccSelected) FontWeight.Bold else FontWeight.Medium,
                      color = if (isAccSelected) AcadCyan else AcadTextSecondary,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Interactive Highlighted Transcript Lines
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = AcadBgCard),
          border = BorderStroke(1.dp, AcadBorder)
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Interactive Transcript",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = AcadTextPrimary
              )
              Text(
                text = "Tap any sentence to replay",
                style = MaterialTheme.typography.labelSmall,
                color = AcadCyan
              )
            }

            activeItem.lines.forEachIndexed { idx, line ->
              val isCurrentLine = idx == currentLineIndex
              val isTermSaved = savedTerms.contains("${activeItem.id}_$idx")
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isCurrentLine) AcadCyan.copy(alpha = 0.13f) else AcadBgElevated,
                border = BorderStroke(
                  width = if (isCurrentLine) 1.5.dp else 1.dp,
                  color = if (isCurrentLine) AcadCyan else AcadBorder
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    isPlaying = false
                    onUpdateLinePosition(activeItem.id, idx)
                    speakLineAtIndex(idx)
                  }
                  .testTag("listen_line_card_$idx")
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Surface(
                        color = if (isCurrentLine) AcadCyan else AcadBgCard,
                        shape = RoundedCornerShape(6.dp)
                      ) {
                        Text(
                          text = "${idx + 1}. ${line.speaker}",
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.ExtraBold,
                          color = if (isCurrentLine) Color(0xFF04101E) else AcadCyan,
                          modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                      }
                      if (isCurrentLine) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                          imageVector = Icons.Default.GraphicEq,
                          contentDescription = "Active sentence",
                          tint = AcadCyan,
                          modifier = Modifier.size(16.dp)
                        )
                      }
                    }

                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      if (line.keyVocabWord.isNotBlank()) {
                        Surface(
                          color = if (isTermSaved) AcadEmerald.copy(alpha = 0.18f) else AcadBgCard,
                          shape = RoundedCornerShape(6.dp),
                          border = BorderStroke(
                            1.dp,
                            if (isTermSaved) AcadEmerald.copy(alpha = 0.5f) else AcadBorder
                          ),
                          modifier = Modifier.clickable {
                            if (!isTermSaved) {
                              onSaveVocabToFlashcard(line.keyVocabWord, line.keyVocabMeaning, "")
                              savedTerms = savedTerms + "${activeItem.id}_$idx"
                            }
                          }
                        ) {
                          Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            Icon(
                              imageVector = if (isTermSaved) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                              contentDescription = "Save vocabulary",
                              tint = if (isTermSaved) AcadEmerald else AcadCyan,
                              modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                              text = if (isTermSaved) "${line.keyVocabWord} ✓" else "+ ${line.keyVocabWord}",
                              style = MaterialTheme.typography.labelSmall,
                              fontWeight = FontWeight.SemiBold,
                              color = if (isTermSaved) AcadEmerald else AcadCyan
                            )
                          }
                        }
                      }
                      Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Replay sentence",
                        tint = if (isCurrentLine) AcadCyan else AcadTextSecondary,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = line.targetText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrentLine) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = AcadTextPrimary
                  )
                  AnimatedVisibility(visible = showTranslation) {
                    Text(
                      text = line.nativeTranslation,
                      style = MaterialTheme.typography.bodySmall,
                      color = AcadTextSecondary,
                      modifier = Modifier.padding(top = 3.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Listening Comprehension Quiz Card
      item {
        val isAlreadyCompleted = completedIds.contains(activeItem.id)
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = AcadBgCard),
          border = BorderStroke(1.dp, AcadBorder)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Listening Comprehension Check",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = AcadTextPrimary
              )
              Surface(
                color = if (isAlreadyCompleted) AcadEmerald.copy(alpha = 0.16f) else AcadAmber.copy(alpha = 0.16f),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = if (isAlreadyCompleted) "Reviewed ✓" else "+35 XP",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (isAlreadyCompleted) AcadEmerald else AcadAmber,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            activeItem.questions.forEachIndexed { qIdx, q ->
              Text(
                text = "${qIdx + 1}. ${q.question}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = AcadTextPrimary
              )
              Spacer(modifier = Modifier.height(6.dp))
              q.options.forEachIndexed { optIdx, opt ->
                val isSelected = quizAnswers[qIdx] == optIdx
                val isCorrectOpt = optIdx == q.correctIndex
                val chipColor = when {
                  quizSubmitted && isCorrectOpt -> AcadEmerald.copy(alpha = 0.18f)
                  quizSubmitted && isSelected && !isCorrectOpt -> AcadRose.copy(alpha = 0.18f)
                  isSelected -> AcadCyan.copy(alpha = 0.18f)
                  else -> AcadBgElevated
                }
                val borderColor = when {
                  quizSubmitted && isCorrectOpt -> AcadEmerald
                  quizSubmitted && isSelected && !isCorrectOpt -> AcadRose
                  isSelected -> AcadCyan
                  else -> AcadBorder
                }
                Surface(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable(enabled = !quizSubmitted) {
                      quizAnswers = quizAnswers + (qIdx to optIdx)
                    },
                  shape = RoundedCornerShape(10.dp),
                  color = chipColor,
                  border = BorderStroke(1.dp, borderColor)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = opt,
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                      color = AcadTextPrimary,
                      modifier = Modifier.weight(1f)
                    )
                    if (quizSubmitted && isCorrectOpt) {
                      Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AcadEmerald,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }
                }
              }
              if (quizSubmitted) {
                Text(
                  text = q.explanation,
                  style = MaterialTheme.typography.labelSmall,
                  color = AcadCyan,
                  modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
              } else {
                Spacer(modifier = Modifier.height(8.dp))
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = {
                  quizSubmitted = true
                  val correct = activeItem.questions.indices.count { idx ->
                    quizAnswers[idx] == activeItem.questions[idx].correctIndex
                  }
                  val pct = ((correct * 100.0) / activeItem.questions.size.coerceAtLeast(1)).toInt()
                  onCompleteItem(
                    activeItem.id,
                    "Listening: ${activeItem.title}",
                    pct,
                    if (isAlreadyCompleted) 0 else 35
                  )
                },
                enabled = !quizSubmitted && quizAnswers.size == activeItem.questions.size,
                modifier = Modifier
                  .weight(1f)
                  .testTag("submit_listening_quiz_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = AcadCyan,
                  contentColor = Color(0xFF04101E)
                )
              ) {
                Text(
                  text = when {
                    quizSubmitted && isAlreadyCompleted -> "Review Complete ✓ (No Duplicate XP)"
                    quizSubmitted -> "Quiz Completed! +35 XP"
                    else -> "Check Comprehension"
                  },
                  fontWeight = FontWeight.ExtraBold
                )
              }
              if (quizSubmitted) {
                OutlinedButton(
                  onClick = {
                    quizAnswers = emptyMap()
                    quizSubmitted = false
                  },
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(1.dp, AcadBorder)
                ) {
                  Text("Retry", color = AcadCyan, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadingAcademySection(
  passages: List<ReadingAcademyPassage>,
  bookmarkedIds: Set<String>,
  completedIds: Set<String>,
  speechManager: SpeechManager?,
  listState: LazyListState,
  selectedPassageId: String,
  onSelectPassageId: (String) -> Unit,
  savedParagraphPositions: Map<String, Int>,
  onUpdateParagraphPosition: (passageId: String, paragraphIdx: Int) -> Unit,
  onToggleBookmark: (String) -> Unit,
  onCompletePassage: (id: String, title: String, scorePercent: Int, xpBonus: Int) -> Unit,
  onSaveVocabToFlashcard: (front: String, back: String, phonetic: String) -> Unit,
  onGenerateNewStory: ((cefrLevel: String, category: String, topic: String) -> Unit)? = null,
  isGeneratingStory: Boolean = false,
  generationErrorMessage: String? = null
) {
  var selectedCefrFilter by remember { mutableStateOf("All") }
  var selectedCategoryFilter by remember { mutableStateOf("All") }
  var showBookmarkedOnly by remember { mutableStateOf(false) }
  var searchReadingQuery by remember { mutableStateOf("") }
  var showGenerateStoryDialog by remember { mutableStateOf(false) }
  var genStoryCefr by remember { mutableStateOf("B1") }
  var genStoryCategory by remember { mutableStateOf("Graded Story") }
  var genStoryTopic by remember { mutableStateOf("") }

  var fontScale by remember { mutableStateOf(ReadingFontScale.MEDIUM) }
  var lineSpacing by remember { mutableStateOf(ReadingLineSpacing.COMFORTABLE) }
  var readingWidthMode by remember { mutableStateOf(ReadingWidthMode.STANDARD) }
  var showParallelTranslation by remember { mutableStateOf(true) }
  var showReadingSettingsMenu by remember { mutableStateOf(false) }

  val filteredPassages = remember(passages, selectedCefrFilter, selectedCategoryFilter, showBookmarkedOnly, bookmarkedIds, searchReadingQuery) {
    passages.filter { p ->
      val matchesCefr = selectedCefrFilter == "All" || p.cefrLevel.equals(selectedCefrFilter, ignoreCase = true)
      val matchesCat = selectedCategoryFilter == "All" || p.category.equals(selectedCategoryFilter, ignoreCase = true)
      val matchesBookmark = !showBookmarkedOnly || bookmarkedIds.contains(p.id)
      val q = searchReadingQuery.trim()
      val matchesSearch = q.isBlank() || p.title.contains(q, ignoreCase = true) || p.category.contains(q, ignoreCase = true) || p.paragraphs.any { it.contains(q, ignoreCase = true) }
      matchesCefr && matchesCat && matchesBookmark && matchesSearch
    }
  }

  val activePassage = filteredPassages.firstOrNull { it.id == selectedPassageId }
    ?: filteredPassages.firstOrNull()
    ?: passages.firstOrNull()

  val totalParagraphs = activePassage?.paragraphs?.size?.coerceAtLeast(1) ?: 1
  val activeParagraphIndex = if (activePassage != null) {
    (savedParagraphPositions[activePassage.id] ?: 0).coerceIn(0, totalParagraphs - 1)
  } else {
    0
  }

  var isNarratingPassage by remember(activePassage?.id) { mutableStateOf(false) }
  var selectedGloss by remember(activePassage?.id) { mutableStateOf<ReadingWordGloss?>(null) }
  var savedGlossWords by remember { mutableStateOf(setOf<String>()) }
  var quizAnswers by remember(activePassage?.id) { mutableStateOf<Map<Int, Int>>(emptyMap()) }
  var quizSubmitted by remember(activePassage?.id) { mutableStateOf(false) }

  // Paragraph-by-paragraph audio playback with active paragraph highlighting
  LaunchedEffect(isNarratingPassage, activeParagraphIndex, activePassage?.id) {
    if (isNarratingPassage && activePassage != null) {
      val paragraphText = activePassage.paragraphs.getOrNull(activeParagraphIndex)
      if (!paragraphText.isNullOrBlank()) {
        speechManager?.speak(paragraphText, activePassage.languageCode, 0.95f)
        val words = paragraphText.split(Regex("\\s+")).size.coerceAtLeast(5)
        val waitMs = (words * 380L).coerceIn(2200L, 6500L)
        delay(waitMs)
        if (isNarratingPassage) {
          if (activeParagraphIndex + 1 < activePassage.paragraphs.size) {
            onUpdateParagraphPosition(activePassage.id, activeParagraphIndex + 1)
          } else {
            isNarratingPassage = false
          }
        }
      } else {
        isNarratingPassage = false
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      speechManager?.stopSpeaking()
    }
  }

  val categories = remember(passages) {
    listOf("All") + passages.map { it.category }.distinct()
  }

  LazyColumn(
    state = listState,
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 14.dp)
      .testTag("learn_reading_list"),
    contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Books & Reading Library Shelf at top with compact design & dynamic multi-source catalog
    item {
      BooksAndReadingLibraryShelfCard(
        languageCode = activePassage?.languageCode ?: passages.firstOrNull()?.languageCode ?: "en",
        speechManager = speechManager,
        completedBookIds = completedIds,
        onCompleteBook = onCompletePassage,
        onSaveVocabToFlashcard = onSaveVocabToFlashcard
      )
    }

    // Compact Reading Academy Section (A1–C2 + Search + Story Carousel)
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AcadBgCard),
        border = BorderStroke(1.dp, AcadBorder.copy(alpha = 0.55f))
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Surface(
                shape = CircleShape,
                color = AcadIndigo.copy(alpha = 0.16f),
                modifier = Modifier.size(30.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = AcadIndigo,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "Reading Academy",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.ExtraBold,
                  color = AcadTextPrimary
                )
                Text(
                  text = "Graded interactive stories • CEFR A1–C2",
                  style = MaterialTheme.typography.labelSmall,
                  color = AcadTextSecondary
                )
              }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Surface(
                color = if (showBookmarkedOnly) AcadAmber.copy(alpha = 0.2f) else AcadBgElevated,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { showBookmarkedOnly = !showBookmarkedOnly }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = if (showBookmarkedOnly) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Filter Bookmarked",
                    tint = if (showBookmarkedOnly) AcadAmber else AcadTextSecondary,
                    modifier = Modifier.size(13.dp)
                  )
                }
              }
              if (onGenerateNewStory != null) {
                Surface(
                  color = AcadIndigo.copy(alpha = 0.18f),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier
                    .clickable { showGenerateStoryDialog = true }
                    .testTag("generate_new_reading_story_button")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = AcadIndigo, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Custom Topic", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AcadIndigo)
                  }
                }
              }
              Surface(
                color = AcadEmerald.copy(alpha = 0.14f),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = "${completedIds.count { id -> passages.any { it.id == id } }}/${passages.size}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = AcadEmerald,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Full-width Search Input
          OutlinedTextField(
            value = searchReadingQuery,
            onValueChange = { searchReadingQuery = it },
            placeholder = { Text("Search stories by title or topic...", style = MaterialTheme.typography.bodySmall, color = AcadTextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AcadIndigo, modifier = Modifier.size(16.dp)) },
            trailingIcon = {
              if (searchReadingQuery.isNotBlank()) {
                IconButton(onClick = { searchReadingQuery = "" }, modifier = Modifier.size(24.dp)) {
                  Icon(Icons.Default.Close, contentDescription = "Clear search", tint = AcadTextSecondary, modifier = Modifier.size(14.dp))
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("search_reading_stories_input")
          )

          Spacer(modifier = Modifier.height(6.dp))

          // Clean Horizontal CEFR Level Filter Row (A1-C2)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            listOf("All", "A1", "A2", "B1", "B2", "C1", "C2").forEach { level ->
              FilterChip(
                selected = selectedCefrFilter == level,
                onClick = { selectedCefrFilter = level },
                label = {
                  Text(
                    text = if (level == "All") "All Levels" else level,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = AcadIndigo.copy(alpha = 0.22f),
                  selectedLabelColor = AcadIndigo,
                  containerColor = AcadBgElevated,
                  labelColor = AcadTextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = selectedCefrFilter == level,
                  borderColor = Color.Transparent,
                  selectedBorderColor = AcadIndigo.copy(alpha = 0.5f)
                )
              )
            }
          }

          if (isGeneratingStory) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = AcadIndigo)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Crafting a fresh reading passage with vocabulary gloss...",
              style = MaterialTheme.typography.labelSmall,
              color = AcadIndigo,
              fontWeight = FontWeight.Medium
            )
          }

          if (!generationErrorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
              color = AcadAmber.copy(alpha = 0.16f),
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, AcadAmber.copy(alpha = 0.4f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = generationErrorMessage,
                  style = MaterialTheme.typography.labelSmall,
                  color = AcadAmber,
                  modifier = Modifier.weight(1f)
                )
                Text(
                  text = "Retry",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = AcadAmber,
                  modifier = Modifier
                    .clickable { onGenerateNewStory?.invoke(genStoryCefr, genStoryCategory, genStoryTopic) }
                    .padding(4.dp)
                )
              }
            }
          }

          if (showGenerateStoryDialog) {
            AlertDialog(
              onDismissRequest = { showGenerateStoryDialog = false },
              title = {
                Text(
                  text = "Generate AI Reading Story",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = AcadTextPrimary
                )
              },
              text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Text(
                    text = "Select CEFR Level:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AcadTextPrimary
                  )
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    listOf("A1", "A2", "B1", "B2", "C1", "C2").forEach { level ->
                      FilterChip(
                        selected = genStoryCefr == level,
                        onClick = { genStoryCefr = level },
                        label = { Text(level, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                          selectedContainerColor = AcadIndigo.copy(alpha = 0.25f),
                          selectedLabelColor = AcadIndigo
                        )
                      )
                    }
                  }

                  Text(
                    text = "Story Category / Genre:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AcadTextPrimary
                  )
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    listOf("Everyday Life", "Travel", "Culture", "Science & Tech", "Workplace", "Graded Story").forEach { cat ->
                      FilterChip(
                        selected = genStoryCategory == cat,
                        onClick = { genStoryCategory = cat },
                        label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                          selectedContainerColor = AcadIndigo.copy(alpha = 0.25f),
                          selectedLabelColor = AcadIndigo
                        )
                      )
                    }
                  }

                  OutlinedTextField(
                    value = genStoryTopic,
                    onValueChange = { genStoryTopic = it },
                    label = { Text("Specific Theme or Topic (Optional)") },
                    placeholder = { Text("e.g., A festival celebration in a mountain village") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                  )
                }
              },
              confirmButton = {
                Button(
                  onClick = {
                    showGenerateStoryDialog = false
                    onGenerateNewStory?.invoke(genStoryCefr, genStoryCategory, genStoryTopic)
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = AcadIndigo, contentColor = Color.White)
                ) {
                  Text("Generate with Gemini", fontWeight = FontWeight.Bold)
                }
              },
              dismissButton = {
                OutlinedButton(onClick = { showGenerateStoryDialog = false }) {
                  Text("Cancel")
                }
              }
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Story Cards Carousel
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            filteredPassages.forEach { p ->
              val isSelected = p.id == activePassage?.id
              val isDone = completedIds.contains(p.id)
              val isBookmarked = bookmarkedIds.contains(p.id)
              val wordCount = p.paragraphs.sumOf { it.split(Regex("\\s+")).size }
              val readMinutes = maxOf(2, (wordCount / 35))
              val savedPara = savedParagraphPositions[p.id] ?: 0

              Surface(
                color = if (isSelected) AcadIndigo.copy(alpha = 0.2f) else AcadBgElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                  1.dp,
                  if (isSelected) AcadIndigo else if (isDone) AcadEmerald.copy(alpha = 0.5f) else AcadBorder
                ),
                modifier = Modifier
                  .clickable {
                    speechManager?.stopSpeaking()
                    isNarratingPassage = false
                    onSelectPassageId(p.id)
                  }
                  .testTag("reading_story_chip_${p.id}")
              ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Surface(
                      color = AcadIndigo.copy(alpha = 0.22f),
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text(
                        text = p.cefrLevel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = AcadIndigo,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                    Text(
                      text = "~$readMinutes min",
                      style = MaterialTheme.typography.labelSmall,
                      color = AcadTextSecondary
                    )
                    if (isBookmarked) {
                      Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = AcadAmber,
                        modifier = Modifier.size(13.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = p.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AcadTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = when {
                      isDone -> "Read ✓ • History Saved"
                      savedPara > 0 -> "Resume ¶${savedPara + 1}/${p.paragraphs.size}"
                      else -> p.category
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDone) AcadEmerald else AcadTextSecondary
                  )
                }
              }
            }
          }
        }
      }
    }

    if (activePassage != null) {
      // Reading Preferences & Audio Controls Card
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = AcadBgCard),
          border = BorderStroke(1.dp, AcadBorder)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            // Story Header + Bookmark + Audio Play
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Surface(
                    color = AcadIndigo.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Text(
                      text = "${activePassage.cefrLevel} • ${activePassage.category}",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.ExtraBold,
                      color = AcadIndigo,
                      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                  }
                  if (completedIds.contains(activePassage.id)) {
                    Surface(
                      color = AcadEmerald.copy(alpha = 0.16f),
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Icon(
                          imageVector = Icons.Default.History,
                          contentDescription = null,
                          tint = AcadEmerald,
                          modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                          text = "Read",
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.Bold,
                          color = AcadEmerald
                        )
                      }
                    }
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = activePassage.title,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = AcadTextPrimary
                )
              }

              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                  color = if (showParallelTranslation) AcadEmerald.copy(alpha = 0.16f) else AcadBgElevated,
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier
                    .clickable { showParallelTranslation = !showParallelTranslation }
                    .testTag("reading_parallel_translation_toggle")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.Translate,
                      contentDescription = "Toggle Parallel Translation",
                      tint = if (showParallelTranslation) AcadEmerald else AcadTextSecondary,
                      modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = if (showParallelTranslation) "Translation On" else "Translate",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = if (showParallelTranslation) AcadEmerald else AcadTextSecondary
                    )
                  }
                }

                IconButton(
                  onClick = { showReadingSettingsMenu = !showReadingSettingsMenu },
                  modifier = Modifier
                    .size(36.dp)
                    .testTag("reading_settings_menu_button")
                ) {
                  Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Reading Settings",
                    tint = if (showReadingSettingsMenu) AcadCyan else AcadTextSecondary
                  )
                }

                IconButton(
                  onClick = { onToggleBookmark(activePassage.id) },
                  modifier = Modifier
                    .size(36.dp)
                    .testTag("reading_bookmark_button")
                ) {
                  Icon(
                    imageVector = if (bookmarkedIds.contains(activePassage.id)) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark story",
                    tint = if (bookmarkedIds.contains(activePassage.id)) AcadAmber else AcadTextSecondary
                  )
                }
              }
            }

            // Collapsible Reading Settings Menu (Text Size, Line Spacing, Reading Width, Parallel Translation)
            AnimatedVisibility(visible = showReadingSettingsMenu) {
              Surface(
                color = AcadBgElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 10.dp)
                  .testTag("reading_settings_panel")
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "Reading Settings",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.ExtraBold,
                      color = AcadTextPrimary
                    )
                    Text(
                      text = "Done",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = AcadCyan,
                      modifier = Modifier.clickable { showReadingSettingsMenu = false }
                    )
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text("Text Size", style = MaterialTheme.typography.labelSmall, color = AcadTextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      ReadingFontScale.entries.forEach { scale ->
                        val selected = fontScale == scale
                        Surface(
                          color = if (selected) AcadCyan.copy(alpha = 0.22f) else AcadBgCard,
                          shape = RoundedCornerShape(8.dp),
                          modifier = Modifier
                            .clickable { fontScale = scale }
                            .testTag("reading_font_${scale.name.lowercase()}")
                        ) {
                          Text(
                            text = scale.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selected) AcadCyan else AcadTextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                          )
                        }
                      }
                    }
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text("Line Spacing", style = MaterialTheme.typography.labelSmall, color = AcadTextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      ReadingLineSpacing.entries.forEach { spacing ->
                        val selected = lineSpacing == spacing
                        Surface(
                          color = if (selected) AcadIndigo.copy(alpha = 0.22f) else AcadBgCard,
                          shape = RoundedCornerShape(8.dp),
                          modifier = Modifier
                            .clickable { lineSpacing = spacing }
                            .testTag("reading_spacing_${spacing.name.lowercase()}")
                        ) {
                          Text(
                            text = spacing.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) AcadIndigo else AcadTextSecondary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                          )
                        }
                      }
                    }
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text("Reading Width", style = MaterialTheme.typography.labelSmall, color = AcadTextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      ReadingWidthMode.entries.forEach { widthOpt ->
                        val selected = readingWidthMode == widthOpt
                        Surface(
                          color = if (selected) AcadEmerald.copy(alpha = 0.2f) else AcadBgCard,
                          shape = RoundedCornerShape(8.dp),
                          modifier = Modifier
                            .clickable { readingWidthMode = widthOpt }
                            .testTag("reading_width_${widthOpt.name.lowercase()}")
                        ) {
                          Text(
                            text = widthOpt.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) AcadEmerald else AcadTextSecondary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                          )
                        }
                      }
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Paragraph Audio Player Bar
            Surface(
              color = AcadBgElevated,
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, AcadBorder)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(
                    onClick = {
                      val prev = (activeParagraphIndex - 1).coerceAtLeast(0)
                      onUpdateParagraphPosition(activePassage.id, prev)
                      speechManager?.speak(activePassage.paragraphs[prev], activePassage.languageCode, 0.95f)
                    },
                    enabled = activeParagraphIndex > 0,
                    modifier = Modifier
                      .size(32.dp)
                      .testTag("reading_prev_para_button")
                  ) {
                    Icon(
                      imageVector = Icons.Default.SkipPrevious,
                      contentDescription = "Previous paragraph",
                      tint = if (activeParagraphIndex > 0) AcadTextPrimary else AcadTextSecondary.copy(alpha = 0.4f),
                      modifier = Modifier.size(18.dp)
                    )
                  }

                  FilledTonalButton(
                    onClick = {
                      if (isNarratingPassage) {
                        isNarratingPassage = false
                        speechManager?.stopSpeaking()
                      } else {
                        isNarratingPassage = true
                      }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                      containerColor = if (isNarratingPassage) AcadAmber.copy(alpha = 0.25f) else AcadCyan.copy(alpha = 0.2f),
                      contentColor = if (isNarratingPassage) AcadAmber else AcadCyan
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("reading_play_pause_button")
                  ) {
                    Icon(
                      imageVector = if (isNarratingPassage) Icons.Default.Pause else Icons.AutoMirrored.Filled.VolumeUp,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                      text = if (isNarratingPassage) "Pause Audio" else "Read Aloud",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold
                    )
                  }

                  IconButton(
                    onClick = {
                      val next = (activeParagraphIndex + 1).coerceAtMost(totalParagraphs - 1)
                      onUpdateParagraphPosition(activePassage.id, next)
                      speechManager?.speak(activePassage.paragraphs[next], activePassage.languageCode, 0.95f)
                    },
                    enabled = activeParagraphIndex < totalParagraphs - 1,
                    modifier = Modifier
                      .size(32.dp)
                      .testTag("reading_next_para_button")
                  ) {
                    Icon(
                      imageVector = Icons.Default.SkipNext,
                      contentDescription = "Next paragraph",
                      tint = if (activeParagraphIndex < totalParagraphs - 1) AcadTextPrimary else AcadTextSecondary.copy(alpha = 0.4f),
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }

                Text(
                  text = "¶ ${activeParagraphIndex + 1}/$totalParagraphs",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = AcadCyan
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tapped Word Dictionary Popup Banner
            AnimatedVisibility(visible = selectedGloss != null) {
              selectedGloss?.let { gloss ->
                val isWordSaved = savedGlossWords.contains("${activePassage.id}_${gloss.word}")
                Card(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("reading_word_gloss_card"),
                  shape = RoundedCornerShape(14.dp),
                  colors = CardDefaults.cardColors(containerColor = AcadBgElevated),
                  border = BorderStroke(1.dp, AcadCyan.copy(alpha = 0.5f))
                ) {
                  Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(
                          text = gloss.word,
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.ExtraBold,
                          color = AcadCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                          color = AcadBgCard,
                          shape = RoundedCornerShape(6.dp)
                        ) {
                          Text(
                            text = gloss.partOfSpeech,
                            style = MaterialTheme.typography.labelSmall,
                            color = AcadTextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                      }
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                          onClick = {
                            speechManager?.speak(gloss.word, activePassage.languageCode)
                          },
                          modifier = Modifier.size(32.dp)
                        ) {
                          Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Pronounce word",
                            tint = AcadCyan,
                            modifier = Modifier.size(18.dp)
                          )
                        }
                        IconButton(
                          onClick = { selectedGloss = null },
                          modifier = Modifier.size(32.dp)
                        ) {
                          Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss dictionary",
                            tint = AcadTextSecondary,
                            modifier = Modifier.size(18.dp)
                          )
                        }
                      }
                    }
                    Text(
                      text = "${gloss.translation} • /${gloss.phonetic}/",
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      color = AcadTextPrimary
                    )
                    Text(
                      text = gloss.definition,
                      style = MaterialTheme.typography.bodySmall,
                      color = AcadTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(
                      onClick = {
                        if (!isWordSaved) {
                          onSaveVocabToFlashcard(gloss.word, gloss.translation, gloss.phonetic)
                          savedGlossWords = savedGlossWords + "${activePassage.id}_${gloss.word}"
                        }
                      },
                      colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isWordSaved) AcadEmerald.copy(alpha = 0.2f) else AcadCyan.copy(alpha = 0.2f),
                        contentColor = if (isWordSaved) AcadEmerald else AcadCyan
                      ),
                      modifier = Modifier.testTag("save_reading_gloss_word_btn")
                    ) {
                      Icon(
                        imageVector = if (isWordSaved) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = if (isWordSaved) "Saved to Flashcards ✓" else "Save '${gloss.word}' to Flashcards",
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                }
              }
            }

            // Comfortable Book-Style Paragraphs with Active Highlighting & Tap-Any-Word
            activePassage.paragraphs.forEachIndexed { idx, paragraph ->
              val isCurrentParagraph = idx == activeParagraphIndex
              Surface(
                color = if (isCurrentParagraph) AcadIndigo.copy(alpha = 0.10f) else AcadBgElevated.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                  width = if (isCurrentParagraph) 1.5.dp else 0.5.dp,
                  color = if (isCurrentParagraph) AcadCyan.copy(alpha = 0.65f) else AcadBorder.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
                  .clickable {
                    onUpdateParagraphPosition(activePassage.id, idx)
                  }
                  .testTag("reading_paragraph_$idx")
              ) {
                Column(modifier = Modifier.padding(horizontal = readingWidthMode.horizontalPaddingDp.dp, vertical = 12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "Paragraph ${idx + 1}",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = if (isCurrentParagraph) AcadCyan else AcadTextSecondary
                    )
                    IconButton(
                      onClick = {
                        onUpdateParagraphPosition(activePassage.id, idx)
                        speechManager?.speak(paragraph, activePassage.languageCode, 0.95f)
                      },
                      modifier = Modifier.size(26.dp)
                    ) {
                      Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Listen to paragraph",
                        tint = if (isCurrentParagraph) AcadCyan else AcadTextSecondary,
                        modifier = Modifier.size(15.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(4.dp))

                  val words = paragraph.split(" ").filter { it.isNotBlank() }
                  FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalArrangement = Arrangement.spacedBy(lineSpacing.wordRowSpacingDp.dp)
                  ) {
                    words.forEach { rawWord ->
                      val cleanKey = rawWord
                        .lowercase()
                        .trim { !it.isLetterOrDigit() }
                      val glossMatch = activePassage.glossary[cleanKey]
                      val isSelectedWord = selectedGloss?.word?.equals(cleanKey, ignoreCase = true) == true
                      Text(
                        text = rawWord,
                        fontSize = fontScale.bodySizeSp.sp,
                        lineHeight = (fontScale.bodySizeSp * lineSpacing.lineHeightMultiplier).sp,
                        fontWeight = if (glossMatch != null) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                          isSelectedWord -> Color(0xFF04101E)
                          glossMatch != null -> AcadCyan
                          else -> AcadTextPrimary
                        },
                        modifier = Modifier
                          .clip(RoundedCornerShape(4.dp))
                          .background(
                            when {
                              isSelectedWord -> AcadCyan
                              glossMatch != null -> AcadCyan.copy(alpha = 0.12f)
                              else -> Color.Transparent
                            }
                          )
                          .clickable {
                            onUpdateParagraphPosition(activePassage.id, idx)
                            selectedGloss = glossMatch ?: ReadingWordGloss(
                              word = rawWord.trim { !it.isLetterOrDigit() }.ifBlank { rawWord },
                              translation = activePassage.paragraphTranslations
                                .getOrNull(idx)
                                ?.let { "Context: $it" }
                                ?: "Tap speaker to hear pronunciation",
                              partOfSpeech = "word in context",
                              definition = "From paragraph ${idx + 1} of '${activePassage.title}'. Tap Save to add to your SRS Flashcards.",
                              phonetic = cleanKey
                            )
                            speechManager?.speak(cleanKey.ifBlank { rawWord }, activePassage.languageCode)
                          }
                          .padding(horizontal = 2.dp, vertical = 1.dp)
                      )
                    }
                  }

                  val rawTrans = activePassage.paragraphTranslations.getOrNull(idx)
                  val validTrans = rawTrans?.takeIf { !isDuplicateReadingTranslation(paragraph, it) }
                  AnimatedVisibility(visible = showParallelTranslation && validTrans != null) {
                    if (validTrans != null) {
                      Text(
                        text = validTrans,
                        fontSize = (fontScale.bodySizeSp - 2).coerceAtLeast(12).sp,
                        fontStyle = FontStyle.Italic,
                        color = AcadTextSecondary,
                        modifier = Modifier.padding(top = 6.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Reading Comprehension Quiz Card
      item {
        val isAlreadyCompleted = completedIds.contains(activePassage.id)
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = AcadBgCard),
          border = BorderStroke(1.dp, AcadBorder)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Reading Comprehension Quiz",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = AcadTextPrimary
              )
              Surface(
                color = if (isAlreadyCompleted) AcadEmerald.copy(alpha = 0.16f) else AcadAmber.copy(alpha = 0.16f),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = if (isAlreadyCompleted) "Read ✓" else "+40 XP",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (isAlreadyCompleted) AcadEmerald else AcadAmber,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            activePassage.questions.forEachIndexed { qIdx, q ->
              Text(
                text = "${qIdx + 1}. ${q.question}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = AcadTextPrimary
              )
              Spacer(modifier = Modifier.height(6.dp))
              q.options.forEachIndexed { optIdx, opt ->
                val isSelected = quizAnswers[qIdx] == optIdx
                val isCorrectOpt = optIdx == q.correctIndex
                val bgColor = when {
                  quizSubmitted && isCorrectOpt -> AcadEmerald.copy(alpha = 0.18f)
                  quizSubmitted && isSelected && !isCorrectOpt -> AcadRose.copy(alpha = 0.18f)
                  isSelected -> AcadIndigo.copy(alpha = 0.2f)
                  else -> AcadBgElevated
                }
                val borderColor = when {
                  quizSubmitted && isCorrectOpt -> AcadEmerald
                  quizSubmitted && isSelected && !isCorrectOpt -> AcadRose
                  isSelected -> AcadIndigo
                  else -> AcadBorder
                }
                Surface(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable(enabled = !quizSubmitted) {
                      quizAnswers = quizAnswers + (qIdx to optIdx)
                    },
                  shape = RoundedCornerShape(10.dp),
                  color = bgColor,
                  border = BorderStroke(1.dp, borderColor)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = opt,
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                      color = AcadTextPrimary,
                      modifier = Modifier.weight(1f)
                    )
                    if (quizSubmitted && isCorrectOpt) {
                      Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AcadEmerald,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }
                }
              }
              if (quizSubmitted) {
                Text(
                  text = q.explanation,
                  style = MaterialTheme.typography.labelSmall,
                  color = AcadCyan,
                  modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
              } else {
                Spacer(modifier = Modifier.height(8.dp))
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = {
                  quizSubmitted = true
                  val correct = activePassage.questions.indices.count { idx ->
                    quizAnswers[idx] == activePassage.questions[idx].correctIndex
                  }
                  val pct = ((correct * 100.0) / activePassage.questions.size.coerceAtLeast(1)).toInt()
                  onCompletePassage(
                    activePassage.id,
                    "Reading: ${activePassage.title}",
                    pct,
                    if (isAlreadyCompleted) 0 else 40
                  )
                },
                enabled = !quizSubmitted && quizAnswers.size == activePassage.questions.size,
                modifier = Modifier
                  .weight(1f)
                  .testTag("submit_reading_quiz_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = AcadCyan,
                  contentColor = Color(0xFF04101E)
                )
              ) {
                Text(
                  text = when {
                    quizSubmitted && isAlreadyCompleted -> "Review Complete ✓ (No Duplicate XP)"
                    quizSubmitted -> "Passage Completed! +40 XP"
                    else -> "Submit Reading Quiz"
                  },
                  fontWeight = FontWeight.ExtraBold
                )
              }
              if (quizSubmitted) {
                OutlinedButton(
                  onClick = {
                    quizAnswers = emptyMap()
                    quizSubmitted = false
                  },
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(1.dp, AcadBorder)
                ) {
                  Text("Retry", color = AcadCyan, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }
}

private data class AiLabCardSpec(
  val title: String,
  val subtitle: String,
  val badge: String,
  val skillFocus: String,
  val icon: ImageVector,
  val accentColor: Color,
  val buttonLabel: String,
  val testTag: String,
  val onClick: () -> Unit
)

@Composable
fun LearnAiLabsSection(
  listState: LazyListState,
  onOpenImmersionMode: () -> Unit,
  onOpenMistakeRecovery: () -> Unit,
  onOpenConversationRepairLab: () -> Unit,
  onOpenConversationDetective: () -> Unit,
  onOpenTeachTheAi: () -> Unit,
  onOpenAccentStudio: () -> Unit = {},
  onSwitchToCustomLessonGenerator: () -> Unit = {}
) {
  val labs = listOf(
    AiLabCardSpec(
      title = "Living Language World",
      subtitle = "Step into interactive 3D-style cafés, airports, and apartments with real-time AI dialogue.",
      badge = "Interactive Immersion",
      skillFocus = "Speaking & Pragmatics",
      icon = Icons.Default.Public,
      accentColor = AcadCyan,
      buttonLabel = "Enter Immersion World",
      testTag = "learn_open_immersion_btn",
      onClick = onOpenImmersionMode
    ),
    AiLabCardSpec(
      title = "Intelligent Mistake Recovery",
      subtitle = "Turn your personal grammar and vocabulary errors into targeted AI correction drills.",
      badge = "Adaptive Memory",
      skillFocus = "Grammar & Accuracy",
      icon = Icons.Default.Psychology,
      accentColor = AcadAmber,
      buttonLabel = "Start Mistake Recovery",
      testTag = "learn_open_mistake_recovery_btn",
      onClick = onOpenMistakeRecovery
    ),
    AiLabCardSpec(
      title = "Conversation Repair Lab",
      subtitle = "Practice recovering smoothly when conversations break down, speed up, or get confusing.",
      badge = "Fluency Reflex",
      skillFocus = "Listening & Recovery",
      icon = Icons.Default.Build,
      accentColor = AcadEmerald,
      buttonLabel = "Launch Repair Lab",
      testTag = "learn_open_repair_lab_btn",
      onClick = onOpenConversationRepairLab
    ),
    AiLabCardSpec(
      title = "Conversation Detective",
      subtitle = "Analyze realistic dialogues to spot cultural faux pas, tone mismatches, and hidden grammar clues.",
      badge = "Pragmatics Mystery",
      skillFocus = "Nuance & Register",
      icon = Icons.Default.Search,
      accentColor = AcadIndigo,
      buttonLabel = "Solve Dialogue Case",
      testTag = "learn_open_detective_btn",
      onClick = onOpenConversationDetective
    ),
    AiLabCardSpec(
      title = "Teach the AI (Feynman Lab)",
      subtitle = "Master rules deeply by explaining vocabulary and grammar to a curious AI student.",
      badge = "Active Recall",
      skillFocus = "Deep Mastery",
      icon = Icons.Default.School,
      accentColor = Color(0xFFF472B6),
      buttonLabel = "Start Teaching Session",
      testTag = "learn_open_teach_ai_btn",
      onClick = onOpenTeachTheAi
    )
  )

  LazyColumn(
    state = listState,
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 14.dp)
      .testTag("learn_ai_labs_list"),
    contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AcadBgCard),
        border = BorderStroke(1.dp, AcadCyan.copy(alpha = 0.35f))
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = AcadCyan,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Interactive AI Labs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = AcadTextPrimary
              )
            }
            Surface(
              color = AcadCyan.copy(alpha = 0.16f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Wifi,
                  contentDescription = null,
                  tint = AcadCyan,
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Live AI",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = AcadCyan
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Practice real-world scenarios, repair broken conversations, and reinforce your personal learning memory.",
            style = MaterialTheme.typography.bodySmall,
            color = AcadTextSecondary
          )
        }
      }
    }

    items(labs, key = { it.testTag }) { lab ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AcadBgCard),
        border = BorderStroke(1.dp, lab.accentColor.copy(alpha = 0.35f))
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = lab.accentColor.copy(alpha = 0.16f),
              modifier = Modifier.size(42.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = lab.icon,
                  contentDescription = null,
                  tint = lab.accentColor,
                  modifier = Modifier.size(22.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Surface(
                  color = lab.accentColor.copy(alpha = 0.16f),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                    text = lab.badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = lab.accentColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
                Text(
                  text = lab.skillFocus,
                  style = MaterialTheme.typography.labelSmall,
                  color = AcadTextSecondary
                )
              }
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = lab.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = AcadTextPrimary
              )
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = lab.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = AcadTextSecondary
          )
          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = lab.onClick,
            modifier = Modifier
              .fillMaxWidth()
              .height(42.dp)
              .testTag(lab.testTag),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = lab.accentColor,
              contentColor = Color(0xFF04101E)
            )
          ) {
            Text(
              text = lab.buttonLabel,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.ExtraBold
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BooksAndReadingLibraryShelfCard(
  languageCode: String,
  speechManager: SpeechManager?,
  completedBookIds: Set<String>,
  onCompleteBook: (id: String, title: String, scorePercent: Int, xpBonus: Int) -> Unit,
  onSaveVocabToFlashcard: (front: String, back: String, phonetic: String) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val allLoadedBooks = remember(languageCode) {
    mutableStateListOf<com.example.data.repository.LibraryBookItem>().apply {
      addAll(com.example.data.repository.InteractiveGamesCatalogAndAiService.getExpandedLibraryBooksCatalog(languageCode))
    }
  }
  val categories = remember {
    listOf(
      "All",
      "Personal Development",
      "Self-Help",
      "Classic Literature",
      "Philosophy",
      "Human Audio",
      "Full Text",
      "Motivation",
      "Business",
      "Fiction",
      "Short Stories",
      "Everyday Language"
    )
  }
  var selectedCategory by remember { mutableStateOf("All") }
  var selectedLetter by remember { mutableStateOf("All") }
  var searchQuery by remember { mutableStateOf("") }
  var showAllCatalogBrowser by remember { mutableStateOf(false) }
  var currentCatalogPage by remember { mutableIntStateOf(1) }
  var isLoadingMoreBooks by remember { mutableStateOf(false) }

  var activeBook by remember { mutableStateOf<com.example.data.repository.LibraryBookItem?>(null) }
  var currentPageIndex by remember(activeBook?.id) { mutableIntStateOf(0) }
  var selectedQuizOption by remember(activeBook?.id) { mutableStateOf<Int?>(null) }
  var savedWordsInBook by remember { mutableStateOf(setOf<String>()) }

  // Real chapter & human audio state for full books (Gutendex + Gutenberg + LibriVox)
  var loadedFullChapters by remember(activeBook?.id) {
    mutableStateOf<List<com.example.data.model.FullBookChapter>>(emptyList())
  }
  var selectedChapterIndex by remember(activeBook?.id) { mutableIntStateOf(0) }
  var isLoadingFullBook by remember(activeBook?.id) { mutableStateOf(false) }
  var isPlayingHumanAudio by remember(activeBook?.id, selectedChapterIndex) { mutableStateOf(false) }
  var humanAudioError by remember(activeBook?.id, selectedChapterIndex) { mutableStateOf<String?>(null) }
  var aiBookInsightText by remember(activeBook?.id, selectedChapterIndex) { mutableStateOf<String?>(null) }
  var isGeneratingAiBookInsight by remember(activeBook?.id) { mutableStateOf(false) }

  val mediaPlayerRef = remember { mutableStateOf<android.media.MediaPlayer?>(null) }
  var isMpPrepared by remember(activeBook?.id, selectedChapterIndex) { mutableStateOf(false) }
  var isMpPreparing by remember(activeBook?.id, selectedChapterIndex) { mutableStateOf(false) }

  fun releaseBookMediaPlayerSafely() {
    isMpPrepared = false
    isMpPreparing = false
    isPlayingHumanAudio = false
    val mp = mediaPlayerRef.value
    mediaPlayerRef.value = null
    if (mp != null) {
      try {
        mp.setOnPreparedListener(null)
        mp.setOnCompletionListener(null)
        mp.setOnErrorListener(null)
      } catch (_: Exception) {}
      try {
        mp.reset()
      } catch (_: Exception) {}
      try {
        mp.release()
      } catch (_: Exception) {}
    }
  }

  DisposableEffect(activeBook?.id, selectedChapterIndex) {
    onDispose {
      releaseBookMediaPlayerSafely()
    }
  }

  fun mergeDiscoveredBooks(newBooks: List<com.example.data.repository.LibraryBookItem>) {
    if (newBooks.isEmpty()) return
    val existingKeys = allLoadedBooks.map { "${it.title.lowercase().trim()}::${it.author.lowercase().trim()}" }.toMutableSet()
    for (b in newBooks) {
      val k = "${b.title.lowercase().trim()}::${b.author.lowercase().trim()}"
      if (existingKeys.add(k)) {
        allLoadedBooks.add(b)
      }
    }
  }

  // Automatically fetch page 1 from Gutendex, LibriVox, Open Library, and Google Books
  LaunchedEffect(languageCode, selectedCategory) {
    isLoadingMoreBooks = true
    val remotePage = com.example.data.repository.InteractiveGamesCatalogAndAiService.fetchMultiSourceBooksPage(
      languageCode = languageCode,
      searchQuery = searchQuery,
      category = selectedCategory,
      page = 1
    )
    mergeDiscoveredBooks(remotePage)
    isLoadingMoreBooks = false
  }

  // When a book with an AudiobookBook reference is selected, load real chapters & LibriVox audio
  LaunchedEffect(activeBook?.id) {
    val book = activeBook ?: return@LaunchedEffect
    val abRef = book.audiobookRef ?: return@LaunchedEffect
    if (abRef.parsedChapters.isNotEmpty()) {
      loadedFullChapters = abRef.parsedChapters
    }
    isLoadingFullBook = true
    val loadRes = com.example.data.repository.GutendexAndLibriVoxService.loadCompleteBookChaptersAndAudio(
      context = context,
      book = abRef
    )
    if (loadRes.chapters.isNotEmpty()) {
      loadedFullChapters = loadRes.chapters
    }
    isLoadingFullBook = false
  }

  val filteredBooks = remember(allLoadedBooks.size, selectedCategory, selectedLetter, searchQuery) {
    com.example.data.repository.InteractiveGamesCatalogAndAiService.filterLibraryBooksByLetterAndSearch(
      books = allLoadedBooks.toList(),
      selectedCategory = selectedCategory,
      selectedLetter = selectedLetter,
      searchQuery = searchQuery
    )
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("books_reading_library_section"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = AcadBgCard),
    border = BorderStroke(1.dp, AcadBorder.copy(alpha = 0.55f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Compact Header with Dynamic Book Count & View All Toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Books & Reading Library",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = AcadTextPrimary
          )
          Text(
            text = "Gutenberg • LibriVox • Open Library • Google Books",
            style = MaterialTheme.typography.labelSmall,
            color = AcadTextSecondary
          )
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Surface(
            color = AcadCyan.copy(alpha = 0.14f),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(
              text = "${filteredBooks.size} Books",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.ExtraBold,
              color = AcadCyan,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
          Surface(
            color = if (showAllCatalogBrowser) AcadCyan else AcadBgElevated,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .clickable { showAllCatalogBrowser = !showAllCatalogBrowser }
              .testTag("books_view_all_button")
          ) {
            Text(
              text = if (showAllCatalogBrowser) "Compact" else "View All",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.ExtraBold,
              color = if (showAllCatalogBrowser) Color(0xFF04101E) else AcadCyan,
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
            )
          }
        }
      }

      // Simple Horizontal Category Filter Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        categories.forEach { cat ->
          val isSelected = selectedCategory == cat
          FilterChip(
            selected = isSelected,
            onClick = { selectedCategory = cat },
            label = {
              Text(
                text = cat,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = AcadCyan.copy(alpha = 0.22f),
              selectedLabelColor = AcadCyan,
              containerColor = AcadBgElevated,
              labelColor = AcadTextSecondary
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = isSelected,
              borderColor = Color.Transparent,
              selectedBorderColor = AcadCyan.copy(alpha = 0.5f)
            ),
            modifier = Modifier.testTag("book_category_chip_${cat.lowercase().replace(" ", "_")}")
          )
        }
      }

      // Expandable "View All" Catalog Search & A–Z Browser
      AnimatedVisibility(visible = showAllCatalogBrowser) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { q ->
              searchQuery = q
              if (q.length >= 3) {
                scope.launch {
                  isLoadingMoreBooks = true
                  val found = com.example.data.repository.InteractiveGamesCatalogAndAiService.fetchMultiSourceBooksPage(
                    languageCode = languageCode,
                    searchQuery = q,
                    category = selectedCategory,
                    page = 1
                  )
                  mergeDiscoveredBooks(found)
                  isLoadingMoreBooks = false
                }
              }
            },
            placeholder = {
              Text(
                "Search title, author, Gutenberg, LibriVox, Open Library...",
                style = MaterialTheme.typography.bodySmall,
                color = AcadTextSecondary
              )
            },
            leadingIcon = {
              Icon(Icons.Default.Search, contentDescription = null, tint = AcadCyan, modifier = Modifier.size(16.dp))
            },
            trailingIcon = {
              if (searchQuery.isNotBlank()) {
                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                  Icon(Icons.Default.Close, contentDescription = "Clear", tint = AcadTextSecondary, modifier = Modifier.size(14.dp))
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("books_library_search_input")
          )
        }
      }

      if (isLoadingMoreBooks) {
        LinearProgressIndicator(
          modifier = Modifier
            .fillMaxWidth()
            .height(2.dp),
          color = AcadCyan
        )
      }

      // Featured & Paginated Books Horizontally Scrollable Carousel
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        filteredBooks.forEach { book ->
          val isSelected = activeBook?.id == book.id
          val isDone = completedBookIds.contains(book.id)
          Column(
            modifier = Modifier
              .width(142.dp)
              .clickable {
                activeBook = if (isSelected) null else book
                currentPageIndex = 0
                selectedChapterIndex = 0
                selectedQuizOption = null
              }
              .testTag("book_library_card_${book.id}")
          ) {
            val coverBg = Color(book.coverPrimaryHex)
            val rawAccent = Color(book.coverAccentHex)
            val coverLuminance = 0.299f * coverBg.red + 0.587f * coverBg.green + 0.114f * coverBg.blue
            val isLightCover = coverLuminance > 0.62f
            val coverTitleColor = if (isLightCover) Color(0xFF0F172A) else Color.White
            val coverSubtitleColor = if (isLightCover) Color(0xFF334155) else Color.White.copy(alpha = 0.85f)
            val coverAuthorColor = if (isLightCover) Color(0xFF0F766E) else rawAccent
            val badgeBgColor = if (isLightCover) Color(0xFF0F172A).copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.45f)
            val badgeTextColor = if (isLightCover) Color(0xFFF8FAFC) else rawAccent

            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .height(178.dp),
              shape = RoundedCornerShape(14.dp),
              color = coverBg,
              border = BorderStroke(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) AcadCyan else if (isLightCover) Color(0xFFCBD5E1) else rawAccent.copy(alpha = 0.45f)
              )
            ) {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBgColor
                  ) {
                    Text(
                      text = book.difficulty,
                      color = badgeTextColor,
                      fontSize = 9.5.sp,
                      fontWeight = FontWeight.ExtraBold,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                  if (isDone) {
                    Icon(
                      imageVector = Icons.Default.CheckCircle,
                      contentDescription = "Completed",
                      tint = AcadEmerald,
                      modifier = Modifier.size(15.dp)
                    )
                  } else if (book.hasHumanAudio) {
                    Icon(
                      imageVector = Icons.Default.Headphones,
                      contentDescription = "LibriVox Human Audio",
                      tint = badgeTextColor,
                      modifier = Modifier.size(14.dp)
                    )
                  }
                }

                Column(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text(
                    text = book.title.uppercase(),
                    color = coverTitleColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    lineHeight = 14.5.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                  )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                  HorizontalDivider(
                    color = if (isLightCover) Color(0xFF94A3B8).copy(alpha = 0.45f) else rawAccent.copy(alpha = 0.45f)
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = book.author,
                    color = coverAuthorColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = "${book.sourceName} • ${book.estimatedMinutes}m",
                    color = coverSubtitleColor,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
            Spacer(modifier = Modifier.height(5.dp))
            Text(
              text = book.title,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = AcadTextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = buildString {
                append(book.sourceName)
                if (book.hasHumanAudio) append(" • Audio")
                else if (book.hasFullText) append(" • Text")
              },
              style = MaterialTheme.typography.labelSmall,
              color = if (book.hasHumanAudio) AcadAmber else AcadTextSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Load More Card at end of carousel (and also inside View All)
        Surface(
          modifier = Modifier
            .width(124.dp)
            .height(178.dp)
            .clickable(enabled = !isLoadingMoreBooks) {
              scope.launch {
                isLoadingMoreBooks = true
                val nextPage = currentCatalogPage + 1
                val more = com.example.data.repository.InteractiveGamesCatalogAndAiService.fetchMultiSourceBooksPage(
                  languageCode = languageCode,
                  searchQuery = searchQuery,
                  category = selectedCategory,
                  page = nextPage
                )
                mergeDiscoveredBooks(more)
                currentCatalogPage = nextPage
                isLoadingMoreBooks = false
              }
            }
            .testTag("books_load_more_button"),
          shape = RoundedCornerShape(14.dp),
          color = AcadBgElevated,
          border = BorderStroke(1.dp, AcadCyan.copy(alpha = 0.4f))
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Load More Books",
              tint = AcadCyan,
              modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = if (isLoadingMoreBooks) "Loading..." else "Load More Books",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.ExtraBold,
              color = AcadCyan,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Gutendex, LibriVox & Open Library",
              style = MaterialTheme.typography.labelSmall,
              color = AcadTextSecondary,
              textAlign = TextAlign.Center,
              fontSize = 9.5.sp
            )
          }
        }
      }

      // Interactive Book & Audiobook Reader when a book is selected
      val book = activeBook
      if (book != null) {
        val activeChapter = loadedFullChapters.getOrNull(selectedChapterIndex)
        val pages = if (activeChapter != null && activeChapter.paragraphs.isNotEmpty()) {
          activeChapter.paragraphs.chunked(3).map { it.joinToString("\n\n") }
        } else {
          book.pages
        }
        val safePage = currentPageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
        val pageText = pages.getOrElse(safePage) { "" }
        val chapterAudioUrl = activeChapter?.librivoxAudioUrl
          ?: book.audiobookRef?.librivoxEditions?.firstOrNull()?.sections?.getOrNull(selectedChapterIndex)?.listenUrl

        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("book_interactive_reader_card"),
          shape = RoundedCornerShape(16.dp),
          color = AcadBgElevated,
          border = BorderStroke(1.dp, AcadCyan.copy(alpha = 0.65f))
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
                  text = "${book.sourceName} • ${book.difficulty} • Page ${safePage + 1}/${pages.size}",
                  color = AcadCyan,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.ExtraBold
                )
                Text(
                  text = book.title,
                  color = AcadTextPrimary,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.ExtraBold
                )
                Text(
                  text = "By ${book.author}",
                  color = AcadTextSecondary,
                  fontSize = 12.sp
                )
              }
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!chapterAudioUrl.isNullOrBlank()) {
                  FilledTonalButton(
                    onClick = {
                      if (isPlayingHumanAudio || isMpPreparing) {
                        if (isMpPrepared) {
                          try {
                            mediaPlayerRef.value?.pause()
                          } catch (_: Exception) {}
                          isPlayingHumanAudio = false
                        } else {
                          releaseBookMediaPlayerSafely()
                        }
                      } else {
                        humanAudioError = null
                        val existing = mediaPlayerRef.value
                        if (existing != null && isMpPrepared) {
                          try {
                            existing.start()
                            isPlayingHumanAudio = true
                          } catch (_: Exception) {
                            releaseBookMediaPlayerSafely()
                          }
                        } else if (existing != null) {
                          releaseBookMediaPlayerSafely()
                        }
                        if (mediaPlayerRef.value == null) {
                          try {
                            isMpPreparing = true
                            isMpPrepared = false
                            val mp = android.media.MediaPlayer().apply {
                              setDataSource(chapterAudioUrl)
                              setOnPreparedListener { player ->
                                if (mediaPlayerRef.value === player) {
                                  isMpPreparing = false
                                  isMpPrepared = true
                                  try {
                                    player.start()
                                    isPlayingHumanAudio = true
                                  } catch (_: Exception) {
                                    releaseBookMediaPlayerSafely()
                                  }
                                }
                              }
                              setOnCompletionListener {
                                isPlayingHumanAudio = false
                              }
                              setOnErrorListener { _, _, _ ->
                                releaseBookMediaPlayerSafely()
                                humanAudioError = "LibriVox stream unavailable right now. Using AI Read Aloud."
                                speechManager?.speak(pageText, languageCode, 0.94f)
                                true
                              }
                              prepareAsync()
                            }
                            mediaPlayerRef.value = mp
                          } catch (_: Exception) {
                            releaseBookMediaPlayerSafely()
                            humanAudioError = "Unable to stream chapter audio. Using AI Read Aloud."
                            speechManager?.speak(pageText, languageCode, 0.94f)
                          }
                        }
                      }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                      containerColor = if (isPlayingHumanAudio) AcadAmber.copy(alpha = 0.25f) else AcadAmber.copy(alpha = 0.16f),
                      contentColor = AcadAmber
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                  ) {
                    Icon(
                      imageVector = if (isPlayingHumanAudio) Icons.Default.Pause else Icons.Default.Headphones,
                      contentDescription = "Human Audiobook",
                      modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = if (isPlayingHumanAudio) "Pause" else "Audiobook",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.ExtraBold
                    )
                  }
                }

                IconButton(
                  onClick = { speechManager?.speak(pageText, languageCode, 0.92f) },
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AcadCyan.copy(alpha = 0.2f))
                    .testTag("book_reader_listen_btn")
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Listen to page",
                    tint = AcadCyan,
                    modifier = Modifier.size(18.dp)
                  )
                }
                IconButton(
                  onClick = {
                    speechManager?.stopSpeaking()
                    releaseBookMediaPlayerSafely()
                    activeBook = null
                  },
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AcadBgCard)
                ) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close book reader",
                    tint = AcadTextSecondary,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }

            if (isLoadingFullBook) {
              LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = AcadCyan)
            }

            if (!humanAudioError.isNullOrBlank()) {
              Text(
                text = humanAudioError.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = AcadAmber
              )
            }

            // Chapter Selector Row when book has multiple chapters
            if (loadedFullChapters.size > 1) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                loadedFullChapters.forEachIndexed { chIdx, ch ->
                  val isChSelected = selectedChapterIndex == chIdx
                  FilterChip(
                    selected = isChSelected,
                    onClick = {
                      selectedChapterIndex = chIdx
                      currentPageIndex = 0
                    },
                    label = {
                      Text(
                        text = ch.title.take(28),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isChSelected) FontWeight.ExtraBold else FontWeight.Medium
                      )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = AcadCyan.copy(alpha = 0.22f),
                      selectedLabelColor = AcadCyan,
                      containerColor = AcadBgCard,
                      labelColor = AcadTextSecondary
                    )
                  )
                }
              }
            }

            // Page Text Box
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = AcadBgCard,
              border = BorderStroke(1.dp, AcadBorder.copy(alpha = 0.55f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = pageText,
                color = AcadTextPrimary,
                fontSize = 14.5.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(14.dp)
              )
            }

            // Page Navigation + AI Study Guide Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedButton(
                onClick = { if (safePage > 0) currentPageIndex = safePage - 1 },
                enabled = safePage > 0,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, AcadBorder)
              ) {
                Text("Previous", fontSize = 12.sp)
              }
              Surface(
                color = AcadIndigo.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .clickable(enabled = !isGeneratingAiBookInsight) {
                    scope.launch {
                      isGeneratingAiBookInsight = true
                      val prompt = "Provide a concise 3-bullet CEFR ${book.difficulty} summary and 3 key vocabulary explanations for this excerpt from '${book.title}' by ${book.author}:\n\n${pageText.take(600)}"
                      val res = com.example.ai.GeminiTutorService.generateGeminiText(prompt, "READING_SUMMARY")
                      aiBookInsightText = res ?: "Study Summary (${book.title}):\n• ${book.summaryTagline}\n• Key Vocabulary: ${book.glossary.entries.take(3).joinToString { "${it.key} (${it.value})" }}"
                      isGeneratingAiBookInsight = false
                    }
                  }
                  .testTag("book_ai_study_guide_btn")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AcadIndigo, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (isGeneratingAiBookInsight) "Analyzing..." else "AI Study Notes",
                    color = AcadIndigo,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold
                  )
                }
              }
              Button(
                onClick = { if (safePage < pages.size - 1) currentPageIndex = safePage + 1 },
                enabled = safePage < pages.size - 1,
                colors = ButtonDefaults.buttonColors(
                  containerColor = AcadCyan,
                  contentColor = Color(0xFF04101E)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("book_reader_next_page_btn")
              ) {
                Text("Next Page", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
              }
            }

            if (!aiBookInsightText.isNullOrBlank()) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = AcadIndigo.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, AcadIndigo.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = aiBookInsightText.orEmpty(),
                  style = MaterialTheme.typography.bodySmall,
                  color = AcadTextPrimary,
                  modifier = Modifier.padding(12.dp)
                )
              }
            }

            // Interactive Vocabulary Glossary Chips (Tap to hear & save)
            Text(
              text = "Key Book Vocabulary (Tap to listen & save to Flashcards):",
              color = AcadCyan,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
            FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              book.glossary.forEach { (term, definition) ->
                val isSaved = savedWordsInBook.contains("${book.id}_$term")
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = if (isSaved) AcadEmerald.copy(alpha = 0.18f) else AcadBgCard,
                  border = BorderStroke(1.dp, if (isSaved) AcadEmerald else AcadCyan.copy(alpha = 0.4f)),
                  modifier = Modifier.clickable {
                    speechManager?.speak(term, languageCode)
                    if (!isSaved) {
                      onSaveVocabToFlashcard(term, definition, "")
                      savedWordsInBook = savedWordsInBook + "${book.id}_$term"
                    }
                  }
                ) {
                  Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                      text = if (isSaved) "$term ✓ Saved" else "+ $term",
                      color = if (isSaved) AcadEmerald else AcadTextPrimary,
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 12.sp
                    )
                    Text(
                      text = definition,
                      color = AcadTextSecondary,
                      fontSize = 11.sp
                    )
                  }
                }
              }
            }

            // Comprehension Check
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = AcadBgCard,
              border = BorderStroke(1.dp, AcadBorder.copy(alpha = 0.55f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = "Book Comprehension Check (+45 XP)",
                  color = AcadAmber,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 12.5.sp
                )
                val activeQuestion = book.questions.firstOrNull()
                if (activeQuestion != null) {
                  Text(
                    text = activeQuestion.question,
                    color = AcadTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                  )
                  for ((idx, opt) in activeQuestion.options.withIndex()) {
                    val isChosen = selectedQuizOption == idx
                    val isRight = idx == activeQuestion.correctIndex
                    val bgColor = when {
                      selectedQuizOption != null && isRight -> AcadEmerald.copy(alpha = 0.2f)
                      isChosen && !isRight -> AcadRose.copy(alpha = 0.2f)
                      else -> AcadBgElevated
                    }
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = bgColor,
                      border = BorderStroke(
                        1.dp,
                        if (selectedQuizOption != null && isRight) AcadEmerald else AcadBorder
                      ),
                      modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                          selectedQuizOption = idx
                          if (idx == activeQuestion.correctIndex) {
                            onCompleteBook(book.id, "Book: ${book.title}", 100, 45)
                          }
                        }
                        .testTag("book_quiz_option_$idx")
                    ) {
                      Text(
                        text = opt,
                        color = AcadTextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
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

