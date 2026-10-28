package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.PackageInfoCompat
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.RiskHigh
import com.example.ui.theme.RiskMedium
import com.example.ui.theme.RiskSafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Model representing a single permission requested by an application.
 */
data class AppRequestedPermission(
  val fullName: String,
  val simpleName: String,
  val isGranted: Boolean,
  val isDangerous: Boolean,
  val group: String,
  val protectionLevel: String
)

/**
 * Model representing an installed application with its complete list of requested permissions.
 */
data class InstalledAppWithPermissions(
  val appName: String,
  val packageName: String,
  val versionName: String,
  val versionCode: Long,
  val isSystemApp: Boolean,
  val targetSdk: Int,
  val permissions: List<AppRequestedPermission>,
  val totalPermissions: Int = permissions.size,
  val grantedPermissions: Int = permissions.count { it.isGranted },
  val deniedPermissions: Int = permissions.count { !it.isGranted },
  val dangerousPermissions: Int = permissions.count { it.isDangerous }
)

enum class AppPermissionsFilter(val label: String) {
  ALL("All Apps"),
  USER_ONLY("User Apps"),
  SYSTEM_ONLY("System Apps"),
  HAS_DANGEROUS("Dangerous"),
  REQUESTS_CAMERA("Camera"),
  REQUESTS_LOCATION("Location"),
  REQUESTS_MIC("Microphone"),
  REQUESTS_STORAGE("Storage")
}

/**
 * Queries Android's PackageManager API to retrieve all installed packages and their requested permissions.
 */
suspend fun queryInstalledAppsAndPermissions(context: Context): List<InstalledAppWithPermissions> =
  withContext(Dispatchers.IO) {
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

    val appList = mutableListOf<InstalledAppWithPermissions>()

    for (pkg in packageInfos) {
      val appInfo = pkg.applicationInfo ?: continue
      val appName = runCatching { pm.getApplicationLabel(appInfo).toString() }.getOrDefault(pkg.packageName)
      val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

      val requestedPerms = pkg.requestedPermissions ?: emptyArray()
      val flagsArray = pkg.requestedPermissionsFlags ?: IntArray(0)

      val permissionsList = mutableListOf<AppRequestedPermission>()

      for (i in requestedPerms.indices) {
        val permName = requestedPerms[i]

        val flagGranted = if (i < flagsArray.size) {
          (flagsArray[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
        } else false

        val checkGranted = runCatching {
          pm.checkPermission(permName, pkg.packageName) == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

        val isGranted = flagGranted || checkGranted

        // Determine group and danger
        val isDangerous = permName.contains("CAMERA", ignoreCase = true) ||
          permName.contains("LOCATION", ignoreCase = true) ||
          permName.contains("RECORD_AUDIO", ignoreCase = true) ||
          permName.contains("READ_CONTACTS", ignoreCase = true) ||
          permName.contains("WRITE_CONTACTS", ignoreCase = true) ||
          permName.contains("READ_SMS", ignoreCase = true) ||
          permName.contains("SEND_SMS", ignoreCase = true) ||
          permName.contains("READ_CALL_LOG", ignoreCase = true) ||
          permName.contains("READ_EXTERNAL_STORAGE", ignoreCase = true) ||
          permName.contains("WRITE_EXTERNAL_STORAGE", ignoreCase = true) ||
          permName.contains("MANAGE_EXTERNAL_STORAGE", ignoreCase = true) ||
          permName.contains("REQUEST_INSTALL_PACKAGES", ignoreCase = true)

        val group = when {
          permName.contains("CAMERA", ignoreCase = true) -> "Camera"
          permName.contains("LOCATION", ignoreCase = true) -> "Location"
          permName.contains("AUDIO", ignoreCase = true) || permName.contains("MIC", ignoreCase = true) -> "Microphone"
          permName.contains("STORAGE", ignoreCase = true) || permName.contains("MEDIA", ignoreCase = true) -> "Storage"
          permName.contains("SMS", ignoreCase = true) -> "SMS"
          permName.contains("CONTACTS", ignoreCase = true) -> "Contacts"
          permName.contains("PHONE", ignoreCase = true) || permName.contains("CALL", ignoreCase = true) -> "Phone"
          permName.contains("NOTIFICATION", ignoreCase = true) -> "Notifications"
          permName.contains("BLUETOOTH", ignoreCase = true) -> "Bluetooth"
          else -> "General / System"
        }

        val protection = if (isDangerous) "Dangerous (Runtime)" else "Normal (Install-time)"

        permissionsList.add(
          AppRequestedPermission(
            fullName = permName,
            simpleName = permName.substringAfterLast('.'),
            isGranted = isGranted,
            isDangerous = isDangerous,
            group = group,
            protectionLevel = protection
          )
        )
      }

      val sortedPermissions = permissionsList.sortedWith(
        compareByDescending<AppRequestedPermission> { it.isDangerous }
          .thenByDescending { it.isGranted }
          .thenBy { it.simpleName }
      )

      appList.add(
        InstalledAppWithPermissions(
          appName = appName,
          packageName = pkg.packageName,
          versionName = pkg.versionName ?: "1.0",
          versionCode = PackageInfoCompat.getLongVersionCode(pkg),
          isSystemApp = isSystem,
          targetSdk = appInfo.targetSdkVersion,
          permissions = sortedPermissions
        )
      )
    }

    appList.sortedWith(
      compareByDescending<InstalledAppWithPermissions> { it.dangerousPermissions }
        .thenByDescending { it.totalPermissions }
        .thenBy { it.isSystemApp }
        .thenBy { it.appName.lowercase() }
    )
  }

/**
 * Screen that displays all installed applications on the device and their requested permissions
 * queried directly using Android's PackageManager API.
 */
@Composable
fun InstalledAppsPermissionsScreen(
  onAppClick: ((InstalledAppWithPermissions) -> Unit)? = null,
  onOpenAppSettings: ((String) -> Unit)? = null,
  showHeader: Boolean = true,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val clipboardManager = LocalClipboardManager.current

  var allApps by remember { mutableStateOf<List<InstalledAppWithPermissions>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableStateOf(AppPermissionsFilter.ALL) }
  var expandedPackageNames by remember { mutableStateOf<Set<String>>(emptySet()) }

  fun loadApps() {
    scope.launch {
      isLoading = true
      allApps = queryInstalledAppsAndPermissions(context)
      isLoading = false
    }
  }

  LaunchedEffect(Unit) {
    loadApps()
  }

  val filteredApps = remember(allApps, searchQuery, selectedFilter) {
    allApps.filter { app ->
      val matchesSearch = searchQuery.isBlank() ||
        app.appName.contains(searchQuery, ignoreCase = true) ||
        app.packageName.contains(searchQuery, ignoreCase = true) ||
        app.permissions.any {
          it.fullName.contains(searchQuery, ignoreCase = true) ||
            it.simpleName.contains(searchQuery, ignoreCase = true) ||
            it.group.contains(searchQuery, ignoreCase = true)
        }

      val matchesFilter = when (selectedFilter) {
        AppPermissionsFilter.ALL -> true
        AppPermissionsFilter.USER_ONLY -> !app.isSystemApp
        AppPermissionsFilter.SYSTEM_ONLY -> app.isSystemApp
        AppPermissionsFilter.HAS_DANGEROUS -> app.dangerousPermissions > 0
        AppPermissionsFilter.REQUESTS_CAMERA -> app.permissions.any { it.group == "Camera" }
        AppPermissionsFilter.REQUESTS_LOCATION -> app.permissions.any { it.group == "Location" }
        AppPermissionsFilter.REQUESTS_MIC -> app.permissions.any { it.group == "Microphone" }
        AppPermissionsFilter.REQUESTS_STORAGE -> app.permissions.any { it.group == "Storage" }
      }

      matchesSearch && matchesFilter
    }
  }

  val totalAppsCount = allApps.size
  val totalPermissionsCount = remember(allApps) { allApps.sumOf { it.totalPermissions } }
  val totalDangerousCount = remember(allApps) { allApps.sumOf { it.dangerousPermissions } }
  val totalGrantedCount = remember(allApps) { allApps.sumOf { it.grantedPermissions } }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .testTag("installed_apps_permissions_screen")
  ) {
    if (showHeader) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "PackageManager Permissions",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Inspecting declared & runtime permissions across all installed packages",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        IconButton(
          onClick = { loadApps() },
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .testTag("refresh_permissions_button")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
        }
      }
    }

    // Telemetry metric cards
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 10.dp)
    ) {
      MetricBadgeCard(
        label = "Total Apps",
        value = totalAppsCount.toString(),
        icon = Icons.Default.Android,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.weight(1f)
      )
      MetricBadgeCard(
        label = "Requested Perms",
        value = totalPermissionsCount.toString(),
        icon = Icons.Default.Security,
        tint = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.weight(1f)
      )
      MetricBadgeCard(
        label = "Dangerous",
        value = totalDangerousCount.toString(),
        icon = Icons.Default.Warning,
        tint = RiskHigh,
        modifier = Modifier.weight(1f)
      )
      MetricBadgeCard(
        label = "Granted",
        value = totalGrantedCount.toString(),
        icon = Icons.Default.CheckCircle,
        tint = RiskSafe,
        modifier = Modifier.weight(1f)
      )
    }

    // Search bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("Search apps or permissions (e.g. CAMERA, Location)…", fontSize = 13.sp) },
      leadingIcon = {
        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      },
      trailingIcon = {
        if (searchQuery.isNotBlank()) {
          IconButton(onClick = { searchQuery = "" }) {
            Icon(Icons.Default.Close, contentDescription = "Clear")
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("permissions_search_field")
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Filter Chips
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(AppPermissionsFilter.values()) { filter ->
        FilterChip(
          selected = selectedFilter == filter,
          onClick = { selectedFilter = filter },
          label = { Text(filter.label, fontSize = 12.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
          ),
          modifier = Modifier.testTag("filter_chip_${filter.name}")
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (isLoading) {
      Box(
        modifier = Modifier.fillMaxSize().weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Scanning PackageManager for installed packages & permissions…",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else if (filteredApps.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "No applications match your filter.",
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize().weight(1f).testTag("installed_apps_permissions_list")
      ) {
        items(filteredApps, key = { it.packageName }) { app ->
          val isExpanded = expandedPackageNames.contains(app.packageName)

          AppPermissionCard(
            app = app,
            isExpanded = isExpanded,
            onToggleExpand = {
              expandedPackageNames = if (isExpanded) {
                expandedPackageNames - app.packageName
              } else {
                expandedPackageNames + app.packageName
              }
            },
            onOpenSettings = {
              if (onOpenAppSettings != null) {
                onOpenAppSettings(app.packageName)
              } else {
                runCatching {
                  val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${app.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                  }
                  context.startActivity(intent)
                }.onFailure {
                  Toast.makeText(context, "Unable to open system settings for ${app.appName}", Toast.LENGTH_SHORT).show()
                }
              }
            },
            onCopyPermission = { perm ->
              clipboardManager.setText(AnnotatedString(perm))
              Toast.makeText(context, "Copied: $perm", Toast.LENGTH_SHORT).show()
            }
          )
        }
      }
    }
  }
}

@Composable
fun AppPermissionCard(
  app: InstalledAppWithPermissions,
  isExpanded: Boolean,
  onToggleExpand: () -> Unit,
  onOpenSettings: () -> Unit,
  onCopyPermission: (String) -> Unit
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("app_permission_card_${app.packageName}")
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onToggleExpand() }
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (app.dangerousPermissions > 0) Icons.Default.Shield else Icons.Default.Android,
            contentDescription = null,
            tint = if (app.dangerousPermissions > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = app.appName,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f, fill = false)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (app.isSystemApp) MaterialTheme.colorScheme.surfaceVariant else EmeraldTertiary.copy(alpha = 0.15f)
            ) {
              Text(
                text = if (app.isSystemApp) "SYS" else "USER",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (app.isSystemApp) MaterialTheme.colorScheme.onSurfaceVariant else EmeraldTertiary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Text(
            text = app.packageName,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = "v${app.versionName} • Target SDK ${app.targetSdk}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary
          )
        }

        IconButton(
          onClick = onToggleExpand,
          modifier = Modifier.size(36.dp).testTag("toggle_expand_${app.packageName}")
        ) {
          Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Permission metrics summary chips
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
          Text(
            text = "${app.totalPermissions} Perms",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
          )
        }

        if (app.dangerousPermissions > 0) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = RiskHigh.copy(alpha = 0.15f)
          ) {
            Text(
              text = "${app.dangerousPermissions} Dangerous",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = RiskHigh,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = RiskSafe.copy(alpha = 0.15f)
        ) {
          Text(
            text = "${app.grantedPermissions} Granted",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = RiskSafe,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
          )
        }

        if (app.deniedPermissions > 0) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
          ) {
            Text(
              text = "${app.deniedPermissions} Denied",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
          onClick = onOpenSettings,
          modifier = Modifier.size(28.dp).testTag("open_settings_${app.packageName}")
        ) {
          Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "App Settings",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      // Quick Capability Group Icons
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 6.dp)
      ) {
        if (app.permissions.any { it.group == "Camera" }) {
          CapabilityIconBadge(icon = Icons.Default.CameraAlt, label = "Camera")
        }
        if (app.permissions.any { it.group == "Location" }) {
          CapabilityIconBadge(icon = Icons.Default.LocationOn, label = "Location")
        }
        if (app.permissions.any { it.group == "Microphone" }) {
          CapabilityIconBadge(icon = Icons.Default.Mic, label = "Microphone")
        }
        if (app.permissions.any { it.group == "Storage" }) {
          CapabilityIconBadge(icon = Icons.Default.Folder, label = "Storage")
        }
        if (app.permissions.any { it.group == "Notifications" }) {
          CapabilityIconBadge(icon = Icons.Default.Notifications, label = "Notifications")
        }
      }

      // Expanded permissions breakdown
      AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        Column(modifier = Modifier.padding(top = 12.dp)) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "Requested Permissions (${app.permissions.size}):",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(6.dp))

          if (app.permissions.isEmpty()) {
            Text(
              text = "No permissions requested in AndroidManifest.",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              app.permissions.forEach { perm ->
                PermissionRowItem(perm = perm, onCopy = { onCopyPermission(perm.fullName) })
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedButton(
            onClick = onOpenSettings,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.fillMaxWidth().testTag("manage_in_settings_${app.packageName}")
          ) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Manage Permissions in System Settings", fontSize = 12.sp)
          }
        }
      }
    }
  }
}

@Composable
fun PermissionRowItem(
  perm: AppRequestedPermission,
  onCopy: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
      Icon(
        imageVector = if (perm.isGranted) Icons.Default.CheckCircle else Icons.Default.Close,
        contentDescription = null,
        tint = if (perm.isGranted) RiskSafe else MaterialTheme.colorScheme.outline,
        modifier = Modifier.size(16.dp)
      )

      Spacer(modifier = Modifier.width(8.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = perm.simpleName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (perm.isDangerous) RiskHigh.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
          ) {
            Text(
              text = if (perm.isDangerous) "DANGEROUS" else "NORMAL",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = if (perm.isDangerous) RiskHigh else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
          }
        }
        Text(
          text = perm.fullName,
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.width(6.dp))

      Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (perm.isGranted) RiskSafe.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
      ) {
        Text(
          text = if (perm.isGranted) "GRANTED" else "DENIED",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = if (perm.isGranted) RiskSafe else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
      }

      IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(13.dp))
      }
    }
  }
}

@Composable
fun CapabilityIconBadge(icon: ImageVector, label: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
      .padding(horizontal = 6.dp, vertical = 2.dp)
  ) {
    Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
    Spacer(modifier = Modifier.width(4.dp))
    Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Medium)
  }
}

@Composable
fun MetricBadgeCard(
  label: String,
  value: String,
  icon: ImageVector,
  tint: Color,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = tint)
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
  }
}
