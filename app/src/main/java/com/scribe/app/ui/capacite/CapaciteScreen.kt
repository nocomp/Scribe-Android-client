package com.scribe.app.ui.capacite

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.scribe.app.data.local.SecureStore
import com.scribe.app.data.remote.dto.DeclarationCreateRequest
import com.scribe.app.data.remote.dto.ReferentielDto
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

data class DeclForm(
    val point: String = "matin",
    val litsH: String = "0",
    val litsF: String = "0",
    val litsI: String = "0",
    val statutLits: String = "normal",
    val statutRh: String = "complet",
    val statutMateriel: String = "ok",
    val alerteLits: Boolean = false,
    val alerteRh: Boolean = false,
    val alerteMateriel: Boolean = false,
    val commentaire: String = "",
)

data class CapaciteUiState(
    val loading: Boolean = true,
    val refs: List<ReferentielDto> = emptyList(),
    val selected: ReferentielDto? = null,
    val form: DeclForm = DeclForm(),
    val submitting: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class CapaciteViewModel @Inject constructor(
    private val repo: ContentRepository,
    private val store: SecureStore,
) : ViewModel() {

    private val _ui = MutableStateFlow(CapaciteUiState())
    val ui: StateFlow<CapaciteUiState> = _ui.asStateFlow()

    val points = listOf("matin", "aprem", "soir")
    val statutsLits = listOf("normal", "tension", "critique", "ferme")
    val statutsRh = listOf("complet", "tension", "critique")
    val statutsMateriel = listOf("ok", "degrade", "hs")

    private var redacteur: String = ""

    init { load() }

    fun load() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            redacteur = store.snapshot().let { it.displayName ?: it.username ?: "" }
            repo.referentiel()
                .onSuccess { r -> _ui.update { it.copy(loading = false, refs = r) } }
                .onFailure { e -> _ui.update { it.copy(loading = false, error = e.message ?: "Erreur") } }
        }
    }

    fun open(ref: ReferentielDto) {
        val d = ref.derniereDeclaration
        _ui.update {
            it.copy(
                selected = ref,
                form = DeclForm(
                    point = d?.point ?: "matin",
                    litsH = (d?.litsVidesH ?: 0).toString(),
                    litsF = (d?.litsVidesF ?: 0).toString(),
                    litsI = (d?.litsVidesI ?: 0).toString(),
                    statutLits = d?.statutLits ?: "normal",
                    statutRh = d?.statutRh ?: "complet",
                    statutMateriel = d?.statutMateriel ?: "ok",
                    commentaire = d?.commentaireGeneral ?: "",
                ),
            )
        }
    }

    fun back() = _ui.update { it.copy(selected = null) }
    fun clearMessage() = _ui.update { it.copy(message = null) }

    fun update(transform: (DeclForm) -> DeclForm) =
        _ui.update { it.copy(form = transform(it.form)) }

    fun submit() {
        val s = _ui.value
        val ref = s.selected ?: return
        if (s.submitting) return
        _ui.update { it.copy(submitting = true) }
        viewModelScope.launch {
            val f = s.form
            val req = DeclarationCreateRequest(
                referentielId = ref.id,
                redacteur = redacteur.ifBlank { "Mobile" },
                point = f.point,
                litsVidesH = f.litsH.toIntOrNull() ?: 0,
                litsVidesF = f.litsF.toIntOrNull() ?: 0,
                litsVidesI = f.litsI.toIntOrNull() ?: 0,
                statutLits = f.statutLits,
                statutRh = f.statutRh,
                statutMateriel = f.statutMateriel,
                alerteLits = f.alerteLits,
                alerteRh = f.alerteRh,
                alerteMateriel = f.alerteMateriel,
                commentaireGeneral = f.commentaire.ifBlank { null },
            )
            repo.declare(req)
                .onSuccess {
                    _ui.update { it.copy(submitting = false, selected = null, message = "Déclaration enregistrée") }
                    load()
                }
                .onFailure { e ->
                    _ui.update { it.copy(submitting = false, message = "Échec : ${e.message}") }
                }
        }
    }
}

@Composable
fun CapaciteSection(
    modifier: Modifier = Modifier,
    viewModel: CapaciteViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val sel = ui.selected
    BackHandler(enabled = sel != null) { viewModel.back() }

    if (sel == null) {
        when {
            ui.loading ->
                Box(modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            ui.error != null && ui.refs.isEmpty() ->
                Box(modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                    Text(ui.error!!, color = MaterialTheme.colorScheme.error)
                }
            ui.refs.isEmpty() ->
                Box(modifier.fillMaxSize(), Alignment.Center) { Text("Aucune unité de capacité.") }
            else -> {
                Column(modifier.fillMaxSize()) {
                    ui.message?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    }
                    LazyColumn(
                        Modifier.fillMaxSize().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(ui.refs, key = { it.id }) { ref ->
                            RefCard(ref, onClick = { viewModel.open(ref) })
                        }
                    }
                }
            }
        }
    } else {
        DeclarationForm(
            ref = sel,
            ui = ui,
            viewModel = viewModel,
            modifier = modifier,
        )
    }
}

@Composable
private fun RefCard(ref: ReferentielDto, onClick: () -> Unit) {
    val d = ref.derniereDeclaration
    val dispo = if (d != null) d.litsVidesH + d.litsVidesF + d.litsVidesI else null
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(statutColor(ref.statutGlobal)))
                Spacer(Modifier.size(8.dp))
                Text(ref.serviceNom ?: "—", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    if (dispo != null) "$dispo / ${ref.capaciteTotale} lits" else "non déclaré",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.size(4.dp))
            Text(
                listOfNotNull(ref.pole, ref.site).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun DeclarationForm(
    ref: ReferentielDto,
    ui: CapaciteUiState,
    viewModel: CapaciteViewModel,
    modifier: Modifier = Modifier,
) {
    val f = ui.form
    Column(modifier.fillMaxSize()) {
        if (ui.submitting) LinearProgressIndicator(Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            TextButton(onClick = { viewModel.back() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                Spacer(Modifier.size(4.dp))
                Text("Unités")
            }
            Spacer(Modifier.size(4.dp))
            Text(ref.serviceNom ?: "", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Label("Point de situation")
            ChipRow(viewModel.points, f.point) { v -> viewModel.update { it.copy(point = v) } }

            Spacer(Modifier.height(14.dp))
            Label("Lits disponibles")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("H", f.litsH, Modifier.weight(1f)) { v -> viewModel.update { it.copy(litsH = v) } }
                NumField("F", f.litsF, Modifier.weight(1f)) { v -> viewModel.update { it.copy(litsF = v) } }
                NumField("Indif.", f.litsI, Modifier.weight(1f)) { v -> viewModel.update { it.copy(litsI = v) } }
            }

            Spacer(Modifier.height(14.dp))
            Label("Statut lits")
            ChipRow(viewModel.statutsLits, f.statutLits) { v -> viewModel.update { it.copy(statutLits = v) } }
            Label("Statut RH")
            ChipRow(viewModel.statutsRh, f.statutRh) { v -> viewModel.update { it.copy(statutRh = v) } }
            Label("Statut matériel")
            ChipRow(viewModel.statutsMateriel, f.statutMateriel) { v -> viewModel.update { it.copy(statutMateriel = v) } }

            Spacer(Modifier.height(8.dp))
            SwitchRow("Alerte lits", f.alerteLits) { v -> viewModel.update { it.copy(alerteLits = v) } }
            SwitchRow("Alerte RH", f.alerteRh) { v -> viewModel.update { it.copy(alerteRh = v) } }
            SwitchRow("Alerte matériel", f.alerteMateriel) { v -> viewModel.update { it.copy(alerteMateriel = v) } }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = f.commentaire,
                onValueChange = { v -> viewModel.update { it.copy(commentaire = v) } },
                label = { Text("Commentaire") },
                modifier = Modifier.fillMaxWidth().height(90.dp),
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "Cocher une alerte créera automatiquement un incident, comme sur le site.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.submit() },
                enabled = !ui.submitting,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Enregistrer la déclaration") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
}

@Composable
private fun ChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row {
        options.forEach { o ->
            FilterChip(
                selected = selected == o,
                onClick = { onSelect(o) },
                label = { Text(o) },
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}

@Composable
private fun NumField(label: String, value: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(4)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Switch(checked = checked, onCheckedChange = onChange)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun statutColor(s: String?): Color = when (s) {
    "ferme", "critique", "hs", "insuffisant" -> RougeMarianne
    "tension", "degrade" -> OrangeWarning
    "normal", "complet", "ok" -> VertOk
    else -> Color(0xFF94A3B8)
}
