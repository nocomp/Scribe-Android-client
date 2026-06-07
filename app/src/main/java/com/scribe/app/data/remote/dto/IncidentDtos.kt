package com.scribe.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Sortie de GET /api/v1/sitrep/history (modèle IncidentOut côté serveur). */
@JsonClass(generateAdapter = false)
data class IncidentDto(
    val id: Int,
    val timestamp: String? = null,
    @Json(name = "declarant_nom") val declarantNom: String? = null,
    @Json(name = "directeur_crise") val directeurCrise: String? = null,
    @Json(name = "site_id") val siteId: String? = null,
    @Json(name = "unite_fonctionnelle") val uniteFonctionnelle: String? = null,
    @Json(name = "type_crise") val typeCrise: String? = null,
    val urgency: Int = 1,
    val fait: String? = null,
    val analyse: String? = null,
    val status: String? = null,
    @Json(name = "completion_percent") val completionPercent: Int = 0,
    @Json(name = "impact_fonctionnel") val impactFonctionnel: Boolean = false,
    // Chaîne JSON : [{label, done, done_at}] — parsée côté client.
    val jalons: String? = null,
)

/** Corps de POST /api/v1/sitrep/post (modèle IncidentCreate côté serveur). */
@JsonClass(generateAdapter = false)
data class CreateIncidentRequest(
    @Json(name = "declarant_nom") val declarantNom: String,
    @Json(name = "site_id") val siteId: String,
    val fait: String,
    @Json(name = "type_crise") val typeCrise: String = "SANITAIRE",
    val urgency: Int = 1,
    @Json(name = "unite_fonctionnelle") val uniteFonctionnelle: String? = "",
    @Json(name = "directeur_crise") val directeurCrise: String? = null,
    val analyse: String? = "",
    @Json(name = "impact_fonctionnel") val impactFonctionnel: Boolean = false,
)

/** PUT /api/v1/sitrep/{id}/status */
@JsonClass(generateAdapter = false)
data class StatusUpdateRequest(
    val status: String,
    @Json(name = "completion_percent") val completionPercent: Int? = null,
)

/** Élément de jalon (label, done, done_at). */
@JsonClass(generateAdapter = false)
data class JalonDto(
    val label: String = "",
    val done: Boolean = false,
    @Json(name = "done_at") val doneAt: String? = null,
)

/** PUT /api/v1/sitrep/{id}/jalons */
@JsonClass(generateAdapter = false)
data class JalonUpdateRequest(
    val jalons: List<JalonDto>,
)
