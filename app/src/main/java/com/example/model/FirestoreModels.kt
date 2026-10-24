package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
  val userId: String = "",
  val email: String = "",
  val displayName: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudAuditReport(
  @DocumentId val id: String = "",
  val userId: String = "",
  val packageName: String = "",
  val appName: String = "",
  val riskScore: Int = 0,
  val riskLevel: String = "LOW",
  val detectedThreats: List<String> = emptyList(),
  @ServerTimestamp val timestamp: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudBatterySnapshot(
  @DocumentId val id: String = "",
  val userId: String = "",
  val batteryLevel: Int = 0,
  val temperatureCelsius: Double = 0.0,
  val chargingType: String = "",
  val highDrainAppsCount: Int = 0,
  @ServerTimestamp val timestamp: Timestamp? = null
)
