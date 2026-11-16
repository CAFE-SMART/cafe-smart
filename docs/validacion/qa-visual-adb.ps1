param(
    [ValidateSet('emulator-5554','DEQCAQEUVS9PNRDQ')]
    [string[]]$Dispositivos = @('emulator-5554','DEQCAQEUVS9PNRDQ'),
    [switch]$ComprobarFunciones,
    [ValidatePattern('^[0-9a-f]{40}$')]
    [string]$HeadEsperado = '98d1050a1dbc133124d833619af40aca2e4fa135',
    [string]$PrivateRoot = $env:CAFESMART_QA_ROOT
)
# Ejecutar en PowerShell normal. No instala, no crea gastos y no ejecuta instrumentadas.
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'qa-file-hash.ps1')
. (Join-Path $PSScriptRoot 'qa-private.ps1')
function Texto-QA([string]$Value) {
    return ([regex]::Replace($Value.Normalize([Text.NormalizationForm]::FormD), '\p{Mn}', '') -replace '\s+', ' ').Trim().ToLowerInvariant()
}
function Limites-QA($Node) {
    if ($Node.bounds -notmatch '^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$') { throw 'Control sin limites verificables.' }
    $left=[int]$Matches[1]; $top=[int]$Matches[2]; $right=[int]$Matches[3]; $bottom=[int]$Matches[4]
    if ($right -le $left -or $bottom -le $top) { throw 'Control fuera de pantalla.' }
    return [pscustomobject]@{X=[int](($left+$right)/2);Y=[int](($top+$bottom)/2);Right=$right;Bottom=$bottom}
}
function Nodos-QA($Ui,[string]$Label) {
    $wanted=Texto-QA $Label
    return @($Ui.SelectNodes('//node') | Where-Object {
        $_.enabled -eq 'true' -and ($_.package -eq 'com.cafesmart.app') -and
        ((Texto-QA $_.text) -ceq $wanted -or (Texto-QA $_.'content-desc') -ceq $wanted)
    })
}
function Campo-NodoQA($Ui,[int]$Index) {
    if ($Index -notin @(0,1)) { throw 'Tipo de campo QA desconocido.' }
    $label=if ($Index -eq 0) { 'correo electronico' } else { 'contrasena' }
    $found=@($Ui.SelectNodes('//node[@package="com.cafesmart.app" and @enabled="true"]') | Where-Object {
        if ($_.class -ne 'android.widget.EditText' -and $_.editable -ne 'true') { return $false }
        $previous=$_.SelectSingleNode('preceding-sibling::node[@class="android.widget.TextView"][1]')
        $description=Texto-QA $_.'content-desc'
        $hasLabel=($previous -and (Texto-QA $previous.text) -ceq $label) -or $description -ceq $label
        # AuthField: etiqueta Text seguida de OutlinedTextField. Nunca por indice
        # global ni por cantidad total: IME/scroll pueden ocultar el otro campo.
        $hasLabel -and (($Index -eq 1 -and $_.password -eq 'true') -or ($Index -eq 0 -and $_.password -eq 'false'))
    })
    if ($found.Count -gt 1) { throw 'Campo QA ambiguo; no se escribe.' }
    if (!$found.Count) { return $null }
    Limites-QA $found[0] | Out-Null
    return $found[0]
}
if ($ComprobarFunciones) {
    [xml]$fixture='<hierarchy><node package="com.cafesmart.app" enabled="true" text="Inventario" bounds="[10,20][110,70]"/><node package="otra.app" enabled="true" text="Inventario" bounds="[0,0][20,20]"/></hierarchy>'
    $nodes=@(Nodos-QA $fixture 'Inventario')
    if ($nodes.Count -ne 1 -or (Limites-QA $nodes[0]).X -ne 60) { throw 'Fallo selector/bounds.' }
    $bad=$false
    try { Limites-QA ([pscustomobject]@{bounds='[0,0][0,0]'}) | Out-Null } catch { $bad=$true }
    if (!$bad -or (Texto-QA ('Sesi'+[char]0x00f3+'n')) -cne 'sesion') { throw 'Fallo normalizacion/limites invalidos.' }
    Write-Host 'Funciones puras verificadas; sin ADB, cuentas ni dispositivos.'
    return
}

$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$PrivateRoot=Get-QAPrivateRoot -Repo $repo -Root $PrivateRoot
$account=Get-QALocalAccount -PrivateRoot $PrivateRoot
$QaEmail=$account.Email; $QaPassword=$account.Password
$adb=Join-Path (Get-QASdkRoot) 'platform-tools\adb.exe'
$stamp=(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[guid]::NewGuid().ToString('N').Substring(0,8)
$helper=Join-Path $PSScriptRoot 'validar-dos-dispositivos.ps1'
# Cargar solo las funciones de captura nativa ya verificadas; nunca ejecutar su accion principal.
$tokens=$null; $parseErrors=$null
$ast=[Management.Automation.Language.Parser]::ParseFile($helper,[ref]$tokens,[ref]$parseErrors)
if ($parseErrors.Count) { throw ($parseErrors | Out-String) }
foreach ($name in @('Quote-NativeArgument','Invoke-NativeProcess','ADB-Checked')) {
    $functionAst=@($ast.FindAll({param($n) $n -is [Management.Automation.Language.FunctionDefinitionAst] -and $n.Name -eq $name},$true))
    if ($functionAst.Count -ne 1) { throw "Funcion auxiliar no unica: $name" }
    . ([scriptblock]::Create($functionAst[0].Extent.Text))
}
function QA-Adb([string[]]$Arguments,[switch]$Sensitive) { return ADB-Checked (@('-s',$Serial)+$Arguments) -Sensitive:$Sensitive }
function App-QA {
    $focus=(QA-Adb @('shell','dumpsys','window')) -join "`n"
    if ($focus -notmatch 'mCurrentFocus=.*\bcom\.cafesmart\.app/') {
        throw 'Cafe Smart no tiene foco. No se captura ni controla otra aplicacion.'
    }
}
function Leer-QA {
    App-QA
    $id=[guid]::NewGuid().ToString('N')
    $local=Join-Path $run ('ui-'+$id+'.xml')
    # XML solo en memoria hasta sanearlo; no guardar secretos ni en pull/logs.
    $raw=(QA-Adb @('exec-out','uiautomator','dump','/dev/tty') -Sensitive) -join "`n"
    $xmlMatch=[regex]::Match($raw,'(?s)<\?xml.*?</hierarchy>')
    if (!$xmlMatch.Success) { throw 'UIAutomator no devolvio una jerarquia XML completa.' }
    [xml]$doc=$xmlMatch.Value
    [xml]$safe=$doc.OuterXml
    foreach ($node in $safe.SelectNodes('//node[@password="true"]')) {
        foreach ($child in @($node)+@($node.SelectNodes('.//node'))) {
            foreach ($attribute in @('text','content-desc','hint')) {
                if ($child.HasAttribute($attribute)) { $child.SetAttribute($attribute,'[OMITIDO]') }
            }
        }
    }
    $safe.OuterXml.Replace($QaPassword,'[OMITIDO]') | Set-Content -LiteralPath $local -Encoding utf8
    return ,$doc
}
function Capturar-QA([string]$Name) {
    App-QA
    $captureUi=Leer-QA
    if (@($captureUi.SelectNodes('//node[@class="android.widget.EditText" or @password="true"]')).Count) {
        throw 'No se captura un formulario con posibles credenciales.'
    }
    $path=Join-Path $run ($Name+'.png')
    if (Test-Path $path) { throw 'No se sobrescribe una captura existente.' }
    $remote='/sdcard/cafesmart-qa-'+[guid]::NewGuid().ToString('N')+'.png'
    QA-Adb @('shell','screencap','-p',$remote) | Out-Null
    QA-Adb @('pull',$remote,$path) | Out-Null
    Add-Type -AssemblyName System.Drawing
    $image=[Drawing.Image]::FromFile($path)
    try { if ($image.Width -le 0) { throw 'PNG invalido.' } } finally { $image.Dispose() }
    $script:shots+=@{archivo=$path;sha256=(Get-QAFileHash $path).Hash}
    return $path
}
function Pulsar-NodoQA($Node) {
    $target=$Node
    while ($target.Name -eq 'node' -and $target.clickable -ne 'true' -and $target.ParentNode.Name -eq 'node') { $target=$target.ParentNode }
    if ($target.clickable -ne 'true') { throw 'No se encontro un control pulsable.' }
    $b=Limites-QA $target
    App-QA
    QA-Adb @('shell','input','tap',"$($b.X)","$($b.Y)") | Out-Null
}
function Buscar-PulsarQA([string]$Label) {
    for ($attempt=0;$attempt -lt 4;$attempt++) {
        $ui=Leer-QA
        $nodes=@(Nodos-QA $ui $Label)
        if ($nodes.Count -gt 1 -and $Label -ceq 'Verde') {
            # Inventario tiene un filtro Verde y una tarjeta Verde. Abrir la
            # tarjeta que muestra su cantidad de sublotes, no el filtro.
            $nodes=@($nodes | Where-Object {
                $target=$_
                while ($target.clickable -ne 'true' -and $target.ParentNode.Name -eq 'node') { $target=$target.ParentNode }
                @($target.SelectNodes('.//node') | Where-Object { (Texto-QA $_.text) -match '^(\d+|sin) sublotes?$' }).Count -eq 1
            })
        }
        if (!$nodes.Count -and $Label -ceq 'Inventario') {
            # La barra actual dibuja el boton central sin texto ni content-desc.
            # Identificarlo solo entre Compras y Ventas de la misma fila inferior.
            $left=@(Nodos-QA $ui 'Compras' | Sort-Object { (Limites-QA $_).Y } -Descending)
            $right=@(Nodos-QA $ui 'Ventas' | Sort-Object { (Limites-QA $_).Y } -Descending)
            if ($left.Count -and $right.Count) {
                $l=Limites-QA $left[0]; $r=Limites-QA $right[0]
                if ([Math]::Abs($l.Y-$r.Y) -le 10) {
                    $nodes=@($ui.SelectNodes('//node[@package="com.cafesmart.app" and @clickable="true" and @enabled="true"]') | Where-Object {
                        $b=Limites-QA $_
                        $b.X -gt $l.X -and $b.X -lt $r.X -and
                        [Math]::Abs($b.Y-$l.Y) -le ($r.X-$l.X)/3 -and
                        2*($b.Right-$b.X) -lt ($r.X-$l.X)/2 -and
                        !$_.text -and !$_.'content-desc' -and !@($_.SelectNodes('.//node[@text!="" or @content-desc!=""]')).Count
                    })
                }
            }
        }
        if ($nodes.Count -eq 1) { Pulsar-NodoQA $nodes[0]; return }
        if ($nodes.Count -gt 1) { throw "Control ambiguo: $Label" }
        if ($attempt -eq 3) { break }
        # Resolucion real del dispositivo; no depender de nodos recortados/virtualizados.
        $size=(QA-Adb @('shell','wm','size')) -join "`n"
        $sizes=@([regex]::Matches($size,'(?:Physical|Override) size:\s*(\d+)x(\d+)'))
        if (!$sizes.Count) { throw 'Sin viewport verificable.' }
        $width=[int]$sizes[-1].Groups[1].Value
        $height=[int]$sizes[-1].Groups[2].Value
        App-QA
        QA-Adb @('shell','input','swipe',"$([int]($width/2))","$([int]($height*.75))","$([int]($width/2))","$([int]($height*.3))",'300') | Out-Null
    }
    throw "Control no accesible sin modificar datos: $Label"
}
function Esperar-QA([string]$Label) {
    for ($attempt=0;$attempt -lt 4;$attempt++) {
        $ui=Leer-QA
        if (@(Nodos-QA $ui $Label).Count) { return ,$ui }
        Start-Sleep -Milliseconds 500
    }
    throw "No se observo la pantalla esperada: $Label"
}
function Ocultar-TecladoQA {
    App-QA
    $ime=(QA-Adb @('shell','dumpsys','input_method') -Sensitive) -join "`n"
    if ($ime -match '\bmInputShown=true\b') {
        # Escape no oculto Gboard en el Pixel; Back solo si el IME esta abierto.
        QA-Adb @('shell','input','keyevent','KEYCODE_BACK') | Out-Null
        $after=(QA-Adb @('shell','dumpsys','input_method') -Sensitive) -join "`n"
        if ($after -notmatch '\bmInputShown=false\b') { throw 'No se confirmo teclado oculto; no se pulsa otra pantalla.' }
    } elseif ($ime -notmatch '\bmInputShown=false\b') { throw 'Estado del teclado no verificable.' }
}
function Campo-QA([int]$Index,[string]$Value) {
    if ($Value -notmatch '^[A-Za-z0-9@.!_-]+$') { throw 'Entrada no admitida por el ejecutor QA.' }
    App-QA
    Ocultar-TecladoQA
    $ui=Leer-QA
    $field=Campo-NodoQA $ui $Index
    if (!$field) { throw 'Campo etiquetado no visible tras ocultar teclado; no se escribe. Requiere observacion visual.' }
    if ($Index -eq 0 -and $field.text -and
        (!$field.text.StartsWith('qa-',[StringComparison]::Ordinal) -or !$Value.StartsWith($field.text,[StringComparison]::Ordinal))) {
        throw 'Correo preexistente ajeno a QA; no se modifica.'
    }
    if ($Index -eq 1 -and $field.text) { throw 'Contrasena preexistente; no se modifica.' }
    $b=Limites-QA $field
    App-QA
    QA-Adb @('shell','input','tap',"$($b.X)","$($b.Y)") | Out-Null
    $focused=Leer-QA
    $focusedField=Campo-NodoQA $focused $Index
    if (!$focusedField -or $focusedField.focused -ne 'true') { throw 'No se confirmo el foco del campo etiquetado; no se escribe.' }
    $length=$focusedField.text.Length
    if ($length -gt 80) { throw 'Contenido preexistente no reconocido.' }
    if ($Index -eq 0 -and !$Value.StartsWith($focusedField.text,[StringComparison]::Ordinal)) {
        throw 'El contenido del correo cambio; no se escribe.'
    }
    if ($Index -eq 1 -and $length) { throw 'Contrasena preexistente; no se modifica.' }
    # En el Pixel se observaron eventos perdidos al enviar todo el correo de golpe.
    # Completar solo un prefijo QA comprobado, sin borrar texto, y espaciar eventos.
    if ($length) { QA-Adb @('shell','input','keyevent','KEYCODE_MOVE_END') | Out-Null }
    foreach ($character in $Value.Substring($length).ToCharArray()) {
        App-QA
        QA-Adb @('shell','input','text',[string]$character) -Sensitive | Out-Null
        Start-Sleep -Milliseconds 150
    }
    Start-Sleep -Milliseconds 350
    $after=Campo-NodoQA (Leer-QA) $Index
    if (!$after -or $after.focused -ne 'true') { throw 'El foco cambio durante la entrada; detener sin reintentar.' }
    if ($Index -eq 0 -and $after.text -cne $Value) { throw 'El correo QA no se introdujo completo; detener sin enviar login.' }
    if ($Index -eq 1 -and ($after.password -ne 'true' -or $after.text.Length -ne $Value.Length)) { throw 'Longitud/ocultacion de contrasena no verificable; no enviar login.' }
}

Push-Location $repo
try {
    if ((git branch --show-current) -cne 'feat/gastos-ciclo2' -or (git rev-parse HEAD) -cne $HeadEsperado) { throw 'Rama/HEAD inesperados.' }
    foreach ($Serial in $Dispositivos) {
        $folder=if ($Serial -eq 'emulator-5554') { 'pixel-9-pro' } else { 'xiaomi-android-16' }
        $run=Join-Path $PrivateRoot ('gastos-sublote\'+$folder+'\qa-adb-'+$stamp)
        New-Item -ItemType Directory -Path $run | Out-Null
        $script:shots=@()
        $results=[ordered]@{serial=$Serial;fecha=(Get-Date -Format o);login='NO EJECUTADO';inicio='NO EJECUTADO';inventario='NO EJECUTADO';detalle='NO EJECUTADO';gastos='NO EJECUTADO';vacio='NO EJECUTADO';lista='NO EJECUTADO';total='NO EJECUTADO';volverCompose='NO EJECUTADO';volverSistema='NO EJECUTADO';errorRecuperacion='BLOQUEADO: sin mecanismo grafico seguro conocido';revisionVisual='PENDIENTE DE REVISAR PNG'}
        try {
            # Precondicion real: servidor, reverse, APK firmado y recursos demo; no instala.
            & $helper -Accion VerificarAuthLocal -Serial $Serial -HeadEsperado $HeadEsperado -PrivateRoot $PrivateRoot
            App-QA
            $ui=Leer-QA
            if (@(Nodos-QA $ui 'Entrar').Count -or (Campo-NodoQA $ui 0)) {
                Campo-QA 0 $QaEmail
                Campo-QA 1 $QaPassword
                App-QA
                Ocultar-TecladoQA
                Buscar-PulsarQA 'Entrar'
                $ui=Esperar-QA 'Bienvenido a'
                $results.login='VERIFICADO EN JERARQUIA UI; revisar captura'
            } else {
                # Una sesion abierta puede conservar el scroll de Inicio.
                Buscar-PulsarQA 'Inicio'
                for ($attempt=0;$attempt -lt 4;$attempt++) {
                    $ui=Leer-QA
                    if (@(Nodos-QA $ui 'Bienvenido a').Count) { break }
                    if (@($ui.SelectNodes('//node[@class="android.widget.EditText"]')).Count) { throw 'No se desplaza un formulario para buscar Inicio.' }
                    $size=(QA-Adb @('shell','wm','size')) -join "`n"
                    $sizes=@([regex]::Matches($size,'(?:Physical|Override) size:\s*(\d+)x(\d+)'))
                    if (!$sizes.Count) { throw 'Sin viewport verificable.' }
                    $width=[int]$sizes[-1].Groups[1].Value; $height=[int]$sizes[-1].Groups[2].Value
                    App-QA
                    QA-Adb @('shell','input','swipe',"$([int]($width/2))","$([int]($height*.3))","$([int]($width/2))","$([int]($height*.75))",'300') | Out-Null
                }
                $ui=Esperar-QA 'Bienvenido a'
                $results.login='NO EJECUTADO: sesion ya abierta; no se cierra'
            }
            Capturar-QA '00-inicio' | Out-Null
            $results.inicio='OBSERVADO; revisar PNG'
            Buscar-PulsarQA 'Inventario'
            $ui=Esperar-QA 'Inventario'
            Capturar-QA '00-inventario' | Out-Null
            $results.inventario='OBSERVADO; revisar PNG'
            Buscar-PulsarQA 'Verde'
            Buscar-PulsarQA 'Ver sublotes de una calidad'
            Buscar-PulsarQA 'Detalle del sublote (ejemplo)'
            $ui=Esperar-QA 'Detalle del sublote #1'
            $results.detalle='OBSERVADO: #1, flujo provisional'
            Buscar-PulsarQA 'Gastos del sublote'
            $ui=Esperar-QA 'Gastos del sublote'
            Capturar-QA '01-acceso-gastos-sublote' | Out-Null
            $results.gastos='OBSERVADO; revisar PNG e ID'
            $allText=($ui.SelectNodes('//node[@package="com.cafesmart.app"]') | ForEach-Object { $_.text }) -join "`n"
            if ((Texto-QA $allText).Contains('este sublote todavia no tiene gastos registrados.')) {
                Capturar-QA '02-estado-vacio' | Out-Null
                $results.vacio='MENSAJE OBSERVADO; revisar PNG'
                $money=@([regex]::Matches($allText,'\$\s*([0-9][0-9.,]*)'))
                if ($money.Count -eq 1 -and ($money[0].Groups[1].Value -replace '[.,]','') -match '^0+$') {
                    Capturar-QA '04-total-asignado' | Out-Null
                    $results.total='CERO OBSERVADO con estado vacio; revisar PNG'
                } else { $results.total='PENDIENTE: no se identifico un total cero unico' }
                $results.lista='BLOQUEADO: sublote vacio; no se crean gastos'
            } elseif ((Texto-QA $allText).Contains('valor asignado:')) {
                Capturar-QA '03-lista-gastos' | Out-Null
                Capturar-QA '04-total-asignado' | Out-Null
                $results.lista='FILAS OBSERVADAS; revisar datos y filtrado'
                $results.total='PENDIENTE de suma manual de todas las asignaciones; no inferir de filas parciales'
            } else { $results.lista='BLOQUEADO: estado no reconocido'; $results.total='PENDIENTE' }
            Buscar-PulsarQA 'Volver'
            $ui=Esperar-QA 'Detalle del sublote #1'
            Capturar-QA '06-navegacion' | Out-Null
            $results.volverCompose='DETALLE #1 OBSERVADO tras Volver; revisar PNG'
            Buscar-PulsarQA 'Gastos del sublote'
            $ui=Esperar-QA 'Gastos del sublote'
            App-QA
            QA-Adb @('shell','input','keyevent','KEYCODE_BACK') | Out-Null
            $ui=Esperar-QA 'Detalle del sublote #1'
            $results.volverSistema='DETALLE #1 OBSERVADO tras Back; revisar XML'
        } catch {
            $results.bloqueo=$_.Exception.Message
            $_ | Out-String | Set-Content (Join-Path $run 'error.txt') -Encoding utf8
            Write-Host "QA detenido en $Serial : $($_.Exception.Message)"
        } finally {
            $results.capturas=$script:shots
            $results | ConvertTo-Json -Depth 7 | Set-Content (Join-Path $run 'resultados.json') -Encoding utf8
            Write-Host "Evidencias de $Serial : $run"
            $report=Join-Path $run 'informe-ejecucion.md'
            $summary=@('',('### Ejecucion visual ADB '+$Serial+' / '+$stamp),'',
                'Resultados de observacion automatica; los PNG requieren revision visual. No se repitieron instrumentadas.',
                '', '| Caso | Resultado observado |', '|---|---|')
            foreach ($key in @('login','inicio','inventario','detalle','gastos','vacio','lista','total','volverCompose','volverSistema','errorRecuperacion')) {
                $summary+='| '+$key+' | '+($results[$key] -replace '\|','/ ' -replace '[\r\n]+',' ')+' |'
            }
            $summary+=@('',('Directorio: `'+$run+'`. Capturas reales: '+$script:shots.Count+'.'))
            if ($results.bloqueo) { $summary+=('Bloqueo: '+$results.bloqueo) }
            $summary | Add-Content -LiteralPath $report -Encoding utf8
        }
    }
} finally { Pop-Location }
