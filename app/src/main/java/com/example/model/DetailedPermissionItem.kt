package com.example.model

/**
 * Detailed permission representation with real-time grant status, protection level,
 * system descriptions, and security audit assessment.
 */
data class DetailedPermissionItem(
  val fullName: String,
  val simpleName: String,
  val isGranted: Boolean,
  val isDangerous: Boolean,
  val protectionLevel: String,
  val groupName: String,
  val label: String,
  val description: String,
  val riskLevel: ThreatLevel = ThreatLevel.SAFE,
  val riskRecommendation: String = ""
)
