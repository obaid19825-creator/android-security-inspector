package com.securityinspector.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Annotated for Hilt dependency injection.
 *
 * This app performs read-only inspection of the device it runs on. It does not
 * request root, does not attempt to bypass platform security, and does not
 * interact with other installed applications beyond reading public metadata
 * via documented [android.content.pm.PackageManager] APIs.
 */
@HiltAndroidApp
class SecurityInspectorApp : Application()
