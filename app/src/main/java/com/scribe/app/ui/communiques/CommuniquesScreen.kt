package com.scribe.app.ui.communiques

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.scribe.app.data.remote.dto.CommuniqueDto
import com.scribe.app.data.remote.dto.ServiceStatutDto
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

data class CommuniquesUiState(
    val loading: Boolean = true,
    val data: CommuniqueDto? = null,
    val error: String? = null,
)

@HiltViewModel
class CommuniquesViewModel @Inject constructor(
    private val repo: ContentRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(CommuniquesUiState())
    val ui: StateFlow<CommuniquesUiState> = _ui.asStateFlow()

    init { load() }

    fun load() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.communique()
                .onSuccess { d -> _ui.update { it.copy(loading = false, data = d) } }
                .onFailure { e -> _ui.update { it.copy(loading = false, error = e.message ?: "Erreur") } }
        }
    }
}

@Composable
fun CommuniquesSection(
    modifier: Modifier = Modifier,
    viewModel: CommuniquesViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    when {
        ui.loading ->
            Box(modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        ui.data == null ->
            Box(modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                Text(ui.error ?: "Aucun communiqué.", color = MaterialTheme.colorScheme.error)
            }
        else -> {
            val d = ui.data!!
            Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                if (!d.siteNom.isNullOrBlank()) {
                    Text(d.siteNom!!, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                }
                NiveauBadge(d.niveauGlobal)
                Spacer(Modifier.height(12.dp))

                if (!d.messagePublic.isNullOrBlank()) {
                    Text(d.messagePublic!!, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(6.dp))
                }
                if (!d.updatedBy.isNullOrBlank() || !d.updatedAt.isNullOrBlank()) {
                    Text(
                        "Mis à jour" + (d.updatedBy?.takeIf { it.isNotBlank() }?.let { " par $it" } ?: "") +
                            (d.updatedAt?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }

                StatutGroup("Systèmes d'information", d.servicesSi)
                StatutGroup("Prise en charge des patients", d.priseEnCharge)

                val faqItems = d.faq.filter { !it.question.isNullOrBlank() }
                if (faqItems.isNotEmpty()) {
                    SectionTitle("Questions fréquentes")
                    faqItems.forEach { f ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(f.question ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                if (!f.reponse.isNullOrBlank()) {
                                    Text(f.reponse!!, style = MaterialTheme.typography.bodyMedium)
                                } else {
                                    Text("Réponse non renseignée", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }

                if (d.chronologie.isNotEmpty()) {
                    SectionTitle("Chronologie")
                    d.chronologie.forEach { c ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(c.ts ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(2.dp))
                                Text(c.texte ?: "", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(18.dp))
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun StatutGroup(title: String, items: List<ServiceStatutDto>) {
    if (items.isEmpty()) return
    SectionTitle(title)
    items.forEach { s ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        ) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(statutColor(s.statut)))
            Spacer(Modifier.size(10.dp))
            Text(s.label ?: s.id ?: "—", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(statutLabel(s.statut), style = MaterialTheme.typography.labelMedium, color = statutColor(s.statut))
        }
    }
}

@Composable
private fun NiveauBadge(niveau: String?) {
    val (label, color) = when (niveau) {
        "OPERATIONNEL" -> "Opérationnel" to VertOk
        "PERTURBE" -> "Perturbé" to OrangeWarning
        "INCIDENT_MAJEUR" -> "Incident majeur" to RougeMarianne
        "MAINTENANCE" -> "Maintenance" to MaterialTheme.colorScheme.secondary
        else -> (niveau ?: "—") to MaterialTheme.colorScheme.primary
    }
    Surface(color = color, shape = RoundedCornerShape(8.dp)) {
        Text(
            label, color = Color.White, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

private fun statutColor(s: String?): Color {
    val u = s?.uppercase() ?: ""
    return when {
        u == "OK" || u == "OPERATIONNEL" || u == "NORMAL" -> VertOk
        u.contains("DEGRAD") || u == "TENSION" -> OrangeWarning
        u.isBlank() -> Color(0xFF94A3B8)
        else -> RougeMarianne
    }
}

private fun statutLabel(s: String?): String = when (s?.uppercase()) {
    "OK" -> "OK"
    "DEGRADE", "DÉGRADÉ" -> "Dégradé"
    "HS", "PANNE", "KO" -> "En panne"
    null, "" -> "—"
    else -> s!!
}
