package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import com.example.data.model.AudiobookBookmark
import com.example.data.model.ListeningAcademyItem
import com.example.data.model.UserProfile
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ai.AudiobookLearningService
import com.example.data.model.AudiobookBook
import com.example.data.model.BookCategoryCluster
import com.example.data.model.BookContentType
import com.example.data.model.Language
import com.example.data.repository.AudiobookCatalogRepository
import com.example.data.repository.GutendexAndLibriVoxService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DeepNavyBg = Color(0xFF060D1E)
private val DeepNavyCard = Color(0xFF0F1B38)
private val DeepNavyElevated = Color(0xFF14244A)
private val DeepNavyBorder = Color(0xFF1E305C)
private val CyanAccent = Color(0xFF06B6D4)
private val EmeraldAccent = Color(0xFF10B981)
private val AmberAccent = Color(0xFFF59E0B)
private val PurpleAccent = Color(0xFF8B5CF6)
private val TextWhite = Color(0xFFFFFFFF)
private val TextLight = Color(0xFFCBD5E1)
private val TextMuted = Color(0xFF94A3B8)

/**
 * In-memory + SharedPreferences first-page cache so reopening the Book & Audio Library is instant.
 */
private object FirstPageBookCatalogCache {
  private const val PREFS_NAME = "lingosphere_book_catalog_cache_v1"
  private const val KEY_TOTAL_COUNT = "cached_total_count"

  @Volatile
  var cachedFirstPageBooks: List<AudiobookBook> = emptyList()

  @Volatile
  var cachedTotalCount: Int = 0

  fun saveFirstPage(context: android.content.Context, books: List<AudiobookBook>, totalCount: Int) {
    if (books.isEmpty()) return
    cachedFirstPageBooks = books
    cachedTotalCount = maxOf(totalCount, books.size)
    runCatching {
      context.applicationContext
        .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .edit()
        .putInt(KEY_TOTAL_COUNT, cachedTotalCount)
        .apply()
    }
  }

  fun loadTotalCount(context: android.content.Context): Int {
    if (cachedTotalCount > 0) return cachedTotalCount
    return runCatching {
      context.applicationContext
        .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getInt(KEY_TOTAL_COUNT, 0)
    }.getOrDefault(0)
  }
}

/**
 * Dedicated Category Cluster Discovery Page:
 * Supports Motivation and Success, Wealth and Business, Psychology, Fiction and Stories,
 * Education, and Lifestyle with subcategory chips, live Gutendex API book loading,
 * pagination ("Load More"), and optional AI lesson generation.
 */
@Composable
fun CategoryBooksExplorerScreen(
  cluster: BookCategoryCluster,
  userId: String,
  targetLang: Language,
  isProUnlocked: Boolean = false,
  userProfile: UserProfile? = null,
  onOpenSubscription: () -> Unit = {},
  onSelectBook: (AudiobookBook) -> Unit,
  onOpenCreateListening: () -> Unit = {},
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var selectedSubCategory by remember(cluster.id) { mutableStateOf("All") }
  var selectedLetter by remember(cluster.id) { mutableStateOf("All") }
  var searchQuery by remember { mutableStateOf("") }

  var remoteBooks by remember(cluster.id) { mutableStateOf<List<AudiobookBook>>(emptyList()) }
  var nextPageUrl by remember(cluster.id) { mutableStateOf<String?>(null) }
  var isLoadingRemote by remember(cluster.id) { mutableStateOf(false) }
  var isLoadingMore by remember(cluster.id) { mutableStateOf(false) }
  var loadError by remember(cluster.id) { mutableStateOf<String?>(null) }

  val localBooks = remember(cluster.id, userId) {
    AudiobookLearningService.getAllAvailableBooks(context, userId).filter { book ->
      AudiobookCatalogRepository.matchesCategoryTab(book, cluster.title) ||
        cluster.subCategories.any { sub ->
          book.subCategory.equals(sub, ignoreCase = true) ||
            book.subjects.any { it.contains(sub, ignoreCase = true) }
        }
    }
  }

  fun refreshClusterBooks(subCat: String) {
    scope.launch {
      isLoadingRemote = true
      loadError = null
      val topicQuery = if (subCat == "All") {
        cluster.gutendexTopicQuery.ifBlank { cluster.shortChipLabel }
      } else {
        subCat
      }
      val pageRes = GutendexAndLibriVoxService.fetchBooksPage(
        topic = topicQuery,
        languageCode = null,
        sort = "popular"
      )
      remoteBooks = pageRes.books
      nextPageUrl = pageRes.nextPageUrl
      loadError = pageRes.errorMessage
      isLoadingRemote = false
    }
  }

  // Query live Gutendex catalog for this category / subcategory
  LaunchedEffect(cluster.id, selectedSubCategory, targetLang.code) {
    isLoadingRemote = true
    loadError = null
    val topicQuery = if (selectedSubCategory == "All") {
      cluster.gutendexTopicQuery.ifBlank { cluster.shortChipLabel }
    } else {
      selectedSubCategory
    }
    val pageRes = GutendexAndLibriVoxService.fetchBooksPage(
      topic = topicQuery,
      languageCode = null,
      sort = "popular"
    )
    remoteBooks = pageRes.books
    nextPageUrl = pageRes.nextPageUrl
    loadError = pageRes.errorMessage
    isLoadingRemote = false
  }

  val combinedBooks = remember(localBooks, remoteBooks, selectedSubCategory, searchQuery) {
    val merged = mergeDeduplicatedCatalogBooks(localBooks, remoteBooks)
    val filtered = merged.filter { b ->
      val matchesSub = selectedSubCategory == "All" ||
        b.subCategory.equals(selectedSubCategory, ignoreCase = true) ||
        b.subjects.any { it.contains(selectedSubCategory, ignoreCase = true) } ||
        remoteBooks.any { r -> r.id == b.id }

      val matchesQuery = searchQuery.isBlank() ||
        b.title.contains(searchQuery, ignoreCase = true) ||
        b.author.contains(searchQuery, ignoreCase = true) ||
        b.subCategory.contains(searchQuery, ignoreCase = true)

      matchesSub && matchesQuery
    }
    filtered
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DeepNavyBg)
      .padding(horizontal = 20.dp, vertical = 14.dp)
      .testTag("category_books_explorer_screen")
  ) {
    // Top Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(DeepNavyCard)
      ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "${cluster.iconEmoji} ${cluster.title}",
          color = TextWhite,
          fontSize = 18.sp,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "${cluster.subCategories.size} topics • Complete books & audio lessons",
          color = CyanAccent,
          fontSize = 12.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Search inside category
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("Search in ${cluster.title}...", color = TextMuted, fontSize = 13.sp) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanAccent) },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { searchQuery = "" }) {
            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
          }
        }
      },
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextWhite,
        unfocusedTextColor = TextWhite,
        focusedBorderColor = CyanAccent,
        unfocusedBorderColor = DeepNavyBorder,
        focusedContainerColor = DeepNavyCard,
        unfocusedContainerColor = DeepNavyCard
      ),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Subcategory Filter Chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      val allSubs = listOf("All") + cluster.subCategories
      allSubs.forEach { sub ->
        val isSelected = selectedSubCategory == sub
        FilterChip(
          selected = isSelected,
          onClick = { selectedSubCategory = sub },
          label = {
            Text(
              text = sub,
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(cluster.accentColorHex).copy(alpha = 0.24f),
            selectedLabelColor = Color(cluster.accentColorHex),
            containerColor = DeepNavyCard,
            labelColor = TextLight
          )
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (isLoadingRemote && combinedBooks.isEmpty()) {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator(color = CyanAccent)
          Spacer(modifier = Modifier.height(8.dp))
          Text("Loading books in ${cluster.title}...", color = TextLight, fontSize = 13.sp)
        }
      }
    } else if (loadError != null && combinedBooks.isEmpty()) {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
          border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(loadError!!, color = TextLight, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = { refreshClusterBooks(selectedSubCategory) },
              colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF041220)),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Retry", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    } else {
      LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        itemsIndexed(combinedBooks, key = { _, book -> book.id }) { index, book ->
          AudiobookCompactListItem(
            book = book,
            targetLang = targetLang,
            onClick = { onSelectBook(book) },
            isProUnlocked = isProUnlocked
          )
          if (!isProUnlocked && (index + 1) % 8 == 0 && com.example.ads.AdService.shouldShowAds(userProfile)) {
            Spacer(modifier = Modifier.height(10.dp))
            com.example.ads.LingoBookListNativeAdCard(
              userProfile = userProfile,
              slotIndex = (index + 1) / 8,
              onGoProClick = onOpenSubscription
            )
          }
        }

        // Load More Pagination Button
        if (!nextPageUrl.isNullOrBlank()) {
          item {
            OutlinedButton(
              onClick = {
                val url = nextPageUrl ?: return@OutlinedButton
                scope.launch {
                  isLoadingMore = true
                  val moreRes = GutendexAndLibriVoxService.fetchBooksPage(pageUrl = url)
                  remoteBooks = (remoteBooks + moreRes.books).distinctBy { it.id }
                  nextPageUrl = moreRes.nextPageUrl
                  isLoadingMore = false
                }
              },
              enabled = !isLoadingMore,
              border = BorderStroke(1.dp, CyanAccent),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
            ) {
              if (isLoadingMore) {
                CircularProgressIndicator(color = CyanAccent, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Loading more books...")
              } else {
                Text("Load More Books", fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        // Separate option to generate an original AI listening lesson if desired
        item {
          Card(
            onClick = onOpenCreateListening,
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DeepNavyElevated),
            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(22.dp))
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Want a custom lesson on '$selectedSubCategory'?",
                  color = TextWhite,
                  fontSize = 13.5.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Generate an original AI audio lesson tailored to your language level.",
                  color = TextMuted,
                  fontSize = 11.5.sp
                )
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Real-time Search & Pagination Discovery Screen (Section 12):
 * Searches Gutendex API by Book Title, Author, Subject, Genre, and Language,
 * and supports pagination via the API's "next" field.
 */
@Composable
fun AudiobookSearchDiscoveryScreen(
  userId: String,
  targetLang: Language,
  isProUnlocked: Boolean = false,
  userProfile: UserProfile? = null,
  onOpenSubscription: () -> Unit = {},
  onSelectBook: (AudiobookBook) -> Unit,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var query by remember { mutableStateOf("") }
  var selectedLetter by remember { mutableStateOf("All") }
  var selectedCategoryFilter by remember { mutableStateOf("All") }
  var selectedLanguageCode by remember { mutableStateOf("ALL") }
  var selectedNarratorFilter by remember { mutableStateOf("All Narrators") }
  var filterByTargetLangOnly by remember { mutableStateOf(false) }
  var filterOnlyWithAudio by remember { mutableStateOf(false) }
  var filterOnlyWithText by remember { mutableStateOf(false) }
  var filterOnlyAiNarration by remember { mutableStateOf(false) }

  var remoteResults by remember { mutableStateOf<List<AudiobookBook>>(emptyList()) }
  var totalCount by remember { mutableStateOf(0) }
  var gutendexNextPageUrl by remember { mutableStateOf<String?>(null) }
  var librivoxNextOffset by remember { mutableStateOf<Int?>(null) }
  var hasMoreRemote by remember { mutableStateOf(false) }
  var isSearching by remember { mutableStateOf(false) }
  var isLoadingNextPage by remember { mutableStateOf(false) }
  var searchError by remember { mutableStateOf<String?>(null) }
  var searchJob by remember { mutableStateOf<Job?>(null) }

  val localBooks = remember(userId) {
    AudiobookLearningService.getAllAvailableBooks(context, userId)
  }

  val effectiveLanguageParam = when {
    filterByTargetLangOnly -> targetLang.code
    selectedLanguageCode != "ALL" -> selectedLanguageCode
    else -> null
  }

  fun executeRemoteSearch(
    searchText: String = query,
    catFilter: String = selectedCategoryFilter,
    langParam: String? = effectiveLanguageParam,
    narratorFilter: String = selectedNarratorFilter,
    withAudio: Boolean = filterOnlyWithAudio,
    withText: Boolean = filterOnlyWithText,
    withAi: Boolean = filterOnlyAiNarration,
    immediate: Boolean = false
  ) {
    searchJob?.cancel()
    searchJob = scope.launch {
      if (!immediate && searchText.isNotBlank()) delay(400L)
      isSearching = true
      searchError = null

      val pageRes = GutendexAndLibriVoxService.searchCatalogPaginated(
        query = searchText.trim(),
        languageCode = langParam,
        categoryTab = catFilter,
        onlyWithAudio = withAudio,
        onlyWithText = withText,
        selectedLetter = null,
        narratorFilter = narratorFilter.takeIf { !it.equals("All Narrators", ignoreCase = true) },
        onlyAiNarration = withAi,
        sortAlphabetically = false
      )
      remoteResults = pageRes.books
      totalCount = pageRes.totalCount
      gutendexNextPageUrl = pageRes.gutendexNextPageUrl
      librivoxNextOffset = pageRes.librivoxNextOffset
      hasMoreRemote = pageRes.hasMore
      searchError = pageRes.errorMessage
      isSearching = false
    }
  }

  LaunchedEffect(
    selectedCategoryFilter,
    selectedLanguageCode,
    selectedNarratorFilter,
    filterByTargetLangOnly,
    filterOnlyWithAudio,
    filterOnlyWithText,
    filterOnlyAiNarration
  ) {
    executeRemoteSearch(immediate = true)
  }

  val displayedBooks = remember(
    localBooks,
    remoteResults,
    query,
    selectedCategoryFilter,
    effectiveLanguageParam,
    selectedNarratorFilter,
    filterOnlyWithAudio,
    filterOnlyWithText,
    filterOnlyAiNarration
  ) {
    val matchingLocal = localBooks.filter { book ->
      val matchesText = GutendexAndLibriVoxService.matchesSearchQuery(book, query)
      val matchesCat = AudiobookCatalogRepository.matchesCategoryTab(book, selectedCategoryFilter)
      val matchesNarrator = GutendexAndLibriVoxService.matchesNarratorFilter(book, selectedNarratorFilter)

      val matchesLang = effectiveLanguageParam == null ||
        book.languages.any { it.equals(effectiveLanguageParam, ignoreCase = true) }

      val matchesAudio = !filterOnlyWithAudio ||
        (book.hasHumanNarration || book.librivoxEditions.isNotEmpty())

      val matchesFullText = !filterOnlyWithText ||
        (book.hasFullTextAvailable || !book.fullTextUrl.isNullOrBlank() || book.gutendexId != null)

      val matchesAi = !filterOnlyAiNarration ||
        (book.hasFullTextAvailable || book.gutendexId != null || book.parsedChapters.isNotEmpty() || book.episodes.isNotEmpty())

      matchesText && matchesCat && matchesNarrator && matchesLang && matchesAudio && matchesFullText && matchesAi
    }

    val matchingRemote = remoteResults.filter { book ->
      val matchesText = GutendexAndLibriVoxService.matchesSearchQuery(book, query)
      val matchesCat = AudiobookCatalogRepository.matchesCategoryTab(book, selectedCategoryFilter)
      val matchesNarrator = GutendexAndLibriVoxService.matchesNarratorFilter(book, selectedNarratorFilter)
      val matchesLang = effectiveLanguageParam == null ||
        book.languages.any { it.equals(effectiveLanguageParam, ignoreCase = true) }
      val matchesAudio = !filterOnlyWithAudio ||
        (book.hasHumanNarration || book.librivoxEditions.isNotEmpty())
      val matchesFullText = !filterOnlyWithText ||
        (book.hasFullTextAvailable || !book.fullTextUrl.isNullOrBlank() || book.gutendexId != null)
      val matchesAi = !filterOnlyAiNarration ||
        (book.hasFullTextAvailable || book.gutendexId != null || book.parsedChapters.isNotEmpty() || book.episodes.isNotEmpty())
      matchesText && matchesCat && matchesNarrator && matchesLang && matchesAudio && matchesFullText && matchesAi
    }

    mergeDeduplicatedCatalogBooks(matchingLocal, matchingRemote)
  }

  val availableNarrators = remember(localBooks, remoteResults) {
    val dynamicNarrators = (localBooks + remoteResults)
      .flatMap { it.librivoxEditions }
      .map { it.narratorName.trim() }
      .filter { it.isNotBlank() && !it.equals("Human Narrator", ignoreCase = true) && !it.equals("Human Reader", ignoreCase = true) }
    (AudiobookCatalogRepository.POPULAR_NARRATORS + dynamicNarrators).distinct()
  }

  val hasActiveSearchFilters = query.isNotBlank() ||
    selectedCategoryFilter != "All" ||
    selectedLanguageCode != "ALL" ||
    selectedNarratorFilter != "All Narrators" ||
    filterByTargetLangOnly ||
    filterOnlyWithAudio ||
    filterOnlyWithText ||
    filterOnlyAiNarration

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DeepNavyBg)
      .padding(horizontal = 20.dp, vertical = 14.dp)
      .testTag("audiobook_search_screen")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(DeepNavyCard)
      ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
      }
      Spacer(modifier = Modifier.width(12.dp))
      Text(
        text = "Search Complete Books & Audio",
        color = TextWhite,
        fontSize = 18.sp,
        fontWeight = FontWeight.ExtraBold
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedTextField(
      value = query,
      onValueChange = { newText ->
        query = newText
        executeRemoteSearch(searchText = newText)
      },
      placeholder = { Text("Search book title, author, subject, or genre...", color = TextMuted, fontSize = 13.sp) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanAccent) },
      trailingIcon = {
        if (query.isNotEmpty()) {
          IconButton(
            onClick = {
              query = ""
              executeRemoteSearch(searchText = "", immediate = true)
            },
            modifier = Modifier.testTag("audiobook_search_clear_btn")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
          }
        }
      },
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextWhite,
        unfocusedTextColor = TextWhite,
        focusedBorderColor = CyanAccent,
        unfocusedBorderColor = DeepNavyBorder,
        focusedContainerColor = DeepNavyCard,
        unfocusedContainerColor = DeepNavyCard
      ),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("audiobook_search_input")
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Category, Availability & Language Filter Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (hasActiveSearchFilters) {
        FilterChip(
          selected = false,
          onClick = {
            query = ""
            selectedCategoryFilter = "All"
            selectedLanguageCode = "ALL"
            selectedNarratorFilter = "All Narrators"
            filterByTargetLangOnly = false
            filterOnlyWithAudio = false
            filterOnlyWithText = false
            filterOnlyAiNarration = false
            executeRemoteSearch(searchText = "", immediate = true)
          },
          label = { Text("Clear filters", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
          colors = FilterChipDefaults.filterChipColors(
            containerColor = AmberAccent.copy(alpha = 0.18f),
            labelColor = AmberAccent
          ),
          modifier = Modifier.testTag("search_clear_filters_chip")
        )
      }

      FilterChip(
        selected = filterByTargetLangOnly,
        onClick = { filterByTargetLangOnly = !filterByTargetLangOnly },
        label = { Text("${targetLang.flagEmoji} ${targetLang.name} Only", fontSize = 11.5.sp) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = EmeraldAccent.copy(alpha = 0.22f),
          selectedLabelColor = EmeraldAccent,
          containerColor = DeepNavyCard,
          labelColor = TextLight
        )
      )

      FilterChip(
        selected = filterOnlyWithAudio,
        onClick = { filterOnlyWithAudio = !filterOnlyWithAudio },
        label = { Text("🎧 Human Audio", fontSize = 11.5.sp) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = AmberAccent.copy(alpha = 0.22f),
          selectedLabelColor = AmberAccent,
          containerColor = DeepNavyCard,
          labelColor = TextLight
        )
      )

      FilterChip(
        selected = filterOnlyAiNarration,
        onClick = { filterOnlyAiNarration = !filterOnlyAiNarration },
        label = { Text("🤖 AI Narration", fontSize = 11.5.sp) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = CyanAccent.copy(alpha = 0.22f),
          selectedLabelColor = CyanAccent,
          containerColor = DeepNavyCard,
          labelColor = TextLight
        )
      )

      FilterChip(
        selected = filterOnlyWithText,
        onClick = { filterOnlyWithText = !filterOnlyWithText },
        label = { Text("📖 Has Full Text", fontSize = 11.5.sp) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = PurpleAccent.copy(alpha = 0.22f),
          selectedLabelColor = PurpleAccent,
          containerColor = DeepNavyCard,
          labelColor = TextLight
        )
      )

      AudiobookCatalogRepository.MAIN_CATEGORY_TABS.forEach { tab ->
        val isSel = selectedCategoryFilter == tab
        FilterChip(
          selected = isSel,
          onClick = { selectedCategoryFilter = tab },
          label = { Text(tab, fontSize = 11.5.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CyanAccent.copy(alpha = 0.22f),
            selectedLabelColor = CyanAccent,
            containerColor = DeepNavyCard,
            labelColor = TextLight
          )
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Narrator & Language Filter Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      AudiobookCatalogRepository.CATALOG_LANGUAGE_OPTIONS.forEach { (code, label) ->
        val isSel = !filterByTargetLangOnly && selectedLanguageCode == code
        FilterChip(
          selected = isSel,
          onClick = {
            filterByTargetLangOnly = false
            selectedLanguageCode = code
          },
          label = { Text(label, fontSize = 11.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = EmeraldAccent.copy(alpha = 0.22f),
            selectedLabelColor = EmeraldAccent,
            containerColor = DeepNavyCard,
            labelColor = TextLight
          ),
          modifier = Modifier.testTag("search_lang_chip_${code.lowercase()}")
        )
      }

      availableNarrators.forEach { narrator ->
        val isSel = selectedNarratorFilter == narrator
        val tagKey = narrator.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        FilterChip(
          selected = isSel,
          onClick = { selectedNarratorFilter = narrator },
          label = { Text(if (narrator == "All Narrators") "🎙️ All Narrators" else "🎙️ $narrator", fontSize = 11.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AmberAccent.copy(alpha = 0.22f),
            selectedLabelColor = AmberAccent,
            containerColor = DeepNavyCard,
            labelColor = TextLight
          ),
          modifier = Modifier.testTag("search_narrator_chip_$tagKey")
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      val effectiveTotal = maxOf(totalCount, displayedBooks.size)
      val plusSign = if (hasMoreRemote) "+" else ""
      val countText = when {
        isSearching && displayedBooks.isEmpty() -> "Searching catalog..."
        displayedBooks.isEmpty() -> "No books found"
        else -> "${"%,d".format(effectiveTotal)}$plusSign books"
      }
      Text(countText, color = TextMuted, fontSize = 12.sp)
      if (isSearching) {
        CircularProgressIndicator(color = CyanAccent, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (isSearching && displayedBooks.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator(color = CyanAccent)
          Spacer(modifier = Modifier.height(10.dp))
          Text("Searching books & audio catalog...", color = TextLight, fontSize = 13.sp)
        }
      }
    } else if (searchError != null && displayedBooks.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
          border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(searchError!!, color = TextLight, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = {
                executeRemoteSearch(immediate = true)
              },
              colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF041220)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("search_retry_btn")
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Retry Search", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    } else if (!isSearching && displayedBooks.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
          border = BorderStroke(1.dp, DeepNavyBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = if (query.isNotBlank()) "No books matched \"$query\"" else "No books matched the selected filters",
              color = TextWhite,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
              onClick = {
                query = ""
                selectedLetter = "All"
                selectedCategoryFilter = "All"
                selectedLanguageCode = "ALL"
                selectedNarratorFilter = "All Narrators"
                filterByTargetLangOnly = false
                filterOnlyWithAudio = false
                filterOnlyWithText = false
                filterOnlyAiNarration = false
              },
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Reset Filters", color = CyanAccent, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    } else {
      val listState = rememberLazyListState()
      var searchAutoPrefetchCount by remember(query, selectedLetter, selectedCategoryFilter, selectedLanguageCode) {
        mutableIntStateOf(0)
      }
      var searchEmptyPageCount by remember(query, selectedLetter, selectedCategoryFilter, selectedLanguageCode) {
        mutableIntStateOf(0)
      }
      val shouldAutoLoadMore by remember {
        derivedStateOf {
          val layoutInfo = listState.layoutInfo
          val total = layoutInfo.totalItemsCount
          val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
          val isNearBottom = total > 0 && lastVisible >= (total - 5).coerceAtLeast(0)
          val needsMoreToFill = displayedBooks.size < 30 && searchAutoPrefetchCount < 3
          hasMoreRemote &&
            !isLoadingNextPage &&
            !isSearching &&
            searchEmptyPageCount < 3 &&
            (isNearBottom || needsMoreToFill)
        }
      }

      LaunchedEffect(shouldAutoLoadMore, gutendexNextPageUrl, librivoxNextOffset) {
        if (shouldAutoLoadMore) {
          isLoadingNextPage = true
          searchAutoPrefetchCount++
          val prevCount = remoteResults.size
          val nextRes = GutendexAndLibriVoxService.searchCatalogPaginated(
            query = query.trim(),
            languageCode = effectiveLanguageParam,
            categoryTab = selectedCategoryFilter,
            onlyWithAudio = filterOnlyWithAudio,
            onlyWithText = filterOnlyWithText,
            gutendexPageUrl = gutendexNextPageUrl,
            librivoxOffset = librivoxNextOffset ?: 0,
            selectedLetter = selectedLetter.takeIf { !it.equals("All", ignoreCase = true) },
            narratorFilter = selectedNarratorFilter.takeIf { !it.equals("All Narrators", ignoreCase = true) },
            onlyAiNarration = filterOnlyAiNarration,
            sortAlphabetically = true
          )
          if (nextRes.books.isNotEmpty()) {
            remoteResults = mergeDeduplicatedCatalogBooks(remoteResults, nextRes.books)
          }
          if (remoteResults.size > prevCount) {
            searchEmptyPageCount = 0
          } else {
            searchEmptyPageCount++
          }
          gutendexNextPageUrl = nextRes.gutendexNextPageUrl
          librivoxNextOffset = nextRes.librivoxNextOffset
          hasMoreRemote = nextRes.hasMore
          isLoadingNextPage = false
        }
      }

      LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        if (searchError != null) {
          item(key = "search_offline_banner") {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = DeepNavyCard,
              border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.45f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Showing saved library • Online search unavailable",
                  color = AmberAccent,
                  fontSize = 11.5.sp,
                  modifier = Modifier.weight(1f)
                )
                Surface(
                  onClick = {
                    executeRemoteSearch(immediate = true)
                  },
                  shape = RoundedCornerShape(8.dp),
                  color = CyanAccent
                ) {
                  Text("Retry", color = Color(0xFF041220), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
              }
            }
          }
        }

        itemsIndexed(displayedBooks, key = { _, book -> book.id }) { index, book ->
          AudiobookCompactListItem(
            book = book,
            targetLang = targetLang,
            onClick = { onSelectBook(book) },
            isProUnlocked = isProUnlocked
          )
          if (!isProUnlocked && (index + 1) % 8 == 0 && com.example.ads.AdService.shouldShowAds(userProfile)) {
            Spacer(modifier = Modifier.height(10.dp))
            com.example.ads.LingoBookListNativeAdCard(
              userProfile = userProfile,
              slotIndex = (index + 1) / 8,
              onGoProClick = onOpenSubscription
            )
          }
        }

        if (hasMoreRemote) {
          item(key = "search_load_more") {
            Button(
              onClick = {
                scope.launch {
                  isLoadingNextPage = true
                  val nextRes = GutendexAndLibriVoxService.searchCatalogPaginated(
                    query = query.trim(),
                    languageCode = effectiveLanguageParam,
                    categoryTab = selectedCategoryFilter,
                    onlyWithAudio = filterOnlyWithAudio,
                    onlyWithText = filterOnlyWithText,
                    gutendexPageUrl = gutendexNextPageUrl,
                    librivoxOffset = librivoxNextOffset ?: 0,
                    selectedLetter = selectedLetter.takeIf { !it.equals("All", ignoreCase = true) },
                    narratorFilter = selectedNarratorFilter.takeIf { !it.equals("All Narrators", ignoreCase = true) },
                    onlyAiNarration = filterOnlyAiNarration,
                    sortAlphabetically = true
                  )
                  if (nextRes.books.isNotEmpty()) {
                    remoteResults = mergeDeduplicatedCatalogBooks(remoteResults, nextRes.books)
                  }
                  gutendexNextPageUrl = nextRes.gutendexNextPageUrl
                  librivoxNextOffset = nextRes.librivoxNextOffset
                  hasMoreRemote = nextRes.hasMore
                  isLoadingNextPage = false
                }
              },
              enabled = !isLoadingNextPage,
              colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF041220)),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("search_load_more_btn")
            ) {
              if (isLoadingNextPage) {
                CircularProgressIndicator(color = Color(0xFF041220), strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Loading Next Page...", fontWeight = FontWeight.Bold)
              } else {
                Text("Load More Books", fontWeight = FontWeight.ExtraBold)
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Compact book list card displaying cover image, language badge, human audio / AI narration / text-only
 * availability badges, narrator info, title, author, and chapter count.
 */
@Composable
fun AudiobookCompactListItem(
  book: AudiobookBook,
  targetLang: Language,
  onClick: () -> Unit,
  isProUnlocked: Boolean = false
) {
  val grad = book.coverGradientColors.map { Color(it) }.ifEmpty { listOf(DeepNavyCard, DeepNavyElevated) }
  val hasHumanAudio = book.hasVerifiedHumanAudio || (book.contentType == BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK && book.hasHumanNarration) || book.librivoxEditions.isNotEmpty()
  val hasFullBook = book.hasVerifiedFullText || book.hasFullTextAvailable || book.gutendexId != null || !book.fullTextUrl.isNullOrBlank() || book.parsedChapters.isNotEmpty()
  val hasAiNarration = hasFullBook || book.episodes.isNotEmpty() || book.contentType == BookContentType.AI_LEARNING_SUMMARY
  val chCount = book.totalChapterCount
  val primaryLangCode = (book.languages.firstOrNull() ?: book.targetLanguageCode).uppercase().ifBlank { "EN" }
  val primaryNarrator = book.librivoxEditions.firstOrNull()?.narratorName
    ?.takeIf { it.isNotBlank() && !it.equals("Human Narrator", ignoreCase = true) && !it.equals("Human Reader", ignoreCase = true) }
  val openedBooksThisMonth by com.example.data.repository.AccessPolicyManager.openedBookIdsThisMonth.collectAsState()
  val policyLimits by com.example.data.repository.AccessPolicyManager.policyState.collectAsState()
  val cleanBookKey = remember(book.id, book.gutendexId) {
    book.id.trim().ifBlank { book.gutendexId?.let { "gutenberg_$it" } ?: "" }
  }
  val isWithinMonthlyAllowance = remember(isProUnlocked, cleanBookKey, openedBooksThisMonth, policyLimits.freeBooksPerMonth) {
    isProUnlocked ||
      (cleanBookKey.isNotBlank() && openedBooksThisMonth.any { it.equals(cleanBookKey, ignoreCase = true) }) ||
      openedBooksThisMonth.size < policyLimits.freeBooksPerMonth.coerceAtLeast(1)
  }
  val requiresProLock = !isProUnlocked && !isWithinMonthlyAllowance

  Card(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
    border = BorderStroke(1.dp, DeepNavyBorder),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("book_item_${book.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = grad.first(),
        modifier = Modifier.size(width = 56.dp, height = 76.dp)
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(grad)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.MenuBook, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(24.dp))
          if (book.coverArtUrl.isNotBlank()) {
            AsyncImage(
              model = book.coverArtUrl,
              contentDescription = book.title,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(5.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
          if (requiresProLock) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = AmberAccent.copy(alpha = 0.20f),
              border = BorderStroke(0.8.dp, AmberAccent.copy(alpha = 0.6f)),
              modifier = Modifier
                .height(20.dp)
                .testTag("pro_lock_pill_${book.id}")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(horizontal = 6.dp)
              ) {
                LingoGoldLockIcon(size = 11.dp)
                Text(
                  text = "Pro",
                  color = AmberAccent,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.ExtraBold
                )
              }
            }
          } else {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = EmeraldAccent.copy(alpha = 0.20f),
              modifier = Modifier
                .height(20.dp)
                .testTag("free_pill_${book.id}")
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                Text(
                  text = "FREE",
                  color = EmeraldAccent,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.ExtraBold
                )
              }
            }
          }
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = PurpleAccent.copy(alpha = 0.20f)
          ) {
            Text(
              text = primaryLangCode,
              color = PurpleAccent,
              fontSize = 9.sp,
              fontWeight = FontWeight.ExtraBold,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
          }
          if (hasHumanAudio) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = AmberAccent.copy(alpha = 0.18f),
              modifier = Modifier.testTag("badge_human_audio_${book.id}")
            ) {
              Text(
                text = "Human Audio",
                color = AmberAccent,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          if (hasAiNarration) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = CyanAccent.copy(alpha = 0.16f),
              modifier = Modifier.testTag("badge_ai_narration_${book.id}")
            ) {
              Text(
                text = "AI Narration",
                color = CyanAccent,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          if (chCount > 0) {
            Text(
              text = "$chCount ch",
              color = CyanAccent,
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = book.title,
          color = TextWhite,
          fontSize = 14.5.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )

        val metaSubtitle = buildString {
          append(book.author)
          append(" • ")
          append(book.primaryCategory)
          if (!primaryNarrator.isNullOrBlank()) {
            append(" • 🎙️ ")
            append(primaryNarrator)
          }
        }
        Text(
          text = metaSubtitle,
          color = TextLight,
          fontSize = 11.5.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      IconButton(
        onClick = onClick,
        modifier = Modifier
          .size(48.dp)
          .testTag("play_book_btn_${book.id}")
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = "Open Book", tint = CyanAccent)
      }
    }
  }
}

/**
 * Elegant Card for horizontal book carousels on the main Listening Hub.
 * Displays real Gutenberg cover image (via AsyncImage) with fallback gradient cover,
 * content badge, small 20dp PRO pill when locked for Free users, title, and author.
 */
@Composable
fun AudiobookCarouselCard(
  book: AudiobookBook,
  targetLang: Language,
  onClick: () -> Unit,
  isProUnlocked: Boolean = false
) {
  val grad = book.coverGradientColors.map { Color(it) }.ifEmpty { listOf(DeepNavyCard, DeepNavyElevated) }
  val hasRealAudio = book.hasVerifiedHumanAudio || (book.contentType == BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK && book.hasHumanNarration) || book.librivoxEditions.isNotEmpty()
  val hasRealBookText = book.hasVerifiedFullText || book.hasFullTextAvailable || book.gutendexId != null || !book.fullTextUrl.isNullOrBlank() || book.parsedChapters.isNotEmpty()
  val chCount = book.totalChapterCount
  val langCode = (book.languages.firstOrNull() ?: book.targetLanguageCode).uppercase().ifBlank { "EN" }
  val openedBooksThisMonth by com.example.data.repository.AccessPolicyManager.openedBookIdsThisMonth.collectAsState()
  val policyLimits by com.example.data.repository.AccessPolicyManager.policyState.collectAsState()
  val cleanBookKey = remember(book.id, book.gutendexId) {
    book.id.trim().ifBlank { book.gutendexId?.let { "gutenberg_$it" } ?: "" }
  }
  val isWithinMonthlyAllowance = remember(isProUnlocked, cleanBookKey, openedBooksThisMonth, policyLimits.freeBooksPerMonth) {
    isProUnlocked ||
      (cleanBookKey.isNotBlank() && openedBooksThisMonth.any { it.equals(cleanBookKey, ignoreCase = true) }) ||
      openedBooksThisMonth.size < policyLimits.freeBooksPerMonth.coerceAtLeast(1)
  }
  val requiresProLock = !isProUnlocked && !isWithinMonthlyAllowance

  val badgeText = when {
    requiresProLock -> "Pro"
    hasRealAudio && hasRealBookText -> "FREE · FULL"
    hasRealAudio -> "FREE · AUDIO"
    hasRealBookText -> "FREE · BOOK"
    else -> "AI-GENERATED"
  }
  val badgeColor = when {
    requiresProLock -> AmberAccent
    hasRealAudio -> EmeraldAccent
    hasRealBookText -> EmeraldAccent
    else -> CyanAccent
  }

  Column(
    modifier = Modifier
      .width(120.dp)
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .testTag("audiobook_card_${book.id}")
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = grad.first(),
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
      modifier = Modifier
        .width(120.dp)
        .height(170.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Brush.linearGradient(grad))
      ) {
        Column(
          modifier = Modifier
            .align(Alignment.Center)
            .padding(10.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            imageVector = if (hasRealAudio) Icons.Default.Headphones else Icons.Default.MenuBook,
            contentDescription = null,
            tint = TextWhite.copy(alpha = 0.85f),
            modifier = Modifier.size(28.dp)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = book.title,
            color = TextWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 3,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
          )
        }

        if (book.coverArtUrl.isNotBlank()) {
          AsyncImage(
            model = book.coverArtUrl,
            contentDescription = book.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        }

        if (requiresProLock) {
          Box(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(8.dp)
          ) {
            LingoGoldLockIcon(size = 16.dp, contentDescription = "Pro book")
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = book.title,
      color = TextWhite,
      fontSize = 13.sp,
      lineHeight = 18.sp,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.fillMaxWidth()
    )

    Text(
      text = book.author,
      color = Color(0xFFA8B3CF),
      fontSize = 12.sp,
      lineHeight = 16.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.fillMaxWidth()
    )
  }
}

/**
 * Holds catalog browsing & pagination state across Book Detail / Player navigation
 * so the user's scroll position, loaded books, pagination cursors, and filters are preserved.
 */
class AudiobooksCatalogSessionState(initialLetter: String = "All") {
  var selectedLetter by mutableStateOf(initialLetter)
  var selectedCategoryTab by mutableStateOf("All")
  var selectedLanguageCode by mutableStateOf("ALL")
  var selectedNarratorFilter by mutableStateOf("All Narrators")
  var filterOnlyAudio by mutableStateOf(false)
  var filterOnlyText by mutableStateOf(false)
  var filterOnlyAiNarration by mutableStateOf(false)
  var filterByTargetLangOnly by mutableStateOf(false)
  var searchQuery by mutableStateOf("")

  var remoteBooks by mutableStateOf<List<AudiobookBook>>(emptyList())
  var gutendexNextPageUrl by mutableStateOf<String?>(null)
  var librivoxNextOffset by mutableStateOf<Int?>(null)
  var hasMoreRemote by mutableStateOf(false)
  var totalCatalogCount by mutableIntStateOf(0)
  var lastLoadedFilterSignature by mutableStateOf("")
  var hasCompletedInitialLoad by mutableStateOf(false)
  var autoPrefetchPagesCount by mutableIntStateOf(0)
  var consecutiveEmptyPages by mutableIntStateOf(0)

  var savedScrollIndex by mutableIntStateOf(0)
  var savedScrollOffset by mutableIntStateOf(0)
}

/**
 * Dedicated Complete A–Z Book Library & Audiobooks Category Page:
 * Houses the full A–Z book catalog, alphabetical navigation (A–Z), all 21 genres/categories,
 * language filters, narrator filters, human audio / AI narration / full text availability filters,
 * live Gutendex & LibriVox search, infinite scroll, and Load More pagination.
 */
@Composable
fun AudiobooksCategoryScreen(
  userId: String,
  targetLang: Language,
  selectedCefrLevel: String = "B1",
  initialSelectedLetter: String = "All",
  sessionState: AudiobooksCatalogSessionState = remember(initialSelectedLetter) {
    AudiobooksCatalogSessionState(initialSelectedLetter)
  },
  isProUnlocked: Boolean = false,
  userProfile: UserProfile? = null,
  onOpenSubscription: () -> Unit = {},
  onSelectBook: (AudiobookBook) -> Unit,
  onOpenSearch: () -> Unit,
  onOpenCreateListening: () -> Unit = {},
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var selectedLetter by sessionState::selectedLetter
  var selectedCategoryTab by sessionState::selectedCategoryTab
  var selectedLanguageCode by sessionState::selectedLanguageCode
  var selectedNarratorFilter by sessionState::selectedNarratorFilter
  var filterOnlyAudio by sessionState::filterOnlyAudio
  var filterOnlyText by sessionState::filterOnlyText
  var filterOnlyAiNarration by sessionState::filterOnlyAiNarration
  var filterByTargetLangOnly by sessionState::filterByTargetLangOnly
  var searchQuery by sessionState::searchQuery

  var remoteBooks by sessionState::remoteBooks
  var gutendexNextPageUrl by sessionState::gutendexNextPageUrl
  var librivoxNextOffset by sessionState::librivoxNextOffset
  var hasMoreRemote by sessionState::hasMoreRemote
  var totalCatalogCount by sessionState::totalCatalogCount

  var isLoading by remember { mutableStateOf(false) }
  var isLoadingMore by remember { mutableStateOf(false) }
  var apiErrorMessage by remember { mutableStateOf<String?>(null) }
  var partialSourceNotice by remember { mutableStateOf<String?>(null) }
  var paginationErrorMessage by remember { mutableStateOf<String?>(null) }
  var searchJob by remember { mutableStateOf<Job?>(null) }

  // Restore cached first page immediately on open if remoteBooks is empty
  LaunchedEffect(Unit) {
    if (remoteBooks.isEmpty() && FirstPageBookCatalogCache.cachedFirstPageBooks.isNotEmpty()) {
      remoteBooks = FirstPageBookCatalogCache.cachedFirstPageBooks
      if (totalCatalogCount == 0) {
        totalCatalogCount = FirstPageBookCatalogCache.loadTotalCount(context)
      }
    }
  }

  val listState = rememberLazyListState(
    initialFirstVisibleItemIndex = sessionState.savedScrollIndex,
    initialFirstVisibleItemScrollOffset = sessionState.savedScrollOffset
  )

  LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
    sessionState.savedScrollIndex = listState.firstVisibleItemIndex
    sessionState.savedScrollOffset = listState.firstVisibleItemScrollOffset
  }

  val defaultCatalog = remember(userId, remoteBooks.size) {
    AudiobookLearningService.getAllAvailableBooks(context, userId).distinctBy { it.id }
  }

  val effectiveLanguageParam = when {
    filterByTargetLangOnly -> targetLang.code
    selectedLanguageCode != "ALL" -> selectedLanguageCode
    else -> null
  }

  val hasAnyActiveFilter = searchQuery.isNotBlank() ||
    selectedCategoryTab != "All" ||
    selectedLanguageCode != "ALL" ||
    selectedNarratorFilter != "All Narrators" ||
    filterOnlyAudio ||
    filterOnlyText ||
    filterOnlyAiNarration ||
    filterByTargetLangOnly

  fun clearAllCatalogFilters() {
    searchQuery = ""
    selectedLetter = "All"
    selectedCategoryTab = "All"
    selectedLanguageCode = "ALL"
    selectedNarratorFilter = "All Narrators"
    filterOnlyAudio = false
    filterOnlyText = false
    filterOnlyAiNarration = false
    filterByTargetLangOnly = false
  }

  fun buildFilterSignature(
    categoryTab: String = selectedCategoryTab,
    search: String = searchQuery,
    onlyAudio: Boolean = filterOnlyAudio,
    onlyText: Boolean = filterOnlyText,
    onlyAi: Boolean = filterOnlyAiNarration,
    langParam: String? = effectiveLanguageParam,
    narrator: String = selectedNarratorFilter
  ): String {
    return "ALL|$categoryTab|${search.trim().lowercase()}|$onlyAudio|$onlyText|$onlyAi|${langParam.orEmpty()}|$narrator"
  }

  // Load books from live Gutendex + LibriVox APIs with cancellation & 400ms debounce
  fun loadCategoryBooks(
    categoryTab: String = selectedCategoryTab,
    search: String = searchQuery,
    onlyAudio: Boolean = filterOnlyAudio,
    onlyText: Boolean = filterOnlyText,
    onlyAi: Boolean = filterOnlyAiNarration,
    langParam: String? = effectiveLanguageParam,
    narrator: String = selectedNarratorFilter,
    immediate: Boolean = false,
    resetPagination: Boolean = true
  ) {
    val sig = buildFilterSignature(categoryTab, search, onlyAudio, onlyText, onlyAi, langParam, narrator)
    searchJob?.cancel()
    searchJob = scope.launch {
      if (!immediate && search.isNotBlank()) delay(400L)
      isLoading = true
      apiErrorMessage = null
      partialSourceNotice = null
      paginationErrorMessage = null
      if (resetPagination) {
        gutendexNextPageUrl = null
        librivoxNextOffset = null
        sessionState.autoPrefetchPagesCount = 0
        sessionState.consecutiveEmptyPages = 0
        sessionState.savedScrollIndex = 0
        sessionState.savedScrollOffset = 0
      }
      val pageRes = GutendexAndLibriVoxService.searchCatalogPaginated(
        query = search.trim(),
        languageCode = langParam,
        categoryTab = categoryTab,
        onlyWithAudio = onlyAudio,
        onlyWithText = onlyText,
        selectedLetter = null,
        narratorFilter = narrator.takeIf { !it.equals("All Narrators", ignoreCase = true) },
        onlyAiNarration = onlyAi,
        sortAlphabetically = false
      )
      remoteBooks = pageRes.books
      gutendexNextPageUrl = pageRes.gutendexNextPageUrl
      librivoxNextOffset = pageRes.librivoxNextOffset
      hasMoreRemote = pageRes.hasMore
      totalCatalogCount = pageRes.totalCount
      apiErrorMessage = pageRes.errorMessage
      partialSourceNotice = pageRes.partialSourceNotice
      if (categoryTab == "All" && search.isBlank() && !onlyAudio && !onlyText && !onlyAi && langParam == null && pageRes.books.isNotEmpty()) {
        FirstPageBookCatalogCache.saveFirstPage(context, pageRes.books, pageRes.totalCount)
      }
      sessionState.lastLoadedFilterSignature = sig
      sessionState.hasCompletedInitialLoad = true
      isLoading = false
    }
  }

  LaunchedEffect(
    selectedCategoryTab,
    selectedLanguageCode,
    selectedNarratorFilter,
    filterOnlyAudio,
    filterOnlyText,
    filterOnlyAiNarration,
    filterByTargetLangOnly
  ) {
    selectedLetter = "All"
    val currentSig = buildFilterSignature()
    if (!sessionState.hasCompletedInitialLoad || sessionState.lastLoadedFilterSignature != currentSig) {
      loadCategoryBooks(immediate = true, resetPagination = true)
    }
  }

  val displayedBooks = remember(
    defaultCatalog,
    remoteBooks,
    selectedCategoryTab,
    effectiveLanguageParam,
    selectedNarratorFilter,
    filterOnlyAudio,
    filterOnlyText,
    filterOnlyAiNarration,
    searchQuery
  ) {
    val matchingLocal = defaultCatalog.filter { book ->
      val matchesCat = AudiobookCatalogRepository.matchesCategoryTab(book, selectedCategoryTab)
      val matchesNarrator = GutendexAndLibriVoxService.matchesNarratorFilter(book, selectedNarratorFilter)
      val matchesAudio = !filterOnlyAudio || (book.hasHumanNarration || book.librivoxEditions.isNotEmpty())
      val matchesText = !filterOnlyText || (book.hasFullTextAvailable || !book.fullTextUrl.isNullOrBlank() || book.gutendexId != null)
      val matchesAi = !filterOnlyAiNarration || (book.hasFullTextAvailable || book.gutendexId != null || book.parsedChapters.isNotEmpty() || book.episodes.isNotEmpty())
      val matchesLang = effectiveLanguageParam == null || book.languages.any { it.equals(effectiveLanguageParam, ignoreCase = true) }
      val matchesSearch = GutendexAndLibriVoxService.matchesSearchQuery(book, searchQuery)

      matchesCat && matchesNarrator && matchesAudio && matchesText && matchesAi && matchesLang && matchesSearch
    }

    val matchingRemote = remoteBooks.filter { book ->
      val matchesCat = AudiobookCatalogRepository.matchesCategoryTab(book, selectedCategoryTab)
      val matchesNarrator = GutendexAndLibriVoxService.matchesNarratorFilter(book, selectedNarratorFilter)
      val matchesAudio = !filterOnlyAudio || (book.hasHumanNarration || book.librivoxEditions.isNotEmpty())
      val matchesText = filterOnlyWithTextMatches(book, filterOnlyText)
      val matchesAi = !filterOnlyAiNarration || (book.hasFullTextAvailable || book.gutendexId != null || book.parsedChapters.isNotEmpty() || book.episodes.isNotEmpty())
      val matchesLang = effectiveLanguageParam == null || book.languages.any { it.equals(effectiveLanguageParam, ignoreCase = true) }
      val matchesSearch = GutendexAndLibriVoxService.matchesSearchQuery(book, searchQuery)
      matchesCat && matchesNarrator && matchesAudio && matchesText && matchesAi && matchesLang && matchesSearch
    }

    mergeDeduplicatedCatalogBooks(matchingLocal, matchingRemote)
  }

  val availableNarrators = remember(defaultCatalog, remoteBooks) {
    val dynamicNarrators = (defaultCatalog + remoteBooks)
      .flatMap { it.librivoxEditions }
      .map { it.narratorName.trim() }
      .filter { it.isNotBlank() && !it.equals("Human Narrator", ignoreCase = true) && !it.equals("Human Reader", ignoreCase = true) }
    (AudiobookCatalogRepository.POPULAR_NARRATORS + dynamicNarrators).distinct()
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DeepNavyBg)
      .padding(horizontal = 20.dp, vertical = 14.dp)
      .testTag("audiobooks_category_screen")
  ) {
    // Top Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
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
            .background(DeepNavyCard)
            .testTag("audiobooks_back_button")
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "Book & Audio Library",
            color = TextWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "Complete Project Gutenberg & LibriVox catalog",
            color = CyanAccent,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = { loadCategoryBooks(immediate = true, resetPagination = true) },
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(DeepNavyCard)
            .testTag("audiobooks_refresh_btn")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh Library", tint = CyanAccent, modifier = Modifier.size(20.dp))
        }

        IconButton(
          onClick = onOpenSearch,
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(DeepNavyCard)
        ) {
          Icon(Icons.Default.Search, contentDescription = "Search", tint = CyanAccent, modifier = Modifier.size(20.dp))
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Sticky Search text field with 400ms debounce & Clear (X) button
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { newText ->
        searchQuery = newText
        loadCategoryBooks(search = newText)
      },
      placeholder = { Text("Search by title, author, or keyword...", color = TextMuted, fontSize = 12.5.sp) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp)) },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(
            onClick = {
              searchQuery = ""
              loadCategoryBooks(search = "", immediate = true)
            },
            modifier = Modifier.testTag("audiobooks_search_clear_btn")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
          }
        }
      },
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextWhite,
        unfocusedTextColor = TextWhite,
        focusedBorderColor = CyanAccent,
        unfocusedBorderColor = DeepNavyBorder,
        focusedContainerColor = DeepNavyCard,
        unfocusedContainerColor = DeepNavyCard
      ),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("audiobooks_category_search_input")
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Row 1 — Format Filters: All Formats | Human Audio | AI Narration | Clear filters
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      FilterChip(
        selected = !filterOnlyAudio && !filterOnlyText && !filterOnlyAiNarration,
        onClick = {
          filterOnlyAudio = false
          filterOnlyText = false
          filterOnlyAiNarration = false
        },
        label = { Text("All Formats", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = CyanAccent.copy(alpha = 0.22f),
          selectedLabelColor = CyanAccent,
          containerColor = DeepNavyCard,
          labelColor = TextLight
        ),
        modifier = Modifier.testTag("filter_all_formats")
      )

      FilterChip(
        selected = filterOnlyAudio,
        onClick = {
          filterOnlyAudio = !filterOnlyAudio
          if (filterOnlyAudio) {
            filterOnlyText = false
            filterOnlyAiNarration = false
          }
        },
        label = { Text("🎧 Human Audio", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = AmberAccent.copy(alpha = 0.22f),
          selectedLabelColor = AmberAccent,
          containerColor = DeepNavyCard,
          labelColor = TextLight
        ),
        modifier = Modifier.testTag("filter_only_audio")
      )

      FilterChip(
        selected = filterOnlyAiNarration,
        onClick = {
          filterOnlyAiNarration = !filterOnlyAiNarration
          if (filterOnlyAiNarration) {
            filterOnlyAudio = false
          }
        },
        label = { Text("🤖 AI Narration", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = CyanAccent.copy(alpha = 0.22f),
          selectedLabelColor = CyanAccent,
          containerColor = DeepNavyCard,
          labelColor = TextLight
        ),
        modifier = Modifier.testTag("filter_only_ai_narration")
      )

      FilterChip(
        selected = filterOnlyText,
        onClick = {
          filterOnlyText = !filterOnlyText
          if (filterOnlyText) filterOnlyAudio = false
        },
        label = { Text("📖 Full Books", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = EmeraldAccent.copy(alpha = 0.22f),
          selectedLabelColor = EmeraldAccent,
          containerColor = DeepNavyCard,
          labelColor = TextLight
        ),
        modifier = Modifier.testTag("filter_only_text")
      )

      FilterChip(
        selected = filterByTargetLangOnly,
        onClick = { filterByTargetLangOnly = !filterByTargetLangOnly },
        label = { Text("${targetLang.flagEmoji} ${targetLang.name} Only", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = PurpleAccent.copy(alpha = 0.22f),
          selectedLabelColor = PurpleAccent,
          containerColor = DeepNavyCard,
          labelColor = TextLight
        ),
        modifier = Modifier.testTag("filter_target_lang_only")
      )

      if (hasAnyActiveFilter) {
        FilterChip(
          selected = false,
          onClick = { clearAllCatalogFilters() },
          label = { Text("Clear filters", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          colors = FilterChipDefaults.filterChipColors(
            containerColor = AmberAccent.copy(alpha = 0.18f),
            labelColor = AmberAccent
          ),
          modifier = Modifier.testTag("audiobooks_clear_filters_btn")
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Row 2 — Categories
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      AudiobookCatalogRepository.MAIN_CATEGORY_TABS.forEach { tab ->
        val isSel = selectedCategoryTab == tab
        FilterChip(
          selected = isSel,
          onClick = { selectedCategoryTab = tab },
          label = { Text(tab, fontSize = 11.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CyanAccent.copy(alpha = 0.22f),
            selectedLabelColor = CyanAccent,
            containerColor = DeepNavyCard,
            labelColor = TextLight
          ),
          modifier = Modifier.testTag("category_chip_${tab.lowercase().replace(Regex("[^a-z0-9]+"), "_")}")
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Row 3 — Languages & Narrators
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      AudiobookCatalogRepository.CATALOG_LANGUAGE_OPTIONS.forEach { (code, label) ->
        val isSel = !filterByTargetLangOnly && selectedLanguageCode == code
        FilterChip(
          selected = isSel,
          onClick = {
            filterByTargetLangOnly = false
            selectedLanguageCode = code
          },
          label = { Text(label, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = EmeraldAccent.copy(alpha = 0.22f),
            selectedLabelColor = EmeraldAccent,
            containerColor = DeepNavyCard,
            labelColor = TextLight
          ),
          modifier = Modifier.testTag("lang_chip_${code.lowercase()}")
        )
      }

      availableNarrators.forEach { narrator ->
        val isSel = selectedNarratorFilter == narrator
        val tagKey = narrator.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        FilterChip(
          selected = isSel,
          onClick = { selectedNarratorFilter = narrator },
          label = {
            Text(
              text = if (narrator == "All Narrators") "🎙️ All Narrators" else "🎙️ $narrator",
              fontSize = 11.sp,
              fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AmberAccent.copy(alpha = 0.22f),
            selectedLabelColor = AmberAccent,
            containerColor = DeepNavyCard,
            labelColor = TextLight
          ),
          modifier = Modifier.testTag("narrator_chip_$tagKey")
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Live API Results Count Header (zero A-Z text)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      val effectiveTotal = maxOf(totalCatalogCount, displayedBooks.size)
      val plusSuffix = if (librivoxNextOffset != null || hasMoreRemote) "+" else ""
      val headerText = when {
        isLoading && displayedBooks.isEmpty() -> "Loading books & audiobooks..."
        apiErrorMessage != null && displayedBooks.isEmpty() -> "Catalog temporarily unavailable"
        displayedBooks.isEmpty() -> "No books found"
        else -> "${"%,d".format(effectiveTotal)}$plusSuffix books"
      }
      Text(
        text = headerText,
        color = TextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.testTag("audiobooks_count_text")
      )
      if (isLoading) {
        CircularProgressIndicator(color = CyanAccent, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (isLoading && displayedBooks.isEmpty()) {
      // Skeleton loader cards on initial load
      Column(
        modifier = Modifier
          .fillMaxSize()
          .testTag("audiobooks_skeleton_loader"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        repeat(5) {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
            border = BorderStroke(1.dp, DeepNavyBorder),
            modifier = Modifier
              .fillMaxWidth()
              .height(96.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(width = 56.dp, height = 76.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(DeepNavyElevated)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Box(
                  modifier = Modifier
                    .width(120.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(DeepNavyElevated)
                )
                Box(
                  modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(DeepNavyElevated)
                )
                Box(
                  modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(DeepNavyElevated)
                )
              }
            }
          }
        }
      }
    } else if (apiErrorMessage != null && displayedBooks.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center
      ) {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
          border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.55f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = apiErrorMessage!!,
              color = TextWhite,
              fontSize = 13.5.sp,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.material3.Button(
              onClick = {
                loadCategoryBooks(immediate = true)
              },
              colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF041220)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("audiobooks_category_retry_btn")
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Retry", fontWeight = FontWeight.ExtraBold)
            }
          }
        }
      }
    } else if (!isLoading && displayedBooks.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center
      ) {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
          border = BorderStroke(1.dp, DeepNavyBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "No books found",
              color = TextWhite,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = if (searchQuery.isNotBlank()) {
                "No books matched \"$searchQuery\" in $selectedCategoryTab."
              } else {
                "No books matched the active filters in $selectedCategoryTab."
              },
              color = TextLight,
              fontSize = 13.sp,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.material3.OutlinedButton(
              onClick = { clearAllCatalogFilters() },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("audiobooks_reset_filters_btn")
            ) {
              Text("Clear filters", color = CyanAccent, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    } else {
      val shouldAutoLoadMore by remember {
        derivedStateOf {
          val layoutInfo = listState.layoutInfo
          val total = layoutInfo.totalItemsCount
          val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
          val isNearBottom = total > 0 && lastVisible >= (total - 5).coerceAtLeast(0)
          val needsMoreToFillInitialCatalog = displayedBooks.size < 30 && sessionState.autoPrefetchPagesCount < 3
          hasMoreRemote &&
            !isLoadingMore &&
            !isLoading &&
            paginationErrorMessage == null &&
            sessionState.consecutiveEmptyPages < 3 &&
            (isNearBottom || needsMoreToFillInitialCatalog)
        }
      }

      LaunchedEffect(shouldAutoLoadMore, gutendexNextPageUrl, librivoxNextOffset) {
        if (shouldAutoLoadMore) {
          isLoadingMore = true
          paginationErrorMessage = null
          sessionState.autoPrefetchPagesCount++
          val prevRemoteCount = remoteBooks.size
          val moreRes = GutendexAndLibriVoxService.searchCatalogPaginated(
            query = searchQuery.trim(),
            languageCode = effectiveLanguageParam,
            categoryTab = selectedCategoryTab,
            onlyWithAudio = filterOnlyAudio,
            onlyWithText = filterOnlyText,
            gutendexPageUrl = gutendexNextPageUrl,
            librivoxOffset = librivoxNextOffset ?: 0,
            selectedLetter = null,
            narratorFilter = selectedNarratorFilter.takeIf { !it.equals("All Narrators", ignoreCase = true) },
            onlyAiNarration = filterOnlyAiNarration,
            sortAlphabetically = false
          )
          if (moreRes.books.isNotEmpty()) {
            remoteBooks = mergeDeduplicatedCatalogBooks(remoteBooks, moreRes.books)
          }
          if (remoteBooks.size > prevRemoteCount) {
            sessionState.consecutiveEmptyPages = 0
          } else {
            sessionState.consecutiveEmptyPages++
          }
          gutendexNextPageUrl = moreRes.gutendexNextPageUrl
          librivoxNextOffset = moreRes.librivoxNextOffset
          hasMoreRemote = moreRes.hasMore
          if (moreRes.totalCount > totalCatalogCount) {
            totalCatalogCount = moreRes.totalCount
          }
          paginationErrorMessage = moreRes.errorMessage
          isLoadingMore = false
        }
      }

      // Continuous Books Collection List
      LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
          .fillMaxSize()
          .testTag("audiobooks_category_lazy_list")
      ) {
        if (apiErrorMessage != null || partialSourceNotice != null) {
          item(key = "category_offline_banner") {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = DeepNavyCard,
              border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.45f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = partialSourceNotice ?: "Showing saved library • Online catalog unavailable",
                  color = AmberAccent,
                  fontSize = 11.5.sp,
                  modifier = Modifier.weight(1f)
                )
                Surface(
                  onClick = {
                    loadCategoryBooks(immediate = true)
                  },
                  shape = RoundedCornerShape(8.dp),
                  color = CyanAccent,
                  modifier = Modifier.testTag("category_offline_retry_btn")
                ) {
                  Text("Retry", color = Color(0xFF041220), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
              }
            }
          }
        }

        itemsIndexed(displayedBooks, key = { _, book -> book.id }) { index, book ->
          AudiobookCompactListItem(
            book = book,
            targetLang = targetLang,
            onClick = { onSelectBook(book) },
            isProUnlocked = isProUnlocked
          )
          if (!isProUnlocked && (index + 1) % 8 == 0 && com.example.ads.AdService.shouldShowAds(userProfile)) {
            Spacer(modifier = Modifier.height(10.dp))
            com.example.ads.LingoBookListNativeAdCard(
              userProfile = userProfile,
              slotIndex = (index + 1) / 8,
              onGoProClick = onOpenSubscription
            )
          }
        }

        if (isLoadingMore) {
          item(key = "infinite_scroll_spinner") {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("infinite_scroll_loading_indicator"),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              CircularProgressIndicator(color = CyanAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Loading more...", color = TextLight, fontSize = 12.sp)
            }
          }
        }

        if (hasMoreRemote || paginationErrorMessage != null) {
          item(key = "category_load_more") {
            Column(modifier = Modifier.fillMaxWidth()) {
              if (paginationErrorMessage != null) {
                Text(
                  text = paginationErrorMessage!!,
                  color = AmberAccent,
                  fontSize = 12.sp,
                  modifier = Modifier.padding(bottom = 6.dp)
                )
              }
              androidx.compose.material3.Button(
                onClick = {
                  scope.launch {
                    isLoadingMore = true
                    paginationErrorMessage = null
                    sessionState.consecutiveEmptyPages = 0
                    val moreRes = GutendexAndLibriVoxService.searchCatalogPaginated(
                      query = searchQuery.trim(),
                      languageCode = effectiveLanguageParam,
                      categoryTab = selectedCategoryTab,
                      onlyWithAudio = filterOnlyAudio,
                      onlyWithText = filterOnlyText,
                      gutendexPageUrl = gutendexNextPageUrl,
                      librivoxOffset = librivoxNextOffset ?: 0,
                      selectedLetter = null,
                      narratorFilter = selectedNarratorFilter.takeIf { !it.equals("All Narrators", ignoreCase = true) },
                      onlyAiNarration = filterOnlyAiNarration,
                      sortAlphabetically = false
                    )
                    if (moreRes.books.isNotEmpty()) {
                      remoteBooks = mergeDeduplicatedCatalogBooks(remoteBooks, moreRes.books)
                    }
                    gutendexNextPageUrl = moreRes.gutendexNextPageUrl
                    librivoxNextOffset = moreRes.librivoxNextOffset
                    hasMoreRemote = moreRes.hasMore
                    if (moreRes.totalCount > totalCatalogCount) {
                      totalCatalogCount = moreRes.totalCount
                    }
                    paginationErrorMessage = moreRes.errorMessage
                    isLoadingMore = false
                  }
                },
                enabled = !isLoadingMore,
                colors = ButtonDefaults.buttonColors(containerColor = DeepNavyElevated, contentColor = CyanAccent),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 8.dp)
                  .testTag("category_load_more_btn")
              ) {
                if (isLoadingMore) {
                  CircularProgressIndicator(color = CyanAccent, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Loading more...")
                } else {
                  Text("Load More Books & Audiobooks", fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        } else if (!isLoading && displayedBooks.isNotEmpty()) {
          item(key = "category_end_of_results") {
            Text(
              text = "All ${displayedBooks.size} matching books loaded",
              color = TextMuted,
              fontSize = 11.5.sp,
              textAlign = TextAlign.Center,
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("end_of_catalog_results_footer")
            )
          }
        }
      }
    }
  }
}

private fun filterOnlyWithTextMatches(book: AudiobookBook, onlyText: Boolean): Boolean {
  return !onlyText || book.hasFullTextAvailable || !book.fullTextUrl.isNullOrBlank() || book.gutendexId != null
}

fun mergeDeduplicatedCatalogBooks(
  primary: List<AudiobookBook>,
  secondary: List<AudiobookBook>
): List<AudiobookBook> {
  val result = primary.toMutableList()
  for (candidate in secondary) {
    val existingIdx = result.indexOfFirst { existing ->
      existing.id == candidate.id ||
        GutendexAndLibriVoxService.isSameBookTitleAndAuthor(
          candidate.title,
          candidate.author,
          existing.title,
          existing.author
        )
    }
    if (existingIdx < 0) {
      result.add(candidate)
    } else {
      val existing = result[existingIdx]
      val mergedEditions = (existing.librivoxEditions + candidate.librivoxEditions).distinctBy { it.id }
      result[existingIdx] = existing.copy(
        librivoxEditions = mergedEditions,
        gutendexId = existing.gutendexId ?: candidate.gutendexId,
        fullTextUrl = existing.fullTextUrl ?: candidate.fullTextUrl,
        hasFullTextAvailable = existing.hasFullTextAvailable || candidate.hasFullTextAvailable,
        parsedChapters = if (existing.parsedChapters.isNotEmpty()) existing.parsedChapters else candidate.parsedChapters,
        contentType = if (mergedEditions.isNotEmpty()) BookContentType.LIBRIVOX_HUMAN_AUDIOBOOK else existing.contentType
      )
    }
  }
  return result
}

/**
 * Dedicated My Library Screen (Section 2 & 5 of user requirements):
 * Cleanly organizes Saved Books, Favorites, Listening History, Bookmarks, and Completed Lessons
 * into 7 tabs without taking over the home screen.
 */
@Composable
fun MyLibraryDedicatedScreen(
  userProfile: UserProfile,
  targetLang: Language,
  allBooks: List<AudiobookBook>,
  onSelectBook: (AudiobookBook) -> Unit,
  onSelectLesson: (ListeningAcademyItem) -> Unit = {},
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val libraryTabs = remember {
    listOf(
      "Continue Listening",
      "Saved Books",
      "Favorites",
      "Listening History",
      "Completed Books",
      "Bookmarks",
      "Recently Opened"
    )
  }

  var selectedTab by remember { mutableStateOf("Continue Listening") }
  var tabBooks by remember { mutableStateOf<List<AudiobookBook>>(emptyList()) }
  var userBookmarks by remember { mutableStateOf<List<AudiobookBookmark>>(emptyList()) }
  var isLoading by remember { mutableStateOf(false) }

  LaunchedEffect(selectedTab, userProfile.userId, allBooks) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
      isLoading = true
      if (selectedTab == "Bookmarks") {
        val bms = AudiobookLearningService.getBookmarksForUser(context, userProfile.userId)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
          userBookmarks = bms
          tabBooks = emptyList()
          isLoading = false
        }
      } else {
        val list = when (selectedTab) {
          "Continue Listening" -> AudiobookLearningService.getContinueListeningBooks(context, userProfile.userId)
          "Saved Books" -> AudiobookLearningService.getSavedBooks(context, userProfile.userId)
          "Favorites" -> AudiobookLearningService.getFavoriteBooks(context, userProfile.userId)
          "Listening History" -> AudiobookLearningService.getRecentlyListenedBooks(context, userProfile.userId)
          "Completed Books" -> AudiobookLearningService.getCompletedBooks(context, userProfile.userId)
          "Recently Opened" -> AudiobookLearningService.getRecentlyOpenedBooks(context, userProfile.userId)
          else -> emptyList()
        }
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
          tabBooks = list.ifEmpty { allBooks.take(3) }
          userBookmarks = emptyList()
          isLoading = false
        }
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DeepNavyBg)
      .padding(horizontal = 20.dp, vertical = 14.dp)
      .testTag("my_library_screen")
  ) {
    // Top Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(DeepNavyCard)
      ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          text = "My Library",
          color = TextWhite,
          fontSize = 20.sp,
          fontWeight = FontWeight.ExtraBold
        )
        Text(
          text = "Your saved books, audio, and learning progress",
          color = CyanAccent,
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 7 Horizontal Tabs
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
    ) {
      libraryTabs.forEach { tab ->
        val isSelected = selectedTab == tab
        FilterChip(
          selected = isSelected,
          onClick = { selectedTab = tab },
          label = {
            Text(
              text = tab,
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CyanAccent.copy(alpha = 0.22f),
            selectedLabelColor = CyanAccent,
            containerColor = DeepNavyCard,
            labelColor = TextLight
          ),
          shape = RoundedCornerShape(12.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (isLoading) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = CyanAccent)
      }
    } else if (selectedTab == "Bookmarks") {
      if (userBookmarks.isEmpty()) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = DeepNavyCard,
          border = BorderStroke(1.dp, DeepNavyBorder),
          modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
          Text(
            text = "No bookmarks saved yet. Tap the Bookmark icon inside any book chapter to save your place with one tap.",
            color = TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(20.dp),
            textAlign = TextAlign.Center
          )
        }
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 20.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(userBookmarks, key = { it.id }) { bm ->
            val matchedBook = allBooks.find { it.id == bm.bookId }
            Surface(
              onClick = { matchedBook?.let { onSelectBook(it) } },
              shape = RoundedCornerShape(14.dp),
              color = DeepNavyCard,
              border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "${matchedBook?.title ?: "Saved Book"} • Ch. ${bm.chapterIndex + 1}",
                    color = CyanAccent,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                  )
                  Icon(Icons.Default.Bookmark, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                }
                if (bm.snippetText.isNotBlank()) {
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = "\"${bm.snippetText}\"",
                    color = TextWhite,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }
      }
    } else {
      if (tabBooks.isEmpty()) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = DeepNavyCard,
          border = BorderStroke(1.dp, DeepNavyBorder),
          modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
          Text(
            text = "No books in \"$selectedTab\" yet. Explore audiobooks or classic literature to add content here.",
            color = TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(20.dp),
            textAlign = TextAlign.Center
          )
        }
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 20.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(tabBooks, key = { it.id }) { book ->
            AudiobookCompactListItem(
              book = book,
              targetLang = targetLang,
              onClick = { onSelectBook(book) },
              isProUnlocked = userProfile.isEntitlementVerified
            )
          }
        }
      }
    }
  }
}
