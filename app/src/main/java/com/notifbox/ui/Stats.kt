package com.notifbox.ui

import com.notifbox.data.NotificationEntity
import java.util.Calendar

internal const val DAY_MS = 24L * 60 * 60 * 1000

internal data class Stats(
    val total: Int,
    val filtered: Int,
    val appCount: Int,
    val today: Int,
    val perDay: List<DayCount>,
    val hourly: List<Int>,
    val topApps: List<AppCount>,
    val ruleHits: Map<Long, Int>,
    val keywords: List<Pair<String, Int>>,
)

internal data class DayCount(val label: String, val count: Int)

data class AppCount(val packageName: String, val label: String, val count: Int)

private val STOPWORDS = setOf(
    "的", "了", "在", "是", "我", "你", "他", "她", "它", "和", "与", "就", "都", "也", "要",
    "不", "有", "这", "那", "已", "为", "到", "个", "条", "新", "您", "com", "www", "http",
    "https", "app", "android", "上", "下", "中", "请", "您的", "一个",
)

internal fun computeStats(all: List<NotificationEntity>): Stats {
    val todayStart = startOfToday()
    val weekdays = arrayOf("日", "一", "二", "三", "四", "五", "六")
    val cal = Calendar.getInstance()

    val perDay = (6 downTo 0).map { offset ->
        val start = todayStart - offset * DAY_MS
        cal.timeInMillis = start
        DayCount("周" + weekdays[cal.get(Calendar.DAY_OF_WEEK) - 1], all.count { it.postedAt in start until start + DAY_MS })
    }

    val hourly = IntArray(24)
    all.forEach { cal.timeInMillis = it.postedAt; hourly[cal.get(Calendar.HOUR_OF_DAY)]++ }

    val topApps = all.groupBy { it.packageName }
        .map { (pkg, list) -> AppCount(pkg, list.first().appLabel, list.size) }
        .sortedByDescending { it.count }
        .take(8)

    val ruleHits = all.mapNotNull { it.matchedRuleId }.groupingBy { it }.eachCount()

    return Stats(
        total = all.size,
        filtered = all.count { it.filtered },
        appCount = all.map { it.packageName }.distinct().size,
        today = all.count { it.postedAt >= todayStart },
        perDay = perDay,
        hourly = hourly.toList(),
        topApps = topApps,
        ruleHits = ruleHits,
        keywords = topKeywords(all),
    )
}

/** Approximate keyword frequency: Latin words + Chinese bigrams, minus stopwords. */
internal fun topKeywords(all: List<NotificationEntity>): List<Pair<String, Int>> {
    val counts = HashMap<String, Int>()
    val cjk = Regex("[\\u4e00-\\u9fa5]{2,}")
    val latin = Regex("[A-Za-z]{2,}")
    all.forEach { n ->
        val text = listOfNotNull(n.title, n.text).joinToString(" ")
        latin.findAll(text).forEach {
            val k = it.value.lowercase()
            if (k !in STOPWORDS) counts[k] = (counts[k] ?: 0) + 1
        }
        cjk.findAll(text).forEach { run ->
            val s = run.value
            for (i in 0..s.length - 2) {
                val bg = s.substring(i, i + 2)
                if (bg !in STOPWORDS) counts[bg] = (counts[bg] ?: 0) + 1
            }
        }
    }
    return counts.entries.filter { it.value >= 2 }.sortedByDescending { it.value }.take(20).map { it.key to it.value }
}

private fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
