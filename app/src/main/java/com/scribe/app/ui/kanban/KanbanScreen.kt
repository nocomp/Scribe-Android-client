package com.scribe.app.ui.kanban

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.scribe.app.data.remote.dto.TaskDto
import com.scribe.app.data.repository.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

val COLONNES = listOf("BACKLOG", "EN_COURS", "EN_ATTENTE", "TERMINÉ")

data class KanbanUiState(
    val loading: Boolean = true,
    val tasks: List<TaskDto> = emptyList(),
    val busy: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class KanbanViewModel @Inject constructor(
    private val repo: ContentRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(KanbanUiState())
    val ui: StateFlow<KanbanUiState> = _ui.asStateFlow()

    init { load() }

    fun load() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.tasks()
                .onSuccess { t -> _ui.update { it.copy(loading = false, tasks = t) } }
                .onFailure { e -> _ui.update { it.copy(loading = false, error = e.message ?: "Erreur") } }
        }
    }

    fun move(task: TaskDto, toColonne: String) {
        if (_ui.value.busy) return
        _ui.update { it.copy(busy = true) }
        viewModelScope.launch {
            repo.moveTask(task.id, toColonne)
                .onSuccess { _ui.update { it.copy(busy = false) }; load() }
                .onFailure { e -> _ui.update { it.copy(busy = false, error = e.message) }; load() }
        }
    }
}

@Composable
fun KanbanSection(
    modifier: Modifier = Modifier,
    viewModel: KanbanViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    when {
        ui.loading ->
            Box(modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        ui.error != null && ui.tasks.isEmpty() ->
            Box(modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                Text(ui.error!!, color = MaterialTheme.colorScheme.error)
            }
        else -> LazyColumn(
            modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            COLONNES.forEach { col ->
                val items = ui.tasks.filter { it.colonne == col }
                item(key = "col_$col") {
                    Text(
                        "$col  (${items.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp, start = 4.dp),
                    )
                }
                if (items.isEmpty()) {
                    item(key = "empty_$col") {
                        Text("—", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.padding(start = 8.dp))
                    }
                } else {
                    items.forEach { task ->
                        item(key = "task_${task.id}") {
                            TaskCard(task, viewModel)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskCard(task: TaskDto, viewModel: KanbanViewModel) {
    val idx = COLONNES.indexOf(task.colonne)
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
            COLONNES.filter { it != task.colonne }.forEach { col ->
                DropdownMenuItem(
                    text = { Text("Déplacer vers $col") },
                    onClick = { menu = false; viewModel.move(task, col) },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            IconButton(
                onClick = { if (idx > 0) viewModel.move(task, COLONNES[idx - 1]) },
                enabled = idx > 0,
            ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Précédent") }

            Column(Modifier.weight(1f)) {
                Text(task.titre ?: "(sans titre)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (!task.assignee.isNullOrBlank() || !task.description.isNullOrBlank()) {
                    Spacer(Modifier.size(2.dp))
                    Text(
                        listOfNotNull(task.assignee?.ifBlank { null }, task.description?.ifBlank { null }).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2,
                    )
                }
            }

            IconButton(
                onClick = { if (idx < COLONNES.size - 1) viewModel.move(task, COLONNES[idx + 1]) },
                enabled = idx in 0 until (COLONNES.size - 1),
            ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Suivant") }
        }
    }
}
