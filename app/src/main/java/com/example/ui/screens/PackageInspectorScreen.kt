package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.example.ui.components.InstalledPackagesList
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppPackageItem
import com.example.model.ThreatLevel
import com.example.ui.theme.RiskHigh
import com.example.ui.theme.RiskMedium
import com.example.ui.theme.RiskSafe
import com.example.viewmodel.AppFilter

@Composable
fun PackageInspectorScreen(
  apps: List<AppPackageItem>,
  allApps: List<AppPackageItem>,
  isLoading: Boolean,
  searchQuery: String,
  selectedFilter: AppFilter,
  onSearchChanged: (String) -> Unit,
  onFilterChanged: (AppFilter) -> Unit,
  onAppSelected: (AppPackageItem) -> Unit,
  onRefresh: () -> Unit,
  onBackupPackage: ((String) -> Unit)? = null,
  onInspectPermissions: ((AppPackageItem) -> Unit)? = null
) {
  val totalCount = allApps.size
  val highRiskCount = allApps.count { it.threatLevel == ThreatLevel.HIGH || it.threatScore >= 25 }
  val userCount = allApps.count { !it.isSystemApp }
  val systemCount = allApps.count { it.isSystemApp }

  var inspectorMode by remember { mutableIntStateOf(0) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(8.dp))

    // Top Mode Switcher
    TabRow(
      selectedTabIndex = inspectorMode,
      containerColor = Color.Transparent,
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 8.dp)
        .testTag("package_inspector_tab_row")
    ) {
      Tab(
        selected = inspectorMode == 0,
        onClick = { inspectorMode = 0 },
        text = { Text("App Permissions", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
        modifier = Modifier.testTag("tab_app_permissions")
      )
      Tab(
        selected = inspectorMode == 1,
        onClick = { inspectorMode = 1 },
        text = { Text("Threat Audit", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
        modifier = Modifier.testTag("tab_threat_audit")
      )
      Tab(
        selected = inspectorMode == 2,
        onClick = { inspectorMode = 2 },
        text = { Text("Version Codes", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
        modifier = Modifier.testTag("tab_packages_version_codes")
      )
    }

    when (inspectorMode) {
      0 -> {
        InstalledAppsPermissionsScreen(
          showHeader = false,
          onAppClick = { appWithPerms ->
            val matching = allApps.firstOrNull { it.packageName == appWithPerms.packageName }
            if (matching != null) {
              onAppSelected(matching)
            }
          }
        )
      }
      2 -> {
        InstalledPackagesList(
          onPackageClick = { pkg ->
            val matching = allApps.firstOrNull { it.packageName == pkg.packageName }
            if (matching != null) {
              onAppSelected(matching)
            }
          },
          onBackupClick = if (onBackupPackage != null) { { pkg -> onBackupPackage(pkg.packageName) } } else null,
          showHeader = true
        )
      }
      else -> {

    // Search bar & refresh
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChanged,
        placeholder = { Text("Search installed packages…") },
        leadingIcon = {
          Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingIcon = {
          if (searchQuery.isNotBlank()) {
            IconButton(onClick = { onSearchChanged("") }) {
              Icon(Icons.Default.Close, contentDescription = "Clear Search")
            }
          }
        },
        singleLine = true,
        modifier = Modifier
          .weight(1f)
          .testTag("app_search_input")
      )

      Spacer(modifier = Modifier.width(8.dp))

      IconButton(
        onClick = onRefresh,
        modifier = Modifier
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surfaceVariant)
          .testTag("refresh_apps_button")
      ) {
        Icon(Icons.Default.Refresh, contentDescription = "Refresh Packages", tint = MaterialTheme.colorScheme.primary)
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Summary Stat Pill Cards
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      SummaryMiniCard(
        label = "Total Packages",
        count = totalCount,
        icon = Icons.Default.Apps,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.weight(1f)
      )
      SummaryMiniCard(
        label = "Audited Clean",
        count = totalCount - highRiskCount,
        icon = Icons.Default.CheckCircle,
        tint = RiskSafe,
        modifier = Modifier.weight(1f)
      )
      SummaryMiniCard(
        label = "Elevated Risk",
        count = highRiskCount,
        icon = Icons.Default.Warning,
        tint = if (highRiskCount > 0) RiskHigh else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Filter Chips
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      item {
        FilterChip(
          selected = selectedFilter == AppFilter.ALL,
          onClick = { onFilterChanged(AppFilter.ALL) },
          label = { Text("All ($totalCount)") },
          modifier = Modifier.testTag("filter_all")
        )
      }
      item {
        FilterChip(
          selected = selectedFilter == AppFilter.USER_ONLY,
          onClick = { onFilterChanged(AppFilter.USER_ONLY) },
          label = { Text("User Apps ($userCount)") },
          modifier = Modifier.testTag("filter_user")
        )
      }
      item {
        FilterChip(
          selected = selectedFilter == AppFilter.HIGH_RISK,
          onClick = { onFilterChanged(AppFilter.HIGH_RISK) },
          label = { Text("High Risk ($highRiskCount)") },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = RiskHigh.copy(alpha = 0.25f),
            selectedLabelColor = RiskHigh
          ),
          modifier = Modifier.testTag("filter_risk")
        )
      }
      item {
        FilterChip(
          selected = selectedFilter == AppFilter.SYSTEM_ONLY,
          onClick = { onFilterChanged(AppFilter.SYSTEM_ONLY) },
          label = { Text("System ($systemCount)") },
          modifier = Modifier.testTag("filter_system")
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (isLoading) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.height(12.dp))
          Text("Scanning installed packages & permissions…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    } else if (apps.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Text("No matching packages found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        items(apps, key = { it.packageName }) { app ->
          AppListItem(
            app = app,
            onClick = { onAppSelected(app) },
            onInspectPermissions = if (onInspectPermissions != null) { { onInspectPermissions(app) } } else null
          )
        }
        item {
          Spacer(modifier = Modifier.height(24.dp))
        }
      }
    }
  }
}
}
}

@Composable
fun SummaryMiniCard(
  label: String,
  count: Int,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  tint: androidx.compose.ui.graphics.Color,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(text = count.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = tint)
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(text = label, fontSize = 10.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
fun AppListItem(
  app: AppPackageItem,
  onClick: () -> Unit,
  onInspectPermissions: (() -> Unit)? = null
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
      .clickable(onClick = onClick)
      .testTag("app_item_${app.packageName}")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Default.Security,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = app.appName,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false)
          )
          if (app.isSystemApp) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
              modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text("SYS", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }

        Text(
          text = app.packageName,
          fontSize = 11.sp,
          maxLines = 1,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(top = 2.dp)
        ) {
          Text(
            text = "v${app.versionName} • SDK ${app.targetSdk}",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.primary
          )

          val dangerousCount = app.permissions.count { it.isHighRisk }
          if (dangerousCount > 0) {
            Text(
              text = " • $dangerousCount risk perms",
              fontSize = 10.sp,
              color = RiskHigh,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      val (badgeColor, badgeText) = when (app.threatLevel) {
        ThreatLevel.HIGH -> Pair(RiskHigh, "HIGH")
        ThreatLevel.MEDIUM -> Pair(RiskMedium, "AUDIT")
        ThreatLevel.SAFE -> Pair(RiskSafe, "SAFE")
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(badgeColor.copy(alpha = 0.15f))
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = badgeText,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = badgeColor
        )
      }

      if (onInspectPermissions != null) {
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(
          onClick = onInspectPermissions,
          modifier = Modifier
            .size(36.dp)
            .testTag("inspect_perms_${app.packageName}")
        ) {
          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = "Inspect permissions for ${app.appName}",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }
}
