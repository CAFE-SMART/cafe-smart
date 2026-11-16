param([string]$PrivateRoot=$env:CAFESMART_QA_ROOT)
# Cuenta local SOLO en Authentication Emulator; datos fuera del checkout.
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot '..\..\docs\validacion\qa-private.ps1')
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$PrivateRoot=Get-QAPrivateRoot -Repo $repo -Root $PrivateRoot
$account=Get-QALocalAccount -PrivateRoot $PrivateRoot
$project = 'demo-cafesmart-qa'
$hostLocal = 'http://127.0.0.1:9099'
$key = 'AIzaSy000000000000000000000000000000000'
$email = $account.Email
$password = $account.Password
$logDir=Join-Path $PrivateRoot 'firebase-local'
New-Item -ItemType Directory -Path $logDir -Force|Out-Null
$log = Join-Path $logDir ('qa-local-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8) + '.json')

function Auth-Local([string]$Operation, [string]$PasswordValue) {
    # El destino no se recibe por parametro: nunca puede apuntar a Firebase remoto.
    $body = @{email=$email;password=$PasswordValue;returnSecureToken=$true} | ConvertTo-Json
    return Invoke-RestMethod -Uri ($hostLocal + '/identitytoolkit.googleapis.com/v1/accounts:' + $Operation + '?key=' + $key) -Method Post -ContentType 'application/json' -Body $body -TimeoutSec 15
}
function Error-AuthLocal($Record) {
    if ($Record.ErrorDetails.Message) {
        try { return (($Record.ErrorDetails.Message | ConvertFrom-Json).error.message) } catch { }
    }
    return $Record.Exception.Message
}

try {
    $configPath = Join-Path $PSScriptRoot '..\..\app\src\debug\google-services.json'
    $config = Get-Content -Raw $configPath | ConvertFrom-Json
    if ($config.project_info.project_id -cne $project -or $config.client[0].api_key[0].current_key -cne $key) {
        throw 'Configuracion debug distinta del proyecto local esperado.'
    }
    Invoke-RestMethod -Uri ($hostLocal + '/emulator/v1/projects/' + $project + '/config') -TimeoutSec 10 | Out-Null
    $created = $false
    try {
        $account = Auth-Local 'signUp' $password
        $created = $true
    } catch {
        if ((Error-AuthLocal $_) -notmatch '^EMAIL_EXISTS(?:\s|$)') { throw }
        # No restablecer ni editar una cuenta existente: exigir la clave sintetica conocida.
        $account = Auth-Local 'signInWithPassword' $password
    }
    $session = Auth-Local 'signInWithPassword' $password
    if (!$session.localId -or !$session.idToken) { throw 'Falta confirmacion de login local.' }
    $payload = $session.idToken.Split('.')[1].Replace('-','+').Replace('_','/')
    $payload = $payload.PadRight($payload.Length + ((4 - $payload.Length % 4) % 4), '=')
    $claims = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($payload)) | ConvertFrom-Json
    if ($claims.aud -cne $project) { throw 'Token de proyecto distinto del demo local.' }
    $wrongRejected = $false
    try { Auth-Local 'signInWithPassword' 'Incorrecta-QA-local-2026!' | Out-Null } catch {
        if ((Error-AuthLocal $_) -match '^(INVALID_PASSWORD|INVALID_LOGIN_CREDENTIALS)(?:\s|$)') { $wrongRejected = $true } else { throw }
    }
    if (!$wrongRejected) { throw 'El servidor no rechazo la clave incorrecta.' }
    [ordered]@{fecha=(Get-Date -Format o);proyecto=$project;host=$hostLocal;email=$email;cuentaCreada=$created;loginREST='APROBADO';claveIncorrectaREST='APROBADO';loginPixel='NO EJECUTADO';loginXiaomi='NO EJECUTADO'} |
        ConvertTo-Json | Set-Content -LiteralPath $log -Encoding utf8
    Write-Host 'Cuenta QA local disponible. Login REST y rechazo de clave incorrecta comprobados.'
    Write-Host "Registro sin tokens: $log"
    Write-Host 'Pendiente: probar la interfaz en Pixel y Xiaomi. Estos resultados no aprueban las pantallas.'
} catch {
    [ordered]@{fecha=(Get-Date -Format o);estado='FALLIDO O BLOQUEADO';error=$_.Exception.Message} |
        ConvertTo-Json | Set-Content -LiteralPath $log -Encoding utf8
    throw
}
