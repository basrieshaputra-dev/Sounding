package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CalibrationEngine
import com.example.data.firebase.CloudSnapshotResult
import com.example.data.firebase.CurrentTankStockDoc
import com.example.data.firebase.FirestorePlantConfig
import com.example.data.firebase.FirestoreSoundingRepository
import com.example.data.firebase.HistoryRecordItem
import com.example.data.firebase.OperationType
import com.example.data.firebase.ShiftReportCloudDoc
import com.example.data.firebase.SoundingHistoryDoc
import com.example.data.firebase.handleFirestoreError
import com.example.data.local.AppDatabase
import com.example.data.local.LicenseActivationResult
import com.example.data.local.LicenseManager
import com.example.data.local.LicenseState
import com.example.data.local.ProductPreferences
import com.example.data.local.SharedPreferencesLicenseStorage
import com.example.data.local.ShiftDraftPreferences
import com.example.data.local.SoundingLogEntity
import com.example.data.local.SoundingLogRepository
import com.example.data.model.CalibrationDataset
import com.example.data.model.ReportTimeSlot
import com.example.data.model.ShiftDraftEngine
import com.example.data.model.ShiftReportDraft
import com.example.data.model.SoundingCalculation
import com.example.data.model.TankShiftDraftRow
import com.example.data.model.TankSpec
import com.example.data.model.TransferLogItem
import com.example.util.NetworkConnectivityMonitor
import com.example.util.SoundingFormatters
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

const val FILTER_ALL = "ALL"

enum class AppTab {
    CALCULATOR,
    HISTORY,
    DRAFT,
    ABOUT
}

data class DateFilterOption(
    val dateKey: String,
    val label: String
)

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
    val historyShift: String = FILTER_ALL,
    val draftDateKey: String = FILTER_ALL,
    val draftShift: String = FILTER_ALL,
    val activeDraftDateKey: String = FirestorePlantConfig.formatDateKey(System.currentTimeMillis()),
    val viewingCloudReportMeta: Pair<String, Long>? = null,
    val isViewingOtherShiftReport: Boolean = false
)

private data class CloudBundle(
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
    // Cloud & Realtime Cross-Shift Monitoring state
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
    val historyShiftFilter: String = FILTER_ALL,
    val availableHistoryDates: List<DateFilterOption> = emptyList(),
    val cloudShiftReports: List<ShiftReportCloudDoc> = emptyList(),
    val filteredCloudShiftReports: List<ShiftReportCloudDoc> = emptyList(),
    val draftFilterDateKey: String = FILTER_ALL,
    val draftFilterShift: String = FILTER_ALL,
    val availableDraftDates: List<DateFilterOption> = emptyList(),
    val activeDraftDateKey: String = "",
    val activeDraftUpdatedBy: String = "",
    val activeDraftUpdatedAtMs: Long = 0L,
    val isViewingOtherShiftReport: Boolean = false
) {
    val totalRecordedTon: Double get() = totalRecordedKg / 1000.0
    val isManualTankSelected: Boolean get() = selectedTank?.isManualKg == true
}

class SoundingViewModel(
    private val appContext: Context,
    private val localRepository: SoundingLogRepository,
    private val cloudRepository: FirestoreSoundingRepository,
    private val networkMonitor: NetworkConnectivityMonitor,
    private val productPreferences: ProductPreferences,
    private val licenseManager: LicenseManager,
    private val shiftDraftPreferences: ShiftDraftPreferences
) : ViewModel() {

    private val currentUser = Firebase.auth.currentUser
    private val currentUserUid: String = currentUser?.uid.orEmpty()
    private val currentUserName: String = currentUser?.displayName?.trim().orEmpty()
    private val currentUserEmail: String = currentUser?.email?.trim().orEmpty()

    private val datasetFlow = MutableStateFlow<CalibrationDataset?>(null)
    private val inputState = MutableStateFlow(CalculatorInputState())
    private val filterState = MutableStateFlow(FilterState())
    private val cloudErrorFlow = MutableStateFlow<String?>(null)

    private val favoriteProductsFlow = MutableStateFlow<Set<String>>(productPreferences.loadFavoriteProducts())
    private val darkThemeFlow = MutableStateFlow(productPreferences.loadDarkTheme(defaultValue = false))
    private val lastOperatorFlow = MutableStateFlow(
        productPreferences.loadLastOperator().ifBlank { currentUserName }
    )
    private val licenseStateFlow = MutableStateFlow(licenseManager.refreshState())
    private val draftFlow = MutableStateFlow(
        shiftDraftPreferences.loadDraft()
            ?: ShiftDraftEngine.createInitialDraft(
                logs = emptyList(),
                defaultOperator = lastOperatorFlow.value
            )
    )

    private var draftCloudSyncJob: Job? = null

    private data class PrefsSnapshot(
        val favorites: Set<String>,
        val isDark: Boolean,
        val operator: String,
        val licenseState: LicenseState,
        val draft: ShiftReportDraft
    )

    private val prefsFlow = combine(
        favoriteProductsFlow,
        darkThemeFlow,
        lastOperatorFlow,
        licenseStateFlow,
        draftFlow
    ) { favorites, isDark, operator, license, draft ->
        PrefsSnapshot(favorites, isDark, operator, license, draft)
    }

    // Two-tier error-handling on every Firestore snapshot Flow
    private val tankStocksStream = cloudRepository.observeCurrentTankStocks()
        .catch { e ->
            handleFlowException(e, FirestorePlantConfig.COLLECTION_CURRENT_TANK_STOCKS)
            emit(CloudSnapshotResult(emptyList(), isFromCache = true))
        }

    private val cloudHistoryStream = cloudRepository.observeSoundingHistory()
        .catch { e ->
            handleFlowException(e, FirestorePlantConfig.COLLECTION_SOUNDING_HISTORY)
            emit(CloudSnapshotResult(emptyList(), isFromCache = true))
        }

    private val cloudShiftReportsStream = cloudRepository.observeShiftReports()
        .catch { e ->
            handleFlowException(e, FirestorePlantConfig.COLLECTION_SHIFT_REPORTS)
            emit(CloudSnapshotResult(emptyList(), isFromCache = true))
        }

    private val cloudBundleFlow = combine(
        tankStocksStream,
        cloudHistoryStream,
        cloudShiftReportsStream,
        networkMonitor.isOnline,
        cloudErrorFlow
    ) { stocks, history, reports, isOnline, cloudErr ->
        CloudBundle(
            tankStocks = stocks,
            history = history,
            shiftReports = reports,
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
        val (favorites, isDark, operator, license, rawDraft) = prefs

        // Merge cloud history (all shifts realtime) with local-only Room entries
        val mergedHistory = mergeCloudAndLocalHistory(
            cloudDocs = cloud.history.data,
            localLogs = localLogs,
            defaultOperator = operator
        )

        val availableHistoryDates = mergedHistory
            .map { it.dateKey to SoundingFormatters.formatLocalDate(it.entity.createdAtEpochMs) }
            .distinctBy { it.first }
            .sortedByDescending { it.first }
            .map { (key, label) -> DateFilterOption(dateKey = key, label = label) }

        val filteredHistory = mergedHistory.filter { record ->
            val dateMatches = filters.historyDateKey == FILTER_ALL || record.dateKey == filters.historyDateKey
            val shiftMatches = filters.historyShift == FILTER_ALL ||
                record.shiftLabel.equals(filters.historyShift, ignoreCase = true)
            dateMatches && shiftMatches
        }

        val filteredLogEntities = filteredHistory.map { it.entity }
        val allMergedEntities = mergedHistory.map { it.entity }
        val totalFilteredKg = filteredLogEntities.sumOf { it.stockKg }

        val availableDraftDates = cloud.shiftReports.data
            .map { it.dateKey to it.dateLabel.ifBlank { it.dateKey } }
            .distinctBy { it.first }
            .sortedByDescending { it.first }
            .map { (key, label) -> DateFilterOption(dateKey = key, label = label) }

        val filteredShiftReports = cloud.shiftReports.data.filter { report ->
            val dateMatches = filters.draftDateKey == FILTER_ALL || report.dateKey == filters.draftDateKey
            val shiftMatches = filters.draftShift == FILTER_ALL ||
                report.shift.equals(filters.draftShift, ignoreCase = true)
            dateMatches && shiftMatches
        }

        // Sync draft automatically with matching shift/date logs when not inspecting an archived cloud report
        val draftLogsForSync = mergedHistory
            .filter {
                it.dateKey == filters.activeDraftDateKey &&
                    (it.shiftLabel.isBlank() || it.shiftLabel.equals(rawDraft.shift.label, ignoreCase = true))
            }
            .map { it.entity }
            .ifEmpty { allMergedEntities }

        val syncedDraft = if (filters.isViewingOtherShiftReport) {
            rawDraft
        } else {
            ShiftDraftEngine.syncDraftWithHistory(
                current = rawDraft,
                logs = draftLogsForSync,
                defaultOperator = operator,
                forceOverwriteManual = false
            )
        }

        val hasPendingWrites = cloud.tankStocks.hasPendingWrites ||
            cloud.history.hasPendingWrites ||
            cloud.shiftReports.hasPendingWrites
        val isOnlineRealtime = cloud.isNetworkOnline && cloud.cloudError == null

        if (dataset == null || !input.isLoaded) {
            SoundingUiState(
                isLoaded = false,
                isDarkTheme = isDark,
                selectedTab = input.selectedTab,
                favoriteProducts = favorites,
                lastOperatorName = operator,
                logs = filteredLogEntities,
                totalRecordedKg = totalFilteredKg,
                licenseState = license,
                shiftDraft = syncedDraft,
                isOnlineRealtime = isOnlineRealtime,
                hasPendingCloudWrites = hasPendingWrites,
                cloudSyncError = cloud.cloudError,
                currentUserUid = currentUserUid,
                currentUserName = currentUserName,
                currentUserEmail = currentUserEmail,
                currentTankStocks = cloud.tankStocks.data,
                historyRecords = mergedHistory,
                filteredHistoryRecords = filteredHistory,
                historyDateFilter = filters.historyDateKey,
                historyShiftFilter = filters.historyShift,
                availableHistoryDates = availableHistoryDates,
                cloudShiftReports = cloud.shiftReports.data,
                filteredCloudShiftReports = filteredShiftReports,
                draftFilterDateKey = filters.draftDateKey,
                draftFilterShift = filters.draftShift,
                availableDraftDates = availableDraftDates,
                activeDraftDateKey = filters.activeDraftDateKey,
                activeDraftUpdatedBy = filters.viewingCloudReportMeta?.first.orEmpty(),
                activeDraftUpdatedAtMs = filters.viewingCloudReportMeta?.second ?: 0L,
                isViewingOtherShiftReport = filters.isViewingOtherShiftReport
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
                lastOperatorName = operator,
                calculation = calc,
                logs = filteredLogEntities,
                totalRecordedKg = totalFilteredKg,
                licenseState = license,
                shiftDraft = syncedDraft,
                isOnlineRealtime = isOnlineRealtime,
                hasPendingCloudWrites = hasPendingWrites,
                cloudSyncError = cloud.cloudError,
                currentUserUid = currentUserUid,
                currentUserName = currentUserName,
                currentUserEmail = currentUserEmail,
                currentTankStocks = cloud.tankStocks.data,
                historyRecords = mergedHistory,
                filteredHistoryRecords = filteredHistory,
                historyDateFilter = filters.historyDateKey,
                historyShiftFilter = filters.historyShift,
                availableHistoryDates = availableHistoryDates,
                cloudShiftReports = cloud.shiftReports.data,
                filteredCloudShiftReports = filteredShiftReports,
                draftFilterDateKey = filters.draftDateKey,
                draftFilterShift = filters.draftShift,
                availableDraftDates = availableDraftDates,
                activeDraftDateKey = filters.activeDraftDateKey,
                activeDraftUpdatedBy = filters.viewingCloudReportMeta?.first.orEmpty(),
                activeDraftUpdatedAtMs = filters.viewingCloudReportMeta?.second ?: 0L,
                isViewingOtherShiftReport = filters.isViewingOtherShiftReport
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
            isOnlineRealtime = networkMonitor.isCurrentlyOnline(),
            currentUserUid = currentUserUid,
            currentUserName = currentUserName,
            currentUserEmail = currentUserEmail,
            activeDraftDateKey = filterState.value.activeDraftDateKey
        )
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
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
        }
    }

    private fun handleFlowException(error: Throwable, path: String) {
        val firestoreEx = (error as? FirebaseFirestoreException)
            ?: (error.cause as? FirebaseFirestoreException)
        if (error is CancellationException && firestoreEx == null) {
            throw error
        }
        val exception = (firestoreEx ?: error as? Exception) ?: Exception(error)
        runCatching { handleFirestoreError(exception, OperationType.LIST, path) }
        Log.e("SoundingViewModel", "Firestore stream error on $path", exception)
        cloudErrorFlow.value = exception.localizedMessage ?: "Sinkronisasi cloud terganggu"
    }

    private fun mergeCloudAndLocalHistory(
        cloudDocs: List<SoundingHistoryDoc>,
        localLogs: List<SoundingLogEntity>,
        defaultOperator: String
    ): List<HistoryRecordItem> {
        val cloudRecords = cloudDocs.map { doc ->
            val entity = doc.toSoundingLogEntity()
            val resolvedDateKey = doc.dateKey.ifBlank {
                FirestorePlantConfig.formatDateKey(entity.createdAtEpochMs)
            }
            HistoryRecordItem(
                entity = entity,
                cloudDocId = doc.id,
                authorUid = doc.userId,
                operatorName = doc.operatorName,
                shiftLabel = doc.shift.ifBlank {
                    ShiftDraftEngine.defaultShiftForEpoch(entity.createdAtEpochMs).label
                },
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
            val shiftLabel = local.shift.ifBlank {
                ShiftDraftEngine.defaultShiftForEpoch(epoch).label
            }
            HistoryRecordItem(
                entity = local,
                cloudDocId = null,
                authorUid = currentUserUid,
                operatorName = defaultOperator,
                shiftLabel = shiftLabel,
                dateKey = FirestorePlantConfig.formatDateKey(epoch),
                isCloudSynced = false
            )
        }

        return (cloudRecords + localOnlyRecords)
            .sortedByDescending { it.entity.createdAtEpochMs }
    }

    fun setHistoryDateFilter(dateKey: String) {
        filterState.update { it.copy(historyDateKey = dateKey) }
    }

    fun setHistoryShiftFilter(shift: String) {
        filterState.update { it.copy(historyShift = shift) }
    }

    fun setDraftDateFilter(dateKey: String) {
        filterState.update { it.copy(draftDateKey = dateKey) }
    }

    fun setDraftShiftFilter(shift: String) {
        filterState.update { it.copy(draftShift = shift) }
    }

    fun openCloudShiftReport(report: ShiftReportCloudDoc) {
        val parsed = report.toShiftReportDraft() ?: return
        draftFlow.value = parsed
        filterState.update {
            it.copy(
                activeDraftDateKey = report.dateKey.ifBlank { it.activeDraftDateKey },
                viewingCloudReportMeta = report.operatorName to report.updatedAtEpochMs,
                isViewingOtherShiftReport = true
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            shiftDraftPreferences.saveDraft(parsed)
        }
    }

    fun returnToCurrentShiftDraft() {
        val todayKey = FirestorePlantConfig.formatDateKey(System.currentTimeMillis())
        val currentShift = ShiftDraftEngine.defaultShiftForEpoch(System.currentTimeMillis())
        val existingTodayReport = uiState.value.cloudShiftReports.firstOrNull {
            it.dateKey == todayKey && it.shift.equals(currentShift.label, ignoreCase = true)
        }
        val restored = existingTodayReport?.toShiftReportDraft()
            ?: ShiftDraftEngine.createInitialDraft(
                logs = uiState.value.historyRecords
                    .filter { it.dateKey == todayKey }
                    .map { it.entity },
                defaultOperator = lastOperatorFlow.value
            )
        draftFlow.value = restored
        filterState.update {
            it.copy(
                activeDraftDateKey = todayKey,
                viewingCloudReportMeta = null,
                isViewingOtherShiftReport = false
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            shiftDraftPreferences.saveDraft(restored)
        }
    }

    fun saveDraftToCloudNow(onResult: (Boolean) -> Unit = {}) {
        val draft = uiState.value.shiftDraft
        val dateKey = filterState.value.activeDraftDateKey.ifBlank {
            FirestorePlantConfig.formatDateKey(System.currentTimeMillis())
        }
        viewModelScope.launch(Dispatchers.IO) {
            shiftDraftPreferences.saveDraft(draft)
            val result = cloudRepository.saveShiftReport(draft = draft, dateKey = dateKey)
            if (result.isSuccess) {
                cloudErrorFlow.value = null
            }
            launch(Dispatchers.Main) {
                onResult(result.isSuccess)
            }
        }
    }

    fun refreshLicenseState() {
        licenseStateFlow.value = licenseManager.refreshState()
    }

    fun activateLicense(rawCode: String): LicenseActivationResult {
        val result = licenseManager.activateCode(rawCode)
        licenseStateFlow.value = licenseManager.refreshState()
        return result
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
        val base = uiState.value.shiftDraft
        val updated = transform(base)
        draftFlow.value = updated
        val dateKey = filterState.value.activeDraftDateKey.ifBlank {
            FirestorePlantConfig.formatDateKey(System.currentTimeMillis())
        }
        viewModelScope.launch(Dispatchers.IO) {
            shiftDraftPreferences.saveDraft(updated)
        }
        // Debounced auto-sync to Firestore shift_reports (works offline via persistent cache)
        draftCloudSyncJob?.cancel()
        draftCloudSyncJob = viewModelScope.launch(Dispatchers.IO) {
            delay(750L)
            val result = cloudRepository.saveShiftReport(draft = updated, dateKey = dateKey)
            if (result.isSuccess) {
                cloudErrorFlow.value = null
            }
        }
    }

    fun updateDraftDate(dateLabel: String) {
        updateAndPersistDraft { it.copy(dateLabel = dateLabel) }
    }

    fun updateDraftShift(shift: ReportTimeSlot) {
        updateAndPersistDraft { it.copy(shift = shift) }
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
        val activeShift = uiState.value.shiftDraft.shift
        val matchingLogs = uiState.value.historyRecords
            .filter {
                it.dateKey == activeDateKey &&
                    (it.shiftLabel.isBlank() || it.shiftLabel.equals(activeShift.label, ignoreCase = true))
            }
            .map { it.entity }
            .ifEmpty { uiState.value.historyRecords.map { it.entity } }
        val operator = lastOperatorFlow.value
        filterState.update { it.copy(isViewingOtherShiftReport = false) }
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
        val todayKey = FirestorePlantConfig.formatDateKey(System.currentTimeMillis())
        val logs = uiState.value.historyRecords
            .filter { it.dateKey == todayKey }
            .map { it.entity }
            .ifEmpty { uiState.value.historyRecords.map { it.entity } }
        val operator = lastOperatorFlow.value
        val fresh = ShiftDraftEngine.createInitialDraft(
            logs = logs,
            defaultOperator = operator
        )
        draftFlow.value = fresh
        filterState.update {
            it.copy(
                activeDraftDateKey = todayKey,
                viewingCloudReportMeta = null,
                isViewingOtherShiftReport = false
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            shiftDraftPreferences.clearDraft()
            shiftDraftPreferences.saveDraft(fresh)
            cloudRepository.saveShiftReport(fresh, todayKey)
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
        viewModelScope.launch(Dispatchers.IO) {
            productPreferences.saveFavoriteProducts(updated)
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
        val activeShift = uiState.value.shiftDraft.shift
        val operatorName = lastOperatorFlow.value.ifBlank { currentUserName }
        val now = System.currentTimeMillis()

        viewModelScope.launch(Dispatchers.IO) {
            // 1. Save immediately to local Room database for instant offline durability
            localRepository.saveCalculation(
                calc = calc,
                shiftLabel = activeShift.label,
                recordedAtEpochMs = now
            )
            // 2. Save to Firestore sounding_history + current_tank_stocks (queued automatically when offline)
            val cloudResult = cloudRepository.saveSoundingAndTankStock(
                calc = calc,
                shift = activeShift,
                operatorName = operatorName,
                recordedAtEpochMs = now
            )
            if (cloudResult.isSuccess) {
                cloudErrorFlow.value = null
            }
            launch(Dispatchers.Main) {
                onSaved(calc.tankId, SoundingFormatters.formatIndonesianInteger(calc.stockKg))
            }
        }
    }

    fun deleteHistoryRecord(
        item: HistoryRecordItem,
        onResult: (deleted: Boolean, tankId: String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var deleted = false
            if (item.cloudDocId != null) {
                if (item.authorUid == currentUserUid) {
                    val res = cloudRepository.deleteSoundingHistory(item.cloudDocId)
                    deleted = res.isSuccess
                }
            } else if (item.entity.id > 0L) {
                localRepository.deleteLog(item.entity.id)
                deleted = true
            }
            if (item.entity.id > 0L) {
                localRepository.deleteLog(item.entity.id)
            }
            launch(Dispatchers.Main) {
                onResult(deleted, item.entity.tankId)
            }
        }
    }

    fun clearLocalLogs(onCleared: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            localRepository.clearAll()
            // Also delete own cloud records if present
            val ownCloudIds = uiState.value.historyRecords
                .filter { it.cloudDocId != null && it.authorUid == currentUserUid }
                .mapNotNull { it.cloudDocId }
            ownCloudIds.forEach { docId ->
                cloudRepository.deleteSoundingHistory(docId)
            }
            launch(Dispatchers.Main) {
                onCleared()
            }
        }
    }

    fun getCsvExportText(): String? {
        val logs = uiState.value.logs
        if (logs.isEmpty()) return null
        return SoundingFormatters.buildCsvContent(logs)
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
        fun provideFactory(appContext: Context): ViewModelProvider.Factory {
            val ctx = appContext.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SoundingViewModel(
                        appContext = ctx,
                        localRepository = SoundingLogRepository(
                            AppDatabase.getInstance(ctx).soundingLogDao()
                        ),
                        cloudRepository = FirestoreSoundingRepository(ctx),
                        networkMonitor = NetworkConnectivityMonitor(ctx),
                        productPreferences = ProductPreferences(ctx),
                        licenseManager = LicenseManager(SharedPreferencesLicenseStorage(ctx)),
                        shiftDraftPreferences = ShiftDraftPreferences(ctx)
                    ) as T
                }
            }
        }
    }
}
