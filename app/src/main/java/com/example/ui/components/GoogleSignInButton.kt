package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "GoogleSignIn"

/**
 * Registered Android OAuth 2.0 SHA-1 certificate hashes in app/google-services.json
 * for package com.aistudio.lingosphere.kxpqzm (Firebase project gen-lang-client-0662926032).
 */
val REGISTERED_OAUTH_SHA1_HASHES: Set<String> = setOf(
    "8E:26:8B:6F:5E:FD:A8:12:22:38:AE:1F:F9:3B:EA:3B:40:33:81:64",
    "FA:80:C0:E7:C9:15:91:C6:14:DC:C3:FE:D6:94:18:F8:A5:3F:25:4F",
    "1F:2E:9D:24:ED:06:0E:8B:95:96:C8:7B:A4:45:5C:90:83:5B:AE:D7",
    "78:AE:AE:D2:B4:8A:D3:D0:AC:CD:BE:30:D6:67:9A:F5:B6:DC:C7:E1"
)

data class ApkSigningConfigReport(
    val packageName: String,
    val sha1Colon: String,
    val sha1HexLower: String,
    val sha256Colon: String,
    val webClientId: String,
    val isSha1RegisteredInGoogleServices: Boolean,
    val isWebClientConfigured: Boolean
)

/**
 * Inspects the running APK's signing certificate (SHA-1 and SHA-256) and Web OAuth Client ID
 * to diagnose any OAuth [28444] / DEVELOPER_ERROR mismatches on Play-signed or custom-signed builds.
 */
@Suppress("DEPRECATION")
@SuppressLint("PackageManagerGetSignatures")
fun inspectApkSigningConfig(context: Context): ApkSigningConfigReport {
    val pkgName = context.packageName
    val webClientId = runCatching {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", pkgName)
        if (resId != 0) context.getString(resId) else context.getString(R.string.default_web_client_id)
    }.getOrDefault("")

    var sha1Colon = ""
    var sha256Colon = ""

    runCatching {
        val pm = context.packageManager
        val certBytes: ByteArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val pkgInfo = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signingInfo = pkgInfo.signingInfo
            val signatures = if (signingInfo != null) {
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
            } else {
                null
            }
            signatures?.firstOrNull()?.toByteArray()
        } else {
            val pkgInfo = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES)
            pkgInfo.signatures?.firstOrNull()?.toByteArray()
        }

        if (certBytes != null && certBytes.isNotEmpty()) {
            sha1Colon = MessageDigest.getInstance("SHA-1")
                .digest(certBytes)
                .joinToString(":") { "%02X".format(Locale.US, it) }
            sha256Colon = MessageDigest.getInstance("SHA-256")
                .digest(certBytes)
                .joinToString(":") { "%02X".format(Locale.US, it) }
        }
    }.onFailure { e ->
        Log.w(TAG, "Unable to read APK signing certificate at runtime: ${e.message}")
    }

    val normalizedSha1 = sha1Colon.uppercase(Locale.US)
    val sha1HexLower = normalizedSha1.replace(":", "").lowercase(Locale.US)
    val isRegistered = normalizedSha1.isBlank() || REGISTERED_OAUTH_SHA1_HASHES.contains(normalizedSha1)
    val isWebConfigured = webClientId.isNotBlank() && webClientId != "YOUR_WEB_CLIENT_ID"

    return ApkSigningConfigReport(
        packageName = pkgName,
        sha1Colon = normalizedSha1,
        sha1HexLower = sha1HexLower,
        sha256Colon = sha256Colon.uppercase(Locale.US),
        webClientId = webClientId,
        isSha1RegisteredInGoogleServices = isRegistered,
        isWebClientConfigured = isWebConfigured
    )
}

/**
 * Unwraps a [Context] hierarchy to find the hosting [ComponentActivity].
 * Credential Manager requires an Activity context to anchor its bottom sheet UI.
 */
internal tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Resolves the active `default_web_client_id` generated from `app/google-services.json`.
 */
internal fun resolveServerClientId(context: Context): String? {
    val clientId = try {
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resId != 0) {
            context.getString(resId)
        } else {
            context.getString(R.string.default_web_client_id)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to resolve default_web_client_id resource", e)
        return null
    }
    if (clientId.isBlank() || clientId == "YOUR_WEB_CLIENT_ID") {
        return null
    }
    return clientId
}

/**
 * Exchanges the CredentialManager response for a Firebase Auth session.
 * Guarantees that both "Continue with Google" and "Continue with Gmail One-Tap"
 * authenticate the exact same federated Google identity (`GoogleAuthProvider.getCredential`)
 * and never create duplicate Firebase accounts for the same Google user.
 */
private suspend fun handleSignInResult(
    result: GetCredentialResponse,
    onAuthSuccess: (uid: String, displayName: String, email: String) -> Unit,
    onSignInError: (String) -> Unit
) {
    val credential = result.credential
    if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        try {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken
            val googleAccountEmail = googleIdTokenCredential.id.trim().lowercase(Locale.US)
            val authCredential = GoogleAuthProvider.getCredential(idToken, null)

            val existingUser: FirebaseUser? = Firebase.auth.currentUser
            val authUser: FirebaseUser? = if (
                existingUser != null &&
                !existingUser.email.isNullOrBlank() &&
                existingUser.email!!.trim().lowercase(Locale.US) == googleAccountEmail
            ) {
                // Refresh existing Firebase session in-place so the UID never changes
                runCatching {
                    existingUser.reauthenticate(authCredential).await()
                    Firebase.auth.currentUser ?: existingUser
                }.getOrElse {
                    Firebase.auth.signInWithCredential(authCredential).await().user
                }
            } else {
                Firebase.auth.signInWithCredential(authCredential).await().user
                    ?: Firebase.auth.currentUser
            }

            val resolvedUid = authUser?.uid?.takeIf { it.isNotBlank() } ?: googleAccountEmail
            val resolvedEmail = authUser?.email?.trim()?.lowercase(Locale.US)?.takeIf { it.isNotBlank() }
                ?: googleAccountEmail
            val resolvedName = authUser?.displayName?.trim()?.takeIf { it.isNotBlank() }
                ?: googleIdTokenCredential.displayName?.trim()?.takeIf { it.isNotBlank() }
                ?: resolvedEmail.substringBefore("@").replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
                }.ifBlank { "Language Learner" }

            onAuthSuccess(resolvedUid, resolvedName, resolvedEmail)
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "Invalid Google ID token response", e)
            onSignInError("Invalid Google ID token: ${e.localizedMessage}")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase signInWithCredential failed", e)
            onSignInError("Authentication failed: ${e.localizedMessage ?: "Please try again."}")
        }
    } else {
        Log.e(TAG, "Unexpected credential type: ${credential.type}")
        onSignInError("Sign-in failed: unexpected credential type (${credential.type}).")
    }
}

/**
 * Formats a detailed, actionable error message if error 28444 or DEVELOPER_ERROR occurs
 * (e.g., if an external APK was re-signed by Google Play App Signing or a custom release key).
 */
private fun formatCredentialErrorMessage(
    context: Context,
    e: GetCredentialException
): String {
    val rawMessage = e.message ?: e.localizedMessage ?: "Sign-in failed"
    val is28444OrDevError = rawMessage.contains("28444") ||
        rawMessage.contains("Developer console is not set up correctly", ignoreCase = true) ||
        rawMessage.contains("10:") ||
        e is GetCredentialProviderConfigurationException

    if (is28444OrDevError) {
        val report = inspectApkSigningConfig(context)
        Log.e(
            TAG,
            "Google Sign-In OAuth configuration error [28444]. " +
                "pkg=${report.packageName}, apkSha1=${report.sha1Colon}, apkSha256=${report.sha256Colon}, " +
                "sha1InGoogleServices=${report.isSha1RegisteredInGoogleServices}"
        )
        return if (!report.isSha1RegisteredInGoogleServices && report.sha1Colon.isNotBlank()) {
            "[28444] This APK is signed with SHA-1 ${report.sha1Colon} (not yet registered in Firebase for ${report.packageName}). Add this SHA-1 in Firebase Console -> Project Settings."
        } else {
            "[28444] Google OAuth configuration mismatch for ${report.packageName} (SHA-1: ${report.sha1Colon.ifBlank { "unknown" }}). Please verify Play Services & Firebase Auth Google provider."
        }
    }
    return "Sign-in failed: ${e.localizedMessage ?: rawMessage}"
}

/**
 * Launches the standard "Continue with Google" Credential Manager flow using
 * [GetSignInWithGoogleOption], with an automatic single-option fallback to [GetGoogleIdOption]
 * if the device's Credential Provider prefers the Google ID token request.
 *
 * Each [GetCredentialRequest] contains strictly ONE option per request.
 */
fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager = CredentialManager.create(context),
    onSignInSuccess: () -> Unit,
    onSignInError: (String) -> Unit = { Log.e(TAG, it) },
    onLoadingChanged: (Boolean) -> Unit = {},
    scope: CoroutineScope
) {
    onGoogleSignInClicked(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = { _, _, _ -> onSignInSuccess() },
        onAuthError = onSignInError,
        scope = scope,
        onAuthCancelled = {},
        onLoadingChanged = onLoadingChanged
    )
}

/**
 * Overload returning resolved `(uid, displayName, email)` for [AuthScreen].
 */
fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager = CredentialManager.create(context),
    onAuthSuccess: (uid: String, displayName: String, email: String) -> Unit,
    onAuthError: (String) -> Unit = { Log.e(TAG, it) },
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {},
    onLoadingChanged: (Boolean) -> Unit = {}
) {
    val activity = context.findActivity()
    if (activity == null) {
        onAuthError("Unable to start Google Sign-In: Activity context not available.")
        return
    }

    val clientId = resolveServerClientId(activity)
    if (clientId == null) {
        val msg = "Google Sign-In is not configured (missing default_web_client_id)."
        Log.e(TAG, msg)
        onAuthError(msg)
        return
    }

    val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId)
        .build()

    val primaryRequest = GetCredentialRequest.Builder()
        .addCredentialOption(signInWithGoogleOption)
        .build()

    scope.launch {
        onLoadingChanged(true)
        try {
            val result = credentialManager.getCredential(
                request = primaryRequest,
                context = activity
            )
            handleSignInResult(result, onAuthSuccess, onAuthError)
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "User cancelled Google Sign-In")
            onAuthCancelled()
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Primary GetSignInWithGoogleOption failed (${e.type}: ${e.message}); trying single-option GetGoogleIdOption fallback")
            val fallbackGoogleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()
            val fallbackRequest = GetCredentialRequest.Builder()
                .addCredentialOption(fallbackGoogleIdOption)
                .build()
            try {
                val fallbackResult = credentialManager.getCredential(
                    request = fallbackRequest,
                    context = activity
                )
                handleSignInResult(fallbackResult, onAuthSuccess, onAuthError)
            } catch (cancelEx: GetCredentialCancellationException) {
                Log.i(TAG, "User cancelled fallback Google Sign-In")
                onAuthCancelled()
            } catch (noCredEx: NoCredentialException) {
                Log.w(TAG, "No Google account available on device", noCredEx)
                onAuthError("No Google account found on this device. Please add a Google account in Android Settings.")
            } catch (fallbackEx: GetCredentialException) {
                Log.e(TAG, "Fallback GetGoogleIdOption also failed", fallbackEx)
                onAuthError(formatCredentialErrorMessage(activity, fallbackEx))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Google Sign-In", e)
            onAuthError("Sign-in error: ${e.localizedMessage ?: "Please try again."}")
        } finally {
            onLoadingChanged(false)
        }
    }
}

/**
 * Launches the "Continue with Gmail One-Tap" Credential Manager flow using
 * [GetGoogleIdOption] (`setAutoSelectEnabled(true)`, `setFilterByAuthorizedAccounts(false)`),
 * and automatically falls back to a single-option [GetSignInWithGoogleOption] request if
 * One-Tap has no pre-authorized credential ready.
 *
 * Uses the exact same `GoogleAuthProvider.getCredential(idToken, null)` Firebase Auth exchange
 * as [onGoogleSignInClicked], ensuring zero duplicate accounts for the same Google identity.
 */
fun onGmailOneTapSignInClicked(
    context: Context,
    credentialManager: CredentialManager = CredentialManager.create(context),
    onAuthSuccess: (uid: String, displayName: String, email: String) -> Unit,
    onAuthError: (String) -> Unit = { Log.e(TAG, it) },
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {},
    onLoadingChanged: (Boolean) -> Unit = {}
) {
    val activity = context.findActivity()
    if (activity == null) {
        onAuthError("Unable to start Gmail One-Tap: Activity context not available.")
        return
    }

    val clientId = resolveServerClientId(activity)
    if (clientId == null) {
        val msg = "Gmail One-Tap is not configured (missing default_web_client_id)."
        Log.e(TAG, msg)
        onAuthError(msg)
        return
    }

    val oneTapGoogleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val oneTapRequest = GetCredentialRequest.Builder()
        .addCredentialOption(oneTapGoogleIdOption)
        .build()

    scope.launch {
        onLoadingChanged(true)
        try {
            val result = credentialManager.getCredential(
                request = oneTapRequest,
                context = activity
            )
            handleSignInResult(result, onAuthSuccess, onAuthError)
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "User cancelled Gmail One-Tap Sign-In")
            onAuthCancelled()
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Gmail One-Tap GetGoogleIdOption failed (${e.type}: ${e.message}); falling back to GetSignInWithGoogleOption")
            val fallbackSignInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId)
                .build()
            val fallbackRequest = GetCredentialRequest.Builder()
                .addCredentialOption(fallbackSignInOption)
                .build()
            try {
                val fallbackResult = credentialManager.getCredential(
                    request = fallbackRequest,
                    context = activity
                )
                handleSignInResult(fallbackResult, onAuthSuccess, onAuthError)
            } catch (cancelEx: GetCredentialCancellationException) {
                Log.i(TAG, "User cancelled fallback Sign-In")
                onAuthCancelled()
            } catch (noCredEx: NoCredentialException) {
                Log.w(TAG, "No Google account available on device", noCredEx)
                onAuthError("No Google account found on this device. Please add a Google account in Android Settings.")
            } catch (fallbackEx: GetCredentialException) {
                Log.e(TAG, "Fallback GetSignInWithGoogleOption also failed", fallbackEx)
                onAuthError(formatCredentialErrorMessage(activity, fallbackEx))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Gmail One-Tap Sign-In", e)
            onAuthError("Sign-in error: ${e.localizedMessage ?: "Please try again."}")
        } finally {
            onLoadingChanged(false)
        }
    }
}

/**
 * Signs the user out of BOTH Firebase Auth and Jetpack Credential Manager.
 */
fun appSignOut(
    context: Context,
    credentialManager: CredentialManager = CredentialManager.create(context),
    onSignOutComplete: () -> Unit = {},
    scope: CoroutineScope
) {
    scope.launch {
        Firebase.auth.signOut()
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear CredentialManager state on sign-out", e)
        }
        onSignOutComplete()
    }
}

@Composable
fun GoogleSignInButton(
    onSignInSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    onSignInError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = {
            if (!isLoading) {
                onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onSignInSuccess = onSignInSuccess,
                    onSignInError = onSignInError,
                    onLoadingChanged = { isLoading = it },
                    scope = coroutineScope
                )
            }
        },
        enabled = !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .testTag("google_sign_in_button"),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "G",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFF4285F4)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isLoading) "Signing in..." else "Continue with Google",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
