package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.view.autofill.AutofillManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.firebase.CheckEmailVerificationOutcome
import com.example.data.firebase.EmailVerificationAuthHelper
import com.example.data.firebase.SendEmailVerificationOutcome
import com.example.data.firebase.SendPasswordResetOutcome
import com.example.data.local.OperatorIdentityFormatter
import com.example.data.local.PasswordHashUtil
import com.example.data.local.PendingEmailVerification
import com.example.data.local.VerifiedOperatorAccount
import com.example.ui.ResolveResetEmailResult
import com.example.ui.UsernamePasswordLoginResult
import com.example.ui.theme.BrandDeepTeal
import com.example.ui.theme.BrandLeafGreen
import com.example.ui.theme.MonoTypography
import com.example.ui.theme.SaveGradientGreen
import com.example.ui.theme.SoundingThemeExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private val LOGIN_WHITESPACE_REGEX = Regex("\\s+")

private fun sanitizeLoginIdentifierInput(raw: String): String {
    return raw.trim().lowercase(Locale.US).replace(LOGIN_WHITESPACE_REGEX, "").take(254)
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.androidAutofill(
    autofillTypes: List<AutofillType>,
    onFill: (String) -> Unit
): Modifier = composed {
    if (autofillTypes.isEmpty()) return@composed this
    val autofill = LocalAutofill.current
    val autofillTree = LocalAutofillTree.current
    val currentOnFill by rememberUpdatedState(onFill)
    val autofillNode = remember(autofillTypes) {
        AutofillNode(
            autofillTypes = autofillTypes,
            onFill = { value -> currentOnFill(value) }
        )
    }
    DisposableEffect(autofillTree, autofillNode) {
        autofillTree += autofillNode
        onDispose {
            autofillTree.children.remove(autofillNode.id)
        }
    }
    this
        .onGloballyPositioned { coordinates ->
            autofillNode.boundingBox = coordinates.boundsInWindow()
        }
        .onFocusChanged { focusState ->
            autofill?.run {
                if (focusState.isFocused) {
                    requestAutofillForNode(autofillNode)
                } else {
                    cancelAutofillForNode(autofillNode)
                }
            }
        }
}

private fun commitAndroidAutofill(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        runCatching {
            context.getSystemService(AutofillManager::class.java)?.commit()
        }
    }
}

private enum class AuthScreenTab {
    LOGIN_VERIFIED,
    REGISTER_EMAIL
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OperatorAuthScreen(
    verifiedAccounts: List<VerifiedOperatorAccount>,
    savedCredentialUsernames: Set<String> = emptySet(),
    pendingVerification: PendingEmailVerification?,
    initialAccountName: String,
    initialEmail: String = "",
    forceRegisterTab: Boolean = false,
    allowCancelBack: Boolean = false,
    @Suppress("UNUSED_PARAMETER")
    isOnlineRealtime: Boolean = false,
    onLoadSavedPassword: (accountName: String) -> String? = { null },
    onForgetSavedAccount: (accountName: String) -> Unit = {},
    onLoginWithUsernamePassword: suspend (rawUsername: String, rawPassword: String, rememberCredentials: Boolean) -> UsernamePasswordLoginResult,
    onResolvePasswordResetEmail: suspend (rawInput: String) -> ResolveResetEmailResult,
    onSavePendingVerification: (rawUsername: String, email: String, passwordHash: String, rawPasswordToRemember: String?) -> Unit,
    onClearPendingVerification: () -> Unit,
    onAccountEmailVerified: (rawUsername: String, email: String, passwordHash: String, rawPasswordToRemember: String?) -> Unit,
    onCancelBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val extras = SoundingThemeExtras.colors

    BackHandler(enabled = allowCancelBack) {
        onCancelBack()
    }

    var activeTab by remember(forceRegisterTab, pendingVerification != null, verifiedAccounts.isNotEmpty()) {
        mutableStateOf(
            when {
                forceRegisterTab || pendingVerification != null -> AuthScreenTab.REGISTER_EMAIL
                verifiedAccounts.isNotEmpty() -> AuthScreenTab.LOGIN_VERIFIED
                else -> AuthScreenTab.REGISTER_EMAIL
            }
        )
    }

    val initialCleanAccount = remember(initialAccountName, pendingVerification) {
        when {
            pendingVerification != null -> pendingVerification.accountName
            initialAccountName.isNotBlank() -> OperatorIdentityFormatter.sanitizeUsername(initialAccountName)
            else -> ""
        }
    }

    var usernameInput by remember(initialAccountName, pendingVerification?.accountName) {
        mutableStateOf(initialCleanAccount)
    }
    var emailInput by remember(initialEmail, pendingVerification?.email) {
        mutableStateOf(pendingVerification?.email ?: initialEmail)
    }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var rememberCredentials by remember { mutableStateOf(true) }
    var isAutoFilledPassword by remember { mutableStateOf(false) }
    var forgetCandidateAccount by remember { mutableStateOf<String?>(null) }
    var savedCredNotice by remember { mutableStateOf<String?>(null) }

    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var notRegisteredUsername by remember { mutableStateOf<String?>(null) }
    var statusBannerError by remember { mutableStateOf<String?>(null) }
    var resetSuccessEmail by remember { mutableStateOf<String?>(null) }
    var resetErrorMessage by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var isSendingEmail by remember { mutableStateOf(false) }
    var isSendingPasswordReset by remember { mutableStateOf(false) }
    var isCheckingVerification by remember { mutableStateOf(false) }
    var resendCooldownSec by remember { mutableIntStateOf(0) }
    var resetCooldownSec by remember { mutableIntStateOf(0) }

    val isLoginEmailTyped = remember(usernameInput) {
        usernameInput.contains("@")
    }
    val previewAccountName = remember(usernameInput, verifiedAccounts) {
        if (usernameInput.contains("@")) {
            val normalizedEmail = EmailVerificationAuthHelper.normalizeEmail(usernameInput)
            verifiedAccounts.firstOrNull { it.email.equals(normalizedEmail, ignoreCase = true) }
                ?.accountName
                ?: normalizedEmail
        } else {
            OperatorIdentityFormatter.sanitizeUsername(usernameInput)
        }
    }

    val isAwaitingVerification = pendingVerification != null

    // Auto-fill remembered password if initial pre-filled account has saved credentials on Login tab
    LaunchedEffect(initialCleanAccount, activeTab) {
        if (activeTab == AuthScreenTab.LOGIN_VERIFIED &&
            initialCleanAccount.isNotBlank() &&
            passwordInput.isEmpty() &&
            savedCredentialUsernames.any { it.equals(initialCleanAccount, ignoreCase = true) }
        ) {
            val savedPwd = onLoadSavedPassword(initialCleanAccount)
            if (!savedPwd.isNullOrEmpty()) {
                passwordInput = savedPwd
                isAutoFilledPassword = true
            }
        }
    }

    LaunchedEffect(resendCooldownSec) {
        if (resendCooldownSec > 0) {
            delay(1000L)
            resendCooldownSec -= 1
        }
    }

    LaunchedEffect(resetCooldownSec) {
        if (resetCooldownSec > 0) {
            delay(1000L)
            resetCooldownSec -= 1
        }
    }

    fun checkVerificationStatus(silentIfPending: Boolean = false) {
        val pending = pendingVerification ?: return
        if (isCheckingVerification) return
        if (!silentIfPending) {
            statusBannerError = null
        }
        isCheckingVerification = true
        scope.launch {
            try {
                when (
                    val outcome = EmailVerificationAuthHelper.checkEmailVerificationStatus(
                        context = context,
                        expectedEmail = pending.email
                    )
                ) {
                    is CheckEmailVerificationOutcome.Verified -> {
                        statusBannerError = null
                        commitAndroidAutofill(context)
                        onAccountEmailVerified(
                            pending.accountName,
                            outcome.email,
                            pending.passwordHash,
                            null
                        )
                    }
                    is CheckEmailVerificationOutcome.NotYetVerified -> {
                        if (!silentIfPending) {
                            statusBannerError = outcome.message
                        }
                    }
                    is CheckEmailVerificationOutcome.NoActiveSession -> {
                        if (!silentIfPending) {
                            statusBannerError = outcome.message
                        }
                    }
                    is CheckEmailVerificationOutcome.Error -> {
                        if (!silentIfPending) {
                            statusBannerError = outcome.message
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!silentIfPending) {
                    statusBannerError = e.localizedMessage
                        ?: context.getString(R.string.auth_verify_error_generic)
                }
            } finally {
                isCheckingVerification = false
            }
        }
    }

    // Automatically check user.isEmailVerified when returning from email client
    DisposableEffect(lifecycleOwner, pendingVerification?.accountName, pendingVerification?.email) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingVerification != null) {
                checkVerificationStatus(silentIfPending = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun submitUsernamePasswordLogin() {
        if (isLoggingIn) return
        val liveIdentifier = sanitizeLoginIdentifierInput(usernameInput)
        usernameError = null
        passwordError = null
        notRegisteredUsername = null
        resetErrorMessage = null

        if (liveIdentifier.isEmpty()) {
            usernameError = context.getString(R.string.username_error_empty)
            return
        }
        if (liveIdentifier.contains("@") && !EmailVerificationAuthHelper.isValidEmail(liveIdentifier)) {
            usernameError = context.getString(R.string.auth_email_error_invalid)
            return
        }
        if (passwordInput.trim().isEmpty()) {
            passwordError = context.getString(R.string.auth_password_error_empty)
            return
        }

        isLoggingIn = true
        scope.launch {
            try {
                when (
                    val result = onLoginWithUsernamePassword(
                        liveIdentifier,
                        passwordInput,
                        rememberCredentials
                    )
                ) {
                    is UsernamePasswordLoginResult.Success -> {
                        commitAndroidAutofill(context)
                        // Authenticated locally (with background cloud sync if online)
                    }
                    is UsernamePasswordLoginResult.EmptyUsername -> {
                        usernameError = context.getString(R.string.username_error_empty)
                    }
                    is UsernamePasswordLoginResult.EmptyPassword -> {
                        passwordError = context.getString(R.string.auth_password_error_empty)
                    }
                    is UsernamePasswordLoginResult.PasswordTooShortForMigration -> {
                        passwordError = context.getString(R.string.auth_password_error_short)
                    }
                    is UsernamePasswordLoginResult.UserNotRegistered -> {
                        notRegisteredUsername = result.username
                    }
                    is UsernamePasswordLoginResult.EmailNotYetVerified -> {
                        resetErrorMessage = context.getString(
                            R.string.auth_verify_not_yet_verified,
                            result.email
                        )
                    }
                    is UsernamePasswordLoginResult.AuthError -> {
                        resetErrorMessage = result.message
                    }
                    is UsernamePasswordLoginResult.InvalidPassword -> {
                        passwordError = context.getString(R.string.auth_password_error_wrong)
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                resetErrorMessage = e.localizedMessage
                    ?: context.getString(R.string.auth_error_network)
            } finally {
                isLoggingIn = false
            }
        }
    }

    fun submitForgotPassword() {
        if (isSendingPasswordReset) return
        val liveInput = sanitizeLoginIdentifierInput(usernameInput)
        resetSuccessEmail = null
        resetErrorMessage = null
        if (liveInput.isEmpty()) {
            resetErrorMessage = context.getString(R.string.auth_forgot_password_empty_email)
            return
        }
        isSendingPasswordReset = true
        scope.launch {
            try {
                when (val resolved = onResolvePasswordResetEmail(liveInput)) {
                    is ResolveResetEmailResult.EmptyInput -> {
                        resetErrorMessage = context.getString(R.string.auth_forgot_password_empty_email)
                    }
                    is ResolveResetEmailResult.NotFound -> {
                        resetErrorMessage = if (resolved.isNetworkOnline) {
                            context.getString(
                                R.string.auth_forgot_password_username_not_found_online,
                                resolved.query
                            )
                        } else {
                            context.getString(
                                R.string.auth_forgot_password_username_not_found_offline,
                                resolved.query
                            )
                        }
                    }
                    is ResolveResetEmailResult.Resolved -> {
                        when (
                            val outcome = EmailVerificationAuthHelper.sendPasswordResetEmail(
                                context = context,
                                rawEmail = resolved.email
                            )
                        ) {
                            is SendPasswordResetOutcome.Sent -> {
                                resetSuccessEmail = outcome.email
                                resetErrorMessage = null
                                resetCooldownSec = 30
                            }
                            is SendPasswordResetOutcome.Error -> {
                                resetSuccessEmail = null
                                resetErrorMessage = outcome.message
                            }
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                resetSuccessEmail = null
                resetErrorMessage = context.getString(
                    R.string.auth_forgot_password_failed,
                    e.localizedMessage ?: context.getString(R.string.auth_error_network)
                )
            } finally {
                isSendingPasswordReset = false
            }
        }
    }

    fun submitSendVerificationEmail(isResend: Boolean = false) {
        if (isSendingEmail) return
        val targetUsername = if (isResend && pendingVerification != null) {
            pendingVerification.accountName
        } else {
            OperatorIdentityFormatter.sanitizeUsername(usernameInput)
        }
        val targetEmail = EmailVerificationAuthHelper.normalizeEmail(
            if (isResend && pendingVerification != null) pendingVerification.email else emailInput
        )
        val trimmedPassword = if (isResend) "" else passwordInput.trim()

        if (targetUsername.isEmpty()) {
            usernameError = context.getString(R.string.username_error_empty)
            return
        }
        if (!EmailVerificationAuthHelper.isValidEmail(targetEmail)) {
            emailError = context.getString(R.string.auth_email_error_invalid)
            return
        }
        if (!isResend) {
            if (trimmedPassword.isEmpty()) {
                passwordError = context.getString(R.string.auth_password_error_required)
                return
            }
            if (trimmedPassword.length < EmailVerificationAuthHelper.MIN_PASSWORD_LENGTH) {
                passwordError = context.getString(R.string.auth_password_error_short)
                return
            }
        }

        usernameError = null
        emailError = null
        passwordError = null
        statusBannerError = null
        isSendingEmail = true

        scope.launch {
            try {
                val computedPasswordHash = if (isResend) {
                    pendingVerification?.passwordHash.orEmpty()
                } else {
                    withContext(Dispatchers.Default) {
                        PasswordHashUtil.hashPassword(trimmedPassword)
                    }
                }
                when (
                    val outcome = EmailVerificationAuthHelper.sendStandardVerificationEmail(
                        context = context,
                        rawEmail = targetEmail,
                        customPassword = trimmedPassword,
                        isResend = isResend
                    )
                ) {
                    is SendEmailVerificationOutcome.Sent -> {
                        onSavePendingVerification(
                            targetUsername,
                            outcome.email,
                            computedPasswordHash,
                            if (!isResend && rememberCredentials) trimmedPassword else null
                        )
                        resendCooldownSec = 30
                    }
                    is SendEmailVerificationOutcome.AlreadyVerified -> {
                        commitAndroidAutofill(context)
                        onAccountEmailVerified(
                            targetUsername,
                            outcome.email,
                            computedPasswordHash,
                            if (!isResend && rememberCredentials) trimmedPassword else null
                        )
                    }
                    is SendEmailVerificationOutcome.EmailAlreadyInUse -> {
                        statusBannerError = outcome.message
                    }
                    is SendEmailVerificationOutcome.ProviderNotEnabled -> {
                        statusBannerError = outcome.message
                    }
                    is SendEmailVerificationOutcome.Error -> {
                        statusBannerError = outcome.message
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                statusBannerError = context.getString(
                    R.string.auth_email_send_failed,
                    e.localizedMessage ?: context.getString(R.string.auth_error_network)
                )
            } finally {
                isSendingEmail = false
            }
        }
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            extras.headerGradientStart,
            extras.headerGradientMid,
            extras.headerGradientEnd
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .safeDrawingPadding()
            .testTag("username_login_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (allowCancelBack) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    TextButton(
                        onClick = onCancelBack,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                        modifier = Modifier.testTag("auth_back_to_app_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.auth_action_back_to_app),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_header_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 21.sp
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
                    color = extras.headerSubtitleAccent,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.about_company_identity),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.84f),
                    textAlign = TextAlign.Center
                )
            }

            // Main Auth Card (Masuk Akun Username + Password / Buat Akun + Verifikasi Email 1 Kali)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, extras.cardBorder),
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AuthModeSegmentedRow(
                        activeTab = activeTab,
                        onSelectTab = { nextTab ->
                            if (nextTab == AuthScreenTab.REGISTER_EMAIL && usernameInput.contains("@")) {
                                val typedEmail = EmailVerificationAuthHelper.normalizeEmail(usernameInput)
                                if (emailInput.isBlank() && EmailVerificationAuthHelper.isValidEmail(typedEmail)) {
                                    emailInput = typedEmail
                                }
                                usernameInput = OperatorIdentityFormatter.sanitizeUsername(typedEmail)
                            }
                            if (isAutoFilledPassword) {
                                passwordInput = ""
                                isAutoFilledPassword = false
                            }
                            activeTab = nextTab
                            usernameError = null
                            emailError = null
                            passwordError = null
                            notRegisteredUsername = null
                            statusBannerError = null
                            resetSuccessEmail = null
                            resetErrorMessage = null
                        }
                    )

                    if (activeTab == AuthScreenTab.LOGIN_VERIFIED) {
                        if (verifiedAccounts.isNotEmpty()) {
                            VerifiedOperatorAccountsSection(
                                verifiedAccounts = verifiedAccounts,
                                savedCredentialUsernames = savedCredentialUsernames,
                                selectedAccountName = previewAccountName,
                                chipTagPrefix = "saved_operator_chip_",
                                onSelectAccount = { account ->
                                    usernameInput = account.accountName
                                    val savedPwd = onLoadSavedPassword(account.accountName)
                                    if (!savedPwd.isNullOrEmpty()) {
                                        passwordInput = savedPwd
                                        isAutoFilledPassword = true
                                        rememberCredentials = true
                                    } else {
                                        passwordInput = ""
                                        isAutoFilledPassword = false
                                    }
                                    usernameError = null
                                    passwordError = null
                                    notRegisteredUsername = null
                                    resetSuccessEmail = null
                                    resetErrorMessage = null
                                    savedCredNotice = null
                                },
                                onForgetAccount = { account ->
                                    forgetCandidateAccount = account.accountName
                                }
                            )
                        }

                        if (!savedCredNotice.isNullOrBlank()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("saved_credentials_removed_notice"),
                                shape = MaterialTheme.shapes.small,
                                color = extras.elevatedSurface,
                                border = BorderStroke(1.dp, extras.cardBorder)
                            ) {
                                Text(
                                    text = savedCredNotice.orEmpty(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }

                        val isLoginMatchedVerified = verifiedAccounts.any {
                            it.accountName.equals(previewAccountName, ignoreCase = true) ||
                                (isLoginEmailTyped && it.email.equals(usernameInput, ignoreCase = true))
                        }
                        UsernameOutlinedField(
                            value = usernameInput,
                            errorText = usernameError,
                            tag = "username_login_input",
                            allowEmailIdentifier = true,
                            isVerifiedMatch = isLoginMatchedVerified,
                            autofillTypes = listOf(AutofillType.Username, AutofillType.EmailAddress),
                            onValueChange = { raw ->
                                usernameInput = sanitizeLoginIdentifierInput(raw)
                                isAutoFilledPassword = false
                                usernameError = null
                                notRegisteredUsername = null
                                resetErrorMessage = null
                                savedCredNotice = null
                            },
                            onDone = { submitUsernamePasswordLogin() }
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            PasswordOutlinedField(
                                value = passwordInput,
                                visible = isPasswordVisible,
                                errorText = passwordError,
                                helperText = if (isAutoFilledPassword) {
                                    stringResource(R.string.auth_saved_credentials_autofilled)
                                } else {
                                    null
                                },
                                tag = "login_password_input",
                                autofillTypes = listOf(AutofillType.Password),
                                onValueChange = {
                                    passwordInput = it
                                    isAutoFilledPassword = false
                                    if (passwordError != null) passwordError = null
                                    if (resetErrorMessage != null) resetErrorMessage = null
                                },
                                onToggleVisibility = { isPasswordVisible = !isPasswordVisible },
                                onDone = { submitUsernamePasswordLogin() }
                            )

                            RememberCredentialsCheckboxRow(
                                checked = rememberCredentials,
                                tag = "login_remember_credentials_checkbox",
                                onCheckedChange = { rememberCredentials = it }
                            )
                        }

                        if (!resetSuccessEmail.isNullOrBlank()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("forgot_password_success_banner"),
                                shape = MaterialTheme.shapes.medium,
                                color = extras.leafContainer,
                                border = BorderStroke(1.dp, extras.leafAccent.copy(alpha = 0.55f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.MarkEmailRead,
                                            contentDescription = null,
                                            tint = extras.onLeafContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.auth_forgot_password_sent_title),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = extras.onLeafContainer
                                        )
                                    }
                                    Text(
                                        text = stringResource(
                                            R.string.auth_forgot_password_sent_desc,
                                            resetSuccessEmail.orEmpty()
                                        ),
                                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        if (!resetErrorMessage.isNullOrBlank()) {
                            ErrorBannerBox(
                                message = resetErrorMessage.orEmpty(),
                                modifier = Modifier.testTag("forgot_password_error_banner")
                            )
                        }

                        if (!notRegisteredUsername.isNullOrBlank()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("username_not_registered_banner"),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(
                                            R.string.auth_username_not_registered,
                                            notRegisteredUsername.orEmpty()
                                        ),
                                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            activeTab = AuthScreenTab.REGISTER_EMAIL
                                            notRegisteredUsername = null
                                            statusBannerError = null
                                            passwordError = null
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("auth_jump_to_register_button"),
                                        shape = MaterialTheme.shapes.small,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PersonAdd,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.auth_action_go_register),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }

                        // Capsule / pill green-lime gradient Masuk Aplikasi CTA
                        val pillShape = RoundedCornerShape(999.dp)
                        val loginGradient = Brush.horizontalGradient(
                            colors = listOf(SaveGradientGreen, extras.limeAccent, extras.primaryCtaEnd)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .clip(pillShape)
                                .background(loginGradient)
                                .clickable(
                                    enabled = !isLoggingIn,
                                    onClick = { submitUsernamePasswordLogin() }
                                )
                                .testTag("username_login_submit_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isLoggingIn) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = extras.onLimeButton
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.auth_logging_in),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = extras.onLimeButton
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Login,
                                        contentDescription = null,
                                        tint = extras.onLimeButton,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.username_login_action),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = extras.onLimeButton
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(
                                onClick = { submitForgotPassword() },
                                enabled = !isSendingPasswordReset && resetCooldownSec == 0,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("forgot_password_button"),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = extras.interactiveAccent
                                )
                            ) {
                                if (isSendingPasswordReset) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = extras.interactiveAccent
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.auth_sending_password_reset),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.LockReset,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (resetCooldownSec > 0) {
                                            stringResource(R.string.auth_forgot_password_cooldown, resetCooldownSec)
                                        } else {
                                            stringResource(R.string.auth_action_forgot_password)
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    } else {
                        // REGISTER & STANDARD FIREBASE EMAIL VERIFICATION TAB
                        if (!isAwaitingVerification) {
                            UsernameOutlinedField(
                                value = usernameInput,
                                errorText = usernameError,
                                tag = "register_username_input",
                                isVerifiedMatch = verifiedAccounts.any {
                                    it.accountName.equals(previewAccountName, ignoreCase = true)
                                },
                                autofillTypes = listOf(AutofillType.Username),
                                onValueChange = { raw ->
                                    usernameInput = OperatorIdentityFormatter.sanitizeUsername(raw)
                                    usernameError = null
                                    statusBannerError = null
                                },
                                onDone = {}
                            )

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { raw ->
                                    emailInput = raw.trim()
                                    if (emailError != null) emailError = null
                                    if (statusBannerError != null) statusBannerError = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .androidAutofill(
                                        autofillTypes = listOf(AutofillType.EmailAddress),
                                        onFill = { filled ->
                                            emailInput = filled.trim()
                                            if (emailError != null) emailError = null
                                            if (statusBannerError != null) statusBannerError = null
                                        }
                                    )
                                    .testTag("register_email_input"),
                                singleLine = true,
                                label = { Text(stringResource(R.string.auth_email_label)) },
                                placeholder = { Text(stringResource(R.string.auth_email_placeholder)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Email,
                                        contentDescription = null,
                                        tint = extras.interactiveAccent
                                    )
                                },
                                isError = emailError != null,
                                supportingText = if (emailError != null) {
                                    { Text(text = emailError.orEmpty()) }
                                } else {
                                    null
                                },
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.None,
                                    autoCorrectEnabled = false,
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = extras.leafAccent,
                                    focusedLabelColor = extras.leafAccentReadable,
                                    cursorColor = extras.leafAccentReadable
                                ),
                                shape = MaterialTheme.shapes.medium
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                PasswordOutlinedField(
                                    value = passwordInput,
                                    visible = isPasswordVisible,
                                    errorText = passwordError,
                                    placeholderText = stringResource(R.string.auth_password_register_placeholder),
                                    helperText = null,
                                    tag = "register_password_input",
                                    autofillTypes = listOf(AutofillType.NewPassword),
                                    onValueChange = {
                                        passwordInput = it
                                        if (passwordError != null) passwordError = null
                                        if (statusBannerError != null) statusBannerError = null
                                    },
                                    onToggleVisibility = { isPasswordVisible = !isPasswordVisible },
                                    onDone = { submitSendVerificationEmail(false) }
                                )

                                RememberCredentialsCheckboxRow(
                                    checked = rememberCredentials,
                                    tag = "register_remember_credentials_checkbox",
                                    onCheckedChange = { rememberCredentials = it }
                                )
                            }

                            if (!statusBannerError.isNullOrBlank()) {
                                ErrorBannerBox(message = statusBannerError.orEmpty())
                            }

                            Button(
                                onClick = { submitSendVerificationEmail(false) },
                                enabled = !isSendingEmail,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("send_email_verification_button"),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = extras.executiveActionSurface,
                                    contentColor = Color.White
                                )
                            ) {
                                if (isSendingEmail) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.auth_sending_code),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.MarkEmailRead,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.auth_action_send_code),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        } else {
                            // Step 2: Standard Firebase Email Verification sent -> open email link & tap Check Status
                            val pending = pendingVerification!!
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = extras.leafContainer,
                                border = BorderStroke(1.dp, extras.leafAccent.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.MarkEmailRead,
                                            contentDescription = null,
                                            tint = extras.onLeafContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.auth_code_sent_title),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = extras.onLeafContainer
                                        )
                                    }
                                    Text(
                                        text = pending.accountName,
                                        style = MonoTypography.subMetric.copy(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = extras.onLeafContainer
                                    )
                                    Text(
                                        text = stringResource(R.string.auth_code_sent_desc, pending.email),
                                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (!statusBannerError.isNullOrBlank()) {
                                ErrorBannerBox(message = statusBannerError.orEmpty())
                            }

                            Button(
                                onClick = { checkVerificationStatus(silentIfPending = false) },
                                enabled = !isCheckingVerification,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("check_email_verification_button"),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandLeafGreen,
                                    contentColor = Color.White
                                )
                            ) {
                                if (isCheckingVerification) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.auth_checking_verification),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.VerifiedUser,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.auth_action_check_verify_activate),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { submitSendVerificationEmail(isResend = true) },
                                    enabled = !isSendingEmail && resendCooldownSec == 0,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                        .testTag("resend_verification_code_button"),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (resendCooldownSec > 0) {
                                            stringResource(R.string.auth_action_resend_countdown, resendCooldownSec)
                                        } else {
                                            stringResource(R.string.auth_action_resend_code)
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        statusBannerError = null
                                        onClearPendingVerification()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                        .testTag("change_verification_email_button"),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text(
                                        text = stringResource(R.string.auth_action_change_email),
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.app_header_credit),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.80f)
            )
        }
    }

    val pendingForget = forgetCandidateAccount
    if (pendingForget != null) {
        ForgetSavedAccountDialog(
            accountName = pendingForget,
            isActiveSessionOnly = false,
            onConfirm = {
                forgetCandidateAccount = null
                onForgetSavedAccount(pendingForget)
                if (previewAccountName.equals(pendingForget, ignoreCase = true) ||
                    usernameInput.equals(pendingForget, ignoreCase = true)
                ) {
                    usernameInput = ""
                    passwordInput = ""
                    isAutoFilledPassword = false
                }
                savedCredNotice = context.getString(R.string.msg_saved_credentials_removed, pendingForget)
            },
            onDismiss = { forgetCandidateAccount = null }
        )
    }
}

@Composable
private fun AuthModeSegmentedRow(
    activeTab: AuthScreenTab,
    onSelectTab: (AuthScreenTab) -> Unit
) {
    val extras = SoundingThemeExtras.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = extras.elevatedSurface,
        border = BorderStroke(1.dp, extras.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AuthTabPill(
                label = stringResource(R.string.auth_tab_login),
                icon = Icons.Filled.Login,
                selected = activeTab == AuthScreenTab.LOGIN_VERIFIED,
                tag = "auth_tab_login",
                onClick = { onSelectTab(AuthScreenTab.LOGIN_VERIFIED) },
                modifier = Modifier.weight(1f)
            )
            AuthTabPill(
                label = stringResource(R.string.auth_tab_register),
                icon = Icons.Filled.PersonAdd,
                selected = activeTab == AuthScreenTab.REGISTER_EMAIL,
                tag = "auth_tab_register",
                onClick = { onSelectTab(AuthScreenTab.REGISTER_EMAIL) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AuthTabPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extras = SoundingThemeExtras.colors
    Surface(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(9.dp),
        color = if (selected) extras.executiveActionSurface else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ErrorBannerBox(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun RememberCredentialsCheckboxRow(
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    val extras = SoundingThemeExtras.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(
                role = Role.Checkbox,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(horizontal = 2.dp, vertical = 4.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = extras.leafAccent,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkmarkColor = Color.White
            )
        )
        Text(
            text = stringResource(R.string.auth_remember_credentials_label),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ForgetSavedAccountDialog(
    accountName: String,
    isActiveSessionOnly: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.auth_forget_account_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Text(
                text = stringResource(
                    if (isActiveSessionOnly) {
                        R.string.auth_forget_active_password_message
                    } else {
                        R.string.auth_forget_account_message
                    },
                    accountName
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("confirm_forget_saved_account_button")
            ) {
                Text(stringResource(R.string.auth_forget_account_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_forget_saved_account_button")
            ) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PasswordOutlinedField(
    value: String,
    visible: Boolean,
    errorText: String?,
    placeholderText: String = stringResource(R.string.auth_password_placeholder),
    helperText: String? = null,
    tag: String,
    autofillTypes: List<AutofillType> = listOf(AutofillType.Password),
    onValueChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onDone: () -> Unit
) {
    val extras = SoundingThemeExtras.colors
    val supporting = errorText ?: helperText?.takeIf { it.isNotBlank() }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .androidAutofill(
                autofillTypes = autofillTypes,
                onFill = onValueChange
            )
            .testTag(tag),
        singleLine = true,
        label = { Text(stringResource(R.string.auth_password_label)) },
        placeholder = { Text(placeholderText) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = extras.interactiveAccent
            )
        },
        trailingIcon = {
            IconButton(
                onClick = onToggleVisibility,
                modifier = Modifier.testTag("${tag}_toggle_visibility")
            ) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = stringResource(
                        if (visible) R.string.cd_hide_password else R.string.cd_show_password
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        isError = errorText != null,
        supportingText = if (supporting != null) {
            { Text(text = supporting) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = extras.leafAccent,
            focusedLabelColor = extras.leafAccentReadable,
            cursorColor = extras.leafAccentReadable
        ),
        shape = MaterialTheme.shapes.medium
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun UsernameOutlinedField(
    value: String,
    errorText: String?,
    tag: String,
    allowEmailIdentifier: Boolean = false,
    isVerifiedMatch: Boolean = false,
    autofillTypes: List<AutofillType> = listOf(AutofillType.Username),
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
) {
    val extras = SoundingThemeExtras.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .androidAutofill(
                autofillTypes = autofillTypes,
                onFill = onValueChange
            )
            .testTag(tag),
        singleLine = true,
        label = {
            Text(
                stringResource(
                    if (allowEmailIdentifier) R.string.auth_login_identifier_label else R.string.username_input_label
                )
            )
        },
        placeholder = {
            Text(
                stringResource(
                    if (allowEmailIdentifier) R.string.auth_login_identifier_placeholder else R.string.username_input_placeholder
                )
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Badge,
                contentDescription = null,
                tint = extras.interactiveAccent
            )
        },
        trailingIcon = if (isVerifiedMatch && value.isNotBlank()) {
            {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = null,
                    tint = extras.leafAccentReadable,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            null
        },
        isError = errorText != null,
        supportingText = if (errorText != null) {
            { Text(text = errorText) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = if (allowEmailIdentifier) KeyboardType.Email else KeyboardType.Ascii,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = extras.leafAccent,
            focusedLabelColor = extras.leafAccentReadable,
            cursorColor = extras.leafAccentReadable
        ),
        shape = MaterialTheme.shapes.medium
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SwitchUsernameBottomSheet(
    currentAccountName: String,
    verifiedAccounts: List<VerifiedOperatorAccount>,
    savedCredentialUsernames: Set<String> = emptySet(),
    onLoadSavedPassword: (accountName: String) -> String? = { null },
    onForgetSavedAccount: (accountName: String) -> Unit = {},
    onAuthenticateSwitch: suspend (rawUsername: String, rawPassword: String, rememberCredentials: Boolean) -> UsernamePasswordLoginResult,
    onRequestCreateVerifiedAccount: (rawUsername: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val extras = SoundingThemeExtras.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var usernameInput by remember(currentAccountName) {
        mutableStateOf(OperatorIdentityFormatter.sanitizeUsername(currentAccountName))
    }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var rememberCredentials by remember { mutableStateOf(true) }
    var isAutoFilledPassword by remember { mutableStateOf(false) }
    var forgetCandidateAccount by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var notRegisteredUsername by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }

    val previewAccountName = remember(usernameInput) {
        OperatorIdentityFormatter.sanitizeUsername(usernameInput)
    }
    val isSameAsCurrent = remember(previewAccountName, currentAccountName) {
        previewAccountName.isNotBlank() &&
            previewAccountName.equals(currentAccountName, ignoreCase = true)
    }
    val isPreviewVerified = remember(previewAccountName, verifiedAccounts, currentAccountName) {
        previewAccountName.isNotBlank() && (
            verifiedAccounts.any { it.accountName.equals(previewAccountName, ignoreCase = true) } ||
                previewAccountName.equals(currentAccountName, ignoreCase = true)
            )
    }

    fun submitSwitch() {
        if (isAuthenticating) return
        val clean = OperatorIdentityFormatter.sanitizeUsername(usernameInput)
        errorMsg = null
        passwordError = null
        notRegisteredUsername = null

        if (clean.isEmpty()) {
            errorMsg = context.getString(R.string.username_error_empty)
            return
        }
        if (clean.equals(currentAccountName, ignoreCase = true)) {
            onDismiss()
            return
        }
        if (passwordInput.trim().isEmpty()) {
            passwordError = context.getString(R.string.auth_password_error_empty)
            return
        }

        isAuthenticating = true
        scope.launch {
            try {
                when (val result = onAuthenticateSwitch(clean, passwordInput, rememberCredentials)) {
                    is UsernamePasswordLoginResult.Success -> {
                        commitAndroidAutofill(context)
                        // Dismissed by caller
                    }
                    is UsernamePasswordLoginResult.EmptyUsername -> {
                        errorMsg = context.getString(R.string.username_error_empty)
                    }
                    is UsernamePasswordLoginResult.EmptyPassword -> {
                        passwordError = context.getString(R.string.auth_password_error_empty)
                    }
                    is UsernamePasswordLoginResult.PasswordTooShortForMigration -> {
                        passwordError = context.getString(R.string.auth_password_error_short)
                    }
                    is UsernamePasswordLoginResult.UserNotRegistered -> {
                        notRegisteredUsername = result.username
                    }
                    is UsernamePasswordLoginResult.EmailNotYetVerified -> {
                        errorMsg = context.getString(R.string.auth_verify_not_yet_verified, result.email)
                    }
                    is UsernamePasswordLoginResult.AuthError -> {
                        errorMsg = result.message
                    }
                    is UsernamePasswordLoginResult.InvalidPassword -> {
                        passwordError = context.getString(R.string.auth_password_error_wrong)
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMsg = e.localizedMessage ?: context.getString(R.string.auth_error_network)
            } finally {
                isAuthenticating = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = extras.glassSheetSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(bottom = 24.dp)
                .testTag("switch_username_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = null,
                    tint = extras.interactiveAccent,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.switch_username_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.switch_username_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (verifiedAccounts.isNotEmpty()) {
                VerifiedOperatorAccountsSection(
                    verifiedAccounts = verifiedAccounts,
                    savedCredentialUsernames = savedCredentialUsernames,
                    selectedAccountName = previewAccountName,
                    chipTagPrefix = "switch_saved_chip_",
                    onSelectAccount = { account ->
                        usernameInput = account.accountName
                        val savedPwd = onLoadSavedPassword(account.accountName)
                        if (!savedPwd.isNullOrEmpty()) {
                            passwordInput = savedPwd
                            isAutoFilledPassword = true
                            rememberCredentials = true
                        } else {
                            passwordInput = ""
                            isAutoFilledPassword = false
                        }
                        errorMsg = null
                        passwordError = null
                        notRegisteredUsername = null
                    },
                    onForgetAccount = { account ->
                        forgetCandidateAccount = account.accountName
                    }
                )
            }

            UsernameOutlinedField(
                value = usernameInput,
                errorText = errorMsg,
                tag = "switch_username_input",
                isVerifiedMatch = isPreviewVerified,
                autofillTypes = listOf(AutofillType.Username),
                onValueChange = { raw ->
                    usernameInput = OperatorIdentityFormatter.sanitizeUsername(raw)
                    isAutoFilledPassword = false
                    errorMsg = null
                    notRegisteredUsername = null
                },
                onDone = { submitSwitch() }
            )

            if (!isSameAsCurrent && previewAccountName.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    PasswordOutlinedField(
                        value = passwordInput,
                        visible = isPasswordVisible,
                        errorText = passwordError,
                        helperText = if (isAutoFilledPassword) {
                            stringResource(R.string.auth_saved_credentials_autofilled)
                        } else {
                            null
                        },
                        tag = "switch_password_input",
                        autofillTypes = listOf(AutofillType.Password),
                        onValueChange = {
                            passwordInput = it
                            isAutoFilledPassword = false
                            if (passwordError != null) passwordError = null
                        },
                        onToggleVisibility = { isPasswordVisible = !isPasswordVisible },
                        onDone = { submitSwitch() }
                    )

                    RememberCredentialsCheckboxRow(
                        checked = rememberCredentials,
                        tag = "switch_remember_credentials_checkbox",
                        onCheckedChange = { rememberCredentials = it }
                    )
                }
            }

            if (!notRegisteredUsername.isNullOrBlank()) {
                ErrorBannerBox(
                    message = stringResource(
                        R.string.auth_username_not_registered,
                        notRegisteredUsername.orEmpty()
                    ),
                    modifier = Modifier.testTag("switch_user_not_registered_banner")
                )
            }

            Button(
                onClick = { submitSwitch() },
                enabled = !isAuthenticating,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("switch_username_confirm_button"),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandLeafGreen,
                    contentColor = Color.White
                )
            ) {
                if (isAuthenticating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.auth_logging_in),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.switch_username_confirm),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            OutlinedButton(
                onClick = { onRequestCreateVerifiedAccount(if (isPreviewVerified) "" else previewAccountName) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("switch_create_new_account_button"),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, extras.interactiveAccent),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = extras.interactiveAccent)
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.switch_username_create_new),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    val pendingForget = forgetCandidateAccount
    if (pendingForget != null) {
        val isActiveAccount = pendingForget.equals(currentAccountName, ignoreCase = true)
        ForgetSavedAccountDialog(
            accountName = pendingForget,
            isActiveSessionOnly = isActiveAccount,
            onConfirm = {
                forgetCandidateAccount = null
                onForgetSavedAccount(pendingForget)
                if (previewAccountName.equals(pendingForget, ignoreCase = true)) {
                    passwordInput = ""
                    isAutoFilledPassword = false
                    if (!isActiveAccount) {
                        usernameInput = OperatorIdentityFormatter.sanitizeUsername(currentAccountName)
                    }
                }
            },
            onDismiss = { forgetCandidateAccount = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VerifiedOperatorAccountsSection(
    verifiedAccounts: List<VerifiedOperatorAccount>,
    savedCredentialUsernames: Set<String> = emptySet(),
    selectedAccountName: String,
    chipTagPrefix: String,
    onSelectAccount: (VerifiedOperatorAccount) -> Unit,
    onForgetAccount: ((VerifiedOperatorAccount) -> Unit)? = null
) {
    val extras = SoundingThemeExtras.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.username_saved_list_label),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = extras.interactiveAccent
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            verifiedAccounts.forEach { account ->
                val selected = selectedAccountName.equals(account.accountName, ignoreCase = true)
                val hasSavedPassword = savedCredentialUsernames.any {
                    it.equals(account.accountName, ignoreCase = true)
                }
                val forgetCd = stringResource(R.string.cd_forget_saved_account, account.accountName)
                val savedKeyCd = stringResource(R.string.cd_saved_password_indicator)
                Surface(
                    modifier = Modifier.heightIn(min = 44.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) extras.leafContainer else extras.elevatedSurface,
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) extras.leafAccent else extras.cardBorder
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(topStart = 999.dp, bottomStart = 999.dp))
                                .clickable { onSelectAccount(account) }
                                .padding(
                                    start = 12.dp,
                                    end = if (onForgetAccount != null) 4.dp else 12.dp,
                                    top = 8.dp,
                                    bottom = 8.dp
                                )
                                .testTag("$chipTagPrefix${account.accountName}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = null,
                                tint = if (selected) extras.onLeafContainer else extras.leafAccentReadable,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = account.accountName,
                                style = MonoTypography.badge.copy(
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selected) extras.onLeafContainer else MaterialTheme.colorScheme.onSurface
                            )
                            if (hasSavedPassword) {
                                Icon(
                                    imageVector = Icons.Filled.Key,
                                    contentDescription = savedKeyCd,
                                    tint = if (selected) extras.onLeafContainer else extras.leafAccentReadable,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        if (onForgetAccount != null) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 2.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        role = Role.Button,
                                        onClick = { onForgetAccount(account) }
                                    )
                                    .semantics { contentDescription = forgetCd }
                                    .testTag("forget_${chipTagPrefix}${account.accountName}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = if (selected) {
                                        extras.onLeafContainer.copy(alpha = 0.78f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
