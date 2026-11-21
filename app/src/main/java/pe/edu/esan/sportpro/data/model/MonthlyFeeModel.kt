package pe.edu.esan.sportpro.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

object MonthlyFeeStatus {
    const val PENDING = "Pendiente"
    const val PAID = "Pagado"
    const val EXPIRED = "Vencido"
}

/**
 * Representa una mensualidad guardada en el campo tipo Lista/Array "monthlyFees"
 * dentro del documento del jugador en "players/{playerId}" (US-08).
 */
data class MonthlyFeeModel(
    val id: String = "",
    val playerId: String = "",
    val playerName: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val category: String = "",
    val period: String = "", // Formato "MM/YYYY"
    val amount: Double = 0.0,
    val dueDate: String = "", // Formato "YYYY-MM-DD" o "DD/MM/YYYY"
    val status: String = MonthlyFeeStatus.PENDING,
    val updatedBy: String = "", // Correo/ID del ADM que modificó
    val updatedAt: Timestamp? = null, // Fecha de cambio de estado (ej. a Pagado)
    val createdBy: String = "",
    val createdAt: Timestamp? = null
) {
    @get:Exclude
    val isPaid: Boolean get() = status == MonthlyFeeStatus.PAID

    @get:Exclude
    val isExpired: Boolean get() = status == MonthlyFeeStatus.EXPIRED

    @get:Exclude
    val isPending: Boolean get() = status == MonthlyFeeStatus.PENDING

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "playerId" to playerId,
            "playerName" to playerName,
            "teamId" to teamId,
            "teamName" to teamName,
            "category" to category,
            "period" to period,
            "amount" to amount,
            "dueDate" to dueDate,
            "status" to status,
            "updatedBy" to updatedBy,
            "updatedAt" to updatedAt,
            "createdBy" to createdBy,
            "createdAt" to (createdAt ?: Timestamp.now())
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>, fallbackPlayerId: String = "", fallbackPlayerName: String = "", fallbackTeamId: String = "", fallbackTeamName: String = "", fallbackCategory: String = ""): MonthlyFeeModel {
            return MonthlyFeeModel(
                id = map["id"] as? String ?: "",
                playerId = (map["playerId"] as? String ?: "").ifBlank { fallbackPlayerId },
                playerName = (map["playerName"] as? String ?: "").ifBlank { fallbackPlayerName },
                teamId = (map["teamId"] as? String ?: "").ifBlank { fallbackTeamId },
                teamName = (map["teamName"] as? String ?: "").ifBlank { fallbackTeamName },
                category = (map["category"] as? String ?: "").ifBlank { fallbackCategory },
                period = map["period"] as? String ?: "",
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                dueDate = map["dueDate"] as? String ?: "",
                status = map["status"] as? String ?: MonthlyFeeStatus.PENDING,
                updatedBy = map["updatedBy"] as? String ?: "",
                updatedAt = map["updatedAt"] as? Timestamp,
                createdBy = map["createdBy"] as? String ?: "",
                createdAt = map["createdAt"] as? Timestamp
            )
        }
    }
}
