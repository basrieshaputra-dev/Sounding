package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.LicenseManager
import com.example.data.local.LicenseState
import com.example.ui.AppUpdateUiState
import com.example.ui.components.AboutAppUpdateCard
import com.example.ui.theme.MonoTypography
import com.example.ui.theme.SoundingThemeExtras
import com.example.util.GitHubReleaseInfo
import com.example.util.SoundingFormatters

private const val ABOUT_LOG_TAG = "AboutScreen"
private val AboutCardShape = RoundedCornerShape(20.dp)
private val AboutCtaShape = RoundedCornerShape(16.dp)

@Immutable
private data class SafeAboutLicenseSnapshot(
    val isActivated: Boolean,
    val remainingTrialDays: Int,
    val trialProgress: Float,
    val activatedCode: String,
    val activatedAtLabel: String,
    val installDateLabel: String,
    val trialEndDateLabel: String,
    val deviceIdLabel: String,
    val heroStatusText: String,
    val developerStatusText: String
)

private fun buildSafeLicenseSnapshot(
    context: Context,
    licenseState: LicenseState
): SafeAboutLicenseSnapshot {
    val safeActivated = try {
        licenseState.isActivated
    } catch (_: Throwable) {
        false
    }

    val safeRemainingDays = try {
        val maxDays = LicenseManager.TRIAL_DURATION_DAYS.coerceAtLeast(1)
        licenseState.remainingTrialDays.coerceIn(0, maxDays)
    } catch (_: Throwable) {
        0
    }

    val safeProgress = try {
        val denom = LicenseManager.TRIAL_DURATION_DAYS.coerceAtLeast(1).toFloat()
        (safeRemainingDays.toFloat() / denom).coerceIn(0f, 1f)
    } catch (_: Throwable) {
        0f
    }

    val safeCode = try {
        licenseState.activatedCode.trim().ifEmpty { "-" }
    } catch (_: Throwable) {
        "-"
    }

    val safeActivatedAtLabel = try {
        val ms = licenseState.activatedAtEpochMs
        if (ms > 0L) SoundingFormatters.formatDisplayDateTime(ms) else "-"
    } catch (_: Throwable) {
        "-"
    }

    val safeInstallDateLabel = try {
        val ms = licenseState.firstLaunchEpochMs
        if (ms > 0L) SoundingFormatters.formatLocalDate(ms) else "-"
    } catch (_: Throwable) {
        "-"
    }

    val safeTrialEndDateLabel = try {
        val ms = licenseState.trialEndEpochMs
        if (ms > 0L) SoundingFormatters.formatLocalDate(ms) else "-"
    } catch (_: Throwable) {
        "-"
    }

    val safeDeviceId = try {
        licenseState.deviceId.trim().ifEmpty { "-" }
    } catch (_: Throwable) {
        "-"
    }

    val fallbackActiveLabel = try {
        context.getString(R.string.about_status_active_fallback)
    } catch (_: Throwable) {
        "Aktif"
    }

    val safeHeroStatus = try {
        if (safeActivated) {
            context.getString(R.string.about_caption_licensed)
        } else {
            context.getString(R.string.about_caption_trial_days, safeRemainingDays)
        }
    } catch (err: Throwable) {
        Log.w(ABOUT_LOG_TAG, "Fallback hero status text used", err)
        if (safeActivated) fallbackActiveLabel else "Trial • Sisa $safeRemainingDays Hari"
    }

    val safeDeveloperStatus = try {
        if (safeActivated) {
            context.getString(R.string.about_dev_value_status_licensed)
        } else {
            context.getString(R.string.about_dev_value_status_trial, safeRemainingDays)
        }
    } catch (err: Throwable) {
        Log.w(ABOUT_LOG_TAG, "Fallback developer status text used", err)
        if (safeActivated) fallbackActiveLabel else "Masa Trial 60 Hari • Sisa $safeRemainingDays Hari"
    }

    return SafeAboutLicenseSnapshot(
        isActivated = safeActivated,
        remainingTrialDays = safeRemainingDays,
        trialProgress = safeProgress,
        activatedCode = safeCode,
        activatedAtLabel = safeActivatedAtLabel,
        installDateLabel = safeInstallDateLabel,
        trialEndDateLabel = safeTrialEndDateLabel,
        deviceIdLabel = safeDeviceId,
        heroStatusText = safeHeroStatus,
        developerStatusText = safeDeveloperStatus
    )
}

private fun resolveSafeAppVersionLabel(context: Context): String {
    return try {
        val pm = context.packageManager
        val pkgName = context.packageName
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(
                pkgName,
                android.content.pm.PackageManager.PackageInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkgName, 0)
        }
        val versionName = info?.versionName?.trim().orEmpty().ifEmpty { "1.0" }
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info?.longVersionCode ?: 1L
        } else {
            @Suppress("DEPRECATION")
            (info?.versionCode ?: 1).toLong()
        }
        "v$versionName ($versionCode)"
    } catch (err: Throwable) {
        Log.w(ABOUT_LOG_TAG, "Fallback app version used", err)
        "v1.0"
    }
}

private fun runSafeAboutAction(
    context: Context,
    actionName: String,
    block: () -> Unit
) {
    try {
        block()
    } catch (err: Throwable) {
        Log.e(ABOUT_LOG_TAG, "Safe About action failed ($actionName)", err)
        runCatching {
            Toast.makeText(
                context,
                context.getString(R.string.msg_about_action_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

private fun copyDeviceIdSafely(
    context: Context,
    deviceId: String
) {
    runSafeAboutAction(context, "copyDeviceId") {
        val cleanId = deviceId.trim()
        if (cleanId.isEmpty() || cleanId == "-") {
            Toast.makeText(
                context,
                context.getString(R.string.msg_about_action_failed),
                Toast.LENGTH_SHORT
            ).show()
            return@runSafeAboutAction
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Device ID", cleanId))
            Toast.makeText(
                context,
                context.getString(R.string.msg_device_id_copied),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.msg_clipboard_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@Composable
fun AboutScreen(
    licenseState: LicenseState = LicenseState(),
    activeAccountName: String = "",
    isCloudConnected: Boolean = false,
    cloudEmail: String = "",
    isOnlineRealtime: Boolean = false,
    onOpenLicenseActivation: () -> Unit = {},
    onOpenSwitchAccount: () -> Unit = {},
    onConnectCloud: () -> Unit = {},
    onDisconnectCloud: () -> Unit = {},
    onLogoutAccount: () -> Unit = {},
    appUpdateState: AppUpdateUiState = AppUpdateUiState(),
    onCheckForUpdate: () -> Unit = {},
    onDownloadUpdate: (GitHubReleaseInfo) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Pure derived values computed safely once per state change (zero side effects on scroll)
    val safeLicense = remember(licenseState) {
        buildSafeLicenseSnapshot(context = context, licenseState = licenseState)
    }
    val appVersionLabel = remember(context) {
        resolveSafeAppVersionLabel(context)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("about_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        // Single safe vertical scroll container — no LazyColumn or nested scrollables
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AboutHeroIdentityCard(safeLicense = safeLicense)

            AboutAccountCard(
                activeAccountName = activeAccountName,
                isCloudConnected = isCloudConnected,
                cloudEmail = cloudEmail,
                isOnlineRealtime = isOnlineRealtime,
                onOpenSwitchAccount = {
                    runSafeAboutAction(context, "openSwitchAccount", onOpenSwitchAccount)
                },
                onConnectCloud = {
                    runSafeAboutAction(context, "connectCloud", onConnectCloud)
                },
                onRequestSignOut = {
                    runSafeAboutAction(context, "openSignOutDialog") {
                        showSignOutDialog = true
                    }
                },
                onRequestLogout = {
                    runSafeAboutAction(context, "openLogoutDialog") {
                        showLogoutDialog = true
                    }
                }
            )

            AboutLicenseStatusCard(
                safeLicense = safeLicense,
                onOpenLicenseActivation = {
                    runSafeAboutAction(context, "openLicenseActivation", onOpenLicenseActivation)
                },
                onCopyDeviceId = {
                    copyDeviceIdSafely(context, safeLicense.deviceIdLabel)
                }
            )

            AboutAppUpdateCard(
                updateState = appUpdateState,
                installedVersionLabel = appVersionLabel,
                onCheckForUpdate = {
                    runSafeAboutAction(context, "checkAppUpdate", onCheckForUpdate)
                },
                onDownloadUpdate = { release ->
                    runSafeAboutAction(context, "downloadAppUpdate") {
                        onDownloadUpdate(release)
                    }
                }
            )

            AboutFunctionSummaryCard()

            AboutNarrativeCard()

            AboutDeveloperCard(
                safeLicense = safeLicense,
                appVersionLabel = appVersionLabel
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.about_sign_out_confirm_title)) },
            text = { Text(stringResource(R.string.about_sign_out_confirm_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        runSafeAboutAction(context, "disconnectCloud", onDisconnectCloud)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_sign_out_button")
                ) {
                    Text(stringResource(R.string.about_action_sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.about_logout_confirm_title)) },
            text = { Text(stringResource(R.string.about_logout_confirm_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        runSafeAboutAction(context, "logoutAccount", onLogoutAccount)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_logout_account_button")
                ) {
                    Text(stringResource(R.string.about_action_logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun AboutAccountCard(
    activeAccountName: String,
    isCloudConnected: Boolean,
    cloudEmail: String,
    isOnlineRealtime: Boolean,
    onOpenSwitchAccount: () -> Unit,
    onConnectCloud: () -> Unit,
    onRequestSignOut: () -> Unit,
    onRequestLogout: () -> Unit
) {
    val extras = SoundingThemeExtras.colors
    val safeAccountDisplay = activeAccountName.trim().ifEmpty { "-" }
    val safeEmailDisplay = cloudEmail.trim()
    val statusAccent = if (isOnlineRealtime) extras.leafAccentReadable else MaterialTheme.colorScheme.onSurfaceVariant
    val statusBg = if (isOnlineRealtime) extras.leafContainer else extras.innerContainer
    val statusBorder = if (isOnlineRealtime) {
        extras.leafAccent.copy(alpha = 0.40f)
    } else {
        extras.cardBorder
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("about_account_card"),
        shape = AboutCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        shadowElevation = if (extras.isDark) 0.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Compact header: section title + live cloud badge on one row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SectionTitleRow(
                        icon = if (isOnlineRealtime) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                        title = stringResource(R.string.about_section_account),
                        accentColor = if (isOnlineRealtime) extras.leafAccentReadable else extras.interactiveAccent
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = statusBg,
                    border = BorderStroke(1.dp, statusBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusAccent)
                        )
                        Text(
                            text = stringResource(
                                if (isOnlineRealtime) R.string.status_online_realtime else R.string.status_offline_local
                            ),
                            style = MonoTypography.badge.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = statusAccent,
                            maxLines = 1
                        )
                    }
                }
            }

            // Unified single row: Active account + verified email/status + Ganti Akun button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = extras.innerContainer,
                border = BorderStroke(1.dp, extras.cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = safeAccountDisplay,
                            style = MonoTypography.subMetric.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("about_active_account_name")
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isCloudConnected) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = extras.leafAccentReadable,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = when {
                                    isCloudConnected && safeEmailDisplay.isNotEmpty() -> safeEmailDisplay
                                    isCloudConnected -> stringResource(R.string.about_account_signed_in)
                                    else -> stringResource(R.string.about_cloud_offline_short)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Button(
                        onClick = onOpenSwitchAccount,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("about_switch_account_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = extras.sagePrimary,
                            contentColor = extras.onSagePrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.action_switch_username),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                    }
                }
            }

            // Compact side-by-side secondary actions: Cloud Sync toggle + Logout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCloudConnected) {
                    OutlinedButton(
                        onClick = onRequestSignOut,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("about_sign_out_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.about_action_disconnect_cloud_short),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onConnectCloud,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("about_connect_cloud_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        border = BorderStroke(1.dp, extras.sagePrimary.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = extras.sagePrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudSync,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.about_action_connect_cloud_short),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (activeAccountName.isNotBlank()) {
                    OutlinedButton(
                        onClick = onRequestLogout,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("about_logout_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.about_action_logout_short),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutHeroIdentityCard(
    safeLicense: SafeAboutLicenseSnapshot
) {
    val extras = SoundingThemeExtras.colors
    val heroBrush = Brush.linearGradient(
        colors = listOf(
            extras.headerGradientStart,
            extras.headerGradientMid,
            extras.headerGradientEnd
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("about_hero_card"),
        shape = AboutCardShape,
        color = Color.Transparent,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroBrush)
                .drawBehind {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = size.minDimension * 0.58f,
                        center = Offset(size.width * 0.92f, size.height * 0.14f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = size.minDimension * 0.44f,
                        center = Offset(size.width * 0.08f, size.height * 0.92f)
                    )
                }
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Company Logo Badge (proportional in Warm Off-White #FFFDF8 container)
            Surface(
                modifier = Modifier
                    .size(64.dp)
                    .testTag("about_company_logo_badge"),
                shape = RoundedCornerShape(18.dp),
                color = extras.logoBadgeSurface,
                shadowElevation = 3.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_company_logo),
                        contentDescription = stringResource(R.string.cd_company_logo),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
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
                        fontSize = 22.sp,
                        lineHeight = 27.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("about_app_title")
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
            }

            // Company identity pill
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Business,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = stringResource(R.string.about_company_identity),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.15.sp
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Operational trial / license classification strip
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = Color.Black.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = safeLicense.heroStatusText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.about_caption_tagline),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White.copy(alpha = 0.88f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutLicenseStatusCard(
    safeLicense: SafeAboutLicenseSnapshot,
    onOpenLicenseActivation: () -> Unit,
    onCopyDeviceId: () -> Unit
) {
    val extras = SoundingThemeExtras.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("about_license_card"),
        shape = AboutCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        shadowElevation = if (extras.isDark) 0.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionTitleRow(
                icon = if (safeLicense.isActivated) Icons.Filled.Verified else Icons.Filled.VpnKey,
                title = stringResource(R.string.about_section_license),
                accentColor = if (safeLicense.isActivated) extras.leafAccentReadable else extras.interactiveAccent
            )

            if (safeLicense.isActivated) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = extras.leafContainer,
                    border = BorderStroke(1.dp, extras.leafAccent.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = null,
                                tint = extras.onLeafContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.about_license_active_badge),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = extras.onLeafContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = stringResource(
                                R.string.about_license_activated_summary,
                                safeLicense.activatedCode.ifBlank { "-" },
                                safeLicense.activatedAtLabel.ifBlank { "-" }
                            ),
                            style = MonoTypography.badge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                // Compact trial status strip: badge, progress bar, and combined install–end date range
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = extras.innerContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.about_license_trial_badge,
                                safeLicense.remainingTrialDays
                            ),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        LinearProgressIndicator(
                            progress = { safeLicense.trialProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = extras.sagePrimary,
                            trackColor = extras.cardBorder
                        )
                        Text(
                            text = stringResource(
                                R.string.about_license_period_range,
                                safeLicense.installDateLabel.ifBlank { "-" },
                                safeLicense.trialEndDateLabel.ifBlank { "-" }
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Button(
                    onClick = onOpenLicenseActivation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("about_open_activation_button"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extras.sagePrimary,
                        contentColor = extras.onSagePrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.VpnKey,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.about_license_action_enter_code),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = extras.cardBorder.copy(alpha = 0.7f))

            // Compact inline Device ID + Salin button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Smartphone,
                        contentDescription = null,
                        tint = extras.interactiveAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = stringResource(R.string.about_license_device_id_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = safeLicense.deviceIdLabel,
                            style = MonoTypography.badge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("about_device_id_value")
                        )
                    }
                }
                TextButton(
                    onClick = onCopyDeviceId,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("about_copy_device_id_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = extras.interactiveAccent
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = stringResource(R.string.about_action_copy_device_id),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.about_action_copy_short),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutFunctionSummaryCard() {
    val extras = SoundingThemeExtras.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("about_summary_card"),
        shape = AboutCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        shadowElevation = if (extras.isDark) 0.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitleRow(
                icon = Icons.Filled.Calculate,
                title = stringResource(R.string.about_section_functions),
                accentColor = extras.interactiveAccent
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureMiniTile(
                    icon = Icons.Filled.Science,
                    title = stringResource(R.string.about_feat_volume_title),
                    subtitle = stringResource(R.string.about_feat_volume_desc),
                    accentColor = extras.interactiveAccent,
                    modifier = Modifier.weight(1f)
                )
                FeatureMiniTile(
                    icon = Icons.Filled.Thermostat,
                    title = stringResource(R.string.about_feat_vcf_title),
                    subtitle = stringResource(R.string.about_feat_vcf_desc),
                    accentColor = extras.leafAccentReadable,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureMiniTile(
                    icon = Icons.Filled.Storage,
                    title = stringResource(R.string.about_feat_diagram_title),
                    subtitle = stringResource(R.string.about_feat_diagram_desc),
                    accentColor = extras.interactiveAccent,
                    modifier = Modifier.weight(1f)
                )
                FeatureMiniTile(
                    icon = Icons.Filled.Description,
                    title = stringResource(R.string.about_feat_report_title),
                    subtitle = stringResource(R.string.about_feat_report_desc),
                    accentColor = extras.leafAccentReadable,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FeatureMiniTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val extras = SoundingThemeExtras.colors
    Surface(
        modifier = modifier,
        shape = AboutCtaShape,
        color = extras.innerContainer,
        border = BorderStroke(1.dp, extras.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(accentColor.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AboutNarrativeCard() {
    val extras = SoundingThemeExtras.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("about_narrative_card"),
        shape = AboutCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        shadowElevation = if (extras.isDark) 0.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitleRow(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.about_section_about_app),
                accentColor = extras.interactiveAccent
            )

            Text(
                text = stringResource(R.string.about_paragraph_main),
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 21.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            AboutHighlightCallout(
                icon = Icons.Filled.OfflinePin,
                text = stringResource(R.string.about_highlight_offline),
                containerColor = extras.leafContainer,
                accentColor = extras.leafAccentReadable,
                textColor = MaterialTheme.colorScheme.onSurface
            )

            AboutHighlightCallout(
                icon = Icons.Filled.SyncAlt,
                text = stringResource(R.string.about_highlight_reverse_calc),
                containerColor = extras.resultSurface,
                accentColor = extras.interactiveAccent,
                textColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AboutHighlightCallout(
    icon: ImageVector,
    text: String,
    containerColor: Color,
    accentColor: Color,
    textColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                ),
                color = textColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AboutDeveloperCard(
    safeLicense: SafeAboutLicenseSnapshot,
    appVersionLabel: String
) {
    val extras = SoundingThemeExtras.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("about_developer_card"),
        shape = AboutCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        shadowElevation = if (extras.isDark) 0.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitleRow(
                icon = Icons.Filled.Verified,
                title = stringResource(R.string.about_section_developer),
                accentColor = extras.leafAccentReadable
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = AboutCtaShape,
                color = extras.innerContainer,
                border = BorderStroke(1.dp, extras.cardBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DeveloperSpecRow(
                        icon = Icons.Filled.Person,
                        label = stringResource(R.string.about_dev_label_name),
                        value = stringResource(R.string.about_dev_value_name),
                        useMonoValue = false
                    )
                    HorizontalDivider(
                        color = extras.cardBorder,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    DeveloperSpecRow(
                        icon = Icons.Filled.Badge,
                        label = stringResource(R.string.about_dev_label_id),
                        value = stringResource(R.string.about_dev_value_id),
                        useMonoValue = true
                    )
                    HorizontalDivider(
                        color = extras.cardBorder,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    DeveloperSpecRow(
                        icon = Icons.Filled.Business,
                        label = stringResource(R.string.about_dev_label_unit),
                        value = stringResource(R.string.about_dev_value_unit),
                        useMonoValue = false
                    )
                    HorizontalDivider(
                        color = extras.cardBorder,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    DeveloperSpecRow(
                        icon = Icons.Filled.Info,
                        label = stringResource(R.string.about_dev_label_version),
                        value = appVersionLabel.ifBlank { "v1.0" },
                        useMonoValue = true
                    )
                    HorizontalDivider(
                        color = extras.cardBorder,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OfflinePin,
                            contentDescription = null,
                            tint = extras.leafAccentReadable,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.about_dev_label_status),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = extras.leafContainer,
                                border = BorderStroke(1.dp, extras.leafAccent.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = safeLicense.developerStatusText,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = extras.onLeafContainer,
                                    modifier = Modifier
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                        .testTag("about_developer_status_text")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeveloperSpecRow(
    icon: ImageVector,
    label: String,
    value: String,
    useMonoValue: Boolean
) {
    val extras = SoundingThemeExtras.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = extras.interactiveAccent,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value.ifBlank { "-" },
                style = if (useMonoValue) {
                    MonoTypography.subMetric
                } else {
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 19.sp
                    )
                },
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SectionTitleRow(
    icon: ImageVector,
    title: String,
    accentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(accentColor)
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(19.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
