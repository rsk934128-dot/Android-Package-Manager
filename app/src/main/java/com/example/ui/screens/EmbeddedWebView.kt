package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SavedWebApp
import com.example.model.WebNotificationType
import com.example.ui.components.SecureAppWebView
import com.example.ui.components.rememberSecureWebViewController
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.RiskHigh
import com.example.ui.theme.RiskSafe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmbeddedWebView(
  webApp: SavedWebApp,
  isBackgroundActive: Boolean,
  onClose: () -> Unit,
  onToggleBackground: (Boolean) -> Unit,
  onNotificationReceived: (appName: String, title: String, body: String, type: WebNotificationType) -> Unit,
  onDownloadRequested: ((url: String, fileName: String?) -> Unit)? = null
) {
  val context = LocalContext.current
  var showSecuritySheet by remember { mutableStateOf(false) }

  // Extract initial domain
  val initialHost = remember(webApp.url) {
    try {
      Uri.parse(if (webApp.url.contains("://")) webApp.url else "https://${webApp.url}").host ?: ""
    } catch (e: Exception) {
      ""
    }
  }

  val initialDomains = remember(initialHost) {
    if (initialHost.isNotBlank()) listOf(initialHost, "*.run.app") else listOf("*.run.app")
  }

  val controller = rememberSecureWebViewController(
    initialAllowedDomains = initialDomains,
    initialBlockInsecureContent = true,
    initialWhitelistEnforced = false
  )

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    // Top Bar for In-App Browser Experience
    Card(
      shape = RoundedCornerShape(0.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onClose,
          modifier = Modifier.size(36.dp).testTag("webview_close_button")
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close Web App", tint = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Security Status & Title Pill
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.weight(1f)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (controller.isSecureConnection) Icons.Default.Lock else Icons.Default.LockOpen,
              contentDescription = if (controller.isSecureConnection) "SSL Encrypted" else "Insecure",
              tint = if (controller.isSecureConnection) RiskSafe else RiskHigh,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = controller.pageTitle.ifBlank { webApp.name },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = controller.currentUrl.ifBlank { webApp.url },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            if (controller.isWhitelistEnforced) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = EmeraldTertiary.copy(alpha = 0.2f),
                modifier = Modifier.padding(start = 4.dp)
              ) {
                Text(
                  text = "WHITELIST",
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold,
                  color = EmeraldTertiary,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Security & Cache Controls Quick Button
        IconButton(
          onClick = { showSecuritySheet = true },
          modifier = Modifier.size(36.dp).testTag("webview_security_controls")
        ) {
          Icon(
            Icons.Default.Security,
            contentDescription = "Security & Cache Settings",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(19.dp)
          )
        }

        // Background Keep-Alive Indicator
        Box(
          modifier = Modifier
            .clip(CircleShape)
            .background(if (isBackgroundActive) EmeraldTertiary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isBackgroundActive) EmeraldTertiary else MaterialTheme.colorScheme.outline)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (isBackgroundActive) "BG ACTIVE" else "BG OFF",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = if (isBackgroundActive) EmeraldTertiary else MaterialTheme.colorScheme.outline
            )
          }
        }
      }
    }

    // Core Reusable SecureAppWebView Component
    Box(modifier = Modifier.weight(1f)) {
      SecureAppWebView(
        url = webApp.url,
        appName = webApp.name,
        controller = controller,
        enableJavaScript = true,
        enableDesktopMode = webApp.desktopMode,
        showToolbar = false,
        onNotificationReceived = onNotificationReceived,
        onDownloadRequested = onDownloadRequested,
        modifier = Modifier.fillMaxSize()
      )
    }

    // Bottom Navigation Bar for Web Controls
    Card(
      shape = RoundedCornerShape(0.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = { controller.goBack() },
            enabled = controller.canGoBack,
            modifier = Modifier.testTag("webview_back")
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }

          IconButton(
            onClick = { controller.goForward() },
            enabled = controller.canGoForward,
            modifier = Modifier.testTag("webview_forward")
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
          }

          IconButton(
            onClick = { controller.reload() },
            modifier = Modifier.testTag("webview_refresh")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh Page")
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Security, Whitelist & Cache Settings
          IconButton(
            onClick = { showSecuritySheet = true },
            modifier = Modifier.testTag("webview_security_bottom_btn")
          ) {
            Icon(
              Icons.Default.Shield,
              contentDescription = "Privacy & Cache Management",
              tint = MaterialTheme.colorScheme.primary
            )
          }

          // Toggle Desktop / Mobile mode
          IconButton(
            onClick = {
              val newMode = !controller.isDesktopMode
              controller.setDesktopModeEnabled(newMode)
              Toast.makeText(context, if (newMode) "Desktop mode enabled" else "Mobile mode enabled", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.testTag("webview_desktop_toggle")
          ) {
            Icon(
              if (controller.isDesktopMode) Icons.Default.Computer else Icons.Default.Smartphone,
              contentDescription = "Toggle Desktop Mode",
              tint = if (controller.isDesktopMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Test Simulated Alert / Call for the web app
          IconButton(
            onClick = {
              onNotificationReceived(
                webApp.name,
                "Call/Message Alert",
                "Background notification test for ${webApp.name}",
                WebNotificationType.INCOMING_CALL
              )
              Toast.makeText(context, "Triggered test alert for ${webApp.name}", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.testTag("webview_test_alert")
          ) {
            Icon(
              Icons.Default.NotificationsActive,
              contentDescription = "Test Notification Alert",
              tint = MaterialTheme.colorScheme.primary
            )
          }

          // Open in External Browser
          IconButton(
            onClick = {
              runCatching {
                val targetUrl = controller.currentUrl.ifBlank { webApp.url }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                context.startActivity(intent)
              }
            },
            modifier = Modifier.testTag("webview_open_external")
          ) {
            Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in Chrome/Browser")
          }
        }
      }
    }
  }

  // Security & Cache Bottom Sheet
  if (showSecuritySheet) {
    EmbeddedSecuritySheet(
      controller = controller,
      onDismiss = { showSecuritySheet = false }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmbeddedSecuritySheet(
  controller: com.example.ui.components.SecureWebViewController,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var newDomainInput by remember { mutableStateOf("") }
  var feedbackMessage by remember { mutableStateOf<String?>(null) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("embedded_security_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
        .padding(bottom = 28.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Security & Privacy Settings",
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Insecure content blocking, domain whitelist & cache",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      feedbackMessage?.let { msg ->
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = EmeraldTertiary.copy(alpha = 0.15f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldTertiary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = msg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = EmeraldTertiary)
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Spacer(modifier = Modifier.height(12.dp))

      // Insecure Content Toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Block Insecure Content", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          Text("Enforce HTTPS and block mixed HTTP elements", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
          checked = controller.blockInsecureContent,
          onCheckedChange = {
            controller.blockInsecureContent = it
            feedbackMessage = if (it) "Insecure HTTP blocking enabled" else "Insecure HTTP blocking disabled"
          },
          modifier = Modifier.testTag("embedded_toggle_insecure")
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Spacer(modifier = Modifier.height(12.dp))

      // Whitelist Toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Restrict Navigation to Whitelist", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          Text("Prevent redirection to unapproved external domains", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
          checked = controller.isWhitelistEnforced,
          onCheckedChange = {
            controller.isWhitelistEnforced = it
            feedbackMessage = if (it) "Domain whitelist enforced" else "Domain whitelist disabled"
          },
          modifier = Modifier.testTag("embedded_toggle_whitelist")
        )
      }

      if (controller.isWhitelistEnforced) {
        Spacer(modifier = Modifier.height(8.dp))
        Text("Whitelisted Domains (${controller.allowedDomains.size}):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
          items(controller.allowedDomains) { domain ->
            InputChip(
              selected = true,
              onClick = {},
              label = { Text(domain, fontSize = 11.sp) },
              trailingIcon = {
                IconButton(onClick = { controller.removeAllowedDomain(domain) }, modifier = Modifier.size(16.dp)) {
                  Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(12.dp))
                }
              },
              colors = InputChipDefaults.inputChipColors(selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(
            value = newDomainInput,
            onValueChange = { newDomainInput = it },
            placeholder = { Text("e.g. *.run.app, github.com", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("embedded_domain_input")
          )
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (newDomainInput.isNotBlank()) {
                controller.addAllowedDomain(newDomainInput)
                newDomainInput = ""
                feedbackMessage = "Domain added to whitelist"
              }
            },
            enabled = newDomainInput.isNotBlank(),
            modifier = Modifier.testTag("embedded_domain_add_button")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add", fontSize = 12.sp)
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Spacer(modifier = Modifier.height(12.dp))

      // On-demand Cache & Cookie Clearing
      Text("Clear Storage & Cache On Demand", fontWeight = FontWeight.Bold, fontSize = 13.sp)
      Text("Purge cached files and session cookies immediately", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = {
            controller.clearCache(includeDiskFiles = true) {
              feedbackMessage = "Cache cleared successfully"
              Toast.makeText(context, "Cache Cleared", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier.weight(1f).testTag("embedded_clear_cache")
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Clear Cache", fontSize = 11.sp)
        }

        OutlinedButton(
          onClick = {
            controller.clearCookies {
              feedbackMessage = "Cookies cleared successfully"
              Toast.makeText(context, "Cookies Cleared", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier.weight(1f).testTag("embedded_clear_cookies")
        ) {
          Icon(Icons.Default.Cookie, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Clear Cookies", fontSize = 11.sp)
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Button(
        onClick = {
          controller.clearAllBrowsingData {
            feedbackMessage = "All web data and cookies wiped"
            Toast.makeText(context, "All Browsing Data Reset", Toast.LENGTH_SHORT).show()
          }
        },
        modifier = Modifier.fillMaxWidth().testTag("embedded_wipe_all_data"),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
      ) {
        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Wipe All Data & Reset Session", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}
