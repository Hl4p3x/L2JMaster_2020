@echo off
title Login Server Console : L2JMaster
color 3F

:start
echo Starting L2J Login Server.
echo.

SET ASIGNED_MEMORY=256m

java -Xms128m -Xmx%ASIGNED_MEMORY% -jar login.jar

if ERRORLEVEL 2 goto restart
if ERRORLEVEL 1 goto error
goto end

:restart
echo.
echo Admin Restarted Login Server.
echo.
goto start

:error
echo.
echo Login Server terminated abnormally!
echo.

:end
echo.
echo Login Server Terminated.
echo.
pause