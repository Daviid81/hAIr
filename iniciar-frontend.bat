
@echo off

title hAIr - Frontend

echo ==========================================
echo          INICIANDO FRONTEND HAIR
echo ==========================================
echo.

:: Acceder a la carpeta frontend.
cd /d "%~dp0frontend"

:: Comprobar que Node.js esta instalado.
where node >nul 2>&1

if errorlevel 1 (
    echo ERROR: Node.js no esta instalado o no esta disponible.
    pause
    exit /b 1
)

:: Comprobar que npm esta disponible.
where npm.cmd >nul 2>&1

if errorlevel 1 (
    echo ERROR: npm no esta disponible.
    pause
    exit /b 1
)

:: Instalar dependencias si no existen.
if not exist "node_modules" (
    echo Instalando dependencias...
    call npm.cmd install

    if errorlevel 1 (
        echo ERROR: No se pudieron instalar las dependencias.
        pause
        exit /b 1
    )
)

:: Arrancar Vite.
echo Iniciando frontend...
call npm.cmd run dev

pause
