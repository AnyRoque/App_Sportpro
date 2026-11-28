# Cambios implementados

- Se integraron los modelos, repositorios y pantallas modificados en 24-11 para Firebase Auth y Cloud Firestore. La interfaz sigue en Kotlin y Compose y mantiene la navegación, el splash y el fondo de SportPro de main.
- Se conservaron el soporte de fotos de comunidad mediante Supabase y los reintentos, el modelo predeterminado y los mensajes de error de Gemini.
- Los nuevos registros de negocio utilizan secuencias transaccionales y conservan los UID de autenticación. Se incluyen los vínculos de convocatorias, alineaciones y resúmenes, el historial de asistencia y las reservas de DNI y cuotas.
- Se integraron el manifiesto de debug, los índices, las reglas y los scripts de inventario, migración, verificación y restauración.
- Al editar un partido se conservan el marcador, el cronómetro y los vínculos a convocatorias y alineaciones. Los rivales registrados se relacionan por ID cuando hay una coincidencia única de nombre.
- Se restringió la lectura de partidos a jugadores y tutores incluidos en el registro. La agenda prueba los vínculos de equipo en el servidor y la asistencia grupal se filtra por el DT responsable. Las reglas comprueban la propiedad de fichas y proyecciones, las operaciones del partido y el tipo de remitente de los avisos.
- Un alta de Auth interrumpida puede volver a intentar crear su perfil desde el formulario de registro tras comprobar las credenciales y la ausencia del perfil en el servidor.
- La subida de foto de perfil lee hasta 5 MB mediante APIs compatibles con Android 26. El cronómetro y los porcentajes toman la configuración regional observable de Compose.
- El estado de ejecución y las pruebas que requieren un entorno adicional están en [PRUEBAS_FUNCIONALES.md](PRUEBAS_FUNCIONALES.md).
