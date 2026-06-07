package com.scribe.app.ui.messages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.scribe.app.data.remote.dto.ContactDto
import com.scribe.app.data.remote.dto.MessageDto
import com.scribe.app.data.remote.dto.MessageSendRequest
import com.scribe.app.data.repository.ContentRepository
import com.scribe.app.ui.theme.VertOk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ComposeForm(
    val query: String = "",
    val selected: ContactDto? = null,
    val sujet: String = "",
    val contenu: String = "",
    val replyTo: Int? = null,
)

data class MessagesUiState(
    val loading: Boolean = true,
    val messages: List<MessageDto> = emptyList(),
    val error: String? = null,
    val detail: MessageDto? = null,
    val composing: Boolean = false,
    val contacts: List<ContactDto> = emptyList(),
    val loadingContacts: Boolean = false,
    val compose: ComposeForm = ComposeForm(),
    val sending: Boolean = false,
    val banner: String? = null,
)

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val repo: ContentRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(MessagesUiState())
    val ui: StateFlow<MessagesUiState> = _ui.asStateFlow()

    init { load() }

    fun load() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.messages()
                .onSuccess { m -> _ui.update { it.copy(loading = false, messages = m) } }
                .onFailure { e -> _ui.update { it.copy(loading = false, error = e.message ?: "Erreur") } }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            _ui.value.messages.filter { !it.lu && !it.isMine }.forEach { repo.markRead(it.id) }
            load()
        }
    }

    // ----- Détail -----
    fun openMessage(m: MessageDto) {
        _ui.update { it.copy(detail = m) }
        if (!m.lu && !m.isMine) {
            viewModelScope.launch {
                repo.markRead(m.id)
                load()
            }
        }
    }

    fun closeMessage() = _ui.update { it.copy(detail = null) }

    // ----- Composer / répondre / transférer -----
    private fun ensureContacts() {
        if (_ui.value.contacts.isEmpty() && !_ui.value.loadingContacts) {
            _ui.update { it.copy(loadingContacts = true) }
            viewModelScope.launch {
                repo.annuaire()
                    .onSuccess { c -> _ui.update { it.copy(loadingContacts = false, contacts = c) } }
                    .onFailure { e -> _ui.update { it.copy(loadingContacts = false, banner = "Carnet indisponible (${e.message})") } }
            }
        }
    }

    fun startCompose() {
        _ui.update { it.copy(composing = true, detail = null, compose = ComposeForm()) }
        ensureContacts()
    }

    fun reply(m: MessageDto) {
        val dest = m.expediteurId?.let { ContactDto(id = it, displayName = m.expediteurNom) }
        _ui.update {
            it.copy(
                composing = true, detail = null,
                compose = ComposeForm(
                    selected = dest,
                    sujet = "Re: ${m.sujet ?: ""}",
                    replyTo = m.id,
                ),
            )
        }
        if (dest == null) ensureContacts()
    }

    fun forward(m: MessageDto) {
        _ui.update {
            it.copy(
                composing = true, detail = null,
                compose = ComposeForm(
                    selected = null,
                    sujet = "Tr: ${m.sujet ?: ""}",
                    contenu = "--- Message de ${m.expediteurNom ?: "?"} ---\n${m.contenu ?: ""}\n--- fin ---\n",
                ),
            )
        }
        ensureContacts()
    }

    fun cancelCompose() = _ui.update { it.copy(composing = false) }
    fun clearBanner() = _ui.update { it.copy(banner = null) }

    fun setQuery(v: String) = _ui.update { it.copy(compose = it.compose.copy(query = v)) }
    fun selectContact(c: ContactDto?) = _ui.update { it.copy(compose = it.compose.copy(selected = c)) }
    fun setSujet(v: String) = _ui.update { it.copy(compose = it.compose.copy(sujet = v)) }
    fun setContenu(v: String) = _ui.update { it.copy(compose = it.compose.copy(contenu = v)) }

    fun send() {
        val s = _ui.value
        val dest = s.compose.selected ?: return
        if (s.sending || s.compose.contenu.isBlank()) return
        _ui.update { it.copy(sending = true) }
        viewModelScope.launch {
            val req = MessageSendRequest(
                destinataireId = dest.id,
                sujet = s.compose.sujet.ifBlank { "(sans sujet)" },
                contenu = s.compose.contenu.trim(),
                replyTo = s.compose.replyTo,
            )
            repo.sendMessage(req)
                .onSuccess {
                    _ui.update { it.copy(sending = false, composing = false, banner = "Message envoyé") }
                    load()
                }
                .onFailure { e -> _ui.update { it.copy(sending = false, banner = "Échec : ${e.message}") } }
        }
    }
}

@Composable
fun MessagesSection(
    modifier: Modifier = Modifier,
    viewModel: MessagesViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    BackHandler(enabled = ui.composing) { viewModel.cancelCompose() }
    BackHandler(enabled = !ui.composing && ui.detail != null) { viewModel.closeMessage() }
    when {
        ui.composing -> ComposeView(ui, viewModel, modifier)
        ui.detail != null -> DetailView(ui.detail!!, viewModel, modifier)
        else -> InboxView(ui, viewModel, modifier)
    }
}

@Composable
private fun InboxView(ui: MessagesUiState, viewModel: MessagesViewModel, modifier: Modifier) {
    Column(modifier.fillMaxSize()) {
        ui.banner?.let {
            Text(it, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Button(
                onClick = { viewModel.startCompose() },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Nouveau message")
            }
            IconButton(onClick = { viewModel.markAllRead() }) {
                Icon(Icons.Filled.DoneAll, contentDescription = "Tout marquer comme lu",
                    tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { viewModel.load() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Rafraîchir",
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
        when {
            ui.loading ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            ui.error != null && ui.messages.isEmpty() ->
                Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                    Text(ui.error!!, color = MaterialTheme.colorScheme.error)
                }
            ui.messages.isEmpty() ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Aucun message.") }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ui.messages, key = { it.id }) { m ->
                    MessageCard(m) { viewModel.openMessage(m) }
                }
            }
        }
    }
}

@Composable
private fun MessageCard(m: MessageDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    m.sujet ?: "(sans sujet)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (m.lu) FontWeight.Normal else FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                if (!m.lu && !m.isMine) Text("●", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.size(4.dp))
            Text(m.contenu ?: "", style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Spacer(Modifier.size(6.dp))
            Text(
                (if (m.isMine) "À ${m.destinataireNom ?: "—"}" else "De ${m.expediteurNom ?: "—"}") +
                    (m.createdAt?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun DetailView(m: MessageDto, viewModel: MessagesViewModel, modifier: Modifier) {
    Column(modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            TextButton(onClick = { viewModel.closeMessage() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                Spacer(Modifier.size(4.dp))
                Text("Boîte")
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(m.sujet ?: "(sans sujet)", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(
                (if (m.isMine) "À : ${m.destinataireNom ?: "—"}" else "De : ${m.expediteurNom ?: "—"}") +
                    (m.createdAt?.let { "  ·  $it" } ?: ""),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(16.dp))
            Text(m.contenu ?: "", style = MaterialTheme.typography.bodyLarge)

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!m.isMine) {
                    Button(onClick = { viewModel.reply(m) }) {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Répondre")
                    }
                }
                OutlinedButton(onClick = { viewModel.forward(m) }) {
                    Icon(Icons.Filled.Forward, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Transférer")
                }
            }
        }
    }
}

@Composable
private fun ComposeView(ui: MessagesUiState, viewModel: MessagesViewModel, modifier: Modifier) {
    val f = ui.compose
    Column(modifier.fillMaxSize()) {
        if (ui.sending) LinearProgressIndicator(Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            TextButton(onClick = { viewModel.cancelCompose() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                Spacer(Modifier.size(4.dp))
                Text("Boîte")
            }
            Spacer(Modifier.size(4.dp))
            Text("Nouveau message", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        if (f.selected == null) {
            Text("Choisir un destinataire", style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
            OutlinedTextField(
                value = f.query,
                onValueChange = { viewModel.setQuery(it) },
                label = { Text("Rechercher un contact") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            if (ui.loadingContacts) {
                Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator() }
            } else {
                val q = f.query.trim().lowercase()
                val filtered = ui.contacts.filter {
                    q.isBlank() ||
                        (it.displayName ?: "").lowercase().contains(q) ||
                        (it.service ?: "").lowercase().contains(q) ||
                        (it.role ?: "").lowercase().contains(q)
                }
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(filtered, key = { it.id }) { c -> ContactRow(c) { viewModel.selectContact(c) } }
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(onlineColor(f.selected!!.online)))
                    Spacer(Modifier.size(8.dp))
                    Text("À : ${f.selected!!.displayName ?: "—"}", style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.selectContact(null) }) { Text("Changer") }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = f.sujet, onValueChange = { viewModel.setSujet(it) },
                    label = { Text("Sujet") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = f.contenu, onValueChange = { viewModel.setContenu(it) },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.send() },
                    enabled = !ui.sending && f.contenu.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Envoyer") }
            }
        }
    }
}

@Composable
private fun ContactRow(c: ContactDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(onlineColor(c.online)))
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(c.displayName ?: "—", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    listOfNotNull(c.role, c.inactivityLabel?.ifBlank { null }).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

private fun onlineColor(s: String?): Color = when (s) {
    "online" -> VertOk
    "today" -> Color(0xFF3B82F6)
    else -> Color(0xFF94A3B8)
}
