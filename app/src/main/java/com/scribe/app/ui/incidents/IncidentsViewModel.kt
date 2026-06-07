package com.scribe.app.ui.incidents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.scribe.app.data.remote.dto.IncidentDto
import com.scribe.app.data.repository.IncidentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IncidentsUiState(
    val loading: Boolean = false,
    val incidents: List<IncidentDto> = emptyList(),
    val activeOnly: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class IncidentsViewModel @Inject constructor(
    private val repo: IncidentRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(IncidentsUiState())
    val ui: StateFlow<IncidentsUiState> = _ui.asStateFlow()

    init { refresh() }

    fun toggleActiveOnly(v: Boolean) {
        _ui.update { it.copy(activeOnly = v) }
        refresh()
    }

    fun refresh() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.list()
                .onSuccess { list ->
                    val filtered = if (_ui.value.activeOnly) {
                        list.filter { it.status?.uppercase() != "ARCHIVÉ" && it.status?.uppercase() != "RÉSOLU" }
                    } else list
                    _ui.update { it.copy(loading = false, incidents = filtered) }
                }
                .onFailure { e ->
                    _ui.update { it.copy(loading = false, error = e.message ?: "Erreur de chargement") }
                }
        }
    }
}
