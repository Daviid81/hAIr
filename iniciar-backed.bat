
@echo off

title hAIr - Backend

echo ==========================================
echo          INICIANDO BACKEND HAIR
echo ==========================================
echo.

:: Ejecutar el script PowerShell.
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\iniciar-backend.ps1"

echo.
echo ==========================================
echo          BACKEND FINALIZADO
echo ==========================================

pause
