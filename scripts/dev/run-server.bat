@echo off
setlocal enabledelayedexpansion
pushd "%~dp0\..\.."
call gradlew.bat :app:run --args="--server --headless"
popd
