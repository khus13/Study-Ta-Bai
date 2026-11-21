@echo off
rem Build and run without Maven. Needs a JDK 17+. Put pdfbox + its dependencies in .\lib for PDF support.
cd /d "%~dp0"
if not exist out mkdir out
dir /s /b src\main\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt || exit /b 1
xcopy /s /y /q src\main\resources out\ > nul
java -cp "out;lib/*" ph.studytabai.Main
