@echo off
setlocal

set "MAVEN_WRAPPER_DIR=%~dp0.mvn\wrapper"
set "MAVEN_WRAPPER_JAR=%MAVEN_WRAPPER_DIR%\maven-wrapper.jar"
set "MAVEN_WRAPPER_PROPERTIES=%MAVEN_WRAPPER_DIR%\maven-wrapper.properties"

if not exist "%MAVEN_WRAPPER_PROPERTIES%" (
  echo [ERROR] Missing "%MAVEN_WRAPPER_PROPERTIES%"
  exit /b 1
)

for /f "usebackq tokens=1,* delims==" %%A in (`findstr /b /c:"wrapperUrl=" "%MAVEN_WRAPPER_PROPERTIES%"`) do set "WRAPPER_URL=%%B"
if not defined WRAPPER_URL (
  echo [ERROR] wrapperUrl not set in "%MAVEN_WRAPPER_PROPERTIES%"
  exit /b 1
)

if not exist "%MAVEN_WRAPPER_JAR%" (
  echo [INFO] Downloading Maven Wrapper jar...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$p='%MAVEN_WRAPPER_JAR%'; $u='%WRAPPER_URL%'; New-Item -ItemType Directory -Force -Path ([IO.Path]::GetDirectoryName($p)) | Out-Null; (New-Object Net.WebClient).DownloadFile($u,$p)"
  if errorlevel 1 (
    echo [ERROR] Failed downloading Maven Wrapper jar from "%WRAPPER_URL%"
    exit /b 1
  )
)

set "MAVEN_PROJECTBASEDIR=%~dp0"
if "%MAVEN_PROJECTBASEDIR:~-1%"=="\" set "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%"
java -classpath "%MAVEN_WRAPPER_JAR%" -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*

endlocal
