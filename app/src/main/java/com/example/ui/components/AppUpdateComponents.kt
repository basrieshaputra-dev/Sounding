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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.AppUpdateStatus
import com.example.ui.AppUpdateUiState
import com.example.ui.theme.MonoTypography
import com.example.ui.theme.SaveGradientGreen
import com.example.ui.theme.SoundingThemeExtras
import com.example.util.AppUpdateChecker
import com.example.util.GitHubReleaseInfo

@Composable
fun UpdateAvailableBanner(
    currentVersionLabel: String,
    release: GitHubReleaseInfo,
    onOpenDetails: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extras = SoundingThemeExtras.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenDetails)
            .testTag("update_available_banner"),
        shape = RoundedCornerShape(16.dp),
        color = extras.resultSurface,
        border = BorderStroke(1.2.dp, extras.leafAccent),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(extras.leafAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.update_banner_title, release.displayVersion),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.update_banner_subtitle, currentVersionLabel),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = extras.leafAccent,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onOpenDetails)
                    .testTag("update_banner_action_button")
            ) {
                Text(
                    text = stringResource(R.string.update_banner_action_view),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("update_banner_dismiss_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_dismiss_update_banner),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun UpdateAvailableDialog(
    currentVersionLabel: String,
    release: GitHubReleaseInfo,
    onDownloadApk: (GitHubReleaseInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val extras = SoundingThemeExtras.colors
    val notesText = release.body.trim().ifEmpty {
        stringResource(R.string.update_release_notes_fallback)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(extras.leafContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.NewReleases,
                    contentDescription = null,
                    tint = extras.leafAccentReadable,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.update_dialog_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = extras.leafContainer,
                    border = BorderStroke(1.dp, extras.leafAccent.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = stringResource(
                            R.string.update_dialog_version_transition,
                            currentVersionLabel,
                            release.displayVersion
                        ),
                        style = MonoTypography.badge.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = extras.onLeafContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.update_dialog_release_notes_label),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = extras.elevatedSurface,
                    border = BorderStroke(1.dp, extras.cardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = notesText,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("update_dialog_release_notes")
                        )
                    }
                }
            }
        },
        confirmButton = {
            val pillShape = RoundedCornerShape(999.dp)
            val ctaGradient = Brush.horizontalGradient(
                colors = listOf(SaveGradientGreen, extras.limeAccent, extras.primaryCtaEnd)
            )
            Button(
                onClick = { onDownloadApk(release) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(pillShape)
                    .background(ctaGradient, pillShape)
                    .testTag("update_dialog_download_button"),
                shape = pillShape,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.update_action_download_apk),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("update_dialog_dismiss_button"),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = stringResource(R.string.update_action_later),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    )
}

@Composable
fun AboutAppUpdateCard(
    updateState: AppUpdateUiState,
    installedVersionLabel: String,
    onCheckForUpdate: () -> Unit,
    onDownloadUpdate: (GitHubReleaseInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val extras = SoundingThemeExtras.colors
    val release = updateState.availableRelease

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("about_update_card"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (updateState.hasUpdate) extras.leafAccent else extras.cardBorder
        ),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        .background(if (updateState.hasUpdate) extras.leafAccentReadable else extras.interactiveAccent)
                )
                Icon(
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = if (updateState.hasUpdate) extras.leafAccentReadable else extras.interactiveAccent,
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    text = stringResource(R.string.about_section_update),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Version & Repository info box
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.about_update_installed_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = installedVersionLabel.ifBlank { updateState.currentVersionLabel },
                            style = MonoTypography.badge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(color = extras.cardBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.about_update_repo_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${AppUpdateChecker.GITHUB_OWNER}/${AppUpdateChecker.GITHUB_REPO}",
                            style = MonoTypography.badge,
                            color = extras.interactiveAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Live status strip
            val statusText = when (updateState.status) {
                AppUpdateStatus.IDLE -> stringResource(R.string.about_update_status_idle)
                AppUpdateStatus.CHECKING -> stringResource(R.string.about_update_status_checking)
                AppUpdateStatus.UP_TO_DATE -> stringResource(
                    R.string.about_update_status_up_to_date,
                    updateState.latestTagSeen.ifBlank { updateState.currentVersionLabel }
                )
                AppUpdateStatus.UPDATE_AVAILABLE -> stringResource(
                    R.string.about_update_status_available,
                    release?.displayVersion ?: "-"
                )
                AppUpdateStatus.OFFLINE_OR_TIMEOUT -> stringResource(R.string.about_update_status_offline)
                AppUpdateStatus.RATE_LIMITED -> stringResource(R.string.about_update_status_rate_limited)
                AppUpdateStatus.NO_RELEASES_YET -> stringResource(R.string.about_update_status_no_release)
                AppUpdateStatus.ERROR -> stringResource(R.string.about_update_status_error)
            }

            val statusContainerColor = when (updateState.status) {
                AppUpdateStatus.UPDATE_AVAILABLE, AppUpdateStatus.UP_TO_DATE -> extras.leafContainer
                AppUpdateStatus.OFFLINE_OR_TIMEOUT, AppUpdateStatus.RATE_LIMITED, AppUpdateStatus.ERROR -> extras.warningSurface
                else -> extras.elevatedSurface
            }
            val statusBorderColor = when (updateState.status) {
                AppUpdateStatus.UPDATE_AVAILABLE, AppUpdateStatus.UP_TO_DATE -> extras.leafAccent.copy(alpha = 0.45f)
                AppUpdateStatus.OFFLINE_OR_TIMEOUT, AppUpdateStatus.RATE_LIMITED, AppUpdateStatus.ERROR -> extras.warningBorder
                else -> extras.cardBorder
            }
            val statusTextColor = when (updateState.status) {
                AppUpdateStatus.UPDATE_AVAILABLE, AppUpdateStatus.UP_TO_DATE -> extras.onLeafContainer
                AppUpdateStatus.OFFLINE_OR_TIMEOUT, AppUpdateStatus.RATE_LIMITED, AppUpdateStatus.ERROR -> extras.warningText
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_update_status_box"),
                shape = MaterialTheme.shapes.medium,
                color = statusContainerColor,
                border = BorderStroke(1.dp, statusBorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusIcon = when (updateState.status) {
                        AppUpdateStatus.UPDATE_AVAILABLE -> Icons.Filled.NewReleases
                        AppUpdateStatus.UP_TO_DATE -> Icons.Filled.CheckCircle
                        AppUpdateStatus.OFFLINE_OR_TIMEOUT, AppUpdateStatus.RATE_LIMITED, AppUpdateStatus.ERROR -> Icons.Filled.CloudOff
                        else -> Icons.Filled.SystemUpdate
                    }
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        ),
                        color = statusTextColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (release != null && updateState.hasUpdate) {
                val pillShape = RoundedCornerShape(999.dp)
                val downloadGradient = Brush.horizontalGradient(
                    colors = listOf(SaveGradientGreen, extras.limeAccent, extras.primaryCtaEnd)
                )
                Button(
                    onClick = { onDownloadUpdate(release) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(pillShape)
                        .background(downloadGradient, pillShape)
                        .testTag("about_download_update_button"),
                    shape = pillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.update_action_download_apk),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            OutlinedButton(
                onClick = onCheckForUpdate,
                enabled = !updateState.isChecking,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("about_check_update_button"),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, extras.interactiveAccent),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = extras.interactiveAccent
                )
            ) {
                if (updateState.isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = extras.interactiveAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.update_action_checking),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.update_action_check_now),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
