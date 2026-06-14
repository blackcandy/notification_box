package com.notifbox.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.notifbox.util.BatteryOptimization
import com.notifbox.util.NotificationAccess

private val RETENTION_OPTIONS = listOf(7, 30, 90, 365)

@Composable
fun SettingsScreen(vm: NotifViewModel, onOpenRules: () -> Unit) {
    val context = LocalContext.current
    val removeFromShade by vm.removeFromShade.collectAsState()
    val retentionDays by vm.retentionDays.collectAsState()
    val rules by vm.rules.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }
    // Re-check when the screen resumes (user may have just toggled battery optimization).
    val lifecycleOwner = LocalLifecycleOwner.current
    val isIgnoringBattery by produceState(
        initialValue = BatteryOptimization.isIgnoring(context),
        lifecycleOwner,
    ) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            value = BatteryOptimization.isIgnoring(context)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        NbCard(onClick = onOpenRules) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("过滤规则", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "管理关键词 / 正则 / 按应用过滤" +
                            (if (rules.isNotEmpty()) " · ${rules.size} 条" else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        NbCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("过滤后从通知栏移除", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "命中规则的通知会被悄悄收走，仍保留在历史里。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = removeFromShade, onCheckedChange = { vm.setRemoveFromShade(it) })
            }
        }

        NbCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("历史保留时长", style = MaterialTheme.typography.titleSmall)
                Text(
                    "超过该时长的通知会在后台自动清理。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RETENTION_OPTIONS.forEach { days ->
                        FilterChip(
                            selected = retentionDays == days,
                            onClick = { vm.setRetentionDays(days) },
                            label = { Text(if (days >= 365) "1 年" else "$days 天") },
                        )
                    }
                }
            }
        }

        NbCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("后台保活", style = MaterialTheme.typography.titleSmall)
                Text(
                    "国产系统可能在后台杀掉应用，导致漏记通知。建议：①加入电池白名单；" +
                        "②到系统「应用信息 → 耗电管理 / 自启动」里允许本应用后台运行、关闭省电限制。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { context.startActivity(BatteryOptimization.requestIntent(context)) },
                    enabled = !isIgnoringBattery,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (isIgnoringBattery) "已加入电池白名单 ✓" else "申请加入电池白名单")
                }
                OutlinedButton(
                    onClick = { context.startActivity(BatteryOptimization.appDetailsIntent(context)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("打开系统设置（开启自启动）")
                }
                OutlinedButton(
                    onClick = { NotificationAccess.forceRebind(context) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("重新连接监听服务（漏记时点这里）")
                }
                Text(
                    "在系统页里：允许「自启动」、设为「不限制 / 允许后台运行」。" +
                        "另外在最近任务里把本应用「下拉锁定」，划掉时就不会被杀。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        TextButton(onClick = { confirmClear = true }) {
            Text("清空所有历史记录", color = MaterialTheme.colorScheme.error)
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空历史") },
            text = { Text("将永久删除所有已记录的通知，规则不受影响。") },
            confirmButton = {
                TextButton(onClick = { vm.clearHistory(); confirmClear = false }) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("取消") }
            },
        )
    }
}
