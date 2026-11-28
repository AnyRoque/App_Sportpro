# Migración de Firebase

El script `scripts/Invoke-SportProFirestoreMigration.ps1` permite inventariar el origen, generar respaldo y correspondencias, copiar a un destino demo, verificar y ensayar la restauración. Los modos de aplicación y restauración están limitados a proyectos demo. Los documentos de usuario mantienen el UID de Firebase Auth. Los datos de autenticación y los objetos Supabase tienen procedimientos de respaldo independientes.

1. Congela las escrituras del entorno que vayas a inventariar. Preserva las reglas desplegadas y guarda el respaldo y plan con acceso restringido.
2. En un emulador con datos sintéticos, ejecuta `DryRun`. Revisa las huellas SHA-256, los máximos de los contadores, los vínculos, las subcolecciones y la separación de contactos privados.
3. En un destino demo separado ejecuta `Apply` y `Verify`. Repite `Apply` para probar reanudación sin duplicados. Para conservar rutas antiguas, ejecuta `Restore` en un tercer entorno.
4. Antes de un corte real, compara documentos y relaciones, verifica las consultas de clientes antiguos y define el destino y la ventana de mantenimiento. La inicialización remota de contadores requiere autorización expresa.
5. Para restaurar, conserva el respaldo previo, las reglas y el APK anterior. Reconcilia las escrituras posteriores al corte en un entorno aislado antes de redirigir clientes.

Los comandos completos, las opciones de autenticación y la recuperación están en la [guía de migración](docs/MIGRATION_GUIDE.md). El estado del ensayo integrado está en [PRUEBAS_FUNCIONALES.md](PRUEBAS_FUNCIONALES.md).
