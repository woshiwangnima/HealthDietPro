$sessionDir = "C:\Users\WSW\AndroidStudioProjects\HealthDietPro\.superpowers\brainstorm\1782490749158"
$env:BRAINSTORM_DIR = $sessionDir
$env:BRAINSTORM_HOST = "127.0.0.1"
$env:BRAINSTORM_URL_HOST = "localhost"
Remove-Item "$sessionDir\state\server-stopped" -Force -ErrorAction SilentlyContinue
node "C:\Users\WSW\.config\opencode\superpowers\skills\brainstorming\scripts\server.cjs"
