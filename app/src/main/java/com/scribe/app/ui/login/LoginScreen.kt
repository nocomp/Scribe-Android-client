package com.scribe.app.ui.login

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scribe.app.R

@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val languages by viewModel.languages.collectAsStateWithLifecycle()
    val dict by viewModel.dict.collectAsStateWithLifecycle()
    fun t(key: String, fallback: String): String = dict[key] ?: fallback

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_scribe),
            contentDescription = "Logo SCRIBE",
            modifier = Modifier.size(width = 84.dp, height = 72.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text("SCRIBE", style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(
            t("app.tagline", "Gestion de crise hospitalière"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(24.dp))

        if (ui.step == LoginUiState.Step.CREDENTIALS) {
            // --- Sélecteur de langue ---
            var langMenu by remember { mutableStateOf(false) }
            val current = languages.firstOrNull { it.code == ui.lang } ?: languages.firstOrNull()
            Box {
                OutlinedButton(onClick = { langMenu = true; viewModel.onLangMenuOpen() }) {
                    Text("🌐  ${current?.flag ?: ""} ${current?.name ?: ui.lang}")
                }
                DropdownMenu(expanded = langMenu, onDismissRequest = { langMenu = false }) {
                    languages.forEach { l ->
                        DropdownMenuItem(
                            text = { Text("${l.flag ?: ""}  ${l.name ?: l.code}") },
                            onClick = { langMenu = false; viewModel.selectLang(l.code) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = ui.host,
                onValueChange = viewModel::onHost,
                label = { Text(t("login.server", "Adresse de l'instance")) },
                placeholder = { Text("ex. scribe.mon-hopital.fr ou 203.0.113.10:8000") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = ui.useHttp, onCheckedChange = viewModel::onUseHttp)
                Text("  ${t("login.http", "Connexion non sécurisée (HTTP)")}",
                    style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = ui.username,
                onValueChange = viewModel::onUsername,
                label = { Text(t("login.username", "Identifiant")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = ui.password,
                onValueChange = viewModel::onPassword,
                label = { Text(t("login.password", "Mot de passe")) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            )
        } else {
            Text(t("login.mfa_title", "Double authentification"),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                t("login.mfa_help", "Saisissez le code de votre application d'authentification (ou un code de secours)."),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = ui.mfaCode,
                onValueChange = viewModel::onMfaCode,
                label = { Text(t("login.mfa_code", "Code à 6 chiffres")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            )
        }

        ui.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = viewModel::submit,
            enabled = !ui.loading && (
                if (ui.step == LoginUiState.Step.CREDENTIALS) ui.canSubmitCredentials else ui.canSubmitMfa
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (ui.loading) {
                CircularProgressIndicator(Modifier.height(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            } else {
                Text(
                    if (ui.step == LoginUiState.Step.CREDENTIALS) t("login.btn_connect", "Se connecter")
                    else t("login.btn_validate", "Valider le code")
                )
            }
        }
    }
}
