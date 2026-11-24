
@echo off
setlocal

cd /d "%~dp0"

echo =====================================
echo       STUDY TA BAI - BUILDING
echo =====================================

if not exist out mkdir out
if exist sources.txt del sources.txt

echo Finding Java files...

for /r "src\main" %%F in (*.java) do (
    echo "%%~fF">>sources.txt
)

echo Fixing Java source paths...

powershell -NoProfile -Command "(Get-Content sources.txt) -replace '\\','/' | Set-Content sources.txt -Encoding ASCII"

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
