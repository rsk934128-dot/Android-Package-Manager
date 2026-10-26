package com.example.ui.components

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Data representation of an installed package inspected via PackageManager.
 */
data class InstalledPackageItem(
  val appName: String,
  val packageName: String,
  val versionName: String,
  val versionCode: Long,
  val isSystemApp: Boolean,
  val firstInstallTime: Long = 0L,
  val lastUpdateTime: Long = 0L,
  val targetSdk: Int = 0
)

enum class PackageFilterType(val label: String) {
  ALL("All Packages"),
  USER("User Apps"),
  SYSTEM("System Apps")
}

enum class PackageSortType(val label: String) {
  NAME_ASC("Name (A-Z)"),
  VERSION_CODE_DESC("Version Code (High)"),
  VERSION_CODE_ASC("Version Code (Low)"),
  PACKAGE_NAME("Package Name")
}

/**
 * Loads installed packages from the device using Android [PackageManager].
 */
fun queryInstalledPackagesFromSystem(context: Context): List<InstalledPackageItem> {
  val pm = context.packageManager
  val flags = 0
  val rawPackages: List<PackageInfo> = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
      @Suppress("DEPRECATION")
      pm.getInstalledPackages(flags)
    }
  } catch (e: Exception) {
    emptyList()
  }

  return rawPackages.map { info ->
    val appName = try {
      info.applicationInfo?.loadLabel(pm)?.toString() ?: info.packageName
    } catch (e: Exception) {
      info.packageName
    }

    val versionCode = PackageInfoCompat.getLongVersionCode(info)
    val versionName = info.versionName ?: "N/A"
    val isSystem = (info.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
    val targetSdk = info.applicationInfo?.targetSdkVersion ?: 0

    InstalledPackageItem(
      appName = appName,
      packageName = info.packageName,
      versionName = versionName,
      versionCode = versionCode,
      isSystemApp = isSystem,
      firstInstallTime = info.firstInstallTime,
      lastUpdateTime = info.lastUpdateTime,
      targetSdk = targetSdk
    )
  }
}

/**
 * Composable to display a list of installed packages on the device with their names
 * and version codes, using PackageManager API.
 *
 * Includes:
 * - Direct PackageManager integration
 * - Display of App Name, Package Name, Version Code, and Version Name
 * - Search filtering by package or application label
 * - Category filters (All, User Apps, System Apps)
 * - Sorting by Name, Version Code (high to low / low to high)
 * - One-tap clipboard copy for package identifier and version code
 * - Fast, asynchronous loading that preserves 60/120 FPS UI fluidity
 */
@Composable
fun InstalledPackagesList(
  modifier: Modifier = Modifier,
  initialPackages: List<InstalledPackageItem>? = null,
  onPackageClick: ((InstalledPackageItem) -> Unit)? = null,
  onBackupClick: ((InstalledPackageItem) -> Unit)? = null,
  showHeader: Boolean = true
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val coroutineScope = rememberCoroutineScope()

  var packagesList by remember { mutableStateOf(initialPackages ?: emptyList()) }
  var isLoading by remember { mutableStateOf(initialPackages == null) }
  var searchQuery by remember { mutableStateOf("") }
  var filterType by remember { mutableStateOf(PackageFilterType.ALL) }
  var sortType by remember { mutableStateOf(PackageSortType.NAME_ASC) }

  // Load packages asynchronously on background dispatcher if not provided
  fun refreshPackages() {
    isLoading = true
    coroutineScope.launch {
      val loaded = withContext(Dispatchers.IO) {
        queryInstalledPackagesFromSystem(context)
      }
      packagesList = loaded
      isLoading = false
    }
  }

  LaunchedEffect(Unit) {
    if (initialPackages == null) {
      refreshPackages()
    }
  }

  // Filter and sort the list
  val filteredAndSortedPackages = remember(packagesList, searchQuery, filterType, sortType) {
    val filtered = packagesList.filter { pkg ->
      val matchesSearch = searchQuery.isBlank() ||
        pkg.appName.contains(searchQuery, ignoreCase = true) ||
        pkg.packageName.contains(searchQuery, ignoreCase = true)

      val matchesCategory = when (filterType) {
        PackageFilterType.ALL -> true
        PackageFilterType.USER -> !pkg.isSystemApp
        PackageFilterType.SYSTEM -> pkg.isSystemApp
      }

      matchesSearch && matchesCategory
    }

    when (sortType) {
      PackageSortType.NAME_ASC -> filtered.sortedBy { it.appName.lowercase() }
      PackageSortType.VERSION_CODE_DESC -> filtered.sortedByDescending { it.versionCode }
      PackageSortType.VERSION_CODE_ASC -> filtered.sortedBy { it.versionCode }
      PackageSortType.PACKAGE_NAME -> filtered.sortedBy { it.packageName.lowercase() }
    }
  }

  val totalCount = packagesList.size
  val userCount = remember(packagesList) { packagesList.count { !it.isSystemApp } }
  val systemCount = remember(packagesList) { packagesList.count { it.isSystemApp } }

  Column(modifier = modifier.fillMaxSize()) {
    if (showHeader) {
      // Metric Summary Header
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.Apps,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(22.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Installed Packages",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "PackageManager API Inspection",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            IconButton(
              onClick = { refreshPackages() },
              modifier = Modifier.size(36.dp).testTag("refresh_packages_button")
            ) {
              Icon(Icons.Default.Refresh, contentDescription = "Refresh Packages", modifier = Modifier.size(20.dp))
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Mini statistics badges
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(text = "Total", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = totalCount.toString(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
              }
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(text = "User Apps", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(text = userCount.toString(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
              }
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(text = "System", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(text = systemCount.toString(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
              }
            }
          }
        }
      }
    }

    // Search Input Field
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("Search by name or package…", fontSize = 13.sp) },
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
        .testTag("package_search_input")
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Filter and Sort Chips
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(PackageFilterType.entries.toTypedArray()) { type ->
        val count = when (type) {
          PackageFilterType.ALL -> totalCount
          PackageFilterType.USER -> userCount
          PackageFilterType.SYSTEM -> systemCount
        }
        FilterChip(
          selected = filterType == type,
          onClick = { filterType = type },
          label = { Text("${type.label} ($count)", fontSize = 11.sp) }
        )
      }

      // Sort Toggle Chip
      item {
        FilterChip(
          selected = true,
          onClick = {
            // Cycle through sort types
            sortType = when (sortType) {
              PackageSortType.NAME_ASC -> PackageSortType.VERSION_CODE_DESC
              PackageSortType.VERSION_CODE_DESC -> PackageSortType.VERSION_CODE_ASC
              PackageSortType.VERSION_CODE_ASC -> PackageSortType.PACKAGE_NAME
              PackageSortType.PACKAGE_NAME -> PackageSortType.NAME_ASC
            }
          },
          leadingIcon = {
            Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(14.dp))
          },
          label = { Text(sortType.label, fontSize = 11.sp) }
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // List Body or Loading State
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
            text = "Querying PackageManager API…",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else if (filteredAndSortedPackages.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "No packages match \"$searchQuery\"",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 13.sp
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .testTag("installed_packages_list"),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(
          items = filteredAndSortedPackages,
          key = { it.packageName }
        ) { pkg ->
          InstalledPackageCard(
            item = pkg,
            onClick = {
              onPackageClick?.invoke(pkg)
            },
            onCopy = {
              clipboardManager.setText(AnnotatedString("${pkg.packageName} (Version Code: ${pkg.versionCode})"))
              Toast.makeText(context, "Copied: ${pkg.packageName}", Toast.LENGTH_SHORT).show()
            },
            onBackupClick = if (onBackupClick != null) { { onBackupClick(pkg) } } else null
          )
        }
      }
    }
  }
}

/**
 * Individual Card displaying an Installed Package's name, version code, and details.
 */
@Composable
fun InstalledPackageCard(
  item: InstalledPackageItem,
  onClick: () -> Unit,
  onCopy: () -> Unit,
  onBackupClick: (() -> Unit)? = null
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("installed_package_item_${item.packageName}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // App Avatar / Icon Representation
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (item.isSystemApp) {
          MaterialTheme.colorScheme.secondaryContainer
        } else {
          MaterialTheme.colorScheme.primaryContainer
        },
        modifier = Modifier.size(42.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          if (item.isSystemApp) {
            Icon(
              Icons.Default.SystemUpdate,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(20.dp)
            )
          } else {
            Text(
              text = item.appName.take(1).uppercase(),
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // App Name, Package Name, and Version Details
      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = item.appName,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
          )

          // Version Code Badge (Highlighting Version Code as requested)
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.padding(start = 6.dp)
          ) {
            Text(
              text = "Code: ${item.versionCode}",
              fontSize = 10.sp,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Package Name
        Text(
          text = item.packageName,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Sub-details: Version Name and System / User Tag
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "v${item.versionName}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.outline
          )

          Text(
            text = "•",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.outline
          )

          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (item.isSystemApp) EmeraldTertiary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
          ) {
            Text(
              text = if (item.isSystemApp) "SYSTEM" else "USER",
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              color = if (item.isSystemApp) EmeraldTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
          }

          if (item.targetSdk > 0) {
            Text(
              text = "• Target SDK ${item.targetSdk}",
              fontSize = 9.sp,
              color = MaterialTheme.colorScheme.outline
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(6.dp))

      if (onBackupClick != null) {
        IconButton(
          onClick = onBackupClick,
          modifier = Modifier.size(32.dp).testTag("backup_${item.packageName}")
        ) {
          Icon(
            Icons.Default.Archive,
            contentDescription = "Backup APK to External Storage",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(17.dp)
          )
        }
      }

      // Copy package and version code button
      IconButton(
        onClick = onCopy,
        modifier = Modifier.size(32.dp).testTag("copy_${item.packageName}")
      ) {
        Icon(
          Icons.Default.ContentCopy,
          contentDescription = "Copy Package Name & Version Code",
          tint = MaterialTheme.colorScheme.outline,
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}
