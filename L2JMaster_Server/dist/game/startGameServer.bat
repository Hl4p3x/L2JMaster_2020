@echo off
title Game Server Console : L2JMaster
color 3F
:start
echo.

SET ASIGNED_MEMORY=6144m

java -Xms3072m -Xmx%ASIGNED_MEMORY% -jar core.jar

if ERRORLEVEL 2 goto restart
if ERRORLEVEL 1 goto error
goto end

:restart
echo.
echo Admin Restarted Game Server.
echo.
goto start

:error
echo.
echo Game Server Terminated Abnormally!
echo.

:end
echo.
echo Game Server Terminated.
echo.
pause