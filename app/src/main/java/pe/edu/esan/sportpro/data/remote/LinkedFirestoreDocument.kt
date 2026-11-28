package pe.edu.esan.sportpro.data.remote

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T : Any> DocumentReference.observeLinkedDocument(
    field: String,
    collection: CollectionReference,
    fallback: String,
    type: Class<T>
): Flow<T?> = callbackFlow {
    val listener = addSnapshotListener { snapshot, failure ->
        if (failure != null) close(failure)
        else trySend(snapshot?.getString(field) ?: fallback)
    }
    awaitClose { listener.remove() }
}.distinctUntilChanged().flatMapLatest { id ->
    callbackFlow {
        val listener = collection.document(id).addSnapshotListener { snapshot, failure ->
            if (failure != null) close(failure)
            else trySend(snapshot?.toObject(type))
        }
        awaitClose { listener.remove() }
    }
}
