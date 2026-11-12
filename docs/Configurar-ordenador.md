# hAIr – Guía de instalación y ejecución

## 1. Requisitos previos

Antes de ejecutar el proyecto, es necesario tener instaladas las siguientes herramientas:

| Herramienta | Versión requerida |
|---|---|
| Java JDK | 21 |
| Node.js | Versión LTS |
| npm | Incluido con Node.js |
| MySQL Server | Versión compatible con el proyecto |
| Git | Versión actual |

También se recomienda utilizar IntelliJ IDEA para trabajar con el backend y Visual Studio Code para el frontend.

**Importante:** No es necesario instalar Maven manualmente. El backend incluye Maven Wrapper (`mvnw`), que descarga y utiliza la versión de Maven configurada en el proyecto.

## 2. Descargar el proyecto

Abrir una terminal y situarse en la carpeta donde se quiera guardar el proyecto.

Ejecutar:

```bash
git clone URL_DEL_REPOSITORIO
```

Entrar en la carpeta del proyecto:

```bash
cd hAIr
```

La estructura principal del proyecto es:

```text
hAIr/
├── backend/
├── frontend/
├── database/
└── docs/
```

## 3. Configurar Java 21

### 3.1. Instalar Java

Descargar e instalar el JDK 21.

Se puede comprobar la instalación ejecutando:

```bash
java -version
```

Debe aparecer una versión 21 de Java.

### 3.2. Configurar JAVA_HOME en Windows

Es necesario crear la variable de entorno `JAVA_HOME`, que indica dónde está instalado el JDK.

Primero, localizar la carpeta de instalación de Java.

Por ejemplo:

```text
C:\Program Files\Java\jdk-21
```

La ruta puede variar dependiendo de dónde se haya instalado.

**Configuración mediante Windows:**

1. Abrir el menú Inicio.
2. Buscar «Editar las variables de entorno del sistema».
3. Entrar en «Variables de entorno».
4. En «Variables de usuario», seleccionar «Nueva».
5. Escribir como nombre:

```text
JAVA_HOME
```

6. Como valor, introducir la ruta de instalación del JDK 21. Por ejemplo:

```text
C:\Program Files\Java\jdk-21
```

7. Aceptar los cambios.
8. Buscar la variable `Path` dentro de las variables de usuario.
9. Editarla y añadir una nueva entrada:

```text
%JAVA_HOME%\bin
```

10. Aceptar todas las ventanas.

**Alternativa mediante CMD:**

También se puede configurar utilizando:

```cmd
setx JAVA_HOME "C:\Program Files\Java\jdk-21"
```

La ruta debe sustituirse por la instalación real de cada ordenador.

Después de ejecutar el comando, hay que cerrar la terminal y abrir una nueva.

### 3.3. Comprobar Java

Ejecutar:

```cmd
echo %JAVA_HOME%
```

Después:

```cmd
java -version
```

La primera orden debe mostrar la ruta del JDK y la segunda debe mostrar Java 21.

## 4. Configurar Node.js y npm

### 4.1. Instalar Node.js

Descargar e instalar la versión LTS de Node.js desde su página oficial:

https://nodejs.org/

Durante la instalación, mantener las opciones predeterminadas.

### 4.2. Comprobar la instalación

Abrir una terminal nueva y ejecutar:

```cmd
node -v
```

```cmd
npm -v
```

Ambos comandos deben mostrar sus respectivas versiones.

### 4.3. Solucionar el bloqueo de npm en PowerShell

En algunos ordenadores, PowerShell puede impedir la ejecución de `npm.ps1` por su política de seguridad.

Hay dos alternativas:

**Alternativa A: utilizar CMD**

Abrir el Símbolo del sistema y ejecutar los comandos normalmente:

```cmd
npm install
```

**Alternativa B: permitir scripts locales en PowerShell**

Abrir PowerShell y ejecutar:

```powershell
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```

Cuando solicite confirmación, aceptar el cambio.

Después, cerrar y abrir una nueva terminal de PowerShell.

A partir de ese momento se podrá utilizar `npm` directamente.

## 5. Configurar la base de datos MySQL

El proyecto utiliza MySQL como sistema gestor de bases de datos.

Para preparar el entorno:

1. Instalar MySQL Server.
2. Iniciar el servicio de MySQL.
3. Crear la base de datos correspondiente al proyecto.
4. Ejecutar los scripts SQL disponibles en la carpeta `database/`, siguiendo el orden indicado por el equipo.
5. Configurar las credenciales de conexión del backend.

La configuración de conexión se encuentra en:

```text
backend/src/main/resources/application.properties
```

Los datos de conexión deben coincidir con los de la instalación local de cada integrante.

**Importante:** No se deben subir contraseñas personales ni credenciales privadas al repositorio. Cada integrante debe configurar sus propias credenciales siguiendo las instrucciones del equipo.

## 6. Preparar el backend

Abrir una terminal y acceder a la carpeta:

```cmd
cd hAIr\backend
```

### 6.1. Comprobar Maven Wrapper

Ejecutar:

```cmd
mvnw.cmd -version
```

Debe aparecer la versión de Maven y Java 21.

En PowerShell también se puede utilizar:

```powershell
.\mvnw.cmd -version
```

### 6.2. Descargar las dependencias

Ejecutar:

```cmd
mvnw.cmd clean install
```

Este comando descarga las dependencias necesarias, compila el proyecto y ejecuta las pruebas configuradas.

### 6.3. Arrancar Spring Boot

Ejecutar:

```cmd
mvnw.cmd spring-boot:run
```

Si todo funciona correctamente, Spring Boot iniciará el servidor backend.

La dirección local prevista es:

```text
http://localhost:8080
```

No cerrar esta terminal mientras se esté utilizando el backend.

## 7. Preparar el frontend

Abrir una segunda terminal y acceder a la carpeta:

```cmd
cd hAIr\frontend
```

### 7.1. Instalar dependencias

Ejecutar:

```cmd
npm install
```

Este comando instala las dependencias definidas en `package.json`.

No es necesario instalar React, Vite u otras dependencias manualmente si ya están declaradas en el proyecto.

### 7.2. Arrancar React

Ejecutar:

```cmd
npm run dev
```

Si se utiliza PowerShell y aparece el error de ejecución de scripts, se puede utilizar:

```powershell
npm.cmd run dev
```

Vite mostrará la dirección local de la aplicación, normalmente:

```text
http://localhost:5173
```

No cerrar esta terminal mientras se esté utilizando el frontend.

## 8. Ejecutar la aplicación

Para trabajar con hAIr, es necesario mantener abiertos dos procesos:

| Terminal | Carpeta | Comando |
|---|---|---|
| Terminal 1 | `backend/` | `mvnw.cmd spring-boot:run` |
| Terminal 2 | `frontend/` | `npm run dev` |

Una vez iniciados ambos procesos, abrir el navegador y acceder a:

```text
http://localhost:5173
```

El frontend realizará las peticiones HTTP necesarias al backend.

## 9. Problemas frecuentes

### Error: JAVA_HOME environment variable is not defined correctly

**Causa:** Java no está configurado correctamente.

**Solución:** comprobar que `JAVA_HOME` apunta a la carpeta raíz del JDK 21, no a la carpeta `bin`.

Ejemplo correcto:

```text
C:\Program Files\Java\jdk-21
```

Ejemplo incorrecto:

```text
C:\Program Files\Java\jdk-21\bin
```

Después de modificar la variable, abrir una terminal nueva.

### Error: npm.ps1 cannot be loaded because running scripts is disabled

**Causa:** PowerShell está bloqueando la ejecución de scripts.

**Solución:** utilizar CMD o configurar PowerShell con:

```powershell
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```

### Error: Address already in use

**Causa:** El puerto que necesita la aplicación ya está siendo utilizado por otro proceso.

**Solución:** comprobar si ya existe otra instancia del backend o frontend en ejecución.

### Error: Cannot connect to MySQL

**Causa:** El servidor MySQL no está iniciado o los datos de conexión no son correctos.

**Solución:** comprobar que MySQL está funcionando, que la base de datos existe y que las credenciales configuradas son correctas.

### Error: Failed to resolve dependencies

**Causa:** Maven o npm no han podido descargar alguna dependencia.

**Solución:** comprobar la conexión a Internet y volver a ejecutar el comando correspondiente.

Para Maven:

```cmd
mvnw.cmd clean install
```

Para npm:

```cmd
npm install
```

## 10. Recomendaciones para trabajar en equipo

- Antes de comenzar a trabajar, ejecutar `git pull` para obtener los últimos cambios.
- No modificar archivos que correspondan a otro integrante sin coordinarlo previamente.
- No subir contraseñas, tokens ni claves privadas al repositorio.
- No subir las carpetas `node_modules/`, `dist/` ni `target/`.
- No modificar las versiones de las dependencias sin comunicarlo al equipo.
- Cuando se añadan nuevas dependencias al proyecto, actualizar y subir los archivos de configuración correspondientes.

**Resumen de comandos principales:**

```bash
# Descargar el proyecto
git clone URL_DEL_REPOSITORIO

# Entrar en el backend
cd hAIr/backend

# Comprobar Maven y Java
mvnw.cmd -version

# Compilar el backend
mvnw.cmd clean install

# Arrancar el backend
mvnw.cmd spring-boot:run
```

En otra terminal:

```bash
# Entrar en el frontend
cd hAIr/frontend

# Instalar dependencias
npm install

# Arrancar el frontend
npm run dev
```

Con estos pasos, cada integrante podrá preparar su entorno local y ejecutar hAIr de manera independiente.