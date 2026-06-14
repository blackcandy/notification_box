package com.notifbox.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert
    suspend fun insert(entity: NotificationEntity): Long

    @Query("SELECT * FROM notifications ORDER BY postedAt DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE filtered = 0 ORDER BY postedAt DESC")
    fun observeInbox(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE filtered = 1 ORDER BY postedAt DESC")
    fun observeFiltered(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getById(id: Long): NotificationEntity?

    /** Most recent record for a system notification key, used to dedupe re-posts. */
    @Query("SELECT * FROM notifications WHERE sbnKey = :key ORDER BY postedAt DESC LIMIT 1")
    suspend fun latestByKey(key: String): NotificationEntity?

    @Query("DELETE FROM notifications WHERE postedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM notifications")
    suspend fun clear()
}
