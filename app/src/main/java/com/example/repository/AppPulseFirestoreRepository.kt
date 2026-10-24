package com.example.repository

import android.content.Context
import com.example.R
import com.example.model.CloudAuditReport
import com.example.model.CloudBatterySnapshot
import com.example.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AppPulseFirestoreRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      FirebaseApp.getInstance(),
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid ?: error("Operation requires authenticated user")
  }

  suspend fun saveUserProfile(displayName: String?, email: String) {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val snapshot = userRef.get().await()

    if (!snapshot.exists()) {
      val payload = mapOf(
        "userId" to uid,
        "email" to email,
        "displayName" to (displayName ?: ""),
        "createdAt" to FieldValue.serverTimestamp()
      )
      userRef.set(payload).await()
    }
  }

  suspend fun saveAuditReport(
    packageName: String,
    appName: String,
    riskScore: Int,
    riskLevel: String,
    detectedThreats: List<String>
  ): String {
    val uid = requireUserId()
    val reportId = UUID.randomUUID().toString()
    val reportRef = db.collection("users").document(uid).collection("audit_reports").document(reportId)

    val payload = mapOf(
      "id" to reportId,
      "userId" to uid,
      "packageName" to packageName,
      "appName" to appName,
      "riskScore" to riskScore,
      "riskLevel" to riskLevel,
      "detectedThreats" to detectedThreats,
      "timestamp" to FieldValue.serverTimestamp()
    )

    reportRef.set(payload).await()
    return reportId
  }

  suspend fun saveBatterySnapshot(
    batteryLevel: Int,
    temperatureCelsius: Double,
    chargingType: String,
    highDrainAppsCount: Int
  ): String {
    val uid = requireUserId()
    val snapshotId = UUID.randomUUID().toString()
    val snapshotRef = db.collection("users").document(uid).collection("battery_snapshots").document(snapshotId)

    val payload = mapOf(
      "id" to snapshotId,
      "userId" to uid,
      "batteryLevel" to batteryLevel,
      "temperatureCelsius" to temperatureCelsius,
      "chargingType" to chargingType,
      "highDrainAppsCount" to highDrainAppsCount,
      "timestamp" to FieldValue.serverTimestamp()
    )

    snapshotRef.set(payload).await()
    return snapshotId
  }

  fun observeAuditReports(userId: String): Flow<List<CloudAuditReport>> {
    return db.collection("users").document(userId).collection("audit_reports")
      .snapshots()
      .map { querySnapshot ->
        querySnapshot.toObjects(CloudAuditReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
  }

  fun observeBatterySnapshots(userId: String): Flow<List<CloudBatterySnapshot>> {
    return db.collection("users").document(userId).collection("battery_snapshots")
      .snapshots()
      .map { querySnapshot ->
        querySnapshot.toObjects(CloudBatterySnapshot::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
  }
}
