package com.scribe.app.ui.incidents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.scribe.app.data.remote.dto.CreateIncidentRequest
import com.scribe.app.data.repository.IncidentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateIncidentUiState(
    val declarantNom: String = "",
    val siteId: String = "",
    val uniteFonctionnelle: String = "",
    val typeCrise: String = "SANITAIRE",
    val urgency: Int = 1,
    val fait: String = "",
    val analyse: String = "",
    val impactFonctionnel: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
) {
    val canSubmit get() = declarantNom.isNotBlank() && siteId.isNotBlank() && fait.isNotBlank()
}

@HiltViewModel
class CreateIncidentViewModel @Inject constructor(
    private val repo: IncidentRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(CreateIncidentUiState())
    val ui: StateFlow<CreateIncidentUiState> = _ui.asStateFlow()

    val typesCrise = listOf("SANITAIRE", "CYBER", "MIXTE")
    val niveauxUrgence = listOf(1, 2, 3, 4)

    init {
        viewModelScope.launch {
            val d = repo.formDefaults()
            _ui.update { it.copy(declarantNom = d.declarantNom, siteId = d.lastSite) }
        }
    }

    fun onDeclarant(v: String) = _ui.update { it.copy(declarantNom = v) }
    fun onSite(v: String) = _ui.update { it.copy(siteId = v) }
    fun onUf(v: String) = _ui.update { it.copy(uniteFonctionnelle = v) }
    fun onType(v: String) = _ui.update { it.copy(typeCrise = v) }
    fun onUrgency(v: Int) = _ui.update { it.copy(urgency = v) }
    fun onFait(v: String) = _ui.update { it.copy(fait = v, error = null) }
    fun onAnalyse(v: String) = _ui.update { it.copy(analyse = v) }
    fun onImpact(v: Boolean) = _ui.update { it.copy(impactFonctionnel = v) }

    fun submit() {
        val s = _ui.value
        if (s.submitting || !s.canSubmit) return
        _ui.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val req = CreateIncidentRequest(
                declarantNom = s.declarantNom.trim(),
                siteId = s.siteId.trim(),
                fait = s.fait.trim(),
                typeCrise = s.typeCrise,
                urgency = s.urgency,
                uniteFonctionnelle = s.uniteFonctionnelle.trim(),
                analyse = s.analyse.trim(),
                impactFonctionnel = s.impactFonctionnel,
            )
            repo.create(req)
                .onSuccess { _ui.update { it.copy(submitting = false, done = true) } }
                .onFailure { e ->
                    _ui.update { it.copy(submitting = false, error = e.message ?: "Échec de l'envoi") }
                }
        }
    }
}
