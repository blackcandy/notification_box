package com.notifbox.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** How a rule matches text. AI-based matching is a future [MatchType]. */
enum class MatchType { CONTAINS, REGEX, PACKAGE }

/**
 * A user-defined filtering rule. When [enabled] and a notification matches
 * [pattern] per [matchType], the notification is marked filtered (hidden from the
 * main inbox, kept in history).
 */
@Entity(tableName = "rules")
data class FilterRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val matchType: MatchType,
    val pattern: String,
    val enabled: Boolean = true,
)
