package pe.edu.esan.sportpro

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

/**
 * Clase Application de la aplicación SportPro.
 * Configura la caché persistente offline de Cloud Firestore para garantzar funcionamiento 100% gratuito (Plan Spark de Firebase).
 */
class SportProApp : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.USE_FIREBASE_EMULATOR) {
            configureFirebaseEmulator()
            return
        }

        try {
            // Configurar almacenamiento en memoria/caché local ilimitada para lectura/escritura sin conexión
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                        .build()
                )
                .build()

            FirebaseFirestore.getInstance().firestoreSettings = settings
        } catch (_: Exception) {
            // Firestore ya inicializado por el SDK
        }
    }

    private fun configureFirebaseEmulator() {
        FirebaseApp.getApps(this).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }?.delete()
        val options = FirebaseOptions.Builder()
            .setProjectId("demo-sportpro")
            .setApplicationId("1:111111111111:android:1111111111111111")
            .setApiKey("fake-api-key")
            .build()
        val app = FirebaseApp.initializeApp(this, options)
        FirebaseAuth.getInstance(app).useEmulator("10.0.2.2", 9099)
        FirebaseFirestore.getInstance(app).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(com.google.firebase.firestore.MemoryCacheSettings.newBuilder().build())
                .build()
            useEmulator("10.0.2.2", 8080)
        }
    }
}
