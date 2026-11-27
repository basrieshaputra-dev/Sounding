package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Email
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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

private enum class AuthScreenTab {
    LOGIN_VERIFIED,
    REGISTER_EMAIL
}

@Composable
fun OperatorAuthScreen(
    verifiedAccounts: List<VerifiedOperatorAccount>,
    pendingVerification: PendingEmailVerification?,
    initialAccountName: String,
    initialEmail: String = "",
    forceRegisterTab: Boolean = false,
    allowCancelBack: Boolean = false,
    isOnlineRealtime: Boolean = false,
    onLoginWithUsernamePassword: suspend (rawUsername: String, rawPassword: String) -> UsernamePasswordLoginResult,
    onResolvePasswordResetEmail: suspend (rawInput: String) -> ResolveResetEmailResult,
    onSavePendingVerification: (rawUsername: String, email: String, passwordHash: String) -> Unit,
    onClearPendingVerification: () -> Unit,
    onAccountEmailVerified: (rawUsername: String, email: String, passwordHash: String) -> Unit,
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
                        onAccountEmailVerified(
                            pending.accountName,
                            outcome.email,
                            pending.passwordHash
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
                when (val result = onLoginWithUsernamePassword(liveIdentifier, passwordInput)) {
                    is UsernamePasswordLoginResult.Success -> {
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
                        onSavePendingVerification(targetUsername, outcome.email, computedPasswordHash)
                        resendCooldownSec = 30
                    }
                    is SendEmailVerificationOutcome.AlreadyVerified -> {
                        onAccountEmailVerified(targetUsername, outcome.email, computedPasswordHash)
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
                        Text(
                            text = stringResource(R.string.auth_login_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.auth_login_subtitle),
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (verifiedAccounts.isNotEmpty()) {
                            VerifiedOperatorAccountsSection(
                                verifiedAccounts = verifiedAccounts,
                                selectedAccountName = previewAccountName,
                                chipTagPrefix = "saved_operator_chip_",
                                onSelectAccount = { account ->
                                    usernameInput = account.accountName
                                    usernameError = null
                                    passwordError = null
                                    notRegisteredUsername = null
                                    resetSuccessEmail = null
                                    resetErrorMessage = null
                                }
                            )
                        }

                        UsernameOutlinedField(
                            value = usernameInput,
                            errorText = usernameError,
                            tag = "username_login_input",
                            allowEmailIdentifier = true,
                            onValueChange = { raw ->
                                usernameInput = sanitizeLoginIdentifierInput(raw)
                                usernameError = null
                                notRegisteredUsername = null
                                resetErrorMessage = null
                            },
                            onDone = { submitUsernamePasswordLogin() }
                        )

                        AccountPreviewBanner(
                            previewAccountName = previewAccountName,
                            isVerifiedBadge = verifiedAccounts.any {
                                it.accountName.equals(previewAccountName, ignoreCase = true) ||
                                    (isLoginEmailTyped && it.email.equals(usernameInput, ignoreCase = true))
                            },
                            modifier = Modifier.testTag("login_account_preview")
                        )

                        // Password field + Lupa Password button right below it (no email field needed on Login tab)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            PasswordOutlinedField(
                                value = passwordInput,
                                visible = isPasswordVisible,
                                errorText = passwordError,
                                helperText = stringResource(R.string.auth_password_login_helper),
                                tag = "login_password_input",
                                onValueChange = {
                                    passwordInput = it
                                    if (passwordError != null) passwordError = null
                                    if (resetErrorMessage != null) resetErrorMessage = null
                                },
                                onToggleVisibility = { isPasswordVisible = !isPasswordVisible },
                                onDone = { submitUsernamePasswordLogin() }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
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
                    } else {
                        // REGISTER & STANDARD FIREBASE EMAIL VERIFICATION TAB
                        Text(
                            text = stringResource(R.string.auth_register_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.auth_register_subtitle),
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!isAwaitingVerification) {
                            UsernameOutlinedField(
                                value = usernameInput,
                                errorText = usernameError,
                                tag = "register_username_input",
                                onValueChange = { raw ->
                                    usernameInput = OperatorIdentityFormatter.sanitizeUsername(raw)
                                    usernameError = null
                                    statusBannerError = null
                                },
                                onDone = {}
                            )

                            AccountPreviewBanner(
                                previewAccountName = previewAccountName,
                                isVerifiedBadge = verifiedAccounts.any {
                                    it.accountName.equals(previewAccountName, ignoreCase = true)
                                },
                                modifier = Modifier.testTag("register_account_preview")
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
                                supportingText = {
                                    Text(text = emailError ?: stringResource(R.string.auth_email_helper))
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

                            PasswordOutlinedField(
                                value = passwordInput,
                                visible = isPasswordVisible,
                                errorText = passwordError,
                                helperText = stringResource(R.string.auth_password_register_helper),
                                tag = "register_password_input",
                                onValueChange = {
                                    passwordInput = it
                                    if (passwordError != null) passwordError = null
                                    if (statusBannerError != null) statusBannerError = null
                                },
                                onToggleVisibility = { isPasswordVisible = !isPasswordVisible },
                                onDone = { submitSendVerificationEmail(false) }
                            )

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

                    HorizontalDivider(color = extras.cardBorder)

                    // Compact hybrid status footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isOnlineRealtime) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = if (isOnlineRealtime) extras.leafAccentReadable else extras.interactiveAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.auth_offline_ready_hint),
                            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
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
private fun PasswordOutlinedField(
    value: String,
    visible: Boolean,
    errorText: String?,
    helperText: String,
    tag: String,
    onValueChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onDone: () -> Unit
) {
    val extras = SoundingThemeExtras.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        singleLine = true,
        label = { Text(stringResource(R.string.auth_password_label)) },
        placeholder = { Text(stringResource(R.string.auth_password_placeholder)) },
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
        supportingText = {
            Text(text = errorText ?: helperText)
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

@Composable
private fun UsernameOutlinedField(
    value: String,
    errorText: String?,
    tag: String,
    allowEmailIdentifier: Boolean = false,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
) {
    val extras = SoundingThemeExtras.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
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
        isError = errorText != null,
        supportingText = {
            Text(
                text = errorText ?: stringResource(
                    if (allowEmailIdentifier) R.string.auth_login_identifier_helper else R.string.username_input_helper
                )
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchUsernameBottomSheet(
    currentAccountName: String,
    verifiedAccounts: List<VerifiedOperatorAccount>,
    onAuthenticateSwitch: suspend (rawUsername: String, rawPassword: String) -> UsernamePasswordLoginResult,
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
                when (val result = onAuthenticateSwitch(clean, passwordInput)) {
                    is UsernamePasswordLoginResult.Success -> {
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
                    selectedAccountName = previewAccountName,
                    chipTagPrefix = "switch_saved_chip_",
                    onSelectAccount = { account ->
                        usernameInput = account.accountName
                        errorMsg = null
                        passwordError = null
                        notRegisteredUsername = null
                    }
                )
            }

            UsernameOutlinedField(
                value = usernameInput,
                errorText = errorMsg,
                tag = "switch_username_input",
                onValueChange = { raw ->
                    usernameInput = OperatorIdentityFormatter.sanitizeUsername(raw)
                    errorMsg = null
                    notRegisteredUsername = null
                },
                onDone = { submitSwitch() }
            )

            AccountPreviewBanner(
                previewAccountName = previewAccountName,
                isVerifiedBadge = isPreviewVerified,
                modifier = Modifier.testTag("switch_account_preview")
            )

            if (!isSameAsCurrent && previewAccountName.isNotBlank()) {
                PasswordOutlinedField(
                    value = passwordInput,
                    visible = isPasswordVisible,
                    errorText = passwordError,
                    helperText = stringResource(R.string.auth_password_login_helper),
                    tag = "switch_password_input",
                    onValueChange = {
                        passwordInput = it
                        if (passwordError != null) passwordError = null
                    },
                    onToggleVisibility = { isPasswordVisible = !isPasswordVisible },
                    onDone = { submitSwitch() }
                )
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VerifiedOperatorAccountsSection(
    verifiedAccounts: List<VerifiedOperatorAccount>,
    selectedAccountName: String,
    chipTagPrefix: String,
    onSelectAccount: (VerifiedOperatorAccount) -> Unit
) {
    val extras = SoundingThemeExtras.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.username_saved_list_label),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = extras.interactiveAccent
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            verifiedAccounts.forEach { account ->
                val selected = selectedAccountName.equals(account.accountName, ignoreCase = true)
                Surface(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clickable { onSelectAccount(account) }
                        .testTag("$chipTagPrefix${account.accountName}"),
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) extras.leafContainer else extras.elevatedSurface,
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) extras.leafAccent else extras.cardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
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
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountPreviewBanner(
    previewAccountName: String,
    isVerifiedBadge: Boolean = false,
    modifier: Modifier = Modifier
) {
    val extras = SoundingThemeExtras.colors
    val hasValidAccount = previewAccountName.isNotBlank()
    val displayText = if (hasValidAccount) {
        previewAccountName
    } else {
        stringResource(R.string.username_preview_empty)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (hasValidAccount) extras.leafContainer else extras.elevatedSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (hasValidAccount) extras.leafAccent.copy(alpha = 0.55f) else extras.cardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.username_preview_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = displayText,
                    style = MonoTypography.subMetric.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = if (hasValidAccount) extras.onLeafContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (hasValidAccount) {
                Icon(
                    imageVector = if (isVerifiedBadge) Icons.Filled.Verified else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = extras.leafAccentReadable,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
