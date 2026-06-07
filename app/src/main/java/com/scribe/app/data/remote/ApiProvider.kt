package com.scribe.app.data.remote

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Construit (et reconstruit) le client Retrofit selon l'URL d'instance courante.
 * L'URL n'est pas fixe : chaque hôpital a sa propre instance SCRIBE.
 */
@Singleton
class ApiProvider @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
) {
    @Volatile
    private var baseUrl: String? = null

    @Volatile
    private var cached: ScribeApi? = null

    fun setBaseUrl(url: String) {
        if (url != baseUrl) {
            baseUrl = url
            cached = null
        }
    }

    fun hasBaseUrl(): Boolean = !baseUrl.isNullOrBlank()

    /** @throws IllegalStateException si aucune URL d'instance n'a été configurée. */
    fun api(): ScribeApi {
        cached?.let { return it }
        val url = baseUrl ?: error("Aucune URL d'instance configurée")
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        return retrofit.create(ScribeApi::class.java).also { cached = it }
    }
}
