package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SplashPureWhite = Color(0xFFFFFFFF)
private val SplashDeepTealText = Color(0xFF0A3832)
private val SplashMutedSlateText = Color(0xFF5A6B67)

/**
 * Full-bleed pure white (#FFFFFF) opening splash screen for E - TXT.
 * Displays the woven E - TXT emblem seamlessly on white with a crisp fade + pop entrance
 * (~600ms) and developer/deploy attribution pinned cleanly at the bottom, then hands off
 * smoothly after ~1.35s without delaying background auto-login or Firebase initialization.
 */
@Composable
fun AppSplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.84f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(14f) }

    LaunchedEffect(Unit) {
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 560, easing = FastOutSlowInEasing)
            )
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.74f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            delay(180L)
            launch {
                textAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 440, easing = FastOutSlowInEasing)
                )
            }
            launch {
                textOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 440, easing = FastOutSlowInEasing)
                )
            }
        }
        delay(1350L)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashPureWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp)
            .testTag("app_splash_screen")
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_etxt_foreground),
                contentDescription = stringResource(R.string.cd_splash_logo),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(188.dp)
                    .graphicsLayer {
                        alpha = logoAlpha.value
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    }
                    .testTag("splash_logo_image")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.app_header_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    letterSpacing = 2.2.sp
                ),
                color = SplashDeepTealText,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = textAlpha.value
                    translationY = textOffsetY.value
                }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .graphicsLayer {
                    alpha = textAlpha.value
                    translationY = textOffsetY.value * 0.5f
                }
                .testTag("splash_deploy_footer"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.splash_deployed_label),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.4.sp
                ),
                color = SplashMutedSlateText,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.splash_deployed_author),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                ),
                color = SplashDeepTealText,
                textAlign = TextAlign.Center
            )
        }
    }
}
