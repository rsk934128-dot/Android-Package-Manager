package com.example.manager

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.example.model.AppPackageItem
import com.example.model.CloneType
import com.example.model.ClonedAppItem
import com.example.model.SavedWebApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class AppClonerManager(private val context: Context) {

  companion object {
    private const val TAG = "AppClonerManager"
    private const val PREFS_NAME = "apppulse_cloned_apps_prefs"
    private const val KEY_CLONES_JSON = "cloned_apps_json"
  }

  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _clonedApps = MutableStateFlow<List<ClonedAppItem>>(emptyList())
  val clonedApps: StateFlow<List<ClonedAppItem>> = _clonedApps.asStateFlow()

  private val _activeClonedApp = MutableStateFlow<ClonedAppItem?>(null)
  val activeClonedApp: StateFlow<ClonedAppItem?> = _activeClonedApp.asStateFlow()

  init {
    loadClones()
  }

  private fun loadClones() {
    val rawJson = prefs.getString(KEY_CLONES_JSON, null)
    if (rawJson.isNullOrBlank()) {
      // Seed an initial Dual Account clone for Rubel AI Studio
      val seedClone = ClonedAppItem(
        id = UUID.randomUUID().toString(),
        originalPackageOrUrl = WebAppManager.RUBEL_STUDIO_URL,
        cloneName = "Rubel AI Studio (Work Account)",
        cloneBadge = "Work",
        clonePackageSuffix = "work",
        iconEmoji = "💼",
        cloneType = CloneType.WEB_APP_DUAL_SPACE,
        targetUrl = WebAppManager.RUBEL_STUDIO_URL,
        isolatedStorageDir = "clone_rubel_work",
        colorTintHex = 0xFF3B82F6L, // Blue
        desktopMode = false
      )
      val initialList = listOf(seedClone)
      _clonedApps.value = initialList
      saveClonesToDisk(initialList)
    } else {
      runCatching {
        val array = JSONArray(rawJson)
        val list = mutableListOf<ClonedAppItem>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          list.add(
            ClonedAppItem(
              id = obj.optString("id", UUID.randomUUID().toString()),
              originalPackageOrUrl = obj.getString("originalPackageOrUrl"),
              cloneName = obj.getString("cloneName"),
              cloneBadge = obj.optString("cloneBadge", "Clone 1"),
              clonePackageSuffix = obj.optString("clonePackageSuffix", "clone1"),
              iconEmoji = obj.optString("iconEmoji", "👥"),
              cloneType = runCatching { CloneType.valueOf(obj.getString("cloneType")) }.getOrDefault(CloneType.WEB_APP_DUAL_SPACE),
              targetUrl = obj.optString("targetUrl", null).takeIf { it?.isNotBlank() == true },
              isolatedStorageDir = obj.optString("isolatedStorageDir", "clone_default"),
              createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
              launchCount = obj.optInt("launchCount", 0),
              desktopMode = obj.optBoolean("desktopMode", false),
              colorTintHex = obj.optLong("colorTintHex", 0xFF10B981L)
            )
          )
        }
        _clonedApps.value = list
      }.onFailure { e ->
        Log.e(TAG, "Error parsing cloned apps json: ${e.message}")
        _clonedApps.value = emptyList()
      }
    }
  }

  private fun saveClonesToDisk(list: List<ClonedAppItem>) {
    runCatching {
      val array = JSONArray()
      for (item in list) {
        val obj = JSONObject().apply {
          put("id", item.id)
          put("originalPackageOrUrl", item.originalPackageOrUrl)
          put("cloneName", item.cloneName)
          put("cloneBadge", item.cloneBadge)
          put("clonePackageSuffix", item.clonePackageSuffix)
          put("iconEmoji", item.iconEmoji)
          put("cloneType", item.cloneType.name)
          put("targetUrl", item.targetUrl)
          put("isolatedStorageDir", item.isolatedStorageDir)
          put("createdAt", item.createdAt)
          put("launchCount", item.launchCount)
          put("desktopMode", item.desktopMode)
          put("colorTintHex", item.colorTintHex)
        }
        array.put(obj)
      }
      prefs.edit().putString(KEY_CLONES_JSON, array.toString()).apply()
    }
  }

  fun createWebAppClone(
    webApp: SavedWebApp,
    cloneName: String,
    cloneBadge: String = "Clone 1",
    iconEmoji: String = "👥",
    colorTintHex: Long = 0xFF10B981L
  ): ClonedAppItem {
    val dirName = "clone_${webApp.id.take(8)}_${System.currentTimeMillis()}"
    val newClone = ClonedAppItem(
      id = UUID.randomUUID().toString(),
      originalPackageOrUrl = webApp.url,
      cloneName = cloneName.ifBlank { "${webApp.name} ($cloneBadge)" },
      cloneBadge = cloneBadge,
      clonePackageSuffix = cloneBadge.lowercase().replace(" ", ""),
      iconEmoji = iconEmoji,
      cloneType = CloneType.WEB_APP_DUAL_SPACE,
      targetUrl = webApp.url,
      isolatedStorageDir = dirName,
      desktopMode = webApp.desktopMode,
      colorTintHex = colorTintHex
    )
    val updated = _clonedApps.value + newClone
    _clonedApps.value = updated
    saveClonesToDisk(updated)
    return newClone
  }

  fun createNativeAppClone(
    app: AppPackageItem,
    cloneName: String,
    cloneBadge: String = "Clone 1",
    iconEmoji: String = "👥",
    colorTintHex: Long = 0xFF3B82F6L
  ): ClonedAppItem {
    val dirName = "clone_pkg_${app.packageName.replace(".", "_")}_${System.currentTimeMillis()}"
    val newClone = ClonedAppItem(
      id = UUID.randomUUID().toString(),
      originalPackageOrUrl = app.packageName,
      cloneName = cloneName.ifBlank { "${app.appName} ($cloneBadge)" },
      cloneBadge = cloneBadge,
      clonePackageSuffix = "${app.packageName}.${cloneBadge.lowercase().replace(" ", "")}",
      iconEmoji = iconEmoji,
      cloneType = CloneType.NATIVE_APP,
      targetUrl = null,
      isolatedStorageDir = dirName,
      colorTintHex = colorTintHex
    )
    val updated = _clonedApps.value + newClone
    _clonedApps.value = updated
    saveClonesToDisk(updated)
    return newClone
  }

  fun deleteClone(cloneId: String) {
    val clone = _clonedApps.value.firstOrNull { it.id == cloneId }
    if (clone != null) {
      clearCloneStorage(clone)
    }
    val updated = _clonedApps.value.filterNot { it.id == cloneId }
    _clonedApps.value = updated
    saveClonesToDisk(updated)
  }

  fun openClone(clone: ClonedAppItem) {
    recordLaunch(clone.id)
    _activeClonedApp.value = clone
  }

  fun closeActiveClone() {
    _activeClonedApp.value = null
  }

  fun recordLaunch(cloneId: String) {
    val updated = _clonedApps.value.map {
      if (it.id == cloneId) it.copy(launchCount = it.launchCount + 1) else it
    }
    _clonedApps.value = updated
    saveClonesToDisk(updated)
  }

  fun clearCloneStorage(clone: ClonedAppItem) {
    runCatching {
      val cloneDir = File(context.filesDir, "dual_space/${clone.isolatedStorageDir}")
      if (cloneDir.exists()) {
        cloneDir.deleteRecursively()
      }
    }
  }

  fun launchNativeClone(clone: ClonedAppItem): Result<Boolean> {
    recordLaunch(clone.id)
    return runCatching {
      val pm = context.packageManager
      val intent = pm.getLaunchIntentForPackage(clone.originalPackageOrUrl)
      if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        intent.putExtra("is_cloned_instance", true)
        intent.putExtra("clone_id", clone.id)
        intent.putExtra("clone_badge", clone.badgeText())
        context.startActivity(intent)
        true
      } else {
        throw IllegalStateException("Package not installed: ${clone.originalPackageOrUrl}")
      }
    }
  }

  private fun ClonedAppItem.badgeText(): String = cloneBadge
}
