package com.scribe.app.ui.incidents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.FilterChip
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scribe.app.data.remote.dto.IncidentDto
import com.scribe.app.ui.theme.OrangeWarning
import com.scribe.app.ui.theme.RougeMarianne
import com.scribe.app.ui.theme.VertOk

@Composable
fun IncidentsSection(
    modifier: Modifier = Modifier,
    onOpenDetail: (Int) -> Unit,
    viewModel: IncidentsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            FilterChip(
                selected = ui.activeOnly,
                onClick = { viewModel.toggleActiveOnly(true) },
                label = { Text("Actifs") },
            )
            Spacer(Modifier.size(8.dp))
            FilterChip(
                selected = !ui.activeOnly,
                onClick = { viewModel.toggleActiveOnly(false) },
                label = { Text("Tous") },
            )
        }
        when {
            ui.loading && ui.incidents.isEmpty() ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            ui.error != null && ui.incidents.isEmpty() ->
                Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                    Text(ui.error!!, color = MaterialTheme.colorScheme.error)
                }
            ui.incidents.isEmpty() ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Aucun incident.") }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ui.incidents, key = { it.id }) { inc ->
                    IncidentCard(inc, onClick = { onOpenDetail(inc.id) })
                }
            }
        }
    }
}

@Composable
private fun IncidentCard(incident: IncidentDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(urgencyColor(incident.urgency)))
                Spacer(Modifier.size(8.dp))
                Text(
                    incident.typeCrise ?: "—",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.weight(1f))
                Text(incident.status ?: "", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.size(6.dp))
            Text(incident.fait ?: "(sans description)", style = MaterialTheme.typography.bodyMedium, maxLines = 3)
            Spacer(Modifier.size(6.dp))
            Text(
                listOfNotNull(incident.siteId, incident.uniteFonctionnelle?.ifBlank { null }).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

private fun urgencyColor(u: Int): Color = when {
    u >= 4 -> RougeMarianne
    u == 3 -> OrangeWarning
    else -> VertOk
}
