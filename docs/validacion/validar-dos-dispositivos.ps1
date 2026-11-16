param(
    [Parameter(Mandatory=$true)]
    [ValidateSet('Preparar','Instalar','Capturar','Instrumentadas','InstrumentadasXiaomi','LoginResponsive','LoginNavigation','ConectarAuthLocal','VerificarAuthLocal')][string]$Accion,
    [ValidateSet('emulator-5554','DEQCAQEUVS9PNRDQ')][string]$Serial,
    [ValidateSet('01-acceso-gastos-sublote','02-estado-vacio','03-lista-gastos','04-total-asignado','05-estado-error','06-navegacion')][string]$Pantalla,
    [ValidatePattern('^[0-9a-f]{40}$')]
    [string]$HeadEsperado = '98d1050a1dbc133124d833619af40aca2e4fa135',
    [string]$PrivateRoot = $env:CAFESMART_QA_ROOT
)
# PowerShell local normal. No modifica fuentes, indice Git, ajustes ni borra datos.
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'qa-file-hash.ps1')
. (Join-Path $PSScriptRoot 'qa-private.ps1')
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$sdk = Get-QASdkRoot
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$PrivateRoot = Get-QAPrivateRoot -Repo $repo -Root $PrivateRoot
$apk = Join-Path $PrivateRoot 'binarios\app-debug.apk'
$testApk = Join-Path $PrivateRoot 'binarios\app-debug-androidTest.apk'
$base = Join-Path $PrivateRoot 'gastos-sublote'
$buildManifest = Join-Path $base 'build-local-actual.json'
if(Test-Path -LiteralPath $buildManifest) {
    $registered=Get-Content -LiteralPath $buildManifest -Raw -Encoding utf8|ConvertFrom-Json
    if($registered.apkPath){$apk=$registered.apkPath}
    if($registered.testApkPath){$testApk=$registered.testApkPath}
    foreach($binary in @($apk,$testApk)) {
        if(![IO.Path]::GetFullPath($binary).StartsWith($PrivateRoot+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Binario fuera de la carpeta privada.'}
    }
}
$run = Join-Path $base ('ejecucion-' + $Accion + '-' + $Serial + '-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8))
New-Item -ItemType Directory -Path $run -Force | Out-Null
$originalJava = $env:JAVA_HOME
Push-Location $repo
function Quote-NativeArgument([string]$Value) {
    # Reglas de argumentos Win32; no interviene un shell para los .exe.
    $escaped = [regex]::Replace($Value, '(\\*)"', '$1$1\"')
    $escaped = [regex]::Replace($escaped, '(\\+)$', '$1$1')
    return '"' + $escaped + '"'
}
function Invoke-NativeProcess {
    param(
        [string]$FilePath, [string[]]$Arguments, [string]$Name,
        [string]$Directory = $repo, [string]$LogDirectory = $run,
        [switch]$AllowFailure, [switch]$Sensitive
    )
    $prefix = Join-Path $LogDirectory (($Name -replace '[^a-zA-Z0-9-]', '-') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8))
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo.FileName = $FilePath
    $process.StartInfo.Arguments = (@($Arguments | ForEach-Object { Quote-NativeArgument $_ }) -join ' ')
    if ([IO.Path]::GetExtension($FilePath) -in @('.bat','.cmd')) {
        # cmd es necesario para .bat. Estos comandos solo reciben rutas y argumentos
        # controlados; rechazar expansion/metacaracteres en vez de interpolarlos.
        foreach ($value in @($FilePath) + @($Arguments)) {
            if ($value -match '[\r\n"%!^&|<>]') { throw 'Argumento batch no admitido; no se ejecuto el comando.' }
        }
        $batchArgs = @($Arguments | ForEach-Object { '"' + $_ + '"' }) -join ' '
        $process.StartInfo.FileName = $env:ComSpec
        $process.StartInfo.Arguments = '/d /v:off /s /c ""' + $FilePath + '" ' + $batchArgs + '"'
    }
    $process.StartInfo.WorkingDirectory = $Directory
    $process.StartInfo.UseShellExecute = $false
    $process.StartInfo.CreateNoWindow = $true
    $process.StartInfo.RedirectStandardOutput = $true
    $process.StartInfo.RedirectStandardError = $true
    $process.StartInfo.StandardOutputEncoding = New-Object Text.UTF8Encoding($false)
    $process.StartInfo.StandardErrorEncoding = New-Object Text.UTF8Encoding($false)
    $loggedArguments = if ($Sensitive) { @('[CONTENIDO OMITIDO]') } else { @($Arguments) }
    [ordered]@{fecha=(Get-Date -Format o);programa=$FilePath;argumentos=$loggedArguments;directorio=$Directory} |
        ConvertTo-Json | Set-Content -LiteralPath ($prefix + '.command.json') -Encoding utf8
    try {
        if (!$process.Start()) { throw "No se pudo iniciar $FilePath" }
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()
        $process.WaitForExit()
        $stdout = $stdoutTask.GetAwaiter().GetResult()
        $stderr = $stderrTask.GetAwaiter().GetResult()
        $exitCode = $process.ExitCode
        if ($Sensitive) {
            '[CONTENIDO OMITIDO]' | Set-Content -LiteralPath ($prefix + '.stdout.txt') -Encoding utf8
            '[CONTENIDO OMITIDO]' | Set-Content -LiteralPath ($prefix + '.stderr.txt') -Encoding utf8
        } else {
            $stdout | Set-Content -LiteralPath ($prefix + '.stdout.txt') -Encoding utf8
            $stderr | Set-Content -LiteralPath ($prefix + '.stderr.txt') -Encoding utf8
        }
        $exitCode | Set-Content -LiteralPath ($prefix + '.exit-code.txt') -Encoding utf8
        if ($exitCode -ne 0 -and !$AllowFailure) {
            throw "$Name fallo (codigo $exitCode). Registros completos: $prefix.*"
        }
        return [pscustomobject]@{StdOut=$stdout;StdErr=$stderr;ExitCode=$exitCode;LogPrefix=$prefix}
    } catch {
        if ($Sensitive) {
            'Operacion sensible fallida; consultar codigo de salida.' | Set-Content -LiteralPath ($prefix + '.error.txt') -Encoding utf8
            throw "Operacion sensible fallida. Revisar codigo de salida en $prefix.*"
        }
        else { $_ | Out-String | Set-Content -LiteralPath ($prefix + '.error.txt') -Encoding utf8 }
        throw
    } finally { $process.Dispose() }
}
function Git-Checked([string[]]$Arguments) {
    $gitExe = (Get-Command git.exe -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
    $result = Invoke-NativeProcess -FilePath $gitExe -Arguments $Arguments -Name git
    return @($result.StdOut -split '\r\n|\n|\r' | Where-Object { $_ -ne '' })
}
function Git-DiffCheck([string]$Directory, [string]$LogDirectory) {
    # PS 5.1 convierte stderr nativo redirigido con 2>&1 en ErrorRecord.
    # Leer ambos canales por .NET conserva Stop y decide por ExitCode de Git.
    $gitExe = (Get-Command git.exe -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
    $result = Invoke-NativeProcess -FilePath $gitExe -Arguments @('diff','--check') -Name git-diff-check -Directory $Directory -LogDirectory $LogDirectory -AllowFailure
    $result.StdOut | Set-Content -LiteralPath (Join-Path $LogDirectory 'diff-check.txt') -Encoding utf8
    $result.StdErr | Set-Content -LiteralPath (Join-Path $LogDirectory 'diff-check-stderr.txt') -Encoding utf8
    $result.ExitCode | Set-Content -LiteralPath (Join-Path $LogDirectory 'diff-check-exit-code.txt') -Encoding utf8
    if ($result.ExitCode -ne 0) {
        throw "git diff --check fallo (codigo $($result.ExitCode)). Revisar diff-check.txt y diff-check-stderr.txt en $LogDirectory."
    }
}
function ADB-Checked([string[]]$Arguments, [switch]$Sensitive) {
    $result = Invoke-NativeProcess -FilePath $adb -Arguments $Arguments -Name adb -Sensitive:$Sensitive
    # Conservar tambien el registro agregado anterior, sin mezclar stderr con
    # los valores devueltos a pm path, getprop, devices e instrumentacion.
    $entry = if ($Sensitive) { @('ADB: contenido sensible omitido', "ExitCode: $($result.ExitCode)") }
        else { @("Argumentos: $($Arguments -join ' ')", 'STDOUT:', $result.StdOut, 'STDERR:', $result.StdErr) }
    $entry |
        Out-File (Join-Path $run 'adb.log') -Append -Encoding utf8
    return @($result.StdOut -split '\r\n|\n|\r' | Where-Object { $_ -ne '' })
}
function Fuente-Huella {
    $files = @(Get-ChildItem app/src,gradle -Recurse -File)
    $files += @(Get-Item app/build.gradle.kts,build.gradle.kts,settings.gradle.kts,gradle.properties,gradlew.bat,local.properties,app/google-services.json -ErrorAction SilentlyContinue)
    return @($files | Sort-Object FullName -Unique | ForEach-Object {
        [ordered]@{ruta=$_.FullName.Substring($repo.Length+1);sha256=(Get-QAFileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash}
    }) | ConvertTo-Json -Compress
}
function ConvertFrom-SignerCertificates([string]$OutputText) {
    $digests = @()
    foreach ($line in ($OutputText -split '\r\n|\n|\r')) {
        if ($line -notmatch '(?i)certificate\s+SHA-?256\s+digest\s*:') { continue }
        # Acepta Signer #1 y V2 Signer:/V3.0 Signer:, nunca public key digest.
        if ($line -notmatch '(?i)^\s*(?:Signer\s+#\d+|V\d+(?:\.\d+)?\s+Signer(?:\s+#\d+)?)\s*:?\s+certificate\s+SHA-?256\s+digest\s*:\s*(?<hex>[0-9a-f][0-9a-f:\s]*)\s*$') {
            throw 'Formato de certificado SHA-256 no reconocido. Revisar stdout de apksigner; no instalar.'
        }
        $digest = ($Matches.hex -replace '[:\s]', '').ToUpperInvariant()
        if ($digest -notmatch '^[0-9A-F]{64}$') { throw 'Huella de certificado SHA-256 incompleta; no instalar.' }
        $digests += $digest
    }
    if (!$digests.Count) { throw 'No se obtuvo certificado verificado. Revisar stdout/stderr de apksigner; no instalar.' }
    # Comparar el conjunto completo de certificados, sin etiquetas ni orden de esquemas.
    return (@($digests | Sort-Object -Unique) -join ';')
}
function Firma([string]$Archivo) {
    $signer = Join-Path $sdk 'build-tools\37.0.0\apksigner.bat'
    if (!(Test-Path -LiteralPath $signer)) { throw 'Falta apksigner 37.0.0; no instalar sin comprobar firma.' }
    $result = Invoke-NativeProcess -FilePath $signer -Arguments @('verify','--print-certs',$Archivo) -Name apksigner
    # Solo se extrae despues de que verify devuelva 0. Un fallo nunca autoriza instalar.
    return ConvertFrom-SignerCertificates $result.StdOut
}
function Assert-LocalApkMetadata([string]$ManifestText, [string]$ResourcesText) {
    if ($ManifestText -notmatch 'A: package="com\.cafesmart\.app"' -or
        $ManifestText -notmatch 'android:name[^\r\n]*="com\.cafesmart\.app\.qa\.LocalAuthApplication"' -or
        $ManifestText -notmatch 'android:debuggable[^\r\n]*0xffffffff' -or
        $ManifestText -match 'com\.google\.firebase\.provider\.FirebaseInitProvider') {
        throw 'Identidad/inicializacion debug del APK no reconocida; no autenticar.'
    }
    $expected = @{
        project_id='demo-cafesmart-qa'
        google_app_id='1:123456789012:android:0123456789abcdef012345'
        google_api_key='AIzaSy000000000000000000000000000000000'
    }
    foreach ($key in $expected.Keys) {
        $pattern='(?m)^\s*resource [^\r\n]+ com\.cafesmart\.app:string/'+$key+':[^\r\n]*\r?\n\s*\(string8\) "([^"\r\n]*)"'
        $values=@([regex]::Matches($ResourcesText,$pattern))
        if (!$values.Count -or @($values | Where-Object { $_.Groups[1].Value -cne $expected[$key] }).Count) {
            throw "Recurso Firebase local no verificable: $key. No autenticar."
        }
    }
}
function Rutas-ApkPruebas {
    $paquetePruebas = 'com.cafesmart.app.test'
    # pm path puede devolver 1 sin texto si el paquete no existe. Consultar
    # primero la lista con codigo 0; no aceptar cualquier fallo como ausencia.
    $packages = @(ADB-Checked @('-s',$Serial,'shell','pm','list','packages',$paquetePruebas))
    foreach ($line in $packages) {
        if ($line -notmatch '^package:[A-Za-z0-9_.]+$') {
            throw 'Respuesta inesperada al consultar el paquete de pruebas; no instalar.'
        }
    }
    if (@($packages | Where-Object { $_ -ceq ('package:' + $paquetePruebas) }).Count -eq 0) {
        Write-Host 'Paquete de pruebas ausente segun pm list packages (codigo 0); primera instalacion permitida con firma local verificada.'
        return @()
    }
    # Si aparece instalado, path/pull/verificacion deben completarse. No
    # convertir errores ni una respuesta contradictoria en primera instalacion.
    $paths = @(ADB-Checked @('-s',$Serial,'shell','pm','path',$paquetePruebas))
    if (!$paths.Count -or @($paths | Where-Object { $_ -notmatch '^package:/.+\.apk$' }).Count) {
        throw 'El paquete de pruebas figura instalado pero sus rutas APK no son verificables; no instalar.'
    }
    return $paths
}
function Instalar-Seguro([string]$Paquete,[string]$Archivo) {
    $localFirma = Firma $Archivo
    $paths = if ($Paquete -ceq 'com.cafesmart.app.test') {
        @(Rutas-ApkPruebas)
    } else {
        @(ADB-Checked @('-s',$Serial,'shell','pm','path',$Paquete))
    }
    $remote = @($paths | Where-Object { "$_" -match '^package:' })
    if ($remote.Count) {
        $installed = Join-Path $run ($Paquete + '-instalado.apk')
        ADB-Checked @('-s',$Serial,'pull',("$($remote[0])" -replace '^package:','').Trim(),$installed) | Out-Host
        if ((Firma $installed) -ne $localFirma) { throw "Firma incompatible: $Paquete. No se instala ni desinstala." }
    }
    # -r conserva datos; sin -d, sin desinstalar, sin pm clear. Android comprueba compatibilidad.
    $result = ADB-Checked @('-s',$Serial,'install','-r',$Archivo)
    if (("$result") -notmatch 'Success') { throw "Instalacion sin confirmacion: $result" }
}
$mainFailed = $false
try {
    if ((Git-Checked @('branch','--show-current')) -ne 'feat/gastos-ciclo2') { throw 'Rama inesperada.' }
    if ((Git-Checked @('rev-parse','HEAD')) -ne $HeadEsperado) { throw 'HEAD cambio; revisar antes de continuar.' }
    Git-Checked @('status','--short','--branch') | Set-Content (Join-Path $run 'git-antes.txt')
    $stashAntes = @(Git-Checked @('stash','list','--format=%H %gs'))
    $stashAntes | Set-Content (Join-Path $run 'stashes-antes.txt')
    # Reutilizar JAVA_HOME/PATH existentes, sin reconfigurar Java.
    if ($Accion -eq 'Preparar') {
        $before = Fuente-Huella
        # Solo empaquetado incremental; no repite JVM ni instrumentadas del Pixel.
        # El criterio existente del daemon exige Java 25; no se modifica.
        $build = Invoke-NativeProcess -FilePath (Join-Path $repo 'gradlew.bat') -Arguments @(':app:assembleDebug',':app:assembleDebugAndroidTest','--console=plain','--no-watch-fs') -Name gradle -AllowFailure
        $build.StdOut | Set-Content (Join-Path $run 'build.log') -Encoding utf8
        $build.StdErr | Set-Content (Join-Path $run 'build-stderr.txt') -Encoding utf8
        $build.StdOut | Out-Host
        $build.StdErr | Out-Host
        if ($build.ExitCode -ne 0) { throw "Build fallido (codigo $($build.ExitCode)); conservar logs. No instalar." }
        if ($before -ne (Fuente-Huella)) { throw 'Fuentes cambiaron durante build.' }
        $export = & (Join-Path $PSScriptRoot 'archivar-evidencias.ps1') -PrivateRoot $PrivateRoot -Retirar -SinRespaldo
        $exports = Get-Content (Join-Path $export.destino 'manifest-traslado.json') -Raw -Encoding utf8|ConvertFrom-Json
        $apk = @($exports|Where-Object{$_.path.Replace('\','/') -ceq 'app/build/outputs/apk/debug/app-debug.apk'})[0].destination
        $testApk = @($exports|Where-Object{$_.path.Replace('\','/') -ceq 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'})[0].destination
        if(!$apk -or !$testApk){throw 'APK no encontrado en la exportacion verificada.'}
        if (Test-Path $buildManifest) { Copy-Item $buildManifest (Join-Path $run 'build-manifest-anterior.json') }
        [ordered]@{fecha=(Get-Date -Format o);head=(Git-Checked @('rev-parse','HEAD'));fuentes=$before;apkPath=$apk;testApkPath=$testApk;apk=(Get-QAFileHash $apk).Hash;testApk=(Get-QAFileHash $testApk).Hash} | ConvertTo-Json -Depth 5 | Set-Content $buildManifest -Encoding utf8
        Write-Host 'APK actuales registrados. No se instalaron ni se ejecutaron pruebas.'
        return
    }
    if (!$Serial) { throw 'Indicar -Serial explicitamente.' }
    if ($Accion -eq 'InstrumentadasXiaomi' -and $Serial -ne 'DEQCAQEUVS9PNRDQ') { throw 'Solo Xiaomi autorizado para esta ejecucion.' }
    $devices = @(ADB-Checked @('devices','-l'))
    $devices | Out-Host
    $entry = @($devices | Where-Object { "$_" -match ('^' + [regex]::Escape($Serial) + '\s+device(?:\s|$)') })
    if ($entry.Count -ne 1) { throw 'Serial no disponible como device. Si unauthorized, autorizar USB en el celular; si offline, revisar conexion sin borrar datos.' }
    foreach ($prop in @('ro.product.model','ro.build.version.release','ro.build.version.sdk','sys.boot_completed')) {
        "$prop=$(ADB-Checked @('-s',$Serial,'shell','getprop',$prop))" | Add-Content (Join-Path $run 'dispositivo.txt')
    }
    if (("$(ADB-Checked @('-s',$Serial,'shell','getprop','sys.boot_completed'))").Trim() -ne '1') { throw 'Arranque incompleto.' }
    if ([int]("$(ADB-Checked @('-s',$Serial,'shell','getprop','ro.build.version.sdk'))").Trim() -lt 24) { throw 'Android inferior al minSdk 24.' }
    if ($Accion -in @('ConectarAuthLocal','VerificarAuthLocal')) {
        $localConfig = Get-Content -Raw (Join-Path $repo 'app\src\debug\google-services.json') | ConvertFrom-Json
        if ($localConfig.project_info.project_id -cne 'demo-cafesmart-qa') { throw 'Proyecto debug inesperado; no conectar.' }
        # Solo loopback. No se cambia firewall, red del celular ni configuracion Firebase real.
        Invoke-RestMethod -Uri 'http://127.0.0.1:9099/emulator/v1/projects/demo-cafesmart-qa/config' -Method Get -TimeoutSec 10 | Out-Null
        $reverse = @(ADB-Checked @('-s',$Serial,'reverse','--list'))
        if (@($reverse | Where-Object { $_ -match '\s+tcp:9099\s+' -and $_ -notmatch '\s+tcp:9099\s+tcp:9099(?:\s|$)' }).Count) {
            throw 'Ya existe un puente 9099 con otro destino; no se reemplaza.'
        }
        if ($Accion -eq 'ConectarAuthLocal') {
            if (!@($reverse | Where-Object { $_ -match '\s+tcp:9099\s+tcp:9099(?:\s|$)' }).Count) {
                ADB-Checked @('-s',$Serial,'reverse','tcp:9099','tcp:9099') | Out-Host
            }
            $reverse = @(ADB-Checked @('-s',$Serial,'reverse','--list'))
        }
        $reverse | Set-Content (Join-Path $run 'reverse-auth-local.txt') -Encoding utf8
        if (!@($reverse | Where-Object { $_ -match '\s+tcp:9099\s+tcp:9099(?:\s|$)' }).Count) { throw 'Falta el puente local 9099.' }
        if ($Accion -eq 'VerificarAuthLocal') {
            if (!(Test-Path $buildManifest)) { throw 'Falta el manifiesto de Preparar.' }
            $localBuild = Get-Content -Raw $buildManifest | ConvertFrom-Json
            if ($localBuild.fuentes -ne (Fuente-Huella) -or $localBuild.apk -ne (Get-QAFileHash $apk).Hash) {
                throw 'Build o fuentes distintos de Preparar; no se verifica otro APK.'
            }
            $paths = @(ADB-Checked @('-s',$Serial,'shell','pm','path','com.cafesmart.app'))
            if ($paths.Count -ne 1 -or $paths[0] -notmatch '^package:/.+\.apk$') { throw 'APK instalado no verificable como monolitico.' }
            $installedLocal = Join-Path $run 'qa-app-instalada.apk'
            ADB-Checked @('-s',$Serial,'pull',($paths[0] -replace '^package:','').Trim(),$installedLocal) | Out-Host
            if ((Get-QAFileHash $installedLocal).Hash -ne $localBuild.apk) { throw 'El APK instalado no coincide con el build local.' }
            if ((Firma $installedLocal) -cne (Firma $apk)) { throw 'Firma instalada incompatible con el APK local.' }
            $aapt=Join-Path $sdk 'build-tools\37.0.0\aapt.exe'
            $apkManifest=Invoke-NativeProcess -FilePath $aapt -Arguments @('dump','xmltree',$installedLocal,'AndroidManifest.xml') -Name apk-identidad
            $apkResources=Invoke-NativeProcess -FilePath $aapt -Arguments @('dump','--values','resources',$installedLocal) -Name apk-recursos -Sensitive
            Assert-LocalApkMetadata $apkManifest.StdOut $apkResources.StdOut
            # La huella de fuentes coincide con Preparar. Comprobar tambien la
            # implementacion auditada que valida opciones ANTES de initializeApp
            # y llama useEmulator(127.0.0.1,9099), sin fallback remoto.
            $authSource=Join-Path $repo 'app\src\debug\java\com\cafesmart\app\qa\LocalAuthApplication.kt'
            if ((Get-QAFileHash $authSource).Hash -cne '325910B44D3FE3242547B0266A1EE11350D8A922CFC782140125C6AC5772CA9C') {
                throw 'Inicializador debug distinto del auditado; revisar aislamiento antes del login.'
            }
            $pidLocal = ("$(ADB-Checked @('-s',$Serial,'shell','pidof','com.cafesmart.app'))").Trim()
            if ($pidLocal -notmatch '^\d+$') { throw 'La app no tiene un proceso unico verificable.' }
            $marker = @(ADB-Checked @('-s',$Serial,'logcat','-d','--pid',$pidLocal,'-s','CafeSmartQA:I','*:S'))
            $marker | Set-Content (Join-Path $run 'qa-auth-local-logcat.txt') -Encoding utf8
            $hasMarker=[bool]($marker -match 'Auth local: project=demo-cafesmart-qa host=127\.0\.0\.1 port=9099')
            if (@($marker | Where-Object { $_ -match 'Auth local:' -and $_ -notmatch 'Auth local: project=demo-cafesmart-qa host=127\.0\.0\.1 port=9099' }).Count) {
                throw 'Logcat contradice el aislamiento local; no autenticar.'
            }
            [ordered]@{serial=$Serial;proyecto='demo-cafesmart-qa';apkHashVerificado=$true;firmaVerificada=$true;recursosDebugVerificados=$true;inicializadorAuditado=$true;puente9099=$true;servidorLocal=$true;pid=$pidLocal;marcadorDisponible=$hasMarker} |
                ConvertTo-Json | Set-Content (Join-Path $run 'aislamiento-local.json') -Encoding utf8
            Write-Host "APK, firma, recursos demo, proceso, reverse y servidor local verificados. Marcador historico disponible: $hasMarker. Login todavia no aprobado."
        } else { Write-Host "Puente Auth local configurado para $Serial." }
        return
    }
    if (!(Test-Path $buildManifest)) { throw 'Ejecutar primero -Accion Preparar.' }
    $manifest = Get-Content -Raw $buildManifest | ConvertFrom-Json
    if ($manifest.fuentes -ne (Fuente-Huella) -or $manifest.apk -ne (Get-QAFileHash $apk).Hash -or $manifest.testApk -ne (Get-QAFileHash $testApk).Hash) { throw 'Build o fuentes cambiaron; ejecutar Preparar.' }
    Copy-Item $buildManifest (Join-Path $run 'build-manifest.json')
    if ($Accion -eq 'Instalar') {
        Instalar-Seguro 'com.cafesmart.app' $apk
        ADB-Checked @('-s',$Serial,'shell','am','start','-W','-n','com.cafesmart.app/.MainActivity') | Out-Host
        Write-Host 'Inicio solicitado. Revisar respuesta y ausencia de cierres en pantalla; esto no aprueba validacion visual.'
    } elseif ($Accion -in @('Instrumentadas','InstrumentadasXiaomi','LoginResponsive','LoginNavigation')) {
        # Verificar binario instalado, sin reinstalar ni alterar sesion antes de comprobarlo.
        $paths = @(ADB-Checked @('-s',$Serial,'shell','pm','path','com.cafesmart.app'))
        $remote = @($paths | Where-Object { "$_" -match '^package:' })
        if ($remote.Count -ne 1) { throw 'Se espera un APK monolitico; instalar primero con este script.' }
        $installed = Join-Path $run 'app-instalada.apk'
        ADB-Checked @('-s',$Serial,'pull',("$($remote[0])" -replace '^package:','').Trim(),$installed) | Out-Host
        if ((Get-QAFileHash $installed).Hash -ne $manifest.apk) { throw 'APK instalado distinto del build registrado.' }
        Instalar-Seguro 'com.cafesmart.app.test' $testApk
        # La navegacion y Room cambiaron en main: revalidar solo estas clases.
        # AppDatabaseTest usa una base en memoria, sin tocar cafesmart.db.
        $classes = 'com.cafesmart.app.ui.gastos.GastosSubloteNavigationTest'
        $expectedTests = 4
        if ($Accion -eq 'Instrumentadas') {
            $classes += ',com.cafesmart.app.ui.gastos.GastosNavigationScopeTest,com.cafesmart.app.data.local.AppDatabaseTest'
            $expectedTests = 11
        }
        if ($Accion -eq 'LoginResponsive') {
            $classes = 'com.cafesmart.app.ui.auth.LoginResponsiveTest,com.cafesmart.app.ui.gastos.GastosNavigationScopeTest'
            $expectedTests = 5
        }
        if ($Accion -eq 'LoginNavigation') {
            $classes = 'com.cafesmart.app.ui.gastos.GastosNavigationScopeTest'
            $expectedTests = 1
        }
        $result = @(ADB-Checked @('-s',$Serial,'shell','am','instrument','-w','-r','-e','class',$classes,'com.cafesmart.app.test/androidx.test.runner.AndroidJUnitRunner'))
        $result | Set-Content (Join-Path $run ('instrumentadas-'+$Serial+'.txt')) -Encoding utf8
        $result | Out-Host
        if (($result -join "`n") -notmatch ('OK \('+$expectedTests+' tests?\)') -or ($result -join "`n") -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed') { throw 'No hay confirmacion de todas las pruebas; analizar salida original.' }
        Write-Host "APROBADO: $expectedTests/$expectedTests instrumentadas en $Serial. No equivale a validacion visual."
    } else {
        if (!$Pantalla) { throw 'Indicar -Pantalla.' }
        $paths = @(ADB-Checked @('-s',$Serial,'shell','pm','path','com.cafesmart.app'))
        $remotePaths = @($paths | Where-Object { "$_" -match '^package:' })
        if ($remotePaths.Count -ne 1) { throw 'APK instalado no verificable como APK monolitico.' }
        $installed = Join-Path $run 'app-instalada.apk'
        ADB-Checked @('-s',$Serial,'pull',("$($remotePaths[0])" -replace '^package:','').Trim(),$installed) | Out-Host
        if ((Get-QAFileHash $installed).Hash -ne $manifest.apk) { throw 'APK instalado distinto del build registrado; no capturar como version actual.' }
        $folder = if ($Serial -eq 'emulator-5554') { 'pixel-9-pro' } else { 'xiaomi-android-16' }
        $destDir = Join-Path $base $folder
        New-Item -ItemType Directory -Path $destDir -Force | Out-Null
        $dest = Join-Path $destDir ($Pantalla + '.png')
        if (Test-Path $dest) { throw 'Captura existente: revisar; no se sobrescribe.' }
        Write-Host 'Mostrar la pantalla real y revisar ID, datos, foco y barras. No capturar otra pantalla para completar la lista.'
        if ((Read-Host 'Escribir CAPTURAR si la pantalla requerida esta visible; otra respuesta cancela') -cne 'CAPTURAR') { throw 'Captura cancelada; caso pendiente.' }
        $observacion = Read-Host 'Describir ID, datos conocidos, resultado e interacciones realmente verificadas'
        if ([string]::IsNullOrWhiteSpace($observacion)) { throw 'Falta observacion.' }
        $remote = '/sdcard/cafesmart-' + [guid]::NewGuid().ToString('N') + '.png'
        ADB-Checked @('-s',$Serial,'shell','screencap','-p',$remote) | Out-Host
        ADB-Checked @('-s',$Serial,'pull',$remote,$dest) | Out-Host
        Add-Type -AssemblyName System.Drawing
        $img = [Drawing.Image]::FromFile($dest)
        try { if ($img.Width -le 0 -or $img.Height -le 0) { throw 'Imagen invalida.' } } finally { $img.Dispose() }
        [ordered]@{fecha=(Get-Date -Format o);serial=$Serial;imagen=$dest;sha256=(Get-QAFileHash $dest).Hash;observacion=$observacion;estado='CAPTURADO; pendiente de revision visual'} | ConvertTo-Json | Set-Content (Join-Path $run 'captura.json') -Encoding utf8
        Start-Process -FilePath $dest
    }
} catch {
    $mainFailed = $true
    $_ | Out-String | Set-Content (Join-Path $run 'error.txt') -Encoding utf8
    throw
} finally {
    $cleanupErrors = @()
    try {
        if ($Serial -and (Test-Path $adb)) {
            foreach ($diagnostic in @('crash','ventanas')) {
                try {
                    $diagnosticArgs = if ($diagnostic -eq 'crash') { @('-s',$Serial,'logcat','-d','-b','crash','-v','threadtime') } else { @('-s',$Serial,'shell','dumpsys','window') }
                    $result = Invoke-NativeProcess -FilePath $adb -Arguments $diagnosticArgs -Name $diagnostic
                    $result.StdOut | Set-Content (Join-Path $run ($diagnostic + '.txt')) -Encoding utf8
                } catch { $cleanupErrors += ($_ | Out-String) }
            }
        }
        try {
            Git-Checked @('status','--short','--branch') | Set-Content (Join-Path $run 'git-despues.txt')
            Git-Checked @('rev-parse','HEAD') | Set-Content (Join-Path $run 'head.txt')
            Git-Checked @('stash','list','--format=%H %gs') | Set-Content (Join-Path $run 'stashes-despues.txt')
            Git-DiffCheck -Directory $repo -LogDirectory $run
        } catch { $cleanupErrors += ($_ | Out-String) }
        if ($cleanupErrors.Count) {
            $cleanupErrors | Set-Content (Join-Path $run 'errores-diagnostico.txt') -Encoding utf8
            Write-Host 'Fallaron comprobaciones finales; revisar errores-diagnostico.txt.'
        }
    } finally {
        # Restaurar el entorno tambien si Git devuelve un error autentico.
        Pop-Location
        $env:JAVA_HOME = $originalJava
        Write-Host "Resultados conservados: $run"
    }
    # Conservar la excepcion principal si ya fallo la accion. Si solo falla el
    # diagnostico, tambien informar fallo; nunca convertirlo en aprobacion.
    if ($cleanupErrors.Count -and !$mainFailed) { throw 'Fallaron comprobaciones finales; revisar errores-diagnostico.txt.' }
}
