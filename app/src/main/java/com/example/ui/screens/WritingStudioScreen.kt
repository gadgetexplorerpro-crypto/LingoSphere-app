package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.ai.AiErrorHandler
import com.example.ai.GeminiTutorService
import com.example.ai.UpgradeSheetMode
import com.example.data.model.Language
import com.example.data.model.UserProfile
import com.example.data.model.WritingCoachResult
import com.example.speech.SpeechManager
import com.example.ui.components.LingoCalmAiErrorBanner
import com.example.ui.components.LingoGoldLockIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val NavyBackground = Color(0xFF060D1D)
private val NavyCardBg = Color(0xFF0D162B)
private val NavyCardElevated = Color(0xFF13203D)
private val NavyBorder = Color(0xFF1D2E52)
private val ElectricBlue = Color(0xFF3B82F6)
private val CyanAccent = Color(0xFF38BDF8)
private val GoldAccent = Color(0xFFFBBF24)
private val EmeraldAccent = Color(0xFF10B981)
private val CoralAccent = Color(0xFFF43F5E)
private val PurpleAccent = Color(0xFF8B5CF6)
private val TextWhite = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)
private val TextLight = Color(0xFFCBD5E1)

private enum class WritingViewMode {
  HOME,
  TOPIC_SELECT,
  EDITOR,
  EVALUATION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingStudioScreen(
  userProfile: UserProfile,
  speechManager: SpeechManager,
  onAddXp: (Long) -> Unit = {},
  onOpenSubscription: () -> Unit = {},
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val targetLang = remember(userProfile.targetLanguageCode) { Language.findByCode(userProfile.targetLanguageCode) }
  val nativeLang = remember(userProfile.nativeLanguageCode) { Language.findByCode(userProfile.nativeLanguageCode) }

  val isVerifiedPro = userProfile.isEntitlementVerified
  var viewMode by remember { mutableStateOf(WritingViewMode.HOME) }
  var selectedCategory by remember { mutableStateOf("Daily Life") }
  var writingTopic by remember { mutableStateOf("My Daily Routine") }
  var customTopicInput by remember { mutableStateOf("") }
  var userTextContent by remember { mutableStateOf("") }
  var isEvaluating by remember { mutableStateOf(false) }
  var writingErrorMessage by remember { mutableStateOf<String?>(null) }
  var writingResult by remember { mutableStateOf<WritingCoachResult?>(null) }
  var savedDraftsCount by remember { mutableIntStateOf(3) }

  BackHandler {
    when (viewMode) {
      WritingViewMode.HOME -> onBack()
      WritingViewMode.TOPIC_SELECT -> viewMode = WritingViewMode.HOME
      WritingViewMode.EDITOR -> viewMode = WritingViewMode.TOPIC_SELECT
      WritingViewMode.EVALUATION -> viewMode = WritingViewMode.HOME
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(NavyBackground)
  ) {
    when (viewMode) {
      WritingViewMode.HOME -> {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 36.dp)
        ) {
          // Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = onBack,
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(NavyCardElevated)
                  .testTag("writing_back_btn")
              ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
              }
              Spacer(modifier = Modifier.width(12.dp))
              Text("Writing Studio", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              if (!isVerifiedPro) {
                Surface(
                  onClick = onOpenSubscription,
                  shape = RoundedCornerShape(20.dp),
                  color = NavyCardElevated,
                  border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.45f)),
                  modifier = Modifier.padding(end = 8.dp)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upgrade", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
              Surface(
                shape = RoundedCornerShape(18.dp),
                color = NavyCardElevated,
                border = BorderStroke(1.dp, NavyBorder)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(targetLang.flagEmoji, fontSize = 14.sp)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(targetLang.name, color = CyanAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text("Improve your writing, one sentence at a time.", color = TextMuted, fontSize = 14.sp)

          Spacer(modifier = Modifier.height(16.dp))

          // Progress Card
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCardBg),
            border = BorderStroke(1.dp, NavyBorder)
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  "Your Writing Progress",
                  color = TextWhite,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                )
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = PurpleAccent.copy(alpha = 0.2f)
                ) {
                  Text("Level ${userProfile.proficiencyLevel.take(2).uppercase()}", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
              }
              Spacer(modifier = Modifier.height(10.dp))
              LinearProgressIndicator(
                progress = { 0.4f },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(6.dp)
                  .clip(CircleShape),
                color = CyanAccent,
                trackColor = NavyBorder
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text("4 of 10 writing tasks completed this week", color = TextMuted, fontSize = 12.sp)
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Hero Start Writing Button Card
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { viewMode = WritingViewMode.TOPIC_SELECT },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCardBg),
            border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.45f))
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                  modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(ElectricBlue.copy(alpha = 0.25f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.Edit, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(26.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                  Text("Start Writing", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                  Spacer(modifier = Modifier.height(2.dp))
                  Text("Practice writing with your personal AI coach.", color = TextMuted, fontSize = 13.sp)
                }
              }
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(ElectricBlue),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextWhite, modifier = Modifier.size(20.dp))
              }
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          // 2-Column Practice Choices Grid
          Text("Practice Categories", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(10.dp))

          val categories = listOf(
            Triple("Daily Writing", "Short, everyday topics", Icons.Default.Today),
            Triple("Emails & Messages", "Emails, chat & formal text", Icons.Default.Email),
            Triple("Stories & Essays", "Short stories, essays & ideas", Icons.AutoMirrored.Filled.MenuBook),
            Triple("Fix My Writing", "Check and improve your text", Icons.Default.AutoFixHigh)
          )

          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            categories.chunked(2).forEach { rowItems ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                rowItems.forEach { (catTitle, catDesc, catIcon) ->
                  Card(
                    onClick = {
                      selectedCategory = catTitle
                      writingTopic = when (catTitle) {
                        "Daily Writing" -> "My Morning Routine"
                        "Emails & Messages" -> "A polite email requesting information"
                        "Stories & Essays" -> "An unforgettable vacation memory"
                        else -> "Free writing correction"
                      }
                      userTextContent = ""
                      viewMode = WritingViewMode.EDITOR
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyCardBg),
                    border = BorderStroke(1.dp, NavyBorder)
                  ) {
                    Column(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                    ) {
                      Box(
                        modifier = Modifier
                          .size(38.dp)
                          .clip(CircleShape)
                          .background(CyanAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                      ) {
                        Icon(catIcon, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                      }
                      Spacer(modifier = Modifier.height(12.dp))
                      Text(catTitle, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(catDesc, color = TextMuted, fontSize = 11.sp, maxLines = 2, lineHeight = 15.sp)
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          // Recent Writings Header & List
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              "Recent Writings",
              color = TextWhite,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { Toast.makeText(context, "$savedDraftsCount saved writing drafts", Toast.LENGTH_SHORT).show() }) {
              Text("See All", color = CyanAccent, fontSize = 13.sp)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Card(
            onClick = {
              writingTopic = "My Daily Routine"
              userTextContent = "Every morning I wake up at seven o'clock. I drink a cup of coffee and read the news."
              viewMode = WritingViewMode.EDITOR
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCardBg),
            border = BorderStroke(1.dp, NavyBorder)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(CircleShape)
                  .background(EmeraldAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(22.dp))
              }
              Spacer(modifier = Modifier.width(14.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text("My Daily Routine", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Checked • 3 improvements suggested", color = TextMuted, fontSize = 12.sp)
              }
              Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
            }
          }
        }
      }

      WritingViewMode.TOPIC_SELECT -> {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(bottom = 36.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = { viewMode = WritingViewMode.HOME },
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Choose Topic", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.height(16.dp))
          Text("Select what you'd like to write about in ${targetLang.name}.", color = TextMuted, fontSize = 14.sp)
          Spacer(modifier = Modifier.height(16.dp))

          val topics = listOf(
            "My Daily Routine",
            "Friends & Family",
            "Work & School",
            "Travel & Adventures",
            "My Opinion on Technology",
            "Surprise Me with an AI Topic"
          )

          topics.forEach { topic ->
            Card(
              onClick = {
                writingTopic = topic
                userTextContent = ""
                viewMode = WritingViewMode.EDITOR
              },
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = NavyCardBg),
              border = BorderStroke(1.dp, NavyBorder),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                ) {
                  Icon(Icons.Default.EditNote, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(22.dp))
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(topic, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
          Text("Or enter your own topic:", color = TextMuted, fontSize = 13.sp)
          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = customTopicInput,
            onValueChange = { customTopicInput = it },
            placeholder = { Text("e.g. My favorite recipe", color = TextMuted) },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = TextWhite,
              unfocusedTextColor = TextWhite,
              focusedBorderColor = CyanAccent,
              unfocusedBorderColor = NavyBorder,
              cursorColor = CyanAccent
            ),
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(12.dp))

          Button(
            onClick = {
              if (customTopicInput.isNotBlank()) {
                writingTopic = customTopicInput.trim()
                userTextContent = ""
                viewMode = WritingViewMode.EDITOR
              } else {
                Toast.makeText(context, "Please enter a topic", Toast.LENGTH_SHORT).show()
              }
            },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
          ) {
            Text("Start Writing Task", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
          }
        }
      }

      WritingViewMode.EDITOR -> {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(bottom = 36.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .weight(1f, fill = false)
                .padding(end = 8.dp)
            ) {
              IconButton(
                onClick = { viewMode = WritingViewMode.TOPIC_SELECT },
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(Color.White.copy(alpha = 0.08f))
              ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
              }
              Spacer(modifier = Modifier.width(12.dp))
              Text("AI Writing Editor", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Surface(
              shape = RoundedCornerShape(14.dp),
              color = NavyBorder
            ) {
              Text("CEFR ${userProfile.proficiencyLevel.take(2).uppercase()}", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Topic Banner
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCardBg),
            border = BorderStroke(1.dp, NavyBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text("Topic: $writingTopic", color = CyanAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(4.dp))
              Text("Write 3-5 sentences in ${targetLang.name} describing your thoughts on this topic.", color = TextLight, fontSize = 14.sp, lineHeight = 20.sp)
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Text Editor
          OutlinedTextField(
            value = userTextContent,
            onValueChange = { userTextContent = it },
            placeholder = { Text("Start typing your text in ${targetLang.name} here...", color = TextMuted) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = TextWhite,
              unfocusedTextColor = TextWhite,
              focusedBorderColor = CyanAccent,
              unfocusedBorderColor = NavyBorder,
              cursorColor = CyanAccent
            ),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 180.dp)
              .testTag("writing_text_editor")
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            val wordsCount = userTextContent.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
            Text(
              "Word count: $wordsCount words",
              color = TextMuted,
              fontSize = 12.sp,
              modifier = Modifier.weight(1f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              TextButton(onClick = {
                try {
                  speechManager.speak(userTextContent, targetLang.code)
                } catch (_: Exception) {}
              }) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Listen", color = CyanAccent, fontSize = 13.sp)
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          fun submitWritingForEvaluation() {
            if (userTextContent.isBlank()) {
              Toast.makeText(context, "Please write something before checking", Toast.LENGTH_SHORT).show()
              return
            }
            if (!isVerifiedPro) {
              AiErrorHandler.requestUpgrade(
                mode = UpgradeSheetMode.AI_UPGRADE,
                featureKey = "writing_coach",
                headline = "Unlock AI Writing Coach",
                reason = "Upgrade to LingoSphere Pro for instant grammar corrections, style coaching, and CEFR writing scores."
              )
              return
            }
            isEvaluating = true
            writingErrorMessage = null
            scope.launch {
              try {
                val report = GeminiTutorService.evaluateWritingAndGrammar(
                  userText = userTextContent,
                  writingFormat = "Paragraph",
                  targetLanguage = targetLang,
                  nativeLanguage = nativeLang,
                  proficiencyLevel = userProfile.proficiencyLevel
                )
                writingResult = report
                onAddXp(20L)
                viewMode = WritingViewMode.EVALUATION
              } catch (e: Exception) {
                writingErrorMessage = AiErrorHandler.handle(e, contextTag = "WritingEvaluation").userMessage
              } finally {
                isEvaluating = false
              }
            }
          }

          if (writingErrorMessage != null) {
            LingoCalmAiErrorBanner(
              message = writingErrorMessage,
              onRetry = { submitWritingForEvaluation() },
              onDismiss = { writingErrorMessage = null }
            )
            Spacer(modifier = Modifier.height(12.dp))
          }

          // Check My Writing Button
          Button(
            onClick = { submitWritingForEvaluation() },
            enabled = !isEvaluating && userTextContent.isNotBlank(),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("check_my_writing_btn")
          ) {
            if (isEvaluating) {
              CircularProgressIndicator(color = TextWhite, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(10.dp))
              Text("AI Coach analyzing...", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            } else {
              if (!isVerifiedPro) {
                LingoGoldLockIcon(size = 18.dp)
              } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TextWhite)
              }
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (isVerifiedPro) "Check My Writing" else "Unlock Writing Check",
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
              )
            }
          }
        }
      }

      WritingViewMode.EVALUATION -> {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(bottom = 36.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = { viewMode = WritingViewMode.HOME },
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
            ) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = TextWhite)
            }
            Text("Writing Feedback", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.size(40.dp))
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Corrected Version Card
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCardBg),
            border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.5f))
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Your Improved Writing", color = EmeraldAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
              }
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = writingResult?.correctedText.orEmpty().ifBlank { userTextContent },
                color = TextWhite,
                fontSize = 15.sp,
                lineHeight = 22.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // What You Can Improve Card
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCardBg),
            border = BorderStroke(1.dp, NavyBorder)
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("What You Can Improve", color = GoldAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
              }
              Spacer(modifier = Modifier.height(12.dp))

              val corrections = writingResult?.corrections.orEmpty()
              if (corrections.isEmpty()) {
                Text("Fantastic writing! No major grammar or spelling errors were found.", color = TextLight, fontSize = 14.sp)
              } else {
                corrections.forEach { item ->
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 6.dp)
                  ) {
                    Text("• Original: \"${item.originalSegment}\"", color = TextMuted, fontSize = 13.sp)
                    Text("  Corrected: \"${item.correctedSegment}\"", color = CyanAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("  ${item.explanationNative}", color = TextLight, fontSize = 12.sp, lineHeight = 17.sp)
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                }
              }

              if (!writingResult?.summaryFeedbackNative.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("AI Coach Note: ${writingResult?.summaryFeedbackNative}", color = TextLight, fontSize = 13.sp, lineHeight = 19.sp)
              }
            }
          }

          Spacer(modifier = Modifier.height(24.dp))

          // Actions
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            OutlinedButton(
              onClick = { viewMode = WritingViewMode.EDITOR },
              shape = RoundedCornerShape(24.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
              border = BorderStroke(1.dp, NavyBorder),
              modifier = Modifier
                .weight(1f)
                .height(50.dp)
            ) {
              Text("Try Again", fontWeight = FontWeight.Bold)
            }

            Button(
              onClick = {
                savedDraftsCount++
                Toast.makeText(context, "Writing saved successfully! 📝", Toast.LENGTH_SHORT).show()
                viewMode = WritingViewMode.HOME
              },
              shape = RoundedCornerShape(24.dp),
              colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
              modifier = Modifier
                .weight(1f)
                .height(50.dp)
            ) {
              Text("Save Writing", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
