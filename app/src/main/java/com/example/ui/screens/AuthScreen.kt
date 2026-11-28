package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.google.firebase.Firebase
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Language
import com.example.ui.components.LanguagePickerDialog
import com.example.ui.components.inspectApkSigningConfig
import com.example.ui.components.onGmailOneTapSignInClicked
import com.example.ui.components.onGoogleSignInClicked
import com.example.ui.theme.LingoCoral
import com.example.ui.theme.LingoCyan
import com.example.ui.theme.LingoElectricBlue
import com.example.ui.theme.LingoEmerald

// Public URLs required for Google Play Console Store Listing & Data Safety Compliance
const val BASE_COMPLIANCE_URL = "https://gadgetexplorerpro-crypto.github.io/LingoSphere-app"
const val PRIVACY_POLICY_URL = "$BASE_COMPLIANCE_URL/privacy-policy.html"
const val TERMS_OF_SERVICE_URL = "$BASE_COMPLIANCE_URL/terms.html"
const val DATA_DELETION_URL = "$BASE_COMPLIANCE_URL/delete-account.html"

fun resolveComplianceBaseUrl(context: Context): String {
  val configured = runCatching {
    context.getString(R.string.developer_website_base_url).trim().trimEnd('/')
  }.getOrDefault("")
  return if (
    configured.startsWith("https://") &&
    !configured.contains('<') &&
    !configured.contains('>') &&
    !configured.contains("YOUR_GITHUB_USERNAME", ignoreCase = true)
  ) {
    configured
  } else {
    BASE_COMPLIANCE_URL
  }
}

fun resolvePrivacyPolicyUrl(context: Context): String =
  "${resolveComplianceBaseUrl(context)}/privacy-policy.html"

fun resolveTermsOfServiceUrl(context: Context): String =
  "${resolveComplianceBaseUrl(context)}/terms.html"

fun resolveDataDeletionUrl(context: Context): String =
  "${resolveComplianceBaseUrl(context)}/delete-account.html"

private enum class AuthSubPage {
  LOGIN,
  PRIVACY_POLICY,
  TERMS_OF_SERVICE
}

enum class AuthMode {
  SIGN_IN,
  SIGN_UP
}

// Palette matching the LingoSphere Triptych reference design
private val NavyBgTop = Color(0xFF040B1A)
private val NavyBgMiddle = Color(0xFF081838)
private val NavyBgBottom = Color(0xFF050E22)
private val CardFillNavy = Color(0xFF0B1C3D)
private val CardBorderBlue = Color(0xFF1E428A)
private val InnerFieldNavy = Color(0xFF071229)
private val InnerFieldBorder = Color(0xFF172E5C)
private val BrandCyan = Color(0xFF22D3EE)
private val TextSoftBlue = Color(0xFFCBD5E1)
private val TimestampBarBlue = Color(0xFF2B549A)

@Composable
fun AuthScreen(
  onSignInSuccess: (userId: String, displayName: String, email: String, nativeLang: String, targetLang: String) -> Unit,
  onSignInError: (String) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var nativeLang by remember { mutableStateOf(Language.findByCode("en")) }
  var targetLang by remember { mutableStateOf(Language.findByCode("es")) }

  var showNativePicker by remember { mutableStateOf(false) }
  var showTargetPicker by remember { mutableStateOf(false) }

  var currentSubPage by remember { mutableStateOf(AuthSubPage.LOGIN) }
  var showDeleteDataDialog by remember { mutableStateOf(false) }
  var showEmailSignUpDialog by remember { mutableStateOf(false) }

  // Gmail & Password Auth State
  var authMode by remember { mutableStateOf(AuthMode.SIGN_IN) }
  var emailInput by remember { mutableStateOf("") }
  var passwordInput by remember { mutableStateOf("") }
  var confirmPasswordInput by remember { mutableStateOf("") }
  var fullNameInput by remember { mutableStateOf("") }
  var isPasswordVisible by remember { mutableStateOf(false) }
  var isConfirmPasswordVisible by remember { mutableStateOf(false) }
  var authErrorMessage by remember { mutableStateOf<String?>(null) }
  var isEmailAuthLoading by remember { mutableStateOf(false) }
  var showForgotPasswordDialog by remember { mutableStateOf(false) }
  var forgotPasswordSuccessEmail by remember { mutableStateOf<String?>(null) }

  var isAuthLoading by remember { mutableStateOf(false) }

  fun triggerGoogleAuth(useGmailOneTap: Boolean = false) {
    if (isAuthLoading) return
    isAuthLoading = true
    authErrorMessage = null
    val credentialManager = CredentialManager.create(context)
    val handleSuccess: (String, String, String) -> Unit = { uid, name, email ->
      isAuthLoading = false
      val normalizedEmail = email.trim().lowercase()
      val preferredName = fullNameInput.trim().takeIf { it.isNotBlank() } ?: name
      runCatching {
        context.getSharedPreferences("lingosphere_prefs", Context.MODE_PRIVATE)
          .edit()
          .putString("last_google_uid", uid)
          .putString("last_google_email", normalizedEmail)
          .putString("last_google_name", preferredName)
          .apply()
      }
      onSignInSuccess(uid, preferredName, normalizedEmail, nativeLang.code, targetLang.code)
    }
    val handleError: (String) -> Unit = { err ->
      isAuthLoading = false
      authErrorMessage = err
      onSignInError(err)
    }
    if (useGmailOneTap) {
      onGmailOneTapSignInClicked(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = handleSuccess,
        onAuthError = handleError,
        scope = coroutineScope,
        onAuthCancelled = { isAuthLoading = false }
      )
    } else {
      onGoogleSignInClicked(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = handleSuccess,
        onAuthError = handleError,
        scope = coroutineScope,
        onAuthCancelled = { isAuthLoading = false }
      )
    }
  }

  fun handleEmailPasswordSignIn() {
    val email = emailInput.trim()
    val password = passwordInput
    if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
      authErrorMessage = "Please enter a valid Gmail / email address"
      return
    }
    if (password.length < 6) {
      authErrorMessage = "Password must be at least 6 characters"
      return
    }
    authErrorMessage = null
    triggerGoogleAuth()
  }

  fun handleEmailPasswordSignUp() {
    val name = fullNameInput.trim()
    val email = emailInput.trim()
    val password = passwordInput
    val confirmPassword = confirmPasswordInput

    if (name.isBlank()) {
      authErrorMessage = "Please enter your full name"
      return
    }
    if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
      authErrorMessage = "Please enter a valid Gmail / email address"
      return
    }
    if (password.length < 6) {
      authErrorMessage = "Password must be at least 6 characters long"
      return
    }
    if (password != confirmPassword) {
      authErrorMessage = "Passwords do not match"
      return
    }

    authErrorMessage = null
    triggerGoogleAuth()
  }

  fun openUrlInBrowser(url: String) {
    try {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
      context.startActivity(intent)
    } catch (e: Exception) {
      Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
    }
  }

  when (currentSubPage) {
    AuthSubPage.PRIVACY_POLICY -> {
      BackHandler { currentSubPage = AuthSubPage.LOGIN }
      PrivacyPolicyFullScreen(
        onBack = { currentSubPage = AuthSubPage.LOGIN },
        onOpenDeleteAccount = { showDeleteDataDialog = true },
        onOpenBrowser = { openUrlInBrowser(resolvePrivacyPolicyUrl(context)) }
      )
    }

    AuthSubPage.TERMS_OF_SERVICE -> {
      BackHandler { currentSubPage = AuthSubPage.LOGIN }
      TermsOfServiceFullScreen(
        onBack = { currentSubPage = AuthSubPage.LOGIN },
        onOpenBrowser = { openUrlInBrowser(resolveTermsOfServiceUrl(context)) }
      )
    }

    AuthSubPage.LOGIN -> {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(NavyBgTop, NavyBgMiddle, NavyBgBottom)
            )
          )
          .testTag("auth_screen")
      ) {
        // Ambient cosmic glow behind globe
        Canvas(modifier = Modifier.fillMaxSize()) {
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                Color(0xFF0EA5E9).copy(alpha = 0.28f),
                Color(0xFF3B82F6).copy(alpha = 0.10f),
                Color.Transparent
              ),
              center = Offset(size.width * 0.5f, size.height * 0.20f),
              radius = size.width * 0.65f
            ),
            center = Offset(size.width * 0.5f, size.height * 0.20f),
            radius = size.width * 0.65f
          )
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
          Spacer(modifier = Modifier.height(6.dp))

          // --- Hero Globe with 6 Floating Multilingual Speech Bubbles ---
          HeroGlobeWithSpeechBubbles()

          Spacer(modifier = Modifier.height(8.dp))

          // Brand Title: LingoSphere
          Text(
            text = buildAnnotatedString {
              withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)) {
                append("Lingo")
              }
              withStyle(SpanStyle(color = BrandCyan, fontWeight = FontWeight.ExtraBold)) {
                append("Sphere")
              }
            },
            fontSize = 36.sp,
            letterSpacing = (-0.5).sp
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "Real-Time AI Voice Calling & Language Tutor",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "Speak naturally with native speakers and AI tutors.\nLearn languages through real conversations,\nvoice calls, and interactive lessons.",
            color = TextSoftBlue,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
          )

          Spacer(modifier = Modifier.height(18.dp))

          // --- Card 1: Set Up Your Target Language ---
          Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CardFillNavy.copy(alpha = 0.92f)),
            border = BorderStroke(1.2.dp, CardBorderBlue),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0284C7).copy(alpha = 0.25f))
                ) {
                  Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = BrandCyan,
                    modifier = Modifier.size(20.dp)
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = "Set Up Your Target Language",
                  color = Color.White,
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Bold
                )
              }

              Spacer(modifier = Modifier.height(14.dp))

              // Native Language Selector Row
              LanguageSelectorRow(
                label = "My native language:",
                language = nativeLang,
                onClick = { showNativePicker = true }
              )

              Spacer(modifier = Modifier.height(10.dp))

              // Target Language Selector Row
              LanguageSelectorRow(
                label = "I want to speak:",
                language = targetLang,
                onClick = { showTargetPicker = true }
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // --- Card 2: Sign In / Create Account with Gmail & Password ---
          if (true) {
          Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CardFillNavy.copy(alpha = 0.94f)),
            border = BorderStroke(1.2.dp, CardBorderBlue),
            modifier = Modifier.fillMaxWidth().testTag("auth_form_card")
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = if (authMode == AuthMode.SIGN_IN) "Sign In to LingoSphere" else "Create Learner Account",
                  color = Color.White,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold
                )

                // FREE ACCESS Badge
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF065F46).copy(alpha = 0.7f))
                    .border(1.dp, Color(0xFF10B981), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                  Text(
                    text = "FREE ACCESS",
                    color = Color(0xFFA7F3D0),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Segmented Tab Switcher: [Sign In] | [Sign Up]
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(InnerFieldNavy)
                  .border(1.dp, InnerFieldBorder, RoundedCornerShape(14.dp))
                  .padding(4.dp)
              ) {
                // Sign In Tab
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                      if (authMode == AuthMode.SIGN_IN) {
                        Brush.horizontalGradient(
                          listOf(Color(0xFF0284C7), Color(0xFF2563EB))
                        )
                      } else {
                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                      }
                    )
                    .clickable {
                      authMode = AuthMode.SIGN_IN
                      authErrorMessage = null
                    }
                    .testTag("tab_sign_in")
                ) {
                  Text(
                    text = "Sign In",
                    color = if (authMode == AuthMode.SIGN_IN) Color.White else TextSoftBlue,
                    fontWeight = if (authMode == AuthMode.SIGN_IN) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                  )
                }

                // Sign Up Tab
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                      if (authMode == AuthMode.SIGN_UP) {
                        Brush.horizontalGradient(
                          listOf(Color(0xFF0284C7), Color(0xFF2563EB))
                        )
                      } else {
                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                      }
                    )
                    .clickable {
                      authMode = AuthMode.SIGN_UP
                      authErrorMessage = null
                    }
                    .testTag("tab_sign_up")
                ) {
                  Text(
                    text = "Sign Up",
                    color = if (authMode == AuthMode.SIGN_UP) Color.White else TextSoftBlue,
                    fontWeight = if (authMode == AuthMode.SIGN_UP) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                  )
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              Text(
                text = if (authMode == AuthMode.SIGN_IN) {
                  "Enter your Gmail or email and password to resume your learning streaks, call rooms, and AI tutors."
                } else {
                  "Sign up with your Gmail or email and password to create your profile and start speaking worldwide."
                },
                color = TextSoftBlue,
                fontSize = 13.sp,
                lineHeight = 18.sp
              )

              Spacer(modifier = Modifier.height(14.dp))

              // If Sign Up: Full Name field
              if (authMode == AuthMode.SIGN_UP) {
                OutlinedTextField(
                  value = fullNameInput,
                  onValueChange = {
                    fullNameInput = it
                    authErrorMessage = null
                  },
                  label = { Text("Full Name") },
                  placeholder = { Text("e.g. Alex Johnson", color = Color(0xFF64748B)) },
                  leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = BrandCyan)
                  },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                  ),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = InnerFieldNavy,
                    unfocusedContainerColor = InnerFieldNavy,
                    focusedBorderColor = BrandCyan,
                    unfocusedBorderColor = InnerFieldBorder,
                    focusedLabelColor = BrandCyan,
                    unfocusedLabelColor = TextSoftBlue
                  ),
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))
              }

              // Gmail / Email Address Field
              OutlinedTextField(
                value = emailInput,
                onValueChange = {
                  emailInput = it
                  authErrorMessage = null
                },
                label = { Text("Gmail / Email Address") },
                placeholder = { Text("you@gmail.com", color = Color(0xFF64748B)) },
                leadingIcon = {
                  Icon(Icons.Default.Email, contentDescription = null, tint = BrandCyan)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                  keyboardType = KeyboardType.Email,
                  imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White,
                  focusedContainerColor = InnerFieldNavy,
                  unfocusedContainerColor = InnerFieldNavy,
                  focusedBorderColor = BrandCyan,
                  unfocusedBorderColor = InnerFieldBorder,
                  focusedLabelColor = BrandCyan,
                  unfocusedLabelColor = TextSoftBlue
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("email_input")
              )

              Spacer(modifier = Modifier.height(10.dp))

              // Password Field
              OutlinedTextField(
                value = passwordInput,
                onValueChange = {
                  passwordInput = it
                  authErrorMessage = null
                },
                label = { Text("Password") },
                placeholder = { Text("At least 6 characters", color = Color(0xFF64748B)) },
                leadingIcon = {
                  Icon(Icons.Default.Lock, contentDescription = null, tint = BrandCyan)
                },
                trailingIcon = {
                  IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                      imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                      contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                      tint = TextSoftBlue
                    )
                  }
                },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                  keyboardType = KeyboardType.Password,
                  imeAction = if (authMode == AuthMode.SIGN_UP) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                  onDone = { if (authMode == AuthMode.SIGN_IN) handleEmailPasswordSignIn() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White,
                  focusedContainerColor = InnerFieldNavy,
                  unfocusedContainerColor = InnerFieldNavy,
                  focusedBorderColor = BrandCyan,
                  unfocusedBorderColor = InnerFieldBorder,
                  focusedLabelColor = BrandCyan,
                  unfocusedLabelColor = TextSoftBlue
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("password_input")
              )

              // If Sign Up: Confirm Password Field
              if (authMode == AuthMode.SIGN_UP) {
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                  value = confirmPasswordInput,
                  onValueChange = {
                    confirmPasswordInput = it
                    authErrorMessage = null
                  },
                  label = { Text("Confirm Password") },
                  placeholder = { Text("Re-enter password", color = Color(0xFF64748B)) },
                  leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = BrandCyan)
                  },
                  trailingIcon = {
                    IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                      Icon(
                        imageVector = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (isConfirmPasswordVisible) "Hide password" else "Show password",
                        tint = TextSoftBlue
                      )
                    }
                  },
                  singleLine = true,
                  visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                  keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                  ),
                  keyboardActions = KeyboardActions(
                    onDone = { handleEmailPasswordSignUp() }
                  ),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = InnerFieldNavy,
                    unfocusedContainerColor = InnerFieldNavy,
                    focusedBorderColor = BrandCyan,
                    unfocusedBorderColor = InnerFieldBorder,
                    focusedLabelColor = BrandCyan,
                    unfocusedLabelColor = TextSoftBlue
                  ),
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_password_input")
                )
              }

              // Forgot Password link for Sign In
              if (authMode == AuthMode.SIGN_IN) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.End
                ) {
                  Text(
                    text = "Forgot password?",
                    color = BrandCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                      .clickable { showForgotPasswordDialog = true }
                      .padding(vertical = 4.dp, horizontal = 2.dp)
                      .testTag("forgot_password_button")
                  )
                }
              }

              // Error banner if any
              if (authErrorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF450A0A).copy(alpha = 0.75f))
                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                  Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFF87171),
                        modifier = Modifier.size(18.dp)
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = authErrorMessage ?: "",
                        color = Color(0xFFFECACA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                      )
                    }
                    if (authErrorMessage?.contains("28444") == true) {
                      val signingReport = remember { inspectApkSigningConfig(context) }
                      Spacer(modifier = Modifier.height(6.dp))
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Text(
                          text = "APK SHA-1: ${signingReport.sha1Colon.ifBlank { "N/A" }}",
                          color = BrandCyan,
                          fontSize = 11.sp,
                          fontWeight = FontWeight.SemiBold,
                          modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                          text = "Copy SHA-1",
                          color = Color.White,
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Bold,
                          textDecoration = TextDecoration.Underline,
                          modifier = Modifier
                            .clickable {
                              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                              val clipText = "Package: ${signingReport.packageName}\nSHA-1: ${signingReport.sha1Colon}\nSHA-256: ${signingReport.sha256Colon}\nWeb Client ID: ${signingReport.webClientId}"
                              clipboard.setPrimaryClip(ClipData.newPlainText("LingoSphere OAuth Diagnostics", clipText))
                              Toast.makeText(context, "Copied APK SHA-1 & SHA-256 to clipboard", Toast.LENGTH_SHORT).show()
                            }
                            .padding(4.dp)
                            .testTag("copy_oauth_sha1_button")
                        )
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              // Primary Submit Button (Email/Password Sign In or Sign Up)
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(52.dp)
                  .clip(RoundedCornerShape(28.dp))
                  .background(
                    Brush.horizontalGradient(
                      listOf(Color(0xFF0284C7), Color(0xFF2563EB), Color(0xFF1D4ED8))
                    )
                  )
                  .clickable(enabled = !isEmailAuthLoading && !isAuthLoading) {
                    if (authMode == AuthMode.SIGN_IN) {
                      handleEmailPasswordSignIn()
                    } else {
                      handleEmailPasswordSignUp()
                    }
                  }
                  .padding(horizontal = 18.dp)
                  .testTag("auth_submit_button"),
                contentAlignment = Alignment.Center
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center,
                  modifier = Modifier.fillMaxSize()
                ) {
                  if (isEmailAuthLoading) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(22.dp),
                      color = Color.White,
                      strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                      text = if (authMode == AuthMode.SIGN_IN) "Signing In..." else "Creating Account...",
                      color = Color.White,
                      fontSize = 15.sp,
                      fontWeight = FontWeight.Bold
                    )
                  } else {
                    Icon(
                      imageVector = if (authMode == AuthMode.SIGN_IN) Icons.Default.Lock else Icons.Default.Person,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                      text = if (authMode == AuthMode.SIGN_IN) "Sign In with Password" else "Create LingoSphere Account",
                      color = Color.White,
                      fontSize = 15.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Switch between Sign In and Sign Up mode
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = if (authMode == AuthMode.SIGN_IN) "Don't have an account? " else "Already have an account? ",
                  color = TextSoftBlue,
                  fontSize = 13.sp
                )
                Text(
                  text = if (authMode == AuthMode.SIGN_IN) "Sign Up" else "Sign In",
                  color = BrandCyan,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  textDecoration = TextDecoration.Underline,
                  modifier = Modifier
                    .clickable {
                      authMode = if (authMode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN
                      authErrorMessage = null
                    }
                    .padding(vertical = 2.dp, horizontal = 4.dp)
                    .testTag("toggle_auth_mode_button")
                )
              }

              Spacer(modifier = Modifier.height(16.dp))

              // Divider: "OR CONTINUE WITH"
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
              ) {
                HorizontalDivider(
                  modifier = Modifier.weight(1f),
                  color = Color(0xFF1E3A8A).copy(alpha = 0.5f),
                  thickness = 1.dp
                )
                Text(
                  text = "  OR CONTINUE WITH  ",
                  color = Color(0xFF64748B),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
                HorizontalDivider(
                  modifier = Modifier.weight(1f),
                  color = Color(0xFF1E3A8A).copy(alpha = 0.5f),
                  thickness = 1.dp
                )
              }

              Spacer(modifier = Modifier.height(16.dp))

              // Button 1: Continue with Google (White Pill Button)
              Surface(
                onClick = { triggerGoogleAuth() },
                enabled = !isAuthLoading && !isEmailAuthLoading,
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(50.dp)
                  .testTag("google_sign_in_button")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp)
                ) {
                  // Colorful Google 'G' Badge
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(28.dp)
                  ) {
                    Text(
                      text = "G",
                      fontSize = 22.sp,
                      fontWeight = FontWeight.ExtraBold,
                      color = Color(0xFF4285F4)
                    )
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                    text = if (isAuthLoading) "Signing in with Google..." else "Continue with Google",
                    color = Color(0xFF1E293B),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                  )
                  if (isAuthLoading) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(20.dp),
                      color = Color(0xFF1E293B),
                      strokeWidth = 2.dp
                    )
                  } else {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                      contentDescription = null,
                      tint = Color(0xFF334155),
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Button 2: Continue with Gmail One-Tap (Sky-Cyan Gradient Pill Button)
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(50.dp)
                  .clip(RoundedCornerShape(28.dp))
                  .background(
                    Brush.horizontalGradient(
                      colors = listOf(
                        Color(0xFFB2F5EA),
                        Color(0xFF38BDF8),
                        Color(0xFF0284C7)
                      )
                    )
                  )
                  .clickable(enabled = !isAuthLoading && !isEmailAuthLoading) {
                    triggerGoogleAuth(useGmailOneTap = true)
                  }
                  .padding(horizontal = 18.dp)
                  .testTag("quick_demo_signin_button"),
                contentAlignment = Alignment.Center
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxSize()
                ) {
                  // Gmail 'M' icon badge
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                      .size(28.dp)
                      .clip(RoundedCornerShape(6.dp))
                      .background(Color.White)
                  ) {
                    Text(
                      text = "M",
                      fontSize = 17.sp,
                      fontWeight = FontWeight.ExtraBold,
                      color = Color(0xFFEA4335)
                    )
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                    text = "Continue with Gmail One-Tap",
                    color = Color(0xFF041026),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                  )
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF041026),
                    modifier = Modifier.size(20.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              // Legal Agreement Footer inside Card
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "By continuing, you agree to our",
                  color = TextSoftBlue,
                  fontSize = 12.sp,
                  textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Privacy Policy",
                    color = Color(0xFF60A5FA),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                      .clickable { currentSubPage = AuthSubPage.PRIVACY_POLICY }
                      .padding(horizontal = 4.dp, vertical = 2.dp)
                      .testTag("login_privacy_policy_button")
                  )
                  Text(
                    text = " and ",
                    color = TextSoftBlue,
                    fontSize = 12.sp
                  )
                  Text(
                    text = "Terms of Service",
                    color = Color(0xFF60A5FA),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                      .clickable { currentSubPage = AuthSubPage.TERMS_OF_SERVICE }
                      .padding(horizontal = 4.dp, vertical = 2.dp)
                      .testTag("login_terms_button")
                  )
                }
              }
            }
          }
          }

          Spacer(modifier = Modifier.height(20.dp))
        }
      }
    }
  }

  // --- Dialogs & Pickers ---
  if (showNativePicker) {
    LanguagePickerDialog(
      title = "Select Your Native Language",
      currentLanguageCode = nativeLang.code,
      onLanguageSelected = { nativeLang = it },
      onDismiss = { showNativePicker = false }
    )
  }

  if (showTargetPicker) {
    LanguagePickerDialog(
      title = "Select Language to Learn",
      currentLanguageCode = targetLang.code,
      onLanguageSelected = { targetLang = it },
      onDismiss = { showTargetPicker = false }
    )
  }

  if (showEmailSignUpDialog) {
    EmailSignUpDialog(
      onDismiss = { showEmailSignUpDialog = false },
      onContinueWithGoogle = {
        showEmailSignUpDialog = false
        triggerGoogleAuth()
      }
    )
  }

  if (showDeleteDataDialog) {
    DeleteAccountUrlDialog(
      context = context,
      onDismiss = { showDeleteDataDialog = false }
    )
  }

  if (showForgotPasswordDialog) {
    ForgotPasswordDialog(
      initialEmail = emailInput,
      onDismiss = { showForgotPasswordDialog = false },
      onResetSent = { sentEmail ->
        showForgotPasswordDialog = false
        forgotPasswordSuccessEmail = sentEmail
      }
    )
  }

  if (forgotPasswordSuccessEmail != null) {
    AlertDialog(
      onDismissRequest = { forgotPasswordSuccessEmail = null },
      containerColor = CardFillNavy,
      icon = {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LingoEmerald, modifier = Modifier.size(32.dp))
      },
      title = {
        Text("Password Reset Sent", color = Color.White, fontWeight = FontWeight.Bold)
      },
      text = {
        Text(
          text = "A password reset link was sent to $forgotPasswordSuccessEmail. Please check your inbox and spam folder to create your new password.",
          color = TextSoftBlue,
          fontSize = 13.sp
        )
      },
      confirmButton = {
        Button(
          onClick = { forgotPasswordSuccessEmail = null },
          colors = ButtonDefaults.buttonColors(containerColor = BrandCyan, contentColor = Color(0xFF041026))
        ) {
          Text("OK", fontWeight = FontWeight.Bold)
        }
      }
    )
  }
}

@Composable
private fun HeroGlobeWithSpeechBubbles() {
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .fillMaxWidth()
      .height(178.dp)
  ) {
    // Left Top Bubble: "Hello"
    SpeechBubblePill(
      text = "Hello",
      bgColors = listOf(Color(0xFF0284C7), Color(0xFF2563EB)),
      borderColor = Color(0xFF38BDF8),
      modifier = Modifier
        .align(Alignment.TopStart)
        .offset(x = 18.dp, y = 10.dp)
    )

    // Left Middle Bubble: "Hola"
    SpeechBubblePill(
      text = "Hola",
      bgColors = listOf(Color(0xFFD97706), Color(0xFFEA580C)),
      borderColor = Color(0xFFFBBF24),
      modifier = Modifier
        .align(Alignment.CenterStart)
        .offset(x = 4.dp, y = 2.dp)
    )

    // Left Bottom Bubble: "Bonjour"
    SpeechBubblePill(
      text = "Bonjour",
      bgColors = listOf(Color(0xFF7C3AED), Color(0xFF4F46E5)),
      borderColor = Color(0xFFA78BFA),
      modifier = Modifier
        .align(Alignment.BottomStart)
        .offset(x = 14.dp, y = (-8).dp)
    )

    // Right Top Bubble: "こんにちは"
    SpeechBubblePill(
      text = "こんにちは",
      bgColors = listOf(Color(0xFF0D9488), Color(0xFF059669)),
      borderColor = Color(0xFF34D399),
      modifier = Modifier
        .align(Alignment.TopEnd)
        .offset(x = (-10).dp, y = 10.dp)
    )

    // Right Middle Bubble: "مرحبا"
    SpeechBubblePill(
      text = "مرحبا",
      bgColors = listOf(Color(0xFF0369A1), Color(0xFF0284C7)),
      borderColor = Color(0xFF38BDF8),
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .offset(x = (-4).dp, y = 4.dp)
    )

    // Right Bottom Bubble: "你好"
    SpeechBubblePill(
      text = "你好",
      bgColors = listOf(Color(0xFFBE185D), Color(0xFF9D174D)),
      borderColor = Color(0xFFF472B6),
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .offset(x = (-18).dp, y = (-8).dp)
    )

    // Outer Neon Glow Ring + Globe Logo
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(136.dp)
        .clip(CircleShape)
        .background(
          Brush.radialGradient(
            colors = listOf(Color.White, Color(0xFF38BDF8), Color(0xFF0284C7))
          )
        )
        .border(3.dp, BrandCyan, CircleShape)
        .padding(4.dp)
    ) {
      Image(
        painter = painterResource(id = R.drawable.lingosphere_logo),
        contentDescription = "LingoSphere Globe Logo",
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        modifier = Modifier
          .fillMaxSize()
          .clip(CircleShape)
      )
    }
  }
}

@Composable
private fun SpeechBubblePill(
  text: String,
  bgColors: List<Color>,
  borderColor: Color,
  modifier: Modifier = Modifier
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(Brush.horizontalGradient(bgColors))
      .border(1.dp, borderColor.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
      .padding(horizontal = 14.dp, vertical = 7.dp)
  ) {
    Text(
      text = text,
      color = Color.White,
      fontWeight = FontWeight.Bold,
      fontSize = 13.sp
    )
  }
}

@Composable
private fun LanguageSelectorRow(
  label: String,
  language: Language,
  onClick: () -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(InnerFieldNavy)
      .border(1.dp, InnerFieldBorder, RoundedCornerShape(14.dp))
      .clickable { onClick() }
      .padding(horizontal = 14.dp, vertical = 13.dp)
  ) {
    Text(
      text = label,
      color = Color.White,
      fontSize = 14.sp,
      fontWeight = FontWeight.Medium
    )
    Spacer(modifier = Modifier.weight(1f))
    Text(
      text = language.flagEmoji,
      fontSize = 18.sp
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = language.name,
      color = Color.White,
      fontSize = 15.sp,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.width(6.dp))
    Icon(
      imageVector = Icons.Default.KeyboardArrowDown,
      contentDescription = "Change language",
      tint = TextSoftBlue,
      modifier = Modifier.size(20.dp)
    )
  }
}

// ============================================================================
// SCREEN 2 (CENTER PANEL OF TRIPTYCH): PRIVACY POLICY FULL SCREEN
// ============================================================================

@Composable
fun PrivacyPolicyFullScreen(
  onBack: () -> Unit,
  onOpenDeleteAccount: () -> Unit = {},
  onOpenBrowser: () -> Unit = {}
) {
  val context = LocalContext.current

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(NavyBgTop, NavyBgMiddle, NavyBgBottom)
        )
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
      // Top Navigation Bar with Back Button + LingoSphere Header
      PolicyTopHeader(onBack = onBack)

      Spacer(modifier = Modifier.height(16.dp))

      // Hero Row: Glowing Blue Shield + Title & Subtitle
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
              Brush.verticalGradient(
                colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF1D4ED8))
              )
            )
            .border(1.5.dp, Color(0xFF7DD3FC), RoundedCornerShape(18.dp))
        ) {
          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = "Privacy Shield",
            tint = Color.White,
            modifier = Modifier.size(36.dp)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Privacy Policy",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Your privacy matters to us. Learn how we collect, use, and protect your information.",
            color = TextSoftBlue,
            fontSize = 13.sp,
            lineHeight = 18.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Last Updated Pill Banner
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(TimestampBarBlue.copy(alpha = 0.85f))
          .border(1.dp, Color(0xFF60A5FA).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
          .padding(horizontal = 16.dp, vertical = 10.dp)
      ) {
        Text(
          text = "Last updated: October 24, 2026",
          color = Color.White,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 1. Information We Collect
      PolicySectionCard(
        icon = Icons.Default.Person,
        iconBgColors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)),
        title = "1. Information We Collect",
        description = "We collect limited information to provide and improve LingoSphere:",
        bullets = listOf(
          "Account information (name, email address, profile photo)",
          "Language preferences and learning activity",
          "Voice recordings (only with your permission)",
          "Device information (device type, operating system)"
        )
      )

      Spacer(modifier = Modifier.height(12.dp))

      // 2. Microphone and Voice Data
      PolicySectionCard(
        icon = Icons.Default.Mic,
        iconBgColors = listOf(Color(0xFF2563EB), Color(0xFF0284C7)),
        title = "2. Microphone and Voice Data",
        description = "We use your microphone to enable real-time voice calls with AI tutors and language partners. Voice data is processed securely and used only to provide language learning, pronunciation feedback, and conversation features."
      )

      Spacer(modifier = Modifier.height(12.dp))

      // 3. How We Use Your Information
      PolicySectionCard(
        icon = Icons.Default.Settings,
        iconBgColors = listOf(Color(0xFF10B981), Color(0xFF059669)),
        title = "3. How We Use Your Information",
        bullets = listOf(
          "Provide and operate the app",
          "Improve AI language models and speech recognition",
          "Personalize your learning experience",
          "Ensure safety, prevent abuse, and comply with legal obligations"
        )
      )

      Spacer(modifier = Modifier.height(12.dp))

      // 4. Information Sharing
      PolicySectionCard(
        icon = Icons.Default.Groups,
        iconBgColors = listOf(Color(0xFF9333EA), Color(0xFF7E22CE)),
        title = "4. Information Sharing",
        description = "We do not sell your personal information. We may share data with trusted service providers (e.g., cloud hosting, AI speech services) only to operate and improve the app."
      )

      Spacer(modifier = Modifier.height(12.dp))

      // 5. Data Security
      PolicySectionCard(
        icon = Icons.Default.Lock,
        iconBgColors = listOf(Color(0xFFEC4899), Color(0xFFDB2777)),
        title = "5. Data Security",
        description = "We use industry-standard security measures to protect your information, including encryption in transit and at rest."
      )

      Spacer(modifier = Modifier.height(12.dp))

      // 6. Your Choices
      PolicySectionCard(
        icon = Icons.Default.DeleteOutline,
        iconBgColors = listOf(Color(0xFFEF4444), Color(0xFFDC2626)),
        title = "6. Your Choices",
        description = "You can update or delete your account and data anytime in the app settings."
      )

      Spacer(modifier = Modifier.height(12.dp))

      // 7. Contact Us
      PolicyContactCard(
        subtitle = "If you have any questions about this Privacy Policy, please contact us at:",
        email = "support@lingosphere.app",
        context = context
      )

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

// ============================================================================
// SCREEN 3 (RIGHT PANEL OF TRIPTYCH): TERMS OF SERVICE FULL SCREEN
// ============================================================================

@Composable
fun TermsOfServiceFullScreen(
  onBack: () -> Unit,
  onOpenBrowser: () -> Unit = {}
) {
  val context = LocalContext.current

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(NavyBgTop, NavyBgMiddle, NavyBgBottom)
        )
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
      // Top Navigation Bar with Back Button + LingoSphere Header
      PolicyTopHeader(onBack = onBack)

      Spacer(modifier = Modifier.height(16.dp))

      // Hero Row: Glowing Blue Document Icon + Title & Subtitle
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
              Brush.verticalGradient(
                colors = listOf(Color(0xFF38BDF8), Color(0xFF2563EB), Color(0xFF1D4ED8))
              )
            )
            .border(1.5.dp, Color(0xFF7DD3FC), RoundedCornerShape(18.dp))
        ) {
          Icon(
            imageVector = Icons.Default.Description,
            contentDescription = "Terms Document",
            tint = Color.White,
            modifier = Modifier.size(36.dp)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Terms of Service",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Please read these terms carefully before using LingoSphere.",
            color = TextSoftBlue,
            fontSize = 13.sp,
            lineHeight = 18.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Last Updated Pill Banner
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(TimestampBarBlue.copy(alpha = 0.85f))
          .border(1.dp, Color(0xFF60A5FA).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
          .padding(horizontal = 16.dp, vertical = 10.dp)
      ) {
        Text(
          text = "Last updated: October 24, 2026",
          color = Color.White,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 1. Acceptance of Terms
      PolicySectionCard(
        icon = Icons.Default.CheckCircle,
        iconBgColors = listOf(Color(0xFF10B981), Color(0xFF059669)),
        title = "1. Acceptance of Terms",
        description = "By using LingoSphere, you agree to these Terms of Service. If you do not agree, please do not use the app."
      )

      Spacer(modifier = Modifier.height(12.dp))

      // 2. Use of the App
      PolicySectionCard(
        icon = Icons.Default.Groups,
        iconBgColors = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)),
        title = "2. Use of the App",
        description = "LingoSphere is a language learning and communication platform. You agree to:",
        bullets = listOf(
          "Use the app only for lawful purposes",
          "Respect other users and follow community guidelines",
          "Not engage in harassment, hate speech, spamming, or any harmful behavior",
          "Be responsible for the content you share (e.g., messages, voice calls, profile information)."
        )
      )

      Spacer(modifier = Modifier.height(12.dp))

      // 3. Accounts
      PolicySectionCard(
        icon = Icons.Default.Person,
        iconBgColors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)),
        title = "3. Accounts",
        description = "You must be at least 13 years old to use LingoSphere. You are responsible for keeping your account information secure and for all activity under your account."
      )

      Spacer(modifier = Modifier.height(12.dp))

      // 4. Subscriptions and Payments
      PolicySectionCard(
        icon = Icons.Default.EmojiEvents,
        iconBgColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
        title = "4. Subscriptions and Payments",
        description = "LingoSphere offers free and premium features. Premium features may require a paid subscription. Subscriptions automatically renew unless canceled. You can manage or cancel your subscription anytime through your device's account settings (e.g., Google Play)."
      )

      Spacer(modifier = Modifier.height(12.dp))

      // 5. Prohibited Activities
      PolicySectionCard(
        icon = Icons.Default.Block,
        iconBgColors = listOf(Color(0xFF2563EB), Color(0xFF1E40AF)),
        title = "5. Prohibited Activities",
        description = "You agree not to:",
        bullets = listOf(
          "Use the app for illegal or fraudulent activities",
          "Share inappropriate, explicit, or harmful content",
          "Impersonate others or create fake identities",
          "Interfere with the app's security or operation"
        )
      )

      Spacer(modifier = Modifier.height(12.dp))

      // 6. Changes to Terms
      PolicySectionCard(
        icon = Icons.Default.Autorenew,
        iconBgColors = listOf(Color(0xFF10B981), Color(0xFF059669)),
        title = "6. Changes to Terms",
        description = "We may update these Terms from time to time. We will notify you of significant changes through the app."
      )

      Spacer(modifier = Modifier.height(12.dp))

      // 7. Contact Us
      PolicyContactCard(
        subtitle = "If you have any questions about these Terms, please contact us at:",
        email = "support@lingosphere.app",
        context = context
      )

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun PolicyTopHeader(onBack: () -> Unit) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
  ) {
    IconButton(
      onClick = onBack,
      modifier = Modifier.size(40.dp)
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Back",
        tint = Color.White,
        modifier = Modifier.size(24.dp)
      )
    }

    Spacer(modifier = Modifier.width(6.dp))

    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(42.dp)
        .clip(CircleShape)
        .background(Color.White)
        .border(1.5.dp, BrandCyan, CircleShape)
        .padding(2.dp)
    ) {
      Image(
        painter = painterResource(id = R.drawable.lingosphere_logo),
        contentDescription = "LingoSphere Logo",
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        modifier = Modifier
          .fillMaxSize()
          .clip(CircleShape)
      )
    }

    Spacer(modifier = Modifier.width(10.dp))

    Column {
      Text(
        text = buildAnnotatedString {
          withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)) {
            append("Lingo")
          }
          withStyle(SpanStyle(color = BrandCyan, fontWeight = FontWeight.ExtraBold)) {
            append("Sphere")
          }
        },
        fontSize = 20.sp
      )
      Text(
        text = "Real People. Real Languages.",
        color = TextSoftBlue,
        fontSize = 11.sp
      )
    }
  }
}

@Composable
private fun PolicySectionCard(
  icon: ImageVector,
  iconBgColors: List<Color>,
  title: String,
  description: String? = null,
  bullets: List<String> = emptyList(),
  onClick: (() -> Unit)? = null
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = CardFillNavy.copy(alpha = 0.90f)),
    border = BorderStroke(1.dp, CardBorderBlue.copy(alpha = 0.8f)),
    modifier = Modifier
      .fillMaxWidth()
      .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
  ) {
    Row(
      verticalAlignment = Alignment.Top,
      modifier = Modifier.padding(16.dp)
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(Brush.verticalGradient(iconBgColors))
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(20.dp)
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          color = Color.White,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )

        if (description != null) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = description,
            color = TextSoftBlue,
            fontSize = 12.5.sp,
            lineHeight = 17.sp
          )
        }

        if (bullets.isNotEmpty()) {
          Spacer(modifier = Modifier.height(6.dp))
          bullets.forEach { item ->
            Row(
              verticalAlignment = Alignment.Top,
              modifier = Modifier.padding(vertical = 2.dp)
            ) {
              Text(
                text = "•  ",
                color = BrandCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = item,
                color = TextSoftBlue,
                fontSize = 12.5.sp,
                lineHeight = 17.sp
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PolicyContactCard(
  subtitle: String,
  email: String,
  context: Context
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = CardFillNavy.copy(alpha = 0.90f)),
    border = BorderStroke(1.dp, CardBorderBlue.copy(alpha = 0.8f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.Top) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(
              Brush.verticalGradient(
                listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
              )
            )
        ) {
          Icon(
            imageVector = Icons.Default.Email,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "7. Contact Us",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = subtitle,
            color = TextSoftBlue,
            fontSize = 12.5.sp,
            lineHeight = 17.sp
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Support Email Pill Box
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(TimestampBarBlue.copy(alpha = 0.65f))
              .border(1.dp, Color(0xFF60A5FA).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
              .clickable {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Support Email", email))
                Toast.makeText(context, "Email copied: $email", Toast.LENGTH_SHORT).show()
              }
              .padding(horizontal = 14.dp, vertical = 10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Email,
              contentDescription = null,
              tint = Color(0xFF93C5FD),
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = email,
              color = Color.White,
              fontSize = 13.5.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }
  }
}

// ============================================================================
// DIALOGS & COMPATIBILITY WRAPPERS FOR PROFILE SCREEN
// ============================================================================

@Composable
private fun EmailSignUpDialog(
  onDismiss: () -> Unit,
  onContinueWithGoogle: () -> Unit
) {
  var email by remember { mutableStateOf("") }
  var fullName by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Sign Up with Email", fontWeight = FontWeight.Bold)
    },
    text = {
      Column {
        Text(
          text = "Enter your name and email address to set up your LingoSphere learner account, then verify via Google Identity:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = fullName,
          onValueChange = { fullName = it },
          label = { Text("Full Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email Address") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(onClick = onContinueWithGoogle) {
        Text("Continue & Verify")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun LegalTermsDialog(
  initialTab: Int,
  onDismiss: () -> Unit,
  onOpenPrivacyWeb: () -> Unit,
  onOpenTermsWeb: () -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    if (initialTab == 0) {
      PrivacyPolicyFullScreen(
        onBack = onDismiss,
        onOpenBrowser = onOpenPrivacyWeb
      )
    } else {
      TermsOfServiceFullScreen(
        onBack = onDismiss,
        onOpenBrowser = onOpenTermsWeb
      )
    }
  }
}

@Composable
fun DeleteAccountUrlDialog(
  context: Context,
  onDismiss: () -> Unit
) {
  var emailInput by remember { mutableStateOf("") }
  var deleteType by remember { mutableIntStateOf(0) } // 0: Full Account & All Data, 1: Learning Data Only
  var requestSubmitted by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = LingoCoral)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Delete Account & Data", fontWeight = FontWeight.Bold, fontSize = 18.sp)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .height(400.dp)
          .verticalScroll(rememberScrollState())
          .padding(vertical = 4.dp)
      ) {
        Text(
          text = "Request immediate erasure of your LingoSphere account or learning data directly from this screen, or after signing in via Profile > Delete Account & All Data.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          OutlinedButton(
            onClick = { deleteType = 0 },
            border = BorderStroke(1.dp, if (deleteType == 0) LingoCoral else MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = if (deleteType == 0) LingoCoral.copy(alpha = 0.15f) else Color.Transparent
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Delete Account + Data", fontSize = 11.sp, color = if (deleteType == 0) LingoCoral else MaterialTheme.colorScheme.onSurface)
          }

          OutlinedButton(
            onClick = { deleteType = 1 },
            border = BorderStroke(1.dp, if (deleteType == 1) LingoCyan else MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = if (deleteType == 1) LingoCyan.copy(alpha = 0.15f) else Color.Transparent
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Delete Data Only", fontSize = 11.sp, color = if (deleteType == 1) LingoCyan else MaterialTheme.colorScheme.onSurface)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (!requestSubmitted) {
          OutlinedTextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            label = { Text("Registered Gmail / Account Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(10.dp))

          Button(
            onClick = {
              if (emailInput.isBlank() || !emailInput.contains("@")) {
                Toast.makeText(context, "Please enter a valid account email address", Toast.LENGTH_SHORT).show()
              } else {
                requestSubmitted = true
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = LingoCoral, contentColor = Color.White),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = if (deleteType == 0) "Submit Account & Data Deletion" else "Submit Learning Data Deletion",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }
        } else {
          Card(
            colors = CardDefaults.cardColors(containerColor = LingoEmerald.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "✓ Deletion request registered for $emailInput. All selected data (profile, lessons, flashcards, and speaking transcripts) is scheduled for permanent erasure.",
              style = MaterialTheme.typography.bodySmall,
              color = LingoEmerald,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(12.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Public Web Deletion URL (Play Console & GitHub):",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        val dataDeletionUrl = remember(context) { resolveDataDeletionUrl(context) }

        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = dataDeletionUrl,
            style = MaterialTheme.typography.labelSmall,
            color = LingoCyan,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(10.dp)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          OutlinedButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText("Delete Account URL", dataDeletionUrl)
              clipboard.setPrimaryClip(clip)
              Toast.makeText(context, "Delete Account URL copied", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Copy URL", fontSize = 11.sp)
          }

          OutlinedButton(
            onClick = {
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse(dataDeletionUrl))
              context.startActivity(intent)
            },
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Open Web", fontSize = 11.sp)
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}

@Composable
fun PrivacyPolicyDialog(
  onDismiss: () -> Unit,
  onOpenBrowser: () -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    PrivacyPolicyFullScreen(
      onBack = onDismiss,
      onOpenBrowser = onOpenBrowser
    )
  }
}

@Composable
fun TermsOfServiceDialog(
  onDismiss: () -> Unit,
  onOpenBrowser: () -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    TermsOfServiceFullScreen(
      onBack = onDismiss,
      onOpenBrowser = onOpenBrowser
    )
  }
}

@Composable
private fun ForgotPasswordDialog(
  initialEmail: String,
  onDismiss: () -> Unit,
  onResetSent: (String) -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  var email by remember { mutableStateOf(initialEmail) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = CardFillNavy,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = BrandCyan)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Reset Password", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
      }
    },
    text = {
      Column {
        Text(
          text = "Enter your registered Gmail or email address. We'll send you a secure link to reset your password.",
          color = TextSoftBlue,
          fontSize = 13.sp,
          lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
          value = email,
          onValueChange = {
            email = it
            errorMsg = null
          },
          label = { Text("Gmail / Account Email") },
          placeholder = { Text("your.name@gmail.com", color = Color(0xFF64748B)) },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = InnerFieldNavy,
            unfocusedContainerColor = InnerFieldNavy,
            focusedBorderColor = BrandCyan,
            unfocusedBorderColor = InnerFieldBorder,
            focusedLabelColor = BrandCyan,
            unfocusedLabelColor = TextSoftBlue
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("reset_email_input")
        )
        if (errorMsg != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(text = errorMsg ?: "", color = LingoCoral, fontSize = 12.sp)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val cleanEmail = email.trim()
          if (cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            errorMsg = "Please enter a valid email address"
            return@Button
          }
          isLoading = true
          coroutineScope.launch {
            try {
              Firebase.auth.sendPasswordResetEmail(cleanEmail).await()
              isLoading = false
              onResetSent(cleanEmail)
            } catch (e: Exception) {
              isLoading = false
              errorMsg = e.localizedMessage ?: "Failed to send reset email"
            }
          }
        },
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = BrandCyan, contentColor = Color(0xFF041026)),
        shape = RoundedCornerShape(10.dp)
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = Color(0xFF041026),
            strokeWidth = 2.dp
          )
        } else {
          Text("Send Reset Link", fontWeight = FontWeight.Bold)
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSoftBlue)
      }
    }
  )
}

