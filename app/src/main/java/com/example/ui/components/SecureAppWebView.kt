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
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.model.WebNotificationType
import com.example.ui.theme.RiskHigh
import com.example.ui.theme.RiskSafe

/**
 * Controller class to manipulate the [SecureAppWebView] from external composables.
 */
@Stable
class SecureWebViewController {
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
  var lastError by mutableStateOf<String?>(null)
    internal set
  var isDesktopMode by mutableStateOf(false)
    internal set
  var blockInsecureContent by mutableStateOf(true)
  var isWhitelistEnforced by mutableStateOf(false)
  val allowedDomains = mutableStateListOf<String>()

  fun goBack() {
    webView?.goBack()
  }

  fun goForward() {
    webView?.goForward()
  }

  fun reload() {
    lastError = null
    webView?.reload()
  }

  fun loadUrl(url: String) {
    lastError = null
    val secureUrl = sanitizeUrl(url)
    if (blockInsecureContent && secureUrl.startsWith("http://", ignoreCase = true)) {
      val host = Uri.parse(secureUrl).host?.lowercase() ?: ""
      if (host != "localhost" && host != "127.0.0.1") {
        lastError = "Insecure connection blocked: HTTP is restricted for improved security."
        return
      }
    }
    if (isWhitelistEnforced && !isDomainAllowed(secureUrl)) {
      lastError = "Navigation blocked: Domain not in allowed whitelist."
      return
    }
    currentUrl = secureUrl
    webView?.loadUrl(secureUrl)
  }

  fun isDomainAllowed(targetUrl: String): Boolean {
    if (!isWhitelistEnforced || allowedDomains.isEmpty()) return true
    val host = try {
      val uri = Uri.parse(if (targetUrl.contains("://")) targetUrl else "https://$targetUrl")
      uri.host?.lowercase() ?: targetUrl.lowercase()
    } catch (e: Exception) {
      targetUrl.lowercase()
    }
    return allowedDomains.any { pattern ->
      val p = pattern.trim().lowercase().removePrefix("https://").removePrefix("http://").removeSuffix("/")
      when {
        p == "*" -> true
        p.startsWith("*.") -> host == p.removePrefix("*.") || host.endsWith(".${p.removePrefix("*.")}")
        host == p -> true
        host.endsWith(".$p") -> true
        else -> false
      }
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

  fun clearCache(includeDiskFiles: Boolean = true, onComplete: (() -> Unit)? = null) {
    webView?.clearCache(includeDiskFiles)
    runCatching { WebStorage.getInstance().deleteAllData() }
    onComplete?.invoke()
  }

  fun clearCookies(onComplete: (() -> Unit)? = null) {
    val cookieManager = CookieManager.getInstance()
    cookieManager.removeAllCookies {
      cookieManager.flush()
      onComplete?.invoke()
    }
  }

  fun clearAllBrowsingData(onComplete: (() -> Unit)? = null) {
    clearCache(true)
    clearCookies {
      webView?.apply {
        clearFormData()
        clearHistory()
        clearSslPreferences()
      }
      onComplete?.invoke()
    }
  }

  fun setDesktopModeEnabled(enable: Boolean) {
    isDesktopMode = enable
    webView?.settings?.apply {
      userAgentString = if (enable) {
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
      } else {
        null
      }
    }
    reload()
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
}

@Composable
fun rememberSecureWebViewController(
  initialAllowedDomains: List<String> = emptyList(),
  initialBlockInsecureContent: Boolean = true,
  initialWhitelistEnforced: Boolean = false
): SecureWebViewController {
  return remember {
    SecureWebViewController().apply {
      allowedDomains.addAll(initialAllowedDomains)
      blockInsecureContent = initialBlockInsecureContent
      isWhitelistEnforced = initialWhitelistEnforced
    }
  }
}

/**
 * Safe JavaScript bridge for web apps.
 */
class SecureJsBridge(
  private val appIdentifier: String,
  private val onWebAlert: (appName: String, title: String, body: String, type: WebNotificationType) -> Unit
) {
  @JavascriptInterface
  fun postNotification(title: String?, body: String?, typeString: String?) {
    val cleanTitle = (title ?: "Web Notification").take(150)
    val cleanBody = (body ?: "").take(500)
    val type = runCatching {
      WebNotificationType.valueOf(typeString ?: WebNotificationType.GENERAL.name)
    }.getOrDefault(WebNotificationType.GENERAL)

    onWebAlert(appIdentifier, cleanTitle, cleanBody, type)
  }
}

/**
 * Production-ready, security-hardened reusable WebView component for Android.
 *
 * Implements:
 * - Disabled local file access vectors (file://, content://)
 * - Safe Browsing enforcement
 * - MIXED_CONTENT_NEVER_ALLOW (prevents SSL downgrade attacks)
 * - Sanitized navigation & external intent delegation
 * - WebRTC camera/audio permission management
 * - Injected notification & title change monitoring bridge
 * - Error state detection with Retry affordance
 * - Smooth Material 3 loading indicators and SSL padlock status
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SecureAppWebView(
  url: String,
  modifier: Modifier = Modifier,
  appName: String = "Web App",
  controller: SecureWebViewController = rememberSecureWebViewController(),
  enableJavaScript: Boolean = true,
  enableDesktopMode: Boolean = false,
  showToolbar: Boolean = true,
  onNotificationReceived: ((appName: String, title: String, body: String, type: WebNotificationType) -> Unit)? = null,
  onDownloadRequested: ((url: String, fileName: String?) -> Unit)? = null,
  onClose: (() -> Unit)? = null
) {
  val context = LocalContext.current

  // Intercept back key if webview can navigate backwards
  BackHandler(enabled = controller.canGoBack) {
    controller.goBack()
  }

  val notificationHookScript = """
    (function() {
      if (!window.SecureAndroidNotificationHooked) {
        window.SecureAndroidNotificationHooked = true;
        const OriginalNotification = window.Notification;
        window.Notification = function(title, options) {
          options = options || {};
          const body = options.body || "";
          const isCall = /call|ringing|incoming|voice|video|meeting|webrtc/i.test(title + " " + body);
          const type = isCall ? "INCOMING_CALL" : "INCOMING_MESSAGE";
          if (window.AndroidBridge && window.AndroidBridge.postNotification) {
            window.AndroidBridge.postNotification(title, body, type);
          }
          if (OriginalNotification) {
            try { return new OriginalNotification(title, options); } catch(e){}
          }
        };
        window.Notification.permission = "granted";
        window.Notification.requestPermission = function(cb) {
          if (cb) cb("granted");
          return Promise.resolve("granted");
        };
      }

      // Title observer for background message badges like "(2) New Call"
      let prevTitle = document.title;
      const observer = new MutationObserver(function() {
        if (document.title !== prevTitle) {
          prevTitle = document.title;
          if (/\([0-9]+\)/.test(prevTitle) || /call|ringing|alert|message/i.test(prevTitle)) {
            if (window.AndroidBridge && window.AndroidBridge.postNotification) {
              const isCall = /call|ringing|incoming/i.test(prevTitle);
              window.AndroidBridge.postNotification(
                isCall ? "Incoming Web Call" : "Web Notification",
                prevTitle,
                isCall ? "INCOMING_CALL" : "INCOMING_MESSAGE"
              );
            }
          }
        }
      });
      const titleElem = document.querySelector('title');
      if (titleElem) {
        observer.observe(titleElem, { subtree: true, characterData: true, childList: true });
      }
    })();
  """.trimIndent()

  Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    // Optional Top Bar with Security Status & Controls
    if (showToolbar) {
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
              .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (onClose != null) {
              IconButton(onClick = onClose, modifier = Modifier.size(36.dp).testTag("secure_webview_close")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close View")
              }
            }

            // SSL Padlock & Host Bar
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = if (controller.isSecureConnection) Icons.Default.Lock else Icons.Default.LockOpen,
                  contentDescription = if (controller.isSecureConnection) "Encrypted HTTPS" else "Not Encrypted",
                  tint = if (controller.isSecureConnection) RiskSafe else RiskHigh,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = controller.pageTitle.ifBlank { appName },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
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
              }
            }

            IconButton(
              onClick = { controller.reload() },
              modifier = Modifier.size(36.dp).testTag("secure_webview_reload")
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

    // Main WebView Container
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

            // Security Hardened WebSettings
            settings.apply {
              this.javaScriptEnabled = enableJavaScript
              this.domStorageEnabled = true
              this.databaseEnabled = true
              this.mediaPlaybackRequiresUserGesture = false

              // Security: Disable file system access exploits
              this.allowFileAccess = false
              this.allowContentAccess = false

              // Security: Block HTTP content inside HTTPS web pages
              this.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

              this.useWideViewPort = true
              this.loadWithOverviewMode = true
              this.setSupportZoom(true)
              this.builtInZoomControls = true
              this.displayZoomControls = false

              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.safeBrowsingEnabled = true
              }

              if (enableDesktopMode) {
                this.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
              }
            }

            // JavaScript Bridge for Alerts
            if (onNotificationReceived != null) {
              addJavascriptInterface(
                SecureJsBridge(appName, onNotificationReceived),
                "AndroidBridge"
              )
            }

            // WebChromeClient handles titles, progress, WebRTC permissions
            webChromeClient = object : WebChromeClient() {
              override fun onProgressChanged(view: WebView?, newProgress: Int) {
                controller.progress = newProgress / 100f
                controller.isLoading = newProgress < 100
              }

              override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) {
                  controller.pageTitle = title
                }
              }

              override fun onPermissionRequest(request: PermissionRequest?) {
                request?.let {
                  try {
                    // Grant WebRTC camera/audio for web app voice and video calls
                    it.grant(it.resources)
                  } catch (e: Exception) {
                    it.deny()
                  }
                }
              }
            }

            // WebViewClient with SSL verification and error handling
            webViewClient = object : WebViewClient() {
              override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                val clean = pageUrl ?: ""
                controller.currentUrl = clean
                controller.isSecureConnection = clean.startsWith("https://", ignoreCase = true)
                controller.canGoBack = canGoBack()
                controller.canGoForward = canGoForward()
                controller.lastError = null
              }

              override fun onPageFinished(view: WebView?, pageUrl: String?) {
                val clean = pageUrl ?: ""
                controller.currentUrl = clean
                controller.isSecureConnection = clean.startsWith("https://", ignoreCase = true)
                controller.canGoBack = canGoBack()
                controller.canGoForward = canGoForward()

                // Inject secure notification bridge
                view?.evaluateJavascript(notificationHookScript, null)
              }

              override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                // Security: Do NOT call handler?.proceed() blindly. Cancel by default on invalid certs to prevent MITM
                controller.isSecureConnection = false
                controller.lastError = "SSL Certificate Warning: Insecure connection (${error?.primaryError})"
                handler?.cancel()
              }

              override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true) {
                  controller.lastError = error?.description?.toString() ?: "Failed to load page"
                }
              }

              override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                val didCrash = detail?.didCrash() ?: false
                val reason = if (didCrash) "Web view renderer reset. Tap Try Again to reload." else "System reclaimed web view memory."
                controller.lastError = reason
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
                val targetUrl = request?.url?.toString() ?: return false
                val scheme = request.url?.scheme?.lowercase() ?: ""

                // Handle intent, tel, mailto protocols safely
                return when (scheme) {
                  "tel", "mailto", "sms" -> {
                    runCatching {
                      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                      ctx.startActivity(intent)
                    }
                    true
                  }
                  "http", "https" -> false // Load inside WebView
                  else -> true // Block unhandled dangerous schemes
                }
              }
            }

            // Download Listener
            setDownloadListener { downloadUrl, _, _, _, _ ->
              onDownloadRequested?.invoke(downloadUrl, null)
            }

            controller.webView = this
            val sanitized = controller.sanitizeUrl(url)
            controller.currentUrl = sanitized
            loadUrl(sanitized)
          }
        },
        modifier = Modifier.fillMaxSize().testTag("secure_webview_native")
      )

      // Error Recovery Overlay
      controller.lastError?.let { errText ->
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .align(Alignment.Center)
            .testTag("secure_webview_error_card")
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
              Icon(Icons.Default.Warning, contentDescription = null, tint = RiskHigh, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Unable to Load Secure Webpage",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = errText,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
              onClick = { controller.reload() },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Try Again")
            }
          }
        }
      }
    }
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
