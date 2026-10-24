package com.example.manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.SavedWebApp
import com.example.model.WebAppCategory
import com.example.model.WebAppNotificationEvent
import com.example.model.WebNotificationType
import com.example.service.WebAppBackgroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class WebAppManager(private val context: Context) {

  companion object {
    private const val TAG = "WebAppManager"
    private const val PREFS_NAME = "apppulse_saved_web_apps"
    private const val KEY_APPS_JSON = "saved_web_apps_json"
    private const val KEY_SERVICE_ENABLED = "background_service_enabled"

    const val RUBEL_STUDIO_URL = "https://rubel-ai-assistant-coding-studio-563399721287.us-west1.run.app/"
  }

  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _savedWebApps = MutableStateFlow<List<SavedWebApp>>(emptyList())
  val savedWebApps: StateFlow<List<SavedWebApp>> = _savedWebApps.asStateFlow()

  private val _activeWebApp = MutableStateFlow<SavedWebApp?>(null)
  val activeWebApp: StateFlow<SavedWebApp?> = _activeWebApp.asStateFlow()

  private val _isBackgroundServiceActive = MutableStateFlow(false)
  val isBackgroundServiceActive: StateFlow<Boolean> = _isBackgroundServiceActive.asStateFlow()

  private val _recentNotifications = MutableStateFlow<List<WebAppNotificationEvent>>(emptyList())
  val recentNotifications: StateFlow<List<WebAppNotificationEvent>> = _recentNotifications.asStateFlow()

  init {
    loadSavedApps()
    _isBackgroundServiceActive.value = prefs.getBoolean(KEY_SERVICE_ENABLED, false)
    if (_isBackgroundServiceActive.value) {
      val firstApp = _savedWebApps.value.firstOrNull()?.name ?: "Web Apps"
      WebAppBackgroundService.start(context, firstApp)
    }
  }

  private fun loadSavedApps() {
    val rawJson = prefs.getString(KEY_APPS_JSON, null)
    if (rawJson.isNullOrBlank()) {
      // Seed default web apps, starting with user's Rubel AI Studio
      val defaultList = listOf(
        SavedWebApp(
          id = UUID.randomUUID().toString(),
          name = "Rubel AI Coding Studio",
          url = RUBEL_STUDIO_URL,
          category = WebAppCategory.AI_ASSISTANT,
          keepAliveInBackground = true,
          enableNotifications = true,
          desktopMode = false,
          iconEmoji = "🤖"
        ),
        SavedWebApp(
          id = UUID.randomUUID().toString(),
          name = "Telegram Web Messenger",
          url = "https://web.telegram.org/a/",
          category = WebAppCategory.MESSAGING,
          keepAliveInBackground = true,
          enableNotifications = true,
          desktopMode = false,
          iconEmoji = "💬"
        ),
        SavedWebApp(
          id = UUID.randomUUID().toString(),
          name = "Jitsi Free Video & Voice Calls",
          url = "https://meet.jit.si/",
          category = WebAppCategory.VOICE_CALL,
          keepAliveInBackground = true,
          enableNotifications = true,
          desktopMode = false,
          iconEmoji = "📞"
        )
      )
      _savedWebApps.value = defaultList
      saveAppsToDisk(defaultList)
    } else {
      runCatching {
        val jsonArray = JSONArray(rawJson)
        val list = mutableListOf<SavedWebApp>()
        for (i in 0 until jsonArray.length()) {
          val obj = jsonArray.getJSONObject(i)
          list.add(
            SavedWebApp(
              id = obj.optString("id", UUID.randomUUID().toString()),
              name = obj.getString("name"),
              url = obj.getString("url"),
              category = runCatching {
                WebAppCategory.valueOf(obj.optString("category", WebAppCategory.GENERAL.name))
              }.getOrDefault(WebAppCategory.GENERAL),
              keepAliveInBackground = obj.optBoolean("keepAliveInBackground", true),
              enableNotifications = obj.optBoolean("enableNotifications", true),
              desktopMode = obj.optBoolean("desktopMode", false),
              lastAccessed = obj.optLong("lastAccessed", System.currentTimeMillis()),
              createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
              iconEmoji = obj.optString("iconEmoji", "🌐")
            )
          )
        }
        _savedWebApps.value = list
      }.onFailure {
        Log.e(TAG, "Failed parsing saved web apps: ${it.localizedMessage}")
      }
    }
  }

  private fun saveAppsToDisk(apps: List<SavedWebApp>) {
    runCatching {
      val jsonArray = JSONArray()
      for (app in apps) {
        val obj = JSONObject().apply {
          put("id", app.id)
          put("name", app.name)
          put("url", app.url)
          put("category", app.category.name)
          put("keepAliveInBackground", app.keepAliveInBackground)
          put("enableNotifications", app.enableNotifications)
          put("desktopMode", app.desktopMode)
          put("lastAccessed", app.lastAccessed)
          put("createdAt", app.createdAt)
          put("iconEmoji", app.iconEmoji)
        }
        jsonArray.put(obj)
      }
      prefs.edit().putString(KEY_APPS_JSON, jsonArray.toString()).apply()
    }
  }

  fun addWebApp(
    name: String,
    url: String,
    category: WebAppCategory,
    keepAlive: Boolean = true,
    enableNotifications: Boolean = true,
    desktopMode: Boolean = false
  ): SavedWebApp {
    val cleanUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
      "https://$url"
    } else url

    val newApp = SavedWebApp(
      id = UUID.randomUUID().toString(),
      name = name.trim().ifBlank { "My Web App" },
      url = cleanUrl.trim(),
      category = category,
      keepAliveInBackground = keepAlive,
      enableNotifications = enableNotifications,
      desktopMode = desktopMode,
      iconEmoji = category.iconEmoji
    )

    val updatedList = listOf(newApp) + _savedWebApps.value
    _savedWebApps.value = updatedList
    saveAppsToDisk(updatedList)
    return newApp
  }

  fun updateWebApp(updatedApp: SavedWebApp) {
    val updatedList = _savedWebApps.value.map {
      if (it.id == updatedApp.id) updatedApp else it
    }
    _savedWebApps.value = updatedList
    saveAppsToDisk(updatedList)
    if (_activeWebApp.value?.id == updatedApp.id) {
      _activeWebApp.value = updatedApp
    }
  }

  fun deleteWebApp(appId: String) {
    val updatedList = _savedWebApps.value.filter { it.id != appId }
    _savedWebApps.value = updatedList
    saveAppsToDisk(updatedList)
    if (_activeWebApp.value?.id == appId) {
      _activeWebApp.value = null
    }
  }

  fun openWebApp(app: SavedWebApp) {
    val touched = app.copy(lastAccessed = System.currentTimeMillis())
    updateWebApp(touched)
    _activeWebApp.value = touched

    if (touched.keepAliveInBackground && !_isBackgroundServiceActive.value) {
      toggleBackgroundService(true, touched.name)
    }
  }

  fun closeActiveWebApp() {
    _activeWebApp.value = null
  }

  fun toggleBackgroundService(enable: Boolean, appName: String? = null) {
    _isBackgroundServiceActive.value = enable
    prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enable).apply()
    val name = appName ?: _activeWebApp.value?.name ?: _savedWebApps.value.firstOrNull()?.name ?: "Web Apps"
    if (enable) {
      WebAppBackgroundService.start(context, name)
    } else {
      WebAppBackgroundService.stop(context)
    }
  }

  fun handleWebNotification(
    appName: String,
    title: String,
    body: String,
    type: WebNotificationType = WebNotificationType.GENERAL
  ) {
    val event = WebAppNotificationEvent(
      appName = appName,
      title = title,
      body = body,
      type = type
    )
    _recentNotifications.value = (listOf(event) + _recentNotifications.value).take(30)

    // Trigger Android Notification System (Ringtone / Heads-up alert)
    WebAppBackgroundService.postNotificationAlert(
      context = context,
      appName = appName,
      title = title,
      body = body,
      type = type
    )
  }

  fun clearNotificationHistory() {
    _recentNotifications.value = emptyList()
  }
}
