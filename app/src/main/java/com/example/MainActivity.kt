package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.key
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.ProductPreferences
import com.example.ui.AppTab
import com.example.ui.SoundingUiState
import com.example.ui.SoundingViewModel
import com.example.ui.components.ExportDraftJpgBottomSheet
import com.example.ui.components.ExportJpgReportSheet
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.DraftReportScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LicenseActivationBottomSheet
import com.example.ui.screens.LicenseExpiredScreen
import com.example.ui.screens.performFirebaseSignOut
import com.example.ui.theme.MonoTypography
import com.example.ui.theme.SoundingTangkiTheme
import com.example.ui.theme.SoundingThemeExtras
import com.example.util.DailyReportJpgGenerator
import com.example.util.ShiftDraftJpgGenerator
import com.example.util.SoundingFormatters
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            val auth = remember { Firebase.auth }
            var currentUser by remember { mutableStateOf(auth.currentUser) }

            DisposableEffect(auth) {
                val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                    currentUser = firebaseAuth.currentUser
                }
                auth.addAuthStateListener(listener)
                onDispose { auth.removeAuthStateListener(listener) }
            }

            val user = currentUser
            if (user == null) {
                val isDarkDefault = remember {
                    ProductPreferences(applicationContext).loadDarkTheme(defaultValue = false)
                }
                SoundingTangkiTheme(darkTheme = isDarkDefault) {
                    AuthScreen(
                        onSignedIn = { signedInUser -> currentUser = signedInUser }
                    )
                }
            } else {
                key(user.uid) {
                    val viewModel: SoundingViewModel = viewModel(
                        key = user.uid,
                        factory = SoundingViewModel.provideFactory(applicationContext)
                    )
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    SoundingTangkiTheme(darkTheme = uiState.isDarkTheme) {
                        SoundingTangkiApp(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SoundingTangkiApp(
    uiState: SoundingUiState,
    viewModel: SoundingViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showExportJpgSheet by remember { mutableStateOf(false) }
    var showExportDraftJpgSheet by remember { mutableStateOf(false) }
    var showLicenseActivationSheet by remember { mutableStateOf(false) }
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
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
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
                    isDarkTheme = uiState.isDarkTheme,
                    isOnlineRealtime = uiState.isOnlineRealtime,
                    isAboutSelected = uiState.selectedTab == AppTab.ABOUT,
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

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
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
                                    copyToClipboard(context, summary)
                                    showFeedback(context.getString(R.string.msg_copied_wa))
                                },
                                onShareWhatsApp = { calc ->
                                    val summary = SoundingFormatters.formatWhatsAppSummary(calc)
                                    sharePlainText(context, summary)
                                }
                            )

                            AppTab.HISTORY -> HistoryScreen(
                                uiState = uiState,
                                onSelectDateFilter = viewModel::setHistoryDateFilter,
                                onSelectShiftFilter = viewModel::setHistoryShiftFilter,
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
                                        csvDocumentLauncher.launch("rekap_sounding_tangki.csv")
                                    }
                                },
                                onCopyLogWa = { log ->
                                    val summary = SoundingFormatters.formatLogWhatsAppSummary(log)
                                    copyToClipboard(context, summary)
                                    showFeedback(context.getString(R.string.msg_copied_wa))
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
                                cloudShiftReports = uiState.filteredCloudShiftReports,
                                availableDraftDates = uiState.availableDraftDates,
                                selectedDraftDateFilter = uiState.draftFilterDateKey,
                                selectedDraftShiftFilter = uiState.draftFilterShift,
                                isViewingOtherShiftReport = uiState.isViewingOtherShiftReport,
                                onSelectDraftDateFilter = viewModel::setDraftDateFilter,
                                onSelectDraftShiftFilter = viewModel::setDraftShiftFilter,
                                onOpenCloudShiftReport = { report ->
                                    viewModel.openCloudShiftReport(report)
                                    showFeedback(
                                        context.getString(
                                            R.string.msg_loaded_shift_report,
                                            report.dateLabel.ifBlank { report.dateKey },
                                            report.shift
                                        )
                                    )
                                },
                                onReturnToActiveShift = viewModel::returnToCurrentShiftDraft,
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
                                onShiftChanged = viewModel::updateDraftShift,
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
                                userName = uiState.currentUserName,
                                userEmail = uiState.currentUserEmail,
                                isOnlineRealtime = uiState.isOnlineRealtime,
                                onOpenLicenseActivation = { showLicenseActivationSheet = true },
                                onSignOut = {
                                    scope.launch {
                                        performFirebaseSignOut(context)
                                    }
                                }
                            )
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
                        shareJpgImage(context, file)
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
                        jpgDocumentLauncher.launch("laporan_sounding_harian.jpg")
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
                        shareJpgImage(context, file)
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
                        jpgDocumentLauncher.launch("draft_laporan_shift.jpg")
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
}

@Composable
private fun AppBrandHeader(
    isDarkTheme: Boolean,
    isOnlineRealtime: Boolean,
    isAboutSelected: Boolean,
    onToggleTheme: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val extras = SoundingThemeExtras.colors
    val headerBrush = Brush.horizontalGradient(
        colors = listOf(extras.headerGradientStart, extras.headerGradientEnd)
    )
    val toggleCd = stringResource(
        if (isDarkTheme) R.string.cd_switch_to_light else R.string.cd_switch_to_dark
    )
    val aboutCd = stringResource(R.string.cd_open_about)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBrush)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_header_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        lineHeight = 20.sp
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("header_title")
                )
                Text(
                    text = stringResource(R.string.app_header_subtitle),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.White.copy(alpha = 0.95f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("header_subtitle")
                )
                Text(
                    text = stringResource(R.string.app_header_credit),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color.White.copy(alpha = 0.86f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("header_credit")
                )
            }

            Column(
                modifier = Modifier.padding(start = 10.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Top Row: Dynamic ONLINE • REALTIME / OFFLINE • LOKAL badge + Company Logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    val statusBg = if (isOnlineRealtime) {
                        Color(0xFF15803D).copy(alpha = 0.38f)
                    } else {
                        Color(0xFFB45309).copy(alpha = 0.42f)
                    }
                    val statusBorder = if (isOnlineRealtime) {
                        Color(0xFF86EFAC).copy(alpha = 0.72f)
                    } else {
                        Color(0xFFFDE68A).copy(alpha = 0.72f)
                    }
                    val dotColor = if (isOnlineRealtime) Color(0xFF4ADE80) else Color(0xFFFBBF24)

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = statusBg,
                        border = BorderStroke(1.dp, statusBorder),
                        modifier = Modifier.testTag("header_connection_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Icon(
                                imageVector = if (isOnlineRealtime) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = stringResource(
                                    if (isOnlineRealtime) R.string.status_online_realtime else R.string.status_offline_local
                                ),
                                style = MonoTypography.badge.copy(fontSize = 9.5.sp),
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("header_company_logo"),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_company_logo),
                            contentDescription = stringResource(R.string.cd_company_logo),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(5.dp)
                        )
                    }
                }

                // Bottom Row: Info (About) button + Quick Theme Switcher Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val pillShape = RoundedCornerShape(999.dp)
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
                                Color.White
                            } else {
                                Color.White.copy(alpha = 0.20f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isAboutSelected) Color.White else Color.White.copy(alpha = 0.36f)
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAboutSelected) Icons.Filled.Info else Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = if (isAboutSelected) extras.headerGradientStart else Color.White,
                                    modifier = Modifier.size(16.dp)
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
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Surface(
                            shape = pillShape,
                            color = Color.White.copy(alpha = 0.20f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.36f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = stringResource(
                                        if (isDarkTheme) R.string.theme_dark_label else R.string.theme_light_label
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
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
        selectedIconColor = extras.onLeafContainer,
        selectedTextColor = extras.leafAccentReadable,
        indicatorColor = extras.leafContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
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
            label = { Text(stringResource(R.string.tab_calculator)) },
            modifier = Modifier.testTag("nav_tab_calculator")
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
                                containerColor = extras.leafAccent,
                                contentColor = Color.White
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
                    text = if (logCount > 0) {
                        stringResource(R.string.tab_history_with_count, logCount)
                    } else {
                        stringResource(R.string.tab_history)
                    }
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
            label = { Text(stringResource(R.string.tab_draft)) },
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
            label = { Text(stringResource(R.string.tab_about)) },
            modifier = Modifier.testTag("nav_tab_about")
        )
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
        selectedIconColor = extras.onLeafContainer,
        selectedTextColor = extras.leafAccentReadable,
        indicatorColor = extras.leafContainer,
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
            selected = selectedTab == AppTab.HISTORY,
            onClick = { onSelectTab(AppTab.HISTORY) },
            colors = railColors,
            icon = {
                BadgedBox(
                    badge = {
                        if (logCount > 0) {
                            Badge(
                                containerColor = extras.leafAccent,
                                contentColor = Color.White
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

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Sounding Tangki", text))
}

private fun sharePlainText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

private fun shareJpgImage(context: Context, imageFile: java.io.File) {
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
}
