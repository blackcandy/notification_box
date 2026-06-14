package com.notifbox.filter

import com.notifbox.data.FilterRule
import com.notifbox.data.MatchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {

    private fun rule(type: MatchType, pattern: String, enabled: Boolean = true, id: Long = 1) =
        FilterRule(id = id, name = pattern, matchType = type, pattern = pattern, enabled = enabled)

    @Test fun `contains matches case-insensitively in title or text`() {
        val rules = listOf(rule(MatchType.CONTAINS, "促销"))
        assertTrue(RuleEngine.evaluate(rules, "com.shop", "限时促销", null).filtered)
        assertTrue(RuleEngine.evaluate(rules, "com.shop", null, "全场大促销！").filtered)
        assertFalse(RuleEngine.evaluate(rules, "com.bank", "转账成功", "到账100元").filtered)
    }

    @Test fun `package rule matches exact package`() {
        val rules = listOf(rule(MatchType.PACKAGE, "com.spam.app"))
        assertTrue(RuleEngine.evaluate(rules, "com.spam.app", "hi", null).filtered)
        assertFalse(RuleEngine.evaluate(rules, "com.good.app", "hi", null).filtered)
    }

    @Test fun `regex rule matches pattern`() {
        val rules = listOf(rule(MatchType.REGEX, "\\d{6}"))
        assertTrue(RuleEngine.evaluate(rules, "com.bank", "验证码 123456", null).filtered)
        assertFalse(RuleEngine.evaluate(rules, "com.bank", "验证码 12", null).filtered)
    }

    @Test fun `disabled rules are skipped`() {
        val rules = listOf(rule(MatchType.CONTAINS, "促销", enabled = false))
        assertFalse(RuleEngine.evaluate(rules, "com.shop", "促销", null).filtered)
    }

    @Test fun `first matching enabled rule wins and is reported`() {
        val rules = listOf(
            rule(MatchType.CONTAINS, "广告", id = 7),
            rule(MatchType.CONTAINS, "促销", id = 8),
        )
        val verdict = RuleEngine.evaluate(rules, "com.shop", "促销广告", null)
        assertTrue(verdict.filtered)
        assertEquals(7L, verdict.matchedRuleId)
    }

    @Test fun `invalid regex does not crash and does not match`() {
        val rules = listOf(rule(MatchType.REGEX, "[unclosed"))
        assertFalse(RuleEngine.evaluate(rules, "com.x", "anything", null).filtered)
    }
}
