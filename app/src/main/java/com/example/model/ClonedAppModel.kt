package com.example.model

enum class CloneType {
  NATIVE_APP,
  WEB_APP_DUAL_SPACE
}

data class ClonedAppItem(
  val id: String = java.util.UUID.randomUUID().toString(),
  val originalPackageOrUrl: String,
  val cloneName: String,
  val cloneBadge: String = "Clone 1", // e.g. "Clone 1", "Work", "Private", "Account 2"
  val clonePackageSuffix: String = "clone1",
  val iconEmoji: String = "👥",
  val cloneType: CloneType = CloneType.WEB_APP_DUAL_SPACE,
  val targetUrl: String? = null,
  val isolatedStorageDir: String = "clone_${System.currentTimeMillis()}",
  val createdAt: Long = System.currentTimeMillis(),
  val launchCount: Int = 0,
  val desktopMode: Boolean = false,
  val colorTintHex: Long = 0xFF10B981L // Emerald default
)
