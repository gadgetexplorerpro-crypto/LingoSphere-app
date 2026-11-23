package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CoffeeMaker
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.Light
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Window
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.Language
import com.example.data.repository.DynamicAiGameEngine
import com.example.data.repository.DynamicLevelExercise
import com.example.data.repository.GameInteractionMechanic
import com.example.data.repository.LearningResponseEvaluation
import com.example.data.repository.ValidatedAnswerOption
import com.example.data.repository.VerifiedVisualConcept
import com.example.data.repository.VerifiedVisualConceptRegistry
import com.example.speech.SpeechManager
import kotlin.math.abs

private val GridDeepNavy = Color(0xFF071329)
private val GridCardNavy = Color(0xFF102342)
private val GridOptionNavy = Color(0xFF152C52)
private val GridCyan = Color(0xFF14C8E8)
private val GridGreen = Color(0xFF00E676)
private val GridRed = Color(0xFFFF5252)
private val GridAmber = Color(0xFFFFB300)

/**
 * LingoPanda-style 2x2 Horizontal Answer Grid (`| Option A | Option B |` / `| Option C | Option D |`).
 * Evaluates answers STRICTLY by stable `ValidatedAnswerOption.id` against `correctOptionId` —
 * never by fragile array indexes!
 */
@Composable
fun LingoPandaHorizontalAnswerGrid(
  options: List<ValidatedAnswerOption>,
  correctOptionId: String,
  selectedOptionId: String?,
  onSelectOption: (ValidatedAnswerOption) -> Unit,
  modifier: Modifier = Modifier,
  tagPrefix: String = "ultimate_option"
) {
  val isAnswered = !selectedOptionId.isNullOrBlank()
  val rows = remember(options) { options.chunked(2) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("${tagPrefix}_grid_2x2"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    rows.forEachIndexed { rowIdx, rowItems ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        rowItems.forEachIndexed { colIdx, option ->
          val globalIdx = rowIdx * 2 + colIdx
          val letterLabel = ('A' + globalIdx).toString()
          val isChosen = isAnswered && option.id == selectedOptionId
          val isVerifiedCorrect = option.id == correctOptionId

          val containerColor = when {
            !isAnswered -> GridOptionNavy
            isVerifiedCorrect -> GridGreen.copy(alpha = 0.22f)
            isChosen && !isVerifiedCorrect -> GridRed.copy(alpha = 0.22f)
            else -> GridOptionNavy.copy(alpha = 0.52f)
          }

          val borderColor = when {
            !isAnswered -> Color.White.copy(alpha = 0.11f)
            isVerifiedCorrect -> GridGreen
            isChosen && !isVerifiedCorrect -> GridRed
            else -> Color.White.copy(alpha = 0.06f)
          }

          val badgeBg = when {
            !isAnswered -> GridCyan.copy(alpha = 0.16f)
            isVerifiedCorrect -> GridGreen.copy(alpha = 0.28f)
            isChosen && !isVerifiedCorrect -> GridRed.copy(alpha = 0.28f)
            else -> Color.White.copy(alpha = 0.08f)
          }

          val badgeTextTint = when {
            !isAnswered -> GridCyan
            isVerifiedCorrect -> GridGreen
            isChosen && !isVerifiedCorrect -> GridRed
            else -> Color.White.copy(alpha = 0.55f)
          }

          Surface(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .defaultMinSize(minHeight = 58.dp)
              .clickable(enabled = !isAnswered) { onSelectOption(option) }
              .testTag("${tagPrefix}_$globalIdx"),
            shape = RoundedCornerShape(14.dp),
            color = containerColor,
            border = BorderStroke(if (isAnswered && (isVerifiedCorrect || isChosen)) 1.6.dp else 1.dp, borderColor)
          ) {
            Row(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = CircleShape,
                color = badgeBg,
                modifier = Modifier.size(24.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = letterLabel,
                    color = badgeTextTint,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold
                  )
                }
              }

              Spacer(modifier = Modifier.width(8.dp))

              Text(
                text = option.text,
                color = Color.White,
                fontSize = 13.sp,
                lineHeight = 16.5.sp,
                fontWeight = if (isChosen || (isAnswered && isVerifiedCorrect)) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
              )

              if (isAnswered && isVerifiedCorrect) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = "Correct answer",
                  tint = GridGreen,
                  modifier = Modifier
                    .size(18.dp)
                    .testTag("${tagPrefix}_correct_check_$globalIdx")
                )
              } else if (isAnswered && isChosen && !isVerifiedCorrect) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                  imageVector = Icons.Default.Cancel,
                  contentDescription = "Incorrect selection",
                  tint = GridRed,
                  modifier = Modifier
                    .size(18.dp)
                    .testTag("${tagPrefix}_wrong_mark_$globalIdx")
                )
              }
            }
          }
        }

        if (rowItems.size == 1) {
          Spacer(modifier = Modifier.weight(1f))
        }
      }
    }
  }
}

/**
 * Concept-verified visual card for Picture Word Quiz and Picture Description.
 * Renders real photographs (bundled local scene photos or verified Wikimedia Commons object photos)
 * after deterministically confirming the concept matches the intended answer.
 * Never renders random geometric shapes or unrelated clothing illustrations.
 */
@Composable
fun VerifiedConceptVisualCard(
  visualConceptId: String,
  targetPhrase: String,
  englishMeaning: String,
  modifier: Modifier = Modifier,
  height: Dp = 184.dp,
  resolvedImageUrl: String = "",
  visualIconKey: String = "",
  categoryHint: String = ""
) {
  val context = LocalContext.current
  var retryNonce by remember(visualConceptId, targetPhrase) { mutableIntStateOf(0) }
  var coilErrorHealCount by remember(visualConceptId, targetPhrase) { mutableIntStateOf(0) }

  val verifiedConcept: VerifiedVisualConcept? = remember(
    visualConceptId,
    targetPhrase,
    englishMeaning,
    resolvedImageUrl,
    visualIconKey,
    categoryHint
  ) {
    var candidate = VerifiedVisualConceptRegistry.getById(visualConceptId)
      ?: VerifiedVisualConceptRegistry.findMatchingConcept(targetPhrase, englishMeaning)
    if (candidate == null && visualConceptId.startsWith("dyn:", ignoreCase = true)) {
      val cleanEng = englishMeaning.substringBefore("•").trim().ifBlank { targetPhrase }
      candidate = VerifiedVisualConceptRegistry.registerDynamicConcept(
        conceptId = visualConceptId,
        englishWord = cleanEng,
        targetWord = targetPhrase,
        languageCode = "en",
        domainLabel = categoryHint.ifBlank { "Vocabulary" },
        definitionQuestionEn = cleanEng,
        exampleSentence = englishMeaning,
        imageUrl = resolvedImageUrl,
        iconKey = visualIconKey.ifBlank { cleanEng.lowercase() },
        levelBand = 1
      )
    }
    if (candidate != null && VerifiedVisualConceptRegistry.verifyConceptMatchesAnswer(
        conceptId = candidate.conceptId,
        targetPhrase = targetPhrase,
        englishMeaning = englishMeaning
      )
    ) {
      candidate
    } else {
      null
    }
  }

  val initialSafeUrl = remember(verifiedConcept?.conceptId, verifiedConcept?.wikimediaPhotoUrl, resolvedImageUrl) {
    val engWord = verifiedConcept?.englishWord ?: englishMeaning.substringBefore("•").trim()
    val domain = categoryHint.ifBlank { verifiedConcept?.domainLabel.orEmpty() }
    listOf(resolvedImageUrl, verifiedConcept?.wikimediaPhotoUrl.orEmpty())
      .firstOrNull { url ->
        url.isNotBlank() && VerifiedVisualConceptRegistry.validateImageUrlForConcept(url, engWord, domain)
      }
      .orEmpty()
  }

  var activePhotoUrl by remember(visualConceptId, targetPhrase, initialSafeUrl) {
    mutableStateOf(initialSafeUrl)
  }
  var attemptedWikiHeal by remember(visualConceptId, targetPhrase, retryNonce) {
    mutableStateOf(false)
  }

  LaunchedEffect(verifiedConcept?.conceptId, retryNonce, coilErrorHealCount) {
    val concept = verifiedConcept ?: return@LaunchedEffect
    if (concept.localDrawableResId != null) return@LaunchedEffect
    val needsHeal = activePhotoUrl.isBlank() || coilErrorHealCount > 0
    if (needsHeal && !attemptedWikiHeal) {
      attemptedWikiHeal = true
      val resolved = VerifiedVisualConceptRegistry.resolveAndVerifyDynamicImageUrl(
        englishConcept = concept.englishWord,
        categoryHint = categoryHint.ifBlank { concept.domainLabel },
        context = context
      )
      if (resolved.isNotBlank() &&
        resolved != activePhotoUrl &&
        VerifiedVisualConceptRegistry.validateImageUrlForConcept(
          resolved,
          concept.englishWord,
          categoryHint.ifBlank { concept.domainLabel }
        )
      ) {
        activePhotoUrl = resolved
      }
    }
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .height(height)
      .clip(RoundedCornerShape(18.dp))
      .testTag("verified_visual_card_${verifiedConcept?.conceptId ?: "unverified"}"),
    shape = RoundedCornerShape(18.dp),
    color = Color(0xFF13294B),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
  ) {
    if (verifiedConcept == null) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = Icons.Default.PhotoCamera,
          contentDescription = "Visual clue unavailable",
          tint = GridCyan,
          modifier = Modifier.size(34.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Read the clue carefully to select the matching word",
          color = Color.White.copy(alpha = 0.8f),
          fontSize = 12.sp,
          textAlign = TextAlign.Center
        )
      }
    } else if (verifiedConcept.localDrawableResId != null) {
      Box(modifier = Modifier.fillMaxSize()) {
        Image(
          painter = painterResource(id = verifiedConcept.localDrawableResId),
          contentDescription = verifiedConcept.domainLabel,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = GridDeepNavy.copy(alpha = 0.72f),
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(10.dp)
        ) {
          Text(
            text = verifiedConcept.domainLabel,
            color = GridCyan,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }
    } else if (activePhotoUrl.isBlank()) {
      VerifiedExactConceptFallbackIllustration(
        concept = verifiedConcept,
        isLoading = !attemptedWikiHeal,
        onRetry = {
          coilErrorHealCount = 0
          attemptedWikiHeal = false
          retryNonce++
        }
      )
    } else {
      val imageRequest = remember(activePhotoUrl, retryNonce) {
        ImageRequest.Builder(context)
          .data(activePhotoUrl)
          .addHeader("User-Agent", "LingoSphere/1.0 (Android; Language Learning App; contact: support@lingosphere.app)")
          .crossfade(true)
          .setParameter("retry", retryNonce)
          .build()
      }

      SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = verifiedConcept.domainLabel,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
        onError = {
          if (coilErrorHealCount == 0 && !attemptedWikiHeal) {
            coilErrorHealCount = 1
          }
        },
        loading = {
          VerifiedExactConceptFallbackIllustration(
            concept = verifiedConcept,
            isLoading = true,
            onRetry = {
              coilErrorHealCount = 0
              retryNonce++
            }
          )
        },
        error = {
          VerifiedExactConceptFallbackIllustration(
            concept = verifiedConcept,
            isLoading = false,
            onRetry = {
              coilErrorHealCount = 0
              attemptedWikiHeal = false
              retryNonce++
            }
          )
        }
      )
    }
  }
}

@Composable
fun VerifiedExactConceptFallbackIllustration(
  concept: VerifiedVisualConcept,
  isLoading: Boolean,
  onRetry: () -> Unit
) {
  val exactIcon: ImageVector = remember(concept.conceptId, concept.iconKey, concept.englishWord) {
    iconForConceptKey(concept.conceptId, concept.iconKey, concept.englishWord)
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(Color(0xFF17345F), Color(0xFF0E213D))
        )
      )
      .padding(14.dp)
      .testTag("verified_fallback_illustration_${concept.conceptId}"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Surface(
        shape = CircleShape,
        color = GridCyan.copy(alpha = 0.16f),
        border = BorderStroke(1.5.dp, GridCyan.copy(alpha = 0.55f)),
        modifier = Modifier.size(72.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = exactIcon,
            contentDescription = concept.domainLabel,
            tint = GridCyan,
            modifier = Modifier.size(40.dp)
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = concept.domainLabel,
        color = Color.White.copy(alpha = 0.9f),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      if (!isLoading) {
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = GridCyan.copy(alpha = 0.16f),
          border = BorderStroke(1.dp, GridCyan.copy(alpha = 0.45f)),
          modifier = Modifier
            .clickable { onRetry() }
            .testTag("reload_photo_action_chip")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Retry photo",
              tint = GridCyan,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "HD Photo",
              color = GridCyan,
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

private fun iconForConceptKey(
  conceptId: String,
  iconKey: String = conceptId,
  englishWord: String = ""
): ImageVector {
  val probe = "${conceptId.lowercase()} ${iconKey.lowercase()} ${englishWord.lowercase()}"
  return when {
    "bread" in probe || "croissant" in probe || "bakery" in probe || "pastry" in probe -> Icons.Default.BakeryDining
    "table" in probe || "desk" in probe -> Icons.Default.TableRestaurant
    "lamp" in probe || "lantern" in probe || "chandelier" in probe -> Icons.Default.Light
    "door" in probe -> Icons.Default.MeetingRoom
    "clock" in probe || "pendulum" in probe -> Icons.Default.Schedule
    "chair" in probe || "sofa" in probe || "couch" in probe -> Icons.Default.Chair
    "bed" in probe || "pillow" in probe -> Icons.Default.Bed
    "window" in probe || "mirror" in probe -> Icons.Default.Window
    "key" in probe -> Icons.Default.Key
    "book" in probe || "library" in probe || "notebook" in probe || "dictionary" in probe || "manuscript" in probe -> Icons.Default.MenuBook
    "kettle" in probe || "thermos" in probe -> Icons.Default.CoffeeMaker
    "coffee" in probe || "espresso" in probe || "cup" in probe || "mug" in probe || "tea" in probe -> Icons.Default.LocalCafe
    "tshirt" in probe || "jacket" in probe || "scarf" in probe || "gloves" in probe || "hat" in probe || "sweater" in probe || "coat" in probe || "tie" in probe || "apron" in probe || "clothing" in probe -> Icons.Default.Checkroom
    "sunglasses" in probe || "eyeglasses" in probe || "glasses" in probe -> Icons.Default.WbSunny
    "sneakers" in probe || "boots" in probe || "shoes" in probe -> Icons.Default.DirectionsRun
    "backpack" in probe || "basket" in probe -> Icons.Default.Backpack
    "wallet" in probe -> Icons.Default.Wallet
    "watch" in probe || "wristwatch" in probe -> Icons.Default.Watch
    "umbrella" in probe -> Icons.Default.Umbrella
    "soup" in probe -> Icons.Default.SoupKitchen
    "juice" in probe || "drink" in probe || "water" in probe || "milk" in probe -> Icons.Default.LocalBar
    "apple" in probe || "banana" in probe || "cheese" in probe || "sandwich" in probe || "salad" in probe || "pizza" in probe || "tomato" in probe || "strawberry" in probe || "lunch" in probe || "food" in probe -> Icons.Default.LunchDining
    "plate" in probe || "fork" in probe || "spoon" in probe || "restaurant" in probe -> Icons.Default.Restaurant
    "airplane" in probe || "flight" in probe || "airport" in probe -> Icons.Default.Flight
    "train" in probe || "metro" in probe || "viaduct" in probe -> Icons.Default.Train
    "bicycle" in probe || "bike" in probe -> Icons.Default.DirectionsBike
    "bus" in probe -> Icons.Default.DirectionsBus
    "ship" in probe || "boat" in probe || "ferry" in probe || "kayak" in probe -> Icons.Default.DirectionsBoat
    "car" in probe || "taxi" in probe -> Icons.Default.DirectionsCar
    "suitcase" in probe || "luggage" in probe || "passport" in probe -> Icons.Default.Luggage
    "hospital" in probe || "clinic" in probe -> Icons.Default.LocalHospital
    "pharmacy" in probe || "stethoscope" in probe || "thermometer" in probe || "medicine" in probe -> Icons.Default.MedicalServices
    "cat" in probe || "dog" in probe || "bird" in probe || "fish" in probe || "horse" in probe || "butterfly" in probe -> Icons.Default.Pets
    "cloud" in probe || "rain" in probe || "snow" in probe -> Icons.Default.Cloud
    "moon" in probe || "night" in probe -> Icons.Default.NightsStay
    "star" in probe || "galaxy" in probe -> Icons.Default.Star
    "sun" in probe || "rainbow" in probe -> Icons.Default.WbSunny
    "mountain" in probe || "volcano" in probe || "glacier" in probe || "waterfall" in probe || "canyon" in probe || "island" in probe -> Icons.Default.Landscape
    "tree" in probe || "flower" in probe || "forest" in probe || "leaf" in probe || "acorn" in probe || "trowel" in probe -> Icons.Default.Park
    "compass" in probe || "map" in probe || "tent" in probe || "binoculars" in probe || "lighthouse" in probe -> Icons.Default.Explore
    "violin" in probe || "piano" in probe || "guitar" in probe || "cello" in probe || "symphony" in probe || "drum" in probe || "music" in probe -> Icons.Default.MusicNote
    "microscope" in probe || "telescope" in probe || "observatory" in probe || "beaker" in probe || "satellite" in probe || "solar" in probe || "turbine" in probe || "science" in probe -> Icons.Default.Science
    "museum" in probe || "cathedral" in probe || "theater" in probe || "courthouse" in probe || "amphitheater" in probe || "aqueduct" in probe -> Icons.Default.AccountBalance
    "building" in probe || "bridge" in probe || "hotel" in probe || "tower" in probe -> Icons.Default.Apartment
    "laptop" in probe -> Icons.Default.LaptopMac
    "keyboard" in probe -> Icons.Default.Keyboard
    "headphones" in probe -> Icons.Default.Headphones
    "microphone" in probe -> Icons.Default.Mic
    "phone" in probe -> Icons.Default.Smartphone
    "camera" in probe -> Icons.Default.PhotoCamera
    "trophy" in probe -> Icons.Default.EmojiEvents
    "paint" in probe || "easel" in probe || "sculpture" in probe || "art" in probe -> Icons.Default.Palette
    "school" in probe || "classroom" in probe || "campus" in probe -> Icons.Default.School
    "briefcase" in probe || "office" in probe -> Icons.Default.Work
    "receipt" in probe || "supermarket" in probe || "shop" in probe || "cart" in probe -> Icons.Default.ShoppingCart
    "bag" in probe -> Icons.Default.ShoppingBag
    "planet" in probe || "earth" in probe || "globe" in probe -> Icons.Default.Public
    else -> Icons.Default.PhotoCamera
  }
}

/**
 * Sleek, Compact Horizontal Audio Player for Listening & Spelling games.
 * Eliminates oversized empty audio boxes and keeps speed pills (`0.75x`, `1.0x`, `1.25x`)
 * strictly on a single horizontal line (`softWrap = false, maxLines = 1`) on all mobile screens.
 */
@Composable
fun CompactHorizontalAudioPlayer(
  spokenText: String,
  languageCode: String,
  speechManager: SpeechManager,
  modifier: Modifier = Modifier,
  initialSpeed: Float = 1.0f,
  lockSlowSpeeds: Boolean = false,
  labelBadge: String? = null,
  onPlayed: () -> Unit = {}
) {
  val isSpeaking by speechManager.isSpeaking.collectAsState()
  var selectedSpeed by remember(spokenText, initialSpeed) { mutableFloatStateOf(initialSpeed) }
  val speedOptions = remember { listOf(0.75f to "0.75x", 1.0f to "1.0x", 1.25f to "1.25x") }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("compact_horizontal_audio_player"),
    shape = RoundedCornerShape(14.dp),
    color = Color(0xFF13294B),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
  ) {
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
      if (!labelBadge.isNullOrBlank()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.GraphicEq,
              contentDescription = null,
              tint = if (isSpeaking) GridGreen else GridCyan,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = labelBadge,
              color = GridCyan,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false
            )
          }
          if (isSpeaking) {
            Text(
              text = "Playing...",
              color = GridGreen,
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Left: Play/Stop + Replay compact controls
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Button(
            onClick = {
              if (isSpeaking) {
                speechManager.stopSpeaking()
              } else {
                onPlayed()
                speechManager.speakWithRate(spokenText, languageCode, selectedSpeed)
              }
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isSpeaking) GridRed else GridCyan
            ),
            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp),
            modifier = Modifier
              .defaultMinSize(minWidth = 64.dp, minHeight = 34.dp)
              .testTag("compact_audio_play_stop_button")
          ) {
            Icon(
              imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
              contentDescription = if (isSpeaking) "Stop audio" else "Play audio",
              tint = if (isSpeaking) Color.White else GridDeepNavy,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (isSpeaking) "Stop" else "Play",
              color = if (isSpeaking) Color.White else GridDeepNavy,
              fontSize = 12.sp,
              fontWeight = FontWeight.ExtraBold,
              maxLines = 1,
              softWrap = false
            )
          }

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
            modifier = Modifier
              .size(34.dp)
              .clickable {
                onPlayed()
                speechManager.speakWithRate(spokenText, languageCode, selectedSpeed)
              }
              .testTag("compact_audio_replay_button")
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = "Replay audio",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }

        // Right: Single-line Horizontal Speed Chips (`0.75x`, `1.0x`, `1.25x`) — NEVER wraps vertically!
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          speedOptions.forEach { (rate, textLabel) ->
            val isDisabled = lockSlowSpeeds && rate < 1.2f
            val isSelected = abs(selectedSpeed - rate) < 0.05f
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = when {
                isSelected -> GridCyan.copy(alpha = 0.24f)
                isDisabled -> Color.White.copy(alpha = 0.04f)
                else -> Color.White.copy(alpha = 0.08f)
              },
              border = BorderStroke(
                1.dp,
                if (isSelected) GridCyan else Color.White.copy(alpha = 0.12f)
              ),
              modifier = Modifier
                .width(44.dp)
                .height(28.dp)
                .clickable(enabled = !isDisabled) {
                  selectedSpeed = rate
                  onPlayed()
                  speechManager.speakWithRate(spokenText, languageCode, rate)
                }
                .testTag("audio_speed_chip_$textLabel")
            ) {
              Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = textLabel,
                  color = when {
                    isSelected -> GridCyan
                    isDisabled -> Color.White.copy(alpha = 0.3f)
                    else -> Color.White.copy(alpha = 0.88f)
                  },
                  fontSize = 10.5.sp,
                  fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                  maxLines = 1,
                  softWrap = false
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
 * Listening Game #35 (`AUDIO_WORD_MATCH`):
 * Playable audio speaker clips on the left matched with shuffled meanings on the right.
 */
@Composable
fun AudioWordMatchMechanicStage(
  exercise: DynamicLevelExercise,
  languageCode: String,
  speechManager: SpeechManager,
  isAnswered: Boolean,
  onCompleted: (LearningResponseEvaluation) -> Unit
) {
  val pairs = remember(exercise.id) {
    exercise.matchingPairs.distinctBy { it.first.lowercase() }.take(5)
  }
  val rightShuffled = remember(exercise.id) {
    val base = pairs.mapIndexed { idx, p -> idx to p.second }
    val shift = (exercise.levelNumber + 2) % base.size.coerceAtLeast(1)
    val rotated = base.indices.map { i -> base[(i + shift) % base.size] }
    if (rotated.map { it.first } == base.map { it.first } && base.size > 1) base.reversed() else rotated
  }

  var selectedAudioPairId by remember(exercise.id) { mutableStateOf<Int?>(null) }
  var matchedPairIds by remember(exercise.id) { mutableStateOf<Set<Int>>(emptySet()) }
  var wrongFlashRightIdx by remember(exercise.id) { mutableStateOf<Int?>(null) }
  var wrongAttempts by remember(exercise.id) { mutableIntStateOf(0) }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("audio_word_match_stage"),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      text = "Matched ${matchedPairIds.size}/${pairs.size} spoken words • Tap a speaker clip, then tap its meaning",
      color = GridCyan,
      fontSize = 11.5.sp,
      fontWeight = FontWeight.Bold
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Left Column: Audio Speaker Clips (text stays hidden until matched!)
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        pairs.forEachIndexed { pairId, (spokenWord, _) ->
          val isMatched = pairId in matchedPairIds
          val isSelected = selectedAudioPairId == pairId
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = 48.dp)
              .clickable(enabled = !isMatched && !isAnswered) {
                selectedAudioPairId = pairId
                wrongFlashRightIdx = null
                speechManager.speak(spokenWord, languageCode)
              }
              .testTag("audio_match_clip_$pairId"),
            shape = RoundedCornerShape(12.dp),
            color = when {
              isMatched -> GridGreen.copy(alpha = 0.2f)
              isSelected -> GridCyan.copy(alpha = 0.24f)
              else -> GridOptionNavy
            },
            border = BorderStroke(
              1.2.dp,
              when {
                isMatched -> GridGreen
                isSelected -> GridCyan
                else -> Color.White.copy(alpha = 0.12f)
              }
            )
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = if (isMatched) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Play audio clip ${pairId + 1}",
                tint = if (isMatched) GridGreen else GridCyan,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (isMatched) spokenWord else "Audio Clip #${pairId + 1}",
                color = Color.White,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }

      // Right Column: Shuffled Meanings
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        rightShuffled.forEachIndexed { rowIdx, (pairId, meaningText) ->
          val isMatched = pairId in matchedPairIds
          val isWrong = wrongFlashRightIdx == rowIdx
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = 48.dp)
              .clickable(enabled = !isMatched && !isAnswered && selectedAudioPairId != null) {
                val chosenAudio = selectedAudioPairId ?: return@clickable
                if (chosenAudio == pairId) {
                  val nextMatched = matchedPairIds + pairId
                  matchedPairIds = nextMatched
                  selectedAudioPairId = null
                  wrongFlashRightIdx = null
                  if (nextMatched.size == pairs.size) {
                    val score = (100 - wrongAttempts * 10).coerceIn(72, 100)
                    onCompleted(
                      LearningResponseEvaluation(
                        isPassed = true,
                        accuracyScore = score,
                        matchedWords = pairs.map { it.first },
                        missingWords = emptyList(),
                        feedbackTitle = "All ${pairs.size} Audio Clips Matched! ($score%)",
                        detailedExplanation = pairs.joinToString(" • ") { "${it.first} = ${it.second}" },
                        suggestedImprovement = "Sharp auditory recognition!"
                      )
                    )
                  }
                } else {
                  wrongAttempts++
                  wrongFlashRightIdx = rowIdx
                }
              }
              .testTag("audio_match_meaning_$rowIdx"),
            shape = RoundedCornerShape(12.dp),
            color = when {
              isMatched -> GridGreen.copy(alpha = 0.2f)
              isWrong -> GridRed.copy(alpha = 0.22f)
              else -> GridOptionNavy
            },
            border = BorderStroke(
              1.2.dp,
              when {
                isMatched -> GridGreen
                isWrong -> GridRed
                else -> Color.White.copy(alpha = 0.12f)
              }
            )
          ) {
            Box(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
              contentAlignment = Alignment.CenterStart
            ) {
              Text(
                text = meaningText,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = if (isMatched) FontWeight.Bold else FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Listening Game #36 (`LISTEN_AND_TYPE`):
 * Compact horizontal audio player + live transcription input field.
 */
@Composable
fun ListenAndTypeMechanicStage(
  exercise: DynamicLevelExercise,
  languageCode: String,
  speechManager: SpeechManager,
  typedAnswer: String,
  onTypedAnswerChange: (String) -> Unit,
  isAnswered: Boolean,
  onCompleted: (LearningResponseEvaluation) -> Unit
) {
  val lang = Language.findByCode(languageCode)
  val wordCount = remember(exercise.targetPhrase) {
    exercise.targetPhrase.trim().split(Regex("\\s+")).count { it.isNotBlank() }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("listen_and_type_stage"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    CompactHorizontalAudioPlayer(
      spokenText = exercise.targetPhrase,
      languageCode = languageCode,
      speechManager = speechManager,
      labelBadge = "Dictation Audio ($wordCount ${if (wordCount == 1) "word" else "words"})"
    )

    OutlinedTextField(
      value = typedAnswer,
      onValueChange = onTypedAnswerChange,
      enabled = !isAnswered,
      label = { Text("Type what you hear in ${lang.name}") },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("listen_and_type_input"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = GridCyan,
        unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
      )
    )

    if (!isAnswered) {
      Button(
        onClick = {
          val eval = DynamicAiGameEngine.evaluateLearningResponse(
            userResponse = typedAnswer,
            targetPhrase = exercise.targetPhrase,
            mechanic = GameInteractionMechanic.LISTEN_AND_TYPE
          )
          onCompleted(eval)
        },
        enabled = typedAnswer.isNotBlank(),
        modifier = Modifier
          .fillMaxWidth()
          .height(46.dp)
          .testTag("check_listen_and_type_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GridCyan)
      ) {
        Text("Check Transcription", color = GridDeepNavy, fontWeight = FontWeight.ExtraBold)
      }
    }
  }
}

/**
 * Listening Game #37 (`SOUND_DETECTIVE`):
 * Minimal-pair / phonetic contrast comparison (`Compare A` vs `Compare B`) + Mystery Target Clip + 2x2 Grid.
 */
@Composable
fun SoundDetectiveMechanicStage(
  exercise: DynamicLevelExercise,
  languageCode: String,
  speechManager: SpeechManager,
  selectedOptionId: String?,
  onSelectOption: (ValidatedAnswerOption) -> Unit
) {
  val contrastWord = exercise.secondaryAudioPhrase.ifBlank {
    exercise.resolvedAnswerOptions().firstOrNull { it.id != exercise.resolvedCorrectOptionId() }?.text.orEmpty()
  }
  val swapOrder = remember(exercise.id) { (exercise.levelNumber % 2) == 1 }
  val sampleA = if (swapOrder) contrastWord else exercise.targetPhrase
  val sampleB = if (swapOrder) exercise.targetPhrase else contrastWord

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("sound_detective_stage"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedButton(
        onClick = { speechManager.speakWithRate(sampleA, languageCode, 0.9f) },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        modifier = Modifier
          .weight(1f)
          .testTag("sound_detective_sample_a")
      ) {
        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = GridCyan, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Compare Sound A", color = Color.White, fontSize = 12.sp, maxLines = 1, softWrap = false)
      }
      OutlinedButton(
        onClick = { speechManager.speakWithRate(sampleB, languageCode, 0.9f) },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        modifier = Modifier
          .weight(1f)
          .testTag("sound_detective_sample_b")
      ) {
        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = GridAmber, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Compare Sound B", color = Color.White, fontSize = 12.sp, maxLines = 1, softWrap = false)
      }
    }

    CompactHorizontalAudioPlayer(
      spokenText = exercise.targetPhrase,
      languageCode = languageCode,
      speechManager = speechManager,
      labelBadge = "Mystery Target Clip — Which word was spoken?"
    )

    LingoPandaHorizontalAnswerGrid(
      options = exercise.resolvedAnswerOptions(),
      correctOptionId = exercise.resolvedCorrectOptionId(),
      selectedOptionId = selectedOptionId,
      onSelectOption = onSelectOption
    )
  }
}

/**
 * Listening Game #38 (`LISTENING_STORY_QUIZ`):
 * Multi-sentence spoken mini-story passage + 2x2 comprehension grid + post-answer story transcript.
 */
@Composable
fun ListeningStoryQuizStage(
  exercise: DynamicLevelExercise,
  languageCode: String,
  speechManager: SpeechManager,
  selectedOptionId: String?,
  onSelectOption: (ValidatedAnswerOption) -> Unit
) {
  val storyAudio = exercise.secondaryAudioPhrase.ifBlank { exercise.targetPhrase }
  val isAnswered = !selectedOptionId.isNullOrBlank()

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("listening_story_quiz_stage"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    CompactHorizontalAudioPlayer(
      spokenText = storyAudio,
      languageCode = languageCode,
      speechManager = speechManager,
      labelBadge = "Short Story Passage"
    )

    if (isAnswered) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = GridOptionNavy,
        border = BorderStroke(1.dp, GridCyan.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Story Transcript: \"$storyAudio\"",
          color = Color.White.copy(alpha = 0.9f),
          fontSize = 12.sp,
          modifier = Modifier.padding(10.dp)
        )
      }
    }

    LingoPandaHorizontalAnswerGrid(
      options = exercise.resolvedAnswerOptions(),
      correctOptionId = exercise.resolvedCorrectOptionId(),
      selectedOptionId = selectedOptionId,
      onSelectOption = onSelectOption
    )
  }
}

/**
 * Listening Game #39 (`MISSING_AUDIO_WORD` / Key Audio Detail):
 * Plays the complete spoken sentence while displaying a `[ _____ ]` cloze sentence + 2x2 grid.
 */
@Composable
fun MissingAudioWordStage(
  exercise: DynamicLevelExercise,
  languageCode: String,
  speechManager: SpeechManager,
  selectedOptionId: String?,
  onSelectOption: (ValidatedAnswerOption) -> Unit
) {
  val fullSpoken = exercise.secondaryAudioPhrase.ifBlank { exercise.targetPhrase }
  val maskedLine = exercise.clozeMaskedSentence.ifBlank { "[ _____ ]" }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("missing_audio_word_stage"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    CompactHorizontalAudioPlayer(
      spokenText = fullSpoken,
      languageCode = languageCode,
      speechManager = speechManager,
      labelBadge = "Full Spoken Sentence"
    )

    Surface(
      shape = RoundedCornerShape(12.dp),
      color = GridOptionNavy,
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("missing_audio_cloze_card")
    ) {
      Text(
        text = maskedLine,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 14.5.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 12.dp)
      )
    }

    LingoPandaHorizontalAnswerGrid(
      options = exercise.resolvedAnswerOptions(),
      correctOptionId = exercise.resolvedCorrectOptionId(),
      selectedOptionId = selectedOptionId,
      onSelectOption = onSelectOption
    )
  }
}

/**
 * Listening Game #40 (`CONVERSATION_LISTENING` / Dialogue Listening):
 * Two-speaker (`Speaker A` & `Speaker B`) audio exchange + 2x2 comprehension grid.
 */
@Composable
fun DialogueListeningStage(
  exercise: DynamicLevelExercise,
  languageCode: String,
  speechManager: SpeechManager,
  selectedOptionId: String?,
  onSelectOption: (ValidatedAnswerOption) -> Unit
) {
  val lineA = exercise.dialogueSpeakerALine.ifBlank { "Excuse me?" }
  val lineB = exercise.dialogueSpeakerBLine.ifBlank { exercise.targetPhrase }
  val combinedDialogue = "$lineA ... $lineB"
  val isAnswered = !selectedOptionId.isNullOrBlank()

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("dialogue_listening_stage"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = GridOptionNavy,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = Modifier
          .weight(1f)
          .clickable { speechManager.speakWithRate(lineA, languageCode, 0.95f) }
          .testTag("dialogue_speaker_a_button")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = GridCyan, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isAnswered) "A: $lineA" else "Speaker A Audio",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = if (isAnswered) 2 else 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = GridOptionNavy,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = Modifier
          .weight(1f)
          .clickable { speechManager.speakWithRate(lineB, languageCode, 1.0f) }
          .testTag("dialogue_speaker_b_button")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = GridAmber, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isAnswered) "B: $lineB" else "Speaker B Audio",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = if (isAnswered) 2 else 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }

    CompactHorizontalAudioPlayer(
      spokenText = combinedDialogue,
      languageCode = languageCode,
      speechManager = speechManager,
      labelBadge = "Full 2-Speaker Exchange"
    )

    LingoPandaHorizontalAnswerGrid(
      options = exercise.resolvedAnswerOptions(),
      correctOptionId = exercise.resolvedCorrectOptionId(),
      selectedOptionId = selectedOptionId,
      onSelectOption = onSelectOption
    )
  }
}

/**
 * Listening Game #41 (`FAST_SPEECH_CHALLENGE`):
 * Plays brisk 1.25x native cadence first; unlocks slower speed comparison after first fast listen or answer.
 */
@Composable
fun FastSpeechChallengeStage(
  exercise: DynamicLevelExercise,
  languageCode: String,
  speechManager: SpeechManager,
  selectedOptionId: String?,
  onSelectOption: (ValidatedAnswerOption) -> Unit
) {
  var hasHeardFastClip by remember(exercise.id) { mutableStateOf(false) }
  val isAnswered = !selectedOptionId.isNullOrBlank()

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("fast_speech_challenge_stage"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Default.Speed, contentDescription = null, tint = GridAmber, modifier = Modifier.size(15.dp))
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = if (hasHeardFastClip || isAnswered) {
          "Fast clip played — compare speeds or select your answer"
        } else {
          "Brisk 1.25x cadence first — tap Play to unlock slower comparison"
        },
        color = GridAmber,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold
      )
    }

    CompactHorizontalAudioPlayer(
      spokenText = exercise.targetPhrase,
      languageCode = languageCode,
      speechManager = speechManager,
      initialSpeed = 1.25f,
      lockSlowSpeeds = !hasHeardFastClip && !isAnswered,
      labelBadge = "Native Brisk Cadence (1.25x)",
      onPlayed = { hasHeardFastClip = true }
    )

    LingoPandaHorizontalAnswerGrid(
      options = exercise.resolvedAnswerOptions(),
      correctOptionId = exercise.resolvedCorrectOptionId(),
      selectedOptionId = selectedOptionId,
      onSelectOption = onSelectOption
    )
  }
}
