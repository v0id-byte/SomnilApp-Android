package com.somnil.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somnil.app.data.local.SleepSessionDao
import com.somnil.app.domain.model.SleepSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val sleepSessionDao: SleepSessionDao
) : ViewModel() {

    private val _sessions = MutableStateFlow<List<SleepSession>>(emptyList())
    val sessions: StateFlow<List<SleepSession>> = _sessions.asStateFlow()

    init {
        loadSessions()
    }

    private fun loadSessions() {
        viewModelScope.launch {
            sleepSessionDao.getAllSessions().collect { entities ->
                _sessions.value = entities.map { it.toSession() }
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            // TODO: Delete from database when delete method is added to DAO
            _sessions.value = _sessions.value.filter { it.id != sessionId }
        }
    }

    fun refresh() {
        loadSessions()
    }
}