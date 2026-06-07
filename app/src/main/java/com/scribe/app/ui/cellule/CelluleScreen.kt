package com.scribe.app.ui.cellule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.scribe.app.data.local.SecureStore
import com.scribe.app.data.remote.dto.DecisionCreateRequest
import com.scribe.app.data.remote.dto.DecisionDto
import com.scribe.app.data.repository.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CelluleUiState(
    val loading: Boolean = true,
    val decisions: List<DecisionDto> = emptyList(),
    val error: String? = null,
    val composing: Boolean = false,
    val contenu: String = "",
    val base: String = "Plan Blanc",
    val sending: Boolean = false,
    val banner: String? = null,
)

@HiltViewModel
class CelluleViewModel @Inject constructor(
    private val repo: ContentRepository,
    private val store: SecureStore,
) : ViewModel() {
    private val _ui = MutableStateFlow(CelluleUiState())
    val ui: StateFlow<CelluleUiState> = _ui.asStateFlow()
    private var responsable: String = ""

    init { load() }

    fun load() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            responsable = store.snapshot().let { it.displayName ?: it.username ?: "" }
            repo.decisions()
                .onSuccess { d -> _ui.update { it.copy(loading = false, decisions = d) } }
                .onFailure { e -> _ui.update { it.copy(loading = false, error = e.message ?: "Erreur") } }
        }
    }

    fun startCompose() = _ui.update { it.copy(composing = true, contenu = "", base = "Plan Blanc") }
    fun cancel() = _ui.update { it.copy(composing = false) }
    fun setContenu(v: String) = _ui.update { it.copy(contenu = v) }
    fun setBase(v: String) = _ui.update { it.copy(base = v) }

    fun submit() {
        val s = _ui.value
        if (s.sending || s.contenu.isBlank()) return
        _ui.update { it.copy(sending = true) }
        viewModelScope.launch {
            repo.createDecision(
                DecisionCreateRequest(
                    contenu = s.contenu.trim(),
                    responsable = responsable.ifBlank { "Mobile" },
                    baseReglementaire = s.base.ifBlank { "Plan Blanc" },
                )
            ).onSuccess {
                _ui.update { it.copy(sending = false, composing = false, banner = "Décision enregistrée") }
                load()
            }.onFailure { e -> _ui.update { it.copy(sending = false, banner = "Échec : ${e.message}") } }
        }
    }
}

@Composable
fun CelluleSection(
    modifier: Modifier = Modifier,
    viewModel: CelluleViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    BackHandler(enabled = ui.composing) { viewModel.cancel() }

    if (ui.composing) {
        Column(modifier.fillMaxSize()) {
            if (ui.sending) LinearProgressIndicator(Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                TextButton(onClick = { viewModel.cancel() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    Spacer(Modifier.size(4.dp))
                    Text("Décisions")
                }
            }
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                OutlinedTextField(
                    value = ui.contenu, onValueChange = { viewModel.setContenu(it) },
                    label = { Text("Décision") },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = ui.base, onValueChange = { viewModel.setBase(it) },
                    label = { Text("Base réglementaire") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.submit() },
                    enabled = !ui.sending && ui.contenu.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Émettre la décision") }
            }
        }
    } else {
        Column(modifier.fillMaxSize()) {
            ui.banner?.let {
                Text(it, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            }
            Button(
                onClick = { viewModel.startCompose() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Nouvelle décision")
            }
            when {
                ui.loading ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                ui.error != null && ui.decisions.isEmpty() ->
                    Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                        Text(ui.error!!, color = MaterialTheme.colorScheme.error)
                    }
                ui.decisions.isEmpty() ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Aucune décision.") }
                else -> LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ui.decisions, key = { it.id }) { DecisionCard(it) }
                }
            }
        }
    }
}

@Composable
private fun DecisionCard(d: DecisionDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(d.contenu ?: "", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.size(6.dp))
            Text(
                listOfNotNull(
                    d.responsable?.ifBlank { null },
                    d.baseReglementaire?.ifBlank { null },
                    d.timestamp,
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}
