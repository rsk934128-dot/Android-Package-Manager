package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppPackageItem
import com.example.model.ThreatLevel
import com.example.ui.theme.RiskHigh
import com.example.ui.theme.RiskMedium
import com.example.ui.theme.RiskSafe
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailSheet(
  app: AppPackageItem,
  onDismiss: () -> Unit,
  onLaunch: (String) -> Unit,
  onOpenSettings: (String) -> Unit,
  onUninstall: (String) -> Unit,
  onClone: ((AppPackageItem) -> Unit)? = null,
  onBackup: ((AppPackageItem) -> Unit)? = null,
  onInspectPermissions: ((AppPackageItem) -> Unit)? = null
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("app_detail_sheet")
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
      item {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(
            modifier = Modifier
              .size(54.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = "App Icon",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp)
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = app.appName,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = app.packageName,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "v${app.versionName} (Build ${app.versionCode}) • Target SDK ${app.targetSdk}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Threat Score Banner
        val (badgeColor, badgeText) = when (app.threatLevel) {
          ThreatLevel.HIGH -> Pair(RiskHigh, "High Risk Package (Dropper/Monitor Risk)")
          ThreatLevel.MEDIUM -> Pair(RiskMedium, "Elevated Permissions (Audit Recommended)")
          ThreatLevel.SAFE -> Pair(RiskSafe, "Verified Clean Package")
        }

        Card(
          colors = CardDefaults.cardColors(containerColor = badgeColor.copy(alpha = 0.15f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
          ) {
            Icon(
              imageVector = if (app.threatLevel == ThreatLevel.SAFE) Icons.Default.Security else Icons.Default.Warning,
              contentDescription = null,
              tint = badgeColor,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = badgeText,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = badgeColor
              )
              Text(
                text = "Threat Audit Score: ${app.threatScore}/100",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        if (app.threatReasons.isNotEmpty()) {
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Risk Factors Detected:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          app.threatReasons.forEach { reason ->
            Text(
              text = "• $reason",
              style = MaterialTheme.typography.bodySmall,
              color = RiskHigh,
              modifier = Modifier.padding(vertical = 2.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = { onLaunch(app.packageName) },
            modifier = Modifier
              .weight(1f)
              .testTag("launch_app_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Open", color = MaterialTheme.colorScheme.onPrimaryContainer)
          }

          OutlinedButton(
            onClick = { onOpenSettings(app.packageName) },
            modifier = Modifier
              .weight(1f)
              .testTag("settings_app_button")
          ) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Details")
          }

          if (onClone != null) {
            FilledTonalButton(
              onClick = { onClone(app) },
              modifier = Modifier
                .weight(1f)
                .testTag("clone_app_detail_button")
            ) {
              Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Clone")
            }
          }

          if (!app.isSystemApp) {
            Button(
              onClick = { onUninstall(app.packageName) },
              colors = ButtonDefaults.buttonColors(containerColor = RiskHigh.copy(alpha = 0.2f)),
              modifier = Modifier.testTag("uninstall_app_button")
            ) {
              Icon(Icons.Default.Delete, contentDescription = null, tint = RiskHigh, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Uninstall", color = RiskHigh)
            }
          }
        }

        if (onBackup != null) {
          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = { onBackup(app) },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("backup_app_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
          ) {
            Icon(
              Icons.Default.Archive,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              "Backup APK to External Storage (Offline)",
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Info metadata
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "System Package: ${if (app.isSystemApp) "Yes (ROM)" else "No (User App)"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          if (app.firstInstallTime > 0) {
            Text(
              text = "Installed: ${dateFormat.format(Date(app.firstInstallTime))}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Declared Permissions (${app.permissions.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          if (onInspectPermissions != null) {
            FilledTonalButton(
              onClick = { onInspectPermissions(app) },
              modifier = Modifier.testTag("inspect_permissions_sheet_button")
            ) {
              Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Inspect Status", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
      }

      items(app.permissions) { perm ->
        Card(
          colors = CardDefaults.cardColors(
            containerColor = if (perm.isHighRisk) RiskHigh.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = perm.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (perm.isHighRisk) RiskHigh else MaterialTheme.colorScheme.onSurface
              )
              if (perm.isHighRisk) {
                Box(
                  modifier = Modifier
                    .clip(CircleShape)
                    .background(RiskHigh.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = "DANGEROUS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = RiskHigh
                  )
                }
              }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = perm.category,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = perm.explanation,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(30.dp))
      }
    }
  }
}
