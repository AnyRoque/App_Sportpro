# Migración y recuperación

El script PowerShell inventaría documentos y subcolecciones con la API REST de Firestore y conserva las representaciones tipadas del backend. Genera un respaldo y un plan con SHA‑256. Los dos artefactos pueden contener datos personales o claves históricas. Guárdalos fuera del repositorio, con acceso restringido. La huella comprueba integridad del archivo durante su transferencia. Exporta y custodia por separado Firebase Authentication y el inventario de objetos de Supabase si necesitas recuperar sistemas externos.

## Ensayo

1. Empieza con datos sintéticos y congela las escrituras durante el inventario para obtener un estado coherente. La API recorre padres ausentes para descubrir descendientes. Captura todas las páginas.
2. Genera el respaldo y la correspondencia.

   ```powershell
   $migrate = '.\scripts\Invoke-SportProFirestoreMigration.ps1'
   & $migrate -Mode DryRun -Project demo-sportpro -SourceEmulator 127.0.0.1:8080 -Workspace 'F:\SportProReview\inventario'
   ```

3. Revisa `firestore-plan.json` por entidad y comprueba los UID de administradores. Examina además referencias en campos libres, claves dinámicas, almacenamiento, reacciones y notificaciones. Los IDs de texto solo se reescriben en campos y tipos conocidos. Las referencias nativas internas sin documento detienen la copia para revisión.
La copia prepara los vínculos de las convocatorias, alineaciones y resúmenes, los índices de reportantes y los perfiles privados. Para el detalle de entrenamiento de jugadores y tutores, reconstruye las proyecciones que falten tomando los valores registrados en la asistencia grupal. El respaldo conserva la estructura original para restauración.

4. Arranca un destino de demostración vacío separado, con su propio puerto o instancia. Aplica la copia usando el mismo respaldo y plan. Puedes reanudar con los mismos archivos si los documentos ya copiados coinciden.

   ```powershell
   & $migrate -Mode Apply -Project demo-sportpro -DestinationProject demo-sportpro-migrated -DestinationEmulator 127.0.0.1:8081 -Workspace 'F:\SportProReview\inventario'
   & $migrate -Mode Verify -Project demo-sportpro -DestinationProject demo-sportpro-migrated -DestinationEmulator 127.0.0.1:8081 -Workspace 'F:\SportProReview\inventario'
   ```

5. Con otro proyecto demo vacío, ejecuta `Restore` para demostrar la recuperación con rutas antiguas y verificar que el volumen de documentos coincide. Compara recuentos, miembros, equipos, eventos, marcador, mensualidades, adjuntos y relaciones de navegación desde la app.
6. Para inicializar una base sin migrar documentos, usa `Seed` con el inventario de esa base y el destino explícito. El modo conserva valores anteriores y usa precondición de escritura para evitar sobrescribir un aumento concurrente.

## Ensayo automatizado

Con el emulador en `127.0.0.1:8080`, `scripts/Test-SportProFirestoreMigration.ps1` crea tres proyectos `demo-sp-*` con datos sintéticos, ejecuta dos veces la copia y comprueba tanto el destino como la restauración. `scripts/Test-SportProFirestorePlan.ps1` comprueba la correspondencia con un respaldo sintético sin necesitar el emulador. En Windows, si el sistema ejecuta solo scripts firmados por defecto, se usó `powershell.exe -NoProfile -NonInteractive -ExecutionPolicy RemoteSigned -File` para el proceso de prueba de estos scripts locales.

## Corte y vuelta atrás

En producción, inventaría mediante `DryRun` con un token IAM de corta duración suministrado en `SPORTPRO_FIRESTORE_TOKEN`, con lectura restringida. `Apply` y `Restore` operan solo en proyectos demo aislados. `Seed` remoto requiere `-AuthorizeLiveWrite` y autorización previa del titular. El primer corte real requiere congelar escrituras, respaldar las reglas desplegadas, comparar los datos ensayados, decidir cómo redirigir clientes a rutas nuevas y autorizar de forma explícita cualquier retirada de documentos originales. Las colecciones enumeradas por consultas producirían duplicados si se copian en la misma base mientras coexisten ambas versiones sin una estrategia de lectura.

Para volver atrás, conserva el respaldo, las reglas, las referencias de almacenamiento y el APK anterior. Ensaya `Restore` en un entorno separado. Registra las escrituras que ocurrieron después del corte y decide cómo reincorporarlas antes de redirigir clientes. El script de ensayo evita eliminar o sobrescribir datos que difieran del plan.

[Referencia REST para listar documentos](https://firebase.google.com/docs/firestore/reference/rest/v1/projects.databases.documents/list)
