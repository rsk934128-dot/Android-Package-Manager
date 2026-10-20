package com.example.manager

import android.content.Context
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class DownloaderManager(private val context: Context) {

  private val scope = CoroutineScope(Dispatchers.IO)
  private var currentDownloadJob: Job? = null

  private val _currentDownload = MutableStateFlow<DownloadItem?>(null)
  val currentDownload: StateFlow<DownloadItem?> = _currentDownload.asStateFlow()

  private val _downloadedFiles = MutableStateFlow<List<File>>(emptyList())
  val downloadedFiles: StateFlow<List<File>> = _downloadedFiles.asStateFlow()

  private val downloadDir: File by lazy {
    val dir = File(context.getExternalFilesDir(null) ?: context.cacheDir, "apk_downloads")
    if (!dir.exists()) dir.mkdirs()
    dir
  }

  init {
    refreshDownloadedFiles()
  }

  fun refreshDownloadedFiles() {
    val files = downloadDir.listFiles { file ->
      file.isFile && (file.extension.equals("apk", ignoreCase = true) || file.extension.equals("bin", ignoreCase = true))
    }?.sortedByDescending { it.lastModified() } ?: emptyList()
    _downloadedFiles.value = files
  }

  fun deleteDownloadedFile(file: File): Boolean {
    val deleted = file.delete()
    refreshDownloadedFiles()
    return deleted
  }

  fun clearAllDownloads() {
    downloadDir.listFiles()?.forEach { it.delete() }
    refreshDownloadedFiles()
  }

  fun startDownload(urlStr: String, customFileName: String? = null) {
    currentDownloadJob?.cancel()

    val fileName = customFileName?.takeIf { it.isNotBlank() }
      ?: extractFileNameFromUrl(urlStr)

    val targetFile = File(downloadDir, fileName)
    val taskId = UUID.randomUUID().toString()

    val initialItem = DownloadItem(
      id = taskId,
      fileName = fileName,
      url = urlStr,
      progress = 0,
      status = DownloadStatus.CONNECTING,
      localFilePath = targetFile.absolutePath
    )
    _currentDownload.value = initialItem

    currentDownloadJob = scope.launch {
      try {
        downloadInternal(urlStr, targetFile)
      } catch (e: CancellationException) {
        _currentDownload.update {
          it?.copy(
            status = DownloadStatus.PAUSED,
            speedText = "0 KB/s"
          )
        }
      } catch (e: Exception) {
        _currentDownload.update {
          it?.copy(
            status = DownloadStatus.FAILED,
            errorMessage = e.localizedMessage ?: "Download failed: Unknown network error",
            speedText = "0 KB/s"
          )
        }
      } finally {
        refreshDownloadedFiles()
      }
    }
  }

  private suspend fun downloadInternal(urlStr: String, targetFile: File) = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
      val url = URL(urlStr)
      connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 15000
        readTimeout = 20000
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", "AppPulseDownloader/1.0 (Android)")
      }

      val responseCode = connection.responseCode
      if (responseCode !in 200..299) {
        throw IllegalStateException("Server returned HTTP $responseCode")
      }

      val contentLength = connection.contentLengthLong
      _currentDownload.update {
        it?.copy(
          status = DownloadStatus.DOWNLOADING,
          totalBytes = if (contentLength > 0) contentLength else 0L
        )
      }

      connection.inputStream.use { input ->
        FileOutputStream(targetFile).use { output ->
          val buffer = ByteArray(8192)
          var bytesRead = 0
          var totalRead = 0L
          var lastTime = System.currentTimeMillis()
          var bytesSinceLastTime = 0L

          while (coroutineContext.isActive) {
            val count = input.read(buffer)
            if (count == -1) break
            output.write(buffer, 0, count)
            totalRead += count
            bytesSinceLastTime += count

            val currentTime = System.currentTimeMillis()
            val timeDelta = currentTime - lastTime

            if (timeDelta >= 500) {
              val speedBytesPerSec = if (timeDelta > 0) (bytesSinceLastTime * 1000) / timeDelta else 0L
              val speedFormatted = formatSpeed(speedBytesPerSec)
              val progress = if (contentLength > 0) {
                ((totalRead * 100) / contentLength).toInt().coerceIn(0, 100)
              } else {
                ((totalRead / (1024 * 1024)) % 100).toInt()
              }

              _currentDownload.update {
                it?.copy(
                  progress = progress,
                  downloadedBytes = totalRead,
                  speedText = speedFormatted
                )
              }

              lastTime = currentTime
              bytesSinceLastTime = 0L
            }
          }
          output.flush()
        }
      }

      _currentDownload.update {
        it?.copy(
          progress = 100,
          status = DownloadStatus.COMPLETED,
          speedText = "Complete",
          localFilePath = targetFile.absolutePath
        )
      }
    } finally {
      connection?.disconnect()
    }
  }

  fun startDemoSimulation(title: String, sampleSizeMb: Int = 15) {
    currentDownloadJob?.cancel()
    val fileName = "${title.lowercase().replace(" ", "_")}_v2.apk"
    val targetFile = File(downloadDir, fileName)

    val item = DownloadItem(
      id = UUID.randomUUID().toString(),
      fileName = fileName,
      url = "https://demo.apppulse.network/releases/$fileName",
      progress = 0,
      totalBytes = sampleSizeMb * 1024L * 1024L,
      downloadedBytes = 0L,
      status = DownloadStatus.DOWNLOADING,
      speedText = "0 KB/s",
      localFilePath = targetFile.absolutePath
    )
    _currentDownload.value = item

    currentDownloadJob = scope.launch {
      val totalBytes = item.totalBytes
      var downloaded = 0L
      val step = totalBytes / 40

      while (isActive && downloaded < totalBytes) {
        delay(120)
        downloaded = (downloaded + step).coerceAtMost(totalBytes)
        val progress = ((downloaded * 100) / totalBytes).toInt()
        val speedMb = (2.4 + (downloaded % 5) * 0.3)
        _currentDownload.update {
          it?.copy(
            progress = progress,
            downloadedBytes = downloaded,
            speedText = String.format("%.1f MB/s", speedMb)
          )
        }
      }

      // Generate a mock test file so installer can point to it
      runCatching {
        if (!targetFile.exists()) {
          targetFile.writeText("AppPulse Sample Package Demo Placeholder\nSize: $sampleSizeMb MB\nDate: ${System.currentTimeMillis()}")
        }
      }

      _currentDownload.update {
        it?.copy(
          progress = 100,
          downloadedBytes = totalBytes,
          status = DownloadStatus.COMPLETED,
          speedText = "Finished",
          localFilePath = targetFile.absolutePath
        )
      }
      refreshDownloadedFiles()
    }
  }

  fun cancelDownload() {
    currentDownloadJob?.cancel()
    _currentDownload.update {
      it?.copy(
        status = DownloadStatus.IDLE,
        speedText = "Cancelled"
      )
    }
  }

  private fun extractFileNameFromUrl(url: String): String {
    return try {
      val path = URL(url).path
      val lastPart = path.substringAfterLast('/')
      if (lastPart.isNotBlank() && lastPart.contains('.')) {
        lastPart
      } else {
        "download_${System.currentTimeMillis()}.apk"
      }
    } catch (e: Exception) {
      "download_${System.currentTimeMillis()}.apk"
    }
  }

  private fun formatSpeed(bytesPerSec: Long): String {
    return when {
      bytesPerSec >= 1024 * 1024 -> String.format("%.2f MB/s", bytesPerSec / (1024.0 * 1024.0))
      bytesPerSec >= 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
      else -> "$bytesPerSec B/s"
    }
  }
}
