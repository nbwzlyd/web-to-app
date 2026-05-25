package com.webtoapp.ui.components

import android.graphics.BitmapFactory
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.io.File

private fun parseStatusBarComposeColor(backgroundColor: String?, overlayAlpha: Float): Color? {
    if (backgroundColor.isNullOrBlank()) return null
    val trimmed = backgroundColor.trim()
    if (trimmed.equals("transparent", ignoreCase = true)) return Color.Transparent
    return try {
        val argb = android.graphics.Color.parseColor(trimmed)
        if (android.graphics.Color.alpha(argb) == 0) return Color.Transparent
        val base = Color(argb)
        // 8 位 #AARRGGBB 已含透明度，不再叠加 overlayAlpha，避免 #00000000 变成不透明黑
        if (trimmed.removePrefix("#").length == 8) base else base.copy(alpha = overlayAlpha)
    } catch (_: Exception) {
        null
    }
}







@Composable
fun StatusBarBackground(
    backgroundType: String,
    backgroundColor: String?,
    backgroundImagePath: String?,
    alpha: Float,
    heightDp: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val topInsetPx = WindowInsets.statusBars.getTop(density)
    val systemStatusBarHeight = if (topInsetPx > 0) {
        with(density) { topInsetPx.toDp() }
    } else {
        24.dp
    }


    val actualHeight = if (heightDp > 0) heightDp.dp else systemStatusBarHeight


    val imageBitmap = remember(backgroundImagePath) {
        if (backgroundType == "IMAGE" && !backgroundImagePath.isNullOrEmpty()) {
            try {

                val file = File(backgroundImagePath)
                if (file.exists()) {
                    BitmapFactory.decodeFile(backgroundImagePath)?.asImageBitmap()
                } else {

                    val assetPath = backgroundImagePath.removePrefix("assets/")
                    context.assets.open(assetPath).use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("StatusBarBackground", "加载状态栏背景图片失败: $backgroundImagePath", e)
                null
            }
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(actualHeight)
            .statusBarsPadding()
    ) {
        when {
            backgroundType == "IMAGE" && imageBitmap != null -> {

                Image(
                    bitmap = imageBitmap,
                    contentDescription = Strings.statusBarBackground,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 1f - alpha)),
                    contentScale = ContentScale.Crop,
                    alpha = alpha
                )
            }
            else -> {
                val bgColor = parseStatusBarComposeColor(backgroundColor, alpha)
                if (bgColor != null && bgColor != Color.Transparent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(bgColor)
                    )
                }
            }
        }
    }
}









@Composable
fun StatusBarOverlay(
    show: Boolean,
    backgroundType: String,
    backgroundColor: String?,
    backgroundImagePath: String?,
    alpha: Float,
    heightDp: Int,
    modifier: Modifier = Modifier
) {
    if (!show) return

    val context = LocalContext.current
    val density = LocalDensity.current

    val topInsetPx = WindowInsets.statusBars.getTop(density)
    val systemStatusBarHeight = if (topInsetPx > 0) {
        with(density) { topInsetPx.toDp() }
    } else {
        24.dp
    }


    val actualHeight = if (heightDp > 0) heightDp.dp else systemStatusBarHeight


    val imageBitmap = remember(backgroundImagePath) {
        if (backgroundType == "IMAGE" && !backgroundImagePath.isNullOrEmpty()) {
            try {

                val file = File(backgroundImagePath)
                if (file.exists()) {
                    BitmapFactory.decodeFile(backgroundImagePath)?.asImageBitmap()
                } else {

                    val assetPath = backgroundImagePath.removePrefix("assets/")
                    context.assets.open(assetPath).use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("StatusBarOverlay", "加载状态栏背景图片失败: $backgroundImagePath", e)
                null
            }
        } else {
            null
        }
    }


    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(actualHeight)
    ) {
        when {
            backgroundType == "IMAGE" && imageBitmap != null -> {

                Image(
                    bitmap = imageBitmap,
                    contentDescription = Strings.statusBarBackground,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = alpha
                )
            }
            else -> {
                val bgColor = parseStatusBarComposeColor(backgroundColor, alpha)
                if (bgColor != null && bgColor != Color.Transparent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(bgColor)
                    )
                }
            }
        }
    }
}
