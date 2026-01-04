@echo off
set "port=8111"
set "jarName=yl-client-plugin.jar"

@REM 设置服务名称
set "SERVICE_NAME=YlClientPlugin"
@REM 设置服务显示名称
set "DISPLAY_NAME=Yl Client Plugin"
@REM 设置服务描述
set "DESCRIPTION=Yl client plugin"
@REM 设置服务的可执行文件路径
set "BIN_PATH=%~dp0install.bat"

REM 检查服务是否存在
sc query %SERVICE_NAME% > nul 2>&1
if %errorlevel% neq 0 (
    @REM 创建服务
    sc create "%SERVICE_NAME%" binPath= "%BIN_PATH%" start= auto type= own DisplayName= "%DISPLAY_NAME%"
    @REM 设置服务描述
    sc description "%SERVICE_NAME%" "%DESCRIPTION%"
    @REM 启动服务
    sc start "%SERVICE_NAME%"
    GOTO :EOF
)

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%port%"') do (
    taskkill /F /PID %%a
    timeout /T 5 > null
    goto :break_loop
)
:break_loop
start "yl-client-plugin" "%~dp0jre\bin\javaw" -jar "%~dp0lib\yl-client-plugin.jar"

GOTO :EOF