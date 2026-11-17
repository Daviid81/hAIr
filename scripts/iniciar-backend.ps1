# ==========================================
# INICIO AUTOMATICO DEL BACKEND DE HAIR
# ==========================================

# Detener el script si ocurre algun error.
$ErrorActionPreference = "Stop"

# Obtener las rutas principales del proyecto.
$projectRoot = Split-Path -Parent $PSScriptRoot
$backendPath = Join-Path $projectRoot "backend"

# Comprobar que existe la carpeta backend.
if (-not (Test-Path $backendPath)) {
    Write-Host "ERROR: No se encuentra la carpeta backend." -ForegroundColor Red
    exit 1
}

# ==========================================
# LOCALIZAR POSIBLES INSTALACIONES DE JAVA
# ==========================================

$javaCandidates = @()

# Comprobar si JAVA_HOME ya esta configurado.
if ($env:JAVA_HOME) {
    $javaCandidates += $env:JAVA_HOME
}

# Buscar Java desde el PATH.
$javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue

if ($javaCommand) {

    # Consultar la ubicacion de Java sin detener el script
    # porque Java escribe parte de su informacion en stderr.
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"

    $settings = & $javaCommand.Source -XshowSettings:properties -version 2>&1 | Out-String

    $ErrorActionPreference = $previousErrorActionPreference

    # Extraer la propiedad java.home.
    if ($settings -match '(?m)^\s*java\.home\s*=\s*(.+?)\s*$') {
        $javaCandidates += $Matches[1].Trim()
    }

}

# Buscar javac para localizar la carpeta del JDK.
$javacCommand = Get-Command javac.exe -ErrorAction SilentlyContinue

if ($javacCommand) {

    # Obtener la carpeta bin y despues la raiz del JDK.
    $jdkPath = Split-Path -Parent (Split-Path -Parent $javacCommand.Source)

    $javaCandidates += $jdkPath
}

# Comprobar ubicaciones habituales de instalacion.
$searchRoots = @(
    "$env:ProgramFiles\Java",
    "$env:ProgramFiles\Eclipse Adoptium",
    "$env:ProgramFiles\Microsoft",
    "$env:USERPROFILE\.jdks",
    "$env:LOCALAPPDATA\Programs\Eclipse Adoptium"
)

foreach ($root in $searchRoots) {

    if (Test-Path $root) {

        $folders = Get-ChildItem $root -Directory -ErrorAction SilentlyContinue

        foreach ($folder in $folders) {
            $javaCandidates += $folder.FullName
        }
    }
}

# ==========================================
# COMPROBAR QUE SEA JDK 21
# ==========================================

$selectedJava = $null

foreach ($candidate in ($javaCandidates | Select-Object -Unique)) {

    # Comprobar que existen java.exe y javac.exe.
    $javaPath = Join-Path $candidate "bin\java.exe"
    $javacPath = Join-Path $candidate "bin\javac.exe"

    if ((Test-Path $javaPath) -and (Test-Path $javacPath)) {

        # Consultar la version instalada.

        # Consultar la version de Java sin tratar stderr como una excepcion.
        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"

        $versionInfo = & $javaPath -version 2>&1 | Out-String

        $ErrorActionPreference = $previousErrorActionPreference

        # Aceptar exclusivamente Java 21.
        if ($versionInfo -match 'version "21(?:\.|")') {

            $selectedJava = $candidate
            break
        }
    }
}

# Detener la ejecucion si no encontramos Java 21.
if (-not $selectedJava) {

    Write-Host ""
    Write-Host "ERROR: No se ha encontrado un JDK 21." -ForegroundColor Red
    Write-Host ""
    Write-Host "Instala Java JDK 21 desde:"
    Write-Host "https://adoptium.net/temurin/releases/?version=21"
    Write-Host ""

    exit 1
}

# ==========================================
# CONFIGURAR JAVA TEMPORALMENTE
# ==========================================

# Estas variables solo afectan a este proceso.
$env:JAVA_HOME = $selectedJava
$env:Path = "$selectedJava\bin;$env:Path"

Write-Host ""
Write-Host "Java 21 encontrado correctamente." -ForegroundColor Green
Write-Host "JAVA_HOME: $env:JAVA_HOME"

# ==========================================
# ARRANCAR SPRING BOOT
# ==========================================

Write-Host ""
Write-Host "Iniciando backend de hAIr..." -ForegroundColor Cyan
Write-Host ""

# Situarse en la carpeta del backend.
Push-Location $backendPath

try {

    # Ejecutar Maven Wrapper.
    & .\mvnw.cmd spring-boot:run

    # Obtener el resultado de Maven.
    $exitCode = $LASTEXITCODE

}
finally {

    # Recuperar la ubicacion anterior.
    Pop-Location
}

# Devolver el resultado de la ejecucion.
exit $exitCode
