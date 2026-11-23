package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.AiCustomLesson
import com.example.data.model.TutorVoice
import com.example.data.model.VocabFlashcard
import com.example.data.repository.CustomInteractiveLessonData
import com.example.data.repository.InteractiveGamesCatalogAndAiService
import com.example.data.repository.PictureWordMode
import com.example.data.repository.PictureWordQuestion
import com.example.data.repository.SpellBeeDifficulty
import com.example.data.repository.VisualIllustrationKind
import com.example.data.repository.VocabCategoryItem
import com.example.speech.SpeechManager
import kotlinx.coroutines.launch

private val DeepNavyBg = Color(0xFF071329)
private val CardNavyBg = Color(0xFF102342)
private val ElevatedCardNavy = Color(0xFF162E54)
private val CyanAccent = Color(0xFF14C8E8)
private val PurpleAccent = Color(0xFF7C4DFF)
private val SuccessGreen = Color(0xFF2E9B4F)
private val BrightGreen = Color(0xFF00E676)
private val AmberGold = Color(0xFFFFC107)
private val SoftCoralRed = Color(0xFFFF5252)

/**
 * Renders rich, crisp visual illustrations for clothing, objects, buildings, actions, and people
 * so every Picture Word Game question and Vocabulary Category card has an accurate, high-craft visual.
 */
@Composable
fun VisualSceneAndClothingIllustration(
  kind: VisualIllustrationKind,
  modifier: Modifier = Modifier,
  height: Dp = 185.dp
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(height)
      .clip(RoundedCornerShape(20.dp))
      .background(
        when (kind) {
          VisualIllustrationKind.CROP_TOP_PORTRAIT -> Brush.verticalGradient(
            listOf(Color(0xFF1F8A96), Color(0xFF156772))
          )
          VisualIllustrationKind.COFFEE_SHOP_SCENE,
          VisualIllustrationKind.AIRPORT_TERMINAL_SCENE,
          VisualIllustrationKind.RESTAURANT_DINING_SCENE,
          VisualIllustrationKind.JOB_INTERVIEW_SCENE -> Brush.verticalGradient(
            listOf(Color(0xFF132A4A), Color(0xFF0B1A30))
          )
          else -> Brush.verticalGradient(
            listOf(Color(0xFFF8FAFC), Color(0xFFEEF2F7))
          )
        }
      )
      .border(2.dp, CyanAccent.copy(alpha = 0.45f), RoundedCornerShape(20.dp)),
    contentAlignment = Alignment.Center
  ) {
    when (kind) {
      VisualIllustrationKind.COFFEE_SHOP_SCENE -> {
        Image(
          painter = painterResource(id = R.drawable.img_listening_coffee_shop),
          contentDescription = "Coffee shop scene",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      }
      VisualIllustrationKind.AIRPORT_TERMINAL_SCENE -> {
        Image(
          painter = painterResource(id = R.drawable.img_listening_airport),
          contentDescription = "Airport scene",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      }
      VisualIllustrationKind.RESTAURANT_DINING_SCENE -> {
        Image(
          painter = painterResource(id = R.drawable.img_listening_restaurant),
          contentDescription = "Restaurant scene",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      }
      VisualIllustrationKind.JOB_INTERVIEW_SCENE -> {
        Image(
          painter = painterResource(id = R.drawable.img_listening_interview),
          contentDescription = "Interview scene",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      }
      else -> {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width
          val h = size.height
          val cx = w / 2f
          val cy = h / 2f

          when (kind) {
            VisualIllustrationKind.CROP_TOP_PORTRAIT -> {
              // Studio teal backdrop glow
              drawCircle(
                color = Color(0xFF34B3C2).copy(alpha = 0.35f),
                radius = h * 0.48f,
                center = Offset(cx, cy)
              )
              // Wavy dark-brown hair back layer
              drawRoundRect(
                color = Color(0xFF2D1B14),
                topLeft = Offset(cx - 58.dp.toPx(), cy - 68.dp.toPx()),
                size = Size(116.dp.toPx(), 112.dp.toPx()),
                cornerRadius = CornerRadius(48.dp.toPx())
              )
              // Neck & shoulders
              drawRoundRect(
                color = Color(0xFFF2C2A5),
                topLeft = Offset(cx - 14.dp.toPx(), cy - 18.dp.toPx()),
                size = Size(28.dp.toPx(), 28.dp.toPx()),
                cornerRadius = CornerRadius(10.dp.toPx())
              )
              // Arms & waist (showing crop top cut above waist)
              drawRoundRect(
                color = Color(0xFFF2C2A5),
                topLeft = Offset(cx - 64.dp.toPx(), cy + 6.dp.toPx()),
                size = Size(22.dp.toPx(), 78.dp.toPx()),
                cornerRadius = CornerRadius(11.dp.toPx())
              )
              drawRoundRect(
                color = Color(0xFFF2C2A5),
                topLeft = Offset(cx + 42.dp.toPx(), cy + 6.dp.toPx()),
                size = Size(22.dp.toPx(), 78.dp.toPx()),
                cornerRadius = CornerRadius(11.dp.toPx())
              )
              drawRoundRect(
                color = Color(0xFFE8B598),
                topLeft = Offset(cx - 38.dp.toPx(), cy + 60.dp.toPx()),
                size = Size(76.dp.toPx(), 24.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx())
              )
              // Light-blue Crop Top garment (focal point)
              val topPath = Path().apply {
                moveTo(cx - 48.dp.toPx(), cy + 4.dp.toPx())
                lineTo(cx + 48.dp.toPx(), cy + 4.dp.toPx())
                lineTo(cx + 58.dp.toPx(), cy + 26.dp.toPx())
                lineTo(cx + 40.dp.toPx(), cy + 32.dp.toPx())
                lineTo(cx + 38.dp.toPx(), cy + 60.dp.toPx())
                lineTo(cx - 38.dp.toPx(), cy + 60.dp.toPx())
                lineTo(cx - 40.dp.toPx(), cy + 32.dp.toPx())
                lineTo(cx - 58.dp.toPx(), cy + 26.dp.toPx())
                close()
              }
              drawPath(path = topPath, color = Color(0xFFB3DCE4))
              drawPath(path = topPath, color = Color(0xFF7FB8C4), style = Stroke(width = 2.dp.toPx()))
              // Smiling face
              drawCircle(
                color = Color(0xFFF5C9AE),
                radius = 28.dp.toPx(),
                center = Offset(cx, cy - 36.dp.toPx())
              )
              // Smile arc
              drawArc(
                color = Color(0xFFFFFFFF),
                startAngle = 15f,
                sweepAngle = 150f,
                useCenter = true,
                topLeft = Offset(cx - 10.dp.toPx(), cy - 34.dp.toPx()),
                size = Size(20.dp.toPx(), 11.dp.toPx())
              )
            }

            VisualIllustrationKind.SUMMER_STRIPED_TSHIRT -> {
              val shirtPath = Path().apply {
                moveTo(cx - 32.dp.toPx(), cy - 52.dp.toPx())
                lineTo(cx + 32.dp.toPx(), cy - 52.dp.toPx())
                lineTo(cx + 68.dp.toPx(), cy - 16.dp.toPx())
                lineTo(cx + 50.dp.toPx(), cy + 4.dp.toPx())
                lineTo(cx + 36.dp.toPx(), cy - 10.dp.toPx())
                lineTo(cx + 40.dp.toPx(), cy + 56.dp.toPx())
                lineTo(cx - 40.dp.toPx(), cy + 56.dp.toPx())
                lineTo(cx - 36.dp.toPx(), cy - 10.dp.toPx())
                lineTo(cx - 50.dp.toPx(), cy + 4.dp.toPx())
                lineTo(cx - 68.dp.toPx(), cy - 16.dp.toPx())
                close()
              }
              drawPath(shirtPath, color = Color(0xFFE08E1B))
              // Horizontal stripes
              for (i in -3..4) {
                val y = cy + (i * 12).dp.toPx()
                drawLine(
                  color = Color(0xFFB73225),
                  start = Offset(cx - 36.dp.toPx(), y),
                  end = Offset(cx + 36.dp.toPx(), y),
                  strokeWidth = 3.dp.toPx(),
                  cap = StrokeCap.Round
                )
              }
              // Collar cutout
              drawCircle(
                color = Color(0xFFF8FAFC),
                radius = 18.dp.toPx(),
                center = Offset(cx, cy - 52.dp.toPx())
              )
              drawPath(shirtPath, color = Color(0xFF4A2511), style = Stroke(width = 2.2.dp.toPx()))
            }

            VisualIllustrationKind.WINTER_PINK_HOODIE -> {
              // Hood
              drawCircle(
                color = Color(0xFFF8CFD2),
                radius = 24.dp.toPx(),
                center = Offset(cx, cy - 44.dp.toPx())
              )
              drawCircle(
                color = Color(0xFFE5AFB4),
                radius = 15.dp.toPx(),
                center = Offset(cx, cy - 44.dp.toPx())
              )
              // Hoodie body & long sleeves
              val hoodiePath = Path().apply {
                moveTo(cx - 28.dp.toPx(), cy - 28.dp.toPx())
                lineTo(cx + 28.dp.toPx(), cy - 28.dp.toPx())
                lineTo(cx + 62.dp.toPx(), cy + 48.dp.toPx())
                lineTo(cx + 46.dp.toPx(), cy + 54.dp.toPx())
                lineTo(cx + 32.dp.toPx(), cy + 6.dp.toPx())
                lineTo(cx + 32.dp.toPx(), cy + 58.dp.toPx())
                lineTo(cx - 32.dp.toPx(), cy + 58.dp.toPx())
                lineTo(cx - 32.dp.toPx(), cy + 6.dp.toPx())
                lineTo(cx - 46.dp.toPx(), cy + 54.dp.toPx())
                lineTo(cx - 62.dp.toPx(), cy + 48.dp.toPx())
                close()
              }
              drawPath(hoodiePath, color = Color(0xFFFAD4D8))
              drawPath(hoodiePath, color = Color(0xFF9E6B70), style = Stroke(width = 2.dp.toPx()))
              // Kangaroo pocket
              drawRoundRect(
                color = Color(0xFFF2BFC4),
                topLeft = Offset(cx - 20.dp.toPx(), cy + 26.dp.toPx()),
                size = Size(40.dp.toPx(), 22.dp.toPx()),
                cornerRadius = CornerRadius(6.dp.toPx())
              )
            }

            VisualIllustrationKind.BLUE_SNEAKERS_FOOTWEAR -> {
              // Back sneaker
              drawRoundRect(
                color = Color(0xFF2B6CB0),
                topLeft = Offset(cx - 24.dp.toPx(), cy - 32.dp.toPx()),
                size = Size(76.dp.toPx(), 38.dp.toPx()),
                cornerRadius = CornerRadius(16.dp.toPx())
              )
              // Front sneaker
              drawRoundRect(
                color = Color(0xFF3182CE),
                topLeft = Offset(cx - 56.dp.toPx(), cy - 4.dp.toPx()),
                size = Size(86.dp.toPx(), 42.dp.toPx()),
                cornerRadius = CornerRadius(18.dp.toPx())
              )
              // White rubber sole
              drawRoundRect(
                color = Color(0xFFFFFFFF),
                topLeft = Offset(cx - 56.dp.toPx(), cy + 28.dp.toPx()),
                size = Size(86.dp.toPx(), 10.dp.toPx()),
                cornerRadius = CornerRadius(5.dp.toPx())
              )
              // White shoelaces
              for (i in 0..2) {
                val lx = cx - 22.dp.toPx() + (i * 10).dp.toPx()
                drawLine(
                  color = Color.White,
                  start = Offset(lx, cy + 2.dp.toPx()),
                  end = Offset(lx + 6.dp.toPx(), cy + 14.dp.toPx()),
                  strokeWidth = 2.5.dp.toPx(),
                  cap = StrokeCap.Round
                )
              }
            }

            VisualIllustrationKind.PENDANT_NECKLACE_ACCESSORY -> {
              // Golden chain loop
              drawOval(
                color = Color(0xFFB7791F),
                topLeft = Offset(cx - 38.dp.toPx(), cy - 48.dp.toPx()),
                size = Size(76.dp.toPx(), 78.dp.toPx()),
                style = Stroke(width = 3.5.dp.toPx())
              )
              // Purple gemstone pendant
              drawOval(
                color = Color(0xFF805AD5),
                topLeft = Offset(cx - 14.dp.toPx(), cy + 24.dp.toPx()),
                size = Size(28.dp.toPx(), 36.dp.toPx())
              )
              drawOval(
                color = Color(0xFFD69E2E),
                topLeft = Offset(cx - 14.dp.toPx(), cy + 24.dp.toPx()),
                size = Size(28.dp.toPx(), 36.dp.toPx()),
                style = Stroke(width = 2.5.dp.toPx())
              )
            }

            VisualIllustrationKind.SUNGLASSES_AND_CAP,
            VisualIllustrationKind.YELLOW_SWEATER -> {
              // Cozy yellow knitted sweater
              val swPath = Path().apply {
                moveTo(cx - 34.dp.toPx(), cy - 46.dp.toPx())
                lineTo(cx + 34.dp.toPx(), cy - 46.dp.toPx())
                lineTo(cx + 58.dp.toPx(), cy + 36.dp.toPx())
                lineTo(cx + 38.dp.toPx(), cy + 42.dp.toPx())
                lineTo(cx + 32.dp.toPx(), cy - 4.dp.toPx())
                lineTo(cx + 34.dp.toPx(), cy + 46.dp.toPx())
                lineTo(cx - 34.dp.toPx(), cy + 46.dp.toPx())
                lineTo(cx - 32.dp.toPx(), cy - 4.dp.toPx())
                lineTo(cx - 38.dp.toPx(), cy + 42.dp.toPx())
                lineTo(cx - 58.dp.toPx(), cy + 36.dp.toPx())
                close()
              }
              drawPath(swPath, color = Color(0xFFF6AD1B))
              drawPath(swPath, color = Color(0xFF744210), style = Stroke(width = 2.dp.toPx()))
            }

            VisualIllustrationKind.SCHOOL_TROPHY -> {
              // Handles
              drawOval(
                color = Color(0xFFDD6B20),
                topLeft = Offset(cx - 52.dp.toPx(), cy - 42.dp.toPx()),
                size = Size(32.dp.toPx(), 44.dp.toPx()),
                style = Stroke(width = 4.dp.toPx())
              )
              drawOval(
                color = Color(0xFFDD6B20),
                topLeft = Offset(cx + 20.dp.toPx(), cy - 42.dp.toPx()),
                size = Size(32.dp.toPx(), 44.dp.toPx()),
                style = Stroke(width = 4.dp.toPx())
              )
              // Golden Cup
              drawRoundRect(
                color = Color(0xFFF6C324),
                topLeft = Offset(cx - 32.dp.toPx(), cy - 46.dp.toPx()),
                size = Size(64.dp.toPx(), 56.dp.toPx()),
                cornerRadius = CornerRadius(18.dp.toPx())
              )
              // Stem
              drawRect(
                color = Color(0xFFDD6B20),
                topLeft = Offset(cx - 8.dp.toPx(), cy + 10.dp.toPx()),
                size = Size(16.dp.toPx(), 22.dp.toPx())
              )
              // Blue Podium Base
              drawRoundRect(
                color = Color(0xFF2B6CB0),
                topLeft = Offset(cx - 28.dp.toPx(), cy + 30.dp.toPx()),
                size = Size(56.dp.toPx(), 18.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx())
              )
            }

            VisualIllustrationKind.CITY_BUILDINGS -> {
              // Left yellow building
              drawRect(
                color = Color(0xFFECC94B),
                topLeft = Offset(cx - 58.dp.toPx(), cy - 34.dp.toPx()),
                size = Size(36.dp.toPx(), 84.dp.toPx())
              )
              // Right yellow building
              drawRect(
                color = Color(0xFFECC94B),
                topLeft = Offset(cx + 22.dp.toPx(), cy - 34.dp.toPx()),
                size = Size(36.dp.toPx(), 84.dp.toPx())
              )
              // Center blue tower
              drawRect(
                color = Color(0xFF3182CE),
                topLeft = Offset(cx - 24.dp.toPx(), cy - 48.dp.toPx()),
                size = Size(48.dp.toPx(), 98.dp.toPx())
              )
              // Windows grid
              for (r in 0..2) {
                for (c in -1..0) {
                  drawRect(
                    color = Color.White,
                    topLeft = Offset(cx + (c * 16 + 4).dp.toPx(), cy - 32.dp.toPx() + (r * 18).dp.toPx()),
                    size = Size(10.dp.toPx(), 10.dp.toPx())
                  )
                }
              }
            }

            VisualIllustrationKind.BUSINESS_RUNNER_WORK -> {
              // Head
              drawCircle(
                color = Color(0xFFF2C2A5),
                radius = 12.dp.toPx(),
                center = Offset(cx + 14.dp.toPx(), cy - 42.dp.toPx())
              )
              // Suit jacket
              drawRoundRect(
                color = Color(0xFF8C7A6B),
                topLeft = Offset(cx - 14.dp.toPx(), cy - 26.dp.toPx()),
                size = Size(34.dp.toPx(), 42.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx())
              )
              // Running legs
              drawLine(
                color = Color(0xFF1A202C),
                start = Offset(cx, cy + 14.dp.toPx()),
                end = Offset(cx + 20.dp.toPx(), cy + 50.dp.toPx()),
                strokeWidth = 10.dp.toPx(),
                cap = StrokeCap.Round
              )
              drawLine(
                color = Color(0xFF1A202C),
                start = Offset(cx - 4.dp.toPx(), cy + 14.dp.toPx()),
                end = Offset(cx - 28.dp.toPx(), cy + 36.dp.toPx()),
                strokeWidth = 10.dp.toPx(),
                cap = StrokeCap.Round
              )
              // Brown Leather Briefcase
              drawRoundRect(
                color = Color(0xFF744210),
                topLeft = Offset(cx - 48.dp.toPx(), cy - 12.dp.toPx()),
                size = Size(32.dp.toPx(), 24.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx())
              )
            }

            VisualIllustrationKind.FRIENDS_PEACE_PEOPLE -> {
              // Left friend (pink top)
              drawCircle(
                color = Color(0xFFF2C2A5),
                radius = 16.dp.toPx(),
                center = Offset(cx - 18.dp.toPx(), cy - 22.dp.toPx())
              )
              drawRoundRect(
                color = Color(0xFFF687B3),
                topLeft = Offset(cx - 38.dp.toPx(), cy - 4.dp.toPx()),
                size = Size(38.dp.toPx(), 54.dp.toPx()),
                cornerRadius = CornerRadius(12.dp.toPx())
              )
              // Right friend (blue overalls & green shirt)
              drawCircle(
                color = Color(0xFFC07A55),
                radius = 17.dp.toPx(),
                center = Offset(cx + 16.dp.toPx(), cy - 28.dp.toPx())
              )
              drawRoundRect(
                color = Color(0xFF2B6CB0),
                topLeft = Offset(cx - 2.dp.toPx(), cy - 8.dp.toPx()),
                size = Size(40.dp.toPx(), 58.dp.toPx()),
                cornerRadius = CornerRadius(12.dp.toPx())
              )
            }

            else -> {}
          }
        }
      }
    }
  }
}

/**
 * High-craft Picture Word Game ("Picture Quiz") with 7 Game Modes, visual scene/clothing cards,
 * Hint button, green selection pills, audio pronunciation, and AI question expansion.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PictureWordGameView(
  languageName: String,
  languageCode: String,
  cefrLevel: String,
  speechManager: SpeechManager,
  onSaveFlashcard: (VocabFlashcard) -> Unit,
  onCompleteGame: (String, Int, Int) -> Unit,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)
  val scope = rememberCoroutineScope()
  var selectedMode by remember { mutableStateOf<PictureWordMode?>(null) }
  var questions by remember(languageCode, selectedMode) {
    mutableStateOf(InteractiveGamesCatalogAndAiService.getPictureWordQuestions(languageCode, selectedMode))
  }
  var currentIndex by remember(questions) { mutableIntStateOf(0) }
  var selectedOptionId by remember(currentIndex, questions) { mutableStateOf<String?>(null) }
  var showHint by remember(currentIndex, questions) { mutableStateOf(false) }
  var sessionScore by remember { mutableIntStateOf(0) }
  var isGeneratingAiBatch by remember { mutableStateOf(false) }
  var savedWordNotice by remember { mutableStateOf<String?>(null) }

  val currentQuestion: PictureWordQuestion? = questions.getOrNull(currentIndex)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DeepNavyBg)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag("picture_word_game_screen")
  ) {
    // Top Progress & Close Header (inspired by reference screenshot 1)
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      LinearProgressIndicator(
        progress = {
          if (questions.isEmpty()) 0f else ((currentIndex + 1).toFloat() / questions.size.toFloat()).coerceIn(0.08f, 1f)
        },
        modifier = Modifier
          .weight(1f)
          .height(8.dp)
          .clip(RoundedCornerShape(999.dp)),
        color = PurpleAccent,
        trackColor = Color.White.copy(alpha = 0.14f)
      )
      Spacer(modifier = Modifier.width(12.dp))
      IconButton(
        onClick = onBack,
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(CardNavyBg)
          .testTag("picture_word_close_button")
      ) {
        Icon(Icons.Default.Close, contentDescription = "Close Picture Word Game", tint = Color.White)
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 7 Modes Horizontal Selector
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(vertical = 4.dp)
    ) {
      item {
        FilterChip(
          selected = selectedMode == null,
          onClick = { selectedMode = null },
          label = { Text("All 7 Modes", fontSize = 12.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CyanAccent.copy(alpha = 0.22f),
            selectedLabelColor = CyanAccent,
            containerColor = CardNavyBg,
            labelColor = Color.White.copy(alpha = 0.85f)
          )
        )
      }
      items(PictureWordMode.entries) { mode ->
        FilterChip(
          selected = selectedMode == mode,
          onClick = { selectedMode = mode },
          label = { Text(mode.label, fontSize = 12.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CyanAccent.copy(alpha = 0.22f),
            selectedLabelColor = CyanAccent,
            containerColor = CardNavyBg,
            labelColor = Color.White.copy(alpha = 0.85f)
          )
        )
      }
    }

    if (currentQuestion != null) {
      Spacer(modifier = Modifier.height(8.dp))

      // Category & Score Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = currentQuestion.categoryTitle,
          style = MaterialTheme.typography.labelLarge,
          color = CyanAccent,
          fontWeight = FontWeight.Bold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Star, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "$sessionScore XP",
            color = AmberGold,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Verified Visual Concept Card (falls back to scene illustration only if no concept ID is set)
      if (currentQuestion.visualConceptId.isNotBlank()) {
        VerifiedConceptVisualCard(
          visualConceptId = currentQuestion.visualConceptId,
          targetPhrase = currentQuestion.targetWord,
          englishMeaning = currentQuestion.targetTranslation,
          height = 196.dp
        )
      } else {
        VisualSceneAndClothingIllustration(
          kind = currentQuestion.illustrationKind,
          height = 196.dp
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // "Select:" prompt + "Hint" pill button (matching Screenshot 1 layout)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = currentQuestion.promptLabel,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFB39DDB),
            fontWeight = FontWeight.ExtraBold
          )
          Text(
            text = currentQuestion.mode.instruction,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
          )
        }
        Surface(
          shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 12.dp, bottomEnd = 12.dp),
          color = PurpleAccent,
          modifier = Modifier
            .clickable { showHint = !showHint }
            .testTag("picture_word_hint_button")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Hint", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Default.Visibility, contentDescription = "Show hint", tint = Color.White, modifier = Modifier.size(16.dp))
          }
        }
      }

      AnimatedVisibility(visible = showHint, enter = fadeIn(), exit = fadeOut()) {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
          colors = CardDefaults.cardColors(containerColor = ElevatedCardNavy),
          border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = AmberGold)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "${currentQuestion.hintText} (${currentQuestion.targetTranslation})",
              color = Color.White,
              style = MaterialTheme.typography.bodySmall
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // LingoPanda-style 2x2 Horizontal Answer Grid with stable answer IDs
      val resolvedOptions = remember(currentQuestion.id, currentQuestion.options) {
        currentQuestion.resolvedAnswerOptions()
      }
      val resolvedCorrectId = remember(currentQuestion.id, currentQuestion.options) {
        currentQuestion.resolvedCorrectOptionId()
      }

      LingoPandaHorizontalAnswerGrid(
        options = resolvedOptions,
        correctOptionId = resolvedCorrectId,
        selectedOptionId = selectedOptionId,
        tagPrefix = "picture_word_option",
        onSelectOption = { opt ->
          selectedOptionId = opt.id
          speechManager.speak(opt.text, languageCode)
          if (opt.id == resolvedCorrectId) {
            sessionScore += 10
          }
        }
      )

      // Answer Feedback Card + Pronunciation + Example Sentence
      if (selectedOptionId != null) {
        val isCorrect = selectedOptionId == resolvedCorrectId
        Spacer(modifier = Modifier.height(16.dp))
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("picture_word_feedback_card"),
          colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) Color(0xFF0E3B2E) else Color(0xFF3E1826)
          ),
          border = BorderStroke(
            1.5.dp,
            if (isCorrect) BrightGreen else SoftCoralRed
          ),
          shape = RoundedCornerShape(16.dp)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Refresh,
                  contentDescription = null,
                  tint = if (isCorrect) BrightGreen else AmberGold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = if (isCorrect) "Correct! +10 XP" else "Not quite — Try again or review below",
                  color = Color.White,
                  fontWeight = FontWeight.Bold
                )
              }
              Row {
                IconButton(
                  onClick = { speechManager.speak(currentQuestion.targetWord, languageCode) }
                ) {
                  Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen pronunciation", tint = CyanAccent)
                }
                IconButton(
                  onClick = {
                    onSaveFlashcard(
                      VocabFlashcard(
                        id = "pw_${currentQuestion.id}",
                        word = currentQuestion.targetWord,
                        translation = currentQuestion.targetTranslation,
                        phonetic = currentQuestion.phonetic,
                        partOfSpeech = "Visual Vocabulary",
                        cefrLevel = cefrLevel,
                        category = currentQuestion.categoryTitle,
                        exampleSentence = currentQuestion.exampleSentence,
                        exampleTranslation = currentQuestion.exampleTranslation
                      )
                    )
                    savedWordNotice = "Saved '${currentQuestion.targetWord}' to your vocabulary deck"
                  }
                ) {
                  Icon(Icons.Default.BookmarkBorder, contentDescription = "Save word", tint = AmberGold)
                }
              }
            }

            Text(
              text = "${currentQuestion.targetWord} ${currentQuestion.phonetic} — ${currentQuestion.targetTranslation}",
              color = CyanAccent,
              fontWeight = FontWeight.SemiBold,
              fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = currentQuestion.explanation,
              color = Color.White.copy(alpha = 0.9f),
              style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Example: \"${currentQuestion.exampleSentence}\"",
              color = Color.White,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium
            )
            if (!savedWordNotice.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(text = savedWordNotice!!, color = BrightGreen, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              if (!isCorrect) {
                OutlinedButton(
                  onClick = { selectedOptionId = null },
                  modifier = Modifier.weight(1f),
                  border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                ) {
                  Text("Retry Question", color = Color.White)
                }
              }
              Button(
                onClick = {
                  savedWordNotice = null
                  if (currentIndex + 1 < questions.size) {
                    currentIndex++
                  } else {
                    onCompleteGame("picture_word_game", 100, (sessionScore.coerceAtLeast(35)))
                    currentIndex = 0
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                modifier = Modifier
                  .weight(1f)
                  .testTag("picture_word_next_button")
              ) {
                Text(
                  text = if (currentIndex + 1 < questions.size) "Next Picture" else "Finish & Claim XP",
                  color = DeepNavyBg,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Load fresh AI-generated visual questions
      OutlinedButton(
        onClick = {
          if (!isGeneratingAiBatch) {
            isGeneratingAiBatch = true
            scope.launch {
              val newBatch = InteractiveGamesCatalogAndAiService.generateAiPictureWordBatch(
                languageName = languageName,
                languageCode = languageCode,
                cefrLevel = cefrLevel,
                categoryFocus = selectedMode?.label ?: "Everyday Scenes & Clothing"
              )
              questions = newBatch
              currentIndex = 0
              isGeneratingAiBatch = false
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.45f))
      ) {
        if (isGeneratingAiBatch) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CyanAccent)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Preparing New Visual Questions...", color = CyanAccent)
        } else {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Load New Challenge Questions", color = CyanAccent)
        }
      }
    }
  }
}

/**
 * SpellBee Game: Speaks words aloud via SpeechManager and lets learners spell them letter-by-letter.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpellBeeGameView(
  languageName: String,
  languageCode: String,
  speechManager: SpeechManager,
  onCompleteGame: (String, Int, Int) -> Unit,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)
  var difficulty by remember { mutableStateOf(SpellBeeDifficulty.BEGINNER) }
  val words = remember(languageCode, difficulty) {
    InteractiveGamesCatalogAndAiService.getSpellBeeWords(languageCode, difficulty)
  }
  var wordIndex by remember(words) { mutableIntStateOf(0) }
  val current = words.getOrNull(wordIndex) ?: words.first()

  var typedLetters by remember(current) { mutableStateOf("") }
  var showHint by remember(current) { mutableStateOf(false) }
  var streak by remember { mutableIntStateOf(0) }
  var totalPoints by remember { mutableIntStateOf(0) }
  var isSubmitted by remember(current) { mutableStateOf(false) }

  val isCorrect = typedLetters.equals(current.word, ignoreCase = true)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DeepNavyBg)
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
      .testTag("spellbee_game_screen")
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Column {
          Text("SpellBee", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
          Text("Listen & spell letter by letter • $languageName", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
        }
      }
      Surface(
        shape = RoundedCornerShape(999.dp),
        color = AmberGold.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, AmberGold)
      ) {
        Text(
          text = "Streak $streak • $totalPoints pts",
          color = AmberGold,
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Difficulty Chips
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      SpellBeeDifficulty.entries.forEach { diff ->
        FilterChip(
          selected = difficulty == diff,
          onClick = { difficulty = diff },
          label = { Text(diff.label) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AmberGold.copy(alpha = 0.25f),
            selectedLabelColor = AmberGold,
            containerColor = CardNavyBg,
            labelColor = Color.White
          )
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Audio Speaker Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = CardNavyBg),
      border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.45f)),
      shape = RoundedCornerShape(20.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Word ${wordIndex + 1} of ${words.size}",
          color = AmberGold,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Button(
            onClick = { speechManager.speak(current.word, languageCode, 0.95f) },
            colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
            modifier = Modifier.testTag("spellbee_play_audio_button")
          ) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = DeepNavyBg)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Hear Word", color = DeepNavyBg, fontWeight = FontWeight.Bold)
          }
          OutlinedButton(
            onClick = { speechManager.speakWithRate(current.word, languageCode, 0.70f) },
            border = BorderStroke(1.dp, CyanAccent)
          ) {
            Icon(Icons.Default.SlowMotionVideo, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Slow", color = CyanAccent)
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Meaning: ${current.translation}",
          color = Color.White,
          fontWeight = FontWeight.SemiBold
        )
        TextButtonOrHint(
          showHint = showHint,
          hintText = "${current.hint} (Starts with '${current.word.first()}')",
          onToggle = { showHint = !showHint }
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Letter-by-letter slots with live accuracy colors
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      current.word.forEachIndexed { idx, expectedChar ->
        val actualChar = typedLetters.getOrNull(idx)
        val slotMatches = actualChar != null && actualChar.equals(expectedChar, ignoreCase = true)
        val slotColor = when {
          actualChar == null -> CardNavyBg
          slotMatches -> SuccessGreen
          else -> SoftCoralRed
        }
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(slotColor)
            .border(1.5.dp, if (actualChar != null) Color.White else CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = actualChar?.toString() ?: "_",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Interactive Letter Pool
    Text("Tap letters to spell:", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
    Spacer(modifier = Modifier.height(8.dp))
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      current.scrambledPool.forEachIndexed { idx, letter ->
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = ElevatedCardNavy,
          border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
          modifier = Modifier
            .clickable {
              if (typedLetters.length < current.word.length) {
                val updated = typedLetters + letter
                typedLetters = updated
                if (updated.length == current.word.length) {
                  isSubmitted = true
                  if (updated.equals(current.word, ignoreCase = true)) {
                    streak++
                    totalPoints += difficulty.xpPerWord
                    speechManager.speak(current.word, languageCode)
                  } else {
                    streak = 0
                  }
                }
              }
            }
            .testTag("spellbee_letter_$idx")
        ) {
          Text(
            text = letter,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
      OutlinedButton(
        onClick = {
          if (typedLetters.isNotEmpty()) {
            typedLetters = typedLetters.dropLast(1)
            isSubmitted = false
          }
        },
        modifier = Modifier.weight(1f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
      ) {
        Text("Backspace", color = Color.White)
      }
      OutlinedButton(
        onClick = {
          typedLetters = ""
          isSubmitted = false
        },
        modifier = Modifier.weight(1f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
      ) {
        Text("Retry Word", color = Color.White)
      }
    }

    if (isSubmitted) {
      Spacer(modifier = Modifier.height(14.dp))
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = if (isCorrect) Color(0xFF0E3B2E) else Color(0xFF3E1826)
        ),
        border = BorderStroke(1.dp, if (isCorrect) BrightGreen else SoftCoralRed)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = if (isCorrect) "Perfect Spelling! +${difficulty.xpPerWord} XP" else "Correct spelling: ${current.word}",
            color = Color.White,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = current.exampleSentence, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = {
              if (wordIndex + 1 < words.size) {
                wordIndex++
              } else {
                onCompleteGame("spell_bee", 100, totalPoints.coerceAtLeast(35))
                wordIndex = 0
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = if (wordIndex + 1 < words.size) "Next Word" else "Complete SpellBee & Claim XP",
              color = DeepNavyBg,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TextButtonOrHint(showHint: Boolean, hintText: String, onToggle: () -> Unit) {
  Spacer(modifier = Modifier.height(6.dp))
  Surface(
    shape = RoundedCornerShape(999.dp),
    color = ElevatedCardNavy,
    modifier = Modifier.clickable(onClick = onToggle)
  ) {
    Text(
      text = if (showHint) "Hint: $hintText" else "Need a Hint? Tap here",
      color = CyanAccent,
      fontSize = 12.sp,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    )
  }
}

/**
 * Wordigo Game: Guess the 5-letter word in 6 attempts with Green/Yellow/Gray feedback and keyboard.
 */
@Composable
fun WordigoGameView(
  languageName: String,
  languageCode: String,
  speechManager: SpeechManager,
  onCompleteGame: (String, Int, Int) -> Unit,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)
  val puzzles = remember(languageCode) {
    InteractiveGamesCatalogAndAiService.getWordigoPuzzles(languageCode)
  }
  var isDailyMode by remember { mutableStateOf(true) }
  var puzzleIndex by remember { mutableIntStateOf(0) }
  val activePuzzle = if (isDailyMode) puzzles.first() else puzzles[puzzleIndex % puzzles.size]
  val wordLength = activePuzzle.targetWord.length.coerceAtLeast(5)

  val guesses = remember(activePuzzle) { mutableStateListOf<String>() }
  var currentDraft by remember(activePuzzle) { mutableStateOf("") }

  val hasWon = guesses.any { it.equals(activePuzzle.targetWord, ignoreCase = true) }
  val isGameOver = hasWon || guesses.size >= 6

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DeepNavyBg)
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
      .testTag("wordigo_game_screen")
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Column {
          Text("Wordigo", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
          Text("Guess the $wordLength-letter word in 6 tries", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
          selected = isDailyMode,
          onClick = { isDailyMode = true },
          label = { Text("Daily", fontSize = 12.sp) }
        )
        FilterChip(
          selected = !isDailyMode,
          onClick = {
            isDailyMode = false
            puzzleIndex = (puzzleIndex + 1) % puzzles.size
          },
          label = { Text("Practice", fontSize = 12.sp) }
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = "Clue: ${activePuzzle.definition} (${activePuzzle.difficulty})",
      color = CyanAccent,
      fontSize = 13.sp,
      fontWeight = FontWeight.Medium
    )

    Spacer(modifier = Modifier.height(12.dp))

    // 6-Attempt Letter Grid
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      for (row in 0 until 6) {
        val committedWord = guesses.getOrNull(row)
        val isCurrentRow = row == guesses.size && !isGameOver
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          for (col in 0 until wordLength) {
            val ch: Char? = when {
              committedWord != null -> committedWord.getOrNull(col)
              isCurrentRow -> currentDraft.getOrNull(col)
              else -> null
            }
            val targetChar = activePuzzle.targetWord.getOrNull(col)
            val boxColor = when {
              committedWord == null -> CardNavyBg
              ch != null && targetChar != null && ch.equals(targetChar, ignoreCase = true) -> SuccessGreen
              ch != null && activePuzzle.targetWord.contains(ch, ignoreCase = true) -> Color(0xFFD99B00)
              else -> Color(0xFF2D3748)
            }
            Box(
              modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(boxColor)
                .border(
                  width = 1.5.dp,
                  color = if (isCurrentRow && ch != null) CyanAccent else Color.White.copy(alpha = 0.22f),
                  shape = RoundedCornerShape(8.dp)
                ),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = ch?.toString() ?: "",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // On-screen Keyboard
    val rows = listOf(
      "QWERTYUIOP",
      "ASDFGHJKL",
      "ZXCVBNM"
    )
    rows.forEach { rowChars ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
      ) {
        rowChars.forEach { keyChar ->
          val keyStr = keyChar.toString()
          val isGreen = guesses.any { g ->
            g.indices.any { i ->
              g.getOrNull(i)?.equals(keyChar, ignoreCase = true) == true &&
                activePuzzle.targetWord.getOrNull(i)?.equals(keyChar, ignoreCase = true) == true
            }
          }
          val isYellow = !isGreen && guesses.any { g ->
            g.contains(keyChar, ignoreCase = true) && activePuzzle.targetWord.contains(keyChar, ignoreCase = true)
          }
          val isGray = !isGreen && !isYellow && guesses.any { g -> g.contains(keyChar, ignoreCase = true) }

          val keyBg = when {
            isGreen -> SuccessGreen
            isYellow -> Color(0xFFD99B00)
            isGray -> Color(0xFF1A2436)
            else -> ElevatedCardNavy
          }

          Surface(
            shape = RoundedCornerShape(6.dp),
            color = keyBg,
            modifier = Modifier
              .height(40.dp)
              .width(30.dp)
              .clickable(enabled = !isGameOver) {
                if (currentDraft.length < wordLength) {
                  currentDraft += keyStr
                }
              }
              .testTag("wordigo_key_$keyStr")
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(keyStr, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
      OutlinedButton(
        onClick = {
          if (currentDraft.isNotEmpty()) currentDraft = currentDraft.dropLast(1)
        },
        enabled = !isGameOver && currentDraft.isNotEmpty(),
        modifier = Modifier.weight(1f)
      ) {
        Text("Delete", color = Color.White)
      }
      Button(
        onClick = {
          if (currentDraft.length == wordLength && !isGameOver) {
            guesses.add(currentDraft.uppercase())
            if (currentDraft.equals(activePuzzle.targetWord, ignoreCase = true)) {
              speechManager.speak(activePuzzle.targetWord, languageCode)
              onCompleteGame("wordigo_${activePuzzle.id}", 100, 40)
            }
            currentDraft = ""
          }
        },
        enabled = !isGameOver && currentDraft.length == wordLength,
        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
        modifier = Modifier
          .weight(1f)
          .testTag("wordigo_submit_button")
      ) {
        Text("Submit Guess", color = DeepNavyBg, fontWeight = FontWeight.Bold)
      }
    }

    if (isGameOver) {
      Spacer(modifier = Modifier.height(14.dp))
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardNavyBg),
        border = BorderStroke(1.5.dp, if (hasWon) BrightGreen else AmberGold)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (hasWon) "Solved in ${guesses.size} tries! +40 XP" else "Answer: ${activePuzzle.targetWord}",
              color = if (hasWon) BrightGreen else AmberGold,
              fontWeight = FontWeight.ExtraBold
            )
            IconButton(onClick = { speechManager.speak(activePuzzle.targetWord, languageCode) }) {
              Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Hear word", tint = CyanAccent)
            }
          }
          Text(
            text = "${activePuzzle.targetWord} ${activePuzzle.phonetic} — ${activePuzzle.translation}",
            color = Color.White,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Example: \"${activePuzzle.exampleSentence}\"",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp
          )
          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = {
              isDailyMode = false
              puzzleIndex++
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Play Next Wordigo Puzzle", color = DeepNavyBg, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

/**
 * Combined 4-stage Daily Language Challenge:
 * 1) Learn 5 useful words
 * 2) Listen to 2 short conversations
 * 3) Speak 3 sentences
 * 4) Complete 1 mini quiz
 */
@Composable
fun DailyLanguageChallengeView(
  languageName: String,
  languageCode: String,
  speechManager: SpeechManager,
  isAlreadyCompletedToday: Boolean,
  onCompleteDailyChallenge: () -> Unit,
  onBack: () -> Unit,
  userProfile: com.example.data.model.UserProfile? = null,
  onGrantBonusXp: (Int) -> Unit = {}
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current
  val isListening by speechManager.isListening.collectAsState()
  val spokenText by speechManager.spokenText.collectAsState()

  val micPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) speechManager.startListening(languageCode)
  }

  var currentStage by remember { mutableIntStateOf(0) } // 0: 5 Words, 1: 2 Dialogues, 2: 3 Spoken Sentences, 3: Mini Quiz
  val reviewedWordIndices = remember { mutableStateListOf<Int>() }
  val listenedDialogues = remember { mutableStateListOf<Int>() }
  val spokenSentencesDone = remember { mutableStateListOf<Int>() }
  var quizSelectedOptionId by remember { mutableStateOf<String?>(null) }
  var completedBanner by remember { mutableStateOf(isAlreadyCompletedToday) }
  var dailyDoubleXpClaimed by remember { mutableStateOf(false) }
  var dailyRewardMessage by remember { mutableStateOf<String?>(null) }

  val dailyWords = remember(languageCode) {
    InteractiveGamesCatalogAndAiService.getPictureWordQuestions(languageCode).take(5)
  }
  val dialogues = remember {
    listOf(
      "Good morning! Could I get an iced coffee and a croissant, please?" to "At the neighborhood café counter",
      "Excuse me, which platform does the express train to the airport depart from?" to "At the central train station"
    )
  }
  val speakingLines = remember(dailyWords) {
    dailyWords.take(3).map { it.exampleSentence }
  }

  var lastConsumedDailySpeech by remember(currentStage) {
    mutableStateOf(speechManager.spokenText.value)
  }

  LaunchedEffect(currentStage) {
    if (currentStage == 2) {
      speechManager.clearSessionSpeechState()
      lastConsumedDailySpeech = ""
    }
  }

  LaunchedEffect(spokenText, currentStage) {
    if (spokenText.isNotBlank() && spokenText != lastConsumedDailySpeech && currentStage == 2) {
      lastConsumedDailySpeech = spokenText
      if (spokenSentencesDone.size < 3 && !spokenSentencesDone.contains(spokenSentencesDone.size)) {
        spokenSentencesDone.add(spokenSentencesDone.size)
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DeepNavyBg)
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
      .testTag("daily_language_challenge_screen")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Column {
          Text("Daily Language Challenge", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
          Text("4-step daily fluency builder • +50 XP", style = MaterialTheme.typography.bodySmall, color = AmberGold)
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Stage indicator pills
    val stageLabels = listOf("1. 5 Words", "2. 2 Dialogues", "3. Speak 3x", "4. Mini Quiz")
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      stageLabels.forEachIndexed { idx, label ->
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (currentStage == idx) CyanAccent else CardNavyBg,
          modifier = Modifier
            .weight(1f)
            .clickable { currentStage = idx }
        ) {
          Text(
            text = label,
            color = if (currentStage == idx) DeepNavyBg else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    when (currentStage) {
      0 -> {
        Text("Step 1: Tap to listen & learn 5 useful words (${reviewedWordIndices.size}/5)", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        dailyWords.forEachIndexed { idx, item ->
          val isDone = reviewedWordIndices.contains(idx)
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .clickable {
                if (!reviewedWordIndices.contains(idx)) reviewedWordIndices.add(idx)
                speechManager.speak(item.targetWord, languageCode)
              },
            colors = CardDefaults.cardColors(containerColor = CardNavyBg),
            border = BorderStroke(1.dp, if (isDone) BrightGreen else Color.White.copy(alpha = 0.12f))
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("${item.targetWord} ${item.phonetic}", color = CyanAccent, fontWeight = FontWeight.Bold)
                Text(item.targetTranslation, color = Color.White, fontSize = 13.sp)
                Text(item.exampleSentence, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
              }
              Icon(
                imageVector = if (isDone) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = if (isDone) BrightGreen else CyanAccent
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
          onClick = { currentStage = 1 },
          colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Continue to 2 Short Conversations", color = DeepNavyBg, fontWeight = FontWeight.Bold)
        }
      }

      1 -> {
        Text("Step 2: Listen to 2 short conversations (${listenedDialogues.size}/2)", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        dialogues.forEachIndexed { idx, (line, contextLabel) ->
          val isDone = listenedDialogues.contains(idx)
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = CardNavyBg),
            border = BorderStroke(1.dp, if (isDone) BrightGreen else CyanAccent.copy(alpha = 0.35f))
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(contextLabel, color = AmberGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(4.dp))
              Text("\"$line\"", color = Color.White, fontWeight = FontWeight.SemiBold)
              Spacer(modifier = Modifier.height(8.dp))
              Button(
                onClick = {
                  if (!listenedDialogues.contains(idx)) listenedDialogues.add(idx)
                  speechManager.speak(line, languageCode)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElevatedCardNavy)
              ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = CyanAccent)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isDone) "Played ✓ (Listen Again)" else "Play Conversation", color = CyanAccent)
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
          onClick = { currentStage = 2 },
          colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Continue to Speak 3 Sentences", color = DeepNavyBg, fontWeight = FontWeight.Bold)
        }
      }

      2 -> {
        Text("Step 3: Speak 3 sentences aloud (${spokenSentencesDone.size}/3)", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        speakingLines.forEachIndexed { idx, sentence ->
          val isDone = spokenSentencesDone.contains(idx)
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = CardNavyBg),
            border = BorderStroke(1.dp, if (isDone) BrightGreen else PurpleAccent.copy(alpha = 0.45f))
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text("Sentence ${idx + 1}", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Text(sentence, color = Color.White, fontWeight = FontWeight.SemiBold)
              Spacer(modifier = Modifier.height(8.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { speechManager.speak(sentence, languageCode) }) {
                  Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Listen", color = CyanAccent)
                }
                Button(
                  onClick = {
                    val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                      if (isListening) speechManager.finishListeningNow() else speechManager.startListening(languageCode)
                    } else {
                      micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    if (!spokenSentencesDone.contains(idx)) spokenSentencesDone.add(idx)
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = if (isDone) SuccessGreen else PurpleAccent)
                ) {
                  Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(if (isDone) "Spoken ✓" else "Speak Now", color = Color.White)
                }
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
          onClick = { currentStage = 3 },
          colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Continue to Final Mini Quiz", color = DeepNavyBg, fontWeight = FontWeight.Bold)
        }
      }

      else -> {
        val targetQ = dailyWords.first()
        Text("Step 4: Complete 1 Mini Quiz", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = CardNavyBg)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("Which word matches '${targetQ.targetTranslation}'?", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            LingoPandaHorizontalAnswerGrid(
              options = targetQ.resolvedAnswerOptions(),
              correctOptionId = targetQ.resolvedCorrectOptionId(),
              selectedOptionId = quizSelectedOptionId,
              tagPrefix = "daily_quiz_option",
              onSelectOption = { opt -> quizSelectedOptionId = opt.id }
            )

            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = {
                completedBanner = true
                onCompleteDailyChallenge()
              },
              enabled = quizSelectedOptionId != null,
              colors = ButtonDefaults.buttonColors(containerColor = BrightGreen),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("complete_daily_challenge_button")
            ) {
              Text(
                text = if (completedBanner) "Daily Challenge Completed ✓" else "Claim +50 XP & Update Daily Streak",
                color = DeepNavyBg,
                fontWeight = FontWeight.ExtraBold
              )
            }

            // Daily Challenge: no ads before it starts or during stages.
            // One optional "Watch an ad for double XP" rewarded ad after it is completed (Free users only).
            if (completedBanner && !dailyDoubleXpClaimed && com.example.ads.AdService.shouldShowAds(userProfile)) {
              Spacer(modifier = Modifier.height(10.dp))
              OutlinedButton(
                onClick = {
                  val started = com.example.ads.AdService.showRewardedAdForGameBonus(
                    userProfile = userProfile,
                    rewardType = com.example.ads.RewardedFeatureType.GAME_DOUBLE_XP,
                    itemId = "daily_challenge_double_xp_$languageCode",
                    onRewardEarned = {
                      // Grant Double XP ONLY inside reward callback
                      dailyDoubleXpClaimed = true
                      onGrantBonusXp(50)
                      dailyRewardMessage = "Double XP earned! +50 Bonus XP added."
                    },
                    onFailedOrCancelled = { reason ->
                      dailyRewardMessage = reason
                    }
                  )
                  if (started) {
                    com.example.ads.AdService.completeActiveRewardedAdWithReward()
                  }
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("daily_challenge_double_xp_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CyanAccent)
              ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Watch an ad for double XP (+50 XP)",
                  color = CyanAccent,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp
                )
              }
            }

            if (dailyRewardMessage != null) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = dailyRewardMessage!!,
                color = BrightGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Custom Lesson Creator:
 * "Create Your Lesson — What would you like to practice?"
 * Generates structured lessons via GeminiTutorService, saves them persistently so users can reopen
 * them without regenerating, and hosts a real contextual conversation practice stage.
 */
@Composable
fun CustomLessonCreatorView(
  languageName: String,
  languageCode: String,
  cefrLevel: String,
  speechManager: SpeechManager,
  savedCustomLessons: List<AiCustomLesson>,
  onSaveCustomLesson: (AiCustomLesson) -> Unit,
  onSaveFlashcard: (VocabFlashcard) -> Unit,
  onCompleteLesson: (String, Int, Int) -> Unit,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val isListening by speechManager.isListening.collectAsState()
  val spokenText by speechManager.spokenText.collectAsState()

  val micPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) speechManager.startListening(languageCode)
  }

  var promptInput by remember { mutableStateOf("") }
  var selectedLevel by remember { mutableStateOf(cefrLevel.ifBlank { "A2" }) }
  var selectedTopic by remember { mutableStateOf("Ordering Food & Cafés") }
  var selectedDuration by remember { mutableIntStateOf(10) }
  var selectedMode by remember { mutableStateOf("Voice & Text") }
  var isGenerating by remember { mutableStateOf(false) }

  var activeLesson by remember { mutableStateOf<CustomInteractiveLessonData?>(null) }
  var activeLessonTab by remember { mutableIntStateOf(0) } // 0: Vocab & Sentences, 1: Listening & Quiz, 2: Live Practice

  // Real conversation turns for the active custom lesson (never fabricates fake user turns)
  val chatTurns = remember(activeLesson) {
    mutableStateListOf<Pair<String, String>>().apply {
      activeLesson?.let { add("Tutor" to it.starterTutorMessage) }
    }
  }
  var userDraftMessage by remember { mutableStateOf("") }
  var latestFeedbackNote by remember { mutableStateOf<String?>(null) }
  var isTutorReplying by remember { mutableStateOf(false) }

  LaunchedEffect(spokenText) {
    if (spokenText.isNotBlank() && activeLesson != null && activeLessonTab == 2) {
      userDraftMessage = spokenText
    }
  }

  val examplePrompts = listOf(
    "I want to practice ordering food",
    "Teach me $languageName for a job interview",
    "I want to speak with a hotel receptionist",
    "I need 10 useful shopping sentences"
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DeepNavyBg)
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
      .testTag("custom_lesson_creator_screen")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
          if (activeLesson != null) activeLesson = null else onBack()
        }) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Column {
          Text(
            text = if (activeLesson == null) "Create Your Lesson" else activeLesson!!.title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold
          )
          Text(
            text = if (activeLesson == null) "What would you like to practice?" else "${activeLesson!!.proficiencyLevel} • ${activeLesson!!.durationMinutes} min",
            style = MaterialTheme.typography.bodySmall,
            color = CyanAccent
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (activeLesson == null) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardNavyBg),
        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(20.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("Describe your goal or scenario:", color = Color.White, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = promptInput,
            onValueChange = { promptInput = it },
            placeholder = { Text("e.g., I want to practice ordering food at a café...") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("custom_lesson_prompt_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedBorderColor = CyanAccent,
              unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
            )
          )

          Spacer(modifier = Modifier.height(10.dp))
          Text("Quick ideas:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
          Spacer(modifier = Modifier.height(6.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(examplePrompts) { sample ->
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = ElevatedCardNavy,
                border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.5f)),
                modifier = Modifier.clickable { promptInput = sample }
              ) {
                Text(
                  text = sample,
                  color = Color.White,
                  fontSize = 12.sp,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Text("Proficiency Level:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("A1", "A2", "B1", "B2", "C1").forEach { lvl ->
              FilterChip(
                selected = selectedLevel == lvl,
                onClick = { selectedLevel = lvl },
                label = { Text(lvl) }
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text("Duration & Mode:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 15).forEach { mins ->
              FilterChip(
                selected = selectedDuration == mins,
                onClick = { selectedDuration = mins },
                label = { Text("$mins min") }
              )
            }
            listOf("Voice & Text", "Voice Only").forEach { mode ->
              FilterChip(
                selected = selectedMode == mode,
                onClick = { selectedMode = mode },
                label = { Text(mode) }
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          Button(
            onClick = {
              if (!isGenerating) {
                isGenerating = true
                scope.launch {
                  val generated = InteractiveGamesCatalogAndAiService.generateCustomInteractiveLesson(
                    userPrompt = promptInput,
                    languageName = languageName,
                    languageCode = languageCode,
                    proficiencyLevel = selectedLevel,
                    topic = selectedTopic,
                    durationMinutes = selectedDuration,
                    interactionMode = selectedMode
                  )
                  onSaveCustomLesson(InteractiveGamesCatalogAndAiService.toPersistedAiCustomLesson(generated))
                  activeLesson = generated
                  activeLessonTab = 0
                  isGenerating = false
                }
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("generate_custom_lesson_button")
          ) {
            if (isGenerating) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = DeepNavyBg)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Building Your Lesson...", color = DeepNavyBg, fontWeight = FontWeight.Bold)
            } else {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DeepNavyBg)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Start Personalized Lesson", color = DeepNavyBg, fontWeight = FontWeight.ExtraBold)
            }
          }
        }
      }

      // Saved Custom Lessons List
      if (savedCustomLessons.isNotEmpty()) {
        Spacer(modifier = Modifier.height(18.dp))
        Text("Your Saved Custom Lessons", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        savedCustomLessons.forEach { saved ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .clickable {
                activeLesson = InteractiveGamesCatalogAndAiService.fromPersistedAiCustomLesson(saved)
                activeLessonTab = 0
              },
            colors = CardDefaults.cardColors(containerColor = CardNavyBg),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(saved.title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(saved.userGoalPrompt, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
              Icon(Icons.Default.PlayArrow, contentDescription = "Open lesson", tint = CyanAccent)
            }
          }
        }
      }
    } else {
      val lesson = activeLesson!!
      // Lesson Stage Tabs
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("1. Vocabulary", "2. Listen & Quiz", "3. Live Practice").forEachIndexed { idx, tabTitle ->
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (activeLessonTab == idx) CyanAccent else CardNavyBg,
            modifier = Modifier
              .weight(1f)
              .clickable { activeLessonTab = idx }
          ) {
            Text(
              text = tabTitle,
              color = if (activeLessonTab == idx) DeepNavyBg else Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(vertical = 10.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      when (activeLessonTab) {
        0 -> {
          Text("Key Vocabulary & Useful Sentences", color = Color.White, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(8.dp))
          lesson.vocabulary.forEach { item ->
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
              colors = CardDefaults.cardColors(containerColor = CardNavyBg)
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("${item.word} — ${item.translation}", color = CyanAccent, fontWeight = FontWeight.Bold)
                  Row {
                    IconButton(onClick = { speechManager.speak(item.word, languageCode) }) {
                      Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = CyanAccent)
                    }
                    IconButton(onClick = {
                      onSaveFlashcard(InteractiveGamesCatalogAndAiService.toFlashcard(item, lesson.title))
                    }) {
                      Icon(Icons.Default.BookmarkBorder, contentDescription = "Save", tint = AmberGold)
                    }
                  }
                }
                Text(item.exampleSentence, color = Color.White, fontSize = 13.sp)
                Text(item.exampleTranslation, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
              }
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = { activeLessonTab = 1 },
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Next: Listening & Interactive Quiz", color = DeepNavyBg, fontWeight = FontWeight.Bold)
          }
        }

        1 -> {
          var selectedListenIdx by remember { mutableIntStateOf(-1) }
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardNavyBg)
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text("Listening Exercise", color = AmberGold, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(8.dp))
              Button(
                onClick = { speechManager.speak(lesson.listeningPrompt, languageCode) },
                colors = ButtonDefaults.buttonColors(containerColor = ElevatedCardNavy)
              ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = CyanAccent)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Play Spoken Passage", color = CyanAccent)
              }
              Spacer(modifier = Modifier.height(10.dp))
              Text(lesson.listeningQuestion, color = Color.White, fontWeight = FontWeight.SemiBold)
              Spacer(modifier = Modifier.height(8.dp))
              lesson.listeningOptions.forEachIndexed { idx, opt ->
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = if (selectedListenIdx == idx) CyanAccent.copy(alpha = 0.25f) else ElevatedCardNavy,
                  border = BorderStroke(1.dp, if (selectedListenIdx == idx) CyanAccent else Color.White.copy(alpha = 0.15f)),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { selectedListenIdx = idx }
                ) {
                  Text(opt, color = Color.White, modifier = Modifier.padding(12.dp))
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = { activeLessonTab = 2 },
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Next: Live Conversation Practice", color = DeepNavyBg, fontWeight = FontWeight.Bold)
          }
        }

        else -> {
          Text("Real Conversation Practice (Speak or Type)", color = Color.White, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(8.dp))
          chatTurns.forEach { (speaker, text) ->
            val isTutor = speaker == "Tutor"
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (isTutor) CardNavyBg else PurpleAccent.copy(alpha = 0.35f)
              )
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(speaker, color = if (isTutor) CyanAccent else AmberGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  Text(text, color = Color.White, fontSize = 14.sp)
                }
                if (isTutor) {
                  IconButton(onClick = { speechManager.speak(text, languageCode) }) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Hear reply", tint = CyanAccent)
                  }
                }
              }
            }
          }

          if (!latestFeedbackNote.isNullOrBlank()) {
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xFF0E3B2E)),
              border = BorderStroke(1.dp, BrightGreen)
            ) {
              Text(
                text = "Learning Feedback: $latestFeedbackNote",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.padding(12.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text("Suggested replies you can try:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
            items(lesson.suggestedUserReplies) { suggestion ->
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = ElevatedCardNavy,
                modifier = Modifier.clickable { userDraftMessage = suggestion }
              ) {
                Text(suggestion, color = CyanAccent, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = userDraftMessage,
              onValueChange = { userDraftMessage = it },
              placeholder = { Text("Type or use mic to reply...") },
              modifier = Modifier.weight(1f),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = CyanAccent
              )
            )
            IconButton(
              onClick = {
                val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (hasPerm) {
                  if (isListening) speechManager.finishListeningNow() else speechManager.startListening(languageCode)
                } else {
                  micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
              },
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isListening) SoftCoralRed else PurpleAccent)
            ) {
              Icon(
                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = "Microphone",
                tint = Color.White
              )
            }
            IconButton(
              onClick = {
                val msg = userDraftMessage.trim()
                if (msg.isNotBlank() && !isTutorReplying) {
                  chatTurns.add("You" to msg)
                  userDraftMessage = ""
                  isTutorReplying = true
                  scope.launch {
                    val (reply, _, feedback) = InteractiveGamesCatalogAndAiService.generateContextualPracticeReply(
                      languageName = languageName,
                      languageCode = languageCode,
                      proficiencyLevel = lesson.proficiencyLevel,
                      scenarioTitle = lesson.title,
                      conversationHistory = chatTurns.toList(),
                      latestUserMessage = msg
                    )
                    chatTurns.add("Tutor" to reply)
                    latestFeedbackNote = feedback
                    isTutorReplying = false
                    speechManager.speak(reply, languageCode)
                  }
                }
              },
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CyanAccent)
            ) {
              Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = DeepNavyBg)
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          Button(
            onClick = {
              onCompleteLesson(lesson.id, 100, 45)
              activeLesson = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = BrightGreen),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Complete Lesson & Claim +45 XP", color = DeepNavyBg, fontWeight = FontWeight.ExtraBold)
          }
        }
      }
    }
  }
}

/**
 * Interactive views for the remaining 6 core games in the 10-game suite:
 * Word Match, Listening Challenge, Sentence Builder, Pronunciation Challenge,
 * Conversation Challenge, and Vocabulary Memory Game.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordMatchInteractiveGameView(
  languageCode: String,
  speechManager: SpeechManager,
  onCompleteGame: (String, Int, Int) -> Unit,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)
  val pairs = remember(languageCode) {
    InteractiveGamesCatalogAndAiService.getPictureWordQuestions(languageCode).take(6)
      .map { it.targetWord to it.targetTranslation }
  }
  val leftWords = remember(pairs) { pairs.map { it.first }.shuffled() }
  val rightMeanings = remember(pairs) { pairs.map { it.second }.shuffled() }
  val matchedWords = remember { mutableStateListOf<String>() }
  var selectedLeft by remember { mutableStateOf<String?>(null) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DeepNavyBg)
      .padding(16.dp)
      .testTag("word_match_game_screen")
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
      }
      Text("Word Match (${matchedWords.size}/${pairs.size})", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Words", color = CyanAccent, fontWeight = FontWeight.Bold)
        leftWords.forEach { w ->
          val isMatched = matchedWords.contains(w)
          val isSelected = selectedLeft == w
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = when {
              isMatched -> SuccessGreen.copy(alpha = 0.4f)
              isSelected -> CyanAccent.copy(alpha = 0.3f)
              else -> CardNavyBg
            },
            border = BorderStroke(1.dp, if (isSelected) CyanAccent else Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
              .fillMaxWidth()
              .clickable(enabled = !isMatched) {
                selectedLeft = w
                speechManager.speak(w, languageCode)
              }
          ) {
            Text(w, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(14.dp))
          }
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Meanings", color = AmberGold, fontWeight = FontWeight.Bold)
        rightMeanings.forEach { m ->
          val ownerWord = pairs.firstOrNull { it.second == m }?.first
          val isMatched = ownerWord != null && matchedWords.contains(ownerWord)
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isMatched) SuccessGreen.copy(alpha = 0.4f) else CardNavyBg,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
              .fillMaxWidth()
              .clickable(enabled = !isMatched && selectedLeft != null) {
                if (ownerWord != null && selectedLeft == ownerWord) {
                  matchedWords.add(ownerWord)
                  selectedLeft = null
                  if (matchedWords.size == pairs.size) {
                    onCompleteGame("word_match_game", 100, 30)
                  }
                } else {
                  selectedLeft = null
                }
              }
          ) {
            Text(m, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(14.dp))
          }
        }
      }
    }
  }
}

@Composable
fun VocabularyMemoryGridGameView(
  languageCode: String,
  speechManager: SpeechManager,
  onCompleteGame: (String, Int, Int) -> Unit,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)
  val baseItems = remember(languageCode) {
    InteractiveGamesCatalogAndAiService.getPictureWordQuestions(languageCode).take(4)
  }
  val deck = remember(baseItems) {
    (baseItems.map { Triple(it.id, it.targetWord, "WORD") } +
      baseItems.map { Triple(it.id, it.targetTranslation, "MEANING") }).shuffled()
  }
  val matchedIds = remember { mutableStateListOf<String>() }
  val flippedIndices = remember { mutableStateListOf<Int>() }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DeepNavyBg)
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
      .testTag("vocabulary_memory_game_screen")
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
      }
      Text("Vocabulary Memory Game", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
    }
    Text("Tap cards to match each word with its meaning (${matchedIds.size}/${baseItems.size})", color = CyanAccent, fontSize = 13.sp)
    Spacer(modifier = Modifier.height(14.dp))

    for (row in 0 until 4) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        for (col in 0 until 2) {
          val cardIdx = row * 2 + col
          val card = deck.getOrNull(cardIdx) ?: continue
          val isMatched = matchedIds.contains(card.first)
          val isFlipped = isMatched || flippedIndices.contains(cardIdx)

          Surface(
            shape = RoundedCornerShape(16.dp),
            color = when {
              isMatched -> SuccessGreen.copy(alpha = 0.45f)
              isFlipped -> ElevatedCardNavy
              else -> CardNavyBg
            },
            border = BorderStroke(1.5.dp, if (isFlipped) CyanAccent else Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 92.dp)
              .clickable(enabled = !isMatched) {
                if (flippedIndices.size == 2) {
                  flippedIndices.clear()
                }
                if (!flippedIndices.contains(cardIdx)) {
                  flippedIndices.add(cardIdx)
                  if (card.third == "WORD") speechManager.speak(card.second, languageCode)
                  if (flippedIndices.size == 2) {
                    val firstCard = deck[flippedIndices[0]]
                    val secondCard = deck[flippedIndices[1]]
                    if (firstCard.first == secondCard.first) {
                      matchedIds.add(firstCard.first)
                      flippedIndices.clear()
                      if (matchedIds.size == baseItems.size) {
                        onCompleteGame("vocabulary_memory_game", 100, 35)
                      }
                    }
                  }
                }
              }
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
              Text(
                text = if (isFlipped) card.second else "?",
                color = if (isFlipped) Color.White else CyanAccent,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
              )
            }
          }
        }
      }
    }
  }
}
