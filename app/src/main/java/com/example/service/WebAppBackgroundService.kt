package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.WebNotificationType

class WebAppBackgroundService : Service() {

  companion object {
    private const val TAG = "WebAppBgService"
    const val CHANNEL_ID_KEEPALIVE = "channel_web_apps_keepalive"
    const val CHANNEL_ID_ALERTS = "channel_web_apps_alerts"
    const val FOREGROUND_NOTIFICATION_ID = 2001
    const val ALERT_NOTIFICATION_ID_BASE = 3000

    const val ACTION_START = "com.example.webapp.ACTION_START"
    const val ACTION_STOP = "com.example.webapp.ACTION_STOP"
    const val ACTION_POST_ALERT = "com.example.webapp.ACTION_POST_ALERT"

    const val EXTRA_APP_NAME = "extra_app_name"
    const val EXTRA_ALERT_TITLE = "extra_alert_title"
    const val EXTRA_ALERT_BODY = "extra_alert_body"
    const val EXTRA_ALERT_TYPE = "extra_alert_type"

    var isRunning: Boolean = false
      private set

    fun start(context: Context, appName: String = "Web Apps") {
      val intent = Intent(context, WebAppBackgroundService::class.java).apply {
        action = ACTION_START
        putExtra(EXTRA_APP_NAME, appName)
      }
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(intent)
        } else {
          context.startService(intent)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to start WebAppBackgroundService: ${e.localizedMessage}")
      }
    }

    fun stop(context: Context) {
      val intent = Intent(context, WebAppBackgroundService::class.java).apply {
        action = ACTION_STOP
      }
      try {
        context.stopService(intent)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to stop WebAppBackgroundService: ${e.localizedMessage}")
      }
    }

    fun postNotificationAlert(
      context: Context,
      appName: String,
      title: String,
      body: String,
      type: WebNotificationType
    ) {
      val intent = Intent(context, WebAppBackgroundService::class.java).apply {
        action = ACTION_POST_ALERT
        putExtra(EXTRA_APP_NAME, appName)
        putExtra(EXTRA_ALERT_TITLE, title)
        putExtra(EXTRA_ALERT_BODY, body)
        putExtra(EXTRA_ALERT_TYPE, type.name)
      }
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(intent)
        } else {
          context.startService(intent)
        }
      } catch (e: Exception) {
        // Fallback: direct notification manager post
        triggerDirectAlert(context, appName, title, body, type)
      }
    }

    fun triggerDirectAlert(
      context: Context,
      appName: String,
      title: String,
      body: String,
      type: WebNotificationType
    ) {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      ensureAlertChannel(context, notificationManager)

      val openIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("nav_tab", 4) // Web Apps tab
      }
      val pendingIntent = PendingIntent.getActivity(
        context,
        System.currentTimeMillis().toInt(),
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val isCall = type == WebNotificationType.INCOMING_CALL
      val soundUri = if (isCall) {
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
      } else {
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      }

      val iconText = if (isCall) "📞 Incoming Call" else "💬 New Alert"
      val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("$iconText: $appName")
        .setContentText(if (title.isNotBlank()) "$title - $body" else body)
        .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n$body"))
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setCategory(if (isCall) NotificationCompat.CATEGORY_CALL else NotificationCompat.CATEGORY_MESSAGE)
        .setAutoCancel(true)
        .setSound(soundUri)
        .setVibrate(longArrayOf(0, 500, 250, 500, 250, 1000))
        .setContentIntent(pendingIntent)
        .build()

      val notificationId = ALERT_NOTIFICATION_ID_BASE + (System.currentTimeMillis() % 1000).toInt()
      notificationManager.notify(notificationId, notification)
    }

    private fun ensureAlertChannel(context: Context, notificationManager: NotificationManager) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val audioAttributes = AudioAttributes.Builder()
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
          .build()

        val alertChannel = NotificationChannel(
          CHANNEL_ID_ALERTS,
          "Web App Calls & Messages Alerts",
          NotificationManager.IMPORTANCE_HIGH
        ).apply {
          description = "Immediate Heads-up notifications for calls, chats, and web events"
          enableLights(true)
          enableVibration(true)
          setSound(soundUri, audioAttributes)
        }
        notificationManager.createNotificationChannel(alertChannel)
      }
    }
  }

  private var wakeLock: PowerManager.WakeLock? = null
  private var currentAppName: String = "Rubel AI & Web Apps"

  override fun onCreate() {
    super.onCreate()
    createNotificationChannels()
    acquireWakeLock()
    isRunning = true
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP -> {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
      }
      ACTION_POST_ALERT -> {
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: currentAppName
        val title = intent.getStringExtra(EXTRA_ALERT_TITLE) ?: "New Alert"
        val body = intent.getStringExtra(EXTRA_ALERT_BODY) ?: "You have an incoming notification"
        val typeStr = intent.getStringExtra(EXTRA_ALERT_TYPE) ?: WebNotificationType.GENERAL.name
        val type = runCatching { WebNotificationType.valueOf(typeStr) }.getOrDefault(WebNotificationType.GENERAL)

        triggerDirectAlert(applicationContext, appName, title, body, type)
      }
      else -> {
        val appName = intent?.getStringExtra(EXTRA_APP_NAME) ?: currentAppName
        currentAppName = appName
        startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification(appName))
      }
    }

    return START_STICKY
  }

  private fun buildForegroundNotification(appName: String): Notification {
    val openIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra("nav_tab", 4)
    }
    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val stopIntent = Intent(this, WebAppBackgroundService::class.java).apply {
      action = ACTION_STOP
    }
    val stopPendingIntent = PendingIntent.getService(
      this,
      1,
      stopIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    return NotificationCompat.Builder(this, CHANNEL_ID_KEEPALIVE)
      .setSmallIcon(android.R.drawable.ic_dialog_info)
      .setContentTitle("🌐 Web Apps Active in Background")
      .setContentText("$appName is running. Real-time call & message alerts active.")
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setOngoing(true)
      .setContentIntent(pendingIntent)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Service", stopPendingIntent)
      .build()
  }

  private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = getSystemService(NotificationManager::class.java)

      val keepAliveChannel = NotificationChannel(
        CHANNEL_ID_KEEPALIVE,
        "Web Apps Background Keep-Alive",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Keeps web applications active in background for background call and message monitoring"
        setShowBadge(false)
      }

      val alertChannel = NotificationChannel(
        CHANNEL_ID_ALERTS,
        "Web App Calls & Messages Alerts",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Immediate Heads-up notifications for calls, chats, and web events"
        enableLights(true)
        enableVibration(true)
      }

      notificationManager.createNotificationChannel(keepAliveChannel)
      notificationManager.createNotificationChannel(alertChannel)
    }
  }

  private fun acquireWakeLock() {
    try {
      val powerManager = getSystemService(POWER_SERVICE) as PowerManager
      wakeLock = powerManager.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        "AppPulse:WebAppBackgroundKeepAlive"
      ).apply {
        setReferenceCounted(false)
        acquire(12 * 60 * 60 * 1000L) // up to 12 hours max safety
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to acquire wake lock: ${e.localizedMessage}")
    }
  }

  private fun releaseWakeLock() {
    try {
      wakeLock?.let {
        if (it.isHeld) it.release()
      }
      wakeLock = null
    } catch (e: Exception) {
      Log.w(TAG, "Failed to release wake lock: ${e.localizedMessage}")
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    releaseWakeLock()
    isRunning = false
  }

  override fun onBind(intent: Intent?): IBinder? = null
}
