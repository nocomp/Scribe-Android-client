package com.scribe.app.ui.incidents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateIncidentScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: CreateIncidentViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    LaunchedEffect(ui.done) { if (ui.done) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Déclarer un incident") },
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
        Column(
            Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            OutlinedTextField(
                value = ui.declarantNom, onValueChange = viewModel::onDeclarant,
                label = { Text("Déclarant *") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = ui.siteId, onValueChange = viewModel::onSite,
                label = { Text("Site *") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = ui.uniteFonctionnelle, onValueChange = viewModel::onUf,
                label = { Text("Unité fonctionnelle") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Text("Type de crise", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.padding(top = 6.dp)) {
                viewModel.typesCrise.forEach { t ->
                    FilterChip(
                        selected = ui.typeCrise == t,
                        onClick = { viewModel.onType(t) },
                        label = { Text(t) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Niveau d'urgence", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.padding(top = 6.dp)) {
                viewModel.niveauxUrgence.forEach { n ->
                    FilterChip(
                        selected = ui.urgency == n,
                        onClick = { viewModel.onUrgency(n) },
                        label = { Text(n.toString()) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = ui.fait, onValueChange = viewModel::onFait,
                label = { Text("Fait / description *") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = ui.analyse, onValueChange = viewModel::onAnalyse,
                label = { Text("Analyse (optionnel)") },
                modifier = Modifier.fillMaxWidth().height(90.dp),
            )

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = ui.impactFonctionnel, onCheckedChange = viewModel::onImpact)
                Text("  Impact fonctionnel (panne, équipement HS…)")
            }

            ui.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::submit,
                enabled = ui.canSubmit && !ui.submitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (ui.submitting) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else Text("Déclarer l'incident")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
