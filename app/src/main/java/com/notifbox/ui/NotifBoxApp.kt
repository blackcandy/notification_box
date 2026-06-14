package com.notifbox.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.notifbox.data.NotificationEntity
import java.text.DateFormat
import java.util.Date

// DateFormat is not thread-safe; use one instance per thread.
private val DATE_TIME_FORMAT = ThreadLocal.withInitial {
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
}

// Saves only the set of collapsed app labels so collapse state survives config changes.
private val CollapsedSaver = listSaver<SnapshotStateMap<String, Boolean>, String>(
    save = { map -> map.filter { it.value }.keys.toList() },
    restore = { list -> mutableStateMapOf(*list.map { it to true }.toTypedArray()) },
)

private enum class Tab(val route: String, val label: String) {
    Inbox("inbox", "收件箱"),
    Filtered("filtered", "已过滤"),
    Stats("stats", "统计"),
    Settings("settings", "设置"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotifBoxRoot(vm: NotifViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination
    val route = current?.route
    val onTab = Tab.entries.any { tab -> current?.hierarchy?.any { it.route == tab.route } == true }
    val title = when {
        route == "rules" -> "过滤规则"
        route?.startsWith("detail") == true -> "通知详情"
        else -> "通知滤盒"
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (!onTab) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (onTab) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        val selected = current?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    when (tab) {
                                        Tab.Inbox -> Icons.Filled.Inbox
                                        Tab.Filtered -> Icons.Filled.Block
                                        Tab.Stats -> Icons.Filled.Insights
                                        Tab.Settings -> Icons.Filled.Settings
                                    },
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Inbox.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.Inbox.route) {
                val items by vm.inbox.collectAsState()
                NotificationList(
                    items,
                    empty = "暂无通知。授予通知访问权限后，新通知会出现在这里。",
                    onClick = { navController.navigate("detail/$it") },
                )
            }
            composable(Tab.Filtered.route) {
                val items by vm.filtered.collectAsState()
                NotificationList(
                    items,
                    empty = "还没有被规则过滤的通知。",
                    onClick = { navController.navigate("detail/$it") },
                )
            }
            composable(Tab.Stats.route) {
                StatsScreen(vm)
            }
            composable(Tab.Settings.route) {
                SettingsScreen(vm, onOpenRules = { navController.navigate("rules") })
            }
            composable("rules") {
                RulesScreen(vm)
            }
            composable("detail/{id}") { entry ->
                val id = entry.arguments?.getString("id")?.toLongOrNull() ?: -1L
                DetailScreen(vm, id)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotificationList(
    items: List<NotificationEntity>,
    empty: String,
    onClick: (Long) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyState(Icons.Outlined.Inbox, empty)
        return
    }

    var rawQuery by rememberSaveable { mutableStateOf("") }
    var selectedPackage by rememberSaveable { mutableStateOf<String?>(null) }
    // Collapse state survives config changes via a custom Saver.
    val collapsed = rememberSaveable(saver = CollapsedSaver) { mutableStateMapOf() }
    // Debounce the search query: clears immediately, filters after 300 ms of idle typing.
    val q by produceState(initialValue = "", rawQuery) {
        if (rawQuery.isBlank()) value = "" else { delay(300); value = rawQuery.trim() }
    }

    val messagedApps = remember(items) {
        items.distinctBy { it.packageName }.map { InstalledApp(it.packageName, it.appLabel) }
    }
    val visible = remember(items, selectedPackage, q) {
        items.filter { n ->
            (selectedPackage == null || n.packageName == selectedPackage) &&
                (q.isEmpty() ||
                    n.appLabel.contains(q, ignoreCase = true) ||
                    n.title?.contains(q, ignoreCase = true) == true ||
                    n.text?.contains(q, ignoreCase = true) == true)
        }
    }
    val groups = remember(visible) { visible.groupBy { it.appLabel } }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AppFilterDropdown(
                selectedPackage = selectedPackage,
                onSelect = { selectedPackage = it },
                messagedApps = messagedApps,
            )
            OutlinedTextField(
                value = rawQuery,
                onValueChange = { rawQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("搜索通知…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (rawQuery.isNotEmpty()) {
                        IconButton(onClick = { rawQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "清除")
                        }
                    }
                },
                singleLine = true,
            )
        }

        if (visible.isEmpty()) {
            EmptyState(Icons.Outlined.SearchOff, "没有匹配的通知。")
            return@Column
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            groups.forEach { (app, list) ->
                // When a single app is filtered in the picker, skip headers/collapse.
                val grouped = selectedPackage == null
                val isCollapsed = grouped && collapsed[app] == true
                if (grouped) {
                    stickyHeader(key = "header_$app") {
                        GroupHeader(
                            app = app,
                            count = list.size,
                            collapsed = isCollapsed,
                            onToggle = { collapsed[app] = !(collapsed[app] ?: false) },
                        )
                    }
                }
                // Collapsed groups still show the single most recent notification.
                val shown = if (isCollapsed) list.take(1) else list
                items(shown, key = { it.id }) { n -> NotificationCard(n, onClick) }
            }
        }
    }
}

@Composable
private fun GroupHeader(app: String, count: Int, collapsed: Boolean, onToggle: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(top = 10.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                app,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(6.dp))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    count.toString(),
                    Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(
                if (collapsed) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                contentDescription = if (collapsed) "展开" else "折叠",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NotificationCard(n: NotificationEntity, onClick: (Long) -> Unit) {
    NbCard(onClick = { onClick(n.id) }, contentPadding = PaddingValues(14.dp)) {
      Row {
        AppAvatar(n.packageName, n.appLabel)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        n.title?.takeIf { it.isNotBlank() } ?: n.appLabel,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        formatTime(n.postedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                n.text?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
      }
    }

@Composable
private fun EmptyState(icon: ImageVector, text: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outlineVariant,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

internal fun formatTime(epoch: Long): String = DATE_TIME_FORMAT.get()!!.format(Date(epoch))
