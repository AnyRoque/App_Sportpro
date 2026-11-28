# Errores corregidos

| Hallazgo | Resolución local |
| --- | --- |
| IDs automáticos como única clave de negocio | Reservas transaccionales por entidad y reglas que verifican la reserva junto con el registro. |
| Edición de ficha con riesgo de pérdida de cuotas embebidas | Se actualizan campos identificados del perfil y el contacto, preservando los campos de mensualidades. |
| Convocatorias, alineaciones y resúmenes sin un ID independiente | Se enlaza el documento de negocio desde el partido y se mantiene lectura de rutas históricas. |
| Asistencia de sesión y proyección individual podían divergir | Se escriben en la misma transacción junto con el estado del entrenamiento. |
| Publicación reportada más de una vez por la misma cuenta | Se registra el reporte y su índice de unicidad en una transacción. |
| Edición de jugador o tutor bloqueada por una consulta global | La edición se realiza sobre la ficha autorizada, con las restricciones de UID y DNI de Firestore. |
| Consultas de entrenamiento anteriores conservaban listeners al cambiar de equipo | La consulta activa se vuelve a suscribir con los vínculos aceptados y se cancela con el ciclo de vida del flujo. |
| Reintentos de IA en riesgo durante la integración | Se combinaron los reintentos de main con el vínculo de resumen de 24-11. |
| Editar el formulario de partido sobrescribía el estado deportivo | El formulario guarda solo los campos de programación y conserva convocatorias, alineaciones, marcador y cronómetro. |
| Lectura de partidos por cualquier cuenta autenticada | Las cuentas JUG y PAD requieren su UID en la lista de miembros. El personal conserva las lecturas operativas. |
| Un DT ajeno podía escribir proyecciones de asistencia | Las reglas cotejan el DT, el equipo y el vínculo entre entrenamiento, jugador y asistencia. |
| Agenda legible por cuentas ajenas al equipo | Firestore valida el enlace del jugador o tutor con una ficha activa y aceptada. |
| Avisos nuevos podían fallar al deserializarse al guardar el correlativo | El modelo lee el ID de negocio y usa el ID del documento como respaldo para avisos históricos. |
| El emulador de Auth secundario del alta ADM reutilizaba la configuración de otras instancias de prueba | La instancia secundaria usa una clave de demo independiente y conserva la sesión del administrador que autoriza. |
| Historial de asistencia en orden incorrecto al cambiar de mes o año | Se compara la fecha interpretada en el formato del formulario. |
| Pantallas usaban una API de lectura desde Android 33 y una configuración regional ajena a Compose | La lectura de fotos usa un búfer limitado. El cronómetro y los porcentajes se recalculan si cambia la configuración regional. |

Consulta [PRUEBAS_FUNCIONALES.md](PRUEBAS_FUNCIONALES.md) para saber qué verificaciones se ejecutaron sobre este árbol.
