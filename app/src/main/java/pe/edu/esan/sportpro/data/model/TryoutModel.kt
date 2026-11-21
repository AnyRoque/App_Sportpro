package pe.edu.esan.sportpro.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Documento tryouts/{tryoutId} (US-10: Convocatoria abierta / aviso de pruebas).
 */
data class TryoutModel(
    @DocumentId var id: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val verifiedTeam: Boolean = true, // Insignia "Equipo verificado"
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val positionsSought: String = "", // ej. "Delantero, Portero, Volante"
    val tryoutDate: String = "", // Formato "YYYY-MM-DD" o "DD/MM/YYYY"
    val tryoutTime: String = "", // Formato "HH:mm" o "10:00 AM"
    val address: String = "",
    val district: String = "",
    val requirements: String = "", // Opcional
    val slots: Int = 0, // Cupos disponibles
    val registrationDeadline: String = "", // Fecha límite de inscripción
    val isFree: Boolean = true, // Convocatoria gratuita
    val dtUid: String = "",
    val dtName: String = "",
    val interestedCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

/**
 * Documento tryouts/{tryoutId}/candidates/{candidateId}.
 * NUNCA almacena ni expone números telefónicos de menores de edad (regla de privacidad).
 */
data class TryoutCandidateModel(
    @DocumentId var id: String = "",
    val tryoutId: String = "",
    val userUid: String = "",
    val name: String = "", // Nombre completo
    val age: Int = 0, // Edad
    val position: String = "", // Posición elegida
    val role: String = "", // "JUG" o "PAD"
    @ServerTimestamp val createdAt: Timestamp? = null
)
