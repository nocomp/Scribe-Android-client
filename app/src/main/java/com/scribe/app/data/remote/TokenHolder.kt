package com.scribe.app.data.remote

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Conserve le JWT en mémoire pour l'intercepteur OkHttp (lecture synchrone).
 * Hydraté au démarrage par SessionRepository.bootstrap() et mis à jour au login.
 */
@Singleton
class TokenHolder @Inject constructor() {
    @Volatile
    var token: String? = null

    @Volatile
    var lang: String = "fr"
}
