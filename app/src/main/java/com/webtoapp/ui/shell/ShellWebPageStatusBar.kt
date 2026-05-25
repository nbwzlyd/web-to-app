package com.webtoapp.ui.shell

import android.webkit.WebView
import com.webtoapp.core.shell.ShellConfig
import com.webtoapp.core.webview.ColorThemeBridge
import com.webtoapp.data.model.StatusBarColorMode
import com.webtoapp.data.model.WebViewConfig

/**
 * 仅在「全屏 + 显示状态栏 + 跟随网页(WEB_PAGE)」时同步网页导航栏颜色。
 * CUSTOM / THEME / TRANSPARENT 等模式不受影响。
 */
fun resolveStatusBarColorMode(
    lightMode: String,
    darkMode: String,
    isDarkTheme: Boolean
): String = if (isDarkTheme) darkMode else lightMode

fun shouldApplyWebPageStatusBarColor(
    hideToolbar: Boolean,
    showStatusBarInFullscreen: Boolean,
    colorMode: String
): Boolean = hideToolbar && showStatusBarInFullscreen && colorMode == StatusBarColorMode.WEB_PAGE.name

fun shouldInstallWebPageColorBridge(
    hideToolbar: Boolean,
    showStatusBarInFullscreen: Boolean,
    lightMode: String,
    darkMode: String
): Boolean = hideToolbar && showStatusBarInFullscreen &&
    (lightMode == StatusBarColorMode.WEB_PAGE.name || darkMode == StatusBarColorMode.WEB_PAGE.name)

fun ShellConfig.shouldApplyWebPageStatusBarColor(isDarkTheme: Boolean): Boolean =
    shouldApplyWebPageStatusBarColor(
        webViewConfig.hideToolbar,
        webViewConfig.showStatusBarInFullscreen,
        resolveStatusBarColorMode(
            webViewConfig.statusBarColorMode,
            webViewConfig.statusBarColorModeDark,
            isDarkTheme
        )
    )

fun ShellConfig.shouldInstallWebPageColorBridge(): Boolean =
    shouldInstallWebPageColorBridge(
        webViewConfig.hideToolbar,
        webViewConfig.showStatusBarInFullscreen,
        webViewConfig.statusBarColorMode,
        webViewConfig.statusBarColorModeDark
    )

fun WebViewConfig.shouldApplyWebPageStatusBarColor(isDarkTheme: Boolean): Boolean =
    shouldApplyWebPageStatusBarColor(
        hideToolbar,
        showStatusBarInFullscreen,
        resolveStatusBarColorMode(
            statusBarColorMode.name,
            statusBarColorModeDark.name,
            isDarkTheme
        )
    )

fun WebViewConfig.shouldInstallWebPageColorBridge(): Boolean =
    shouldInstallWebPageColorBridge(
        hideToolbar,
        showStatusBarInFullscreen,
        statusBarColorMode.name,
        statusBarColorModeDark.name
    )

fun startWebPageStatusBarColorTracking(view: WebView, bridge: ColorThemeBridge, url: String?) {
    ColorThemeBridge.scheduleColorExtract(view)
}
