package com.scribe.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ---- Brancardage ----
@JsonClass(generateAdapter = false)
data class MissionDto(
    val id: Int,
    @Json(name = "ref_patient") val refPatient: String? = null,
    @Json(name = "uf_origine") val ufOrigine: String? = null,
    @Json(name = "chambre_depart") val chambreDepart: String? = null,
    @Json(name = "uf_destination") val ufDestination: String? = null,
    @Json(name = "chambre_arrivee") val chambreArrivee: String? = null,
    @Json(name = "type_transport") val typeTransport: String? = null,
    @Json(name = "priorite_label") val prioriteLabel: String? = null,
    val motif: String? = null,
    val statut: String? = null,
    @Json(name = "statut_label") val statutLabel: String? = null,
    @Json(name = "agent_nom") val agentNom: String? = null,
)

@JsonClass(generateAdapter = false)
data class PriseEnChargeRequest(
    @Json(name = "agent_nom") val agentNom: String,
    @Json(name = "agent_tel") val agentTel: String? = null,
)

@JsonClass(generateAdapter = false)
data class ArriveeRequest(val commentaire: String? = null)

@JsonClass(generateAdapter = false)
data class MissionPatchRequest(val statut: String, val commentaire: String? = null)
