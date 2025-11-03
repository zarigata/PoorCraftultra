@echo off
echo === Compiling Inventory UI Test ===

REM Create output directory
if not exist "build\test-classes" mkdir build\test-classes

REM Get classpath from gradle
gradle -q dependencies --configuration runtimeClasspath > build\deps.txt

REM Compile only the necessary classes
echo Compiling inventory classes...
javac -cp "build\classes\java\main;%USERPROFILE%\.gradle\caches\modules-2\files-2.1\*\*\*.jar" ^
  -d build\test-classes ^
  src\test\java\com\poorcraft\ultra\inventory\InventoryUIManualTest.java

if errorlevel 1 (
    echo Compilation failed!
    exit /b 1
)

echo.
echo === Running Inventory UI Test ===
echo Press E to open/close inventory
echo.

REM Run the test
java -cp "build\classes\java\main;build\test-classes;%USERPROFILE%\.gradle\caches\modules-2\files-2.1\*\*\*.jar" ^
  -Xmx1G ^
  com.poorcraft.ultra.inventory.InventoryUIManualTest

pause
