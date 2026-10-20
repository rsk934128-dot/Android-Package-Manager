package com.example.model

enum class DownloadStatus {
  IDLE,
  CONNECTING,
  DOWNLOADING,
  PAUSED,
  COMPLETED,
  FAILED
}

enum class ThreatLevel {
  SAFE,
  MEDIUM,
  HIGH
}

data class DownloadItem(
  val id: String,
  val fileName: String,
  val url: String,
  val progress: Int = 0,
  val totalBytes: Long = 0L,
  val downloadedBytes: Long = 0L,
  val speedText: String = "0 KB/s",
  val status: DownloadStatus = DownloadStatus.IDLE,
  val errorMessage: String? = null,
  val localFilePath: String? = null,
  val timestamp: Long = System.currentTimeMillis()
)

data class PermissionDetail(
  val name: String,
  val isHighRisk: Boolean,
  val category: String,
  val explanation: String
)

data class AppPackageItem(
  val appName: String,
  val packageName: String,
  val versionName: String,
  val versionCode: Long,
  val isSystemApp: Boolean,
  val targetSdk: Int,
  val minSdk: Int = 0,
  val firstInstallTime: Long = 0L,
  val lastUpdateTime: Long = 0L,
  val permissions: List<PermissionDetail> = emptyList(),
  val threatLevel: ThreatLevel = ThreatLevel.SAFE,
  val threatScore: Int = 0,
  val threatReasons: List<String> = emptyList()
)

data class SystemDiagnostic(
  val androidVersion: String,
  val sdkInt: Int,
  val securityPatch: String,
  val deviceModel: String,
  val manufacturer: String,
  val isOnline: Boolean,
  val networkType: String,
  val ipAddress: String,
  val totalStorageBytes: Long,
  val freeStorageBytes: Long,
  val totalRamBytes: Long,
  val freeRamBytes: Long,
  val canInstallPackages: Boolean
)

data class ManifestAuditResult(
  val packageName: String,
  val launcherActivity: String,
  val otherActivities: List<String>,
  val minSdk: Int,
  val fileProviderAuthority: String?,
  val sensitivePermissions: List<String>,
  val spywareKeywordsFound: List<String>,
  val detectedVulnerabilities: List<String>,
  val threatScore: Int, // 0 - 100
  val threatLevel: ThreatLevel,
  val verdictSummary: String
)
