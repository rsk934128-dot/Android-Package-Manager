package com.example.model

import android.graphics.drawable.Drawable

enum class BatteryDrainSeverity {
  LOW,
  MODERATE,
  HIGH,
  CRITICAL
}

data class AppBatteryDrainInfo(
  val appName: String,
  val packageName: String,
  val icon: Drawable? = null,
  val drainScore: Int, // 0 to 100
  val drainSeverity: BatteryDrainSeverity,
  val estimatedDrainPerHourMah: Float,
  val drainFactors: List<String>,
  val isRunningInBackground: Boolean,
  val hasBatteryOptimizationExemption: Boolean,
  val importanceCategory: String,
  val wakeLocksDeclared: Boolean,
  val backgroundLocationDeclared: Boolean
)

data class BatteryHealthReport(
  val batteryLevel: Int,
  val isCharging: Boolean,
  val chargingType: String,
  val temperatureCelsius: Float,
  val voltageMv: Int,
  val healthStatus: String,
  val isPowerSaveMode: Boolean,
  val drainingApps: List<AppBatteryDrainInfo>,
  val recommendations: List<String>
)
