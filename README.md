# SportPro

Aplicación Android en Kotlin y Jetpack Compose para administrar equipos, jugadores y tutores, entrenamientos, asistencia, partidos y comunidad deportiva. Incluye administración, mensualidades, pruebas deportivas, avisos y resúmenes de partido.

## Abrir y compilar

1. Abre esta carpeta en Android Studio y sincroniza Gradle. Instala SDK 37 y usa el JDK de ejecución configurado por el wrapper y el proyecto.
2. Coloca la configuración Android de Firebase de `pe.edu.esan.sportpro` en `app/google-services.json`. Android Studio configura la ruta del SDK mediante `local.properties`.
3. Si vas a probar fotos y resúmenes, configura `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`, `GEMINI_API_KEY` y opcionalmente `GEMINI_MODEL` en `local.properties`.
4. Ejecuta `gradlew.bat clean assembleDebug test lintDebug` en Windows. La aplicación normal usa el proyecto indicado por `google-services.json`.

## Probar sin datos reales

Arranca Auth y Firestore Emulator con `firebase emulators:start --only auth,firestore --project demo-sportpro`. Ejecuta las pruebas instrumentadas con un emulador Android activo mediante `gradlew.bat connectedDebugAndroidTest -PsportproFirebaseEmulator=true`. Esa variante usa `10.0.2.2` y el proyecto `demo-sportpro`. La prueba automatizada prepara sus propios contadores de demo.

- [Comparación de versiones](COMPARACION_VERSIONES.md)
- [Cambios implementados](CAMBIOS_IMPLEMENTADOS.md)
- [Esquema de Firebase](ESTRUCTURA_FIREBASE.md)
- [Respaldo y migración](MIGRACION_FIREBASE.md)
- [Matriz de pruebas](PRUEBAS_FUNCIONALES.md)
- [Errores corregidos](ERRORES_CORREGIDOS.md)
- [Configuración externa pendiente](PENDIENTES_FIREBASE.md)
