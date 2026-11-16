param([string]$PrivateRoot=$env:CAFESMART_QA_ROOT,[switch]$Retirar,[switch]$SinRespaldo,[string]$CuentaDesdeRespaldo)
$ErrorActionPreference='Stop'
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..')).TrimEnd('\')
. (Join-Path $PSScriptRoot 'qa-private.ps1')
. (Join-Path $PSScriptRoot 'qa-file-hash.ps1')
function Get-ArchiveHash([string]$Path) {
    $stream=[IO.File]::Open($Path,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::ReadWrite)
    $hasher=[Security.Cryptography.SHA256]::Create()
    try { [pscustomobject]@{Hash=[BitConverter]::ToString($hasher.ComputeHash($stream)).Replace('-','')} }
    finally { $stream.Dispose();$hasher.Dispose() }
}
$PrivateRoot=Get-QAPrivateRoot -Repo $repo -Root $PrivateRoot
if($CuentaDesdeRespaldo) {
    $source=[IO.Path]::GetFullPath($CuentaDesdeRespaldo)
    if(!$source.StartsWith($PrivateRoot+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'El respaldo de cuenta debe estar en la carpeta privada.'}
    $text=[IO.File]::ReadAllText($source)
    $account=@{}
    foreach($match in [regex]::Matches($text,'(?m)^\$(email|password)\s*=\s*''([^'']+)''')){$account[$match.Groups[1].Value]=$match.Groups[2].Value}
    if(!$account.email -or !$account.password){throw 'No se encontro una cuenta local completa en el respaldo.'}
    $directory=Join-Path $PrivateRoot 'credenciales'
    $target=Join-Path $directory 'auth-local.json'
    if(Test-Path -LiteralPath $target){throw 'No se sobrescribe una cuenta privada existente.'}
    New-Item -ItemType Directory -Path $directory -Force|Out-Null
    $account|ConvertTo-Json|Set-Content -LiteralPath $target -Encoding utf8
    $check=Get-Content -LiteralPath $target -Raw -Encoding utf8|ConvertFrom-Json
    if($check.email -cne $account.email -or $check.password -cne $account.password){throw 'La cuenta privada copiada no coincide con el respaldo.'}
    return [pscustomobject]@{cuentaLocalExternalizada=$true;valoresImpresos=$false}
}
$batch=Join-Path $PrivateRoot ('entrega-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[guid]::NewGuid().ToString('N').Substring(0,8))
Push-Location $repo
try {
    # Solo resultados generados revisados; nunca fuentes Kotlin ni assets de producto.
    $artifacts=@()
    foreach($base in @('app/build/outputs','app/build/reports','app/build/test-results','build/reports')) {
        if(Test-Path -LiteralPath $base) {
            $artifacts+=@(Get-ChildItem -LiteralPath $base -Recurse -File -Force|ForEach-Object{$_.FullName.Substring($repo.Length+1)})
        }
    }
    $artifacts+=@(Get-ChildItem tools/firebase-auth -File -Force -ErrorAction SilentlyContinue|
        Where-Object{$_.Name -like '*-debug.log' -or $_.Name -like 'qa-local-*.json'}|
        ForEach-Object{$_.FullName.Substring($repo.Length+1)})
    $tracked=@(git ls-files)
    if($LASTEXITCODE -ne 0){throw 'No se pudo consultar el indice.'}
    foreach($file in $artifacts){if($file.Replace('\','/') -cin $tracked){throw "Evidencia versionada: $file. Requiere autorizacion sobre Git."}}
    $backups=@()
    if(!$SinRespaldo) {
        $backups=@(git diff --name-only)+@(git ls-files --others --exclude-standard -- app/src docs/validacion tools/firebase-auth)+@('.gitignore')
        $backups=@($backups|Where-Object{$_ -cnotin $artifacts -and (Test-Path -LiteralPath $_ -PathType Leaf)}|Sort-Object -Unique)
    }
    $rows=@($artifacts|ForEach-Object{[pscustomobject]@{path=$_;kind='evidencia';remove=$true}})+
        @($backups|ForEach-Object{[pscustomobject]@{path=$_;kind='respaldo-codigo';remove=$false}})
    New-Item -ItemType Directory -Path $batch -Force|Out-Null
    $manifest=foreach($row in $rows) {
        $source=[IO.Path]::GetFullPath((Join-Path $repo $row.path))
        if(!$source.StartsWith($repo+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Origen fuera del checkout.'}
        $group=if(!$row.remove){'respaldos-codigo'}elseif($row.path -like '*androidTest-results*'){'pixel'}elseif($row.path -like '*test-results*'){'pruebas-jvm'}elseif($row.path -like '*outputs*apk*'){'binarios'}elseif($row.path -like '*firebase-auth*'){'firebase-local'}else{'logs-diagnosticos'}
        # Evitar MAX_PATH en Windows PowerShell: nombre corto y unico;
        # el manifiesto conserva siempre la ruta original completa.
        $name=[IO.Path]::GetFileNameWithoutExtension($row.path)
        if($name.Length -gt 60){$name=$name.Substring(0,60)}
        $pathHasher=[Security.Cryptography.SHA256]::Create()
        try {$suffix=[BitConverter]::ToString($pathHasher.ComputeHash([Text.Encoding]::UTF8.GetBytes($row.path))).Replace('-','').Substring(0,12)} finally {$pathHasher.Dispose()}
        $shortName=$name+'-'+$suffix+[IO.Path]::GetExtension($row.path)
        $destination=[IO.Path]::GetFullPath((Join-Path (Join-Path $batch $group) $shortName))
        if(!$destination.StartsWith($batch+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Destino fuera del archivo privado.'}
        New-Item -ItemType Directory -Path (Split-Path $destination) -Force|Out-Null
        if(Test-Path -LiteralPath $destination){throw 'Colision de nombres en el respaldo; no se sobrescribe.'}
        $bytes=(Get-Item -LiteralPath $source).Length
        $hash=(Get-ArchiveHash $source).Hash
        $inputStream=[IO.File]::Open($source,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::ReadWrite)
        try { $outputStream=[IO.File]::Create($destination);try{$inputStream.CopyTo($outputStream)}finally{$outputStream.Dispose()} } finally {$inputStream.Dispose()}
        if((Get-Item -LiteralPath $destination).Length -ne $bytes -or
            (Get-ArchiveHash $destination).Hash -cne $hash -or (Get-ArchiveHash $source).Hash -cne $hash) {
            throw "Copia no verificada: $($row.path)"
        }
        [pscustomobject]@{path=$row.path;source=$source;destination=$destination;kind=$row.kind;bytes=$bytes;sha256=$hash;verified=$true;remove=$row.remove;removed=$false;error=$null}
    }
    $manifest|ConvertTo-Json -Depth 4|Set-Content (Join-Path $batch 'copias-verificadas.json') -Encoding utf8
    git status --short --branch|Set-Content (Join-Path $batch 'git-inicial.txt') -Encoding utf8
    git stash list --format='%gd %H %gs'|Set-Content (Join-Path $batch 'stashes-iniciales.txt') -Encoding utf8
    git diff --binary --output="$batch/trabajo-inicial.patch"
    # Todas las copias estan verificadas antes de retirar el primer original.
    if($Retirar) {
        foreach($row in $manifest|Where-Object{$_.remove}) {
            try {
                if(!$row.source.StartsWith($repo+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Origen no permitido.'}
                if((Get-ArchiveHash $row.source).Hash -cne $row.sha256 -or (Get-ArchiveHash $row.destination).Hash -cne $row.sha256){throw 'Archivo cambiado tras la verificacion.'}
                Remove-Item -LiteralPath $row.source -ErrorAction Stop
                $row.removed=$true
            } catch { $row.error=$_.Exception.Message }
        }
    }
    $manifest|ConvertTo-Json -Depth 4|Set-Content (Join-Path $batch 'manifest-traslado.json') -Encoding utf8
    $summary=[pscustomobject]@{destino=$batch;evidenciasCopiadas=@($manifest|Where-Object{$_.remove}).Count;evidenciasRetiradas=@($manifest|Where-Object{$_.removed}).Count;respaldosCodigo=@($manifest|Where-Object{!$_.remove}).Count;errores=@($manifest|Where-Object{$_.error}|ForEach-Object{$_.path})}
    $summary|ConvertTo-Json -Depth 4|Set-Content (Join-Path $batch 'resumen-traslado.json') -Encoding utf8
    if($summary.errores.Count){throw "Hay originales no retirados. Revisar $batch/manifest-traslado.json"}
    return $summary
} finally { Pop-Location }
