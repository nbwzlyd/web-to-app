package com.webtoapp.core.apkbuilder

internal fun testApkConfig(
    appName: String,
    packageName: String,
    targetUrl: String
): ApkConfig = ApkConfig(appName, packageName, targetUrl)
