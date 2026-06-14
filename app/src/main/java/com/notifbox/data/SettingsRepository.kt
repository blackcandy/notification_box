package com.notifbox.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** User preferences backed by DataStore. */
class SettingsRepository(private val context: Context) {

    val removeFromShade: Flow<Boolean> =
        context.dataStore.data.map { it[REMOVE_FROM_SHADE] ?: true }

    val retentionDays: Flow<Int> =
        context.dataStore.data.map { it[RETENTION_DAYS] ?: DEFAULT_RETENTION_DAYS }

    suspend fun setRemoveFromShade(value: Boolean) {
        context.dataStore.edit { it[REMOVE_FROM_SHADE] = value }
    }

    suspend fun setRetentionDays(days: Int) {
        context.dataStore.edit { it[RETENTION_DAYS] = days }
    }

    companion object {
        const val DEFAULT_RETENTION_DAYS = 30
        private val REMOVE_FROM_SHADE = booleanPreferencesKey("remove_from_shade")
        private val RETENTION_DAYS = intPreferencesKey("retention_days")
    }
}
