# Estructura de Firestore de SportPro

La estructura y los correlativos actuales se documentan en [docs/FIRESTORE_SCHEMA.md](docs/FIRESTORE_SCHEMA.md). El inventario siguiente corresponde al esquema anterior.

Los datos se guardan por apartado en colecciones; los registros relacionados con un partido o jugador se guardan como subcolecciones cuando corresponde. La consola de Firestore muestra cada documento y sus campos. El orden visual de los campos lo define la consola y no se puede fijar desde Android Studio.

## Colecciones principales

| Colección | Contenido |
|---|---|
| `users/{uid}` | Cuenta, nombre, correo, teléfono, rol, aceptación de términos y estado activo. |
| `Admins/{documentId}` | Registro de administradores autorizados: datos de contacto, club, creador y estado. |
| `teams/{teamId}` | Equipo, categoría, DT, estado de aprobación y datos de entrenamiento. |
| `players/{playerId}` | Perfil deportivo y personal, equipo, vínculo, contacto y estado. |
| `trainings/{trainingId}` | Sesión de entrenamiento, objetivos, ejercicios, horario y estado. |
| `attendance/{trainingId}` | Asistencia de la sesión y resumen por estado. |
| `matches/{matchId}` | Partido, equipos, horario, marcador, estado y miembros. |
| `announcements/{announcementId}` | Anuncios y destinatarios. |
| `notifications/{notificationId}` | Avisos por usuario, tipo, relación y estado de lectura. |
| `posts/{postId}` | Publicaciones de comunidad y contadores. |
| `reports/{reportId}` | Reportes de contenido. |
| `summaries/{matchId}` | Resumen del partido y aprobación. |
| `event_catalog/{eventId}` | Catálogo configurable de eventos deportivos. |
| `match_events/{eventId}` | Catálogo de eventos globales que usa el sistema. |
| `tryouts/{tryoutId}` | Convocatorias abiertas a pruebas deportivas. |
| `tryoutReports/{reportId}` | Reportes sobre convocatorias abiertas. |
| `dniIndex/{dni}` | Índice auxiliar para evitar repetir DNI. |
| `eventCancellationRequests/{requestId}` | Solicitudes de anulación de eventos. |

## Subcolecciones y datos anidados

- `matches/{matchId}/callups/{playerId}`: convocatoria, equipo propietario y respuesta de asistencia.
- `matches/{matchId}/lineup/{teamId}`: formación, titulares, posiciones y suplentes de cada equipo.
- `matches/{matchId}/events/{eventId}`: eventos del partido.
- `matches/{matchId}/events/{eventId}/history/{historyId}`: auditoría de cambios de evento.
- `players/{playerId}/attendanceHistory/{trainingId}`: historial individual de asistencia.
- `posts/{postId}/comments/{commentId}`: comentarios.
- `tryouts/{tryoutId}/candidates/{candidateId}`: postulantes.
- `players/{playerId}.monthlyFees`: arreglo de mensualidades actualmente embebido en el documento del jugador.

## Fechas y actualización en tiempo real

- `createdAt`, `updatedAt`, `savedAt` y marcas equivalentes deben ser fechas de Firestore (`Timestamp`), preferentemente generadas por el servidor.
- Las fechas de negocio como día del partido o fecha de entrenamiento pueden seguir como texto `dd/MM/yyyy`, junto con la hora como `HH:mm`, porque son datos introducidos y mostrados por la app.
- Las pantallas que necesitan refresco inmediato usan listeners de Firestore (`addSnapshotListener`). En Firebase Console, el indicador “Actualizaciones en tiempo real activadas” confirma que la vista está escuchando cambios.

## Nota de compatibilidad

La colección `Admins` tenía `createdAt` guardado como texto en registros anteriores. Los nuevos registros usarán `Timestamp`; los documentos antiguos no se convierten solos y requieren una migración explícita si se desea un único tipo en todo el histórico.
