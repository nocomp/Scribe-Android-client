package com.scribe.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.scribe.app.data.i18n.LocaleStore
import com.scribe.app.data.remote.dto.LanguageDto
import com.scribe.app.data.repository.LoginOutcome
import com.scribe.app.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val host: String = "",
    val useHttp: Boolean = false,
    val username: String = "",
    val password: String = "",
    val mfaCode: String = "",
    val lang: String = "fr",
    val step: Step = Step.CREDENTIALS,
    val loading: Boolean = false,
    val error: String? = null,
) {
    enum class Step { CREDENTIALS, MFA }
    val canSubmitCredentials get() = host.isNotBlank() && username.isNotBlank() && password.isNotBlank()
    val canSubmitMfa get() = mfaCode.length >= 6
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val session: SessionRepository,
    private val locale: LocaleStore,
) : ViewModel() {

    private val _ui = MutableStateFlow(LoginUiState(lang = locale.code))
    val ui: StateFlow<LoginUiState> = _ui.asStateFlow()

    val languages: StateFlow<List<LanguageDto>> = locale.languages
    val dict: StateFlow<Map<String, String>> = locale.dict

    fun onLangMenuOpen() {
        val s = _ui.value
        if (s.host.isNotBlank()) viewModelScope.launch { locale.loadLanguages(s.host.trim(), s.useHttp) }
    }

    fun selectLang(code: String) {
        _ui.update { it.copy(lang = code) }
        val s = _ui.value
        viewModelScope.launch { locale.apply(s.host.trim(), s.useHttp, code) }
    }

    fun onHost(v: String) = _ui.update { it.copy(host = v, error = null) }
    fun onUseHttp(v: Boolean) = _ui.update { it.copy(useHttp = v) }
    fun onUsername(v: String) = _ui.update { it.copy(username = v, error = null) }
    fun onPassword(v: String) = _ui.update { it.copy(password = v, error = null) }
    fun onMfaCode(v: String) = _ui.update { it.copy(mfaCode = v.filter(Char::isDigit).take(8), error = null) }

    fun submit() {
        val s = _ui.value
        if (s.loading) return
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = if (s.step == LoginUiState.Step.CREDENTIALS) {
                session.login(s.host, s.useHttp, s.username.trim(), s.password)
            } else {
                session.verifyMfa(s.mfaCode)
            }
            result
                .onSuccess { outcome ->
                    when (outcome) {
                        is LoginOutcome.Success -> _ui.update { it.copy(loading = false) }
                        is LoginOutcome.MfaRequired -> _ui.update { it.copy(loading = false, step = LoginUiState.Step.MFA) }
                    }
                }
                .onFailure { e -> _ui.update { it.copy(loading = false, error = mapError(e)) } }
        }
    }

    private fun mapError(e: Throwable): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("401") || msg.contains("403") -> "Identifiants ou code incorrects."
            msg.contains("Unable to resolve host") || msg.contains("failed to connect") ->
                "Instance injoignable. Vérifiez l'adresse, le port et le réseau."
            msg.contains("CertPath") || msg.contains("SSL") ->
                "Erreur TLS. Essayez l'option « connexion non sécurisée (HTTP) »."
            else -> "Échec de la connexion. ($msg)"
        }
    }
}
