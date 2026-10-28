package com.example.repository

import android.content.Context
import com.example.model.CloudAuditReport
import com.example.model.CloudBatterySnapshot
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AppPulseFirestoreRepository(
  private val context: Context,
  private val customDb: FirebaseFirestore? = null,
  private val customAuth: FirebaseAuth? = null
) {

  constructor(db: FirebaseFirestore, auth: FirebaseAuth) : this(
    context = db.app.applicationContext,
    customDb = db,
    customAuth = auth
  )

  private val app: FirebaseApp?
    get() = runCatching {
      if (FirebaseApp.getApps(context).isEmpty()) {
        FirebaseApp.initializeApp(context)
      }
      FirebaseApp.getInstance()
    }.getOrNull()

  private val auth: FirebaseAuth?
    get() = customAuth ?: runCatching {
      if (FirebaseApp.getApps(context).isEmpty()) {
        FirebaseApp.initializeApp(context)
      }
      FirebaseAuth.getInstance()
    }.getOrNull()

  private val db: FirebaseFirestore?
    get() = customDb ?: runCatching {
      val firebaseApp = app ?: return null
      val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
      val dbId = if (resId != 0) runCatching { context.getString(resId) }.getOrNull() else null

      if (!dbId.isNullOrBlank()) {
        FirebaseFirestore.getInstance(firebaseApp, dbId)
      } else {
        FirebaseFirestore.getInstance(firebaseApp)
      }
    }.getOrNull()

  private fun requireUserId(): String {
    val currentAuth = auth ?: error("Firebase Auth is not available")
    return currentAuth.currentUser?.uid ?: error("Operation requires authenticated user")
  }

  private fun requireDb(): FirebaseFirestore {
    return db ?: error("Firestore database is not available")
  }

  suspend fun saveUserProfile(displayName: String?, email: String) {
    val uid = requireUserId()
    val userRef = requireDb().collection("users").document(uid)
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
    val reportRef = requireDb().collection("users").document(uid).collection("audit_reports").document(reportId)

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
    val snapshotRef = requireDb().collection("users").document(uid).collection("battery_snapshots").document(snapshotId)

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
    val currentDb = db ?: return flowOf(emptyList())
    return currentDb.collection("users").document(userId).collection("audit_reports")
      .snapshots()
      .map { querySnapshot ->
        querySnapshot.toObjects(CloudAuditReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
  }

  fun observeBatterySnapshots(userId: String): Flow<List<CloudBatterySnapshot>> {
    val currentDb = db ?: return flowOf(emptyList())
    return currentDb.collection("users").document(userId).collection("battery_snapshots")
      .snapshots()
      .map { querySnapshot ->
        querySnapshot.toObjects(CloudBatterySnapshot::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
  }
}
