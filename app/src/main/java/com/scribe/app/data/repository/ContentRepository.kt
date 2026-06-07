package com.scribe.app.data.repository

import com.scribe.app.data.remote.ApiProvider
import com.scribe.app.data.remote.dto.ChatMessageDto
import com.scribe.app.data.remote.dto.ChatMessageSendRequest
import com.scribe.app.data.remote.dto.CommuniqueDto
import com.scribe.app.data.remote.dto.ContactDto
import com.scribe.app.data.remote.dto.DeclarationCreateRequest
import com.scribe.app.data.remote.dto.MessageDto
import com.scribe.app.data.remote.dto.MessageSendRequest
import com.scribe.app.data.remote.dto.PoleSyntheseDto
import com.scribe.app.data.remote.dto.PresenceUserDto
import com.scribe.app.data.remote.dto.ReferentielDto
import com.scribe.app.data.remote.dto.SalonCreateRequest
import com.scribe.app.data.remote.dto.SalonDto
import com.scribe.app.data.remote.dto.DecisionCreateRequest
import com.scribe.app.data.remote.dto.DecisionDto
import com.scribe.app.data.remote.dto.TaskDto
import com.scribe.app.data.remote.dto.TaskMoveRequest
import com.scribe.app.data.remote.dto.TransfertDto
import com.scribe.app.data.remote.dto.TransfertStatutRequest
import com.scribe.app.data.remote.dto.ArriveeRequest
import com.scribe.app.data.remote.dto.MissionDto
import com.scribe.app.data.remote.dto.MissionPatchRequest
import com.scribe.app.data.remote.dto.PriseEnChargeRequest
import com.scribe.app.data.remote.dto.StatsDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepository @Inject constructor(
    private val api: ApiProvider,
) {
    suspend fun stats(): Result<StatsDto> = runCatching { api.api().stats() }
    suspend fun communique(): Result<CommuniqueDto> = runCatching { api.api().communique() }
    suspend fun capacite(): Result<Map<String, Map<String, PoleSyntheseDto>>> =
        runCatching { api.api().capaciteSynthese() }
    suspend fun referentiel(): Result<List<ReferentielDto>> =
        runCatching { api.api().capaciteReferentiel() }
    suspend fun declare(req: DeclarationCreateRequest): Result<Unit> =
        runCatching { api.api().declareCapacite(req) }
    suspend fun messages(): Result<List<MessageDto>> = runCatching { api.api().messages() }
    suspend fun annuaire(): Result<List<ContactDto>> = runCatching { api.api().annuaireMessagerie() }
    suspend fun sendMessage(req: MessageSendRequest): Result<Unit> =
        runCatching { api.api().sendMessage(req) }
    suspend fun markRead(id: Int): Result<Unit> = runCatching { api.api().markMessageRead(id) }
    suspend fun salons(): Result<List<SalonDto>> = runCatching { api.api().chatSalons() }
    suspend fun salonMessages(id: Int): Result<List<ChatMessageDto>> =
        runCatching { api.api().chatMessages(id) }
    suspend fun postChatMessage(salonId: Int, contenu: String): Result<Unit> =
        runCatching { api.api().postChatMessage(salonId, ChatMessageSendRequest(contenu)) }
    suspend fun presence(): Result<Map<String, List<PresenceUserDto>>> =
        runCatching { api.api().chatPresence() }
    suspend fun ping(): Result<Unit> = runCatching { api.api().chatPing() }
    suspend fun createSalon(nom: String): Result<Unit> =
        runCatching { api.api().createSalon(SalonCreateRequest(nom = nom, type = "local", icone = "🔒")) }

    suspend fun unread(): Int = runCatching { api.api().unreadMessages().count }.getOrDefault(0)
    suspend fun tasks(): Result<List<TaskDto>> = runCatching { api.api().tasks() }
    suspend fun moveTask(id: Int, colonne: String): Result<Unit> =
        runCatching { api.api().moveTask(id, TaskMoveRequest(colonne)) }
    suspend fun decisions(): Result<List<DecisionDto>> = runCatching { api.api().decisions() }
    suspend fun createDecision(req: DecisionCreateRequest): Result<Unit> =
        runCatching { api.api().createDecision(req) }
    suspend fun transferts(): Result<List<TransfertDto>> = runCatching { api.api().transferts() }
    suspend fun transfertStatut(id: Int, statut: String): Result<Unit> =
        runCatching { api.api().transfertStatut(id, TransfertStatutRequest(statut)) }
    suspend fun missions(): Result<List<MissionDto>> = runCatching { api.api().missions() }
    suspend fun brcPrendre(id: Int, nom: String): Result<Unit> =
        runCatching { api.api().brcPrendre(id, PriseEnChargeRequest(nom)) }
    suspend fun brcArrivee(id: Int): Result<Unit> =
        runCatching { api.api().brcArrivee(id, ArriveeRequest()) }
    suspend fun brcPatch(id: Int, statut: String): Result<Unit> =
        runCatching { api.api().brcPatch(id, MissionPatchRequest(statut)) }
}
