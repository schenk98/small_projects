$ErrorActionPreference = "Stop"

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $here

function Resolve-PythonCmd {
  # Many scraping deps (notably lxml) often lag on bleeding-edge Python.
  # Prefer a stable interpreter if available.
  foreach ($v in @("3.12", "3.11", "3.10")) {
    try {
      py -$v -c "import sys; print(sys.version)" *> $null
      return "py -$v"
    } catch {}
  }
  return "py"
}

$py = Resolve-PythonCmd

if (!(Test-Path ".venv")) {
  & $py -m venv .venv
}

& .\.venv\Scripts\python -m pip install -U pip
& .\.venv\Scripts\pip install -r requirements.txt

# Playwright is intentionally opt-in. On Python 3.14 it commonly fails (greenlet).
# If you want it, install with:
#   .\.venv\Scripts\pip install -r requirements-browser.txt
#   .\.venv\Scripts\python -m playwright install chromium

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"

& .\.venv\Scripts\python .\src\stankov_scraper\cli.py `
  --seed "https://www.mestostankov.cz/" `
  --out (Join-Path $here "out\$timestamp") `
  --max-pages 2500 `
  --concurrency 8 `
  --delay-ms 200 `
  --download-assets `
  --no-browser-fallback
