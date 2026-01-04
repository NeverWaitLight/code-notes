@echo off
set "port=8111"

@REM 设置服务名称
set "SERVICE_NAME=YlClientPlugin"
@REM 检查服务是否存在
sc query %SERVICE_NAME% > nul 2>&1
if %errorlevel% equ 0 (
    sc delete "%SERVICE_NAME%"
)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%port%"') do (
    taskkill /F /PID %%a
    goto :break_loop
)
:break_loop
pause