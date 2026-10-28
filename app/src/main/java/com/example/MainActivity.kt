package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Badge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.manager.RemoteServerState
import com.example.model.AppPackageItem
import com.example.model.DownloadStatus
import com.example.ui.components.AppDetailSheet
import com.example.ui.components.CloneAppDialog
import com.example.ui.screens.AppPermissionsScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.DownloaderScreen
import com.example.ui.screens.PackageInspectorScreen
import com.example.ui.screens.ThreatAuditScreen
import com.example.ui.screens.WebAppsScreen
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RiskSafe
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: MainViewModel by viewModels()

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val navTab = intent?.getIntExtra("nav_tab", -1) ?: -1
    if (navTab >= 0) {
      viewModel.setTab(navTab)
    }

    setContent {
      MyApplicationTheme {
        val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
        val currentDownload by viewModel.currentDownload.collectAsStateWithLifecycle()
        val downloadedFiles by viewModel.downloadedFiles.collectAsStateWithLifecycle()
        val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
        val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
        val isLoadingApps by viewModel.isLoadingApps.collectAsStateWithLifecycle()
        val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
        val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
        val selectedAppDetail by viewModel.selectedAppForDetail.collectAsStateWithLifecycle()
        val systemDiagnostic by viewModel.systemDiagnostic.collectAsStateWithLifecycle()
        val remoteServerState by viewModel.remoteServerState.collectAsStateWithLifecycle()
        val batteryReport by viewModel.batteryReport.collectAsStateWithLifecycle()
        val manifestAudit by viewModel.manifestAuditResult.collectAsStateWithLifecycle()
        val customManifestInput by viewModel.customManifestInput.collectAsStateWithLifecycle()
        val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
        val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()
        val savedWebApps by viewModel.savedWebApps.collectAsStateWithLifecycle()
        val activeWebApp by viewModel.activeWebApp.collectAsStateWithLifecycle()
        val isWebAppBackgroundActive by viewModel.isWebAppBackgroundActive.collectAsStateWithLifecycle()
        val webAppNotificationHistory by viewModel.webAppNotificationHistory.collectAsStateWithLifecycle()
        val clonedApps by viewModel.clonedApps.collectAsStateWithLifecycle()
        val activeClonedApp by viewModel.activeClonedApp.collectAsStateWithLifecycle()
        val apkBackups by viewModel.apkBackups.collectAsStateWithLifecycle()
        val batchBackupProgress by viewModel.batchBackupProgress.collectAsStateWithLifecycle()
        val selectedAppForPermissions by viewModel.selectedAppForPermissions.collectAsStateWithLifecycle()
        val isSigningIn by viewModel.isSigningIn.collectAsStateWithLifecycle()
        val authStatusEvent by viewModel.authStatusEvent.collectAsStateWithLifecycle()

        LaunchedEffect(authStatusEvent) {
          authStatusEvent?.let { (success, message) ->
            Toast.makeText(this@MainActivity, message, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
            viewModel.clearAuthStatusEvent()
          }
        }

        var appToCloneForNativeDialog by remember { mutableStateOf<AppPackageItem?>(null) }

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          topBar = {
            if (activeWebApp == null && activeClonedApp == null && selectedAppForPermissions == null) {
              CenterAlignedTopAppBar(
              title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(28.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      Icons.Default.Shield,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onPrimary,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = stringResource(id = R.string.app_name),
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RiskSafe.copy(alpha = 0.15f)
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Box(
                        modifier = Modifier
                          .size(6.dp)
                          .clip(CircleShape)
                          .background(RiskSafe)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = "Healthy",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = RiskSafe
                      )
                    }
                  }
                }
              },
              actions = {
                val user = currentUser
                if (user != null) {
                  Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                      .padding(end = 8.dp)
                      .testTag("user_profile_chip")
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                      Box(
                        modifier = Modifier
                          .size(20.dp)
                          .clip(CircleShape)
                          .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                      ) {
                        Text(
                          text = (user.displayName?.take(1) ?: user.email?.take(1) ?: "U").uppercase(),
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onPrimary
                        )
                      }
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = (user.displayName ?: user.email ?: "User").take(8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      IconButton(
                        onClick = {
                          viewModel.signOut()
                          Toast.makeText(this@MainActivity, "Signed out", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                          .size(20.dp)
                          .testTag("sign_out_button")
                      ) {
                        Icon(
                          Icons.Default.Logout,
                          contentDescription = "Sign Out",
                          tint = MaterialTheme.colorScheme.onPrimaryContainer,
                          modifier = Modifier.size(13.dp)
                        )
                      }
                    }
                  }
                } else {
                  Button(
                    onClick = {
                      Toast.makeText(this@MainActivity, "Opening Google Sign-In…", Toast.LENGTH_SHORT).show()
                      viewModel.signInWithGoogle(this@MainActivity)
                    },
                    enabled = !isSigningIn,
                    modifier = Modifier
                      .padding(end = 8.dp)
                      .height(36.dp)
                      .testTag("google_sign_in_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                  ) {
                    if (isSigningIn) {
                      CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = "Signing In…",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                      )
                    } else {
                      Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = "Sign In",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                }
              },
              colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
              )
            )
          }
        },
        bottomBar = {
          if (activeWebApp == null && activeClonedApp == null && selectedAppForPermissions == null) {
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              modifier = Modifier.testTag("bottom_nav_bar")
            ) {
              NavigationBarItem(
                selected = activeTab == 0,
                onClick = { viewModel.setTab(0) },
                icon = {
                  val isDownloading = currentDownload?.status == DownloadStatus.DOWNLOADING
                  BadgedBox(badge = {
                    if (isDownloading) {
                      Badge { Text("1") }
                    }
                  }) {
                    Icon(Icons.Default.Download, contentDescription = "Downloader")
                  }
                },
                label = { Text("Downloader") },
                modifier = Modifier.testTag("nav_tab_downloader")
              )

              NavigationBarItem(
                selected = activeTab == 1,
                onClick = { viewModel.setTab(1) },
                icon = {
                  Icon(Icons.Default.Apps, contentDescription = "Inspector")
                },
                label = { Text("Inspector") },
                modifier = Modifier.testTag("nav_tab_inspector")
              )

              NavigationBarItem(
                selected = activeTab == 2,
                onClick = { viewModel.setTab(2) },
                icon = {
                  Icon(Icons.Default.Shield, contentDescription = "Threat Audit")
                },
                label = { Text("Audit") },
                modifier = Modifier.testTag("nav_tab_audit")
              )

              NavigationBarItem(
                selected = activeTab == 3,
                onClick = { viewModel.setTab(3) },
                icon = {
                  Icon(Icons.Default.Memory, contentDescription = "Diagnostics")
                },
                label = { Text("Diagnostics") },
                modifier = Modifier.testTag("nav_tab_diagnostics")
              )

              NavigationBarItem(
                selected = activeTab == 4,
                onClick = { viewModel.setTab(4) },
                icon = {
                  BadgedBox(badge = {
                    if (isWebAppBackgroundActive || clonedApps.isNotEmpty()) {
                      Box(
                        modifier = Modifier
                          .size(7.dp)
                          .clip(CircleShape)
                          .background(EmeraldTertiary)
                      )
                    }
                  }) {
                    Icon(Icons.Default.Language, contentDescription = "Apps Cloner & Hub")
                  }
                },
                label = { Text("Cloner & Hub") },
                modifier = Modifier.testTag("nav_tab_webapps")
              )
            }
          }
        }
      ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) {
            if (selectedAppForPermissions != null) {
              AppPermissionsScreen(
                app = selectedAppForPermissions!!,
                allApps = installedApps,
                onBack = { viewModel.selectAppForPermissions(null) },
                onSelectDifferentApp = { viewModel.selectAppForPermissions(it) },
                onOpenSettings = { pkg ->
                  val intent = viewModel.createManagePermissionsIntent(pkg)
                  runCatching { startActivity(intent) }.onFailure {
                    viewModel.installerManager.openAppDetails(pkg)
                  }
                },
                onLoadPermissions = { pkg -> viewModel.loadPermissionsForPackage(pkg) }
              )
            } else {
              when (activeTab) {
                0 -> DownloaderScreen(
                currentDownload = currentDownload,
                downloadedFiles = downloadedFiles,
                apkBackups = apkBackups,
                batchBackupProgress = batchBackupProgress,
                onStartDownload = { url, fileName -> viewModel.startUrlDownload(url, fileName) },
                onStartDemo = { name, sizeMb -> viewModel.startDemoSimulation(name, sizeMb) },
                onCancelDownload = { viewModel.cancelDownload() },
                onInstallApk = { file ->
                  val result = viewModel.installerManager.installApk(file)
                  result.onFailure {
                    Toast.makeText(this@MainActivity, "Install prompt error: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                  }
                },
                onDeleteFile = { file -> viewModel.deleteApkFile(file) },
                onClearCache = { viewModel.clearAllCachedDownloads() },
                onBackupAllUserApps = {
                  viewModel.backupAllUserPackages { results ->
                    Toast.makeText(this@MainActivity, "Successfully backed up ${results.size} apps offline!", Toast.LENGTH_SHORT).show()
                  }
                },
                onDeleteBackup = { item ->
                  viewModel.deleteBackup(item)
                  Toast.makeText(this@MainActivity, "Deleted backup: ${item.appName}", Toast.LENGTH_SHORT).show()
                },
                onClearBackups = {
                  val count = viewModel.clearAllBackups()
                  Toast.makeText(this@MainActivity, "Cleared $count offline backups", Toast.LENGTH_SHORT).show()
                },
                onShareBackup = { item ->
                  runCatching {
                    val intent = viewModel.shareBackup(item)
                    startActivity(android.content.Intent.createChooser(intent, "Share Offline APK: ${item.appName}"))
                  }.onFailure {
                    Toast.makeText(this@MainActivity, "Share error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                  }
                },
                onVerifyBackup = { item ->
                  viewModel.verifyBackupIntegrity(item)
                }
              )

              1 -> PackageInspectorScreen(
                apps = filteredApps,
                allApps = installedApps,
                isLoading = isLoadingApps,
                searchQuery = searchQuery,
                selectedFilter = selectedFilter,
                onSearchChanged = { viewModel.setSearchQuery(it) },
                onFilterChanged = { viewModel.setFilter(it) },
                onAppSelected = { app -> viewModel.selectAppDetail(app) },
                onInspectPermissions = { app -> viewModel.selectAppForPermissions(app) },
                onRefresh = { viewModel.refreshInstalledApps() },
                onBackupPackage = { pkgName ->
                  viewModel.backupInstalledPackage(pkgName) { res ->
                    res.onSuccess {
                      Toast.makeText(this@MainActivity, "APK for ${it.appName} backed up offline successfully!", Toast.LENGTH_SHORT).show()
                    }.onFailure {
                      Toast.makeText(this@MainActivity, "Backup failed: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                  }
                }
              )

              2 -> ThreatAuditScreen(
                manifestInput = customManifestInput,
                auditResult = manifestAudit,
                defaultSampleText = viewModel.defaultSampleAuditText,
                onInputChanged = { viewModel.setManifestInput(it) },
                onRunAudit = { viewModel.runManifestAudit(it) },
                onSaveToCloud = {
                  if (currentUser == null) {
                    Toast.makeText(this@MainActivity, "Please sign in with Google first", Toast.LENGTH_SHORT).show()
                    viewModel.signInWithGoogle(this@MainActivity)
                  } else {
                    viewModel.syncCurrentThreatReportToCloud { success, msg ->
                      Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                  }
                }
              )

              3 -> DiagnosticsScreen(
                diagnostic = systemDiagnostic,
                remoteServerState = remoteServerState,
                batteryReport = batteryReport,
                onRefresh = {
                  viewModel.refreshDiagnostics()
                  viewModel.refreshBatteryReport()
                },
                onClearCache = { viewModel.clearAllCachedDownloads() },
                onToggleRemoteServer = { viewModel.toggleRemoteServer(it) },
                onPlayFindDeviceSound = { viewModel.triggerFindPhoneAlarm() },
                onOpenBatteryOptimization = { pkg -> viewModel.openBatteryOptimizationSettings(pkg) },
                onOpenAppDetails = { pkg -> viewModel.openAppDetailsSettings(pkg) },
                onScanAndAlertDrain = { viewModel.scanAndNotifyHighDrainTasks(bypassCooldown = true) },
                onSyncBatteryToCloud = {
                  if (currentUser == null) {
                    Toast.makeText(this@MainActivity, "Please sign in with Google first", Toast.LENGTH_SHORT).show()
                    viewModel.signInWithGoogle(this@MainActivity)
                  } else {
                    viewModel.syncCurrentBatterySnapshotToCloud { success, msg ->
                      Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                  }
                }
              )

              4 -> WebAppsScreen(
                savedApps = savedWebApps,
                activeApp = activeWebApp,
                isBackgroundActive = isWebAppBackgroundActive,
                notificationHistory = webAppNotificationHistory,
                clonedApps = clonedApps,
                activeClonedApp = activeClonedApp,
                onOpenApp = { viewModel.openWebApp(it) },
                onCloseApp = { viewModel.closeActiveWebApp() },
                onAddApp = { name, url, category, keepAlive, desktopMode ->
                  viewModel.addWebApp(name, url, category, keepAlive, desktopMode)
                },
                onDeleteApp = { viewModel.deleteWebApp(it) },
                onToggleBackgroundService = { viewModel.toggleWebAppBackgroundService(it) },
                onTriggerNotification = { appName, title, body, type ->
                  viewModel.triggerWebNotification(appName, title, body, type)
                },
                onOpenClone = { viewModel.openClone(it) },
                onCloseClone = { viewModel.closeActiveClone() },
                onCloneWebApp = { app, name, badge, emoji, color ->
                  viewModel.cloneWebApp(app, name, badge, emoji, color)
                },
                onDeleteClone = { viewModel.deleteClone(it) },
                onClearCloneData = { viewModel.clearCloneStorage(it) },
                onLaunchNativeClone = { clone ->
                  val res = viewModel.launchNativeClone(clone)
                  res.onFailure {
                    Toast.makeText(this@MainActivity, "Launch failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                  }
                },
                onDownloadRequested = { url, fileName ->
                  viewModel.startUrlDownload(url, fileName)
                }
              )
            }
          }

            // Bottom sheet for app details
            selectedAppDetail?.let { app ->
              AppDetailSheet(
                app = app,
                onDismiss = { viewModel.selectAppDetail(null) },
                onLaunch = { pkg ->
                  val res = viewModel.installerManager.launchApp(pkg)
                  res.onFailure {
                    Toast.makeText(this@MainActivity, "Unable to launch $pkg", Toast.LENGTH_SHORT).show()
                  }
                },
                onOpenSettings = { pkg ->
                  viewModel.installerManager.openAppDetails(pkg)
                },
                onUninstall = { pkg ->
                  viewModel.installerManager.uninstallApp(pkg)
                },
                onClone = { appToClone ->
                  viewModel.selectAppDetail(null)
                  appToCloneForNativeDialog = appToClone
                },
                onBackup = { appToBackup ->
                  viewModel.backupInstalledPackage(appToBackup.packageName) { res ->
                    res.onSuccess {
                      Toast.makeText(this@MainActivity, "Offline APK for ${it.appName} saved (${it.fileSizeBytes / (1024 * 1024)} MB)", Toast.LENGTH_SHORT).show()
                    }.onFailure {
                      Toast.makeText(this@MainActivity, "Backup failed: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                  }
                },
                onInspectPermissions = { appToInspect ->
                  viewModel.selectAppDetail(null)
                  viewModel.selectAppForPermissions(appToInspect)
                }
              )
            }

            // Dialog for cloning a native APK / installed package
            appToCloneForNativeDialog?.let { targetNativeApp ->
              CloneAppDialog(
                originalAppName = targetNativeApp.appName,
                defaultBadge = "Clone 1",
                onDismiss = { appToCloneForNativeDialog = null },
                onConfirm = { cloneName, cloneBadge, emoji, colorTintHex ->
                  viewModel.cloneNativeApp(targetNativeApp, cloneName, cloneBadge, emoji, colorTintHex)
                  appToCloneForNativeDialog = null
                  viewModel.setTab(4) // Navigate to Cloner Hub to view the cloned app
                  Toast.makeText(this@MainActivity, "Cloned ${targetNativeApp.appName} successfully!", Toast.LENGTH_SHORT).show()
                }
              )
            }
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    val navTab = intent.getIntExtra("nav_tab", -1)
    if (navTab >= 0) {
      viewModel.setTab(navTab)
    }
  }
}
