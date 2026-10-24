package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SystemDiagnostic
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.RiskHigh
import com.example.ui.theme.RiskSafe

import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Power
import com.example.model.BatteryHealthReport
import com.example.model.BatteryDrainSeverity
import com.example.model.AppBatteryDrainInfo

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ContentCopy
import com.example.manager.RemoteServerState
import androidx.compose.material.icons.filled.CloudSync
import android.widget.Toast

@Composable
fun DiagnosticsScreen(
  diagnostic: SystemDiagnostic?,
  remoteServerState: RemoteServerState,
  batteryReport: BatteryHealthReport?,
  onRefresh: () -> Unit,
  onClearCache: () -> Unit,
  onToggleRemoteServer: (Boolean) -> Unit,
  onPlayFindDeviceSound: () -> Unit,
  onOpenBatteryOptimization: (String?) -> Unit,
  onOpenAppDetails: (String) -> Unit,
  onScanAndAlertDrain: () -> Unit,
  onSyncBatteryToCloud: (() -> Unit)? = null
) {
  val context = LocalContext.current

  val notificationLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      onScanAndAlertDrain()
      Toast.makeText(context, "Scanning tasks and triggering battery notifications...", Toast.LENGTH_SHORT).show()
    } else {
      Toast.makeText(context, "Notification permission denied. Enable in Settings for alerts.", Toast.LENGTH_SHORT).show()
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          Text(
            text = "Device & System Diagnostics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Hardware, network telemetry and installer permissions",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        IconButton(
          onClick = onRefresh,
          modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .testTag("refresh_diagnostics_button")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
    }

    diagnostic?.let { diag ->
      // Install Permission Status Card
      item {
        val hasInstallPerm = diag.canInstallPackages
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (hasInstallPerm) RiskSafe.copy(alpha = 0.12f) else RiskHigh.copy(alpha = 0.12f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = if (hasInstallPerm) Icons.Default.CheckCircle else Icons.Default.Warning,
                  contentDescription = null,
                  tint = if (hasInstallPerm) RiskSafe else RiskHigh,
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = "Unknown APK Install Permission",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = if (hasInstallPerm) "Granted: Can install APKs via FileProvider" else "Restricted: Requires toggle in System Settings",
                    fontSize = 11.sp,
                    color = if (hasInstallPerm) RiskSafe else RiskHigh
                  )
                }
              }
            }

            if (!hasInstallPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              Spacer(modifier = Modifier.height(12.dp))
              Button(
                onClick = {
                  val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                  }
                  context.startActivity(intent)
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("open_install_permission_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
              ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Allow APK Installation in Settings", fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
      }

      // Remote Web Dashboard Card
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (remoteServerState.isRunning) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
          ),
          modifier = Modifier.fillMaxWidth().testTag("remote_server_card")
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Default.Computer,
                  contentDescription = null,
                  tint = if (remoteServerState.isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = "Web Remote Dashboard",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = if (remoteServerState.isRunning) "Server Active • Accessible via Wi-Fi" else "Disabled • Run server for PC access",
                    fontSize = 11.sp,
                    color = if (remoteServerState.isRunning) RiskSafe else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Switch(
                checked = remoteServerState.isRunning,
                onCheckedChange = { onToggleRemoteServer(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
              )
            }

            if (remoteServerState.isRunning) {
              Spacer(modifier = Modifier.height(12.dp))

              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant)
                  .padding(10.dp)
              ) {
                Column {
                  Text(
                    text = "Open in Computer / Laptop Browser:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = remoteServerState.serverUrl,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                  )
                  Text(
                    text = "Security Access PIN: ${remoteServerState.securityPin}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldTertiary
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = {
                    onPlayFindDeviceSound()
                    Toast.makeText(context, "Testing alarm sound...", Toast.LENGTH_SHORT).show()
                  },
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Test Alert", fontSize = 11.sp)
                }

                OutlinedButton(
                  onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Server URL", remoteServerState.serverUrl)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "URL copied to clipboard!", Toast.LENGTH_SHORT).show()
                  },
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Copy URL", fontSize = 11.sp)
                }
              }

              if (remoteServerState.lastCommand != "None") {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Last Action: ${remoteServerState.lastCommand}",
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
      }

      // Network Telemetry
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Network Telemetry",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            DiagRow("Status", if (diag.isOnline) "Connected & Online" else "Disconnected (Offline)")
            DiagRow("Interface Type", diag.networkType)
            DiagRow("Local IP Address", diag.ipAddress)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
      }

      // Storage & RAM Gauges
      item {
        val usedStorage = (diag.totalStorageBytes - diag.freeStorageBytes).coerceAtLeast(0L)
        val storagePercent = if (diag.totalStorageBytes > 0) (usedStorage.toFloat() / diag.totalStorageBytes.toFloat()) else 0f

        val usedRam = (diag.totalRamBytes - diag.freeRamBytes).coerceAtLeast(0L)
        val ramPercent = if (diag.totalRamBytes > 0) (usedRam.toFloat() / diag.totalRamBytes.toFloat()) else 0f

        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Storage, contentDescription = null, tint = EmeraldTertiary, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Device Storage & RAM",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Storage bar
            Text(
              text = "Internal Storage: ${formatGigabytes(usedStorage)} used of ${formatGigabytes(diag.totalStorageBytes)}",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
              progress = { storagePercent },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
              color = EmeraldTertiary,
              trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // RAM bar
            Text(
              text = "System RAM: ${formatGigabytes(usedRam)} used of ${formatGigabytes(diag.totalRamBytes)}",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
              progress = { ramPercent },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
              color = MaterialTheme.colorScheme.secondary,
              trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
              onClick = onClearCache,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("clear_download_cache_button")
            ) {
              Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Clean Temporary APK Cache")
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
      }

      // Battery Telemetry & Background Tasks Drain Monitor
      batteryReport?.let { bReport ->
        item {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().testTag("battery_monitor_card")
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = if (bReport.isCharging) Icons.Default.BatteryChargingFull else if (bReport.batteryLevel <= 20) Icons.Default.BatteryAlert else Icons.Default.BatteryStd,
                    contentDescription = null,
                    tint = if (bReport.isCharging) EmeraldTertiary else if (bReport.batteryLevel <= 20) RiskHigh else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Battery & Background Drain",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }

                Surface(
                  shape = RoundedCornerShape(20.dp),
                  color = if (bReport.isCharging) EmeraldTertiary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                  Text(
                    text = "${bReport.batteryLevel}% • ${bReport.chargingType}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (bReport.isCharging) EmeraldTertiary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              LinearProgressIndicator(
                progress = { bReport.batteryLevel / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = if (bReport.batteryLevel <= 20) RiskHigh else if (bReport.isCharging) EmeraldTertiary else MaterialTheme.colorScheme.primary
              )

              Spacer(modifier = Modifier.height(10.dp))

              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Temp: ${String.format("%.1f", bReport.temperatureCelsius)}°C", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Voltage: ${bReport.voltageMv} mV", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Health: ${bReport.healthStatus}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = EmeraldTertiary)
              }

              Spacer(modifier = Modifier.height(12.dp))

              OutlinedButton(
                onClick = { onOpenBatteryOptimization(null) },
                modifier = Modifier.fillMaxWidth()
              ) {
                Icon(Icons.Default.Power, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Android Battery Optimization Settings", fontSize = 11.sp)
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
        }

        // List of Background Tasks Draining Battery
        item {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().testTag("background_tasks_card")
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Bolt, contentDescription = null, tint = RiskHigh, modifier = Modifier.size(20.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "High-Drain Background Tasks",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }

                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = RiskHigh.copy(alpha = 0.15f)
                ) {
                  Text(
                    text = "${bReport.drainingApps.size} detected",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = RiskHigh,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                  )
                }
              }

              Text(
                text = "Identified background processes holding WakeLocks, running foreground services, or polling GPS.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
              )

              if (bReport.drainingApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                  Text("No aggressive background drain detected.", fontSize = 12.sp, color = RiskSafe)
                }
              } else {
                bReport.drainingApps.take(6).forEach { app ->
                  val badgeColor = when (app.drainSeverity) {
                    BatteryDrainSeverity.CRITICAL -> RiskHigh
                    BatteryDrainSeverity.HIGH -> Color(0xFFE65100)
                    BatteryDrainSeverity.MODERATE -> Color(0xFFF57C00)
                    BatteryDrainSeverity.LOW -> RiskSafe
                  }

                  Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                  ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text(app.appName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                          Text(app.packageName, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Surface(
                          shape = RoundedCornerShape(6.dp),
                          color = badgeColor.copy(alpha = 0.2f)
                        ) {
                          Text(
                            text = "${app.drainScore}/100 • ~${String.format("%.1f", app.estimatedDrainPerHourMah)} mAh/h",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                          )
                        }
                      }

                      Spacer(modifier = Modifier.height(6.dp))

                      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                          shape = RoundedCornerShape(4.dp),
                          color = MaterialTheme.colorScheme.surface
                        ) {
                          Text(
                            text = app.importanceCategory,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }

                        if (app.wakeLocksDeclared) {
                          Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface
                          ) {
                            Text(
                              text = "WakeLock Active",
                              fontSize = 9.sp,
                              color = Color(0xFFE65100),
                              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                          }
                        }
                      }

                      Spacer(modifier = Modifier.height(4.dp))

                      app.drainFactors.forEach { factor ->
                        Text("• $factor", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }

                      Spacer(modifier = Modifier.height(6.dp))

                      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                          onClick = { onOpenBatteryOptimization(app.packageName) },
                          modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                          Text("Restrict Battery", fontSize = 10.sp)
                        }

                        OutlinedButton(
                          onClick = { onOpenAppDetails(app.packageName) },
                          modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                          Text("Force Stop", fontSize = 10.sp, color = RiskHigh)
                        }
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              OutlinedButton(
                onClick = {
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                  } else {
                    onScanAndAlertDrain()
                    Toast.makeText(context, "Scanning tasks and triggering battery notifications...", Toast.LENGTH_SHORT).show()
                  }
                },
                modifier = Modifier.fillMaxWidth().testTag("scan_and_alert_button")
              ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan & Send Alert Notification", fontSize = 11.sp)
              }

              onSyncBatteryToCloud?.let { syncAction ->
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                  onClick = syncAction,
                  modifier = Modifier.fillMaxWidth().testTag("sync_battery_cloud_button"),
                  colors = ButtonDefaults.buttonColors(containerColor = EmeraldTertiary)
                ) {
                  Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Backup Snapshot to Cloud (Firestore)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
        }
      }
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Android, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Android System Specifications",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            DiagRow("OS Release", diag.androidVersion)
            DiagRow("API SDK Level", "${diag.sdkInt}")
            DiagRow("Security Patch", diag.securityPatch)
            DiagRow("Device Model", diag.deviceModel)
            DiagRow("Manufacturer", diag.manufacturer)
          }
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
private fun DiagRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = label,
      fontSize = 12.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

private fun formatGigabytes(bytes: Long): String {
  val gb = bytes / (1024.0 * 1024.0 * 1024.0)
  return String.format("%.1f GB", gb)
}
