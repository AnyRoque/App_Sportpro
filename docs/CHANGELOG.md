# Cambios de Firestore

- Correlativos transaccionales para los flujos de creación integrados, con IDs de cuenta separados de Firebase Auth.
- Alta ADM en una sola transacción de Firestore para ficha, perfil y auditoría. La creación de la cuenta Auth sigue siendo una operación separada.
- Asistencia grupal con correlativo, vínculo al entrenamiento y escrituras conjuntas del historial individual.
- Correlativo CUO y validación de período dentro de la transacción de alta de mensualidades. Los montos, estados y campos existentes permanecen embebidos.
- Auditoría general para las creaciones principales y actualizaciones integradas. Conservación de la bitácora especializada de eventos.
- Reglas locales, configuración del emulador, script PowerShell para inventario y ensayo de migración, y pruebas unitarias de formato y límites.

## Segunda etapa

- CON, ALI, REP y RES en rutas de negocio nuevas. Los índices de cada partido y de cada reportante conservan las búsquedas y la unicidad.
- Contactos personales de jugadores en una subcolección con permisos propios. Los jugadores y tutores leen asistencias individuales desde el detalle de la sesión.
- El plan de migración incorpora los cuatro correlativos, los vínculos de partido, los índices de reporte y los perfiles privados. También prepara las proyecciones de asistencia que falten en la copia de ensayo.
- Archivos nuevos: `LinkedFirestoreDocument.kt`. Actualización de `MatchRepository.kt`, `CommunityRepository.kt`, `SummaryRepository.kt`, `TryoutRepository.kt`, `PlayerRepository.kt`, `AttendanceRepository.kt`, `EventCatalogRepository.kt`, `FirestoreFields.kt` y `SportProIdGenerator.kt`. También se actualizaron los modelos de convocatoria, alineación, reporte y resumen, `TrainingDetailScreen.kt`, `PlayerEditSection.kt`, las reglas, la migración y las pruebas.

## Validación local

- Ensayo ejecutado contra Firestore Emulator con proyectos demo. Se comprobó la copia repetible, la restauración, las referencias y la separación de contactos históricos.
- Se corrigieron el filtro de documentos técnicos del plan y la decodificación UTF-8 de respuestas REST en Windows PowerShell. Se añadió `Test-SportProFirestoreMigration.ps1`.
- El generador vuelve a intentar una reserva simultánea cuando el servidor confirma que el contador avanzó. Las reglas mantienen la protección contra IDs duplicados y el rechazo de escrituras sin permiso.
- Pasaron cinco escenarios de Android contra los emuladores de Auth y Firestore.

## Archivos

- Kotlin nuevos: `SportProIdGenerator.kt` y `FirestoreFields.kt` en `data/remote`.
- Repositorios modificados: administración, anuncios, asistencia, autenticación, comunidad, catálogo de eventos, jugadores, equipos, partidos, entrenamientos, pruebas, resúmenes y mensualidades.
- Modelos modificados: usuario, anuncios/publicaciones/comentarios, asistencia, equipo, jugador, partido, entrenamiento, evento en vivo, bitácora, catálogo, solicitud y prueba/postulante.
- Compilación y arranque modificados: `app/build.gradle.kts` y `SportProApp.kt`. Manifest y política de red de debug añadidos para el emulador.
- Firestore: `firestore.rules` modificado, `firebase.json` y `firestore.indexes.json` añadidos.
- Pruebas y migración: `SportProIdTest.kt`, `FirestoreEmulatorTest.kt`, `Invoke-SportProFirestoreMigration.ps1`, `Test-SportProFirestorePlan.ps1`.
- Documentación: actualización de referencia en `FIRESTORE_SCHEMA.md` y nueve archivos nuevos en `docs/`.
