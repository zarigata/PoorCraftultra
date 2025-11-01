@echo off
REM Run wrapper script for Windows
REM Detects Gradle or Maven and runs appropriate run command

echo === Starting Poorcraft Ultra ===

REM Show Java version
where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ERROR: Java not found in PATH
    exit /b 1
)

echo Java version:
java -version

REM Detect and run build tool
if exist "gradlew.bat" (
    echo Using Gradle wrapper
    call gradlew.bat run
) else if exist "mvnw.cmd" (
    echo Using Maven wrapper ^(fallback^)
    call mvnw.cmd exec:java
) else (
    echo ERROR: No build tool found ^(gradlew.bat or mvnw.cmd^)
    exit /b 1
)
