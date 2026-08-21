$ErrorActionPreference = "Stop"

trap {
    Write-Host "AGP import failed: $($_.Exception.Message)" -ForegroundColor Red
    Read-Host "Press Enter to close this window" | Out-Null
    exit 1
}

$Adb = "C:\Users\WSW\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$Package = "com.woshiwangnima.healthdietpro"
$Preview = Join-Path $PSScriptRoot "output\agp_preview.json"
if (-not (Test-Path -LiteralPath $Preview -PathType Leaf)) {
    throw "Preview JSON is missing. Run 01-GeneratePreview.ps1 and review its PNG first."
}

Write-Host "AGP import preparation"
Write-Host "1. Review tools/agp_pdf/output/agp_preview.png against the PDF."
Write-Host "2. On the test device, open HealthDietPro and switch to the intended current user."
Write-Host "3. Confirm the device already has a debug APK built from this code revision installed."
Read-Host "Press Enter to stage the confirmed preview and import it into that current user" | Out-Null

$Devices = @(& $Adb devices | Where-Object { $_ -match "`tdevice$" } | ForEach-Object { ($_ -split "`t")[0] })
if ($Devices.Count -ne 1) {
    throw "Exactly one connected device is required; found $($Devices.Count)."
}
$Device = $Devices[0]
$RemoteStaging = "/data/local/tmp/healthdietpro_agp_preview.json"
$ResultFile = "files/agp_import/import_result.txt"

& $Adb -s $Device shell run-as $Package mkdir -p files/agp_import
if ($LASTEXITCODE -ne 0) { throw "Unable to create the app import directory. Is the debug APK installed?" }
& $Adb -s $Device shell run-as $Package rm -f $ResultFile files/agp_import/pending_agp_preview.json
if ($LASTEXITCODE -ne 0) { throw "Unable to clear a previous AGP import result in the app private directory." }

& $Adb -s $Device push $Preview $RemoteStaging
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& $Adb -s $Device shell run-as $Package cp $RemoteStaging files/agp_import/pending_agp_preview.json
if ($LASTEXITCODE -ne 0) { throw "Unable to stage the preview JSON into the app private directory." }
& $Adb -s $Device shell rm -f $RemoteStaging
if ($LASTEXITCODE -ne 0) { throw "Unable to remove the temporary device staging file." }
& $Adb -s $Device shell am start -n "$Package/.MainActivity" --ez import_agp_preview true
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$Deadline = (Get-Date).AddSeconds(30)
while ((Get-Date) -lt $Deadline) {
    & $Adb -s $Device shell run-as $Package test -f $ResultFile
    if ($LASTEXITCODE -eq 0) {
        $Result = (& $Adb -s $Device shell run-as $Package cat $ResultFile).Trim()
        if (-not $Result) {
            Start-Sleep -Milliseconds 500
            continue
        }
        if ($Result -match "^SUCCESS:(\d+)$") {
            Write-Host "AGP import succeeded. Added $($Matches[1]) blood glucose records to the device current user."
            Read-Host "Press Enter to close this window" | Out-Null
            exit 0
        }
        if ($Result -match "^FAILED:(.+)$") {
            throw "AGP import failed in the app: $($Matches[1])"
        }
        throw "AGP import returned an unrecognized result: $Result"
    }
    Start-Sleep -Milliseconds 500
}
throw "Timed out waiting for the app import result. Keep the preview JSON; install a debug APK built from the AGP importer code, select the target user in the app, then retry."
