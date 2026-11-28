# Comparación de versiones de SportPro

| Versión | Raíz Android | Estado de partida |
| --- | --- | --- |
| App_Sportpro-main | `F:/ANDROID STUDIO/27-11` | Repositorio `AnyRoque/App_Sportpro`, rama `main`, commit `72a0816` |
| 24-11 | `F:/ANDROID STUDIO/24-11` | Carpeta extraída, commit `f72042a` más cambios de trabajo. Los 180 archivos seleccionados del ZIP coinciden por SHA-256. |

La comparación recursiva de `app/src` se hizo por contenido con Git. El historial de `main` desde `f72042a` permite distinguir las funciones que se incorporaron posteriormente al proyecto base. El respaldo del código versionado es `SportPro-base-72a0816.zip`.

| Área | App_Sportpro-main | 24-11 | Decisión |
| --- | --- | --- | --- |
| Identidad y arquitectura | Kotlin, Compose, MVVM, Navigation Compose, `pe.edu.esan.sportpro` | Misma arquitectura, identificador y proyecto Firebase `sportpro-73035` | Conservar la identidad y las tecnologías |
| Acceso y navegación | Splash, fondo de SportPro, tratamiento de barras y flujos de navegación posteriores | Flujos de Auth y presentación anteriores | Conservar los recorridos y recursos de main, usar el repositorio Auth de la referencia |
| Comunidad | Fotos en publicaciones mediante Supabase | Correlativos, transacción para reportes y reglas de moderación | Combinar UI y almacenamiento de main con persistencia de 24-11 |
| Jugadores y tutores | Planteles, solicitud, edición y protección de mensualidades al actualizar | ID, contacto privado, reconciliación de fichas y actualización de miembros de partidos | Integrar preservando mensualidades |
| Equipos y entrenamientos | Equipos, aprobación, agenda y asistencia | Transacciones, referencias y vistas de asistencia por rol | Incorporar reglas y transacciones junto con sus modelos y pantallas |
| Partidos | Programación, convocatoria, alineación, directo, gestión de eventos y marcador | Correlativos y referencias de convocatorias, alineaciones, partido y eventos | Integrar flujos y consultas compatibles con documentos históricos |
| IA y resultados | Reintentos y errores detallados de Gemini | Resumen con ID propio y vínculo al partido | Conservar reintentos, mensajes y modelo configurado en main |
| Administración | Usuarios, equipos, catálogo, mensualidades y moderación | Altas Firestore atómicas, contadores, auditoría y validación de escrituras | Integrar el backend local con las pantallas existentes |
| Compilación y manifiesto | Módulo Android y configuración normal | Opción de compilación y manifiesto de debug para Emulator Suite | Integrar únicamente los cambios de emulación |
| Firebase y migración | Auth y Firestore, reglas y configuración básica | Reglas ampliadas, índices declarados, emuladores y scripts de inventario, copia y restauración | Integrar localmente, conservar datos y configuración de producción |
| Pruebas | Validadores unitarios | IDs, pruebas Android de Firestore y scripts de ensayo | Incorporar y ejecutar según disponibilidad |

Las rutas con prefijo `main/` corresponden a `app/src/main/java/pe/edu/esan/sportpro/`. Los elementos locales, cachés, archivos de IDE, registros y APK previos quedaron fuera de la integración.

| Diferencia | Archivo | Decisión |
| --- | --- | --- |
| Nuevo | `app/src/androidTest/java/pe/edu/esan/sportpro/FirestoreEmulatorTest.kt` | Integrar 24-11 |
| Nuevo | `app/src/debug/AndroidManifest.xml` | Integrar 24-11 |
| Nuevo | `app/src/debug/res/xml/firebase_emulator_network_security.xml` | Integrar 24-11 |
| Modificado | `main/SportProApp.kt` | Integrar 24-11 |
| Modificado | `main/data/model/AdminModel.kt` | Integrar 24-11 |
| Modificado | `main/data/model/AttendanceModel.kt` | Integrar 24-11 |
| Modificado | `main/data/model/CallUpModel.kt` | Integrar 24-11 |
| Modificado | `main/data/model/CommunityModels.kt` | Integrar 24-11 |
| Modificado | `main/data/model/EventAuditModel.kt` | Integrar 24-11 |
| Modificado | `main/data/model/EventCancellationRequestModel.kt` | Integrar 24-11 |
| Modificado | `main/data/model/EventCatalogModel.kt` | Integrar 24-11 |
| Modificado | `main/data/model/LineupModel.kt` | Integrar 24-11 |
| Modificado | `main/data/model/MatchEventModel.kt` | Integrar 24-11 |
| Modificado | `main/data/model/MatchModel.kt` | Integrar 24-11 |
| Modificado | `main/data/model/PlayerModel.kt` | Integrar 24-11 |
| Modificado | `main/data/model/TeamModel.kt` | Integrar 24-11 |
| Modificado | `main/data/model/TrainingModel.kt` | Integrar 24-11 |
| Modificado | `main/data/model/TryoutModel.kt` | Integrar 24-11 |
| Modificado | `main/data/model/UserModel.kt` | Integrar 24-11 |
| Modificado | `main/data/remote/AdminRepository.kt` | Integrar 24-11 |
| Modificado | `main/data/remote/AnnouncementRepository.kt` | Integrar 24-11 |
| Modificado | `main/data/remote/AttendanceRepository.kt` | Integrar 24-11 |
| Solo en main | `main/data/remote/CommunityImageStorage.kt` | Conservar la versión de main |
| Modificado | `main/data/remote/CommunityRepository.kt` | Integrar 24-11 |
| Modificado | `main/data/remote/EventCatalogRepository.kt` | Integrar 24-11 |
| Modificado | `main/data/remote/FirebaseAuthManager.kt` | Integrar 24-11 |
| Nuevo | `main/data/remote/FirestoreFields.kt` | Integrar 24-11 |
| Nuevo | `main/data/remote/LinkedFirestoreDocument.kt` | Integrar 24-11 |
| Modificado | `main/data/remote/MatchRepository.kt` | Integrar 24-11 |
| Modificado | `main/data/remote/MonthlyFeeRepository.kt` | Integrar 24-11 |
| Modificado | `main/data/remote/PlayerRepository.kt` | Integrar ambas líneas |
| Nuevo | `main/data/remote/SportProIdGenerator.kt` | Integrar 24-11 |
| Modificado | `main/data/remote/SummaryRepository.kt` | Integrar ambas líneas |
| Modificado | `main/data/remote/TeamRepository.kt` | Integrar 24-11 |
| Modificado | `main/data/remote/TrainingRepository.kt` | Integrar 24-11 |
| Modificado | `main/data/remote/TryoutRepository.kt` | Integrar 24-11 |
| Modificado | `main/presentation/auth/ForgotPasswordScreen.kt` | Conservar la versión de main |
| Modificado | `main/presentation/auth/LoginScreen.kt` | Conservar la versión de main |
| Modificado | `main/presentation/auth/RegisterScreen.kt` | Conservar la versión de main |
| Modificado | `main/presentation/community/CommunityScreens.kt` | Conservar la versión de main |
| Modificado | `main/presentation/community/CommunityViewModel.kt` | Conservar la versión de main |
| Solo en main | `main/presentation/components/StatusBarIcons.kt` | Conservar la versión de main |
| Modificado | `main/presentation/matches/CallUpFormScreen.kt` | Integrar 24-11 |
| Modificado | `main/presentation/matches/CallUpViewModel.kt` | Integrar 24-11 |
| Modificado | `main/presentation/matches/LineupScreen.kt` | Integrar 24-11 |
| Modificado | `main/presentation/matches/LineupViewModel.kt` | Integrar 24-11 |
| Modificado | `main/presentation/matches/MatchConsoleViewModel.kt` | Integrar 24-11 |
| Modificado | `main/presentation/matches/MatchViewModel.kt` | Integrar 24-11 |
| Modificado | `main/presentation/navigation/AppNavGraph.kt` | Conservar la versión de main |
| Modificado | `main/presentation/navigation/DrawerScaffold.kt` | Conservar la versión de main |
| Modificado | `main/presentation/players/PlayerDetailScreen.kt` | Integrar 24-11 |
| Modificado | `main/presentation/players/PlayerFormViewModel.kt` | Integrar 24-11 |
| Modificado | `main/presentation/players/RosterViewModel.kt` | Integrar 24-11 |
| Modificado | `main/presentation/profile/PlayerEditSection.kt` | Integrar 24-11 |
| Solo en main | `main/presentation/splash/SplashScreen.kt` | Conservar la versión de main |
| Modificado | `main/presentation/training/TrainingDetailScreen.kt` | Integrar 24-11 |
| Modificado | `main/presentation/tryouts/TryoutsNav.kt` | Conservar la versión de main |
| Solo en main | `app/src/main/res/drawable/fondo_inicio.png` | Conservar la versión de main |
| Nuevo | `app/src/test/java/pe/edu/esan/sportpro/SportProIdTest.kt` | Integrar 24-11 |
