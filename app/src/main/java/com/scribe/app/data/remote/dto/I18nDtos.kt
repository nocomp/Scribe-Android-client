package com.scribe.app.data.remote.dto

import com.squareup.moshi.JsonClass

/** GET /api/v1/i18n/languages */
@JsonClass(generateAdapter = false)
data class LanguageDto(
    val code: String,
    val name: String? = null,
    val flag: String? = null,
    val direction: String? = null,
)
