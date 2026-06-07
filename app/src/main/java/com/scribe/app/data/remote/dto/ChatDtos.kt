package com.scribe.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** POST /api/v1/chat/salons/{id}/messages */
@JsonClass(generateAdapter = false)
data class ChatMessageSendRequest(
    val contenu: String,
)

/** GET /api/v1/chat/presence → { sigle: [PresenceUserDto] } */
@JsonClass(generateAdapter = false)
data class PresenceUserDto(
    @Json(name = "user_id") val userId: Int? = null,
    @Json(name = "display_name") val displayName: String? = null,
)

/** POST /api/v1/chat/salons */
@JsonClass(generateAdapter = false)
data class SalonCreateRequest(
    val nom: String,
    val description: String? = null,
    val couleur: String = "#003189",
    val icone: String = "🔒",
    val type: String = "prive",
)
