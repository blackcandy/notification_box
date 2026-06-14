package com.notifbox.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One captured notification. We persist a flattened snapshot rather than the live
 * StatusBarNotification because the system recycles those once dismissed.
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appLabel: String,
    val title: String?,
    val text: String?,
    val subText: String?,
    val postedAt: Long,
    /** Result of the rule engine: see [com.notifbox.filter.Verdict]. */
    val filtered: Boolean = false,
    val matchedRuleId: Long? = null,
)
