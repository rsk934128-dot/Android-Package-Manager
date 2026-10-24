package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClonedAppItem
import com.example.ui.components.WebContainer
import com.example.ui.components.rememberWebContainerController
import com.example.ui.theme.RiskSafe

@Composable
fun ClonedAppViewer(
  clone: ClonedAppItem,
  onClose: () -> Unit,
  onClearStorage: (ClonedAppItem) -> Unit
) {
  val context = LocalContext.current
  val targetUrl = clone.targetUrl ?: "https://rubel-ai-assistant-coding-studio-563399721287.us-west1.run.app/"
  val accentColor = Color(clone.colorTintHex)

  val controller = rememberWebContainerController(
    initialAllowedDomains = listOf("*.run.app", "google.com"),
    initialBlockInsecureContent = true,
    initialWhitelistEnforced = false
  )

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    // Dual Space Clone Top Banner
    Card(
      shape = RoundedCornerShape(0.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onClose,
          modifier = Modifier.size(36.dp).testTag("clone_viewer_close")
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close Clone Instance")
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Clone Identity Pill
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = accentColor.copy(alpha = 0.15f),
          modifier = Modifier.weight(1f)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = accentColor.copy(alpha = 0.25f),
              modifier = Modifier.size(28.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(clone.iconEmoji, fontSize = 14.sp)
              }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = clone.cloneName,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = accentColor
                ) {
                  Text(
                    text = clone.cloneBadge.uppercase(),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                  )
                }
              }
              Text(
                text = "Isolated Sandbox • Separate Account",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Reset Clone Data Action
        IconButton(
          onClick = {
            controller.clearAllBrowsingData {
              onClearStorage(clone)
              Toast.makeText(context, "Clone data & cookies wiped", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier.size(36.dp).testTag("clone_viewer_wipe")
        ) {
          Icon(
            Icons.Default.DeleteSweep,
            contentDescription = "Wipe Clone Data",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(19.dp)
          )
        }

        IconButton(
          onClick = { controller.reload() },
          modifier = Modifier.size(36.dp).testTag("clone_viewer_reload")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Reload Clone", modifier = Modifier.size(18.dp))
        }
      }
    }

    // Embedded WebContainer running the isolated clone instance
    WebContainer(
      url = targetUrl,
      controller = controller,
      showAddressBar = true,
      blockInsecureContent = true,
      enforceDomainWhitelist = false,
      modifier = Modifier.weight(1f)
    )
  }
}
