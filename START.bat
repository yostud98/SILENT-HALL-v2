@echo off
echo ============================================
echo   UMAT AI Quiz System — Quick Launcher
echo ============================================
echo.

:MENU
echo Select what to start:
echo  1. Backend (Spring Boot)
echo  2. Frontend (HTTP Server)
echo  3. Electron Browser (Dev)
echo  4. Electron Browser (Production)
echo  5. AI Proctoring Setup
echo  6. All (Backend + Frontend)
echo  0. Exit
echo.
set /p choice="Enter choice: "

if "%choice%"=="1" goto BACKEND
if "%choice%"=="2" goto FRONTEND
if "%choice%"=="3" goto ELECTRON_DEV
if "%choice%"=="4" goto ELECTRON_PROD
if "%choice%"=="5" goto AI_SETUP
if "%choice%"=="6" goto ALL
if "%choice%"=="0" exit
goto MENU

:BACKEND
cd backend
start "UMAT Backend" cmd /k "mvn spring-boot:run"
cd ..
goto MENU

:FRONTEND
cd frontend
start "UMAT Frontend" cmd /k "python -m http.server 3000"
cd ..
goto MENU

:ELECTRON_DEV
cd electron
start "UMAT Electron" cmd /k "npm run dev"
cd ..
goto MENU

:ELECTRON_PROD
cd electron
start "UMAT Electron Kiosk" cmd /k "npm start"
cd ..
goto MENU

:AI_SETUP
cd ai
pip install -r requirements.txt
python download_model.py
pause
goto MENU

:ALL
cd backend
start "UMAT Backend" cmd /k "mvn spring-boot:run"
cd ..\frontend
start "UMAT Frontend" cmd /k "python -m http.server 3000"
cd ..
echo.
echo Both services started!
echo Backend: http://localhost:8080/api
echo Frontend: http://localhost:3000
echo.
pause
goto MENU
