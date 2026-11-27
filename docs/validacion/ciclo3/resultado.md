# Validación local de Límites y Gráficas

Rama `feat/integracion-ciclo3`. Ejecución en `QA_Desechable_20261123` (`emulator-5554`), con componentes Compose y datos ficticios. Estas capturas no son de una sesión autenticada ni acreditan navegación completa.

- `limites-predeterminados.png`: pantalla de Límites con almacenamiento en memoria; se confirmó visualmente el contraste del botón de restablecimiento tras corregirlo.
- `grafica-vacia.png`: componente de gráfica con estado vacío ficticio.
- `grafica-gastos-ficticios.png`: componente con dos meses de gastos ficticios; ingresos y utilidad no aparecen como cifras.

## Resultados

- `testDebugUnitTest`: 1.032 pruebas, 0 fallos, 0 errores, 0 omitidas.
- `assembleDebug` y `assembleDebugAndroidTest`: compilación correcta.
- Instrumentación individual: `GraficasFinancierasUiTest` 3/3 y `LimitesEntradaUiTest` 2/2.
- Tras corregir el contraste y agregar la prueba del negativo: ejecución conjunta de esas clases y `EvidenciaCiclo3UiTest`, 8/8.
- La prueba de preferencias escribe, lee desde otra instancia y restablece en el emulador; no comprueba un reinicio completo de la aplicación.
- El error previo de instrumentación coincidió con un ANR de arranque de la app y presión de CPU elevada en el emulador. Tras completar el arranque las pruebas se ejecutaron; no se ha demostrado una causa única ni una corrección permanente del entorno.

## Pendientes y protección

No se instaló ninguna APK en el Xiaomi. El paquete protegido `com.cafesmart.app.qatest20261123` se observó instalado en el usuario 0; no se leyó ni modificó su base. No se ha verificado visualmente el inventario ni la cifra de 834 kg en esta ejecución.

Pendientes: validación física con una variante aislada y Firebase de prueba verificado; autenticación financiera y navegación completa; cierre y reapertura real de la app; recorridos de Compras, Gastos y regresión visual; pruebas de datos ficticios de extremo a extremo. El diseño y los contratos siguen pendientes de revisión del equipo. No se realizó commit, push ni merge.

## Continuación: Xiaomi físico

La sección anterior describe el estado antes de esta continuación. Se generó en una copia temporal una APK debug con `applicationId` `com.cafesmart.app.qa.ciclo3`; su APK de instrumentación usa `com.cafesmart.app.qa.ciclo3.test` y apunta al paquete nuevo. Ambas usan la misma firma Android Debug (SHA-256 `7b853db1c9841f503fb7ac01efd3a2898781cb60a689f655b1810f6c1c14ec74`). Ninguno de los identificadores existía previamente en los perfiles consultados. Room usa un directorio privado diferente y esquema versión 3, sin migración destructiva.

La configuración de QA inicializa exclusivamente el proyecto sintético `demo-cafesmart-qa`, desactiva recopilación y conecta Auth al emulador local, sin alternativa remota. Se verificó el servidor y una prueba instrumentada inició sesión en el Xiaomi con una cuenta ficticia (1/1). El paquete protegido no fue reinstalado ni modificado; sus metadatos de instalación observados antes y después coinciden. No se leyó su base y no se ha comprobado visualmente la cifra de 834 kg.

Resultados físicos:

- Componentes: `LimitesEntradaUiTest` 2/2, `GraficasFinancierasUiTest` 3/3 y `EvidenciaCiclo3UiTest` 3/3. Las imágenes están en `xiaomi-componentes/`; sus estados y gastos son ficticios. La captura de Límites se tomó en un host de pruebas Compose y el borde inferior queda parcialmente oscurecido por la barra del sistema; no prueba la presentación de la actividad completa.
- Recorrido real de Ajustes → Límites: se guardaron los seis valores, se detuvo el proceso aislado y se confirmaron tras reiniciarlo. Se restablecieron los valores originales y se verificaron después de otro cierre completo. Tres pruebas instrumentadas seleccionadas, 3/3.
- Una primera ejecución de pruebas de componentes quedó esperando al no estar la actividad en primer plano y fue interrumpida; luego las clases pasaron de forma independiente. Dos intentos del recorrido financiero no pasaron: el primero por una condición de espera del arnés y el segundo por tiempo de espera con la pantalla bloqueada. No se clasifican como fallos funcionales de la app ni como pruebas aprobadas.
- El acceso a la gráfica dentro del Resumen financiero, su contraseña, las operaciones de Compras y Gastos de extremo a extremo, y la regresión visual completa siguen pendientes. Las capturas de los componentes de gráficas no prueban ese recorrido.

Las pruebas JVM y del emulador indicadas arriba son resultados de la ejecución anterior y no se repitieron de forma indiscriminada. No se hizo commit, push ni merge; los cuatro stashes se conservaron.

## Continuación: resumen financiero en Xiaomi

Con el Xiaomi desbloqueado, la variante aislada en primer plano y Auth Emulator respondiendo, `FinanzasFisicoQaTest` pasó 1/1. Desde Ajustes se creó una clave financiera ficticia, se accedió al Resumen, se observó «Mes a mes» y su estado vacío, y al salir y regresar se confirmó que se vuelve a pedir la clave. La captura `xiaomi-componentes/resumen-real-vacio.png` corresponde al recorrido real, revisada para excluir credenciales y datos privados.

La captura también muestra que la tarjeta general del resumen presenta una ganancia de $0 y afirma que no hay ventas, aunque las fuentes de ventas y compras no están conectadas. La gráfica advierte sobre esa falta de conexión; la representación del resumen general debe revisarse con el equipo antes de concluir que los datos financieros son completos.

Durante la preparación del caso con un gasto ficticio, el Xiaomi volvió a bloquear la pantalla; se detuvo el recorrido conforme a la instrucción. La nueva prueba se compiló en la copia temporal, pero no se ejecutó ni insertó gastos. Quedan pendientes el resumen con gastos ficticios, las validaciones de extremo a extremo de Compras y Gastos con límites modificados y restablecimiento, y la regresión de Home, Inventario, Gastos, Compras y Ajustes. No se ejecutaron de nuevo las pruebas ya aprobadas.

## Incidente: desinstalaciones desde el lanzador del Xiaomi (26-nov-2026)

Al retomar la validación, `com.cafesmart.app.qa.ciclo3` ya no estaba instalada y, minutos después, también desapareció `com.cafesmart.app.qatest20261123` (la instalación protegida con los 834 kg de la cuenta QA). El registro del sistema del Xiaomi (filtrado solo por los paquetes de Café Smart) muestra que **ambas desinstalaciones las pidió el lanzador de MIUI (`com.miui.home`) desde su diálogo «Desinstalar»**, no ADB ni ninguna prueba:

- 23:08:12 `Uninstall pkg: com.cafesmart.app.qa.ciclo3 u0 flags:4 ... of com.miui.home`
- 23:14:27 `Uninstall pkg: com.cafesmart.app.qatest20261123 u0 flags:4 ... of com.miui.home` → `PACKAGE_FULLY_REMOVED` (se borraron sus datos).

Consecuencias:
- La base Room de la cuenta QA (los 834 kg) **se perdió**: no queda en `pm list packages -u` y nunca se hizo respaldo (estaba prohibido extraer bases). No se puede recuperar.
- Quedan instalados solo `com.cafesmart.app.qatest20261123.test`, `com.cafesmart.app.qa.ciclo3.test` y `com.cafesmart.app.qa20261117.test` (paquetes de instrumentación, sin datos del negocio).
- La APK aislada se volvió a compilar y verificar (`com.cafesmart.app.qa.ciclo3`, proyecto `demo-cafesmart-qa`, firma debug `7b853db1…`), pero **no se instaló**: la validación se detuvo por el incidente.
- Firebase Auth Emulator sigue activo (proyecto `demo-cafesmart-qa`, cuenta ficticia presente) y `adb reverse tcp:9099` también.

Estado de los bloques pendientes: Gráfica con gasto ficticio, Límites en Compras y Gastos y Regresión visual: **BLOQUEADOS**, sin ejecutar.

## Continuación: reinstalación y las tres validaciones pendientes (27-nov-2026)

La usuaria confirmó que ella desinstaló las apps de Café Smart del teléfono. Se reinstaló solo la variante aislada.

**Instalación**
- APK: `com.cafesmart.app.qa.ciclo3` (SHA-256 `15a38634…`) y `com.cafesmart.app.qa.ciclo3.test` (`d3abc3f3…`), compiladas en la copia temporal de QA, cuyo código de producción es igual al del worktree (incluye Límites y Gráficas). Proyecto Firebase `demo-cafesmart-qa`, firma debug `7b853db1…`, Room versión 3, perfil 0. MIUI pidió «Instalar vía USB» y la usuaria lo aceptó.
- Firebase Auth Emulator activo (puerto 9099, `adb reverse` activo). Inicio de sesión por la pantalla real con la cuenta ficticia del emulador: correcto.
- Al ser una instalación nueva, la semilla de ejemplo de depuración cargó 834 kg en la base AISLADA de esta app (no son los datos de la instalación protegida, que se perdieron al desinstalarla).

**Matriz de casos nuevos (Xiaomi, recorrido completo por la interfaz real)**

| # | Caso | Resultado | Evidencia |
|---|---|---|---|
| 1 | Login con cuenta ficticia del emulador | APROBADO | `Q01_login`, `Q02_home_tras_login` |
| 2 | Crear clave financiera y entrar al Resumen; «Mes a mes» vacío | APROBADO | `G01_resumen_inicio` |
| 3 | Registrar gasto ficticio «QA grafica ciclo3» $ 75.000, pagado, general, 26-11-2026 | APROBADO (Room: 1 gasto, 75.000, sin repartos) | `G02`–`G04` |
| 4 | Gráfica: una barra de Gastos en **Nov**, escala «hasta $ 75.000» | APROBADO | `G05_resumen_con_gasto` |
| 5 | Sin doble conteo: «Gastos registrados $ 75.000» = Room; escala no es 150.000 | APROBADO | `G06_grafica_mes_a_mes` |
| 6 | Ajustes → Límites: cambiar peso 10–500, precio 2.000–50.000, gasto 5.000–900.000 y guardar | APROBADO (SharedPreferences con los 6 valores) | `L01`–`L03` |
| 7 | Compras: ayudas muestran los límites nuevos al instante | APROBADO | `C02_compra_paso2` |
| 8 | Compras: 5 kg y 600 kg → «Escribe un peso entre 10 y 500 kg.»; 100 kg válido | APROBADO | `C03_peso_limites`, `C03_compra_errores_limites` |
| 9 | Compras: $ 1.500 y $ 60.000 → «Escribe un valor entre $2.000 y $50.000 pesos colombianos.»; $ 15.000 válido | APROBADO | `C03_compra_errores_limites`, `C04_precio_valido` |
| 10 | Gastos: $ 4.000 → «El valor mínimo es 5.000 pesos»; $ 950.000 → «El valor máximo es 900.000 pesos»; $ 50.000 pide confirmar (se canceló) | APROBADO | `E00`–`E02` |
| 11 | Gastos: el campo no admite más dígitos que el máximo (1234567 → 123.456) | APROBADO | — |
| 12 | Restablecer → preferencias vacías → cierre total (`force-stop`) → reabrir | APROBADO | `L04_restablecer`, `L05_tras_reabrir` |
| 13 | Tras reabrir, Compras: 5 kg y $ 1.500 válidos; ayudas «Entre 1 y 99.999 kg», «Entre $ 1.000 y $ 100.000» | APROBADO | `C05_compra_tras_restablecer` |
| 14 | Tras reabrir, Gastos: $ 500 → «El valor mínimo es 1.000 pesos»; $ 950.000 aceptado (se canceló) | APROBADO | `E03`, `E04` |
| 15 | Regresión: Inicio, Inventario (834 kg, Verde, grupo), menú de orden, Gastos (lista y Pagados), Ajustes, Resumen y regreso | APROBADO con observaciones | `V01`–`V08`, `R01`, `G05`, `G06` |

Ninguna compra se confirmó; en Room solo queda el gasto ficticio de $ 75.000.

**Problemas encontrados (sin corregir; ninguno impide el uso)**
1. **«Tu ganancia de hoy $ 0»** (Resumen financiero): la utilidad se calcula solo con ventas (`resumirVentas`). Como las ventas no están conectadas (`OrigenFinanciero.ventas()` devuelve vacío), la tarjeta siempre muestra $ 0, aunque haya $ 75.000 de gastos. Propuesta para Andrés (sin tocar su código): que `OrigenFinanciero` informe qué fuentes están conectadas y, mientras ventas no lo esté, la tarjeta diga «La ganancia se calculará cuando se conecten las ventas» en vez de «$ 0». La gráfica ya avisa esto.
2. **Límites de entrada sin aviso al guardar**: se guardan bien, pero la pantalla no confirma nada.
3. **Límites de entrada sin separador de miles** (2000 en vez de 2.000), distinto del resto de la app; la línea de «Valores originales» tampoco los usa.
4. «Volver a los valores originales» actúa sin pedir confirmación (decidir con el equipo si debe pedirla).

**Pendiente de decisión**
- Andrés: tarjeta de ganancia (punto 1), diseño aprobado de Límites y de Gráficas, textos nuevos.
- Katherine / equipo: peso mínimo (PR #41).

**Protección**
- La instalación protegida `com.cafesmart.app.qatest20261123` ya no existe (la desinstaló la usuaria antes de esta continuación); no se instaló ni se tocó nada con ese nombre. Siguen instalados sus paquetes de instrumentación (`qatest20261123.test`, `qa20261117.test`).
- Cuatro stashes intactos. Sin commit, push ni merge.

## Corrección final de Límites (27-nov-2026)

- **Aviso al guardar:** la pantalla usa el aviso superior de la app (`AvisoSuperior`): verde con «Límites guardados.» o «Se restablecieron los valores originales.», rojo con «Revisa los campos indicados.». Se anuncia a TalkBack y se ve aunque el formulario esté desplazado. Antes el texto quedaba al final de la zona desplazable y no se veía.
- **Puntos de miles:** los campos muestran «99.999», «100.000» y «50.000.000», y al editar solo se guardan los dígitos, así que lo que reciben la validación y las preferencias no cambia. Se conserva el «-» inicial para que siga el aviso de negativos. «Volver a los valores originales» mantiene su comportamiento.
- **Pruebas:**
  - JVM 1036/1036 (4 nuevas en `FormatoLimitesTest`).
  - `LimitesEntradaUiTest` 3/3 en el Xiaomi (variante aislada). Un primer intento dio 1/3 con «No compose hierarchies found» porque otra app estaba al frente; con la app aislada al frente pasaron las 3.
  - Comprobación visual: valores con puntos, edición «1.000 → 2.500», aviso verde y valor guardado como `2500`; después se restablecieron los originales.

## Evidencias incluidas en el repositorio

Para no subir 112 MB, el repositorio incluye solo 8 capturas del Xiaomi, en `evidencias/` (JPEG de 800 px de ancho, calidad 90, ≈ 1,7 MB en total):

| Archivo | Qué muestra |
|---|---|
| `G05_resumen_con_gasto.jpg` | Gráfica «Mes a mes» con el gasto ficticio de $ 75.000 en noviembre |
| `G06_grafica_mes_a_mes.jpg` | «Gastos registrados $ 75.000», sin doble conteo, y la tarjeta de ganancia en $ 0 |
| `C03_compra_errores_limites.jpg` | Compras rechaza 5 kg y $ 60.000 con los límites modificados |
| `E00_gasto_minimo.jpg` | Gastos: «El valor mínimo es 5.000 pesos» |
| `E01_gasto_maximo.jpg` | Gastos: «El valor máximo es 900.000 pesos» |
| `L05_tras_reabrir.jpg` | Límites tras restablecer, cerrar y volver a abrir la app |
| `L07_aviso_guardado.jpg` | Aviso «Límites guardados.» y valores con puntos de miles |
| `V06_menu_orden.jpg` | Menú «Más reciente / Más antiguo» legible |

Las 40 capturas originales del Xiaomi y las 3 del emulador, que este documento nombra arriba como `xiaomi-componentes/...` y `*.png`, se conservan como respaldo local fuera del repositorio (`AndroidStudioProjects\respaldos\cafe-smart-ciclo3-evidencias-originales`). No contienen contraseñas ni datos personales.

## Pendientes del equipo (NO resueltos)

**Andrés**
1. Aprobar la pantalla **Ajustes → Límites de entrada** (no hay Figma aprobado). Esta rama habilita la fila en `SettingsScreen.kt`, que antes decía «Próximamente», y muestra un mismo límite de precio para compra y venta.
2. Decidir si «Volver a los valores originales» debe pedir confirmación.
3. Aprobar el diseño de las **Gráficas financieras**. Mientras no haya ventas conectadas, la gráfica muestra solo gastos.
4. Decidir sobre la tarjeta **«Tu ganancia de hoy $ 0»**: propuesta en `propuesta-ganancia-resumen.md`.
5. Aprobar los textos nuevos que ve el usuario (avisos de Límites, «Mes a mes» y los mensajes de las gráficas).

**Katherine (y el equipo)**
1. Decidir el **peso mínimo de compra**: 1 kg (PR #38, en `main`) o «mayor que 0» (PR #41). Hay conflicto en `PurchaseRules.kt`. Esta rama mantiene 1 kg con `PESO_MIN_KG` como alias obsoleto.
2. Conectar Ventas (#43) y la merma de Secado (#44-#46) a `OrigenFinanciero`, para que las gráficas y el Resumen muestren ingresos y utilidad.

## Comandos para ejecutar y verificar las pruebas

La guía completa y comprobada está en [`COMANDOS-PRUEBAS.md`](COMANDOS-PRUEBAS.md): requisitos, compilación, pruebas JVM, pruebas instrumentadas, recorrido en el Xiaomi con la variante aislada, emulador de Firebase Auth, resultados esperados y solución de problemas. Resumen:

```powershell
cd "C:\Users\Usuario\AndroidStudioProjects\cafe-smart-ciclo3"
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebugAndroidTest
```

Las pruebas instrumentadas del worktree van **solo en un emulador desechable**, porque su paquete es `com.cafesmart.app`. En un teléfono real se usa la variante aislada `com.cafesmart.app.qa.ciclo3` (ver la guía).
