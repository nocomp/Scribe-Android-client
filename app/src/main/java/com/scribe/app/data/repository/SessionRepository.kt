package com.scribe.app.data.repository

import com.scribe.app.data.local.SecureStore
import com.scribe.app.data.remote.ApiProvider
import com.scribe.app.data.remote.TokenHolder
import com.scribe.app.data.remote.dto.LoginRequest
import com.scribe.app.data.remote.dto.LoginResponse
import com.scribe.app.data.remote.dto.MfaVerifyRequest
import com.scribe.app.util.UrlNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AuthState {
    data object Loading : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(val displayName: String?, val role: String?) : AuthState
}

/** Résultat d'une tentative de connexion (1re phase). */
sealed interface LoginOutcome {
    data object Success : LoginOutcome
    data class MfaRequired(val mfaToken: String) : LoginOutcome
}

@Singleton
class SessionRepository @Inject constructor(
    private val api: ApiProvider,
    private val tokenHolder: TokenHolder,
    private val store: SecureStore,
) {
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    // Contexte de connexion en cours, mémorisé entre la phase 1 et la phase MFA.
    private var pendingBaseUrl: String? = null
    private var pendingUseHttp: Boolean = false

    /** Hydrate la session persistée au démarrage. */
    suspend fun bootstrap() {
        val s = store.snapshot()
        if (!s.baseUrl.isNullOrBlank() && !s.token.isNullOrBlank()) {
            api.setBaseUrl(s.baseUrl)
            tokenHolder.token = s.token
            // Vérifie que le token est encore valide.
            val valid = runCatching { api.api().me() }.isSuccess
            if (valid) {
                _state.value = AuthState.LoggedIn(s.displayName, s.role)
                return
            }
            store.clearToken()
            tokenHolder.token = null
        }
        _state.value = AuthState.LoggedOut
    }

    suspend fun login(
        host: String,
        useHttp: Boolean,
        username: String,
        password: String,
    ): Result<LoginOutcome> {
        val baseUrl = UrlNormalizer.build(host, useHttp)
            ?: return Result.failure(IllegalArgumentException("Adresse d'instance invalide"))
        pendingBaseUrl = baseUrl
        pendingUseHttp = useHttp
        api.setBaseUrl(baseUrl)
        return runCatching {
            val resp = api.api().login(LoginRequest(username, password))
            handleLoginResponse(resp, baseUrl, useHttp)
        }
    }

    suspend fun verifyMfa(code: String): Result<LoginOutcome> {
        val baseUrl = pendingBaseUrl
            ?: return Result.failure(IllegalStateException("Aucune connexion en cours"))
        val mfaToken = pendingMfaToken
            ?: return Result.failure(IllegalStateException("Token MFA absent"))
        return runCatching {
            val resp = api.api().mfaVerify(MfaVerifyRequest(mfaToken, code))
            handleLoginResponse(resp, baseUrl, pendingUseHttp)
        }
    }

    private var pendingMfaToken: String? = null

    private suspend fun handleLoginResponse(
        resp: LoginResponse,
        baseUrl: String,
        useHttp: Boolean,
    ): LoginOutcome {
        if (resp.requireMfa && !resp.mfaToken.isNullOrBlank()) {
            pendingMfaToken = resp.mfaToken
            return LoginOutcome.MfaRequired(resp.mfaToken)
        }
        val token = resp.token
            ?: throw IllegalStateException("Réponse de connexion sans token")
        tokenHolder.token = token
        store.saveSession(
            baseUrl = baseUrl,
            useHttp = useHttp,
            token = token,
            displayName = resp.user?.displayName,
            username = resp.user?.username,
            role = resp.user?.role,
        )
        pendingMfaToken = null
        _state.value = AuthState.LoggedIn(resp.user?.displayName, resp.user?.role)
        return LoginOutcome.Success
    }

    suspend fun logout() {
        store.clearToken()
        tokenHolder.token = null
        _state.value = AuthState.LoggedOut
    }
}
