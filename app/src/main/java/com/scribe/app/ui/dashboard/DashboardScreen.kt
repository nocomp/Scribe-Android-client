package com.scribe.app.ui.dashboard

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.scribe.app.data.auth.Feature
import com.scribe.app.data.auth.Permissions
import com.scribe.app.data.auth.UserRole
import com.scribe.app.data.repository.ContentRepository
import com.scribe.app.ui.theme.OrangeWarning
import com.scribe.app.ui.theme.RougeMarianne
import com.scribe.app.ui.theme.VertOk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val loading: Boolean = true,
    val ouverts: Int = 0,
    val critiques: Int = 0,
    val cyber: Int = 0,
    val sanitaire: Int = 0,
    val tasksEnCours: Int = 0,
    val transferts: Int = 0,
    val unread: Int = 0,
    val secteursImpactes: Int = 0,
    val error: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: ContentRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(DashboardUiState())
    val ui: StateFlow<DashboardUiState> = _ui.asStateFlow()

    fun load() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.stats().onSuccess { s ->
                _ui.update { it.copy(ouverts = s.ouverts, critiques = s.critical, cyber = s.cyber, sanitaire = s.sanitaire) }
            }.onFailure { e -> _ui.update { it.copy(error = e.message) } }

            repo.tasks().onSuccess { t ->
                _ui.update { it.copy(tasksEnCours = t.count { task -> task.colonne != "TERMINÉ" }) }
            }
            repo.transferts().onSuccess { tr -> _ui.update { it.copy(transferts = tr.size) } }
            _ui.update { it.copy(unread = repo.unread()) }
            repo.capacite().onSuccess { map ->
                val impacted = map.values.sumOf { poles ->
                    poles.values.count { it.statutPole in setOf("tension", "critique", "ferme") }
                }
                _ui.update { it.copy(secteursImpactes = impacted) }
            }
            _ui.update { it.copy(loading = false) }
        }
    }
}

private data class DashTile(
    val label: String,
    val value: String,
    val accent: Color,
    val feature: Feature,
    val onClick: () -> Unit,
)

@Composable
fun DashboardSection(
    modifier: Modifier = Modifier,
    role: UserRole,
    onGoIncidents: () -> Unit,
    onGoKanban: () -> Unit,
    onGoTransferts: () -> Unit,
    onGoMessages: () -> Unit,
    onGoSoins: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Vue d'ensemble", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Touchez une tuile pour ouvrir la section",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(16.dp))

        if (ui.loading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() }
        } else {
            val tiles = listOf(
                DashTile("Incidents ouverts", ui.ouverts.toString(), MaterialTheme.colorScheme.primary, Feature.INCIDENTS, onGoIncidents),
                DashTile("Critiques", ui.critiques.toString(), RougeMarianne, Feature.INCIDENTS, onGoIncidents),
                DashTile("Tâches en cours", ui.tasksEnCours.toString(), MaterialTheme.colorScheme.secondary, Feature.KANBAN, onGoKanban),
                DashTile("Transferts en cours", ui.transferts.toString(), VertOk, Feature.TRANSFERTS, onGoTransferts),
                DashTile("Messages non lus", ui.unread.toString(), RougeMarianne, Feature.MESSAGES, onGoMessages),
                DashTile("Secteurs soins impactés", ui.secteursImpactes.toString(), OrangeWarning, Feature.SOINS, onGoSoins),
            ).filter { Permissions.allows(it.feature, role) }

            tiles.chunked(2).forEach { rowTiles ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowTiles.forEach { t ->
                        StatCard(t.label, t.value, t.accent, Modifier.weight(1f), t.onClick)
                    }
                    if (rowTiles.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = accent)
            Spacer(Modifier.size(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
