package com.scribe.app.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Injecte "Authorization: Bearer <JWT>" sur tous les appels API,
 * SAUF login et MFA (qui n'ont pas encore de token).
 * Équivalent Android du wrapper apiFetch() du front web.
 */
class AuthInterceptor @Inject constructor(
    private val tokenHolder: TokenHolder,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val isAuthFree = path.endsWith("/auth/login") || path.contains("/mfa/")
        val token = tokenHolder.token

        var builder = request.newBuilder()
        builder = builder.header("Accept-Language", tokenHolder.lang)
        if (!isAuthFree && !token.isNullOrBlank()) {
            builder = builder.header("Authorization", "Bearer $token")
        }
        return chain.proceed(builder.build())
    }
}
