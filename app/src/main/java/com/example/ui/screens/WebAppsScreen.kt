package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CloneType
import com.example.model.ClonedAppItem
import com.example.model.SavedWebApp
import com.example.model.WebAppCategory
import com.example.model.WebAppNotificationEvent
import com.example.model.WebNotificationType
import com.example.ui.components.CloneAppDialog
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.RiskHigh
import com.example.ui.theme.RiskSafe
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WebAppsScreen(
  savedApps: List<SavedWebApp>,
  activeApp: SavedWebApp?,
  isBackgroundActive: Boolean,
  notificationHistory: List<WebAppNotificationEvent>,
  clonedApps: List<ClonedAppItem> = emptyList(),
  activeClonedApp: ClonedAppItem? = null,
  onOpenApp: (SavedWebApp) -> Unit,
  onCloseApp: () -> Unit,
  onAddApp: (name: String, url: String, category: WebAppCategory, keepAlive: Boolean, desktopMode: Boolean) -> Unit,
  onDeleteApp: (String) -> Unit,
  onToggleBackgroundService: (Boolean) -> Unit,
  onTriggerNotification: (appName: String, title: String, body: String, type: WebNotificationType) -> Unit,
  onOpenClone: (ClonedAppItem) -> Unit = {},
  onCloseClone: () -> Unit = {},
  onCloneWebApp: (SavedWebApp, String, String, String, Long) -> Unit = { _, _, _, _, _ -> },
  onDeleteClone: (String) -> Unit = {},
  onClearCloneData: (ClonedAppItem) -> Unit = {},
  onLaunchNativeClone: (ClonedAppItem) -> Unit = {},
  onDownloadRequested: ((url: String, fileName: String?) -> Unit)? = null
) {
  val context = LocalContext.current
  var showAddDialog by remember { mutableStateOf(false) }
  var appToCloneForDialog by remember { mutableStateOf<SavedWebApp?>(null) }
  var selectedCategoryFilter by remember { mutableStateOf<WebAppCategory?>(null) }
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Dual Space Cloner, 1 = Web Portals, 2 = Alerts

  // Permission launcher for POST_NOTIFICATIONS & RECORD_AUDIO
  val audioPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val notifGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: true
    val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: true
    if (notifGranted && audioGranted) {
      Toast.makeText(context, "Permissions enabled for calls & notifications", Toast.LENGTH_SHORT).show()
    }
  }

  // Active Cloned App Viewer
  if (activeClonedApp != null) {
    ClonedAppViewer(
      clone = activeClonedApp,
      onClose = onCloseClone,
      onClearStorage = onClearCloneData
    )
    return
  }

  // Active Web App Viewer
  if (activeApp != null) {
    EmbeddedWebView(
      webApp = activeApp,
      isBackgroundActive = isBackgroundActive,
      onClose = onCloseApp,
      onToggleBackground = onToggleBackgroundService,
      onNotificationReceived = onTriggerNotification,
      onDownloadRequested = onDownloadRequested
    )
    return
  }

  // Main Dual Space & Web Hub Dashboard
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(bottom = 24.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(8.dp))

      // Header Banner: App Cloner & Dual Space Hub
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(CircleShape)
                  .background(EmeraldTertiary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.Layers,
                  contentDescription = null,
                  tint = EmeraldTertiary,
                  modifier = Modifier.size(24.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Apps Cloner & Dual Space",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "অ্যাপস ক্লোনার • মাল্টি অ্যাকাউন্ট ও ডুয়েল স্পেস",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // 24/7 Background Keep-Alive Master Toggle
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (isBackgroundActive) EmeraldTertiary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "Background Keep-Alive Service",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  if (isBackgroundActive) {
                    Surface(
                      shape = RoundedCornerShape(10.dp),
                      color = EmeraldTertiary.copy(alpha = 0.2f)
                    ) {
                      Text(
                        text = "ACTIVE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldTertiary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }
                }
                Text(
                  text = "Keeps clones & web applications active 24/7 so background calls and alerts fire instantly.",
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(top = 2.dp)
                )
              }

              Switch(
                checked = isBackgroundActive,
                onCheckedChange = { checked ->
                  if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    audioPermissionLauncher.launch(
                      arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.RECORD_AUDIO
                      )
                    )
                  }
                  onToggleBackgroundService(checked)
                  Toast.makeText(
                    context,
                    if (checked) "24/7 Keep-Alive Started" else "Background Service Stopped",
                    Toast.LENGTH_SHORT
                  ).show()
                },
                modifier = Modifier.testTag("background_service_switch")
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Top Sub-Tabs: 0 = Cloner (ডুয়েল স্পেস), 1 = Web Portals, 2 = Alerts Log
          TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
          ) {
            Tab(
              selected = selectedTab == 0,
              onClick = { selectedTab = 0 },
              text = { Text("Clones (${clonedApps.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
              selected = selectedTab == 1,
              onClick = { selectedTab = 1 },
              text = { Text("Web Apps (${savedApps.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
            )
            Tab(
              selected = selectedTab == 2,
              onClick = { selectedTab = 2 },
              text = { Text("Alerts (${notificationHistory.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
    }

    // TAB 0: APP CLONER & DUAL SPACE
    if (selectedTab == 0) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Active Cloned Instances (ক্লোন করা অ্যাপস)",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Parallel dual accounts with isolated cookies & data",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Button(
            onClick = {
              // Clone the featured Rubel AI Studio or prompt
              val defaultTarget = savedApps.firstOrNull()
              if (defaultTarget != null) {
                appToCloneForDialog = defaultTarget
              } else {
                showAddDialog = true
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldTertiary),
            modifier = Modifier.testTag("create_clone_button")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Clone App", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
      }

      if (clonedApps.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text("👥", fontSize = 32.sp)
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "No cloned applications yet.",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Clone Rubel AI Studio or any installed app to manage dual accounts with isolated data.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
              )
            }
          }
        }
      } else {
        items(clonedApps, key = { it.id }) { clone ->
          ClonedAppCard(
            clone = clone,
            onLaunch = {
              if (clone.cloneType == CloneType.WEB_APP_DUAL_SPACE) {
                onOpenClone(clone)
              } else {
                onLaunchNativeClone(clone)
              }
            },
            onDelete = {
              onDeleteClone(clone.id)
              Toast.makeText(context, "Deleted clone: ${clone.cloneName}", Toast.LENGTH_SHORT).show()
            },
            onClearData = {
              onClearCloneData(clone)
              Toast.makeText(context, "Reset data for ${clone.cloneName}", Toast.LENGTH_SHORT).show()
            }
          )
          Spacer(modifier = Modifier.height(10.dp))
        }
      }
    }

    // TAB 1: SAVED WEB APPS
    if (selectedTab == 1) {
      // Featured Hero App Card: Rubel AI Coding Studio
      savedApps.firstOrNull { it.url.contains("rubel-ai-assistant") }?.let { rubelApp ->
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("rubel_app_hero_card")
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(42.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text("🤖", fontSize = 22.sp)
                    }
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(
                      text = rubelApp.name,
                      fontWeight = FontWeight.Bold,
                      fontSize = 15.sp,
                      color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                      text = "Featured AI & Coding Platform",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                  }
                }

                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = EmeraldTertiary.copy(alpha = 0.2f)
                ) {
                  Text(
                    text = "ORIGINAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldTertiary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = rubelApp.url,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )

              Spacer(modifier = Modifier.height(12.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Button(
                  onClick = { onOpenApp(rubelApp) },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("launch_rubel_studio_button"),
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                  Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Open Original", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                  onClick = { appToCloneForDialog = rubelApp },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("clone_rubel_studio_button")
                ) {
                  Icon(Icons.Default.Layers, contentDescription = null, tint = EmeraldTertiary, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Clone Work Acc", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EmeraldTertiary)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
        }
      }

      // Category Filter Chips & Add Button
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Saved Web Applications (${savedApps.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.testTag("add_web_app_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add URL", fontSize = 11.sp)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          item {
            FilterChip(
              selected = selectedCategoryFilter == null,
              onClick = { selectedCategoryFilter = null },
              label = { Text("All (${savedApps.size})") }
            )
          }
          items(WebAppCategory.entries.toTypedArray()) { cat ->
            val count = savedApps.count { it.category == cat }
            FilterChip(
              selected = selectedCategoryFilter == cat,
              onClick = {
                selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
              },
              label = { Text("${cat.iconEmoji} ${cat.title} ($count)") }
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
      }

      val filteredApps = savedApps.filter {
        selectedCategoryFilter == null || it.category == selectedCategoryFilter
      }

      if (filteredApps.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "No web applications found in this category.\nTap '+ Add URL' to save one.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
        }
      } else {
        items(filteredApps, key = { it.id }) { app ->
          SavedWebAppCardWithClone(
            app = app,
            onLaunch = { onOpenApp(app) },
            onClone = { appToCloneForDialog = app },
            onDelete = { onDeleteApp(app.id) },
            onTestAlert = {
              onTriggerNotification(
                app.name,
                "Alert from ${app.name}",
                "Incoming notification from ${app.name}",
                if (app.category == WebAppCategory.VOICE_CALL) WebNotificationType.INCOMING_CALL else WebNotificationType.INCOMING_MESSAGE
              )
              Toast.makeText(context, "Alert triggered for ${app.name}", Toast.LENGTH_SHORT).show()
            }
          )
          Spacer(modifier = Modifier.height(10.dp))
        }
      }
    }

    // TAB 2: ALERTS LOG
    if (selectedTab == 2) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Recent Alerts & Background Notifications",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          OutlinedButton(
            onClick = {
              onTriggerNotification(
                "Rubel AI Studio",
                "📞 Incoming Web Call Alert",
                "Call alert detected from Rubel AI Studio assistant",
                WebNotificationType.INCOMING_CALL
              )
              Toast.makeText(context, "Test Call Notification Triggered!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.testTag("test_call_alert_button")
          ) {
            Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = EmeraldTertiary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Test Alert", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
      }

      if (notificationHistory.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
              Text("No background alerts recorded yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      } else {
        items(notificationHistory.take(12)) { notif ->
          val isCall = notif.type == WebNotificationType.INCOMING_CALL
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (isCall) RiskHigh.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
          ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (isCall) Icons.Default.PhoneInTalk else Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = if (isCall) RiskHigh else EmeraldTertiary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text(text = notif.appName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                  Text(
                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(notif.timestamp)),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                Text(text = "${notif.title} • ${notif.body}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
              }
            }
          }
        }
      }
    }
  }

  // Add Web App Modal Dialog
  if (showAddDialog) {
    AddWebAppDialog(
      onDismiss = { showAddDialog = false },
      onConfirm = { name, url, category, keepAlive, desktopMode ->
        onAddApp(name, url, category, keepAlive, desktopMode)
        showAddDialog = false
        Toast.makeText(context, "Saved web app: $name", Toast.LENGTH_SHORT).show()
      }
    )
  }

  // Clone App Customizer Modal Dialog
  appToCloneForDialog?.let { targetApp ->
    CloneAppDialog(
      originalAppName = targetApp.name,
      defaultBadge = "Work",
      onDismiss = { appToCloneForDialog = null },
      onConfirm = { cloneName, cloneBadge, emoji, colorTintHex ->
        onCloneWebApp(targetApp, cloneName, cloneBadge, emoji, colorTintHex)
        appToCloneForDialog = null
        selectedTab = 0 // Switch to Clones tab to see the new clone
        Toast.makeText(context, "Created clone: $cloneName", Toast.LENGTH_SHORT).show()
      }
    )
  }
}

/**
 * Card displaying an individual Cloned Application with its isolated badge and actions.
 */
@Composable
fun ClonedAppCard(
  clone: ClonedAppItem,
  onLaunch: () -> Unit,
  onDelete: () -> Unit,
  onClearData: () -> Unit
) {
  val accentColor = Color(clone.colorTintHex)

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier.fillMaxWidth().testTag("cloned_app_card_${clone.id.take(6)}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Surface(
            shape = CircleShape,
            color = accentColor.copy(alpha = 0.2f),
            modifier = Modifier.size(40.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(clone.iconEmoji, fontSize = 20.sp)
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = clone.cloneName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Text(
              text = if (clone.cloneType == CloneType.WEB_APP_DUAL_SPACE) "Dual Space • Isolated Cookies" else "Native Cloned APK",
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = accentColor
        ) {
          Text(
            text = clone.cloneBadge.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Launches: ${clone.launchCount}",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onClearData,
            modifier = Modifier.size(32.dp).testTag("clone_clear_${clone.id.take(6)}")
          ) {
            Icon(Icons.Default.DeleteSweep, contentDescription = "Wipe Data", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
          }

          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp).testTag("clone_delete_${clone.id.take(6)}")
          ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Clone", tint = RiskHigh, modifier = Modifier.size(18.dp))
          }

          Button(
            onClick = onLaunch,
            modifier = Modifier.height(34.dp).testTag("clone_launch_${clone.id.take(6)}"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Open Clone", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

/**
 * Saved Web App Card with direct Clone button.
 */
@Composable
fun SavedWebAppCardWithClone(
  app: SavedWebApp,
  onLaunch: () -> Unit,
  onClone: () -> Unit,
  onDelete: () -> Unit,
  onTestAlert: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier.fillMaxWidth().testTag("saved_web_app_card_${app.id.take(6)}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(38.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(app.iconEmoji, fontSize = 20.sp)
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = app.name,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = app.url,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = MaterialTheme.colorScheme.secondaryContainer
        ) {
          Text(
            text = app.category.title,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          if (app.keepAliveInBackground) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = EmeraldTertiary.copy(alpha = 0.15f)
            ) {
              Text(
                text = "24/7 Keep-Alive",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldTertiary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Clone action
          IconButton(
            onClick = onClone,
            modifier = Modifier.size(32.dp).testTag("card_clone_${app.id.take(6)}")
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Clone App", tint = EmeraldTertiary, modifier = Modifier.size(18.dp))
          }

          IconButton(
            onClick = onTestAlert,
            modifier = Modifier.size(32.dp).testTag("card_test_alert_${app.id.take(6)}")
          ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = "Test Notification", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
          }

          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp).testTag("card_delete_${app.id.take(6)}")
          ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Web App", tint = RiskHigh, modifier = Modifier.size(18.dp))
          }

          Button(
            onClick = onLaunch,
            modifier = Modifier.height(34.dp).testTag("card_launch_${app.id.take(6)}"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
          ) {
            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Open", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun AddWebAppDialog(
  onDismiss: () -> Unit,
  onConfirm: (name: String, url: String, category: WebAppCategory, keepAlive: Boolean, desktopMode: Boolean) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var url by remember { mutableStateOf("https://") }
  var selectedCategory by remember { mutableStateOf(WebAppCategory.AI_ASSISTANT) }
  var keepAlive by remember { mutableStateOf(true) }
  var desktopMode by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Add Web App / Portal", fontWeight = FontWeight.Bold)
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("App Name") },
          placeholder = { Text("e.g. Rubel AI Assistant, My Chat") },
          modifier = Modifier.fillMaxWidth().testTag("add_app_name_input"),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = url,
          onValueChange = { url = it },
          label = { Text("Web URL / Link") },
          placeholder = { Text("https://example.com") },
          modifier = Modifier.fillMaxWidth().testTag("add_app_url_input"),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Select Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(WebAppCategory.entries.toTypedArray()) { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text("${cat.iconEmoji} ${cat.title}", fontSize = 11.sp) }
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = keepAlive, onCheckedChange = { keepAlive = it })
          Text("Keep alive in background for 24/7 alerts", fontSize = 11.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = desktopMode, onCheckedChange = { desktopMode = it })
          Text("Request Desktop Version", fontSize = 11.sp)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (url.isNotBlank() && url != "https://" && url != "http://") {
            onConfirm(name, url, selectedCategory, keepAlive, desktopMode)
          }
        },
        enabled = url.isNotBlank() && url != "https://" && url != "http://",
        modifier = Modifier.testTag("dialog_confirm_add")
      ) {
        Text("Save & Add")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
