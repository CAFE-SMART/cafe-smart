# PR #31: limpieza local e integración de main

## Alcance y protección

Preparación local del PR de Gastos, sin publicar cambios ni implementar Room para Gastos.

- Rama de trabajo: `fix/pr31-limpieza-main`, creada desde `feat/gastos-ciclo2` (`6c7ecf7b355365e3163a550721155d86f0d83994`).
- Respaldo completo: `backup/pr31-antes-limpieza-main-20261116`, en ese mismo commit. Contiene todos los archivos originales retirados del diff, incluidas herramientas y evidencias.
- `origin/main` incorporado: `2f3681d3dc1bfadd5d38ab9f01f2de054de02a8b`, fusión del PR #30. Se utilizó `git merge --no-commit --no-ff origin/main` y se revisó el árbol antes de confirmar.
- No hubo conflictos de fusión ni decisiones entre implementaciones rivales. La semilla de debug, su alternativa vacía de release, MainActivity, README y sus pruebas quedan exactamente como en main.
- El PR #26 seguía abierto al consultar, con cabecera `b4204b9a1642503ff1399459ed8a0986723b1097`. No se incorporó desde ninguna rama ni se trasladaron las correcciones de Compras de `feat/gastos-room-home`.
- Ramas originales protegidas: `feat/gastos-room-home` en `ffa2f5be77906fda310469ad9dbaaf40630bdbaf`; `feat/gastos-ciclo2` en `6c7ecf7b355365e3163a550721155d86f0d83994`; `main` local en `4b32cc86541eab6bdfaceb27431f9ccb5d06fa22`.
- Cuatro stashes preservados, incluidos ambos «Respaldo Home antes de Gastos ciclo 2»: `f3e0b1cefc893cc59fae4aed716c4a54ce5aa929`, `28eb672850a7487d8a4f39012a91ae613b1aca0f`, `0380e2c3ebe3992c640b498730e21b0124e9e720`, `bb2bd11694d2c1da54e6d4b04e13de64245e6ce8`.

## Qué se conserva y qué se excluye frente a main

Se conserva íntegro el código de Gastos del PR: registro, límites y validaciones de campos, selección múltiple y asignación de importes, lista general, lectura de gastos por sublote y estados de carga/error/reintento. También sus pruebas unitarias e instrumentadas. `AppNavHost.kt` conserva `GastosSubloteRoute`; Lista, Registro y Selector siguen compartiendo el ViewModel con propietario HOME.

`DemoGastosRepository` permanece explícitamente en memoria. Su sustitución por Room corresponde a #71, no a esta limpieza. No se presenta la demostración como persistencia real ni se modifica Inicio.

Se recuperó **LoginScreen.kt exactamente desde origin/main**, pues su rediseño no era necesario para Gastos. No se tocaron AuthViewModel, validaciones, repositorios de autenticación ni preferencias de sesión. La prueba `LoginResponsiveTest` del diseño descartado se retiró: exigía, por ejemplo, un solo logo y visibilidad inicial sin scroll. Se añadió `LoginMainRegressionTest`, limitada a los controles y navegación requeridos por esta revisión, sin imponer un diseño nuevo.

Se excluyeron del diff:

```text
tools/firebase-auth/.gitignore
tools/firebase-auth/README.md
tools/firebase-auth/crear-cuenta-qa.ps1
tools/firebase-auth/firebase.json
tools/firebase-auth/iniciar-emulador-qa.ps1
tools/firebase-auth/package-lock.json
tools/firebase-auth/package.json
docs/validacion/2026-11-09-validacion-final-gastos-sublote.md
docs/validacion/README.md
docs/validacion/archivar-evidencias.ps1
docs/validacion/diagnostico-xiaomi.ps1
docs/validacion/qa-file-hash.ps1
docs/validacion/qa-private.ps1
docs/validacion/qa-visual-adb.ps1
docs/validacion/validar-dos-dispositivos.ps1
app/src/androidTest/java/com/cafesmart/app/ui/auth/LoginResponsiveTest.kt
```

La evidencia histórica mezcla Login, dispositivos, configuración de emulador y herramientas generales; se conserva completa en el respaldo en lugar de presentarla como resultado de esta revisión. Las herramientas Firebase y sus dependencias se excluyen por ser infraestructura local de QA. Los archivos ignorados existentes no se borraron. Las reglas de `.gitignore` y `app/src/debug/.gitignore` se conservan para excluir credenciales, overlays de QA y artefactos privados de la entrega.

La limpieza afecta al diff del PR, no reescribe su commit original: los archivos retirados continúan disponibles en su historia y en el respaldo. Si alguna vez apareciera un secreto real en esa historia, retirarlo del diff no equivaldría a revocarlo ni sanear la historia.

## Login y navegación comprobados

- Validación de correo/contraseña vacíos: no llama al repositorio; credenciales sintéticas válidas sí lo llaman.
- «Recordar mi cuenta»: semántica de casilla y cambio de estado; conserva la selección al ir a Recuperar contraseña y volver.
- «¿Olvidaste tu contraseña?»: acción clicable y navegación real a `Routes.FORGOT_PASSWORD`, con retorno al Login.
- Mostrar/ocultar contraseña conserva las descripciones accesibles. Las aserciones semánticas son una comprobación básica, no una auditoría completa de accesibilidad ni evidencia de TalkBack/WCAG.
- La regresión N01 comprueba entrada real por el formulario, navegación a Inicio, propietario de Gastos y cierre/reingreso usando autenticación local inyectada.
- No se enviaron correos ni se autenticaron cuentas reales contra Firebase. La recuperación y decisiones de sesión también tienen pruebas unitarias con fuentes controladas. No se afirma validación del servicio remoto.

## Pruebas ejecutadas

Sobre el árbol integrado y limpio de los cambios de Login/herramientas:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest --console=plain
```

Resultado: **BUILD SUCCESSFUL**, 3 min 5 s. **514 pruebas unitarias, 0 fallos, 0 errores, 0 omitidas**. Incluyen 15 de LoginValidationTest, 11 de RecuperarContrasenaViewModelTest, 3 de SesionInicialTest y 39 de Gastos. La cifra no se compara con la otra rama como pérdida de pruebas: esta rama no incorpora el PR #26 ni su auditoría local.

Se actualizaron ambos APK en `emulator-5554` mediante `adb install -r`, sin desinstalar ni limpiar datos. El primer arranque instrumentado terminó en `failed to attach`, sin ejecutar casos. Un segundo intento inició las pruebas sin cambios de configuración. No se atribuye ese fallo al producto ni se cuentan casos como aprobados en ese intento.

Resultado instrumentado final: **15 aprobadas, 0 fallidas**, 191,143 s. Incluye LoginMainRegressionTest (2), GastosCiclo2Test (3), GastosSubloteNavigationTest (4), GastosNavigationScopeTest (1), RegistroGastoResponsiveTest (4) y G16 de GastosFlowTest (1). Cubren lista/registro, vacío y filtros, selección múltiple con conservación del borrador/reparto, gasto por sublote y reintento, límites/teclado/contraste del formulario y guardado desde Inicio. Total de esta revisión: **529 pruebas aprobadas (514 unitarias + 15 instrumentadas)**.

Las pruebas usan el host Compose y autenticación inyectada; no lanzan MainActivity ni la semilla para poblar bases personales. No se ejecutó `connectedDebugAndroidTest` ni pruebas que borren bases en disco. El teléfono físico conectado no se utilizó.

Las salidas completas y las verificaciones locales se conservan en `.qa-backups/pr31-limpieza-main/`, excluido de Git. La configuración debug local preexistente se mantuvo; se registraron y compararon sus hashes sin copiar sus valores a este informe.

## Revisión de credenciales y del diff

El escaneo del diff final no detectó patrones de claves privadas, cuentas de servicio, claves API de Google ni tokens de proveedores. También se revisaron los 35 archivos del PR original: dos scripts retirados contenían una clave ficticia de emulador, formada por el prefijo de prueba y 33 ceros. No se incluyen esos valores en el informe. La revisión por patrones no constituye garantía de detección de cualquier secreto posible.

LoginScreen y el código de Compras/DAO son idénticos a origin/main. El código original de Gastos y su navegación son idénticos al respaldo. No se incorporan auditorías ni parches de la rama protegida. El commit que contiene este informe identifica el resultado local revisado.

## Pendientes de #71 y #78

- #71: entidades/DAO y repositorio de Gastos en la base existente, migración no destructiva, IDs e idempotencia y lectura real de sublotes. Conservar el ViewModel existente y su propietario HOME. Coordinar el aislamiento por negocio antes de cerrar el esquema.
- #78: observar Gastos persistentes e Inventario real, reutilizar la fuente de #76 cuando esté disponible y sustituir cifras fijas de Inicio con estados de carga/error. Los ejemplos sembrados por #30 son ficticios aunque estén almacenados en Room; no acreditan datos reales del negocio.
- Antes de retomar esas tareas en `feat/gastos-room-home`, comparar los nuevos commits de Carlos con las correcciones locales y coordinar su integración. Esta ejecución no actualizó ni cambió esa rama.

## Publicación posterior, solo con nueva autorización

La rama del PR y GitHub permanecen intactos. No se ejecutó push ni se modificó el PR #31. Para actualizarlo posteriormente:

1. Verificar árbol limpio, crear un respaldo adicional y ejecutar `git fetch origin`.
2. Confirmar que la cabecera remota del PR sigue siendo ancestro de `fix/pr31-limpieza-main`: `git merge-base --is-ancestor origin/feat/gastos-ciclo2 fix/pr31-limpieza-main`. Si no lo es, integrar y revisar primero el trabajo nuevo; no forzar la referencia.
3. Comprobar si origin/main avanzó desde el commit validado, integrar sus cambios si corresponde y repetir las pruebas afectadas. Revisar `git diff origin/main...fix/pr31-limpieza-main`.
4. Solo tras autorizar expresamente la publicación, un push normal de `fix/pr31-limpieza-main:feat/gastos-ciclo2` actualiza el PR existente, sin crear otro y sin force-push. Git rechazará un avance remoto concurrente incompatible; en ese caso, volver a comparar e integrar.
5. Verificar en GitHub que el diff final y el CI correspondan a la revisión. No fusionar el PR sin una autorización independiente.
