# Café Smart: validación y propuesta de entrega

Actualizado: 15 de noviembre de 2026. Rama `feat/gastos-ciclo2`, HEAD `98d1050`.
Estado: Gastos y Login listos para revisión; cierre Xiaomi del formulario pendiente de la captura con teclado.
Sin staging, commits, push ni PR. Cuatro stashes conservados e índice vacío.

## Compilación de los archivos propuestos

La copia aislada ya aprobada contiene fuentes versionadas con los cambios de la
tarea y archivos nuevos propuestos. Excluye configuraciones privadas, overlay
Firebase local, `.idea`, `.codex`, respaldos, cachés y resultados anteriores.
Se releyeron sus manifiestos y XML: los 188 archivos de código y compilación
siguen idénticos por SHA-256. No se repitió Gradle en esta continuación.

| Comprobación de la copia aislada | Resultado conservado |
|---|---|
| `assembleDebug` sin JSON Firebase | Falla en `processDebugGoogleServices`: falta el archivo |
| Configuración para compilar | Ejemplo público `app/google-services.json.example`, igual que CI |
| `assembleDebug` con ejemplo | Aprobado |
| `assembleDebugAndroidTest` | Aprobado: compilación, sin ejecución en dispositivos |
| `GastosSubloteTest` | 7/7 |
| `LoginValidationTest` | 15/15 |
| `AuthViewModelFirebaseTest` | 7/7, Firebase simulado |

Total: 29/29 JVM, sin fallos, errores u omisiones. Build final código 0 en 5m 20s.
Se usaron SDK 37, wrapper Gradle 9.5.0 y criterio de daemon JDK 25; herramientas y
caché de dependencias existentes, sin caché de tareas/configuración ni outputs
copiados. No había init scripts ni propiedades privadas en el Gradle home usado.
El SDK se indicó mediante el entorno, sin copiar `local.properties`.

Un compañero debe preparar Firebase o copiar el ejemplo público para compilar.
El ejemplo contiene placeholders: no acredita autenticación real ni conexión al
emulador. Este requisito ya existe en main. No se alteró el original para ocultarlo.
Solo quedaron avisos de bibliotecas nativas y un `!!` innecesario en una prueba
ajena de Compras. El procedimiento reproducible está en el README de validación.

## Registro de Gastos: correcciones y validación del 15/11/2026

La regla existente `ExpenseRules.LARGO_DESCRIPCION_GASTO_MAXIMO` fija 200
caracteres. Gastos no la aplicaba ni mostraba el límite. El formulario ahora
recorta escritura y pegado a 200, evita partir pares sustitutos, presenta
contador y ayuda contextual, y vuelve a validar al guardar. La descripción sigue
siendo opcional. El campo muestra hasta cuatro líneas y el formulario conserva
el scroll vertical.

El selector heredaba el color blanco de `labelLarge` sobre una superficie clara;
ahora define explícitamente el color del texto y del fondo. Las etiquetas son
`Seleccionar sublotes`, `1 sublote seleccionado` y `n sublotes seleccionados`.
El test inspecciona el render en 0, 1 y 2 selecciones y confirma contraste sobre
4.5:1. En móviles con menos de 480 dp, Monto ocupa una fila completa y Fecha
pasa debajo. El prefijo `$` se mantiene separado del valor editable; se
comprobaron los 19 dígitos de `Long.MAX_VALUE` sin recorte. No se cambió el
modelo monetario ni su aritmética.

El mismo ViewModel conserva selección, ID de sublote, borrador y asignaciones.
Las pruebas verifican que acotar descripción preserve las asignaciones, que la
suma coincida con el total y que un reparto inválido no se guarde.

Archivos tocados por esta corrección: `RegistrarGastoScreen.kt`,
`AsociarSublotesScreen.kt`, `GastosState.kt`, `GastosValidation.kt`,
`GastosViewModel.kt`, `GastosValidationTest.kt`, `GastosViewModelTest.kt`,
`GastosCiclo2Test.kt`, `GastosFlowTest.kt` y el nuevo
`RegistroGastoResponsiveTest.kt`.

Resultados nuevos, separados de los resultados históricos:

| Comprobación | Resultado |
|---|---|
| Copia aislada pública (ejemplo Firebase; sin archivos locales privados) | `assembleDebug`, `assembleDebugAndroidTest` y 32/32 JVM focalizadas (22 validaciones, 10 ViewModel) aprobadas. SDK indicado solo por `ANDROID_HOME` del proceso. |
| Recompilación del APK AndroidTest después de ajustar las aserciones/captura | Aprobada en la copia aislada privada con overlay QA debug; esos ajustes no cambiaron código de app. |
| Pixel: nuevo formulario y casos afectados de Gastos ciclo 2/flujo | 8/8 instrumentadas aprobadas. |
| Compose a 360 y 390 dp | Límite y contador, alto acotado, ancho Monto, 19 dígitos, etiquetas 0/1/2, contraste y scroll hasta Guardar comprobados por test. Se guardaron seis PNG privados. |
| Xiaomi, Registro con teclado abierto y carácter 201 | Captura real de 1220×2712 px: `200/200 caracteres`, mensaje de límite, descripción visible y Monto `$0` visible sobre el teclado. La usuaria confirmó que al intentar otro carácter el contador sigue en 200 y el campo no acepta más texto. No se guardó gasto. |

Los tests usan un borrador Compose aislado; no guardan gastos ni llaman a
Firebase. Capturas y resultados están fuera del repositorio en
`Documents/CafeSmart-QA-Privado/registro-gastos-20261115-182456-7fb94119/` y
`Documents/CafeSmart-QA-Privado/xiaomi/diagnostico-20261115-214543-1ca0b176/` y
`Documents/CafeSmart-QA-Privado/validacion-final-xiaomi-20261115/teclado-abierto-2200/`.

## Login y Gastos

Login mantiene su firma, campos, límites, errores, visibilidad de contraseña,
recordar cuenta, Entrar, recuperación, registro, Google, ayuda, soporte y salida.
Conserva indicador de carga en Entrar y bloqueo de envíos repetidos. AuthFlow,
AuthViewModel, AuthComponents y AuthRepository no difieren de main.
El cambio de 897 a 166 líneas combina comentarios/formato y presentación: logo
único de 28 dp, tarjeta compacta, scroll, insets de teclado y enlaces adaptables.
Debe revisarlo el responsable del módulo compartido. No se volvió a probar OAuth.

Gastos usa Sublote.id, ViewModel compartido en HOME, asignaciones observables,
filtrado por sublote, total BigInteger, vacío y error recuperable/reintento.
Importes no nulos, suma, filtrado y recuperación tienen cobertura automatizada;
no se certifica su reproducción visual en el flujo normal.

## Xiaomi: alternativa manual completada para el estado vacío

Validación final ejecutada el 15/11/2026 en Xiaomi Android 16 (API 36), serial
`DEQCAQEUVS9PNRDQ`, modelo reportado por Android `24090RA29G`.

| Comprobación de acceso | Resultado |
|---|---|
| Ruta ADB | Existe en el SDK indicado por la usuaria. |
| ADB y servidor | ADB 1.0.41 se ejecuta; `adb devices -l` respondió y listó el Xiaomi como `device` (también está conectado el Pixel). |
| Café Smart | `com.cafesmart.app`, versión 1.0, instalado; `MainActivity` ya estaba en primer plano. No se abrió otra instancia, reinstaló ni borró datos. |
| Pantalla | Resolución 1220×2712, densidad 520 dpi, escala de fuente 1.1. `exec-out screencap -p` guardó un PNG válido. |
| Teclado | `dumpsys input_method` reportó `mInputShown=true` y ventana IME visible. |
| Interacción ADB | No se enviaron taps ni texto. El intento histórico fue rechazado con `SecurityException: INJECT_EVENTS`; no se repitió. `pm check-permission` no está disponible en este Android (`Unknown command`), por lo que no se afirma una comprobación nueva del permiso. |

La jerarquía leída y saneada muestra la pantalla Registro de Gastos y el
contador `200/200 caracteres`. La captura real permite observar el texto dentro
del campo, el mensaje de límite y el campo Monto completo y accesible encima del
teclado. La fecha queda parcialmente bajo el teclado, por lo que requeriría
desplazamiento para verla; no se envió un gesto ADB. La captura no muestra que se
haya pulsado Guardar. Después de la captura, la usuaria probó físicamente un
carácter adicional: confirmó que el campo no lo acepta y el contador permanece
en `200/200`. Este límite queda aprobado por observación manual; no fue una
inyección ADB ni una aserción automatizada del Xiaomi.

La captura y manifiesto SHA-256 están en
`Documents/CafeSmart-QA-Privado/validacion-final-xiaomi-20261115/teclado-abierto-2200/`.
PNG `registro-teclado-abierto.png`, 3,261,269 bytes, SHA-256
`269a15537456b25e5573749ed26eb5ea5cb8ffc924f5c678c6089567af6d230a`.
El XML y resumen iniciales, con los textos de campos redactados, están en el
subdirectorio `xiaomi/diagnostico-20261115-215914-3b484159/` de la misma carpeta.

La navegación histórica de Login, Inicio, Inventario, sublote #1 y Gastos vacío/$0
se conserva como evidencia previa, no se repitió en esta ejecución. No se volvió
a intentar INJECT_EVENTS ni se modificó seguridad. El script de diagnóstico
mantiene lectura predeterminada, sin pulsar, escribir ni crear archivos en el
teléfono.

| Observación nueva | Resultado |
|---|---|
| Login vacío | Completo, logo único y acciones visibles, sin cortes ni superposiciones |
| Registro/login | Registro reportado por la usuaria; entrada de credenciales y cuenta exacta no observadas |
| Inicio | Bienvenida, resumen y navegación observados con sesión abierta |
| Inventario | Pantalla y tarjeta Verde observadas |
| Selección Verde e intermedias | Interacción manual reportada; no se capturó cada pantalla intermedia |
| Detalle provisional #1 | Observado; sigue indicando pendiente de implementar |
| Gastos del sublote | Sublote #1 · Lote #1, mensaje vacío y total asignado $0 observados |
| Volver | Resultado observado en detalle #1 tras la acción manual |
| Atrás del teléfono | Resultado observado en detalle #1 tras reentrada y acción manual |

Las capturas revisadas no muestran cortes ni superposiciones en Login vacío y
Gastos del sublote. No se repitieron teclado, fuente ampliada ni instrumentadas.
También se observaron Gastos generales y Ajustes: no se confundieron con el módulo
objetivo. El regreso de Gastos generales a Ajustes coincide con ese origen en el
código; no se documenta como fallo del retorno del sublote.

Se conservaron diez capturas nuevas y sus diagnósticos exclusivamente fuera del
repositorio, con manifiesto privado de tamaños/SHA-256 y observaciones reales.
La imagen de Ajustes contiene perfil y queda privada, sin publicar sus valores.
Las etiquetas solicitadas del script no se consideran aprobaciones automáticas.

## Resultados históricos y privacidad

Resultados anteriores confirmados: JVM 71/71; instrumentadas Pixel 11/11 y Xiaomi
11/11; posteriores al Login, Pixel 5/5 y Xiaomi 1/1. Pixel conserva el recorrido
real aprobado de Login, Inicio, Inventario, detalle, vacío/$0 y navegación.
No se volvieron a ejecutar ni se atribuyen a esta continuación.

Las evidencias eliminadas por la usuaria no se buscaron ni se consideran
recuperadas. Las 81 evidencias archivadas y sus respaldos externos se conservaron;
no se repitió limpieza. Ningún PNG, APK, log, XML/JSON temporal o dato de perfil
forma parte de la propuesta Git.

## Git, entrega y límites

La consulta remota actual mantiene main en `98d1050`; `feat/gastos-ciclo2` sigue
sin referencia remota. La base no avanzó: no se integraron cambios ni se observan
conflictos actuales con main. Actualizar esta comprobación si main avanza.
La propuesta de [README.md](README.md) contiene 22 rutas principales (código,
pruebas y documentación) y 13 herramientas opcionales. Esta actualización
modificó código del formulario y sus pruebas, además del informe. No se preparó
el índice, ni se crearon commits, push o PR.

Pendientes explícitos: observar la secuencia de login con la cuenta QA designada
si se exige certificar ese caso exacto; revisar el cambio compartido de Login;
validación visual de lista no nula y error real cuando haya un mecanismo seguro.
Room e Inventario real siguen fuera del alcance demo. No se crearon gastos,
modificaron datos reales, borraron archivos ni alteraron los cuatro stashes.
