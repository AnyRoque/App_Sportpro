# Informe de pruebas de la referencia 24-11

La ejecución de la versión integrada está en [PRUEBAS_FUNCIONALES.md](../PRUEBAS_FUNCIONALES.md).

La línea base compiló con `assembleDebug` y ejecutó las 10 pruebas de `ValidatorsTest`. La app usa JUnit 4 local y tiene dependencias de Compose Test y Espresso con el runner AndroidJUnitRunner. Las tres pruebas de `SportProIdTest` cubren formato, límites y prefijos independientes. `FirestoreEmulatorTest` cubre cinco escenarios con Auth y Firestore Emulator. Ejercita alta de DT y JUG, aprobación de equipo con una cuenta ADM de prueba, altas concurrentes y snapshots, reportes únicos, privacidad del contacto, fallo de conexión y reintento, y las relaciones de partido, convocatoria, alineación, gol, entrenamiento y asistencia.

## Matriz de Emulator Suite

Para ejecutar la prueba instrumentada usa `./gradlew :app:connectedDebugAndroidTest -PsportproFirebaseEmulator=true` con Auth Emulator, Firestore Emulator y sus contadores preparados. Si Gradle no puede resolver UTP, compila `:app:assembleDebug` y `:app:assembleDebugAndroidTest` con la misma propiedad e instala ambos APK en el emulador. Ejecuta la clase `pe.edu.esan.sportpro.FirestoreEmulatorTest` con `adb shell am instrument -w -e class pe.edu.esan.sportpro.FirestoreEmulatorTest pe.edu.esan.sportpro.test/androidx.test.runner.AndroidJUnitRunner`.

| Flujo | Verificación |
| --- | --- |
| ADM, DT, JUG y PAD | Alta y login, UID y roles, sesión anterior y recuperación de contraseña. |
| Equipos y jugadores | Crear, aprobar, asignar, editar, desactivar y rechazar DNI repetido. |
| Partidos y entrenamientos | Crear dos de cada uno, editar, cancelar, observar desde dos sesiones y comprobar rival externo. |
| Asistencia | Guardar dos veces una misma sesión, comprobar el ID estable, historiales individuales y rechazo de filas duplicadas. |
| Convocatoria y alineación | Crear, responder, cambiar formación y comprobar los roles. |
| Eventos | Registrar gol, tarjeta, sustitución u otro evento, corregir y confirmar marcador y auditoría. |
| Concurrencia y offline | Crear desde dos sesiones, desconectar una sesión durante una transacción y comprobar contador y documento después. |
| Reglas | Probar accesos por rol, inmutabilidad del ID y protección de contadores con un proyecto demo. |
| Migración | Probar respaldo, copia, reejecución, verificación y recuperación en otro proyecto demo. |

## Resultados locales

| Comprobación | Resultado |
| --- | --- |
| `:app:assembleDebug` | Pasó con la configuración normal y con el proyecto demo. |
| `:app:testDebugUnitTest` | 13 pruebas pasaron, 0 fallos, 0 omitidas. |
| `:app:assembleDebugAndroidTest -PsportproFirebaseEmulator=true` | APK instrumentado compilado. |
| `firebase.json` y `firestore.indexes.json` | JSON válido. |
| Reglas y pruebas contra Auth y Firestore Emulator | 5 pruebas instrumentadas pasaron en el emulador Android con Firestore Emulator 1.21.0 y Auth Emulator. Se usó ADB porque Gradle requería una dependencia de UTP sin caché. |
| Test PowerShell de la correspondencia | 12 aserciones aprobadas, incluidas rutas CON, ALI, REP y RES. |
| Migración y recuperación en Firestore Emulator | Pasaron DryRun, Apply, la segunda ejecución de Apply, Verify y Restore. Se verificaron 13 documentos operativos en la copia y 11 documentos originales en la restauración. El ensayo comprueba vínculos, aislamiento de datos personales, reconstrucción de asistencia y preservación de caracteres UTF-8. |
| Datos de producción | La verificación se limitó a proyectos demo con datos sintéticos. |

En los ensayos iniciales las reglas rechazaron una reserva simultánea antes de que el SDK devolviera un conflicto de versión. El generador reintenta esa operación si una lectura de servidor confirma que otro cliente avanzó uno de sus contadores. Una denegación sin avance conserva el error de permisos. La prueba de red usa un destino local sin servicio y verifica que la operación aparece una sola vez al reintentar en el destino disponible. El resumen con Gemini, las pruebas de UI de todos los módulos y el corte de datos reales quedan fuera de esta ejecución.
