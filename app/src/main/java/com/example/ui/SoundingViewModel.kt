package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CalibrationEngine
import com.example.data.local.AppDatabase
import com.example.data.local.ProductPreferences
import com.example.data.local.SoundingLogEntity
import com.example.data.local.SoundingLogRepository
import com.example.data.model.CalibrationDataset
import com.example.data.model.SoundingCalculation
import com.example.data.model.TankSpec
import com.example.util.SoundingFormatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab {
    CALCULATOR,
    HISTORY
}

private data class CalculatorInputState(
    val isLoaded: Boolean = false,
    val selectedTab: AppTab = AppTab.CALCULATOR,
    val selectedTankIndex: Int = 0,
    val selectedProduct: String = "",
    val soundingInput: String = "",
    val tempInput: String = "30",
    val manualKgInput: String = "",
    val manualNoteInput: String = ""
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
    val manualKgInput: String = "",
    val manualNoteInput: String = "",
    val lastOperatorName: String = "",
    val calculation: SoundingCalculation? = null,
    val logs: List<SoundingLogEntity> = emptyList(),
    val totalRecordedKg: Long = 0L
) {
    val totalRecordedTon: Double get() = totalRecordedKg / 1000.0
    val isManualTankSelected: Boolean get() = selectedTank?.isManualKg == true
}

class SoundingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SoundingLogRepository(
        AppDatabase.getInstance(application).soundingLogDao()
    )
    private val productPreferences = ProductPreferences(application)

    private val datasetFlow = MutableStateFlow<CalibrationDataset?>(null)
    private val inputState = MutableStateFlow(CalculatorInputState())
    private val favoriteProductsFlow = MutableStateFlow<Set<String>>(productPreferences.loadFavoriteProducts())
    private val darkThemeFlow = MutableStateFlow(productPreferences.loadDarkTheme(defaultValue = false))
    private val lastOperatorFlow = MutableStateFlow(productPreferences.loadLastOperator())

    private val prefsFlow = combine(
        favoriteProductsFlow,
        darkThemeFlow,
        lastOperatorFlow
    ) { favorites, isDark, operator ->
        Triple(favorites, isDark, operator)
    }

    val uiState: StateFlow<SoundingUiState> = combine(
        datasetFlow,
        inputState,
        prefsFlow,
        repository.logs
    ) { dataset, input, prefs, allLogs ->
        val (favorites, isDark, operator) = prefs
        val totalKg = allLogs.sumOf { it.stockKg }
        if (dataset == null || !input.isLoaded) {
            SoundingUiState(
                isLoaded = false,
                isDarkTheme = isDark,
                selectedTab = input.selectedTab,
                favoriteProducts = favorites,
                lastOperatorName = operator,
                logs = allLogs,
                totalRecordedKg = totalKg
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

            // Favorited products always appear at the very top, preserving original order within each group
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
                manualKgInput = input.manualKgInput,
                manualNoteInput = input.manualNoteInput,
                lastOperatorName = operator,
                calculation = calc,
                logs = allLogs,
                totalRecordedKg = totalKg
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SoundingUiState(
            isDarkTheme = darkThemeFlow.value,
            favoriteProducts = favoriteProductsFlow.value,
            lastOperatorName = lastOperatorFlow.value
        )
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = CalibrationEngine.loadFromAssets(getApplication())
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
                tempInput = nextTemp
            )
        }
    }

    fun selectProduct(product: String) {
        inputState.update { it.copy(selectedProduct = product) }
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
        inputState.update { it.copy(soundingInput = sanitized) }
    }

    fun updateTempInput(raw: String) {
        val sanitized = sanitizeNumericInput(raw, allowNegative = true)
        inputState.update { it.copy(tempInput = sanitized) }
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
                manualKgInput = "",
                manualNoteInput = "",
                tempInput = if (tank?.tRef == 30.0) "30" else it.tempInput
            )
        }
    }

    fun saveCurrentCalculation(onSaved: (tankId: String, formattedKg: String) -> Unit) {
        val calc = uiState.value.calculation ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveCalculation(calc)
            launch(Dispatchers.Main) {
                onSaved(calc.tankId, SoundingFormatters.formatIndonesianInteger(calc.stockKg))
            }
        }
    }

    fun deleteLog(log: SoundingLogEntity, onDeleted: (tankId: String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteLog(log.id)
            launch(Dispatchers.Main) {
                onDeleted(log.tankId)
            }
        }
    }

    fun clearAllLogs(onCleared: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
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
}
