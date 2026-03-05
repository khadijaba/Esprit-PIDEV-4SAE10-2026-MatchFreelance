@echo off
REM Run MySQL migration. Set MYSQL_BIN to your mysql.exe path if mysql is not in PATH.
set MYSQL_BIN=mysql
if defined MYSQL_PATH set MYSQL_BIN=%MYSQL_PATH%
set SCRIPT=%~dp0src\main\resources\db\migration\mysql_create_and_migrate_interviews.sql
"%MYSQL_BIN%" -u root < "%SCRIPT%"
if errorlevel 1 echo Migration failed. Ensure MySQL is running and mysql is in PATH (or set MYSQL_PATH).
if not errorlevel 1 echo Migration completed.
