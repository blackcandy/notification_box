package com.notifbox.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(val packageName: String, val label: String)

/**
 * Installed apps worth filtering by: every third-party app plus user-facing system
 * apps (those with a launcher). Pure background system packages are dropped. Loaded
 * once on a background thread, third-party apps sorted first.
 */
@Composable
fun rememberInstalledApps(): List<InstalledApp> {
    val context = LocalContext.current
    val apps by produceState(initialValue = emptyList<InstalledApp>()) {
        value = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val launchable = pm.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0,
            ).mapTo(HashSet()) { it.activityInfo.packageName }

            pm.getInstalledApplications(0)
                .asSequence()
                .filter { it.packageName != context.packageName }
                .mapNotNull { ai ->
                    val nonSystem = ai.flags and ApplicationInfo.FLAG_SYSTEM == 0
                    // Keep third-party apps and any system app the user can launch;
                    // drop system background packages (incl. non-launchable updated
                    // system apps like WebView / Accessibility Suite).
                    if (!nonSystem && ai.packageName !in launchable) return@mapNotNull null
                    Triple(nonSystem, ai.packageName, pm.getApplicationLabel(ai).toString())
                }
                .sortedWith(compareByDescending<Triple<Boolean, String, String>> { it.first }
                    .thenBy { it.third.lowercase() })
                .map { InstalledApp(it.second, it.third) }
                .toList()
        }
    }
    return apps
}

/**
 * Dropdown (with search) that filters notifications by any installed app (null = all).
 * [messagedApps] are apps that have appeared in notification history; they're pinned
 * to the top in recency order so the user's actual senders are easy to reach.
 */
@Composable
fun AppFilterDropdown(
    selectedPackage: String?,
    onSelect: (String?) -> Unit,
    messagedApps: List<InstalledApp>,
    modifier: Modifier = Modifier,
) {
    val installed = rememberInstalledApps()
    var expanded by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }

    // Messaged apps first (recency order), then every other installed app.
    val ordered = remember(installed, messagedApps) {
        val pinned = messagedApps.map { it.packageName }.toHashSet()
        messagedApps + installed.filter { it.packageName !in pinned }
    }

    val shown = remember(search, ordered) {
        val q = search.trim()
        if (q.isEmpty()) ordered
        else ordered.filter { it.label.contains(q, true) || it.packageName.contains(q, true) }
    }

    Box(modifier) {
        IconButton(onClick = { search = ""; expanded = true }) {
            if (selectedPackage != null) {
                AsyncAppIcon(selectedPackage, size = 26.dp)
            } else {
                Icon(
                    Icons.Filled.Apps,
                    contentDescription = "按应用筛选",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(300.dp),
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                placeholder = { Text("搜索应用…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
            )
            DropdownMenuItem(
                text = { Text("全部应用") },
                onClick = { onSelect(null); expanded = false },
            )
            HorizontalDivider()
            // DropdownMenu measures content intrinsics, which a LazyColumn can't answer.
            // Pinning a fixed width + height makes those queries fast-return → no crash,
            // while the list stays lazily composed (so opening it isn't janky).
            val listHeight = (52.dp * shown.size).coerceIn(0.dp, 320.dp)
            LazyColumn(Modifier.width(276.dp).height(listHeight)) {
                items(shown, key = { it.packageName }) { app ->
                    DropdownMenuItem(
                        text = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { AsyncAppIcon(app.packageName) },
                        onClick = { onSelect(app.packageName); expanded = false },
                    )
                }
            }
        }
    }
}

/** App icon loaded off the main thread, so a long menu opens without jank. */
@Composable
private fun AsyncAppIcon(packageName: String, size: Dp = 28.dp) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName).toBitmap().asImageBitmap()
            }.getOrNull()
        }
    }
    val b = bitmap
    if (b != null) {
        Image(b, null, Modifier.size(size).clip(CircleShape), contentScale = ContentScale.Crop)
    } else {
        Box(Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
    }
}
