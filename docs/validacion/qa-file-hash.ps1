# SHA-256 disponible tambien cuando PowerShell no carga Get-FileHash.
# No depende de modulos, no escribe archivos y cierra siempre el stream.
function Get-QAFileHash {
    param(
        [Parameter(Mandatory=$true, Position=0)]
        [Alias('LiteralPath')][string]$Path,
        [ValidateSet('SHA256')][string]$Algorithm = 'SHA256'
    )
    $absolute = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($Path)
    $stream = [IO.File]::OpenRead($absolute)
    $hasher = [Security.Cryptography.SHA256]::Create()
    try {
        [pscustomobject]@{
            Algorithm = 'SHA256'
            Hash = [BitConverter]::ToString($hasher.ComputeHash($stream)).Replace('-', '')
            Path = $absolute
        }
    } finally {
        $stream.Dispose()
        $hasher.Dispose()
    }
}
