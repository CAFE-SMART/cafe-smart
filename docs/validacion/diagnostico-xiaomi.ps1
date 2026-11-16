param(
    [string]$PrivateRoot=$env:CAFESMART_QA_ROOT,
    [switch]$SoloLectura=$true,
    [switch]$FormularioGastos,
    [ValidateSet('estado','login-vacio','inicio','inventario','verde','detalle-1','gastos-vacio-total','volver','atras-sistema','login-teclado','fuente-ampliada','registro-antes','registro-despues','registro-teclado','seleccion-sublotes')]
    [string]$PasoManual='estado'
)
$ErrorActionPreference='Stop'
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
. (Join-Path $PSScriptRoot 'qa-private.ps1')
$PrivateRoot=Get-QAPrivateRoot -Repo $repo -Root $PrivateRoot
$adb=Join-Path (Get-QASdkRoot) 'platform-tools\adb.exe'
$Serial='DEQCAQEUVS9PNRDQ'
$run=Join-Path $PrivateRoot ('xiaomi\diagnostico-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[guid]::NewGuid().ToString('N').Substring(0,8))
New-Item -ItemType Directory -Path $run -Force|Out-Null
$tokens=$null;$errors=$null
$ast=[Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot 'validar-dos-dispositivos.ps1'),[ref]$tokens,[ref]$errors)
if($errors.Count){throw 'Error de sintaxis en funciones ADB.'}
foreach($name in @('Quote-NativeArgument','Invoke-NativeProcess')) {
    $fn=@($ast.FindAll({param($n)$n -is [Management.Automation.Language.FunctionDefinitionAst] -and $n.Name -eq $name},$true))
    if($fn.Count -ne 1){throw 'Funcion nativa no unica.'}
    . ([scriptblock]::Create($fn[0].Extent.Text))
}
$devices=Invoke-NativeProcess -FilePath $adb -Arguments @('devices','-l') -Name devices
if($devices.StdOut -notmatch ('(?m)^'+[regex]::Escape($Serial)+'\s+device\b')){throw 'Xiaomi no esta conectado como device.'}
# KEYCODE_UNKNOWN prueba la autorizacion sin pulsar un control o cerrar una pantalla.
$probe=$null
if(!$SoloLectura) {
    $probe=Invoke-NativeProcess -FilePath $adb -Arguments @('-s',$Serial,'shell','input','keyevent','KEYCODE_UNKNOWN') -Name control -AllowFailure
}
$focus=Invoke-NativeProcess -FilePath $adb -Arguments @('-s',$Serial,'shell','dumpsys','window') -Name foco
$summary=[ordered]@{serial=$Serial;fecha=(Get-Date -Format o);soloLectura=[bool]$SoloLectura;pasoManual=$PasoManual;controlADB=$(if($SoloLectura){'NO COMPROBADO: sin inyeccion'}else{$probe.ExitCode -eq 0});codigo=$(if($probe){$probe.ExitCode}else{$null});login='NO EJECUTADO POR SCRIPT';recorrido='NO EJECUTADO POR SCRIPT';seguridadModificada=$false;captura='NO REALIZADA: Cafe Smart sin foco';destino=$run}
if($focus.StdOut -match 'mCurrentFocus=.*\bcom\.cafesmart\.app/') {
    $ui=Invoke-NativeProcess -FilePath $adb -Arguments @('-s',$Serial,'exec-out','uiautomator','dump','/dev/tty') -Name ui -Sensitive
    $xmlMatch=[regex]::Match($ui.StdOut,'(?s)<\?xml.*?</hierarchy>')
    if($xmlMatch.Success) {
        [xml]$doc=$xmlMatch.Value
        $fields=@($doc.SelectNodes('//node[@class="android.widget.EditText" or @password="true"]'))
        $hasData=@($fields|Where-Object{$_.text.Length -gt 0}).Count -gt 0
        $esRegistro=$doc.SelectNodes('//node[@package="com.cafesmart.app" and @text="Registro de Gastos"]').Count -gt 0
        $sinClaves=$doc.SelectNodes('//node[@password="true"]').Count -eq 0
        $capturaFormulario=$FormularioGastos -and $esRegistro -and $sinClaves
        if($capturaFormulario) {
            # Respaldo privado del borrador antes de una actualización del APK demo.
            @($fields|ForEach-Object{[pscustomobject]@{texto=$_.text;bounds=$_.bounds}})|ConvertTo-Json -Depth 3|
                Set-Content (Join-Path $run 'borrador-gastos-privado.json') -Encoding utf8
        }
        foreach($field in $fields){foreach($node in @($field)+@($field.SelectNodes('.//node'))){foreach($attr in @('text','hint','content-desc')){if($node.HasAttribute($attr)){$node.SetAttribute($attr,'[OMITIDO]')}}}}
        $doc.OuterXml|Set-Content (Join-Path $run 'jerarquia-saneada.xml') -Encoding utf8
        $summary.captura='OMITIDA: formulario con datos'
        if(!$hasData -or $capturaFormulario) {
            # Revalidar foco; leer PNG binario por stdout, sin crear archivos en el telefono.
            $current=Invoke-NativeProcess -FilePath $adb -Arguments @('-s',$Serial,'shell','dumpsys','window') -Name foco-captura
            if($current.StdOut -notmatch 'mCurrentFocus=.*\bcom\.cafesmart\.app/'){throw 'Cafe Smart perdio el foco; no se captura.'}
            $path=Join-Path $run 'estado-observado.png'
            $start=New-Object Diagnostics.ProcessStartInfo
            $start.FileName=$adb
            $start.Arguments='-s '+$Serial+' exec-out screencap -p'
            $start.UseShellExecute=$false
            $start.CreateNoWindow=$true
            $start.RedirectStandardOutput=$true
            $start.RedirectStandardError=$true
            $process=New-Object Diagnostics.Process
            $process.StartInfo=$start
            $stream=$null
            try {
                $null=$process.Start()
                $stderr=$process.StandardError.ReadToEndAsync()
                $stream=[IO.File]::Open($path,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write)
                $process.StandardOutput.BaseStream.CopyTo($stream)
                $stream.Dispose();$stream=$null
                $process.WaitForExit()
                if($process.ExitCode -ne 0){throw 'Fallo al leer captura ADB; conservar diagnostico privado.'}
                $null=$stderr.GetAwaiter().GetResult()
                $bytes=[IO.File]::ReadAllBytes($path)
                if($bytes.Length -lt 8 -or [BitConverter]::ToString($bytes,0,8) -ne '89-50-4E-47-0D-0A-1A-0A'){throw 'Captura no es PNG valido.'}
                $summary.captura='PNG GUARDADO: requiere revision visual, sin aprobar el paso'
            } finally {
                if($stream){$stream.Dispose()}
                $process.Dispose()
            }
        }
    }
}
$summary|ConvertTo-Json|Set-Content (Join-Path $run 'resumen.json') -Encoding utf8
[pscustomobject]$summary
