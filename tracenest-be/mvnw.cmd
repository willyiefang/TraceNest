@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script, version 3.3.2
@REM ----------------------------------------------------------------------------
@ECHO OFF
SETLOCAL

set MAVEN_WRAPPER_DIR=%~dp0.mvn\wrapper
set MAVEN_WRAPPER_JAR=%MAVEN_WRAPPER_DIR%\maven-wrapper.jar
set MAVEN_WRAPPER_PROPERTIES=%MAVEN_WRAPPER_DIR%\maven-wrapper.properties

if not exist "%MAVEN_WRAPPER_PROPERTIES%" (
  echo [ERROR] Missing %MAVEN_WRAPPER_PROPERTIES%
  exit /b 1
)

for /f "usebackq delims==" %%A in (`findstr /b /c:"wrapperUrl=" "%MAVEN_WRAPPER_PROPERTIES%"`) do set WRAPPER_URL=%%B
for /f "usebackq delims==" %%A in (`findstr /b /c:"distributionUrl=" "%MAVEN_WRAPPER_PROPERTIES%"`) do set DIST_URL=%%B

if "%WRAPPER_URL%"=="" (
  echo [ERROR] wrapperUrl not set in %MAVEN_WRAPPER_PROPERTIES%
  exit /b 1
)

if not exist "%MAVEN_WRAPPER_JAR%" (
  echo [INFO] Downloading Maven Wrapper jar...
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$p='%MAVEN_WRAPPER_JAR%'; $u='%WRAPPER_URL%';" ^
    "New-Item -ItemType Directory -Force -Path ([IO.Path]::GetDirectoryName($p)) | Out-Null;" ^
    "(New-Object Net.WebClient).DownloadFile($u,$p)"
  if errorlevel 1 (
    echo [ERROR] Failed downloading Maven Wrapper jar from %WRAPPER_URL%
    exit /b 1
  )
)

set MAVEN_PROJECTBASEDIR=%~dp0

java -classpath "%MAVEN_WRAPPER_JAR%" ^
  -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" ^
  org.apache.maven.wrapper.MavenWrapperMain %*

ENDLOCAL
