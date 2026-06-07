package com.scribe.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** GET /api/v1/auth/annuaire-messagerie — un contact du carnet d'adresses. */
@JsonClass(generateAdapter = false)
data class ContactDto(
    val id: Int,
    val username: String? = null,
    @Json(name = "display_name") val displayName: String? = null,
    val service: String? = null,
    val role: String? = null,
    @Json(name = "site_tag") val siteTag: String? = null,
    val online: String? = null,
    @Json(name = "inactivity_label") val inactivityLabel: String? = null,
)

/** POST /api/v1/messagerie */
@JsonClass(generateAdapter = false)
data class MessageSendRequest(
    @Json(name = "destinataire_id") val destinataireId: Int,
    val sujet: String,
    val contenu: String,
    @Json(name = "reply_to") val replyTo: Int? = null,
)
