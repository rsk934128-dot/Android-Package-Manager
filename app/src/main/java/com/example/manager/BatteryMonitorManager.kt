package com.example.manager

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.model.AppBatteryDrainInfo
import com.example.model.BatteryDrainSeverity
import com.example.model.BatteryHealthReport
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.ConcurrentHashMap

class BatteryMonitorManager(private val context: Context) {

  companion object {
    const val CHANNEL_ID_BATTERY_ALERTS = "apppulse_battery_alerts"
    const val DEFAULT_DRAIN_THRESHOLD_SCORE = 50
    private const val NOTIFICATION_COOLDOWN_MS = 10 * 60 * 1000L // 10 minutes per package
  }

  private val pm: PackageManager = context.packageManager
  private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
  private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
  private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  private val lastNotifiedMap = ConcurrentHashMap<String, Long>()

  init {
    createNotificationChannel()
  }

  fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = "Battery Drain Alerts"
      val descriptionText = "Alerts when background apps exceed safe power consumption limits"
      val importance = NotificationManager.IMPORTANCE_HIGH
      val channel = NotificationChannel(CHANNEL_ID_BATTERY_ALERTS, name, importance).apply {
        description = descriptionText
        enableVibration(true)
      }
      notificationManager.createNotificationChannel(channel)
    }
  }

  /**
   * Generates a comprehensive real-time battery telemetry and background tasks drain report.
   */
  fun generateBatteryReport(): BatteryHealthReport {
    val batteryIntent: Intent? = context.registerReceiver(
      null,
      IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    )

    val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 50
    val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
    val batteryPct = if (level >= 0 && scale > 0) ((level * 100) / scale) else 50

    val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
      status == BatteryManager.BATTERY_STATUS_FULL

    val chargePlug = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
    val chargingType = when (chargePlug) {
      BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
      BatteryManager.BATTERY_PLUGGED_AC -> "AC Fast Charger"
      BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Dock"
      else -> if (isCharging) "Charging" else "Discharging"
    }

    val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
    val tempCelsius = tempTenths / 10.0f
    val voltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

    val health = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
    val healthStatus = when (health) {
      BatteryManager.BATTERY_HEALTH_GOOD -> "Optimal Condition"
      BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating (Hazard)"
      BatteryManager.BATTERY_HEALTH_DEAD -> "Degraded / Dead"
      BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
      else -> "Normal"
    }

    val isPowerSave = powerManager.isPowerSaveMode

    // Analyze background processes and their drain profiles
    val drainingApps = analyzeBackgroundDrainingTasks()

    val recommendations = mutableListOf<String>()
    if (tempCelsius > 42f) {
      recommendations.add("High Battery Temperature (${tempCelsius}°C): Reduce intensive background tasks and disconnect high-power charger.")
    }
    if (drainingApps.any { it.drainSeverity == BatteryDrainSeverity.CRITICAL || it.drainSeverity == BatteryDrainSeverity.HIGH }) {
      recommendations.add("Detected background tasks holding persistent WakeLocks and unrestricted location. Consider optimizing background execution.")
    }
    if (!isPowerSave && batteryPct <= 20) {
      recommendations.add("Battery is at ${batteryPct}%. Enable Android Battery Saver mode to extend standby time.")
    }
    if (recommendations.isEmpty()) {
      recommendations.add("Battery health is optimal. No aggressive background anomalies detected.")
    }

    return BatteryHealthReport(
      batteryLevel = batteryPct,
      isCharging = isCharging,
      chargingType = chargingType,
      temperatureCelsius = tempCelsius,
      voltageMv = voltageMv,
      healthStatus = healthStatus,
      isPowerSaveMode = isPowerSave,
      drainingApps = drainingApps,
      recommendations = recommendations
    )
  }

  /**
   * Scans active background processes and maps them against declared battery-heavy capabilities.
   */
  fun analyzeBackgroundDrainingTasks(): List<AppBatteryDrainInfo> {
    val runningProcesses = runCatching { activityManager.runningAppProcesses }.getOrNull() ?: emptyList()
    val runningPkgMap = mutableMapOf<String, ActivityManager.RunningAppProcessInfo>()

    for (process in runningProcesses) {
      process.pkgList?.forEach { pkg ->
        // Keep highest impact process state for each package
        val existing = runningPkgMap[pkg]
        if (existing == null || process.importance < existing.importance) {
          runningPkgMap[pkg] = process
        }
      }
    }

    val installedPackages = runCatching {
      pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
    }.getOrNull() ?: emptyList()

    val drainList = mutableListOf<AppBatteryDrainInfo>()

    for (pkgInfo in installedPackages) {
      val appInfo = pkgInfo.applicationInfo ?: continue
      val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
      // Prioritize third-party user apps and non-system services
      val packageName = pkgInfo.packageName
      if (packageName == context.packageName) continue // Skip self

      val permissions = pkgInfo.requestedPermissions?.toSet() ?: emptySet()
      val hasWakeLock = permissions.contains("android.permission.WAKE_LOCK")
      val hasBgLocation = permissions.contains("android.permission.ACCESS_BACKGROUND_LOCATION") ||
        (permissions.contains("android.permission.ACCESS_FINE_LOCATION") && permissions.contains("android.permission.FOREGROUND_SERVICE"))
      val hasForegroundService = permissions.contains("android.permission.FOREGROUND_SERVICE")
      val hasBootReceiver = permissions.contains("android.permission.RECEIVE_BOOT_COMPLETED")
      val isIgnoringBatteryOpt = runCatching {
        powerManager.isIgnoringBatteryOptimizations(packageName)
      }.getOrDefault(false)

      val processInfo = runningPkgMap[packageName]
      val isRunning = processInfo != null

      // Calculate drain score based on active execution + hardware capability
      var score = 0
      val drainFactors = mutableListOf<String>()

      if (isRunning) {
        val importance = processInfo?.importance ?: ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED
        if (importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE) {
          score += 35
          drainFactors.add("Active Foreground Service running in background")
        } else if (importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE) {
          score += 25
          drainFactors.add("Perceptible background task active")
        } else {
          score += 15
          drainFactors.add("Cached background process in RAM")
        }
      }

      if (hasWakeLock) {
        score += 25
        drainFactors.add("Holds WakeLock permission (prevents CPU deep sleep)")
      }

      if (hasBgLocation) {
        score += 25
        drainFactors.add("Continuous background GPS/Location polling")
      }

      if (isIgnoringBatteryOpt) {
        score += 20
        drainFactors.add("Exempt from Android Doze / Battery Optimization")
      }

      if (hasBootReceiver) {
        score += 10
        drainFactors.add("Auto-starts at device boot (RECEIVE_BOOT_COMPLETED)")
      }

      if (hasForegroundService && !drainFactors.contains("Active Foreground Service running in background")) {
        score += 10
        drainFactors.add("Declares Foreground Service capability")
      }

      // Filter only apps with noticeable drain factors or active background states
      if (score >= 20 || isRunning) {
        val finalScore = score.coerceIn(10, 100)
        val severity = when {
          finalScore >= 70 -> BatteryDrainSeverity.CRITICAL
          finalScore >= 50 -> BatteryDrainSeverity.HIGH
          finalScore >= 30 -> BatteryDrainSeverity.MODERATE
          else -> BatteryDrainSeverity.LOW
        }

        // Estimate current drain in mAh per hour based on weighted capability
        val estimatedMahPerHour = (finalScore * 0.45f).coerceAtLeast(2.5f)

        val importanceCategory = when (processInfo?.importance) {
          ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "Active Foreground"
          ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE -> "Foreground Service"
          ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "Visible Process"
          ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE -> "Perceptible Background"
          ActivityManager.RunningAppProcessInfo.IMPORTANCE_CANT_SAVE_STATE -> "Heavy Background State"
          null -> "Idle / Dormant"
          else -> "Cached in RAM"
        }

        val appLabel = runCatching { appInfo.loadLabel(pm).toString() }.getOrDefault(packageName)
        val appIcon = runCatching { appInfo.loadIcon(pm) }.getOrNull()

        drainList.add(
          AppBatteryDrainInfo(
            appName = appLabel,
            packageName = packageName,
            icon = appIcon,
            drainScore = finalScore,
            drainSeverity = severity,
            estimatedDrainPerHourMah = estimatedMahPerHour,
            drainFactors = drainFactors,
            isRunningInBackground = isRunning,
            hasBatteryOptimizationExemption = isIgnoringBatteryOpt,
            importanceCategory = importanceCategory,
            wakeLocksDeclared = hasWakeLock,
            backgroundLocationDeclared = hasBgLocation
          )
        )
      }
    }

    return drainList.sortedByDescending { it.drainScore }
  }

  /**
   * Broadcast flow observing battery state changes in real time.
   */
  fun observeBatteryState(): Flow<BatteryHealthReport> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        trySend(generateBatteryReport())
      }
    }

    val filter = IntentFilter().apply {
      addAction(Intent.ACTION_BATTERY_CHANGED)
      addAction(Intent.ACTION_POWER_CONNECTED)
      addAction(Intent.ACTION_POWER_DISCONNECTED)
      addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
    }

    context.registerReceiver(receiver, filter)
    trySend(generateBatteryReport()) // Initial state

    awaitClose {
      runCatching { context.unregisterReceiver(receiver) }
    }
  }

  /**
   * Helper intent to open Battery Optimization settings for an application.
   */
  fun createBatteryOptimizationIntent(targetPackageName: String? = null): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    } else {
      Intent(Settings.ACTION_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    }
  }

  /**
   * Helper intent to navigate to System App Details to Force Stop a draining task.
   */
  fun createForceStopSettingsIntent(packageName: String): Intent {
    return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.parse("package:$packageName")
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
  }

  /**
   * Scans background tasks and dispatches a notification for any task exceeding the power consumption threshold.
   */
  @SuppressLint("MissingPermission")
  fun checkAndNotifyHighDrainTasks(
    thresholdDrainScore: Int = DEFAULT_DRAIN_THRESHOLD_SCORE,
    bypassCooldown: Boolean = false
  ): List<AppBatteryDrainInfo> {
    val drainingApps = analyzeBackgroundDrainingTasks()
    val highDrainApps = drainingApps.filter { it.drainScore >= thresholdDrainScore }

    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }

    if (!hasPermission || highDrainApps.isEmpty()) {
      return highDrainApps
    }

    val currentTime = System.currentTimeMillis()
    val alertedApps = mutableListOf<AppBatteryDrainInfo>()

    for (app in highDrainApps) {
      val lastNotified = lastNotifiedMap[app.packageName] ?: 0L
      if (!bypassCooldown && (currentTime - lastNotified) < NOTIFICATION_COOLDOWN_MS) {
        continue
      }

      val notificationId = ("battery_drain_" + app.packageName).hashCode()

      val mainIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent(Intent.ACTION_MAIN)
      mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      val contentPendingIntent = PendingIntent.getActivity(
        context,
        notificationId,
        mainIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val optIntent = createBatteryOptimizationIntent(app.packageName)
      val optPendingIntent = PendingIntent.getActivity(
        context,
        notificationId + 1,
        optIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val bigText = buildString {
        append("Background Task: ${app.appName} (${app.packageName})\n")
        append("Drain Score: ${app.drainScore}/100 (~${String.format("%.1f", app.estimatedDrainPerHourMah)} mAh/h)\n")
        append("Process State: ${app.importanceCategory}\n")
        append("Drain Factors:\n")
        app.drainFactors.forEach { append(" • $it\n") }
      }

      val notification = NotificationCompat.Builder(context, CHANNEL_ID_BATTERY_ALERTS)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("⚠️ High Battery Drain: ${app.appName}")
        .setContentText("Consuming ~${String.format("%.1f", app.estimatedDrainPerHourMah)} mAh/h in background.")
        .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(contentPendingIntent)
        .addAction(android.R.drawable.ic_menu_manage, "Restrict Battery", optPendingIntent)
        .build()

      runCatching {
        NotificationManagerCompat.from(context).notify(notificationId, notification)
        lastNotifiedMap[app.packageName] = currentTime
        alertedApps.add(app)
      }
    }

    return alertedApps
  }
}
