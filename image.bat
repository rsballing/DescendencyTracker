@echo off
setlocal

echo =======================================================
echo Setting up Environment Variables...
echo =======================================================

set "PROJECT_ROOT=C:\Users\Owner\IdeaProjects\DescendencyTracker"
set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.10"
set "WIX_HOME=C:\Program Files (x86)\WiX Toolset v3.11\bin"
set "PATH=%JAVA_HOME%\bin;%WIX_HOME%;%PATH%"

REM CHANGE THESE THREE LINES TO MATCH YOUR ACTUAL FILES
set "ARTIFACT_DIR=%PROJECT_ROOT%\out\artifacts\DescendencyTracker_jar"
set "MAIN_JAR=DescendencyTracker.jar"
set "ICON_FILE=%PROJECT_ROOT%\DesTracker.ico"

echo.
echo =======================================================
echo Verifying tools...
echo =======================================================
where jpackage
if errorlevel 1 (
  echo ERROR: jpackage was not found.
  pause
  exit /b 1
)

if not exist "%ARTIFACT_DIR%" (
  echo ERROR: Artifact directory not found:
  echo %ARTIFACT_DIR%
  pause
  exit /b 1
)

if not exist "%ARTIFACT_DIR%\%MAIN_JAR%" (
  echo ERROR: Main JAR not found:
  echo %ARTIFACT_DIR%\%MAIN_JAR%
  pause
  exit /b 1
)

if not exist "%ICON_FILE%" (
  echo ERROR: Icon file not found:
  echo %ICON_FILE%
  pause
  exit /b 1
)

echo.
echo =======================================================
echo Preparing output folders...
echo =======================================================
if exist "%PROJECT_ROOT%\dist\app-image" rmdir /s /q "%PROJECT_ROOT%\dist\app-image"
if exist "%PROJECT_ROOT%\dist\installer" rmdir /s /q "%PROJECT_ROOT%\dist\installer"
mkdir "%PROJECT_ROOT%\dist\app-image"
mkdir "%PROJECT_ROOT%\dist\installer"

echo.
echo =======================================================
echo Step 1: Building app image...
echo =======================================================
jpackage ^
  --type app-image ^
  --input "%ARTIFACT_DIR%" ^
  --dest "%PROJECT_ROOT%\dist\app-image" ^
  --name "DescendencyTracker" ^
  --main-class family.balling.descendencytracker.app.AppLauncher ^
  --main-jar "%MAIN_JAR%" ^
  --icon "%ICON_FILE%" ^
  --app-version 1.0.0 ^
  --vendor "Riley Balling"

if errorlevel 1 (
  echo ERROR: app-image build failed.
  pause
  exit /b 1
)

echo.
echo =======================================================
echo Step 2: Building Windows installer...
echo =======================================================
jpackage ^
  --type exe ^
  --input "%ARTIFACT_DIR%" ^
  --dest "%PROJECT_ROOT%\dist\installer" ^
  --name "DescendencyTracker" ^
  --main-class family.balling.descendencytracker.app.AppLauncher ^
  --main-jar "%MAIN_JAR%" ^
  --icon "%ICON_FILE%" ^
  --app-version 1.0.0 ^
  --vendor "Riley Balling" ^
  --win-per-user-install ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser ^
  --win-menu-group "DescendencyTracker"

if errorlevel 1 (
  echo ERROR: installer build failed.
  pause
  exit /b 1
)

echo.
echo =======================================================
echo Build Complete!
echo App image:  %PROJECT_ROOT%\dist\app-image
echo Installer:  %PROJECT_ROOT%\dist\installer
echo =======================================================
pause