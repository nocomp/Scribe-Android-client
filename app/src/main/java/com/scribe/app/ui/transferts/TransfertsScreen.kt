package com.scribe.app.ui.transferts

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.scribe.app.data.remote.dto.TransfertDto
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

private val STATUTS = listOf("EN_PREPARATION", "EN_COURS", "ARRIVE", "ANNULE")

data class TransfertsUiState(
    val loading: Boolean = true,
    val transferts: List<TransfertDto> = emptyList(),
    val busy: Boolean = false,
    val error: String? = null,
    val banner: String? = null,
)

@HiltViewModel
class TransfertsViewModel @Inject constructor(
    private val repo: ContentRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(TransfertsUiState())
    val ui: StateFlow<TransfertsUiState> = _ui.asStateFlow()

    init { load() }

    fun load() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.transferts()
                .onSuccess { t -> _ui.update { it.copy(loading = false, transferts = t) } }
                .onFailure { e -> _ui.update { it.copy(loading = false, error = e.message ?: "Erreur") } }
        }
    }

    fun changeStatut(id: Int, statut: String) {
        if (_ui.value.busy) return
        _ui.update { it.copy(busy = true) }
        viewModelScope.launch {
            repo.transfertStatut(id, statut)
                .onSuccess { _ui.update { it.copy(busy = false, banner = "Statut → $statut") }; load() }
                .onFailure { e ->
                    val msg = if (e.message?.contains("400") == true)
                        "Refusé (un retour en arrière exige un motif côté serveur)."
                    else "Échec : ${e.message}"
                    _ui.update { it.copy(busy = false, banner = msg) }
                }
        }
    }
}

@Composable
fun TransfertsSection(
    modifier: Modifier = Modifier,
    viewModel: TransfertsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize()) {
        if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        ui.banner?.let {
            Text(it, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Text(
            "Appui long sur un transfert pour changer le statut",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        when {
            ui.loading ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            ui.error != null && ui.transferts.isEmpty() ->
                Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                    Text(ui.error!!, color = MaterialTheme.colorScheme.error)
                }
            ui.transferts.isEmpty() ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Aucun transfert en cours.") }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ui.transferts, key = { it.id }) { t -> TransfertCard(t, viewModel) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransfertCard(t: TransfertDto, viewModel: TransfertsViewModel) {
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
            STATUTS.filter { it != t.statut }.forEach { st ->
                DropdownMenuItem(
                    text = { Text("Passer à $st") },
                    onClick = { menu = false; viewModel.changeStatut(t.id, st) },
                )
            }
        }
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    listOfNotNull(t.etablissementOrigine, t.uniteOrigine).joinToString(" / ").ifBlank { "?" },
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text("  →  ", style = MaterialTheme.typography.bodyMedium)
                Text(
                    listOfNotNull(t.etablissementDestination, t.uniteDestination).joinToString(" / ").ifBlank { "?" },
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.size(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatutPill(t.statut)
                Spacer(Modifier.weight(1f))
                if (!t.horodatageDepart.isNullOrBlank()) {
                    Text("Départ : ${t.horodatageDepart}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun StatutPill(statut: String?) {
    val color = when (statut) {
        "EN_COURS" -> VertOk
        "EN_PREPARATION" -> OrangeWarning
        "ARRIVE" -> Color(0xFF3B82F6)
        else -> Color(0xFF94A3B8)
    }
    Surface(color = color, shape = RoundedCornerShape(6.dp)) {
        Text(statut ?: "—", color = Color.White, style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}
