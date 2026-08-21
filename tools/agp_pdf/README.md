# AGP PDF Tools

This directory contains the reviewed workflow for recovering CGM samples from
the vector paths in an AGP PDF's `每日血糖曲线` section.

- `input/`: place exactly one source AGP PDF here.
- `01-GeneratePreview.ps1`: extracts the PDF to audit JSON, renders a PNG, and
  opens the PNG. It never connects to an Android device.
- `02-ImportConfirmedPreview.ps1`: requires typed `IMPORT`, stages the already
  reviewed JSON to the only connected device, and asks the app to atomically
  import it into its current user.
- `parse_agp_pdf.py`: extracts readings to the audit JSON.
- `plot_agp_preview.py`: renders the JSON into daily charts for visual review.

Move each user-provided source PDF into `input/`. Keep the original outside
version control if it contains personal health information.

Run from the repository root:

```powershell
.\tools\agp_pdf\01-GeneratePreview.ps1
# Review tools/agp_pdf/output/agp_preview.png.
.\tools\agp_pdf\02-ImportConfirmedPreview.ps1
```

Before running the second script, ensure the test device already has the debug
APK containing the AGP importer installed, then open the app and select the
intended current user. The script presents these requirements, waits for Enter,
and then reports either the inserted record count or the app error in the same
terminal. It automatically detects the only connected ADB device and never asks
for a device ID. When double-clicked, it keeps the PowerShell window open after
both success and failure until Enter is pressed.

Review requirements before any import implementation is run:

1. Every day has the expected number of points based on the PDF active-time
   summary; partial first/last days are allowed and must not be padded.
2. Each reconstructed daily mean is within `0.02 mmol/L` of the PDF MBG.
3. The rendered curve visually agrees with the matching PDF daily chart.
4. The preview JSON is an audit artifact, not an Android archive format.
5. `02-ImportConfirmedPreview.ps1` only accepts one connected device. It never
   selects a user from the PDF: the app resolves its current user at import
   time, asks the user to select one existing blood-glucose source setting,
   merges records in one archive transaction, skips exact timestamp/value
   duplicates, and does not replay historical alerts.
