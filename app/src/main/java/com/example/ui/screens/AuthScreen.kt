package com.example.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.example.ui.theme.BrandDeepTeal
import com.example.ui.theme.BrandLeafGreen
import com.example.ui.theme.SoundingThemeExtras
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

suspend fun performFirebaseSignOut(context: Context) {
    withContext(NonCancellable) {
        Firebase.auth.signOut()
        runCatching {
            val cm = CredentialManager.create(context)
            cm.clearCredentialState(ClearCredentialStateRequest())
        }
    }
}

private suspend fun attemptSilentGoogleSignIn(
    context: Context,
    credentialManager: CredentialManager
): FirebaseUser? {
    return try {
        val serverClientId = context.getString(R.string.default_web_client_id)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val firebaseCred = GoogleAuthProvider.getCredential(googleIdToken, null)
            Firebase.auth.signInWithCredential(firebaseCred).await().user
        } else {
            null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // Silent auto sign-in is best-effort; user can tap interactive button
        null
    }
}

@Composable
fun AuthScreen(
    onSignedIn: (FirebaseUser) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()
    val extras = SoundingThemeExtras.colors

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val autoUser = attemptSilentGoogleSignIn(context, credentialManager)
        if (autoUser != null) {
            onSignedIn(autoUser)
        }
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(BrandDeepTeal, Color(0xFF135E56), BrandLeafGreen)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .safeDrawingPadding()
            .testTag("auth_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pure-white elevated logo container
            Surface(
                modifier = Modifier.size(88.dp),
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_company_logo),
                    contentDescription = stringResource(R.string.cd_company_logo),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_header_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.app_header_subtitle),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp
                    ),
                    color = Color.White.copy(alpha = 0.92f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.about_company_identity),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.84f),
                    textAlign = TextAlign.Center
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.auth_card_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.auth_card_subtitle),
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AuthFeatureRow(
                        icon = Icons.Filled.CloudDone,
                        text = stringResource(R.string.auth_feat_realtime),
                        accentColor = extras.interactiveAccent
                    )
                    AuthFeatureRow(
                        icon = Icons.Filled.OfflinePin,
                        text = stringResource(R.string.auth_feat_offline),
                        accentColor = extras.leafAccentReadable
                    )
                    AuthFeatureRow(
                        icon = Icons.Filled.Groups,
                        text = stringResource(R.string.auth_feat_cross_shift),
                        accentColor = extras.interactiveAccent
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.errorContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (isLoading) return@Button
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    val serverClientId = context.getString(R.string.default_web_client_id)
                                    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId)
                                        .build()
                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(signInOption)
                                        .build()
                                    val result = credentialManager.getCredential(context, request)
                                    val credential = result.credential
                                    if (credential is CustomCredential &&
                                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                    ) {
                                        val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                                        val firebaseCred = GoogleAuthProvider.getCredential(token, null)
                                        val user = Firebase.auth.signInWithCredential(firebaseCred).await().user
                                        if (user != null) {
                                            onSignedIn(user)
                                        } else {
                                            errorMessage = context.getString(R.string.auth_error_generic)
                                        }
                                    } else {
                                        errorMessage = context.getString(R.string.auth_error_generic)
                                    }
                                } catch (_: GetCredentialCancellationException) {
                                    // User cancelled sign-in sheet; reset spinner silently
                                } catch (e: GetCredentialException) {
                                    Log.e("AuthScreen", "Credential error", e)
                                    errorMessage = context.getString(
                                        R.string.auth_error_detail,
                                        e.localizedMessage ?: e.type
                                    )
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    Log.e("AuthScreen", "Firebase Google Sign-In failed", e)
                                    errorMessage = context.getString(
                                        R.string.auth_error_detail,
                                        e.localizedMessage ?: context.getString(R.string.auth_error_generic)
                                    )
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("google_sign_in_button"),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandDeepTeal,
                            contentColor = Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.auth_signing_in),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Login,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.auth_action_google_signin),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.app_header_credit),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.80f)
            )
        }
    }
}

@Composable
private fun AuthFeatureRow(
    icon: ImageVector,
    text: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
