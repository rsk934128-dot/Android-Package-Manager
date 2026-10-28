package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppPackageItem
import com.example.model.DetailedPermissionItem
import com.example.model.ThreatLevel
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.RiskHigh
import com.example.ui.theme.RiskMedium
import com.example.ui.theme.RiskSafe

enum class PermissionFilter {
  ALL,
  GRANTED,
  DENIED,
  DANGEROUS,
  NORMAL
}

/**
 * Screen that lists all permissions for a selected app and provides full visibility
 * into their real-time granted/denied status, protection levels, and security details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPermissionsScreen(
  app: AppPackageItem,
  allApps: List<AppPackageItem>,
  onBack: () -> Unit,
  onSelectDifferentApp: (AppPackageItem) -> Unit,
  onOpenSettings: (String) -> Unit,
  onLoadPermissions: (String) -> List<DetailedPermissionItem>
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current

  BackHandler { onBack() }

  var permissionsList by remember(app.packageName) { mutableStateOf<List<DetailedPermissionItem>>(emptyList()) }
  var isLoading by remember(app.packageName) { mutableStateOf(true) }
  var searchQuery by remember { mutableStateOf("") }
  var filter by remember { mutableStateOf(PermissionFilter.ALL) }

  var selectedPermissionForDetail by remember { mutableStateOf<DetailedPermissionItem?>(null) }
  var showAppPickerSheet by remember { mutableStateOf(false) }

  // Load permissions whenever app changes
  fun refresh() {
    isLoading = true
    permissionsList = onLoadPermissions(app.packageName)
    isLoading = false
  }

  LaunchedEffect(app.packageName) {
    refresh()
  }

  // Filter and Search logic
  val filteredPermissions = remember(permissionsList, searchQuery, filter) {
    permissionsList.filter { perm ->
      val matchesSearch = searchQuery.isBlank() ||
        perm.simpleName.contains(searchQuery, ignoreCase = true) ||
        perm.fullName.contains(searchQuery, ignoreCase = true) ||
        perm.label.contains(searchQuery, ignoreCase = true) ||
        perm.groupName.contains(searchQuery, ignoreCase = true) ||
        perm.description.contains(searchQuery, ignoreCase = true)

      val matchesFilter = when (filter) {
        PermissionFilter.ALL -> true
        PermissionFilter.GRANTED -> perm.isGranted
        PermissionFilter.DENIED -> !perm.isGranted
        PermissionFilter.DANGEROUS -> perm.isDangerous
        PermissionFilter.NORMAL -> !perm.isDangerous
      }

      matchesSearch && matchesFilter
    }
  }

  val totalCount = permissionsList.size
  val grantedCount = remember(permissionsList) { permissionsList.count { it.isGranted } }
  val deniedCount = remember(permissionsList) { permissionsList.count { !it.isGranted } }
  val dangerousCount = remember(permissionsList) { permissionsList.count { it.isDangerous } }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Permission Inspector",
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp
            )
            Text(
              text = app.appName,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("permissions_screen_back_button")
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(
            onClick = { showAppPickerSheet = true },
            modifier = Modifier.testTag("switch_app_button")
          ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = "Switch App", tint = MaterialTheme.colorScheme.primary)
          }

          IconButton(
            onClick = { onOpenSettings(app.packageName) },
            modifier = Modifier.testTag("manage_settings_top_button")
          ) {
            Icon(Icons.Default.Settings, contentDescription = "Open Settings", tint = MaterialTheme.colorScheme.primary)
          }

          IconButton(
            onClick = { refresh() },
            modifier = Modifier.testTag("refresh_permissions_button")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(6.dp))

      // Selected App Banner Card
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.size(46.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = app.appName.take(1).uppercase(),
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = app.appName,
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = if (app.isSystemApp) EmeraldTertiary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                  Text(
                    text = if (app.isSystemApp) "SYSTEM" else "USER APP",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (app.isSystemApp) EmeraldTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
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

              Text(
                text = "v${app.versionName} (Build ${app.versionCode}) • Target SDK ${app.targetSdk}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Quick Action: Manage in Android Settings
          Button(
            onClick = { onOpenSettings(app.packageName) },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("manage_app_permissions_settings_button")
          ) {
            Icon(
              Icons.Default.Settings,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Change Permissions in Android Settings",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Status Metrics Grid
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        PermissionMetricCard(
          label = "Total Declared",
          count = totalCount,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.weight(1f)
        )
        PermissionMetricCard(
          label = "Granted",
          count = grantedCount,
          tint = EmeraldTertiary,
          modifier = Modifier.weight(1f)
        )
        PermissionMetricCard(
          label = "Denied / Off",
          count = deniedCount,
          tint = if (deniedCount > 0) RiskHigh else MaterialTheme.colorScheme.outline,
          modifier = Modifier.weight(1f)
        )
        PermissionMetricCard(
          label = "Dangerous",
          count = dangerousCount,
          tint = if (dangerousCount > 0) RiskMedium else MaterialTheme.colorScheme.outline,
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Search field
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search permissions, labels, groups…", fontSize = 12.sp) },
        leadingIcon = {
          Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingIcon = {
          if (searchQuery.isNotBlank()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Close, contentDescription = "Clear search")
            }
          }
        },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("permission_search_input")
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Filter Chips
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        item {
          FilterChip(
            selected = filter == PermissionFilter.ALL,
            onClick = { filter = PermissionFilter.ALL },
            label = { Text("All ($totalCount)", fontSize = 11.sp) },
            modifier = Modifier.testTag("filter_perm_all")
          )
        }
        item {
          FilterChip(
            selected = filter == PermissionFilter.GRANTED,
            onClick = { filter = PermissionFilter.GRANTED },
            label = { Text("Granted ($grantedCount)", fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = EmeraldTertiary.copy(alpha = 0.2f),
              selectedLabelColor = EmeraldTertiary
            ),
            modifier = Modifier.testTag("filter_perm_granted")
          )
        }
        item {
          FilterChip(
            selected = filter == PermissionFilter.DENIED,
            onClick = { filter = PermissionFilter.DENIED },
            label = { Text("Denied ($deniedCount)", fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = RiskHigh.copy(alpha = 0.2f),
              selectedLabelColor = RiskHigh
            ),
            modifier = Modifier.testTag("filter_perm_denied")
          )
        }
        item {
          FilterChip(
            selected = filter == PermissionFilter.DANGEROUS,
            onClick = { filter = PermissionFilter.DANGEROUS },
            label = { Text("Dangerous ($dangerousCount)", fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = RiskMedium.copy(alpha = 0.2f),
              selectedLabelColor = RiskMedium
            ),
            modifier = Modifier.testTag("filter_perm_dangerous")
          )
        }
        item {
          FilterChip(
            selected = filter == PermissionFilter.NORMAL,
            onClick = { filter = PermissionFilter.NORMAL },
            label = { Text("Normal (${totalCount - dangerousCount})", fontSize = 11.sp) },
            modifier = Modifier.testTag("filter_perm_normal")
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Content List or Loading State
      if (isLoading) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Inspecting application permissions & status…",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else if (filteredPermissions.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
          ) {
            Icon(
              Icons.Default.Security,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.outline,
              modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = if (permissionsList.isEmpty()) {
                "This application does not declare any permissions."
              } else {
                "No permissions match current filter or \"$searchQuery\""
              },
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 13.sp
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .testTag("permissions_lazy_column"),
          contentPadding = PaddingValues(bottom = 24.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(
            items = filteredPermissions,
            key = { it.fullName }
          ) { item ->
            PermissionItemCard(
              item = item,
              onClick = { selectedPermissionForDetail = item },
              onCopy = {
                clipboardManager.setText(AnnotatedString(item.fullName))
                Toast.makeText(context, "Copied: ${item.simpleName}", Toast.LENGTH_SHORT).show()
              }
            )
          }
        }
      }
    }
  }

  // Permission Details Dialog
  selectedPermissionForDetail?.let { detail ->
    PermissionDetailDialog(
      item = detail,
      packageName = app.packageName,
      onDismiss = { selectedPermissionForDetail = null },
      onOpenSettings = {
        selectedPermissionForDetail = null
        onOpenSettings(app.packageName)
      },
      onCopy = {
        clipboardManager.setText(AnnotatedString(detail.fullName))
        Toast.makeText(context, "Copied identifier to clipboard", Toast.LENGTH_SHORT).show()
      }
    )
  }

  // App Switcher Bottom Sheet
  if (showAppPickerSheet) {
    AppPickerBottomSheet(
      allApps = allApps,
      currentPackageName = app.packageName,
      onDismiss = { showAppPickerSheet = false },
      onSelectApp = { selected ->
        showAppPickerSheet = false
        onSelectDifferentApp(selected)
      }
    )
  }
}

@Composable
fun PermissionMetricCard(
  label: String,
  count: Int,
  tint: Color,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
      Text(
        text = count.toString(),
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = tint
      )
      Text(
        text = label,
        fontSize = 9.sp,
        maxLines = 1,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
fun PermissionItemCard(
  item: DetailedPermissionItem,
  onClick: () -> Unit,
  onCopy: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("permission_card_${item.simpleName}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Status Indicator Pill (Granted vs Denied)
      Surface(
        shape = CircleShape,
        color = if (item.isGranted) EmeraldTertiary.copy(alpha = 0.15f) else RiskHigh.copy(alpha = 0.15f),
        modifier = Modifier.size(36.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = if (item.isGranted) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = if (item.isGranted) "Granted" else "Denied",
            tint = if (item.isGranted) EmeraldTertiary else RiskHigh,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Permission Info & Tags
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = item.label,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
          )

          // Status Badge
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (item.isGranted) EmeraldTertiary.copy(alpha = 0.18f) else RiskHigh.copy(alpha = 0.18f),
            modifier = Modifier.padding(start = 6.dp)
          ) {
            Text(
              text = if (item.isGranted) "GRANTED" else "NOT GRANTED",
              fontSize = 9.sp,
              fontWeight = FontWeight.ExtraBold,
              color = if (item.isGranted) EmeraldTertiary else RiskHigh,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = item.simpleName,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Group & Protection Level Badges
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
          ) {
            Text(
              text = item.groupName,
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
            )
          }

          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (item.isDangerous) RiskMedium.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
          ) {
            Text(
              text = item.protectionLevel,
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              color = if (item.isDangerous) RiskMedium else MaterialTheme.colorScheme.outline,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = item.description,
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.width(6.dp))

      // Copy Button
      IconButton(
        onClick = onCopy,
        modifier = Modifier.size(30.dp).testTag("copy_perm_${item.simpleName}")
      ) {
        Icon(
          Icons.Default.ContentCopy,
          contentDescription = "Copy Permission",
          tint = MaterialTheme.colorScheme.outline,
          modifier = Modifier.size(15.dp)
        )
      }
    }
  }
}

/**
 * Modal dialog presenting comprehensive details and security explanations for a specific permission.
 */
@Composable
fun PermissionDetailDialog(
  item: DetailedPermissionItem,
  packageName: String,
  onDismiss: () -> Unit,
  onOpenSettings: () -> Unit,
  onCopy: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
          shape = CircleShape,
          color = if (item.isGranted) EmeraldTertiary.copy(alpha = 0.15f) else RiskHigh.copy(alpha = 0.15f),
          modifier = Modifier.size(32.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (item.isGranted) Icons.Default.CheckCircle else Icons.Default.Cancel,
              contentDescription = null,
              tint = if (item.isGranted) EmeraldTertiary else RiskHigh,
              modifier = Modifier.size(18.dp)
            )
          }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(text = item.label, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text(text = item.simpleName, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
        }
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(10.dp))

        // Current Status Card
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (item.isGranted) EmeraldTertiary.copy(alpha = 0.12f) else RiskHigh.copy(alpha = 0.12f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(10.dp)
          ) {
            Icon(
              imageVector = if (item.isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
              contentDescription = null,
              tint = if (item.isGranted) EmeraldTertiary else RiskHigh,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = if (item.isGranted) "Status: GRANTED (Active)" else "Status: NOT GRANTED (Restricted)",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (item.isGranted) EmeraldTertiary else RiskHigh
              )
              Text(
                text = if (item.isGranted) {
                  "This application is currently authorized to use this capability."
                } else {
                  "The application requested this permission, but access is currently denied or not granted."
                },
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Full Permission Identifier
        Text(text = "System Identifier:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
            .clickable(onClick = onCopy)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
          ) {
            Text(
              text = item.fullName,
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.weight(1f)
            )
            Icon(
              Icons.Default.ContentCopy,
              contentDescription = "Copy",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Metadata Tags (Group & Protection)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Group", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
            Text(item.groupName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
          }
          Column(modifier = Modifier.weight(1f)) {
            Text("Protection", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
            Text(item.protectionLevel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Explanation & Recommendations
        Text(text = "Description & Purpose:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(
          text = item.description,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Security Recommendation:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(
          text = item.riskRecommendation,
          fontSize = 11.sp,
          color = if (item.isDangerous && item.isGranted) RiskHigh else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 2.dp)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onOpenSettings,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.testTag("dialog_open_settings_button")
      ) {
        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Modify in Settings", fontSize = 12.sp)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}

/**
 * Bottom sheet to pick any other installed app to inspect its permissions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerBottomSheet(
  allApps: List<AppPackageItem>,
  currentPackageName: String,
  onDismiss: () -> Unit,
  onSelectApp: (AppPackageItem) -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var search by remember { mutableStateOf("") }

  val filtered = remember(allApps, search) {
    allApps.filter {
      search.isBlank() ||
        it.appName.contains(search, ignoreCase = true) ||
        it.packageName.contains(search, ignoreCase = true)
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("app_picker_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
        .padding(bottom = 24.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Select App to Inspect",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      OutlinedTextField(
        value = search,
        onValueChange = { search = it },
        placeholder = { Text("Search installed apps…", fontSize = 12.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("app_picker_search")
      )

      Spacer(modifier = Modifier.height(10.dp))

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(360.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        items(filtered, key = { it.packageName }) { appItem ->
          val isSelected = appItem.packageName == currentPackageName
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelectApp(appItem) }
              .testTag("pick_app_${appItem.packageName}")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(12.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(36.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = appItem.appName.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                  )
                }
              }

              Spacer(modifier = Modifier.width(10.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = appItem.appName,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "${appItem.packageName} • ${appItem.permissions.size} perms",
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }
    }
  }
}
