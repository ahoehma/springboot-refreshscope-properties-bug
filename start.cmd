@echo off
REM Builds both modules then starts SBA (9090) and app (8080) in two new windows.
REM Open http://localhost:9090 after a few seconds.

cd /d "%~dp0"

echo === Building both modules ===
call mvn -q -DskipTests install
if errorlevel 1 (
  echo BUILD FAILED
  exit /b 1
)

echo === Starting SBA server on port 9090 ===
start "SBA :9090" cmd /k "cd /d %~dp0sba-service && mvn -q spring-boot:run"

echo === Waiting 8s for SBA to come up ===
timeout /t 8 /nobreak > nul

echo === Starting app service on port 8080 ===
start "APP :8080" cmd /k "cd /d %~dp0app-service && mvn -q spring-boot:run"

echo.
echo Open http://localhost:9090 in a few seconds to see the app-service registered.
echo Then drill into Instances -^> app-service -^> Configuration Properties.
