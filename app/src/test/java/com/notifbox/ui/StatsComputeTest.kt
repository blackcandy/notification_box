package com.notifbox.ui

import com.notifbox.data.NotificationEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsComputeTest {

    private fun notif(
        pkg: String = "com.app",
        label: String = "App",
        title: String? = null,
        text: String? = null,
        postedAt: Long = System.currentTimeMillis(),
        filtered: Boolean = false,
        matchedRuleId: Long? = null,
    ) = NotificationEntity(
        packageName = pkg, appLabel = label, title = title, text = text,
        subText = null, postedAt = postedAt, filtered = filtered, matchedRuleId = matchedRuleId,
    )

    @Test fun `totals and filtered count`() {
        val s = computeStats(
            listOf(
                notif(filtered = true),
                notif(filtered = true),
                notif(filtered = false),
            ),
        )
        assertEquals(3, s.total)
        assertEquals(2, s.filtered)
    }

    @Test fun `app count is distinct packages`() {
        val s = computeStats(
            listOf(
                notif(pkg = "com.a"),
                notif(pkg = "com.a"),
                notif(pkg = "com.b"),
            ),
        )
        assertEquals(2, s.appCount)
    }

    @Test fun `top apps ranked by count, capped at 8`() {
        val all = buildList {
            repeat(5) { add(notif(pkg = "com.busy", label = "Busy")) }
            repeat(2) { add(notif(pkg = "com.quiet", label = "Quiet")) }
            // 8 more distinct apps to exceed the cap
            repeat(8) { add(notif(pkg = "com.x$it", label = "X$it")) }
        }
        val s = computeStats(all)
        assertEquals("com.busy", s.topApps.first().packageName)
        assertEquals(5, s.topApps.first().count)
        assertEquals(8, s.topApps.size)
    }

    @Test fun `rule hits grouped by matched rule id`() {
        val s = computeStats(
            listOf(
                notif(filtered = true, matchedRuleId = 1),
                notif(filtered = true, matchedRuleId = 1),
                notif(filtered = true, matchedRuleId = 2),
                notif(filtered = false),
            ),
        )
        assertEquals(2, s.ruleHits[1])
        assertEquals(1, s.ruleHits[2])
    }

    @Test fun `keywords surface repeated chinese bigrams above threshold`() {
        val all = List(3) { notif(text = "限时促销活动") }
        val words = computeStats(all).keywords.map { it.first }
        assertTrue("expected 促销 bigram, got $words", "促销" in words)
    }

    @Test fun `keywords drop singletons`() {
        // "促销" appears only once -> below the >=2 threshold.
        val s = computeStats(listOf(notif(text = "促销")))
        assertTrue(s.keywords.isEmpty())
    }

    @Test fun `hourly has 24 buckets and perDay has 7`() {
        val s = computeStats(listOf(notif()))
        assertEquals(24, s.hourly.size)
        assertEquals(7, s.perDay.size)
    }
}
