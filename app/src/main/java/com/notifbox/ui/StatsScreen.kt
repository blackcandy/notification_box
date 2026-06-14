package com.notifbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notifbox.data.FilterRule
import com.notifbox.data.MatchType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StatsScreen(vm: NotifViewModel) {
    val all by vm.all.collectAsState()
    val rules by vm.rules.collectAsState()

    if (all.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
            Text("还没有数据。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    // Computed off the main thread; null for the first frame while it runs.
    val statsState by produceState<Stats?>(initialValue = null, all) {
        value = withContext(Dispatchers.Default) { computeStats(all) }
    }
    val stats = statsState ?: return
    val ruledPackages = remember(rules) {
        rules.filter { it.matchType == MatchType.PACKAGE }.map { it.pattern }.toSet()
    }
    var pendingApp by remember { mutableStateOf<AppCount?>(null) }

    LazyColumn(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SummaryCard(stats) }
        item { FilterEffectCard(stats) }
        item { WeekChartCard(stats.perDay) }
        item { HourlyCard(stats.hourly) }
        item { AppRankingCard(stats.topApps, ruledPackages, onPick = { pendingApp = it }) }
        item { RuleHitCard(rules, stats.ruleHits) }
        if (stats.keywords.isNotEmpty()) item { KeywordsCard(stats.keywords) }
    }

    pendingApp?.let { app ->
        AlertDialog(
            onDismissRequest = { pendingApp = null },
            title = { Text("添加过滤规则") },
            text = { Text("将「${app.label}」加入过滤（按包名）。之后它的通知会进入「已过滤」。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.addRule(FilterRule(name = app.label, matchType = MatchType.PACKAGE, pattern = app.packageName))
                    pendingApp = null
                }) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { pendingApp = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun SummaryCard(s: Stats) {
    NbCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatCell("总通知", s.total.toString(), Modifier.weight(1f))
            StatCell("已过滤", s.filtered.toString(), Modifier.weight(1f))
            StatCell("应用数", s.appCount.toString(), Modifier.weight(1f))
            StatCell("今日", s.today.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FilterEffectCard(s: Stats) {
    val rate = if (s.total == 0) 0f else s.filtered.toFloat() / s.total
    NbCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("过滤效果", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(
                    "已拦截 ${s.filtered} 条 · ${(rate * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Bar(rate)
            Text(
                if (s.filtered == 0) "还没有通知被规则拦截。点下方应用排行可一键加规则。"
                else "这些通知已被悄悄收走，仍保留在「已过滤」里。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeekChartCard(perDay: List<DayCount>) {
    val max = (perDay.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    NbCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("近 7 天", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(
                Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                perDay.forEach { d ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Text(d.count.toString(), style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(4.dp))
                        VBar(0.7f, 86 * d.count / max)
                        Spacer(Modifier.height(4.dp))
                        Text(d.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun HourlyCard(hourly: List<Int>) {
    val max = (hourly.maxOrNull() ?: 0).coerceAtLeast(1)
    NbCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("24 小时分布", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(
                Modifier.fillMaxWidth().height(90.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                hourly.forEach { c ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
                        VBar(1f, 78 * c / max)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("0", "6", "12", "18", "23").forEach {
                    Text("${it}时", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AppRankingCard(apps: List<AppCount>, ruledPackages: Set<String>, onPick: (AppCount) -> Unit) {
    val max = (apps.firstOrNull()?.count ?: 0).coerceAtLeast(1)
    NbCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("应用排行", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("点应用可加过滤规则", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            apps.forEach { app ->
                val ruled = app.packageName in ruledPackages
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppAvatar(app.packageName, app.label, size = 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(app.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(app.count.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Bar(app.count.toFloat() / max)
                    }
                    Spacer(Modifier.width(6.dp))
                    if (ruled) {
                        Text("已过滤", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    } else {
                        IconButton(onClick = { onPick(app) }) {
                            Icon(
                                Icons.Filled.AddCircle,
                                contentDescription = "为${app.label}加过滤规则",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleHitCard(rules: List<FilterRule>, hits: Map<Long, Int>) {
    NbCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("规则命中排行", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (rules.isEmpty()) {
                Text("还没有过滤规则。点上方应用排行，或去「规则」页添加。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val ranked = rules.sortedByDescending { hits[it.id] ?: 0 }
                val max = (ranked.maxOfOrNull { hits[it.id] ?: 0 } ?: 0).coerceAtLeast(1)
                ranked.forEach { rule ->
                    val c = hits[rule.id] ?: 0
                    Column(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(rule.pattern, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$c 次", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Bar(c.toFloat() / max)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeywordsCard(keywords: List<Pair<String, Int>>) {
    val max = keywords.firstOrNull()?.second ?: 1
    NbCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("高频词", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                keywords.forEach { (word, count) ->
                    val size = (13 + 11f * count / max).sp
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            word,
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = size,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

/** A horizontal proportional bar (0..1). */
@Composable
private fun Bar(fraction: Float) {
    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.primary))
    }
}

/** A vertical bar of the given pixel-ish height (dp), filling [widthFraction] of its slot. */
@Composable
private fun VBar(widthFraction: Float, heightDp: Int) {
    Box(
        Modifier.fillMaxWidth(widthFraction)
            .height(heightDp.dp.coerceAtLeast(3.dp))
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            .background(MaterialTheme.colorScheme.primary),
    )
}
