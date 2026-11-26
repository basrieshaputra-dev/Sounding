package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Logout
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.LicenseManager
import com.example.data.local.LicenseState
import com.example.ui.theme.BrandDeepTeal
import com.example.ui.theme.BrandLeafGreen
import com.example.ui.theme.MonoTypography
import com.example.ui.theme.SoundingThemeExtras
import com.example.util.SoundingFormatters

@Composable
fun AboutScreen(
    licenseState: LicenseState = LicenseState(),
    userName: String = "",
    userEmail: String = "",
    isOnlineRealtime: Boolean = false,
    onOpenLicenseActivation: () -> Unit = {},
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSignOutDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("about_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "about_hero") {
                AboutHeroIdentityCard(licenseState = licenseState)
            }
            if (userEmail.isNotBlank() || userName.isNotBlank()) {
                item(key = "about_account") {
                    AboutAccountCard(
                        userName = userName,
                        userEmail = userEmail,
                        isOnlineRealtime = isOnlineRealtime,
                        onRequestSignOut = { showSignOutDialog = true }
                    )
                }
            }
            item(key = "about_license") {
                AboutLicenseStatusCard(
                    licenseState = licenseState,
                    onOpenLicenseActivation = onOpenLicenseActivation
                )
            }
            item(key = "about_summary") {
                AboutFunctionSummaryCard()
            }
            item(key = "about_description") {
                AboutNarrativeCard()
            }
            item(key = "about_developer") {
                AboutDeveloperCard(licenseState = licenseState)
            }
            item(key = "about_bottom_spacer") {
                Spacer(modifier = Modifier.height(8.dp))
            }
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
                        onSignOut()
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
}

@Composable
private fun AboutAccountCard(
    userName: String,
    userEmail: String,
    isOnlineRealtime: Boolean,
    onRequestSignOut: () -> Unit
) {
    val extras = SoundingThemeExtras.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("about_account_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionTitleRow(
                icon = if (isOnlineRealtime) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                title = stringResource(R.string.about_section_account),
                accentColor = if (isOnlineRealtime) extras.leafAccentReadable else extras.interactiveAccent
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = extras.elevatedSurface,
                border = BorderStroke(1.dp, extras.cardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = stringResource(R.string.about_account_signed_in),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (userName.isNotBlank()) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (userEmail.isNotBlank()) {
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            if (isOnlineRealtime) R.string.status_online_realtime else R.string.status_offline_local
                        ),
                        style = MonoTypography.badge,
                        color = if (isOnlineRealtime) extras.leafAccentReadable else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedButton(
                onClick = onRequestSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_sign_out_button"),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.about_action_sign_out),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun AboutHeroIdentityCard(
    licenseState: LicenseState
) {
    val heroBrush = Brush.linearGradient(
        colors = listOf(BrandDeepTeal, Color(0xFF1A6457), BrandLeafGreen)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("about_hero_card"),
        shape = MaterialTheme.shapes.large,
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
            // Pure-white elevated container for corporate logo
            Surface(
                modifier = Modifier
                    .size(84.dp)
                    .testTag("about_company_logo"),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.85f))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_company_logo),
                    contentDescription = stringResource(R.string.cd_company_logo),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(11.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_header_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 21.sp,
                        lineHeight = 26.sp
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
                    color = Color.White.copy(alpha = 0.94f),
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

            // Operational trial classification strip
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
                        text = if (licenseState.isActivated) {
                            stringResource(R.string.about_caption_licensed)
                        } else {
                            stringResource(
                                R.string.about_caption_trial_days,
                                licenseState.remainingTrialDays
                            )
                        },
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
    licenseState: LicenseState,
    onOpenLicenseActivation: () -> Unit
) {
    val extras = SoundingThemeExtras.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("about_license_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (licenseState.isActivated) extras.leafAccent.copy(alpha = 0.45f) else extras.cardBorder
        ),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitleRow(
                icon = if (licenseState.isActivated) Icons.Filled.Verified else Icons.Filled.VpnKey,
                title = stringResource(R.string.about_section_license),
                accentColor = if (licenseState.isActivated) extras.leafAccentReadable else extras.interactiveAccent
            )

            if (licenseState.isActivated) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = extras.leafContainer,
                    border = BorderStroke(1.dp, extras.leafAccent.copy(alpha = 0.40f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = null,
                                tint = extras.onLeafContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.about_license_active_badge),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = extras.onLeafContainer
                            )
                        }
                        Text(
                            text = stringResource(R.string.about_license_active_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (licenseState.activatedCode.isNotEmpty()) {
                            HorizontalDivider(color = extras.leafAccent.copy(alpha = 0.25f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.about_license_code_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = licenseState.activatedCode,
                                    style = MonoTypography.subMetric.copy(fontWeight = FontWeight.Bold),
                                    color = extras.onLeafContainer
                                )
                            }
                        }
                        if (licenseState.activatedAtEpochMs > 0L) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.about_license_date_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = SoundingFormatters.formatDisplayDateTime(licenseState.activatedAtEpochMs),
                                    style = MonoTypography.badge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            } else {
                val progress = (
                    licenseState.remainingTrialDays.toFloat() /
                        LicenseManager.TRIAL_DURATION_DAYS.toFloat()
                    ).coerceIn(0f, 1f)

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = extras.elevatedSurface,
                    border = BorderStroke(1.dp, extras.cardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.about_license_trial_badge,
                                licenseState.remainingTrialDays
                            ),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = extras.leafAccent,
                            trackColor = extras.cardBorder
                        )
                        if (licenseState.trialEndEpochMs > 0L) {
                            Text(
                                text = stringResource(
                                    R.string.about_license_trial_until,
                                    SoundingFormatters.formatLocalDate(licenseState.trialEndEpochMs)
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Button(
                    onClick = onOpenLicenseActivation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("about_open_activation_button"),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandDeepTeal,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.VpnKey,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.about_license_action_enter_code),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
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
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        tonalElevation = 1.dp
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
        shape = MaterialTheme.shapes.medium,
        color = extras.elevatedSurface,
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
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        tonalElevation = 1.dp
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
    licenseState: LicenseState
) {
    val extras = SoundingThemeExtras.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("about_developer_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        tonalElevation = 1.dp
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
                shape = MaterialTheme.shapes.medium,
                color = extras.elevatedSurface,
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
                                    text = if (licenseState.isActivated) {
                                        stringResource(R.string.about_dev_value_status_licensed)
                                    } else {
                                        stringResource(
                                            R.string.about_dev_value_status_trial,
                                            licenseState.remainingTrialDays
                                        )
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = extras.onLeafContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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
                text = value,
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
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
