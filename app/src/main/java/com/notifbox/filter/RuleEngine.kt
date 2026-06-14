package com.notifbox.filter

import com.notifbox.data.FilterRule
import com.notifbox.data.MatchType

/** Outcome of evaluating a notification against the active rules. */
data class Verdict(val filtered: Boolean, val matchedRuleId: Long? = null)

/**
 * Stateless rule evaluator. The first enabled rule that matches wins.
 *
 * This is intentionally a plain function over data so it is trivial to unit-test
 * and, later, to swap/augment with an on-device AI classifier without touching
 * the listener service.
 */
object RuleEngine {

    fun evaluate(
        rules: List<FilterRule>,
        packageName: String,
        title: String?,
        text: String?,
    ): Verdict {
        val haystack = listOfNotNull(title, text).joinToString("\n")
        for (rule in rules) {
            if (!rule.enabled) continue
            val matched = when (rule.matchType) {
                MatchType.CONTAINS -> haystack.contains(rule.pattern, ignoreCase = true)
                MatchType.PACKAGE -> packageName.equals(rule.pattern, ignoreCase = true)
                MatchType.REGEX -> runCatching {
                    Regex(rule.pattern, RegexOption.IGNORE_CASE).containsMatchIn(haystack)
                }.getOrDefault(false)
            }
            if (matched) return Verdict(filtered = true, matchedRuleId = rule.id)
        }
        return Verdict(filtered = false)
    }
}
