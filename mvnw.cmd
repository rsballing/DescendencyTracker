@echo off
setlocal

set "MVNW_REPO_DIR=%~dp0"
if "%MVNW_REPO_DIR:~-1%"=="\" set "MVNW_REPO_DIR=%MVNW_REPO_DIR:~0,-1%"

set "MVNW_MAVEN_CMD="

for /f "delims=" %%F in ('dir /b /s "%USERPROFILE%\.m2\wrapper\dists\apache-maven-*-bin\*\apache-maven-*\bin\mvn.cmd" 2^>nul') do (
    set "MVNW_MAVEN_CMD=%%F"
    goto :run
)

if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.3\plugins\maven\lib\maven3\bin\mvn.cmd" (
    set "MVNW_MAVEN_CMD=C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.3\plugins\maven\lib\maven3\bin\mvn.cmd"
    goto :run
)

echo Could not find a local Maven installation for mvnw.cmd. 1>&2
echo Checked %%USERPROFILE%%\.m2\wrapper\dists and IntelliJ IDEA bundled Maven. 1>&2
exit /b 1

:run
call "%MVNW_MAVEN_CMD%" -f "%MVNW_REPO_DIR%\pom.xml" %*
exit /b %ERRORLEVEL%
