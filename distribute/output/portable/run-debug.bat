@echo off
setlocal
cd /d "%~dp0"
set "JAVA_EXE=%~dp0jre\bin\java.exe"
if not exist "%JAVA_EXE%" set "JAVA_EXE=java"
"%JAVA_EXE%" -Dfile.encoding=UTF-8 -jar "%~dp0DrillDown.jar" debug
endlocal
