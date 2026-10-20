package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.manager.ApkInstallerManager
import com.example.manager.DownloaderManager
import com.example.manager.RemoteServerManager
import com.example.manager.RemoteServerState
import com.example.manager.SecurityAuditEngine
import com.example.manager.SystemDiagnosticsManager
import com.example.model.AppPackageItem
import com.example.model.DownloadItem
import com.example.model.ManifestAuditResult
import com.example.model.SystemDiagnostic
import com.example.model.ThreatLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class AppFilter {
  ALL,
  USER_ONLY,
  SYSTEM_ONLY,
  HIGH_RISK
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

  val installerManager = ApkInstallerManager(application)
  val downloaderManager = DownloaderManager(application)
  val auditEngine = SecurityAuditEngine(application)
  val diagnosticsManager = SystemDiagnosticsManager(application)

  val remoteServerManager = RemoteServerManager(
    context = application,
    onRemoteDownloadRequested = { url, fileName ->
      startUrlDownload(url, fileName)
    },
    diagnosticsProvider = {
      _systemDiagnostic.value
    }
  )
  val remoteServerState: StateFlow<RemoteServerState> = remoteServerManager.serverState

  private val _activeTab = MutableStateFlow(0)
  val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

  val currentDownload: StateFlow<DownloadItem?> = downloaderManager.currentDownload
  val downloadedFiles: StateFlow<List<File>> = downloaderManager.downloadedFiles

  private val _installedApps = MutableStateFlow<List<AppPackageItem>>(emptyList())
  val installedApps: StateFlow<List<AppPackageItem>> = _installedApps.asStateFlow()

  private val _isLoadingApps = MutableStateFlow(false)
  val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedFilter = MutableStateFlow(AppFilter.ALL)
  val selectedFilter: StateFlow<AppFilter> = _selectedFilter.asStateFlow()

  private val _selectedAppForDetail = MutableStateFlow<AppPackageItem?>(null)
  val selectedAppForDetail: StateFlow<AppPackageItem?> = _selectedAppForDetail.asStateFlow()

  private val _systemDiagnostic = MutableStateFlow<SystemDiagnostic?>(null)
  val systemDiagnostic: StateFlow<SystemDiagnostic?> = _systemDiagnostic.asStateFlow()

  val defaultSampleAuditText = """
Package: com.mtf.app
Launcher Activity: com.service.downloadapp.AppMain
Secondary: com.service.downloadapp.DownloadAPPActivity
FileProvider: androidx.core.content.FileProvider (authority: com.mtf.app.provider)
Permissions:
 - android.permission.INTERNET
 - android.permission.ACCESS_NETWORK_STATE
 - android.permission.REQUEST_INSTALL_PACKAGES
 - android.permission.REQUEST_DELETE_PACKAGES
 - android.permission.DUMP
Embedded Tracking Markers:
 - mobile.monitor.child2022
 - mobile.parental2025
 - monitor.phone062026
 - phone.child2020, phone.child2024, phone.monitor2026
Shell Protection: com.protect2025
  """.trimIndent()

  private val _manifestAuditResult = MutableStateFlow<ManifestAuditResult?>(null)
  val manifestAuditResult: StateFlow<ManifestAuditResult?> = _manifestAuditResult.asStateFlow()

  private val _customManifestInput = MutableStateFlow(defaultSampleAuditText)
  val customManifestInput: StateFlow<String> = _customManifestInput.asStateFlow()

  val filteredApps: StateFlow<List<AppPackageItem>> = combine(
    _installedApps,
    _searchQuery,
    _selectedFilter
  ) { apps, query, filter ->
    apps.filter { item ->
      val matchesQuery = query.isBlank() ||
        item.appName.contains(query, ignoreCase = true) ||
        item.packageName.contains(query, ignoreCase = true)

      val matchesFilter = when (filter) {
        AppFilter.ALL -> true
        AppFilter.USER_ONLY -> !item.isSystemApp
        AppFilter.SYSTEM_ONLY -> item.isSystemApp
        AppFilter.HIGH_RISK -> item.threatLevel == ThreatLevel.HIGH || item.threatScore >= 25
      }

      matchesQuery && matchesFilter
    }
  }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  init {
    refreshInstalledApps()
    refreshDiagnostics()
    runManifestAudit(defaultSampleAuditText)
  }

  fun setTab(index: Int) {
    _activeTab.value = index
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setFilter(filter: AppFilter) {
    _selectedFilter.value = filter
  }

  fun selectAppDetail(app: AppPackageItem?) {
    _selectedAppForDetail.value = app
  }

  fun setManifestInput(text: String) {
    _customManifestInput.value = text
  }

  fun refreshInstalledApps() {
    viewModelScope.launch {
      _isLoadingApps.value = true
      val apps = auditEngine.scanInstalledApps()
      _installedApps.value = apps
      _isLoadingApps.value = false
    }
  }

  fun refreshDiagnostics() {
    val canInstall = installerManager.canInstallPackages()
    _systemDiagnostic.value = diagnosticsManager.getSystemDiagnostics(canInstall)
  }

  fun startUrlDownload(url: String, fileName: String? = null) {
    downloaderManager.startDownload(url, fileName)
  }

  fun startDemoSimulation(name: String, sizeMb: Int = 18) {
    downloaderManager.startDemoSimulation(name, sizeMb)
  }

  fun cancelDownload() {
    downloaderManager.cancelDownload()
  }

  fun installApkFile(file: File) {
    installerManager.installApk(file)
  }

  fun deleteApkFile(file: File) {
    downloaderManager.deleteDownloadedFile(file)
  }

  fun clearAllCachedDownloads() {
    downloaderManager.clearAllDownloads()
  }

  fun runManifestAudit(text: String) {
    _manifestAuditResult.value = auditEngine.auditManifestText(text)
  }

  fun toggleRemoteServer(enable: Boolean) {
    if (enable) {
      remoteServerManager.startServer(8080)
    } else {
      remoteServerManager.stopServer()
    }
  }

  fun triggerFindPhoneAlarm() {
    remoteServerManager.playFindDeviceSound()
  }

  override fun onCleared() {
    super.onCleared()
    remoteServerManager.stopServer()
  }
}
