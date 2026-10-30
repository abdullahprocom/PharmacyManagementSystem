@echo off
cd /d "%~dp0"
title PharmaCare Desktop POS

echo ========================================================
echo       Starting PharmaCare Desktop POS Application...
echo ========================================================
echo.

set "PATH=C:\Program Files\Apache NetBeans\java\maven\bin;%PATH%"
set "MAVEN_OPTS=-Xms32m -Xmx256m -XX:+UseSerialGC"

call mvn javafx:run

if %errorlevel% neq 0 (
    echo.
    echo Application exited with an error. Check messages above.
    pause
)
