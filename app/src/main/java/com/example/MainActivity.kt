package com.example

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.firebase.EmailVerificationAuthHelper
import com.example.data.firebase.FirebaseSafeProvider
import com.example.ui.AppTab
import com.example.ui.AppUpdateStatus
import com.example.ui.AppUpdateUiState
import com.example.ui.AppUpdateViewModel
import com.example.ui.SoundingUiState
import com.example.ui.SoundingViewModel
import com.example.ui.UsernamePasswordLoginResult
import com.example.ui.components.ExportDraftJpgBottomSheet
import com.example.ui.components.ExportJpgReportSheet
import com.example.ui.components.UpdateAvailableBanner
import com.example.ui.components.UpdateAvailableDialog
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.BlendingScreen
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.DraftReportScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LicenseActivationBottomSheet
import com.example.ui.screens.LicenseExpiredScreen
import com.example.ui.screens.OperatorAuthScreen
import com.example.ui.screens.SwitchUsernameBottomSheet
import com.example.ui.theme.MonoTypography
import com.example.ui.theme.SoundingTangkiTheme
import com.example.ui.theme.SoundingThemeExtras
import com.example.util.AppUpdateChecker
import com.example.util.AppUpdateNotifier
import com.example.util.DailyReportJpgGenerator
import com.example.util.GitHubReleaseInfo
import com.example.util.ShiftDraftJpgGenerator
import com.example.util.SoundingFormatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class EmailVerificationRequest(
    val username: String,
    val email: String = ""
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseSafeProvider.ensureInitialized(applicationContext)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
            }
        }
        setContent {
            val viewModel: SoundingViewModel = viewModel(
                factory = SoundingViewModel.provideFactory(applicationContext)
            )
            val updateViewModel: AppUpdateViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val updateUiState by updateViewModel.uiState.collectAsStateWithLifecycle()
            var authOverlayRequest by remember { mutableStateOf<EmailVerificationRequest?>(null) }
            var hasRequestedNotifPermission by remember { mutableStateOf(false) }
            var deferredNotificationRelease by remember { mutableStateOf<GitHubReleaseInfo?>(null) }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                val queued = deferredNotificationRelease
                deferredNotificationRelease = null
                if (granted && queued != null) {
                    AppUpdateNotifier.showUpdateNotification(applicationContext, queued)
                }
            }

            // Automatic asynchronous check for latest GitHub release once when the app opens
            LaunchedEffect(Unit) {
                AppUpdateNotifier.ensureNotificationChannel(applicationContext)
                updateViewModel.checkAutomaticallyOnce()
            }

            // Dispatch local Android notification safely when an update is detected
            LaunchedEffect(updateUiState.pendingNotificationRelease) {
                val pendingRelease = updateUiState.pendingNotificationRelease ?: return@LaunchedEffect
                updateViewModel.consumePendingNotification()
                if (AppUpdateNotifier.hasNotificationPermission(applicationContext)) {
                    AppUpdateNotifier.showUpdateNotification(applicationContext, pendingRelease)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasRequestedNotifPermission) {
                    hasRequestedNotifPermission = true
                    deferredNotificationRelease = pendingRelease
                    runCatching {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }.onFailure {
                        deferredNotificationRelease = null
                    }
                }
            }

            fun openUpdateDownload(release: GitHubReleaseInfo) {
                val opened = AppUpdateChecker.openUpdateDownloadUrl(
                    context = this@MainActivity,
                    rawUrl = release.bestDownloadUrl
                )
                if (!opened) {
                    runCatching {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.msg_update_browser_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            val showAuthScreen = !uiState.isOperatorLoggedIn || authOverlayRequest != null

            SoundingTangkiTheme(darkTheme = uiState.isDarkTheme) {
                if (showAuthScreen) {
                    val req = authOverlayRequest
                    OperatorAuthScreen(
                        verifiedAccounts = uiState.verifiedAccounts,
                        savedCredentialUsernames = uiState.savedCredentialUsernames,
                        pendingVerification = uiState.pendingEmailVerification,
                        initialAccountName = req?.username ?: uiState.activeAccountName,
                        initialEmail = req?.email ?: uiState.activeOperatorEmail,
                        forceRegisterTab = req != null,
                        allowCancelBack = uiState.isOperatorLoggedIn && req != null,
                        isOnlineRealtime = uiState.isOnlineRealtime,
                        onLoadSavedPassword = viewModel::getSavedPasswordForAccount,
                        onForgetSavedAccount = viewModel::forgetSavedAccountCredentials,
                        onLoginWithUsernamePassword = { rawUsername, rawPassword, rememberCredentials ->
                            val result = viewModel.authenticateWithUsernameAndPassword(
                                rawIdentifier = rawUsername,
                                rawPassword = rawPassword,
                                rememberCredentials = rememberCredentials
                            )
                            if (result is UsernamePasswordLoginResult.Success) {
                                authOverlayRequest = null
                            }
                            result
                        },
                        onResolvePasswordResetEmail = viewModel::resolveEmailForPasswordReset,
                        onSavePendingVerification = { rawUsername, email, passwordHash, rawPasswordToRemember ->
                            viewModel.savePendingEmailVerification(
                                rawUsername = rawUsername,
                                email = email,
                                passwordHash = passwordHash,
                                rawPasswordToRemember = rawPasswordToRemember
                            )
                        },
                        onClearPendingVerification = {
                            viewModel.clearPendingEmailVerification()
                        },
                        onAccountEmailVerified = { rawUsername, email, passwordHash, rawPasswordToRemember ->
                            viewModel.completeVerifiedAccountRegistration(
                                rawUsername = rawUsername,
                                email = email,
                                passwordHash = passwordHash,
                                rawPasswordToRemember = rawPasswordToRemember
                            )
                            authOverlayRequest = null
                        },
                        onCancelBack = { authOverlayRequest = null }
                    )
                } else {
                    SoundingTangkiApp(
                        uiState = uiState,
                        viewModel = viewModel,
                        updateUiState = updateUiState,
                        onCheckForUpdate = { updateViewModel.checkForUpdates(isManual = true) },
                        onOpenUpdateDialog = updateViewModel::openUpdateDialog,
                        onDismissUpdateBanner = updateViewModel::dismissUpdateBanner,
                        onDownloadUpdate = ::openUpdateDownload,
                        onOpenEmailVerification = { username, email ->
                            authOverlayRequest = EmailVerificationRequest(
                                username = username,
                                email = email
                            )
                        }
                    )
                }

                val dialogRelease = updateUiState.availableRelease
                if (updateUiState.showUpdateDialog && dialogRelease != null) {
                    UpdateAvailableDialog(
                        currentVersionLabel = updateUiState.currentVersionLabel,
                        release = dialogRelease,
                        onDownloadApk = { release ->
                            updateViewModel.dismissUpdateDialog()
                            openUpdateDownload(release)
                        },
                        onDismiss = updateViewModel::dismissUpdateDialog
                    )
                }
            }
        }
    }
}

@Composable
fun SoundingTangkiApp(
    uiState: SoundingUiState,
    viewModel: SoundingViewModel,
    updateUiState: AppUpdateUiState,
    onCheckForUpdate: () -> Unit,
    onOpenUpdateDialog: () -> Unit,
    onDismissUpdateBanner: () -> Unit,
    onDownloadUpdate: (GitHubReleaseInfo) -> Unit,
    onOpenEmailVerification: (username: String, email: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showExportJpgSheet by remember { mutableStateOf(false) }
    var showExportDraftJpgSheet by remember { mutableStateOf(false) }
    var showLicenseActivationSheet by remember { mutableStateOf(false) }
    var showSwitchUsernameSheet by remember { mutableStateOf(false) }
    var pendingJpgBytes by remember { mutableStateOf<ByteArray?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshLicenseState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun showFeedback(message: String) {
        scope.launch {
            runCatching {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    // Surface any Firebase / Firestore / local persistence error on screen cleanly instead of force-closing
    LaunchedEffect(uiState.cloudSyncError) {
        val err = uiState.cloudSyncError
        if (!err.isNullOrBlank()) {
            runCatching {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = err,
                    duration = SnackbarDuration.Long
                )
            }
            viewModel.clearCloudError()
        }
    }

    // Surface feedback after a manual "Cek Update" action from About screen
    LaunchedEffect(updateUiState.manualCheckSequence) {
        if (updateUiState.manualCheckSequence <= 0) return@LaunchedEffect
        val feedbackMessage = when (updateUiState.status) {
            AppUpdateStatus.UP_TO_DATE -> context.getString(
                R.string.msg_update_up_to_date,
                updateUiState.latestTagSeen.ifBlank { updateUiState.currentVersionLabel }
            )
            AppUpdateStatus.UPDATE_AVAILABLE -> context.getString(
                R.string.msg_update_found,
                updateUiState.availableRelease?.displayVersion ?: "-"
            )
            AppUpdateStatus.OFFLINE_OR_TIMEOUT -> context.getString(R.string.about_update_status_offline)
            AppUpdateStatus.RATE_LIMITED -> context.getString(R.string.about_update_status_rate_limited)
            AppUpdateStatus.NO_RELEASES_YET -> context.getString(R.string.about_update_status_no_release)
            AppUpdateStatus.ERROR -> context.getString(R.string.about_update_status_error)
            else -> null
        }
        if (!feedbackMessage.isNullOrBlank()) {
            showFeedback(feedbackMessage)
        }
    }

    fun openEmailCloudVerification() {
        onOpenEmailVerification(
            uiState.activeAccountName,
            uiState.activeOperatorEmail.ifBlank { uiState.currentUserEmail }
        )
    }

    val csvDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val csvData = viewModel.getCsvExportText() ?: return@rememberLauncherForActivityResult
        scope.launch {
            val savedOk = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(csvData.toByteArray(Charsets.UTF_8))
                    } != null
                }.getOrDefault(false)
            }
            showFeedback(
                context.getString(
                    if (savedOk) R.string.msg_csv_exported else R.string.msg_csv_export_failed
                )
            )
        }
    }

    val jpgDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri ->
        val bytes = pendingJpgBytes
        pendingJpgBytes = null
        if (uri == null || bytes == null) return@rememberLauncherForActivityResult
        scope.launch {
            val savedOk = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(bytes)
                    } != null
                }.getOrDefault(false)
            }
            showFeedback(
                context.getString(
                    if (savedOk) R.string.msg_jpg_saved else R.string.msg_jpg_failed
                )
            )
        }
    }

    // When 60-day trial has expired and no permanent license is active, lock the app behind activation
    if (uiState.licenseState.isTrialExpired && !uiState.licenseState.isActivated) {
        LicenseExpiredScreen(
            onActivateLicense = viewModel::activateLicense,
            onActivatedSuccess = { code ->
                showFeedback(context.getString(R.string.msg_license_activated, code))
            }
        )
        return
    }

    // Hardware back from secondary tabs returns cleanly to Calculator tab
    BackHandler(enabled = uiState.selectedTab != AppTab.CALCULATOR) {
        viewModel.selectTab(AppTab.CALCULATOR)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 720.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                AppBrandHeader(
                    activeAccountName = uiState.activeAccountName,
                    isDarkTheme = uiState.isDarkTheme,
                    isCloudConnected = uiState.isCloudConnected,
                    isOnlineRealtime = uiState.isOnlineRealtime,
                    isAboutSelected = uiState.selectedTab == AppTab.ABOUT,
                    onOpenSwitchAccount = { showSwitchUsernameSheet = true },
                    onConnectCloud = ::openEmailCloudVerification,
                    onToggleTheme = viewModel::toggleTheme,
                    onOpenAbout = {
                        viewModel.selectTab(
                            if (uiState.selectedTab == AppTab.ABOUT) AppTab.CALCULATOR else AppTab.ABOUT
                        )
                    }
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    SoundingBottomNav(
                        selectedTab = uiState.selectedTab,
                        logCount = uiState.historyRecords.size,
                        onSelectTab = viewModel::selectTab
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    SoundingNavRail(
                        selectedTab = uiState.selectedTab,
                        logCount = uiState.historyRecords.size,
                        onSelectTab = viewModel::selectTab
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val bannerRelease = updateUiState.availableRelease
                    if (updateUiState.showUpdateBanner && bannerRelease != null) {
                        UpdateAvailableBanner(
                            currentVersionLabel = updateUiState.currentVersionLabel,
                            release = bannerRelease,
                            onOpenDetails = onOpenUpdateDialog,
                            onDismiss = onDismissUpdateBanner
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (!uiState.isLoaded) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = SoundingThemeExtras.colors.leafAccent
                            )
                        } else {
                        when (uiState.selectedTab) {
                            AppTab.CALCULATOR -> CalculatorScreen(
                                uiState = uiState,
                                onSelectTank = viewModel::selectTank,
                                onSelectProduct = viewModel::selectProduct,
                                onToggleFavoriteProduct = viewModel::toggleFavoriteProduct,
                                onSoundingChanged = viewModel::updateSoundingInput,
                                onTempChanged = viewModel::updateTempInput,
                                onDiagramStockKgChanged = viewModel::updateDiagramStockKgInput,
                                onDiagramStockKgEditDone = viewModel::commitDiagramStockKgEdit,
                                onManualKgChanged = viewModel::updateManualKgInput,
                                onManualNoteChanged = viewModel::updateManualNoteInput,
                                onResetInput = viewModel::resetSoundingInput,
                                onSaveHistory = {
                                    viewModel.saveCurrentCalculation { tankId, kgFormatted ->
                                        showFeedback(
                                            context.getString(R.string.msg_saved, tankId, kgFormatted)
                                        )
                                    }
                                },
                                onCopyWhatsApp = { calc ->
                                    val summary = SoundingFormatters.formatWhatsAppSummary(calc)
                                    val copied = copyToClipboard(context, summary)
                                    showFeedback(
                                        context.getString(
                                            if (copied) R.string.msg_copied_wa else R.string.msg_clipboard_failed
                                        )
                                    )
                                },
                                onShareWhatsApp = { calc ->
                                    val summary = SoundingFormatters.formatWhatsAppSummary(calc)
                                    if (!sharePlainText(context, summary)) {
                                        showFeedback(context.getString(R.string.msg_share_failed))
                                    }
                                }
                            )

                            AppTab.BLENDING -> BlendingScreen(
                                activeAccountName = uiState.activeAccountName,
                                onCopyBlendingSummary = { summary ->
                                    val copied = copyToClipboard(context, summary)
                                    showFeedback(
                                        context.getString(
                                            if (copied) R.string.msg_blending_copied else R.string.msg_clipboard_failed
                                        )
                                    )
                                }
                            )

                            AppTab.HISTORY -> HistoryScreen(
                                uiState = uiState,
                                onSelectDateFilter = viewModel::setHistoryDateFilter,
                                onSelectAccountFilter = viewModel::setHistoryAccountFilter,
                                onOpenExportJpg = {
                                    if (uiState.logs.isEmpty()) {
                                        showFeedback(context.getString(R.string.msg_no_data_export))
                                    } else {
                                        showExportJpgSheet = true
                                    }
                                },
                                onExportCsv = {
                                    if (uiState.logs.isEmpty()) {
                                        showFeedback(context.getString(R.string.msg_no_data_export))
                                    } else {
                                        runCatching {
                                            csvDocumentLauncher.launch("rekap_sounding_tangki.csv")
                                        }.onFailure {
                                            showFeedback(context.getString(R.string.msg_file_picker_failed))
                                        }
                                    }
                                },
                                onCopyLogWa = { log ->
                                    val summary = SoundingFormatters.formatLogWhatsAppSummary(log)
                                    val copied = copyToClipboard(context, summary)
                                    showFeedback(
                                        context.getString(
                                            if (copied) R.string.msg_copied_wa else R.string.msg_clipboard_failed
                                        )
                                    )
                                },
                                onDeleteRecord = { record ->
                                    viewModel.deleteHistoryRecord(record) { deleted, tankId ->
                                        if (deleted) {
                                            showFeedback(context.getString(R.string.msg_deleted, tankId))
                                        } else {
                                            showFeedback(context.getString(R.string.msg_cannot_delete_other_shift))
                                        }
                                    }
                                },
                                onClearAllLogs = {
                                    viewModel.clearLocalLogs {
                                        showFeedback(context.getString(R.string.msg_all_cleared))
                                    }
                                }
                            )

                            AppTab.DRAFT -> DraftReportScreen(
                                draft = uiState.shiftDraft,
                                historyLogs = uiState.historyRecords.map { it.entity },
                                tankCapacitiesTon = remember(uiState.tanks) {
                                    uiState.tanks.associate { it.id to it.capacityTon }
                                },
                                cloudShiftReports = uiState.filteredCloudShiftReports,
                                availableDraftDates = uiState.availableDraftDates,
                                availableDraftAccounts = uiState.availableDraftAccounts,
                                selectedDraftDateFilter = uiState.draftFilterDateKey,
                                selectedDraftAccountFilter = uiState.draftFilterAccount,
                                isViewingCloudReport = uiState.isViewingCloudReport,
                                onSelectDraftDateFilter = viewModel::setDraftDateFilter,
                                onSelectDraftAccountFilter = viewModel::setDraftAccountFilter,
                                onOpenCloudShiftReport = { report ->
                                    viewModel.openCloudShiftReport(report)
                                    showFeedback(
                                        context.getString(
                                            R.string.msg_loaded_shift_report,
                                            report.dateLabel.ifBlank { report.dateKey },
                                            report.displayAccountName.ifBlank { "-" }
                                        )
                                    )
                                },
                                onReturnToActiveDraft = viewModel::returnToCurrentDraft,
                                onSaveDraftToCloud = {
                                    viewModel.saveDraftToCloudNow { synced ->
                                        showFeedback(
                                            context.getString(
                                                if (synced && uiState.isOnlineRealtime) {
                                                    R.string.msg_draft_saved_cloud
                                                } else {
                                                    R.string.msg_draft_saved_offline
                                                }
                                            )
                                        )
                                    }
                                },
                                onDateChanged = viewModel::updateDraftDate,
                                onOperatorChanged = viewModel::updateDraftOperator,
                                onNbChanged = viewModel::updateDraftNb,
                                onUpdateTankRow = viewModel::updateDraftTankRow,
                                onSaveTransferRow = viewModel::saveDraftTransfer,
                                onDeleteTransferRow = viewModel::deleteDraftTransfer,
                                onSyncFromHistory = {
                                    viewModel.syncDraftFromHistory()
                                    showFeedback(context.getString(R.string.msg_draft_synced))
                                },
                                onResetDraft = {
                                    viewModel.resetShiftDraft()
                                    showFeedback(context.getString(R.string.msg_draft_reset))
                                },
                                onOpenExportDraftJpg = { showExportDraftJpgSheet = true }
                            )

                            AppTab.ABOUT -> AboutScreen(
                                licenseState = uiState.licenseState,
                                activeAccountName = uiState.activeAccountName,
                                isCloudConnected = uiState.isCloudConnected,
                                cloudEmail = uiState.currentUserEmail.ifBlank { uiState.activeOperatorEmail },
                                isOnlineRealtime = uiState.isOnlineRealtime,
                                onOpenLicenseActivation = { showLicenseActivationSheet = true },
                                onOpenSwitchAccount = { showSwitchUsernameSheet = true },
                                onConnectCloud = ::openEmailCloudVerification,
                                onDisconnectCloud = {
                                    scope.launch {
                                        runCatching {
                                            EmailVerificationAuthHelper.performFirebaseSignOut(context)
                                        }
                                        showFeedback(context.getString(R.string.msg_cloud_disconnected))
                                    }
                                },
                                onLogoutAccount = {
                                    viewModel.logoutOperatorSession()
                                },
                                appUpdateState = updateUiState,
                                onCheckForUpdate = onCheckForUpdate,
                                onDownloadUpdate = onDownloadUpdate
                            )
                        }
                    }
                    }
                }
            }
        }
    }

    if (showExportJpgSheet) {
        ExportJpgReportSheet(
            initialOperatorName = uiState.lastOperatorName,
            latestLogEpochMs = uiState.logs.firstOrNull()?.createdAtEpochMs,
            onShareJpg = { metadata ->
                showExportJpgSheet = false
                viewModel.saveLastOperator(metadata.operatorName)
                val logsSnapshot = uiState.logs
                scope.launch {
                    val fileResult = withContext(Dispatchers.IO) {
                        runCatching {
                            val bytes = DailyReportJpgGenerator.renderToJpegBytes(
                                context = context,
                                logs = logsSnapshot,
                                metadata = metadata
                            )
                            DailyReportJpgGenerator.writeReportToCacheFile(context, bytes)
                        }
                    }
                    val file = fileResult.getOrNull()
                    if (file != null) {
                        if (!shareJpgImage(context, file)) {
                            showFeedback(context.getString(R.string.msg_share_failed))
                        }
                    } else {
                        showFeedback(context.getString(R.string.msg_jpg_failed))
                    }
                }
            },
            onDownloadJpg = { metadata ->
                showExportJpgSheet = false
                viewModel.saveLastOperator(metadata.operatorName)
                val logsSnapshot = uiState.logs
                scope.launch {
                    val bytesResult = withContext(Dispatchers.IO) {
                        runCatching {
                            DailyReportJpgGenerator.renderToJpegBytes(
                                context = context,
                                logs = logsSnapshot,
                                metadata = metadata
                            )
                        }
                    }
                    val bytes = bytesResult.getOrNull()
                    if (bytes != null) {
                        pendingJpgBytes = bytes
                        runCatching {
                            jpgDocumentLauncher.launch("laporan_sounding_harian.jpg")
                        }.onFailure {
                            pendingJpgBytes = null
                            showFeedback(context.getString(R.string.msg_file_picker_failed))
                        }
                    } else {
                        showFeedback(context.getString(R.string.msg_jpg_failed))
                    }
                }
            },
            onDismiss = { showExportJpgSheet = false }
        )
    }

    if (showExportDraftJpgSheet) {
        ExportDraftJpgBottomSheet(
            onShareJpg = {
                showExportDraftJpgSheet = false
                val draftSnapshot = uiState.shiftDraft
                scope.launch {
                    val fileResult = withContext(Dispatchers.IO) {
                        runCatching {
                            val bytes = ShiftDraftJpgGenerator.renderToJpegBytes(
                                context = context,
                                draft = draftSnapshot
                            )
                            ShiftDraftJpgGenerator.writeDraftToCacheFile(context, bytes)
                        }
                    }
                    val file = fileResult.getOrNull()
                    if (file != null) {
                        if (!shareJpgImage(context, file)) {
                            showFeedback(context.getString(R.string.msg_share_failed))
                        }
                    } else {
                        showFeedback(context.getString(R.string.msg_draft_jpg_failed))
                    }
                }
            },
            onDownloadJpg = {
                showExportDraftJpgSheet = false
                val draftSnapshot = uiState.shiftDraft
                scope.launch {
                    val bytesResult = withContext(Dispatchers.IO) {
                        runCatching {
                            ShiftDraftJpgGenerator.renderToJpegBytes(
                                context = context,
                                draft = draftSnapshot
                            )
                        }
                    }
                    val bytes = bytesResult.getOrNull()
                    if (bytes != null) {
                        pendingJpgBytes = bytes
                        runCatching {
                            jpgDocumentLauncher.launch("draft_laporan_shift.jpg")
                        }.onFailure {
                            pendingJpgBytes = null
                            showFeedback(context.getString(R.string.msg_file_picker_failed))
                        }
                    } else {
                        showFeedback(context.getString(R.string.msg_draft_jpg_failed))
                    }
                }
            },
            onDismiss = { showExportDraftJpgSheet = false }
        )
    }

    if (showLicenseActivationSheet && !uiState.licenseState.isActivated) {
        LicenseActivationBottomSheet(
            onActivateLicense = viewModel::activateLicense,
            onActivatedSuccess = { code ->
                showLicenseActivationSheet = false
                showFeedback(context.getString(R.string.msg_license_activated, code))
            },
            onDismiss = { showLicenseActivationSheet = false }
        )
    }

    if (showSwitchUsernameSheet) {
        SwitchUsernameBottomSheet(
            currentAccountName = uiState.activeAccountName,
            verifiedAccounts = uiState.verifiedAccounts,
            savedCredentialUsernames = uiState.savedCredentialUsernames,
            onLoadSavedPassword = viewModel::getSavedPasswordForAccount,
            onForgetSavedAccount = { forgottenAccount ->
                viewModel.forgetSavedAccountCredentials(forgottenAccount)
                showFeedback(
                    context.getString(R.string.msg_saved_credentials_removed, forgottenAccount)
                )
            },
            onAuthenticateSwitch = { rawUsername, rawPassword, rememberCredentials ->
                val result = viewModel.authenticateWithUsernameAndPassword(
                    rawIdentifier = rawUsername,
                    rawPassword = rawPassword,
                    rememberCredentials = rememberCredentials
                )
                if (result is UsernamePasswordLoginResult.Success) {
                    showSwitchUsernameSheet = false
                    showFeedback(
                        context.getString(R.string.msg_username_switched, result.accountName)
                    )
                }
                result
            },
            onRequestCreateVerifiedAccount = { rawUsername ->
                showSwitchUsernameSheet = false
                onOpenEmailVerification(rawUsername, "")
            },
            onDismiss = { showSwitchUsernameSheet = false }
        )
    }
}

@Composable
private fun AppBrandHeader(
    activeAccountName: String,
    isDarkTheme: Boolean,
    isCloudConnected: Boolean,
    isOnlineRealtime: Boolean,
    isAboutSelected: Boolean,
    onOpenSwitchAccount: () -> Unit,
    onConnectCloud: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val extras = SoundingThemeExtras.colors
    val headerBrush = Brush.horizontalGradient(
        colors = listOf(
            extras.headerGradientStart,
            extras.headerGradientMid,
            extras.headerGradientEnd
        )
    )
    val toggleCd = stringResource(
        if (isDarkTheme) R.string.cd_switch_to_light else R.string.cd_switch_to_dark
    )
    val aboutCd = stringResource(R.string.cd_open_about)
    val switchAccountCd = stringResource(R.string.cd_switch_username)
    val logoCd = stringResource(R.string.cd_company_logo)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shadowElevation = if (isDarkTheme) 0.dp else 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBrush)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Company Logo inside soft Warm Off-White (#FFFDF8) 14dp rounded badge
                Surface(
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("header_company_logo_badge"),
                    shape = RoundedCornerShape(14.dp),
                    color = extras.logoBadgeSurface,
                    shadowElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_company_logo),
                            contentDescription = logoCd,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Compact Title + Inline Account Pill + Subtitle
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.app_header_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                lineHeight = 20.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("header_title")
                        )

                        if (activeAccountName.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color.White.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable(
                                        role = Role.Button,
                                        onClick = onOpenSwitchAccount
                                    )
                                    .semantics { contentDescription = switchAccountCd }
                                    .testTag("header_switch_account_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.92f),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = activeAccountName,
                                        style = MonoTypography.badge.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.app_header_subtitle),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            letterSpacing = 0.45.sp
                        ),
                        color = extras.headerSubtitleAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("header_subtitle")
                    )

                    // Compact Connection Status Pill neatly tucked below subtitle
                    val statusBg = if (isOnlineRealtime) {
                        extras.emeraldAccent.copy(alpha = 0.22f)
                    } else {
                        extras.amberAccent.copy(alpha = 0.24f)
                    }
                    val statusBorder = if (isOnlineRealtime) {
                        extras.emeraldAccent.copy(alpha = 0.65f)
                    } else {
                        extras.amberAccent.copy(alpha = 0.65f)
                    }
                    val dotColor = if (isOnlineRealtime) extras.onlineIndicatorDot else extras.amberAccent

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = statusBg,
                        border = BorderStroke(1.dp, statusBorder),
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(
                                enabled = !isCloudConnected,
                                role = Role.Button,
                                onClick = onConnectCloud
                            )
                            .testTag("header_connection_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.5.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Icon(
                                imageVector = if (isOnlineRealtime) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (isOnlineRealtime) Color(0xFFA7F3D0) else Color(0xFFFDE68A),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = stringResource(
                                    if (isOnlineRealtime) R.string.status_online_realtime else R.string.status_offline_local
                                ),
                                style = MonoTypography.badge.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Right Utility Controls: Compact About & Theme Switcher
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val pillShape = RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(pillShape)
                            .clickable(
                                role = Role.Button,
                                onClick = onOpenAbout
                            )
                            .semantics { contentDescription = aboutCd }
                            .testTag("header_about_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = pillShape,
                            color = if (isAboutSelected) {
                                extras.logoBadgeSurface
                            } else {
                                Color.White.copy(alpha = 0.16f)
                            }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAboutSelected) Icons.Filled.Info else Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = if (isAboutSelected) extras.headerGradientStart else Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(pillShape)
                            .clickable(
                                role = Role.Switch,
                                onClick = onToggleTheme
                            )
                            .semantics { contentDescription = toggleCd }
                            .testTag("theme_toggle_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = pillShape,
                            color = Color.White.copy(alpha = 0.16f)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
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
private fun SoundingBottomNav(
    selectedTab: AppTab,
    logCount: Int,
    onSelectTab: (AppTab) -> Unit
) {
    val extras = SoundingThemeExtras.colors
    val navColors = NavigationBarItemDefaults.colors(
        selectedIconColor = extras.navSelectedIcon,
        selectedTextColor = extras.navSelectedText,
        indicatorColor = extras.navIndicator,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(
            color = extras.cardBorder.copy(alpha = 0.7f),
            thickness = 0.8.dp
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                selected = selectedTab == AppTab.CALCULATOR,
                onClick = { onSelectTab(AppTab.CALCULATOR) },
                colors = navColors,
                icon = {
                    Icon(
                        imageVector = if (selectedTab == AppTab.CALCULATOR) {
                            Icons.Filled.Calculate
                        } else {
                            Icons.Outlined.Calculate
                        },
                        contentDescription = stringResource(R.string.tab_calculator)
                    )
                },
                label = {
                    Text(
                        text = stringResource(R.string.tab_calculator),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.testTag("nav_tab_calculator")
            )

            NavigationBarItem(
                selected = selectedTab == AppTab.BLENDING,
                onClick = { onSelectTab(AppTab.BLENDING) },
                colors = navColors,
                icon = {
                    Icon(
                        imageVector = if (selectedTab == AppTab.BLENDING) {
                            Icons.Filled.Science
                        } else {
                            Icons.Outlined.Science
                        },
                        contentDescription = stringResource(R.string.tab_blending_full)
                    )
                },
                label = {
                    Text(
                        text = stringResource(R.string.tab_blending),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.testTag("nav_tab_blending")
            )

            NavigationBarItem(
                selected = selectedTab == AppTab.HISTORY,
                onClick = { onSelectTab(AppTab.HISTORY) },
                colors = navColors,
                icon = {
                    BadgedBox(
                        badge = {
                            if (logCount > 0) {
                                Badge(
                                    containerColor = extras.sagePrimary,
                                    contentColor = extras.onSagePrimary
                                ) {
                                    Text(text = logCount.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (selectedTab == AppTab.HISTORY) {
                                Icons.Filled.History
                            } else {
                                Icons.Outlined.History
                            },
                            contentDescription = stringResource(R.string.tab_history)
                        )
                    }
                },
                label = {
                    Text(
                        text = stringResource(R.string.tab_history),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.testTag("nav_tab_history")
            )

            NavigationBarItem(
                selected = selectedTab == AppTab.DRAFT,
                onClick = { onSelectTab(AppTab.DRAFT) },
                colors = navColors,
                icon = {
                    Icon(
                        imageVector = if (selectedTab == AppTab.DRAFT) {
                            Icons.Filled.EditNote
                        } else {
                            Icons.Outlined.EditNote
                        },
                        contentDescription = stringResource(R.string.tab_draft_full)
                    )
                },
                label = {
                    Text(
                        text = stringResource(R.string.tab_draft),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.testTag("nav_tab_draft")
            )

            NavigationBarItem(
                selected = selectedTab == AppTab.ABOUT,
                onClick = { onSelectTab(AppTab.ABOUT) },
                colors = navColors,
                icon = {
                    Icon(
                        imageVector = if (selectedTab == AppTab.ABOUT) {
                            Icons.Filled.Info
                        } else {
                            Icons.Outlined.Info
                        },
                        contentDescription = stringResource(R.string.tab_about)
                    )
                },
                label = {
                    Text(
                        text = stringResource(R.string.tab_about),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.testTag("nav_tab_about")
            )
        }
    }
}

@Composable
private fun SoundingNavRail(
    selectedTab: AppTab,
    logCount: Int,
    onSelectTab: (AppTab) -> Unit
) {
    val extras = SoundingThemeExtras.colors
    val railColors = NavigationRailItemDefaults.colors(
        selectedIconColor = extras.navSelectedIcon,
        selectedTextColor = extras.navSelectedText,
        indicatorColor = extras.navIndicator,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        NavigationRailItem(
            selected = selectedTab == AppTab.CALCULATOR,
            onClick = { onSelectTab(AppTab.CALCULATOR) },
            colors = railColors,
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.CALCULATOR) {
                        Icons.Filled.Calculate
                    } else {
                        Icons.Outlined.Calculate
                    },
                    contentDescription = stringResource(R.string.tab_calculator)
                )
            },
            label = { Text(stringResource(R.string.tab_calculator)) },
            modifier = Modifier.testTag("nav_rail_calculator")
        )

        NavigationRailItem(
            selected = selectedTab == AppTab.BLENDING,
            onClick = { onSelectTab(AppTab.BLENDING) },
            colors = railColors,
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.BLENDING) {
                        Icons.Filled.Science
                    } else {
                        Icons.Outlined.Science
                    },
                    contentDescription = stringResource(R.string.tab_blending_full)
                )
            },
            label = { Text(stringResource(R.string.tab_blending)) },
            modifier = Modifier.testTag("nav_rail_blending")
        )

        NavigationRailItem(
            selected = selectedTab == AppTab.HISTORY,
            onClick = { onSelectTab(AppTab.HISTORY) },
            colors = railColors,
            icon = {
                BadgedBox(
                    badge = {
                        if (logCount > 0) {
                            Badge(
                                containerColor = extras.sagePrimary,
                                contentColor = extras.onSagePrimary
                            ) {
                                Text(text = logCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (selectedTab == AppTab.HISTORY) {
                            Icons.Filled.History
                        } else {
                            Icons.Outlined.History
                        },
                        contentDescription = stringResource(R.string.tab_history)
                    )
                }
            },
            label = { Text(stringResource(R.string.tab_history)) },
            modifier = Modifier.testTag("nav_rail_history")
        )

        NavigationRailItem(
            selected = selectedTab == AppTab.DRAFT,
            onClick = { onSelectTab(AppTab.DRAFT) },
            colors = railColors,
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.DRAFT) {
                        Icons.Filled.EditNote
                    } else {
                        Icons.Outlined.EditNote
                    },
                    contentDescription = stringResource(R.string.tab_draft_full)
                )
            },
            label = { Text(stringResource(R.string.tab_draft)) },
            modifier = Modifier.testTag("nav_rail_draft")
        )

        NavigationRailItem(
            selected = selectedTab == AppTab.ABOUT,
            onClick = { onSelectTab(AppTab.ABOUT) },
            colors = railColors,
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.ABOUT) {
                        Icons.Filled.Info
                    } else {
                        Icons.Outlined.Info
                    },
                    contentDescription = stringResource(R.string.tab_about)
                )
            },
            label = { Text(stringResource(R.string.tab_about)) },
            modifier = Modifier.testTag("nav_rail_about")
        )
    }
}

private fun copyToClipboard(context: Context, text: String): Boolean {
    return runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return false
        clipboard.setPrimaryClip(ClipData.newPlainText("Sounding Tangki", text))
        true
    }.getOrDefault(false)
}

private fun sharePlainText(context: Context, text: String): Boolean {
    return runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, null))
        true
    }.getOrDefault(false)
}

private fun shareJpgImage(context: Context, imageFile: java.io.File): Boolean {
    return runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("Laporan Sounding Harian", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
        true
    }.getOrDefault(false)
}
