# Demarre evaluation-reports-py (depuis ce dossier).
# Sous Windows, --host 0.0.0.0 peut provoquer WinError 10013 ; en local 127.0.0.1 suffit.
# Autre port : $env:EVAL_REPORTS_PORT = 8091 ; .\run.ps1
# (Penser a aligner Gateway / proxy sur le meme port.)
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$port = if ($env:EVAL_REPORTS_PORT) { [int]$env:EVAL_REPORTS_PORT } else { 8090 }

if (-not (Test-Path ".\.venv\Scripts\python.exe")) {
    Write-Host "Creation du venv .venv ..."
    py -3 -m venv .venv
}
& .\.venv\Scripts\python.exe -m pip install --upgrade pip setuptools wheel
& .\.venv\Scripts\pip.exe install -r requirements.txt

Write-Host "Service : http://127.0.0.1:${port}/docs |  Sante : http://127.0.0.1:${port}/api/evaluation-reports/health"
& .\.venv\Scripts\python.exe -m uvicorn app.main:app --reload --host 127.0.0.1 --port $port
