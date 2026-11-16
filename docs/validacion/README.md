# QA y propuesta de entrega

Los resultados privados se guardan por defecto en `Documents/CafeSmart-QA-Privado`
del usuario. `CAFESMART_QA_ROOT` o `-PrivateRoot` permiten otro destino externo;
se rechaza cualquier destino dentro del checkout. No adjuntar evidencias al commit.

## Herramientas opcionales de QA

- `archivar-evidencias.ps1`: copia resultados generados y verifica tamano/SHA-256.
  Solo `-Retirar` permite retirar originales, individualmente, tras verificar todas
  las copias. Rechaza archivos versionados y no borra carpetas. Los nombres cortos
  evitan MAX_PATH; el manifiesto privado conserva las rutas originales.
- `validar-dos-dispositivos.ps1`: conserva las comprobaciones de APK, firma,
  fuentes y aislamiento local. `Preparar` exporta los resultados de compilacion
  antes de retirar sus copias locales; el manifiesto apunta a los APK externos.
- `qa-visual-adb.ps1`: interfaz real, capturas y resultados privados; nunca crea
  gastos para completar un caso. `-ComprobarFunciones` solo comprueba selectores.
- `diagnostico-xiaomi.ps1`: conexion, autorizacion de control y estado observable;
  no modifica seguridad ni aprueba un login. Solo captura Cafe Smart si tiene foco
  y el formulario no contiene datos. La lectura es predeterminada; `-SoloLectura` omite cualquier inyeccion y
  lee el PNG por stdout sin crear archivos en el telefono.
- `qa-private.ps1` y `qa-file-hash.ps1`: rutas, cuenta privada y hashes.

La cuenta se lee de variables de entorno o de `credenciales/auth-local.json`
en la carpeta privada. No incluir sus valores en codigo, documentacion o argumentos
de comandos compartidos. Ver tambien `tools/firebase-auth/README.md`.
Si falta un manifiesto eliminado, no se inventa su procedencia: para una instalacion
nueva se requiere `Preparar`. No hace falta repetirlo para esta limpieza.

## Propuesta de archivos para un futuro commit (sin staging)

Codigo y pruebas de la tarea:

```text
app/src/main/java/com/cafesmart/app/data/gastos/GastosRepository.kt
app/src/main/java/com/cafesmart/app/navigation/AppNavHost.kt
app/src/main/java/com/cafesmart/app/viewmodel/gastos/GastosViewModel.kt
app/src/main/java/com/cafesmart/app/viewmodel/gastos/GastosSubloteUiState.kt
app/src/main/java/com/cafesmart/app/ui/gastos/GastosSubloteScreen.kt
app/src/main/java/com/cafesmart/app/ui/auth/LoginScreen.kt
app/src/test/java/com/cafesmart/app/viewmodel/gastos/GastosSubloteTest.kt
app/src/androidTest/java/com/cafesmart/app/ui/gastos/GastosFlowTest.kt
app/src/androidTest/java/com/cafesmart/app/ui/gastos/GastosSubloteNavigationTest.kt
app/src/androidTest/java/com/cafesmart/app/ui/auth/LoginResponsiveTest.kt
app/src/main/java/com/cafesmart/app/ui/gastos/RegistrarGastoScreen.kt
app/src/main/java/com/cafesmart/app/ui/gastos/AsociarSublotesScreen.kt
app/src/main/java/com/cafesmart/app/viewmodel/gastos/GastosState.kt
app/src/main/java/com/cafesmart/app/viewmodel/gastos/GastosValidation.kt
app/src/test/java/com/cafesmart/app/viewmodel/gastos/GastosValidationTest.kt
app/src/test/java/com/cafesmart/app/viewmodel/gastos/GastosViewModelTest.kt
app/src/androidTest/java/com/cafesmart/app/ui/gastos/GastosCiclo2Test.kt
app/src/androidTest/java/com/cafesmart/app/ui/gastos/RegistroGastoResponsiveTest.kt
```

Higiene y documentacion breve:

```text
.gitignore
app/src/debug/.gitignore
docs/validacion/README.md
docs/validacion/2026-11-09-validacion-final-gastos-sublote.md
```

Herramientas opcionales, revisar como conjunto:

```text
docs/validacion/archivar-evidencias.ps1
docs/validacion/diagnostico-xiaomi.ps1
docs/validacion/qa-file-hash.ps1
docs/validacion/qa-private.ps1
docs/validacion/qa-visual-adb.ps1
docs/validacion/validar-dos-dispositivos.ps1
tools/firebase-auth/.gitignore
tools/firebase-auth/README.md
tools/firebase-auth/crear-cuenta-qa.ps1
tools/firebase-auth/firebase.json
tools/firebase-auth/iniciar-emulador-qa.ps1
tools/firebase-auth/package.json
tools/firebase-auth/package-lock.json
```

El lockfile y la configuracion demo no son resultados de pruebas ni credenciales.

Excluir capturas, APK, logs, jerarquias XML, JSON temporales, carpetas de ejecucion,
respaldos privados, `.idea`, `.codex`, `.env`, configuraciones Firebase privadas,
overlay local de `app/src/debug` y `node_modules`. Los assets de la app y la
documentacion funcional de otros integrantes se conservan.

## Compilacion de la entrega sin configuracion privada

La descarga sola falla en `processDebugGoogleServices`: falta `google-services.json`.
Es un requisito previo de main, documentado en el README principal. El CI utiliza
el ejemplo publico; para comprobar solo compilacion en una copia aislada sin el
overlay local de QA se puede usar el mismo procedimiento:

```powershell
Copy-Item app/google-services.json.example app/google-services.json
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --no-configuration-cache --no-build-cache --max-workers=2 --console=plain
.\gradlew.bat :app:testDebugUnitTest --tests com.cafesmart.app.viewmodel.gastos.GastosSubloteTest --tests com.cafesmart.app.ui.auth.LoginValidationTest --tests com.cafesmart.app.ui.auth.AuthViewModelFirebaseTest --no-configuration-cache --no-build-cache --max-workers=2 --console=plain
```

El ejemplo contiene placeholders: permite comprobar compilacion, no autenticar
contra Firebase real ni contra el emulador. Para ejecutar la app con Firebase se
necesita la configuracion autorizada del equipo o el overlay QA local excluido.
Nunca sustituir archivos privados existentes con este procedimiento.

Se requiere Android SDK 37 y sus herramientas; indicar el SDK mediante el entorno
o un `local.properties` propio. El wrapper fija Gradle 9.5.0 y el criterio del
daemon exige JDK 25. La comprobacion reutiliza SDK, JDK y cache de dependencias;
no copia resultados de compilacion ni usa cache de tareas. Un equipo nuevo
necesita acceso a los repositorios de dependencias y a la descarga del toolchain.

## Alternativa manual para Xiaomi, sin inyeccion

Navegar fisicamente en Cafe Smart. Despues de cada paso ejecutar el comando
siguiente cambiando `inicio` por la etiqueta de la tabla. No se necesita habilitar
el control ADB para leer capturas; no se vuelve a probar el permiso denegado.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/validacion/diagnostico-xiaomi.ps1 -SoloLectura -PasoManual inicio
```

| Etiqueta | Accion fisica y comprobacion esperada |
|---|---|
| `login-vacio` | Login sin datos: logo unico, campos y acciones visibles |
| `inicio` | Escribir la cuenta QA local y pulsar Entrar; comprobar Inicio |
| `inventario` | Abrir Inventario y revisar las tarjetas |
| `verde` | Seleccionar Verde |
| `detalle-1` | Ver sublotes de una calidad y abrir Detalle del sublote (ejemplo); comprobar #1 |
| `gastos-vacio-total` | Abrir Gastos del sublote; comprobar ID, mensaje vacio y total $0 |
| `volver` | Pulsar Volver; comprobar retorno al detalle #1 |
| `atras-sistema` | Reabrir Gastos, pulsar Atras del telefono y comprobar detalle #1 |

Comprobar margenes, textos, scroll, botones y ausencia de cortes en las pantallas
observadas. `login-teclado` y `fuente-ampliada` son etiquetas adicionales para
observacion manual cuando corresponda. No repetir las instrumentadas aprobadas.
No usar cuentas personales ni crear gastos para completar capturas.

Cada ejecucion crea un directorio externo nuevo y solo lee el dispositivo.
Los campos editables se omiten del XML; no se captura un formulario con datos.
Para revisar Registro de Gastos se admite `-FormularioGastos`: solo si el titulo
es `Registro de Gastos` y no hay campos de contrasena. Guarda captura y respaldo
del borrador exclusivamente en privado. No habilitar esta excepcion para Login.
Nunca crea PNG en el telefono. La etiqueta expresa el paso solicitado, no una
aprobacion: hay que revisar la imagen y documentar el resultado realmente visible.
Si no hay foco de Cafe Smart, no captura otras aplicaciones. Si el login falla,
registrar el error observado y detener el recorrido sin inventar resultados.

## Propuesta de pull request, sin publicar

Titulo: **feat: consultar gastos por sublote y compactar el Login responsive**

Descripcion propuesta:

> La ruta de gastos del detalle provisional de Inventario ahora muestra el
> sublote consultado, sus asignaciones, el total, estado vacio y errores con
> reintento. Conserva el ViewModel de la sesion en HOME y el retorno al detalle.
>
> El Login conserva sus contratos y acciones; utiliza un unico logo de 28 dp,
> menos espacio vertical y enlaces que se ajustan al ancho. Mantiene scroll para
> teclado y fuentes ampliadas, errores, recuperacion y estado de carga en Entrar.
> Se solicita revision del responsable de autenticacion por tratarse de UI compartida.
>
> Registro de Gastos reutiliza el maximo de 200 caracteres ya definido por
> `ExpenseRules`, muestra contador y bloquea escritura/pegado excesivos. El
> selector de sublotes muestra etiquetas gramaticales y contraste probado; el
> campo Monto ocupa el ancho disponible en movil. El reparto se conserva y
> valida contra el total antes de guardar.
>
> Las herramientas QA opcionales escriben fuera del checkout. No se entregan
> evidencias, credenciales ni el overlay Firebase local.
>
> Validacion nueva: copia aislada con el ejemplo publico Firebase,
> `assembleDebug`, `assembleDebugAndroidTest` y 32/32 JVM focalizadas aprobadas
> (GastosValidationTest 22 y GastosViewModelTest 10). En Pixel, 8/8 pruebas
> instrumentadas del formulario, reparto y scroll aprobadas; seis capturas
> Compose privadas a 360/390 dp.
>
> Alcance demo: Gastos sigue en memoria y el detalle de Inventario es provisional.
> Room e Inventario real quedan fuera del cambio. Lista no vacia, asignacion,
> suma invalida y errores recuperables tienen cobertura automatizada; no se
> crearon gastos en dispositivos. En Xiaomi se observo por captura de solo
> lectura la etiqueta `1 sublote seleccionado` legible y la asignacion por ID.
> El control tactil ADB sigue sin comprobar por `INJECT_EVENTS`; falta una
> captura manual del campo con teclado abierto. El recorrido anterior vacio/$0
> sigue siendo evidencia historica, no se repitio en esta validacion.

No atribuir al ejemplo publico de Firebase una validacion de login.
La lista anterior suma 22 archivos principales y 13 opcionales de herramientas;
las herramientas deben revisarse como conjunto, sin `node_modules`.
