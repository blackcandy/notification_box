package com.notifbox.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Upsert
    suspend fun upsert(rule: FilterRule): Long

    @Delete
    suspend fun delete(rule: FilterRule)

    @Query("SELECT * FROM rules ORDER BY id")
    fun observeAll(): Flow<List<FilterRule>>

    @Query("SELECT * FROM rules WHERE enabled = 1")
    suspend fun enabledRules(): List<FilterRule>
}
