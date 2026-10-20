package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ContentCopy
import com.example.manager.RemoteServerState
import android.widget.Toast

@Composable
fun DiagnosticsScreen(
  diagnostic: SystemDiagnostic?,
  remoteServerState: RemoteServerState,
  onRefresh: () -> Unit,
  onClearCache: () -> Unit,
  onToggleRemoteServer: (Boolean) -> Unit,
  onPlayFindDeviceSound: () -> Unit
) {
  val context = LocalContext.current

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

      // Android Environment Specs
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
