package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.DynamicLevelExercise
import com.example.data.repository.LearningResponseEvaluation
import com.example.data.repository.WordSearchDifficultyAndTopicEngine
import com.example.speech.SpeechManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private val StageDeepNavy = Color(0xFF071329)
private val StageCardNavy = Color(0xFF102342)
private val StageElevatedNavy = Color(0xFF162F57)
private val StageCyan = Color(0xFF14C8E8)
private val StageGreen = Color(0xFF00E676)
private val StageWordigoGreen = Color(0xFF00C853)
private val StageAmber = Color(0xFFFFB300)
private val StageCoral = Color(0xFFFF5252)

// ==================== 1. WORDIGO 6-TRY LETTER-FEEDBACK GRID ====================

private enum class WordigoTileState { EMPTY, ABSENT, PRESENT_WRONG_SPOT, CORRECT_SPOT }

private fun scoreWordigoGuess(guess: String, target: String): List<WordigoTileState> {
  val len = target.length
  val g = guess.uppercase().padEnd(len, ' ').take(len)
  val t = target.uppercase()
  val result = MutableList(len) { WordigoTileState.ABSENT }
  val remainingCounts = mutableMapOf<Char, Int>()

  for (i in 0 until len) {
    if (g[i] == t[i]) {
      result[i] = WordigoTileState.CORRECT_SPOT
    } else {
      remainingCounts[t[i]] = (remainingCounts[t[i]] ?: 0) + 1
    }
  }
  for (i in 0 until len) {
    if (result[i] != WordigoTileState.CORRECT_SPOT) {
      val ch = g[i]
      val rem = remainingCounts[ch] ?: 0
      if (rem > 0) {
        result[i] = WordigoTileState.PRESENT_WRONG_SPOT
        remainingCounts[ch] = rem - 1
      }
    }
  }
  return result
}

@Composable
fun WordigoGridMechanicStage(
  exercise: DynamicLevelExercise,
  languageCode: String,
  speechManager: SpeechManager,
  isAnswered: Boolean,
  onCompleted: (LearningResponseEvaluation) -> Unit
) {
  val secretWord = remember(exercise.id) {
    val letters = exercise.targetPhrase.trim().filter { it.isLetter() }.uppercase()
    if (letters.length in 3..7) letters else "LEARN"
  }
  val wordLen = secretWord.length
  val maxGuesses = 6

  var guesses by remember(exercise.id) { mutableStateOf<List<String>>(emptyList()) }
  var currentDraft by remember(exercise.id) { mutableStateOf("") }

  val keyStates = remember(guesses, secretWord) {
    val map = mutableMapOf<Char, WordigoTileState>()
    guesses.forEach { g ->
      val states = scoreWordigoGuess(g, secretWord)
      g.forEachIndexed { idx, ch ->
        val st = states.getOrElse(idx) { WordigoTileState.ABSENT }
        val prev = map[ch]
        if (prev != WordigoTileState.CORRECT_SPOT) {
          if (st == WordigoTileState.CORRECT_SPOT || prev == null || prev == WordigoTileState.ABSENT) {
            map[ch] = st
          }
        }
      }
    }
    map
  }

  fun submitGuess() {
    if (currentDraft.length != wordLen || isAnswered) return
    val nextGuesses = guesses + currentDraft.uppercase()
    guesses = nextGuesses
    val won = currentDraft.equals(secretWord, ignoreCase = true)
    currentDraft = ""

    if (won) {
      speechManager.speak(exercise.targetPhrase, languageCode)
      onCompleted(
        LearningResponseEvaluation(
          isPassed = true,
          accuracyScore = (100 - (nextGuesses.size - 1) * 8).coerceIn(75, 100),
          matchedWords = listOf(secretWord),
          missingWords = emptyList(),
          feedbackTitle = "Solved in ${nextGuesses.size} ${if (nextGuesses.size == 1) "Try" else "Tries"}!",
          detailedExplanation = "$secretWord — ${exercise.translation}",
          suggestedImprovement = "Strong letter deduction!"
        )
      )
    } else if (nextGuesses.size >= maxGuesses) {
      onCompleted(
        LearningResponseEvaluation(
          isPassed = false,
          accuracyScore = 40,
          matchedWords = emptyList(),
          missingWords = listOf(secretWord),
          feedbackTitle = "Secret Word: $secretWord",
          detailedExplanation = "$secretWord means \"${exercise.translation}\".",
          suggestedImprovement = "Use yellow tiles to reposition valid letters."
        )
      )
    }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("wordigo_grid_stage"),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = StageElevatedNavy,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = "Clue: ${exercise.translation}",
        color = StageCyan,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(10.dp)
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 6-row Wordigo Grid
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      for (rowIdx in 0 until maxGuesses) {
        val submitted = guesses.getOrNull(rowIdx)
        val isActiveRow = rowIdx == guesses.size && !isAnswered
        val rowScores = if (submitted != null) scoreWordigoGuess(submitted, secretWord) else null

        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
          for (colIdx in 0 until wordLen) {
            val ch = when {
              submitted != null -> submitted.getOrNull(colIdx)?.toString() ?: ""
              isActiveRow -> currentDraft.getOrNull(colIdx)?.toString() ?: ""
              else -> ""
            }
            val state = rowScores?.getOrNull(colIdx) ?: WordigoTileState.EMPTY
            val tileBg = when (state) {
              WordigoTileState.CORRECT_SPOT -> StageWordigoGreen
              WordigoTileState.PRESENT_WRONG_SPOT -> StageAmber
              WordigoTileState.ABSENT -> Color(0xFF26354D)
              WordigoTileState.EMPTY -> StageElevatedNavy
            }
            val borderCol = when {
              state != WordigoTileState.EMPTY -> tileBg
              isActiveRow && colIdx == currentDraft.length -> StageCyan
              else -> Color.White.copy(alpha = 0.2f)
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = tileBg,
              border = BorderStroke(1.5.dp, borderCol),
              modifier = Modifier.size(if (wordLen >= 6) 40.dp else 44.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = ch,
                  color = if (state == WordigoTileState.PRESENT_WRONG_SPOT || state == WordigoTileState.CORRECT_SPOT) StageDeepNavy else Color.White,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 17.sp
                )
              }
            }
          }
        }
      }
    }

    if (!isAnswered) {
      Spacer(modifier = Modifier.height(14.dp))
      val keyboardRows = remember(secretWord) {
        val base = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
        val extraChars = secretWord.filter { c -> base.none { it.contains(c) } }.toSet().joinToString("")
        if (extraChars.isEmpty()) base else listOf(extraChars) + base
      }

      Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        keyboardRows.forEach { rowChars ->
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            rowChars.forEach { keyChar ->
              val kState = keyStates[keyChar]
              val kBg = when (kState) {
                WordigoTileState.CORRECT_SPOT -> StageWordigoGreen
                WordigoTileState.PRESENT_WRONG_SPOT -> StageAmber
                WordigoTileState.ABSENT -> Color(0xFF1A2538)
                else -> StageElevatedNavy
              }
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = kBg,
                modifier = Modifier
                  .height(38.dp)
                  .widthIn(min = 28.dp)
                  .clickable {
                    if (currentDraft.length < wordLen) {
                      currentDraft += keyChar
                    }
                  }
                  .testTag("wordigo_key_$keyChar")
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                  Text(
                    text = keyChar.toString(),
                    color = if (kState == WordigoTileState.CORRECT_SPOT || kState == WordigoTileState.PRESENT_WRONG_SPOT) StageDeepNavy else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              if (currentDraft.isNotEmpty()) currentDraft = currentDraft.dropLast(1)
            },
            enabled = currentDraft.isNotEmpty(),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Delete", color = Color.White)
          }
          Button(
            onClick = { submitGuess() },
            enabled = currentDraft.length == wordLen,
            colors = ButtonDefaults.buttonColors(containerColor = StageCyan),
            modifier = Modifier
              .weight(1.4f)
              .testTag("wordigo_submit_guess_button")
          ) {
            Text("Guess (${guesses.size + 1}/$maxGuesses)", color = StageDeepNavy, fontWeight = FontWeight.ExtraBold)
          }
        }
      }
    }
  }
}

// ==================== 2. MEMORY CARD FLIP GRID (12 CARDS / 6 PAIRS) ====================

private data class MemoryDeckCard(
  val cardIndex: Int,
  val pairId: Int,
  val text: String,
  val isTargetLang: Boolean
)

@Composable
fun MemoryCardFlipMechanicStage(
  exercise: DynamicLevelExercise,
  languageCode: String,
  speechManager: SpeechManager,
  isAnswered: Boolean,
  onCompleted: (LearningResponseEvaluation) -> Unit
) {
  val scope = rememberCoroutineScope()
  val pairs = remember(exercise.id) {
    exercise.matchingPairs.ifEmpty { listOf(exercise.targetPhrase to exercise.translation) }.take(6)
  }

  val deck = remember(exercise.id) {
    val raw = mutableListOf<MemoryDeckCard>()
    pairs.forEachIndexed { pIdx, (left, right) ->
      raw.add(MemoryDeckCard(pIdx * 2, pIdx, left, true))
      raw.add(MemoryDeckCard(pIdx * 2 + 1, pIdx, right, false))
    }
    // Deterministic shuffle so cards are mixed
    val shift = (exercise.levelNumber * 5 + 3) % raw.size.coerceAtLeast(1)
    raw.indices.map { idx -> raw[(idx * 5 + shift) % raw.size] }
      .distinctBy { it.cardIndex }
      .let { if (it.size == raw.size) it else raw.shuffled() }
  }

  var matchedPairIds by remember(exercise.id) { mutableStateOf<Set<Int>>(emptySet()) }
  var flippedCardIndices by remember(exercise.id) { mutableStateOf<List<Int>>(emptyList()) }
  var movesCount by remember(exercise.id) { mutableIntStateOf(0) }
  var isBusyChecking by remember(exercise.id) { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("memory_card_flip_stage")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Matched: ${matchedPairIds.size} / ${pairs.size} pairs",
        color = StageCyan,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "Moves: $movesCount",
        color = Color.White.copy(alpha = 0.75f),
        fontSize = 12.sp
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 3 columns grid
    val cols = 3
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      for (rowStart in deck.indices step cols) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          for (c in 0 until cols) {
            val pos = rowStart + c
            if (pos < deck.size) {
              val card = deck[pos]
              val isMatched = card.pairId in matchedPairIds
              val isFlipped = pos in flippedCardIndices || isMatched

              Surface(
                modifier = Modifier
                  .weight(1f)
                  .heightIn(min = 74.dp)
                  .clickable(enabled = !isMatched && !isFlipped && !isBusyChecking && !isAnswered) {
                    if (card.isTargetLang) {
                      speechManager.speak(card.text, languageCode)
                    }
                    val nextFlipped = flippedCardIndices + pos
                    flippedCardIndices = nextFlipped
                    if (nextFlipped.size == 2) {
                      movesCount++
                      val firstCard = deck[nextFlipped[0]]
                      val secondCard = deck[nextFlipped[1]]
                      if (firstCard.pairId == secondCard.pairId) {
                        val nextMatched = matchedPairIds + firstCard.pairId
                        matchedPairIds = nextMatched
                        flippedCardIndices = emptyList()
                        if (nextMatched.size == pairs.size) {
                          onCompleted(
                            LearningResponseEvaluation(
                              isPassed = true,
                              accuracyScore = 100,
                              matchedWords = pairs.map { it.first },
                              missingWords = emptyList(),
                              feedbackTitle = "Memory Board Cleared!",
                              detailedExplanation = exercise.explanation,
                              suggestedImprovement = "Completed in $movesCount moves."
                            )
                          )
                        }
                      } else {
                        isBusyChecking = true
                        scope.launch {
                          delay(650L)
                          flippedCardIndices = emptyList()
                          isBusyChecking = false
                        }
                      }
                    }
                  }
                  .testTag("memory_card_$pos"),
                shape = RoundedCornerShape(12.dp),
                color = when {
                  isMatched -> StageGreen.copy(alpha = 0.22f)
                  isFlipped -> StageElevatedNavy
                  else -> Color(0xFF0C1E3B)
                },
                border = BorderStroke(
                  1.2.dp,
                  when {
                    isMatched -> StageGreen
                    isFlipped -> StageCyan
                    else -> StageCyan.copy(alpha = 0.3f)
                  }
                )
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier.padding(6.dp)
                ) {
                  if (isFlipped) {
                    Text(
                      text = card.text,
                      color = Color.White,
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.5.sp,
                      textAlign = TextAlign.Center,
                      maxLines = 3,
                      overflow = TextOverflow.Ellipsis
                    )
                  } else {
                    Text(
                      text = "?",
                      color = StageCyan,
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 20.sp
                    )
                  }
                }
              }
            } else {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}

// ==================== 3. SHUFFLED WORD MATCH PAIRS (NON-ALIGNED COLUMNS) ====================

@Composable
fun ShuffledWordMatchMechanicStage(
  exercise: DynamicLevelExercise,
  languageCode: String,
  speechManager: SpeechManager,
  isAnswered: Boolean,
  onCompleted: (LearningResponseEvaluation) -> Unit
) {
  val pairs = remember(exercise.id) {
    exercise.matchingPairs.ifEmpty { listOf(exercise.targetPhrase to exercise.translation) }.take(6)
  }

  // Shuffle right-hand column indices so right answers are NEVER aligned directly across!
  val rightOrder = remember(exercise.id) {
    if (pairs.size <= 1) listOf(0)
    else {
      val shifted = pairs.indices.map { i -> (i + (exercise.levelNumber % (pairs.size - 1)) + 1) % pairs.size }
      shifted
    }
  }

  var matchedPairIndices by remember(exercise.id) { mutableStateOf<Set<Int>>(emptySet()) }
  var selectedLeftIdx by remember(exercise.id) { mutableStateOf<Int?>(null) }
  var wrongFlashRightPos by remember(exercise.id) { mutableStateOf<Int?>(null) }
  var mistakesMade by remember(exercise.id) { mutableIntStateOf(0) }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("shuffled_word_match_stage"),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Text(
      text = "Tap an expression on the left, then tap its matching meaning on the right:",
      color = Color.White.copy(alpha = 0.8f),
      fontSize = 12.sp
    )

    pairs.indices.forEach { rowIdx ->
      val (leftWord, _) = pairs[rowIdx]
      val rightOriginalPairIdx = rightOrder[rowIdx]
      val rightMeaning = pairs[rightOriginalPairIdx].second

      val leftMatched = rowIdx in matchedPairIndices
      val rightMatched = rightOriginalPairIdx in matchedPairIndices
      val leftSelected = selectedLeftIdx == rowIdx
      val rightWrong = wrongFlashRightPos == rowIdx

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .clickable(enabled = !leftMatched && !isAnswered) {
              selectedLeftIdx = rowIdx
              wrongFlashRightPos = null
              speechManager.speak(leftWord, languageCode)
            }
            .testTag("match_left_$rowIdx"),
          shape = RoundedCornerShape(10.dp),
          color = when {
            leftMatched -> StageGreen.copy(alpha = 0.22f)
            leftSelected -> StageCyan.copy(alpha = 0.28f)
            else -> StageElevatedNavy
          },
          border = BorderStroke(
            1.2.dp,
            when {
              leftMatched -> StageGreen
              leftSelected -> StageCyan
              else -> Color.White.copy(alpha = 0.15f)
            }
          )
        ) {
          Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(text = leftWord, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
          }
        }

        Surface(
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .clickable(enabled = !rightMatched && selectedLeftIdx != null && !isAnswered) {
              val chosenLeft = selectedLeftIdx ?: return@clickable
              if (chosenLeft == rightOriginalPairIdx) {
                val nextMatched = matchedPairIndices + chosenLeft
                matchedPairIndices = nextMatched
                selectedLeftIdx = null
                wrongFlashRightPos = null
                if (nextMatched.size == pairs.size) {
                  val score = (100 - mistakesMade * 10).coerceIn(72, 100)
                  onCompleted(
                    LearningResponseEvaluation(
                      isPassed = true,
                      accuracyScore = score,
                      matchedWords = pairs.map { it.first },
                      missingWords = emptyList(),
                      feedbackTitle = "All ${pairs.size} Pairs Matched!",
                      detailedExplanation = exercise.explanation,
                      suggestedImprovement = "Accurate vocabulary recall!"
                    )
                  )
                }
              } else {
                mistakesMade++
                wrongFlashRightPos = rowIdx
                selectedLeftIdx = null
              }
            }
            .testTag("match_right_$rowIdx"),
          shape = RoundedCornerShape(10.dp),
          color = when {
            rightMatched -> StageGreen.copy(alpha = 0.22f)
            rightWrong -> StageCoral.copy(alpha = 0.22f)
            else -> StageElevatedNavy
          },
          border = BorderStroke(
            1.2.dp,
            when {
              rightMatched -> StageGreen
              rightWrong -> StageCoral
              else -> Color.White.copy(alpha = 0.15f)
            }
          )
        ) {
          Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(text = rightMeaning, color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
          }
        }
      }
    }
  }
}

// ==================== 4. INTERACTIVE PROGRESSIVE WORD SEARCH GRID ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveWordSearchStage(
  exercise: DynamicLevelExercise,
  languageCode: String,
  speechManager: SpeechManager,
  isAnswered: Boolean,
  onCompleted: (LearningResponseEvaluation) -> Unit
) {
  val grid = remember(exercise.id) { exercise.wordSearchGrid }
  val targets = remember(exercise.id) { exercise.wordSearchTargets }
  val wsSpec = remember(exercise.levelNumber) {
    WordSearchDifficultyAndTopicEngine.specForLevel(exercise.levelNumber)
  }

  var foundWords by remember(exercise.id) { mutableStateOf<Set<String>>(emptySet()) }
  var foundCells by remember(exercise.id) { mutableStateOf<Set<Pair<Int, Int>>>(emptySet()) }
  var startCell by remember(exercise.id) { mutableStateOf<Pair<Int, Int>?>(null) }

  fun checkLineSelection(r1: Int, c1: Int, r2: Int, c2: Int) {
    val coords = WordSearchDifficultyAndTopicEngine.resolveLineCoords(r1, c1, r2, c2)
    if (coords == null) {
      startCell = r2 to c2
      return
    }
    val selectedStr = coords.mapNotNull { (r, c) -> grid.getOrNull(r)?.getOrNull(c) }.joinToString("")
    val selectedNorm = WordSearchDifficultyAndTopicEngine.normalizeGridWord(selectedStr)
    val reversedNorm = selectedNorm.reversed()

    val matchedTarget = targets.firstOrNull { (w, _) ->
      val targetNorm = WordSearchDifficultyAndTopicEngine.normalizeGridWord(w)
      targetNorm == selectedNorm || targetNorm == reversedNorm
    }

    startCell = null
    if (matchedTarget != null && matchedTarget.first !in foundWords) {
      val updatedWords = foundWords + matchedTarget.first
      foundWords = updatedWords
      foundCells = foundCells + coords
      speechManager.speak(matchedTarget.first, languageCode)

      if (updatedWords.size >= targets.size && targets.isNotEmpty()) {
        onCompleted(
          LearningResponseEvaluation(
            isPassed = true,
            accuracyScore = 100,
            matchedWords = updatedWords.toList(),
            missingWords = emptyList(),
            feedbackTitle = "All ${targets.size} Hidden Words Found!",
            detailedExplanation = exercise.explanation,
            suggestedImprovement = "Sharp visual scanning!"
          )
        )
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("word_search_grid_stage"),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    val gridRowsCount = grid.size.coerceAtLeast(1)
    val gridColsCount = grid.firstOrNull()?.length?.coerceAtLeast(1) ?: gridRowsCount

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = RoundedCornerShape(999.dp),
        color = StageCyan.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, StageCyan.copy(alpha = 0.45f))
      ) {
        Text(
          text = "${wsSpec.tier.label} • ${gridRowsCount}×${gridColsCount}",
          color = StageCyan,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
      }
      Text(
        text = wsSpec.directionSummary,
        color = Color.White.copy(alpha = 0.72f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = "Tap the first and last letter of each hidden word (${foundWords.size}/${targets.size} found):",
      color = Color.White.copy(alpha = 0.85f),
      fontSize = 12.sp,
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Clues chips
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      targets.forEach { (wordUpper, clue) ->
        val isFound = wordUpper in foundWords
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (isFound) StageGreen.copy(alpha = 0.2f) else StageElevatedNavy,
          border = BorderStroke(1.dp, if (isFound) StageGreen else Color.White.copy(alpha = 0.14f))
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            if (isFound) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StageGreen, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
              text = if (isFound) wordUpper else "${wordUpper.first()}${"•".repeat((wordUpper.length - 1).coerceAtLeast(1))} ($clue)",
              color = if (isFound) StageGreen else Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Responsive Square Interactive Letter Grid fitting 5x5 up to 15x15 with zero horizontal cutoff
    BoxWithConstraints(
      modifier = Modifier.fillMaxWidth(),
      contentAlignment = Alignment.Center
    ) {
      val cellGap = when {
        gridColsCount >= 13 -> 2.dp
        gridColsCount >= 9 -> 3.dp
        else -> 4.dp
      }
      val totalGapWidth = cellGap * (gridColsCount - 1).coerceAtLeast(0)
      val maxAllowedCell = if (gridColsCount <= 6) 44.dp else 38.dp
      val computedCellSize = ((maxWidth - totalGapWidth) / gridColsCount.toFloat())
        .coerceIn(18.dp, maxAllowedCell)
      val cellCorner = if (gridColsCount >= 12) 4.dp else 7.dp
      val cellFontSize = when {
        gridColsCount >= 14 -> 10.sp
        gridColsCount >= 12 -> 11.sp
        gridColsCount >= 9 -> 12.5.sp
        else -> 15.sp
      }

      Column(verticalArrangement = Arrangement.spacedBy(cellGap)) {
        grid.forEachIndexed { rIdx, rowStr ->
          Row(horizontalArrangement = Arrangement.spacedBy(cellGap)) {
            rowStr.forEachIndexed { cIdx, ch ->
              val coord = rIdx to cIdx
              val isStart = startCell == coord
              val isFoundCell = coord in foundCells

              Surface(
                shape = RoundedCornerShape(cellCorner),
                color = when {
                  isStart -> StageCyan
                  isFoundCell -> StageGreen.copy(alpha = 0.3f)
                  else -> StageElevatedNavy
                },
                border = BorderStroke(
                  1.dp,
                  when {
                    isStart -> StageCyan
                    isFoundCell -> StageGreen
                    else -> Color.White.copy(alpha = 0.14f)
                  }
                ),
                modifier = Modifier
                  .size(computedCellSize)
                  .clickable(enabled = !isAnswered) {
                    val currentStart = startCell
                    if (currentStart == null) {
                      startCell = coord
                    } else if (currentStart == coord) {
                      startCell = null
                    } else {
                      checkLineSelection(currentStart.first, currentStart.second, rIdx, cIdx)
                    }
                  }
                  .testTag("ws_cell_${rIdx}_$cIdx")
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = ch.toString(),
                    color = if (isStart) StageDeepNavy else Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = cellFontSize
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

// ==================== 5. GRAMMAR REPAIR STAGE (TAP ERROR WORD OR EDIT SENTENCE) ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GrammarRepairMechanicStage(
  exercise: DynamicLevelExercise,
  languageCode: String,
  speechManager: SpeechManager,
  isAnswered: Boolean,
  onCompleted: (LearningResponseEvaluation) -> Unit
) {
  val brokenSentence = remember(exercise.id) {
    exercise.incorrectSentence.ifBlank { exercise.targetPhrase }
  }
  val brokenWords = remember(brokenSentence) {
    brokenSentence.split(Regex("\\s+")).filter { it.isNotBlank() }
  }
  var selectedWordIdx by remember(exercise.id) { mutableStateOf<Int?>(null) }
  var editedSentence by remember(exercise.id) { mutableStateOf(brokenSentence) }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("grammar_repair_stage")
  ) {
    Text(
      text = "Meaning: \"${exercise.translation}\"",
      color = StageCyan,
      fontSize = 12.5.sp,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))

    Surface(
      shape = RoundedCornerShape(14.dp),
      color = StageElevatedNavy,
      border = BorderStroke(1.dp, StageAmber.copy(alpha = 0.45f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = "1. Tap the word that needs fixing (or edit directly below):",
          color = Color.White.copy(alpha = 0.78f),
          fontSize = 11.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          brokenWords.forEachIndexed { idx, w ->
            val isSelected = selectedWordIdx == idx
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isSelected) StageAmber.copy(alpha = 0.28f) else StageCardNavy,
              border = BorderStroke(1.dp, if (isSelected) StageAmber else Color.White.copy(alpha = 0.2f)),
              modifier = Modifier.clickable(enabled = !isAnswered) {
                selectedWordIdx = idx
                val cleanToken = w.trim('.', ',', '!', '?')
                if (exercise.errorWord.isNotBlank() && cleanToken.equals(exercise.errorWord, ignoreCase = true) && exercise.correctedWord.isNotBlank()) {
                  val suffix = w.takeLastWhile { !it.isLetterOrDigit() }
                  val updatedWords = brokenWords.toMutableList()
                  updatedWords[idx] = exercise.correctedWord + suffix
                  editedSentence = updatedWords.joinToString(" ")
                }
              }
            ) {
              Text(
                text = w,
                color = if (isSelected) StageAmber else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedTextField(
      value = editedSentence,
      onValueChange = { editedSentence = it },
      enabled = !isAnswered,
      label = { Text("2. Repaired Sentence") },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("grammar_repair_input"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = StageCyan,
        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
      )
    )

    if (!isAnswered) {
      Spacer(modifier = Modifier.height(10.dp))
      Button(
        onClick = {
          val normUser = editedSentence.trim().lowercase().replace(Regex("[.,!?]"), "").replace(Regex("\\s+"), " ")
          val normTarget = exercise.targetPhrase.trim().lowercase().replace(Regex("[.,!?]"), "").replace(Regex("\\s+"), " ")
          val passed = normUser == normTarget
          if (passed) {
            speechManager.speak(exercise.targetPhrase, languageCode)
          }
          onCompleted(
            LearningResponseEvaluation(
              isPassed = passed,
              accuracyScore = if (passed) 100 else 45,
              matchedWords = if (passed) listOf(exercise.correctedWord.ifBlank { exercise.targetPhrase }) else emptyList(),
              missingWords = if (passed) emptyList() else listOf(exercise.targetPhrase),
              feedbackTitle = if (passed) "Sentence Repaired!" else "Not Fully Repaired Yet",
              detailedExplanation = if (passed) exercise.explanation else "Corrected sentence: \"${exercise.targetPhrase}\"",
              suggestedImprovement = exercise.hint
            )
          )
        },
        enabled = editedSentence.isNotBlank(),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("submit_grammar_repair_button"),
        colors = ButtonDefaults.buttonColors(containerColor = StageCyan)
      ) {
        Text("Verify Repair", color = StageDeepNavy, fontWeight = FontWeight.ExtraBold)
      }
    }
  }
}

// ==================== 6. ESCAPE ROOM 4-LOCK VAULT STAGE ====================

@Composable
fun EscapeRoomVaultHeader(
  currentStage: Int,
  totalStages: Int,
  unlockedCodes: List<String>
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Color(0xFF0B1A33),
    border = BorderStroke(1.dp, StageAmber.copy(alpha = 0.5f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("escape_room_vault_header")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(
        text = "ESCAPE VAULT LOCKS • STAGE $currentStage OF $totalStages",
        color = StageAmber,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        for (s in 1..totalStages) {
          val code = unlockedCodes.getOrNull(s - 1)
          val isUnlocked = code != null
          val isCurrent = s == currentStage && !isUnlocked
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = when {
              isUnlocked -> StageGreen.copy(alpha = 0.2f)
              isCurrent -> StageAmber.copy(alpha = 0.2f)
              else -> StageCardNavy
            },
            border = BorderStroke(
              1.dp,
              when {
                isUnlocked -> StageGreen
                isCurrent -> StageAmber
                else -> Color.White.copy(alpha = 0.12f)
              }
            ),
            modifier = Modifier.weight(1f)
          ) {
            Column(
              modifier = Modifier.padding(vertical = 8.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                contentDescription = null,
                tint = if (isUnlocked) StageGreen else if (isCurrent) StageAmber else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = code ?: "LOCK $s",
                color = if (isUnlocked) StageGreen else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
              )
            }
          }
        }
      }
    }
  }
}
