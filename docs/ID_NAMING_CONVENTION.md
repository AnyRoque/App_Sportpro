# Identificadores

Los IDs correlativos usan `SP-<PREFIJO>-<SEIS DÍGITOS>`. Cada contador guarda el último número asignado. La secuencia empieza en 1 y se agota en 999999. Un contador de una colección vacía se inicializa en 0. Para una base existente, usa el inventario del script para detectar máximos y conservar el último valor previo.

| Uso | Prefijo |
| --- | --- |
| Administrador, DT, cuenta JUG/PAD | ADM, DT, USU |
| Jugador, equipo, partido, entrenamiento, asistencia grupal | JUG, EQU, PAR, ENT, ASI |
| Evento, gol, tarjeta, sustitución | EVT, GOL, TAR, SUS |
| Prueba, postulante, catálogo de eventos, solicitud de anulación, historial de evento | PRU, POS, CAT, SOL, HIS |
| Anuncio, publicación, comentario, notificación, auditoría | ANU, PUB, COM, NOT, AUD |
| Mensualidad | CUO |
| Convocatoria, alineación, reporte, resumen | CON, ALI, REP, RES |

`users/{uid}.id` guarda el identificador de negocio. La ficha ADM y su perfil Auth comparten el mismo ID administrativo. Los índices técnicos conservan sus claves operativas. Las convocatorias, alineaciones, reportes y resúmenes nuevos usan correlativos. El partido conserva mapas de relación con jugador y equipo y el vínculo a su resumen. El índice `reportOwners` conserva una entrada por cuenta y publicación o prueba. La app resuelve rutas históricas cuando falta el vínculo. La proyección de asistencia conserva el ID del entrenamiento como clave técnica.

Las mensualidades nuevas guardan el ID CUO dentro del arreglo actual y lo reservan en `feeIndex`. El registro de pago conserva el estado embebido. Las cuotas históricas conservan sus claves.

Una creación depende del contador, de la autorización y de la disponibilidad de Internet. Firestore reintenta transacciones en concurrencia. Si las reglas rechazan una reserva simultánea antes de que se detecte el conflicto de versión, SportPro comprueba en el servidor los contadores afectados y puede volver a intentar la transacción hasta dos veces. Un resultado de red indeterminado requiere comprobar si aparece el registro antes de volver a enviarlo.
