package com.example.manager

import android.content.Context
import android.media.RingtoneManager
import android.os.BatteryManager
import android.os.Build
import com.example.model.SystemDiagnostic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.random.Random

data class RemoteServerState(
  val isRunning: Boolean = false,
  val serverUrl: String = "",
  val securityPin: String = "",
  val activeConnections: Int = 0,
  val lastCommand: String = "None"
)

class RemoteServerManager(
  private val context: Context,
  private val onRemoteDownloadRequested: (url: String, fileName: String?) -> Unit,
  private val diagnosticsProvider: () -> SystemDiagnostic?
) {

  private var serverSocket: ServerSocket? = null
  private var serverJob: Job? = null
  private val scope = CoroutineScope(Dispatchers.IO)

  private val _serverState = MutableStateFlow(
    RemoteServerState(
      securityPin = generateRandomPin()
    )
  )
  val serverState: StateFlow<RemoteServerState> = _serverState.asStateFlow()

  fun startServer(port: Int = 8080): Result<String> {
    return runCatching {
      stopServer()

      val sSocket = ServerSocket().apply {
        reuseAddress = true
        bind(InetSocketAddress(port))
      }
      serverSocket = sSocket

      val currentPin = _serverState.value.securityPin

      serverJob = scope.launch {
        while (isActive && !sSocket.isClosed) {
          try {
            val clientSocket = sSocket.accept()
            launch(Dispatchers.IO) {
              handleClientConnection(clientSocket, currentPin)
            }
          } catch (e: Exception) {
            if (sSocket.isClosed) break
          }
        }
      }

      val diag = diagnosticsProvider()
      val ip = diag?.ipAddress ?: "127.0.0.1"
      val url = "http://$ip:$port"

      _serverState.value = _serverState.value.copy(
        isRunning = true,
        serverUrl = url,
        lastCommand = "Server started"
      )

      url
    }
  }

  fun stopServer() {
    runCatching {
      serverJob?.cancel()
      serverJob = null
      serverSocket?.close()
      serverSocket = null
      _serverState.value = _serverState.value.copy(
        isRunning = false,
        lastCommand = "Server stopped"
      )
    }
  }

  fun playFindDeviceSound() {
    runCatching {
      val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
      val r = RingtoneManager.getRingtone(context, notification)
      r.play()
    }
  }

  private fun handleClientConnection(socket: Socket, pin: String) {
    socket.use { client ->
      try {
        val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
        val output = client.getOutputStream()

        val requestLine = reader.readLine() ?: return
        val parts = requestLine.split(" ")
        if (parts.size < 2) return
        val path = parts[1]

        var contentLength = 0
        var line: String? = reader.readLine()
        while (!line.isNullOrBlank()) {
          if (line.startsWith("Content-Length:", ignoreCase = true)) {
            contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
          }
          line = reader.readLine()
        }

        val body = if (contentLength > 0) {
          val charArray = CharArray(contentLength)
          var readCount = 0
          while (readCount < contentLength) {
            val r = reader.read(charArray, readCount, contentLength - readCount)
            if (r == -1) break
            readCount += r
          }
          String(charArray, 0, readCount)
        } else {
          ""
        }

        when {
          path == "/" || path.startsWith("/?") -> {
            val diag = diagnosticsProvider()
            val html = getDashboardHtml(diag, pin)
            sendResponse(output, 200, "text/html; charset=UTF-8", html.toByteArray(Charsets.UTF_8))
          }
          path.startsWith("/api/status") -> {
            val diag = diagnosticsProvider()
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val batteryLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            val json = """
              {
                "device": "${diag?.deviceModel ?: Build.MODEL}",
                "android": "${diag?.androidVersion ?: Build.VERSION.RELEASE}",
                "battery": $batteryLevel,
                "online": ${diag?.isOnline ?: true},
                "network": "${diag?.networkType ?: "Wi-Fi"}",
                "storageFreeGb": ${(diag?.freeStorageBytes ?: 0) / (1024.0 * 1024.0 * 1024.0)},
                "storageTotalGb": ${(diag?.totalStorageBytes ?: 0) / (1024.0 * 1024.0 * 1024.0)},
                "canInstallApk": ${diag?.canInstallPackages ?: false}
              }
            """.trimIndent()
            sendResponse(output, 200, "application/json; charset=UTF-8", json.toByteArray(Charsets.UTF_8))
          }
          path.startsWith("/api/play-sound") -> {
            playFindDeviceSound()
            _serverState.value = _serverState.value.copy(lastCommand = "Alert Ring Triggered via Web")
            val json = """{"success":true,"message":"Alert sound played on device!"}"""
            sendResponse(output, 200, "application/json; charset=UTF-8", json.toByteArray(Charsets.UTF_8))
          }
          path.startsWith("/api/push-download") -> {
            val url = Regex(""""url"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1)
              ?: body.substringAfter("url=").substringBefore("&").replace("%3A", ":").replace("%2F", "/")
            if (url.isNotBlank()) {
              scope.launch(Dispatchers.Main) {
                onRemoteDownloadRequested(url, null)
              }
              _serverState.value = _serverState.value.copy(lastCommand = "APK Download Pushed: $url")
              val json = """{"success":true,"message":"Download started on Android device!","url":"$url"}"""
              sendResponse(output, 200, "application/json; charset=UTF-8", json.toByteArray(Charsets.UTF_8))
            } else {
              val json = """{"success":false,"message":"Invalid download URL provided."}"""
              sendResponse(output, 400, "application/json; charset=UTF-8", json.toByteArray(Charsets.UTF_8))
            }
          }
          else -> {
            val notFound = "404 Not Found"
            sendResponse(output, 404, "text/plain", notFound.toByteArray(Charsets.UTF_8))
          }
        }
      } catch (_: Exception) {}
    }
  }

  private fun sendResponse(output: OutputStream, statusCode: Int, contentType: String, body: ByteArray) {
    val statusText = when (statusCode) {
      200 -> "OK"
      400 -> "Bad Request"
      404 -> "Not Found"
      else -> "OK"
    }
    val header = "HTTP/1.1 $statusCode $statusText\r\n" +
      "Content-Type: $contentType\r\n" +
      "Content-Length: ${body.size}\r\n" +
      "Access-Control-Allow-Origin: *\r\n" +
      "Connection: close\r\n\r\n"

    output.write(header.toByteArray(Charsets.UTF_8))
    output.write(body)
    output.flush()
  }

  private fun generateRandomPin(): String {
    return (1000 + Random.nextInt(9000)).toString()
  }

  private fun getDashboardHtml(diag: SystemDiagnostic?, pin: String): String {
    return """
      <!DOCTYPE html>
      <html lang="en">
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>AppPulse • Remote Web Management</title>
        <style>
          * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
          body { background: #090D16; color: #F3F4F6; padding: 24px; }
          .container { max-width: 800px; margin: 0 auto; }
          .header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; border-bottom: 1px solid #1F2937; padding-bottom: 16px; }
          .badge { background: #10B98126; color: #10B981; padding: 6px 12px; border-radius: 20px; font-weight: bold; font-size: 13px; }
          .card { background: #111827; border: 1px solid #1F2937; border-radius: 14px; padding: 20px; margin-bottom: 16px; }
          .card h2 { font-size: 16px; color: #00E5FF; margin-bottom: 12px; }
          .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; margin-bottom: 16px; }
          .stat { background: #1F2937; padding: 14px; border-radius: 10px; }
          .stat-val { font-size: 18px; font-weight: bold; margin-top: 4px; color: #FFFFFF; }
          .stat-lbl { font-size: 12px; color: #9CA3AF; }
          button { background: #00E5FF; color: #00363D; border: none; padding: 12px 20px; border-radius: 8px; font-weight: bold; cursor: pointer; font-size: 14px; transition: 0.2s; }
          button:hover { opacity: 0.9; }
          .btn-alarm { background: #EF4444; color: #FFFFFF; margin-top: 8px; }
          input { width: 100%; padding: 12px; background: #1F2937; border: 1px solid #374151; color: white; border-radius: 8px; margin-bottom: 10px; }
        </style>
      </head>
      <body>
        <div class="container">
          <div class="header">
            <div>
              <h1>AppPulse 🛡️ Remote Manager</h1>
              <p style="color:#9CA3AF;font-size:13px;margin-top:4px;">Local Wi-Fi Browser Dashboard • PIN: <strong>$pin</strong></p>
            </div>
            <div class="badge">● CONNECTED</div>
          </div>

          <div class="card">
            <h2>📱 Device Information</h2>
            <div class="grid">
              <div class="stat"><div class="stat-lbl">Device Model</div><div class="stat-val">${diag?.deviceModel ?: Build.MODEL}</div></div>
              <div class="stat"><div class="stat-lbl">Android Version</div><div class="stat-val">${diag?.androidVersion ?: Build.VERSION.RELEASE}</div></div>
              <div class="stat"><div class="stat-lbl">Network</div><div class="stat-val">${diag?.networkType ?: "Wi-Fi"}</div></div>
              <div class="stat"><div class="stat-lbl">Local IP</div><div class="stat-val">${diag?.ipAddress ?: "127.0.0.1"}</div></div>
            </div>
          </div>

          <div class="card">
            <h2>🔔 Find My Device • Remote Alert</h2>
            <p style="color:#9CA3AF;font-size:13px;margin-bottom:12px;">Trigger a loud audible alarm on your smartphone from this browser.</p>
            <button class="btn-alarm" onclick="playSound()">🔊 Play Alarm on Phone</button>
            <span id="sound-status" style="margin-left:12px;font-size:13px;color:#10B981;"></span>
          </div>

          <div class="card">
            <h2>📥 Remote APK Downloader Push</h2>
            <p style="color:#9CA3AF;font-size:13px;margin-bottom:12px;">Push any APK link to your Android phone to start downloading immediately.</p>
            <input type="text" id="apk-url" placeholder="https://example.com/app_update.apk" />
            <button onclick="pushDownload()">🚀 Push & Download on Phone</button>
            <div id="dl-status" style="margin-top:10px;font-size:13px;color:#00E5FF;"></div>
          </div>
        </div>

        <script>
          function playSound() {
            fetch('/api/play-sound', { method: 'POST' })
              .then(r => r.json())
              .then(d => { document.getElementById('sound-status').innerText = '✅ Alarm is ringing on phone!'; })
              .catch(e => { alert('Error playing sound: ' + e); });
          }

          function pushDownload() {
            const url = document.getElementById('apk-url').value;
            if (!url) { alert('Please enter an APK URL'); return; }
            fetch('/api/push-download', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ url: url })
            })
            .then(r => r.json())
            .then(d => { document.getElementById('dl-status').innerText = '✅ ' + d.message; })
            .catch(e => { alert('Error pushing download: ' + e); });
          }
        </script>
      </body>
      </html>
    """.trimIndent()
  }
}
