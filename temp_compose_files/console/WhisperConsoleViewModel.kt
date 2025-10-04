package com.mira.com.whisper.console

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mira.com.whisper.console.data.WhisperJobRepo
import com.mira.com.whisper.console.model.JobRow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ConsoleState(
    val jobs: List<JobRow> = emptyList(),
    val isWatching: Boolean = true
)

class WhisperConsoleViewModel(
    private val repo: WhisperJobRepo = WhisperJobRepo()
) : ViewModel() {

    private val _state = MutableStateFlow(ConsoleState())
    val state: StateFlow<ConsoleState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            repo.watchJobs().collect {
                refresh()
            }
        }
    }

    fun refresh() {
        val jobs = repo.listJobs()
        _state.update { it.copy(jobs = jobs) }
    }

    override fun onCleared() {
        super.onCleared()
        repo.stopWatching()
    }
}
