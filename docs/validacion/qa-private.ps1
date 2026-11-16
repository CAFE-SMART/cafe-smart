# Directorio de trabajo privado, siempre fuera del checkout.
function Get-QASdkRoot {
    if($env:ANDROID_HOME){return $env:ANDROID_HOME}
    if($env:ANDROID_SDK_ROOT){return $env:ANDROID_SDK_ROOT}
    return (Join-Path ([Environment]::GetFolderPath('UserProfile')) 'AppData\Local\Android\Sdk')
}

function Get-QAPrivateRoot {
    param([string]$Repo,[string]$Root=$env:CAFESMART_QA_ROOT)
    if([string]::IsNullOrWhiteSpace($Root)) {
        $Root=Join-Path ([Environment]::GetFolderPath('UserProfile')) 'Documents\CafeSmart-QA-Privado'
    }
    $repoPath=[IO.Path]::GetFullPath($Repo).TrimEnd('\','/')
    $privatePath=[IO.Path]::GetFullPath($Root).TrimEnd('\','/')
    if($privatePath.Equals($repoPath,[StringComparison]::OrdinalIgnoreCase) -or
        $privatePath.StartsWith($repoPath+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) {
        throw 'La carpeta QA debe estar fuera del repositorio.'
    }
    return $privatePath
}

function Get-QALocalAccount {
    param([string]$PrivateRoot)
    $email=$env:CAFESMART_QA_EMAIL
    $password=$env:CAFESMART_QA_PASSWORD
    if(!$email -or !$password) {
        $path=Join-Path $PrivateRoot 'credenciales\auth-local.json'
        if(!(Test-Path -LiteralPath $path)){throw 'Definir cuenta QA en variables de entorno o en el archivo privado credenciales/auth-local.json.'}
        $account=Get-Content -LiteralPath $path -Raw -Encoding utf8|ConvertFrom-Json
        $email=$account.email; $password=$account.password
    }
    if(!$email -or !$password){throw 'Cuenta QA local incompleta.'}
    return [pscustomobject]@{Email=[string]$email;Password=[string]$password}
}
