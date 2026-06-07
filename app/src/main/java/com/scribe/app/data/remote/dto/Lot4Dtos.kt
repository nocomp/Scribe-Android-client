package com.scribe.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ---- Kanban : GET /api/v1/tasks/ ----
@JsonClass(generateAdapter = false)
data class TaskDto(
    val id: Int,
    val titre: String? = null,
    val description: String? = null,
    val assignee: String? = null,
    val priorite: Int = 0,
    val colonne: String = "BACKLOG",
    @Json(name = "incident_id") val incidentId: Int? = null,
)

@JsonClass(generateAdapter = false)
data class TaskMoveRequest(val colonne: String)

// ---- Cellule : décisions ----
@JsonClass(generateAdapter = false)
data class DecisionDto(
    val id: Int,
    val timestamp: String? = null,
    val contenu: String? = null,
    val responsable: String? = null,
    @Json(name = "base_reglementaire") val baseReglementaire: String? = null,
    @Json(name = "statut_validation") val statutValidation: String? = null,
)

@JsonClass(generateAdapter = false)
data class DecisionCreateRequest(
    val contenu: String,
    val responsable: String? = "",
    @Json(name = "base_reglementaire") val baseReglementaire: String? = "Plan Blanc",
)

// ---- Transferts (anonymisés) ----
@JsonClass(generateAdapter = false)
data class TransfertDto(
    val id: Int,
    @Json(name = "unite_origine") val uniteOrigine: String? = null,
    @Json(name = "etablissement_origine") val etablissementOrigine: String? = null,
    @Json(name = "unite_destination") val uniteDestination: String? = null,
    @Json(name = "etablissement_destination") val etablissementDestination: String? = null,
    val statut: String? = null,
    @Json(name = "horodatage_depart") val horodatageDepart: String? = null,
)

// ---- Compteur non-lus ----
@JsonClass(generateAdapter = false)
data class UnreadDto(val count: Int = 0)

@JsonClass(generateAdapter = false)
data class TransfertStatutRequest(val statut: String, val reason: String? = null)
