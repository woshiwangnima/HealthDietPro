param(
    [Parameter(Mandatory = $true)]
    [string]$Device,
    [string]$PackageName = "com.woshiwangnima.healthdietpro"
)

# Run this before installing a build that requires BodyRecord dates in yyyy-MM-dd HH:mm form.
# Example: .\tools\Migrate-BodyRecordDates.ps1 -Device "emulator-5554"

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$Adb = "C:\Users\WSW\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$PrefsFile = "health_diet_prefs.xml"
$AppPrefsPath = "shared_prefs/$PrefsFile"
$AppPrefsAbsolutePath = "/data/data/$PackageName/$AppPrefsPath"
$Stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$BackupDir = Join-Path $ProjectRoot "tools\device-backups\$Device-$Stamp"
$LocalPrefs = Join-Path $BackupDir $PrefsFile
$VerifyPrefs = Join-Path $BackupDir "verified-$PrefsFile"

if (!(Test-Path -LiteralPath $Adb)) { throw "adb.exe not found: $Adb" }
New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null

function Start-AdbBinary([string[]]$arguments, [string]$outputFile) {
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = [System.Diagnostics.ProcessStartInfo]@{
        FileName = $Adb
        Arguments = ($arguments | ForEach-Object { '"' + $_.Replace('"', '\"') + '"' }) -join ' '
        UseShellExecute = $false
        RedirectStandardOutput = $true
        RedirectStandardError = $true
        CreateNoWindow = $true
    }
    [void]$process.Start()
    $file = [System.IO.File]::Create($outputFile)
    $process.StandardOutput.BaseStream.CopyTo($file)
    $file.Dispose()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw "adb failed: $stderr" }
}

function Write-AdbBinary([string[]]$arguments, [string]$inputFile) {
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = [System.Diagnostics.ProcessStartInfo]@{
        FileName = $Adb
        Arguments = ($arguments | ForEach-Object { '"' + $_.Replace('"', '\"') + '"' }) -join ' '
        UseShellExecute = $false
        RedirectStandardInput = $true
        RedirectStandardError = $true
        CreateNoWindow = $true
    }
    [void]$process.Start()
    $input = [System.IO.File]::OpenRead($inputFile)
    $input.CopyTo($process.StandardInput.BaseStream)
    $input.Dispose()
    $process.StandardInput.Close()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw "adb failed: $stderr" }
}

& $Adb -s $Device get-state | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Device is not available: $Device" }

& $Adb -s $Device shell am force-stop $PackageName
Start-AdbBinary @("-s", $Device, "exec-out", "run-as", $PackageName, "cat", $AppPrefsPath) $LocalPrefs
if (!(Test-Path -LiteralPath $LocalPrefs) -or (Get-Item -LiteralPath $LocalPrefs).Length -eq 0) {
    throw "Unable to read $AppPrefsPath through run-as."
}

$xml = [System.IO.File]::ReadAllText($LocalPrefs, [System.Text.Encoding]::UTF8)
if ($xml -notmatch '<string name="all_users">') { throw "all_users is missing from $PrefsFile" }
$changedCount = 0

$recordDatePattern = 'date&quot;:&quot;(?<date>\d{4}-\d{2}-\d{2}(?: \d{2}:\d{2})?)&quot;(?<tail>,&quot;(?:recordedAtMillis&quot;:\d+,)?unit&quot;:)'
$xml = [regex]::Replace($xml, $recordDatePattern, {
    param($match)
    $original = $match.Groups['date'].Value
    $parsed = if ($original.Length -eq 10) {
        [datetime]::ParseExact($original, 'yyyy-MM-dd', [Globalization.CultureInfo]::InvariantCulture)
    } else {
        [datetime]::ParseExact($original, 'yyyy-MM-dd HH:mm', [Globalization.CultureInfo]::InvariantCulture)
    }
    $normalized = $parsed.ToString('yyyy-MM-dd HH:mm', [Globalization.CultureInfo]::InvariantCulture)
    $offset = [TimeZoneInfo]::Local.GetUtcOffset($parsed)
    $tail = $match.Groups['tail'].Value
    $script:changedCount++
    if ($tail -match 'recordedAtMillis') {
        "date&quot;:&quot;$normalized&quot;$tail"
    } else {
        $epochMillis = [DateTimeOffset]::new($parsed, $offset).ToUnixTimeMilliseconds()
        "date&quot;:&quot;$normalized&quot;,&quot;recordedAtMillis&quot;:$epochMillis,$($tail.Substring(1))"
    }
})

if ($changedCount -eq 0) {
    "No legacy height or weight records found. Backup retained at $BackupDir"
    exit 0
}

[System.IO.File]::WriteAllText($LocalPrefs, $xml, [System.Text.UTF8Encoding]::new($false))

$deviceBackup = "shared_prefs/health_diet_prefs.body-record-v2-backup-$Stamp.xml"
& $Adb -s $Device shell run-as $PackageName cp $AppPrefsPath $deviceBackup
Write-AdbBinary @("-s", $Device, "exec-in", "run-as", $PackageName, "sh", "-c", "cat > $AppPrefsAbsolutePath") $LocalPrefs

Start-AdbBinary @("-s", $Device, "exec-out", "run-as", $PackageName, "cat", $AppPrefsPath) $VerifyPrefs
$verifyXml = [System.IO.File]::ReadAllText($VerifyPrefs, [System.Text.Encoding]::UTF8)
if ($verifyXml -notmatch 'recordedAtMillis') {
    throw "Migration verification failed. Restore from $deviceBackup or $BackupDir"
}
"Migrated $changedCount height/weight records. Computer backup: $BackupDir; device backup: $deviceBackup"
