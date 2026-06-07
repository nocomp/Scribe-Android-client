package com.scribe.app.data.remote

import com.scribe.app.data.remote.dto.ChatMessageDto
import com.scribe.app.data.remote.dto.ChatMessageSendRequest
import com.scribe.app.data.remote.dto.CommuniqueDto
import com.scribe.app.data.remote.dto.ContactDto
import com.scribe.app.data.remote.dto.CreateIncidentRequest
import com.scribe.app.data.remote.dto.DeclarationCreateRequest
import com.scribe.app.data.remote.dto.IncidentDto
import com.scribe.app.data.remote.dto.LanguageDto
import com.scribe.app.data.remote.dto.JalonUpdateRequest
import com.scribe.app.data.remote.dto.LoginRequest
import com.scribe.app.data.remote.dto.LoginResponse
import com.scribe.app.data.remote.dto.MessageDto
import com.scribe.app.data.remote.dto.MessageSendRequest
import com.scribe.app.data.remote.dto.MfaVerifyRequest
import com.scribe.app.data.remote.dto.PoleSyntheseDto
import com.scribe.app.data.remote.dto.PresenceUserDto
import com.scribe.app.data.remote.dto.ReferentielDto
import com.scribe.app.data.remote.dto.SalonCreateRequest
import com.scribe.app.data.remote.dto.SalonDto
import com.scribe.app.data.remote.dto.StatsDto
import com.scribe.app.data.remote.dto.StatusUpdateRequest
import com.scribe.app.data.remote.dto.DecisionCreateRequest
import com.scribe.app.data.remote.dto.DecisionDto
import com.scribe.app.data.remote.dto.TaskDto
import com.scribe.app.data.remote.dto.TaskMoveRequest
import com.scribe.app.data.remote.dto.TransfertDto
import com.scribe.app.data.remote.dto.TransfertStatutRequest
import com.scribe.app.data.remote.dto.UnreadDto
import com.scribe.app.data.remote.dto.ArriveeRequest
import com.scribe.app.data.remote.dto.MissionDto
import com.scribe.app.data.remote.dto.MissionPatchRequest
import com.scribe.app.data.remote.dto.PriseEnChargeRequest
import com.scribe.app.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ScribeApi {

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/v1/mfa/verify")
    suspend fun mfaVerify(@Body body: MfaVerifyRequest): LoginResponse

    @GET("api/v1/auth/me")
    suspend fun me(): UserDto

    @GET("api/v1/auth/annuaire-messagerie")
    suspend fun annuaireMessagerie(): List<ContactDto>

    @GET("api/v1/sitrep/history")
    suspend fun incidents(
        @Query("status") status: String? = null,
        @Query("urgency") urgency: Int? = null,
        @Query("type_crise") typeCrise: String? = null,
    ): List<IncidentDto>

    @POST("api/v1/sitrep/post")
    suspend fun createIncident(@Body body: CreateIncidentRequest): IncidentDto

    @PUT("api/v1/sitrep/{id}/status")
    suspend fun updateIncidentStatus(@Path("id") id: Int, @Body body: StatusUpdateRequest)

    @PUT("api/v1/sitrep/{id}/jalons")
    suspend fun updateIncidentJalons(@Path("id") id: Int, @Body body: JalonUpdateRequest)

    @GET("api/v1/sitrep/stats")
    suspend fun stats(): StatsDto

    @GET("api/v1/status/current")
    suspend fun communique(): CommuniqueDto

    @GET("api/v1/capacite/synthese")
    suspend fun capaciteSynthese(): Map<String, Map<String, PoleSyntheseDto>>

    @GET("api/v1/capacite/referentiel")
    suspend fun capaciteReferentiel(): List<ReferentielDto>

    @POST("api/v1/capacite/declaration")
    suspend fun declareCapacite(@Body body: DeclarationCreateRequest)

    @GET("api/v1/messagerie")
    suspend fun messages(@Query("boite") boite: String = "reception"): List<MessageDto>

    @POST("api/v1/messagerie")
    suspend fun sendMessage(@Body body: MessageSendRequest)

    @PUT("api/v1/messagerie/{id}/lire")
    suspend fun markMessageRead(@Path("id") id: Int)

    @GET("api/v1/chat/salons")
    suspend fun chatSalons(): List<SalonDto>

    @GET("api/v1/chat/salons/{salonId}/messages")
    suspend fun chatMessages(
        @Path("salonId") salonId: Int,
        @Query("limit") limit: Int = 50,
    ): List<ChatMessageDto>

    @POST("api/v1/chat/salons/{salonId}/messages")
    suspend fun postChatMessage(@Path("salonId") salonId: Int, @Body body: ChatMessageSendRequest)

    @GET("api/v1/chat/presence")
    suspend fun chatPresence(): Map<String, List<PresenceUserDto>>

    @POST("api/v1/chat/presence/ping")
    suspend fun chatPing()

    @POST("api/v1/chat/salons")
    suspend fun createSalon(@Body body: SalonCreateRequest)

    @GET("api/v1/messagerie/non-lus")
    suspend fun unreadMessages(): UnreadDto

    @GET("api/v1/tasks/")
    suspend fun tasks(): List<TaskDto>

    @PUT("api/v1/tasks/{id}/move")
    suspend fun moveTask(@Path("id") id: Int, @Body body: TaskMoveRequest)

    @GET("api/v1/cellule/decisions")
    suspend fun decisions(): List<DecisionDto>

    @POST("api/v1/cellule/decisions")
    suspend fun createDecision(@Body body: DecisionCreateRequest)

    @GET("api/v1/transferts")
    suspend fun transferts(): List<TransfertDto>

    @PATCH("api/v1/transferts/{id}/statut")
    suspend fun transfertStatut(@Path("id") id: Int, @Body body: TransfertStatutRequest)

    @GET("api/v1/brancardage/missions")
    suspend fun missions(): List<MissionDto>

    @POST("api/v1/brancardage/missions/{id}/prendre_en_charge")
    suspend fun brcPrendre(@Path("id") id: Int, @Body body: PriseEnChargeRequest)

    @POST("api/v1/brancardage/missions/{id}/arrivee")
    suspend fun brcArrivee(@Path("id") id: Int, @Body body: ArriveeRequest)

    @PATCH("api/v1/brancardage/missions/{id}")
    suspend fun brcPatch(@Path("id") id: Int, @Body body: MissionPatchRequest)

    @GET("api/v1/i18n/languages")
    suspend fun i18nLanguages(): List<LanguageDto>

    @GET("api/v1/i18n/{code}")
    suspend fun i18nDict(@Path("code") code: String): okhttp3.ResponseBody
}
