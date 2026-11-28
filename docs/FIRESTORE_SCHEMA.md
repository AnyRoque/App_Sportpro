# Esquema de Firestore

SportPro usa la base `(default)`. Los nombres de las colecciones operativas se conservan. Las lecturas de modelos con correlativos aceptan documentos antiguos y toman la clave de ruta cuando falta el campo `id`.

| Ruta | Almacenamiento |
| --- | --- |
| `users/{uid}` | Perfil y rol ADM, DT, JUG o PAD. El UID sigue siendo la clave de autorización. Los nuevos perfiles guardan `authUid` e `id` de negocio. |
| `Admins/{id}` | Club y ficha del administrador. Alta atómica con su perfil Firestore. |
| `teams/{id}`, `players/{id}` | Equipos, estados de aprobación, vínculo con jugador/tutor y mensualidades embebidas. |
| `matches/{id}` | Programación, equipo, rival, operador, marcador y cronómetro. |
| `matches/{id}/callups/{SP-CON}`, `lineup/{SP-ALI}` | Convocatoria individual y alineación por equipo. Las rutas históricas por jugador, equipo y `starting` se leen como respaldo. |
| `matches/{id}/events/{id}/history/{historyId}` | Eventos con correlativo y bitácora operativa por evento con correlativo HIS en entradas nuevas. |
| `trainings/{id}`, `attendance/{id}` | Sesión e historial grupal. El entrenamiento apunta a la asistencia nueva por `attendanceId`. Antes se usaba el ID del entrenamiento como clave. |
| `players/{id}/attendanceHistory/{trainingId}`, `attendanceHistory/{recordId}` | Proyección por jugador y ruta superior contemplada por reglas anteriores. |
| `announcements/{id}`, `posts/{id}/comments/{id}` | Anuncios, publicaciones, comentarios, reacciones y moderación. |
| `reports/{SP-REP}`, `summaries/{SP-RES}` | Reporte por publicación y resumen único del partido. Las rutas históricas siguen accesibles hasta la migración. |
| `notifications/{id}` | Notificación con destinatario UID y vínculo al contenido. Los avisos secundarios se intentan después de guardar el registro principal. |
| `event_catalog/{id}`, `match_events/{id}` | Catálogo administrable y lectura del catálogo histórico global. |
| `eventCancellationRequests/{id}` | Solicitudes de anulación asociadas a partido y evento. |
| `tryouts/{id}/candidates/{id}`, `tryouts/{id}/candidateOwners/{uid}` | Prueba deportiva, inscripción y clave técnica de unicidad por cuenta. |
| `tryoutReports/{SP-REP}` | Reporte de moderación de prueba deportiva. Comparte contador REP con `reports`. |
| `posts/{id}/reportOwners/{uid}`, `tryouts/{id}/reportOwners/{uid}` | Índices de unicidad por cuenta y contenido. |
| `players/{id}/private/contact` | DNI, nacimiento, contacto, medidas y datos de emergencia. |
| `dniIndex/{dni}` | Índice técnico de unicidad de la ficha de jugador. |
| `counters/{module}`, `auditLogs/{id}`, `feeIndex/{id}` | Contadores, auditoría de mutaciones integradas y reserva de IDs para cuotas embebidas. |

Los formularios conservan la fecha y hora de presentación. Los partidos y entrenamientos creados o editados incorporan `matchDate` o `trainingDate` como Timestamp cuando la fecha y la hora admiten conversión en la zona America/Lima. El historial de fechas anteriores se conserva sin reinterpretar.

Las fotos están en Supabase Storage, en el bucket `jugadores-fotos` y una carpeta ligada al UID. Los enlaces y rutas del historial se preservan. Para consultas, el código actual utiliza filtros de igualdad, pertenencia a listas u ordenamiento individual. Revisa en el entorno de prueba cualquier consulta que el backend marque como necesitada de un índice compuesto.
