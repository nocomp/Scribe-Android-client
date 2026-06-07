package com.scribe.app.ui.incidents

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.scribe.app.data.remote.dto.IncidentDto
import com.scribe.app.data.remote.dto.JalonDto
import com.scribe.app.data.repository.IncidentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.time.Instant
import javax.inject.Inject

data class JalonUi(val label: String, val done: Boolean, val doneAt: String?)

data class IncidentDetailUiState(
    val loading: Boolean = true,
    val incident: IncidentDto? = null,
    val jalons: List<JalonUi> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class IncidentDetailViewModel @Inject constructor(
    private val repo: IncidentRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val id: Int = savedStateHandle.get<Int>("id") ?: -1
    private val _ui = MutableStateFlow(IncidentDetailUiState())
    val ui: StateFlow<IncidentDetailUiState> = _ui.asStateFlow()

    init { reload() }

    private fun reload() {
        viewModelScope.launch {
            val inc = repo.get(id)
            _ui.update {
                if (inc != null)
                    it.copy(loading = false, incident = inc, jalons = parseJalons(inc.jalons), error = null)
                else
                    it.copy(loading = false, error = "Incident introuvable.")
            }
        }
    }

    fun clearMessage() = _ui.update { it.copy(message = null) }

    fun toggleJalon(index: Int) {
        val current = _ui.value.jalons
        if (index !in current.indices || _ui.value.busy) return
        val now = nowIso()
        val updated = current.mapIndexed { i, j ->
            if (i == index) j.copy(done = !j.done, doneAt = if (!j.done) now else null) else j
        }
        _ui.update { it.copy(busy = true, jalons = updated) }
        viewModelScope.launch {
            repo.updateJalons(id, updated.map { JalonDto(it.label, it.done, it.doneAt) })
                .onSuccess { _ui.update { it.copy(busy = false) }; reload() }
                .onFailure { e ->
                    _ui.update { it.copy(busy = false, message = "Échec jalon (${e.message})") }
                    reload()
                }
        }
    }

    fun changeStatus(status: String) {
        if (_ui.value.busy) return
        _ui.update { it.copy(busy = true) }
        viewModelScope.launch {
            repo.updateStatus(id, status)
                .onSuccess {
                    _ui.update { it.copy(busy = false, message = "Statut mis à jour : $status") }
                    reload()
                }
                .onFailure { e ->
                    val msg = if (status == "RÉSOLU" && (e.message?.contains("400") == true))
                        "Validez au moins un jalon avant de passer en Résolu."
                    else "Échec du changement de statut (${e.message})"
                    _ui.update { it.copy(busy = false, message = msg) }
                }
        }
    }

    private fun nowIso(): String = runCatching { Instant.now().toString() }.getOrDefault("")

    private fun parseJalons(raw: String?): List<JalonUi> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                JalonUi(
                    label = o.optString("label"),
                    done = o.optBoolean("done"),
                    doneAt = o.optString("done_at").ifBlank { null },
                )
            }
        }.getOrDefault(emptyList())
    }
}
