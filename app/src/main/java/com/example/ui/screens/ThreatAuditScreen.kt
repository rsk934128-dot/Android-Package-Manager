package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ManifestAuditResult
import com.example.model.ThreatLevel
import com.example.ui.theme.RiskHigh
import com.example.ui.theme.RiskMedium
import com.example.ui.theme.RiskSafe

import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync

@Composable
fun ThreatAuditScreen(
  manifestInput: String,
  auditResult: ManifestAuditResult?,
  defaultSampleText: String,
  onInputChanged: (String) -> Unit,
  onRunAudit: (String) -> Unit,
  onSaveToCloud: (() -> Unit)? = null
) {
  var showInputEditor by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(8.dp))

      // Header Banner
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(RiskHigh.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Shield, contentDescription = null, tint = RiskHigh, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "APK Manifest & Threat Auditor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Detects App Droppers, Stalkerware & Malicious Loaders",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = { onRunAudit(manifestInput) },
              modifier = Modifier
                .weight(1f)
                .testTag("run_audit_button"),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
              Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Audit", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            onSaveToCloud?.let { saveAction ->
              Button(
                onClick = saveAction,
                modifier = Modifier
                  .weight(1.1f)
                  .testTag("save_cloud_audit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
              ) {
                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sync Cloud", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }

            OutlinedButton(
              onClick = { showInputEditor = !showInputEditor },
              modifier = Modifier.testTag("toggle_editor_button")
            ) {
              Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (showInputEditor) "Hide" else "Raw", fontSize = 12.sp)
            }
          }
        }
      }

      if (showInputEditor) {
        Spacer(modifier = Modifier.height(12.dp))
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "Raw Manifest / String Table Input:",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
              value = manifestInput,
              onValueChange = onInputChanged,
              modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .testTag("raw_manifest_input"),
              textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
              onClick = {
                onInputChanged(defaultSampleText)
                onRunAudit(defaultSampleText)
              },
              modifier = Modifier.align(Alignment.End)
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Reset to Sample (com.mtf.app)", fontSize = 11.sp)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }

    // Results presentation
    auditResult?.let { res ->
      item {
        val (scoreColor, scoreLabel) = when (res.threatLevel) {
          ThreatLevel.HIGH -> Pair(RiskHigh, "CRITICAL RISK (Dropper / Stalkerware)")
          ThreatLevel.MEDIUM -> Pair(RiskMedium, "MEDIUM ELEVATED RISK")
          ThreatLevel.SAFE -> Pair(RiskSafe, "VERIFIED CLEAN")
        }

        // Threat Score Hero Box
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = scoreColor.copy(alpha = 0.12f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("audit_score_card")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column {
                Text(
                  text = scoreLabel,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 13.sp,
                  color = scoreColor
                )
                Text(
                  text = "Security Audit Rating",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }

              Box(
                modifier = Modifier
                  .clip(CircleShape)
                  .background(scoreColor.copy(alpha = 0.2f))
                  .padding(horizontal = 14.dp, vertical = 6.dp)
              ) {
                Text(
                  text = "${res.threatScore}/100",
                  fontWeight = FontWeight.Black,
                  fontSize = 18.sp,
                  color = scoreColor
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = res.verdictSummary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Target Package Metadata
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "Target Package Identification",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow("Target Package", res.packageName)
            InfoRow("Launcher Activity", res.launcherActivity)
            InfoRow("Min SDK", "${res.minSdk} (Supports legacy Android 4.2+)")
            InfoRow("FileProvider Authority", res.fileProviderAuthority ?: "None")
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Vulnerabilities & Behaviors Detected
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.BugReport, contentDescription = null, tint = RiskHigh, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Identified Malicious Behaviors (${res.detectedVulnerabilities.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            res.detectedVulnerabilities.forEach { vuln ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top
              ) {
                Box(
                  modifier = Modifier
                    .padding(top = 4.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(RiskHigh)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = vuln,
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Suspicious Tracking Markers Found
        if (res.spywareKeywordsFound.isNotEmpty()) {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = RiskMedium, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Embedded Surveillance Markers (${res.spywareKeywordsFound.size})",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              res.spywareKeywordsFound.forEach { kw ->
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                  Text(
                    text = kw,
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
        }

        // Analyst Recommendation Box (Bengali & English)
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Policy, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Security Recommendation & Next Steps",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "১. Static Analysis (JADX-GUI): Decompile DEX and smali bytecode to inspect payload URL and dropper triggers.\n২. Sandbox Isolation: Do not install suspicious payloads on physical personal devices; use an isolated VM/emulator.\n৩. Remove Permissions: Revoke Unknown Sources installation privileges immediately.",
              fontSize = 12.sp,
              lineHeight = 18.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
private fun InfoRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = label,
      fontSize = 12.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}
