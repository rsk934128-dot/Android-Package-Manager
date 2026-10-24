package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldTertiary

data class CloneColorOption(val label: String, val colorHex: Long, val color: Color)

val CLONE_COLOR_PRESETS = listOf(
  CloneColorOption("Emerald", 0xFF10B981L, Color(0xFF10B981)),
  CloneColorOption("Blue", 0xFF3B82F6L, Color(0xFF3B82F6)),
  CloneColorOption("Purple", 0xFF8B5CF6L, Color(0xFF8B5CF6)),
  CloneColorOption("Amber", 0xFFF59E0BL, Color(0xFFF59E0B)),
  CloneColorOption("Rose", 0xFFF43F5EL, Color(0xFFF43F5E))
)

val CLONE_BADGE_PRESETS = listOf(
  "Clone 1",
  "Work (অফিস)",
  "Private (ব্যক্তিগত)",
  "Dual #2",
  "Secondary"
)

val CLONE_EMOJI_PRESETS = listOf("👥", "💼", "🔒", "🤖", "💬", "⭐", "📱", "🎯")

@Composable
fun CloneAppDialog(
  originalAppName: String,
  defaultBadge: String = "Clone 1",
  onDismiss: () -> Unit,
  onConfirm: (cloneName: String, cloneBadge: String, emoji: String, colorTintHex: Long) -> Unit
) {
  var cloneName by remember { mutableStateOf("$originalAppName (Clone)") }
  var selectedBadge by remember { mutableStateOf(defaultBadge) }
  var selectedEmoji by remember { mutableStateOf("👥") }
  var selectedColorHex by remember { mutableLongStateOf(CLONE_COLOR_PRESETS[0].colorHex) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(EmeraldTertiary.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Layers, contentDescription = null, tint = EmeraldTertiary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "Clone App / অ্যাপ ক্লোন করুন",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
          )
          Text(
            text = "Multi-Account & Dual Space Instance",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Preview Card
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = Color(selectedColorHex).copy(alpha = 0.2f),
              modifier = Modifier.size(44.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(selectedEmoji, fontSize = 22.sp)
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = cloneName.ifBlank { originalAppName },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Color(selectedColorHex).copy(alpha = 0.2f)
                ) {
                  Text(
                    text = selectedBadge.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(selectedColorHex),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Isolated Sandbox",
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Clone Title Input
        OutlinedTextField(
          value = cloneName,
          onValueChange = { cloneName = it },
          label = { Text("Cloned App Title / নাম") },
          placeholder = { Text("e.g. $originalAppName (Work)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("clone_app_name_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Badge Preset Selection
        Text(
          text = "Select Account Badge / ব্যাজ:",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(CLONE_BADGE_PRESETS) { badge ->
            FilterChip(
              selected = selectedBadge == badge,
              onClick = {
                selectedBadge = badge
                cloneName = "$originalAppName ($badge)"
              },
              label = { Text(badge, fontSize = 11.sp) }
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Color Accent Picker
        Text(
          text = "Clone Icon Color / রঙ:",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          CLONE_COLOR_PRESETS.forEach { preset ->
            val isSelected = selectedColorHex == preset.colorHex
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(preset.color)
                .clickable { selectedColorHex = preset.colorHex },
              contentAlignment = Alignment.Center
            ) {
              if (isSelected) {
                Icon(Icons.Default.Done, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Emoji Picker
        Text(
          text = "Clone Icon Emoji:",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(CLONE_EMOJI_PRESETS) { emoji ->
            val isSelected = selectedEmoji == emoji
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.clickable { selectedEmoji = emoji }
            ) {
              Text(
                text = emoji,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(cloneName, selectedBadge, selectedEmoji, selectedColorHex)
        },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.testTag("dialog_confirm_clone_button")
      ) {
        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Create Clone (ক্লোন তৈরি করুন)", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel (বাতিল)")
      }
    }
  )
}
