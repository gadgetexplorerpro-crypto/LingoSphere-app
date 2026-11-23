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
import com.example.ai.GeminiTutorService
import com.example.data.model.AcademyComprehensionQuestion
import com.example.data.model.Language
import com.example.data.model.UserProfile
import com.example.speech.SpeechManager
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

private enum class GrammarViewMode {
  HOME,
  LEARN_LIST,
  LESSON_ACTIVE,
  LESSON_RESULT,
  MISTAKES_REVIEW
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarStudioScreen(
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

  var viewMode by remember { mutableStateOf(GrammarViewMode.HOME) }
  var selectedCefrLevel by remember { mutableStateOf("A2") }
  var completedLessonsCount by remember { mutableIntStateOf(6) }

  val grammarTopics = remember(targetLang.code, nativeLang.code) {
    com.example.data.repository.CurriculumData.getGrammarStudioTopics(targetLang.code, nativeLang.code)
  }

  val grammarLessons = remember(grammarTopics, targetLang.code) {
    val base = grammarTopics.map { Triple(it.title, it.ruleSummary, it.cefrLevel) }
    if (base.size >= 6) base else base + listOf(
      Triple("${targetLang.name} Past & Future Narrative", "Connect events across time in ${targetLang.name}", "A2"),
      Triple("${targetLang.name} Polite & Modal Requests", "Express ability, advice, and polite workplace requests", "B1"),
      Triple("${targetLang.name} Conditional & Advanced Nuance", "Hypothetical clauses and formal register in ${targetLang.name}", "B2")
    )
  }

  var activeLessonTitle by remember(targetLang.code) {
    mutableStateOf(grammarLessons.firstOrNull()?.first ?: "${targetLang.name} Grammar")
  }
  var currentQuestionIndex by remember { mutableIntStateOf(0) }
  var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
  var isAnswerChecked by remember { mutableStateOf(false) }
  var lessonScoreCorrect by remember { mutableIntStateOf(0) }

  val activeQuizQuestions = remember(targetLang.code, nativeLang.code) {
    com.example.data.repository.CurriculumData
      .getGrammarStudioQuizQuestions(targetLang.code, nativeLang.code)
      .map { q ->
        AcademyComprehensionQuestion(
          question = q.promptSentence,
          options = q.options,
          correctIndex = q.correctIndex,
          explanation = "${q.ruleTitle}: ${q.explanation}"
        )
      }
  }

  BackHandler {
    when (viewMode) {
      GrammarViewMode.HOME -> onBack()
      GrammarViewMode.LEARN_LIST -> viewMode = GrammarViewMode.HOME
      GrammarViewMode.LESSON_ACTIVE -> viewMode = GrammarViewMode.LEARN_LIST
      GrammarViewMode.LESSON_RESULT -> viewMode = GrammarViewMode.LEARN_LIST
      GrammarViewMode.MISTAKES_REVIEW -> viewMode = GrammarViewMode.HOME
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(NavyBackground)
  ) {
    when (viewMode) {
      GrammarViewMode.HOME -> {
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
                  .testTag("grammar_back_btn")
              ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
              }
              Spacer(modifier = Modifier.width(12.dp))
              Text("Grammar Studio", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              if (!userProfile.isEntitlementVerified) {
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
          Text("Understand grammar. Speak with confidence.", color = TextMuted, fontSize = 14.sp)

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
                Text("Your Grammar Progress", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = ElectricBlue.copy(alpha = 0.2f)
                ) {
                  Text("Level $selectedCefrLevel", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
              }
              Spacer(modifier = Modifier.height(10.dp))
              LinearProgressIndicator(
                progress = { (completedLessonsCount.toFloat() / 15f).coerceIn(0f, 1f) },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(6.dp)
                  .clip(CircleShape),
                color = CyanAccent,
                trackColor = NavyBorder
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text("$completedLessonsCount of 15 lessons completed", color = TextMuted, fontSize = 12.sp)
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Main Action: Continue Grammar
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { viewMode = GrammarViewMode.LEARN_LIST },
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
                    .background(CyanAccent.copy(alpha = 0.2f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.School, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(26.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                  Text("Continue Grammar", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                  Spacer(modifier = Modifier.height(2.dp))
                  Text("Resume your structured CEFR learning journey.", color = TextMuted, fontSize = 13.sp)
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

          Spacer(modifier = Modifier.height(16.dp))

          // Three Primary Options Cards
          val options = listOf(
            Triple("Learn Grammar", "Clear rules with real examples", Icons.AutoMirrored.Filled.MenuBook),
            Triple("Practice Grammar", "Interactive exercises & quizzes", Icons.Default.CheckCircle),
            Triple("Fix My Mistakes", "Find and correct your grammar errors", Icons.Default.AutoFixHigh)
          )

          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { (title, subtitle, icon) ->
              Card(
                onClick = {
                  when (title) {
                    "Learn Grammar" -> viewMode = GrammarViewMode.LEARN_LIST
                    "Practice Grammar" -> {
                      activeLessonTitle = "Interactive Quiz"
                      currentQuestionIndex = 0
                      selectedOptionIndex = null
                      isAnswerChecked = false
                      lessonScoreCorrect = 0
                      viewMode = GrammarViewMode.LESSON_ACTIVE
                    }
                    "Fix My Mistakes" -> viewMode = GrammarViewMode.MISTAKES_REVIEW
                  }
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
                      .background(ElectricBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(icon, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(22.dp))
                  }
                  Spacer(modifier = Modifier.width(16.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(subtitle, color = TextMuted, fontSize = 12.sp)
                  }
                  Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                }
              }
            }
          }
        }
      }

      GrammarViewMode.LEARN_LIST -> {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = { viewMode = GrammarViewMode.HOME },
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(Color.White.copy(alpha = 0.08f))
              ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
              }
              Spacer(modifier = Modifier.width(12.dp))
              Text("Grammar Journey", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // CEFR Selector Chips
          val levels = listOf("A1", "A2", "B1", "B2", "C1")
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            levels.forEach { lvl ->
              val isSelected = selectedCefrLevel == lvl
              Surface(
                onClick = { selectedCefrLevel = lvl },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) ElectricBlue else NavyCardElevated,
                border = if (isSelected) null else BorderStroke(1.dp, NavyBorder),
                modifier = Modifier.weight(1f)
              ) {
                Box(
                  modifier = Modifier.padding(vertical = 10.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(lvl, color = if (isSelected) TextWhite else TextMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          grammarLessons.forEachIndexed { idx, (title, desc, lvl) ->
            val isUnlocked = lvl == "A1" || lvl == selectedCefrLevel || idx < 3
            val isDone = idx < 2

            Card(
              onClick = {
                if (isUnlocked) {
                  activeLessonTitle = title
                  currentQuestionIndex = 0
                  selectedOptionIndex = null
                  isAnswerChecked = false
                  lessonScoreCorrect = 0
                  viewMode = GrammarViewMode.LESSON_ACTIVE
                } else {
                  Toast.makeText(context, "Complete earlier lessons to unlock", Toast.LENGTH_SHORT).show()
                }
              },
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = NavyCardBg),
              border = BorderStroke(1.dp, if (isUnlocked) NavyBorder else NavyBorder.copy(alpha = 0.5f)),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isDone) EmeraldAccent.copy(alpha = 0.2f) else ElectricBlue.copy(alpha = 0.2f)),
                  contentAlignment = Alignment.Center
                ) {
                  if (isDone) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(20.dp))
                  } else if (isUnlocked) {
                    Text("${idx + 1}", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                  } else {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                  }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(title, color = if (isUnlocked) TextWhite else TextMuted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(desc, color = TextMuted, fontSize = 12.sp, maxLines = 1)
                }
                Icon(
                  imageVector = if (isUnlocked) Icons.Default.ChevronRight else Icons.Default.Lock,
                  contentDescription = null,
                  tint = if (isUnlocked) CyanAccent else TextMuted
                )
              }
            }
          }
        }
      }

      GrammarViewMode.LESSON_ACTIVE -> {
        val currentQ = activeQuizQuestions.getOrNull(currentQuestionIndex) ?: activeQuizQuestions[0]

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
              onClick = { viewMode = GrammarViewMode.LEARN_LIST },
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
            ) {
              Icon(Icons.Default.Close, contentDescription = "Exit", tint = TextWhite)
            }
            Text(activeLessonTitle, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("${currentQuestionIndex + 1}/${activeQuizQuestions.size}", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          }

          Spacer(modifier = Modifier.height(16.dp))

          LinearProgressIndicator(
            progress = { (currentQuestionIndex + 1).toFloat() / activeQuizQuestions.size.toFloat() },
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(CircleShape),
            color = CyanAccent,
            trackColor = NavyBorder
          )

          Spacer(modifier = Modifier.height(24.dp))

          // Question Card
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCardBg),
            border = BorderStroke(1.dp, NavyBorder)
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Text("Choose the correct option:", color = TextMuted, fontSize = 13.sp)
              Spacer(modifier = Modifier.height(8.dp))
              Text(currentQ.question, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          currentQ.options.forEachIndexed { idx, opt ->
            val isSelected = selectedOptionIndex == idx
            val isCorrectOption = isAnswerChecked && idx == currentQ.correctIndex
            val isWrongSelected = isAnswerChecked && isSelected && !isCorrectOption

            val borderCol = when {
              isCorrectOption -> EmeraldAccent
              isWrongSelected -> CoralAccent
              isSelected -> CyanAccent
              else -> NavyBorder
            }

            Card(
              onClick = {
                if (!isAnswerChecked) {
                  selectedOptionIndex = idx
                }
              },
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(
                containerColor = when {
                  isCorrectOption -> Color(0xFF063324)
                  isWrongSelected -> Color(0xFF3B1219)
                  isSelected -> Color(0xFF122452)
                  else -> NavyCardBg
                }
              ),
              border = BorderStroke(if (isSelected || isCorrectOption) 1.5.dp else 1.dp, borderCol),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                RadioButton(
                  selected = isSelected,
                  onClick = { if (!isAnswerChecked) selectedOptionIndex = idx },
                  colors = RadioButtonDefaults.colors(selectedColor = CyanAccent, unselectedColor = TextMuted)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(opt, color = TextWhite, fontSize = 15.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
              }
            }
          }

          if (isAnswerChecked) {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = NavyCardBg),
              border = BorderStroke(1.dp, if (selectedOptionIndex == currentQ.correctIndex) EmeraldAccent else CoralAccent)
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = if (selectedOptionIndex == currentQ.correctIndex) "✔ Correct!" else "✖ Incorrect",
                  color = if (selectedOptionIndex == currentQ.correctIndex) EmeraldAccent else CoralAccent,
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(currentQ.explanation, color = TextLight, fontSize = 13.sp, lineHeight = 19.sp)
              }
            }
          }

          Spacer(modifier = Modifier.height(24.dp))

          Button(
            onClick = {
              if (!isAnswerChecked) {
                if (selectedOptionIndex != null) {
                  isAnswerChecked = true
                  if (selectedOptionIndex == currentQ.correctIndex) {
                    lessonScoreCorrect++
                  }
                } else {
                  Toast.makeText(context, "Please select an option", Toast.LENGTH_SHORT).show()
                }
              } else {
                if (currentQuestionIndex + 1 < activeQuizQuestions.size) {
                  currentQuestionIndex++
                  selectedOptionIndex = null
                  isAnswerChecked = false
                } else {
                  completedLessonsCount++
                  onAddXp(25L)
                  viewMode = GrammarViewMode.LESSON_RESULT
                }
              }
            },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
          ) {
            Text(if (!isAnswerChecked) "Check Answer" else if (currentQuestionIndex + 1 < activeQuizQuestions.size) "Next Question ➔" else "See Results ➔", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
          }
        }
      }

      GrammarViewMode.LESSON_RESULT -> {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(bottom = 36.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Spacer(modifier = Modifier.height(20.dp))
          Box(
            modifier = Modifier
              .size(90.dp)
              .clip(CircleShape)
              .background(Brush.linearGradient(listOf(GoldAccent, Color(0xFFD97706)))),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = TextWhite, modifier = Modifier.size(50.dp))
          }

          Spacer(modifier = Modifier.height(16.dp))
          Text("Lesson Completed!", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(4.dp))
          Text("You mastered $activeLessonTitle", color = TextMuted, fontSize = 14.sp)

          Spacer(modifier = Modifier.height(24.dp))

          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCardBg)
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text("Score", color = TextMuted, fontSize = 13.sp)
              Text("$lessonScoreCorrect / ${activeQuizQuestions.size}", color = CyanAccent, fontSize = 32.sp, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(12.dp))
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = EmeraldAccent.copy(alpha = 0.2f)
              ) {
                Text("+25 XP Earned", color = EmeraldAccent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
              }
            }
          }

          Spacer(modifier = Modifier.height(30.dp))

          Button(
            onClick = { viewMode = GrammarViewMode.LEARN_LIST },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
          ) {
            Text("Back to Grammar Journey", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
          }
        }
      }

      GrammarViewMode.MISTAKES_REVIEW -> {
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
              onClick = { viewMode = GrammarViewMode.HOME },
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Fix My Mistakes", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.height(16.dp))
          Text("Targeted grammar exercises based on your recent writing and speaking sessions.", color = TextMuted, fontSize = 14.sp)
          Spacer(modifier = Modifier.height(20.dp))

          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCardBg),
            border = BorderStroke(1.dp, NavyBorder)
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Target Area: Prepositions & Tense Agreement", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
              }
              Spacer(modifier = Modifier.height(12.dp))
              Text("You frequently confuse 'in', 'on', and 'at' when describing time and places.", color = TextLight, fontSize = 14.sp, lineHeight = 20.sp)
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          Button(
            onClick = {
              activeLessonTitle = "Mistake Recovery Drill"
              currentQuestionIndex = 0
              selectedOptionIndex = null
              isAnswerChecked = false
              lessonScoreCorrect = 0
              viewMode = GrammarViewMode.LESSON_ACTIVE
            },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
          ) {
            Text("Start Targeted Practice ➔", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
          }
        }
      }
    }
  }
}
