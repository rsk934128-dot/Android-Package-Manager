package com.example.manager

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.model.DetailedPermissionItem
import com.example.model.ThreatLevel

/**
 * Manager responsible for inspecting declared and granted permissions for any installed
 * application on the device, extracting real-time grant status, protection levels,
 * group metadata, and generating management intents.
 */
class AppPermissionManager(private val context: Context) {

  private val pm: PackageManager = context.packageManager

  private val dangerousPermissionsCatalog = mapOf(
    "android.permission.CAMERA" to Pair("Camera", "Allows the application to capture photos and video streams."),
    "android.permission.RECORD_AUDIO" to Pair("Microphone", "Allows capturing audio from device microphone."),
    "android.permission.ACCESS_FINE_LOCATION" to Pair("Location", "Precise GPS location access."),
    "android.permission.ACCESS_COARSE_LOCATION" to Pair("Location", "Approximate network-based location access."),
    "android.permission.ACCESS_BACKGROUND_LOCATION" to Pair("Location", "Access to device location while app is in background."),
    "android.permission.READ_CONTACTS" to Pair("Contacts", "Reads user contacts database and personal details."),
    "android.permission.WRITE_CONTACTS" to Pair("Contacts", "Modifies or creates contact entries."),
    "android.permission.READ_CALL_LOG" to Pair("Call Log", "Reads private incoming and outgoing call records."),
    "android.permission.WRITE_CALL_LOG" to Pair("Call Log", "Modifies call history records."),
    "android.permission.READ_SMS" to Pair("SMS", "Reads incoming SMS messages, including sensitive OTP codes."),
    "android.permission.RECEIVE_SMS" to Pair("SMS", "Monitors incoming SMS messages in real time."),
    "android.permission.SEND_SMS" to Pair("SMS", "Sends SMS messages, potentially incurring carrier charges."),
    "android.permission.READ_CALENDAR" to Pair("Calendar", "Accesses user appointments and calendar events."),
    "android.permission.WRITE_CALENDAR" to Pair("Calendar", "Modifies or adds calendar events."),
    "android.permission.BODY_SENSORS" to Pair("Sensors", "Accesses biometric data from sensors like heart rate monitors."),
    "android.permission.POST_NOTIFICATIONS" to Pair("Notifications", "Displays notifications in system status bar."),
    "android.permission.READ_EXTERNAL_STORAGE" to Pair("Storage", "Reads photos, downloads, and files from external storage."),
    "android.permission.WRITE_EXTERNAL_STORAGE" to Pair("Storage", "Modifies or deletes files on external storage."),
    "android.permission.READ_MEDIA_IMAGES" to Pair("Photos & Videos", "Accesses photos stored on device."),
    "android.permission.READ_MEDIA_VIDEO" to Pair("Photos & Videos", "Accesses video files stored on device."),
    "android.permission.READ_MEDIA_AUDIO" to Pair("Music & Audio", "Accesses audio and music files on device."),
    "android.permission.NEARBY_WIFI_DEVICES" to Pair("Nearby Devices", "Discovers and connects to nearby Wi-Fi devices."),
    "android.permission.BLUETOOTH_SCAN" to Pair("Nearby Devices", "Scans for nearby Bluetooth Low Energy devices."),
    "android.permission.BLUETOOTH_CONNECT" to Pair("Nearby Devices", "Pairs and connects to Bluetooth devices."),
    "android.permission.REQUEST_INSTALL_PACKAGES" to Pair("Special Access", "Allows requesting APK installations without Play Store."),
    "android.permission.REQUEST_DELETE_PACKAGES" to Pair("Special Access", "Can initiate app uninstallation dialogs."),
    "android.permission.SYSTEM_ALERT_WINDOW" to Pair("Special Access", "Displays floating windows or overlays over other applications."),
    "android.permission.WRITE_SETTINGS" to Pair("System Settings", "Can modify device system settings and configuration."),
    "android.permission.DUMP" to Pair("System Diagnostic", "Retrieves system internal dump state."),
    "android.permission.PACKAGE_USAGE_STATS" to Pair("Usage Access", "Monitors usage of other applications on the device.")
  )

  /**
   * Queries all declared permissions for [packageName], resolving real-time granted status,
   * protection level, permission group, system label, and security assessment.
   */
  fun getPermissionsForPackage(packageName: String): List<DetailedPermissionItem> {
    val pkgInfo = try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getPackageInfo(
          packageName,
          PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
        )
      } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
      }
    } catch (e: Exception) {
      return emptyList()
    }

    val requestedPerms = pkgInfo.requestedPermissions ?: emptyArray()
    val requestedFlags = pkgInfo.requestedPermissionsFlags ?: IntArray(0)

    val list = mutableListOf<DetailedPermissionItem>()

    for (i in requestedPerms.indices) {
      val permFullName = requestedPerms[i]
      val simpleName = permFullName.substringAfterLast('.')

      // Determine real-time granted status
      val flagGranted = if (i < requestedFlags.size) {
        (requestedFlags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
      } else false

      val checkGranted = try {
        pm.checkPermission(permFullName, packageName) == PackageManager.PERMISSION_GRANTED
      } catch (e: Exception) {
        false
      }

      val isGranted = flagGranted || checkGranted

      // Inspect system permission metadata
      val permInfo: PermissionInfo? = try {
        pm.getPermissionInfo(permFullName, 0)
      } catch (e: Exception) {
        null
      }

      // Determine protection level
      val rawProtection = permInfo?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          it.protection
        } else {
          @Suppress("DEPRECATION")
          it.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE
        }
      } ?: PermissionInfo.PROTECTION_NORMAL

      val isDangerous = rawProtection == PermissionInfo.PROTECTION_DANGEROUS ||
        dangerousPermissionsCatalog.containsKey(permFullName)

      val protectionLevelStr = when (rawProtection) {
        PermissionInfo.PROTECTION_DANGEROUS -> "Runtime (Dangerous)"
        PermissionInfo.PROTECTION_SIGNATURE -> "Signature (System)"
        PermissionInfo.PROTECTION_INTERNAL -> "Internal (Platform)"
        else -> if (dangerousPermissionsCatalog.containsKey(permFullName)) "Special Access" else "Install-time (Normal)"
      }

      // Determine Group
      val catalogEntry = dangerousPermissionsCatalog[permFullName]
      val groupName = catalogEntry?.first
        ?: permInfo?.group?.substringAfterLast('.')?.replace("PERMGROUP_", "")?.replace("_", " ")?.lowercase()?.replaceFirstChar { it.uppercase() }
        ?: when {
          permFullName.contains("NETWORK", true) || permFullName.contains("INTERNET", true) -> "Network"
          permFullName.contains("STORAGE", true) || permFullName.contains("MEDIA", true) -> "Storage"
          permFullName.contains("WIFI", true) || permFullName.contains("BLUETOOTH", true) -> "Connectivity"
          permFullName.contains("AUDIO", true) || permFullName.contains("CAMERA", true) -> "Media Capture"
          permFullName.contains("LOCATION", true) -> "Location"
          permFullName.contains("SMS", true) || permFullName.contains("PHONE", true) || permFullName.contains("CALL", true) -> "Telephony"
          else -> "General"
        }

      // Determine Label & Description
      val sysLabel = permInfo?.loadLabel(pm)?.toString()
      val sysDesc = permInfo?.loadDescription(pm)?.toString()

      val displayLabel = if (!sysLabel.isNullOrBlank() && sysLabel != permFullName) {
        sysLabel
      } else {
        simpleName.replace('_', ' ').lowercase().split(' ').joinToString(" ") { word ->
          word.replaceFirstChar { it.uppercase() }
        }
      }

      val displayDesc = if (!sysDesc.isNullOrBlank()) {
        sysDesc
      } else {
        catalogEntry?.second ?: "Standard platform permission requested by application."
      }

      // Threat evaluation and recommendation
      val (risk, recommendation) = when {
        isDangerous && isGranted -> Pair(
          ThreatLevel.HIGH,
          "Currently ACTIVE. This grants the app direct access to sensitive hardware or personal user data."
        )
        isDangerous && !isGranted -> Pair(
          ThreatLevel.MEDIUM,
          "Requested by app manifest, but currently REVOKED or NOT GRANTED by user."
        )
        else -> Pair(
          ThreatLevel.SAFE,
          "Standard install-time permission automatically managed by Android."
        )
      }

      list.add(
        DetailedPermissionItem(
          fullName = permFullName,
          simpleName = simpleName,
          isGranted = isGranted,
          isDangerous = isDangerous,
          protectionLevel = protectionLevelStr,
          groupName = groupName,
          label = displayLabel,
          description = displayDesc,
          riskLevel = risk,
          riskRecommendation = recommendation
        )
      )
    }

    // Sort: Dangerous & Granted first, then Dangerous Denied, then Normal
    return list.sortedWith(
      compareByDescending<DetailedPermissionItem> { it.isDangerous && it.isGranted }
        .thenByDescending { it.isDangerous }
        .thenByDescending { it.isGranted }
        .thenBy { it.simpleName }
    )
  }

  /**
   * Creates an Intent to navigate directly to the system App Permissions or App Info screen
   * for the given [packageName], allowing users to modify or revoke permissions.
   */
  fun createManagePermissionsIntent(packageName: String): Intent {
    // Attempt modern MANAGE_APP_PERMISSIONS intent if supported
    val managePermsIntent = Intent("android.intent.action.MANAGE_APP_PERMISSIONS").apply {
      putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    if (managePermsIntent.resolveActivity(pm) != null) {
      return managePermsIntent
    }

    // Standard fallback: Android Application Details Settings
    return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.fromParts("package", packageName, null)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  }
}
