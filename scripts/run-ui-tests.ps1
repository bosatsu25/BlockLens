param(
    [ValidateSet('All', '26.1.2', '26.2', '26.3')]
    [string]$Version = 'All'
)

$ErrorActionPreference = 'Stop'
$targets = [ordered]@{
    '26.1.2' = 'mc26_1_2'
    '26.2' = 'mc26_2'
    '26.3' = 'mc26_3'
}
$gradleCommand = Get-Command gradle -CommandType Application -ErrorAction Stop | Select-Object -First 1
$repositoryRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Push-Location -LiteralPath $repositoryRoot
try {
    foreach ($target in $targets.GetEnumerator()) {
        if ($Version -ne 'All' -and $Version -ne $target.Key) { continue }
        Write-Output "Launching Minecraft $($target.Key): settings UI, save/discard, screenshots and render tests."
        & $gradleCommand.Source --no-daemon --no-parallel --console=plain ":versions:$($target.Value):runClientGameTest"
        if ($LASTEXITCODE -ne 0) {
            throw "Minecraft $($target.Key) client tests failed (exit $LASTEXITCODE)."
        }
        Write-Output "Minecraft $($target.Key): PASS. Look for settings-ui screenshots beneath the module's build directory."
    }
} finally {
    Pop-Location
}
