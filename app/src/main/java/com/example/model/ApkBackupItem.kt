package com.example.model

import java.io.File

/**
 * Metadata and file pointer for an installed APK backed up to external storage.
 */
data class ApkBackupItem(
  val id: String = java.util.UUID.randomUUID().toString(),
  val packageName: String,
  val appName: String,
  val versionName: String,
  val versionCode: Long,
  val fileSizeBytes: Long,
  val backupFile: File,
  val backupTimestamp: Long = System.currentTimeMillis(),
  val isSplitApk: Boolean = false,
  val sha256Checksum: String = ""
)
