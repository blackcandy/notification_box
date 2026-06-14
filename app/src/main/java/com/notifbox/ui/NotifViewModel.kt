package com.notifbox.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notifbox.data.FilterRule
import com.notifbox.data.NotifRepository
import com.notifbox.data.NotificationEntity
import com.notifbox.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotifViewModel(
    private val repo: NotifRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    val inbox = repo.inbox.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val filtered = repo.filtered.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val all = repo.all.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val rules = repo.rules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val removeFromShade =
        settings.removeFromShade.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val retentionDays =
        settings.retentionDays.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_RETENTION_DAYS,
        )

    fun addRule(rule: FilterRule) = viewModelScope.launch { repo.upsertRule(rule) }
    fun toggleRule(rule: FilterRule) = viewModelScope.launch { repo.upsertRule(rule.copy(enabled = !rule.enabled)) }
    fun deleteRule(rule: FilterRule) = viewModelScope.launch { repo.deleteRule(rule) }
    fun clearHistory() = viewModelScope.launch { repo.clearHistory() }

    fun setRemoveFromShade(value: Boolean) = viewModelScope.launch { settings.setRemoveFromShade(value) }
    fun setRetentionDays(days: Int) = viewModelScope.launch { settings.setRetentionDays(days) }

    suspend fun getNotification(id: Long): NotificationEntity? = repo.getById(id)

    class Factory(
        private val repo: NotifRepository,
        private val settings: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NotifViewModel(repo, settings) as T
    }
}
