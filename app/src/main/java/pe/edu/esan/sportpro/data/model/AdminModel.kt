package pe.edu.esan.sportpro.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp

/**
 * Modelo para la colección de Administradores Autorizados ("Admins").
 */
data class AdminModel(
    val id: String = "",
    val name: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val club: String = "",
    val role: String = Role.ADM.name,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    val active: Boolean = true
) {
    @get:Exclude
    val fullName: String get() = "$name $lastName".trim()
}
