package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ai.AiErrorHandler
import com.example.ai.UpgradeSheetMode
import com.example.data.model.ReadingAcademyPassage
import com.example.data.repository.AccessPolicyFeatureKeys
import com.example.ui.components.LingoCalmAiErrorBanner
import com.example.ui.components.LingoCleanCard
import com.example.ui.components.LingoEmptyOrErrorCard
import com.example.ui.components.LingoGoldLockIcon
import com.example.ui.components.LingoPrimaryGradientButton
import com.example.ui.components.LingoSecondaryChip
import com.example.ui.components.LingoTokens
import com.example.ui.components.LingoUnderlineTabBar
import com.example.ui.components.lingoAmbientBackdrop
import com.example.ui.components.springTap

/**
 * Strips trailing language suffixes like " (English)" or " (Spanish)" from story titles.
 */
fun cleanStoryTitle(rawTitle: String): String {
  return rawTitle
    .replace(Regex("\\s*\\((?:English|Spanish|French|German|Italian|Japanese|Korean|Chinese|Mandarin|Arabic|Portuguese|Hindi|Turkish|Russian|Dutch|Swedish|Polish|Greek|Hebrew|Vietnamese|Thai|Indonesian|Swahili|[A-Za-z]{2,18})\\)\\s*$", RegexOption.IGNORE_CASE), "")
    .trim()
    .ifBlank { rawTitle.trim() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryBrowserScreen(
  passages: List<ReadingAcademyPassage>,
  bookmarkedIds: Set<String>,
  completedIds: Set<String>,
  savedParagraphPositions: Map<String, Int>,
  initialCefr: String,
  isGeneratingStory: Boolean,
  generationErrorMessage: String?,
  isProUnlocked: Boolean = true,
  onGenerateNewStory: (cefrLevel: String, category: String, topic: String) -> Unit,
  onToggleBookmark: (String) -> Unit,
  onSelectStory: (ReadingAcademyPassage) -> Unit,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)

  var selectedTabIdx by remember { mutableIntStateOf(0) } // 0 = All, 1 = Graded, 2 = Cultural
  var selectedCefrFilter by remember { mutableStateOf("All") }
  var selectedTopicFilter by remember { mutableStateOf("All") }
  var selectedLengthFilter by remember { mutableStateOf("Any") } // Any, Short, Medium, Long
  var showBookmarkedOnly by remember { mutableStateOf(false) }
  var showFilterSheet by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var showGenerateSheet by remember { mutableStateOf(false) }

  var genStoryCefr by remember(initialCefr) { mutableStateOf(initialCefr.ifBlank { "B1" }) }
  var genStoryCategory by remember { mutableStateOf("Graded Story") }
  var genStoryLength by remember { mutableStateOf("Medium") }
  var genStoryTopic by remember { mutableStateOf("") }

  val tabs = remember { listOf("All", "Graded", "Cultural") }
  val topics = remember(passages) {
    listOf("All") + passages.map { it.category }.filter { it.isNotBlank() }.distinct()
  }
  val activeFilterCount = (if (selectedCefrFilter != "All") 1 else 0) +
    (if (selectedTopicFilter != "All") 1 else 0) +
    (if (selectedLengthFilter != "Any") 1 else 0)

  val filteredPassages = remember(
    passages,
    selectedTabIdx,
    selectedCefrFilter,
    selectedTopicFilter,
    selectedLengthFilter,
    showBookmarkedOnly,
    bookmarkedIds,
    searchQuery
  ) {
    passages
      .distinctBy { it.id }
      .filter { p ->
        val matchesTab = when (selectedTabIdx) {
          1 -> p.category.contains("Graded", ignoreCase = true) || p.category.contains("Everyday", ignoreCase = true) || p.category.contains("Story", ignoreCase = true)
          2 -> p.category.contains("Cultur", ignoreCase = true) || p.category.contains("Travel", ignoreCase = true) || p.category.contains("History", ignoreCase = true)
          else -> true
        }
        val matchesCefr = selectedCefrFilter == "All" || p.cefrLevel.equals(selectedCefrFilter, ignoreCase = true)
        val matchesTopic = selectedTopicFilter == "All" || p.category.equals(selectedTopicFilter, ignoreCase = true)
        val wordCount = p.paragraphs.sumOf { it.split(Regex("\\s+")).size }
        val readMinutes = maxOf(2, wordCount / 35)
        val matchesLength = when (selectedLengthFilter) {
          "Short" -> readMinutes <= 3
          "Medium" -> readMinutes in 4..6
          "Long" -> readMinutes >= 7
          else -> true
        }
        val matchesBookmark = !showBookmarkedOnly || bookmarkedIds.contains(p.id)
        val q = searchQuery.trim()
        val matchesSearch = q.isBlank() ||
          cleanStoryTitle(p.title).contains(q, ignoreCase = true) ||
          p.category.contains(q, ignoreCase = true) ||
          p.paragraphs.any { it.contains(q, ignoreCase = true) }
        matchesTab && matchesCefr && matchesTopic && matchesLength && matchesBookmark && matchesSearch
      }
  }

  fun openNewStoryIfPro() {
    if (!isProUnlocked) {
      AiErrorHandler.requestUpgrade(
        mode = UpgradeSheetMode.BOOKS,
        featureKey = AccessPolicyFeatureKeys.PERSONALIZED_MATERIAL,
        headline = "Unlock all stories and audio",
        reason = "Generate custom stories, read graded stories, and unlock interactive quizzes with LingoSphere Pro."
      )
      return
    }
    showGenerateSheet = true
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .lingoAmbientBackdrop()
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .testTag("story_browser_screen")
  ) {
    // Clean header: Back, Stories, Bookmark filter, Small gradient New Story button
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(LingoTokens.bgGlassCard)
            .border(1.dp, LingoTokens.borderSubtle, CircleShape)
            .testTag("stories_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Back",
            tint = LingoTokens.textPrimary
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = "Stories",
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          color = LingoTokens.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        IconButton(
          onClick = { showBookmarkedOnly = !showBookmarkedOnly },
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(LingoTokens.bgGlassCard)
            .border(
              1.dp,
              if (showBookmarkedOnly) LingoTokens.brandCyan else LingoTokens.borderSubtle,
              CircleShape
            )
            .testTag("stories_bookmark_filter_button")
        ) {
          Icon(
            imageVector = if (showBookmarkedOnly) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = "Bookmarked stories",
            tint = if (showBookmarkedOnly) LingoTokens.brandCyan else LingoTokens.textSecondary,
            modifier = Modifier.size(20.dp)
          )
        }

        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(LingoTokens.brandHorizontalGradient)
            .springTap { openNewStoryIfPro() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("generate_new_reading_story_button")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = if (isProUnlocked) Icons.Outlined.AutoAwesome else Icons.Outlined.Lock,
              contentDescription = null,
              tint = if (isProUnlocked) LingoTokens.onGradientInk else LingoTokens.onGradientInk,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = "New Story",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = LingoTokens.onGradientInk,
              maxLines = 1
            )
          }
        }
      }
    }

    // Search + Filter icon row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = {
          Text(
            "Search stories...",
            fontSize = 14.sp,
            color = LingoTokens.textSecondary
          )
        },
        leadingIcon = {
          Icon(Icons.Outlined.Search, contentDescription = null, tint = LingoTokens.textSecondary)
        },
        trailingIcon = {
          if (searchQuery.isNotBlank()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = LingoTokens.textSecondary)
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = LingoTokens.brandCyan,
          unfocusedBorderColor = LingoTokens.borderSubtle,
          focusedContainerColor = LingoTokens.bgGlassCard,
          unfocusedContainerColor = LingoTokens.bgGlassCard,
          focusedTextColor = LingoTokens.textPrimary,
          unfocusedTextColor = LingoTokens.textPrimary
        ),
        modifier = Modifier
          .weight(1f)
          .testTag("search_reading_stories_input")
      )

      Box {
        IconButton(
          onClick = { showFilterSheet = true },
          modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(LingoTokens.bgGlassCard)
            .border(
              1.dp,
              if (activeFilterCount > 0) LingoTokens.brandCyan else LingoTokens.borderSubtle,
              RoundedCornerShape(18.dp)
            )
            .testTag("stories_filter_button")
        ) {
          Icon(
            imageVector = Icons.Outlined.Tune,
            contentDescription = "Filter stories",
            tint = if (activeFilterCount > 0) LingoTokens.brandCyan else LingoTokens.textPrimary
          )
        }
        if (activeFilterCount > 0) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(top = 6.dp, end = 6.dp)
              .size(16.dp)
              .clip(CircleShape)
              .background(LingoTokens.brandCyan)
          ) {
            Text(
              text = "$activeFilterCount",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = LingoTokens.onGradientInk
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // ONE underline tab bar: All | Graded | Cultural
    LingoUnderlineTabBar(
      tabs = tabs,
      selectedIndex = selectedTabIdx,
      onSelectTab = { selectedTabIdx = it },
      modifier = Modifier.padding(horizontal = 20.dp)
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 112.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      if (isGeneratingStory) {
        item {
          LingoCleanCard(contentPadding = 18.dp) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              CircularProgressIndicator(
                color = LingoTokens.brandCyan,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(22.dp)
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Writing your story...",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = LingoTokens.textPrimary
                )
                Text(
                  text = "Tailoring vocabulary for $genStoryCefr level",
                  fontSize = 13.sp,
                  color = LingoTokens.textSecondary
                )
              }
            }
          }
        }
      }

      if (!generationErrorMessage.isNullOrBlank()) {
        item {
          LingoCalmAiErrorBanner(
            message = generationErrorMessage,
            onRetry = {
              if (isProUnlocked) {
                onGenerateNewStory(genStoryCefr, genStoryCategory, genStoryTopic)
              } else {
                openNewStoryIfPro()
              }
            },
            autoDismissMillis = 0L
          )
        }
      }

      if (filteredPassages.isEmpty()) {
        item {
          LingoEmptyOrErrorCard(
            title = "No matching stories",
            message = "Try adjusting your filters or create a fresh story on any topic.",
            actionLabel = if (activeFilterCount > 0) "Clear filters" else "New Story",
            onAction = {
              if (activeFilterCount > 0) {
                selectedCefrFilter = "All"
                selectedTopicFilter = "All"
                selectedLengthFilter = "Any"
                showBookmarkedOnly = false
              } else {
                openNewStoryIfPro()
              }
            }
          )
        }
      } else {
        items(filteredPassages, key = { it.id }) { passage ->
          val isDone = completedIds.contains(passage.id)
          val isBookmarked = bookmarkedIds.contains(passage.id)
          val savedPara = savedParagraphPositions[passage.id] ?: 0
          val wordCount = passage.paragraphs.sumOf { it.split(Regex("\\s+")).size }
          val readMinutes = maxOf(2, wordCount / 35)
          val displayTitle = remember(passage.title) { cleanStoryTitle(passage.title) }

          LingoCleanCard(
            onClick = {
              if (!isProUnlocked) {
                AiErrorHandler.requestUpgrade(
                  mode = UpgradeSheetMode.BOOKS,
                  featureKey = AccessPolicyFeatureKeys.BOOKS_READING,
                  headline = "Unlock all stories and audio",
                  reason = "Stories, word coaching, and comprehension quizzes are included with LingoSphere Pro."
                )
              } else {
                onSelectStory(passage)
              }
            },
            contentPadding = 20.dp,
            modifier = Modifier.testTag("reading_story_chip_${passage.id}")
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top
            ) {
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                // Plain text level + dot + duration (no boxed tags)
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Text(
                    text = "${passage.cefrLevel} · $readMinutes min",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LingoTokens.textSecondary
                  )
                  if (isDone) {
                    Icon(
                      imageVector = Icons.Outlined.CheckCircle,
                      contentDescription = "Completed",
                      tint = LingoTokens.statusOnline,
                      modifier = Modifier.size(15.dp)
                    )
                  } else if (savedPara > 0) {
                    Text(
                      text = "· In progress",
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Medium,
                      color = LingoTokens.brandCyan
                    )
                  }
                }

                Text(
                  text = displayTitle,
                  fontSize = 17.sp,
                  lineHeight = 23.sp,
                  fontWeight = FontWeight.Bold,
                  color = LingoTokens.textPrimary,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )

                Text(
                  text = passage.paragraphs.firstOrNull().orEmpty(),
                  fontSize = 14.sp,
                  lineHeight = 20.sp,
                  color = LingoTokens.textSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }

              Spacer(modifier = Modifier.width(10.dp))

              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                if (!isProUnlocked) {
                  LingoGoldLockIcon(size = 16.dp)
                }
                IconButton(
                  onClick = { onToggleBookmark(passage.id) },
                  modifier = Modifier.size(44.dp)
                ) {
                  Icon(
                    imageVector = if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (isBookmarked) "Remove bookmark" else "Save story",
                    tint = if (isBookmarked) LingoTokens.brandCyan else LingoTokens.textSecondary,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  // Filter Bottom Sheet (CEFR Level, Topic, Length)
  if (showFilterSheet) {
    ModalBottomSheet(
      onDismissRequest = { showFilterSheet = false },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      containerColor = Color(0xFF0E1422),
      shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Filter stories",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = LingoTokens.textPrimary
          )
          TextButton(
            onClick = {
              selectedCefrFilter = "All"
              selectedTopicFilter = "All"
              selectedLengthFilter = "Any"
            }
          ) {
            Text("Reset", color = LingoTokens.brandCyan, fontWeight = FontWeight.SemiBold)
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Level", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LingoTokens.textSecondary)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("All", "A1", "A2", "B1", "B2", "C1", "C2").forEach { level ->
              LingoSecondaryChip(
                text = if (level == "All") "All levels" else level,
                selected = selectedCefrFilter == level,
                onClick = { selectedCefrFilter = level }
              )
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Length", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LingoTokens.textSecondary)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("Any", "Short", "Medium", "Long").forEach { lengthOpt ->
              LingoSecondaryChip(
                text = lengthOpt,
                selected = selectedLengthFilter == lengthOpt,
                onClick = { selectedLengthFilter = lengthOpt },
                modifier = Modifier.weight(1f)
              )
            }
          }
        }

        if (topics.size > 1) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Topic", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LingoTokens.textSecondary)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              topics.forEach { topic ->
                LingoSecondaryChip(
                  text = topic,
                  selected = selectedTopicFilter == topic,
                  onClick = { selectedTopicFilter = topic }
                )
              }
            }
          }
        }

        LingoPrimaryGradientButton(
          text = "Apply filters",
          onClick = { showFilterSheet = false },
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }

  // New Story Bottom Sheet
  if (showGenerateSheet) {
    ModalBottomSheet(
      onDismissRequest = { showGenerateSheet = false },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      containerColor = Color(0xFF0E1422),
      shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text(
          text = "New story",
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = LingoTokens.textPrimary
        )

        OutlinedTextField(
          value = genStoryTopic,
          onValueChange = { genStoryTopic = it },
          label = { Text("Topic (optional)") },
          placeholder = { Text("e.g., A rainy afternoon in Kyoto") },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LingoTokens.brandCyan,
            unfocusedBorderColor = LingoTokens.borderSubtle,
            focusedContainerColor = LingoTokens.bgGlassCard,
            unfocusedContainerColor = LingoTokens.bgGlassCard,
            focusedTextColor = LingoTokens.textPrimary,
            unfocusedTextColor = LingoTokens.textPrimary
          ),
          modifier = Modifier.fillMaxWidth()
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Level", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LingoTokens.textSecondary)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("A1", "A2", "B1", "B2", "C1", "C2").forEach { level ->
              LingoSecondaryChip(
                text = level,
                selected = genStoryCefr == level,
                onClick = { genStoryCefr = level }
              )
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Length", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LingoTokens.textSecondary)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("Short", "Medium", "Long").forEach { length ->
              LingoSecondaryChip(
                text = length,
                selected = genStoryLength == length,
                onClick = { genStoryLength = length },
                modifier = Modifier.weight(1f)
              )
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Style", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LingoTokens.textSecondary)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("Graded Story", "Cultural", "Everyday Life", "Travel", "Work").forEach { cat ->
              LingoSecondaryChip(
                text = cat,
                selected = genStoryCategory == cat,
                onClick = { genStoryCategory = cat }
              )
            }
          }
        }

        LingoPrimaryGradientButton(
          text = "Generate",
          icon = Icons.Outlined.AutoAwesome,
          onClick = {
            showGenerateSheet = false
            val enrichedTopic = if (genStoryTopic.isBlank()) {
              "$genStoryLength $genStoryCategory story"
            } else {
              "$genStoryTopic ($genStoryLength length)"
            }
            onGenerateNewStory(genStoryCefr, genStoryCategory, enrichedTopic)
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("confirm_generate_story_button")
        )
      }
    }
  }
}
