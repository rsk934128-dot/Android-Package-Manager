package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AppPulse", appName)
  }

  @Test
  fun `test manifest audit detects dropper and stalkerware`() {
    val sampleText = """
      Package: com.mtf.app
      android.permission.REQUEST_INSTALL_PACKAGES
      android.permission.REQUEST_DELETE_PACKAGES
      android.permission.DUMP
      mobile.monitor.child2022
      com.protect2025
    """.trimIndent()

    val context = ApplicationProvider.getApplicationContext<Context>()
    val engine = com.example.manager.SecurityAuditEngine(context)
    val result = engine.auditManifestText(sampleText)

    assertEquals(com.example.model.ThreatLevel.HIGH, result.threatLevel)
    org.junit.Assert.assertTrue(result.threatScore >= 60)
  }

  @Test
  fun `test battery monitor manager generates health report`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val batteryManager = com.example.manager.BatteryMonitorManager(context)
    val report = batteryManager.generateBatteryReport()

    org.junit.Assert.assertNotNull(report)
    org.junit.Assert.assertTrue(report.batteryLevel in 0..100)
    org.junit.Assert.assertTrue(report.recommendations.isNotEmpty())

    val optIntent = batteryManager.createBatteryOptimizationIntent("com.example.test")
    org.junit.Assert.assertNotNull(optIntent)
  }

  @Test
  fun `test battery notification system and threshold trigger`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val batteryManager = com.example.manager.BatteryMonitorManager(context)

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val channel = notificationManager.getNotificationChannel(com.example.manager.BatteryMonitorManager.CHANNEL_ID_BATTERY_ALERTS)
    org.junit.Assert.assertNotNull(channel)
    org.junit.Assert.assertEquals(android.app.NotificationManager.IMPORTANCE_HIGH, channel.importance)

    val alerted = batteryManager.checkAndNotifyHighDrainTasks(thresholdDrainScore = 20, bypassCooldown = true)
    org.junit.Assert.assertNotNull(alerted)
  }

  @Test
  fun `test WebContainerController sanitizes malicious schemes and formats urls`() {
    val controller = com.example.ui.components.WebContainerController()

    // Test https preservation
    assertEquals("https://rubel-ai-assistant-coding-studio-563399721287.us-west1.run.app/",
      controller.sanitizeUrl("https://rubel-ai-assistant-coding-studio-563399721287.us-west1.run.app/"))

    // Test unadorned domain prepends https
    assertEquals("https://example.com", controller.sanitizeUrl("example.com"))

    // Test malicious schemes neutralized
    assertEquals("about:blank", controller.sanitizeUrl("javascript:alert(1)"))
    assertEquals("about:blank", controller.sanitizeUrl("file:///data/data/com.example/databases/db"))
    assertEquals("about:blank", controller.sanitizeUrl("content://media/external/images/media"))
    assertEquals("about:blank", controller.sanitizeUrl("data:text/html,<html>Evil</html>"))

    // Test initial states
    org.junit.Assert.assertTrue(controller.isSecureConnection)
    org.junit.Assert.assertFalse(controller.canGoBack)
    org.junit.Assert.assertFalse(controller.canGoForward)
    org.junit.Assert.assertFalse(controller.isLoading)
  }

  @Test
  fun `test WebContainerController domain whitelisting and wildcard matching`() {
    val controller = com.example.ui.components.WebContainerController(
      initialAllowedDomains = listOf("*.run.app", "google.com", "myportal.net"),
      initialWhitelistEnforced = true
    )

    // Allowed domains
    org.junit.Assert.assertTrue(controller.isDomainAllowed("https://rubel-ai-assistant-coding-studio-563399721287.us-west1.run.app/"))
    org.junit.Assert.assertTrue(controller.isDomainAllowed("https://sub.run.app/page"))
    org.junit.Assert.assertTrue(controller.isDomainAllowed("https://google.com/search"))
    org.junit.Assert.assertTrue(controller.isDomainAllowed("https://mail.google.com/"))
    org.junit.Assert.assertTrue(controller.isDomainAllowed("myportal.net"))

    // Disallowed domains
    org.junit.Assert.assertFalse(controller.isDomainAllowed("https://malicious-phishing.com/login"))
    org.junit.Assert.assertFalse(controller.isDomainAllowed("https://run.app.evil.org/"))
    org.junit.Assert.assertFalse(controller.isDomainAllowed("https://unknown-domain.io/"))

    // Add domain on the fly
    controller.addAllowedDomain("github.com")
    org.junit.Assert.assertTrue(controller.isDomainAllowed("https://github.com/repository"))

    // Remove domain
    controller.removeAllowedDomain("github.com")
    org.junit.Assert.assertFalse(controller.isDomainAllowed("https://github.com/repository"))

    // When whitelist enforcement is disabled, all domains are permitted
    controller.isWhitelistEnforced = false
    org.junit.Assert.assertTrue(controller.isDomainAllowed("https://anywhere.org"))
  }

  @Test
  fun `test WebContainerController insecure content blocking`() {
    val controller = com.example.ui.components.WebContainerController(
      initialBlockInsecureContent = true
    )

    // HTTP should be flagged as insecure
    org.junit.Assert.assertTrue(controller.isInsecureScheme("http://example.com/login"))
    org.junit.Assert.assertTrue(controller.isInsecureScheme("http://unencrypted-site.org"))

    // HTTPS should be considered secure
    org.junit.Assert.assertFalse(controller.isInsecureScheme("https://example.com/login"))
    org.junit.Assert.assertFalse(controller.isInsecureScheme("https://rubel-ai-assistant-coding-studio-563399721287.us-west1.run.app"))

    // Localhost / 127.0.0.1 HTTP allowed for debugging
    org.junit.Assert.assertFalse(controller.isInsecureScheme("http://localhost:8080/"))
    org.junit.Assert.assertFalse(controller.isInsecureScheme("http://127.0.0.1:3000/"))

    // When loading an insecure HTTP url with blockInsecureContent = true, it gets blocked with error message
    controller.loadUrl("http://insecure-site.com")
    org.junit.Assert.assertNotNull(controller.errorMessage)
    org.junit.Assert.assertTrue(controller.errorMessage!!.contains("Insecure"))
  }

  @Test
  fun `test WebContainerController clear cache and cookies callbacks`() {
    val controller = com.example.ui.components.WebContainerController()

    var cacheCleared = false
    controller.clearCache(includeDiskFiles = true) {
      cacheCleared = true
    }
    org.junit.Assert.assertTrue(cacheCleared)

    var cookiesCleared = false
    controller.clearCookies {
      cookiesCleared = true
    }
    org.junit.Assert.assertTrue(cookiesCleared)

    var allDataWiped = false
    controller.clearAllBrowsingData {
      allDataWiped = true
    }
    org.junit.Assert.assertTrue(allDataWiped)
  }

  @Test
  fun `test AppClonerManager creates and tracks web and native clones`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val clonerManager = com.example.manager.AppClonerManager(context)

    // Initial clones list should contain seed or be valid
    val initialCount = clonerManager.clonedApps.value.size

    // Test creating a web app clone
    val sampleWebApp = com.example.model.SavedWebApp(
      id = "test_web_app",
      name = "Rubel AI Studio",
      url = "https://rubel-ai-assistant-coding-studio-563399721287.us-west1.run.app/"
    )

    val webClone = clonerManager.createWebAppClone(
      webApp = sampleWebApp,
      cloneName = "Rubel AI Studio (Work)",
      cloneBadge = "Work",
      iconEmoji = "💼",
      colorTintHex = 0xFF3B82F6L
    )

    org.junit.Assert.assertNotNull(webClone)
    assertEquals("Rubel AI Studio (Work)", webClone.cloneName)
    assertEquals("Work", webClone.cloneBadge)
    assertEquals(com.example.model.CloneType.WEB_APP_DUAL_SPACE, webClone.cloneType)
    org.junit.Assert.assertTrue(webClone.isolatedStorageDir.startsWith("clone_"))
    assertEquals(initialCount + 1, clonerManager.clonedApps.value.size)

    // Test launch tracking
    clonerManager.recordLaunch(webClone.id)
    val fetchedClone = clonerManager.clonedApps.value.first { it.id == webClone.id }
    assertEquals(1, fetchedClone.launchCount)

    // Test creating native app clone
    val sampleNativeApp = com.example.model.AppPackageItem(
      appName = "Calculator",
      packageName = "com.android.calculator2",
      versionName = "1.0",
      versionCode = 1L,
      isSystemApp = false,
      targetSdk = 34
    )

    val nativeClone = clonerManager.createNativeAppClone(
      app = sampleNativeApp,
      cloneName = "Calculator (Private)",
      cloneBadge = "Private",
      iconEmoji = "🔒",
      colorTintHex = 0xFF8B5CF6L
    )

    org.junit.Assert.assertNotNull(nativeClone)
    assertEquals(com.example.model.CloneType.NATIVE_APP, nativeClone.cloneType)
    assertEquals("Calculator (Private)", nativeClone.cloneName)
    org.junit.Assert.assertTrue(nativeClone.clonePackageSuffix.contains("com.android.calculator2"))

    // Test deletion
    clonerManager.deleteClone(webClone.id)
    org.junit.Assert.assertFalse(clonerManager.clonedApps.value.any { it.id == webClone.id })

    clonerManager.deleteClone(nativeClone.id)
    org.junit.Assert.assertFalse(clonerManager.clonedApps.value.any { it.id == nativeClone.id })
  }

  @Test
  fun `test queryInstalledPackagesFromSystem returns package names and version codes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val packages = com.example.ui.components.queryInstalledPackagesFromSystem(context)

    org.junit.Assert.assertNotNull(packages)
    org.junit.Assert.assertTrue(packages.isNotEmpty())

    // Verify self package exists and has non-empty name and valid version code
    val selfPkg = packages.firstOrNull { it.packageName == context.packageName }
    org.junit.Assert.assertNotNull(selfPkg)
    org.junit.Assert.assertTrue(selfPkg!!.appName.isNotBlank())
    org.junit.Assert.assertTrue(selfPkg.packageName.isNotBlank())
    org.junit.Assert.assertTrue(selfPkg.versionCode >= 0)
  }

  @Test
  fun `test ApkBackupManager offline extraction integrity verification and sharing`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val backupManager = com.example.manager.ApkBackupManager(context)

    // Ensure backup directory is created
    org.junit.Assert.assertTrue(backupManager.backupDirectory.exists())

    // Create a mock installed package APK in sourceDir if not present
    val pm = context.packageManager
    val pkgInfo = pm.getPackageInfo(context.packageName, 0)
    val dummyApk = java.io.File(context.cacheDir, "mock_installed_app.apk")
    dummyApk.writeBytes("PK\u0003\u0004DummyApkContent12345".toByteArray())
    pkgInfo.applicationInfo?.sourceDir = dummyApk.absolutePath

    val result = backupManager.backupInstalledPackage(context.packageName)
    org.junit.Assert.assertTrue(result.isSuccess)
    val backupItem = result.getOrThrow()

    assertEquals(context.packageName, backupItem.packageName)
    org.junit.Assert.assertTrue(backupItem.backupFile.exists())
    org.junit.Assert.assertTrue(backupItem.fileSizeBytes > 0)
    org.junit.Assert.assertTrue(backupItem.sha256Checksum.isNotBlank())

    // Verify SHA-256 integrity verification
    val isIntact = backupManager.verifyBackupIntegrity(backupItem)
    org.junit.Assert.assertTrue(isIntact)

    // Verify Share Intent generation
    val shareIntent = backupManager.createShareIntent(backupItem)
    assertEquals(android.content.Intent.ACTION_SEND, shareIntent.action)
    assertEquals("application/vnd.android.package-archive", shareIntent.type)
    org.junit.Assert.assertNotNull(shareIntent.getParcelableExtra<android.net.Uri>(android.content.Intent.EXTRA_STREAM))

    // Verify list persistence
    org.junit.Assert.assertTrue(backupManager.backups.value.any { it.id == backupItem.id })

    // Verify deletion
    val deleted = backupManager.deleteBackup(backupItem)
    org.junit.Assert.assertTrue(deleted)
    org.junit.Assert.assertFalse(backupItem.backupFile.exists())
    org.junit.Assert.assertFalse(backupManager.backups.value.any { it.id == backupItem.id })
  }

  @Test
  fun `test AuthManager getWebClientId safely returns valid client id without R string dependency`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    runCatching {
      if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
        com.google.firebase.FirebaseApp.initializeApp(context)
      }
    }
    val authManager = com.example.manager.AuthManager(context)
    val webClientId = authManager.getWebClientId()

    org.junit.Assert.assertNotNull(webClientId)
    org.junit.Assert.assertTrue(webClientId.isNotBlank())
    org.junit.Assert.assertTrue(webClientId.contains(".apps.googleusercontent.com"))
  }

  @Test
  fun `test AppPermissionManager inspects package permissions and creates settings intent`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val permManager = com.example.manager.AppPermissionManager(context)

    // Inspect own package permissions declared in AndroidManifest
    val permissions = permManager.getPermissionsForPackage(context.packageName)
    org.junit.Assert.assertNotNull(permissions)
    org.junit.Assert.assertTrue("Should have declared permissions", permissions.isNotEmpty())

    // Validate permission item properties
    val firstPerm = permissions.first()
    org.junit.Assert.assertTrue(firstPerm.fullName.isNotBlank())
    org.junit.Assert.assertTrue(firstPerm.simpleName.isNotBlank())
    org.junit.Assert.assertTrue(firstPerm.label.isNotBlank())
    org.junit.Assert.assertTrue(firstPerm.protectionLevel.isNotBlank())
    org.junit.Assert.assertTrue(firstPerm.riskRecommendation.isNotBlank())

    // Verify settings intent creation
    val settingsIntent = permManager.createManagePermissionsIntent(context.packageName)
    org.junit.Assert.assertNotNull(settingsIntent)
    org.junit.Assert.assertTrue(
      settingsIntent.action == "android.intent.action.MANAGE_APP_PERMISSIONS" ||
        settingsIntent.action == android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    )
  }
}
