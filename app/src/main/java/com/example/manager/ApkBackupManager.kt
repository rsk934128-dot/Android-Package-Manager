package com.example.manager

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.example.model.ApkBackupItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

/**
 * Manager to extract, copy, and manage installed APK files to external storage
 * for offline management, reinstallation, and export.
 */
class ApkBackupManager(private val context: Context) {

  companion object {
    private const val TAG = "ApkBackupManager"
    private const val BACKUP_DIR_NAME = "apk_backups"
    private const val PREFS_NAME = "apppulse_apk_backups_prefs"
    private const val KEY_BACKUPS_JSON = "backups_json"
  }

  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _backups = MutableStateFlow<List<ApkBackupItem>>(emptyList())
  val backups: StateFlow<List<ApkBackupItem>> = _backups.asStateFlow()

  val backupDirectory: File
    get() {
      val dir = context.getExternalFilesDir(BACKUP_DIR_NAME)
        ?: File(context.filesDir, BACKUP_DIR_NAME)
      if (!dir.exists()) {
        dir.mkdirs()
      }
      return dir
    }

  init {
    loadBackups()
  }

  /**
   * Loads backed up APK records from storage and synchronizes with actual disk files.
   */
  fun loadBackups() {
    val dir = backupDirectory
    val rawJson = prefs.getString(KEY_BACKUPS_JSON, null)
    val list = mutableListOf<ApkBackupItem>()

    if (!rawJson.isNullOrBlank()) {
      runCatching {
        val array = JSONArray(rawJson)
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          val filePath = obj.getString("backupFilePath")
          val file = File(filePath)
          if (file.exists() && file.length() > 0L) {
            list.add(
              ApkBackupItem(
                id = obj.optString("id", UUID.randomUUID().toString()),
                packageName = obj.getString("packageName"),
                appName = obj.getString("appName"),
                versionName = obj.optString("versionName", "1.0"),
                versionCode = obj.optLong("versionCode", 1L),
                fileSizeBytes = file.length(),
                backupFile = file,
                backupTimestamp = obj.optLong("backupTimestamp", file.lastModified()),
                isSplitApk = obj.optBoolean("isSplitApk", false),
                sha256Checksum = obj.optString("sha256Checksum", "")
              )
            )
          }
        }
      }.onFailure { e ->
        Log.e(TAG, "Error parsing backup metadata: ${e.message}")
      }
    }

    // Also pick up any unindexed APK files located in the backup directory
    val diskApkFiles = dir.listFiles { f -> f.extension.equals("apk", ignoreCase = true) } ?: emptyArray()
    for (apkFile in diskApkFiles) {
      if (list.none { it.backupFile.absolutePath == apkFile.absolutePath }) {
        val parsedPkg = try {
          context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
        } catch (e: Exception) {
          null
        }
        val pkgName = parsedPkg?.packageName ?: apkFile.nameWithoutExtension
        val vCode = parsedPkg?.let { PackageInfoCompat.getLongVersionCode(it) } ?: 1L
        val vName = parsedPkg?.versionName ?: "Unknown"

        list.add(
          ApkBackupItem(
            id = UUID.randomUUID().toString(),
            packageName = pkgName,
            appName = apkFile.nameWithoutExtension.replace("_", " "),
            versionName = vName,
            versionCode = vCode,
            fileSizeBytes = apkFile.length(),
            backupFile = apkFile,
            backupTimestamp = apkFile.lastModified(),
            sha256Checksum = ""
          )
        )
      }
    }

    _backups.value = list.sortedByDescending { it.backupTimestamp }
    saveBackupsToDisk(list)
  }

  private fun saveBackupsToDisk(list: List<ApkBackupItem>) {
    runCatching {
      val array = JSONArray()
      for (item in list) {
        val obj = JSONObject().apply {
          put("id", item.id)
          put("packageName", item.packageName)
          put("appName", item.appName)
          put("versionName", item.versionName)
          put("versionCode", item.versionCode)
          put("backupFilePath", item.backupFile.absolutePath)
          put("backupTimestamp", item.backupTimestamp)
          put("isSplitApk", item.isSplitApk)
          put("sha256Checksum", item.sha256Checksum)
        }
        array.put(obj)
      }
      prefs.edit().putString(KEY_BACKUPS_JSON, array.toString()).apply()
    }
  }

  /**
   * Copies the installed APK from [ApplicationInfo.sourceDir] to the external storage backup directory.
   */
  suspend fun backupInstalledPackage(packageName: String): Result<ApkBackupItem> = withContext(Dispatchers.IO) {
    runCatching {
      val pm = context.packageManager
      val pkgInfo: PackageInfo = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
          @Suppress("DEPRECATION")
          pm.getPackageInfo(packageName, 0)
        }
      } catch (e: Exception) {
        throw IllegalArgumentException("Package not found on system: $packageName", e)
      }

      val appInfo = pkgInfo.applicationInfo
        ?: throw IllegalStateException("ApplicationInfo is null for $packageName")

      val sourcePath = appInfo.sourceDir
      if (sourcePath.isNullOrBlank()) {
        throw IllegalStateException("sourceDir is null or empty for $packageName")
      }

      val sourceApkFile = File(sourcePath)
      if (!sourceApkFile.exists() || sourceApkFile.length() == 0L) {
        throw IllegalStateException("Source APK file not found or inaccessible at $sourcePath")
      }

      val appLabel = try {
        appInfo.loadLabel(pm).toString()
      } catch (e: Exception) {
        packageName
      }

      val safeName = appLabel.replace(Regex("[^a-zA-Z0-9_-]"), "_").trim('_')
      val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)
      val versionName = pkgInfo.versionName ?: "1.0"
      val destFileName = "${safeName}_v${versionName}_${versionCode}.apk"
      val destFile = File(backupDirectory, destFileName)

      // Copy source APK to external storage backup location
      copyFile(sourceApkFile, destFile)

      // Compute SHA-256 for integrity verification
      val checksum = calculateSha256(destFile)

      val backupItem = ApkBackupItem(
        id = UUID.randomUUID().toString(),
        packageName = packageName,
        appName = appLabel,
        versionName = versionName,
        versionCode = versionCode,
        fileSizeBytes = destFile.length(),
        backupFile = destFile,
        backupTimestamp = System.currentTimeMillis(),
        isSplitApk = !appInfo.splitSourceDirs.isNullOrEmpty(),
        sha256Checksum = checksum
      )

      val updated = (_backups.value.filterNot { it.packageName == packageName && it.versionCode == versionCode } + backupItem)
        .sortedByDescending { it.backupTimestamp }

      _backups.value = updated
      saveBackupsToDisk(updated)
      backupItem
    }
  }

  /**
   * Backs up all non-system installed applications sequentially.
   */
  suspend fun backupAllUserPackages(
    onProgress: (current: Int, total: Int, appName: String) -> Unit
  ): List<ApkBackupItem> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val packages = try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
      } else {
        @Suppress("DEPRECATION")
        pm.getInstalledPackages(0)
      }
    } catch (e: Exception) {
      emptyList()
    }

    val userPackages = packages.filter {
      (it.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM == 0
    }

    val results = mutableListOf<ApkBackupItem>()
    userPackages.forEachIndexed { index, pkg ->
      val label = pkg.applicationInfo?.loadLabel(pm)?.toString() ?: pkg.packageName
      onProgress(index + 1, userPackages.size, label)
      backupInstalledPackage(pkg.packageName).onSuccess {
        results.add(it)
      }
    }
    results
  }

  fun deleteBackup(item: ApkBackupItem): Boolean {
    val fileDeleted = runCatching {
      if (item.backupFile.exists()) {
        item.backupFile.delete()
      } else {
        true
      }
    }.getOrDefault(false)

    val updated = _backups.value.filterNot { it.id == item.id }
    _backups.value = updated
    saveBackupsToDisk(updated)
    return fileDeleted
  }

  fun clearAllBackups(): Int {
    var count = 0
    for (item in _backups.value) {
      if (item.backupFile.exists()) {
        if (item.backupFile.delete()) count++
      }
    }
    _backups.value = emptyList()
    saveBackupsToDisk(emptyList())
    return count
  }

  /**
   * Creates an Android Share Sheet Intent via [FileProvider] to export/share the APK offline.
   */
  fun createShareIntent(item: ApkBackupItem): Intent {
    val uri: Uri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.provider",
      item.backupFile
    )

    return Intent(Intent.ACTION_SEND).apply {
      type = "application/vnd.android.package-archive"
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, "APK Backup: ${item.appName}")
      putExtra(Intent.EXTRA_TEXT, "Offline APK Backup of ${item.appName} v${item.versionName} (Package: ${item.packageName})")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  }

  /**
   * Re-evaluates SHA-256 to ensure the stored backup file has not been corrupted or tampered with.
   */
  fun verifyBackupIntegrity(item: ApkBackupItem): Boolean {
    if (!item.backupFile.exists() || item.sha256Checksum.isBlank()) return false
    val currentChecksum = calculateSha256(item.backupFile)
    return currentChecksum.equals(item.sha256Checksum, ignoreCase = true)
  }

  private fun copyFile(src: File, dst: File) {
    FileInputStream(src).use { inStream ->
      FileOutputStream(dst).use { outStream ->
        val buffer = ByteArray(64 * 1024)
        var bytesRead: Int
        while (inStream.read(buffer).also { bytesRead = it } != -1) {
          outStream.write(buffer, 0, bytesRead)
        }
        outStream.flush()
      }
    }
  }

  private fun calculateSha256(file: File): String {
    return try {
      val digest = MessageDigest.getInstance("SHA-256")
      FileInputStream(file).use { fis ->
        val buffer = ByteArray(64 * 1024)
        var bytesRead: Int
        while (fis.read(buffer).also { bytesRead = it } != -1) {
          digest.update(buffer, 0, bytesRead)
        }
      }
      digest.digest().joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
      ""
    }
  }
}
