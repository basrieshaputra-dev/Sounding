package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CalibrationEngine
import com.example.data.firebase.CurrentTankStockDoc
import com.example.data.model.TankSpec
import com.example.ui.theme.MonoTypography
import com.example.ui.theme.SaveGradientGreen
import com.example.ui.theme.SoundingThemeExtras
import com.example.util.SoundingFormatters

private val SheetTopCornerShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
private val BentoCardShape = RoundedCornerShape(20.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TankPickerBottomSheet(
    tanks: List<TankSpec>,
    selectedTankId: String?,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    currentTankStocks: List<CurrentTankStockDoc> = emptyList()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val extras = SoundingThemeExtras.colors

    val maxCapacityTon = remember(tanks) {
        tanks.maxOfOrNull { it.capacityTon }?.coerceAtLeast(1) ?: 100
    }
    val stockByTankId = remember(currentTankStocks) {
        currentTankStocks.associateBy { it.tankId.trim().uppercase() }
    }

    val filteredWithIndices = remember(tanks, query) {
        val q = query.trim().lowercase()
        tanks.mapIndexedNotNull { idx, tank ->
            if (q.isEmpty() ||
                tank.id.lowercase().contains(q) ||
                tank.label.lowercase().contains(q)
            ) {
                idx to tank
            } else {
                null
            }
        }
    }

    val searchFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = extras.limeAccent,
        unfocusedBorderColor = extras.glassBorder,
        focusedLabelColor = extras.leafAccentReadable,
        cursorColor = extras.leafAccentReadable,
        focusedContainerColor = extras.elevatedSurface,
        unfocusedContainerColor = extras.elevatedSurface
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = SheetTopCornerShape,
        containerColor = extras.glassSheetSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.tank_picker_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_tank_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_close)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tank_search_input"),
                colors = searchFieldColors,
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = extras.leafAccentReadable
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_clear_search)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(999.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = filteredWithIndices,
                    key = { (_, tank) -> tank.id }
                ) { (originalIdx, tank) ->
                    val isSelected = tank.id == selectedTankId
                    val liveStock = stockByTankId[tank.id.trim().uppercase()]
                    TankGridBentoCard(
                        tank = tank,
                        isSelected = isSelected,
                        maxCapacityTon = maxCapacityTon,
                        liveStock = liveStock,
                        onClick = {
                            onSelect(originalIdx)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TankGridBentoCard(
    tank: TankSpec,
    isSelected: Boolean,
    maxCapacityTon: Int,
    liveStock: CurrentTankStockDoc?,
    onClick: () -> Unit
) {
    val extras = SoundingThemeExtras.colors
    val hasLiveStock = !tank.isManualKg &&
        tank.capacityTon > 0 &&
        liveStock != null &&
        liveStock.totalStockKg > 0L

    val barProgress = remember(tank, maxCapacityTon, liveStock) {
        when {
            tank.isManualKg -> 0.42f
            hasLiveStock -> {
                val stockTon = (liveStock?.totalStockKg ?: 0L).toDouble() / 1000.0
                (stockTon / tank.capacityTon.toDouble()).toFloat().coerceIn(0.08f, 1f)
            }
            else -> {
                (tank.capacityTon.toFloat() / maxCapacityTon.toFloat()).coerceIn(0.20f, 1f)
            }
        }
    }

    val cardBg = if (isSelected) {
        extras.leafContainer
    } else {
        extras.glassCardSurface
    }
    val borderColor = if (isSelected) {
        extras.limeAccent
    } else {
        extras.glassBorder
    }
    val progressBrush = Brush.horizontalGradient(
        colors = listOf(SaveGradientGreen, extras.limeAccent, extras.primaryCtaEnd)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 128.dp)
            .clip(BentoCardShape)
            .clickable(onClick = onClick)
            .testTag("tank_option_${tank.id}"),
        shape = BentoCardShape,
        color = cardBg,
        border = BorderStroke(
            width = if (isSelected) 1.8.dp else 1.dp,
            color = borderColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Tank Code + Selection Indicator / Capacity Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tank.id,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    ),
                    color = if (isSelected) {
                        extras.onLeafContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = extras.leafAccentReadable,
                        modifier = Modifier.size(19.dp)
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = extras.leafContainer.copy(alpha = if (extras.isDark) 0.9f else 0.85f),
                        border = BorderStroke(0.8.dp, extras.limeAccent.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = if (tank.isManualKg) {
                                stringResource(R.string.tank_grid_manual_badge)
                            } else {
                                stringResource(R.string.tank_grid_cap_pill, tank.capacityTon)
                            },
                            style = MonoTypography.badge.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = extras.onLeafContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            // Visual Capacity Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(extras.progressTrack)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(barProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(progressBrush)
                    )
                }
                if (hasLiveStock && liveStock != null) {
                    Text(
                        text = stringResource(
                            R.string.tank_grid_live_stock_format,
                            SoundingFormatters.formatTon(liveStock.totalStockKg.toDouble() / 1000.0),
                            tank.capacityTon
                        ),
                        style = MonoTypography.badge.copy(fontSize = 9.5.sp),
                        color = extras.leafAccentReadable,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Technical Specs: Meja, Ref, Kap (or Manual description)
            if (tank.isManualKg) {
                Text(
                    text = stringResource(R.string.tank_grid_manual_desc),
                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                    color = extras.leafAccentReadable,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    TankSpecMiniRow(
                        label = stringResource(R.string.tank_grid_meja_label),
                        value = "${SoundingFormatters.formatCompact(tank.tableLevel)} cm"
                    )
                    TankSpecMiniRow(
                        label = stringResource(R.string.tank_grid_ref_label),
                        value = "${SoundingFormatters.formatCompact(tank.tRef)}°C"
                    )
                    TankSpecMiniRow(
                        label = stringResource(R.string.tank_grid_kap_label),
                        value = "${tank.capacityTon} Ton",
                        emphasize = true
                    )
                }
            }
        }
    }
}

@Composable
private fun TankSpecMiniRow(
    label: String,
    value: String,
    emphasize: Boolean = false
) {
    val extras = SoundingThemeExtras.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = value,
            style = MonoTypography.badge.copy(
                fontSize = 10.5.sp,
                fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (emphasize) {
                extras.leafAccentReadable
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductPickerBottomSheet(
    products: List<String>,
    favoriteProducts: Set<String>,
    selectedProduct: String,
    onSelect: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val extras = SoundingThemeExtras.colors

    val filtered = remember(products, favoriteProducts, query) {
        val q = query.trim().lowercase()
        val matching = if (q.isEmpty()) products else products.filter { it.lowercase().contains(q) }
        CalibrationEngine.sortProductsWithFavorites(matching, favoriteProducts)
    }

    val searchFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = extras.limeAccent,
        unfocusedBorderColor = extras.glassBorder,
        focusedLabelColor = extras.leafAccentReadable,
        cursorColor = extras.leafAccentReadable,
        focusedContainerColor = extras.elevatedSurface,
        unfocusedContainerColor = extras.elevatedSurface
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = SheetTopCornerShape,
        containerColor = extras.glassSheetSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.product_picker_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_product_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_close)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_search_input"),
                colors = searchFieldColors,
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = extras.leafAccentReadable
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_clear_search)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(999.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = filtered,
                    key = { _, item -> item }
                ) { index, product ->
                    val isSelected = product == selectedProduct
                    val isFavorite = product in favoriteProducts

                    Surface(
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .clip(BentoCardShape)
                            .clickable {
                                onSelect(product)
                                onDismiss()
                            }
                            .testTag("product_option_$index"),
                        shape = BentoCardShape,
                        color = when {
                            isSelected -> extras.leafContainer
                            isFavorite -> extras.leafContainer.copy(alpha = 0.55f)
                            else -> extras.glassCardSurface
                        },
                        border = BorderStroke(
                            width = if (isSelected || isFavorite) 1.5.dp else 1.dp,
                            color = when {
                                isSelected -> extras.limeAccent
                                isFavorite -> extras.limeAccent.copy(alpha = 0.55f)
                                else -> extras.glassBorder
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = product,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) {
                                        extras.onLeafContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isFavorite) {
                                    Text(
                                        text = stringResource(R.string.favorite_badge_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = extras.leafAccentReadable
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = extras.leafAccentReadable,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onToggleFavorite(product) },
                                    modifier = Modifier.testTag("favorite_product_$index")
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isFavorite) {
                                            extras.limeAccent
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                        },
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isFavorite) {
                                                extras.limeBright
                                            } else {
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                                            }
                                        ),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isFavorite) {
                                                Icons.Filled.Star
                                            } else {
                                                Icons.Outlined.StarOutline
                                            },
                                            contentDescription = stringResource(
                                                if (isFavorite) {
                                                    R.string.cd_favorite_remove
                                                } else {
                                                    R.string.cd_favorite_add
                                                },
                                                product
                                            ),
                                            tint = if (isFavorite) {
                                                extras.onLimeButton
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.padding(6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
