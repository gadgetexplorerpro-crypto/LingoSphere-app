package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Language
import com.example.data.model.LingoVideo
import com.example.data.model.SavedVideo
import com.example.data.model.UserProfile
import com.example.data.model.VideoPlaylist
import com.example.data.model.VideoWatchHistory
import com.example.data.repository.YouTubeVideoService
import com.example.speech.SpeechManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.sp
import com.example.ui.components.sanitizeCreatorName
import com.example.ui.theme.LingoThemeController
import kotlinx.coroutines.launch

private val VideoNavyBg: Color
    get() = if (LingoThemeController.isDarkActive) Color(0xFF060D1D) else Color(0xFFF8FAFC)
private val VideoBgMid: Color
    get() = VideoNavyBg
private val VideoBgBottom: Color
    get() = VideoNavyBg
private val VideoCardBg: Color
    get() = if (LingoThemeController.isDarkActive) Color(0xFF0D162B) else Color(0xFFFFFFFF)
private val VideoBorderColor: Color
    get() = if (LingoThemeController.isDarkActive) Color(0xFF1D2E52) else Color(0xFFE2E8F0)
private val VideoTextPrimary: Color
    get() = if (LingoThemeController.isDarkActive) Color(0xFFF8FAFC) else Color(0xFF0F172A)
private val VideoTextSecondary: Color
    get() = if (LingoThemeController.isDarkActive) Color(0xFF94A3B8) else Color(0xFF475569)
private val VideoAccentCyan = Color(0xFF06B6D4)
private val VideoAccentIndigo = Color(0xFF6366F1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LingoVideosScreen(
    userProfile: UserProfile,
    speechManager: SpeechManager,
    savedVideos: List<SavedVideo>,
    playlists: List<VideoPlaylist>,
    watchHistory: List<VideoWatchHistory>,
    onSelectLanguageCode: (String) -> Unit,
    onToggleSaveVideo: (LingoVideo) -> Unit,
    onCreatePlaylist: (String, String, String, LingoVideo?) -> Unit,
    onAddVideoToPlaylist: (VideoPlaylist, LingoVideo) -> Unit,
    onRemoveVideoFromPlaylist: (VideoPlaylist, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onUpdateWatchProgress: (LingoVideo, Long, Boolean, Int, Int) -> Unit,
    onSaveVocabToFlashcard: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val currentLanguage = remember(userProfile.targetLanguageCode) {
        Language.findByCode(userProfile.targetLanguageCode)
    }

    var activeSection by remember { mutableIntStateOf(0) } // 0=Discover, 1=Saved, 2=Playlists, 3=History
    var selectedLevel by remember { mutableStateOf("All") }
    var selectedCategory by remember { mutableStateOf("All Topics") }
    var searchQuery by remember { mutableStateOf("") }
    var submittedSearchQuery by remember { mutableStateOf("") }

    var videos by remember { mutableStateOf<List<LingoVideo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(0) }
    var isLiveYouTubeApi by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    var selectedVideoForPlayback by remember { mutableStateOf<LingoVideo?>(null) }
    var selectedPlaylistDetail by remember { mutableStateOf<VideoPlaylist?>(null) }
    var showCreatePlaylistModal by remember { mutableStateOf(false) }
    var videoToQuickAddToPlaylist by remember { mutableStateOf<LingoVideo?>(null) }

    val levels = listOf("All", "Beginner", "Intermediate", "Advanced")
    val categories = listOf(
        "All Topics",
        "Conversation",
        "Grammar",
        "Vocabulary",
        "Pronunciation",
        "Listening",
        "Culture & Travel"
    )

    fun fetchVideos(page: Int = 0) {
        scope.launch {
            if (page == 0) {
                isLoading = true
                currentPage = 0
            } else {
                isLoadingMore = true
            }
            val result = YouTubeVideoService.discoverVideos(
                languageCode = currentLanguage.code,
                proficiencyLevel = selectedLevel,
                topicCategory = if (selectedCategory == "All Topics") "" else selectedCategory,
                searchQuery = submittedSearchQuery,
                page = page
            )
            if (page == 0) {
                videos = result.videos
            } else {
                val existingIds = videos.map { it.videoId }.toSet()
                val newVideos = result.videos.filter { it.videoId !in existingIds }
                videos = videos + newVideos
                currentPage = page
            }
            isLiveYouTubeApi = result.isLiveYouTubeApi
            statusMessage = result.statusMessage
            isLoading = false
            isLoadingMore = false
        }
    }

    LaunchedEffect(currentLanguage.code, selectedLevel, selectedCategory, submittedSearchQuery) {
        fetchVideos(page = 0)
    }

    // Full-screen Video Player + AI Learning Assistant Sheet
    selectedVideoForPlayback?.let { activeVideo ->
        val isSaved = savedVideos.any { it.videoId == activeVideo.videoId }
        BackHandler {
            selectedVideoForPlayback = null
        }
        LingoVideoPlayerDetailView(
            video = activeVideo,
            userProfile = userProfile,
            isSaved = isSaved,
            playlists = playlists,
            speechManager = speechManager,
            onToggleSave = { onToggleSaveVideo(activeVideo) },
            onAddToPlaylist = { playlist -> onAddVideoToPlaylist(playlist, activeVideo) },
            onCreatePlaylistWithVideo = { name, desc ->
                onCreatePlaylist(name, desc, currentLanguage.code, activeVideo)
            },
            onSaveVocabToFlashcard = { term, translation, phonetic ->
                onSaveVocabToFlashcard(term, translation, phonetic)
            },
            onUpdateWatchProgress = { watchedSec, completedQuiz, quizScorePercent, xpReward ->
                onUpdateWatchProgress(activeVideo, watchedSec, completedQuiz, quizScorePercent, xpReward)
            },
            onClose = { selectedVideoForPlayback = null }
        )
        return
    }

    if (videoToQuickAddToPlaylist != null) {
        val targetVideo = videoToQuickAddToPlaylist!!
        AddToPlaylistDialog(
            video = targetVideo,
            playlists = playlists,
            onAddExisting = { playlist ->
                onAddVideoToPlaylist(playlist, targetVideo)
                videoToQuickAddToPlaylist = null
            },
            onCreateNew = { name, desc ->
                onCreatePlaylist(name, desc, currentLanguage.code, targetVideo)
                videoToQuickAddToPlaylist = null
            },
            onDismiss = { videoToQuickAddToPlaylist = null }
        )
    }

    if (showCreatePlaylistModal) {
        CreatePlaylistOnlyDialog(
            languageName = currentLanguage.name,
            onDismiss = { showCreatePlaylistModal = false },
            onCreate = { name, desc ->
                onCreatePlaylist(name, desc, currentLanguage.code, null)
                showCreatePlaylistModal = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VideoNavyBg)
    ) {
        // Branded LingoSphere Videos Header (Home-page style)
        Surface(
            color = VideoCardBg,
            border = BorderStroke(1.dp, VideoBorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(VideoAccentCyan, VideoAccentIndigo)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.OndemandVideo,
                                contentDescription = "LingoSphere Videos",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LingoSphere Videos",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = VideoTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Interactive ${currentLanguage.name} Video Lessons & AI Tutor",
                                fontSize = 11.5.sp,
                                color = VideoTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = { fetchVideos(page = 0) },
                        modifier = Modifier.testTag("refresh_videos_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Videos",
                            tint = VideoAccentCyan
                        )
                    }
                }

                // Sub-navigation Pills: Discover, Saved, Playlists, History
                ScrollableTabRow(
                    selectedTabIndex = activeSection,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    contentColor = VideoTextPrimary,
                    divider = {}
                ) {
                    val sections = listOf(
                        Triple("Discover", Icons.Default.Explore, videos.size),
                        Triple("Saved", Icons.Default.Bookmark, savedVideos.size),
                        Triple("Playlists", Icons.AutoMirrored.Filled.PlaylistPlay, playlists.size),
                        Triple("History", Icons.Default.History, watchHistory.size)
                    )
                    sections.forEachIndexed { idx, (label, icon, count) ->
                        Tab(
                            selected = activeSection == idx,
                            onClick = {
                                activeSection = idx
                                selectedPlaylistDetail = null
                            },
                            modifier = Modifier.testTag("videos_section_tab_$idx"),
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = if (activeSection == idx) VideoAccentCyan else VideoTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (idx > 0 && count > 0) "$label ($count)" else label,
                                        color = if (activeSection == idx) VideoAccentCyan else VideoTextSecondary,
                                        fontWeight = if (activeSection == idx) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        when (activeSection) {
            0 -> {
                DiscoverVideosSection(
                    currentLanguage = currentLanguage,
                    onSelectLanguageCode = onSelectLanguageCode,
                    levels = levels,
                    selectedLevel = selectedLevel,
                    onSelectLevel = { selectedLevel = it },
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onSubmitSearch = {
                        submittedSearchQuery = searchQuery.trim()
                        focusManager.clearFocus()
                    },
                    onClearSearch = {
                        searchQuery = ""
                        submittedSearchQuery = ""
                        focusManager.clearFocus()
                    },
                    isLoading = isLoading,
                    isLoadingMore = isLoadingMore,
                    onLoadMore = { fetchVideos(page = currentPage + 1) },
                    statusMessage = statusMessage,
                    videos = videos,
                    savedVideoIds = savedVideos.map { it.videoId }.toSet(),
                    onPlayVideo = { video ->
                        onUpdateWatchProgress(video, 5L, false, 0, 0)
                        selectedVideoForPlayback = video
                    },
                    onToggleSaveVideo = onToggleSaveVideo,
                    onQuickAddToPlaylist = { videoToQuickAddToPlaylist = it },
                    onOpenOnYouTube = { videoId ->
                        val target = videos.find { it.videoId == videoId }
                        if (target != null) {
                            onUpdateWatchProgress(target, 5L, false, 0, 0)
                            selectedVideoForPlayback = target
                        }
                    }
                )
            }
            1 -> {
                SavedVideosSection(
                    savedVideos = savedVideos,
                    onPlayVideo = { saved ->
                        val lingoVideo = saved.toLingoVideo()
                        onUpdateWatchProgress(lingoVideo, 5L, false, 0, 0)
                        selectedVideoForPlayback = lingoVideo
                    },
                    onRemoveSaved = { saved ->
                        onToggleSaveVideo(saved.toLingoVideo())
                    },
                    onAddToPlaylist = { saved ->
                        videoToQuickAddToPlaylist = saved.toLingoVideo()
                    },
                    onExploreClick = { activeSection = 0 }
                )
            }
            2 -> {
                PlaylistsSection(
                    playlists = playlists,
                    selectedPlaylist = selectedPlaylistDetail,
                    onSelectPlaylist = { selectedPlaylistDetail = it },
                    onCreatePlaylistClick = { showCreatePlaylistModal = true },
                    onDeletePlaylist = { playlistId ->
                        onDeletePlaylist(playlistId)
                        if (selectedPlaylistDetail?.id == playlistId || selectedPlaylistDetail?.playlistId == playlistId) {
                            selectedPlaylistDetail = null
                        }
                    },
                    onRemoveVideoFromPlaylist = onRemoveVideoFromPlaylist,
                    onPlayVideoFromPlaylist = { playlist, idx ->
                        val videoId = playlist.videoIds.getOrNull(idx) ?: return@PlaylistsSection
                        val title = playlist.videoTitles.getOrNull(idx) ?: "Language Lesson"
                        val thumb = playlist.videoThumbnails.getOrNull(idx)
                            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                        val channel = sanitizeCreatorName(playlist.videoChannels.getOrNull(idx) ?: "LingoSphere Studio")
                        val match = videos.find { it.videoId == videoId }
                            ?: savedVideos.find { it.videoId == videoId }?.toLingoVideo()
                            ?: watchHistory.find { it.videoId == videoId }?.toLingoVideo()
                            ?: LingoVideo(
                                videoId = videoId,
                                title = title,
                                channelTitle = channel,
                                thumbnailUrl = thumb,
                                languageCode = playlist.languageCode,
                                proficiencyLevel = playlist.proficiencyLevel.takeIf { it != "All" } ?: "Beginner",
                                description = "Selected from playlist '${playlist.name}'."
                            )
                        onUpdateWatchProgress(match, 5L, false, 0, 0)
                        selectedVideoForPlayback = match
                    }
                )
            }
            3 -> {
                WatchHistorySection(
                    watchHistory = watchHistory,
                    savedVideoIds = savedVideos.map { it.videoId }.toSet(),
                    onPlayHistoryItem = { history ->
                        val lingoVideo = history.toLingoVideo()
                        onUpdateWatchProgress(
                            lingoVideo,
                            history.watchedSeconds + 5L,
                            history.completedQuiz,
                            history.quizScorePercent.toInt(),
                            0
                        )
                        selectedVideoForPlayback = lingoVideo
                    },
                    onToggleSaveHistory = { history ->
                        onToggleSaveVideo(history.toLingoVideo())
                    },
                    onExploreClick = { activeSection = 0 }
                )
            }
        }
    }
}

@Composable
private fun DiscoverVideosSection(
    currentLanguage: Language,
    onSelectLanguageCode: (String) -> Unit,
    levels: List<String>,
    selectedLevel: String,
    onSelectLevel: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSubmitSearch: () -> Unit,
    onClearSearch: () -> Unit,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    statusMessage: String?,
    videos: List<LingoVideo>,
    savedVideoIds: Set<String>,
    onPlayVideo: (LingoVideo) -> Unit,
    onToggleSaveVideo: (LingoVideo) -> Unit,
    onQuickAddToPlaylist: (LingoVideo) -> Unit,
    onOpenOnYouTube: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("youtube_video_search_input"),
                placeholder = { Text("Search ${currentLanguage.name} video lessons, grammar, culture...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = onClearSearch) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                        IconButton(
                            onClick = onSubmitSearch,
                            modifier = Modifier.testTag("youtube_video_search_submit")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Run search",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmitSearch() })
            )
        }

        // Language Horizontal Selector
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Target Language",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Language.SUPPORTED_LANGUAGES, key = { it.code }) { lang ->
                        val isSelected = lang.code == currentLanguage.code
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectLanguageCode(lang.code) },
                            label = { Text("${lang.flagEmoji} ${lang.name}") },
                            modifier = Modifier.testTag("video_lang_chip_${lang.code}")
                        )
                    }
                }
            }
        }

        // Proficiency Level + Topic Category Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(levels) { level ->
                        FilterChip(
                            selected = selectedLevel == level,
                            onClick = { onSelectLevel(level) },
                            label = { Text(level) },
                            leadingIcon = if (selectedLevel == level) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.testTag("video_level_chip_$level")
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        AssistChip(
                            onClick = { onSelectCategory(category) },
                            label = { Text(category) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selectedCategory == category) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        )
                    }
                }
            }
        }

        if (!statusMessage.isNullOrBlank()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading ${currentLanguage.name} video lessons...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (videos.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.VideoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "No videos matched your filter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Try selecting 'All' levels or clearing your custom search query.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Featured Hero Video Card
            val featured = videos.first()
            item {
                FeaturedVideoHeroCard(
                    video = featured,
                    isSaved = savedVideoIds.contains(featured.videoId),
                    onPlay = { onPlayVideo(featured) },
                    onToggleSave = { onToggleSaveVideo(featured) },
                    onAddToPlaylist = { onQuickAddToPlaylist(featured) }
                )
            }

            if (videos.size > 1) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recommended ${currentLanguage.name} Videos (${videos.size - 1})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AI Coach Ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                items(videos.drop(1), key = { it.videoId }) { video ->
                    LingoVideoCard(
                        video = video,
                        isSaved = savedVideoIds.contains(video.videoId),
                        onPlay = { onPlayVideo(video) },
                        onToggleSave = { onToggleSaveVideo(video) },
                        onAddToPlaylist = { onQuickAddToPlaylist(video) },
                        onOpenOnYouTube = { onOpenOnYouTube(video.videoId) }
                    )
                }
            }

            // Unlimited Video Lessons - Load More Lessons Button
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingMore) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                            Text(
                                text = "Loading more ${currentLanguage.name} lessons...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Button(
                            onClick = onLoadMore,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("load_more_videos_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Load More Lessons (${videos.size} loaded - Unlimited)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedVideoHeroCard(
    video: LingoVideo,
    isSaved: Boolean,
    onPlay: () -> Unit,
    onToggleSave: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .testTag("featured_video_hero_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = VideoCardBg),
        border = BorderStroke(1.dp, VideoBorderColor)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.78f)
                                )
                            )
                        )
                )

                // Top badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "FEATURED • ${video.proficiencyLevel.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = video.durationText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Center Play CTA
                FilledIconButton(
                    onClick = onPlay,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Featured Video",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Bottom overlay text
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val viewsSuffix = if (video.viewCountText.isNotBlank()) " • ${video.viewCountText}" else ""
                    Text(
                        text = "${sanitizeCreatorName(video.channelTitle)}$viewsSuffix",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlay,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("watch_featured_video_button")
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Watch + AI Study Pack", fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleSave) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Save Video",
                            tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onAddToPlaylist) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = "Add to Playlist"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LingoVideoCard(
    video: LingoVideo,
    isSaved: Boolean,
    onPlay: () -> Unit,
    onToggleSave: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenOnYouTube: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .testTag("lingo_video_card_${video.videoId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VideoCardBg),
        border = BorderStroke(1.dp, VideoBorderColor)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(min = 96.dp, max = 132.dp)
                        .fillMaxWidth(0.36f)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                ) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.78f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = video.durationText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = VideoTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = sanitizeCreatorName(video.channelTitle),
                        style = MaterialTheme.typography.labelMedium,
                        color = VideoAccentCyan,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = VideoAccentCyan.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, VideoAccentCyan.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = video.proficiencyLevel,
                                style = MaterialTheme.typography.labelSmall,
                                color = VideoAccentCyan,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (video.viewCountText.isNotBlank()) {
                            Text(
                                text = video.viewCountText,
                                style = MaterialTheme.typography.labelSmall,
                                color = VideoTextSecondary
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = VideoBorderColor.copy(alpha = 0.6f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onPlay) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Watch + AI Tutor", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleSave) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Save Video",
                            tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onAddToPlaylist) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = "Add to Playlist"
                        )
                    }
                    IconButton(onClick = onPlay) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Play Video Lesson",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedVideosSection(
    savedVideos: List<SavedVideo>,
    onPlayVideo: (SavedVideo) -> Unit,
    onRemoveSaved: (SavedVideo) -> Unit,
    onAddToPlaylist: (SavedVideo) -> Unit,
    onExploreClick: () -> Unit
) {
    val context = LocalContext.current
    if (savedVideos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "No Saved Videos Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Bookmark educational videos while exploring to build your personal study queue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onExploreClick) {
                    Text("Explore Videos")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(savedVideos, key = { it.id.ifBlank { it.videoId } }) { saved ->
                LingoVideoCard(
                    video = saved.toLingoVideo(),
                    isSaved = true,
                    onPlay = { onPlayVideo(saved) },
                    onToggleSave = { onRemoveSaved(saved) },
                    onAddToPlaylist = { onAddToPlaylist(saved) },
                    onOpenOnYouTube = {
                        onPlayVideo(saved)
                    }
                )
            }
        }
    }
}

@Composable
private fun PlaylistsSection(
    playlists: List<VideoPlaylist>,
    selectedPlaylist: VideoPlaylist?,
    onSelectPlaylist: (VideoPlaylist?) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRemoveVideoFromPlaylist: (VideoPlaylist, String) -> Unit,
    onPlayVideoFromPlaylist: (VideoPlaylist, Int) -> Unit
) {
    if (selectedPlaylist != null) {
        val current = playlists.find { it.id == selectedPlaylist.id || it.playlistId == selectedPlaylist.playlistId }
            ?: selectedPlaylist
        val langName = Language.findByCode(current.languageCode).name
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onSelectPlaylist(null) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Playlists")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("All Playlists")
                }
                IconButton(onClick = { onDeletePlaylist(current.playlistId.ifBlank { current.id }) }) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete Playlist",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Text(
                text = current.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            if (current.description.isNotBlank()) {
                Text(
                    text = current.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (current.videoIds.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "This playlist is empty. Tap the Playlist icon on any video card to add lessons here.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(current.videoIds.size) { index ->
                        val videoId = current.videoIds[index]
                        val title = current.videoTitles.getOrNull(index) ?: "Lesson Video ($videoId)"
                        val channel = current.videoChannels.getOrNull(index) ?: langName
                        val thumb = current.videoThumbnails.getOrNull(index)
                            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlayVideoFromPlaylist(current, index) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AsyncImage(
                                    model = thumb,
                                    contentDescription = title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(110.dp)
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = channel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onRemoveVideoFromPlaylist(current, videoId) }
                                ) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove from playlist")
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Study Playlists",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Button(
                        onClick = onCreatePlaylistClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("create_playlist_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Playlist")
                    }
                }
            }

            if (playlists.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.PlaylistPlay,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Organize Lessons by Topic or Exam",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Create custom playlists for JLPT N5, DELE B1, Travel Phrases, or Daily Listening.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(playlists, key = { it.id.ifBlank { it.playlistId } }) { playlist ->
                    val lang = Language.findByCode(playlist.languageCode)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPlaylist(playlist) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.PlaylistPlay,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${lang.flagEmoji} ${lang.name} • ${playlist.videoIds.size} videos",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchHistorySection(
    watchHistory: List<VideoWatchHistory>,
    savedVideoIds: Set<String>,
    onPlayHistoryItem: (VideoWatchHistory) -> Unit,
    onToggleSaveHistory: (VideoWatchHistory) -> Unit,
    onExploreClick: () -> Unit
) {
    if (watchHistory.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "No Watch History Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Videos you watch are automatically logged here so you can resume anytime.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onExploreClick) {
                    Text("Start Watching")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(watchHistory, key = { it.id.ifBlank { it.videoId } }) { history ->
                val lang = Language.findByCode(history.languageCode)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayHistoryItem(history) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = VideoCardBg),
                    border = BorderStroke(1.dp, VideoBorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = history.thumbnailUrl,
                            contentDescription = history.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .widthIn(min = 88.dp, max = 120.dp)
                                .fillMaxWidth(0.34f)
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = history.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${sanitizeCreatorName(history.channelTitle)} • ${lang.flagEmoji} ${lang.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val mins = history.watchedSeconds / 60
                            val secs = history.watchedSeconds % 60
                            Text(
                                text = if (history.completedQuiz) {
                                    "Quiz Completed (${history.quizScorePercent}%) • +${history.xpEarned} XP"
                                } else {
                                    "Watched ${mins}m ${secs}s"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(onClick = { onToggleSaveHistory(history) }) {
                            val isSaved = savedVideoIds.contains(history.videoId)
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Save Video"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatePlaylistOnlyDialog(
    languageName: String,
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create $languageName Playlist", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Playlist Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onCreate(title.trim(), description.trim()) },
                enabled = title.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
