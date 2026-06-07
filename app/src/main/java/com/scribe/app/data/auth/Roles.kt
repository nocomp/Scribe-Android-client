package com.scribe.app.data.auth

/** Rôles backend SCRIBE v3.4 (+ mapping legacy pré-v3.4). */
enum class UserRole(val api: String) {
    CELLULE_CRISE("cellule_crise"),
    SOIGNANT("soignant"),
    ADMIN("admin"),
    UNKNOWN("unknown");

    companion object {
        fun fromApi(value: String?): UserRole = when (value) {
            "cellule_crise" -> CELLULE_CRISE
            "soignant" -> SOIGNANT
            "admin" -> ADMIN
            // Legacy (backend pré-v3.4)
            "directeur", "observateur", "collaborateur" -> CELLULE_CRISE
            else -> UNKNOWN
        }
    }
}

/** Fonctionnalités soumises à droits. */
enum class Feature {
    DASHBOARD, INCIDENTS, SOINS, CAPACITE, CELLULE, KANBAN, COMMUNIQUES,
    TRANSFERTS, BRANCARDAGE, MESSAGES, CHAT, ANNUAIRE
}

/** Matrice des droits (recopiée de la version web). admin court-circuite tout. */
object Permissions {
    private val cellule = setOf(UserRole.CELLULE_CRISE, UserRole.ADMIN)
    private val soignant = setOf(UserRole.SOIGNANT, UserRole.ADMIN)
    private val tous = setOf(UserRole.CELLULE_CRISE, UserRole.SOIGNANT, UserRole.ADMIN)

    fun allows(feature: Feature, role: UserRole): Boolean {
        if (role == UserRole.ADMIN) return true
        if (feature == Feature.DASHBOARD) return true // toujours accessible
        return when (feature) {
            Feature.INCIDENTS, Feature.MESSAGES, Feature.CHAT, Feature.ANNUAIRE -> role in tous
            Feature.SOINS, Feature.CAPACITE, Feature.CELLULE,
            Feature.KANBAN, Feature.COMMUNIQUES -> role in cellule
            Feature.TRANSFERTS, Feature.BRANCARDAGE -> role in soignant
            Feature.DASHBOARD -> true
        }
    }
}
