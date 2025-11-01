@echo off
setlocal enabledelayedexpansion

echo === Poorcraft Ultra Asset Generator ===
echo.

REM Detect Python command
set PYTHON_CMD=
where py >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    set PYTHON_CMD=py
) else (
    where python >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        set PYTHON_CMD=python
    ) else (
        where python3 >nul 2>&1
        if %ERRORLEVEL% EQU 0 (
            set PYTHON_CMD=python3
        )
    )
)

if "%PYTHON_CMD%"=="" (
    echo ERROR: Python not found. Please install Python 3.9 or higher.
    exit /b 1
)

REM Show Python version
echo Python detected: %PYTHON_CMD%
%PYTHON_CMD% --version
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to get Python version
    exit /b 1
)
echo.

REM Default arguments
set "USER_OUTPUT_BASE="
set "USER_SEED="
set "USER_FORCE=0"

REM Resolve important directories
set "SCRIPT_DIR=%~dp0"
for %%i in ("%~dp0..") do set "SCRIPTS_DIR=%%~fi"
for %%i in ("%~dp0..\..") do set "PROJECT_DIR=%%~fi"
set "ORIGINAL_DIR=%CD%"

:parse_args
if "%~1"=="" goto args_done
if /I "%~1"=="--output-base" (
    set "USER_OUTPUT_BASE=%~2"
    shift
    shift
    goto parse_args
)
if /I "%~1"=="--seed" (
    set "USER_SEED=%~2"
    shift
    shift
    goto parse_args
)
if /I "%~1"=="--force" (
    set "USER_FORCE=1"
    shift
    goto parse_args
)
echo WARNING: Unknown argument %~1 ignored
shift
goto parse_args

:args_done

REM Paths
set "VENV_DIR=%PROJECT_DIR%\tools\assets\.venv"
set "TOOLS_DIR=%PROJECT_DIR%\tools\assets"
set "ASSETS_DIR=%PROJECT_DIR%\assets"
set "VENV_PYTHON=%VENV_DIR%\Scripts\python.exe"

REM Create virtual environment if missing
if not exist "%VENV_DIR%" (
    echo Creating virtual environment...
    %PYTHON_CMD% -m venv "%VENV_DIR%"
    if %ERRORLEVEL% NEQ 0 (
        echo ERROR: Failed to create virtual environment
        exit /b 1
    )
    echo [OK] Virtual environment created
    echo.
)

REM Activate virtual environment
echo Activating virtual environment...
call "%VENV_DIR%\Scripts\activate.bat"
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to activate virtual environment
    exit /b 1
)
set "PYTHON=%VENV_PYTHON%"
set "PATH=%VENV_DIR%\Scripts;%PATH%"
set "PYTHONPATH=%TOOLS_DIR%;%VENV_DIR%\Lib\site-packages%"

REM Upgrade pip
echo Upgrading pip...
"%VENV_PYTHON%" -m pip install --quiet --upgrade pip==24.2 setuptools==69.5.1 wheel
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to upgrade pip
    call deactivate
    exit /b 1
)

REM Install requirements
echo Installing dependencies...
"%VENV_PYTHON%" -m pip install -r "%TOOLS_DIR%\requirements.txt" --quiet
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to install dependencies
    call deactivate
    exit /b 1
)
echo [OK] Dependencies installed
echo.

REM Run generator
echo Generating assets...
cd /d "%TOOLS_DIR%"

set "OUTPUT_BASE_ARG=%USER_OUTPUT_BASE%"
if "%OUTPUT_BASE_ARG%"=="" set "OUTPUT_BASE_ARG=%ASSETS_DIR%"

set "SEED_ARG=%USER_SEED%"
if "%SEED_ARG%"=="" set "SEED_ARG=42"

set "FORCE_FLAG="
if "%USER_FORCE%"=="1" set "FORCE_FLAG=--force"

"%VENV_PYTHON%" generate_all.py --output-base "%OUTPUT_BASE_ARG%" --seed "%SEED_ARG%" %FORCE_FLAG%
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Asset generation failed
    cd /d "%ORIGINAL_DIR%"
    call deactivate
    exit /b 1
)

REM Deactivate venv
cd /d "%ORIGINAL_DIR%"
call deactivate

echo.
echo === Assets generated successfully! ===
echo Output directory: %OUTPUT_BASE_ARG%
echo.

exit /b 0
