$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$InputDirectory = Join-Path $PSScriptRoot "input"
$OutputDirectory = Join-Path $PSScriptRoot "output"
New-Item -ItemType Directory -Force -Path $InputDirectory, $OutputDirectory | Out-Null
$PdfFiles = @(Get-ChildItem -LiteralPath $InputDirectory -Filter "*.pdf" -File)
if ($PdfFiles.Count -ne 1) {
    throw "Place exactly one AGP PDF in $InputDirectory before generating a preview."
}

$Preview = Join-Path $OutputDirectory "agp_preview.json"
$Chart = Join-Path $OutputDirectory "agp_preview.png"
uv run python (Join-Path $PSScriptRoot "parse_agp_pdf.py") $PdfFiles[0].FullName $Preview
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
uv run python (Join-Path $PSScriptRoot "plot_agp_preview.py") $Preview $Chart
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Start-Process $Chart
