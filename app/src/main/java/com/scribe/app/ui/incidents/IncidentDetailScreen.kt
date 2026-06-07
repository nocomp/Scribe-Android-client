package com.scribe.app.ui.incidents

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scribe.app.data.remote.dto.IncidentDto
import com.scribe.app.ui.theme.OrangeWarning
import com.scribe.app.ui.theme.RougeMarianne
import com.scribe.app.ui.theme.VertOk

private val STATUTS = listOf("SIGNALÉ", "EN COURS", "RÉSOLU", "ARCHIVÉ")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailScreen(
    onBack: () -> Unit,
    viewModel: IncidentDetailViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Détail de l'incident") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        val incident = ui.incident
        when {
            ui.loading ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            incident == null ->
                Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                    Text(ui.error ?: "Incident introuvable.", color = MaterialTheme.colorScheme.error)
                }
            else -> Column(Modifier.padding(padding).fillMaxSize()) {
                if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                DetailContent(
                    inc = incident,
                    jalons = ui.jalons,
                    busy = ui.busy,
                    onToggleJalon = viewModel::toggleJalon,
                    onChangeStatus = viewModel::changeStatus,
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    inc: IncidentDto,
    jalons: List<JalonUi>,
    busy: Boolean,
    onToggleJalon: (Int) -> Unit,
    onChangeStatus: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(urgencyColor(inc.urgency)))
            Spacer(Modifier.size(10.dp))
            Text(
                "${inc.typeCrise ?: "—"}  •  urgence ${inc.urgency}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // ---- Statut (cliquable) ----
        Spacer(Modifier.height(14.dp))
        Text("STATUT", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            STATUTS.forEach { s ->
                FilterChip(
                    selected = inc.status == s,
                    onClick = { if (!busy && inc.status != s) onChangeStatus(s) },
                    label = { Text(s) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
        Text(
            "Avancement : ${inc.completionPercent} %",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 4.dp),
        )

        // ---- Jalons ----
        Spacer(Modifier.height(16.dp))
        Text("JALONS", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        if (jalons.isEmpty()) {
            Text("Aucun jalon défini.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp))
        } else {
            jalons.forEachIndexed { i, j ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = j.done,
                        onCheckedChange = { if (!busy) onToggleJalon(i) },
                        enabled = !busy,
                    )
                    Text(j.label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // ---- Champs ----
        Field("Fait", inc.fait)
        Field("Analyse", inc.analyse?.ifBlank { null })
        Field("Déclarant", inc.declarantNom)
        Field("Directeur de crise", inc.directeurCrise?.ifBlank { null })
        Field("Site", inc.siteId)
        Field("Unité fonctionnelle", inc.uniteFonctionnelle?.ifBlank { null })
        Field("Horodatage", inc.timestamp)
        Field("Impact fonctionnel", if (inc.impactFonctionnel) "Oui" else "Non")
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Field(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Spacer(Modifier.height(14.dp))
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun urgencyColor(u: Int): Color = when {
    u >= 4 -> RougeMarianne
    u == 3 -> OrangeWarning
    else -> VertOk
}
