@echo off
title Approval Flow System - Start

echo ============================================
echo   Approval Flow System - Start Script
echo ============================================
echo.

REM Check Java
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Java not found, please install JDK 21
    pause
    exit /b 1
)

REM Check Node.js
where node >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Node.js not found, please install Node.js 20+
    pause
    exit /b 1
)

echo [INFO] Java:
java -version 2>&1 | findstr /i "version"
echo.

echo [INFO] Node.js:
node -v
echo.

REM Set directories
set PROJECT_DIR=%~dp0
set BACKEND_DIR=%PROJECT_DIR%backend
set FRONTEND_DIR=%PROJECT_DIR%frontend

REM Check if frontend dependencies installed
if not exist "%FRONTEND_DIR%\node_modules" (
    echo [INFO] First run, installing frontend dependencies...
    cd /d "%FRONTEND_DIR%"
    call npm install
    cd /d "%PROJECT_DIR%"
)

REM Start backend
echo [START] Starting backend (Spring Boot) on port 8080...
start "Backend" cmd /k "cd /d %BACKEND_DIR% && mvnw.cmd spring-boot:run"

REM Wait for backend
echo [WAIT] Waiting for backend to start (about 15s)...
timeout /t 15 /nobreak >nul

REM Start frontend
echo [START] Starting frontend (Vite) on port 5173...
start "Frontend" cmd /k "cd /d %FRONTEND_DIR% && npm run dev"

echo.
echo ============================================
echo   Services started!
echo ============================================
echo   Frontend: http://localhost:5173
echo   Backend:  http://localhost:8080
echo.
echo   Accounts:
echo     Admin:  admin / 123456
echo     User:   zhangsan / 123456
echo ============================================
echo.
echo [TIP] Close this window will NOT stop the services
echo [TIP] To stop services, run stop.bat
echo.
pause
