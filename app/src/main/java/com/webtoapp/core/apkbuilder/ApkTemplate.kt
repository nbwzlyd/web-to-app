 package com.webtoapp.core.apkbuilder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.webtoapp.core.forcedrun.ForcedRunConfig
import com.webtoapp.core.shell.BgmShellItem
import com.webtoapp.core.shell.LrcShellTheme
import java.io.*
import java.util.zip.*





class ApkTemplate(private val context: Context) {

    companion object {

        private const val TEMPLATE_APK = "template/webview_shell.apk"


        const val CONFIG_PATH = "assets/app_config.json"


        val ICON_PATHS = listOf(
            "res/mipmap-mdpi-v4/ic_launcher.png" to 48,
            "res/mipmap-hdpi-v4/ic_launcher.png" to 72,
            "res/mipmap-xhdpi-v4/ic_launcher.png" to 96,
            "res/mipmap-xxhdpi-v4/ic_launcher.png" to 144,
            "res/mipmap-xxxhdpi-v4/ic_launcher.png" to 192
        )


        val ROUND_ICON_PATHS = listOf(
            "res/mipmap-mdpi-v4/ic_launcher_round.png" to 48,
            "res/mipmap-hdpi-v4/ic_launcher_round.png" to 72,
            "res/mipmap-xhdpi-v4/ic_launcher_round.png" to 96,
            "res/mipmap-xxhdpi-v4/ic_launcher_round.png" to 144,
            "res/mipmap-xxxhdpi-v4/ic_launcher_round.png" to 192
        )
    }

    private val templateDir = File(context.cacheDir, "apk_templates")

    init {
        templateDir.mkdirs()
    }





    fun getTemplateApk(): File? {
        val templateFile = File(templateDir, "webview_shell.apk")


        if (templateFile.exists()) {
            return templateFile
        }


        return try {
            context.assets.open(TEMPLATE_APK).use { input ->
                FileOutputStream(templateFile).use { output ->
                    input.copyTo(output)
                }
            }
            templateFile
        } catch (e: Exception) {

            null
        }
    }




    fun hasTemplate(): Boolean {
        return try {
            context.assets.open(TEMPLATE_APK).close()
            true
        } catch (e: Exception) {
            false
        }
    }




    fun createConfigJson(config: ApkConfig): String =
        ApkConfigJsonFactory.create(config)

    fun createEncryptedStubJson(config: ApkConfig): String =
        ApkConfigJsonFactory.createEncryptedStub(config)

    fun scaleBitmapToPng(bitmap: Bitmap, size: Int): ByteArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.PNG, 100, baos)
        if (scaled != bitmap) {
            scaled.recycle()
        }
        return baos.toByteArray()
    }




    fun loadBitmap(iconPath: String): Bitmap? {
        return try {
            if (iconPath.startsWith("/")) {
                BitmapFactory.decodeFile(iconPath)
            } else if (iconPath.startsWith("content://")) {
                context.contentResolver.openInputStream(android.net.Uri.parse(iconPath))?.use {
                    BitmapFactory.decodeStream(it)
                }
            } else {
                BitmapFactory.decodeFile(iconPath)
            }
        } catch (e: Exception) {
            null
        }
    }












    fun createAdaptiveForegroundIcon(bitmap: Bitmap, size: Int): ByteArray {

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)


        val safeZoneSize = (size * 72f / 108f).toInt()
        val padding = (size - safeZoneSize) / 2


        val scaled = Bitmap.createScaledBitmap(bitmap, safeZoneSize, safeZoneSize, true)


        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        canvas.drawBitmap(scaled, padding.toFloat(), padding.toFloat(), paint)

        val baos = ByteArrayOutputStream()
        output.compress(Bitmap.CompressFormat.PNG, 100, baos)

        if (scaled != bitmap) scaled.recycle()
        output.recycle()

        return baos.toByteArray()
    }




    fun createRoundIcon(bitmap: Bitmap, size: Int): ByteArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val canvas = android.graphics.Canvas(output)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }


        val rect = android.graphics.RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawOval(rect, paint)


        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, 0f, 0f, paint)

        val baos = ByteArrayOutputStream()
        output.compress(Bitmap.CompressFormat.PNG, 100, baos)

        if (scaled != bitmap) scaled.recycle()
        output.recycle()

        return baos.toByteArray()
    }




    fun clearCache() {
        templateDir.listFiles()?.forEach { it.delete() }
    }
}


internal object ApkConfigJsonFactory {
    const val SCHEMA_VERSION = 1

    private val configGson: com.google.gson.Gson by lazy {
        com.google.gson.GsonBuilder()
            .enableComplexMapKeySerialization()
            .serializeNulls()
            .create()
    }

    fun create(config: ApkConfig, gson: com.google.gson.Gson = configGson): String {
        ApkConfigValidator.requireValid(config)
        return gson.toJson(config.toShellPayload())
    }

    fun createEncryptedStub(config: ApkConfig, gson: com.google.gson.Gson = configGson): String =
        gson.toJson(config.toEncryptedStubPayload())

    internal fun ApkConfig.toShellPayload(): Map<String, Any?> = linkedMapOf(
        "schemaVersion" to SCHEMA_VERSION,
        "appName" to appName,
        "packageName" to packageName,
        "targetUrl" to targetUrl,
        "versionCode" to versionCode,
        "versionName" to versionName,
        "activationEnabled" to activationEnabled,
        "activationCodes" to activationCodes,
        "activationRequireEveryTime" to activationRequireEveryTime,
        "activationDialogTitle" to activationDialogTitle,
        "activationDialogSubtitle" to activationDialogSubtitle,
        "activationDialogInputLabel" to activationDialogInputLabel,
        "activationDialogButtonText" to activationDialogButtonText,
        "adBlockEnabled" to adBlockEnabled,
        "adBlockRules" to adBlockRules,
        "announcementEnabled" to announcementEnabled,
        "announcementTitle" to announcementTitle,
        "announcementContent" to announcementContent,
        "announcementLink" to announcementLink,
        "announcementLinkText" to announcementLinkText,
        "announcementTemplate" to announcementTemplate,
        "announcementShowEmoji" to announcementShowEmoji,
        "announcementAnimationEnabled" to announcementAnimationEnabled,
        "announcementShowOnce" to announcementShowOnce,
        "announcementRequireConfirmation" to announcementRequireConfirmation,
        "announcementAllowNeverShow" to announcementAllowNeverShow,
        "announcementTriggerOnLaunch" to announcementTriggerOnLaunch,
        "announcementTriggerOnNoNetwork" to announcementTriggerOnNoNetwork,
        "announcementTriggerIntervalMinutes" to announcementTriggerIntervalMinutes,
        "adsEnabled" to adsEnabled,
        "adBannerEnabled" to adBannerEnabled,
        "adBannerId" to adBannerId,
        "adInterstitialEnabled" to adInterstitialEnabled,
        "adInterstitialId" to adInterstitialId,
        "adSplashEnabled" to adSplashEnabled,
        "adSplashId" to adSplashId,
        "splashEnabled" to splashEnabled,
        "splashType" to splashType,
        "splashDuration" to splashDuration,
        "splashClickToSkip" to splashClickToSkip,
        "splashVideoStartMs" to splashVideoStartMs,
        "splashVideoEndMs" to splashVideoEndMs,
        "splashLandscape" to splashLandscape,
        "splashFillScreen" to splashFillScreen,
        "splashEnableAudio" to splashEnableAudio,
        "webViewConfig" to webViewConfigPayload(),
        "appType" to appType,
        "mediaConfig" to mediaConfigPayload(),
        "htmlConfig" to htmlConfigPayload(),
        "galleryConfig" to galleryConfigPayload(),
        "bgmEnabled" to bgmEnabled,
        "bgmPlaylist" to bgmPlaylist,
        "bgmPlayMode" to bgmPlayMode,
        "bgmVolume" to bgmVolume,
        "bgmAutoPlay" to bgmAutoPlay,
        "bgmShowLyrics" to bgmShowLyrics,
        "bgmLrcTheme" to bgmLrcTheme,
        "themeType" to themeType,
        "darkMode" to darkMode,
        "translateEnabled" to translateEnabled,
        "translateTargetLanguage" to translateTargetLanguage,
        "translateShowButton" to translateShowButton,
        "extensionEnabled" to extensionEnabled,
        "extensionFabIcon" to extensionFabIcon,
        "extensionModuleIds" to extensionModuleIds,
        "embeddedExtensionModules" to embeddedExtensionModules.map { it.toPayload() },
        "autoStartConfig" to autoStartConfigPayload(),
        "forcedRunConfig" to forcedRunConfig,
        "isolationEnabled" to isolationEnabled,
        "isolationConfig" to isolationConfigPayload(),
        "backgroundRunEnabled" to backgroundRunEnabled,
        "backgroundRunConfig" to backgroundRunConfigPayload(),
        "notificationEnabled" to notificationEnabled,
        "notificationConfig" to notificationConfigPayload(),
        "blackTechConfig" to blackTechConfig,
        "disguiseConfig" to disguiseConfig,
        "browserDisguiseConfig" to browserDisguiseConfig,
        "deviceDisguiseConfig" to deviceDisguiseConfig,
        "language" to language,
        "engineType" to engineType,
        "networkTrustConfig" to networkTrustConfigPayload(),
        "wordpressConfig" to wordpressConfigPayload(),
        "nodejsConfig" to nodejsConfigPayload(),
        "deepLinkEnabled" to deepLinkEnabled,
        "deepLinkHosts" to deepLinkHosts,
        "phpAppConfig" to phpAppConfigPayload(),
        "pythonAppConfig" to pythonAppConfigPayload(),
        "goAppConfig" to goAppConfigPayload(),
        "multiWebConfig" to multiWebConfigPayload()
    )

    internal fun ApkConfig.toEncryptedStubPayload(): Map<String, Any?> = linkedMapOf(
        "schemaVersion" to SCHEMA_VERSION,
        "appName" to appName,
        "packageName" to packageName,
        "targetUrl" to "",
        "appType" to "",
        "versionCode" to 0,
        "versionName" to "",
        "webViewConfig" to emptyMap<String, Any?>()
    )

    private fun ApkConfig.webViewConfigPayload(): Map<String, Any?> = linkedMapOf(
        "javaScriptEnabled" to javaScriptEnabled,
        "domStorageEnabled" to domStorageEnabled,
        "allowFileAccess" to allowFileAccess,
        "allowContentAccess" to allowContentAccess,
        "cacheEnabled" to cacheEnabled,
        "zoomEnabled" to zoomEnabled,
        "desktopMode" to desktopMode,
        "userAgent" to userAgent,
        "userAgentMode" to userAgentMode,
        "customUserAgent" to customUserAgent,
        "orientationMode" to orientationMode,
        "keyboardAdjustMode" to keyboardAdjustMode,
        "swipeRefreshEnabled" to swipeRefreshEnabled,
        "fullscreenEnabled" to fullscreenEnabled,
        "hideToolbar" to hideToolbar,
        "hideBrowserToolbar" to hideBrowserToolbar,
        "showStatusBarInFullscreen" to showStatusBarInFullscreen,
        "showNavigationBarInFullscreen" to showNavigationBarInFullscreen,
        "showToolbarInFullscreen" to showToolbarInFullscreen,
        "landscapeMode" to landscapeMode,
        "injectScripts" to injectScripts.map { script ->
            linkedMapOf(
                "name" to script.name,
                "code" to script.code,
                "enabled" to script.enabled,
                "runAt" to script.runAt.name
            )
        },
        "statusBarColorMode" to statusBarColorMode,
        "statusBarColor" to statusBarColor,
        "statusBarDarkIcons" to statusBarDarkIcons,
        "statusBarBackgroundType" to statusBarBackgroundType,
        "statusBarBackgroundImage" to if (statusBarBackgroundType == "IMAGE" && !statusBarBackgroundImage.isNullOrEmpty()) {
            "statusbar_background.png"
        } else null,
        "statusBarBackgroundAlpha" to statusBarBackgroundAlpha,
        "statusBarHeightDp" to statusBarHeightDp,
        "statusBarColorModeDark" to statusBarColorModeDark,
        "statusBarColorDark" to statusBarColorDark,
        "statusBarDarkIconsDark" to statusBarDarkIconsDark,
        "statusBarBackgroundTypeDark" to statusBarBackgroundTypeDark,
        "statusBarBackgroundImageDark" to if (statusBarBackgroundTypeDark == "IMAGE" && !statusBarBackgroundImageDark.isNullOrEmpty()) {
            "statusbar_background_dark.png"
        } else null,
        "statusBarBackgroundAlphaDark" to statusBarBackgroundAlphaDark,
        "longPressMenuEnabled" to longPressMenuEnabled,
        "longPressMenuStyle" to longPressMenuStyle,
        "adBlockToggleEnabled" to adBlockToggleEnabled,
        "popupBlockerEnabled" to popupBlockerEnabled,
        "popupBlockerToggleEnabled" to popupBlockerToggleEnabled,
        "openExternalLinks" to openExternalLinks,
        "initialScale" to initialScale,
        "viewportMode" to viewportMode,
        "customViewportWidth" to customViewportWidth,
        "newWindowBehavior" to newWindowBehavior,
        "enablePaymentSchemes" to enablePaymentSchemes,
        "enableShareBridge" to enableShareBridge,
        "enableZoomPolyfill" to enableZoomPolyfill,
        "enableCrossOriginIsolation" to enableCrossOriginIsolation,
        "hideUrlPreview" to hideUrlPreview,
        "disableShields" to disableShields,
        "decodeBase64DeepLinks" to decodeBase64DeepLinks,
        "mediaAutoplayEnabled" to mediaAutoplayEnabled,
        "acceptThirdPartyCookies" to acceptThirdPartyCookies,
        "enableKernelDisguise" to enableKernelDisguise,
        "enableImageRepair" to enableImageRepair,
        "enableScrollMemory" to enableScrollMemory,
        "enableHttpsUpgrade" to enableHttpsUpgrade,
        "enableOAuthExternalRedirect" to enableOAuthExternalRedirect,
        "enableClipboardPolyfill" to enableClipboardPolyfill,
        "enableNotificationPolyfill" to enableNotificationPolyfill,
        "safeBrowsingEnabled" to safeBrowsingEnabled,
        "geolocationEnabled" to geolocationEnabled,
        "enableOrientationPolyfill" to enableOrientationPolyfill,
        "enableCompatPolyfills" to enableCompatPolyfills,
        "enableNativeBridge" to enableNativeBridge,
        "javaScriptCanOpenWindows" to javaScriptCanOpenWindows,
        "databaseEnabled" to databaseEnabled,
        "enableCookiePersistence" to enableCookiePersistence,
        "enablePrivateNetworkBridge" to enablePrivateNetworkBridge,
        "allowMixedContent" to allowMixedContent,
        "enableGpc" to enableGpc,
        "enableCookieConsentBlock" to enableCookieConsentBlock,
        "enableReferrerPolicy" to enableReferrerPolicy,
        "enableTrackerBlocking" to enableTrackerBlocking,
        "enableBlobDownloadInterception" to enableBlobDownloadInterception,
        "keepScreenOn" to keepScreenOn,
        "screenAwakeMode" to screenAwakeMode,
        "screenAwakeTimeoutMinutes" to screenAwakeTimeoutMinutes,
        "screenBrightness" to screenBrightness,
        "performanceOptimization" to performanceOptimization,
        "pwaOfflineEnabled" to pwaOfflineEnabled,
        "pwaOfflineStrategy" to pwaOfflineStrategy,
        "proxyMode" to proxyMode,
        "proxyHost" to proxyHost,
        "proxyPort" to proxyPort,
        "proxyType" to proxyType,
        "pacUrl" to pacUrl,
        "proxyBypassRules" to proxyBypassRules,
        "proxyUsername" to proxyUsername,
        "proxyPassword" to proxyPassword,
        "hostsMappingEnabled" to hostsMappingEnabled,
        "hostsMappings" to hostsMappings.map { entry ->
            linkedMapOf(
                "host" to entry.host,
                "ip" to entry.ip
            )
        },
        "dnsMode" to dnsMode,
        "dnsConfig" to dnsConfig,
        "showFloatingBackButton" to showFloatingBackButton,
        "floatingWindowConfig" to floatingWindowConfigPayload(),
        "errorPageConfig" to errorPageConfigPayload()
    )

    private fun ApkConfig.floatingWindowConfigPayload(): Map<String, Any?> = linkedMapOf(
        "enabled" to floatingWindowEnabled,
        "windowSizePercent" to floatingWindowSizePercent,
        "widthPercent" to floatingWindowWidthPercent,
        "heightPercent" to floatingWindowHeightPercent,
        "lockAspectRatio" to floatingWindowLockAspectRatio,
        "opacity" to floatingWindowOpacity,
        "cornerRadius" to floatingWindowCornerRadius,
        "borderStyle" to floatingWindowBorderStyle,
        "showTitleBar" to floatingWindowShowTitleBar,
        "autoHideTitleBar" to floatingWindowAutoHideTitleBar,
        "startMinimized" to floatingWindowStartMinimized,
        "rememberPosition" to floatingWindowRememberPosition,
        "edgeSnapping" to floatingWindowEdgeSnapping,
        "showResizeHandle" to floatingWindowShowResizeHandle,
        "lockPosition" to floatingWindowLockPosition
    )

    private fun ApkConfig.errorPageConfigPayload(): Map<String, Any?> = linkedMapOf(
        "mode" to errorPageMode,
        "builtInStyle" to errorPageBuiltInStyle,
        "showMiniGame" to errorPageShowMiniGame,
        "miniGameType" to errorPageMiniGameType,
        "autoRetrySeconds" to errorPageAutoRetrySeconds,
        "customHtml" to errorPageCustomHtml,
        "customMediaPath" to errorPageCustomMediaPath,
        "retryButtonText" to errorPageRetryButtonText
    )

    private fun ApkConfig.mediaConfigPayload(): Map<String, Any?> = linkedMapOf(
        "enableAudio" to mediaEnableAudio,
        "loop" to mediaLoop,
        "autoPlay" to mediaAutoPlay,
        "fillScreen" to mediaFillScreen,
        "landscape" to mediaLandscape,
        "keepScreenOn" to mediaKeepScreenOn
    )

    private fun ApkConfig.htmlConfigPayload(): Map<String, Any?> = linkedMapOf(
        "entryFile" to htmlEntryFile,
        "enableJavaScript" to htmlEnableJavaScript,
        "enableLocalStorage" to htmlEnableLocalStorage,
        "landscapeMode" to htmlLandscapeMode
    )

    private fun ApkConfig.galleryConfigPayload(): Map<String, Any?> = linkedMapOf(
        "items" to galleryItems,
        "playMode" to galleryPlayMode,
        "imageInterval" to galleryImageInterval,
        "loop" to galleryLoop,
        "autoPlay" to galleryAutoPlay,
        "backgroundColor" to galleryBackgroundColor,
        "showThumbnailBar" to galleryShowThumbnailBar,
        "showMediaInfo" to galleryShowMediaInfo,
        "orientation" to galleryOrientation,
        "enableAudio" to galleryEnableAudio,
        "videoAutoNext" to galleryVideoAutoNext,
        "shuffleOnLoop" to galleryShuffleOnLoop,
        "defaultView" to galleryDefaultView,
        "gridColumns" to galleryGridColumns,
        "sortOrder" to gallerySortOrder,
        "rememberPosition" to galleryRememberPosition
    )

    private fun ApkConfig.autoStartConfigPayload(): Map<String, Any?>? =
        if (bootStartEnabled || scheduledStartEnabled) {
            linkedMapOf(
                "bootStartEnabled" to bootStartEnabled,
                "scheduledStartEnabled" to scheduledStartEnabled,
                "scheduledTime" to scheduledTime,
                "scheduledDays" to scheduledDays
            )
        } else null

    private fun ApkConfig.isolationConfigPayload(): Map<String, Any?>? {
        val ic = isolationConfig ?: return null
        if (!isolationEnabled) return null
        return linkedMapOf(
            "enabled" to ic.enabled,
            "fingerprintConfig" to linkedMapOf(
                "randomize" to ic.fingerprintConfig.randomize,
                "regenerateOnLaunch" to ic.fingerprintConfig.regenerateOnLaunch,
                "customUserAgent" to ic.fingerprintConfig.customUserAgent,
                "randomUserAgent" to ic.fingerprintConfig.randomUserAgent,
                "fingerprintId" to ic.fingerprintConfig.fingerprintId
            ),
            "headerConfig" to linkedMapOf(
                "enabled" to ic.headerConfig.enabled,
                "randomizeOnRequest" to ic.headerConfig.randomizeOnRequest,
                "dnt" to ic.headerConfig.dnt,
                "spoofClientHints" to ic.headerConfig.spoofClientHints,
                "refererPolicy" to ic.headerConfig.refererPolicy.name
            ),
            "ipSpoofConfig" to linkedMapOf(
                "enabled" to ic.ipSpoofConfig.enabled,
                "spoofMethod" to ic.ipSpoofConfig.spoofMethod.name,
                "customIp" to ic.ipSpoofConfig.customIp,
                "randomIpRange" to ic.ipSpoofConfig.randomIpRange.name,
                "searchKeyword" to ic.ipSpoofConfig.searchKeyword,
                "xForwardedFor" to ic.ipSpoofConfig.xForwardedFor,
                "xRealIp" to ic.ipSpoofConfig.xRealIp,
                "clientIp" to ic.ipSpoofConfig.clientIp
            ),
            "storageIsolation" to ic.storageIsolation,
            "blockWebRTC" to ic.blockWebRTC,
            "protectCanvas" to ic.protectCanvas,
            "protectAudio" to ic.protectAudio,
            "protectWebGL" to ic.protectWebGL,
            "protectFonts" to ic.protectFonts,
            "spoofTimezone" to ic.spoofTimezone,
            "customTimezone" to ic.customTimezone,
            "spoofLanguage" to ic.spoofLanguage,
            "customLanguage" to ic.customLanguage,
            "spoofScreen" to ic.spoofScreen,
            "customScreenWidth" to ic.customScreenWidth,
            "customScreenHeight" to ic.customScreenHeight
        )
    }

    private fun ApkConfig.backgroundRunConfigPayload(): BackgroundRunConfig? =
        backgroundRunConfig.takeIf { backgroundRunEnabled }

    private fun ApkConfig.notificationConfigPayload(): NotificationConfig? =
        notificationConfig.takeIf { notificationEnabled }

    private fun ApkConfig.networkTrustConfigPayload(): Map<String, Any?> = linkedMapOf(
        "trustSystemCa" to networkTrustConfig.trustSystemCa,
        "trustUserCa" to networkTrustConfig.trustUserCa,
        "customCaCertificates" to networkTrustConfig.customCaCertificates.map {
            linkedMapOf(
                "id" to it.id,
                "displayName" to it.displayName,
                "sha256" to it.sha256
            )
        },
        "cleartextTrafficPermitted" to networkTrustConfig.cleartextTrafficPermitted
    )

    private fun ApkConfig.wordpressConfigPayload(): Map<String, Any?> = linkedMapOf(
        "siteTitle" to wordpressSiteTitle,
        "adminUser" to wordpressAdminUser,
        "adminEmail" to wordpressAdminEmail,
        "adminPassword" to wordpressAdminPassword,
        "themeName" to wordpressThemeName,
        "plugins" to wordpressPlugins,
        "activePlugins" to wordpressActivePlugins,
        "permalinkStructure" to wordpressPermalinkStructure,
        "siteLanguage" to wordpressSiteLanguage,
        "autoInstall" to wordpressAutoInstall,
        "phpPort" to wordpressPhpPort,
        "landscapeMode" to wordpressLandscapeMode
    )

    private fun ApkConfig.nodejsConfigPayload(): Map<String, Any?> = linkedMapOf(
        "mode" to nodejsMode,
        "port" to nodejsPort,
        "entryFile" to nodejsEntryFile,
        "envVars" to nodejsEnvVars,
        "landscapeMode" to nodejsLandscapeMode
    )

    private fun ApkConfig.phpAppConfigPayload(): Map<String, Any?> = linkedMapOf(
        "framework" to phpAppFramework,
        "documentRoot" to phpAppDocumentRoot,
        "entryFile" to phpAppEntryFile,
        "port" to phpAppPort,
        "envVars" to phpAppEnvVars,
        "landscapeMode" to phpAppLandscapeMode
    )

    private fun ApkConfig.pythonAppConfigPayload(): Map<String, Any?> = linkedMapOf(
        "framework" to pythonAppFramework,
        "entryFile" to pythonAppEntryFile,
        "entryModule" to pythonAppEntryModule,
        "serverType" to pythonAppServerType,
        "port" to pythonAppPort,
        "envVars" to pythonAppEnvVars,
        "landscapeMode" to pythonAppLandscapeMode
    )

    private fun ApkConfig.goAppConfigPayload(): Map<String, Any?> = linkedMapOf(
        "framework" to goAppFramework,
        "binaryName" to goAppBinaryName,
        "targetArch" to goAppTargetArch,
        "port" to goAppPort,
        "staticDir" to goAppStaticDir,
        "envVars" to goAppEnvVars,
        "landscapeMode" to goAppLandscapeMode
    )

    private fun ApkConfig.multiWebConfigPayload(): Map<String, Any?> = linkedMapOf(
        "sites" to multiWebSites,
        "displayMode" to multiWebDisplayMode,
        "refreshInterval" to multiWebRefreshInterval,
        "showSiteIcons" to multiWebShowSiteIcons,
        "landscapeMode" to multiWebLandscapeMode,
        "projectId" to multiWebProjectId
    )

    private fun EmbeddedExtensionModule.toPayload(): Map<String, Any?> = linkedMapOf(
        "id" to id,
        "name" to name,
        "description" to description,
        "icon" to icon,
        "category" to category,
        "versionName" to versionName,
        "authorName" to authorName,
        "code" to code,
        "cssCode" to cssCode,
        "runAt" to runAt,
        "sourceType" to sourceType,
        "runMode" to runMode,
        "uiConfig" to uiConfig,
        "urlMatches" to urlMatches,
        "configValues" to configValues,
        "configItemCount" to configItemCount,
        "gmGrants" to gmGrants,
        "requireUrls" to requireUrls,
        "requireContents" to requireContents,
        "resources" to resources,
        "noframes" to noframes,
        "enabled" to enabled
    )
}

internal object ApkConfigValidator {
    private val serverBackedAppTypes = setOf(
        "IMAGE",
        "VIDEO",
        "GALLERY",
        "WORDPRESS",
        "NODEJS_APP",
        "PHP_APP",
        "PYTHON_APP",
        "GO_APP",
        "MULTI_WEB"
    )

    fun requireValid(config: ApkConfig) {
        val appType = config.appType.trim().uppercase()
        val error = when {
            appType == "HTML" || appType == "FRONTEND" -> validateHtmlEntry(config.htmlEntryFile)
            appType in serverBackedAppTypes -> null
            config.targetUrl.isBlank() -> "targetUrl must not be blank for appType=$appType"
            else -> null
        }

        require(error == null) {
            "Invalid APK shell config for ${config.packageName}: $error"
        }
    }

    private fun validateHtmlEntry(entryFile: String): String? {
        val normalized = entryFile.trim()
        return when {
            normalized.isBlank() -> "htmlConfig.entryFile must not be blank"
            normalized.substringBeforeLast(".").isBlank() -> {
                "htmlConfig.entryFile must include a filename before the extension"
            }
            else -> null
        }
    }
}




class ApkConfig(
    var appName: String,
    var packageName: String,
    var targetUrl: String
) {
    var versionCode: Int = 1
    var versionName: String = "1.0.0"
    var iconPath: String? = null
    var runtimePermissions: com.webtoapp.data.model.ApkRuntimePermissions = com.webtoapp.data.model.ApkRuntimePermissions()
    var networkTrustConfig: com.webtoapp.data.model.NetworkTrustConfig = com.webtoapp.data.model.NetworkTrustConfig()


    var activationEnabled: Boolean = false
    var activationCodes: List<String> = emptyList()
    var activationRequireEveryTime: Boolean = false
    var activationDialogTitle: String = ""
    var activationDialogSubtitle: String = ""
    var activationDialogInputLabel: String = ""
    var activationDialogButtonText: String = ""


    var adBlockEnabled: Boolean = false
    var adBlockRules: List<String> = emptyList()


    var announcementEnabled: Boolean = false
    var announcementTitle: String = ""
    var announcementContent: String = ""
    var announcementLink: String = ""
    var announcementLinkText: String = ""
    var announcementTemplate: String = "MINIMAL"
    var announcementShowEmoji: Boolean = true
    var announcementAnimationEnabled: Boolean = true
    var announcementShowOnce: Boolean = true
    var announcementRequireConfirmation: Boolean = false
    var announcementAllowNeverShow: Boolean = false
    var announcementTriggerOnLaunch: Boolean = true
    var announcementTriggerOnNoNetwork: Boolean = false
    var announcementTriggerIntervalMinutes: Int = 0


    var adsEnabled: Boolean = false
    var adBannerEnabled: Boolean = false
    var adBannerId: String = ""
    var adInterstitialEnabled: Boolean = false
    var adInterstitialId: String = ""
    var adSplashEnabled: Boolean = false
    var adSplashId: String = ""


    var javaScriptEnabled: Boolean = true
    var domStorageEnabled: Boolean = true
    var allowFileAccess: Boolean = false
    var allowContentAccess: Boolean = true
    var cacheEnabled: Boolean = true
    var zoomEnabled: Boolean = true
    var desktopMode: Boolean = false
    var userAgent: String? = null
    var userAgentMode: String = "DEFAULT"
    var customUserAgent: String? = null
    var hideToolbar: Boolean = false
    var hideBrowserToolbar: Boolean = false
    var showStatusBarInFullscreen: Boolean = false
    var showNavigationBarInFullscreen: Boolean = false
    var showToolbarInFullscreen: Boolean = false
    var landscapeMode: Boolean = false
    var orientationMode: String = "PORTRAIT"
    var injectScripts: List<com.webtoapp.data.model.UserScript> = emptyList()


    var statusBarColorMode: String = "THEME"
    var statusBarColor: String? = null
    var statusBarDarkIcons: Boolean? = null
    var statusBarBackgroundType: String = "COLOR"
    var statusBarBackgroundImage: String? = null
    var statusBarBackgroundAlpha: Float = 1.0f
    var statusBarHeightDp: Int = 0

    var statusBarColorModeDark: String = "THEME"
    var statusBarColorDark: String? = null
    var statusBarDarkIconsDark: Boolean? = null
    var statusBarBackgroundTypeDark: String = "COLOR"
    var statusBarBackgroundImageDark: String? = null
    var statusBarBackgroundAlphaDark: Float = 1.0f
    var longPressMenuEnabled: Boolean = true
    var longPressMenuStyle: String = "FULL"
    var adBlockToggleEnabled: Boolean = false
    var popupBlockerEnabled: Boolean = false
    var popupBlockerToggleEnabled: Boolean = false
    var openExternalLinks: Boolean = false


    var initialScale: Int = 0
    var viewportMode: String = "DEFAULT"
    var customViewportWidth: Int = 0
    var newWindowBehavior: String = "SAME_WINDOW"
    var enablePaymentSchemes: Boolean = true
    var enableShareBridge: Boolean = true
    var enableZoomPolyfill: Boolean = true
    var enableCrossOriginIsolation: Boolean = false
    var hideUrlPreview: Boolean = false
    var disableShields: Boolean = true
    var decodeBase64DeepLinks: Boolean = false
    var mediaAutoplayEnabled: Boolean = true
    var acceptThirdPartyCookies: Boolean = true
    var enableKernelDisguise: Boolean = true
    var enableImageRepair: Boolean = true
    var enableScrollMemory: Boolean = true
    var enableHttpsUpgrade: Boolean = true
    var enableOAuthExternalRedirect: Boolean = true
    var enableClipboardPolyfill: Boolean = true
    var enableNotificationPolyfill: Boolean = true
    var safeBrowsingEnabled: Boolean = true
    var geolocationEnabled: Boolean = true
    var enableOrientationPolyfill: Boolean = true
    var enableCompatPolyfills: Boolean = true
    var enableNativeBridge: Boolean = true
    var javaScriptCanOpenWindows: Boolean = true
    var databaseEnabled: Boolean = true
    var enableCookiePersistence: Boolean = true
    var enablePrivateNetworkBridge: Boolean = true
    var allowMixedContent: Boolean = true
    var enableGpc: Boolean = true
    var enableCookieConsentBlock: Boolean = true
    var enableReferrerPolicy: Boolean = true
    var enableTrackerBlocking: Boolean = true
    var enableBlobDownloadInterception: Boolean = true
    var keepScreenOn: Boolean = false
    var screenAwakeMode: String = "OFF"
    var screenAwakeTimeoutMinutes: Int = 30
    var screenBrightness: Int = -1
    var keyboardAdjustMode: String = "RESIZE"
    var showFloatingBackButton: Boolean = false
    var swipeRefreshEnabled: Boolean = true
    var fullscreenEnabled: Boolean = true
    var performanceOptimization: Boolean = false
    var pwaOfflineEnabled: Boolean = false
    var pwaOfflineStrategy: String = "NETWORK_FIRST"


    var proxyMode: String = "NONE"
    var proxyHost: String = ""
    var proxyPort: Int = 0
    var proxyType: String = "HTTP"
    var pacUrl: String = ""
    var proxyBypassRules: List<String> = emptyList()
    var proxyUsername: String = ""
    var proxyPassword: String = ""
    var hostsMappingEnabled: Boolean = false
    var hostsMappings: List<com.webtoapp.data.model.HostMappingEntry> = emptyList()


    var dnsMode: String = "SYSTEM"
    var dnsConfig: DnsApkConfig = DnsApkConfig()


    var errorPageMode: String = "BUILTIN_STYLE"
    var errorPageBuiltInStyle: String = "MATERIAL"
    var errorPageShowMiniGame: Boolean = false
    var errorPageMiniGameType: String = "RANDOM"
    var errorPageAutoRetrySeconds: Int = 15
    var errorPageCustomHtml: String = ""
    var errorPageCustomMediaPath: String = ""
    var errorPageRetryButtonText: String = ""


    var floatingWindowEnabled: Boolean = false
    var floatingWindowSizePercent: Int = 80
    var floatingWindowWidthPercent: Int = 80
    var floatingWindowHeightPercent: Int = 80
    var floatingWindowLockAspectRatio: Boolean = true
    var floatingWindowOpacity: Int = 100
    var floatingWindowCornerRadius: Int = 16
    var floatingWindowBorderStyle: String = "SUBTLE"
    var floatingWindowShowTitleBar: Boolean = true
    var floatingWindowAutoHideTitleBar: Boolean = false
    var floatingWindowStartMinimized: Boolean = false
    var floatingWindowRememberPosition: Boolean = true
    var floatingWindowEdgeSnapping: Boolean = true
    var floatingWindowShowResizeHandle: Boolean = true
    var floatingWindowLockPosition: Boolean = false


    var splashEnabled: Boolean = false
    var splashType: String = "IMAGE"
    var splashDuration: Int = 3
    var splashClickToSkip: Boolean = true
    var splashVideoStartMs: Long = 0
    var splashVideoEndMs: Long = 5000
    var splashLandscape: Boolean = false
    var splashFillScreen: Boolean = true
    var splashEnableAudio: Boolean = false


    var appType: String = "WEB"
    var mediaEnableAudio: Boolean = true
    var mediaLoop: Boolean = true
    var mediaAutoPlay: Boolean = true
    var mediaFillScreen: Boolean = true
    var mediaLandscape: Boolean = false
    var mediaKeepScreenOn: Boolean = true


    var htmlEntryFile: String = "index.html"
    var htmlEnableJavaScript: Boolean = true
    var htmlEnableLocalStorage: Boolean = true
    var htmlLandscapeMode: Boolean = false


    var galleryItems: List<GalleryShellItemConfig> = emptyList()
    var galleryPlayMode: String = "SEQUENTIAL"
    var galleryImageInterval: Int = 3
    var galleryLoop: Boolean = true
    var galleryAutoPlay: Boolean = false
    var galleryBackgroundColor: String = "#000000"
    var galleryShowThumbnailBar: Boolean = true
    var galleryShowMediaInfo: Boolean = true
    var galleryOrientation: String = "PORTRAIT"
    var galleryEnableAudio: Boolean = true
    var galleryVideoAutoNext: Boolean = true
    var galleryShuffleOnLoop: Boolean = false
    var galleryDefaultView: String = "GRID"
    var galleryGridColumns: Int = 3
    var gallerySortOrder: String = "CUSTOM"
    var galleryRememberPosition: Boolean = true


    var bgmEnabled: Boolean = false
    var bgmPlaylist: List<BgmShellItem> = emptyList()
    var bgmPlayMode: String = "LOOP"
    var bgmVolume: Float = 0.5f
    var bgmAutoPlay: Boolean = true
    var bgmShowLyrics: Boolean = true
    var bgmLrcTheme: LrcShellTheme? = null


    var themeType: String = "AURORA"
    var darkMode: String = "SYSTEM"


    var translateEnabled: Boolean = false
    var translateTargetLanguage: String = "zh-CN"
    var translateShowButton: Boolean = true


    var extensionEnabled: Boolean = false
    var extensionModuleIds: List<String> = emptyList()
    var embeddedExtensionModules: List<EmbeddedExtensionModule> = emptyList()
    var extensionFabIcon: String = ""


    var autoStartEnabled: Boolean = false
    var bootStartEnabled: Boolean = false
    var scheduledStartEnabled: Boolean = false
    var scheduledTime: String = "08:00"
    var scheduledDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7)


    var forcedRunConfig: ForcedRunConfig? = null


    var isolationEnabled: Boolean = false
    var isolationConfig: com.webtoapp.core.isolation.IsolationConfig? = null


    var backgroundRunEnabled: Boolean = false
    var backgroundRunConfig: BackgroundRunConfig? = null


    var notificationEnabled: Boolean = false
    var notificationConfig: NotificationConfig? = null


    var blackTechConfig: com.webtoapp.core.blacktech.BlackTechConfig? = null


    var disguiseConfig: com.webtoapp.core.disguise.DisguiseConfig? = null


    var browserDisguiseConfig: com.webtoapp.core.disguise.BrowserDisguiseConfig? = null


    var deviceDisguiseConfig: com.webtoapp.core.disguise.DeviceDisguiseConfig? = null


    var language: String = "CHINESE"


    var engineType: String = "SYSTEM_WEBVIEW"


    var deepLinkEnabled: Boolean = false
    var deepLinkHosts: List<String> = emptyList()


    var wordpressSiteTitle: String = ""
    var wordpressAdminUser: String = "admin"
    var wordpressAdminEmail: String = ""
    var wordpressAdminPassword: String = "admin"
    var wordpressThemeName: String = ""
    var wordpressPlugins: List<String> = emptyList()
    var wordpressActivePlugins: List<String> = emptyList()
    var wordpressPermalinkStructure: String = "/%postname%/"
    var wordpressSiteLanguage: String = "zh_CN"
    var wordpressAutoInstall: Boolean = true
    var wordpressPhpPort: Int = 0
    var wordpressLandscapeMode: Boolean = false


    var nodejsMode: String = "STATIC"
    var nodejsPort: Int = 0
    var nodejsEntryFile: String = ""
    var nodejsEnvVars: Map<String, String> = emptyMap()
    var nodejsLandscapeMode: Boolean = false


    var phpAppFramework: String = ""
    var phpAppDocumentRoot: String = ""
    var phpAppEntryFile: String = "index.php"
    var phpAppPort: Int = 0
    var phpAppEnvVars: Map<String, String> = emptyMap()
    var phpAppLandscapeMode: Boolean = false


    var pythonAppFramework: String = ""
    var pythonAppEntryFile: String = "app.py"
    var pythonAppEntryModule: String = ""
    var pythonAppServerType: String = "builtin"
    var pythonAppPort: Int = 0
    var pythonAppEnvVars: Map<String, String> = emptyMap()
    var pythonAppLandscapeMode: Boolean = false


    var goAppFramework: String = ""
    var goAppBinaryName: String = ""
    var goAppTargetArch: String = "arm64-v8a"
    var goAppPort: Int = 0
    var goAppStaticDir: String = ""
    var goAppEnvVars: Map<String, String> = emptyMap()
    var goAppLandscapeMode: Boolean = false


    var multiWebSites: List<com.webtoapp.core.shell.MultiWebSiteShellConfig> = emptyList()
    var multiWebDisplayMode: String = "TABS"
    var multiWebRefreshInterval: Int = 30
    var multiWebShowSiteIcons: Boolean = true
    var multiWebLandscapeMode: Boolean = false
    var multiWebProjectId: String = ""
}



data class BackgroundRunConfig(
    val notificationTitle: String = "",
    val notificationContent: String = "",
    val showNotification: Boolean = true,
    val keepCpuAwake: Boolean = true
)




data class NotificationConfig(
    val type: String = "none",
    val pollUrl: String = "",
    val pollIntervalMinutes: Int = 15,
    val pollMethod: String = "GET",
    val pollHeaders: String = "",
    val clickUrl: String = ""
)




data class GalleryShellItemConfig(
    val id: String,
    val assetPath: String,
    val type: String,
    val name: String,
    val duration: Long = 0,
    val thumbnailPath: String? = null
)





data class EmbeddedExtensionModule(
    val id: String,
    val name: String,
    val description: String = "",
    val icon: String = "package",
    val category: String = "OTHER",
    val versionName: String = "1.0.0",
    val authorName: String = "",
    val code: String = "",
    val cssCode: String = "",
    val runAt: String = "DOCUMENT_END",
    val sourceType: String = "CUSTOM",
    val runMode: String = "INTERACTIVE",
    val uiConfig: EmbeddedExtensionModuleUiConfig = EmbeddedExtensionModuleUiConfig(),
    val urlMatches: List<EmbeddedUrlMatchRule> = emptyList(),
    val configValues: Map<String, String> = emptyMap(),
    val configItemCount: Int = 0,
    val gmGrants: List<String> = emptyList(),
    val requireUrls: List<String> = emptyList(),
    val requireContents: Map<String, String> = emptyMap(),
    val resources: Map<String, String> = emptyMap(),
    val noframes: Boolean = false,
    val enabled: Boolean = true
)

data class EmbeddedExtensionModuleUiConfig(
    val type: String = "FLOATING_BUTTON",
    val autoHide: Boolean = false,
    val autoHideDelay: Int = 3000,
    val initiallyHidden: Boolean = false,
    val showOnlyOnMatch: Boolean = true
)




data class EmbeddedUrlMatchRule(
    val pattern: String,
    val isRegex: Boolean = false,
    val exclude: Boolean = false
)




data class DnsApkConfig(
    val provider: String = "cloudflare",
    val customDohUrl: String = "",
    val dohMode: String = "automatic",
    val bypassSystemDns: Boolean = false
)
