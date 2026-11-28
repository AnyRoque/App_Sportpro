# Reglas de seguridad

Las reglas locales validan los correlativos junto con un incremento de contador y la creación de la ruta comprometida. Protegen los campos de identidad y procedencia en las colecciones migradas. El alta pública preserva los roles DT, JUG y PAD. ADM se asigna desde una sesión administrativa con el perfil Auth enlazado a la nueva ficha. La edición propia no concede roles ni reactiva una cuenta.

Las reglas contemplan la propiedad de equipos y entrenamientos, la sesión de la asistencia, la reserva del DNI y la relación de postulantes. Las validaciones adicionales sobre arrays de alineaciones, miembros e historiales siguen dependiendo de flujos existentes o de validaciones del cliente. Las cuentas y los planteles quedan limitados a su titular o tutor donde corresponde y al personal autorizado por rol. Las fichas nuevas guardan los campos personales en `players/{id}/private/contact`. Ese documento se lee por ADM, el DT asignado, el jugador y su tutor. Los datos personales embebidos en fichas históricas se separan durante la migración. Los DT conservan el directorio autenticado que necesita la vinculación por correo. Usuarios autenticados aún pueden crear avisos internos según la política anterior. Revisa el alcance de envío por módulo antes del corte.

Solo ADM lee la auditoría general. Las reglas condicionan una entrada a cambios en el mismo commit, al UID autenticado y a la hora del servidor. Un cliente con acceso autorizado puede omitir auditoría cuando una regla de negocio no la exige. La lista de campos puede ser parcial. Solo se incluyen nombres de campos, sin valores, credenciales, información médica ni DNI. La bitácora de eventos conserva su política de lectura previa. Sus entradas nuevas se crean junto con la modificación del evento y la auditoría general. La auditoría fuerte requiere un escritor de confianza o mediación de servidor, para evaluar y aprobar después.

Las transacciones y lotes tienen un límite de consultas de reglas. Ensaya la asistencia y la convocatoria con un plantel completo. La convocatoria reserva un bloque de correlativos y escribe el bloque en una transacción. Cada documento se comprueba contra el rango y el índice del partido. Las reglas no inspeccionan todas las entradas de un bloque arbitrario, así que la ausencia de huecos en clientes personalizados depende del escritor. Usa Emulator Suite para compilar y probar las reglas antes de publicar.

## Validación de la integración

La agenda de entrenamiento usa un enlace de lectura por cuenta y equipo. Firestore coteja la ficha vigente, el equipo, la pertenencia por UID y el estado aceptado. Los participantes de partidos se comprueban con `memberUids`. Los DT conservan lecturas operativas de partidos para resolver rivales. La asistencia grupal se limita al organizador o ADM y el historial se escribe bajo el jugador del equipo correspondiente. Las notificaciones se validan por tipo y relación con su origen.

Antes de publicar, revisa la separación histórica de contactos, el directorio visible a DT, el alcance de los resúmenes y anuncios, y los contadores sociales. Ensaya con planteles completos. Los límites de llamadas de reglas también aplican a las transacciones de asistencia y convocatoria.

## Evaluación estática del árbol integrado

```json
{
  "score": 2,
  "summary": "La integración protege roles, identificadores, contactos nuevos, partidos de participantes y agenda por equipo. Los datos históricos y el alcance de lectura en módulos que conservan la política anterior requieren revisión antes de producción.",
  "findings": [
    {
      "check": "Confidencialidad del historial y directorio",
      "severity": "major",
      "issue": "Los datos personales embebidos en fichas anteriores conservan su visibilidad de plantel. Las cuentas DT pueden consultar el directorio de usuarios para las vinculaciones por correo.",
      "recommendation": "Verificar la separación de contactos en la copia de ensayo y validar qué campos requiere el directorio operativo."
    },
    {
      "check": "Visibilidad de anuncios y resúmenes",
      "severity": "moderate",
      "issue": "Las reglas permiten su lectura a cuentas autenticadas. La app aplica filtros de destinatarios y publicación.",
      "recommendation": "Alinear las consultas y las reglas con el alcance de destinatarios antes de publicar."
    },
    {
      "check": "Campos y tamaños de comunidad",
      "severity": "moderate",
      "issue": "Los cambios de reacciones y conteo de comentarios conservan validación parcial. La auditoría general tampoco es obligatoria para todas las mutaciones.",
      "recommendation": "Vincular cada cambio al registro individual y validar límites en reglas o en un escritor de confianza."
    }
  ]
}
```

## Evaluación estática de la referencia 24-11

```json
{
  "score": 2,
  "summary": "Las reglas acotan creación correlativa y cambios de identidad. El acceso autenticado a datos personales y la amplitud de escrituras históricas requieren revisión antes del corte.",
  "findings": [
    {
      "check": "Acceso a datos personales",
      "severity": "major",
      "issue": "Los datos personales históricos siguen embebidos hasta el corte. El directorio de cuentas permanece disponible para DT y ADM para vincular jugadores.",
      "recommendation": "Ejecutar la separación histórica en el ensayo y comprobar permisos por rol, tutoría y pertenencia a equipo."
    },
    {
      "check": "Contadores sociales y mensajes",
      "severity": "moderate",
      "issue": "La creación de reportes queda acoplada al contador de moderación. Reacciones, comentarios y notificaciones requieren una revisión de cobertura.",
      "recommendation": "Vincular los cambios de contador a comentarios, reportes y reacciones verificables y delimitar remitentes por módulo."
    },
    {
      "check": "Cobertura de auditoría",
      "severity": "moderate",
      "issue": "La auditoría del cliente cubre mutaciones integradas y puede omitirse en escrituras históricas autorizadas.",
      "recommendation": "Recorrer cada mutación pendiente en el emulador y evaluar un escritor de confianza."
    }
  ]
}
```

[Condiciones y límites de reglas](https://firebase.google.com/docs/firestore/security/rules-conditions)
