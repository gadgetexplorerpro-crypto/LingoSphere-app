package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.CameraDescriptionFeedback
import com.example.ai.CameraLensSceneAnalysis
import com.example.ai.ExploreAiLearningEngine
import com.example.ai.ExploreAiResult
import com.example.ai.ExploreLearnerContext
import com.example.ai.ExploreSessionRecord
import com.example.ai.GeminiTutorService
import com.example.ai.PracticalFeaturesAiService
import com.example.data.model.CameraDetectedItem
import com.example.data.model.CameraSceneResult
import com.example.data.model.Language
import com.example.data.model.RealLifeSpeakingEvaluation
import com.example.data.model.RealLifeSpeakingExercise
import com.example.data.model.SpeakWhatYouSeeEvaluation
import com.example.data.model.SpeakWhatYouSeeQuestion
import com.example.data.model.UserProfile
import com.example.speech.SpeechManager
import com.example.ui.components.LingoCleanCard
import com.example.ui.components.LingoEmptyOrErrorCard
import com.example.ui.components.LingoStatusBadge
import com.example.ui.components.LingoTokens
import com.example.ui.components.LingoTopBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiCameraLanguageScreen(
  userProfile: UserProfile,
  speechManager: SpeechManager,
  onSaveVocabToFlashcard: (term: String, translation: String, phonetic: String) -> Unit,
  onRecordMistake: (category: String, original: String, corrected: String, explanation: String) -> Unit = { _, _, _, _ -> },
  onBack: () -> Unit
) {
  BackHandler { onBack() }
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val targetLang = Language.findByCode(userProfile.targetLanguageCode)
  val nativeLang = Language.findByCode(userProfile.nativeLanguageCode)
  val learnerCtx = remember(userProfile) { ExploreLearnerContext.fromProfile(userProfile) }

  val isListening by speechManager.isListening.collectAsState()
  val spokenText by speechManager.spokenText.collectAsState()

  var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var isAnalyzing by remember { mutableStateOf(false) }
  var cameraResult by remember { mutableStateOf<CameraSceneResult?>(null) }
  var speakingExercises by remember { mutableStateOf<List<RealLifeSpeakingExercise>>(emptyList()) }
  var activeExerciseIdForMic by remember { mutableStateOf<String?>(null) }
  var exerciseDraftInputs by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
  var exerciseEvaluations by remember { mutableStateOf<Map<String, RealLifeSpeakingEvaluation>>(emptyMap()) }
  var evaluatingExerciseId by remember { mutableStateOf<String?>(null) }
  var savedItemsMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var selectedResultTab by remember { mutableIntStateOf(0) } // 0 = Detected Words, 1 = Speak What You See
  var selectedLensMode by remember { mutableStateOf("Scene") } // Object, Scene, Describe-a-photo
  var recentCameraSessions by remember(targetLang.code) {
    mutableStateOf(ExploreAiLearningEngine.loadLocalExploreSessions(context, "camera_lens", targetLang.code))
  }

  // Speak What You See & Grounded Scene Description Q&A
  var cameraAndMicPrivacyConsent by remember { mutableStateOf(true) }
  var aiSceneAnalysis by remember { mutableStateOf<CameraLensSceneAnalysis?>(null) }
  var describeDraft by remember { mutableStateOf("") }
  var isEvaluatingDescription by remember { mutableStateOf(false) }
  var descriptionFeedback by remember { mutableStateOf<CameraDescriptionFeedback?>(null) }
  var descriptionError by remember { mutableStateOf<String?>(null) }
  var followUpHistory by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
  var speakWhatYouSeeQuestions by remember { mutableStateOf<List<SpeakWhatYouSeeQuestion>>(emptyList()) }
  var swysAnswerInputs by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
  var swysEvaluations by remember { mutableStateOf<Map<String, SpeakWhatYouSeeEvaluation>>(emptyMap()) }
  var evaluatingSwysId by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(spokenText, activeExerciseIdForMic) {
    val activeId = activeExerciseIdForMic
    if (activeId != null && spokenText.isNotBlank()) {
      when {
        activeId == "camera_describe_photo" -> {
          describeDraft = spokenText
        }
        activeId.startsWith("swys_") -> {
          val qId = activeId.removePrefix("swys_")
          swysAnswerInputs = swysAnswerInputs + (qId to spokenText)
        }
        else -> {
          exerciseDraftInputs = exerciseDraftInputs + (activeId to spokenText)
        }
      }
    }
  }

  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted && activeExerciseIdForMic != null) {
      speechManager.startListening(targetLang.code)
    } else if (!granted) {
      Toast.makeText(context, "Microphone permission is required for voice practice.", Toast.LENGTH_SHORT).show()
    }
  }

  fun analyzeSceneBitmap(bitmap: Bitmap) {
    if (!cameraAndMicPrivacyConsent) {
      errorMessage = "Please enable Camera & Audio Processing Consent below to analyze photos."
      return
    }
    capturedBitmap = bitmap
    errorMessage = null
    descriptionFeedback = null
    descriptionError = null
    followUpHistory = emptyList()
    isAnalyzing = true
    coroutineScope.launch {
      when (val result = ExploreAiLearningEngine.analyzeCameraSceneBitmap(context, learnerCtx, bitmap)) {
        is ExploreAiResult.Success -> {
          val analysis = result.data
          aiSceneAnalysis = analysis
          val mappedItems = analysis.vocabulary.mapIndexed { idx, v ->
            CameraDetectedItem(
              detectedObjectOrText = analysis.visibleObjects.getOrElse(idx) { v.meaningNative },
              nameInTargetLanguage = v.wordTarget,
              phoneticGuide = v.phonetic,
              nativeTranslation = v.meaningNative,
              grammarNote = "Scene vocabulary (${learnerCtx.cefrLevel})",
              exampleSentenceTarget = v.exampleSentenceTarget,
              exampleSentenceNative = v.exampleSentenceNative
            )
          }
          val mappedResult = CameraSceneResult(
            sceneTitle = analysis.sceneTitle,
            sceneDescription = analysis.sceneSummaryNative,
            items = mappedItems
          )
          cameraResult = mappedResult
          val generated = GeminiTutorService.buildSpeakingExercisesFromCameraScene(mappedResult)
          speakingExercises = generated
          exerciseDraftInputs = generated.associate { it.id to "" }
          val swys = analysis.usefulPhrasesTarget.mapIndexed { idx, (phraseT, phraseN) ->
            SpeakWhatYouSeeQuestion(
              id = "cam_q_$idx",
              detectedObject = analysis.visibleObjects.getOrElse(idx) { analysis.sceneTitle },
              detectedObjectTranslation = analysis.vocabulary.getOrNull(idx)?.wordTarget ?: "",
              questionTargetLanguage = if (idx == 0 && analysis.starterQuestionTarget.isNotBlank()) analysis.starterQuestionTarget else phraseT,
              questionNativeTranslation = if (idx == 0 && analysis.starterQuestionNative.isNotBlank()) analysis.starterQuestionNative else phraseN,
              suggestedVocabulary = analysis.vocabulary.take(4).map { it.wordTarget },
              sampleAnswerTargetLanguage = analysis.vocabulary.getOrNull(idx)?.exampleSentenceTarget ?: phraseT
            )
          }
          speakWhatYouSeeQuestions = swys
          swysAnswerInputs = swys.associate { it.id to "" }
          selectedResultTab = if (selectedLensMode == "Describe-a-photo") 1 else 0
          ExploreAiLearningEngine.saveExploreSession(
            context = context,
            record = ExploreSessionRecord(
              sessionId = "cam_${targetLang.code}_${System.currentTimeMillis()}",
              featureType = "camera_lens",
              languageCode = targetLang.code,
              title = "${selectedLensMode}: ${analysis.sceneTitle}",
              summary = "${analysis.sceneSummaryNative} (${analysis.vocabulary.size} words)",
              cefrLevel = learnerCtx.cefrLevel,
              turnsCount = 1,
              scorePercent = 100
            )
          )
          recentCameraSessions = ExploreAiLearningEngine.loadLocalExploreSessions(context, "camera_lens", targetLang.code)
        }
        is ExploreAiResult.Offline -> {
          errorMessage = "You appear to be offline. Reconnect to the internet and tap Retry to analyze your photo."
        }
        is ExploreAiResult.QuotaExceeded -> {
          errorMessage = result.message
        }
        is ExploreAiResult.Error -> {
          errorMessage = result.message
        }
      }
      isAnalyzing = false
    }
  }

  fun submitPhotoDescription(isFollowUp: Boolean = false) {
    val scene = aiSceneAnalysis ?: return
    val text = describeDraft.trim()
    if (text.isBlank() || isEvaluatingDescription) return
    isEvaluatingDescription = true
    descriptionError = null
    coroutineScope.launch {
      when (val evalRes = ExploreAiLearningEngine.evaluateCameraDescription(
        context = context,
        learnerCtx = learnerCtx,
        scene = scene,
        learnerDescription = text,
        userQuestionOrFollowUp = isFollowUp
      )) {
        is ExploreAiResult.Success -> {
          val fb = evalRes.data
          descriptionFeedback = fb
          followUpHistory = followUpHistory + ("You" to text) + ("AI Tutor" to fb.followUpQuestionTarget)
          describeDraft = ""
          fb.capturedMistake?.let { m ->
            onRecordMistake(m.category, m.originalText, m.correctedText, m.explanation)
          }
          if (fb.followUpQuestionTarget.isNotBlank()) {
            speechManager.speak(fb.followUpQuestionTarget, targetLang.code)
          }
          ExploreAiLearningEngine.saveExploreSession(
            context = context,
            record = ExploreSessionRecord(
              sessionId = "cam_desc_${targetLang.code}_${System.currentTimeMillis()}",
              featureType = "camera_lens",
              languageCode = targetLang.code,
              title = scene.sceneTitle,
              summary = fb.groundedAssessment,
              cefrLevel = learnerCtx.cefrLevel,
              turnsCount = followUpHistory.size,
              scorePercent = ((fb.relevanceScore + fb.grammarScore) / 2).coerceIn(0, 100)
            )
          )
        }
        ExploreAiResult.Offline -> {
          descriptionError = "Offline — reconnect and tap Retry to evaluate your description."
        }
        is ExploreAiResult.QuotaExceeded -> {
          descriptionError = evalRes.message
        }
        is ExploreAiResult.Error -> {
          descriptionError = evalRes.message
        }
      }
      isEvaluatingDescription = false
    }
  }

  val takePictureLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicturePreview()
  ) { bitmap ->
    if (bitmap != null) {
      analyzeSceneBitmap(bitmap)
    }
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      takePictureLauncher.launch(null)
    } else {
      Toast.makeText(context, "Camera permission is required to take photos.", Toast.LENGTH_SHORT).show()
    }
  }

  val pickPhotoLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      val bitmap = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          val source = ImageDecoder.createSource(context.contentResolver, uri)
          ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = true
          }
        } else {
          @Suppress("DEPRECATION")
          MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
      } catch (_: Exception) {
        null
      }

      if (bitmap != null) {
        analyzeSceneBitmap(bitmap)
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(LingoTokens.bgCanvas)
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .imePadding()
  ) {
    LingoTopBar(
      title = "Camera Speaking Lens",
      subtitle = "Turn photos & signs into ${targetLang.name} practice",
      onBack = onBack,
      actions = {
        LingoStatusBadge(
          text = "${targetLang.flagEmoji} ${targetLang.name}",
          color = LingoTokens.accentPrimary,
          modifier = Modifier.padding(end = 4.dp)
        )
      }
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(vertical = 16.dp)
    ) {
      item {
        LingoCleanCard(modifier = Modifier.fillMaxWidth()) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            if (capturedBitmap != null) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(190.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(Color.Black)
              ) {
                Image(
                  bitmap = capturedBitmap!!.asImageBitmap(),
                  contentDescription = "Captured Scene",
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Crop
                )
                if (isAnalyzing) {
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Column(
                      horizontalAlignment = Alignment.CenterHorizontally,
                      modifier = Modifier.padding(16.dp)
                    ) {
                      CircularProgressIndicator(color = LingoTokens.accentPrimary)
                      Spacer(modifier = Modifier.height(10.dp))
                      Text(
                        text = "Identifying objects & building speaking exercises...",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                      )
                    }
                  }
                }
              }
            } else {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(136.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .border(1.dp, LingoTokens.borderSubtle, RoundedCornerShape(14.dp))
                  .background(LingoTokens.bgSubtle),
                contentAlignment = Alignment.Center
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                  Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = LingoTokens.accentPrimary,
                    modifier = Modifier.size(32.dp)
                  )
                  Text(
                    text = "Snap an object, sign, or menu to unlock real-life ${targetLang.name} speaking prompts",
                    fontSize = 13.sp,
                    color = LingoTokens.textSecondary,
                    textAlign = TextAlign.Center
                  )
                }
              }
            }

            FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              listOf("Object", "Scene", "Describe-a-photo").forEach { mode ->
                val selected = selectedLensMode == mode
                FilterChip(
                  selected = selected,
                  onClick = {
                    selectedLensMode = mode
                    if (mode == "Describe-a-photo" && cameraResult != null) {
                      selectedResultTab = 1
                    } else if (cameraResult != null) {
                      selectedResultTab = 0
                    }
                  },
                  label = {
                    Text(
                      text = mode,
                      fontSize = 12.sp,
                      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                  },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = LingoTokens.brandCyan.copy(alpha = 0.18f),
                    selectedLabelColor = LingoTokens.brandCyan,
                    containerColor = LingoTokens.bgSubtle,
                    labelColor = LingoTokens.textSecondary
                  ),
                  modifier = Modifier.testTag("camera_lens_mode_${mode.lowercase().replace('-', '_')}")
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Button(
                onClick = {
                  val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                  ) == PackageManager.PERMISSION_GRANTED
                  if (hasPermission) {
                    takePictureLauncher.launch(null)
                  } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = LingoTokens.accentBlue,
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("camera_take_photo_btn")
              ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Take Photo", maxLines = 1)
              }

              OutlinedButton(
                onClick = {
                  pickPhotoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
                border = BorderStroke(1.dp, LingoTokens.borderFocus),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LingoTokens.textPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("camera_pick_gallery_btn")
              ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pick Image", maxLines = 1)
              }
            }

            Text(
              text = "Capture a real photo or pick an image from your gallery to generate live ${targetLang.name} vocabulary, grounded speaking feedback, and follow-up questions.",
              fontSize = 12.sp,
              color = LingoTokens.textSecondary
            )

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = LingoTokens.bgSubtle,
              border = BorderStroke(1.dp, LingoTokens.borderSubtle),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Camera & Audio Privacy Consent",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = LingoTokens.textPrimary
                  )
                  Text(
                    text = "Photos and voice answers are processed only when you trigger analysis.",
                    fontSize = 11.5.sp,
                    color = LingoTokens.textSecondary
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                  checked = cameraAndMicPrivacyConsent,
                  onCheckedChange = { cameraAndMicPrivacyConsent = it },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = LingoTokens.accentPrimary
                  ),
                  modifier = Modifier.testTag("camera_privacy_consent_switch")
                )
              }
            }
          }
        }
      }

      if (errorMessage != null) {
        item {
          LingoEmptyOrErrorCard(
            title = "Scene Analysis Unavailable",
            message = errorMessage!!,
            isError = true,
            actionLabel = if (capturedBitmap != null) "Retry" else null,
            onAction = capturedBitmap?.let { bmp -> { analyzeSceneBitmap(bmp) } }
          )
        }
      }

      val result = cameraResult
      if (result != null) {
        item {
          LingoCleanCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = result.sceneTitle,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = LingoTokens.textPrimary
              )
              Text(
                text = result.sceneDescription,
                fontSize = 13.sp,
                color = LingoTokens.textSecondary
              )

              FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                FilterChip(
                  selected = selectedResultTab == 0,
                  onClick = { selectedResultTab = 0 },
                  label = { Text("Detected Words (${result.items.size})", fontSize = 12.sp, maxLines = 1, softWrap = false) },
                  colors = FilterChipDefaults.filterChipColors(
                    containerColor = LingoTokens.bgSubtle,
                    labelColor = LingoTokens.textSecondary,
                    selectedContainerColor = LingoTokens.accentPrimary.copy(alpha = 0.16f),
                    selectedLabelColor = LingoTokens.accentPrimary
                  ),
                  modifier = Modifier.testTag("camera_tab_words")
                )
                FilterChip(
                  selected = selectedResultTab == 1,
                  onClick = { selectedResultTab = 1 },
                  label = { Text("Describe & Q&A (${speakWhatYouSeeQuestions.size})", fontSize = 12.sp, maxLines = 1, softWrap = false) },
                  colors = FilterChipDefaults.filterChipColors(
                    containerColor = LingoTokens.bgSubtle,
                    labelColor = LingoTokens.textSecondary,
                    selectedContainerColor = LingoTokens.accentPrimary.copy(alpha = 0.16f),
                    selectedLabelColor = LingoTokens.accentPrimary
                  ),
                  modifier = Modifier.testTag("camera_tab_swys")
                )
              }
            }
          }
        }

        // Grounded "Describe What You See" & Follow-Up Speaking/Typing Practice Card
        aiSceneAnalysis?.let { sceneAnalysis ->
          item {
            LingoCleanCard(
              borderColor = LingoTokens.accentBlue.copy(alpha = 0.4f),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("camera_describe_scene_card")
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                  text = "Describe What You See (${targetLang.name})",
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = LingoTokens.textPrimary
                )
                Text(
                  text = sceneAnalysis.describePromptNative,
                  fontSize = 12.5.sp,
                  color = LingoTokens.textSecondary
                )
                if (sceneAnalysis.visibleObjects.isNotEmpty()) {
                  Text(
                    text = "Visible in photo: ${sceneAnalysis.visibleObjects.joinToString(" · ")}",
                    fontSize = 12.sp,
                    color = LingoTokens.accentPrimary
                  )
                }

                OutlinedTextField(
                  value = describeDraft,
                  onValueChange = { describeDraft = it },
                  placeholder = {
                    Text(
                      "Speak or type your description or a follow-up question in ${targetLang.name}...",
                      fontSize = 12.5.sp,
                      color = LingoTokens.textMuted
                    )
                  },
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LingoTokens.accentPrimary,
                    unfocusedBorderColor = LingoTokens.borderSubtle,
                    focusedTextColor = LingoTokens.textPrimary,
                    unfocusedTextColor = LingoTokens.textPrimary
                  ),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("camera_describe_input")
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  androidx.compose.material3.OutlinedButton(
                    onClick = {
                      if (isListening && activeExerciseIdForMic == "camera_describe_photo") {
                        speechManager.stopListening()
                      } else {
                        activeExerciseIdForMic = "camera_describe_photo"
                        speechManager.clearSpokenText()
                        val hasMic = ContextCompat.checkSelfPermission(
                          context,
                          Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasMic) speechManager.startListening(targetLang.code)
                        else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                      }
                    },
                    border = BorderStroke(1.dp, LingoTokens.borderFocus),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("camera_describe_mic_btn")
                  ) {
                    Icon(
                      if (isListening && activeExerciseIdForMic == "camera_describe_photo") Icons.Default.Stop else Icons.Default.Mic,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      if (isListening && activeExerciseIdForMic == "camera_describe_photo") "Stop" else "Speak",
                      maxLines = 1,
                      softWrap = false
                    )
                  }

                  androidx.compose.material3.Button(
                    onClick = { submitPhotoDescription(isFollowUp = followUpHistory.isNotEmpty()) },
                    enabled = describeDraft.isNotBlank() && !isEvaluatingDescription,
                    colors = ButtonDefaults.buttonColors(
                      containerColor = LingoTokens.accentBlue,
                      contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.3f).testTag("camera_describe_submit_btn")
                  ) {
                    if (isEvaluatingDescription) {
                      CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                      Text("Evaluate", maxLines = 1, softWrap = false)
                    }
                  }
                }

                if (descriptionError != null) {
                  LingoEmptyOrErrorCard(
                    title = "Evaluation Unavailable",
                    message = descriptionError!!,
                    isError = true,
                    actionLabel = "Retry",
                    onAction = { submitPhotoDescription(isFollowUp = followUpHistory.isNotEmpty()) }
                  )
                }

                descriptionFeedback?.let { fb ->
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LingoTokens.bgSubtle,
                    border = BorderStroke(1.dp, LingoTokens.accentEmerald.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(
                      modifier = Modifier.padding(12.dp),
                      verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                      ) {
                        LingoStatusBadge("Scene Match ${fb.relevanceScore}%", LingoTokens.accentEmerald)
                        LingoStatusBadge("Grammar ${fb.grammarScore}%", LingoTokens.accentPrimary)
                      }
                      Text(fb.groundedAssessment, fontSize = 12.5.sp, color = LingoTokens.textPrimary)
                      if (fb.improvedDescriptionTarget.isNotBlank()) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.SpaceBetween,
                          modifier = Modifier.fillMaxWidth()
                        ) {
                          Text(
                            text = "Natural version: ${fb.improvedDescriptionTarget}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LingoTokens.accentEmerald,
                            modifier = Modifier.weight(1f)
                          )
                          IconButton(
                            onClick = { speechManager.speak(fb.improvedDescriptionTarget, targetLang.code) },
                            modifier = Modifier.size(40.dp)
                          ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Hear natural version", tint = LingoTokens.accentPrimary)
                          }
                        }
                      }
                      if (fb.followUpQuestionTarget.isNotBlank()) {
                        Text(
                          text = "Follow-up: ${fb.followUpQuestionTarget}",
                          fontSize = 13.sp,
                          fontWeight = FontWeight.Bold,
                          color = LingoTokens.accentPrimary
                        )
                        if (fb.followUpQuestionNative.isNotBlank()) {
                          Text(fb.followUpQuestionNative, fontSize = 12.sp, color = LingoTokens.textSecondary)
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        if (selectedResultTab == 1) {
          if (speakWhatYouSeeQuestions.isEmpty()) {
            item {
              LingoEmptyOrErrorCard(
                title = "No Q&A Prompts Yet",
                message = "Try analyzing a clearer photo of an object, menu, or street sign to unlock contextual speaking questions."
              )
            }
          } else {
            item {
              LingoCleanCard(
                borderColor = LingoTokens.accentPrimary.copy(alpha = 0.45f),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("speak_what_you_see_section_card")
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                  Text(
                    text = "Speak What You See • Contextual Q&A",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp,
                    color = LingoTokens.accentPrimary
                  )
                  Text(
                    text = "Answer natural questions about the scene. AI evaluates grammar, phrasing, and vocabulary.",
                    fontSize = 12.5.sp,
                    color = LingoTokens.textSecondary
                  )

                  speakWhatYouSeeQuestions.forEachIndexed { qIdx, question ->
                    val qInput = swysAnswerInputs[question.id].orEmpty()
                    val qEval = swysEvaluations[question.id]
                    val micKey = "swys_${question.id}"
                    val isMicListeningForQ = isListening && activeExerciseIdForMic == micKey

                    Surface(
                      shape = RoundedCornerShape(14.dp),
                      color = LingoTokens.bgSubtle,
                      border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("swys_question_card_$qIdx")
                    ) {
                      Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          LingoStatusBadge(
                            text = "Q${qIdx + 1} • ${question.detectedObject} (${question.detectedObjectTranslation})",
                            color = LingoTokens.accentPrimary,
                            modifier = Modifier.weight(1f)
                          )
                          IconButton(
                            onClick = { speechManager.speak(question.questionTargetLanguage, targetLang.code) },
                            modifier = Modifier.size(48.dp)
                          ) {
                            Icon(
                              Icons.AutoMirrored.Filled.VolumeUp,
                              contentDescription = "Hear question",
                              tint = LingoTokens.accentPrimary,
                              modifier = Modifier.size(20.dp)
                            )
                          }
                        }

                        Text(
                          text = question.questionTargetLanguage,
                          fontWeight = FontWeight.Bold,
                          fontSize = 14.5.sp,
                          color = LingoTokens.textPrimary
                        )
                        Text(
                          text = question.questionNativeTranslation,
                          fontSize = 12.5.sp,
                          color = LingoTokens.textSecondary
                        )
                        if (question.suggestedVocabulary.isNotEmpty()) {
                          Text(
                            text = "Useful words: ${question.suggestedVocabulary.joinToString(" • ")}",
                            fontSize = 11.5.sp,
                            color = LingoTokens.accentPrimary
                          )
                        }

                        OutlinedTextField(
                          value = qInput,
                          onValueChange = { swysAnswerInputs = swysAnswerInputs + (question.id to it) },
                          placeholder = {
                            Text(
                              "Speak or type your answer in ${targetLang.name}...",
                              fontSize = 12.5.sp,
                              color = LingoTokens.textMuted
                            )
                          },
                          colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LingoTokens.textPrimary,
                            unfocusedTextColor = LingoTokens.textPrimary,
                            focusedBorderColor = LingoTokens.accentPrimary,
                            unfocusedBorderColor = LingoTokens.borderSubtle,
                            focusedContainerColor = LingoTokens.bgElevated,
                            unfocusedContainerColor = LingoTokens.bgElevated
                          ),
                          shape = RoundedCornerShape(12.dp),
                          modifier = Modifier
                            .fillMaxWidth()
                            .testTag("swys_answer_input_$qIdx"),
                          maxLines = 3
                        )

                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                          OutlinedButton(
                            onClick = {
                              if (isMicListeningForQ) {
                                speechManager.stopListening()
                                activeExerciseIdForMic = null
                              } else {
                                activeExerciseIdForMic = micKey
                                speechManager.clearSpokenText()
                                val hasMic = ContextCompat.checkSelfPermission(
                                  context,
                                  Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasMic) {
                                  speechManager.startListening(targetLang.code)
                                } else {
                                  micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                              }
                            },
                            border = BorderStroke(1.dp, LingoTokens.borderFocus),
                            colors = ButtonDefaults.outlinedButtonColors(
                              contentColor = if (isMicListeningForQ) LingoTokens.accentAmber else LingoTokens.accentPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                          ) {
                            Icon(
                              if (isMicListeningForQ) Icons.Default.Stop else Icons.Default.Mic,
                              contentDescription = null,
                              modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isMicListeningForQ) "Stop" else "Voice", fontSize = 12.sp)
                          }

                          Button(
                            onClick = {
                              evaluatingSwysId = question.id
                              coroutineScope.launch {
                                val evalRes = PracticalFeaturesAiService.evaluateSpeakWhatYouSeeAnswer(
                                  question = question,
                                  spokenAnswer = qInput.trim(),
                                  languageCode = targetLang.code,
                                  nativeLanguage = nativeLang.name
                                )
                                swysEvaluations = swysEvaluations + (question.id to evalRes)
                                evaluatingSwysId = null
                              }
                            },
                            enabled = evaluatingSwysId != question.id && qInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                              containerColor = LingoTokens.accentBlue,
                              contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                              .weight(1f)
                              .testTag("swys_check_answer_btn_$qIdx")
                          ) {
                            Text(
                              if (evaluatingSwysId == question.id) "Checking..." else "Check",
                              fontSize = 12.sp
                            )
                          }
                        }

                        if (qInput.isBlank() && question.sampleAnswerTargetLanguage.isNotBlank()) {
                          TextButton(
                            onClick = {
                              swysAnswerInputs = swysAnswerInputs + (question.id to question.sampleAnswerTargetLanguage)
                            }
                          ) {
                            Text(
                              "Use Sample Answer",
                              color = LingoTokens.accentPrimary,
                              fontSize = 12.sp
                            )
                          }
                        }

                        if (qEval != null) {
                          if (!qEval.aiErrorMessage.isNullOrBlank()) {
                            LingoEmptyOrErrorCard(
                              title = "AI Feedback Unavailable",
                              message = qEval.aiErrorMessage,
                              isError = true
                            )
                          } else {
                            Surface(
                              shape = RoundedCornerShape(12.dp),
                              color = if (!qEval.retryRecommended) LingoTokens.accentEmerald.copy(alpha = 0.14f) else LingoTokens.accentAmber.copy(alpha = 0.14f),
                              modifier = Modifier
                                .fillMaxWidth()
                                .testTag("swys_eval_box_$qIdx")
                            ) {
                              Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                              ) {
                                Text(
                                  text = qEval.grammarAndWordChoiceFeedback,
                                  fontSize = 12.5.sp,
                                  color = LingoTokens.textPrimary
                                )
                                Text(
                                  text = "Polished ${targetLang.name}: ${qEval.correctedSentenceTargetLanguage}",
                                  fontSize = 12.5.sp,
                                  fontWeight = FontWeight.Bold,
                                  color = LingoTokens.accentPrimary
                                )
                                Text(
                                  text = "Follow-up: ${qEval.naturalFollowUpPrompt}",
                                  fontSize = 11.5.sp,
                                  color = LingoTokens.textSecondary
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
            }
          }
        } else {
          if (result.items.isEmpty()) {
            item {
              LingoEmptyOrErrorCard(
                title = "No Objects Detected",
                message = "Try taking a closer, well-lit photo of a menu, sign, or everyday object."
              )
            }
          } else {
            items(result.items.indices.toList()) { index ->
              val item = result.items[index]
              val exercise = speakingExercises.getOrNull(index)
              val isSaved = savedItemsMap[item.nameInTargetLanguage] == true
              val draftText = exercise?.let { exerciseDraftInputs[it.id].orEmpty() }.orEmpty()
              val eval = exercise?.let { exerciseEvaluations[it.id] }
              val isMicActiveForThis = exercise != null && isListening && activeExerciseIdForMic == exercise.id

              LingoCleanCard(
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("detected_vocab_item_${item.detectedObjectOrText}")
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = item.detectedObjectOrText,
                        fontSize = 12.sp,
                        color = LingoTokens.accentPrimary,
                        fontWeight = FontWeight.SemiBold
                      )
                      Text(
                        text = item.nameInTargetLanguage,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = LingoTokens.textPrimary
                      )
                      if (item.phoneticGuide.isNotBlank()) {
                        Text(
                          text = "/ ${item.phoneticGuide} /",
                          fontSize = 12.5.sp,
                          color = LingoTokens.textSecondary
                        )
                      }
                    }

                    IconButton(
                      onClick = {
                        speechManager.speak(item.nameInTargetLanguage, targetLang.code)
                      },
                      modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(LingoTokens.accentPrimary.copy(alpha = 0.14f))
                    ) {
                      Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Pronounce word",
                        tint = LingoTokens.accentPrimary
                      )
                    }
                  }

                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LingoTokens.bgSubtle,
                    border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(
                      modifier = Modifier.padding(12.dp),
                      verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Text(
                        text = "Meaning: ${item.nativeTranslation}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = LingoTokens.textPrimary
                      )
                      if (item.grammarNote.isNotBlank()) {
                        Text(
                          text = item.grammarNote,
                          fontSize = 12.sp,
                          color = LingoTokens.textSecondary
                        )
                      }
                    }
                  }

                  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Text(
                        text = "Example in Context",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LingoTokens.textSecondary
                      )
                      TextButton(
                        onClick = { speechManager.speak(item.exampleSentenceTarget, targetLang.code) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                      ) {
                        Icon(
                          Icons.AutoMirrored.Filled.VolumeUp,
                          contentDescription = null,
                          tint = LingoTokens.accentPrimary,
                          modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Listen", fontSize = 12.sp, color = LingoTokens.accentPrimary)
                      }
                    }
                    Text(
                      text = item.exampleSentenceTarget,
                      fontSize = 14.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = LingoTokens.textPrimary
                    )
                    Text(
                      text = item.exampleSentenceNative,
                      fontSize = 12.5.sp,
                      color = LingoTokens.textSecondary
                    )
                  }

                  if (exercise != null) {
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = LingoTokens.bgSubtle,
                      border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        Text(
                          text = "Practical Speaking Exercise",
                          fontWeight = FontWeight.Bold,
                          fontSize = 13.sp,
                          color = LingoTokens.accentPrimary
                        )
                        Text(
                          text = exercise.promptInstruction,
                          fontSize = 12.5.sp,
                          color = LingoTokens.textPrimary
                        )

                        OutlinedTextField(
                          value = draftText,
                          onValueChange = { updated ->
                            exerciseDraftInputs = exerciseDraftInputs + (exercise.id to updated)
                          },
                          placeholder = {
                            Text(
                              "Speak or type in ${targetLang.name}...",
                              fontSize = 12.sp,
                              color = LingoTokens.textMuted
                            )
                          },
                          colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LingoTokens.textPrimary,
                            unfocusedTextColor = LingoTokens.textPrimary,
                            focusedBorderColor = LingoTokens.accentPrimary,
                            unfocusedBorderColor = LingoTokens.borderSubtle,
                            focusedContainerColor = LingoTokens.bgElevated,
                            unfocusedContainerColor = LingoTokens.bgElevated
                          ),
                          shape = RoundedCornerShape(10.dp),
                          modifier = Modifier
                            .fillMaxWidth()
                            .testTag("real_life_exercise_input_$index"),
                          singleLine = false,
                          maxLines = 3
                        )

                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                          OutlinedButton(
                            onClick = {
                              if (isMicActiveForThis) {
                                speechManager.stopListening()
                                activeExerciseIdForMic = null
                              } else {
                                activeExerciseIdForMic = exercise.id
                                speechManager.clearSpokenText()
                                val hasMic = ContextCompat.checkSelfPermission(
                                  context,
                                  Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasMic) {
                                  speechManager.startListening(targetLang.code)
                                } else {
                                  micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                              }
                            },
                            border = BorderStroke(1.dp, LingoTokens.borderFocus),
                            colors = ButtonDefaults.outlinedButtonColors(
                              contentColor = if (isMicActiveForThis) LingoTokens.accentAmber else LingoTokens.accentPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                              .weight(1f)
                              .testTag("real_life_speak_mic_btn_$index")
                          ) {
                            Icon(
                              if (isMicActiveForThis) Icons.Default.Stop else Icons.Default.Mic,
                              contentDescription = null,
                              modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                              if (isMicActiveForThis) "Stop" else "Speak",
                              fontSize = 12.sp
                            )
                          }

                          Button(
                            onClick = {
                              evaluatingExerciseId = exercise.id
                              coroutineScope.launch {
                                val ev = GeminiTutorService.evaluateRealLifeSpeakingExercise(
                                  exercise = exercise,
                                  userSpokenTranscript = draftText.trim(),
                                  targetLanguage = targetLang.name
                                )
                                exerciseEvaluations = exerciseEvaluations + (exercise.id to ev)
                                evaluatingExerciseId = null
                              }
                            },
                            enabled = evaluatingExerciseId != exercise.id && draftText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                              containerColor = LingoTokens.accentBlue,
                              contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                              .weight(1f)
                              .testTag("real_life_evaluate_btn_$index")
                          ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                              if (evaluatingExerciseId == exercise.id) "Scoring..." else "Evaluate",
                              fontSize = 12.sp
                            )
                          }
                        }

                        if (draftText.isBlank() && exercise.targetSentenceToSpeak.isNotBlank()) {
                          TextButton(
                            onClick = {
                              exerciseDraftInputs = exerciseDraftInputs + (exercise.id to exercise.targetSentenceToSpeak)
                            }
                          ) {
                            Text(
                              "Use Practice Phrase",
                              color = LingoTokens.accentPrimary,
                              fontSize = 12.sp
                            )
                          }
                        }

                        if (eval != null) {
                          Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (eval.passed) LingoTokens.accentEmerald.copy(alpha = 0.14f) else LingoTokens.accentAmber.copy(alpha = 0.14f),
                            modifier = Modifier
                              .fillMaxWidth()
                              .testTag("real_life_eval_result_$index")
                          ) {
                            Column(
                              modifier = Modifier.padding(10.dp),
                              verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                              Text(
                                text = "Overall: ${eval.overallScore}% • Pronunciation: ${eval.pronunciationScore}% • Context: ${eval.contextualAccuracyScore}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (eval.passed) LingoTokens.accentEmerald else LingoTokens.accentAmber
                              )
                              Text(
                                text = eval.feedbackSummary,
                                fontSize = 12.sp,
                                color = LingoTokens.textPrimary
                              )
                              Text(
                                text = "Native Model: ${eval.improvedNativeVersion}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LingoTokens.accentPrimary
                              )
                            }
                          }
                        }
                      }
                    }
                  }

                  Button(
                    onClick = {
                      onSaveVocabToFlashcard(
                        item.nameInTargetLanguage,
                        item.nativeTranslation,
                        item.phoneticGuide
                      )
                      savedItemsMap = savedItemsMap + (item.nameInTargetLanguage to true)
                    },
                    enabled = !isSaved,
                    colors = ButtonDefaults.buttonColors(
                      containerColor = if (isSaved) LingoTokens.accentEmerald else LingoTokens.accentPrimary,
                      contentColor = if (isSaved) Color.White else LingoTokens.onAccent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("save_camera_vocab_btn")
                  ) {
                    Icon(
                      if (isSaved) Icons.Default.Check else Icons.Default.BookmarkAdd,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isSaved) "Saved to Flashcards" else "Save to Flashcards")
                  }
                }
              }
            }
          }
        }
      }

      if (recentCameraSessions.isNotEmpty()) {
        item {
          LingoCleanCard(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("camera_recent_sessions_card")
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "Recent Photo Sessions (${recentCameraSessions.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = LingoTokens.textPrimary
              )
              recentCameraSessions.take(4).forEach { rec ->
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = LingoTokens.bgSubtle,
                  border = BorderStroke(1.dp, LingoTokens.borderSubtle),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                  ) {
                    Text(
                      text = "${rec.title} • ${rec.cefrLevel}",
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 13.sp,
                      color = LingoTokens.textPrimary
                    )
                    Text(
                      text = rec.summary,
                      fontSize = 12.sp,
                      color = LingoTokens.textSecondary,
                      maxLines = 2
                    )
                  }
                }
              }
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}
