@echo off
echo ===============================================================================
echo                     S-EMULATOR REACT CLIENT
echo                        Starting Application
echo ===============================================================================
echo.

echo Installing dependencies...
call npm install
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to install dependencies
    pause
    exit /b 1
)

echo.
echo Dependencies installed successfully!
echo.
echo Starting React development server...
echo The application will open automatically in your browser at http://localhost:3000
echo.
echo NOTE: Keep this window open while using the application
echo       Press Ctrl+C to stop the server
echo.

call npm start

