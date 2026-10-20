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
}
