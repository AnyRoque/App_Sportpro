package pe.edu.esan.sportpro.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import pe.edu.esan.sportpro.BuildConfig
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * US-23: fotos de las publicaciones de la Comunidad en Supabase Storage
 * (mismo proyecto Supabase que la foto del jugador, US-05).
 * - Bucket público "comunidad-fotos": cualquiera con la URL puede ver la imagen.
 * - Solo se sube con sesión de Firebase y dentro de la carpeta del propio usuario: {uid}/...
 * - La imagen se reduce a máx. 1280 px y se guarda como JPG para ahorrar datos.
 */
object CommunityImageStorage {
    private const val BUCKET = "comunidad-fotos"
    private const val MAX_SIDE = 1280
    private const val MAX_BYTES = 5 * 1024 * 1024

    private fun config(): Pair<String, String> {
        val host = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val key = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()
        check(host.startsWith("https://") && key.isNotBlank()) {
            "Configura SUPABASE_URL y SUPABASE_PUBLISHABLE_KEY en local.properties"
        }
        return host to key
    }

    /** Reduce la imagen y la convierte a JPG. */
    private fun compress(context: Context, uri: Uri): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        check(bounds.outWidth > 0 && bounds.outHeight > 0) { "Selecciona una imagen válida" }

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_SIDE || bounds.outHeight / (sample * 2) >= MAX_SIDE) sample *= 2
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("No se pudo leer la imagen")

        val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(decoded.width, decoded.height))
        val bitmap = if (scale < 1f)
            Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
        else decoded

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        val bytes = out.toByteArray()
        check(bytes.size <= MAX_BYTES) { "La imagen debe pesar máximo 5 MB" }
        return bytes
    }

    /** Sube la foto y devuelve su URL pública para guardarla en posts/{id}.imageUrl. */
    suspend fun upload(context: Context, uri: Uri): String {
        val user = FirebaseAuth.getInstance().currentUser ?: error("Debes iniciar sesión")
        val token = user.getIdToken(false).await().token ?: error("No se pudo obtener el token")
        return withContext(Dispatchers.IO) {
            val (host, key) = config()
            val bytes = compress(context, uri)
            val path = "${user.uid}/post_${System.currentTimeMillis()}.jpg"
            val conn = (URL("$host/storage/v1/object/$BUCKET/$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 30000
                doOutput = true
                setRequestProperty("apikey", key)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "image/jpeg")
            }
            try {
                conn.outputStream.use { it.write(bytes) }
                if (conn.responseCode !in 200..299) {
                    val body = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    error("No se pudo subir la foto (Supabase ${conn.responseCode}) $body")
                }
                "$host/storage/v1/object/public/$BUCKET/$path"
            } finally {
                conn.disconnect()
            }
        }
    }
}
