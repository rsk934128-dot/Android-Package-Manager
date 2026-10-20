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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.DownloadStatus
import com.example.ui.components.AppDetailSheet
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.DownloaderScreen
import com.example.ui.screens.PackageInspectorScreen
import com.example.ui.screens.ThreatAuditScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RiskSafe
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: MainViewModel by viewModels()

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

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
        val manifestAudit by viewModel.manifestAuditResult.collectAsStateWithLifecycle()
        val customManifestInput by viewModel.customManifestInput.collectAsStateWithLifecycle()

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          topBar = {
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
                }
              },
              actions = {
                Box(
                  modifier = Modifier
                    .padding(end = 12.dp)
                    .clip(CircleShape)
                    .background(RiskSafe.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(RiskSafe)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "SYSTEM ACTIVE",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = RiskSafe
                    )
                  }
                }
              },
              colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
              )
            )
          },
          bottomBar = {
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
            }
          }
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) {
            when (activeTab) {
              0 -> DownloaderScreen(
                currentDownload = currentDownload,
                downloadedFiles = downloadedFiles,
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
                onClearCache = { viewModel.clearAllCachedDownloads() }
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
                onRefresh = { viewModel.refreshInstalledApps() }
              )

              2 -> ThreatAuditScreen(
                manifestInput = customManifestInput,
                auditResult = manifestAudit,
                defaultSampleText = viewModel.defaultSampleAuditText,
                onInputChanged = { viewModel.setManifestInput(it) },
                onRunAudit = { viewModel.runManifestAudit(it) }
              )

              3 -> DiagnosticsScreen(
                diagnostic = systemDiagnostic,
                onRefresh = { viewModel.refreshDiagnostics() },
                onClearCache = { viewModel.clearAllCachedDownloads() }
              )
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
                }
              )
            }
          }
        }
      }
    }
  }
}
