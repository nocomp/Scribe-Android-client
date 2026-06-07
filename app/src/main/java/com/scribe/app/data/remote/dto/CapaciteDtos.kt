package com.scribe.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** GET /api/v1/capacite/referentiel — une unité + sa dernière déclaration. */
@JsonClass(generateAdapter = false)
data class ReferentielDto(
    val id: Int,
    @Json(name = "service_nom") val serviceNom: String? = null,
    @Json(name = "uf_code") val ufCode: String? = null,
    val pole: String? = null,
    val site: String? = null,
    @Json(name = "capacite_totale") val capaciteTotale: Int = 0,
    @Json(name = "statut_global") val statutGlobal: String? = null,
    @Json(name = "derniere_declaration") val derniereDeclaration: DeclarationDto? = null,
)

@JsonClass(generateAdapter = false)
data class DeclarationDto(
    val point: String? = null,
    @Json(name = "lits_vides_h") val litsVidesH: Int = 0,
    @Json(name = "lits_vides_f") val litsVidesF: Int = 0,
    @Json(name = "lits_vides_i") val litsVidesI: Int = 0,
    @Json(name = "statut_lits") val statutLits: String? = null,
    @Json(name = "statut_rh") val statutRh: String? = null,
    @Json(name = "statut_materiel") val statutMateriel: String? = null,
    @Json(name = "commentaire_general") val commentaireGeneral: String? = null,
    val redacteur: String? = null,
    val horodatage: String? = null,
)

/** POST /api/v1/capacite/declaration */
@JsonClass(generateAdapter = false)
data class DeclarationCreateRequest(
    @Json(name = "referentiel_id") val referentielId: Int,
    val redacteur: String,
    val point: String = "matin",
    @Json(name = "lits_vides_h") val litsVidesH: Int = 0,
    @Json(name = "lits_vides_f") val litsVidesF: Int = 0,
    @Json(name = "lits_vides_i") val litsVidesI: Int = 0,
    @Json(name = "lits_sup") val litsSup: Int = 0,
    @Json(name = "statut_lits") val statutLits: String = "normal",
    @Json(name = "statut_rh") val statutRh: String = "complet",
    @Json(name = "statut_materiel") val statutMateriel: String = "ok",
    @Json(name = "alerte_lits") val alerteLits: Boolean = false,
    @Json(name = "alerte_rh") val alerteRh: Boolean = false,
    @Json(name = "alerte_materiel") val alerteMateriel: Boolean = false,
    @Json(name = "commentaire_general") val commentaireGeneral: String? = null,
    @Json(name = "mode_degrade") val modeDegrade: Boolean = false,
    @Json(name = "besoin_renfort") val besoinRenfort: Int = 0,
    @Json(name = "peut_preter") val peutPreter: Int = 0,
    @Json(name = "tension_activee") val tensionActivee: Int = 0,
)
