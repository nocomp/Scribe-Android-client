package com.scribe.app.ui.chat

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.scribe.app.data.local.SecureStore
import com.scribe.app.data.remote.dto.ChatMessageDto
import com.scribe.app.data.remote.dto.PresenceUserDto
import com.scribe.app.data.remote.dto.SalonDto
import com.scribe.app.data.repository.ContentRepository
import com.scribe.app.ui.theme.VertOk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val loadingSalons: Boolean = true,
    val salons: List<SalonDto> = emptyList(),
    val online: List<PresenceUserDto> = emptyList(),
    val selected: SalonDto? = null,
    val loadingMessages: Boolean = false,
    val messages: List<ChatMessageDto> = emptyList(),
    val presence: List<PresenceUserDto> = emptyList(),
    val input: String = "",
    val sending: Boolean = false,
    val busy: Boolean = false,
    val banner: String? = null,
    val error: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repo: ContentRepository,
    private val store: SecureStore,
) : ViewModel() {
    private val _ui = MutableStateFlow(ChatUiState())
    val ui: StateFlow<ChatUiState> = _ui.asStateFlow()
    private var myName: String = ""

    init {
        viewModelScope.launch { myName = store.snapshot().let { it.displayName ?: it.username ?: "" } }
        loadSalons()
    }

    fun loadSalons() {
        _ui.update { it.copy(loadingSalons = true, error = null) }
        viewModelScope.launch {
            repo.ping()
            repo.salons()
                .onSuccess { s -> _ui.update { it.copy(loadingSalons = false, salons = s) } }
                .onFailure { e -> _ui.update { it.copy(loadingSalons = false, error = e.message ?: "Erreur") } }
            repo.presence().onSuccess { map ->
                val all = map.values.flatten().filter { (it.displayName ?: "") != myName }
                _ui.update { it.copy(online = all) }
            }
        }
    }

    fun openSalon(salon: SalonDto) {
        _ui.update { it.copy(selected = salon, loadingMessages = true, messages = emptyList(), input = "") }
        viewModelScope.launch { repo.ping() }
        refreshMessages(salon.id)
        refreshPresence()
    }

    fun refreshMessages(salonId: Int? = _ui.value.selected?.id) {
        val id = salonId ?: return
        viewModelScope.launch {
            repo.salonMessages(id)
                .onSuccess { m -> _ui.update { it.copy(loadingMessages = false, messages = m) } }
                .onFailure { e -> _ui.update { it.copy(loadingMessages = false, error = e.message ?: "Erreur") } }
        }
    }

    private fun refreshPresence() {
        viewModelScope.launch {
            repo.presence().onSuccess { map ->
                _ui.update { it.copy(presence = map.values.flatten()) }
            }
        }
    }

    fun poll() {
        val id = _ui.value.selected?.id ?: return
        viewModelScope.launch {
            repo.ping()
            repo.salonMessages(id).onSuccess { m -> _ui.update { it.copy(messages = m) } }
            repo.presence().onSuccess { map -> _ui.update { it.copy(presence = map.values.flatten()) } }
        }
    }

    /** Ouvre (ou crée) un salon dédié à 2 personnes. */
    fun openPrivate(contact: PresenceUserDto) {
        if (_ui.value.busy) return
        val other = contact.displayName ?: "?"
        val parts = listOf(myName.ifBlank { "moi" }, other).sorted()
        val nom = "🔒 ${parts[0]} ↔ ${parts[1]}"
        // Le serveur normalise : minuscules + espaces -> tirets.
        val expected = nom.trim().lowercase().replace(" ", "-")
        val existing = _ui.value.salons.firstOrNull { it.nom == expected }
        if (existing != null) { openSalon(existing); return }
        _ui.update { it.copy(busy = true) }
        viewModelScope.launch {
            repo.createSalon(nom)
                .onSuccess {
                    repo.salons().onSuccess { s ->
                        _ui.update { it.copy(busy = false, salons = s) }
                        s.firstOrNull { it.nom == expected }?.let { openSalon(it) }
                            ?: _ui.update { it.copy(banner = "Salon privé créé") }
                    }
                }
                .onFailure { e -> _ui.update { it.copy(busy = false, banner = "Échec création salon (${e.message})") } }
        }
    }

    fun pollSalons() {
        viewModelScope.launch {
            repo.salons().onSuccess { list -> _ui.update { st -> st.copy(salons = list) } }
            repo.presence().onSuccess { map ->
                val all = map.values.flatten().filter { (it.displayName ?: "") != myName }
                _ui.update { st -> st.copy(online = all) }
            }
        }
    }

    fun setInput(v: String) = _ui.update { it.copy(input = v) }
    fun back() = _ui.update { it.copy(selected = null, messages = emptyList(), presence = emptyList()) }

    fun send() {
        val s = _ui.value
        val salon = s.selected ?: return
        val text = s.input.trim()
        if (s.sending || text.isEmpty()) return
        _ui.update { it.copy(sending = true) }
        viewModelScope.launch {
            repo.postChatMessage(salon.id, text)
                .onSuccess {
                    _ui.update { it.copy(sending = false, input = "") }
                    refreshMessages(salon.id)
                    refreshPresence()
                }
                .onFailure { e -> _ui.update { it.copy(sending = false, error = "Échec envoi (${e.message})") } }
        }
    }
}

@Composable
fun ChatSection(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    BackHandler(enabled = ui.selected != null) { viewModel.back() }
    if (ui.selected == null) SalonList(ui, viewModel, modifier)
    else SalonView(ui, viewModel, modifier)
}

@Composable
private fun SalonList(ui: ChatUiState, viewModel: ChatViewModel, modifier: Modifier) {
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000)
            viewModel.pollSalons()
        }
    }
    LazyColumn(
        modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ui.banner?.let {
            item(key = "banner") {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            }
        }
        item(key = "online_title") {
            Text(
                "En ligne (${ui.online.size}) — touchez pour un salon privé",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (ui.online.isEmpty()) {
            item(key = "online_empty") {
                Text("Personne d'autre en ligne.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            items(ui.online, key = { "u_${it.userId}_${it.displayName}" }) { u ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.openPrivate(u) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(VertOk))
                        Spacer(Modifier.size(8.dp))
                        Text(u.displayName ?: "—", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item(key = "salons_title") {
            Text("Salons", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
        }
        if (ui.loadingSalons && ui.salons.isEmpty()) {
            item(key = "salons_loading") {
                Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator() }
            }
        } else {
            items(ui.salons, key = { it.id }) { salon ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.openSalon(salon) },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("${salon.icone ?: "💬"}  ${salon.nom ?: "salon"}",
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (!salon.description.isNullOrBlank()) {
                            Spacer(Modifier.size(4.dp))
                            Text(salon.description!!, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SalonView(ui: ChatUiState, viewModel: ChatViewModel, modifier: Modifier) {
    LaunchedEffect(ui.selected?.id) {
        while (true) {
            delay(5000)
            viewModel.poll()
        }
    }
    Column(modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            TextButton(onClick = { viewModel.back() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                Spacer(Modifier.size(4.dp))
                Text("Salons")
            }
            Spacer(Modifier.size(4.dp))
            Text(ui.selected?.nom ?: "", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        val names = ui.presence.mapNotNull { it.displayName }.distinct()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(VertOk))
            Spacer(Modifier.size(6.dp))
            Text(
                if (names.isEmpty()) "Personne en ligne"
                else "${names.size} en ligne — ${names.joinToString(", ")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                ui.loadingMessages ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                ui.messages.isEmpty() ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Aucun message.") }
                else -> LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(ui.messages, key = { it.id }) { msg -> MessageBubble(msg) }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(8.dp),
        ) {
            OutlinedTextField(
                value = ui.input,
                onValueChange = { viewModel.setInput(it) },
                placeholder = { Text("Votre message…") },
                modifier = Modifier.weight(1f),
                maxLines = 4,
            )
            IconButton(onClick = { viewModel.send() }, enabled = !ui.sending && ui.input.isNotBlank()) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer",
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessageDto) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(10.dp)) {
            Row {
                Text(
                    listOfNotNull(msg.auteurNom, msg.auteurSigle).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                if (!msg.horodatage.isNullOrBlank()) {
                    Text(msg.horodatage!!, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            Spacer(Modifier.size(2.dp))
            Text(msg.contenu ?: "", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
