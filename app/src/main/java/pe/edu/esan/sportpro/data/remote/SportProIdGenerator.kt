package pe.edu.esan.sportpro.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.Source
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.tasks.await
import pe.edu.esan.sportpro.data.model.TeamStatus

internal fun requireTeam(tx: Transaction, teams: CollectionReference, id: String, coachUid: String) {
    require(id.isNotBlank()) { "Selecciona un equipo" }
    val team = tx.get(teams.document(id))
    check(team.exists() && team.getString("dtUid") == coachUid &&
        team.getString("status") == TeamStatus.APPROVED) {
        "El equipo debe estar aprobado y pertenecer al director técnico"
    }
}

internal suspend fun CollectionReference.notifyBestEffort(fields: Map<String, Any?>) {
    try {
        SportProIdGenerator(firestore).create(this, SportProEntity.NOTIFICATION, fields, audit = false)
    } catch (error: kotlinx.coroutines.CancellationException) {
        throw error
    } catch (error: Exception) {
        android.util.Log.w("SportPro", "No se pudo guardar la notificación", error)
    }
}

internal enum class SportProEntity(val counter: String, val prefix: String) {
    ADMIN("admins", "ADM"),
    DIRECTOR("directors", "DT"),
    USER("users", "USU"),
    PLAYER("players", "JUG"),
    TEAM("teams", "EQU"),
    MATCH("matches", "PAR"),
    TRAINING("trainings", "ENT"),
    ATTENDANCE("attendance", "ASI"),
    MONTHLY_FEE("monthlyFees", "CUO"),
    EVENT("events", "EVT"),
    GOAL("goals", "GOL"),
    CARD("cards", "TAR"),
    SUBSTITUTION("substitutions", "SUS"),
    TRYOUT("tryouts", "PRU"),
    CANDIDATE("candidates", "POS"),
    ANNOUNCEMENT("announcements", "ANU"),
    POST("posts", "PUB"),
    COMMENT("comments", "COM"),
    NOTIFICATION("notifications", "NOT"),
    EVENT_CATALOG("eventCatalog", "CAT"),
    EVENT_CANCELLATION("eventCancellations", "SOL"),
    EVENT_HISTORY("eventHistory", "HIS"),
    CALL_UP("callups", "CON"),
    LINEUP("lineups", "ALI"),
    REPORT("reports", "REP"),
    SUMMARY("summaries", "RES"),
    AUDIT("auditLogs", "AUD");

    fun id(sequence: Long): String {
        require(sequence in 1..999999) { "Se agotaron los identificadores del módulo $prefix" }
        return "SP-$prefix-${sequence.toString().padStart(6, '0')}"
    }
}

internal class SportProIdGenerator(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val counterReads = ThreadLocal<MutableMap<DocumentReference, Long>>()

    internal class Allocation(
        val reference: DocumentReference,
        private val counter: DocumentReference,
        private val sequence: Long
    ) {
        fun commit(
            transaction: Transaction,
            fields: Map<String, Any?>,
            logicalId: String = reference.id
        ) {
            transaction.set(counter, mapOf("lastId" to sequence, "documentPath" to reference.path, "allocatedId" to logicalId))
            transaction.set(reference, fields + ("id" to logicalId))
        }
    }

    fun allocate(
        transaction: Transaction,
        collection: CollectionReference,
        entity: SportProEntity,
        technicalKey: String? = null
    ): Pair<Allocation, String> {
        val counter = firestore.collection("counters").document(entity.counter)
        val last = readCounter(transaction, counter, entity)
        val next = last + 1
        val id = entity.id(next)
        val reference = collection.document(technicalKey ?: id)
        return Allocation(reference, counter, next) to id
    }

    fun allocateCallUps(
        transaction: Transaction,
        collection: CollectionReference,
        count: Int
    ): Pair<List<DocumentReference>, () -> Unit> {
        require(count in 1..100) { "Selecciona entre 1 y 100 jugadores" }
        val entity = SportProEntity.CALL_UP
        val counter = firestore.collection("counters").document(entity.counter)
        val last = readCounter(transaction, counter, entity)
        val references = (1..count).map { collection.document(entity.id(last + it)) }
        val commit = {
            transaction.set(counter, mapOf(
                "lastId" to last + count,
                "documentPath" to references.last().path,
                "allocatedId" to references.last().id
            ))
            Unit
        }
        return references to commit
    }

    suspend fun create(
        collection: CollectionReference,
        entity: SportProEntity,
        fields: Map<String, Any?>,
        audit: Boolean = true,
        prepare: (Transaction, DocumentReference) -> (() -> Unit) = { _, _ -> {} }
    ): DocumentReference {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: error("Debes iniciar sesión")
        return transact { tx ->
            val (document, _) = allocate(tx, collection, entity)
            val writeRelated = prepare(tx, document.reference)
            val actorId = if (audit) accountId(tx, uid) else ""
            val dtUid = fields["dtUid"] as? String
            val coach = if (dtUid == uid) actorId else ""
            val change = if (audit) prepareAudit(tx, uid, document.reference, "CREATE", emptyList(), actorId) else ({})
            val metadata = creationMetadata(uid).toMutableMap()
            if (actorId.isNotBlank()) {
                metadata["createdBy"] = actorId
                metadata["updatedBy"] = actorId
            }
            if (coach.startsWith("SP-DT-")) metadata["coachId"] = coach
            document.commit(tx, fields + metadata)
            writeRelated()
            change()
            document.reference
        }
    }

    fun creationMetadata(uid: String): Map<String, Any> = mapOf(
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
        "createdByUid" to uid,
        "updatedByUid" to uid
    )

    fun prepareAudit(
        tx: Transaction,
        uid: String,
        target: DocumentReference,
        operation: String,
        changedFields: List<String>,
        actorId: String = accountId(tx, uid)
    ): () -> Unit {
        val (audit, _) = allocate(tx, firestore.collection("auditLogs"), SportProEntity.AUDIT)
        return {
            audit.commit(tx, mapOf(
                "operation" to operation,
                "collection" to target.parent.id,
                "documentId" to target.id,
                "documentPath" to target.path,
                "performedBy" to actorId,
                "performedByUid" to uid,
                "changedFields" to changedFields,
                "timestamp" to FieldValue.serverTimestamp()
            ))
        }
    }

    suspend fun update(
        target: DocumentReference,
        fields: Map<String, Any?>,
        operation: String = "UPDATE"
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: error("Debes iniciar sesión")
        transact { tx ->
            val existing = tx.get(target)
            check(existing.exists()) { "El registro ya no existe" }
            val changes = changedFields(existing, fields)
            if (changes.isNotEmpty()) {
                val actorId = accountId(tx, uid)
                val audit = prepareAudit(tx, uid, target, operation, changes.keys.sorted(), actorId)
                tx.update(target, changes + mapOf(
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "updatedByUid" to uid,
                    "updatedBy" to actorId
                ))
                audit()
            }
        }
    }

    private fun accountId(tx: Transaction, uid: String): String =
        tx.get(firestore.collection("users").document(uid)).getString("id").orEmpty()

    fun changedFields(
        snapshot: com.google.firebase.firestore.DocumentSnapshot,
        fields: Map<String, Any?>
    ): Map<String, Any?> = fields.filter { (name, value) -> !sameFirestoreValue(snapshot.get(name), value) }

    private fun sameFirestoreValue(previous: Any?, next: Any?): Boolean = when {
        previous is Number && next is Number -> previous.toDouble() == next.toDouble()
        previous is Map<*, *> && next is Map<*, *> ->
            previous.keys == next.keys && next.all { (key, value) -> sameFirestoreValue(previous[key], value) }
        previous is List<*> && next is List<*> ->
            previous.size == next.size && next.indices.all { sameFirestoreValue(previous[it], next[it]) }
        else -> previous == next
    }

    private fun readCounter(
        transaction: Transaction,
        reference: DocumentReference,
        entity: SportProEntity
    ): Long {
        val snapshot = try {
            transaction.get(reference)
        } catch (error: FirebaseFirestoreException) {
            if (error.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw error
            throw IllegalStateException(
                "Firestore bloqueó el acceso a counters/${entity.counter}. Revisa las reglas publicadas.",
                error
            )
        }
        val last = snapshot.getLong("lastId")
            ?: error("Falta inicializar counters/${entity.counter} en Firestore.")
        check(last in 0..999999) { "El contador de ${entity.counter} no es válido" }
        counterReads.get()?.put(reference, last)
        return last
    }

    private suspend fun counterAdvanced(reads: Map<DocumentReference, Long>): Boolean {
        return reads.any { (reference, value) ->
            try {
                val current = reference.get(Source.SERVER).await().getLong("lastId")
                current != null && current > value
            } catch (_: FirebaseFirestoreException) {
                false
            }
        }
    }

    suspend fun <T> transact(action: (Transaction) -> T): T {
        val lastReads = AtomicReference<Map<DocumentReference, Long>>(emptyMap())
        var verifiedRetries = 0
        while (true) {
            try {
                return firestore.runTransaction { transaction ->
                    // Firestore ejecuta cada intento en un callback síncrono de su propio hilo.
                    val reads = mutableMapOf<DocumentReference, Long>()
                    counterReads.set(reads)
                    try {
                        action(transaction)
                    } finally {
                        counterReads.remove()
                        lastReads.set(reads.toMap())
                    }
                }.await()
            } catch (error: FirebaseFirestoreException) {
                // Las reglas pueden rechazar un correlativo que ya ganó otro cliente
                // antes de que el SDK reciba un conflicto de versión.
                val confirmedConflict = error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED &&
                    counterAdvanced(lastReads.get())
                if (confirmedConflict && verifiedRetries < 2) {
                    verifiedRetries++
                    delay(50L * verifiedRetries)
                    continue
                }
                val message = when {
                    confirmedConflict || error.code == FirebaseFirestoreException.Code.ABORTED ->
                        "Hubo cambios simultáneos. Vuelve a intentar la operación."
                    error.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                        error.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                        "Se necesita conexión a Internet para guardar. Comprueba el registro antes de reintentar."
                    else -> throw error
                }
                throw IllegalStateException(message, error)
            }
        }
    }
}
