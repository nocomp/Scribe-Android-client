package com.scribe.app.data.i18n

import com.scribe.app.data.local.SecureStore
import com.scribe.app.data.remote.ApiProvider
import com.scribe.app.data.remote.TokenHolder
import com.scribe.app.data.remote.dto.LanguageDto
import com.scribe.app.util.UrlNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** Magasin de la langue courante + dictionnaire de traduction (réutilise l'i18n SCRIBE). */
@Singleton
class LocaleStore @Inject constructor(
    private val api: ApiProvider,
    private val store: SecureStore,
    private val tokenHolder: TokenHolder,
) {
    private val _languages = MutableStateFlow(BUNDLED)
    val languages: StateFlow<List<LanguageDto>> = _languages.asStateFlow()

    private val _dict = MutableStateFlow<Map<String, String>>(emptyMap())
    val dict: StateFlow<Map<String, String>> = _dict.asStateFlow()

    @Volatile
    var code: String = "fr"
        private set

    /** Récupère la liste des langues de l'instance (endpoint public). */
    suspend fun loadLanguages(host: String, useHttp: Boolean) {
        val url = UrlNormalizer.build(host, useHttp) ?: return
        api.setBaseUrl(url)
        runCatching { api.api().i18nLanguages() }
            .onSuccess { list -> if (list.isNotEmpty()) _languages.value = list }
    }

    /** Choisit la langue et charge son dictionnaire. */
    suspend fun apply(host: String, useHttp: Boolean, langCode: String) {
        UrlNormalizer.build(host, useHttp)?.let { api.setBaseUrl(it) }
        setCode(langCode)
        store.saveLang(langCode)
        loadDict(langCode)
    }

    /** Au démarrage : restaure la langue sauvegardée et son dictionnaire. */
    suspend fun bootstrap() {
        val saved = store.snapshot().lang ?: "fr"
        setCode(saved)
        if (saved != "fr" && api.hasBaseUrl()) loadDict(saved)
    }

    private fun setCode(c: String) {
        code = c
        tokenHolder.lang = c
    }

    private suspend fun loadDict(langCode: String) {
        if (langCode == "fr") { _dict.value = emptyMap(); return } // fr = libellés natifs
        runCatching { flatten(api.api().i18nDict(langCode).string()) }
            .onSuccess { _dict.value = it }
            .onFailure { _dict.value = emptyMap() }
    }

    private fun flatten(json: String): Map<String, String> {
        val out = HashMap<String, String>()
        fun walk(obj: JSONObject, prefix: String) {
            val it = obj.keys()
            while (it.hasNext()) {
                val k = it.next()
                if (k == "_meta") continue
                val key = if (prefix.isEmpty()) k else "$prefix.$k"
                val v = obj.get(k)
                if (v is JSONObject) walk(v, key) else out[key] = v.toString()
            }
        }
        runCatching { walk(JSONObject(json), "") }
        return out
    }

    companion object {
        /** Liste de repli (24 langues UE) si l'instance n'est pas encore joignable. */
        val BUNDLED: List<LanguageDto> = listOf(
            LanguageDto("fr", "Français", "🇫🇷"), LanguageDto("en", "English", "🇬🇧"),
            LanguageDto("de", "Deutsch", "🇩🇪"), LanguageDto("es", "Español", "🇪🇸"),
            LanguageDto("it", "Italiano", "🇮🇹"), LanguageDto("pt", "Português", "🇵🇹"),
            LanguageDto("nl", "Nederlands", "🇳🇱"), LanguageDto("pl", "Polski", "🇵🇱"),
            LanguageDto("ro", "Română", "🇷🇴"), LanguageDto("el", "Ελληνικά", "🇬🇷"),
            LanguageDto("sv", "Svenska", "🇸🇪"), LanguageDto("da", "Dansk", "🇩🇰"),
            LanguageDto("fi", "Suomi", "🇫🇮"), LanguageDto("cs", "Čeština", "🇨🇿"),
            LanguageDto("hu", "Magyar", "🇭🇺"), LanguageDto("bg", "Български", "🇧🇬"),
            LanguageDto("hr", "Hrvatski", "🇭🇷"), LanguageDto("sk", "Slovenčina", "🇸🇰"),
            LanguageDto("sl", "Slovenščina", "🇸🇮"), LanguageDto("et", "Eesti", "🇪🇪"),
            LanguageDto("lt", "Lietuvių", "🇱🇹"), LanguageDto("lv", "Latviešu", "🇱🇻"),
            LanguageDto("ga", "Gaeilge", "🇮🇪"), LanguageDto("mt", "Malti", "🇲🇹"),
        )
    }
}
