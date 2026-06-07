package com.scribe.app.data.repository

import com.scribe.app.data.local.SecureStore
import com.scribe.app.data.remote.ApiProvider
import com.scribe.app.data.remote.dto.CreateIncidentRequest
import com.scribe.app.data.remote.dto.IncidentDto
import com.scribe.app.data.remote.dto.JalonDto
import com.scribe.app.data.remote.dto.JalonUpdateRequest
import com.scribe.app.data.remote.dto.StatusUpdateRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncidentRepository @Inject constructor(
    private val api: ApiProvider,
    private val store: SecureStore,
) {
    private val cache = mutableMapOf<Int, IncidentDto>()

    suspend fun list(status: String? = null): Result<List<IncidentDto>> =
        runCatching {
            val list = api.api().incidents(status = status)
            cache.clear()
            list.forEach { cache[it.id] = it }
            list
        }

    suspend fun get(id: Int): IncidentDto? {
        cache[id]?.let { return it }
        list()
        return cache[id]
    }

    suspend fun create(req: CreateIncidentRequest): Result<IncidentDto> =
        runCatching {
            val created = api.api().createIncident(req)
            store.saveLastSite(req.siteId)
            created
        }

    suspend fun updateStatus(id: Int, status: String, completion: Int? = null): Result<Unit> =
        runCatching {
            api.api().updateIncidentStatus(id, StatusUpdateRequest(status, completion))
            cache.remove(id)
        }

    suspend fun updateJalons(id: Int, jalons: List<JalonDto>): Result<Unit> =
        runCatching {
            api.api().updateIncidentJalons(id, JalonUpdateRequest(jalons))
            cache.remove(id)
        }

    suspend fun formDefaults(): FormDefaults {
        val s = store.snapshot()
        return FormDefaults(declarantNom = s.displayName ?: s.username ?: "", lastSite = s.lastSite ?: "")
    }

    data class FormDefaults(val declarantNom: String, val lastSite: String)
}
