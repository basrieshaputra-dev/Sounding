package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.BlendingEngine
import com.example.data.BlendingQuantities
import com.example.data.PearsonSquareResult
import com.example.data.QuantityInputSource
import com.example.data.ShorteningSpec
import com.example.ui.theme.MonoTypography
import com.example.ui.theme.OilYellowDeep
import com.example.ui.theme.SoundingThemeExtras
import com.example.util.SoundingFormatters
import kotlin.math.abs

private val BlendingCardShape = RoundedCornerShape(20.dp)
private val BlendingPillShape = RoundedCornerShape(999.dp)
private val BlendingCtaShape = RoundedCornerShape(16.dp)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlendingScreen(
    activeAccountName: String = "",
    onCopyBlendingSummary: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val extras = SoundingThemeExtras.colors
    val scrollState = rememberScrollState()

    // Step 1: Selected Shortening Melting Point (default: Shortening 42 - 45)
    val defaultSpec = remember {
        BlendingEngine.SHORTENING_SPECS.firstOrNull { it.id == "sh_42_45" }
            ?: BlendingEngine.SHORTENING_SPECS.first()
    }
    var selectedSpec by remember { mutableStateOf(defaultSpec) }
    var showSpecPicker by remember { mutableStateOf(false) }

    // Active ratio state (either Standard Table or Pearson Square Auto-Corrected)
    var activeRpoPercent by remember { mutableDoubleStateOf(defaultSpec.standardRpoPercent) }
    var activeRpsPercent by remember { mutableDoubleStateOf(defaultSpec.standardRpsPercent) }
    var isRatioAutoCorrected by remember { mutableStateOf(false) }
    var ratioCorrectionNote by remember { mutableStateOf<String?>(null) }

    // Step 2: Actual IV inputs (editable by operator)
    var ivRpoText by remember { mutableStateOf("52.0") }
    var ivRpsText by remember { mutableStateOf("34.5") }

    // Step 3: 3-Way Realtime Linked Quantities (anchor source prevents infinite loops)
    var totalKgText by remember { mutableStateOf("") }
    var rpoKgText by remember { mutableStateOf("") }
    var rpsKgText by remember { mutableStateOf("") }
    var lastEditedSource by remember { mutableStateOf(QuantityInputSource.TOTAL) }
    var quantities by remember { mutableStateOf(BlendingQuantities()) }

    val parsedIvRpo = remember(ivRpoText) {
        BlendingEngine.parseDecimal(ivRpoText) ?: BlendingEngine.DEFAULT_IV_RPO
    }
    val parsedIvRps = remember(ivRpsText) {
        BlendingEngine.parseDecimal(ivRpsText) ?: BlendingEngine.DEFAULT_IV_RPS
    }

    val estimatedIv = remember(activeRpoPercent, activeRpsPercent, parsedIvRpo, parsedIvRps) {
        BlendingEngine.calculateEstimatedIv(
            rpoPercent = activeRpoPercent,
            rpsPercent = activeRpsPercent,
            ivRpo = parsedIvRpo,
            ivRps = parsedIvRps
        )
    }

    val isInSpec = remember(estimatedIv, selectedSpec) {
        BlendingEngine.isWithinTypicalIv(estimatedIv, selectedSpec)
    }

    val isRpsDisabled = activeRpsPercent <= 0.0
    val isUsingStandardRatio = !isRatioAutoCorrected &&
        abs(activeRpoPercent - selectedSpec.standardRpoPercent) < 0.05 &&
        abs(activeRpsPercent - selectedSpec.standardRpsPercent) < 0.05

    fun sanitizeNumericInput(raw: String): String {
        val sb = StringBuilder()
        var hasDecimal = false
        for (ch in raw) {
            if (ch.isDigit()) {
                sb.append(ch)
            } else if ((ch == '.' || ch == ',') && !hasDecimal) {
                hasDecimal = true
                sb.append('.')
            }
        }
        return sb.toString().take(12)
    }

    // Recalculate ONLY the non-active columns from the currently edited source (zero feedback loop)
    fun recalcFromSource(
        source: QuantityInputSource,
        rawText: String,
        rpoPct: Double = activeRpoPercent,
        rpsPct: Double = activeRpsPercent
    ) {
        try {
            val cleaned = sanitizeNumericInput(rawText)
            lastEditedSource = source
            when (source) {
                QuantityInputSource.TOTAL -> totalKgText = cleaned
                QuantityInputSource.RPO -> rpoKgText = cleaned
                QuantityInputSource.RPS -> rpsKgText = cleaned
            }

            val parsedVal = BlendingEngine.parseDecimal(cleaned)
            if (parsedVal == null || parsedVal <= 0.0) {
                quantities = BlendingQuantities()
                if (source != QuantityInputSource.TOTAL) totalKgText = ""
                if (source != QuantityInputSource.RPO) rpoKgText = ""
                if (source != QuantityInputSource.RPS) rpsKgText = ""
                return
            }

            val computed = BlendingEngine.calculateQuantities(
                source = source,
                inputValueKg = parsedVal,
                rpoPercent = rpoPct,
                rpsPercent = rpsPct
            )
            quantities = computed

            if (source != QuantityInputSource.TOTAL) {
                totalKgText = BlendingEngine.formatInputKg(computed.totalKg)
            }
            if (source != QuantityInputSource.RPO) {
                rpoKgText = BlendingEngine.formatInputKg(computed.rpoKg)
            }
            if (source != QuantityInputSource.RPS) {
                rpsKgText = if (rpsPct <= 0.0) "" else BlendingEngine.formatInputKg(computed.rpsKg)
            }
        } catch (_: Throwable) {
            quantities = BlendingQuantities()
        }
    }

    // Re-apply active anchor quantity whenever Melting Point or Ratio changes
    fun applyRatioUpdate(
        nextRpoPct: Double,
        nextRpsPct: Double,
        autoCorrected: Boolean,
        note: String? = null
    ) {
        try {
            activeRpoPercent = nextRpoPct
            activeRpsPercent = nextRpsPct
            isRatioAutoCorrected = autoCorrected
            ratioCorrectionNote = note

            val effectiveSource = if (lastEditedSource == QuantityInputSource.RPS && nextRpsPct <= 0.0) {
                QuantityInputSource.TOTAL
            } else {
                lastEditedSource
            }
            val anchorText = when (effectiveSource) {
                QuantityInputSource.TOTAL -> totalKgText
                QuantityInputSource.RPO -> rpoKgText
                QuantityInputSource.RPS -> rpsKgText
            }
            if (nextRpsPct <= 0.0) {
                rpsKgText = ""
            }
            if (anchorText.isNotBlank()) {
                recalcFromSource(
                    source = effectiveSource,
                    rawText = anchorText,
                    rpoPct = nextRpoPct,
                    rpsPct = nextRpsPct
                )
            } else {
                quantities = BlendingQuantities()
            }
        } catch (_: Throwable) {
            quantities = BlendingQuantities()
        }
    }

    fun onSelectSpec(spec: ShorteningSpec) {
        selectedSpec = spec
        showSpecPicker = false
        applyRatioUpdate(
            nextRpoPct = spec.standardRpoPercent,
            nextRpsPct = spec.standardRpsPercent,
            autoCorrected = false,
            note = null
        )
    }

    fun onAutoCorrectRatio() {
        try {
            when (
                val result = BlendingEngine.calculatePearsonSquareRatio(
                    targetIv = selectedSpec.typicalIvMid,
                    ivRpo = parsedIvRpo,
                    ivRps = parsedIvRps
                )
            ) {
                is PearsonSquareResult.Computed -> {
                    val note = if (result.isClamped) {
                        context.getString(R.string.blending_pearson_clamped_note)
                    } else {
                        context.getString(
                            R.string.blending_pearson_applied_note,
                            SoundingFormatters.formatFixed(selectedSpec.typicalIvMid, 2)
                        )
                    }
                    applyRatioUpdate(
                        nextRpoPct = result.rpoPercent,
                        nextRpsPct = result.rpsPercent,
                        autoCorrected = true,
                        note = note
                    )
                }

                PearsonSquareResult.IdenticalIvError -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.blending_error_identical_iv),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                PearsonSquareResult.InvalidInputError -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.blending_error_invalid_iv),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (_: Throwable) {
            runCatching {
                Toast.makeText(
                    context,
                    context.getString(R.string.msg_about_action_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun onResetRatioToTable() {
        applyRatioUpdate(
            nextRpoPct = selectedSpec.standardRpoPercent,
            nextRpsPct = selectedSpec.standardRpsPercent,
            autoCorrected = false,
            note = null
        )
    }

    fun onResetNumbers() {
        ivRpoText = "52.0"
        ivRpsText = "34.5"
        totalKgText = ""
        rpoKgText = ""
        rpsKgText = ""
        lastEditedSource = QuantityInputSource.TOTAL
        quantities = BlendingQuantities()
        applyRatioUpdate(
            nextRpoPct = selectedSpec.standardRpoPercent,
            nextRpsPct = selectedSpec.standardRpsPercent,
            autoCorrected = false,
            note = null
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("blending_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // STEP 1 — Pilih Melting Point & Rasio Standar Tabel
            BlendingCard(tag = "blending_step1_card") {
                BlendingSectionHeader(
                    stepBadge = "1",
                    title = stringResource(R.string.blending_step1_title),
                    subtitle = stringResource(R.string.blending_step1_subtitle)
                )

                // Selector Trigger
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 58.dp)
                        .clip(BlendingCtaShape)
                        .clickable { showSpecPicker = true }
                        .testTag("blending_spec_selector"),
                    shape = BlendingCtaShape,
                    color = extras.innerContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(extras.leafContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Science,
                                    contentDescription = null,
                                    tint = extras.onLeafContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = selectedSpec.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(
                                        R.string.blending_typical_iv_chip_format,
                                        selectedSpec.typicalIvRangeLabel
                                    ),
                                    style = MonoTypography.badge,
                                    color = extras.leafAccentReadable
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.cd_dropdown),
                            tint = extras.interactiveAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Active Ratio Visual Split Bar + Info Pills
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = BlendingCtaShape,
                    color = extras.sageContainer.copy(alpha = if (extras.isDark) 0.65f else 0.78f),
                    border = BorderStroke(1.dp, extras.cardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(
                                    if (isRatioAutoCorrected) {
                                        R.string.blending_ratio_mode_pearson
                                    } else {
                                        R.string.blending_ratio_mode_standard
                                    }
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = extras.onLeafContainer
                            )
                            Surface(
                                shape = BlendingPillShape,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, extras.cardBorder)
                            ) {
                                Text(
                                    text = "Typical IV ${selectedSpec.typicalIvRangeLabel}",
                                    style = MonoTypography.badge.copy(fontSize = 10.5.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Ratio Proportion Bar (RPO green-lime vs RPS warm gold)
                        val rpoWeight = (activeRpoPercent / 100.0).toFloat().coerceIn(0f, 1f)
                        val rpsWeight = (activeRpsPercent / 100.0).toFloat().coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(BlendingPillShape)
                                .background(extras.progressTrack)
                        ) {
                            if (rpoWeight > 0.001f) {
                                Box(
                                    modifier = Modifier
                                        .weight(rpoWeight)
                                        .fillMaxHeight()
                                        .background(extras.emeraldAccent)
                                )
                            }
                            if (rpsWeight > 0.001f) {
                                Box(
                                    modifier = Modifier
                                        .weight(rpsWeight)
                                        .fillMaxHeight()
                                        .background(extras.amberAccent)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RatioStatChip(
                                label = "RPO",
                                percentText = BlendingEngine.formatPercent(activeRpoPercent),
                                dotColor = extras.emeraldAccent,
                                standardNote = if (isRatioAutoCorrected) {
                                    "Std ${BlendingEngine.formatPercent(selectedSpec.standardRpoPercent)}"
                                } else null,
                                modifier = Modifier.weight(1f)
                            )
                            RatioStatChip(
                                label = "RPS",
                                percentText = BlendingEngine.formatPercent(activeRpsPercent),
                                dotColor = extras.amberAccent,
                                standardNote = if (isRatioAutoCorrected) {
                                    "Std ${BlendingEngine.formatPercent(selectedSpec.standardRpsPercent)}"
                                } else null,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // STEP 2 — Input Nilai IV Aktual & Status Spec + Pearson Square
            BlendingCard(tag = "blending_step2_card") {
                BlendingSectionHeader(
                    stepBadge = "2",
                    title = stringResource(R.string.blending_step2_title),
                    subtitle = stringResource(R.string.blending_step2_subtitle)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = ivRpoText,
                        onValueChange = { raw ->
                            ivRpoText = sanitizeNumericInput(raw)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("blending_iv_rpo_input"),
                        singleLine = true,
                        label = { Text(stringResource(R.string.blending_iv_rpo_label)) },
                        placeholder = { Text("52.0") },
                        supportingText = { Text(stringResource(R.string.blending_iv_rpo_ref)) },
                        textStyle = MonoTypography.subMetric.copy(fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = extras.emeraldAccent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = extras.emeraldAccent,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = extras.emeraldAccent,
                            focusedContainerColor = extras.innerContainer,
                            unfocusedContainerColor = extras.innerContainer
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = ivRpsText,
                        onValueChange = { raw ->
                            ivRpsText = sanitizeNumericInput(raw)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("blending_iv_rps_input"),
                        singleLine = true,
                        label = { Text(stringResource(R.string.blending_iv_rps_label)) },
                        placeholder = { Text("34.5") },
                        supportingText = { Text(stringResource(R.string.blending_iv_rps_ref)) },
                        textStyle = MonoTypography.subMetric.copy(fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = extras.emeraldAccent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = extras.emeraldAccent,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = extras.emeraldAccent,
                            focusedContainerColor = extras.innerContainer,
                            unfocusedContainerColor = extras.innerContainer
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Live Estimated IV + Spec Status Indicator
                val statusContainerColor = if (isInSpec) extras.leafContainer else extras.warningSurface
                val statusBorderColor = if (isInSpec) extras.leafAccent.copy(alpha = 0.55f) else extras.warningBorder
                val statusAccentColor = if (isInSpec) extras.onLeafContainer else extras.warningText

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("blending_iv_status_card"),
                    shape = RoundedCornerShape(16.dp),
                    color = statusContainerColor,
                    border = BorderStroke(1.dp, statusBorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = stringResource(R.string.blending_estimated_iv_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusAccentColor.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = SoundingFormatters.formatFixed(estimatedIv, 2),
                                    style = MonoTypography.heroMetric.copy(
                                        fontSize = 30.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = if (isInSpec) extras.amberTextReadable else extras.warningText,
                                    modifier = Modifier.testTag("blending_estimated_iv_value")
                                )
                            }

                            Surface(
                                shape = BlendingPillShape,
                                color = if (isInSpec) extras.emeraldAccent else extras.warningBorder,
                                modifier = Modifier.testTag("blending_spec_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isInSpec) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
                                        contentDescription = null,
                                        tint = if (isInSpec) extras.onEmeraldAccent else Color(0xFF1F1300),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = stringResource(
                                            if (isInSpec) R.string.blending_status_in_spec else R.string.blending_status_out_spec
                                        ),
                                        style = MonoTypography.badge.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = if (isInSpec) extras.onEmeraldAccent else Color(0xFF1F1300)
                                    )
                                }
                            }
                        }

                        Text(
                            text = stringResource(
                                R.string.blending_target_iv_detail,
                                selectedSpec.typicalIvRangeLabel,
                                SoundingFormatters.formatFixed(selectedSpec.typicalIvMid, 2)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusAccentColor.copy(alpha = 0.88f)
                        )

                        if (!ratioCorrectionNote.isNullOrBlank()) {
                            Text(
                                text = ratioCorrectionNote.orEmpty(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 17.sp
                                ),
                                color = statusAccentColor
                            )
                        }

                        AnimatedVisibility(visible = !isInSpec) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .heightIn(min = 50.dp)
                                    .clip(BlendingCtaShape)
                                    .background(extras.sagePrimary)
                                    .clickable { onAutoCorrectRatio() }
                                    .testTag("blending_auto_correct_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoFixHigh,
                                        contentDescription = null,
                                        tint = extras.onSagePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.blending_action_auto_correct),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = extras.onSagePrimary
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = !isUsingStandardRatio) {
                            OutlinedButton(
                                onClick = { onResetRatioToTable() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                                    .testTag("blending_reset_ratio_button"),
                                shape = BlendingCtaShape,
                                border = BorderStroke(1.dp, statusAccentColor.copy(alpha = 0.55f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = statusAccentColor)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.blending_action_reset_table_ratio),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            // STEP 3 — Input Quantity (Kg) Fleksibel 3 Arah
            BlendingCard(tag = "blending_step3_card") {
                BlendingSectionHeader(
                    stepBadge = "3",
                    title = stringResource(R.string.blending_step3_title),
                    subtitle = stringResource(R.string.blending_step3_subtitle)
                )

                // Quick Total Batch Preset Pill Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        5_000.0 to "5 Ton",
                        10_000.0 to "10 Ton",
                        20_000.0 to "20 Ton",
                        30_000.0 to "30 Ton",
                        50_000.0 to "50 Ton"
                    )
                    presets.forEach { (presetKg, label) ->
                        val isSelected = abs(quantities.totalKg - presetKg) < 0.5 && quantities.totalKg > 0.0
                        Surface(
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .clip(BlendingPillShape)
                                .clickable {
                                    recalcFromSource(
                                        source = QuantityInputSource.TOTAL,
                                        rawText = presetKg.toLong().toString()
                                    )
                                }
                                .testTag("blending_preset_${presetKg.toLong()}"),
                            shape = BlendingPillShape,
                            color = if (isSelected) {
                                extras.amberAccent
                            } else if (extras.isDark) {
                                extras.innerContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) extras.amberAccent else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MonoTypography.badge.copy(
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                    ),
                                    color = if (isSelected) extras.onAmberAccent else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // 1. Total Batch Blending (Kg)
                QuantityLinkedField(
                    value = totalKgText,
                    label = stringResource(R.string.blending_qty_total_label),
                    placeholder = stringResource(R.string.blending_qty_total_placeholder),
                    suffixText = "kg",
                    ratioBadge = "100%",
                    isActiveAnchor = lastEditedSource == QuantityInputSource.TOTAL && totalKgText.isNotEmpty(),
                    enabled = true,
                    helperText = if (quantities.totalKg > 0.0) {
                        "= ${BlendingEngine.formatTonDisplay(quantities.totalKg)} Ton"
                    } else null,
                    tag = "blending_total_kg_input",
                    onValueChange = { raw ->
                        recalcFromSource(QuantityInputSource.TOTAL, raw)
                    }
                )

                // 2. Quantity RPO (Kg)
                QuantityLinkedField(
                    value = rpoKgText,
                    label = stringResource(R.string.blending_qty_rpo_label),
                    placeholder = stringResource(R.string.blending_qty_rpo_placeholder),
                    suffixText = "kg",
                    ratioBadge = BlendingEngine.formatPercent(activeRpoPercent),
                    isActiveAnchor = lastEditedSource == QuantityInputSource.RPO && rpoKgText.isNotEmpty(),
                    enabled = activeRpoPercent > 0.0,
                    helperText = when {
                        activeRpoPercent <= 0.0 -> stringResource(R.string.blending_rpo_zero_hint)
                        quantities.rpoKg > 0.0 -> "= ${BlendingEngine.formatTonDisplay(quantities.rpoKg)} Ton"
                        else -> null
                    },
                    tag = "blending_rpo_kg_input",
                    onValueChange = { raw ->
                        recalcFromSource(QuantityInputSource.RPO, raw)
                    }
                )

                // 3. Quantity RPS (Kg) — disabled cleanly when %RPS == 0%
                QuantityLinkedField(
                    value = if (isRpsDisabled) "" else rpsKgText,
                    label = stringResource(R.string.blending_qty_rps_label),
                    placeholder = if (isRpsDisabled) "0" else stringResource(R.string.blending_qty_rps_placeholder),
                    suffixText = "kg",
                    ratioBadge = BlendingEngine.formatPercent(activeRpsPercent),
                    isActiveAnchor = !isRpsDisabled && lastEditedSource == QuantityInputSource.RPS && rpsKgText.isNotEmpty(),
                    enabled = !isRpsDisabled,
                    helperText = when {
                        isRpsDisabled -> stringResource(R.string.blending_pure_rpo_hint)
                        quantities.rpsKg > 0.0 -> "= ${BlendingEngine.formatTonDisplay(quantities.rpsKg)} Ton"
                        else -> null
                    },
                    tag = "blending_rps_kg_input",
                    onValueChange = { raw ->
                        if (!isRpsDisabled) {
                            recalcFromSource(QuantityInputSource.RPS, raw)
                        }
                    }
                )
            }

            // BENTO SUMMARY CARD — Ringkasan Hasil Blending & Aksi Lapangan
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("blending_summary_bento_card"),
                shape = BlendingCardShape,
                color = extras.resultSurface,
                border = BorderStroke(1.dp, extras.resultBorder),
                shadowElevation = if (extras.isDark) 0.dp else 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(20.dp)
                                    .clip(BlendingPillShape)
                                    .background(extras.emeraldAccent)
                            )
                            Text(
                                text = stringResource(R.string.blending_summary_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = BlendingPillShape,
                            color = extras.leafContainer,
                            border = BorderStroke(1.dp, extras.leafAccent.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = selectedSpec.meltingPointLabel,
                                style = MonoTypography.badge.copy(fontWeight = FontWeight.Bold),
                                color = extras.onLeafContainer,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // 2-Column Bento Tiles: Kebutuhan RPO & Kebutuhan RPS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BentoQuantityTile(
                            title = stringResource(R.string.blending_summary_rpo),
                            ratioBadge = BlendingEngine.formatPercent(activeRpoPercent),
                            kgValue = BlendingEngine.formatKgDisplay(quantities.rpoKg),
                            tonValue = BlendingEngine.formatTonDisplay(quantities.rpoKg),
                            accentColor = extras.leafAccentReadable,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("blending_bento_rpo")
                        )
                        BentoQuantityTile(
                            title = stringResource(R.string.blending_summary_rps),
                            ratioBadge = BlendingEngine.formatPercent(activeRpsPercent),
                            kgValue = BlendingEngine.formatKgDisplay(quantities.rpsKg),
                            tonValue = BlendingEngine.formatTonDisplay(quantities.rpsKg),
                            accentColor = extras.amberTextReadable,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("blending_bento_rps")
                        )
                    }

                    // Full-Width Bento Highlight Row: Total Batch & Estimasi IV Akhir
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, extras.metricCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.blending_summary_total_batch),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${BlendingEngine.formatKgDisplay(quantities.totalKg)} kg",
                                    style = MonoTypography.subMetric.copy(
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = extras.metricValueInk,
                                    modifier = Modifier.testTag("blending_bento_total_kg")
                                )
                                Text(
                                    text = "${BlendingEngine.formatTonDisplay(quantities.totalKg)} Ton",
                                    style = MonoTypography.badge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(44.dp)
                                    .background(extras.cardBorder)
                            )

                            Column(
                                modifier = Modifier
                                    .weight(0.85f)
                                    .padding(start = 14.dp),
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.blending_summary_final_iv),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = SoundingFormatters.formatFixed(estimatedIv, 2),
                                    style = MonoTypography.subMetric.copy(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = if (isInSpec) extras.amberTextReadable else extras.warningText
                                )
                                Text(
                                    text = if (isInSpec) "IN SPEC" else "DI LUAR SPEC",
                                    style = MonoTypography.badge.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = if (isInSpec) extras.emeraldAccent else extras.warningText
                                )
                            }
                        }
                    }

                    // Primary CTA: Salin Hasil Blending + Reset Angka
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1.45f)
                                .heightIn(min = 52.dp)
                                .clip(BlendingCtaShape)
                                .background(extras.sagePrimary)
                                .clickable {
                                    try {
                                        val summary = BlendingEngine.formatWhatsAppSummary(
                                            spec = selectedSpec,
                                            rpoPercent = activeRpoPercent,
                                            rpsPercent = activeRpsPercent,
                                            isAutoCorrected = isRatioAutoCorrected,
                                            ivRpo = parsedIvRpo,
                                            ivRps = parsedIvRps,
                                            estimatedIv = estimatedIv,
                                            inSpec = isInSpec,
                                            quantities = quantities,
                                            operatorName = activeAccountName
                                        )
                                        onCopyBlendingSummary(summary)
                                    } catch (_: Throwable) {
                                        runCatching {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.msg_clipboard_failed),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                                .testTag("blending_copy_wa_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = null,
                                    tint = extras.onSagePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = stringResource(R.string.blending_action_copy_result),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = extras.onSagePrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { onResetNumbers() },
                            modifier = Modifier
                                .weight(0.95f)
                                .heightIn(min = 52.dp)
                                .testTag("blending_reset_button"),
                            shape = BlendingCtaShape,
                            border = BorderStroke(1.dp, extras.cardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = stringResource(R.string.blending_action_reset),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showSpecPicker) {
        ShorteningSpecPickerBottomSheet(
            selectedSpec = selectedSpec,
            onSelect = ::onSelectSpec,
            onDismiss = { showSpecPicker = false }
        )
    }
}

@Composable
private fun BlendingCard(
    tag: String,
    content: @Composable () -> Unit
) {
    val extras = SoundingThemeExtras.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        shape = BlendingCardShape,
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
            content()
        }
    }
}

@Composable
private fun BlendingSectionHeader(
    stepBadge: String,
    title: String,
    subtitle: String
) {
    val extras = SoundingThemeExtras.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(extras.emeraldAccent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepBadge,
                style = MonoTypography.badge.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = extras.onEmeraldAccent
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RatioStatChip(
    label: String,
    percentText: String,
    dotColor: Color,
    standardNote: String?,
    modifier: Modifier = Modifier
) {
    val extras = SoundingThemeExtras.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = percentText,
                    style = MonoTypography.subMetric.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!standardNote.isNullOrBlank()) {
                    Text(
                        text = standardNote,
                        style = MonoTypography.badge.copy(fontSize = 9.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun QuantityLinkedField(
    value: String,
    label: String,
    placeholder: String,
    suffixText: String,
    ratioBadge: String,
    isActiveAnchor: Boolean,
    enabled: Boolean,
    helperText: String?,
    tag: String,
    onValueChange: (String) -> Unit
) {
    val extras = SoundingThemeExtras.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        singleLine = true,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Scale,
                contentDescription = null,
                tint = if (enabled) {
                    if (isActiveAnchor) extras.leafAccentReadable else extras.interactiveAccent
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                }
            )
        },
        trailingIcon = {
            Row(
                modifier = Modifier.padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = BlendingPillShape,
                    color = if (isActiveAnchor) extras.amberContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isActiveAnchor) extras.amberAccent else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Text(
                        text = ratioBadge,
                        style = MonoTypography.badge.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                        color = if (isActiveAnchor) extras.amberTextReadable else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = suffixText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        supportingText = helperText?.let { txt ->
            {
                Text(
                    text = txt,
                    color = if (!enabled) extras.warningText else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        textStyle = MonoTypography.subMetric.copy(
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedBorderColor = extras.emeraldAccent,
            focusedLabelColor = extras.emeraldAccent,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = extras.emeraldAccent,
            unfocusedBorderColor = if (isActiveAnchor) extras.amberAccent else MaterialTheme.colorScheme.outline,
            focusedContainerColor = extras.innerContainer,
            unfocusedContainerColor = extras.innerContainer,
            disabledContainerColor = extras.innerContainer.copy(alpha = 0.55f)
        ),
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun BentoQuantityTile(
    title: String,
    ratioBadge: String,
    kgValue: String,
    tonValue: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val extras = SoundingThemeExtras.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = BlendingPillShape,
                    color = accentColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = ratioBadge,
                        style = MonoTypography.badge.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold),
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = "$kgValue kg",
                style = MonoTypography.subMetric.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$tonValue Ton",
                style = MonoTypography.badge,
                color = accentColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShorteningSpecPickerBottomSheet(
    selectedSpec: ShorteningSpec,
    onSelect: (ShorteningSpec) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val extras = SoundingThemeExtras.colors
    var searchQuery by remember { mutableStateOf("") }

    val filteredSpecs = remember(searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            BlendingEngine.SHORTENING_SPECS
        } else {
            BlendingEngine.SHORTENING_SPECS.filter { spec ->
                spec.name.lowercase().contains(q) ||
                    spec.meltingPointLabel.lowercase().contains(q) ||
                    spec.typicalIvRangeLabel.contains(q) ||
                    BlendingEngine.formatPercent(spec.standardRpoPercent).contains(q)
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
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .testTag("shortening_spec_picker_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.blending_picker_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.blending_picker_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_blending_picker")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_close)
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("blending_picker_search_input"),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.blending_picker_search_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = extras.interactiveAccent
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cd_clear_search)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = extras.leafAccent,
                    cursorColor = extras.leafAccentReadable
                ),
                shape = RoundedCornerShape(14.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(items = filteredSpecs, key = { it.id }) { spec ->
                    val isSelected = spec.id == selectedSpec.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelect(spec) }
                            .testTag("blending_spec_item_${spec.id}"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) extras.leafContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) extras.leafAccent else extras.cardBorder
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = spec.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = if (isSelected) extras.onLeafContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = BlendingPillShape,
                                    color = if (isSelected) {
                                        extras.limeAccent
                                    } else {
                                        extras.elevatedSurface
                                    },
                                    border = BorderStroke(1.dp, extras.cardBorder)
                                ) {
                                    Text(
                                        text = "IV ${spec.typicalIvRangeLabel}",
                                        style = MonoTypography.badge.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (isSelected) extras.onLimeButton else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RPO ${BlendingEngine.formatPercent(spec.standardRpoPercent)}",
                                    style = MonoTypography.badge.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) extras.onLeafContainer else extras.leafAccentReadable
                                )
                                Text(
                                    text = "•",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "RPS ${BlendingEngine.formatPercent(spec.standardRpsPercent)}",
                                    style = MonoTypography.badge.copy(fontWeight = FontWeight.Bold),
                                    color = if (spec.standardRpsPercent <= 0.0) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        OilYellowDeep
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
