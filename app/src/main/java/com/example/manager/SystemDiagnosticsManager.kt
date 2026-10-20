package com.example.manager

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.model.SystemDiagnostic
import java.net.Inet4Address
import java.net.NetworkInterface

class SystemDiagnosticsManager(private val context: Context) {

  fun getSystemDiagnostics(canInstallPackages: Boolean): SystemDiagnostic {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val activeNetwork = cm?.activeNetwork
    val caps = cm?.getNetworkCapabilities(activeNetwork)

    val isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    val networkType = when {
      caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi (High Speed)"
      caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular Mobile Data"
      caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet LAN"
      else -> if (isOnline) "Connected" else "Offline"
    }

    val ipAddress = getLocalIpAddress()

    // Storage
    val dataPath = Environment.getDataDirectory().path
    val stat = StatFs(dataPath)
    val totalStorage = stat.blockSizeLong * stat.blockCountLong
    val freeStorage = stat.blockSizeLong * stat.availableBlocksLong

    // RAM
    val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    actManager?.getMemoryInfo(memInfo)
    val totalRam = memInfo.totalMem
    val freeRam = memInfo.availMem

    val patchLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      Build.VERSION.SECURITY_PATCH
    } else {
      "Standard"
    }

    return SystemDiagnostic(
      androidVersion = "Android ${Build.VERSION.RELEASE}",
      sdkInt = Build.VERSION.SDK_INT,
      securityPatch = patchLevel,
      deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
      manufacturer = Build.MANUFACTURER,
      isOnline = isOnline,
      networkType = networkType,
      ipAddress = ipAddress,
      totalStorageBytes = totalStorage,
      freeStorageBytes = freeStorage,
      totalRamBytes = totalRam,
      freeRamBytes = freeRam,
      canInstallPackages = canInstallPackages
    )
  }

  private fun getLocalIpAddress(): String {
    return runCatching {
      val interfaces = NetworkInterface.getNetworkInterfaces()
      while (interfaces.hasMoreElements()) {
        val intf = interfaces.nextElement()
        val addrs = intf.inetAddresses
        while (addrs.hasMoreElements()) {
          val addr = addrs.nextElement()
          if (!addr.isLoopbackAddress && addr is Inet4Address) {
            return addr.hostAddress ?: "Unknown"
          }
        }
      }
      "127.0.0.1"
    }.getOrDefault("127.0.0.1")
  }
}
