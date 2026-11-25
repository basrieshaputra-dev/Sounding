package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.TankSpec
import com.example.ui.theme.MonoTypography
import com.example.ui.theme.SoundingThemeExtras
import com.example.util.SoundingFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TankPickerBottomSheet(
    tanks: List<TankSpec>,
    selectedTankId: String?,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
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
                    style = MaterialTheme.typography.titleLarge,
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
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
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
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filteredWithIndices,
                    key = { (_, tank) -> tank.id }
                ) { (originalIdx, tank) ->
                    val isSelected = tank.id == selectedTankId
                    val extras = SoundingThemeExtras.colors
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable {
                                onSelect(originalIdx)
                                onDismiss()
                            }
                            .testTag("tank_option_${tank.id}"),
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSelected) extras.leafContainer else extras.elevatedSurface,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) extras.leafAccent else extras.cardBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = tank.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) {
                                        extras.onLeafContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Text(
                                    text = if (tank.isManualKg) {
                                        stringResource(R.string.tank_manual_meta)
                                    } else {
                                        stringResource(
                                            R.string.tank_meta_format,
                                            SoundingFormatters.formatCompact(tank.tableLevel),
                                            SoundingFormatters.formatCompact(tank.tRef),
                                            tank.capacityTon
                                        )
                                    },
                                    style = MonoTypography.badge,
                                    color = if (tank.isManualKg) {
                                        extras.leafAccentReadable
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = extras.leafAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
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

    val filtered = remember(products, favoriteProducts, query) {
        val q = query.trim().lowercase()
        val matching = if (q.isEmpty()) products else products.filter { it.lowercase().contains(q) }
        matching.sortedByDescending { it in favoriteProducts }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
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
                    style = MaterialTheme.typography.titleLarge,
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
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
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
                shape = MaterialTheme.shapes.medium
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
                    val extras = SoundingThemeExtras.colors

                    Surface(
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .clickable {
                                onSelect(product)
                                onDismiss()
                            }
                            .testTag("product_option_$index"),
                        shape = MaterialTheme.shapes.medium,
                        color = when {
                            isSelected -> extras.leafContainer
                            isFavorite -> extras.leafContainer.copy(alpha = 0.50f)
                            else -> extras.elevatedSurface
                        },
                        border = BorderStroke(
                            width = if (isSelected || isFavorite) 1.5.dp else 1.dp,
                            color = when {
                                isSelected -> extras.leafAccent
                                isFavorite -> extras.leafAccent.copy(alpha = 0.65f)
                                else -> extras.cardBorder
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
                                        tint = extras.leafAccent,
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
                                            extras.leafAccent
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                        },
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isFavorite) {
                                                extras.leafAccent
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
                                                MaterialTheme.colorScheme.onSecondary
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
