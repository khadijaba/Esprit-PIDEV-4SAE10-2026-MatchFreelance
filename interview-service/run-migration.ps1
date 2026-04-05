# Run MySQL migration for freelancing_interviews.
# Usage: .\run-migration.ps1
# Or: .\run-migration.ps1 -MysqlPath "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
param([string]$MysqlPath = "mysql")
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$sqlFile = Join-Path $scriptDir "src\main\resources\db\migration\mysql_create_and_migrate_interviews.sql"
if (-not (Test-Path $sqlFile)) { Write-Error "SQL file not found: $sqlFile"; exit 1 }
Get-Content $sqlFile -Raw | & $MysqlPath -u root
if ($LASTEXITCODE -ne 0) { Write-Host "Migration failed. Check MySQL is running and path is correct."; exit $LASTEXITCODE }
Write-Host "Migration completed."
