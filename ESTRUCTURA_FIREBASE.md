# Estructura de Firebase

El Android application ID es `pe.edu.esan.sportpro`. Los archivos locales de ambas versiones indican el mismo proyecto Firebase, `sportpro-73035`. Se usan Firebase Authentication y Cloud Firestore. Las fotos usan Supabase Storage, los avisos internos son documentos Firestore y el resumen usa Gemini mediante REST.

| Datos | Ruta y referencias |
| --- | --- |
| Autenticación y roles | `users/{uid}` con UID de Auth, `authUid`, ID de negocio, rol y estado. Los perfiles ADM se relacionan con `Admins/{id}`. |
| Equipos y planteles | `teams/{id}.dtUid` y `players/{id}` con `teamId`, `dtUid`, `userUid` y `parentUid`. Contacto en `players/{id}/private/contact`. Las cuentas JUG y PAD crean `teams/{id}/members/{uid}` con el ID de su ficha aceptada. Las reglas comprueban el equipo, el UID y el estado de la ficha al leer entrenamientos. |
| Partidos | `matches/{id}` con equipo, DT, `rivalTeamId` cuando el rival está registrado, nombre del rival para equipos externos, operador, miembros, marcador y cronómetro. Las subcolecciones `callups`, `lineup` y `events` contienen los detalles. |
| Entrenamiento | `trainings/{id}` con equipo y DT. `attendance/{id}` contiene la sesión grupal y `players/{id}/attendanceHistory/{trainingId}` mantiene la proyección individual. |
| Otros módulos | `announcements`, `notifications`, `posts`, `reports`, `tryouts`, `tryoutReports`, `summaries`, `event_catalog`, `match_events` y `eventCancellationRequests`. |
| Rutas técnicas | `counters`, `auditLogs`, `dniIndex`, `feeIndex` y subcolecciones de unicidad de solicitudes y reportes. |

Se conserva el esquema existente para que coincida con las consultas y las referencias históricas. Los IDs nuevos siguen el formato de 24-11, por ejemplo `SP-ADM-000001`, `SP-DT-000001`, `SP-JUG-000001`, `SP-EQU-000001`, `SP-PAR-000001` y `SP-ENT-000001`. Los UID y las claves técnicas de enlace siguen siendo independientes. Los contadores requieren inicialización controlada.

Consulta el [esquema de rutas](docs/FIRESTORE_SCHEMA.md), la [nomenclatura completa](docs/ID_NAMING_CONVENTION.md), las [relaciones](docs/ENTITY_RELATIONSHIPS.md) y el [diccionario de campos](docs/DATA_DICTIONARY.md).
