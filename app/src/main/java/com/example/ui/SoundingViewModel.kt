package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.CalibrationEngine
import com.example.data.firebase.CloudCodeClaimResult
import com.example.data.firebase.CloudSnapshotResult
import com.example.data.firebase.CurrentTankStockDoc
import com.example.data.firebase.EmailVerificationAuthHelper
import com.example.data.firebase.FirebaseSafeProvider
import com.example.data.firebase.FirestorePlantConfig
import com.example.data.firebase.FirestoreSoundingRepository
import com.example.data.firebase.HistoryRecordItem
import com.example.data.firebase.OperationType
import com.example.data.firebase.ShiftReportCloudDoc
import com.example.data.firebase.SignInExistingUserOutcome
import com.example.data.firebase.SoundingHistoryDoc
import com.example.data.firebase.handleFirestoreError
import com.example.data.local.AppDatabase
import com.example.data.local.LicenseActivationResult
import com.example.data.local.LicenseManager
import com.example.data.local.LicenseState
import com.example.data.local.OperatorIdentityFormatter
import com.example.data.local.OperatorPreferences
import com.example.data.local.OperatorSession
import com.example.data.local.PasswordHashUtil
import com.example.data.local.PendingEmailVerification
import com.example.data.local.ProductPreferences
import com.example.data.local.SharedPreferencesLicenseStorage
import com.example.data.local.ShiftDraftPreferences
import com.example.data.local.SoundingLogEntity
import com.example.data.local.SoundingLogRepository
import com.example.data.local.VerifiedOperatorAccount
import com.example.data.model.CalibrationDataset
import com.example.data.model.ShiftDraftEngine
import com.example.data.model.ShiftReportDraft
import com.example.data.model.SoundingCalculation
import com.example.data.model.TankShiftDraftRow
import com.example.data.model.TankSpec
import com.example.data.model.TransferLogItem
import com.example.util.NetworkConnectivityMonitor
import com.example.util.SoundingFormatters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

const val FILTER_ALL = "ALL"

enum class AppTab {
    CALCULATOR,
    BLENDING,
    HISTORY,
    DRAFT,
    ABOUT
}

data class DateFilterOption(
    val dateKey: String,
    val label: String
)

sealed interface UsernamePasswordLoginResult {
    data class Success(val accountName: String, val email: String) : UsernamePasswordLoginResult
    data class UserNotRegistered(val username: String) : UsernamePasswordLoginResult
    data class EmailNotYetVerified(val email: String) : UsernamePasswordLoginResult
    data class AuthError(val message: String) : UsernamePasswordLoginResult
    data object InvalidPassword : UsernamePasswordLoginResult
    data object EmptyUsername : UsernamePasswordLoginResult
    data object EmptyPassword : UsernamePasswordLoginResult
    data object PasswordTooShortForMigration : UsernamePasswordLoginResult
}

sealed interface ResolveResetEmailResult {
    data class Resolved(val email: String, val matchedUsername: String = "") : ResolveResetEmailResult
    data object EmptyInput : ResolveResetEmailResult
    data class NotFound(val query: String, val isNetworkOnline: Boolean) : ResolveResetEmailResult
}

private data class CalculatorInputState(
    val isLoaded: Boolean = false,
    val selectedTab: AppTab = AppTab.CALCULATOR,
    val selectedTankIndex: Int = 0,
    val selectedProduct: String = "",
    val soundingInput: String = "",
    val tempInput: String = "30",
    val diagramStockKgInput: String? = null,
    val manualKgInput: String = "",
    val manualNoteInput: String = ""
)

private data class FilterState(
    val historyDateKey: String = FILTER_ALL,
    val historyAccount: String = FILTER_ALL,
    val draftDateKey: String = FILTER_ALL,
    val draftAccount: String = FILTER_ALL,
    val activeDraftDateKey: String = FirestorePlantConfig.formatDateKey(System.currentTimeMillis()),
    val viewingCloudReportMeta: Pair<String, Long>? = null,
    val isViewingCloudReport: Boolean = false
)

private data class CloudUserSnapshot(
    val uid: String = "",
    val email: String = "",
    val displayName: String = ""
) {
    val isConnected: Boolean get() = uid.isNotBlank()
}

private data class CloudBundle(
    val user: CloudUserSnapshot = CloudUserSnapshot(),
    val tankStocks: CloudSnapshotResult<List<CurrentTankStockDoc>> = CloudSnapshotResult(emptyList()),
    val history: CloudSnapshotResult<List<SoundingHistoryDoc>> = CloudSnapshotResult(emptyList()),
    val shiftReports: CloudSnapshotResult<List<ShiftReportCloudDoc>> = CloudSnapshotResult(emptyList()),
    val isNetworkOnline: Boolean = false,
    val cloudError: String? = null
)

data class SoundingUiState(
    val isLoaded: Boolean = false,
    val isDarkTheme: Boolean = false,
    val selectedTab: AppTab = AppTab.CALCULATOR,
    val tanks: List<TankSpec> = emptyList(),
    val selectedTank: TankSpec? = null,
    val products: List<String> = emptyList(),
    val favoriteProducts: Set<String> = emptySet(),
    val selectedProduct: String = "",
    val soundingInput: String = "",
    val tempInput: String = "30",
    val diagramStockKgInput: String? = null,
    val manualKgInput: String = "",
    val manualNoteInput: String = "",
    val lastOperatorName: String = "",
    val calculation: SoundingCalculation? = null,
    val logs: List<SoundingLogEntity> = emptyList(),
    val totalRecordedKg: Long = 0L,
    val licenseState: LicenseState = LicenseState(),
    val shiftDraft: ShiftReportDraft = ShiftReportDraft(),
    // Local account session (works 100% offline & syncs to Firestore)
    val isSessionLoggedIn: Boolean = false,
    val activeAccountName: String = "",
    val activeOperatorEmail: String = "",
    val savedAccountNames: List<String> = emptyList(),
    val verifiedAccounts: List<VerifiedOperatorAccount> = emptyList(),
    val pendingEmailVerification: PendingEmailVerification? = null,
    // Cloud & Realtime Monitoring state
    val isNetworkOnline: Boolean = false,
    val isCloudConnected: Boolean = false,
    val isOnlineRealtime: Boolean = false,
    val hasPendingCloudWrites: Boolean = false,
    val cloudSyncError: String? = null,
    val currentUserUid: String = "",
    val currentUserName: String = "",
    val currentUserEmail: String = "",
    val currentTankStocks: List<CurrentTankStockDoc> = emptyList(),
    val historyRecords: List<HistoryRecordItem> = emptyList(),
    val filteredHistoryRecords: List<HistoryRecordItem> = emptyList(),
    val historyDateFilter: String = FILTER_ALL,
    val historyAccountFilter: String = FILTER_ALL,
    val availableHistoryDates: List<DateFilterOption> = emptyList(),
    val availableHistoryAccounts: List<String> = emptyList(),
    val cloudShiftReports: List<ShiftReportCloudDoc> = emptyList(),
    val filteredCloudShiftReports: List<ShiftReportCloudDoc> = emptyList(),
    val draftFilterDateKey: String = FILTER_ALL,
    val draftFilterAccount: String = FILTER_ALL,
    val availableDraftDates: List<DateFilterOption> = emptyList(),
    val availableDraftAccounts: List<String> = emptyList(),
    val activeDraftDateKey: String = "",
    val activeDraftUpdatedBy: String = "",
    val activeDraftUpdatedAtMs: Long = 0L,
    val isViewingCloudReport: Boolean = false
) {
    val totalRecordedTon: Double get() = totalRecordedKg / 1000.0
    val isManualTankSelected: Boolean get() = selectedTank?.isManualKg == true
    val isOperatorLoggedIn: Boolean get() = isSessionLoggedIn && activeAccountName.isNotBlank()
}

@OptIn(ExperimentalCoroutinesApi::class)
class SoundingViewModel(
    private val appContext: Context,
    private val localRepository: SoundingLogRepository,
    private val cloudRepository: FirestoreSoundingRepository,
    private val networkMonitor: NetworkConnectivityMonitor,
    private val productPreferences: ProductPreferences,
    private val operatorPreferences: OperatorPreferences,
    private val licenseManager: LicenseManager,
    private val shiftDraftPreferences: ShiftDraftPreferences
) : ViewModel() {

    private val auth: FirebaseAuth? = FirebaseSafeProvider.getAuth(appContext)

    private val safeCoroutineHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is CancellationException) return@CoroutineExceptionHandler
        Log.e(TAG, "Caught asynchronous error safely in SoundingViewModel", throwable)
        cloudErrorFlow.value = throwable.localizedMessage
            ?: appContext.getString(R.string.msg_firebase_not_ready)
    }

    private val datasetFlow = MutableStateFlow<CalibrationDataset?>(null)
    private val inputState = MutableStateFlow(CalculatorInputState())
    private val filterState = MutableStateFlow(FilterState())
    private val cloudErrorFlow = MutableStateFlow<String?>(null)

    private val operatorSessionFlow = MutableStateFlow(
        runCatching { operatorPreferences.loadSession() }.getOrDefault(OperatorSession())
    )
    private val favoriteProductsFlow = MutableStateFlow<Set<String>>(
        runCatching { productPreferences.loadFavoriteProducts() }
            .getOrDefault(CalibrationEngine.DEFAULT_FAVORITE_PRODUCTS.toSet())
    )
    private val darkThemeFlow = MutableStateFlow(
        runCatching { productPreferences.loadDarkTheme(defaultValue = false) }.getOrDefault(false)
    )
    private val lastOperatorFlow = MutableStateFlow(
        runCatching {
            operatorSessionFlow.value.activeAccountName.ifBlank {
                OperatorIdentityFormatter.sanitizeUsername(productPreferences.loadLastOperator())
            }
        }.getOrDefault("")
    )
    private val licenseStateFlow = MutableStateFlow(
        runCatching { licenseManager.refreshState() }.getOrDefault(LicenseState())
    )
    private val draftFlow = MutableStateFlow(
        runCatching {
            shiftDraftPreferences.loadDraft()
                ?: ShiftDraftEngine.createInitialDraft(
                    logs = emptyList(),
                    defaultOperator = lastOperatorFlow.value
                )
        }.getOrDefault(ShiftReportDraft())
    )

    private var draftCloudSyncJob: Job? = null

    private val cloudUserFlow: Flow<CloudUserSnapshot> = callbackFlow {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            trySend(CloudUserSnapshot())
            close()
            return@callbackFlow
        }

        fun emitCurrent(user: FirebaseUser?) {
            trySend(
                CloudUserSnapshot(
                    uid = user?.uid.orEmpty(),
                    email = user?.email.orEmpty(),
                    displayName = user?.displayName.orEmpty()
                )
            )
        }
        emitCurrent(runCatching { firebaseAuth.currentUser }.getOrNull())
        val listener = FirebaseAuth.AuthStateListener { activeAuth ->
            val user = runCatching { activeAuth.currentUser }.getOrNull()
            if (user != null) {
                cloudErrorFlow.value = null
            }
            emitCurrent(user)
        }
        val registered = runCatching {
            firebaseAuth.addAuthStateListener(listener)
            true
        }.onFailure { err ->
            Log.w(TAG, "Failed adding AuthStateListener safely", err)
        }.getOrDefault(false)
        awaitClose {
            if (registered) {
                runCatching { firebaseAuth.removeAuthStateListener(listener) }
            }
        }
    }.catch { err ->
        if (err is CancellationException) throw err
        Log.w(TAG, "cloudUserFlow error caught safely", err)
        emit(CloudUserSnapshot())
    }

    private data class PrefsSnapshot(
        val operatorSession: OperatorSession,
        val favorites: Set<String>,
        val isDark: Boolean,
        val operator: String,
        val licenseState: LicenseState,
        val draft: ShiftReportDraft
    )

    private val prefsFlow = combine(
        operatorSessionFlow,
        favoriteProductsFlow,
        darkThemeFlow,
        lastOperatorFlow,
        combine(licenseStateFlow, draftFlow) { l, d -> l to d }
    ) { opSession, favorites, isDark, operator, (license, draft) ->
        PrefsSnapshot(opSession, favorites, isDark, operator, license, draft)
    }

    // Auth-gated Firestore streams: NEVER query or listen when FirebaseAuth has no signed-in user
    private val tankStocksStream: Flow<CloudSnapshotResult<List<CurrentTankStockDoc>>> =
        cloudUserFlow.flatMapLatest { user ->
            if (!user.isConnected) {
                flowOf(CloudSnapshotResult(emptyList(), isFromCache = true))
            } else {
                cloudRepository.observeCurrentTankStocks()
                    .catch { e ->
                        handleFlowException(e, FirestorePlantConfig.COLLECTION_CURRENT_TANK_STOCKS, user.uid)
                        emit(CloudSnapshotResult(emptyList(), isFromCache = true))
                    }
            }
        }

    private val cloudHistoryStream: Flow<CloudSnapshotResult<List<SoundingHistoryDoc>>> =
        cloudUserFlow.flatMapLatest { user ->
            if (!user.isConnected) {
                flowOf(CloudSnapshotResult(emptyList(), isFromCache = true))
            } else {
                cloudRepository.observeSoundingHistory()
                    .catch { e ->
                        handleFlowException(e, FirestorePlantConfig.COLLECTION_SOUNDING_HISTORY, user.uid)
                        emit(CloudSnapshotResult(emptyList(), isFromCache = true))
                    }
            }
        }

    private val cloudShiftReportsStream: Flow<CloudSnapshotResult<List<ShiftReportCloudDoc>>> =
        cloudUserFlow.flatMapLatest { user ->
            if (!user.isConnected) {
                flowOf(CloudSnapshotResult(emptyList(), isFromCache = true))
            } else {
                cloudRepository.observeShiftReports()
                    .catch { e ->
                        handleFlowException(e, FirestorePlantConfig.COLLECTION_SHIFT_REPORTS, user.uid)
                        emit(CloudSnapshotResult(emptyList(), isFromCache = true))
                    }
            }
        }

    private val cloudStreamsCombined = combine(
        cloudUserFlow,
        tankStocksStream,
        cloudHistoryStream,
        cloudShiftReportsStream
    ) { user, stocks, history, reports ->
        CloudBundle(
            user = user,
            tankStocks = stocks,
            history = history,
            shiftReports = reports
        )
    }

    private val cloudBundleFlow = combine(
        cloudStreamsCombined,
        networkMonitor.isOnline,
        cloudErrorFlow
    ) { bundle, isOnline, cloudErr ->
        bundle.copy(
            isNetworkOnline = isOnline,
            cloudError = cloudErr
        )
    }

    private data class CoreInputs(
        val dataset: CalibrationDataset?,
        val input: CalculatorInputState,
        val prefs: PrefsSnapshot,
        val localLogs: List<SoundingLogEntity>
    )

    private val coreInputsFlow = combine(
        datasetFlow,
        inputState,
        prefsFlow,
        localRepository.logs
    ) { dataset, input, prefs, localLogs ->
        CoreInputs(dataset, input, prefs, localLogs)
    }

    val uiState: StateFlow<SoundingUiState> = combine(
        coreInputsFlow,
        cloudBundleFlow,
        filterState
    ) { core, cloud, filters ->
        val (dataset, input, prefs, localLogs) = core
        val (opSession, favorites, isDark, operator, license, rawDraft) = prefs
        val activeAccount = opSession.activeAccountName
        val effectiveOperator = activeAccount.ifBlank { operator }

        val mergedHistory = mergeCloudAndLocalHistory(
            cloudDocs = cloud.history.data,
            localLogs = localLogs,
            defaultAccount = effectiveOperator,
            currentUid = cloud.user.uid
        )

        val availableHistoryDates = mergedHistory
            .map { it.dateKey to SoundingFormatters.formatLocalDate(it.entity.createdAtEpochMs) }
            .distinctBy { it.first }
            .sortedByDescending { it.first }
            .map { (key, label) -> DateFilterOption(dateKey = key, label = label) }

        val availableHistoryAccounts = mergedHistory
            .map { it.displayAccountName.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .sorted()

        val filteredHistory = mergedHistory.filter { record ->
            val dateMatches = filters.historyDateKey == FILTER_ALL || record.dateKey == filters.historyDateKey
            val accountMatches = filters.historyAccount == FILTER_ALL ||
                record.displayAccountName.equals(filters.historyAccount, ignoreCase = true)
            dateMatches && accountMatches
        }

        val filteredLogEntities = filteredHistory.map { it.entity }
        val allMergedEntities = mergedHistory.map { it.entity }
        val totalFilteredKg = filteredLogEntities.sumOf { it.stockKg }

        val availableDraftDates = cloud.shiftReports.data
            .map { it.dateKey to it.dateLabel.ifBlank { it.dateKey } }
            .distinctBy { it.first }
            .sortedByDescending { it.first }
            .map { (key, label) -> DateFilterOption(dateKey = key, label = label) }

        val availableDraftAccounts = cloud.shiftReports.data
            .map { it.displayAccountName.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .sorted()

        val filteredShiftReports = cloud.shiftReports.data.filter { report ->
            val dateMatches = filters.draftDateKey == FILTER_ALL || report.dateKey == filters.draftDateKey
            val accountMatches = filters.draftAccount == FILTER_ALL ||
                report.displayAccountName.equals(filters.draftAccount, ignoreCase = true)
            dateMatches && accountMatches
        }

        val draftLogsForSync = mergedHistory
            .filter { it.dateKey == filters.activeDraftDateKey }
            .map { it.entity }
            .ifEmpty { allMergedEntities }

        val syncedDraft = if (filters.isViewingCloudReport) {
            rawDraft
        } else {
            ShiftDraftEngine.syncDraftWithHistory(
                current = rawDraft,
                logs = draftLogsForSync,
                defaultOperator = effectiveOperator,
                forceOverwriteManual = false
            )
        }

        val hasPendingWrites = cloud.tankStocks.hasPendingWrites ||
            cloud.history.hasPendingWrites ||
            cloud.shiftReports.hasPendingWrites
        val isOnlineRealtime = cloud.isNetworkOnline && cloud.user.isConnected && cloud.cloudError == null

        if (dataset == null || !input.isLoaded) {
            SoundingUiState(
                isLoaded = false,
                isDarkTheme = isDark,
                selectedTab = input.selectedTab,
                favoriteProducts = favorites,
                lastOperatorName = effectiveOperator,
                logs = filteredLogEntities,
                totalRecordedKg = totalFilteredKg,
                licenseState = license,
                shiftDraft = syncedDraft,
                isSessionLoggedIn = opSession.hasActiveSession,
                activeAccountName = activeAccount,
                activeOperatorEmail = opSession.activeOperatorEmail,
                savedAccountNames = opSession.savedAccountNames,
                verifiedAccounts = opSession.verifiedAccounts,
                pendingEmailVerification = opSession.pendingVerification,
                isNetworkOnline = cloud.isNetworkOnline,
                isCloudConnected = cloud.user.isConnected,
                isOnlineRealtime = isOnlineRealtime,
                hasPendingCloudWrites = hasPendingWrites,
                cloudSyncError = cloud.cloudError,
                currentUserUid = cloud.user.uid,
                currentUserName = cloud.user.displayName,
                currentUserEmail = cloud.user.email,
                currentTankStocks = cloud.tankStocks.data,
                historyRecords = mergedHistory,
                filteredHistoryRecords = filteredHistory,
                historyDateFilter = filters.historyDateKey,
                historyAccountFilter = filters.historyAccount,
                availableHistoryDates = availableHistoryDates,
                availableHistoryAccounts = availableHistoryAccounts,
                cloudShiftReports = cloud.shiftReports.data,
                filteredCloudShiftReports = filteredShiftReports,
                draftFilterDateKey = filters.draftDateKey,
                draftFilterAccount = filters.draftAccount,
                availableDraftDates = availableDraftDates,
                availableDraftAccounts = availableDraftAccounts,
                activeDraftDateKey = filters.activeDraftDateKey,
                activeDraftUpdatedBy = filters.viewingCloudReportMeta?.first.orEmpty(),
                activeDraftUpdatedAtMs = filters.viewingCloudReportMeta?.second ?: 0L,
                isViewingCloudReport = filters.isViewingCloudReport
            )
        } else {
            val safeIndex = input.selectedTankIndex.coerceIn(0, (dataset.tanks.size - 1).coerceAtLeast(0))
            val tank = dataset.tanks.getOrNull(safeIndex)
            val product = input.selectedProduct

            val calc = when {
                tank == null -> null
                tank.isManualKg -> {
                    val kgVal = CalibrationEngine.parseDecimalInput(input.manualKgInput)
                    if (kgVal != null && kgVal >= 0.0) {
                        CalibrationEngine.calculateManualKg(
                            tank = tank,
                            product = product,
                            manualKg = kgVal,
                            note = input.manualNoteInput
                        )
                    } else {
                        null
                    }
                }
                else -> {
                    val soundingVal = CalibrationEngine.parseDecimalInput(input.soundingInput)
                    val tempVal = CalibrationEngine.parseDecimalInput(input.tempInput)
                    if (soundingVal != null && tempVal != null && product.isNotEmpty()) {
                        CalibrationEngine.calculate(
                            dataset = dataset,
                            tank = tank,
                            product = product,
                            sounding = soundingVal,
                            temp = tempVal
                        )
                    } else {
                        null
                    }
                }
            }

            val orderedProducts = dataset.productNames.sortedByDescending { it in favorites }

            SoundingUiState(
                isLoaded = true,
                isDarkTheme = isDark,
                selectedTab = input.selectedTab,
                tanks = dataset.tanks,
                selectedTank = tank,
                products = orderedProducts,
                favoriteProducts = favorites,
                selectedProduct = product,
                soundingInput = input.soundingInput,
                tempInput = input.tempInput,
                diagramStockKgInput = input.diagramStockKgInput,
                manualKgInput = input.manualKgInput,
                manualNoteInput = input.manualNoteInput,
                lastOperatorName = effectiveOperator,
                calculation = calc,
                logs = filteredLogEntities,
                totalRecordedKg = totalFilteredKg,
                licenseState = license,
                shiftDraft = syncedDraft,
                isSessionLoggedIn = opSession.hasActiveSession,
                activeAccountName = activeAccount,
                activeOperatorEmail = opSession.activeOperatorEmail,
                savedAccountNames = opSession.savedAccountNames,
                verifiedAccounts = opSession.verifiedAccounts,
                pendingEmailVerification = opSession.pendingVerification,
                isNetworkOnline = cloud.isNetworkOnline,
                isCloudConnected = cloud.user.isConnected,
                isOnlineRealtime = isOnlineRealtime,
                hasPendingCloudWrites = hasPendingWrites,
                cloudSyncError = cloud.cloudError,
                currentUserUid = cloud.user.uid,
                currentUserName = cloud.user.displayName,
                currentUserEmail = cloud.user.email,
                currentTankStocks = cloud.tankStocks.data,
                historyRecords = mergedHistory,
                filteredHistoryRecords = filteredHistory,
                historyDateFilter = filters.historyDateKey,
                historyAccountFilter = filters.historyAccount,
                availableHistoryDates = availableHistoryDates,
                availableHistoryAccounts = availableHistoryAccounts,
                cloudShiftReports = cloud.shiftReports.data,
                filteredCloudShiftReports = filteredShiftReports,
                draftFilterDateKey = filters.draftDateKey,
                draftFilterAccount = filters.draftAccount,
                availableDraftDates = availableDraftDates,
                availableDraftAccounts = availableDraftAccounts,
                activeDraftDateKey = filters.activeDraftDateKey,
                activeDraftUpdatedBy = filters.viewingCloudReportMeta?.first.orEmpty(),
                activeDraftUpdatedAtMs = filters.viewingCloudReportMeta?.second ?: 0L,
                isViewingCloudReport = filters.isViewingCloudReport
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SoundingUiState(
            isDarkTheme = darkThemeFlow.value,
            favoriteProducts = favoriteProductsFlow.value,
            lastOperatorName = lastOperatorFlow.value,
            licenseState = licenseStateFlow.value,
            shiftDraft = draftFlow.value,
            isSessionLoggedIn = operatorSessionFlow.value.hasActiveSession,
            activeAccountName = operatorSessionFlow.value.activeAccountName,
            activeOperatorEmail = operatorSessionFlow.value.activeOperatorEmail,
            savedAccountNames = operatorSessionFlow.value.savedAccountNames,
            verifiedAccounts = operatorSessionFlow.value.verifiedAccounts,
            pendingEmailVerification = operatorSessionFlow.value.pendingVerification,
            isNetworkOnline = networkMonitor.isCurrentlyOnline(),
            isCloudConnected = runCatching { auth?.currentUser != null }.getOrDefault(false),
            isOnlineRealtime = networkMonitor.isCurrentlyOnline() &&
                runCatching { auth?.currentUser != null }.getOrDefault(false),
            currentUserUid = runCatching { auth?.currentUser?.uid.orEmpty() }.getOrDefault(""),
            currentUserName = runCatching { auth?.currentUser?.displayName.orEmpty() }.getOrDefault(""),
            currentUserEmail = runCatching { auth?.currentUser?.email.orEmpty() }.getOrDefault(""),
            activeDraftDateKey = filterState.value.activeDraftDateKey
        )
    )

    init {
        viewModelScope.launch(Dispatchers.IO + safeCoroutineHandler) {
            try {
                val loaded = CalibrationEngine.loadFromAssets(appContext)
                datasetFlow.value = loaded
                val firstTank = loaded.tanks.firstOrNull()
                inputState.update {
                    it.copy(
                        isLoaded = true,
                        selectedTankIndex = 0,
                        selectedProduct = "",
                        tempInput = if (firstTank?.tRef == 30.0) "30" else it.tempInput
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed loading calibration assets", e)
                datasetFlow.value = CalibrationDataset(
                    tanks = emptyList(),
                    calibrationTables = emptyMap(),
                    densityTable = emptyMap(),
                    productNames = emptyList()
                )
                inputState.update { it.copy(isLoaded = true) }
                cloudErrorFlow.value = appContext.getString(R.string.msg_calibration_load_failed)
            }
        }

        // Automatically re-verify ANDROID_ID trial/activation state with device_licenses/{deviceId}
        // and silently heal/restore verified operator profile mapping without ever auto-logging out.
        viewModelScope.launch(Dispatchers.IO + safeCoroutineHandler) {
            combine(cloudUserFlow, networkMonitor.isOnline) { user, isOnline ->
                Triple(user.uid, user.email, user.isConnected && isOnline)
            }
                .distinctUntilChanged()
                .catch { err ->
                    if (err is CancellationException) throw err
                    Log.w(TAG, "License/profile sync observer error caught safely", err)
                }
                .collect { (uid, email, isOnlineConnected) ->
                    if (isOnlineConnected) {
                        syncDeviceLicenseWithCloud()
                        healOrRestoreOperatorProfileFromCloud(uid = uid, email = email)
                    }
                }
        }
    }

    /**
     * Self-heals missing Firestore users/{username} mapping for an active logged-in operator,
     * OR restores a persistent local session when FirebaseAuth still holds a verified user
     * and the user never explicitly logged out. Never logs the user out automatically.
     */
    private suspend fun healOrRestoreOperatorProfileFromCloud(uid: String, email: String) {
        try {
            val session = operatorSessionFlow.value
            if (session.hasActiveSession) {
                val activeName = session.activeAccountName
                val localAcc = session.findVerifiedAccount(activeName)
                val targetEmail = localAcc?.email?.ifBlank { session.activeOperatorEmail }
                    ?: session.activeOperatorEmail.ifBlank { email }
                val targetHash = localAcc?.passwordHash.orEmpty()
                if (activeName.isNotBlank() && targetEmail.isNotBlank() && targetHash.isNotBlank()) {
                    val existingRemote = runCatching {
                        cloudRepository.fetchVerifiedUserProfile(activeName).getOrNull()
                    }.getOrNull()
                    if (existingRemote == null || existingRemote.passwordHash.isBlank()) {
                        cloudRepository.upsertVerifiedUserProfile(
                            username = activeName,
                            email = targetEmail,
                            passwordHash = targetHash,
                            verifiedAtEpochMs = localAcc?.verifiedAtEpochMs?.takeIf { it > 0L }
                                ?: System.currentTimeMillis()
                        )
                    }
                }
                return
            }

            // If user did NOT explicitly log out, restore session seamlessly if FirebaseAuth is verified
            if (session.explicitlyLoggedOut || uid.isBlank()) return
            val firebaseUser = runCatching { auth?.currentUser }.getOrNull() ?: return
            if (!firebaseUser.isEmailVerified) return

            val cleanEmail = email.trim().lowercase()
            val localByEmail = session.findVerifiedAccountByEmail(cleanEmail)
            if (localByEmail != null) {
                val restored = operatorPreferences.saveVerifiedAccount(
                    rawUsername = localByEmail.accountName,
                    email = localByEmail.email,
                    passwordHash = localByEmail.passwordHash,
                    verifiedAtEpochMs = localByEmail.verifiedAtEpochMs.takeIf { it > 0L }
                        ?: System.currentTimeMillis()
                )
                operatorSessionFlow.value = restored
                lastOperatorFlow.value = restored.activeAccountName
                return
            }

            val remote = runCatching {
                cloudRepository.findVerifiedUserProfileByUidOrEmail(uid = uid, email = cleanEmail).getOrNull()
            }.getOrNull()
            if (remote != null && remote.isVerified && remote.username.isNotBlank() && remote.email.isNotBlank()) {
                val restored = operatorPreferences.saveVerifiedAccount(
                    rawUsername = remote.username,
                    email = remote.email,
                    passwordHash = remote.passwordHash,
                    verifiedAtEpochMs = remote.verifiedAtEpochMs.takeIf { it > 0L }
                        ?: System.currentTimeMillis()
                )
                operatorSessionFlow.value = restored
                lastOperatorFlow.value = restored.activeAccountName
                productPreferences.saveLastOperator(restored.activeAccountName)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "healOrRestoreOperatorProfileFromCloud skipped safely", e)
        }
    }

    private suspend fun syncDeviceLicenseWithCloud() {
        try {
            val localState = runCatching { licenseManager.refreshState() }.getOrElse { return }
            val deviceId = localState.deviceId.ifBlank {
                runCatching { licenseManager.getDeviceId() }.getOrDefault("")
            }
            if (deviceId.isBlank()) return
            val activeAccount = operatorSessionFlow.value.activeAccountName
            val result = cloudRepository.syncDeviceLicense(
                deviceId = deviceId,
                localState = localState,
                activeUsername = activeAccount
            )
            result.getOrNull()?.let { syncResult ->
                if (!syncResult.revokedOfflineCode.isNullOrBlank()) {
                    runCatching { licenseManager.revokeUnverifiedOfflineActivation(syncResult.revokedOfflineCode) }
                }
                val remote = syncResult.doc
                val merged = runCatching {
                    licenseManager.mergeRemoteDeviceLicense(
                        remoteFirstInstalledAt = remote.effectiveFirstInstalledAt,
                        remoteTrialEndsAt = remote.effectiveTrialEndsAt,
                        remoteIsActivated = remote.isActivated,
                        remoteActivationCode = remote.effectiveActivationCode,
                        remoteActivatedAtEpochMs = remote.activatedAtEpochMs
                    )
                }.getOrNull()
                if (merged != null) {
                    licenseStateFlow.value = merged
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "syncDeviceLicenseWithCloud failed safely", e)
        }
    }

    private fun handleFlowException(error: Throwable, path: String, listenerUid: String) {
        val firestoreEx = (error as? FirebaseFirestoreException)
            ?: (error.cause as? FirebaseFirestoreException)
        if (error is CancellationException && firestoreEx == null) {
            throw error
        }
        val exception = (firestoreEx ?: error as? Exception) ?: Exception(error)
        val currentUid = runCatching { auth?.currentUser?.uid }.getOrNull()
        if (currentUid == listenerUid) {
            runCatching { handleFirestoreError(exception, OperationType.LIST, path) }
            Log.e(TAG, "Firestore stream error on $path", exception)
            cloudErrorFlow.value = exception.localizedMessage ?: "Sinkronisasi cloud terganggu"
        }
    }

    private fun mergeCloudAndLocalHistory(
        cloudDocs: List<SoundingHistoryDoc>,
        localLogs: List<SoundingLogEntity>,
        defaultAccount: String,
        currentUid: String
    ): List<HistoryRecordItem> {
        val cloudRecords = cloudDocs.map { doc ->
            val entity = doc.toSoundingLogEntity()
            val resolvedDateKey = doc.dateKey.ifBlank {
                FirestorePlantConfig.formatDateKey(entity.createdAtEpochMs)
            }
            val resolvedAccount = doc.displayAccountName
            HistoryRecordItem(
                entity = entity,
                cloudDocId = doc.id,
                authorUid = doc.userId,
                accountName = resolvedAccount,
                operatorName = doc.operatorName.ifBlank { resolvedAccount },
                dateKey = resolvedDateKey,
                isCloudSynced = true
            )
        }

        val localOnlyRecords = localLogs.filter { local ->
            cloudRecords.none { cloudItem ->
                cloudItem.entity.tankId == local.tankId &&
                    abs(cloudItem.entity.createdAtEpochMs - local.createdAtEpochMs) <= 2_500L &&
                    cloudItem.entity.stockKg == local.stockKg
            }
        }.map { local ->
            val epoch = local.createdAtEpochMs.takeIf { it > 0L } ?: System.currentTimeMillis()
            HistoryRecordItem(
                entity = local,
                cloudDocId = null,
                authorUid = currentUid,
                accountName = defaultAccount,
                operatorName = defaultAccount,
                dateKey = FirestorePlantConfig.formatDateKey(epoch),
                isCloudSynced = false
            )
        }

        return (cloudRecords + localOnlyRecords)
            .sortedByDescending { it.entity.createdAtEpochMs }
    }

    /**
     * Logs in or switches active accountName locally without losing
     * local Room data or disconnecting Firebase Cloud sync.
     */
    fun loginOrSwitchAccount(rawUsername: String): String? {
        val clean = OperatorIdentityFormatter.sanitizeUsername(rawUsername)
        if (clean.isEmpty()) return null

        val updatedSession = operatorPreferences.saveActiveAccount(clean)
        operatorSessionFlow.value = updatedSession
        lastOperatorFlow.value = updatedSession.activeAccountName
        productPreferences.saveLastOperator(updatedSession.activeAccountName)

        if (!filterState.value.isViewingCloudReport) {
            val currentDraft = uiState.value.shiftDraft
            val nextDraft = currentDraft.copy(
                operatorName = updatedSession.activeAccountName
            )
            draftFlow.value = nextDraft
            viewModelScope.launch(Dispatchers.IO + safeCoroutineHandler) {
                runCatching { shiftDraftPreferences.saveDraft(nextDraft) }
            }
        }
        if (cloudRepository.currentUserUid() != null && networkMonitor.isCurrentlyOnline()) {
            viewModelScope.launch(Dispatchers.IO + safeCoroutineHandler) {
                syncDeviceLicenseWithCloud()
            }
        }
        return updatedSession.activeAccountName
    }

    /**
     * Explicit user-initiated logout (ONLY called when user consciously taps Logout).
     * Preserves verifiedAccounts locally so future Username+Password sign-in stays effortless.
     */
    fun logoutOperatorSession() {
        draftCloudSyncJob?.cancel()
        val cleared = operatorPreferences.logoutSession()
        operatorSessionFlow.value = cleared
        cloudErrorFlow.value = null
        viewModelScope.launch(Dispatchers.IO + safeCoroutineHandler) {
            EmailVerificationAuthHelper.performFirebaseSignOut(appContext)
        }
    }

    /**
     * Authenticates a verified operator using Username (or direct Email) + Password:
     * 1. Checks local SharedPreferences first so OFFLINE • LOKAL login succeeds immediately.
     * 2. Falls back to Firestore users/{username} (even before Firebase Auth sign-in) and caches
     *    a copy locally for future offline logins.
     * 3. If local password hash doesn't match (e.g., after Firebase password reset) or user enters
     *    their email directly, verifies with Firebase Auth and automatically syncs/recovers the
     *    username profile in both local storage and Firestore users/{username}.
     */
    suspend fun authenticateWithUsernameAndPassword(
        rawIdentifier: String,
        rawPassword: String
    ): UsernamePasswordLoginResult {
        val trimmedInput = rawIdentifier.trim()
        if (trimmedInput.isEmpty()) return UsernamePasswordLoginResult.EmptyUsername
        if (rawPassword.isEmpty()) return UsernamePasswordLoginResult.EmptyPassword

        val isEmailInput = trimmedInput.contains("@") && EmailVerificationAuthHelper.isValidEmail(trimmedInput)
        if (isEmailInput) {
            return authenticateByEmailAndPassword(
                cleanEmail = EmailVerificationAuthHelper.normalizeEmail(trimmedInput),
                rawPassword = rawPassword
            )
        }

        val clean = OperatorIdentityFormatter.sanitizeUsername(trimmedInput)
        if (clean.isEmpty()) return UsernamePasswordLoginResult.EmptyUsername

        // 1. Local-first lookup (works 100% OFFLINE • LOKAL)
        var matched = operatorSessionFlow.value.findVerifiedAccount(clean)

        // 2. Firestore fallback lookup on users/{username} even when FirebaseAuth is signed out
        if (matched == null || matched.passwordHash.isBlank()) {
            val remote = withContext(Dispatchers.IO) {
                runCatching { cloudRepository.fetchVerifiedUserProfile(clean).getOrNull() }.getOrNull()
            }
            if (remote != null && remote.isVerified && remote.email.isNotBlank()) {
                val updated = operatorPreferences.cacheVerifiedAccountFromCloud(
                    rawUsername = remote.username.ifBlank { clean },
                    email = remote.email,
                    passwordHash = remote.passwordHash,
                    verifiedAtEpochMs = remote.verifiedAtEpochMs.takeIf { it > 0L } ?: System.currentTimeMillis()
                )
                operatorSessionFlow.value = updated
                matched = updated.findVerifiedAccount(clean)
            }
        }

        val verifiedAccount = matched ?: return UsernamePasswordLoginResult.UserNotRegistered(clean)

        // 3. Verify password locally first
        val localHashMatches = if (verifiedAccount.passwordHash.isNotBlank()) {
            withContext(Dispatchers.Default) {
                PasswordHashUtil.verifyPassword(rawPassword, verifiedAccount.passwordHash)
            }
        } else {
            false
        }

        var effectiveHash = verifiedAccount.passwordHash
        var signedInViaFirebaseNow = false

        if (!localHashMatches) {
            // Try online Firebase Auth sign-in (handles post-reset password changes or missing local hash)
            val canTryCloudAuth = networkMonitor.isCurrentlyOnline() &&
                EmailVerificationAuthHelper.isValidEmail(verifiedAccount.email)
            if (canTryCloudAuth) {
                when (
                    val authOutcome = withContext(Dispatchers.IO) {
                        EmailVerificationAuthHelper.signInExistingVerifiedUser(
                            context = appContext,
                            rawEmail = verifiedAccount.email,
                            rawPassword = rawPassword
                        )
                    }
                ) {
                    is SignInExistingUserOutcome.Verified -> {
                        signedInViaFirebaseNow = true
                        effectiveHash = withContext(Dispatchers.Default) {
                            PasswordHashUtil.hashPassword(rawPassword)
                        }
                        val updated = operatorPreferences.saveVerifiedAccount(
                            rawUsername = clean,
                            email = authOutcome.email,
                            passwordHash = effectiveHash,
                            verifiedAtEpochMs = verifiedAccount.verifiedAtEpochMs.takeIf { it > 0L }
                                ?: System.currentTimeMillis()
                        )
                        operatorSessionFlow.value = updated
                    }
                    is SignInExistingUserOutcome.NotYetVerified -> {
                        return UsernamePasswordLoginResult.EmailNotYetVerified(authOutcome.email)
                    }
                    is SignInExistingUserOutcome.InvalidCredentials -> {
                        return UsernamePasswordLoginResult.InvalidPassword
                    }
                    is SignInExistingUserOutcome.UserNotFound -> {
                        if (verifiedAccount.passwordHash.isBlank()) {
                            return UsernamePasswordLoginResult.InvalidPassword
                        }
                        return UsernamePasswordLoginResult.InvalidPassword
                    }
                    is SignInExistingUserOutcome.Error -> {
                        if (verifiedAccount.passwordHash.isNotBlank()) {
                            return UsernamePasswordLoginResult.InvalidPassword
                        }
                        return UsernamePasswordLoginResult.AuthError(authOutcome.message)
                    }
                }
            } else if (verifiedAccount.passwordHash.isBlank()) {
                // Offline migration for legacy verified account created before mandatory passwords
                if (rawPassword.length < EmailVerificationAuthHelper.MIN_PASSWORD_LENGTH) {
                    return UsernamePasswordLoginResult.PasswordTooShortForMigration
                }
                effectiveHash = withContext(Dispatchers.Default) {
                    PasswordHashUtil.hashPassword(rawPassword)
                }
                val updated = operatorPreferences.updateVerifiedAccountPasswordHash(clean, effectiveHash)
                operatorSessionFlow.value = updated
            } else {
                return UsernamePasswordLoginResult.InvalidPassword
            }
        }

        // 4. Immediately persist active login session (isLoggedIn = true) locally for OFFLINE • LOKAL safety
        loginOrSwitchAccount(clean)

        // 5. Non-blocking background sync with Firebase Auth & Firestore users/{username} when online
        if (networkMonitor.isCurrentlyOnline()) {
            val targetEmail = verifiedAccount.email
            val hashToSync = effectiveHash
            val verifiedAt = verifiedAccount.verifiedAtEpochMs.takeIf { it > 0L } ?: System.currentTimeMillis()
            viewModelScope.launch(Dispatchers.IO + safeCoroutineHandler) {
                if (!signedInViaFirebaseNow && EmailVerificationAuthHelper.isValidEmail(targetEmail)) {
                    val currentEmail = runCatching { auth?.currentUser?.email.orEmpty() }.getOrDefault("")
                    if (currentEmail.isBlank() || !currentEmail.equals(targetEmail, ignoreCase = true)) {
                        runCatching {
                            EmailVerificationAuthHelper.signInExistingVerifiedUser(
                                context = appContext,
                                rawEmail = targetEmail,
                                rawPassword = rawPassword
                            )
                        }
                    } else {
                        runCatching {
                            EmailVerificationAuthHelper.syncBackgroundFirebaseSession(
                                context = appContext,
                                expectedEmail = targetEmail
                            )
                        }
                    }
                }
                if (cloudRepository.currentUserUid() != null && hashToSync.isNotBlank()) {
                    cloudRepository.upsertVerifiedUserProfile(
                        username = clean,
                        email = targetEmail,
                        passwordHash = hashToSync,
                        verifiedAtEpochMs = verifiedAt
                    )
                    syncDeviceLicenseWithCloud()
                }
            }
        }

        return UsernamePasswordLoginResult.Success(
            accountName = clean,
            email = verifiedAccount.email
        )
    }

    /**
     * Direct Email + Password login and recovery path:
     * Authenticates with Firebase Auth (or local cache when offline), restores/recovers the
     * operator's username from Firestore by uid/email if missing locally, and persists the session.
     */
    private suspend fun authenticateByEmailAndPassword(
        cleanEmail: String,
        rawPassword: String
    ): UsernamePasswordLoginResult {
        val localMatch = operatorSessionFlow.value.findVerifiedAccountByEmail(cleanEmail)
        if (localMatch != null && localMatch.passwordHash.isNotBlank()) {
            val localOk = withContext(Dispatchers.Default) {
                PasswordHashUtil.verifyPassword(rawPassword, localMatch.passwordHash)
            }
            if (localOk) {
                loginOrSwitchAccount(localMatch.accountName)
                if (networkMonitor.isCurrentlyOnline()) {
                    viewModelScope.launch(Dispatchers.IO + safeCoroutineHandler) {
                        val currentEmail = runCatching { auth?.currentUser?.email.orEmpty() }.getOrDefault("")
                        if (!currentEmail.equals(cleanEmail, ignoreCase = true)) {
                            runCatching {
                                EmailVerificationAuthHelper.signInExistingVerifiedUser(
                                    context = appContext,
                                    rawEmail = cleanEmail,
                                    rawPassword = rawPassword
                                )
                            }
                        }
                        if (cloudRepository.currentUserUid() != null) {
                            cloudRepository.upsertVerifiedUserProfile(
                                username = localMatch.accountName,
                                email = cleanEmail,
                                passwordHash = localMatch.passwordHash,
                                verifiedAtEpochMs = localMatch.verifiedAtEpochMs.takeIf { it > 0L }
                                    ?: System.currentTimeMillis()
                            )
                            syncDeviceLicenseWithCloud()
                        }
                    }
                }
                return UsernamePasswordLoginResult.Success(localMatch.accountName, cleanEmail)
            }
        }

        // Authenticate via Firebase Auth when online (recovers missing local or Firestore username mapping)
        val signInOutcome = withContext(Dispatchers.IO) {
            EmailVerificationAuthHelper.signInExistingVerifiedUser(
                context = appContext,
                rawEmail = cleanEmail,
                rawPassword = rawPassword
            )
        }
        return when (signInOutcome) {
            is SignInExistingUserOutcome.Verified -> {
                val uid = signInOutcome.user.uid
                val remoteProfile = withContext(Dispatchers.IO) {
                    runCatching {
                        cloudRepository.findVerifiedUserProfileByUidOrEmail(
                            uid = uid,
                            email = signInOutcome.email
                        ).getOrNull()
                    }.getOrNull()
                }
                val resolvedUsername = remoteProfile?.username?.takeIf { it.isNotBlank() }
                    ?: localMatch?.accountName?.takeIf { it.isNotBlank() }
                    ?: OperatorIdentityFormatter.ensureValidAccountName(signInOutcome.email.substringBefore("@"))
                val newHash = withContext(Dispatchers.Default) {
                    PasswordHashUtil.hashPassword(rawPassword)
                }
                val verifiedAt = remoteProfile?.verifiedAtEpochMs?.takeIf { it > 0L }
                    ?: localMatch?.verifiedAtEpochMs?.takeIf { it > 0L }
                    ?: System.currentTimeMillis()

                val updated = operatorPreferences.saveVerifiedAccount(
                    rawUsername = resolvedUsername,
                    email = signInOutcome.email,
                    passwordHash = newHash,
                    verifiedAtEpochMs = verifiedAt
                )
                operatorSessionFlow.value = updated
                lastOperatorFlow.value = updated.activeAccountName
                productPreferences.saveLastOperator(updated.activeAccountName)

                viewModelScope.launch(Dispatchers.IO + safeCoroutineHandler) {
                    cloudRepository.upsertVerifiedUserProfile(
                        username = resolvedUsername,
                        email = signInOutcome.email,
                        passwordHash = newHash,
                        verifiedAtEpochMs = verifiedAt
                    )
                    syncDeviceLicenseWithCloud()
                }
                UsernamePasswordLoginResult.Success(
                    accountName = resolvedUsername,
                    email = signInOutcome.email
                )
            }
            is SignInExistingUserOutcome.NotYetVerified ->
                UsernamePasswordLoginResult.EmailNotYetVerified(signInOutcome.email)
            is SignInExistingUserOutcome.InvalidCredentials ->
                UsernamePasswordLoginResult.InvalidPassword
            is SignInExistingUserOutcome.UserNotFound ->
                UsernamePasswordLoginResult.UserNotRegistered(cleanEmail)
            is SignInExistingUserOutcome.Error ->
                UsernamePasswordLoginResult.AuthError(signInOutcome.message)
        }
    }

    /**
     * Resolves the destination email for "Lupa Password?" directly from the live input text:
     * 1. If the user entered an email address directly, validates and returns it immediately.
     * 2. Otherwise sanitizes as username and checks local verified/pending/active records.
     * 3. If not found locally, queries Firestore users/{username} directly and caches a local copy.
     */
    suspend fun resolveEmailForPasswordReset(rawInput: String): ResolveResetEmailResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return ResolveResetEmailResult.EmptyInput

        // 1. Direct email input
        if (trimmed.contains("@") && EmailVerificationAuthHelper.isValidEmail(trimmed)) {
            val cleanEmail = EmailVerificationAuthHelper.normalizeEmail(trimmed)
            val localByEmail = operatorSessionFlow.value.findVerifiedAccountByEmail(cleanEmail)
            return ResolveResetEmailResult.Resolved(
                email = cleanEmail,
                matchedUsername = localByEmail?.accountName.orEmpty()
            )
        }

        val cleanUsername = OperatorIdentityFormatter.sanitizeUsername(trimmed)
        if (cleanUsername.isEmpty()) return ResolveResetEmailResult.EmptyInput

        // 2. Check local session records (verifiedAccounts, pendingVerification, activeAccount)
        val session = operatorSessionFlow.value
        val localEmail = session.findVerifiedAccount(cleanUsername)?.email
            ?.takeIf { EmailVerificationAuthHelper.isValidEmail(it) }
            ?: session.pendingVerification
                ?.takeIf { it.accountName.equals(cleanUsername, ignoreCase = true) }
                ?.email
                ?.takeIf { EmailVerificationAuthHelper.isValidEmail(it) }
            ?: session.activeOperatorEmail
                .takeIf {
                    session.activeAccountName.equals(cleanUsername, ignoreCase = true) &&
                        EmailVerificationAuthHelper.isValidEmail(it)
                }

        if (!localEmail.isNullOrBlank()) {
            return ResolveResetEmailResult.Resolved(
                email = EmailVerificationAuthHelper.normalizeEmail(localEmail),
                matchedUsername = cleanUsername
            )
        }

        // 3. Fallback: query Firestore users/{username} directly and cache locally on match
        val remote = withContext(Dispatchers.IO) {
            runCatching { cloudRepository.fetchVerifiedUserProfile(cleanUsername).getOrNull() }.getOrNull()
        }
        val remoteEmail = remote?.email?.trim().orEmpty()
        if (EmailVerificationAuthHelper.isValidEmail(remoteEmail)) {
            val normalizedRemoteEmail = EmailVerificationAuthHelper.normalizeEmail(remoteEmail)
            if (remote != null && remote.isVerified) {
                val updated = operatorPreferences.cacheVerifiedAccountFromCloud(
                    rawUsername = remote.username.ifBlank { cleanUsername },
                    email = normalizedRemoteEmail,
                    passwordHash = remote.passwordHash,
                    verifiedAtEpochMs = remote.verifiedAtEpochMs.takeIf { it > 0L } ?: System.currentTimeMillis()
                )
                operatorSessionFlow.value = updated
            }
            return ResolveResetEmailResult.Resolved(
                email = normalizedRemoteEmail,
                matchedUsername = remote?.username?.ifBlank { cleanUsername } ?: cleanUsername
            )
        }

        return ResolveResetEmailResult.NotFound(
            query = cleanUsername,
            isNetworkOnline = networkMonitor.isCurrentlyOnline()
        )
    }

    fun savePendingEmailVerification(
        rawUsername: String,
        email: String,
        passwordHash: String = ""
    ) {
        val updated = operatorPreferences.savePendingVerification(
            rawUsername = rawUsername,
            email = email,
            passwordHash = passwordHash
        )
        operatorSessionFlow.value = updated
    }

    fun clearPendingEmailVerification() {
        val updated = operatorPreferences.clearPendingVerification()
        operatorSessionFlow.value = updated
    }

    fun completeVerifiedAccountRegistration(
        rawUsername: String,
        email: String,
        passwordHash: String = ""
    ): String? {
        val clean = OperatorIdentityFormatter.sanitizeUsername(rawUsername)
        if (clean.isEmpty()) return null
        val verifiedAt = System.currentTimeMillis()

        val updatedSession = operatorPreferences.saveVerifiedAccount(
            rawUsername = clean,
            email = email,
            passwordHash = passwordHash,
            verifiedAtEpochMs = verifiedAt
        )
        operatorSessionFlow.value = updatedSession
        lastOperatorFlow.value = updatedSession.activeAccountName
        productPreferences.saveLastOperator(updatedSession.activeAccountName)
        cloudErrorFlow.value = null

        val effectiveHash = updatedSession.findVerifiedAccount(clean)?.passwordHash ?: passwordHash

        if (!filterState.value.isViewingCloudReport) {
            val currentDraft = uiState.value.shiftDraft
            val nextDraft = currentDraft.copy(
                operatorName = updatedSession.activeAccountName
            )
            draftFlow.value = nextDraft
            viewModelScope.launch(Dispatchers.IO + safeCoroutineHandler) {
                runCatching { shiftDraftPreferences.saveDraft(nextDraft) }
            }
        }
        if (cloudRepository.currentUserUid() != null && networkMonitor.isCurrentlyOnline()) {
            viewModelScope.launch(Dispatchers.IO + safeCoroutineHandler) {
                if (effectiveHash.isNotBlank()) {
                    cloudRepository.upsertVerifiedUserProfile(
                        username = clean,
                        email = email,
                        passwordHash = effectiveHash,
                        verifiedAtEpochMs = verifiedAt
                    )
                }
                syncDeviceLicenseWithCloud()
            }
        }
        return updatedSession.activeAccountName
    }

    fun clearCloudError() {
        cloudErrorFlow.value = null
    }

    fun setHistoryDateFilter(dateKey: String) {
        filterState.update { it.copy(historyDateKey = dateKey) }
    }

    fun setHistoryAccountFilter(account: String) {
        filterState.update { it.copy(historyAccount = account) }
    }

    fun setDraftDateFilter(dateKey: String) {
        filterState.update { it.copy(draftDateKey = dateKey) }
    }

    fun setDraftAccountFilter(account: String) {
        filterState.update { it.copy(draftAccount = account) }
    }

    fun openCloudShiftReport(report: ShiftReportCloudDoc) {
        val parsed = report.toShiftReportDraft() ?: return
        draftFlow.value = parsed
        filterState.update {
            it.copy(
                activeDraftDateKey = report.dateKey.ifBlank { it.activeDraftDateKey },
                viewingCloudReportMeta = report.displayAccountName to report.updatedAtEpochMs,
                isViewingCloudReport = true
            )
        }
        viewModelScope.launch(Dispatchers.IO + safeCoroutineHandler) {
            runCatching { shiftDraftPreferences.saveDraft(parsed) }
        }
    }

    fun returnToCurrentDraft() {
        val todayKey = FirestorePlantConfig.formatDateKey(System.currentTimeMillis())
        val activeAccount = operatorSessionFlow.value.activeAccountName.ifBlank { lastOperatorFlow.value }
        val existingTodayReport = uiState.value.cloudShiftReports.firstOrNull {
            it.dateKey == todayKey && it.displayAccountName.equals(activeAccount, ignoreCase = true)
        }
        val restored = existingTodayReport?.toShiftReportDraft()
            ?: ShiftDraftEngine.createInitialDraft(
                logs = uiState.value.historyRecords
                    .filter { it.dateKey == todayKey }
                    .map { it.entity },
                defaultOperator = activeAccount
            )
        draftFlow.value = restored
        filterState.update {
            it.copy(
                activeDraftDateKey = todayKey,
                viewingCloudReportMeta = null,
                isViewingCloudReport = false
            )
        }
        viewModelScope.launch(Dispatchers.IO + safeCoroutineHandler) {
            runCatching { shiftDraftPreferences.saveDraft(restored) }
        }
    }

    fun saveDraftToCloudNow(onResult: (Boolean) -> Unit = {}) {
        val now = System.currentTimeMillis()
        val draft = uiState.value.shiftDraft.copy(
            timeLabel = SoundingFormatters.formatLocalTime(now)
        )
        val activeAccount = operatorSessionFlow.value.activeAccountName.ifBlank { lastOperatorFlow.value }
        val dateKey = filterState.value.activeDraftDateKey.ifBlank {
            FirestorePlantConfig.formatDateKey(now)
        }
        draftFlow.value = draft
        viewModelScope.launch(Dispatchers.IO + safeCoroutineHandler) {
            val synced = try {
                shiftDraftPreferences.saveDraft(draft)
                if (cloudRepository.currentUserUid() != null) {
                    val result = cloudRepository.saveShiftReport(
                        draft = draft,
                        dateKey = dateKey,
                        accountName = activeAccount,
                        recordedAtEpochMs = now
                    )
                    if (result.isSuccess) {
                        cloudErrorFlow.value = null
                        true
                    } else {
                        val reason = result.exceptionOrNull()?.localizedMessage ?: "koneksi terputus"
                        cloudErrorFlow.value = appContext.getString(R.string.msg_cloud_sync_failed_offline, reason)
                        false
                    }
                } else {
                    false
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "saveDraftToCloudNow failed safely", e)
                cloudErrorFlow.value = appContext.getString(
                    R.string.msg_cloud_sync_failed_offline,
                    e.localizedMessage ?: "kesalahan jaringan"
                )
                false
            }
            withContext(Dispatchers.Main) {
                onResult(synced)
            }
        }
    }

    fun refreshLicenseState() {
        runCatching { licenseManager.refreshState() }.getOrNull()?.let {
            licenseStateFlow.value = it
        }
        if (cloudRepository.currentUserUid() != null) {
            viewModelScope.launch(Dispatchers.IO + safeCoroutineHandler) {
                syncDeviceLicenseWithCloud()
            }
        }
    }

    suspend fun activateLicense(rawCode: String): LicenseActivationResult {
        return try {
            val normalized = LicenseManager.normalizeCode(rawCode)
            val localCheck = licenseManager.preValidateCode(rawCode)
            if (localCheck != null) {
                licenseStateFlow.value = licenseManager.refreshState()
                return localCheck
            }

            val currentState = licenseManager.refreshState()
            val deviceId = currentState.deviceId.ifBlank { licenseManager.getDeviceId() }
            val activeAccount = operatorSessionFlow.value.activeAccountName
            val now = System.currentTimeMillis()

            if (cloudRepository.currentUserUid() != null) {
                val claimResult = withContext(Dispatchers.IO) {
                    cloudRepository.claimActivationCodeForDevice(
                        code = normalized,
                        deviceId = deviceId,
                        activeUsername = activeAccount,
                        activatedAtEpochMs = now,
                        firstInstalledAt = currentState.firstLaunchEpochMs,
                        trialEndsAt = currentState.trialEndEpochMs
                    )
                }
                when (val outcome = claimResult.getOrNull()) {
                    is CloudCodeClaimResult.BoundToAnotherDevice -> {
                        licenseManager.markCodeUsedLocally(normalized)
                        licenseStateFlow.value = licenseManager.refreshState()
                        return LicenseActivationResult.BoundToOtherDevice(normalized)
                    }
                    is CloudCodeClaimResult.AlreadyBoundToThisDevice -> {
                        val merged = licenseManager.mergeRemoteDeviceLicense(
                            remoteFirstInstalledAt = currentState.firstLaunchEpochMs,
                            remoteTrialEndsAt = currentState.trialEndEpochMs,
                            remoteIsActivated = true,
                            remoteActivationCode = normalized,
                            remoteActivatedAtEpochMs = outcome.claimedAtEpochMs
                        )
                        licenseStateFlow.value = merged
                        return LicenseActivationResult.Success(code = normalized, state = merged)
                    }
                    is CloudCodeClaimResult.Claimed -> {
                        val result = licenseManager.activateCode(normalized)
                        licenseStateFlow.value = licenseManager.refreshState()
                        return result
                    }
                    null -> {
                        // Offline/network fallback: activate locally and re-verify on reconnect
                    }
                }
            }

            val result = licenseManager.activateCode(rawCode)
            licenseStateFlow.value = licenseManager.refreshState()
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "activateLicense failed safely", e)
            cloudErrorFlow.value = e.localizedMessage ?: "Gagal memproses kode aktivasi"
            LicenseActivationResult.InvalidCode(LicenseManager.normalizeCode(rawCode))
        }
    }

    fun toggleTheme() {
        val nextDark = !darkThemeFlow.value
        darkThemeFlow.value = nextDark
        productPreferences.saveDarkTheme(nextDark)
    }

    fun saveLastOperator(operatorName: String) {
        val trimmed = operatorName.trim()
        lastOperatorFlow.value = trimmed
        productPreferences.saveLastOperator(trimmed)
    }

    private fun updateAndPersistDraft(transform: (ShiftReportDraft) -> ShiftReportDraft) {
        val now = System.currentTimeMillis()
        val base = uiState.value.shiftDraft
        val updated = transform(base).copy(
            timeLabel = SoundingFormatters.formatLocalTime(now)
        )
        draftFlow.value = updated
        val dateKey = filterState.value.activeDraftDateKey.ifBlank {
            FirestorePlantConfig.formatDateKey(now)
        }
        val activeAccount = operatorSessionFlow.value.activeAccountName.ifBlank { lastOperatorFlow.value }
        viewModelScope.launch(Dispatchers.IO + safeCoroutineHandler) {
            runCatching { shiftDraftPreferences.saveDraft(updated) }
                .onFailure { Log.w(TAG, "Failed persisting draft locally", it) }
        }
        draftCloudSyncJob?.cancel()
        if (cloudRepository.currentUserUid() != null) {
            draftCloudSyncJob = viewModelScope.launch(Dispatchers.IO + safeCoroutineHandler) {
                try {
                    delay(750L)
                    val result = cloudRepository.saveShiftReport(
                        draft = updated,
                        dateKey = dateKey,
                        accountName = activeAccount,
                        recordedAtEpochMs = System.currentTimeMillis()
                    )
                    if (result.isSuccess) {
                        cloudErrorFlow.value = null
                    } else {
                        val reason = result.exceptionOrNull()?.localizedMessage ?: "sinkronisasi tertunda"
                        cloudErrorFlow.value = appContext.getString(R.string.msg_cloud_sync_failed_offline, reason)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Auto-sync shift draft failed safely", e)
                }
            }
        }
    }

    fun updateDraftDate(dateLabel: String) {
        updateAndPersistDraft { it.copy(dateLabel = dateLabel) }
    }

    fun updateDraftOperator(operatorName: String) {
        updateAndPersistDraft { it.copy(operatorName = operatorName) }
        saveLastOperator(operatorName)
    }

    fun updateDraftNb(nbNote: String) {
        updateAndPersistDraft { it.copy(nbNote = nbNote) }
    }

    fun updateDraftTankRow(updatedRow: TankShiftDraftRow) {
        updateAndPersistDraft { current ->
            val nextRows = current.tankRows.map { existing ->
                if (existing.tankId == updatedRow.tankId) updatedRow else existing
            }
            current.copy(tankRows = nextRows)
        }
    }

    fun saveDraftTransfer(id: Long?, jam: String, keterangan: String, qtyKg: String) {
        if (jam.isBlank() && keterangan.isBlank() && qtyKg.isBlank()) return
        updateAndPersistDraft { current ->
            val nextList = if (id == null) {
                val nextId = (current.transfers.maxOfOrNull { it.id } ?: 0L) + 1L
                current.transfers + TransferLogItem(
                    id = nextId,
                    jam = jam,
                    keterangan = keterangan,
                    qtyKg = qtyKg
                )
            } else {
                current.transfers.map { existing ->
                    if (existing.id == id) {
                        existing.copy(jam = jam, keterangan = keterangan, qtyKg = qtyKg)
                    } else {
                        existing
                    }
                }
            }
            current.copy(transfers = nextList)
        }
    }

    fun deleteDraftTransfer(id: Long) {
        updateAndPersistDraft { current ->
            current.copy(transfers = current.transfers.filterNot { it.id == id })
        }
    }

    fun syncDraftFromHistory() {
        val activeDateKey = filterState.value.activeDraftDateKey
        val matchingLogs = uiState.value.historyRecords
            .filter { it.dateKey == activeDateKey }
            .map { it.entity }
            .ifEmpty { uiState.value.historyRecords.map { it.entity } }
        val operator = operatorSessionFlow.value.activeAccountName.ifBlank { lastOperatorFlow.value }
        filterState.update { it.copy(isViewingCloudReport = false) }
        updateAndPersistDraft { current ->
            ShiftDraftEngine.syncDraftWithHistory(
                current = current,
                logs = matchingLogs,
                defaultOperator = operator,
                forceOverwriteManual = true
            )
        }
    }

    fun resetShiftDraft() {
        val now = System.currentTimeMillis()
        val todayKey = FirestorePlantConfig.formatDateKey(now)
        val logs = uiState.value.historyRecords
            .filter { it.dateKey == todayKey }
            .map { it.entity }
            .ifEmpty { uiState.value.historyRecords.map { it.entity } }
        val operator = operatorSessionFlow.value.activeAccountName.ifBlank { lastOperatorFlow.value }
        val fresh = ShiftDraftEngine.createInitialDraft(
            logs = logs,
            defaultOperator = operator,
            nowEpochMs = now
        )
        draftFlow.value = fresh
        filterState.update {
            it.copy(
                activeDraftDateKey = todayKey,
                viewingCloudReportMeta = null,
                isViewingCloudReport = false
            )
        }
        viewModelScope.launch(Dispatchers.IO + safeCoroutineHandler) {
            try {
                shiftDraftPreferences.clearDraft()
                shiftDraftPreferences.saveDraft(fresh)
                if (cloudRepository.currentUserUid() != null) {
                    val res = cloudRepository.saveShiftReport(
                        draft = fresh,
                        dateKey = todayKey,
                        accountName = operator,
                        recordedAtEpochMs = now
                    )
                    if (res.isFailure) {
                        val reason = res.exceptionOrNull()?.localizedMessage ?: "sinkronisasi tertunda"
                        cloudErrorFlow.value = appContext.getString(R.string.msg_cloud_sync_failed_offline, reason)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "resetShiftDraft failed safely", e)
            }
        }
    }

    fun selectTab(tab: AppTab) {
        inputState.update { it.copy(selectedTab = tab) }
    }

    fun selectTank(index: Int) {
        val dataset = datasetFlow.value ?: return
        val tank = dataset.tanks.getOrNull(index) ?: return
        inputState.update { current ->
            val nextTemp = if (!tank.isManualKg && tank.tRef == 30.0) "30" else current.tempInput
            current.copy(
                selectedTankIndex = index,
                tempInput = nextTemp,
                diagramStockKgInput = null
            )
        }
    }

    fun selectProduct(product: String) {
        inputState.update {
            it.copy(
                selectedProduct = product,
                diagramStockKgInput = null
            )
        }
    }

    fun toggleFavoriteProduct(product: String) {
        val updated = favoriteProductsFlow.value.toMutableSet().apply {
            if (!add(product)) remove(product)
        }.toSet()
        favoriteProductsFlow.value = updated
        viewModelScope.launch(Dispatchers.IO + safeCoroutineHandler) {
            runCatching { productPreferences.saveFavoriteProducts(updated) }
        }
    }

    fun updateSoundingInput(raw: String) {
        val sanitized = sanitizeNumericInput(raw, allowNegative = true)
        inputState.update {
            it.copy(
                soundingInput = sanitized,
                diagramStockKgInput = null
            )
        }
    }

    fun updateTempInput(raw: String) {
        val sanitized = sanitizeNumericInput(raw, allowNegative = true)
        inputState.update {
            it.copy(
                tempInput = sanitized,
                diagramStockKgInput = null
            )
        }
    }

    fun updateDiagramStockKgInput(raw: String) {
        val digits = raw.filter { it.isDigit() }.take(9)
        if (digits.isEmpty()) {
            inputState.update { it.copy(diagramStockKgInput = "") }
            return
        }

        val targetKg = digits.toDoubleOrNull() ?: return
        val dataset = datasetFlow.value ?: return
        inputState.update { current ->
            val safeIndex = current.selectedTankIndex.coerceIn(0, (dataset.tanks.size - 1).coerceAtLeast(0))
            val tank = dataset.tanks.getOrNull(safeIndex)
            val tempVal = CalibrationEngine.parseDecimalInput(current.tempInput)
            val computedSounding = if (
                tank != null &&
                !tank.isManualKg &&
                current.selectedProduct.isNotEmpty() &&
                tempVal != null
            ) {
                CalibrationEngine.calculateSoundingFromStockKg(
                    dataset = dataset,
                    tank = tank,
                    product = current.selectedProduct,
                    targetStockKg = targetKg,
                    temp = tempVal
                )
            } else {
                null
            }

            current.copy(
                diagramStockKgInput = digits,
                soundingInput = computedSounding?.let { SoundingFormatters.formatCompact(it) }
                    ?: current.soundingInput
            )
        }
    }

    fun commitDiagramStockKgEdit() {
        inputState.update { it.copy(diagramStockKgInput = null) }
    }

    fun updateManualKgInput(raw: String) {
        val sanitized = sanitizeNumericInput(raw, allowNegative = false)
        inputState.update { it.copy(manualKgInput = sanitized) }
    }

    fun updateManualNoteInput(raw: String) {
        inputState.update { it.copy(manualNoteInput = raw) }
    }

    fun resetSoundingInput() {
        val tank = uiState.value.selectedTank
        inputState.update {
            it.copy(
                soundingInput = "",
                diagramStockKgInput = null,
                manualKgInput = "",
                manualNoteInput = "",
                tempInput = if (tank?.tRef == 30.0) "30" else it.tempInput
            )
        }
    }

    fun saveCurrentCalculation(onSaved: (tankId: String, formattedKg: String) -> Unit) {
        val calc = uiState.value.calculation ?: return
        val activeAccount = operatorSessionFlow.value.activeAccountName.ifBlank { lastOperatorFlow.value }
        val operatorName = lastOperatorFlow.value.ifBlank { activeAccount }
        val now = System.currentTimeMillis()

        viewModelScope.launch(Dispatchers.IO + safeCoroutineHandler) {
            // 1. Always save locally in Room for instant OFFLINE • LOKAL durability
            val localResult = localRepository.saveCalculation(
                calc = calc,
                shiftLabel = "",
                recordedAtEpochMs = now
            )
            if (localResult.isFailure) {
                cloudErrorFlow.value = appContext.getString(R.string.msg_local_save_failed)
            }
            // 2. Sync to Firestore sounding_history + current_tank_stocks when cloud session is active
            if (cloudRepository.currentUserUid() != null) {
                val cloudResult = cloudRepository.saveSoundingAndTankStock(
                    calc = calc,
                    accountName = activeAccount,
                    operatorName = operatorName,
                    recordedAtEpochMs = now
                )
                if (cloudResult.isSuccess) {
                    cloudErrorFlow.value = null
                } else {
                    val reason = cloudResult.exceptionOrNull()?.localizedMessage ?: "koneksi cloud gagal"
                    cloudErrorFlow.value = appContext.getString(R.string.msg_cloud_sync_failed_offline, reason)
                }
            }
            if (localResult.isSuccess) {
                withContext(Dispatchers.Main) {
                    onSaved(calc.tankId, SoundingFormatters.formatIndonesianInteger(calc.stockKg))
                }
            }
        }
    }

    fun deleteHistoryRecord(
        item: HistoryRecordItem,
        onResult: (deleted: Boolean, tankId: String) -> Unit
    ) {
        val activeUid = cloudRepository.currentUserUid()
        viewModelScope.launch(Dispatchers.IO + safeCoroutineHandler) {
            var deleted = false
            try {
                if (item.cloudDocId != null) {
                    if (activeUid != null && item.authorUid == activeUid) {
                        val res = cloudRepository.deleteSoundingHistory(item.cloudDocId)
                        deleted = res.isSuccess
                        if (res.isFailure) {
                            cloudErrorFlow.value = res.exceptionOrNull()?.localizedMessage
                                ?: "Gagal menghapus catatan dari cloud"
                        }
                    }
                } else if (item.entity.id > 0L) {
                    deleted = localRepository.deleteLog(item.entity.id).isSuccess
                }
                if (item.entity.id > 0L) {
                    localRepository.deleteLog(item.entity.id)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "deleteHistoryRecord failed safely", e)
                cloudErrorFlow.value = e.localizedMessage ?: "Gagal menghapus catatan"
            }
            withContext(Dispatchers.Main) {
                onResult(deleted, item.entity.tankId)
            }
        }
    }

    fun clearLocalLogs(onCleared: () -> Unit) {
        val activeUid = cloudRepository.currentUserUid()
        viewModelScope.launch(Dispatchers.IO + safeCoroutineHandler) {
            try {
                localRepository.clearAll()
                if (activeUid != null) {
                    val ownCloudIds = uiState.value.historyRecords
                        .filter { it.cloudDocId != null && it.authorUid == activeUid }
                        .mapNotNull { it.cloudDocId }
                    ownCloudIds.forEach { docId ->
                        cloudRepository.deleteSoundingHistory(docId)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "clearLocalLogs failed safely", e)
                cloudErrorFlow.value = e.localizedMessage ?: "Sebagian riwayat gagal dihapus"
            }
            withContext(Dispatchers.Main) {
                onCleared()
            }
        }
    }

    fun getCsvExportText(): String? {
        val logs = uiState.value.logs
        if (logs.isEmpty()) return null
        return runCatching { SoundingFormatters.buildCsvContent(logs) }.getOrNull()
    }

    private fun sanitizeNumericInput(raw: String, allowNegative: Boolean): String {
        val sb = StringBuilder()
        var hasDecimal = false
        for ((index, ch) in raw.withIndex()) {
            when {
                ch.isDigit() -> sb.append(ch)
                (ch == '.' || ch == ',') && !hasDecimal -> {
                    hasDecimal = true
                    sb.append(ch)
                }
                allowNegative && ch == '-' && index == 0 -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    companion object {
        private const val TAG = "SoundingViewModel"

        fun provideFactory(appContext: Context): ViewModelProvider.Factory {
            val ctx = appContext.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    FirebaseSafeProvider.ensureInitialized(ctx)
                    return SoundingViewModel(
                        appContext = ctx,
                        localRepository = SoundingLogRepository(
                            AppDatabase.getInstance(ctx).soundingLogDao()
                        ),
                        cloudRepository = FirestoreSoundingRepository(ctx),
                        networkMonitor = NetworkConnectivityMonitor(ctx),
                        productPreferences = ProductPreferences(ctx),
                        operatorPreferences = OperatorPreferences(ctx),
                        licenseManager = LicenseManager(SharedPreferencesLicenseStorage(ctx)),
                        shiftDraftPreferences = ShiftDraftPreferences(ctx)
                    ) as T
                }
            }
        }
    }
}
