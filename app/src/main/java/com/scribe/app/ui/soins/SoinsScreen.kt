package com.scribe.app.ui.soins

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.scribe.app.data.remote.dto.PoleSyntheseDto
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

data class SiteCapacite(val site: String, val poles: List<PoleEntry>)
data class PoleEntry(val pole: String, val data: PoleSyntheseDto)

data class SoinsUiState(
    val loading: Boolean = true,
    val sites: List<SiteCapacite> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class SoinsViewModel @Inject constructor(
    private val repo: ContentRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(SoinsUiState())
    val ui: StateFlow<SoinsUiState> = _ui.asStateFlow()

    init { load() }

    fun load() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.capacite()
                .onSuccess { map ->
                    val sites = map.map { (site, poles) ->
                        val entries = poles.map { (p, d) -> PoleEntry(p, d) }
                            .sortedByDescending { critWeight(it.data.statutPole) }
                        SiteCapacite(site, entries)
                    }.sortedByDescending { s ->
                        s.poles.maxOfOrNull { critWeight(it.data.statutPole) } ?: 0
                    }
                    _ui.update { it.copy(loading = false, sites = sites) }
                }
                .onFailure { e -> _ui.update { it.copy(loading = false, error = e.message ?: "Erreur") } }
        }
    }
}

@Composable
fun SoinsSection(
    modifier: Modifier = Modifier,
    viewModel: SoinsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    when {
        ui.loading ->
            Box(modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        ui.error != null && ui.sites.isEmpty() ->
            Box(modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                Text(ui.error!!, color = MaterialTheme.colorScheme.error)
            }
        ui.sites.isEmpty() ->
            Box(modifier.fillMaxSize(), Alignment.Center) { Text("Aucune donnée de capacité.") }
        else -> LazyColumn(
            modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ui.sites.forEach { site ->
                item(key = "site_${site.site}") {
                    Text(
                        site.site,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                    )
                }
                items(site.poles.size) { idx ->
                    PoleCard(site.poles[idx])
                }
            }
        }
    }
}

@Composable
private fun PoleCard(entry: PoleEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(statutColor(entry.data.statutPole)))
                Spacer(Modifier.size(8.dp))
                Text(entry.pole, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(statutLabel(entry.data.statutPole), style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.size(6.dp))
            val vides = entry.data.litsVidesH + entry.data.litsVidesF + entry.data.litsVidesI
            Text(
                "Lits : ${entry.data.litsTotal} total · $vides disponibles" +
                    if (entry.data.alertes > 0) "  ·  ${entry.data.alertes} alerte(s)" else "",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun statutColor(s: String?): Color = when (s) {
    "ferme" -> RougeMarianne
    "critique" -> RougeMarianne
    "tension" -> OrangeWarning
    "normal" -> VertOk
    else -> Color(0xFF94A3B8)
}

private fun statutLabel(s: String?): String = when (s) {
    "ferme" -> "Fermé"
    "critique" -> "Critique"
    "tension" -> "Tension"
    "normal" -> "Normal"
    else -> "Non déclaré"
}

private fun critWeight(s: String?): Int = when (s) {
    "ferme" -> 4
    "critique", "hs", "insuffisant" -> 3
    "tension", "degrade" -> 2
    "normal", "complet", "ok" -> 1
    else -> 0
}
