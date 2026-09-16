@echo off
REM One-shot helper: stop local Windows Redis service and set it to manual start.
REM Right-click this file and choose "Run as administrator".
REM To restore later: net start Redis

net stop Redis
sc config Redis start= demand

echo.
echo [OK] Local Redis service stopped and set to manual start.
echo      To restore it later, run: net start Redis
pause
