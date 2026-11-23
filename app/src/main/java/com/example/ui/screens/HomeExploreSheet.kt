package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Language
import com.example.data.model.UserProfile
import com.example.ui.components.LingoCleanCard
import com.example.ui.components.LingoEmptyOrErrorCard
import com.example.ui.components.LingoListRow
import com.example.ui.components.LingoSectionHeader
import com.example.ui.components.LingoStatusBadge
import com.example.ui.components.LingoTokens
import com.example.ui.components.LingoTopBar
import com.example.ui.components.formatCefrLevelLabel
import com.example.ui.components.lingoAmbientBackdrop

enum class ExploreCategoryRoute {
  OVERVIEW,
  COMMUNITY_CULTURE,
  AI_PRACTICE_LABS,
  PROGRESS_LANGUAGES,
  LEARNING_TOOLS
}

private data class ExploreDestinationItem(
  val id: String,
  val title: String,
  val subtitle: String,
  val categoryTitle: String,
  val icon: ImageVector,
  val accent: Color,
  val onClick: () -> Unit
)

private data class ExploreSheetCategoryTileSpec(
  val route: ExploreCategoryRoute,
  val title: String,
  val subtitle: String,
  val countLabel: String,
  val icon: ImageVector,
  val accent: Color,
  val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeExploreBottomSheet(
  onNavigateToPractice: () -> Unit,
  onOpenCultureJourney: () -> Unit,
  onNavigateToVideos: () -> Unit,
  onOpenSpeakingRooms: () -> Unit,
  onOpenLanguageExchange: () -> Unit,
  onOpenSmartMatching: () -> Unit,
  onOpenDailyChallenges: () -> Unit,
  onOpenProgressDashboard: () -> Unit,
  onOpenExploreLanguages: () -> Unit,
  onOpenImmersionMode: () -> Unit,
  onOpenConversationReplay: () -> Unit,
  onOpenAccentStudio: () -> Unit,
  onOpenLanguagePortfolio: () -> Unit,
  onOpenMistakeRecovery: () -> Unit,
  onOpenCameraLens: () -> Unit,
  onOpenConversationRepairLab: () -> Unit,
  onOpenLanguageEmergencyMode: () -> Unit,
  onOpenAiDebatePartner: () -> Unit,
  onOpenExplainItThreeWays: () -> Unit,
  onOpenConversationDetective: () -> Unit,
  onOpenTeachTheAi: () -> Unit,
  onOpenInviteFriends: () -> Unit,
  onDismiss: () -> Unit,
  userProfile: UserProfile? = null,
  completedLessonsCount: Int = 0,
  weeklySessionsCount: Int = 0,
  cefrStageLabel: String = "",
  onOpenVocabularyBuilder: () -> Unit = {},
  onOpenListeningPractice: () -> Unit = {},
  onOpenWritingStudio: () -> Unit = {},
  onOpenGrammarStudio: () -> Unit = {},
  onOpenRoleplayStudio: () -> Unit = {},
  onOpenReadingStudio: () -> Unit = {},
  onOpenPronunciation: () -> Unit = {},
  onOpenCategoryRoute: ((ExploreCategoryRoute) -> Unit)? = null
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var currentRoute by remember { mutableStateOf(ExploreCategoryRoute.OVERVIEW) }
  var searchQuery by remember { mutableStateOf("") }

  val communityItems = remember(onDismiss) {
    listOf(
      ExploreDestinationItem("games", "Play & Learn Games", "Interactive language games & challenges", "Community & Culture", Icons.Outlined.SportsEsports, LingoTokens.brandCyan) {
        onDismiss(); onNavigateToPractice()
      },
      ExploreDestinationItem("culture", "Culture Journey", "Global traditions, idioms & passport", "Community & Culture", Icons.Outlined.Public, LingoTokens.brandIndigo) {
        onDismiss(); onOpenCultureJourney()
      },
      ExploreDestinationItem("videos", "Videos", "Native clips with transcripts & quizzes", "Community & Culture", Icons.Outlined.PlayCircle, LingoTokens.brandCyan) {
        onDismiss(); onNavigateToVideos()
      },
      ExploreDestinationItem("rooms", "Speaking Rooms", "Live group audio practice stages", "Community & Culture", Icons.Outlined.Mic, LingoTokens.accentEmerald) {
        onDismiss(); onOpenSpeakingRooms()
      },
      ExploreDestinationItem("exchange", "Language Exchange", "Chat and practice with native speakers", "Community & Culture", Icons.Outlined.SwapHoriz, LingoTokens.brandCyan) {
        onDismiss(); onOpenLanguageExchange()
      },
      ExploreDestinationItem("matching", "Smart Matching", "Find compatible language partners", "Community & Culture", Icons.Outlined.Shuffle, LingoTokens.brandIndigo) {
        onDismiss(); onOpenSmartMatching()
      },
      ExploreDestinationItem("invite", "Invite Friends", "Share LingoSphere and study together", "Community & Culture", Icons.Outlined.Share, LingoTokens.brandIndigo) {
        onDismiss(); onOpenInviteFriends()
      }
    )
  }

  val aiLabItems = remember(onDismiss) {
    listOf(
      ExploreDestinationItem("immersion", "Living Immersion Mode", "Interactive café, airport & everyday dialogues", "AI Practice Labs", Icons.Outlined.AutoAwesome, LingoTokens.brandCyan) {
        onDismiss(); onOpenImmersionMode()
      },
      ExploreDestinationItem("camera", "Camera Speaking Lens", "Translate objects & practice live vocabulary", "AI Practice Labs", Icons.Outlined.CameraAlt, LingoTokens.accentEmerald) {
        onDismiss(); onOpenCameraLens()
      },
      ExploreDestinationItem("emergency", "Emergency Phrase Mode", "Instant travel, medical & urgent help", "AI Practice Labs", Icons.Outlined.LocalHospital, LingoTokens.accentRose) {
        onDismiss(); onOpenLanguageEmergencyMode()
      },
      ExploreDestinationItem("debate", "AI Debate Partner", "Practice persuasive speaking on topics", "AI Practice Labs", Icons.Outlined.Forum, LingoTokens.brandIndigo) {
        onDismiss(); onOpenAiDebatePartner()
      },
      ExploreDestinationItem("repair", "Conversation Repair Lab", "Handle misunderstandings smoothly", "AI Practice Labs", Icons.Outlined.RecordVoiceOver, LingoTokens.accentEmerald) {
        onDismiss(); onOpenConversationRepairLab()
      },
      ExploreDestinationItem("explain3", "Explain It 3 Ways", "Hear beginner, intermediate & advanced phrasing", "AI Practice Labs", Icons.Outlined.AutoAwesome, LingoTokens.brandCyan) {
        onDismiss(); onOpenExplainItThreeWays()
      },
      ExploreDestinationItem("detective", "Conversation Detective", "Spot tone & cultural nuance clues", "AI Practice Labs", Icons.Outlined.Search, LingoTokens.brandIndigo) {
        onDismiss(); onOpenConversationDetective()
      },
      ExploreDestinationItem("recovery", "Mistake Recovery", "Turn past mistakes into targeted drills", "AI Practice Labs", Icons.Outlined.Psychology, LingoTokens.brandCyan) {
        onDismiss(); onOpenMistakeRecovery()
      },
      ExploreDestinationItem("teach", "Teach the AI", "Deepen mastery by explaining concepts", "AI Practice Labs", Icons.Outlined.School, LingoTokens.brandIndigo) {
        onDismiss(); onOpenTeachTheAi()
      },
      ExploreDestinationItem("accent", "Accent & Listening Studio", "Explore regional accents & playback speeds", "AI Practice Labs", Icons.Outlined.GraphicEq, LingoTokens.brandCyan) {
        onDismiss(); onOpenAccentStudio()
      },
      ExploreDestinationItem("replay", "Conversation Replay", "Review & refine your past speaking turns", "AI Practice Labs", Icons.Outlined.History, LingoTokens.brandIndigo) {
        onDismiss(); onOpenConversationReplay()
      }
    )
  }

  val progressItems = remember(onDismiss) {
    listOf(
      ExploreDestinationItem("challenges", "Daily Challenges", "Daily streak missions & habit coach", "Progress & Languages", Icons.Outlined.Celebration, LingoTokens.accentGold) {
        onDismiss(); onOpenDailyChallenges()
      },
      ExploreDestinationItem("dashboard", "Progress Dashboard", "Weekly study time, XP & fluency insights", "Progress & Languages", Icons.Outlined.Insights, LingoTokens.brandCyan) {
        onDismiss(); onOpenProgressDashboard()
      },
      ExploreDestinationItem("portfolio", "Language Portfolio", "Speaking samples & skill passport", "Progress & Languages", Icons.Outlined.WorkOutline, LingoTokens.accentEmerald) {
        onDismiss(); onOpenLanguagePortfolio()
      },
      ExploreDestinationItem("languages", "Explore Languages", "Browse all ${Language.SUPPORTED_LANGUAGES.size} supported languages", "Progress & Languages", Icons.Outlined.Public, LingoTokens.brandIndigo) {
        onDismiss(); onOpenExploreLanguages()
      }
    )
  }

  val learningToolItems = remember(onDismiss) {
    listOf(
      ExploreDestinationItem("listening", "Books & Listening Studio", "Full books, audiobooks & read-along audio", "Learning Tools", Icons.Outlined.Headphones, LingoTokens.brandIndigo) {
        onDismiss(); onOpenListeningPractice()
      },
      ExploreDestinationItem("reading", "Reading Academy", "A1–C2 CEFR stories, bookmarks & quizzes", "Learning Tools", Icons.AutoMirrored.Outlined.MenuBook, LingoTokens.brandCyan) {
        onDismiss(); onOpenReadingStudio()
      },
      ExploreDestinationItem("vocab", "Vocabulary Builder", "Spaced repetition flashcards & decks", "Learning Tools", Icons.Outlined.Style, LingoTokens.accentEmerald) {
        onDismiss(); onOpenVocabularyBuilder()
      },
      ExploreDestinationItem("writing", "Writing Studio", "Guided prompts & AI grammar critique", "Learning Tools", Icons.Outlined.Edit, LingoTokens.brandCyan) {
        onDismiss(); onOpenWritingStudio()
      },
      ExploreDestinationItem("grammar", "Grammar Studio", "Interactive rules & sentence patterns", "Learning Tools", Icons.Outlined.AutoFixHigh, LingoTokens.brandIndigo) {
        onDismiss(); onOpenGrammarStudio()
      },
      ExploreDestinationItem("roleplay", "Roleplay Studio", "Real-life conversations & missions", "Learning Tools", Icons.Outlined.TheaterComedy, LingoTokens.brandCyan) {
        onDismiss(); onOpenRoleplayStudio()
      },
      ExploreDestinationItem("pronunciation", "Pronunciation Coach", "Syllable feedback & accent drills", "Learning Tools", Icons.Outlined.RecordVoiceOver, LingoTokens.accentEmerald) {
        onDismiss(); onOpenPronunciation()
      }
    )
  }

  val allItems = remember(communityItems, aiLabItems, progressItems, learningToolItems) {
    aiLabItems + learningToolItems + communityItems + progressItems
  }

  val filteredSearchItems = remember(searchQuery, allItems) {
    val q = searchQuery.trim()
    if (q.isBlank()) emptyList()
    else allItems.filter {
      it.title.contains(q, ignoreCase = true) ||
        it.subtitle.contains(q, ignoreCase = true) ||
        it.categoryTitle.contains(q, ignoreCase = true)
    }
  }

  val categories = remember {
    listOf(
      ExploreSheetCategoryTileSpec(
        route = ExploreCategoryRoute.COMMUNITY_CULTURE,
        title = "Community & Culture",
        subtitle = "Rooms, partners, games & media",
        countLabel = "7 tools",
        icon = Icons.Outlined.Public,
        accent = LingoTokens.brandIndigo,
        testTag = "explore_category_community"
      ),
      ExploreSheetCategoryTileSpec(
        route = ExploreCategoryRoute.AI_PRACTICE_LABS,
        title = "AI Practice Labs",
        subtitle = "Immersion, camera, debate & repair",
        countLabel = "11 labs",
        icon = Icons.Outlined.AutoAwesome,
        accent = LingoTokens.brandCyan,
        testTag = "explore_category_ai_labs"
      ),
      ExploreSheetCategoryTileSpec(
        route = ExploreCategoryRoute.PROGRESS_LANGUAGES,
        title = "Progress & Languages",
        subtitle = "Stats, challenges, portfolio & switch",
        countLabel = "Overview",
        icon = Icons.Outlined.Insights,
        accent = LingoTokens.accentEmerald,
        testTag = "explore_category_progress"
      ),
      ExploreSheetCategoryTileSpec(
        route = ExploreCategoryRoute.LEARNING_TOOLS,
        title = "Learning Tools",
        subtitle = "Reading, writing, grammar & vocab",
        countLabel = "7 studios",
        icon = Icons.AutoMirrored.Outlined.MenuBook,
        accent = LingoTokens.brandIndigo,
        testTag = "explore_category_tools"
      )
    )
  }

  fun openCategory(route: ExploreCategoryRoute) {
    if (onOpenCategoryRoute != null) {
      onDismiss()
      onOpenCategoryRoute(route)
    } else {
      currentRoute = route
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = LingoTokens.bgCanvas,
    contentColor = LingoTokens.textPrimary,
    modifier = Modifier.testTag("home_explore_all_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .imePadding()
    ) {
      if (currentRoute == ExploreCategoryRoute.OVERVIEW) {
        LazyColumn(
          modifier = Modifier.fillMaxWidth(),
          contentPadding = PaddingValues(start = LingoTokens.screenMargin, end = LingoTokens.screenMargin, bottom = 32.dp),
          verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Explore",
                  style = MaterialTheme.typography.headlineMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = LingoTokens.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Studios, AI labs, speaking rooms & tools.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = LingoTokens.textSecondary
                )
              }
              IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(48.dp)
              ) {
                Icon(
                  imageVector = Icons.Outlined.Close,
                  contentDescription = "Close Explore",
                  tint = LingoTokens.textSecondary
                )
              }
            }
          }

          item {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = {
                Text(
                  "Search labs, studios, games, rooms...",
                  style = MaterialTheme.typography.bodyMedium,
                  color = LingoTokens.textSecondary
                )
              },
              leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = LingoTokens.brandCyan)
              },
              trailingIcon = {
                if (searchQuery.isNotBlank()) {
                  IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = LingoTokens.textSecondary)
                  }
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(LingoTokens.cardRadius),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LingoTokens.brandCyan,
                unfocusedBorderColor = LingoTokens.innerHighlight,
                focusedContainerColor = LingoTokens.bgElevated,
                unfocusedContainerColor = LingoTokens.bgElevated,
                focusedTextColor = LingoTokens.textPrimary,
                unfocusedTextColor = LingoTokens.textPrimary
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("explore_search_input")
            )
          }

          if (searchQuery.isNotBlank()) {
            if (filteredSearchItems.isEmpty()) {
              item {
                LingoEmptyOrErrorCard(
                  title = "No matching features",
                  message = "Try searching for \"immersion\", \"debate\", \"reading\", or \"languages\".",
                  actionLabel = "Clear Search",
                  onAction = { searchQuery = "" }
                )
              }
            } else {
              items(filteredSearchItems, key = { it.id }) { item ->
                ExploreSheetRow(item)
              }
            }
          } else {
            item {
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LingoSectionHeader(title = "Featured", subtitle = "Recommended starting points")
                LazyRow(
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  items(
                    listOf(
                      aiLabItems.first(),
                      learningToolItems.first(),
                      aiLabItems[1]
                    ),
                    key = { it.id }
                  ) { featured ->
                    LingoCleanCard(
                      onClick = featured.onClick,
                      contentPadding = 16.dp,
                      modifier = Modifier.width(232.dp)
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                      ) {
                        Surface(
                          shape = RoundedCornerShape(14.dp),
                          color = featured.accent.copy(alpha = 0.14f),
                          modifier = Modifier.size(44.dp)
                        ) {
                          Box(contentAlignment = Alignment.Center) {
                            Icon(featured.icon, contentDescription = null, tint = featured.accent, modifier = Modifier.size(22.dp))
                          }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                          Text(
                            text = featured.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = LingoTokens.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                          Spacer(modifier = Modifier.height(2.dp))
                          Text(
                            text = featured.subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = LingoTokens.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                        }
                      }
                    }
                  }
                }
              }
            }

            item {
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LingoSectionHeader(title = "Categories", subtitle = "Choose a learning space")
                categories.chunked(2).forEach { pair ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                  ) {
                    pair.forEach { cat ->
                      ExploreCategoryTile(
                        spec = cat,
                        onClick = { openCategory(cat.route) },
                        modifier = Modifier.weight(1f)
                      )
                    }
                    if (pair.size == 1) {
                      Spacer(modifier = Modifier.weight(1f))
                    }
                  }
                }
              }
            }
          }
        }
      } else {
        BackHandler { currentRoute = ExploreCategoryRoute.OVERVIEW }
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = { currentRoute = ExploreCategoryRoute.OVERVIEW },
              modifier = Modifier.size(48.dp)
            ) {
              Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to Explore", tint = LingoTokens.textPrimary)
            }
            Text(
              text = when (currentRoute) {
                ExploreCategoryRoute.AI_PRACTICE_LABS -> "AI Practice Labs"
                ExploreCategoryRoute.PROGRESS_LANGUAGES -> "Progress & Languages"
                ExploreCategoryRoute.COMMUNITY_CULTURE -> "Community & Culture"
                ExploreCategoryRoute.LEARNING_TOOLS -> "Learning Tools"
                else -> "Explore"
              },
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = LingoTokens.textPrimary,
              modifier = Modifier.weight(1f)
            )
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(48.dp)
            ) {
              Icon(Icons.Outlined.Close, contentDescription = "Close", tint = LingoTokens.textSecondary)
            }
          }

          val listToShow = when (currentRoute) {
            ExploreCategoryRoute.COMMUNITY_CULTURE -> communityItems
            ExploreCategoryRoute.AI_PRACTICE_LABS -> aiLabItems
            ExploreCategoryRoute.PROGRESS_LANGUAGES -> progressItems
            ExploreCategoryRoute.LEARNING_TOOLS -> learningToolItems
            else -> allItems
          }

          LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = LingoTokens.screenMargin, end = LingoTokens.screenMargin, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            if (currentRoute == ExploreCategoryRoute.PROGRESS_LANGUAGES && userProfile != null) {
              item {
                ExploreProgressSummaryCard(
                  userProfile = userProfile,
                  completedLessonsCount = completedLessonsCount,
                  weeklySessionsCount = weeklySessionsCount,
                  cefrStageLabel = cefrStageLabel
                )
              }
            }
            items(listToShow, key = { it.id }) { item ->
              ExploreSheetRow(item)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ExploreCategoryTile(
  spec: ExploreSheetCategoryTileSpec,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  LingoCleanCard(
    onClick = onClick,
    contentPadding = 16.dp,
    modifier = modifier.testTag(spec.testTag)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = spec.accent.copy(alpha = 0.14f),
        modifier = Modifier.size(44.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = spec.icon,
            contentDescription = null,
            tint = spec.accent,
            modifier = Modifier.size(22.dp)
          )
        }
      }
      LingoStatusBadge(text = spec.countLabel, color = spec.accent)
    }
    Spacer(modifier = Modifier.height(14.dp))
    Text(
      text = spec.title,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = LingoTokens.textPrimary,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = spec.subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = LingoTokens.textSecondary,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun ExploreSheetRow(item: ExploreDestinationItem) {
  LingoListRow(
    title = item.title,
    subtitle = item.subtitle,
    icon = item.icon,
    iconTint = item.accent,
    onClick = item.onClick
  )
}

@Composable
fun ExploreAiPracticeLabsScreen(
  onOpenImmersionMode: () -> Unit,
  onOpenCameraLens: () -> Unit,
  onOpenLanguageEmergencyMode: () -> Unit,
  onOpenAiDebatePartner: () -> Unit,
  onOpenConversationRepairLab: () -> Unit,
  onOpenExplainItThreeWays: () -> Unit,
  onOpenConversationDetective: () -> Unit,
  onOpenMistakeRecovery: () -> Unit,
  onOpenTeachTheAi: () -> Unit,
  onOpenAccentStudio: () -> Unit,
  onOpenConversationReplay: () -> Unit,
  onBack: () -> Unit
) {
  BackHandler { onBack() }
  val realLifePractice = listOf(
    ExploreDestinationItem("camera", "Camera Speaking Lens", "Translate objects & practice live vocabulary", "Real-Life Practice", Icons.Outlined.CameraAlt, LingoTokens.accentEmerald, onOpenCameraLens),
    ExploreDestinationItem("emergency", "Emergency Phrase Mode", "Instant travel, medical & urgent assistance", "Real-Life Practice", Icons.Outlined.LocalHospital, LingoTokens.accentRose, onOpenLanguageEmergencyMode)
  )
  val conversationSkills = listOf(
    ExploreDestinationItem("debate", "AI Debate Partner", "Practice persuasive speaking with neutral rubric", "Conversation Skills", Icons.Outlined.Forum, LingoTokens.brandIndigo, onOpenAiDebatePartner),
    ExploreDestinationItem("repair", "Conversation Repair Lab", "Handle misunderstandings & ask to repeat naturally", "Conversation Skills", Icons.Outlined.RecordVoiceOver, LingoTokens.accentEmerald, onOpenConversationRepairLab),
    ExploreDestinationItem("explain3", "Explain It 3 Ways", "Beginner, intermediate & advanced phrasing", "Conversation Skills", Icons.Outlined.AutoAwesome, LingoTokens.brandCyan, onOpenExplainItThreeWays),
    ExploreDestinationItem("detective", "Conversation Detective", "Spot tone, politeness & cultural nuance clues", "Conversation Skills", Icons.Outlined.Search, LingoTokens.brandIndigo, onOpenConversationDetective)
  )
  val improveSkills = listOf(
    ExploreDestinationItem("recovery", "Mistake Recovery", "Targeted drills built from your real mistakes", "Improve Your Skills", Icons.Outlined.Psychology, LingoTokens.brandCyan, onOpenMistakeRecovery),
    ExploreDestinationItem("teach", "Teach the AI", "Explain concepts step-by-step to an AI student", "Improve Your Skills", Icons.Outlined.School, LingoTokens.brandIndigo, onOpenTeachTheAi),
    ExploreDestinationItem("accent", "Accent & Listening Studio", "Explore regional accents & playback speeds", "Improve Your Skills", Icons.Outlined.GraphicEq, LingoTokens.brandCyan, onOpenAccentStudio),
    ExploreDestinationItem("replay", "Conversation Replay", "Review and refine your past speaking turns", "Improve Your Skills", Icons.Outlined.History, LingoTokens.brandIndigo, onOpenConversationReplay)
  )

  var selectedLabTab by remember { mutableStateOf("real_life") }
  val labTabs = listOf(
    "real_life" to "Real-life",
    "conversation" to "Conversation",
    "improve" to "Improve"
  )
  val activeGroupTitle = when (selectedLabTab) {
    "conversation" -> "Conversation Skills"
    "improve" -> "Improve Your Skills"
    else -> "Real-Life Practice"
  }
  val activeGroupItems = when (selectedLabTab) {
    "conversation" -> conversationSkills
    "improve" -> improveSkills
    else -> realLifePractice
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .lingoAmbientBackdrop()
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .testTag("explore_ai_practice_labs_screen")
  ) {
    LingoTopBar(
      title = "AI Practice Labs",
      subtitle = "Practice real conversations and build confidence.",
      onBack = onBack
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = LingoTokens.screenMargin, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      item(key = "featured_living_immersion") {
        LingoCleanCard(
          onClick = onOpenImmersionMode,
          contentPadding = 18.dp,
          modifier = Modifier.testTag("featured_living_immersion_card")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = LingoTokens.brandCyan.copy(alpha = 0.16f),
              modifier = Modifier.size(48.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = LingoTokens.brandCyan, modifier = Modifier.size(24.dp))
              }
            }
            Column(modifier = Modifier.weight(1f)) {
              LingoStatusBadge(text = "Featured Lab", color = LingoTokens.brandCyan)
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Living Immersion Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LingoTokens.textPrimary
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Speak with AI characters who remember your progress.",
                style = MaterialTheme.typography.bodyMedium,
                color = LingoTokens.textSecondary
              )
            }
          }
        }
      }

      item(key = "practice_labs_category_chips") {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LingoTokens.bgElevated)
            .padding(4.dp)
            .testTag("practice_labs_category_chips"),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          labTabs.forEach { (id, label) ->
            val selected = selectedLabTab == id
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .then(
                  if (selected) Modifier.background(LingoTokens.brandHorizontalGradient)
                  else Modifier
                )
                .clickable { selectedLabTab = id }
                .padding(vertical = 10.dp, horizontal = 6.dp)
                .testTag("practice_labs_chip_$id"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = label,
                color = if (selected) LingoTokens.onGradientInk else LingoTokens.textSecondary,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }

      item(key = "active_lab_group_$selectedLabTab") {
        LabGroupSection(title = activeGroupTitle, items = activeGroupItems)
      }
    }
  }
}

@Composable
private fun LabGroupSection(
  title: String,
  items: List<ExploreDestinationItem>
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(
      text = title.uppercase(),
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.8.sp,
      color = LingoTokens.textSecondary
    )
    items.forEach { item ->
      ExploreSheetRow(item)
    }
  }
}

@Composable
fun ExploreProgressAndLanguagesScreen(
  userProfile: UserProfile,
  completedLessonsCount: Int,
  weeklySessionsCount: Int,
  cefrStageLabel: String,
  onOpenDailyChallenges: () -> Unit,
  onOpenProgressDashboard: () -> Unit,
  onOpenLanguagePortfolio: () -> Unit,
  onOpenExploreLanguages: () -> Unit,
  onOpenInviteFriends: () -> Unit,
  onBack: () -> Unit
) {
  BackHandler { onBack() }
  val items = listOf(
    ExploreDestinationItem("challenges", "Daily Challenges", "Daily streak missions & habit coach", "Progress", Icons.Outlined.Celebration, LingoTokens.accentGold, onOpenDailyChallenges),
    ExploreDestinationItem("dashboard", "Progress Dashboard", "Weekly study time, XP & fluency trends", "Progress", Icons.Outlined.Insights, LingoTokens.brandCyan, onOpenProgressDashboard),
    ExploreDestinationItem("portfolio", "Language Portfolio", "Speaking samples & practical skill passport", "Progress", Icons.Outlined.WorkOutline, LingoTokens.accentEmerald, onOpenLanguagePortfolio),
    ExploreDestinationItem("languages", "Explore Languages", "Switch target or native across ${Language.SUPPORTED_LANGUAGES.size} languages", "Languages", Icons.Outlined.Public, LingoTokens.brandIndigo, onOpenExploreLanguages),
    ExploreDestinationItem("invite", "Invite Friends", "Share LingoSphere and study together", "Community", Icons.Outlined.Share, LingoTokens.brandCyan, onOpenInviteFriends)
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .lingoAmbientBackdrop()
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .testTag("explore_progress_languages_screen")
  ) {
    LingoTopBar(
      title = "Progress & Languages",
      subtitle = "Track fluency, challenges & language settings",
      onBack = onBack
    )
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = LingoTokens.screenMargin, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        ExploreProgressSummaryCard(
          userProfile = userProfile,
          completedLessonsCount = completedLessonsCount,
          weeklySessionsCount = weeklySessionsCount,
          cefrStageLabel = cefrStageLabel
        )
      }
      items(items, key = { it.id }) { item ->
        ExploreSheetRow(item)
      }
    }
  }
}

@Composable
private fun ExploreProgressSummaryCard(
  userProfile: UserProfile,
  completedLessonsCount: Int,
  weeklySessionsCount: Int,
  cefrStageLabel: String
) {
  val targetLang = Language.findByCode(userProfile.targetLanguageCode)
  val cefrDisplay = formatCefrLevelLabel(cefrStageLabel.ifBlank { userProfile.proficiencyLevel })
  LingoCleanCard(contentPadding = 18.dp) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Your Progress",
          style = MaterialTheme.typography.labelMedium,
          color = LingoTokens.textSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = targetLang.name,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = LingoTokens.textPrimary
        )
      }
      LingoStatusBadge(text = cefrDisplay, color = LingoTokens.brandCyan)
    }
    Spacer(modifier = Modifier.height(16.dp))
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val spacing = if (maxWidth < 340.dp) 8.dp else 12.dp
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing)
      ) {
        ProgressMetricPill("Streak", "${userProfile.currentStreak}d", LingoTokens.accentGold, Modifier.weight(1f))
        ProgressMetricPill("Lessons", "$completedLessonsCount", LingoTokens.brandCyan, Modifier.weight(1f))
        ProgressMetricPill("Sessions", "$weeklySessionsCount", LingoTokens.accentEmerald, Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun ProgressMetricPill(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = LingoTokens.bgSubtle,
    modifier = modifier
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
      Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
      Spacer(modifier = Modifier.height(2.dp))
      Text(text = label, style = MaterialTheme.typography.labelMedium, color = LingoTokens.textSecondary)
    }
  }
}

@Composable
fun ExploreCommunityCultureScreen(
  onNavigateToPractice: () -> Unit,
  onOpenCultureJourney: () -> Unit,
  onNavigateToVideos: () -> Unit,
  onOpenSpeakingRooms: () -> Unit,
  onOpenLanguageExchange: () -> Unit,
  onOpenSmartMatching: () -> Unit,
  onOpenInviteFriends: () -> Unit,
  onBack: () -> Unit
) {
  BackHandler { onBack() }
  val items = listOf(
    ExploreDestinationItem("rooms", "Speaking Rooms", "Live group audio practice stages", "Community", Icons.Outlined.Mic, LingoTokens.accentEmerald, onOpenSpeakingRooms),
    ExploreDestinationItem("exchange", "Language Exchange", "Chat and practice with native speakers", "Community", Icons.Outlined.SwapHoriz, LingoTokens.brandCyan, onOpenLanguageExchange),
    ExploreDestinationItem("matching", "Smart Matching", "Find compatible language partners", "Community", Icons.Outlined.Shuffle, LingoTokens.brandIndigo, onOpenSmartMatching),
    ExploreDestinationItem("culture", "Culture Journey", "Global traditions, idioms & passport", "Culture", Icons.Outlined.Public, LingoTokens.brandCyan, onOpenCultureJourney),
    ExploreDestinationItem("videos", "Videos", "Native clips with transcripts & quizzes", "Media", Icons.Outlined.PlayCircle, LingoTokens.brandIndigo, onNavigateToVideos),
    ExploreDestinationItem("games", "Play & Learn Games", "Interactive language games & challenges", "Games", Icons.Outlined.SportsEsports, LingoTokens.brandCyan, onNavigateToPractice),
    ExploreDestinationItem("invite", "Invite Friends", "Share LingoSphere and study together", "Share", Icons.Outlined.Share, LingoTokens.brandIndigo, onOpenInviteFriends)
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .lingoAmbientBackdrop()
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .testTag("explore_community_culture_screen")
  ) {
    LingoTopBar(
      title = "Community & Culture",
      subtitle = "Practice with speakers, culture guides & media",
      onBack = onBack
    )
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = LingoTokens.screenMargin, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(items, key = { it.id }) { item -> ExploreSheetRow(item) }
    }
  }
}

@Composable
fun ExploreLearningToolsScreen(
  onOpenReadingStudio: () -> Unit,
  onOpenListeningPractice: () -> Unit,
  onOpenVocabularyBuilder: () -> Unit,
  onOpenWritingStudio: () -> Unit,
  onOpenGrammarStudio: () -> Unit,
  onOpenRoleplayStudio: () -> Unit,
  onOpenPronunciation: () -> Unit,
  onBack: () -> Unit
) {
  BackHandler { onBack() }
  val items = listOf(
    ExploreDestinationItem("listening", "Books & Listening Studio", "Full books, audiobooks & synced audio", "Studios", Icons.Outlined.Headphones, LingoTokens.brandIndigo, onOpenListeningPractice),
    ExploreDestinationItem("reading", "Reading Academy", "A1–C2 CEFR stories, bookmarks & quizzes", "Studios", Icons.AutoMirrored.Outlined.MenuBook, LingoTokens.brandCyan, onOpenReadingStudio),
    ExploreDestinationItem("vocab", "Vocabulary Builder", "Spaced repetition flashcards & decks", "Studios", Icons.Outlined.Style, LingoTokens.accentEmerald, onOpenVocabularyBuilder),
    ExploreDestinationItem("writing", "Writing Studio", "Guided prompts & AI grammar critique", "Studios", Icons.Outlined.Edit, LingoTokens.brandCyan, onOpenWritingStudio),
    ExploreDestinationItem("grammar", "Grammar Studio", "Interactive rules & sentence patterns", "Studios", Icons.Outlined.AutoFixHigh, LingoTokens.brandIndigo, onOpenGrammarStudio),
    ExploreDestinationItem("roleplay", "Roleplay Studio", "Real-life conversations & missions", "Studios", Icons.Outlined.TheaterComedy, LingoTokens.brandCyan, onOpenRoleplayStudio),
    ExploreDestinationItem("pronunciation", "Pronunciation Coach", "Syllable feedback & accent drills", "Studios", Icons.Outlined.RecordVoiceOver, LingoTokens.accentEmerald, onOpenPronunciation)
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .lingoAmbientBackdrop()
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .testTag("explore_learning_tools_screen")
  ) {
    LingoTopBar(
      title = "Learning Tools",
      subtitle = "Reading, writing, listening, grammar & speaking studios",
      onBack = onBack
    )
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = LingoTokens.screenMargin, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(items, key = { it.id }) { item -> ExploreSheetRow(item) }
    }
  }
}
