package com.scribe.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.scribe.app.data.repository.AuthState
import com.scribe.app.data.i18n.LocaleStore
import com.scribe.app.data.repository.SessionRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    private val session: SessionRepository,
    private val localeStore: LocaleStore,
) : ViewModel() {

    val state: StateFlow<AuthState> = session.state

    init {
        viewModelScope.launch {
            try {
                delay(800)
                session.bootstrap()
                runCatching { localeStore.bootstrap() }
            } catch (e: Throwable) {
                // Filet de sécurité : on ne reste jamais bloqué sur le splash.
                runCatching { session.logout() }
            }
        }
    }

    fun logout() {
        viewModelScope.launch { session.logout() }
    }
}
