package com.notifbox.data

import kotlinx.coroutines.flow.Flow

/** Single entry point the UI and service use to read/write notifications and rules. */
class NotifRepository(
    private val notificationDao: NotificationDao,
    private val ruleDao: RuleDao,
) {
    val inbox: Flow<List<NotificationEntity>> = notificationDao.observeInbox()
    val filtered: Flow<List<NotificationEntity>> = notificationDao.observeFiltered()
    val all: Flow<List<NotificationEntity>> = notificationDao.observeAll()
    val rules: Flow<List<FilterRule>> = ruleDao.observeAll()

    suspend fun record(entity: NotificationEntity): Long = notificationDao.insert(entity)

    suspend fun getById(id: Long): NotificationEntity? = notificationDao.getById(id)

    suspend fun latestByKey(key: String): NotificationEntity? = notificationDao.latestByKey(key)

    suspend fun enabledRules(): List<FilterRule> = ruleDao.enabledRules()

    suspend fun upsertRule(rule: FilterRule) { ruleDao.upsert(rule) }
    suspend fun deleteRule(rule: FilterRule) { ruleDao.delete(rule) }

    suspend fun clearHistory() = notificationDao.clear()
    suspend fun pruneOlderThan(cutoff: Long) = notificationDao.deleteOlderThan(cutoff)
}
