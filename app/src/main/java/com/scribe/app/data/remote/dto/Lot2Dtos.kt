package com.scribe.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ---- /api/v1/sitrep/stats ----
@JsonClass(generateAdapter = false)
data class StatsDto(
    val total: Int = 0,
    val critical: Int = 0,
    val ouverts: Int = 0,
    val cyber: Int = 0,
    val sanitaire: Int = 0,
)

// ---- /api/v1/status/current (communiqué) ----
@JsonClass(generateAdapter = false)
data class CommuniqueDto(
    @Json(name = "site_nom") val siteNom: String? = null,
    @Json(name = "niveau_global") val niveauGlobal: String? = null,
    @Json(name = "message_public") val messagePublic: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "updated_by") val updatedBy: String? = null,
    @Json(name = "services_si") val servicesSi: List<ServiceStatutDto> = emptyList(),
    @Json(name = "prise_en_charge") val priseEnCharge: List<ServiceStatutDto> = emptyList(),
    val faq: List<FaqDto> = emptyList(),
    val chronologie: List<ChronologieDto> = emptyList(),
    val published: Boolean = false,
)

@JsonClass(generateAdapter = false)
data class ServiceStatutDto(
    val id: String? = null,
    val label: String? = null,
    val statut: String? = null,
)

@JsonClass(generateAdapter = false)
data class FaqDto(
    val question: String? = null,
    val reponse: String? = null,
    val visible: Boolean = false,
)

@JsonClass(generateAdapter = false)
data class ChronologieDto(
    val id: Int? = null,
    val ts: String? = null,
    val texte: String? = null,
    @Json(name = "publie_par") val publiePar: String? = null,
)

// ---- /api/v1/capacite/synthese : Map<site, Map<pole, PoleSyntheseDto>> ----
@JsonClass(generateAdapter = false)
data class PoleSyntheseDto(
    @Json(name = "lits_total") val litsTotal: Int = 0,
    @Json(name = "lits_vides_h") val litsVidesH: Int = 0,
    @Json(name = "lits_vides_f") val litsVidesF: Int = 0,
    @Json(name = "lits_vides_i") val litsVidesI: Int = 0,
    val alertes: Int = 0,
    @Json(name = "non_declares") val nonDeclares: Int = 0,
    @Json(name = "statut_pole") val statutPole: String? = null,
    val services: List<ServiceSyntheseDto> = emptyList(),
)

@JsonClass(generateAdapter = false)
data class ServiceSyntheseDto(
    val id: Int? = null,
    val nom: String? = null,
    val statut: String? = null,
    @Json(name = "lits_vides_h") val litsVidesH: Int? = null,
    @Json(name = "lits_vides_f") val litsVidesF: Int? = null,
    @Json(name = "lits_vides_i") val litsVidesI: Int? = null,
    val alerte: Boolean = false,
)

// ---- /api/v1/messagerie ----
@JsonClass(generateAdapter = false)
data class MessageDto(
    val id: Int,
    val sujet: String? = null,
    val contenu: String? = null,
    @Json(name = "expediteur_id") val expediteurId: Int? = null,
    @Json(name = "expediteur_nom") val expediteurNom: String? = null,
    @Json(name = "destinataire_id") val destinataireId: Int? = null,
    @Json(name = "destinataire_nom") val destinataireNom: String? = null,
    val lu: Boolean = false,
    @Json(name = "reply_to") val replyTo: Int? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "is_mine") val isMine: Boolean = false,
)

// ---- /api/v1/chat ----
@JsonClass(generateAdapter = false)
data class SalonDto(
    val id: Int,
    val nom: String? = null,
    val description: String? = null,
    val icone: String? = null,
    val type: String? = null,
)

@JsonClass(generateAdapter = false)
data class ChatMessageDto(
    val id: Int,
    @Json(name = "auteur_nom") val auteurNom: String? = null,
    @Json(name = "auteur_sigle") val auteurSigle: String? = null,
    val contenu: String? = null,
    val horodatage: String? = null,
)
