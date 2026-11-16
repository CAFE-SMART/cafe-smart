# Authentication Emulator local (opcional)

Este entorno es exclusivo de QA debug. Usa el proyecto demo, loopback y
`adb reverse tcp:9099 tcp:9099`; nunca Firebase de produccion.
El overlay actual de `app/src/debug` se conserva localmente y esta excluido de Git.
No se incluyen cuentas, contrasenas, tokens ni configuraciones privadas en la entrega.

## Salidas y cuenta privada

Por defecto se usa `Documents/CafeSmart-QA-Privado` del usuario. Puede sustituirse
con `CAFESMART_QA_ROOT` o `-PrivateRoot`, siempre fuera del checkout.
Los scripts rechazan un destino dentro del repositorio.

La cuenta se obtiene de `CAFESMART_QA_EMAIL` y `CAFESMART_QA_PASSWORD`, o de
`credenciales/auth-local.json` dentro de la carpeta privada, con propiedades
`email` y `password`. No crear ese archivo dentro del proyecto ni incluirlo en Git.
El registro local no guarda tokens ni imprime la contrasena.

## Uso con las instalaciones existentes

- Dependencias fijadas en `package-lock.json`; `node_modules` queda ignorado.
- `npm run start` inicia solo Auth desde una carpeta de ejecucion privada. La
  configuracion usada y los logs de Firebase quedan alli, sin login o deploy remoto.
- `crear-cuenta-qa.ps1` verifica el proyecto demo y utiliza exclusivamente el
  servidor local para disponer de la cuenta y comprobar su respuesta REST.
  REST no demuestra un login aprobado en la interfaz.
- `docs/validacion/validar-dos-dispositivos.ps1 -Accion Preparar` compila debug y
  androidTest, exporta los resultados por tamano/SHA-256 y registra los APK
  externos en un manifiesto privado. No ejecuta tests por si solo.
- Las acciones `ConectarAuthLocal`, `Instalar`, `VerificarAuthLocal` y
  `Capturar` requieren serial explicito y conservan las comprobaciones de firma,
  recursos demo, fuentes y aislamiento. No instalan sin manifiesto verificable.
- `docs/validacion/qa-visual-adb.ps1` guarda capturas, XML saneados, logs,
  resultados y el informe completo fuera del checkout. No crea gastos.
- `docs/validacion/diagnostico-xiaomi.ps1` comprueba conexion y control sin
  cambiar la seguridad del telefono. No aprueba por si solo el recorrido visual.

La restriccion INJECT_EVENTS requiere habilitacion fisica de la usuaria o QA
manual documentado. No se desinstala, no se borra almacenamiento, no se cambia
la firma y no se fuerza un downgrade. Las actualizaciones reinician el proceso:
no instalar sobre gastos demo sin persistir.

Los resultados antiguos eliminados por la usuaria no se reconstruyen ni se
presentan como evidencia disponible. Conservar por separado cada nueva ejecucion.