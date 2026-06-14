package com.notifbox.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.notifbox.data.NotificationEntity

@Composable
fun DetailScreen(vm: NotifViewModel, id: Long) {
    val notif by produceState<NotificationEntity?>(initialValue = null, id) {
        value = vm.getNotification(id)
    }

    val n = notif
    if (n == null) {
        Column(Modifier.fillMaxSize().padding(24.dp)) { Text("通知不存在或已被清理。") }
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        NbCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppAvatar(n.packageName, n.appLabel, size = 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(n.appLabel, style = MaterialTheme.typography.titleMedium)
                        Text(
                            n.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            formatTime(n.postedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (n.filtered) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(if (n.matchedRuleId != null) "已过滤 · 规则 #${n.matchedRuleId}" else "已过滤")
                        },
                    )
                }
            }
        }

        NbCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Field("标题", n.title)
                Field("内容", n.text)
                Field("副文本", n.subText)
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
