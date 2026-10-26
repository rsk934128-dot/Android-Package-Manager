package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ApkBackupItem
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.RiskHigh
import com.example.ui.theme.RiskSafe
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DownloaderScreen(
  currentDownload: DownloadItem?,
  downloadedFiles: List<File>,
  apkBackups: List<ApkBackupItem> = emptyList(),
  batchBackupProgress: Triple<Int, Int, String>? = null,
  onStartDownload: (String, String?) -> Unit,
  onStartDemo: (String, Int) -> Unit,
  onCancelDownload: () -> Unit,
  onInstallApk: (File) -> Unit,
  onDeleteFile: (File) -> Unit,
  onClearCache: () -> Unit,
  onBackupAllUserApps: () -> Unit = {},
  onDeleteBackup: (ApkBackupItem) -> Unit = {},
  onClearBackups: () -> Unit = {},
  onShareBackup: (ApkBackupItem) -> Unit = {},
  onVerifyBackup: ((ApkBackupItem) -> Boolean)? = null
) {
  var urlText by remember { mutableStateOf("") }
  var fileNameText by remember { mutableStateOf("") }
  val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(8.dp))

      // Hero Banner
      Card(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(16f / 8.5f),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
          Image(
            painter = painterResource(id = R.drawable.hero_app_manager_1792512363531),
            contentDescription = "AppPulse Hero Banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )

          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(
                    Color.Transparent,
                    Color(0xCC090D16),
                    Color(0xF0090D16)
                  )
                )
              )
          )

          Column(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(16.dp)
          ) {
            Text(
              text = "Universal APK Downloader",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = "Background streaming, FileProvider safe installation & security check",
              fontSize = 12.sp,
              color = Color(0xFF94A3B8)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Download Form Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Direct APK URL",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Enter any direct .apk download link to stream and install securely:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = urlText,
            onValueChange = { urlText = it },
            label = { Text("Download URL (https://...)") },
            placeholder = { Text("https://example.com/latest_update.apk") },
            leadingIcon = {
              Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
              if (urlText.isNotBlank()) {
                IconButton(onClick = { urlText = "" }) {
                  Icon(Icons.Default.Close, contentDescription = "Clear URL")
                }
              }
            },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("download_url_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = fileNameText,
            onValueChange = { fileNameText = it },
            label = { Text("Custom File Name (Optional)") },
            placeholder = { Text("sample_update.apk") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("file_name_input")
          )

          Spacer(modifier = Modifier.height(14.dp))

          Button(
            onClick = {
              if (urlText.isNotBlank()) {
                onStartDownload(urlText.trim(), fileNameText.takeIf { it.isNotBlank() })
              }
            },
            enabled = urlText.isNotBlank(),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("start_download_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            )
          ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Background Download", fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Quick Presets
          Text(
            text = "Quick Presets / Testing Channels:",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = {
                urlText = "https://f-droid.org/F-Droid.apk"
                fileNameText = "FDroid_Official.apk"
                onStartDemo("F-Droid Client", 14)
              },
              modifier = Modifier.weight(1f).testTag("preset_fdroid")
            ) {
              Text("F-Droid Demo", fontSize = 11.sp, maxLines = 1)
            }

            OutlinedButton(
              onClick = {
                urlText = "https://github.com/termux/termux-app/releases/download/v0.118.0/termux-app_v0.118.0+github-debug_arm64-v8a.apk"
                fileNameText = "Termux_arm64.apk"
                onStartDemo("Termux App", 24)
              },
              modifier = Modifier.weight(1f).testTag("preset_termux")
            ) {
              Text("Termux Demo", fontSize = 11.sp, maxLines = 1)
            }

            OutlinedButton(
              onClick = {
                urlText = "https://example.com/apppulse_v2.apk"
                fileNameText = "AppPulse_Update_v2.apk"
                onStartDemo("AppPulse v2.0", 12)
              },
              modifier = Modifier.weight(1f).testTag("preset_apppulse")
            ) {
              Text("Update v2.0", fontSize = 11.sp, maxLines = 1)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Active Download Progress Card
      AnimatedVisibility(
        visible = currentDownload != null && currentDownload.status != DownloadStatus.IDLE,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        currentDownload?.let { item ->
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("download_progress_card")
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(40.dp)
                      .clip(CircleShape)
                      .background(
                        when (item.status) {
                          DownloadStatus.COMPLETED -> RiskSafe.copy(alpha = 0.2f)
                          DownloadStatus.FAILED -> RiskHigh.copy(alpha = 0.2f)
                          else -> MaterialTheme.colorScheme.primaryContainer
                        }
                      ),
                    contentAlignment = Alignment.Center
                  ) {
                    when (item.status) {
                      DownloadStatus.COMPLETED -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RiskSafe)
                      DownloadStatus.FAILED -> Icon(Icons.Default.Error, contentDescription = null, tint = RiskHigh)
                      else -> CircularProgressIndicator(
                        progress = { item.progress / 100f },
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                      )
                    }
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(
                      text = item.fileName,
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = "${item.status} • ${item.speedText}",
                      fontSize = 12.sp,
                      color = MaterialTheme.colorScheme.primary
                    )
                  }
                }

                if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.CONNECTING) {
                  IconButton(onClick = onCancelDownload) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel Download", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              LinearProgressIndicator(
                progress = { item.progress / 100f },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(8.dp)
                  .clip(CircleShape),
                color = if (item.status == DownloadStatus.COMPLETED) RiskSafe else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
              )

              Spacer(modifier = Modifier.height(8.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "${item.progress}%",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = formatBytes(item.downloadedBytes) + if (item.totalBytes > 0) " / " + formatBytes(item.totalBytes) else "",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }

              if (item.status == DownloadStatus.COMPLETED && item.localFilePath != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                  onClick = { onInstallApk(File(item.localFilePath)) },
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("install_downloaded_apk_button"),
                  colors = ButtonDefaults.buttonColors(containerColor = EmeraldTertiary)
                ) {
                  Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(20.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Install APK via FileProvider", fontWeight = FontWeight.Bold)
                }
              }

              if (item.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "Error: ${item.errorMessage}",
                  color = RiskHigh,
                  fontSize = 11.sp
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Downloaded APKs Storage Section
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Downloaded Cache (${downloadedFiles.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        if (downloadedFiles.isNotEmpty()) {
          OutlinedButton(
            onClick = onClearCache,
            modifier = Modifier.testTag("clear_cache_button")
          ) {
            Text("Clear All", fontSize = 11.sp)
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
    }

    if (downloadedFiles.isEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              Icons.Default.FileDownload,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "No downloaded APKs in cache",
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "Download an APK or try one of the quick presets above.",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
          }
        }
      }
    } else {
      items(downloadedFiles) { file ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.FileDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = file.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "${formatBytes(file.length())} • ${dateFormat.format(Date(file.lastModified()))}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            IconButton(
              onClick = { onInstallApk(file) },
              modifier = Modifier.testTag("install_file_${file.name}")
            ) {
              Icon(
                Icons.Default.InstallMobile,
                contentDescription = "Install",
                tint = EmeraldTertiary
              )
            }

            IconButton(
              onClick = { onDeleteFile(file) },
              modifier = Modifier.testTag("delete_file_${file.name}")
            ) {
              Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = RiskHigh
              )
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(20.dp))

      // Offline APK Backups Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          Text(
            text = "Offline APK Backups (${apkBackups.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Cloned & archived to external storage",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          FilledTonalButton(
            onClick = onBackupAllUserApps,
            modifier = Modifier.testTag("backup_all_user_apps_button")
          ) {
            Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Backup All", fontSize = 11.sp)
          }

          if (apkBackups.isNotEmpty()) {
            OutlinedButton(
              onClick = onClearBackups,
              modifier = Modifier.testTag("clear_all_backups_button")
            ) {
              Text("Clear", fontSize = 11.sp)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
    }

    // Batch backup progress banner
    if (batchBackupProgress != null) {
      item {
        val (current, total, currentName) = batchBackupProgress
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .testTag("batch_backup_progress_card")
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "Backing up app: $currentName",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Text(
                text = "$current / $total",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
              progress = { if (total > 0) current.toFloat() / total else 0f },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
              color = MaterialTheme.colorScheme.primary,
              trackColor = MaterialTheme.colorScheme.surface
            )
          }
        }
      }
    }

    if (apkBackups.isEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              Icons.Default.Archive,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "No offline APK backups saved yet",
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "Tap 'Backup All' or backup any app from the Inspector tab to save offline copies for restoration or sharing.",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
              modifier = Modifier.padding(top = 4.dp)
            )
          }
        }
      }
    } else {
      items(apkBackups, key = { it.id }) { backup ->
        var isVerified by remember(backup.id) { mutableStateOf<Boolean?>(null) }

        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("apk_backup_item_${backup.packageName}")
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(42.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = backup.appName.take(1).uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                  )
                }
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = backup.appName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                  )
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(start = 6.dp)
                  ) {
                    Text(
                      text = "Code: ${backup.versionCode}",
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                  text = backup.packageName,
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                  text = "v${backup.versionName} • ${formatBytes(backup.fileSizeBytes)} • ${dateFormat.format(Date(backup.backupTimestamp))}",
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action row & Integrity verification
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              // SHA-256 and verification chip
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = when (isVerified) {
                  true -> RiskSafe.copy(alpha = 0.15f)
                  false -> RiskHigh.copy(alpha = 0.15f)
                  null -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.testTag("verify_checksum_${backup.packageName}")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Icon(
                    imageVector = if (isVerified == true) Icons.Default.Verified else Icons.Default.Security,
                    contentDescription = null,
                    tint = if (isVerified == true) RiskSafe else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = when (isVerified) {
                      true -> "SHA-256 Valid"
                      false -> "Checksum Mismatch"
                      null -> "SHA-256: ${backup.sha256Checksum.take(8)}..."
                    },
                    fontSize = 10.sp,
                    color = when (isVerified) {
                      true -> RiskSafe
                      false -> RiskHigh
                      null -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                  )
                }
              }

              // Install, Share, Delete Buttons
              Row(verticalAlignment = Alignment.CenterVertically) {
                // Verify button
                if (isVerified == null && onVerifyBackup != null) {
                  IconButton(
                    onClick = {
                      isVerified = onVerifyBackup(backup)
                    },
                    modifier = Modifier.size(32.dp).testTag("verify_backup_${backup.packageName}")
                  ) {
                    Icon(
                      Icons.Default.Verified,
                      contentDescription = "Verify Integrity",
                      tint = MaterialTheme.colorScheme.outline,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }

                // Install Button
                IconButton(
                  onClick = { onInstallApk(backup.backupFile) },
                  modifier = Modifier.size(32.dp).testTag("install_backup_${backup.packageName}")
                ) {
                  Icon(
                    Icons.Default.InstallMobile,
                    contentDescription = "Install Offline APK",
                    tint = EmeraldTertiary,
                    modifier = Modifier.size(18.dp)
                  )
                }

                // Share / Export Button
                IconButton(
                  onClick = { onShareBackup(backup) },
                  modifier = Modifier.size(32.dp).testTag("share_backup_${backup.packageName}")
                ) {
                  Icon(
                    Icons.Default.Share,
                    contentDescription = "Export / Share Offline APK",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                  )
                }

                // Delete Button
                IconButton(
                  onClick = { onDeleteBackup(backup) },
                  modifier = Modifier.size(32.dp).testTag("delete_backup_${backup.packageName}")
                ) {
                  Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Backup",
                    tint = RiskHigh,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(28.dp))
    }
  }
}

private fun formatBytes(bytes: Long): String {
  if (bytes <= 0) return "0 B"
  val kb = bytes / 1024.0
  val mb = kb / 1024.0
  val gb = mb / 1024.0
  return when {
    gb >= 1.0 -> String.format("%.2f GB", gb)
    mb >= 1.0 -> String.format("%.1f MB", mb)
    kb >= 1.0 -> String.format("%.1f KB", kb)
    else -> "$bytes B"
  }
}
