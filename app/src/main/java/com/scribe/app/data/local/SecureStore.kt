package com.scribe.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "scribe_session")

/**
 * Persiste la session : URL d'instance, option HTTP, JWT (chiffré Keystore),
 * et quelques infos utilisateur pour pré-remplir l'UI.
 */
@Singleton
class SecureStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crypto: CryptoManager,
) {
    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val USE_HTTP = booleanPreferencesKey("use_http")
        val TOKEN_ENC = stringPreferencesKey("token_enc")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val USERNAME = stringPreferencesKey("username")
        val ROLE = stringPreferencesKey("role")
        val LAST_SITE = stringPreferencesKey("last_site")
        val LANG = stringPreferencesKey("lang")
    }

    suspend fun saveSession(
        baseUrl: String,
        useHttp: Boolean,
        token: String,
        displayName: String?,
        username: String?,
        role: String?,
    ) {
        context.dataStore.edit { p ->
            p[Keys.BASE_URL] = baseUrl
            p[Keys.USE_HTTP] = useHttp
            p[Keys.TOKEN_ENC] = crypto.encrypt(token)
            displayName?.let { p[Keys.DISPLAY_NAME] = it }
            username?.let { p[Keys.USERNAME] = it }
            role?.let { p[Keys.ROLE] = it }
        }
    }

    suspend fun saveLastSite(site: String) {
        context.dataStore.edit { it[Keys.LAST_SITE] = site }
    }

    suspend fun saveLang(code: String) {
        context.dataStore.edit { it[Keys.LANG] = code }
    }

    suspend fun clearToken() {
        context.dataStore.edit { it.remove(Keys.TOKEN_ENC) }
    }

    suspend fun snapshot(): Session {
        val p = context.dataStore.data.first()
        return Session(
            baseUrl = p[Keys.BASE_URL],
            useHttp = p[Keys.USE_HTTP] ?: false,
            token = p[Keys.TOKEN_ENC]?.let { crypto.decrypt(it) },
            displayName = p[Keys.DISPLAY_NAME],
            username = p[Keys.USERNAME],
            role = p[Keys.ROLE],
            lastSite = p[Keys.LAST_SITE],
            lang = p[Keys.LANG] ?: "fr",
        )
    }

    data class Session(
        val baseUrl: String?,
        val useHttp: Boolean,
        val token: String?,
        val displayName: String?,
        val username: String?,
        val role: String?,
        val lastSite: String?,
        val lang: String = "fr",
    )
}
