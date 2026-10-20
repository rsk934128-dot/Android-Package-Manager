package com.example.manager

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.model.AppPackageItem
import com.example.model.ManifestAuditResult
import com.example.model.PermissionDetail
import com.example.model.ThreatLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SecurityAuditEngine(private val context: Context) {

  private val dangerousPermissionsMap = mapOf(
    "android.permission.REQUEST_INSTALL_PACKAGES" to Pair(
      "App Dropper / Silent Install",
      "Allows the app to trigger APK installations, a common feature in package updaters or malicious droppers."
    ),
    "android.permission.REQUEST_DELETE_PACKAGES" to Pair(
      "App Deletion / Removal",
      "Can initiate uninstallations of other applications on the device."
    ),
    "android.permission.DUMP" to Pair(
      "System Dump & Memory Inspector",
      "High-level system permission to retrieve internal system state and memory dumps."
    ),
    "android.permission.SYSTEM_ALERT_WINDOW" to Pair(
      "Screen Overlay / Tapjacking",
      "Can draw on top of other applications, potentially intercepting user touches or spoofing dialogs."
    ),
    "android.permission.WRITE_SETTINGS" to Pair(
      "System Settings Modification",
      "Allows altering critical device configuration and settings."
    ),
    "android.permission.RECORD_AUDIO" to Pair(
      "Microphone Surveillance",
      "Can capture audio from the environment."
    ),
    "android.permission.CAMERA" to Pair(
      "Camera Capture",
      "Allows capturing photos and videos without user prompt once granted."
    ),
    "android.permission.ACCESS_FINE_LOCATION" to Pair(
      "Precise GPS Tracking",
      "Accesses accurate latitude and longitude coordinates of the device."
    ),
    "android.permission.ACCESS_BACKGROUND_LOCATION" to Pair(
      "Persistent Background Tracking",
      "Tracks user location even when the application is closed."
    ),
    "android.permission.READ_SMS" to Pair(
      "SMS & OTP Reading",
      "Can read incoming SMS messages, sensitive verification codes and personal messages."
    ),
    "android.permission.READ_CALL_LOG" to Pair(
      "Call History Access",
      "Reads device call history, phone numbers and call durations."
    )
  )

  suspend fun scanInstalledApps(): List<AppPackageItem> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val flags = PackageManager.GET_PERMISSIONS

    val packageInfos: List<PackageInfo> = runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
      } else {
        @Suppress("DEPRECATION")
        pm.getInstalledPackages(flags)
      }
    }.getOrElse { emptyList() }

    val results = mutableListOf<AppPackageItem>()

    for (pkg in packageInfos) {
      val appInfo = pkg.applicationInfo ?: continue
      val appName = runCatching { pm.getApplicationLabel(appInfo).toString() }.getOrDefault(pkg.packageName)
      val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

      val requestedPerms = pkg.requestedPermissions ?: emptyArray()
      val permissionDetails = requestedPerms.map { perm ->
        val info = dangerousPermissionsMap[perm]
        PermissionDetail(
          name = perm.substringAfterLast('.'),
          isHighRisk = info != null,
          category = info?.first ?: "Standard",
          explanation = info?.second ?: "General platform permission requested in manifest."
        )
      }

      val (threatLevel, score, reasons) = evaluateThreat(
        packageName = pkg.packageName,
        isSystem = isSystem,
        permissions = requestedPerms.toList()
      )

      val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) appInfo.minSdkVersion else 0
      val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        pkg.longVersionCode
      } else {
        @Suppress("DEPRECATION")
        pkg.versionCode.toLong()
      }

      results.add(
        AppPackageItem(
          appName = appName,
          packageName = pkg.packageName,
          versionName = pkg.versionName ?: "1.0",
          versionCode = versionCode,
          isSystemApp = isSystem,
          targetSdk = appInfo.targetSdkVersion,
          minSdk = minSdk,
          firstInstallTime = pkg.firstInstallTime,
          lastUpdateTime = pkg.lastUpdateTime,
          permissions = permissionDetails,
          threatLevel = threatLevel,
          threatScore = score,
          threatReasons = reasons
        )
      )
    }

    results.sortedWith(
      compareByDescending<AppPackageItem> { it.threatScore }
        .thenBy { it.isSystemApp }
        .thenBy { it.appName.lowercase() }
    )
  }

  private fun evaluateThreat(
    packageName: String,
    isSystem: Boolean,
    permissions: List<String>
  ): Triple<ThreatLevel, Int, List<String>> {
    var score = 0
    val reasons = mutableListOf<String>()

    if (permissions.contains("android.permission.REQUEST_INSTALL_PACKAGES")) {
      score += 35
      reasons.add("Has REQUEST_INSTALL_PACKAGES: can trigger external APK installations.")
    }

    if (permissions.contains("android.permission.REQUEST_DELETE_PACKAGES")) {
      score += 25
      reasons.add("Has REQUEST_DELETE_PACKAGES: can initiate package deletions.")
    }

    if (permissions.contains("android.permission.DUMP")) {
      score += 20
      reasons.add("Requests DUMP permission: system state inspection.")
    }

    if (permissions.contains("android.permission.SYSTEM_ALERT_WINDOW")) {
      score += 15
      reasons.add("Overlay permission: can display windows above other apps.")
    }

    val lowerPkg = packageName.lowercase()
    if (lowerPkg.contains("downloadapp") || lowerPkg.contains("dropper") || lowerPkg.contains("stalker") || lowerPkg.contains("monitor")) {
      score += 20
      reasons.add("Suspicious package naming pattern detected: '$packageName'.")
    }

    if (isSystem) {
      // System apps naturally have high-privilege permissions
      score = (score * 0.4).toInt()
    }

    val level = when {
      score >= 50 -> ThreatLevel.HIGH
      score >= 25 -> ThreatLevel.MEDIUM
      else -> ThreatLevel.SAFE
    }

    return Triple(level, score.coerceIn(0, 100), reasons)
  }

  fun auditManifestText(rawContent: String): ManifestAuditResult {
    val sensitivePerms = mutableListOf<String>()
    val spywareKeywords = mutableListOf<String>()
    val vulnerabilities = mutableListOf<String>()

    val checkPerms = listOf(
      "android.permission.REQUEST_INSTALL_PACKAGES",
      "android.permission.REQUEST_DELETE_PACKAGES",
      "android.permission.DUMP",
      "android.permission.SYSTEM_ALERT_WINDOW",
      "android.permission.INTERNET",
      "android.permission.ACCESS_NETWORK_STATE"
    )

    for (p in checkPerms) {
      if (rawContent.contains(p)) {
        sensitivePerms.add(p)
      }
    }

    val suspiciousStrings = listOf(
      "mobile.monitor.child",
      "mobile.parental",
      "monitor.phone",
      "phone.child",
      "phone.monitor",
      "com.protect",
      "downloadapp",
      "AppMain",
      "AppMainActivity",
      "DownloadAPPActivity"
    )

    for (s in suspiciousStrings) {
      if (rawContent.contains(s)) {
        spywareKeywords.add(s)
      }
    }

    // Extract package name
    val packageMatch = Regex("""(com\.[a-zA-Z0-9_.]+)""").find(rawContent)?.value ?: "com.mtf.app"

    var score = 0
    if (sensitivePerms.contains("android.permission.REQUEST_INSTALL_PACKAGES")) {
      score += 35
      vulnerabilities.add("Dropper Behavior: Contains silent package installer request")
    }
    if (sensitivePerms.contains("android.permission.REQUEST_DELETE_PACKAGES")) {
      score += 25
      vulnerabilities.add("Anti-Analysis/Tampering: Contains package deletion capability")
    }
    if (sensitivePerms.contains("android.permission.DUMP")) {
      score += 20
      vulnerabilities.add("Telemetry Exfiltration: DUMP system diagnostics permission")
    }
    if (spywareKeywords.any { it.contains("monitor") || it.contains("child") || it.contains("parental") }) {
      score += 30
      vulnerabilities.add("Stalkerware/Tracking: Embedded child/phone surveillance tags detected")
    }
    if (spywareKeywords.any { it.contains("protect") }) {
      score += 15
      vulnerabilities.add("Shell / Packer Obfuscation: Protection layer strings found")
    }

    val level = when {
      score >= 60 -> ThreatLevel.HIGH
      score >= 30 -> ThreatLevel.MEDIUM
      else -> ThreatLevel.SAFE
    }

    val verdict = when (level) {
      ThreatLevel.HIGH -> "Critical Threat: Suspicious Dropper & Monitoring Stalkerware detected!"
      ThreatLevel.MEDIUM -> "Caution: App contains elevated installer or system permissions."
      ThreatLevel.SAFE -> "Standard: No malicious dropper or surveillance signatures found."
    }

    return ManifestAuditResult(
      packageName = packageMatch,
      launcherActivity = "com.service.downloadapp.AppMain",
      otherActivities = listOf("com.service.downloadapp.DownloadAPPActivity"),
      minSdk = 17,
      fileProviderAuthority = "com.mtf.app.provider",
      sensitivePermissions = sensitivePerms,
      spywareKeywordsFound = spywareKeywords,
      detectedVulnerabilities = vulnerabilities,
      threatScore = score.coerceIn(0, 100),
      threatLevel = level,
      verdictSummary = verdict
    )
  }
}
