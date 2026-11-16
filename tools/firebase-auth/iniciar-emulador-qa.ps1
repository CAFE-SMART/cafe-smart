param([string]$PrivateRoot=$env:CAFESMART_QA_ROOT)
$ErrorActionPreference='Stop'
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
. (Join-Path $repo 'docs\validacion\qa-private.ps1')
$PrivateRoot=Get-QAPrivateRoot -Repo $repo -Root $PrivateRoot
$runtime=Join-Path $PrivateRoot ('firebase-runtime\'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[guid]::NewGuid().ToString('N').Substring(0,8))
$cli=Join-Path $PSScriptRoot 'node_modules\firebase-tools\lib\bin\firebase.js'
if(!(Test-Path -LiteralPath $cli)){throw 'Faltan dependencias locales; instalar desde package-lock.json.'}
New-Item -ItemType Directory -Path $runtime -Force|Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'firebase.json') -Destination (Join-Path $runtime 'firebase.json')
$oldDebugPath=$env:FIREBASE_DEBUG_PATH
$env:FIREBASE_DEBUG_PATH=Join-Path $runtime 'firebase-debug.log'
Push-Location $runtime
try {
    & node.exe $cli emulators:start --only auth --project demo-cafesmart-qa --config (Join-Path $runtime 'firebase.json') --non-interactive
    if($LASTEXITCODE -ne 0){throw 'Fallo el emulador local; revisar los logs privados.'}
} finally {Pop-Location;$env:FIREBASE_DEBUG_PATH=$oldDebugPath}
