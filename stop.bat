@echo off
title Approval Flow System - Stop

echo ============================================
echo   Approval Flow System - Stop Script
echo ============================================
echo.

echo [STOP] Stopping backend (port 8080)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    taskkill /F /PID %%a >nul 2>nul
    echo   Killed PID: %%a
)

echo [STOP] Stopping frontend (port 5173)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :5173 ^| findstr LISTENING') do (
    taskkill /F /PID %%a >nul 2>nul
    echo   Killed PID: %%a
)

echo.
echo [DONE] All services stopped
timeout /t 2 /nobreak >nul
