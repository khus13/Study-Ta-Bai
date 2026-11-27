
@echo off
title Study Ta Bai

echo =====================================
echo       STUDY TA BAI - BUILDING
echo =====================================

cd /d "%~dp0"

if not exist out mkdir out

echo.
echo Finding Java files...

if exist sources.txt del sources.txt

for /r "src\main" %%F in (*.java) do (
    set "filepath=%%~fF"
    call set "filepath=%%filepath:\=/%%"
    call echo "%%filepath%%">>sources.txt
)

echo.
echo Compiling Java files...

javac -encoding UTF-8 -d out @sources.txt

if errorlevel 1 (
    echo.
    echo BUILD FAILED!
    pause
    exit /b 1
)

echo.
echo Copying resources...

if exist src\main\resources (
    xcopy /s /y /q src\main\resources out\ > nul
)

echo.
echo Starting Study Ta Bai...

java -cp "out;lib/*" ph.studytabai.Main

pause
