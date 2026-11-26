package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.LicenseActivationResult
import com.example.ui.theme.BrandDeepTeal
import com.example.ui.theme.BrandLeafGreen
import com.example.ui.theme.MonoTypography
import com.example.ui.theme.SoundingThemeExtras
import java.util.Locale

@Composable
fun LicenseExpiredScreen(
    onActivateLicense: (String) -> LicenseActivationResult,
    onActivatedSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val extras = SoundingThemeExtras.colors
    val context = LocalContext.current
    var codeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun attemptActivation() {
        when (val result = onActivateLicense(codeInput)) {
            is LicenseActivationResult.Success -> {
                errorMessage = null
                onActivatedSuccess(result.code)
            }
            is LicenseActivationResult.AlreadyActivated -> {
                errorMessage = null
                onActivatedSuccess(result.activeCode)
            }
            LicenseActivationResult.EmptyCode -> {
                errorMessage = context.getString(R.string.license_error_empty)
            }
            is LicenseActivationResult.AlreadyUsed -> {
                errorMessage = context.getString(R.string.license_error_used, result.code)
            }
            is LicenseActivationResult.InvalidCode -> {
                errorMessage = context.getString(R.string.license_error_invalid)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .testTag("license_expired_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Corporate Branded Hero Banner
            val heroBrush = Brush.linearGradient(
                colors = listOf(BrandDeepTeal, Color(0xFF1A6457), BrandLeafGreen)
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
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
                                radius = size.minDimension * 0.56f,
                                center = Offset(size.width * 0.90f, size.height * 0.14f)
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.06f),
                                radius = size.minDimension * 0.42f,
                                center = Offset(size.width * 0.10f, size.height * 0.90f)
                            )
                        }
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        shadowElevation = 5.dp
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_company_logo),
                            contentDescription = stringResource(R.string.cd_company_logo),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(9.dp)
                        )
                    }

                    Text(
                        text = stringResource(R.string.app_header_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = stringResource(R.string.app_header_subtitle),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        ),
                        color = Color.White.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Black.copy(alpha = 0.24f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.34f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LockClock,
                                contentDescription = null,
                                tint = Color(0xFFFDE047),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.license_expired_badge),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Activation Form Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, extras.cardBorder),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.license_expired_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.license_expired_desc),
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LicenseActivationInputBlock(
                        codeInput = codeInput,
                        onCodeChange = { next ->
                            codeInput = next.uppercase(Locale.US)
                            errorMessage = null
                        },
                        errorMessage = errorMessage,
                        onSubmit = { attemptActivation() }
                    )
                }
            }

            // Support Contact Card
            LicenseContactSupportCard()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseActivationBottomSheet(
    onActivateLicense: (String) -> LicenseActivationResult,
    onActivatedSuccess: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var codeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun attemptActivation() {
        when (val result = onActivateLicense(codeInput)) {
            is LicenseActivationResult.Success -> {
                errorMessage = null
                onActivatedSuccess(result.code)
            }
            is LicenseActivationResult.AlreadyActivated -> {
                errorMessage = null
                onActivatedSuccess(result.activeCode)
            }
            LicenseActivationResult.EmptyCode -> {
                errorMessage = context.getString(R.string.license_error_empty)
            }
            is LicenseActivationResult.AlreadyUsed -> {
                errorMessage = context.getString(R.string.license_error_used, result.code)
            }
            is LicenseActivationResult.InvalidCode -> {
                errorMessage = context.getString(R.string.license_error_invalid)
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
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.license_sheet_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_license_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_close)
                    )
                }
            }

            Text(
                text = stringResource(R.string.license_sheet_subtitle),
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LicenseActivationInputBlock(
                codeInput = codeInput,
                onCodeChange = { next ->
                    codeInput = next.uppercase(Locale.US)
                    errorMessage = null
                },
                errorMessage = errorMessage,
                onSubmit = { attemptActivation() }
            )

            LicenseContactSupportCard()
        }
    }
}

@Composable
private fun LicenseActivationInputBlock(
    codeInput: String,
    onCodeChange: (String) -> Unit,
    errorMessage: String?,
    onSubmit: () -> Unit
) {
    val extras = SoundingThemeExtras.colors

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = codeInput,
            onValueChange = { raw ->
                val filtered = raw.filter { it.isLetterOrDigit() || it == '-' }.take(12)
                onCodeChange(filtered)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("license_code_input"),
            singleLine = true,
            label = { Text(stringResource(R.string.license_input_label)) },
            placeholder = { Text(stringResource(R.string.license_input_placeholder)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.VpnKey,
                    contentDescription = null,
                    tint = extras.interactiveAccent
                )
            },
            isError = errorMessage != null,
            textStyle = MonoTypography.subMetric.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSubmit() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = extras.leafAccent,
                focusedLabelColor = extras.leafAccentReadable,
                cursorColor = extras.leafAccentReadable,
                focusedContainerColor = extras.elevatedSurface,
                unfocusedContainerColor = extras.elevatedSurface
            ),
            shape = MaterialTheme.shapes.medium
        )

        Text(
            text = stringResource(R.string.license_single_use_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        AnimatedVisibility(visible = errorMessage != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("license_error_banner"),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(18.dp)
                    )
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("activate_license_button"),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandLeafGreen,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Filled.LockOpen,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.license_action_activate),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun LicenseContactSupportCard() {
    val extras = SoundingThemeExtras.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("license_contact_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.license_contact_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = extras.interactiveAccent
            )
            Text(
                text = stringResource(R.string.license_contact_desc),
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(color = extras.cardBorder)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = extras.leafAccentReadable,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.about_dev_value_name),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = extras.leafContainer,
                    border = BorderStroke(1.dp, extras.leafAccent.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Badge,
                            contentDescription = null,
                            tint = extras.onLeafContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = stringResource(R.string.license_contact_id_badge),
                            style = MonoTypography.badge,
                            color = extras.onLeafContainer
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.about_dev_value_unit),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
