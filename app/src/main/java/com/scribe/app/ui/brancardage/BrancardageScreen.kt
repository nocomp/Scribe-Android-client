package com.scribe.app.ui.brancardage

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.scribe.app.data.local.SecureStore
import com.scribe.app.data.remote.dto.MissionDto
import com.scribe.app.data.repository.ContentRepository
import com.scribe.app.ui.theme.OrangeWarning
import com.scribe.app.ui.theme.VertOk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrancardageUiState(
    val loading: Boolean = true,
    val missions: List<MissionDto> = emptyList(),
    val busy: Boolean = false,
    val error: String? = null,
    val banner: String? = null,
)

@HiltViewModel
class BrancardageViewModel @Inject constructor(
    private val repo: ContentRepository,
    private val store: SecureStore,
) : ViewModel() {
    private val _ui = MutableStateFlow(BrancardageUiState())
    val ui: StateFlow<BrancardageUiState> = _ui.asStateFlow()
    private var agentNom: String = ""

    init { load() }

    fun load() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            agentNom = store.snapshot().let { it.displayName ?: it.username ?: "Brancardier" }
            repo.missions()
                .onSuccess { m -> _ui.update { it.copy(loading = false, missions = m) } }
                .onFailure { e -> _ui.update { it.copy(loading = false, error = e.message ?: "Erreur") } }
        }
    }

    private fun act(block: suspend () -> Result<Unit>, ok: String) {
        if (_ui.value.busy) return
        _ui.update { it.copy(busy = true) }
        viewModelScope.launch {
            block()
                .onSuccess { _ui.update { it.copy(busy = false, banner = ok) }; load() }
                .onFailure { e -> _ui.update { it.copy(busy = false, banner = "Échec : ${e.message}") }; load() }
        }
    }

    fun prendre(m: MissionDto) = act({ repo.brcPrendre(m.id, agentNom) }, "Mission prise en charge")
    fun arrivee(m: MissionDto) = act({ repo.brcArrivee(m.id) }, "Arrivée confirmée")
    fun annuler(m: MissionDto) = act({ repo.brcPatch(m.id, "ANNULE") }, "Mission annulée")
}

@Composable
fun BrancardageSection(
    modifier: Modifier = Modifier,
    viewModel: BrancardageViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize()) {
        if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        ui.banner?.let {
            Text(it, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Text(
            "Appui long sur une mission pour agir",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        when {
            ui.loading ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            ui.error != null && ui.missions.isEmpty() ->
                Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                    Text(ui.error!!, color = MaterialTheme.colorScheme.error)
                }
            ui.missions.isEmpty() ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Aucune mission de brancardage.") }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ui.missions, key = { it.id }) { m -> MissionCard(m, viewModel) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MissionCard(m: MissionDto, viewModel: BrancardageViewModel) {
    var menu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick = {},
            onLongClick = { menu = true },
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            if (m.statut == "EN_ATTENTE") {
                DropdownMenuItem(text = { Text("Prendre en charge") },
                    onClick = { menu = false; viewModel.prendre(m) })
            }
            if (m.statut == "EN_COURS") {
                DropdownMenuItem(text = { Text("Confirmer l'arrivée (terminé)") },
                    onClick = { menu = false; viewModel.arrivee(m) })
            }
            DropdownMenuItem(text = { Text("Annuler la mission") },
                onClick = { menu = false; viewModel.annuler(m) })
        }
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(statutColor(m.statut)))
                Spacer(Modifier.size(8.dp))
                Text(m.refPatient ?: "Mission #${m.id}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(m.statutLabel ?: m.statut ?: "—", style = MaterialTheme.typography.labelMedium, color = statutColor(m.statut))
            }
            Spacer(Modifier.size(6.dp))
            Text(
                buildString {
                    append(listOfNotNull(m.ufOrigine, m.chambreDepart?.ifBlank { null }).joinToString(" "))
                    append("  →  ")
                    append(listOfNotNull(m.ufDestination, m.chambreArrivee?.ifBlank { null }).joinToString(" "))
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                listOfNotNull(m.typeTransport, m.prioriteLabel, m.agentNom?.ifBlank { null }?.let { "Agent : $it" })
                    .joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

private fun statutColor(s: String?): Color = when (s) {
    "EN_COURS" -> OrangeWarning
    "TERMINE" -> VertOk
    "ANNULE" -> Color(0xFF94A3B8)
    else -> Color(0xFF003189)
}
