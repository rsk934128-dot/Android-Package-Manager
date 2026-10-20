package com.example.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

class ApkInstallerManager(private val context: Context) {

  fun canInstallPackages(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.packageManager.canRequestPackageInstalls()
    } else {
      true
    }
  }

  fun requestInstallPermissionIntent(): Intent? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
        data = Uri.parse("package:${context.packageName}")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    } else {
      null
    }
  }

  fun installApk(file: File): Result<Unit> {
    return runCatching {
      if (!file.exists() || file.length() == 0L) {
        throw IllegalArgumentException("APK file does not exist or is empty")
      }

      val apkUri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
      )

      val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(intent)
    }
  }

  fun uninstallApp(packageName: String): Result<Unit> {
    return runCatching {
      val intent = Intent(Intent.ACTION_DELETE).apply {
        data = Uri.parse("package:$packageName")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(intent)
    }
  }

  fun openAppDetails(packageName: String): Result<Unit> {
    return runCatching {
      val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(intent)
    }
  }

  fun launchApp(packageName: String): Result<Unit> {
    return runCatching {
      val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        ?: throw IllegalStateException("Cannot find launch intent for $packageName")
      intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
      context.startActivity(intent)
    }
  }
}
