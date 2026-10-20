package com.example.manager

import android.content.Context
import android.media.RingtoneManager
import android.os.BatteryManager
import android.os.Build
import com.example.model.SystemDiagnostic
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.net.InetSocketAddress
import java.util.concurrent.Executors
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

  private var httpServer: HttpServer? = null
  private val scope = CoroutineScope(Dispatchers.IO)

  private val _serverState = MutableStateFlow(
    RemoteServerState(
      securityPin = generateRandomPin()
    )
  )
  val serverState: StateFlow<RemoteServerState> = _serverState.asStateFlow()

  fun startServer(port: Int = 8080): Result<String> {
    return runCatching {
      if (httpServer != null) {
        stopServer()
      }

      val server = HttpServer.create(InetSocketAddress(port), 0)
      server.executor = Executors.newCachedThreadPool()

      val currentPin = _serverState.value.securityPin

      // 1. Root HTML Web Dashboard
      server.createContext("/") { exchange ->
        handleDashboardRequest(exchange, currentPin)
      }

      // 2. Telemetry API
      server.createContext("/api/status") { exchange ->
        handleStatusApi(exchange, currentPin)
      }

      // 3. Play Sound (Find My Device)
      server.createContext("/api/play-sound") { exchange ->
        handlePlaySoundApi(exchange, currentPin)
      }

      // 4. Remote APK Download Push
      server.createContext("/api/push-download") { exchange ->
        handlePushDownloadApi(exchange, currentPin)
      }

      server.start()
      httpServer = server

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
      httpServer?.stop(0)
      httpServer = null
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

  private fun handleDashboardRequest(exchange: HttpExchange, pin: String) {
    val diag = diagnosticsProvider()
    val html = getDashboardHtml(diag, pin)
    val bytes = html.toByteArray(Charsets.UTF_8)
    exchange.responseHeaders.set("Content-Type", "text/html; charset=UTF-8")
    exchange.sendResponseHeaders(200, bytes.size.toLong())
    exchange.responseBody.use { it.write(bytes) }
  }

  private fun handleStatusApi(exchange: HttpExchange, pin: String) {
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

    sendJsonResponse(exchange, json)
  }

  private fun handlePlaySoundApi(exchange: HttpExchange, pin: String) {
    if (exchange.requestMethod.equals("POST", ignoreCase = true) || exchange.requestMethod.equals("GET", ignoreCase = true)) {
      playFindDeviceSound()
      _serverState.value = _serverState.value.copy(lastCommand = "Alert Ring Triggered via Web")
      sendJsonResponse(exchange, """{"success":true,"message":"Alert sound played on device!"}""")
    } else {
      exchange.sendResponseHeaders(405, -1)
    }
  }

  private fun handlePushDownloadApi(exchange: HttpExchange, pin: String) {
    if (exchange.requestMethod.equals("POST", ignoreCase = true)) {
      val body = exchange.requestBody.bufferedReader().use { it.readText() }
      val url = Regex(""""url"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1)
        ?: body.substringAfter("url=").substringBefore("&").replace("%3A", ":").replace("%2F", "/")

      if (url.isNotBlank()) {
        scope.launch(Dispatchers.Main) {
          onRemoteDownloadRequested(url, null)
        }
        _serverState.value = _serverState.value.copy(lastCommand = "APK Download Pushed: $url")
        sendJsonResponse(exchange, """{"success":true,"message":"Download started on Android device!","url":"$url"}""")
        return
      }
    }
    sendJsonResponse(exchange, """{"success":false,"message":"Invalid download URL provided."}""")
  }

  private fun sendJsonResponse(exchange: HttpExchange, json: String) {
    val bytes = json.toByteArray(Charsets.UTF_8)
    exchange.responseHeaders.set("Content-Type", "application/json; charset=UTF-8")
    exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
    exchange.sendResponseHeaders(200, bytes.size.toLong())
    exchange.responseBody.use { it.write(bytes) }
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
