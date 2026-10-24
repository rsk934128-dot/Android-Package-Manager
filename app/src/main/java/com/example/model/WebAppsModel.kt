package com.example.model

enum class WebAppCategory(val title: String, val iconEmoji: String) {
  AI_ASSISTANT("AI Assistant", "🤖"),
  MESSAGING("Messenger / Chat", "💬"),
  VOICE_CALL("Audio / Video Call", "📞"),
  DEV_TOOLS("Coding & Tools", "💻"),
  GENERAL("General Web App", "🌐")
}

enum class WebNotificationType {
  GENERAL,
  INCOMING_CALL,
  INCOMING_MESSAGE,
  ALERT
}

data class SavedWebApp(
  val id: String,
  val name: String,
  val url: String,
  val category: WebAppCategory = WebAppCategory.GENERAL,
  val keepAliveInBackground: Boolean = true,
  val enableNotifications: Boolean = true,
  val desktopMode: Boolean = false,
  val lastAccessed: Long = System.currentTimeMillis(),
  val createdAt: Long = System.currentTimeMillis(),
  val iconEmoji: String = "🌐"
)

data class WebAppNotificationEvent(
  val id: String = java.util.UUID.randomUUID().toString(),
  val appName: String,
  val title: String,
  val body: String,
  val type: WebNotificationType = WebNotificationType.GENERAL,
  val timestamp: Long = System.currentTimeMillis()
)
