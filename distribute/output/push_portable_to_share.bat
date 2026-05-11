@echo off
setlocal enableextensions

set "TARGET=\\LAPTOP-FRR221SE\qqbot\drilldown"
set "ZIP=%~dp0DrillDown-portable.zip"

if not exist "%ZIP%" (
  echo Missing archive: "%ZIP%"
  exit /b 1
)

pushd "%TARGET%" || (
  echo Failed to open target share: "%TARGET%"
  exit /b 1
)

echo Clearing target contents...
del /f /q * >nul 2>&1
for /d %%D in (*) do rmdir /s /q "%%D"

echo Copying archive...
copy /y "%ZIP%" "%CD%\DrillDown-portable.zip" >nul
if errorlevel 1 (
  echo Copy failed.
  popd
  exit /b 1
)

popd
echo Done.
endlocal
