package com.scribe.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class LoginRequest(
    val username: String,
    val password: String,
)

@JsonClass(generateAdapter = false)
data class MfaVerifyRequest(
    @Json(name = "mfa_token") val mfaToken: String,
    val code: String,
)

/**
 * Réponse de /auth/login ET de /mfa/verify.
 * Deux cas possibles côté serveur :
 *  - succès direct : { token, user }
 *  - MFA requise   : { require_mfa: true, mfa_token, username }
 */
@JsonClass(generateAdapter = false)
data class LoginResponse(
    val token: String? = null,
    val user: UserDto? = null,
    @Json(name = "require_mfa") val requireMfa: Boolean = false,
    @Json(name = "mfa_token") val mfaToken: String? = null,
    val username: String? = null,
)

@JsonClass(generateAdapter = false)
data class UserDto(
    val id: Int? = null,
    val username: String? = null,
    @Json(name = "display_name") val displayName: String? = null,
    val role: String? = null,
    val perimetre: String? = null,
    @Json(name = "must_change_password") val mustChangePassword: Boolean = false,
)
