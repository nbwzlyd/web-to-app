package com.webtoapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.webtoapp.ui.design.WtaMotion

/**
 * 页面顶部细进度条（Safari / Chrome 风格），用于 WebView 加载反馈。
 * 需置于 WebView 之上（zIndex），否则会被 WebView 遮挡。
 */
@Composable
fun WebViewLoadingBar(
    visible: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (visible) progress.coerceIn(0f, 1f) else 1f,
        animationSpec = WtaMotion.settleSpring(),
        label = "webviewProgress"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = WtaMotion.exitTween(durationMillis = WtaMotion.DurationMedium),
        label = "webviewProgressAlpha"
    )
    if (alpha <= 0f) return

    val barColor = Color(0xFF64B5F6)
    Canvas(
        modifier = modifier
            .zIndex(100f)
            .height(3.dp)
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
    ) {
        drawRect(
            color = barColor,
            size = Size(size.width * animatedProgress, size.height)
        )
    }
}
