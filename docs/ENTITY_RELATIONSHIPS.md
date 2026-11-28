# Relaciones

```mermaid
erDiagram
    USERS ||--o| ADMINS : "authUid"
    USERS ||--o{ TEAMS : "dtUid, coachId"
    USERS o|--o{ PLAYERS : "userUid, parentUid"
    TEAMS o|--o{ PLAYERS : teamId
    TEAMS ||--o{ MATCHES : teamId
    TEAMS ||--o{ TRAININGS : teamId
    TRAININGS ||--o| ATTENDANCE : trainingId
    PLAYERS ||--o{ ATTENDANCE_HISTORY : playerId
    ATTENDANCE ||--o{ ATTENDANCE_HISTORY : attendanceId
    MATCHES ||--o{ CALLUPS : matchId
    PLAYERS ||--o{ CALLUPS : playerId
    MATCHES ||--o{ LINEUP : matchId
    TEAMS ||--o{ LINEUP : teamId
    MATCHES ||--o{ EVENTS : matchId
    EVENTS ||--o{ EVENT_HISTORY : eventId
    EVENTS ||--o{ EVENT_CANCELLATION_REQUESTS : eventId
    MATCHES ||--o| SUMMARIES : matchId
    PLAYERS ||--o{ MONTHLY_FEES : embebidas
    POSTS ||--o{ COMMENTS : postId
    POSTS ||--o{ REPORTS : postId
    TEAMS ||--o{ TRYOUTS : teamId
    TRYOUTS ||--o{ CANDIDATES : tryoutId
    TRYOUTS ||--o{ TRYOUT_REPORTS : tryoutId
    USERS ||--o{ NOTIFICATIONS : recipientUid
    USERS ||--o{ AUDIT_LOGS : performedByUid
```

La selección de rival sigue usando el nombre existente cuando el rival es externo. El plantel se consulta por `players.teamId`. Las listas de convocados y titulares siguen el formato del partido. Las cuotas son elementos de `players.monthlyFees`; el estado pagado pertenece al elemento vigente.

La asistencia usa un único documento grupal por sesión. La transacción valida jugadores del equipo, comprueba IDs repetidos y actualiza la proyección por jugador. Cancela el entrenamiento o partido para conservar el historial. Usa la desactivación del jugador y las transiciones del equipo que ofrece la app. La eliminación de pruebas y anuncios conserva la política previa y necesita una decisión adicional para archivar sus dependencias.

Los listeners de partidos, planteles, entrenamientos, comunidad y otros módulos continúan leyendo las colecciones vigentes. El catálogo mantiene el listener y respaldo local existentes.
