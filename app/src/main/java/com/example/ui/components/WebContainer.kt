package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.RiskHigh
import com.example.ui.theme.RiskSafe

/**
 * Controller for [WebContainer] to handle navigation, domain whitelisting,
 * insecure content blocking, and on-demand cache/cookie management.
 */
@Stable
class WebContainerController(
  initialAllowedDomains: List<String> = emptyList(),
  initialBlockInsecureContent: Boolean = true,
  initialWhitelistEnforced: Boolean = false
) {
  internal var webView: WebView? = null

  var currentUrl by mutableStateOf("")
    internal set
  var pageTitle by mutableStateOf("")
    internal set
  var isLoading by mutableStateOf(false)
    internal set
  var progress by mutableFloatStateOf(0f)
    internal set
  var canGoBack by mutableStateOf(false)
    internal set
  var canGoForward by mutableStateOf(false)
    internal set
  var isSecureConnection by mutableStateOf(true)
    internal set
  var errorMessage by mutableStateOf<String?>(null)
    internal set

  // Whitelist & Security policy states
  var blockInsecureContent by mutableStateOf(initialBlockInsecureContent)
  var isWhitelistEnforced by mutableStateOf(initialWhitelistEnforced)
  val allowedDomains = mutableStateListOf<String>().apply {
    addAll(initialAllowedDomains)
  }

  var isBlockedByWhitelist by mutableStateOf(false)
    internal set
  var blockedAttemptUrl by mutableStateOf<String?>(null)
    internal set

  fun goBack() {
    isBlockedByWhitelist = false
    webView?.goBack()
  }

  fun goForward() {
    isBlockedByWhitelist = false
    webView?.goForward()
  }

  fun reload() {
    errorMessage = null
    isBlockedByWhitelist = false
    webView?.reload()
  }

  fun loadUrl(url: String) {
    errorMessage = null
    isBlockedByWhitelist = false
    val target = sanitizeUrl(url)

    if (blockInsecureContent && isInsecureScheme(target)) {
      errorMessage = "Insecure connection blocked: HTTP is restricted for improved security."
      return
    }

    if (isWhitelistEnforced && !isDomainAllowed(target)) {
      isBlockedByWhitelist = true
      blockedAttemptUrl = target
      errorMessage = "Navigation blocked: Domain is not in the allowed whitelist."
      return
    }

    currentUrl = target
    webView?.loadUrl(target)
  }

  /**
   * Evaluates if a given URL or hostname is permitted under the current domain whitelist.
   */
  fun isDomainAllowed(targetUrlOrHost: String): Boolean {
    if (!isWhitelistEnforced || allowedDomains.isEmpty()) {
      return true
    }

    val host = extractHost(targetUrlOrHost).lowercase()
    if (host.isBlank()) return false

    return allowedDomains.any { pattern ->
      matchesDomainPattern(host, pattern.trim().lowercase())
    }
  }

  fun addAllowedDomain(domain: String) {
    val clean = domain.trim().removePrefix("https://").removePrefix("http://").removeSuffix("/")
    if (clean.isNotBlank() && !allowedDomains.contains(clean)) {
      allowedDomains.add(clean)
    }
  }

  fun removeAllowedDomain(domain: String) {
    allowedDomains.remove(domain)
  }

  fun dismissBlockedAlert() {
    isBlockedByWhitelist = false
    blockedAttemptUrl = null
  }

  /**
   * Clears WebView cache on demand (both memory and disk cache) plus WebStorage (localStorage/sessionStorage).
   */
  fun clearCache(includeDiskFiles: Boolean = true, onComplete: (() -> Unit)? = null) {
    webView?.clearCache(includeDiskFiles)
    runCatching {
      WebStorage.getInstance().deleteAllData()
    }
    onComplete?.invoke()
  }

  /**
   * Clears cookies securely on demand and flushes changes to storage.
   */
  fun clearCookies(onComplete: (() -> Unit)? = null) {
    val cookieManager = CookieManager.getInstance()
    cookieManager.removeAllCookies {
      cookieManager.flush()
      onComplete?.invoke()
    }
  }

  /**
   * Comprehensive privacy wipe: clears cache, cookies, web storage, and navigation history.
   */
  fun clearAllBrowsingData(onComplete: (() -> Unit)? = null) {
    clearCache(includeDiskFiles = true)
    clearCookies {
      webView?.apply {
        clearFormData()
        clearHistory()
        clearSslPreferences()
      }
      onComplete?.invoke()
    }
  }

  internal fun sanitizeUrl(input: String): String {
    val trimmed = input.trim()
    return when {
      trimmed.startsWith("https://", ignoreCase = true) -> trimmed
      trimmed.startsWith("http://", ignoreCase = true) -> trimmed
      trimmed.startsWith("javascript:", ignoreCase = true) -> "about:blank"
      trimmed.startsWith("file:", ignoreCase = true) -> "about:blank"
      trimmed.startsWith("content:", ignoreCase = true) -> "about:blank"
      trimmed.startsWith("data:", ignoreCase = true) -> "about:blank"
      else -> "https://$trimmed"
    }
  }

  internal fun isInsecureScheme(url: String): Boolean {
    val trimmed = url.trim().lowercase()
    val host = extractHost(trimmed).lowercase()
    if (host == "localhost" || host == "127.0.0.1") return false
    return trimmed.startsWith("http://")
  }

  internal fun extractHost(urlOrHost: String): String {
    return try {
      val parsed = if (urlOrHost.contains("://")) Uri.parse(urlOrHost) else Uri.parse("https://$urlOrHost")
      parsed.host ?: urlOrHost
    } catch (e: Exception) {
      urlOrHost
    }
  }

  private fun matchesDomainPattern(host: String, pattern: String): Boolean {
    val cleanPattern = pattern.removePrefix("https://").removePrefix("http://").removeSuffix("/")
    return when {
      cleanPattern == "*" -> true
      cleanPattern.startsWith("*.") -> {
        val root = cleanPattern.removePrefix("*.")
        host == root || host.endsWith(".$root")
      }
      cleanPattern.startsWith(".") -> {
        val root = cleanPattern.removePrefix(".")
        host == root || host.endsWith(".$root")
      }
      host == cleanPattern -> true
      host.endsWith(".$cleanPattern") -> true
      else -> false
    }
  }
}

@Composable
fun rememberWebContainerController(
  initialAllowedDomains: List<String> = emptyList(),
  initialBlockInsecureContent: Boolean = true,
  initialWhitelistEnforced: Boolean = false
): WebContainerController {
  return remember {
    WebContainerController(
      initialAllowedDomains = initialAllowedDomains,
      initialBlockInsecureContent = initialBlockInsecureContent,
      initialWhitelistEnforced = initialWhitelistEnforced
    )
  }
}

/**
 * Reusable [WebContainer] Composable.
 *
 * Provides a production-grade, hardened Android WebView container:
 * - Insecure content blocking: Rejects unencrypted HTTP schemes and enforces MIXED_CONTENT_NEVER_ALLOW
 * - Domain whitelisting: Restricts navigation strictly to specified allowed domains, blocking unauthorized off-site redirects
 * - On-demand cache & cookie management: Dedicated controls to purge cookies, disk/RAM cache, and web storage
 * - Strict zero-trust SSL validation: Rejects invalid or expired SSL certificates to safeguard against MITM attacks
 * - Resilient renderer process lifecycle handling
 * - BackHandler integration for seamless Android back gesture navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebContainer(
  url: String,
  modifier: Modifier = Modifier,
  controller: WebContainerController = rememberWebContainerController(),
  enableJavaScript: Boolean = true,
  acceptCookies: Boolean = true,
  acceptThirdPartyCookies: Boolean = false,
  showAddressBar: Boolean = true,
  allowedDomains: List<String> = emptyList(),
  blockInsecureContent: Boolean = true,
  enforceDomainWhitelist: Boolean = false,
  onTitleChanged: ((String) -> Unit)? = null,
  onUrlChanged: ((String) -> Unit)? = null,
  onError: ((String) -> Unit)? = null
) {
  val context = LocalContext.current
  var showSecuritySheet by remember { mutableStateOf(false) }

  // Sync initial configuration to controller if needed
  remember(allowedDomains, blockInsecureContent, enforceDomainWhitelist) {
    if (allowedDomains.isNotEmpty() && controller.allowedDomains.isEmpty()) {
      controller.allowedDomains.addAll(allowedDomains)
    }
    controller.blockInsecureContent = blockInsecureContent
    if (enforceDomainWhitelist) {
      controller.isWhitelistEnforced = true
    }
    // Also auto-whitelist the initial host if whitelist is enforced
    val initialHost = controller.extractHost(url)
    if (initialHost.isNotBlank() && !controller.allowedDomains.contains(initialHost)) {
      controller.addAllowedDomain(initialHost)
    }
  }

  BackHandler(enabled = controller.canGoBack) {
    controller.goBack()
  }

  Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    if (showAddressBar) {
      Surface(
        shape = RoundedCornerShape(0.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.weight(1f)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = if (controller.isSecureConnection) Icons.Default.Lock else Icons.Default.LockOpen,
                  contentDescription = if (controller.isSecureConnection) "Secure HTTPS" else "Insecure Connection",
                  tint = if (controller.isSecureConnection) RiskSafe else RiskHigh,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = controller.pageTitle.ifBlank { "Web Application" },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = controller.currentUrl.ifBlank { url },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }

                if (controller.isWhitelistEnforced) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmeraldTertiary.copy(alpha = 0.15f),
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

            Spacer(modifier = Modifier.width(4.dp))

            // Security & Privacy Settings Sheet Trigger
            IconButton(
              onClick = { showSecuritySheet = true },
              modifier = Modifier.size(34.dp).testTag("webcontainer_security_settings")
            ) {
              Icon(
                Icons.Default.Security,
                contentDescription = "Security Settings & Cache",
                tint = if (controller.blockInsecureContent || controller.isWhitelistEnforced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
            }

            IconButton(
              onClick = { controller.reload() },
              modifier = Modifier.size(34.dp).testTag("webcontainer_reload")
            ) {
              Icon(Icons.Default.Refresh, contentDescription = "Reload Page", modifier = Modifier.size(18.dp))
            }
          }

          AnimatedVisibility(
            visible = controller.isLoading && controller.progress < 1f,
            enter = fadeIn(),
            exit = fadeOut()
          ) {
            LinearProgressIndicator(
              progress = { controller.progress },
              modifier = Modifier.fillMaxWidth().height(2.dp),
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    }

    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      AndroidView(
        factory = { ctx ->
          WebView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Use native window-backed rendering (avoids forced offscreen layer OOM in emulator)
            runCatching {
              setLayerType(View.LAYER_TYPE_NONE, null)
            }

            // Secure Cookie Policy
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(acceptCookies)
            cookieManager.setAcceptThirdPartyCookies(this, acceptThirdPartyCookies)

            // Hardened WebSettings
            settings.apply {
              this.javaScriptEnabled = enableJavaScript
              this.domStorageEnabled = true
              this.databaseEnabled = true
              this.mediaPlaybackRequiresUserGesture = false

              // Security: Disable file scheme exploits
              this.allowFileAccess = false
              this.allowContentAccess = false

              // Security: Never allow mixed HTTP content over HTTPS
              this.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

              this.useWideViewPort = true
              this.loadWithOverviewMode = true
              this.setSupportZoom(true)
              this.builtInZoomControls = true
              this.displayZoomControls = false

              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.safeBrowsingEnabled = true
              }
            }

            webChromeClient = object : WebChromeClient() {
              override fun onProgressChanged(view: WebView?, newProgress: Int) {
                controller.progress = newProgress / 100f
                controller.isLoading = newProgress < 100
              }

              override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) {
                  controller.pageTitle = title
                  onTitleChanged?.invoke(title)
                }
              }
            }

            webViewClient = object : WebViewClient() {
              override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                val cleanUrl = pageUrl ?: ""
                controller.currentUrl = cleanUrl
                controller.isSecureConnection = cleanUrl.startsWith("https://", ignoreCase = true)
                controller.canGoBack = canGoBack()
                controller.canGoForward = canGoForward()
                controller.errorMessage = null
                controller.isBlockedByWhitelist = false
                onUrlChanged?.invoke(cleanUrl)
              }

              override fun onPageFinished(view: WebView?, pageUrl: String?) {
                val cleanUrl = pageUrl ?: ""
                controller.currentUrl = cleanUrl
                controller.isSecureConnection = cleanUrl.startsWith("https://", ignoreCase = true)
                controller.canGoBack = canGoBack()
                controller.canGoForward = canGoForward()
                CookieManager.getInstance().flush()
              }

              override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                controller.isSecureConnection = false
                val errorMsg = "SSL Certificate Warning: Insecure or invalid certificate detected. Blocked for your security."
                controller.errorMessage = errorMsg
                onError?.invoke(errorMsg)
                // Strict zero-trust cancel
                handler?.cancel()
              }

              override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true) {
                  val desc = error?.description?.toString() ?: "Failed to connect to web app"
                  controller.errorMessage = desc
                  onError?.invoke(desc)
                }
              }

              override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                val didCrash = detail?.didCrash() ?: false
                val reason = if (didCrash) "Web view renderer reset. Tap Retry to reload." else "System reclaimed web view memory."
                controller.errorMessage = reason
                onError?.invoke(reason)
                view?.let { v ->
                  runCatching {
                    (v.parent as? ViewGroup)?.removeView(v)
                    v.stopLoading()
                    v.destroy()
                  }
                }
                controller.webView = null
                return true
              }

              override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val reqUrl = request?.url?.toString() ?: return false
                val scheme = request.url?.scheme?.lowercase() ?: ""

                // 1. Insecure scheme blocking (HTTP enforcement)
                if (controller.blockInsecureContent && scheme == "http") {
                  val host = request.url?.host?.lowercase() ?: ""
                  if (host != "localhost" && host != "127.0.0.1") {
                    controller.errorMessage = "Insecure HTTP connection blocked ($reqUrl). Only HTTPS is allowed."
                    return true
                  }
                }

                // 2. Allowed domain whitelist enforcement
                if (controller.isWhitelistEnforced && !controller.isDomainAllowed(reqUrl)) {
                  controller.isBlockedByWhitelist = true
                  controller.blockedAttemptUrl = reqUrl
                  return true
                }

                // 3. Delegation of safe external intent schemes
                return when (scheme) {
                  "tel", "mailto", "sms" -> {
                    runCatching {
                      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(reqUrl))
                      ctx.startActivity(intent)
                    }
                    true
                  }
                  "http", "https" -> false // Proceed within secure WebView
                  else -> true // Block unauthorized custom protocols
                }
              }
            }

            controller.webView = this
            val sanitized = controller.sanitizeUrl(url)
            controller.currentUrl = sanitized

            if (controller.blockInsecureContent && controller.isInsecureScheme(sanitized)) {
              controller.errorMessage = "Insecure connection blocked: HTTP is restricted."
            } else if (controller.isWhitelistEnforced && !controller.isDomainAllowed(sanitized)) {
              controller.isBlockedByWhitelist = true
              controller.blockedAttemptUrl = sanitized
            } else {
              loadUrl(sanitized)
            }
          }
        },
        modifier = Modifier.fillMaxSize().testTag("webcontainer_view")
      )

      // Whitelist Navigation Blocked Alert Overlay
      if (controller.isBlockedByWhitelist) {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .align(Alignment.Center)
            .testTag("webcontainer_whitelist_blocked_card")
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(RiskHigh.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Shield, contentDescription = null, tint = RiskHigh, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Navigation Blocked",
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "This web app attempted to navigate to an external domain that is not permitted by your security whitelist.",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = controller.blockedAttemptUrl ?: "Unknown URL",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp),
                textAlign = TextAlign.Center
              )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = {
                  controller.dismissBlockedAlert()
                  if (controller.canGoBack) controller.goBack()
                },
                modifier = Modifier.weight(1f).testTag("whitelist_block_return")
              ) {
                Text("Go Back", fontSize = 12.sp)
              }

              Button(
                onClick = {
                  val domainToAllow = controller.extractHost(controller.blockedAttemptUrl ?: "")
                  if (domainToAllow.isNotBlank()) {
                    controller.addAllowedDomain(domainToAllow)
                    val urlToLoad = controller.blockedAttemptUrl ?: ""
                    controller.dismissBlockedAlert()
                    if (urlToLoad.isNotBlank()) {
                      controller.loadUrl(urlToLoad)
                    }
                    Toast.makeText(context, "Added $domainToAllow to whitelist", Toast.LENGTH_SHORT).show()
                  }
                },
                modifier = Modifier.weight(1f).testTag("whitelist_block_allow"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
              ) {
                Text("Allow Domain", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // General Error Fallback Card
      controller.errorMessage?.let { err ->
        if (!controller.isBlockedByWhitelist) {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp)
              .align(Alignment.Center)
              .testTag("webcontainer_error_state")
          ) {
            Column(
              modifier = Modifier.padding(18.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(CircleShape)
                  .background(RiskHigh.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = RiskHigh, modifier = Modifier.size(22.dp))
              }
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = "Web Security Notice",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = err,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(14.dp))
              Button(
                onClick = { controller.reload() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
              ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }

  // Security & Privacy Settings Modal Bottom Sheet
  if (showSecuritySheet) {
    WebContainerSecuritySheet(
      controller = controller,
      onDismiss = { showSecuritySheet = false }
    )
  }

  DisposableEffect(Unit) {
    onDispose {
      controller.webView?.apply {
        stopLoading()
        clearHistory()
        removeAllViews()
        destroy()
      }
      controller.webView = null
    }
  }
}

/**
 * Material 3 Security & Privacy Configuration Sheet.
 * Provides controls for blocking insecure content, whitelist management, and on-demand cache/cookie deletion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebContainerSecuritySheet(
  controller: WebContainerController,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var newDomainInput by remember { mutableStateOf("") }
  var statusFeedback by remember { mutableStateOf<String?>(null) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("webcontainer_security_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
        .padding(bottom = 24.dp)
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
              text = "Web Container Security",
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Content protection & on-demand cache controls",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close Sheet")
        }
      }

      // Feedback banner
      statusFeedback?.let { msg ->
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

      // Section 1: Insecure Content Protection
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Block Insecure Content",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Forces HTTPS and strictly blocks unencrypted HTTP mixed content",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Switch(
          checked = controller.blockInsecureContent,
          onCheckedChange = {
            controller.blockInsecureContent = it
            statusFeedback = if (it) "Insecure HTTP blocking enabled" else "Insecure HTTP blocking disabled"
          },
          modifier = Modifier.testTag("toggle_block_insecure_content")
        )
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Spacer(modifier = Modifier.height(12.dp))

      // Section 2: Domain Whitelist Restriction
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Restrict Navigation to Whitelist",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Blocks redirects and link navigation outside authorized domains",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Switch(
          checked = controller.isWhitelistEnforced,
          onCheckedChange = {
            controller.isWhitelistEnforced = it
            statusFeedback = if (it) "Domain whitelist enforced" else "Domain whitelist disabled"
          },
          modifier = Modifier.testTag("toggle_enforce_whitelist")
        )
      }

      // Domain Whitelist Chips & Input
      if (controller.isWhitelistEnforced) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Allowed Domains (${controller.allowedDomains.size}):",
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (controller.allowedDomains.isEmpty()) {
          Text(
            text = "No domains in whitelist yet. Add a domain below.",
            fontSize = 11.sp,
            color = RiskHigh
          )
        } else {
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(controller.allowedDomains) { domain ->
              InputChip(
                selected = true,
                onClick = {},
                label = { Text(domain, fontSize = 11.sp) },
                trailingIcon = {
                  IconButton(
                    onClick = { controller.removeAllowedDomain(domain) },
                    modifier = Modifier.size(16.dp)
                  ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(12.dp))
                  }
                },
                colors = InputChipDefaults.inputChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                )
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = newDomainInput,
            onValueChange = { newDomainInput = it },
            placeholder = { Text("e.g. *.run.app, google.com", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("whitelist_domain_input")
          )
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (newDomainInput.isNotBlank()) {
                controller.addAllowedDomain(newDomainInput)
                newDomainInput = ""
                statusFeedback = "Domain added to whitelist"
              }
            },
            enabled = newDomainInput.isNotBlank(),
            modifier = Modifier.testTag("whitelist_domain_add_button")
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

      // Section 3: On-Demand Cache & Cookie Purge Actions
      Text(
        text = "On-Demand Data & Storage Controls",
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = "Instantly clear cached assets, cookies, and local web storage",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = {
            controller.clearCache(includeDiskFiles = true) {
              statusFeedback = "WebView disk & RAM cache cleared successfully"
              Toast.makeText(context, "Cache Cleared", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier.weight(1f).testTag("clear_cache_button")
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Clear Cache", fontSize = 11.sp)
        }

        OutlinedButton(
          onClick = {
            controller.clearCookies {
              statusFeedback = "All WebView cookies purged securely"
              Toast.makeText(context, "Cookies Cleared", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier.weight(1f).testTag("clear_cookies_button")
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
            statusFeedback = "All cache, cookies, and browsing data wiped"
            Toast.makeText(context, "All Browsing Data Wiped", Toast.LENGTH_SHORT).show()
          }
        },
        modifier = Modifier.fillMaxWidth().testTag("clear_all_browsing_data_button"),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
      ) {
        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Wipe All Web Data & Reset Session", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}
