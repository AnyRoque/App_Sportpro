# Diccionario de datos

Los tipos Kotlin corresponden a los valores de Firestore. Las propiedades históricas con fecha de texto se conservan. Los permisos de escritura y las pantallas figuran al inicio de cada tabla. Las filas «Según el flujo» se completan cuando la entidad o acción requiere ese dato.

## `users`

Registro, acceso y Perfil. Responsables de escritura: Titular, ADM. Operaciones: Crear, leer, actualizar.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `uid` | String | UID original de Firebase Authentication | Según el flujo | Auth |
| `name` | String | Nombre | Según el flujo | — |
| `lastName` | String | Apellidos | Opcional o con valor inicial | — |
| `email` | String | Correo de contacto | Opcional o con valor inicial | — |
| `phone` | String | Teléfono de contacto | Opcional o con valor inicial | — |
| `role` | String | Rol de acceso | Según el flujo | — |
| `birthDate` | String | Fecha de nacimiento del formulario | Opcional o con valor inicial | — |
| `acceptedTerms` | Boolean | Consentimiento de términos | Opcional o con valor inicial | — |
| `active` | Boolean | Registro activo | Opcional o con valor inicial | — |

## `Admins`

Administración > Alta de administrador. Responsables de escritura: ADM. Operaciones: Crear, leer, actualizar.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Correlativo SP cuando se migra la colección |
| `name` | String | Nombre | Según el flujo | — |
| `lastName` | String | Apellidos | Opcional o con valor inicial | — |
| `email` | String | Correo de contacto | Opcional o con valor inicial | — |
| `phone` | String | Teléfono de contacto | Opcional o con valor inicial | — |
| `club` | String | Club | Opcional o con valor inicial | — |
| `role` | String | Rol de acceso | Según el flujo | — |
| `createdBy` | String | Responsable de creación | Opcional o con valor inicial | users |
| `createdAt` | Timestamp? | Fecha de creación | Opcional o con valor inicial | — |
| `active` | Boolean | Registro activo | Opcional o con valor inicial | — |

## `teams`

Equipos y Aprobación. Responsables de escritura: DT propietario, ADM. Operaciones: Crear, leer, revisar.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Correlativo SP cuando se migra la colección |
| `name` | String | Nombre | Según el flujo | — |
| `nameKey` | String | Name Key | Opcional o con valor inicial | — |
| `category` | String | Categoría deportiva | Opcional o con valor inicial | — |
| `field` | String | Field | Opcional o con valor inicial | — |
| `district` | String | District | Opcional o con valor inicial | — |
| `trainingDays` | List<String> | Training Days | Opcional o con valor inicial | — |
| `trainingTime` | String | Training Time | Opcional o con valor inicial | — |
| `dtUid` | String | UID del director técnico | Según el flujo | users/{uid} |
| `dtName` | String | Nombre del director técnico | Opcional o con valor inicial | — |
| `status` | String | Estado del proceso | Opcional o con valor inicial | — |
| `rejectReason` | String | Reject Reason | Opcional o con valor inicial | — |
| `reviewedBy` | String | Reviewed By | Opcional o con valor inicial | — |
| `reviewedAt` | Timestamp? | Reviewed At | Opcional o con valor inicial | — |
| `createdAt` | Timestamp? | Fecha de creación | Opcional o con valor inicial | — |

## `players`

Plantel, Unirme, Hijos y Perfil. Responsables de escritura: DT propietario, ADM, JUG/PAD vinculados. Operaciones: Crear, leer, editar, asignar, desactivar.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Correlativo SP cuando se migra la colección |
| `teamId` | String | Equipo relacionado | Opcional o con valor inicial | teams |
| `teamName` | String | Nombre del equipo | Opcional o con valor inicial | — |
| `category` | String | Categoría deportiva | Opcional o con valor inicial | — |
| `name` | String | Nombre | Según el flujo | — |
| `lastName` | String | Apellidos | Opcional o con valor inicial | — |
| `birthDate` | String | Fecha de nacimiento del formulario | Opcional o con valor inicial | — |
| `dni` | String | Documento de identidad reservado en el índice | Opcional o con valor inicial | — |
| `sex` | String | Sex | Opcional o con valor inicial | — |
| `photoUrl` | String | URL de foto existente | Opcional o con valor inicial | — |
| `position` | String | Posición deportiva | Opcional o con valor inicial | — |
| `shirtNumber` | Int? | Shirt Number | Opcional o con valor inicial | — |
| `heightCm` | Int? | Height Cm | Opcional o con valor inicial | — |
| `weightKg` | Int? | Weight Kg | Opcional o con valor inicial | — |
| `foot` | String | Foot | Opcional o con valor inicial | — |
| `phone` | String | Teléfono de contacto | Opcional o con valor inicial | — |
| `email` | String | Correo de contacto | Opcional o con valor inicial | — |
| `emergencyName` | String | Nombre del contacto de emergencia | Opcional o con valor inicial | — |
| `emergencyRelation` | String | Emergency Relation | Opcional o con valor inicial | — |
| `emergencyPhone` | String | Teléfono de emergencia | Opcional o con valor inicial | — |
| `parentUid` | String | UID del tutor | Opcional o con valor inicial | users/{uid} |
| `parentRelation` | String | Parent Relation | Opcional o con valor inicial | — |
| `userUid` | String | UID del jugador o participante | Opcional o con valor inicial | users/{uid} |
| `dtUid` | String | UID del director técnico | Según el flujo | users/{uid} |
| `linkStatus` | String | Estado de aprobación del vínculo | Opcional o con valor inicial | — |
| `active` | Boolean | Registro activo | Opcional o con valor inicial | — |
| `createdBy` | String | Responsable de creación | Opcional o con valor inicial | users |
| `createdAt` | Timestamp? | Fecha de creación | Opcional o con valor inicial | — |

## `matches`

Partidos, detalle y consola. Responsables de escritura: DT propietario, DT miembro, operador, ADM. Operaciones: Crear, leer, editar, cancelar.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Correlativo SP cuando se migra la colección |
| `dtUid` | String | UID del director técnico | Según el flujo | users/{uid} |
| `dtName` | String | Nombre del director técnico | Opcional o con valor inicial | — |
| `teamId` | String | Equipo relacionado | Opcional o con valor inicial | teams |
| `teamName` | String | Nombre del equipo | Opcional o con valor inicial | — |
| `category` | String | Categoría deportiva | Opcional o con valor inicial | — |
| `rival` | String | Rival | Opcional o con valor inicial | — |
| `matchType` | String | Match Type | Opcional o con valor inicial | — |
| `condition` | String | Condition | Opcional o con valor inicial | — |
| `modality` | Int | Modality | Opcional o con valor inicial | — |
| `periodMinutes` | Int | Period Minutes | Opcional o con valor inicial | — |
| `date` | String | Fecha de presentación | Opcional o con valor inicial | — |
| `time` | String | Time | Opcional o con valor inicial | — |
| `location` | String | Location | Opcional o con valor inicial | — |
| `notes` | String | Notes | Opcional o con valor inicial | — |
| `operatorUid` | String | UID del operador del encuentro | Opcional o con valor inicial | — |
| `operatorName` | String | Operator Name | Opcional o con valor inicial | — |
| `memberUids` | List<String> | Member Uids | Opcional o con valor inicial | — |
| `status` | String | Estado del proceso | Opcional o con valor inicial | — |
| `cancelReason` | String | Cancel Reason | Opcional o con valor inicial | — |
| `hasCallUp` | Boolean | Has Call Up | Opcional o con valor inicial | — |
| `callUpDeadlineDate` | String | Call Up Deadline Date | Opcional o con valor inicial | — |
| `callUpDeadlineTime` | String | Call Up Deadline Time | Opcional o con valor inicial | — |
| `formation` | String | Formation | Opcional o con valor inicial | — |
| `hasLineup` | Boolean | Has Lineup | Opcional o con valor inicial | — |
| `score` | Map<String, Int> | Marcador propio y rival | Opcional o con valor inicial | — |
| `timerStartTime` | Timestamp? | Timer Start Time | Opcional o con valor inicial | — |
| `timerAccumulatedSeconds` | Long | Timer Accumulated Seconds | Opcional o con valor inicial | — |
| `timerIsRunning` | Boolean | Timer Is Running | Opcional o con valor inicial | — |
| `currentPeriod` | Int | Current Period | Opcional o con valor inicial | — |
| `timerPauseTime` | Timestamp? | Timer Pause Time | Opcional o con valor inicial | — |
| `createdAt` | Timestamp? | Fecha de creación | Opcional o con valor inicial | — |

## `trainings`

Entrenamientos. Responsables de escritura: DT propietario, ADM. Operaciones: Crear, leer, actualizar, cancelar.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Correlativo SP cuando se migra la colección |
| `dtUid` | String | UID del director técnico | Según el flujo | users/{uid} |
| `dtName` | String | Nombre del director técnico | Opcional o con valor inicial | — |
| `teamId` | String | Equipo relacionado | Opcional o con valor inicial | teams |
| `teamName` | String | Nombre del equipo | Opcional o con valor inicial | — |
| `category` | String | Categoría deportiva | Opcional o con valor inicial | — |
| `date` | String | Fecha de presentación | Opcional o con valor inicial | — |
| `startTime` | String | Start Time | Opcional o con valor inicial | — |
| `durationMinutes` | Int | Duration Minutes | Opcional o con valor inicial | — |
| `location` | String | Location | Opcional o con valor inicial | — |
| `objectiveType` | String | Objective Type | Opcional o con valor inicial | — |
| `objectiveDescription` | String | Objective Description | Opcional o con valor inicial | — |
| `exercises` | List<ExerciseModel> | Exercises | Opcional o con valor inicial | — |
| `status` | String | Estado del proceso | Opcional o con valor inicial | — |
| `cancelReason` | String | Cancel Reason | Opcional o con valor inicial | — |
| `duplicatedFromId` | String | Duplicated From Id | Opcional o con valor inicial | — |
| `createdAt` | Timestamp? | Fecha de creación | Opcional o con valor inicial | — |

## `trainings.exercises`

Formulario de entrenamiento. Responsables de escritura: DT propietario, ADM. Operaciones: Editar dentro de la sesión.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Correlativo SP cuando se migra la colección |
| `name` | String | Nombre | Según el flujo | — |
| `description` | String | Descripción del formulario | Opcional o con valor inicial | — |
| `type` | String | Clasificación del registro | Opcional o con valor inicial | — |
| `durationMinutes` | Int | Duration Minutes | Opcional o con valor inicial | — |

## `notifications`

Avisos generados desde repositorios. Responsables de escritura: Usuario autenticado y destinatario. Operaciones: Crear, leer, marcar, eliminar.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Correlativo SP cuando se migra la colección |
| `recipientUid` | String | UID del destinatario | Según el flujo | users/{uid} |
| `teamId` | String | Equipo relacionado | Opcional o con valor inicial | teams |
| `title` | String | Title | Según el flujo | — |
| `message` | String | Message | Opcional o con valor inicial | — |
| `type` | String | Clasificación del registro | Opcional o con valor inicial | — |
| `relatedId` | String | Registro asociado según el tipo de aviso | Opcional o con valor inicial | — |
| `read` | Boolean | Read | Opcional o con valor inicial | — |
| `createdAt` | Timestamp? | Fecha de creación | Opcional o con valor inicial | — |

## `attendance`

Entrenamientos > Asistencia. Responsables de escritura: DT de la sesión, ADM. Operaciones: Crear, consultar, corregir.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Correlativo SP cuando se migra la colección |
| `trainingId` | String | Sesión relacionada | Según el flujo | trainings |
| `teamId` | String | Equipo relacionado | Opcional o con valor inicial | teams |
| `teamName` | String | Nombre del equipo | Opcional o con valor inicial | — |
| `category` | String | Categoría deportiva | Opcional o con valor inicial | — |
| `dtUid` | String | UID del director técnico | Según el flujo | users/{uid} |
| `dtName` | String | Nombre del director técnico | Opcional o con valor inicial | — |
| `date` | String | Fecha de presentación | Opcional o con valor inicial | — |
| `items` | List<PlayerAttendanceItem> | Items | Opcional o con valor inicial | — |
| `totalAttended` | Int | Total Attended | Opcional o con valor inicial | — |
| `totalLate` | Int | Total Late | Opcional o con valor inicial | — |
| `totalAbsent` | Int | Total Absent | Opcional o con valor inicial | — |
| `totalJustified` | Int | Total Justified | Opcional o con valor inicial | — |
| `editedBy` | String | Edited By | Opcional o con valor inicial | — |
| `editedAt` | Timestamp? | Edited At | Opcional o con valor inicial | — |
| `createdAt` | Timestamp? | Fecha de creación | Opcional o con valor inicial | — |

## `attendance.items`

Entrenamientos > Asistencia. Responsables de escritura: DT de la sesión, ADM. Operaciones: Crear y editar dentro de la sesión.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `playerId` | String | Ficha de jugador vinculada | Según el flujo | players |
| `playerName` | String | Player Name | Opcional o con valor inicial | — |
| `photoUrl` | String | URL de foto existente | Opcional o con valor inicial | — |
| `shirtNumber` | Int? | Shirt Number | Opcional o con valor inicial | — |
| `position` | String | Posición deportiva | Opcional o con valor inicial | — |
| `status` | String | Estado del proceso | Opcional o con valor inicial | — |
| `justificationReason` | String | Justification Reason | Opcional o con valor inicial | — |

## `players/{id}/attendanceHistory`

Historial personal. Responsables de escritura: DT de la sesión, ADM. Operaciones: Proyectar y leer.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Correlativo SP cuando se migra la colección |
| `trainingId` | String | Sesión relacionada | Según el flujo | trainings |
| `teamId` | String | Equipo relacionado | Opcional o con valor inicial | teams |
| `teamName` | String | Nombre del equipo | Opcional o con valor inicial | — |
| `date` | String | Fecha de presentación | Opcional o con valor inicial | — |
| `status` | String | Estado del proceso | Opcional o con valor inicial | — |
| `justificationReason` | String | Justification Reason | Opcional o con valor inicial | — |
| `recordedAt` | Timestamp? | Recorded At | Opcional o con valor inicial | — |

## `matches/{id}/callups`

Convocatoria y respuesta. Responsables de escritura: Equipo técnico, JUG/PAD vinculados. Operaciones: Crear, responder, retirar.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Clave vigente de la relación |
| `matchId` | String | Partido relacionado | Según el flujo | matches |
| `teamId` | String | Equipo relacionado | Opcional o con valor inicial | teams |
| `playerId` | String | Ficha de jugador vinculada | Según el flujo | players |
| `playerName` | String | Player Name | Opcional o con valor inicial | — |
| `playerPhotoUrl` | String | Player Photo Url | Opcional o con valor inicial | — |
| `position` | String | Posición deportiva | Opcional o con valor inicial | — |
| `isMinor` | Boolean | Is Minor | Opcional o con valor inicial | — |
| `isStarter` | Boolean | Is Starter | Opcional o con valor inicial | — |
| `userUid` | String | UID del jugador o participante | Opcional o con valor inicial | users/{uid} |
| `parentUid` | String | UID del tutor | Opcional o con valor inicial | users/{uid} |
| `responseStatus` | String | Response Status | Opcional o con valor inicial | — |
| `rejectReason` | String | Reject Reason | Opcional o con valor inicial | — |
| `rejectComment` | String | Reject Comment | Opcional o con valor inicial | — |
| `deadlineDate` | String | Deadline Date | Opcional o con valor inicial | — |
| `deadlineTime` | String | Deadline Time | Opcional o con valor inicial | — |
| `createdAt` | Timestamp? | Fecha de creación | Opcional o con valor inicial | — |
| `updatedAt` | Timestamp? | Última actualización | Opcional o con valor inicial | — |

## `matches/{id}/lineup`

Alineación. Responsables de escritura: DT del equipo, ADM. Operaciones: Crear, leer, editar.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Clave vigente de la relación |
| `matchId` | String | Partido relacionado | Según el flujo | matches |
| `teamId` | String | Equipo relacionado | Opcional o con valor inicial | teams |
| `teamName` | String | Nombre del equipo | Opcional o con valor inicial | — |
| `formation` | String | Formation | Opcional o con valor inicial | — |
| `modality` | Int | Modality | Opcional o con valor inicial | — |
| `positions` | List<TacticalPosition> | Positions | Opcional o con valor inicial | — |
| `substitutePlayerIds` | List<String> | Substitute Player Ids | Opcional o con valor inicial | — |
| `savedBy` | String | Saved By | Opcional o con valor inicial | — |
| `savedAt` | Timestamp? | Saved At | Opcional o con valor inicial | — |

## `matches/{id}/lineup (posiciones)`

Alineación. Responsables de escritura: DT del equipo. Operaciones: Formación y posiciones.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `slotIndex` | Int | Slot Index | Opcional o con valor inicial | — |
| `positionName` | String | Position Name | Opcional o con valor inicial | — |
| `xRatio` | Float | X Ratio | Opcional o con valor inicial | — |
| `yRatio` | Float | Y Ratio | Opcional o con valor inicial | — |
| `playerId` | String? | Ficha de jugador vinculada | Según el flujo | players |
| `playerName` | String? | Player Name | Opcional o con valor inicial | — |
| `shirtNumber` | Int? | Shirt Number | Opcional o con valor inicial | — |

## `matches/{id}/events`

Consola del partido. Responsables de escritura: Equipo técnico, ADM. Operaciones: Crear, leer, corregir, anular.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Correlativo SP cuando se migra la colección |
| `matchId` | String | Partido relacionado | Según el flujo | matches |
| `type` | String | Clasificación del registro | Opcional o con valor inicial | — |
| `minute` | Int | Minute | Opcional o con valor inicial | — |
| `minutoCorregido` | Boolean | Minuto Corregido | Opcional o con valor inicial | — |
| `team` | String | Team | Opcional o con valor inicial | — |
| `playerId` | String? | Ficha de jugador vinculada | Según el flujo | players |
| `playerName` | String | Player Name | Opcional o con valor inicial | — |
| `jugadorNoIdentificado` | Boolean | Jugador No Identificado | Opcional o con valor inicial | — |
| `jugadorSaleId` | String? | Jugador Sale Id | Opcional o con valor inicial | — |
| `jugadorSaleName` | String | Jugador Sale Name | Opcional o con valor inicial | — |
| `jugadorEntraId` | String? | Jugador Entra Id | Opcional o con valor inicial | — |
| `jugadorEntraName` | String | Jugador Entra Name | Opcional o con valor inicial | — |
| `observation` | String | Observation | Opcional o con valor inicial | — |
| `registeredBy` | String | UID del operador | Opcional o con valor inicial | — |
| `registeredByName` | String | Registered By Name | Opcional o con valor inicial | — |
| `status` | String | Estado del proceso | Opcional o con valor inicial | — |
| `pendingSync` | Boolean | Pending Sync | Opcional o con valor inicial | — |
| `registeredAt` | Timestamp? | Fecha de registro del evento | Opcional o con valor inicial | — |

## `matches/{id}/events/{id}/history`

Bitácora de eventos. Responsables de escritura: Equipo técnico, ADM. Operaciones: Crear y leer.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Clave vigente de la relación |
| `eventId` | String | Event Id | Opcional o con valor inicial | events |
| `matchId` | String | Partido relacionado | Según el flujo | matches |
| `tipoCambio` | String | Tipo Cambio | Opcional o con valor inicial | — |
| `usuarioUid` | String | Usuario Uid | Opcional o con valor inicial | — |
| `usuarioNombre` | String | Usuario Nombre | Opcional o con valor inicial | — |
| `motivo` | String | Motivo | Opcional o con valor inicial | — |
| `valorOriginal` | String | Valor Original | Opcional o con valor inicial | — |
| `valorNuevo` | String | Valor Nuevo | Opcional o con valor inicial | — |
| `fechaHora` | Timestamp? | Fecha Hora | Opcional o con valor inicial | — |

## `eventCancellationRequests`

Consola y Moderación de anulaciones. Responsables de escritura: Solicitante y ADM. Operaciones: Crear, leer, resolver.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Correlativo SP cuando se migra la colección |
| `matchId` | String | Partido relacionado | Según el flujo | matches |
| `matchTitle` | String | Match Title | Opcional o con valor inicial | — |
| `eventId` | String | Event Id | Opcional o con valor inicial | events |
| `eventSummary` | String | Event Summary | Opcional o con valor inicial | — |
| `requestingDtUid` | String | Requesting Dt Uid | Opcional o con valor inicial | — |
| `requestingDtName` | String | Requesting Dt Name | Opcional o con valor inicial | — |
| `requestingTeamName` | String | Requesting Team Name | Opcional o con valor inicial | — |
| `justification` | String | Justification | Opcional o con valor inicial | — |
| `status` | String | Estado del proceso | Opcional o con valor inicial | — |
| `adminResponseComment` | String | Admin Response Comment | Opcional o con valor inicial | — |
| `createdAt` | Timestamp? | Fecha de creación | Opcional o con valor inicial | — |
| `resolvedAt` | Timestamp? | Resolved At | Opcional o con valor inicial | — |

## `event_catalog`

Administración > Catálogo de eventos. Responsables de escritura: ADM. Operaciones: Crear, leer, editar, activar.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Correlativo SP cuando se migra la colección |
| `name` | String | Nombre | Según el flujo | — |
| `icon` | String | Icon | Opcional o con valor inicial | — |
| `colorHex` | String | Color Hex | Opcional o con valor inicial | — |
| `requiresTeam` | Boolean | Requires Team | Opcional o con valor inicial | — |
| `requiresPlayer` | Boolean | Requires Player | Opcional o con valor inicial | — |
| `requiresSecondPlayer` | Boolean | Requires Second Player | Opcional o con valor inicial | — |
| `affectsScore` | Boolean | Affects Score | Opcional o con valor inicial | — |
| `accumulatesStat` | String | Accumulates Stat | Opcional o con valor inicial | — |
| `active` | Boolean | Registro activo | Opcional o con valor inicial | — |
| `systemDefault` | Boolean | Evento base del sistema | Opcional o con valor inicial | — |
| `createdAt` | Timestamp? | Fecha de creación | Opcional o con valor inicial | — |

## `tryouts`

Pruebas deportivas. Responsables de escritura: DT propietario. Operaciones: Crear, leer, editar, eliminar.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Correlativo SP cuando se migra la colección |
| `teamId` | String | Equipo relacionado | Opcional o con valor inicial | teams |
| `teamName` | String | Nombre del equipo | Opcional o con valor inicial | — |
| `verifiedTeam` | Boolean | Verified Team | Opcional o con valor inicial | — |
| `title` | String | Title | Según el flujo | — |
| `description` | String | Descripción del formulario | Opcional o con valor inicial | — |
| `category` | String | Categoría deportiva | Opcional o con valor inicial | — |
| `positionsSought` | String | Positions Sought | Opcional o con valor inicial | — |
| `tryoutDate` | String | Tryout Date | Opcional o con valor inicial | — |
| `tryoutTime` | String | Tryout Time | Opcional o con valor inicial | — |
| `address` | String | Address | Opcional o con valor inicial | — |
| `district` | String | District | Opcional o con valor inicial | — |
| `requirements` | String | Requirements | Opcional o con valor inicial | — |
| `slots` | Int | Cupos de la prueba | Opcional o con valor inicial | — |
| `registrationDeadline` | String | Registration Deadline | Opcional o con valor inicial | — |
| `free` | Boolean | Indicador de gratuidad | Opcional o con valor inicial | — |
| `dtUid` | String | UID del director técnico | Según el flujo | users/{uid} |
| `dtName` | String | Nombre del director técnico | Opcional o con valor inicial | — |
| `interestedCount` | Int | Total de inscripciones | Opcional o con valor inicial | — |
| `createdAt` | Timestamp? | Fecha de creación | Opcional o con valor inicial | — |

## `tryouts/{id}/candidates`

Me interesa y Postulantes. Responsables de escritura: JUG/PAD titular, DT propietario, ADM. Operaciones: Inscribirse y consultar.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Correlativo SP cuando se migra la colección |
| `tryoutId` | String | Prueba deportiva relacionada | Según el flujo | tryouts |
| `userUid` | String | UID del jugador o participante | Opcional o con valor inicial | users/{uid} |
| `name` | String | Nombre | Según el flujo | — |
| `age` | Int | Age | Opcional o con valor inicial | — |
| `position` | String | Posición deportiva | Opcional o con valor inicial | — |
| `role` | String | Rol de acceso | Según el flujo | — |
| `createdAt` | Timestamp? | Fecha de creación | Opcional o con valor inicial | — |

## `announcements`

Anuncios. Responsables de escritura: DT/ADM, receptor autenticado. Operaciones: Crear, leer, editar, marcar lectura.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Correlativo SP cuando se migra la colección |
| `title` | String | Title | Según el flujo | — |
| `description` | String | Descripción del formulario | Opcional o con valor inicial | — |
| `type` | String | Clasificación del registro | Opcional o con valor inicial | — |
| `teamId` | String | Equipo relacionado | Opcional o con valor inicial | teams |
| `teamName` | String | Nombre del equipo | Opcional o con valor inicial | — |
| `targetRoles` | List<String> | Target Roles | Opcional o con valor inicial | — |
| `validUntil` | String | Valid Until | Opcional o con valor inicial | — |
| `authorUid` | String | UID del autor | Según el flujo | — |
| `authorName` | String | Nombre visible del autor | Opcional o con valor inicial | — |
| `authorRole` | String | Rol mostrado para el autor | Opcional o con valor inicial | — |
| `edited` | Boolean | Edited | Opcional o con valor inicial | — |
| `readBy` | List<String> | Read By | Opcional o con valor inicial | — |
| `createdAt` | Timestamp? | Fecha de creación | Opcional o con valor inicial | — |

## `posts`

Comunidad y Moderación. Responsables de escritura: Autor, otros usuarios según operación, ADM. Operaciones: Publicar, leer, reaccionar, moderar.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Correlativo SP cuando se migra la colección |
| `text` | String | Text | Opcional o con valor inicial | — |
| `imageUrl` | String | Image Url | Opcional o con valor inicial | — |
| `visibility` | String | Visibility | Opcional o con valor inicial | — |
| `teamId` | String | Equipo relacionado | Opcional o con valor inicial | teams |
| `teamName` | String | Nombre del equipo | Opcional o con valor inicial | — |
| `authorUid` | String | UID del autor | Según el flujo | — |
| `authorName` | String | Nombre visible del autor | Opcional o con valor inicial | — |
| `authorRole` | String | Rol mostrado para el autor | Opcional o con valor inicial | — |
| `reactions` | Map<String, String> | Reactions | Opcional o con valor inicial | — |
| `commentCount` | Int | Comment Count | Opcional o con valor inicial | — |
| `reportCount` | Int | Report Count | Opcional o con valor inicial | — |
| `status` | String | Estado del proceso | Opcional o con valor inicial | — |
| `createdAt` | Timestamp? | Fecha de creación | Opcional o con valor inicial | — |

## `posts/{id}/comments`

Comentarios. Responsables de escritura: Autor, ADM. Operaciones: Crear, leer, eliminar.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Correlativo SP cuando se migra la colección |
| `postId` | String | Publicación relacionada | Según el flujo | posts |
| `text` | String | Text | Opcional o con valor inicial | — |
| `authorUid` | String | UID del autor | Según el flujo | — |
| `authorName` | String | Nombre visible del autor | Opcional o con valor inicial | — |
| `authorRole` | String | Rol mostrado para el autor | Opcional o con valor inicial | — |
| `createdAt` | Timestamp? | Fecha de creación | Opcional o con valor inicial | — |

## `reports`

Reportar y Moderación. Responsables de escritura: Reportante, ADM. Operaciones: Crear, leer, resolver.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Clave vigente de la relación |
| `postId` | String | Publicación relacionada | Según el flujo | posts |
| `postText` | String | Post Text | Opcional o con valor inicial | — |
| `postAuthorUid` | String | Post Author Uid | Opcional o con valor inicial | — |
| `postAuthorName` | String | Post Author Name | Opcional o con valor inicial | — |
| `reason` | String | Reason | Opcional o con valor inicial | — |
| `detail` | String | Detail | Opcional o con valor inicial | — |
| `reporterUid` | String | Reporter Uid | Opcional o con valor inicial | — |
| `status` | String | Estado del proceso | Opcional o con valor inicial | — |
| `action` | String | Action | Opcional o con valor inicial | — |
| `resolvedBy` | String | Resolved By | Opcional o con valor inicial | — |
| `resolvedAt` | Timestamp? | Resolved At | Opcional o con valor inicial | — |
| `createdAt` | Timestamp? | Fecha de creación | Opcional o con valor inicial | — |

## `summaries`

Resumen IA. Responsables de escritura: DT del partido. Operaciones: Generar, editar, aprobar, leer.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Clave vigente de la relación |
| `matchId` | String | Partido relacionado | Según el flujo | matches |
| `teamId` | String | Equipo relacionado | Opcional o con valor inicial | teams |
| `teamName` | String | Nombre del equipo | Opcional o con valor inicial | — |
| `rival` | String | Rival | Opcional o con valor inicial | — |
| `date` | String | Fecha de presentación | Opcional o con valor inicial | — |
| `ownScore` | Int | Own Score | Opcional o con valor inicial | — |
| `rivalScore` | Int | Rival Score | Opcional o con valor inicial | — |
| `aiFacts` | String | Ai Facts | Opcional o con valor inicial | — |
| `aiAnalysis` | String | Ai Analysis | Opcional o con valor inicial | — |
| `finalFacts` | String | Final Facts | Opcional o con valor inicial | — |
| `finalAnalysis` | String | Final Analysis | Opcional o con valor inicial | — |
| `warnings` | List<String> | Warnings | Opcional o con valor inicial | — |
| `status` | String | Estado del proceso | Opcional o con valor inicial | — |
| `generation` | Int | Generation | Opcional o con valor inicial | — |
| `model` | String | Model | Opcional o con valor inicial | — |
| `promptVersion` | String | Prompt Version | Opcional o con valor inicial | — |
| `memberUids` | List<String> | Member Uids | Opcional o con valor inicial | — |
| `createdBy` | String | Responsable de creación | Opcional o con valor inicial | users |
| `approvedBy` | String | Approved By | Opcional o con valor inicial | — |
| `approvedByName` | String | Approved By Name | Opcional o con valor inicial | — |
| `approvedAt` | String | Approved At | Opcional o con valor inicial | — |
| `createdAt` | Timestamp? | Fecha de creación | Opcional o con valor inicial | — |

## `players.monthlyFees`

Administración > Mensualidades. Responsables de escritura: ADM. Operaciones: Registrar y actualizar estado.

| Campo | Tipo | Descripción | Presencia | Formato o relación |
| --- | --- | --- | --- | --- | --- |
| `id` | String | Identificador del registro | Clave o alta nueva | Clave vigente de la relación |
| `playerId` | String | Ficha de jugador vinculada | Según el flujo | players |
| `playerName` | String | Player Name | Opcional o con valor inicial | — |
| `teamId` | String | Equipo relacionado | Opcional o con valor inicial | teams |
| `teamName` | String | Nombre del equipo | Opcional o con valor inicial | — |
| `category` | String | Categoría deportiva | Opcional o con valor inicial | — |
| `period` | String | Period | Opcional o con valor inicial | — |
| `amount` | Double | Amount | Opcional o con valor inicial | — |
| `dueDate` | String | Due Date | Opcional o con valor inicial | — |
| `status` | String | Estado del proceso | Opcional o con valor inicial | — |
| `updatedBy` | String | Updated By | Opcional o con valor inicial | — |
| `updatedAt` | Timestamp? | Última actualización | Opcional o con valor inicial | — |
| `createdBy` | String | Responsable de creación | Opcional o con valor inicial | users |
| `createdAt` | Timestamp? | Fecha de creación | Opcional o con valor inicial | — |

## Metadatos y registros técnicos

| Colección | Campo | Tipo | Presencia y propósito | Origen y operación |
| --- | --- | --- | --- | --- |
| Documentos nuevos integrados | `id` | String | Siempre en las altas transaccionales. Formato SP por entidad. | Repositorios, solo creación o migración |
| Documentos nuevos integrados | `createdAt`, `updatedAt` | Timestamp | Metadatos de creación y actualización del servidor. | Repositorios, actualización limitada |
| Documentos nuevos integrados | `createdByUid`, `updatedByUid` | String | UID del actor. | Repositorios, según el rol |
| Documentos auditados | `createdBy`, `updatedBy` | String | ID de negocio cuando el perfil ya tiene uno. Históricos pueden conservar UID o correo. | Repositorios y ADM |
| `teams`, `players`, `matches`, `trainings`, `tryouts` | `coachId` | String | ID del DT si su perfil ya tiene correlativo. | Creación en el repositorio |
| `matches` | `matchDate` | Timestamp | Fecha del partido, opcional para históricos. | DT, formulario de partido |
| `trainings` | `trainingDate` | Timestamp | Fecha de la sesión, opcional para históricos. | DT, formulario de entrenamiento |
| `trainings`, historial individual | `attendanceId` | String | Vínculo a asistencia grupal. | DT, guardado de asistencia |
| `feeIndex` | `id`, `playerId`, `createdAt` | String, String, Timestamp | Reserva de CUO y asociación con jugador. | ADM, alta atómica de cuota |
| `players` | `latestFeeId` | String | Última reserva relacionada. | ADM, alta de cuota |
| `counters` | `lastId` | Integer | Último correlativo asignado, obligatorio. | Inicialización IAM y transacción |
| `counters` | `allocatedId`, `documentPath` | String | Última asignación y ruta vinculada. | Repositorios, actualización condicionada |
| `auditLogs` | `operation`, `collection`, `documentId`, `documentPath` | String | Mutación y destino, obligatorios. | Repositorios, creación y lectura ADM |
| `auditLogs` | `performedBy`, `performedByUid`, `timestamp` | String, String, Timestamp | Identidades del actor y hora del servidor. ID de negocio vacío en perfiles históricos sin ID. | Repositorios, creación y lectura ADM |
| `auditLogs` | `changedFields` | Array de String | Nombres de campos. Vacío al crear. | Repositorios, creación y lectura ADM |
| `dniIndex` | `playerId` | String | Ficha reservada con clave técnica de DNI. | Alta de jugador, lectura unitaria autenticada |
| `tryouts/{id}/candidateOwners` | `candidateId` | String | Postulante de la cuenta representada por la ruta. | Alta de postulante, lectura de la propia cuenta |
| `tryoutReports` | `tryoutId`, `postId`, `postText`, `postAuthorUid`, `postAuthorName`, `reason`, `detail`, `reporterUid`, `type`, `status` | String | Reporte correlativo sobre una prueba deportiva. El índice de unicidad conserva reportante y prueba. | Reportar aviso, lectura y edición ADM |
| `tryoutReports` | `createdAt` | Timestamp | Creación del reporte. | Repositorio |
| `match_events` | `type` | String | Nombre o tipo del catálogo histórico consultado para validación. | Lectura autenticada |
| `matches` | `callupIds`, `callupUserIds`, `lineupIds`, `summaryId` | Map, Map, Map, String | Índices de jugador/cuenta, alineación de equipo y resumen vigente. | Repositorios, creación transaccional |
| `posts`, `tryouts` / `reportOwners` | `reportId` | String | Índice único por cuenta y contenido. | Reportante en creación transaccional |
| `players/{id}/private/contact` | `dni`, `birthDate`, `phone`, `email`, `heightCm`, `weightKg`, `emergencyName`, `emergencyRelation`, `emergencyPhone`, `parentRelation` | String, Integer opcional en medidas | Perfil personal separado de la ficha deportiva nueva. | ADM, DT del equipo, jugador o tutor vinculado |
| `users`, `Admins` | `authUid` | String | UID original de Firebase Auth y asociación estable de la ficha. | Alta de cuenta o migración verificada |
| `users` | `photoStoragePath` | String | Ruta de la foto en Supabase por UID. | Perfil del titular |
