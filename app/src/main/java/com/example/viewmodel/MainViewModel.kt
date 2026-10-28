package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.app.Activity
import com.example.manager.AppClonerManager
import com.example.manager.AppPermissionManager
import com.example.manager.ApkBackupManager
import com.example.manager.ApkInstallerManager
import com.example.manager.AuthManager
import com.example.manager.BatteryMonitorManager
import com.example.manager.DownloaderManager
import com.example.manager.RemoteServerManager
import com.example.manager.RemoteServerState
import com.example.manager.SecurityAuditEngine
import com.example.manager.SystemDiagnosticsManager
import com.example.manager.WebAppManager
import com.example.model.ApkBackupItem
import com.example.model.ClonedAppItem
import com.example.model.DetailedPermissionItem
import com.example.model.SavedWebApp
import com.example.model.WebAppCategory
import com.example.model.WebAppNotificationEvent
import com.example.model.WebNotificationType
import com.example.repository.AppPulseFirestoreRepository
import com.google.firebase.auth.FirebaseUser
import com.example.model.AppBatteryDrainInfo
import com.example.model.BatteryHealthReport
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
  val batteryMonitorManager = BatteryMonitorManager(application)
  val authManager = AuthManager(application)
  val firestoreRepository = AppPulseFirestoreRepository(application)
  val webAppManager = WebAppManager(application)
  val appClonerManager = AppClonerManager(application)
  val apkBackupManager = ApkBackupManager(application)
  val permissionManager = AppPermissionManager(application)

  val apkBackups: StateFlow<List<ApkBackupItem>> = apkBackupManager.backups
  private val _batchBackupProgress = MutableStateFlow<Triple<Int, Int, String>?>(null)
  val batchBackupProgress: StateFlow<Triple<Int, Int, String>?> = _batchBackupProgress.asStateFlow()

  val savedWebApps: StateFlow<List<SavedWebApp>> = webAppManager.savedWebApps
  val activeWebApp: StateFlow<SavedWebApp?> = webAppManager.activeWebApp
  val isWebAppBackgroundActive: StateFlow<Boolean> = webAppManager.isBackgroundServiceActive
  val webAppNotificationHistory: StateFlow<List<WebAppNotificationEvent>> = webAppManager.recentNotifications

  val clonedApps: StateFlow<List<ClonedAppItem>> = appClonerManager.clonedApps
  val activeClonedApp: StateFlow<ClonedAppItem?> = appClonerManager.activeClonedApp

  val currentUser: StateFlow<FirebaseUser?> = authManager.authStateFlow()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), authManager.currentUser)

  private val _cloudSyncStatus = MutableStateFlow<String?>(null)
  val cloudSyncStatus: StateFlow<String?> = _cloudSyncStatus.asStateFlow()

  private val _batteryReport = MutableStateFlow<BatteryHealthReport?>(null)
  val batteryReport: StateFlow<BatteryHealthReport?> = _batteryReport.asStateFlow()

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

  private val _selectedAppForPermissions = MutableStateFlow<AppPackageItem?>(null)
  val selectedAppForPermissions: StateFlow<AppPackageItem?> = _selectedAppForPermissions.asStateFlow()

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
    refreshBatteryReport()
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

  fun selectAppForPermissions(app: AppPackageItem?) {
    _selectedAppForPermissions.value = app
  }

  fun loadPermissionsForPackage(packageName: String): List<DetailedPermissionItem> {
    return permissionManager.getPermissionsForPackage(packageName)
  }

  fun createManagePermissionsIntent(packageName: String): android.content.Intent {
    return permissionManager.createManagePermissionsIntent(packageName)
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

  fun refreshBatteryReport() {
    viewModelScope.launch {
      _batteryReport.value = batteryMonitorManager.generateBatteryReport()
    }
  }

  fun openBatteryOptimizationSettings(packageName: String? = null) {
    val intent = batteryMonitorManager.createBatteryOptimizationIntent(packageName)
    getApplication<Application>().startActivity(intent)
  }

  fun openAppDetailsSettings(packageName: String) {
    val intent = batteryMonitorManager.createForceStopSettingsIntent(packageName)
    getApplication<Application>().startActivity(intent)
  }

  fun scanAndNotifyHighDrainTasks(
    threshold: Int = BatteryMonitorManager.DEFAULT_DRAIN_THRESHOLD_SCORE,
    bypassCooldown: Boolean = false
  ): List<AppBatteryDrainInfo> {
    val alerted = batteryMonitorManager.checkAndNotifyHighDrainTasks(threshold, bypassCooldown)
    refreshBatteryReport()
    return alerted
  }

  fun signInWithGoogle(activity: Activity) {
    viewModelScope.launch {
      _cloudSyncStatus.value = "Authenticating with Google..."
      authManager.signInWithGoogle(activity)
        .onSuccess { user ->
          _cloudSyncStatus.value = "Signed in as ${user.displayName ?: user.email}"
          firestoreRepository.saveUserProfile(user.displayName, user.email ?: "")
        }
        .onFailure { error ->
          _cloudSyncStatus.value = "Sign-in error: ${error.localizedMessage}"
        }
    }
  }

  fun signOut() {
    viewModelScope.launch {
      authManager.signOut()
      _cloudSyncStatus.value = "Signed out"
    }
  }

  fun syncCurrentThreatReportToCloud(onComplete: (Boolean, String) -> Unit) {
    viewModelScope.launch {
      val user = authManager.currentUser
      if (user == null) {
        onComplete(false, "Please sign in with Google to sync to cloud")
        return@launch
      }

      val audit = _manifestAuditResult.value
      if (audit == null) {
        onComplete(false, "No audit report available to sync")
        return@launch
      }

      runCatching {
        val reportId = firestoreRepository.saveAuditReport(
          packageName = audit.packageName,
          appName = "Audited: ${audit.packageName}",
          riskScore = audit.threatScore,
          riskLevel = audit.threatLevel.name,
          detectedThreats = audit.detectedVulnerabilities
        )
        _cloudSyncStatus.value = "Audit Report synced to Firestore (ID: ${reportId.take(8)})"
        onComplete(true, "Successfully saved to Firebase Firestore!")
      }.onFailure { err ->
        _cloudSyncStatus.value = "Sync failed: ${err.localizedMessage}"
        onComplete(false, "Sync failed: ${err.localizedMessage}")
      }
    }
  }

  fun syncCurrentBatterySnapshotToCloud(onComplete: (Boolean, String) -> Unit) {
    viewModelScope.launch {
      val user = authManager.currentUser
      if (user == null) {
        onComplete(false, "Please sign in with Google to sync to cloud")
        return@launch
      }

      val report = _batteryReport.value ?: batteryMonitorManager.generateBatteryReport()
      runCatching {
        val snapshotId = firestoreRepository.saveBatterySnapshot(
          batteryLevel = report.batteryLevel,
          temperatureCelsius = report.temperatureCelsius.toDouble(),
          chargingType = report.chargingType,
          highDrainAppsCount = report.drainingApps.size
        )
        _cloudSyncStatus.value = "Battery snapshot synced to Firestore (ID: ${snapshotId.take(8)})"
        onComplete(true, "Battery snapshot saved to Firebase Firestore!")
      }.onFailure { err ->
        _cloudSyncStatus.value = "Sync failed: ${err.localizedMessage}"
        onComplete(false, "Sync failed: ${err.localizedMessage}")
      }
    }
  }

  fun openWebApp(app: SavedWebApp) {
    webAppManager.openWebApp(app)
  }

  fun closeActiveWebApp() {
    webAppManager.closeActiveWebApp()
  }

  fun addWebApp(
    name: String,
    url: String,
    category: WebAppCategory,
    keepAlive: Boolean,
    desktopMode: Boolean
  ) {
    webAppManager.addWebApp(
      name = name,
      url = url,
      category = category,
      keepAlive = keepAlive,
      enableNotifications = true,
      desktopMode = desktopMode
    )
  }

  fun deleteWebApp(appId: String) {
    webAppManager.deleteWebApp(appId)
  }

  fun toggleWebAppBackgroundService(enable: Boolean) {
    webAppManager.toggleBackgroundService(enable)
  }

  fun triggerWebNotification(
    appName: String,
    title: String,
    body: String,
    type: WebNotificationType
  ) {
    webAppManager.handleWebNotification(appName, title, body, type)
  }

  // App Cloner & Dual Space Operations
  fun cloneWebApp(
    webApp: SavedWebApp,
    name: String,
    badge: String,
    emoji: String = "👥",
    color: Long = 0xFF10B981L
  ): ClonedAppItem {
    return appClonerManager.createWebAppClone(webApp, name, badge, emoji, color)
  }

  fun cloneNativeApp(
    app: AppPackageItem,
    name: String,
    badge: String,
    emoji: String = "👥",
    color: Long = 0xFF3B82F6L
  ): ClonedAppItem {
    return appClonerManager.createNativeAppClone(app, name, badge, emoji, color)
  }

  fun deleteClone(cloneId: String) {
    appClonerManager.deleteClone(cloneId)
  }

  fun openClone(clone: ClonedAppItem) {
    appClonerManager.openClone(clone)
  }

  fun closeActiveClone() {
    appClonerManager.closeActiveClone()
  }

  fun launchNativeClone(clone: ClonedAppItem): Result<Boolean> {
    return appClonerManager.launchNativeClone(clone)
  }

  fun clearCloneStorage(clone: ClonedAppItem) {
    appClonerManager.clearCloneStorage(clone)
  }

  // APK Backup & Offline Management
  fun backupInstalledPackage(
    packageName: String,
    onResult: (Result<ApkBackupItem>) -> Unit = {}
  ) {
    viewModelScope.launch {
      val res = apkBackupManager.backupInstalledPackage(packageName)
      onResult(res)
    }
  }

  fun backupAllUserPackages(
    onComplete: (List<ApkBackupItem>) -> Unit = {}
  ) {
    viewModelScope.launch {
      val res = apkBackupManager.backupAllUserPackages { current, total, appName ->
        _batchBackupProgress.value = Triple(current, total, appName)
      }
      _batchBackupProgress.value = null
      onComplete(res)
    }
  }

  fun deleteBackup(item: ApkBackupItem): Boolean {
    return apkBackupManager.deleteBackup(item)
  }

  fun clearAllBackups(): Int {
    return apkBackupManager.clearAllBackups()
  }

  fun shareBackup(item: ApkBackupItem): android.content.Intent {
    return apkBackupManager.createShareIntent(item)
  }

  fun verifyBackupIntegrity(item: ApkBackupItem): Boolean {
    return apkBackupManager.verifyBackupIntegrity(item)
  }

  fun refreshBackups() {
    apkBackupManager.loadBackups()
  }

  override fun onCleared() {
    super.onCleared()
    remoteServerManager.stopServer()
  }
}
