# Comandos para ejecutar y verificar las pruebas (ciclo 3: Límites y Gráficas)

Guía para compilar, ejecutar las pruebas y repetir la validación de Límites de entrada y Gráficas financieras sin poner en riesgo otras instalaciones de Café Smart.

> **Advertencia importante:** el `applicationId` del worktree es `com.cafesmart.app`, el mismo de la app normal. Instalar `app-debug.apk` o ejecutar `connectedDebugAndroidTest` en un teléfono que tenga Café Smart **reemplaza esa instalación** (y una reinstalación borra su base Room). Por eso:
> - Las pruebas instrumentadas del worktree se ejecutan **solo en un emulador desechable**.
> - En un teléfono real se usa **solo la variante QA aislada** `com.cafesmart.app.qa.ciclo3` (sección D).
>
> **Instalar, actualizar y desinstalar: qué pasa con los datos**
> - `adb install -r` **actualiza** una app ya instalada y normalmente **conserva** sus datos (base Room, preferencias, sesión). Solo funciona si la firma del APK es la misma que la instalada.
> - `adb uninstall <paquete>` (o desinstalar desde el teléfono) normalmente **borra** los datos privados de esa app: su base Room y sus preferencias. No se recuperan.
> - Una app con **otro `applicationId`** (por ejemplo `com.cafesmart.app.qa.ciclo3`) es otra app para Android: tiene almacenamiento propio y separado, y no lee ni modifica los datos de `com.cafesmart.app` ni de otras variantes.
> - **Nunca instales un APK sin verificar antes su paquete** con `aapt dump badging <apk>` (ver la sección D, paso 3) ni sin usar `-s <serial>`.

## A. Requisitos previos

| Requisito | Valor usado en esta validación |
|---|---|
| JDK | 17, como indica el README del proyecto. Gradle usa el JDK que trae Android Studio. |
| Android SDK | `C:\Users\Usuario\AppData\Local\Android\Sdk` (`sdk.dir` en `local.properties`). `compileSdk` 37 y `minSdk` 24. |
| Directorio | `C:\Users\Usuario\AndroidStudioProjects\cafe-smart-ciclo3` |
| Rama | `feat/integracion-ciclo3` (PR #48) |
| Firebase | `app/google-services.json` propio (no está en el repo; ver README). Solo hace falta para compilar. |
| Emulador | `QA_Desechable_20261123` (`emulator-5554`), solo para pruebas instrumentadas del worktree. |
| Teléfono real | Xiaomi Redmi Note 14 Pro 5G, serial `DEQCAQEUVS9PNRDQ`, solo con la variante QA aislada. |
| Auth local | Firebase Authentication Emulator en `127.0.0.1:9099` con el proyecto de demostración `demo-cafesmart-qa`. Solo hace falta para los recorridos con inicio de sesión de la variante QA (sección E). |

Tipos de prueba:
- **JVM** (`testDebugUnitTest`): lógica pura (límites, validación, persistencia en memoria, serie mensual). Sin dispositivo.
- **Instrumentadas Compose** (`androidTest`): dibujan una pantalla con un estado armado a mano y la recorren en un dispositivo. No usan Room de la cuenta ni Firebase.
- **Validación manual en Xiaomi**: recorridos reales por la app QA aislada (inicio de sesión, gasto, límites en formularios, regresión visual), con capturas.

## B. Compilación y pruebas JVM

```powershell
cd "C:\Users\Usuario\AndroidStudioProjects\cafe-smart-ciclo3"

# APK de depuración. Comprueba que el código compila.
# Resultado: app\build\outputs\apk\debug\app-debug.apk (paquete com.cafesmart.app)
.\gradlew.bat assembleDebug

# Todas las pruebas JVM.
# Resultados: app\build\test-results\testDebugUnitTest\*.xml
# Informe legible: app\build\reports\tests\testDebugUnitTest\index.html
.\gradlew.bat testDebugUnitTest

# APK de pruebas instrumentadas. Comprueba que androidTest compila.
# Resultado: app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk (paquete com.cafesmart.app.test)
.\gradlew.bat assembleDebugAndroidTest

# Solo una clase JVM (por ejemplo, las de Límites):
.\gradlew.bat :app:testDebugUnitTest --tests "*ConfiguracionLimitesTest"
.\gradlew.bat :app:testDebugUnitTest --tests "*FormatoLimitesTest"
.\gradlew.bat :app:testDebugUnitTest --tests "*GraficasFinancierasViewModelTest"
```

## C. Pruebas instrumentadas (emulador desechable)

```powershell
$adb = "C:\Users\Usuario\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb devices -l
# Debe aparecer el emulador, por ejemplo:  emulator-5554  device  product:sdk_gphone64_x86_64 ...

$emu = "emulator-5554"   # usar SIEMPRE -s con el serial exacto

& $adb -s $emu install -r "app\build\outputs\apk\debug\app-debug.apk"
& $adb -s $emu install -r -t "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"

# Límites de entrada (3 pruebas)
& $adb -s $emu shell am instrument -w -r -e class com.cafesmart.app.ui.settings.LimitesEntradaUiTest com.cafesmart.app.test/androidx.test.runner.AndroidJUnitRunner

# Gráficas financieras (3 pruebas)
& $adb -s $emu shell am instrument -w -r -e class com.cafesmart.app.ui.finance.GraficasFinancierasUiTest com.cafesmart.app.test/androidx.test.runner.AndroidJUnitRunner

# Las dos clases en una sola ejecución
& $adb -s $emu shell am instrument -w -r -e class com.cafesmart.app.ui.settings.LimitesEntradaUiTest,com.cafesmart.app.ui.finance.GraficasFinancierasUiTest com.cafesmart.app.test/androidx.test.runner.AndroidJUnitRunner
```

El resultado termina en `OK (N tests)` o en `FAILURES!!!` con el detalle de cada prueba. Comprobado en el APK: el runner es `androidx.test.runner.AndroidJUnitRunner`.

## D. Pruebas en el Xiaomi físico (variante QA aislada)

La variante `com.cafesmart.app.qa.ciclo3` tiene **otro paquete** (no reemplaza ninguna instalación de Café Smart), su propia base Room y solo habla con el emulador local de Auth.

> **No es reproducible desde el repositorio todavía.** Se compiló en una copia temporal (`C:\Users\Usuario\AppData\Local\Temp\cafe-ciclo3-qa-fgda08he`), que agrega solo para QA:
> - `applicationId = "com.cafesmart.app.qa.ciclo3"` en `app/build.gradle.kts`;
> - `app/src/debug/AndroidManifest.xml` y `app/src/debug/java/com/cafesmart/app/qa/LocalAuthApplication.kt`, que inician Firebase solo con el proyecto `demo-cafesmart-qa` y lo conectan al emulador `127.0.0.1:9099`, sin alternativa remota y sin recolección de datos;
> - `app/src/debug/res/xml/qa_auth_network_security.xml`, que permite HTTP solo hacia `127.0.0.1` y `localhost`;
> - `app/google-services.json` sintético del proyecto de demostración;
> - `firebase.json` del emulador.
>
> Esa carpeta temporal puede borrarse en cualquier momento. Para el equipo habría que convertir esta configuración en una variante QA documentada del proyecto (pendiente).

```powershell
$adb = "C:\Users\Usuario\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$xiaomi = "DEQCAQEUVS9PNRDQ"
$qa = "C:\Users\Usuario\AppData\Local\Temp\cafe-ciclo3-qa-fgda08he"   # copia temporal (ver aviso)

# 1. Conexión USB: debe decir "device" (no "unauthorized" ni "offline")
& $adb devices -l

# 2. Pantalla encendida y desbloqueada (no se desbloquea por ADB)
& $adb -s $xiaomi shell dumpsys power | Select-String "mWakefulness="          # Awake
& $adb -s $xiaomi shell dumpsys window | Select-String "mDreamingLockscreen"   # mDreamingLockscreen=false

# 3. Compilar e instalar SOLO la variante aislada. Antes, comprobar el paquete del APK.
cd $qa
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
$aapt = (Get-ChildItem "C:\Users\Usuario\AppData\Local\Android\Sdk\build-tools" | Sort-Object Name | Select-Object -Last 1).FullName + "\aapt.exe"
& $aapt dump badging "app\build\outputs\apk\debug\app-debug.apk" | Select-String "^package"   # name='com.cafesmart.app.qa.ciclo3'
& $adb -s $xiaomi install -r --user 0 "app\build\outputs\apk\debug\app-debug.apk"
& $adb -s $xiaomi install -r -t --user 0 "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
# MIUI puede pedir "Instalar vía USB" en el teléfono: aceptarlo a mano.

# 4. Puerto del emulador de Auth hacia el teléfono
& $adb -s $xiaomi reverse tcp:9099 tcp:9099
& $adb -s $xiaomi reverse --list          # UsbFfs tcp:9099 tcp:9099

# 5. Abrir la app aislada (debe quedar al frente antes de las pruebas Compose)
& $adb -s $xiaomi shell am start -n com.cafesmart.app.qa.ciclo3/com.cafesmart.app.MainActivity

# 6. Pruebas instrumentadas en la variante aislada
& $adb -s $xiaomi shell am instrument -w -r -e class com.cafesmart.app.ui.settings.LimitesEntradaUiTest com.cafesmart.app.qa.ciclo3.test/androidx.test.runner.AndroidJUnitRunner
& $adb -s $xiaomi shell am instrument -w -r -e class com.cafesmart.app.ui.finance.GraficasFinancierasUiTest com.cafesmart.app.qa.ciclo3.test/androidx.test.runner.AndroidJUnitRunner

# 7. Captura de pantalla: se guarda en el teléfono y se copia a la PC
#    (no redirigir la salida binaria con ">" en PowerShell)
& $adb -s $xiaomi shell screencap -p /sdcard/captura_cs.png
& $adb -s $xiaomi pull /sdcard/captura_cs.png ".\captura_cs.png"
& $adb -s $xiaomi shell rm /sdcard/captura_cs.png

# 8. Persistencia: cambiar los límites en Ajustes, cerrar la app por completo y volver a abrirla
& $adb -s $xiaomi shell run-as com.cafesmart.app.qa.ciclo3 cat shared_prefs/limites_de_entrada.xml   # valores guardados
& $adb -s $xiaomi shell am force-stop com.cafesmart.app.qa.ciclo3
& $adb -s $xiaomi shell pidof com.cafesmart.app.qa.ciclo3     # sin salida = proceso cerrado
& $adb -s $xiaomi shell am start -n com.cafesmart.app.qa.ciclo3/com.cafesmart.app.MainActivity
# Comprobar en Ajustes > Límites de entrada, Compras y Gastos que se aplican los valores guardados.
# Tras "Volver a los valores originales", el archivo de preferencias queda vacío (<map />).
```

`run-as` funciona solo porque la variante QA es de depuración; únicamente lee los archivos de esa app aislada.

## E. Firebase Authentication Emulator

En esta validación se usó `firebase-tools` 15.35.0, desde una instalación local fuera del repositorio (`node_modules` en otra carpeta del equipo). El comando equivalente con la misma versión, publicada en npm, es este. Se documenta tal cual, pero **no se ejecutó con `npx` en esta validación**:

```powershell
# En una carpeta con un firebase.json como este (el de la copia QA):
# {"emulators": {"auth": {"host": "127.0.0.1", "port": 9099}, "ui": {"enabled": false}, "singleProjectMode": true}}
npx --yes firebase-tools@15.35.0 emulators:start --only auth --project demo-cafesmart-qa
```

Un proyecto `demo-*` hace que el emulador no use ningún servicio real de Firebase.

Verificaciones (en otra ventana de PowerShell). **Estado:** los tres comandos PowerShell de abajo **no se ejecutaron tal cual**. Los mismos endpoints sí se comprobaron con `curl` en esta validación: `config` respondió HTTP 200, el alta `accounts:signUp` creó una cuenta ficticia (luego borrada) y `accounts:query` listó las cuentas.

```powershell
# El emulador responde (HTTP 200)
(Invoke-WebRequest "http://127.0.0.1:9099/emulator/v1/projects/demo-cafesmart-qa/config" -UseBasicParsing).StatusCode

# Crear una cuenta FICTICIA de prueba (dominio reservado .invalid; elegir una contraseña de prueba,
# no reutilizar contraseñas reales y no guardarla en el repo)
$cuerpo = '{"email":"<cuenta-de-prueba>@example.invalid","password":"<contraseña-de-prueba>","returnSecureToken":false}'
Invoke-RestMethod -Method Post -ContentType "application/json" -Body $cuerpo "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=demo"

# Ver qué cuentas tiene el emulador
Invoke-RestMethod -Method Post -ContentType "application/json" -Headers @{Authorization="Bearer owner"} -Body '{}' "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/projects/demo-cafesmart-qa/accounts:query"
```

El teléfono llega al emulador por `adb reverse tcp:9099 tcp:9099` (sección D, paso 4).

## F. Resultados confirmados (ejecuciones registradas)

Estos resultados son de ejecuciones ya registradas en `resultado.md`. No se obtienen todos con un solo comando.

| Prueba | Resultado |
|---|---|
| `testDebugUnitTest` (rama completa) | **1036/1036** aprobadas, 0 fallas |
| `assembleDebug`, `assembleDebugAndroidTest` | compilan |
| `LimitesEntradaUiTest` en el Xiaomi (variante aislada) | **3/3** |
| Validación física anterior en el Xiaomi (gráfica con gasto, límites en Compras y Gastos, restablecer y reabrir, regresión visual) | **15/15** casos aprobados |

## G. Solución de problemas frecuentes

| Problema | Comprobación segura | Posible solución |
|---|---|---|
| `adb` no se reconoce | `Test-Path "C:\Users\Usuario\AppData\Local\Android\Sdk\platform-tools\adb.exe"` | Usar la ruta completa (variable `$adb`) o agregar `platform-tools` al PATH. |
| `device offline` o `unauthorized` | `& $adb devices -l` | Desconectar y conectar el cable, desbloquear y aceptar «¿Permitir depuración USB?». Si sigue: `& $adb kill-server; & $adb start-server`. |
| `No compose hierarchies found` | `& $adb -s $xiaomi shell dumpsys window \| Select-String mCurrentFocus` | En MIUI la actividad de prueba no se abre si otra app está al frente. Abrir la app aislada con `am start` (sección D, paso 5) y repetir **solo** la clase afectada. |
| `Process crashed` en la instrumentación | `& $adb -s $xiaomi logcat -d -s AndroidRuntime:E` | Revisar la traza. Comprobar que la app y el APK `.test` son de la **misma** compilación (reinstalar los dos) y que el emulador de Auth responde si la prueba inicia sesión. |
| Pantalla bloqueada del Xiaomi | `dumpsys window \| Select-String mDreamingLockscreen` (debe decir `false`) | Desbloquear a mano. No se desbloquea ni se cambian ajustes de seguridad por ADB. |
| Firebase Emulator no responde en 9099 | `Invoke-WebRequest http://127.0.0.1:9099/` y `Get-NetTCPConnection -LocalPort 9099 -State Listen` | Iniciar el emulador (sección E). Revisar que no haya otro programa usando el puerto y repetir `adb reverse tcp:9099 tcp:9099`. |
| MIUI «Instalar vía USB» | La instalación termina en `INSTALL_FAILED_USER_RESTRICTED` | Repetir `adb install` mirando el teléfono y tocar **Instalar** (esperar el contador si aparece). Debe estar activo «Instalar vía USB» en Opciones de desarrollador. |
