package com.scribe.app.util

/**
 * Construit l'URL de base à partir de ce que tape l'utilisateur.
 * L'utilisateur saisit juste un nom de domaine (ou domaine:port, ou ip:port),
 * sans schéma. On retire tout http(s):// éventuellement collé, on enlève les
 * slashs de fin, puis on préfixe par https (ou http si l'option est cochée).
 *
 * Exemples :
 *   ("scribe.mon-etablissement.fr", useHttp=false) -> "https://scribe.mon-etablissement.fr/"
 *   ("203.0.113.10:8000", useHttp=true)        -> "http://203.0.113.10:8000/"
 *   ("http://demo.local:8080/", useHttp=true)  -> "http://demo.local:8080/"
 */
object UrlNormalizer {
    fun build(rawHost: String, useHttp: Boolean): String? {
        var h = rawHost.trim()
        if (h.isEmpty()) return null
        h = h.removePrefix("https://").removePrefix("http://").trimEnd('/')
        if (h.isEmpty()) return null
        val scheme = if (useHttp) "http" else "https"
        return "$scheme://$h/"
    }

    /** Affiche l'hôte sans schéma pour pré-remplir le champ de saisie. */
    fun displayHost(baseUrl: String?): String {
        if (baseUrl.isNullOrBlank()) return ""
        return baseUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')
    }
}
