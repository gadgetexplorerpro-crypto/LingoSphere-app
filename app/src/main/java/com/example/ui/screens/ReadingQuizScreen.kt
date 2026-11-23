package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.LingoCleanCard
import com.example.ui.components.LingoTokens
import com.example.ui.components.LingoTopBar

data class ComprehensionQuestionItem(
  val prompt: String,
  val options: List<String>,
  val correctIndex: Int,
  val explanation: String
)

@Composable
fun ReadingQuizScreen(
  itemTitle: String,
  questions: List<ComprehensionQuestionItem>,
  xpReward: Int,
  isAlreadyCompleted: Boolean,
  onQuizCompleted: (scorePercent: Int, awardedXp: Int) -> Unit,
  onDoneBackToStudio: () -> Unit,
  onBackToReader: () -> Unit
) {
  BackHandler(onBack = onBackToReader)

  var currentIndex by remember(itemTitle) { mutableIntStateOf(0) }
  val selectedOptions = remember(itemTitle) { mutableStateMapOf<Int, Int>() }
  var checkedCurrent by remember(itemTitle, currentIndex) { mutableStateOf(false) }
  var showResults by remember(itemTitle) { mutableStateOf(false) }

  val safeTotal = questions.size.coerceAtLeast(1)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(LingoTokens.bgCanvas)
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .testTag("reading_comprehension_quiz_screen")
  ) {
    LingoTopBar(
      title = if (showResults) "Quiz Results" else "Comprehension Check",
      subtitle = itemTitle,
      onBack = onBackToReader
    )

    if (!showResults && questions.isNotEmpty()) {
      val q = questions[currentIndex.coerceIn(0, questions.lastIndex)]
      val selectedOpt = selectedOptions[currentIndex]
      val progress = (currentIndex + 1).toFloat() / safeTotal.toFloat()

      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
          .fillMaxWidth()
          .height(3.dp),
        color = LingoTokens.accentPrimary,
        trackColor = LingoTokens.bgSubtle
      )

      Column(
        modifier = Modifier
          .weight(1f)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text(
          text = "Question ${currentIndex + 1} of ${questions.size}",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = LingoTokens.accentPrimary
        )

        Text(
          text = q.prompt,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = LingoTokens.textPrimary
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          q.options.forEachIndexed { optIdx, opt ->
            val isSelected = selectedOpt == optIdx
            val isCorrect = optIdx == q.correctIndex

            val containerColor = when {
              checkedCurrent && isCorrect -> LingoTokens.accentEmerald.copy(alpha = 0.16f)
              checkedCurrent && isSelected && !isCorrect -> LingoTokens.accentRose.copy(alpha = 0.16f)
              isSelected -> LingoTokens.accentPrimary.copy(alpha = 0.16f)
              else -> LingoTokens.bgElevated
            }
            val borderColor = when {
              checkedCurrent && isCorrect -> LingoTokens.accentEmerald
              checkedCurrent && isSelected && !isCorrect -> LingoTokens.accentRose
              isSelected -> LingoTokens.accentPrimary
              else -> LingoTokens.borderSubtle
            }

            Surface(
              shape = RoundedCornerShape(14.dp),
              color = containerColor,
              border = BorderStroke(1.2.dp, borderColor),
              modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !checkedCurrent) {
                  selectedOptions[currentIndex] = optIdx
                }
                .testTag("quiz_option_$optIdx")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Text(
                  text = opt,
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                  color = LingoTokens.textPrimary,
                  modifier = Modifier.weight(1f)
                )
                if (checkedCurrent && isCorrect) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Correct",
                    tint = LingoTokens.accentEmerald
                  )
                } else if (checkedCurrent && isSelected && !isCorrect) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Incorrect",
                    tint = LingoTokens.accentRose
                  )
                }
              }
            }
          }
        }

        if (checkedCurrent && q.explanation.isNotBlank()) {
          LingoCleanCard(
            containerColor = LingoTokens.bgSubtle,
            borderColor = LingoTokens.accentPrimary.copy(alpha = 0.35f)
          ) {
            Text(
              text = q.explanation,
              style = MaterialTheme.typography.bodyMedium,
              color = LingoTokens.textPrimary
            )
          }
        }
      }

      Surface(color = LingoTokens.bgCanvas) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
          if (!checkedCurrent) {
            Button(
              onClick = { checkedCurrent = true },
              enabled = selectedOpt != null,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = LingoTokens.accentPrimary,
                contentColor = LingoTokens.onAccentDark
              ),
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("check_reading_answer_btn")
            ) {
              Text("Check Answer", fontWeight = FontWeight.Bold)
            }
          } else {
            val isLast = currentIndex >= questions.lastIndex
            Button(
              onClick = {
                if (!isLast) {
                  currentIndex += 1
                  checkedCurrent = false
                } else {
                  val correctCount = questions.indices.count { idx ->
                    selectedOptions[idx] == questions[idx].correctIndex
                  }
                  val scorePct = ((correctCount * 100.0) / safeTotal).toInt()
                  val earned = if (isAlreadyCompleted) 0 else xpReward
                  onQuizCompleted(scorePct, earned)
                  showResults = true
                }
              },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = LingoTokens.accentPrimary,
                contentColor = LingoTokens.onAccentDark
              ),
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag(if (isLast) "submit_reading_quiz_button" else "next_reading_question_btn")
            ) {
              Text(if (isLast) "See Results" else "Next Question", fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.width(6.dp))
              Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
          }
        }
      }
    } else {
      val correctCount = questions.indices.count { idx ->
        selectedOptions[idx] == questions[idx].correctIndex
      }
      val scorePct = ((correctCount * 100.0) / safeTotal).toInt()
      val earned = if (isAlreadyCompleted) 0 else xpReward

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Surface(
          shape = CircleShape,
          color = LingoTokens.accentEmerald.copy(alpha = 0.16f),
          modifier = Modifier.size(80.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.EmojiEvents,
              contentDescription = null,
              tint = LingoTokens.accentEmerald,
              modifier = Modifier.size(40.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
          text = if (scorePct >= 70) "Great Comprehension!" else "Keep Practicing!",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = LingoTokens.textPrimary,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "You answered $correctCount of $safeTotal correctly ($scorePct%)",
          style = MaterialTheme.typography.bodyLarge,
          color = LingoTokens.textSecondary,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = LingoTokens.bgElevated,
          border = BorderStroke(1.dp, LingoTokens.borderSubtle)
        ) {
          Text(
            text = if (earned > 0) "+$earned XP Earned" else "Review Complete ✓",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (earned > 0) LingoTokens.accentAmber else LingoTokens.accentEmerald,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
          )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
          onClick = onDoneBackToStudio,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = LingoTokens.accentPrimary,
            contentColor = LingoTokens.onAccentDark
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("quiz_results_done_btn")
        ) {
          Text("Back to Books & Listening Studio", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
          onClick = {
            selectedOptions.clear()
            currentIndex = 0
            checkedCurrent = false
            showResults = false
          },
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, LingoTokens.borderSubtle),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp), tint = LingoTokens.textPrimary)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Retry Quiz", color = LingoTokens.textPrimary, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}
