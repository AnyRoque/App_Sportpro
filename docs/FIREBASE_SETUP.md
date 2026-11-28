# Configuración de Firebase

## Entorno local

1. Instala Firebase CLI y la versión de Java compatible con su emulador. Desde la raíz ejecuta `firebase emulators:start --only firestore,auth --project demo-sportpro`. La configuración está en `firebase.json`.
2. Para preparar los contadores en una base de emulador vacía ejecuta primero el inventario y luego `Seed`. Los comandos están en la guía de migración.
3. Crea los usuarios de prueba en Auth Emulator. Para inicializar el primer ADM en el emulador, crea administrativamente `users/{uid}` con el UID de esa cuenta, `uid`, `authUid`, `role = ADM` y `active = true`. Si asignas un ID ADM manual, crea la ficha enlazada y sube el contador ADM al mismo número.
4. Compila con `./gradlew :app:assembleDebug -PsportproFirebaseEmulator=true` e instala ese APK en el emulador de Android. Usa Auth en `10.0.2.2:9099` y Firestore en `10.0.2.2:8080` bajo el proyecto `demo-sportpro`.

## Consola sportpro-73035

1. En **Firestore Database > Data**, confirma que la base activa es `(default)` y registra su edición. La CLI local no permitió verificar la edición remota.
2. En **Firestore Database > Rules**, guarda una copia del texto actualmente publicado y compárala con el repositorio. En **Indexes**, inventaría los índices existentes. El archivo de índices local parte de las consultas encontradas y contiene una lista vacía de índices compuestos. Evita eliminar índices ya publicados al sincronizar.
3. En **Authentication > Users**, comprueba el UID de los administradores iniciales y su perfil `users/{uid}`. En **Sign-in method**, conserva el proveedor Email/Password existente.
4. Si Authentication creó una cuenta y la transacción de Firestore falló, verifica primero el UID y que no exista un perfil. La recuperación de un alta ADM requiere una sesión administrativa o una operación IAM autorizada. Un perfil reparado por inicio de sesión recibe JUG según el flujo existente de recuperación.
5. Prepara el inventario y valida el corte en un proyecto de demostración. Cuando se autorice la publicación, revisa el orden de actualización para que versiones antiguas que aún crean IDs aleatorios no sigan escribiendo durante el corte.
6. Si Supabase ya sirve fotos, conserva el bucket y sus políticas de carpetas por UID. Los cambios de Firestore no requieren trasladar archivos ni activar Firebase Storage.

## Si Firestore rechaza programar un partido

La creación guarda en una sola transacción el partido, su auditoría y el incremento de ambos contadores. Para que funcione en `sportpro-73035`, comprueba en Firestore Console:

- Las reglas publicadas de `counters`, `matches` y `auditLogs` deben autorizar la misma transacción.
- `counters/matches` y `counters/auditLogs` deben contener un `lastId` entero. Inicialízalos con `Seed` a partir del inventario y conserva los valores asignados.
- El perfil `users/{uid}` debe tener rol `DT` y estar activo. El equipo local debe tener `status = APROBADO` y `dtUid` igual al UID de ese DT.

[Local Emulator Suite](https://firebase.google.com/docs/emulator-suite/install_and_configure)
