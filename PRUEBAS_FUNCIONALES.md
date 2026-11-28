# Pruebas funcionales de la integración

Las pruebas de Firebase se ejecutaron con datos sintéticos en `demo-sportpro` y proyectos demo auxiliares. Android se probó en `emulator-5554`.

| Verificación | Resultado |
| --- | --- |
| Sincronización de Gradle | Aprobada en Android Studio. |
| `clean assembleDebug test lintDebug` | Aprobada. Lint detectó inicialmente cuatro errores y pasó después de las correcciones. |
| Pruebas unitarias de validación e IDs | 13 aprobadas, 0 fallidas y 0 omitidas. |
| `connectedDebugAndroidTest` | Falló al resolver el complemento UTP `android-test-plugin-host-additional-test-output:32.3.0` en modo offline. |
| APK demo y APK instrumentada | Compiladas e instaladas. Las 7 pruebas pasaron con AndroidJUnitRunner por ADB, 0 fallidas. |
| Ensayo base del plan PowerShell | 12 aserciones aprobadas. |
| Firebase DryRun, Apply, Apply repetido, Verify y Restore | Aprobados en tres proyectos demo. Se verificaron 13 documentos operativos en el destino y 11 documentos originales en la restauración. |
| Navegación visual | Inicio del DT y pantalla Entrenamientos en el emulador. Se comprobaron las pestañas de lista y semana visibles, los filtros, el estado vacío y el botón de planificación. |

## Cobertura y límites

| Flujo | Estado |
| --- | --- |
| Registro DT y JUG, login y cierre de sesión | Aprobado a través de Auth Emulator. El test alternó perfiles y comprobó los UID y los correlativos. |
| Registro ADM, perfil relacionado y sesión de quien crea | Aprobado. Se rechazó el alta desde DT, se conservó la sesión del administrador que crea la cuenta y se comprobó el perfil nuevo con el UID y el ID ADM. La primera ejecución detectó una excepción de Auth secundaria y la ejecución final pasó después de aislar la instancia del emulador. |
| Registro PAD y recuperación de contraseña | Aprobados en Auth Emulator para alta, nuevo login y solicitud de restablecimiento. La recepción del correo real y el recorrido visual del formulario quedan pendientes. |
| Alta, consulta en tiempo real y aprobación de equipo | Aprobadas. Dos altas concurrentes produjeron dos IDs distintos y la aprobación se probó con una cuenta ADM demo. |
| Jugador vinculado y edición de su contacto | Aprobadas. Se leyó y editó el contacto con el titular y se rechazó la lectura de otra cuenta JUG. |
| Partido, alineación, convocatoria y gol | Aprobados. Se verificaron rutas e IDs, visibilidad para el jugador vinculado y rechazo de una cuenta ajena. |
| Entrenamiento, avisos y asistencia | Aprobados en los escenarios instrumentados de creación, visibilidad, avisos, regrabación de asistencia con el mismo ID e historial individual. Se rechazó el acceso ajeno y la escritura de otro DT. |
| Edición y cancelación de partidos y entrenamientos, y respuesta de convocatoria | Aprobadas a través de los repositorios Android. La edición conservó los vínculos, el marcador y el estado de asistencia. El DT recibió el aviso de confirmación. |
| Comunidad, reporte repetido y fallo de conexión | Aprobados en creación, reporte por cuenta y ausencia de duplicado al repetir una operación después del error. |
| Fotos Supabase, generación y publicación de resúmenes, filtros y estadísticas por toda la interfaz, y datos reales | No ejecutadas. |
| Edición y desactivación de usuarios desde ADM, edición de equipo y proceso de solicitud de ingreso | Cobertura parcial. Se probó la creación ADM, la aprobación de equipo y la vinculación del jugador mediante repositorio. Los demás recorridos de interfaz quedan pendientes. |
| Mensualidades, catálogo de eventos, moderación ADM, pruebas deportivas y publicación de anuncios | Sin una ejecución funcional específica de la versión integrada. |
| Cronómetro completo, corrección y anulación de eventos, y pantallas de resultados | Cobertura parcial. Se registró un gol y se verificó el marcador persistido al editar el partido. Los demás recorridos quedan pendientes. |
| Planteles de tamaño real y compatibilidad con todos los registros históricos | No ejecutadas. |

Para reproducir la prueba por ADB, arranca Auth y Firestore Emulator con el proyecto `demo-sportpro` y compila la APK debug y la APK de instrumentación con `-PsportproFirebaseEmulator=true`. Instala ambas en el emulador Android y ejecuta lo siguiente.

```powershell
adb shell am instrument -w -e class pe.edu.esan.sportpro.FirestoreEmulatorTest pe.edu.esan.sportpro.test/androidx.test.runner.AndroidJUnitRunner
```
