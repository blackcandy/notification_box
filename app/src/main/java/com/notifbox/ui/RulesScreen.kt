package com.notifbox.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.notifbox.data.FilterRule
import com.notifbox.data.MatchType
import com.notifbox.util.NotificationAccess

@Composable
fun RulesScreen(vm: NotifViewModel) {
    val context = LocalContext.current
    val rules by vm.rules.collectAsState()
    // Re-check whenever the screen resumes (user may have just granted access).
    val lifecycleOwner = LocalLifecycleOwner.current
    val accessGranted by produceState(
        initialValue = NotificationAccess.isGranted(context),
        lifecycleOwner,
    ) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            value = NotificationAccess.isGranted(context)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        if (!accessGranted) {
            item {
                NbCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("需要通知访问权限", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "开启后，应用才能记录并过滤到达的通知。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = { context.startActivity(NotificationAccess.settingsIntent()) }) {
                            Text("前往授权")
                        }
                    }
                }
            }
        }

        item { AddRuleForm(onAdd = vm::addRule) }

        items(rules, key = { it.id }) { rule ->
            SwipeableRuleRow(
                rule = rule,
                onToggle = { vm.toggleRule(rule) },
                onDelete = { vm.deleteRule(rule) },
            )
        }
    }
}

@Composable
private fun AddRuleForm(onAdd: (FilterRule) -> Unit) {
    var pattern by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(MatchType.CONTAINS) }

    NbCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("新增过滤规则", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MatchType.entries.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(label(t)) },
                    )
                }
            }
            OutlinedTextField(
                value = pattern,
                onValueChange = { pattern = it },
                label = { Text(hint(type)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
            )
            Button(
                onClick = {
                    if (pattern.isNotBlank()) {
                        onAdd(FilterRule(name = pattern, matchType = type, pattern = pattern.trim()))
                        pattern = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("添加规则") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableRuleRow(rule: FilterRule, onToggle: () -> Unit, onDelete: () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                if (state.targetValue == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.surface,
                label = "swipe-bg",
            )
            Box(
                Modifier.fillMaxSize().clip(NbCardShape).background(color),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 20.dp),
                )
            }
        },
    ) {
        RuleRow(rule = rule, onToggle = onToggle, onDelete = onDelete)
    }
}

@Composable
private fun RuleRow(rule: FilterRule, onToggle: () -> Unit, onDelete: () -> Unit) {
    NbCard(contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    rule.pattern,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    label(rule.matchType),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = rule.enabled, onCheckedChange = { onToggle() })
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun label(t: MatchType): String = when (t) {
    MatchType.CONTAINS -> "包含关键词"
    MatchType.REGEX -> "正则匹配"
    MatchType.PACKAGE -> "按应用包名"
}

private fun hint(t: MatchType): String = when (t) {
    MatchType.CONTAINS -> "关键词，如：促销"
    MatchType.REGEX -> "正则表达式"
    MatchType.PACKAGE -> "包名，如：com.example.app"
}
